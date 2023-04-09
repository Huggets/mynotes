package com.huggets.mynotes.note

import android.content.Context
import android.util.Xml
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.huggets.mynotes.ui.NoteAppUiState
import com.huggets.mynotes.ui.NoteAssociationItemUiState
import com.huggets.mynotes.ui.NoteItemUiState
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
                val list = mutableListOf<Long>()

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

    fun saveNote(note: NoteItemUiState, parentNoteId: Long) {
        viewModelScope.launch {
            val noteId: Long
            if (note.id == 0L) {
                noteId = noteRepository.insert(note.toNote())
            } else {
                noteId = note.id
                noteRepository.update(note.toNote())
            }

            if (parentNoteId != 0L) {
                noteAssociationRepository.insert(
                    NoteAssociationItemUiState(parentNoteId, noteId).toNoteAssociation()
                )
            }
        }
    }

    /**
     * Deletes a note and all its children
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.getChildren(noteId).forEach { child ->
                noteRepository.delete(child.id)
            }
            noteRepository.delete(noteId)
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
        }
    }

    private suspend fun notesToXml(serializer: XmlSerializer) {
        serializer.startTag("", "notes")

        noteRepository.getAllNotes().forEach { note ->
            serializer.startTag("", "note")
            serializer.attribute("", "id", note.id.toString())
            serializer.attribute("", "title", note.title)
            serializer.attribute("", "content", note.content)
            serializer.attribute("", "lastEditTime", note.lastEditTime)
            serializer.endTag("", "note")
        }

        serializer.endTag("", "notes")
    }

    private suspend fun noteAssociationsToXml(serializer: XmlSerializer) {
        serializer.startTag("", "noteAssociations")

        noteAssociationRepository.getAllAssociations().forEach { noteAssociation ->
            serializer.startTag("", "noteAssociation")
            serializer.attribute("", "parentId", noteAssociation.parentId.toString())
            serializer.attribute("", "childId", noteAssociation.childId.toString())
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
                            val id = parser.getAttributeValue("", "id").toLong()
                            val title = parser.getAttributeValue("", "title")
                            val content = parser.getAttributeValue("", "content")
                            val lastEditTime = parser.getAttributeValue("", "lastEditTime")

                            val note = NoteItemUiState(id, title, content, lastEditTime)
                            noteRepository.insert(note.toNote())
                        }
                        "noteAssociation" -> {
                            val parentId = parser.getAttributeValue("", "parentId").toLong()
                            val childId = parser.getAttributeValue("", "childId").toLong()

                            val noteAssociation =
                                NoteAssociationItemUiState(parentId, childId).toNoteAssociation()
                            noteAssociationRepository.insert(noteAssociation)
                        }
                    }
                }
                eventType = parser.next()
            }
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