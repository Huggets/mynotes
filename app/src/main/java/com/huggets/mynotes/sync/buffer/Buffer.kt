package com.huggets.mynotes.sync.buffer

interface Buffer {
    companion object {
        /**
         * Move data to the start of the buffer.
         *
         * It move data from the interval [bufferIndex, bufferMaxIndex) to the start of the buffer.
         *
         * @param buffer The buffer to move data in.
         * @param bufferIndex The index to start moving data from.
         * @param bufferMaxIndex The maximum index to move data from, exclusive.
         *
         * @return The size of the data moved.
         */
        fun moveDataToStart(buffer: ByteArray, bufferIndex: Int, bufferMaxIndex: Int): Int {
            val tmp = buffer.copyOf()
            val copySize = bufferMaxIndex - bufferIndex
            tmp.copyInto(buffer, 0, bufferIndex, bufferMaxIndex)

            return copySize
        }
    }
}
