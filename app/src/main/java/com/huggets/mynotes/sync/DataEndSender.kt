package com.huggets.mynotes.sync

class DataEndSender(
    private val send: (ByteArray, Int, Int) -> Unit
) : Sender() {
    override var allDataNotSent: Boolean = true
        private set

    override fun fill() {}

    override fun send() {
        send(data, 0, data.size)
        allDataNotSent = false
    }

    companion object {
        private val data = ByteArray(1) { Header.DATA_END.value }
    }
}
