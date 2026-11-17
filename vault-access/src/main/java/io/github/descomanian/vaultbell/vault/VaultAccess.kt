package io.github.descomanian.vaultbell.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/** A Markdown file inside the vault. */
data class VaultFile(
    val documentUri: Uri,
    /** Path relative to the vault root with forward slashes, e.g. `Daily/2026-11-14.md`. */
    val relativePath: String,
    val lastModified: Long
)

data class VaultInfo(
    val displayName: String,
    val hasObsidianFolder: Boolean
)

/**
 * Include/exclude folders, relative to the vault root (e.g. `Daily`, `Archiv/2024`).
 * Empty [include] means the whole vault.
 */
data class ScanFilter(
    val include: List<String> = emptyList(),
    val exclude: List<String> = emptyList()
) {
    private val inc = include.map(::normalize).filter { it.isNotEmpty() }
    private val exc = exclude.map(::normalize).filter { it.isNotEmpty() }

    fun acceptsFile(relativePath: String): Boolean {
        if (exc.any { relativePath.isInside(it) }) return false
        return inc.isEmpty() || inc.any { relativePath.isInside(it) }
    }

    /** Whether a directory can contain accepted files (used to prune the walk early). */
    fun mayContain(relativeDir: String): Boolean {
        if (exc.any { relativeDir.isInside(it) }) return false
        return inc.isEmpty() || inc.any { relativeDir.isInside(it) || it.isInside(relativeDir) }
    }

    private fun String.isInside(folder: String) = this == folder || startsWith("$folder/")

    private companion object {
        fun normalize(path: String) = path.trim().trim('/').replace('\\', '/')
    }
}

/**
 * Storage Access Framework access to the vault.
 * Uses [DocumentsContract] queries directly (much faster than DocumentFile for large trees).
 * No MANAGE_EXTERNAL_STORAGE, no file paths.
 */
