package io.kixi

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * TB7 re-check (2026-10-29), Ki.Core, a regression of the F17 fix: Vector.parse strips block
 * comments before line comments, so a block-comment opener inside a `#` or `//` comment opens a
 * block comment, where KD (TB7RecheckKDCommentsTest) reads the same text as two numbers.
 */
class TB7RecheckCoreCommentsTest : FunSpec({
    test("C1 a /* inside a line comment is part of the comment") {
        val problems = ArrayList<String>()
        for (text in listOf(".vec<Int> {\n 1 # see /* the notes\n 2\n}", ".vec<Int> {\n 1 // a /* b\n 2\n}", ".vec<Int> {\n 1 # a */ b\n 2\n}")) {
            try { val v = Vector.parse(text); if (v != Vector.ofInts(1, 2)) problems += "${text.replace("\n", "⏎")} -> $v" } catch (e: Exception) { problems += "${text.replace("\n", "⏎")}: ${e.message}" }
        }
        if (problems.isNotEmpty()) println("[C1] ${problems.size} problems:\n  " + problems.joinToString("\n  "))
        withClue(problems.joinToString("\n")) { problems.size shouldBe 0 }
    }
})
