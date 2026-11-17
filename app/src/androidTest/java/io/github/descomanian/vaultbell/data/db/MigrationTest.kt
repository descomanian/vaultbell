package io.github.descomanian.vaultbell.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Schema 1 (v1.0) → 2 (v1.1). Run on a device or emulator: `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), VaultbellDatabase::class.java)

    @Test
    fun migrate1To2KeepsRowsAndState() {
        helper.createDatabase(DB, 1).apply {
            execSQL(
                "INSERT INTO reminders (id, fileUri, relativePath, lineNumber, title, rawLine, lineHash, dueAt, " +
                    "hasExplicitTime, format, snoozedUntil, completedLocally, firedAt) VALUES " +
                    "(1, 'content://vault/a', 'a.md', 3, 'Done one', '- [ ] Done one (@2026-11-20 18:30)', 'aaaa', 1000, 1, " +
                    "'REMINDER_PLUGIN', NULL, 1, NULL), " +
                    "(2, 'content://vault/a', 'a.md', 4, 'Snoozed', '- [ ] Snoozed (@2026-11-21)', 'bbbb', 2000, 0, " +
                    "'REMINDER_PLUGIN', 2500, 0, 1900)"
            )
            execSQL("INSERT INTO files (uri, relativePath, lastModified) VALUES ('content://vault/a', 'a.md', 42)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 2, true, VaultbellDatabase.MIGRATION_1_2)

        val columns = "id, completedLocally, snoozedUntil, firedAt, source, recurrence, anchorAt, doneInstanceAt, planFiredAt"
        db.query("SELECT $columns FROM reminders ORDER BY id").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(1, c.getInt(1))
            assertTrue(c.isNull(4) && c.isNull(5) && c.isNull(6) && c.isNull(7) && c.isNull(8))
            assertTrue(c.moveToNext())
            assertEquals(2500L, c.getLong(2))
            assertEquals(1900L, c.getLong(3))
            assertEquals(false, c.moveToNext())
        }
        db.query("SELECT lastModified FROM files").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(42L, c.getLong(0))
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
