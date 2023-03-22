package com.huggets.mynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.huggets.mynotes.ui.NoteApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quitApplication: () -> Unit = { finish() }

        setContent {
            NoteApp(quitApplication)
        }
    }
}