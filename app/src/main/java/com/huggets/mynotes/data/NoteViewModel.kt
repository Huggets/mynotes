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
    private val preferenceRepository = PreferenceRepository(context)

    private var noteIdGenerator: Int

    private val _uiState = MutableStateFlow(NoteAppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Default value is 0 but we need to wait for the value to be retrieved from the database

        noteIdGenerator = 0

        viewModelScope.launch {
            preferenceRepository.getPreference(PREFERENCES_NOTE_ID_GENERATOR).apply {
                if (this != null) {
                    noteIdGenerator = value.toInt()
                } else {
                    val preference =
                        Preference(PREFERENCES_NOTE_ID_GENERATOR, noteIdGenerator.toString())
                    preferenceRepository.setPreference(preference)
                }
            }
            _uiState.value = _uiState.value.copy(isInitializationFinished = true)
        }
    }

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
                val list = mutableListOf<Int>()

                it.forEach { id ->
                    list.add(id)
                }

                _uiState.value = _uiState.value.copy(mainNoteIds = list)
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
     * Creates a new note in the database.
     *
     * If a parent note id is not null, the note will be associated with this parent note.
     *
     * @return the id of the new note
     */
    fun createNote(parentId: Int?): Int {
        val newId = generateNewNoteId()

        viewModelScope.launch {
            val currentDate = Date.getCurrentTime()
            val newNote = Note(newId, "", "", currentDate, currentDate)

            noteRepository.insert(newNote)
            associateNote(newId, parentId)
        }

        return newId
    }

    private suspend fun associateNote(id: Int, parentId: Int?) {
        if (parentId != null) {
            noteAssociationRepository.insert(
                NoteAssociationItemUiState(
                    parentId,
                    id,
                ).toNoteAssociation()
            )
        }
    }

    /**
     * Deletes a note and all its children
     */
    fun deleteNote(id: Int) {
        viewModelScope.launch {
            noteRepository.getChildren(id).forEach { child ->
                noteRepository.delete(child.id)
            }
            noteRepository.delete(id)
        }
    }

    fun exportToXml(stream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val serializer = Xml.newSerializer()
            serializer.setOutput(stream, "UTF-8")
            serializer.startDocument("UTF-8", true)

            serializer.startTag("", "data")
            notesToXml(serializer)
            noteAssociationsToXml(serializer)
            serializer.endTag("", "data")

            serializer.endDocument()
            stream.close()
        }
    }

    private suspend fun notesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "notes")

        noteRepository.getAllNotes().forEach { note ->
            serializer.startTag("", "note")
            serializer.attribute("", "id", note.id.toString())
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
                "parentId",
                noteAssociation.parentId.toString()
            )
            serializer.attribute(
                "",
                "childId",
                noteAssociation.childId.toString()
            )
            serializer.endTag("", "noteAssociation")
        }

        serializer.endTag("", "noteAssociations")
    }

    fun importFromXml(stream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "note" -> {
                            val id = parser.getAttributeValue("", "id").toInt()
                            val title = parser.getAttributeValue("", "title")
                            val content = parser.getAttributeValue("", "content")
                            val creationDate = Date.fromString(
                                parser.getAttributeValue("", "creationDate")
                            )
                            val lastEditTime = Date.fromString(
                                parser.getAttributeValue("", "lastEditTime")
                            )

                            val note =
                                NoteItemUiState(id, title, content, creationDate, lastEditTime)
                            noteRepository.insert(note.toNote())
                        }

                        "noteAssociation" -> {
                            val parentId = parser.getAttributeValue("", "parentId").toInt()
                            val childId = parser.getAttributeValue("", "childId").toInt()

                            val noteAssociation =
                                NoteAssociationItemUiState(parentId, childId).toNoteAssociation()
                            noteAssociationRepository.insert(noteAssociation)
                        }
                    }
                }
                eventType = parser.next()
            }

            stream.close()
        }
    }

    private fun generateNewNoteId(): Int {
        val newId = ++noteIdGenerator

        viewModelScope.launch {
            val preference = Preference(
                PREFERENCES_NOTE_ID_GENERATOR,
                newId.toString()
            )

            preferenceRepository.setPreference(preference)
        }

        return newId
    }

    companion object {
        private const val PREFERENCES_NOTE_ID_GENERATOR = "note_id_generator"

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