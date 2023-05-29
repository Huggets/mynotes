package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray
import java.io.IOException
import java.lang.Integer.min

class RequestedNoteReceivingBuffer(
    fetch: (ByteArray, Int, Int) -> Int,
    send: (ByteArray, Int, Int) -> Unit,
) : ReceivingBuffer<Note>(fetch, send) {

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
     * @param index The current index of the buffer.
     * @param maxIndex The max index of the buffer.
     *
     * @return A pair containing the new index and max index of the buffer.
     */
    private fun readString(
        buffer: ByteArray,
        index: Int,
        maxIndex: Int,
    ): Pair<Int, Int> {
        var newIndex = index
        var newMaxIndex = maxIndex

        val headerSize = 4
        fetchMoreDataIfNeeded(buffer, newIndex, newMaxIndex, headerSize).apply {
            newIndex = first
            newMaxIndex = second
        }

        val bytesCount = Int.fromByteArray(buffer, newIndex)
        newIndex += 4

        var bytesFetched = 0

        while (bytesFetched != bytesCount) {
            if (newMaxIndex == newIndex) {
                newMaxIndex = fetchData(buffer, 0, buffer.size)
                newIndex = 0
            }

            val bufferRemaining = newMaxIndex - newIndex
            val remoteRemaining = bytesCount - bytesFetched

            val bytesToCopy = min(bufferRemaining, remoteRemaining)

            buffer.copyInto(stringBuffer, stringIndex, newIndex, newIndex + bytesToCopy)
            stringIndex += bytesToCopy
            newIndex += bytesToCopy
            bytesFetched += bytesToCopy
        }

        return Pair(newIndex, newMaxIndex)
    }

    @Throws(IOException::class)
    override fun readBuffer(
        buffer: ByteArray,
        bufferIndex: Int,
        bufferMaxIndex: Int
    ): Pair<Int, Int> {
        var index = bufferIndex
        var maxIndex = bufferMaxIndex

        while (buffer[index] != Header.REQUESTED_NOTES_BUFFER_END.value) {
            when (buffer[index]) {
                Header.REQUESTED_NOTES.value -> {
                    index++

                    val headerSize = 8
                    fetchMoreDataIfNeeded(buffer, index, maxIndex, headerSize).apply {
                        index = first
                        maxIndex = second
                    }

                    titleLength = Int.fromByteArray(buffer, index)
                    index += 4
                    contentLength = Int.fromByteArray(buffer, index)
                    index += 4

                    stringBuffer = ByteArray(titleLength)
                    stringIndex = 0
                }

                Header.REQUESTED_NOTES_TITLE_LENGTH.value -> {
                    index++

                    readString(buffer, index, maxIndex).apply {
                        index = first
                        maxIndex = second
                    }

                    if (stringIndex == titleLength) {
                        title = String(stringBuffer)
                        stringBuffer = ByteArray(contentLength)
                        stringIndex = 0
                    }
                }

                Header.REQUESTED_NOTES_CONTENT_LENGTH.value -> {
                    index++

                    readString(buffer, index, maxIndex).apply {
                        index = first
                        maxIndex = second
                    }

                    if (stringIndex == contentLength) {
                        content = String(stringBuffer)
                    }
                }

                Header.REQUESTED_NOTES_CREATION_DATE.value -> {
                    index++

                    val headerSize = DataSynchronizer.DATE_SIZE
                    fetchMoreDataIfNeeded(buffer, index, maxIndex, headerSize).apply {
                        index = first
                        maxIndex = second
                    }

                    creationDate = Date.fromByteArray(buffer, index)
                    index += DataSynchronizer.DATE_SIZE
                }

                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value -> {
                    index++

                    val headerSize = DataSynchronizer.DATE_SIZE
                    fetchMoreDataIfNeeded(buffer, index, maxIndex, headerSize).apply {
                        index = first
                        maxIndex = second
                    }

                    modificationDate = Date.fromByteArray(buffer, index)
                    index += DataSynchronizer.DATE_SIZE

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
                    throw IOException("Unknown header: ${buffer[index]}")
                }
            }

            // If the end of the buffer is reached, fetch more data because BUFFER_END is not
            // received
            if (index == maxIndex) {
                maxIndex = fetchData(buffer, 0, buffer.size)
                index = 0
            }
        }

        index++

        sendData(confirmationBuffer, 0, confirmationBuffer.size)

        return Pair(index, maxIndex)
    }

    companion object {
        private val confirmationBuffer =
            ByteArray(1) { Header.REQUESTED_NOTES_BUFFER_RECEIVED.value }
    }
}