class VaultAccess(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val backupDir = File(context.applicationContext.cacheDir, "write-backup")

    fun persistPermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(treeUri, flags)
    }

    fun releasePermission(treeUri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    fun hasReadPermission(treeUri: Uri): Boolean = resolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }

    fun hasWritePermission(treeUri: Uri): Boolean = resolver.persistedUriPermissions.any { it.uri == treeUri && it.isWritePermission }

    /** Reads the folder name and checks for a `.obsidian` sub-folder. */
    fun inspect(treeUri: Uri): VaultInfo {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val name =
            resolver.query(rootUri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: rootId.substringAfterLast(':').substringAfterLast('/')
        val hasObsidian = children(treeUri, rootId).any { it.name == ".obsidian" && it.isDirectory }
        return VaultInfo(displayName = name, hasObsidianFolder = hasObsidian)
    }

    /** Walks the tree and returns all `.md` files accepted by [filter]. Hidden folders (`.obsidian`, `.trash`, `.git`) are skipped. */
    fun listMarkdownFiles(
        treeUri: Uri,
        filter: ScanFilter = ScanFilter()
    ): List<VaultFile> {
        val result = ArrayList<VaultFile>()
        val stack = ArrayDeque<Pair<String, String>>() // documentId to relative dir
        stack.addLast(DocumentsContract.getTreeDocumentId(treeUri) to "")
        while (stack.isNotEmpty()) {
            val (docId, dir) = stack.removeLast()
            for (child in children(treeUri, docId)) {
                if (child.name.startsWith(".")) continue
                val path = if (dir.isEmpty()) child.name else "$dir/${child.name}"
                if (child.isDirectory) {
                    if (filter.mayContain(path)) stack.addLast(child.id to path)
                } else if (child.name.endsWith(".md", ignoreCase = true) && filter.acceptsFile(path)) {
                    result +=
                        VaultFile(
                            documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.id),
                            relativePath = path,
                            lastModified = child.lastModified
                        )
                }
            }
        }
        return result
    }

    /** Lenient read for scanning. Files above [MAX_FILE_BYTES] throw [FileTooLargeException] and are skipped by the scan. */
    fun readText(documentUri: Uri): String = readBytes(documentUri).toString(Charsets.UTF_8)

    fun readBytes(
        documentUri: Uri,
        maxBytes: Int = MAX_FILE_BYTES
    ): ByteArray =
        resolver.openInputStream(documentUri)?.use { readLimited(it, maxBytes) }
            ?: throw IOException("Cannot open $documentUri")

    /**
     * Replaces the file content for an explicit user action (write-back).
     *
     * SAF offers no atomic replace (no rename-over, and delete + create would change the document identity
     * that sync tools track). So: back up [original] to the app cache, write, read back and compare.
     * On any mismatch the original bytes are written back. The backup is deleted unless the restore failed too.
     */
    fun replaceVerified(
        documentUri: Uri,
        original: ByteArray,
        updated: ByteArray
    ): WriteOutcome {
        val backup = backup(original)
        val outcome =
            VerifiedWrite(
                write = { bytes -> write(documentUri, bytes) },
                read = { readBytes(documentUri, maxOf(MAX_FILE_BYTES, original.size, updated.size)) }
            ).replace(original, updated)
        if (outcome != WriteOutcome.DAMAGED) backup?.delete()
        return outcome
    }

    private fun write(
        documentUri: Uri,
        bytes: ByteArray
    ) {
        val out = resolver.openOutputStream(documentUri, "wt") ?: throw IOException("Cannot write $documentUri")
        out.use { it.write(bytes) }
    }

    private fun backup(bytes: ByteArray): File? =
        runCatching {
            backupDir.mkdirs()
            File(backupDir, "note-${System.currentTimeMillis()}.md.bak").apply { writeBytes(bytes) }
        }.getOrNull()

    companion object {
        /** Notes above this size are not read (protects memory on huge or binary files named `.md`). */
        const val MAX_FILE_BYTES = 4 * 1024 * 1024
    }

    private data class Child(val id: String, val name: String, val isDirectory: Boolean, val lastModified: Long)

    private fun children(
        treeUri: Uri,
        parentId: String
    ): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection =
            arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED)
        val list = ArrayList<Child>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2)
                val modified = if (c.isNull(3)) 0L else c.getLong(3)
                list += Child(id, name, mime == Document.MIME_TYPE_DIR, modified)
            }
        }
        return list
    }
}

class FileTooLargeException(limit: Int) : IOException("File is larger than $limit bytes")

/** Reads at most [maxBytes]; throws [FileTooLargeException] instead of loading larger streams. */
fun readLimited(
    input: InputStream,
    maxBytes: Int
): ByteArray {
    val out = ByteArrayOutputStream()
    val chunk = ByteArray(READ_CHUNK)
    var total = 0
    while (true) {
        val n = input.read(chunk)
        if (n < 0) break
        total += n
        if (total > maxBytes) throw FileTooLargeException(maxBytes)
        out.write(chunk, 0, n)
    }
    return out.toByteArray()
}

private const val READ_CHUNK = 64 * 1024

enum class WriteOutcome {
    /** New content written and verified by reading it back. */
    WRITTEN,

    /** Writing failed or read back differently; the original content was restored and verified. */
    ROLLED_BACK,

    /** Writing and restoring failed. A backup of the original stays in the app cache. */
    DAMAGED
}

/** Write, read back, compare, and restore on mismatch. Pure logic, unit-tested without Android. */
class VerifiedWrite(
    private val write: (ByteArray) -> Unit,
    private val read: () -> ByteArray
) {
    fun replace(
        original: ByteArray,
        updated: ByteArray
    ): WriteOutcome {
        if (attempt(updated)) return WriteOutcome.WRITTEN
        return if (attempt(original)) WriteOutcome.ROLLED_BACK else WriteOutcome.DAMAGED
    }

    private fun attempt(bytes: ByteArray): Boolean =
        runCatching {
            write(bytes)
            read().contentEquals(bytes)
        }.getOrDefault(false)
}
