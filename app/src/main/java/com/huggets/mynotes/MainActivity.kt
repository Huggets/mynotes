package com.huggets.mynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.huggets.mynotes.note.NoteViewModel
import com.huggets.mynotes.ui.NoteApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val noteViewModel: NoteViewModel by viewModels { NoteViewModel.Factory }

        val createDocument =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) {
                if (it != null) {
                    applicationContext.contentResolver.openOutputStream(it)?.let { stream ->
                        noteViewModel.exportToXml(stream)
                    }
                }
            }
        val readDocument =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                if (it != null) {
                    applicationContext.contentResolver.openInputStream(it)?.let { stream ->
                        noteViewModel.importFromXml(stream)
                    }
                }
            }

        val quitApplication: () -> Unit = { finish() }

        setContent {
            val exportToXml: () -> Unit = {
                createDocument.launch("notes.xml")
            }
            val importFromXml: () -> Unit = {
                readDocument.launch(arrayOf("text/xml"))
            }

            NoteApp(
                quitApplication = quitApplication,
                exportToXml = exportToXml,
                importFromXml = importFromXml,
                noteViewModel = noteViewModel,
            )
        }
    }
}