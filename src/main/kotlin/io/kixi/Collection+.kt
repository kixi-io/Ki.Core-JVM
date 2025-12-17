package io.kixi

/**
 * Stringify a collection with the given separator. The default is ", ".
 *
 * @receiver Collection<*>
 * @param separator String Defaults to ", "
 * @return String
 */
fun Collection<*>.toString(separator: String = ", ", formatter:(obj:Any?) -> String =
    { obj -> obj.toString() }): String {

    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.iterator()
    while (i.hasNext()) {
        buffer.append(formatter(i.next()))
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}

/**
 * Stringify an array with the given separator. The default is ", ".
 *
 * @receiver Collection<*>
 * @param separator String Defaults to ", "
 * @return String
 */
fun Array<*>.toString(separator: String = ", "): String {
    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.iterator()
    while (i.hasNext()) {
        buffer.append(i.next().toString())
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}
