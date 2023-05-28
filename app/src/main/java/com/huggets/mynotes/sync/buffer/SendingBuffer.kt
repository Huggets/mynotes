package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.bluetooth.BluetoothConnectionManager
import com.huggets.mynotes.sync.DataSynchronizer
import com.huggets.mynotes.sync.DataSynchronizer.Companion.addInt
import java.io.IOException

abstract class SendingBuffer<out InputType>(
    protected var buffer: ByteArray = ByteArray(1024)
) : Buffer {

    val currentElement: InputType
        get() = localData[localDataIndex]

    val elementsCount
        get() = localData.size

    val elementsSent
        get() = localDataIndex

    val allDataNotSent: Boolean
        get() = elementCountNotSent || elementsSent != elementsCount

    protected lateinit var localData: List<@UnsafeVariance InputType>
        private set

    protected var bufferIndex = 0

    protected var localDataIndex = 0

    private var elementCountNotSent = true

    /**
     * Fills the buffer with data to send.
     */
    abstract fun fill()

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
    }

    fun setData(localData: List<@UnsafeVariance InputType>) {
        this.localData = localData
    }

    fun fillElementCount(header: DataSynchronizer.Companion.Header) {
        if (elementCountNotSent) {
            buffer[bufferIndex] = header.value
            bufferIndex += 1
            buffer.addInt(localData.size, bufferIndex)
            bufferIndex += 4

            elementCountNotSent = false
        }
    }
}