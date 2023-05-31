package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.Header
import java.io.IOException
import java.lang.Integer.min

/**
 * Receiver for the notes that this device requested.
 *
 * @param buffer The buffer used to receive the data.
 */
class RequestedNoteBufferReceiver(buffer: ReceivingBuffer) : BufferReceiver<Note>(buffer) {
    override val fetchedData: List<Note>
        get() = requestedNotes

    /**
     * The notes that this device requested.
     */
    private val requestedNotes = mutableListOf<Note>()

    /**
     * The buffer used to read the strings.
     */
    private lateinit var stringBuffer: ByteArray

    /**
     * The index of the next byte to write in the [stringBuffer].
     */
    private var stringIndex = 0

    /**
     * The length of the title of the note that is currently being read.
     */
    private var titleLength = 0

    /**
     * The length of the content of the note that is currently being read.
     */
    private var contentLength = 0

    /**
     * The title of the note that is currently being read.
     */
    private lateinit var title: String

    /**
     * The content of the note that is currently being read.
     */
    private lateinit var content: String

    /**
     * The creation date of the note that is currently being read.
     */
    private lateinit var creationDate: Date

    /**
     * The modification date of the note that is currently being read.
     */
    private lateinit var modificationDate: Date

    /**
     * Read a packet containing a string.
     *
     * This packet start with an Int representing the number of bytes followed by the bytes.
     *
     * @param buffer The buffer to read from.
     */
    private fun readString(buffer: ReceivingBuffer) {
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

                Header.REQUESTED_NOTES_TITLE.value -> {
                    readString(buffer)

                    if (stringIndex == titleLength) {
                        title = String(stringBuffer)
                        stringBuffer = ByteArray(contentLength)
                        stringIndex = 0
                    }
                }

                Header.REQUESTED_NOTES_CONTENT.value -> {
                    readString(buffer)

                    if (stringIndex == contentLength) {
                        content = String(stringBuffer)
                    }
                }

                Header.REQUESTED_NOTES_CREATION_DATE.value -> {
                    val headerSize = Constants.DATE_SIZE
                    buffer.fetchMoreDataIfNeeded(headerSize)

                    creationDate = buffer.getDate()
                }

                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value -> {
                    val headerSize = Constants.DATE_SIZE
                    buffer.fetchMoreDataIfNeeded(headerSize)

                    modificationDate = buffer.getDate()

                    // All information about the note is received, add it to the list

                    requestedNotes.add(
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

        buffer.sendBytes(confirmationBytes, 0, confirmationBytes.size)
    }

    companion object {
        /**
         * The bytes used to confirm that the buffer was received.
         */
        private val confirmationBytes =
            ByteArray(1) { Header.REQUESTED_NOTES_BUFFER_RECEIVED.value }
    }
}