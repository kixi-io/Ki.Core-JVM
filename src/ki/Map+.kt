package ki

fun Map<*,*>.toString(separator: CharSequence = ", ", assignment: CharSequence = "="): String? {
    if(this.isEmpty()) return ""
    val buffer = StringBuffer()
    val i = this.entries.iterator()
    while (i.hasNext()) {
        var pair = i.next();
        buffer.append(pair.key.toString()).append(assignment).append(pair.value.toString())
        if (i.hasNext()) buffer.append(separator)
    }
    return buffer.toString()
}