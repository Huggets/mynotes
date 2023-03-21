package com.huggets.mynotes.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel : ViewModel() {

    private val noteRepository = NoteRepository()

    private val _uiState = MutableStateFlow(NoteAppUiState())

    val uiState = _uiState.asStateFlow()

    fun fetchNotes() {
        _uiState.value = _uiState.value.copy(isFetchingItem = true)

        viewModelScope.launch {
            val noteList = mutableListOf<NoteItemUiState>()

            for (note in noteRepository.notes) {
                noteList.add(NoteItemUiState(note.id, note.title, note.content))
            }

            _uiState.value = _uiState.value.copy(isFetchingItem = false, items = noteList)
        }
    }

    fun saveNote(note: NoteItemUiState) {
        viewModelScope.launch {
            noteRepository.saveNote(note.toNote())
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }
}