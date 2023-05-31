package com.huggets.mynotes.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Receives data from the other device.
 *
 * It can also send data to the other device indicating that the data has been received.
 *
 * @property sharedData The shared data between the sender and the receiver.
 */
class DataReceiver(
    private val sharedData: SharedData,
) {

    /**
     * Whether the other device has sent all the data.
     */
    private var hasOtherDeviceSentEverything = false

    /**
     * Whether the other device has received all the data.
     */
    private var hasOtherDeviceReceivedDataEnd = false

    /**
     * Starts receiving the data.
     *
     * @param coroutineScope The coroutine scope to use.
     * @param onFinish The function to call when the data has been received or an error occurred.
     */
    fun start(coroutineScope: CoroutineScope, onFinish: (Exception?) -> Unit) {
        coroutineScope.launch {
            val exception = receiveData()

            // Stop the sender if an error occurred.

            if (exception != null) {
                sharedData.stop()
            }

            onFinish(exception)
        }
    }

    /**
     * Receives all the data.
     *
     * It receives the data, processes it and sends a confirmation to the other device indicating
     * that it has been received. It repeats this until all the data has been received. If an error
     * occurs, it returns the error.
     *
     * @return The error that occurred or null if no error occurred.
     */
    private suspend fun receiveData(): Exception? {
        try {
            sharedData.fetchData()

            while (sharedData.bytesFetched != -1) {
                // Analyse the current byte (the header) and interpret the data accordingly.

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
                    Header.REQUESTED_NOTES_TITLE.value,
                    Header.REQUESTED_NOTES_CONTENT.value,
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
                    if (hasOtherDeviceSentEverything && hasOtherDeviceReceivedDataEnd) {
                        // All the data has been received.

                        return null
                    }
                    sharedData.fetchData()
                }
            }
        } catch (e: IOException) {
            return e
        }

        return IOException("Connection closed unexpectedly")
    }
}