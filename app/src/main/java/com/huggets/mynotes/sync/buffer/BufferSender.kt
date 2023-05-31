package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.sync.Header
import com.huggets.mynotes.sync.Sender

/**
 * Send data to the other device using a buffer.
 */
abstract class BufferSender<InputType>(
    /**
     * The buffer used to store and send the data.
     */
    protected val buffer: SendingBuffer,

    /**
     * The header used to indicate the total number of elements that will be sent.
     *
     * This header is only sent once, the first time the buffer is sent.
     */
    private val countHeader: Header,
) : Sender() {
    /**
     * The current element processed.
     */
    val currentElement: InputType
        get() = localData[localDataIndex]

    /**
     * The total number of elements that will be sent.
     */
    val localElementCount
        get() = localData.size

    /**
     * Whether all the data has been sent.
     */
    override val allDataNotSent: Boolean
        get() = elementCountNotSent || localDataIndex != localData.size

    /**
     * The data that will be sent.
     */
    protected lateinit var localData: List<InputType>
        private set

    /**
     * The index of the current element.
     */
    protected var localDataIndex = 0

    /**
     * Whether the element count has been sent.
     *
     * Set to false after the first time the buffer is sent.
     */
    private var elementCountNotSent = true

    fun setData(data: List<InputType>) {
        this.localData = data
    }

    /**
     * Fills the buffer with the data that will be sent.
     */
    protected abstract fun fillBuffer()

    /**
     * Fills the buffer with the total element count that will be sent if it hasn't been sent yet.
     */
    private fun fillElementCount() {
        if (elementCountNotSent) {
            buffer.addByte(countHeader.value)
            buffer.addInt(localData.size)
            elementCountNotSent = false
        }
    }

    /**
     * Fills the buffer with the data that will be sent.
     *
     * The first time this function is called, the buffer is also filled with the total element
     * count.
     *
     * This function should be called before [send].
     */
    override fun fill() {
        fillElementCount()
        fillBuffer()
    }

    /**
     * Sends the data in the buffer.
     *
     * This function should be called after [fill].
     */
    override fun send() {
        buffer.sendData()
    }
}