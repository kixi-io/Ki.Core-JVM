package io.kixi

/**
 * Stringify a map with the given separator and assignment character.
 *
 * @receiver Map<*,*>
 * @param separator CharSequence The separator for key/value pairs - default is ", "
 * @param assignment CharSequence The character between the key and value - default is "="
 * @return String?
 */
fun Map<*,*>.toString(separator: CharSequence = ", ", assignment: CharSequence = "=",
    formatter:(obj:Any?) -> String =
        { obj -> obj.toString() }): String? {

    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.entries.iterator()
    while (i.hasNext()) {
        val pair = i.next()
        buffer.append(formatter(pair.key)).append(assignment).append(formatter(pair.value))
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}
