package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import java.io.IOException
import java.lang.Integer.min

class RequestedNoteReceivingBuffer(buffer: RemoteDataBuffer) : ReceivingBuffer<Note>(buffer) {
    override val fetchedData: List<Note>
        get() = neededNotes

    private val neededNotes = mutableListOf<Note>()
    private lateinit var stringBuffer: ByteArray
    private var stringIndex = 0
    private var titleLength = 0
    private var contentLength = 0
    private lateinit var title: String
    private lateinit var content: String
    private lateinit var creationDate: Date
    private lateinit var modificationDate: Date

    /**
     * Read a packet containing a string.
     *
     * This packet start with an Int representing the number of bytes followed by the bytes.
     *
     * @param buffer The buffer to read from.
     */
    private fun readString(buffer: RemoteDataBuffer) {
        val headerSize = 4
        buffer.fetchMoreDataIfNeeded(headerSize)

        val bytesCount = buffer.getInt()
        var bytesFetched = 0

        while (bytesFetched != bytesCount) {
            if (buffer.bytesFetchedAvailable() == 0) {
                buffer.fetchDataFromStart()
            }

            val remoteRemaining = bytesCount - bytesFetched

            val bytesToCopy = min(buffer.bytesFetchedAvailable(), remoteRemaining)

            val bytes = buffer.getBytes(bytesToCopy)
            bytes.copyInto(stringBuffer, stringIndex, 0, bytesToCopy)
            stringIndex += bytesToCopy
            bytesFetched += bytesToCopy
        }
    }

    override fun readBuffer() {
        var header = buffer.getByte()
        while (header != Header.REQUESTED_NOTES_BUFFER_END.value) {
            when (header) {
                Header.REQUESTED_NOTES.value -> {
                    val headerSize = 8
                    buffer.fetchMoreDataIfNeeded(headerSize)

                    titleLength = buffer.getInt()
                    contentLength = buffer.getInt()

                    stringBuffer = ByteArray(titleLength)
                    stringIndex = 0
                }

                Header.REQUESTED_NOTES_TITLE_LENGTH.value -> {
                    readString(buffer)

                    if (stringIndex == titleLength) {
                        title = String(stringBuffer)
                        stringBuffer = ByteArray(contentLength)
                        stringIndex = 0
                    }
                }

                Header.REQUESTED_NOTES_CONTENT_LENGTH.value -> {
                    readString(buffer)

                    if (stringIndex == contentLength) {
                        content = String(stringBuffer)
                    }
                }

                Header.REQUESTED_NOTES_CREATION_DATE.value -> {
                    val headerSize = DataSynchronizer.DATE_SIZE
                    buffer.fetchMoreDataIfNeeded(headerSize)

                    creationDate = buffer.getDate()
                }

                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value -> {
                    val headerSize = DataSynchronizer.DATE_SIZE
                    buffer.fetchMoreDataIfNeeded(headerSize)

                    modificationDate = buffer.getDate()

                    // All information about the note is received, add it to the list
                    neededNotes.add(
                        Note(
                            title,
                            content,
                            creationDate,
                            modificationDate
                        )
                    )
                }

                else -> {
                    throw IOException("Unknown header: $header")
                }
            }

            // If the end of the buffer is reached, fetch more data because BUFFER_END is not
            // received
            if (buffer.bytesFetchedAvailable() == 0) {
                buffer.fetchDataFromStart()
            }

            header = buffer.getByte()
        }

        buffer.sendBytes(confirmationBuffer, 0, confirmationBuffer.size)
    }

    companion object {
        private val confirmationBuffer =
            ByteArray(1) { Header.REQUESTED_NOTES_BUFFER_RECEIVED.value }
    }
}