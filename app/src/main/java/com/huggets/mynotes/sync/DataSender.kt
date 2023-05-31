package com.huggets.mynotes.sync

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.Note.Companion.find
import com.huggets.mynotes.data.NoteAssociation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Sends data to the other device.
 *
 * @property sharedData The data shared between the sender and the receiver.
 */
class DataSender(
    private val sharedData: SharedData,
) {
    /**
     * Starts sending the data.
     *
     * @param coroutineScope The coroutine scope to use.
     * @param onFinish The function to call when the data has been sent or an error occurred.
     */
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

    /**
     * Sends all the data of the given sender.
     *
     * It fills the sender with data, sends it and waits until the data has been received. It
     * repeats this until all the data has been sent. If an error occurs, it returns the error.
     *
     * @param sender The sender that needs to send its data.
     *
     * @return The error that occurred or null if no error occurred.
     */
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

    /**
     * Sends the dates of the notes.
     *
     * For all notes, it sends the creation date and the last modification date.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun sendDates(): Exception? {
        sharedData.datesSender.setData(sharedData.notes)

        return send(sharedData.datesSender)
    }

    /**
     * Finds the notes this device needs to fetch according to the dates it has received.
     *
     * @return The dates of the notes this device needs to fetch.
     */
    private suspend fun getNeededNoteDates(): List<Date> {
        val neededNotes = mutableListOf<Date>()

        // Add the notes that are not on this device or are outdated.

        sharedData.datesReceiver.obtain().forEach {
            sharedData.notes.find(it.first).also { noteFound ->
                if (noteFound == null || noteFound.lastEditTime < it.second) {
                    neededNotes.add(it.first)
                }
            }
        }

        return neededNotes
    }

    /**
     * Sends the dates of the notes this device needs to fetch.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun requestNeededNoteDates(): Exception? {
        val neededNotes = getNeededNoteDates()

        sharedData.neededNotesSender.setData(neededNotes)

        return send(sharedData.neededNotesSender)
    }

    /**
     * Finds the notes the other device needs according the needed notes it has received.
     *
     * @return The notes the other device needs.
     */
    private suspend fun getRequestedNotes(): List<Note> {
        val requestedNotes = mutableListOf<Note>()

        // Add the notes that the other device needs.

        sharedData.neededNotesReceiver.obtain().forEach {
            sharedData.notes.find(it)?.also { note ->
                requestedNotes.add(note)
            }
        }

        return requestedNotes
    }

    /**
     * Sends the notes the other device needs.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun sendRequestedNotes(): Exception? {
        val requestedNotes = getRequestedNotes()

        sharedData.requestedNoteSender.setData(requestedNotes)

        return send(sharedData.requestedNoteSender)
    }

    /**
     * Finds the associations the other device needs according the needed notes it has received.
     *
     * @return The associations the other device needs.
     */
    private suspend fun getRequestedAssociations(): List<NoteAssociation> {
        val associations = mutableListOf<NoteAssociation>()

        // For each needed note, find an association with that note, if any.

        sharedData.neededNotesReceiver.obtain().forEach { date ->
            sharedData.noteAssociations.find { association ->
                date == association.childCreationDate
            }?.also {
                associations.add(it)
            }
        }
        return associations
    }

    /**
     * Sends the associations the other device needs.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun sendAssociations(): Exception? {
        val requestedNoteAssociations = getRequestedAssociations()

        sharedData.associationsSender.setData(requestedNoteAssociations)

        return send(sharedData.associationsSender)
    }

    /**
     * Sends the end of the data.
     *
     * This is used to indicate that all the data has been sent. It is used to prevent the
     * receiver from waiting for more data from the sender.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun sendEndOfData(): Exception? {
        return send(sharedData.dataEndSender)
    }
}