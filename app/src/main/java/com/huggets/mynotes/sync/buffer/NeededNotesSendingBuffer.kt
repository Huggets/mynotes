package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addDate

class NeededNotesSendingBuffer(buffer: ByteArray = ByteArray(1024)) :
    SendingBuffer<Date>(buffer, Header.NEEDED_NOTES_COUNT) {

    override fun fillBuffer() {
        buffer[bufferIndex] = Header.NEEDED_NOTE.value
        bufferIndex += 1

        var requestNoteDateCount = 0
        val requestNoteDateCountBufferIndex = bufferIndex
        bufferIndex += 1

        while (
            (buffer.size % (bufferIndex + DataSynchronizer.DATE_SIZE)) != buffer.size &&
            localDataIndex != localData.size &&
            requestNoteDateCount < 255
        ) {
            buffer.addDate(currentElement, bufferIndex)

            localDataIndex++
            requestNoteDateCount++
            bufferIndex += DataSynchronizer.DATE_SIZE
        }
        buffer[requestNoteDateCountBufferIndex] = requestNoteDateCount.toByte()
    }
}