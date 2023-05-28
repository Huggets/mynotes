package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addDate

class DatesSendingBuffer(buffer: ByteArray = ByteArray(1024)) :
    SendingBuffer<Note>(buffer) {

    override fun fill() {
        bufferIndex = 0

        fillElementCount(Header.DATES_COUNT)

        buffer[bufferIndex] = Header.DATES.value
        bufferIndex += 1

        var noteCount = 0
        val noteCountBufferIndex = bufferIndex
        bufferIndex += 1

        while (
            (buffer.size % (bufferIndex + DataSynchronizer.DATE_SIZE * 2)) != buffer.size &&
            localDataIndex != localData.size &&
            noteCount < 255
        ) {
            buffer.addDate(currentElement.creationDate, bufferIndex)
            bufferIndex += DataSynchronizer.DATE_SIZE
            buffer.addDate(currentElement.lastEditTime, bufferIndex)
            bufferIndex += DataSynchronizer.DATE_SIZE

            localDataIndex++
            noteCount++
        }
        buffer[noteCountBufferIndex] = noteCount.toByte()
    }
}