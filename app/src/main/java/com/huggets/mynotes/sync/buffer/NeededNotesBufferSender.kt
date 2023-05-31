package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.sync.Header

class NeededNotesBufferSender(buffer: SendingBuffer) :
    BufferSender<Date>(buffer, Header.NEEDED_NOTES_COUNT) {

    override fun fillBuffer() {
        buffer.addByte(Header.NEEDED_NOTES.value)

        var requestNoteDateCount = 0
        buffer.saveSpace(1)

        while (
            buffer.spaceIsAvailable(Constants.DATE_SIZE) &&
            localDataIndex != localData.size &&
            requestNoteDateCount < 255
        ) {
            buffer.addDate(currentElement)

            localDataIndex++
            requestNoteDateCount++
        }
        buffer.addByteInSavedSpace(requestNoteDateCount.toByte())
    }
}