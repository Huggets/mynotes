package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.sync.DataSynchronizer.Companion.fromByteArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

abstract class ReceivingBuffer<OutputType>(
    val fetchData: (buffer: ByteArray, offset: Int, length: Int) -> Int,
    val sendData: (buffer: ByteArray, offset: Int, length: Int) -> Unit,
) : Buffer {

    protected abstract val fetchedData: List<OutputType>

    var remoteElementCount: Int = 0
        private set

    private var remoteElementCountFetched: Boolean = false

    private val allDataReceived: Boolean
        get() = remoteElementCountFetched && remoteElementCount == fetchedData.size

    private val mutex = Mutex(true)

    /**
     * Read and process data from the buffer.
     *
     * @param buffer The buffer to read from.
     * @param bufferIndex The index to start reading from.
     * @param bufferMaxIndex The maximum index to read from, exclusive.
     *
     * @return The index where it stopped reading and the total number of bytes available in
     * the buffer.
     */
    @Throws(IOException::class)
    protected abstract fun readBuffer(
        buffer: ByteArray,
        bufferIndex: Int,
        bufferMaxIndex: Int,
    ): Pair<Int, Int>

    fun read(buffer: ByteArray, bufferIndex: Int, bufferMaxIndex: Int): Pair<Int, Int> {
        val result = fetchElementCount(buffer, bufferIndex, bufferMaxIndex).let {
            readBuffer(buffer, it.first, it.second)
        }

        if (allDataReceived && mutex.isLocked) {
            mutex.unlock()
        }

        return result
    }

    private fun fetchElementCount(
        buffer: ByteArray,
        bufferIndex: Int,
        bufferMaxIndex: Int,
    ): Pair<Int, Int> {

        if (remoteElementCountFetched) {
            return Pair(bufferIndex, bufferMaxIndex)
        } else {
            var index = bufferIndex
            var maxIndex = bufferMaxIndex

            index += 1

            if (maxIndex - index < 4) {
                if (maxIndex >= buffer.size) {
                    maxIndex = Buffer.moveDataToStart(buffer, index, maxIndex)
                    index = 0
                }

                maxIndex += fetchData(buffer, maxIndex, buffer.size - maxIndex)
            }

            remoteElementCount = Int.fromByteArray(buffer, index)
            index += 4


            remoteElementCountFetched = true

            return Pair(index, maxIndex)
        }
    }

    /**
     * Obtain the data that was fetched.
     *
     * If the data is not fully received, it suspends until it is.
     *
     * @return The data that was fetched.
     */
    suspend fun obtain(): List<OutputType> {
        mutex.withLock {
            return fetchedData
        }
    }

    /**
     * Fetches more data if the required size is not available.
     *
     * @param buffer The buffer to read from.
     * @param index The current index of the buffer.
     * @param maxIndex The max index of the buffer.
     * @param requiredSize The required size.
     *
     * @return A pair containing the new index and max index of the buffer.
     */
    protected fun fetchMoreDataIfNeeded(
        buffer: ByteArray,
        index: Int,
        maxIndex: Int,
        requiredSize: Int,
    ): Pair<Int, Int> {
        var newIndex = index
        var newMaxIndex = maxIndex

        while (newMaxIndex - newIndex < requiredSize) {
            // Move data to the beginning of the buffer if it is not possible to fetch all
            // the data
            if (buffer.size - newIndex < requiredSize) {
                newMaxIndex = Buffer.moveDataToStart(buffer, newIndex, newMaxIndex)
                newIndex = 0
            }

            newMaxIndex += fetchData(buffer, newMaxIndex, buffer.size - newMaxIndex)
        }

        return Pair(newIndex, newMaxIndex)
    }
}
