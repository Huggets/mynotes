package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.Header
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.CONTENT
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.CREATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.END
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.LAST_MODIFICATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.TITLE

/**
 * Sender for the notes that were requested.
 *
 * @param buffer The buffer used to send the requested notes.
 */
class RequestedNoteBufferSender(buffer: SendingBuffer) :
    BufferSender<Note>(buffer, Header.REQUESTED_NOTES_COUNT) {
    /**
     * The current state of the sender.
     */
    private var state = END

    /**
     * An array storing the bytes of a string that is being sent.
     *
     * It may be the title or the content of a note.
     */
    private lateinit var bytes: ByteArray

    /**
     * The current index of the [bytes] array.
     */
    private var bytesIndex = 0

    /**
     * The maximum number of bytes that can be written in the buffer.
     *
     * It is the number of bytes that are available in the buffer minus one, because the last one
     * is reserved to indicate the end of the buffer.
     */
    private val maxWritingSize
        get() = buffer.bytesAvailable - 1

    /**
     * Adds a date to the buffer.
     *
     * @param header The header of the date.
     * @param date The date to be added.
     *
     * @return True if the date was not added because there was not enough space in the buffer.
     * False otherwise.
     */
    private fun addDate(header: Header, date: Date): Boolean {
        val headerSize = 1 + Constants.DATE_SIZE
        if (maxWritingSize < headerSize) {
            return true
        }

        buffer.addByte(header.value)
        buffer.addDate(date)

        return false
    }

    /**
     * Adds bytes of [bytes] to the buffer.
     *
     * @param header The header corresponding to the data that is being sent.
     *
     * @return True if the bytes were not added because there was not enough space in the buffer.
     * False otherwise.
     */
    private fun addBytes(header: Header): Boolean {
        val headerSize = 5
        if (maxWritingSize < headerSize) {
            return true
        }

        buffer.addByte(header.value)
        buffer.saveSpace(4)

        val copySize =
            Integer.min(bytes.size - bytesIndex, maxWritingSize)

        buffer.addIntInSavedSpace(copySize)

        buffer.addBytes(bytes, bytesIndex, copySize)
        bytesIndex += copySize

        return false
    }

    override fun fillBuffer() {
        while (allDataNotSent) {
            when (state) {
                END -> {
                    val headerSize = 9
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer.addByte(Header.REQUESTED_NOTES.value)
                    buffer.addInt(currentElement.title.toByteArray().size)
                    buffer.addInt(currentElement.content.toByteArray().size)

                    state = TITLE
                    bytes = currentElement.title.toByteArray()
                    bytesIndex = 0
                }

                TITLE -> {
                    val shouldBreak = addBytes(Header.REQUESTED_NOTES_TITLE)
                    if (shouldBreak) {
                        break
                    }

                    val titleCompleted = bytesIndex == bytes.size
                    if (titleCompleted) {
                        state = CONTENT
                        bytes = currentElement.content.toByteArray()
                        bytesIndex = 0
                    }
                }

                CONTENT -> {
                    val shouldBreak = addBytes(Header.REQUESTED_NOTES_CONTENT)
                    if (shouldBreak) {
                        break
                    }

                    val contentCompleted = bytesIndex == bytes.size
                    if (contentCompleted) {
                        state = CREATION_DATE
                    }
                }

                CREATION_DATE -> {
                    val shouldBreak = addDate(
                        Header.REQUESTED_NOTES_CREATION_DATE, currentElement.creationDate
                    )
                    if (shouldBreak) {
                        break
                    }

                    state = LAST_MODIFICATION_DATE
                }

                LAST_MODIFICATION_DATE -> {
                    val shouldBreak = addDate(
                        Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE, currentElement.lastEditTime
                    )
                    if (shouldBreak) {
                        break
                    }

                    state = END
                    localDataIndex++
                }
            }
        }

        buffer.addByte(Header.REQUESTED_NOTES_BUFFER_END.value)
    }

    /**
     * States of the [RequestedNoteBufferSender].
     */
    private enum class State {
        /**
         * The sender is not sending any data.
         */
        END,

        /**
         * The sender is sending the title of the note.
         */
        TITLE,

        /**
         * The sender is sending the content of the note.
         */
        CONTENT,

        /**
         * The sender is sending the creation date of the note.
         */
        CREATION_DATE,

        /**
         * The sender is sending the last modification date of the note.
         */
        LAST_MODIFICATION_DATE,
    }
}