package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.Header
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.CONTENT
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.CREATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.END
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.LAST_MODIFICATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteBufferSender.State.TITLE

class RequestedNoteBufferSender(buffer: SendingBuffer) :
    BufferSender<Note>(buffer, Header.REQUESTED_NOTES_COUNT) {
    private var state = END

    private lateinit var bytes: ByteArray
    private var bytesIndex = 0

    private val maxWritingSize
        get() = buffer.bytesAvailable - 1

    private fun addDate(header: Header, date: Date): Boolean {
        val headerSize = 1 + Constants.DATE_SIZE
        if (maxWritingSize < headerSize) {
            return true
        }

        buffer.addByte(header.value)
        buffer.addDate(date)

        return false
    }

    private fun addString(header: Header): Boolean {
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
                    val shouldBreak = addString(Header.REQUESTED_NOTES_TITLE_LENGTH)
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
                    val shouldBreak = addString(Header.REQUESTED_NOTES_CONTENT_LENGTH)
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

    private enum class State {
        END,
        TITLE,
        CONTENT,
        CREATION_DATE,
        LAST_MODIFICATION_DATE,
    }
}