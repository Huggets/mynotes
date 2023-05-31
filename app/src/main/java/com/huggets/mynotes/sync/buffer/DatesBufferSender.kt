package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.Header

/**
 * Sender for the dates of the notes.
 *
 * @param buffer The buffer used to send the data.
 */
class DatesBufferSender(buffer: SendingBuffer) :
    BufferSender<Note>(buffer, Header.DATES_COUNT) {

    override fun fillBuffer() {
        buffer.addByte(Header.DATES.value)

        // Save the space for the number of dates that will be sent.

        var noteCount = 0
        buffer.saveSpace(1)

        // Add the creation date and the last modification date of each note to the buffer until
        // there is no more space or there are no more notes.

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

        // Write the number of dates that were sent in the space that was saved for it.

        buffer.addByteInSavedSpace(noteCount.toByte())
    }
}