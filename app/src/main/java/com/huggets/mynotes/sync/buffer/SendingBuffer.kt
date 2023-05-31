package com.huggets.mynotes.sync.buffer

import com.huggets.mynotes.data.Date

/**
 * A buffer used to send data to the other device.
 *
 * @property buffer The buffer used to store the data.
 * @property send The function used to send data to the other device.
 */
class SendingBuffer(
    private val buffer: ByteArray,
    private val send: (bytes: ByteArray, offset: Int, length: Int) -> Unit,
) {
    /**
     * The index of the next byte to be added to the buffer.
     */
    private var index: Int = 0

    /**
     * An index used to remember a position in the buffer.
     *
     * Can be used later to add data at this position.
     */
    private var rememberedIndex: Int = 0

    /**
     * The number of bytes left that can be written to the buffer.
     */
    val bytesAvailable: Int
        get() = buffer.size - index

    /**
     * Adds the given bytes to the buffer.
     */
    fun addBytes(bytes: ByteArray, offset: Int, length: Int) {
        bytes.copyInto(buffer, index, offset, offset + length)
        index += length
    }

    /**
     * Adds the given byte to the buffer.
     */
    fun addByte(value: Byte) {
        buffer[index++] = value
    }

    /**
     * Adds the given int to the buffer.
     */
    fun addInt(value: Int) {
        buffer[index++] = (value shr 24).toByte()
        buffer[index++] = (value shr 16).toByte()
        buffer[index++] = (value shr 8).toByte()
        buffer[index++] = value.toByte()
    }

    /**
     * Adds the given date to the buffer.
     */
    fun addDate(date: Date) {
        addInt(date.year)
        addInt(date.month)
        addInt(date.day)
        addInt(date.hour)
        addInt(date.minute)
        addInt(date.second)
        addInt(date.millisecond)
    }

    /**
     * Sends the data in the buffer.
     */
    fun sendData() {
        send(buffer, 0, index)
        index = 0
    }

    /**
     * Saves a space in the buffer to be used later.
     *
     * @param length The length of the space to be saved. It must be less than or equal to the
     * number of bytes left in the buffer and be positive, otherwise method using the saved space
     * will behave unexpectedly.
     */
    fun saveSpace(length: Int) {
        rememberedIndex = index
        index += length
    }

    /**
     * Adds the given byte to the saved space.
     */
    fun addByteInSavedSpace(value: Byte) {
        buffer[rememberedIndex] = value
    }

    /**
     * Adds the given int to the saved space.
     */
    fun addIntInSavedSpace(value: Int) {
        buffer[rememberedIndex] = (value shr 24).toByte()
        buffer[rememberedIndex + 1] = (value shr 16).toByte()
        buffer[rememberedIndex + 2] = (value shr 8).toByte()
        buffer[rememberedIndex + 3] = value.toByte()
    }

    /**
     * Indicates whether there is enough space in the buffer to add the given number of bytes.
     *
     * @param length The number of bytes to be added.
     *
     * @return `true` if there is enough space in the buffer to add the given number of bytes,
     * `false` otherwise.
     */
    fun spaceIsAvailable(length: Int): Boolean {
        return index + length <= buffer.size
    }
}