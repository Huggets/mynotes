package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date

/**
 * A buffer used to receive data from the other device.
 *
 * @property buffer The buffer used to store the data.
 * @property fetch The function used to fetch data from the other device.
 * @property send The function used to send data to the other device.
 */
class ReceivingBuffer(
    private val buffer: ByteArray,
    private val fetch: (bytes: ByteArray, offset: Int, length: Int) -> Int,
    private val send: (bytes: ByteArray, offset: Int, length: Int) -> Unit,
) {
    /**
     * The index of the next byte to be read from the buffer.
     */
    private var index: Int = 0

    /**
     * The index of the last byte in the buffer that has been fetched.
     *
     * It may differ from the buffer size if the buffer is not full. Bytes read after this index
     * are not valid and should not be read.
     */
    private var maxIndex: Int = 0

    /**
     * The size of the buffer.
     */
    val size: Int
        get() = buffer.size

    /**
     * The number of bytes that have been fetched during the last fetch.
     */
    var bytesFetched: Int = 0
        private set

    /**
     * Reads the given number of bytes from the buffer.
     *
     * @return A new array containing the bytes.
     */
    fun getBytes(length: Int): ByteArray {
        return buffer.copyOfRange(index, index + length).also {
            index += length
        }
    }

    /**
     * Read a byte from the buffer.
     */
    fun getByte(): Byte {
        return buffer[index++]
    }

    // TODO Maybe do not use ubyte
    /**
     * Read an unsigned byte from the buffer.
     */
    fun getUByte(): UByte {
        return buffer[index++].toUByte()
    }

    /**
     * Read an int from the buffer.
     */
    fun getInt(): Int {
        return (buffer[index++].toInt() and 0xFF shl 24) or
                (buffer[index++].toInt() and 0xFF shl 16) or
                (buffer[index++].toInt() and 0xFF shl 8) or
                (buffer[index++].toInt() and 0xFF)
    }

    /**
     * Read a date from the buffer.
     */
    fun getDate(): Date {
        return Date(
            year = getInt(),
            month = getInt(),
            day = getInt(),
            hour = getInt(),
            minute = getInt(),
            second = getInt(),
            millisecond = getInt(),
        )
    }

    /**
     * Returns the byte at the current index without moving the index.
     */
    fun lookByte(): Byte {
        return buffer[index]
    }

    /**
     * Skips the given number of bytes.
     */
    fun skip(length: Int) {
        index += length
    }

    /**
     * Returns the number of bytes that can be read from the buffer.
     */
    fun bytesFetchedAvailable(): Int {
        return maxIndex - index
    }

    /**
     * Returns the number of bytes left in the buffer.
     */
    fun bytesLeft(): Int {
        return buffer.size - index
    }

    /**
     * Moves the data not yet read to the start of the buffer.
     */
    fun moveDataToStart() {
        buffer.copyInto(buffer, 0, index, maxIndex)
        maxIndex -= index
        index = 0
    }

    /**
     * Fetches more data from the other device and adds it to the buffer.
     */
    fun fetchData() {
        fetch(buffer, maxIndex, buffer.size - maxIndex).also {
            maxIndex += it
            bytesFetched = it
        }
    }

    /**
     * Fetches more data from the device and adds it at the start of the buffer.
     *
     * All the data that has not been read yet is lost and the index is reset to 0.
     */
    fun fetchDataFromStart() {
        fetch(buffer, 0, buffer.size).also {
            index = 0
            maxIndex = it
            bytesFetched = it
        }
    }

    /**
     * Fetches more data if the required size is not available.
     *
     * @param requiredSize The required size.
     */
    fun fetchMoreDataIfNeeded(requiredSize: Int) {
        while (bytesFetchedAvailable() < requiredSize) {
            // Move data to the beginning of the buffer if it is not possible to fetch all
            // the data
            if (bytesLeft() < requiredSize) {
                moveDataToStart()
            }

            fetchData()
        }
    }

    /**
     * Sends the given bytes to the other device.
     */
    fun sendBytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        send(bytes, offset, length)
    }
}
