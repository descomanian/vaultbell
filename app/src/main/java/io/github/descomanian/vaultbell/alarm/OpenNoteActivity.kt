package io.github.descomanian.vaultbell.alarm

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import io.github.descomanian.vaultbell.VaultbellApplication
import io.github.descomanian.vaultbell.ui.MainActivity

/**
 * Invisible activity behind a notification tap.
 * Tries Obsidian first, then any Markdown viewer, then falls back to the reminder in Vaultbell.
 */
class OpenNoteActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(ReminderAlarms.EXTRA_ID, -1L)
        if (id >= 0) (application as VaultbellApplication).container.repository.onNotificationHandled(id)

        val obsidian = intent.getStringExtra(EXTRA_OBSIDIAN_URI)?.let(Uri::parse)
        val document = intent.getStringExtra(EXTRA_DOCUMENT_URI)?.let(Uri::parse)
        val opened =
            (obsidian != null && tryStart(Intent(Intent.ACTION_VIEW, obsidian))) ||
                (
                    document != null &&
                        tryStart(
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(document, "text/markdown")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    )
        if (!opened) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_REMINDER, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        finish()
    }

    private fun tryStart(intent: Intent): Boolean =
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }

    companion object {
        const val EXTRA_OBSIDIAN_URI = "obsidian_uri"
        const val EXTRA_DOCUMENT_URI = "document_uri"
    }
}
