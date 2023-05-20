package com.huggets.mynotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.huggets.mynotes.data.*

@Database(
    version = 1,
    entities = [Note::class, NoteAssociation::class, Preference::class],
    exportSchema = true,
)
@TypeConverters(Converter::class)
abstract class ApplicationDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteAssociationDao(): NoteAssociationDao
    abstract fun preferenceDao(): PreferenceDao

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