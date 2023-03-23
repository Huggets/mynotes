package com.huggets.mynotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huggets.mynotes.note.Note
import com.huggets.mynotes.note.NoteDao

@Database(
    version = 1,
    entities = [Note::class],
    exportSchema = false,
)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private var db: ApplicationDatabase? = null

        fun getDb(context: Context): ApplicationDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                    context,
                    ApplicationDatabase::class.java,
                    "database"
                ).build()
            }

            return db as ApplicationDatabase
        }
    }
}