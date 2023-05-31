package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.DeletedNote
import com.huggets.mynotes.sync.Header

/**
 * Sender for the deleted notes.
 *
 * @param buffer The buffer used to send the data.
 */
class DeletedNotesSender(buffer: SendingBuffer) :
    BufferSender<DeletedNote>(buffer, Header.DELETED_NOTES_COUNT) {
    override fun fillBuffer() {
        buffer.addByte(Header.DELETED_NOTES.value)

        var sendingCount = 0
        buffer.saveSpace(1)

        while (
            buffer.spaceIsAvailable(Constants.DATE_SIZE) &&
            localDataIndex != localData.size &&
            sendingCount < 255
        ) {
            buffer.addDate(currentElement.creationDate)

            localDataIndex++
            sendingCount++
        }
        buffer.addByteInSavedSpace(sendingCount.toByte())
    }
}