package com.huggets.mynotes.sync.buffer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Receives data from the other device and stores it in a buffer.
 *
 * @param buffer The buffer used to store the data.
 * @param DataType The type of the data that will be received.
 */
abstract class BufferReceiver<DataType>(
    protected val buffer: ReceivingBuffer,
) {
    /**
     * The data that was fetched.
     */
    protected abstract val fetchedData: List<DataType>

    /**
     * The total number of elements that will be received.
     */
    private var remoteElementCount: Int = 0

    /**
     * Whether the total number of elements that will be received was fetched.
     */
    private var remoteElementCountFetched: Boolean = false

    /**
     * Whether all the data was received.
     */
    private val allDataReceived: Boolean
        get() = remoteElementCountFetched && remoteElementCount == fetchedData.size

    /**
     * A mutex used to wait until all the data is received.
     *
     * This blocks the [obtain] method until all the data is received.
     */
    private val lock = Mutex(true)

    /**
     * Reads the total number of elements that will be received if it wasn't already read.
     */
    private fun readElementCount() {
        if (!remoteElementCountFetched) {
            buffer.skip(1) // Skip the header

            if (buffer.bytesFetchedAvailable() < 4) {
                if (buffer.bytesLeft() < 4) {
                    buffer.moveDataToStart()
                }

                buffer.fetchData()
            }

            remoteElementCount = buffer.getInt()

            remoteElementCountFetched = true
        }
    }

    /**
     * Reads and processes the data received.
     */
    protected abstract fun readBuffer()

    /**
     * Reads the data received and eventually the total number of elements that will be received.
     *
     * If the data is fully received, it unlocks the [lock].
     */
    fun read() {
        readElementCount()
        readBuffer()

        if (allDataReceived && lock.isLocked) {
            lock.unlock()
        }
    }

    /**
     * Obtain the data that was fetched.
     *
     * If the data is not fully received, it suspends until it is.
     *
     * @return The data that was fetched.
     */
    suspend fun obtain(): List<DataType> {
        lock.withLock {
            return fetchedData
        }
    }
}
