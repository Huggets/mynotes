package com.huggets.mynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.ui.NoteApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quitApplication: () -> Unit = { finish() }

        setContent {
            NoteApp(
                quitApplication,
                viewModel { NoteViewModel(applicationContext) }
            )
        }
    }
}