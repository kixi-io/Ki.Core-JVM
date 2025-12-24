// package io.kixi.lib

import io.kixi.*

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Hello, " + name + "!")

    val version = Version(23,12,5)
    println("Version: $version")

    for (i in 1..5) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        println("i = $i")
    }

    // Parse a blob literal
    val blob = Ki.parse(".blob(SGVsbG8gV29ybGQh)") as Blob

    // Access the data
    println(blob.decodeToString())  // "Hello World!"
    println(blob.size)              // 12

    // Round-trip
    println(Ki.format(blob))        // .blob(SGVsbG8gV29ybGQh)

    // URL-safe Base64 also works
    println("Raw: " + Blob.parse("SGVsbG8_V29ybGQh"))

    // URL-safe Base64 also works
    println("Literal" + Blob.parseLiteral(".blob(SGVsbG8_V29ybGQh)"))

    /*
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        nanoOfSecond: Int = 0,
        kiTZ: KiTZ
     */

    val pst = KiTZDateTime(2024, 3, 15, kiTZ = KiTZ.US_PST)
    println(pst)
    println(pst.kiFormat())
    println(KiTZDateTime("2024/3/15@14:23:35-US/PST"))
    println(KiTZDateTime("2024/3/15@14:23:35-US/PST").informalFormat())

    // Calls ////

    println(Call("fooFromString"))
    println(Call("fooStringWithNamespace", namespace = "myNS"))
    println(Call(NSID("fooFromNSID")))
    println(Call(NSID("fooFromNSIDWithValues"), 1, 2, 3))

    val colors = Call("foo")
    colors.attributes.set(NSID("color1"), "green")
    colors.attributes.set(NSID("color2"), "blue")
    println(colors)

    val pallet = Call("foo")
    pallet.values.add("purple")
    pallet.attributes.set(NSID("color1"), "green")
    pallet.attributes.set(NSID("color2"), "blue")
    println(pallet)
}