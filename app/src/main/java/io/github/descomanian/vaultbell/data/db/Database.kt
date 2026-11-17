package io.github.descomanian.vaultbell.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.descomanian.vaultbell.parser.Recurrence
import io.github.descomanian.vaultbell.parser.ReminderSource
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders", indices = [Index("fileUri"), Index("dueAt")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileUri: String,
    val relativePath: String,
    val lineNumber: Int,
    val title: String,
    val rawLine: String,
    val lineHash: String,
    /** Epoch millis of the parsed date/time in the device time zone at scan time. */
    val dueAt: Long,
    val hasExplicitTime: Boolean,
    val format: String,
    val snoozedUntil: Long? = null,
    /** Done inside the app. Stays hidden until the line changes (new hash) or disappears. Series never set this. */
    val completedLocally: Boolean = false,
    val firedAt: Long? = null,
    // ---- since schema 2 (v1.1); all nullable so the migration needs no defaults
    /** [io.github.descomanian.vaultbell.parser.ReminderSource] name; null (rows from v1) means a task line. */
    val source: String? = null,
    /** [io.github.descomanian.vaultbell.parser.Recurrence] name, null for one-off reminders. */
    val recurrence: String? = null,
    /** Date written in the note (series anchor). For series [dueAt] is the current instance. Null for v1 rows: same as [dueAt]. */
    val anchorAt: Long? = null,
    /** Due time of the series instance completed last (for Undo and so it is never revived). */
    val doneInstanceAt: Long? = null,
    /** Reminder plan: time of the last plan alarm that fired for the current instance (chain scheduling). */
    val planFiredAt: Long? = null
)

/** When the main alarm should fire: snooze time if snoozed, otherwise the due time. */
val ReminderEntity.triggerAt: Long get() = snoozedUntil ?: dueAt

val ReminderEntity.isSeries: Boolean get() = recurrence != null

val ReminderEntity.isProperty: Boolean get() = source == ReminderSource.PROPERTY.name

/** Date written in the note; rows from v1 have no anchor and use [ReminderEntity.dueAt]. */
val ReminderEntity.anchorOrDue: Long get() = anchorAt ?: dueAt

fun ReminderEntity.rule(): Recurrence? = recurrence?.let { name -> Recurrence.entries.firstOrNull { it.name == name } }

@Entity(tableName = "files")
data class FileStateEntity(
    @PrimaryKey val uri: String,
    val relativePath: String,
    val lastModified: Long
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE completedLocally = 0 ORDER BY COALESCE(snoozedUntil, dueAt), title")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observe(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE completedLocally = 0")
    suspend fun allActive(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE fileUri = :uri")
    suspend fun forFile(uri: String): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders WHERE completedLocally = 0")
    fun observeActiveCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM reminders WHERE fileUri IN (:uris)")
    suspend fun deleteForFiles(uris: List<String>)

    @Query("SELECT * FROM files")
    suspend fun files(): List<FileStateEntity>

    @Upsert
    suspend fun upsertFile(file: FileStateEntity)

    @Query("DELETE FROM files WHERE uri IN (:uris)")
    suspend fun deleteFiles(uris: List<String>)

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM files")
    suspend fun clearFiles()
}

@Database(entities = [ReminderEntity::class, FileStateEntity::class], version = 2, exportSchema = true)
abstract class VaultbellDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "vaultbell.db"

        /** v1.1: source, repeat rule, series state and reminder plan. Keeps done/snooze state of existing rows. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE reminders ADD COLUMN source TEXT")
                    db.execSQL("ALTER TABLE reminders ADD COLUMN recurrence TEXT")
                    db.execSQL("ALTER TABLE reminders ADD COLUMN anchorAt INTEGER")
                    db.execSQL("ALTER TABLE reminders ADD COLUMN doneInstanceAt INTEGER")
                    db.execSQL("ALTER TABLE reminders ADD COLUMN planFiredAt INTEGER")
                }
            }

        fun create(context: Context): VaultbellDatabase =
            Room.databaseBuilder(context.applicationContext, VaultbellDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                // The index is a cache of the vault: rebuilding it is always safe if no migration path exists.
                .fallbackToDestructiveMigration()
                .build()
    }
}
