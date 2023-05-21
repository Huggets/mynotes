package com.huggets.mynotes.data

import android.content.Context
import android.util.Xml
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.huggets.mynotes.ui.state.NoteAppUiState
import com.huggets.mynotes.ui.state.NoteAssociationItemUiState
import com.huggets.mynotes.ui.state.NoteItemUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.InputStream
import java.io.OutputStream

class NoteViewModel(context: Context) : ViewModel() {

    private val noteRepository = NoteRepository(context)
    private val noteAssociationRepository = NoteAssociationRepository(context)
    private val deletedNoteRepository = DeletedNoteRepository(context)

    private val _uiState = MutableStateFlow(NoteAppUiState())
    val uiState = _uiState.asStateFlow()

    fun syncUiState() {
        viewModelScope.launch {
            noteRepository.syncAllNotes().collect {
                val list = mutableListOf<NoteItemUiState>()

                it.forEach { note ->
                    list.add(NoteItemUiState(note))
                }

                _uiState.value = _uiState.value.copy(allNotes = list)
            }
        }

        viewModelScope.launch {
            noteRepository.syncMainNotes().collect {
                val list = mutableListOf<Date>()

                it.forEach { creationDate ->
                    list.add(creationDate)
                }

                _uiState.value = _uiState.value.copy(mainNoteCreationDates = list)
            }
        }

        viewModelScope.launch {
            noteAssociationRepository.syncAllAssociations().collect {
                val list = mutableListOf<NoteAssociationItemUiState>()

                it.forEach { noteAssociation ->
                    list.add(NoteAssociationItemUiState(noteAssociation))
                }

                _uiState.value = _uiState.value.copy(noteAssociations = list)
            }
        }
    }

    fun updateNote(note: NoteItemUiState) {
        viewModelScope.launch {
            noteRepository.update(note.toNote())
        }
    }

    /**
     * Start the creation of a new note in the database.
     *
     * If a parent note's creation date is not null, the note will be associated with this parent note.
     *
     * @param parentCreationDate The creation date of the parent note, if any.
     * @param onCreationDone Callback called when the creation is done.
     */
    fun createNote(parentCreationDate: Date?, onCreationDone: (newNoteCreationDate: Date) -> Unit) {
        viewModelScope.launch {
            val currentDate = Date.getCurrentTime()
            val newNote = Note("", "", currentDate, currentDate)

            noteRepository.insert(newNote)
            associateNote(parentCreationDate, currentDate)

            onCreationDone(currentDate)
        }
    }

    private suspend fun associateNote(parentCreationDate: Date?, creationDate: Date) {
        if (parentCreationDate != null) {
            val noteAssociation = NoteAssociationItemUiState(parentCreationDate, creationDate)
                .toNoteAssociation()

            noteAssociationRepository.insert(noteAssociation)
        }
    }

    /**
     * Deletes a note and all its children
     */
    fun deleteNote(creationDate: Date) {
        viewModelScope.launch {
            noteRepository.getChildren(creationDate).forEach { child ->
                noteRepository.delete(child.creationDate)
                deletedNoteRepository.insert(DeletedNote(child.creationDate))
                noteAssociationRepository.deleteByChildCreationDate(child.creationDate)
            }

            noteRepository.delete(creationDate)
            deletedNoteRepository.insert(DeletedNote(creationDate))
        }
    }

    fun exportToXml(stream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val serializer = Xml.newSerializer()
            serializer.setOutput(stream, "UTF-8")
            serializer.startDocument("UTF-8", true)

            serializer.startTag("", "data")
            serializer.attribute("", "version", "2")
            notesToXml(serializer)
            noteAssociationsToXml(serializer)
            deletedNotesToXml(serializer)
            serializer.endTag("", "data")

            serializer.endDocument()
            stream.close()
        }
    }

    private suspend fun notesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "notes")

        noteRepository.getAllNotes().forEach { note ->
            serializer.startTag("", "note")
            serializer.attribute("", "title", note.title)
            serializer.attribute("", "content", note.content)
            serializer.attribute("", "creationDate", note.creationDate.toString())
            serializer.attribute("", "lastEditTime", note.lastEditTime.toString())
            serializer.endTag("", "note")
        }

        serializer.endTag("", "notes")
    }

    private suspend fun noteAssociationsToXml(serializer: XmlSerializer) {
        serializer.startTag("", "noteAssociations")

        noteAssociationRepository.getAllAssociations().forEach { noteAssociation ->
            serializer.startTag("", "noteAssociation")
            serializer.attribute(
                "",
                "parentCreationDate",
                noteAssociation.parentCreationDate.toString()
            )
            serializer.attribute(
                "",
                "childCreationDate",
                noteAssociation.childCreationDate.toString()
            )
            serializer.endTag("", "noteAssociation")
        }

        serializer.endTag("", "noteAssociations")
    }

    private suspend fun deletedNotesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "deletedNotes")

        deletedNoteRepository.getAllDeletedNotes().forEach { deletedNote ->
            serializer.startTag("", "deletedNote")
            serializer.attribute(
                "",
                "creationDate",
                deletedNote.creationDate.toString()
            )
            serializer.endTag("", "deletedNote")
        }

        serializer.endTag("", "deletedNotes")
    }

    fun importFromXml(stream: InputStream) {
        _uiState.value =
            _uiState.value.copy(isImporting = true, importFailed = false, importFailedMessage = "")

        viewModelScope.launch(Dispatchers.IO) {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")

            val importedNotes = mutableListOf<Note>()
            val importedNoteAssociations = mutableListOf<NoteAssociation>()
            val importedDeletedNotes = mutableListOf<DeletedNote>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "data" -> {
                            val version: Int? = parser.getAttributeValue("", "version")?.toInt()
                            if (version != 2) {
                                stream.close()
                                _uiState.value = _uiState.value.copy(
                                    isImporting = false,
                                    importFailed = true,
                                    importFailedMessage = "Unsupported version"
                                )
                                return@launch
                            }
                        }

                        "note" -> {
                            val title = parser.getAttributeValue("", "title")
                            val content = parser.getAttributeValue("", "content")
                            val creationDate = Date.fromString(
                                parser.getAttributeValue("", "creationDate")
                            )
                            val lastEditTime = Date.fromString(
                                parser.getAttributeValue("", "lastEditTime")
                            )

                            importedNotes.add(Note(title, content, creationDate, lastEditTime))
                        }

                        "noteAssociation" -> {
                            val parentCreationDate = Date.fromString(
                                parser.getAttributeValue("", "parentCreationDate")
                            )
                            val childCreationDate = Date.fromString(
                                parser.getAttributeValue("", "childCreationDate")
                            )

                            importedNoteAssociations.add(
                                NoteAssociation(parentCreationDate, childCreationDate)
                            )
                        }

                        "deletedNote" -> {
                            val creationDate = Date.fromString(
                                parser.getAttributeValue("", "creationDate")
                            )

                            importedDeletedNotes.add(DeletedNote(creationDate))
                        }
                    }
                }
                eventType = parser.next()
            }

            stream.close()

            importedNotes.forEach {
                if (noteRepository.get(it.creationDate) == null) {
                    noteRepository.insert(it)
                }
            }
            noteAssociationRepository.insert(*importedNoteAssociations.toTypedArray())
            deletedNoteRepository.insert(*importedDeletedNotes.toTypedArray())

            importedDeletedNotes.forEach { deletedNote ->
                noteRepository.delete(deletedNote.creationDate)
            }

            noteRepository.getAllNotes().forEach {
                deletedNoteRepository.delete(it.creationDate)
            }

            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])

                return NoteViewModel(application) as? T
                    ?: throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}