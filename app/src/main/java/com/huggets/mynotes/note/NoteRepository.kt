package com.huggets.mynotes.note

class NoteRepository {

    private var _notes = mutableListOf(
        Note(1, "Projet hexaface", "Ça doit être un monde 3D"),
        Note(
            2,
            "TODO",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        ),
        Note(3, "Minecraft", "Faire une partie après-demain"),
    )

    val notes: List<Note>
        get() = _notes

    fun saveNote(note: Note) {
        val foundNote = _notes.find(note.id)

        if (foundNote == null) {
            note.id = noteIdCounter++
            _notes.add(note)
        } else {
            foundNote.title = note.title
            foundNote.content = note.content
        }
    }

    fun deleteNote(noteId: Int): Boolean {
        val foundNote = _notes.find(noteId)

        return _notes.remove(foundNote)
    }

    companion object {
        private var noteIdCounter = 4
    }
}