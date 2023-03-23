package com.huggets.mynotes.note

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(context: Context) : ViewModel() {

    private val repository = NoteRepository(context)

    private val _uiState = MutableStateFlow(NoteAppUiState())
    val uiState = _uiState.asStateFlow()

    fun fetchNotes() {
        _uiState.value = _uiState.value.copy(isFetchingItem = true)

        viewModelScope.launch {
            repository.fetchNotes().collect {
                val list = mutableListOf<NoteItemUiState>()

                it.forEach { note ->
                    list.add(NoteItemUiState(note.id, note.title, note.content))
                }

                _uiState.value = NoteAppUiState(isFetchingItem = false, items = list)
            }
        }
    }

    fun saveNote(note: NoteItemUiState) {
        viewModelScope.launch {
            repository.saveNote(note.toNote())
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }
}