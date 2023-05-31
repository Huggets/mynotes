package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.NoteAssociation
import com.huggets.mynotes.sync.Header

/**
 * Sender for the note associations.
 *
 * @param buffer The buffer used to send the data.
 */
class AssociationsBufferSender(buffer: SendingBuffer) :
    BufferSender<NoteAssociation>(buffer, Header.ASSOCIATIONS_COUNT) {
    override fun fillBuffer() {
        buffer.addByte(Header.ASSOCIATIONS.value)

        var associationsCount = 0
        buffer.saveSpace(1)

        while (
            buffer.spaceIsAvailable(Constants.DATE_SIZE * 2) &&
            localDataIndex != localData.size &&
            associationsCount < 255
        ) {
            buffer.addDate(currentElement.parentCreationDate)
            buffer.addDate(currentElement.childCreationDate)

            localDataIndex++
            associationsCount++
        }
        buffer.addByteInSavedSpace(associationsCount.toByte())
    }
}
