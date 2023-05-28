package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import com.huggets.mynotes.data.Note
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.Header
import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray
import com.huggets.mynotes.sync.buffer.Buffer.Companion.moveDataToStart
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
                    // If not fully received, fetch more data
                    if (maxIndex - index < headerSize) {
                        // Move data to the beginning of the buffer if it is not possible to fetch all
                        // the data
                        if (buffer.size - index < headerSize) {
                            maxIndex = moveDataToStart(buffer, index, maxIndex)
                            index = 0
                        }

                        maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
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

                    val headerSize = 4
                    // If header not fully received, fetch more data
                    if (maxIndex - index < headerSize) {
                        // Move data to the beginning of the buffer if it is not possible to fetch all
                        // the data
                        if (buffer.size - index < headerSize) {
                            maxIndex = moveDataToStart(buffer, index, maxIndex)
                            index = 0
                        }

                        maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
                    }

                    val bytesCount = Int.fromByteArray(buffer, index)
                    index += 4

                    var bytesFetched = 0

                    while (bytesFetched != bytesCount) {
                        if (maxIndex == index) {
                            maxIndex = fetchData(buffer, 0, buffer.size)
                            index = 0
                        }

                        val bufferRemaining = maxIndex - index
                        val remoteRemaining = bytesCount - bytesFetched

                        val bytesToCopy = min(bufferRemaining, remoteRemaining)

                        buffer.copyInto(stringBuffer, stringIndex, index, index + bytesToCopy)
                        stringIndex += bytesToCopy
                        index += bytesToCopy
                        bytesFetched += bytesToCopy
                    }

                    if (stringIndex == titleLength) {
                        title = String(stringBuffer)
                        stringBuffer = ByteArray(contentLength)
                        stringIndex = 0
                    }
                }

                Header.REQUESTED_NOTES_CONTENT_LENGTH.value -> {
                    index++

                    val headerSize = 4

                    // If header not fully received, fetch more data
                    if (maxIndex - index < headerSize) {
                        // Move data to the beginning of the buffer if it is not possible to fetch all
                        // the data
                        if (buffer.size - index < headerSize) {
                            maxIndex = moveDataToStart(buffer, index, maxIndex)
                            index = 0
                        }

                        maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
                    }

                    val bytesCount = Int.fromByteArray(buffer, index)
                    index += 4

                    var bytesFetched = 0

                    while (bytesFetched != bytesCount) {
                        if (maxIndex == index) {
                            maxIndex = fetchData(buffer, 0, buffer.size)
                            index = 0
                        }

                        val bufferRemaining = maxIndex - index
                        val remoteRemaining = bytesCount - bytesFetched

                        val bytesToCopy = min(bufferRemaining, remoteRemaining)

                        buffer.copyInto(stringBuffer, stringIndex, index, index + bytesToCopy)
                        stringIndex += bytesToCopy
                        index += bytesToCopy
                        bytesFetched += bytesToCopy
                    }

                    if (stringIndex == contentLength) {
                        content = String(stringBuffer)
                    }
                }

                Header.REQUESTED_NOTES_CREATION_DATE.value -> {
                    index++

                    val headerSize = DataSynchronizer.DATE_SIZE

                    // If header not fully received, fetch more data
                    if (maxIndex - index < headerSize) {
                        // Move data to the beginning of the buffer if it is not possible to fetch all
                        // the data
                        if (buffer.size - index < headerSize) {
                            maxIndex = moveDataToStart(buffer, index, maxIndex)
                            index = 0
                        }

                        maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
                    }

                    creationDate = Date.fromByteArray(buffer, index)
                    index += DataSynchronizer.DATE_SIZE
                }

                Header.REQUESTED_NOTES_LAST_MODIFICATION_DATE.value -> {
                    index++

                    val headerSize = DataSynchronizer.DATE_SIZE

                    // If header not fully received, fetch more data
                    if (maxIndex - index < headerSize) {
                        // Move data to the beginning of the buffer if it is not possible to fetch all
                        // the data
                        if (buffer.size - index < headerSize) {
                            maxIndex = moveDataToStart(buffer, index, maxIndex)
                            index = 0
                        }

                        maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
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