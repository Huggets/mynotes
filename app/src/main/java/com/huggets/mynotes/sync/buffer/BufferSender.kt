package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.sync.Header
import com.huggets.mynotes.sync.Sender

abstract class BufferSender<InputType>(
    protected val buffer: SendingBuffer,
    private val countHeader: Header,
) : Sender() {
    val currentElement: InputType
        get() = localData[localDataIndex]

    val localElementCount
        get() = localData.size

    override val allDataNotSent: Boolean
        get() = elementCountNotSent || localDataIndex != localData.size

    protected lateinit var localData: List<InputType>
        private set

    protected var localDataIndex = 0

    private var elementCountNotSent = true

    protected abstract fun fillBuffer()

    private fun fillElementCount() {
        if (elementCountNotSent) {
            buffer.addByte(countHeader.value)
            buffer.addInt(localData.size)
            elementCountNotSent = false
        }
    }

    override fun fill() {
        fillElementCount()
        fillBuffer()
    }

    override fun send() {
        buffer.sendData()
    }

    fun setData(data: List<InputType>) {
        this.localData = data
    }
}