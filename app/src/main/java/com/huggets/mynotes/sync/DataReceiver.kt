package com.huggets.mynotes.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

class DataReceiver(
    private val sharedData: SharedData,
) {
    private var hasOtherDeviceSentEverything = false

    private var hasOtherDeviceReceivedDataEnd = false

    private val hasReceivedAllRequestedNotes
        get() = sharedData.requestedNoteReceiver.remoteElementCount == sharedData.neededNotesSender.localElementCount


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
            sharedData.fetchData()

            while (sharedData.bytesFetched != -1) {
                when (sharedData.currentByte) {
                    Header.DATES.value, Header.DATES_COUNT.value -> {
                        sharedData.datesReceiver.read()
                    }

                    Header.DATES_BUFFER_RECEIVED.value -> {
                        sharedData.nextByte()
                        sharedData.datesSender.confirmDataReceived()
                    }

                    Header.NEEDED_NOTES.value, Header.NEEDED_NOTES_COUNT.value -> {
                        sharedData.neededNotesReceiver.read()
                    }

                    Header.NEEDED_NOTES_BUFFER_RECEIVED.value -> {
                        sharedData.nextByte()
                        sharedData.neededNotesSender.confirmDataReceived()
                    }

                    Header.REQUESTED_NOTES.value,
                    Header.REQUESTED_NOTES_COUNT.value,
                    Header.REQUESTED_NOTES_TITLE_LENGTH.value,
                    Header.REQUESTED_NOTES_CONTENT_LENGTH.value,
                    Header.REQUESTED_NOTES_CREATION_DATE.value,
                    Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value,
                    Header.REQUESTED_NOTES_BUFFER_END.value -> {
                        sharedData.requestedNoteReceiver.read()
                    }

                    Header.REQUESTED_NOTES_BUFFER_RECEIVED.value -> {
                        sharedData.nextByte()
                        sharedData.requestedNoteSender.confirmDataReceived()
                    }

                    Header.ASSOCIATIONS.value, Header.ASSOCIATIONS_COUNT.value -> {
                        sharedData.associationsReceiver.read()
                    }

                    Header.ASSOCIATIONS_BUFFER_RECEIVED.value -> {
                        sharedData.nextByte()
                        sharedData.associationsSender.confirmDataReceived()
                    }

                    Header.DATA_END.value -> {
                        sharedData.nextByte()

                        hasOtherDeviceSentEverything = true
                        val dataEndReceived = ByteArray(1) { Header.DATA_END_RECEIVED.value }
                        sharedData.sendData(dataEndReceived, 0, 1)
                    }

                    Header.DATA_END_RECEIVED.value -> {
                        sharedData.nextByte()
                        sharedData.dataEndSender.confirmDataReceived()
                        hasOtherDeviceReceivedDataEnd = true
                    }
                }

                if (sharedData.availableBytes == 0) {
                    if (
                        hasOtherDeviceSentEverything &&
                        hasReceivedAllRequestedNotes &&
                        hasOtherDeviceReceivedDataEnd
                    ) {
                        return null
                    }
                    sharedData.fetchData()
                }
            }
        } catch (e: IOException) {
            return e
        }

        return Exception("Connection closed unexpectedly")
    }
}