package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray

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

        fetchMoreDataIfNeeded(buffer, index, maxIndex, 1).apply {
            index = first
            maxIndex = second
        }

        val dateCount = buffer[index].toUByte().toInt()
        index += 1
        val fetchedDates = mutableListOf<Date>()

        while (fetchedDates.size != dateCount) {
            fetchMoreDataIfNeeded(buffer, index, maxIndex, DataSynchronizer.DATE_SIZE).apply {
                index = first
                maxIndex = second
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