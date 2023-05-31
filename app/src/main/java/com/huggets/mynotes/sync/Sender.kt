package com.huggets.mynotes.sync

import kotlinx.coroutines.channels.Channel

abstract class Sender {
    abstract val allDataNotSent: Boolean

    private val confirmationChannel = Channel<Unit>(Channel.UNLIMITED)

    abstract fun fill()

    abstract fun send()

    suspend fun waitUntilDataIsSent() {
        confirmationChannel.receive()
    }

    suspend fun confirmDataReceived() {
        confirmationChannel.send(Unit)
    }

    fun close() {
        confirmationChannel.close()
    }
}