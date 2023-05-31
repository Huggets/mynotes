package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.Header

class DatesBufferSender(buffer: SendingBuffer) :
    BufferSender<Note>(buffer, Header.DATES_COUNT) {

    override fun fillBuffer() {
        buffer.addByte(Header.DATES.value)

        var noteCount = 0
        buffer.saveSpace(1)

        while (
            buffer.spaceIsAvailable(Constants.DATE_SIZE * 2) &&
            localDataIndex != localData.size &&
            noteCount < 255
        ) {
            buffer.addDate(currentElement.creationDate)
            buffer.addDate(currentElement.lastEditTime)

            localDataIndex++
            noteCount++
        }
        buffer.addByteInSavedSpace(noteCount.toByte())
    }
}