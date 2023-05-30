package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addInt
import java.io.IOException

abstract class SendingBuffer<out InputType>(
    protected var buffer: ByteArray = ByteArray(1024),
    private val countHeader: DataSynchronizer.Companion.Header,
) {

    val currentElement: InputType
        get() = localData[localDataIndex]

    val localElementCount
        get() = localData.size

    val remoteElementsSent
        get() = localDataIndex

    val allDataNotSent: Boolean
        get() = elementCountNotSent || localDataIndex != localData.size

    protected lateinit var localData: List<@UnsafeVariance InputType>
        private set

    protected var bufferIndex = 0

    protected var localDataIndex = 0

    private var elementCountNotSent = true

    protected abstract fun fillBuffer()

    private fun fillElementCount() {
        if (elementCountNotSent) {
            buffer[bufferIndex] = countHeader.value
            bufferIndex += 1
            buffer.addInt(localData.size, bufferIndex)
            bufferIndex += 4

            elementCountNotSent = false
        }
    }

    fun fill() {
        fillElementCount()
        fillBuffer()
    }

    /**
     * Sends data to the other device.
     *
     * It sends the data that is in the buffer from the interval [0, bufferIndex).
     *
     * @throws IOException if an I/O error occurs
     *
     * @param bluetoothConnectionManager the BluetoothConnectionManager to use to send the data
     * to the other device
     */
    @Throws(IOException::class)
    fun send(bluetoothConnectionManager: BluetoothConnectionManager) {
        bluetoothConnectionManager.writeData(buffer, 0, bufferIndex)
        bufferIndex = 0
    }

    fun setData(localData: List<@UnsafeVariance InputType>) {
        this.localData = localData
    }
}