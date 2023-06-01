package com.huggets.mynotes.sync

import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.data.DeletedNoteRepository
import com.huggets.mynotes.data.NoteAssociationRepository
import com.huggets.mynotes.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Function that fetches data from the remote device.
 */
typealias FetchData = (buffer: ByteArray, offset: Int, length: Int) -> Int

/**
 * Function that sends data to the remote device.
 */
typealias SendData = (buffer: ByteArray, offset: Int, length: Int) -> Unit

/**
 * Synchronizes data between two devices.
 *
 * @property fetchData Function that fetches data from the remote device.
 * @property sendData Function that sends data to the remote device.
 * @property noteRepository The repository that contains the notes.
 * @property noteAssociationRepository The repository that contains the note associations.
 * @property deletedNoteRepository The repository that contains the deleted notes.
 */
class DataSynchronizer(
    private val fetchData: FetchData,
    private val sendData: SendData,
    private val noteRepository: NoteRepository,
    private val noteAssociationRepository: NoteAssociationRepository,
    private val deletedNoteRepository: DeletedNoteRepository,
) {
    /**
     * Indicates if the synchronizer is currently synchronizing.
     */
    private var isSynchronizing = false

    /**
     * Indicates if the sender has finished sending data.
     */
    private var senderCompleted = false

    /**
     * Indicates if the receiver has finished receiving data.
     */
    private var receiverCompleted = false

    /**
     * Starts the synchronization in a coroutine. It does nothing if the synchronizer is already
     * in progress.
     *
     * @param onSynchronizationFinished Called when the synchronization has finished.
     */
    fun sync(onSynchronizationFinished: (Exception?) -> Unit) {
        if (isSynchronizing) {
            return
        }

        isSynchronizing = true
        senderCompleted = false
        receiverCompleted = false

        CoroutineScope(Dispatchers.IO).launch {
            val notes = noteRepository.getAllNotes()
            val noteAssociations = noteAssociationRepository.getAllAssociations()
            val deletedNotes = deletedNoteRepository.getAllDeletedNotes()

            val sharedData =
                SharedData(notes, noteAssociations, deletedNotes, fetchData, sendData)

            val sender = DataSender(sharedData)
            val receiver = DataReceiver(sharedData)

            val onSenderFinished: (Exception?) -> Unit = {
                senderCompleted = true
                onSenderOrReceiverFinished(sharedData, it, onSynchronizationFinished)
            }
            val onReceiverFinished: (Exception?) -> Unit = {
                receiverCompleted = true
                onSenderOrReceiverFinished(sharedData, it, onSynchronizationFinished)
            }

            sender.start(this, onSenderFinished)
            receiver.start(this, onReceiverFinished)
        }
    }

    /**
     * Called when the sender or receiver has finished.ç
     *
     * When both the sender and receiver have finished, and no exception occurred, the data in the
     * database is updated.
     *
     * @param sharedData The shared data used by the sender and receiver. It contains the received
     * data.
     * @param exception The exception that occurred when synchronizing or null if no exception
     * occurred.
     * @param onSynchronizationFinished Called when both the sender and receiver have finished.
     */
    private fun onSenderOrReceiverFinished(
        sharedData: SharedData,
        exception: Exception?,
        onSynchronizationFinished: (Exception?) -> Unit
    ) {
        if (senderCompleted && receiverCompleted) {
            if (exception == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateData(sharedData)

                    isSynchronizing = false
                    onSynchronizationFinished(null)
                }
            } else {
                isSynchronizing = false
                onSynchronizationFinished(exception)
            }
        }
    }

    /**
     * Update the data in the database with the received data.
     *
     * @param sharedData The shared data containing the received data.
     */
    private suspend fun updateData(sharedData: SharedData) {
        deleteNotes(sharedData)
        insertNotes(sharedData)
    }

    /**
     * Deletes the notes that were deleted on the remote device. It also deletes the children
     * (even if they were created on this device after the parent was deleted on the remote device).
     *
     * Used by [updateData].
     *
     * @param sharedData The shared data containing the received data.
     */
    private suspend fun deleteNotes(sharedData: SharedData) {
        sharedData.deletedNotesReceiver.obtain().forEach { deletedNote ->
            noteRepository.getChildren(deletedNote.creationDate).forEach { child ->
                noteRepository.delete(child.creationDate)
                deletedNoteRepository.insert(DeletedNote(child.creationDate))
                noteAssociationRepository.deleteByChildCreationDate(child.creationDate)
            }

            noteRepository.delete(deletedNote.creationDate)
            deletedNoteRepository.insert(DeletedNote(deletedNote.creationDate))
        }
    }

    /**
     * Inserts the notes and associations that were created on the remote device.
     */
    private suspend fun insertNotes(sharedData: SharedData) {
        // Get the last deleted notes to avoid inserting notes that were deleted on this device
        val updatedDeletedNotes = deletedNoteRepository.getAllDeletedNotes()

        // Get the notes of the device to know if the fetched notes need to be inserted
        // or updated
        val existingNotes = noteRepository.getAllNotes()

        sharedData.requestedNoteReceiver.obtain().forEach { note ->
            // Try to find if the note was deleted
            val deletedNote = updatedDeletedNotes.find { deletedNote ->
                deletedNote.creationDate == note.creationDate
            }

            // If the note was not deleted, insert or update it
            if (deletedNote == null) {
                val exists = existingNotes.find { existingNote ->
                    existingNote.creationDate == note.creationDate
                } != null

                if (exists) {
                    noteRepository.update(note)
                } else {
                    noteRepository.insert(note)
                }
            }
        }
        sharedData.associationsReceiver.obtain().forEach {
            // Try to find if the child was deleted
            val deletedNote = updatedDeletedNotes.find { deletedNote ->
                deletedNote.creationDate == it.childCreationDate
            }

            // If the child was not deleted, insert the association
            if (deletedNote == null) {
                noteAssociationRepository.insert(it)
            }
        }
    }
}