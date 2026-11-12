package io.github.descomanian.vaultbell.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/** A Markdown file inside the vault. */
data class VaultFile(
    val documentUri: Uri,
    /** Path relative to the vault root with forward slashes, e.g. `Daily/2026-11-14.md`. */
    val relativePath: String,
    val lastModified: Long,
)

data class VaultInfo(
    val displayName: String,
    val hasObsidianFolder: Boolean,
)

/**
 * Include/exclude folders, relative to the vault root (e.g. `Daily`, `Archiv/2024`).
 * Empty [include] means the whole vault.
 */
data class ScanFilter(
    val include: List<String> = emptyList(),
    val exclude: List<String> = emptyList(),
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

    fun persistPermission(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(treeUri, flags)
    }

    fun releasePermission(treeUri: Uri) {
        runCatching {
            resolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
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
        filter: ScanFilter = ScanFilter(),
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
                            lastModified = child.lastModified,
                        )
                }
            }
        }
        return result
    }

    fun readText(documentUri: Uri): String =
        resolver.openInputStream(documentUri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Cannot open $documentUri")

    /** Overwrites the file (truncate). Only called for explicit user actions with a verified line hash. */
    fun writeText(
        documentUri: Uri,
        text: String,
    ) {
        val out = resolver.openOutputStream(documentUri, "wt") ?: error("Cannot write $documentUri")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private data class Child(val id: String, val name: String, val isDirectory: Boolean, val lastModified: Long)

    private fun children(
        treeUri: Uri,
        parentId: String,
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
