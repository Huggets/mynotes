package com.huggets.mynotes.sync

/**
 * Header of the data sent or received.
 *
 * @property value Byte value of the header.
 */
enum class Header(
    val value: Byte,
) {
    /**
     * The sender sends this header to indicate that it has sent all the data.
     */
    DATA_END(0x00),

    /**
     * The receiver sends this header to indicate that it has received all the
     * data.
     */
    DATA_END_RECEIVED(0x01),

    /**
     * The sender sends this header to indicate that it is sending dates.
     */
    DATES(0x02),

    /**
     * The sender sends this header to indicate that it is sending the count of dates.
     */
    DATES_COUNT(0x03),

    /**
     * The receiver sends this header to indicate that it has received the dates.
     */
    DATES_BUFFER_RECEIVED(0x04),

    /**
     * The sender sends this header to indicate that it is sending needed notes.
     */
    NEEDED_NOTES(0x05),

    /**
     * The sender sends this header to indicate that it is sending the count of needed notes.
     */
    NEEDED_NOTES_COUNT(0x06),

    /**
     * The receiver sends this header to indicate that it has received the needed notes.
     */
    NEEDED_NOTES_BUFFER_RECEIVED(0x07),

    /**
     * The sender sends this header to indicate that it is sending requested notes.
     */
    REQUESTED_NOTES(0x08),

    /**
     * The sender sends this header to indicate that it is sending the count of requested notes.
     */
    REQUESTED_NOTES_COUNT(0x09),

    /**
     * The receiver sends this header to indicate that it has received the requested notes.
     */
    REQUESTED_NOTES_BUFFER_RECEIVED(0x0A),

    /**
     * The sender sends this header to indicate that it is the end of the current packet of the
     * requested note.
     *
     * It does not indicate the end of the requested note, just the end of the current part of it.
     */
    REQUESTED_NOTES_BUFFER_END(0x0B),

    /**
     * The sender sends this header to indicate that it is sending the title of the requested note.
     *
     * It may be sent multiple times if the title is too long to fit in a single packet.
     */
    REQUESTED_NOTES_TITLE(0x0C),

    /**
     * The sender sends this header to indicate that it is sending the content of the requested
     * note.
     *
     * It may be sent multiple times if the content is too long to fit in a single packet.
     */
    REQUESTED_NOTES_CONTENT(0x0D),

    /**
     * The sender sends this header to indicate that it is sending the creation date of the
     * requested note.
     */
    REQUESTED_NOTES_CREATION_DATE(0x0E),

    /**
     * The sender sends this header to indicate that it is sending the last modification date of
     * the requested note.
     */
    REQUESTED_NOTES_LAST_MODIFICATION_DATE(0x0F),

    /**
     * The sender sends this header to indicate that it is sending note associations.
     */
    ASSOCIATIONS(0x10),

    /**
     * The sender sends this header to indicate that it is sending the count of note associations.
     */
    ASSOCIATIONS_COUNT(0x11),

    /**
     * The receiver sends this header to indicate that it has received the note associations.
     */
    ASSOCIATIONS_BUFFER_RECEIVED(0x12),

    /**
     * The sender sends this header to indicate that it is sending deleted notes.
     */
    DELETED_NOTES(0x13),

    /**
     * The sender sends this header to indicate that it is sending the count of deleted notes.
     */
    DELETED_NOTES_COUNT(0x14),

    /**
     * The receiver sends this header to indicate that it has received the deleted notes.
     */
    DELETED_NOTES_BUFFER_RECEIVED(0x15),
}