package com.huggets.mynotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.huggets.mynotes.data.*

@Database(
    version = 3,
    entities = [Note::class, NoteAssociation::class, DeletedNote::class],
    exportSchema = true,
)
@TypeConverters(Converter::class)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteAssociationDao(): NoteAssociationDao
    abstract fun deletedNoteDao(): DeletedNoteDao

    companion object {
        private var db: ApplicationDatabase? = null

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