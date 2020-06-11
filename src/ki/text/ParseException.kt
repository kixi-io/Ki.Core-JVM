package ki.text

class ParseException : RuntimeException {

    var line = -1
    var index = -1

    constructor(message:String, index:Int = -1, line:Int = -1, cause:Throwable? = null) : super(message, cause) {
        this.index = index
        this.line = line
    }

    override fun getLocalizedMessage(): String {
        var msg = if(message.isNullOrEmpty()) "ParseException" else "ParseException: \"$message\""

        if(line!=-1) msg+= " line: $line"
        if(index!=-1) msg+= " index: $index"
        if(cause!=null) msg+= " cause: $cause.message"

        return msg;
    }

    override fun toString() : String = getLocalizedMessage()
}