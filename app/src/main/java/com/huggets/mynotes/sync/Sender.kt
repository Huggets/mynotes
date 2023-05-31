package com.huggets.mynotes.sync

import kotlinx.coroutines.channels.Channel

/**
 * Sends data to the other device.
 *
 * It can also wait until the other device has received the data.
 */
abstract class Sender {
    /**
     * Whether all the data has been sent.
     */
    abstract val allDataNotSent: Boolean

    /**
     * A channel that is used to wait until the other device has received the data.
     */
    private val confirmationChannel = Channel<Unit>(Channel.UNLIMITED)

    /**
     * Fills the buffer with data.
     *
     * This function should be called before the [send] function.
     */
    abstract fun fill()

    /**
     * Sends the data previously filled in the buffer.
     *
     * This function should be called after the [fill] function.
     */
    abstract fun send()

    /**
     * Waits until the other device has received the data.
     *
     * This function blocks until the [confirmDataReceived] function is called.
     */
    suspend fun waitUntilDataIsSent() {
        confirmationChannel.receive()
    }

    /**
     * Indicates that the other device has received the data.
     *
     * This unblocks the [waitUntilDataIsSent] function.
     */
    suspend fun confirmDataReceived() {
        confirmationChannel.send(Unit)
    }

    /**
     * Stops the sender.
     */
    fun stop() {
        confirmationChannel.close()
    }
}