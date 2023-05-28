package com.huggets.mynotes.sync

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.data.Note.Companion.find
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.buffer.SendingBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
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

    private suspend fun send(buffer: SendingBuffer<Any>, channel: Channel<Unit>): Exception? {
        while (buffer.allDataNotSent) {
            buffer.fill()
            try {
                buffer.send(sharedData.bluetoothConnectionManager)
                channel.receive()
            } catch (e: IOException) {
                return e
            } catch (e: ClosedReceiveChannelException) {
                return e
            }
        }

        return null
    }

    private suspend fun sendDates(): Exception? {
        sharedData.datesSendingBuffer.setData(sharedData.notes)
        return send(sharedData.datesSendingBuffer, sharedData.datesSendingChannel)
    }

    private suspend fun requestNeededNoteDates(): Exception? {
        // Fetch the notes that this device doesn't have and those that are newer than the ones
        // that this device has
        val neededNotes = mutableListOf<Date>()
        sharedData.datesReceivingBuffer.obtain().forEach {
            sharedData.notes.find(it.first).also { noteFound ->
                if (noteFound == null || noteFound.lastEditTime < it.second) {
                    neededNotes.add(it.first)
                }
            }
        }

        sharedData.neededNotesSendingBuffer.setData(neededNotes)
        return send(sharedData.neededNotesSendingBuffer, sharedData.neededNotesChannel)
    }

    private suspend fun sendRequestedNotes(): Exception? {
        val requestedNotes = mutableListOf<Note>()
        sharedData.neededNotesReceivingBuffer.obtain().forEach {
            sharedData.notes.find(it)?.also { note ->
                requestedNotes.add(note)
            }
        }

        sharedData.requestedNoteSendingBuffer.setData(requestedNotes)
        return send(sharedData.requestedNoteSendingBuffer, sharedData.requestedNoteChannel)
    }

    private suspend fun sendEndOfData(): Exception? {
        val buffer = ByteArray(1) { Header.DATA_END.value }

        try {
            sharedData.bluetoothConnectionManager.writeData(buffer)
            sharedData.dataEndChannel.receive()
        } catch (e: IOException) {
            return e
        } catch (e: ClosedReceiveChannelException) {
            return e
        }

        return null
    }
}