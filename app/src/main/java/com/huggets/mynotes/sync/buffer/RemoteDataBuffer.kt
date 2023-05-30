package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import java.io.IOException


class RemoteDataBuffer(
    private val buffer: ByteArray,

    @get:Throws(IOException::class)
    private val fetch: (bytes: ByteArray, offset: Int, length: Int) -> Int,

    @get:Throws(IOException::class)
    private val send: (bytes: ByteArray, offset: Int, length: Int) -> Unit,
) {
    private var index: Int = 0
    private var maxIndex: Int = 0

    val size: Int
        get() = buffer.size

    var bytesFetched: Int = 0
        private set

    fun moveDataToStart() {
        buffer.copyInto(buffer, 0, index, maxIndex)
        maxIndex -= index
        index = 0
    }

    fun addInt(value: Int) {
        buffer[index++] = (value shr 24).toByte()
        buffer[index++] = (value shr 16).toByte()
        buffer[index++] = (value shr 8).toByte()
        buffer[index++] = value.toByte()
    }

    fun addDate(date: Date) {
        addInt(date.year)
        addInt(date.month)
        addInt(date.day)
        addInt(date.hour)
        addInt(date.minute)
        addInt(date.second)
        addInt(date.millisecond)
    }

    fun getBytes(length: Int): ByteArray {
        return buffer.copyOfRange(index, index + length).also {
            index += length
        }
    }

    fun getByte(): Byte {
        return buffer[index++]
    }

    // TODO Maybe do not use ubyte
    fun getUByte(): UByte {
        return buffer[index++].toUByte()
    }

    fun getInt(): Int {
        return (buffer[index++].toInt() and 0xFF shl 24) or
                (buffer[index++].toInt() and 0xFF shl 16) or
                (buffer[index++].toInt() and 0xFF shl 8) or
                (buffer[index++].toInt() and 0xFF)
    }

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

    @Throws(IOException::class)
    fun fetchData() {
        fetch(buffer, maxIndex, buffer.size - maxIndex).also {
            maxIndex += it
            bytesFetched = it
        }
    }

    @Throws(IOException::class)
    fun fetchDataFromStart() {
        fetch(buffer, 0, buffer.size).also {
            index = 0
            maxIndex = it
            bytesFetched = it
        }
    }

    @Throws(IOException::class)
    fun sendBytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        send(bytes, offset, length)
    }

    /**
     * Fetches more data if the required size is not available.
     *
     * @param requiredSize The required size.
     */
    @Throws(IOException::class)
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
}
