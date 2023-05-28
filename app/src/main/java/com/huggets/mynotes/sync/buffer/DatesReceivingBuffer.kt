package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray
import com.huggets.mynotes.sync.buffer.Buffer.Companion.moveDataToStart

class DatesReceivingBuffer(
    fetch: (ByteArray, Int, Int) -> Int,
    send: (ByteArray, Int, Int) -> Unit,
) : ReceivingBuffer<Pair<Date, Date>>(fetch, send) {

    override val fetchedData: List<Pair<Date, Date>>
        get() = remoteCreationDates

    private val remoteCreationDates = mutableListOf<Pair<Date, Date>>()

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
        val fetchedDates = mutableListOf<Pair<Date, Date>>()

        while (fetchedDates.size != dateCount) {
            // If the two dates were not fully received, fetch more data
            if (maxIndex - index < DataSynchronizer.DATE_SIZE * 2) {
                // Move data to the beginning of the buffer if it is impossible to fit them
                if (buffer.size - index < DataSynchronizer.DATE_SIZE * 2) {
                    maxIndex = moveDataToStart(buffer, index, maxIndex)
                    index = 0
                }

                maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
            }
            val creationDate = Date.fromByteArray(buffer, index)
            index += DataSynchronizer.DATE_SIZE
            val modificationDate = Date.fromByteArray(buffer, index)
            index += DataSynchronizer.DATE_SIZE

            fetchedDates.add(Pair(creationDate, modificationDate))
        }

        remoteCreationDates.addAll(fetchedDates)

        sendData(dateReceivedBuffer, 0, dateReceivedBuffer.size)

        return Pair(index, maxIndex)
    }

    companion object {
        private val dateReceivedBuffer = ByteArray(1) { Header.DATES_BUFFER_RECEIVED.value }
    }
}