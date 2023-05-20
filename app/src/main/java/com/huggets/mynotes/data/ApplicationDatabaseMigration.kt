package com.huggets.mynotes.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new note table and add id column
        database.execSQL(
            """
            CREATE TABLE note_new (
                id INTEGER NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                creation_date TEXT NOT NULL,
                last_edit_time TEXT NOT NULL,
                PRIMARY KEY(`id`)
            );
            """.trimMargin()
        )
        database.execSQL(
            """
            INSERT INTO note_new (id, title, content, creation_date, last_edit_time)
            SELECT (@row_number = @row_number + 1) AS id, note.title, note.content, note.creation_date, note.last_edit_time
            FROM note, (SELECT @row_number = 0) AS r
            ORDER BY note.creation_date;
            """.trimIndent()
        )

        // Save old associations
        database.execSQL(
            """
            CREATE TABLE note_association_old (
                parent_creation_date TEXT NOT NULL,
                child_creation_date TEXT NOT NULL,
                PRIMARY KEY(parent_creation_date, child_creation_date)
            );
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO note_association_old (parent_creation_date, child_creation_date)
            SELECT note_association.parent_creation_date, note_association.child_creation_date
            FROM note_association;
            """.trimIndent()
        )

        // Drop old note and old note_association tables
        database.execSQL("DROP TABLE note")
        database.execSQL("DROP TABLE note_association")

        // Rename new note table
        database.execSQL("ALTER TABLE note_new RENAME TO note")

        // Create new note_association table and change dates to ids
        database.execSQL(
            """
            CREATE TABLE note_association (
                parent_id INTEGER NOT NULL,
                child_id INTEGER NOT NULL,
                PRIMARY KEY(parent_id, child_id), 
                FOREIGN KEY(parent_id) REFERENCES note(id) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(child_id) REFERENCES note(id) ON UPDATE NO ACTION ON DELETE CASCADE
            );
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX index_note_association_child_id ON note_association (child_id);
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO note_association (parent_id, child_id)
            SELECT parent.id, child.id FROM (SELECT * FROM NOTE_ASSOCIATION_OLD) as old
            JOIN note as parent ON old.parent_creation_date = parent.creation_date
            JOIN note as child ON old.child_creation_date = child.creation_date;
            """.trimIndent()
        )

        // Drop old note_association_old table
        database.execSQL("DROP TABLE note_association_old")

        // Create new preference table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS preference (
                name TEXT NOT NULL, 
                value TEXT NOT NULL,
                PRIMARY KEY(name)
            );
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO preference (name, value)
            SELECT 'note_id_generator', max(id) + 1 FROM note;
            """.trimIndent()
        )

        // Create deleted_note table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_note (
                creation_date TEXT NOT NULL,
                PRIMARY KEY(creation_date)
            );
            """.trimIndent()
        )
    }
}