package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addDate
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addInt
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer.State.CONTENT
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer.State.CREATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer.State.END
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer.State.LAST_MODIFICATION_DATE
import com.huggets.mynotes.sync.buffer.RequestedNoteSendingBuffer.State.TITLE

class RequestedNoteSendingBuffer(buffer: ByteArray = ByteArray(1024)) :
    SendingBuffer<Note>(buffer, Header.REQUESTED_NOTES_COUNT) {

    private var state = END

    private lateinit var bytes: ByteArray
    private var bytesIndex = 0

    private val maxWritingSize
        get() = buffer.size - bufferIndex - 1

    override fun fillBuffer() {
        while (allDataNotSent) {
            when (state) {
                END -> {
                    val headerSize = 9
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer[bufferIndex] = Header.REQUESTED_NOTES.value
                    bufferIndex += 1
                    buffer.addInt(currentElement.title.toByteArray().size, bufferIndex)
                    bufferIndex += 4
                    buffer.addInt(currentElement.content.toByteArray().size, bufferIndex)
                    bufferIndex += 4

                    state = TITLE
                    bytes = currentElement.title.toByteArray()
                    bytesIndex = 0
                }

                TITLE -> {
                    val headerSize = 5
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer[bufferIndex] = Header.REQUESTED_NOTES_TITLE_LENGTH.value
                    bufferIndex += 1
                    val sizeBufferIndex = bufferIndex
                    bufferIndex += 4

                    val copySize =
                        Integer.min(bytes.size - bytesIndex, maxWritingSize)
                    buffer.addInt(copySize, sizeBufferIndex)

                    bytes.copyInto(buffer, bufferIndex, bytesIndex, bytesIndex + copySize)
                    bytesIndex += copySize
                    bufferIndex += copySize

                    val titleCompleted = bytesIndex == bytes.size
                    if (titleCompleted) {
                        state = CONTENT
                        bytes = currentElement.content.toByteArray()
                        bytesIndex = 0
                    }
                }

                CONTENT -> {
                    val headerSize = 5
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer[bufferIndex] = Header.REQUESTED_NOTES_CONTENT_LENGTH.value
                    bufferIndex += 1
                    val sizeBufferIndex = bufferIndex
                    bufferIndex += 4

                    val copySize =
                        Integer.min(bytes.size - bytesIndex, maxWritingSize)
                    buffer.addInt(copySize, sizeBufferIndex)

                    bytes.copyInto(buffer, bufferIndex, bytesIndex, bytesIndex + copySize)
                    bytesIndex += copySize
                    bufferIndex += copySize

                    val titleCompleted = bytesIndex == bytes.size
                    if (titleCompleted) {
                        state = CREATION_DATE
                    }
                }

                CREATION_DATE -> {
                    val headerSize = 1 + DataSynchronizer.DATE_SIZE
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer[bufferIndex] =
                        Header.REQUESTED_NOTES_CREATION_DATE.value
                    bufferIndex += 1
                    buffer.addDate(currentElement.creationDate, bufferIndex)
                    bufferIndex += DataSynchronizer.DATE_SIZE

                    state = LAST_MODIFICATION_DATE
                }

                LAST_MODIFICATION_DATE -> {
                    val headerSize = 1 + DataSynchronizer.DATE_SIZE
                    if (maxWritingSize < headerSize) {
                        break
                    }

                    buffer[bufferIndex] = Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value
                    bufferIndex += 1
                    buffer.addDate(currentElement.lastEditTime, bufferIndex)
                    bufferIndex += DataSynchronizer.DATE_SIZE

                    localDataIndex++
                    state = END
                }
            }
        }

        buffer[bufferIndex] = Header.REQUESTED_NOTES_BUFFER_END.value
        bufferIndex += 1
    }

    private enum class State {
        END,
        TITLE,
        CONTENT,
        CREATION_DATE,
        LAST_MODIFICATION_DATE,
    }
}