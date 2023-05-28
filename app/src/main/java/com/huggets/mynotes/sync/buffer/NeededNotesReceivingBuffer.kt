package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray
import com.huggets.mynotes.sync.buffer.Buffer.Companion.moveDataToStart

class NeededNotesReceivingBuffer(
    fetch: (ByteArray, Int, Int) -> Int,
    send: (ByteArray, Int, Int) -> Unit,
) : ReceivingBuffer<Date>(fetch, send) {

    override val fetchedData: List<Date>
        get() = neededNoteDates

    private val neededNoteDates = mutableListOf<Date>()

    override fun readBuffer(
        buffer: ByteArray,
        bufferIndex: Int,
        bufferMaxIndex: Int
    ): Pair<Int, Int> {

        var index = bufferIndex
        var maxIndex = bufferMaxIndex

        index++

        // If there is just the prefix and nothing else, fetch more data
        if (maxIndex - index == 0) {
            // Move data to the beginning of the buffer if it is full
            if (maxIndex >= buffer.size) {
                maxIndex = moveDataToStart(buffer, index, maxIndex)
                index = 0
            }

            maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
        }

        val dateCount = buffer[index++].toUByte().toInt()
        val fetchedDates = mutableListOf<Date>()

        while (fetchedDates.size != dateCount) {
            // If the date was not fully received, fetch more data
            if (maxIndex - index < DataSynchronizer.DATE_SIZE) {
                // Move data to the beginning of the buffer if it is impossible to fit a date
                if (buffer.size - index < DataSynchronizer.DATE_SIZE) {
                    maxIndex = moveDataToStart(buffer, index, maxIndex)
                    index = 0
                }

                maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
            }
            fetchedDates.add(Date.fromByteArray(buffer, index))
            index += DataSynchronizer.DATE_SIZE
        }

        neededNoteDates.addAll(fetchedDates)

        sendData(confirmationBuffer, 0, confirmationBuffer.size)

        return Pair(index, maxIndex)
    }

    companion object {
        private val confirmationBuffer = ByteArray(1) { Header.NEEDED_NOTE_BUFFER_RECEIVED.value }
    }
}