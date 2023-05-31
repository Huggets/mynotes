package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date
import java.io.IOException

class SendingBuffer(
    private val buffer: ByteArray = ByteArray(1024),

    @get:Throws(IOException::class)
    private val send: (bytes: ByteArray, offset: Int, length: Int) -> Unit,
) {
    private var index: Int = 0
    private var rememberedIndex: Int = 0

    val bytesAvailable: Int
        get() = buffer.size - index

    fun addBytes(bytes: ByteArray, offset: Int, length: Int) {
        bytes.copyInto(buffer, index, offset, offset + length)
        index += length
    }

    fun addByte(value: Byte) {
        buffer[index++] = value
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

    fun sendData() {
        send(buffer, 0, index)
        index = 0
    }

    /**
     * Length must be positive or the behaviour will be undefined.
     */
    fun saveSpace(length: Int) {
        rememberedIndex = index
        index += length
    }

    fun addByteInSavedSpace(value: Byte) {
        buffer[rememberedIndex] = value
    }

    fun addIntInSavedSpace(value: Int) {
        buffer[rememberedIndex] = (value shr 24).toByte()
        buffer[rememberedIndex + 1] = (value shr 16).toByte()
        buffer[rememberedIndex + 2] = (value shr 8).toByte()
        buffer[rememberedIndex + 3] = value.toByte()
    }

    fun spaceIsAvailable(length: Int): Boolean {
        return index + length <= buffer.size
    }
}