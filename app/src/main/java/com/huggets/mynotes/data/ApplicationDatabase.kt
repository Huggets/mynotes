package com.huggets.mynotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.huggets.mynotes.data.*

/**
 * Database for the application that holds the notes.
 *
 * The database contains 3 tables:
 * - note: contains the notes,
 * - note_association: contains the associations between notes,
 * - deleted_note: contains the notes that were deleted.
 */
@Database(
    version = 3,
    entities = [Note::class, NoteAssociation::class, DeletedNote::class],
    exportSchema = true,
)
@TypeConverters(Converter::class)
abstract class ApplicationDatabase : RoomDatabase() {

    /**
     * Returns the DAO for the notes.
     */
    abstract fun noteDao(): NoteDao

    /**
     * Returns the DAO for the note associations.
     */
    abstract fun noteAssociationDao(): NoteAssociationDao

    /**
     * Returns the DAO for the deleted notes.
     */
    abstract fun deletedNoteDao(): DeletedNoteDao

    companion object {
        /**
         * The database instance.
         */
        private var db: ApplicationDatabase? = null

        /**
         * Returns the database instance.
         */
        fun getDb(context: Context): ApplicationDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                    context,
                    ApplicationDatabase::class.java,
                    "database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
            }

            return db as ApplicationDatabase
        }
    }
}