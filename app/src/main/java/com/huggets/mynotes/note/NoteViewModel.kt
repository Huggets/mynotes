package com.huggets.mynotes.note

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huggets.mynotes.ui.NoteAppUiState
import com.huggets.mynotes.ui.NoteAssociationItemUiState
import com.huggets.mynotes.ui.NoteItemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(context: Context) : ViewModel() {

    private val noteRepository = NoteRepository(context)
    private val noteAssociationRepository = NoteAssociationRepository(context)

    private val _uiState = MutableStateFlow(NoteAppUiState())
    val uiState = _uiState.asStateFlow()

    fun fetchUiState() {
        viewModelScope.launch {
            noteRepository.fetchAllNotes().collect {
                val list = mutableListOf<NoteItemUiState>()

                it.forEach { note ->
                    list.add(NoteItemUiState(note))
                }

                _uiState.value = _uiState.value.copy(allNotes = list)
            }
        }

        viewModelScope.launch {
            noteRepository.fetchMainNotes().collect {
                val list = mutableListOf<Long>()

                it.forEach { id ->
                    list.add(id)
                }

                _uiState.value = _uiState.value.copy(mainNoteIds = list)
            }
        }

        viewModelScope.launch {
            noteAssociationRepository.fetchAll().collect {
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
                noteId = noteRepository.save(note.toNote())
            } else {
                noteId = note.id
                noteRepository.update(note.toNote())
            }

            if (parentNoteId != 0L) {
                noteAssociationRepository.save(
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
}