package ki

import java.io.PrintStream

/**
 * Ki standard logging. In the future, this will provide options for writing to the
 * filesystem and custom loggers.
 *
 * @param args Array<out Any?>
 * @return String
 */

fun log(vararg args:Any?) = out("", *args /*, stream = System.out*/)

fun err(vararg args:Any?) = out("ERROR> ", *args /*, stream = System.err*/)

fun warn(vararg args:Any?) = out("WARN> ", *args /*, stream = System.err*/)

private fun out(prefix:String = "", vararg args:Any? /*, stream: PrintStream*/) : String {
    var printMe = prefix

    if (args.isEmpty()) {
        // print blank line
    } else if(args.size == 1) {
        printMe += args[0].toString()
    } else {
        printMe += args.toString(", ")!!
    }

    // Hack until we can get System.err working properly
    println(printMe)

    return printMe
}

fun main() {
    log("hi")
    log(1,2,3)

    err("hi")
    err(1,2,3)

    warn("hi")
    warn(1,2,3)
}



