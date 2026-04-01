package com.kitabu.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Tag::class, NoteTag::class, NoteVersion::class, Template::class],
    version = 5,
    exportSchema = false
)
abstract class KitabuDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteVersionDao(): NoteVersionDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile private var INSTANCE: KitabuDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN isDaily INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN dailyDate TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN templateId INTEGER")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL DEFAULT -864536121,
                        createdAt INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS note_tags (
                        noteId INTEGER NOT NULL,
                        tagId  INTEGER NOT NULL,
                        PRIMARY KEY (noteId, tagId),
                        FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE,
                        FOREIGN KEY (tagId)  REFERENCES tags(id)  ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_noteId ON note_tags(noteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId  ON note_tags(tagId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS note_versions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noteId  INTEGER NOT NULL,
                        title   TEXT NOT NULL,
                        content TEXT NOT NULL,
                        savedAt INTEGER NOT NULL,
                        FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_versions_noteId ON note_versions(noteId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name      TEXT NOT NULL,
                        content   TEXT NOT NULL,
                        icon      TEXT NOT NULL DEFAULT '📄',
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v2.1: Add archive support
                db.execSQL("ALTER TABLE notes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                // v2.1: Add scheduled reminders
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderTime INTEGER")
                // Index for archived notes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_archived ON notes(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_reminder ON notes(reminderTime)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v3.0: Trash system
                db.execSQL("ALTER TABLE notes ADD COLUMN isTrashed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN trashedAt INTEGER")
                // v3.0: Favorites system
                db.execSQL("ALTER TABLE notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // Indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_trashed ON notes(isTrashed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_favorite ON notes(isFavorite)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create FTS4 virtual table for full-text search
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts4(title, content, content='notes', content_rowid='id')")

                // Populate FTS table with existing notes
                db.execSQL("INSERT INTO notes_fts(rowid, title, content) SELECT id, title, content FROM notes")

                // Trigger to keep FTS in sync on INSERT
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_insert AFTER INSERT ON notes BEGIN
                        INSERT INTO notes_fts(rowid, title, content) VALUES (NEW.id, NEW.title, NEW.content);
                    END
                """)

                // Trigger to keep FTS in sync on DELETE
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_delete AFTER DELETE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, content) VALUES('delete', OLD.id, OLD.title, OLD.content);
                    END
                """)

                // Trigger to keep FTS in sync on UPDATE
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS notes_fts_update AFTER UPDATE ON notes BEGIN
                        INSERT INTO notes_fts(notes_fts, rowid, title, content) VALUES('delete', OLD.id, OLD.title, OLD.content);
                        INSERT INTO notes_fts(rowid, title, content) VALUES (NEW.id, NEW.title, NEW.content);
                    END
                """)
            }
        }

        fun getDatabase(context: Context): KitabuDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, KitabuDatabase::class.java, "kitabu_db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
