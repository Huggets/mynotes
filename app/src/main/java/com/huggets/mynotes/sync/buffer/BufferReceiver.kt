package com.huggets.mynotes.sync.buffer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

abstract class BufferReceiver<OutputType>(
    protected val buffer: ReceivingBuffer,
) {
    protected abstract val fetchedData: List<OutputType>

    var remoteElementCount: Int = 0
        private set

    private var remoteElementCountFetched: Boolean = false

    private val allDataReceived: Boolean
        get() = remoteElementCountFetched && remoteElementCount == fetchedData.size

    private val mutex = Mutex(true)

    @Throws(IOException::class)
    protected abstract fun readBuffer()

    fun read() {
        fetchElementCount()
        readBuffer()

        if (allDataReceived && mutex.isLocked) {
            mutex.unlock()
        }
    }

    private fun fetchElementCount() {
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
}
