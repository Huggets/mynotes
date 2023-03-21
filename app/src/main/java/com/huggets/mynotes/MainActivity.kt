package com.huggets.mynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import com.huggets.mynotes.ui.NoteApp

class MainActivity : ComponentActivity() {

    private var onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = backPressedCallback()
        }

    private var backPressedCallback: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        val changeOnBackPressCallback: (() -> Unit) -> Unit = {
            backPressedCallback = it
        }
        val quitApplication: () -> Unit = { finish() }

        setContent {
            NoteApp(changeOnBackPressCallback, quitApplication)
        }
    }
}