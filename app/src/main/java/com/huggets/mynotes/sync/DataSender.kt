package com.huggets.mynotes.sync

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.Note.Companion.find
import com.huggets.mynotes.data.NoteAssociation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import java.io.IOException

class DataSender(
    private val sharedData: SharedData,
) {
    fun start(coroutineScope: CoroutineScope, onFinish: (Exception?) -> Unit) {
        coroutineScope.launch {
            val steps = listOf(
                ::sendDates,
                ::requestNeededNoteDates,
                ::sendRequestedNotes,
                ::sendAssociations,
                ::sendEndOfData,
            )

            steps.forEach { sendData ->
                val exception = sendData.invoke()
                if (exception != null) {
                    onFinish(exception)
                    return@launch
                }
            }

            onFinish(null)
        }
    }

    private suspend fun send(sender: Sender): Exception? {
        while (sender.allDataNotSent) {
            sender.fill()
            try {
                sender.send()
                sender.waitUntilDataIsSent()
            } catch (e: IOException) {
                return e
            } catch (e: ClosedReceiveChannelException) {
                return e
            }
        }

        return null
    }

    private suspend fun sendDates(): Exception? {
        sharedData.datesSender.setData(sharedData.notes)

        return send(sharedData.datesSender)
    }

    private suspend fun getNeededNoteDates(): List<Date> {
        val neededNotes = mutableListOf<Date>()

        sharedData.datesReceiver.obtain().forEach {
            sharedData.notes.find(it.first).also { noteFound ->
                if (noteFound == null || noteFound.lastEditTime < it.second) {
                    neededNotes.add(it.first)
                }
            }
        }

        return neededNotes
    }

    private suspend fun requestNeededNoteDates(): Exception? {
        val neededNotes = getNeededNoteDates()

        sharedData.neededNotesSender.setData(neededNotes)

        return send(sharedData.neededNotesSender)
    }

    private suspend fun getRequestedNotes(): List<Note> {
        val requestedNotes = mutableListOf<Note>()

        sharedData.neededNotesReceiver.obtain().forEach {
            sharedData.notes.find(it)?.also { note ->
                requestedNotes.add(note)
            }
        }

        return requestedNotes
    }

    private suspend fun sendRequestedNotes(): Exception? {
        val requestedNotes = getRequestedNotes()

        sharedData.requestedNoteSender.setData(requestedNotes)

        return send(sharedData.requestedNoteSender)
    }

    private suspend fun getRequestedAssociations(): List<NoteAssociation> {
        val associations = mutableListOf<NoteAssociation>()

        sharedData.neededNotesReceiver.obtain().forEach { date ->
            sharedData.noteAssociations.find { association ->
                date == association.childCreationDate
            }?.also {
                associations.add(it)
            }
        }
        return associations
    }

    private suspend fun sendAssociations(): Exception? {
        val requestedNoteAssociations = getRequestedAssociations()

        sharedData.associationsSender.setData(requestedNoteAssociations)

        return send(sharedData.associationsSender)
    }

    private suspend fun sendEndOfData(): Exception? {
        return send(sharedData.dataEndSender)
    }
}