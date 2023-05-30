package com.huggets.mynotes.sync

import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

class DataReceiver(
    private val sharedData: SharedData,
) {
    fun start(coroutineScope: CoroutineScope, onFinish: (Exception?) -> Unit) {
        coroutineScope.launch {
            val exception = receiveData()

            if (exception != null) {
                sharedData.stop()
            }

            onFinish(exception)
        }
    }

    private suspend fun receiveData(): Exception? {
        try {
            sharedData.receivingBuffer.fetchDataFromStart()
        } catch (e: IOException) {
            return e
        }

        while (sharedData.receivingBuffer.bytesFetched != -1) {
            when (sharedData.receivingBuffer.lookByte()) {
                Header.DATES.value, Header.DATES_COUNT.value -> {
                    try {
                        sharedData.datesReceivingBuffer.read()
                    } catch (e: IOException) {
                        return e
                    }
                }

                Header.DATES_BUFFER_RECEIVED.value -> {
                    sharedData.receivingBuffer.skip(1)

                    sharedData.datesSendingChannel.send(Unit)
                }

                Header.NEEDED_NOTE.value, Header.NEEDED_NOTES_COUNT.value -> {
                    try {
                        sharedData.neededNotesReceivingBuffer.read()
                    } catch (e: IOException) {
                        return e
                    }
                }

                Header.NEEDED_NOTE_BUFFER_RECEIVED.value -> {
                    sharedData.receivingBuffer.skip(1)

                    sharedData.neededNotesChannel.send(Unit)
                }

                Header.REQUESTED_NOTES.value,
                Header.REQUESTED_NOTES_COUNT.value,
                Header.REQUESTED_NOTES_TITLE_LENGTH.value,
                Header.REQUESTED_NOTES_CONTENT_LENGTH.value,
                Header.REQUESTED_NOTES_CREATION_DATE.value,
                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value,
                Header.REQUESTED_NOTES_BUFFER_END.value -> {
                    try {
                        sharedData.requestedNoteReceivingBuffer.read()
                    } catch (e: IOException) {
                        return e
                    }

                    if (sharedData.requestedNoteReceivingBuffer.remoteElementCount == sharedData.neededNotesSendingBuffer.localElementCount) {
                        sharedData.hasReceivedAllRequestedNotes = true
                    }
                }

                Header.REQUESTED_NOTES_BUFFER_RECEIVED.value -> {
                    sharedData.receivingBuffer.skip(1)

                    sharedData.requestedNoteChannel.send(Unit)
                }

                Header.DATA_END.value -> {
                    sharedData.receivingBuffer.skip(1)

                    sharedData.hasOtherDeviceSentEverything = true
                    val dataEndReceived = ByteArray(1) { Header.DATA_END_RECEIVED.value }
                    sharedData.bluetoothConnectionManager.writeData(dataEndReceived)
                }

                Header.DATA_END_RECEIVED.value -> {
                    sharedData.receivingBuffer.skip(1)

                    sharedData.dataEndChannel.send(Unit)
                    sharedData.hasOtherDeviceReceivedDataEnd = true
                }

                else -> {
                    return IOException("Unknown header: ${sharedData.receivingBuffer.lookByte()}")
                }
            }

            if (sharedData.receivingBuffer.bytesFetchedAvailable() == 0) {
                if (sharedData.hasOtherDeviceSentEverything && sharedData.hasReceivedAllRequestedNotes && sharedData.hasOtherDeviceReceivedDataEnd) {
                    return null
                }
                try {
                    sharedData.receivingBuffer.fetchDataFromStart()
                } catch (e: IOException) {
                    return e
                }
            }
        }

        return Exception("Connection closed unexpectedly")
    }
}