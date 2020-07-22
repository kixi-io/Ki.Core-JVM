package ki

import java.io.PrintStream

// TODO - warn?

fun log(vararg args:Any?) = out(*args /*, stream = System.out*/)

fun err(vararg args:Any?) = out(*args /*, stream = System.err*/)

fun out(vararg args:Any? /*, stream: PrintStream*/) : String {
    var printMe = ""

    if (args.isEmpty()) {
        // print blank line
    } else if(args.size == 1) {
        printMe = args[0].toString()
    } else {
        printMe = args.toString(", ")!!
    }

    // Hack until we can get System.err working properly
    System.out.println(printMe)

    return printMe
}


