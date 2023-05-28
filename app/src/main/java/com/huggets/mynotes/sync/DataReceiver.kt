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
        val buffer = ByteArray(8096)
        var bufferIndex = 0

        var byteCount = try {
            sharedData.bluetoothConnectionManager.readData(buffer, bufferIndex)
        } catch (e: IOException) {
            return e
        }

        while (byteCount != -1) {
            when (buffer[bufferIndex]) {
                Header.DATES.value, Header.DATES_COUNT.value -> {
                    try {
                        sharedData.datesReceivingBuffer.read(buffer, bufferIndex, byteCount).apply {
                            bufferIndex = first
                            byteCount = second
                        }
                    } catch (e: IOException) {
                        return e
                    }
                }

                Header.DATES_BUFFER_RECEIVED.value -> {
                    sharedData.datesSendingChannel.send(Unit)
                    bufferIndex++
                }

                Header.NEEDED_NOTE.value, Header.NEEDED_NOTES_COUNT.value -> {
                    try {
                        sharedData.neededNotesReceivingBuffer.read(buffer, bufferIndex, byteCount)
                            .apply {
                                bufferIndex = first
                                byteCount = second
                            }
                    } catch (e: IOException) {
                        return e
                    }
                }

                Header.NEEDED_NOTE_BUFFER_RECEIVED.value -> {
                    sharedData.neededNotesChannel.send(Unit)
                    bufferIndex++
                }

                Header.REQUESTED_NOTES.value,
                Header.REQUESTED_NOTES_COUNT.value,
                Header.REQUESTED_NOTES_TITLE_LENGTH.value,
                Header.REQUESTED_NOTES_CONTENT_LENGTH.value,
                Header.REQUESTED_NOTES_CREATION_DATE.value,
                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value,
                Header.REQUESTED_NOTES_BUFFER_END.value -> {
                    try {
                        sharedData.requestedNoteReceivingBuffer.read(buffer, bufferIndex, byteCount)
                            .apply {
                                bufferIndex = first
                                byteCount = second
                            }
                    } catch (e: IOException) {
                        return e
                    }

                    if (sharedData.requestedNoteReceivingBuffer.remoteElementCount == sharedData.neededNotesSendingBuffer.elementsCount) {
                        sharedData.hasReceivedAllRequestedNotes = true
                    }
                }

                Header.REQUESTED_NOTES_BUFFER_RECEIVED.value -> {
                    sharedData.requestedNoteChannel.send(Unit)
                    bufferIndex++
                }

                Header.DATA_END.value -> {
                    sharedData.hasOtherDeviceSentEverything = true
                    val dataEndReceived = ByteArray(1) { Header.DATA_END_RECEIVED.value }
                    sharedData.bluetoothConnectionManager.writeData(dataEndReceived)
                    bufferIndex++
                }

                Header.DATA_END_RECEIVED.value -> {
                    sharedData.dataEndChannel.send(Unit)
                    bufferIndex++
                    sharedData.hasOtherDeviceReceivedDataEnd = true
                }

                else -> {
                    return IOException("Unknown header: ${buffer[bufferIndex]}")
                }
            }

            if (byteCount == bufferIndex) {
                if (sharedData.hasOtherDeviceSentEverything && sharedData.hasReceivedAllRequestedNotes && sharedData.hasOtherDeviceReceivedDataEnd) {
                    return null
                }
                byteCount = try {
                    sharedData.bluetoothConnectionManager.readData(buffer)
                } catch (e: IOException) {
                    return e
                }

                bufferIndex = 0
            }
        }

        return Exception("Connection closed unexpectedly")
    }
}