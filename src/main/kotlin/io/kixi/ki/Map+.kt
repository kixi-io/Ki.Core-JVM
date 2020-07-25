package io.kixi.ki

/**
 * Stringify a map with the given separator and assignment character.
 *
 * @receiver Map<*,*>
 * @param separator CharSequence The separator for key/value pairs - default is ", "
 * @param assignment CharSequence The character between the key and value - default is "="
 * @return String?
 */
fun Map<*,*>.toString(separator: CharSequence = ", ", assignment: CharSequence = "="): String? {
    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.entries.iterator()
    while (i.hasNext()) {
        val pair = i.next()
        buffer.append(pair.key.toString()).append(assignment).append(pair.value.toString())
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}