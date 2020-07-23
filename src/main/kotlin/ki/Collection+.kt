package ki

fun Collection<*>.toString(separator: String? = ", "): String? {
    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.iterator()
    while (i.hasNext()) {
        buffer.append(i.next().toString())
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}

fun Array<*>.toString(separator: String? = ", "): String? {
    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.iterator()
    while (i.hasNext()) {
        buffer.append(i.next().toString())
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}