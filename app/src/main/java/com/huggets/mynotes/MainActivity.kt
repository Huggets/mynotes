package com.huggets.mynotes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.huggets.mynotes.data.NoteViewModel
import com.huggets.mynotes.ui.NoteApp
import java.io.FileNotFoundException
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val noteViewModel: NoteViewModel by viewModels { NoteViewModel.Factory }

        val createDocument =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) {
                if (it != null) {
                    try {
                        applicationContext.contentResolver.openOutputStream(it, "wt")
                            ?.let { stream ->
                                noteViewModel.exportToXml(stream)
                            }
                    } catch (e: FileNotFoundException) {
                        // TODO show a snack bar
                        Log.e("MainActivity", e.stackTraceToString())
                    }
                }
            }
        val readDocument =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) {
                if (it != null) {
                    try {
                        applicationContext.contentResolver.openInputStream(it)?.let { stream ->
                            noteViewModel.importFromXml(stream)
                        }
                    } catch (e: FileNotFoundException) {
                        // TODO show a snack bar
                        Log.e("MainActivity", e.stackTraceToString())
                    }
                }
            }

        val quitApplication: () -> Unit = { finish() }

        setContent {
            val exportToXml: () -> Unit = {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val second = calendar.get(Calendar.SECOND)

                createDocument.launch(
                    "notes_$year-$month-$day-$hour-$minute-$second.xml"
                )
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