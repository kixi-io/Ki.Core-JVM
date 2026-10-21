package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A call's attributes keep the order they were added in (as Kotlin's
 * `mutableMapOf` and KD's map literals do), and that order does not count
 * for equality, as it does not for two maps.
 */
class CallAttributeOrderTest : FunSpec({

    fun call(vararg attrs: Pair<String, Any?>): Call = Call(NSID("f")).apply { for ((k, v) in attrs) setAttribute(k, value = v) }

    test("toString writes the attributes in the order they were added") {
        call("zeta" to 1, "alpha" to 2, "mid" to 3).toString() shouldBe "f(zeta=1, alpha=2, mid=3)"
        call("alpha" to 2, "zeta" to 1).toString() shouldBe "f(alpha=2, zeta=1)"
    }

    test("the same attributes in another order are equal and hash alike") {
        val a = call("zeta" to 1, "alpha" to 2)
        val b = call("alpha" to 2, "zeta" to 1)
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        setOf(a, b).size shouldBe 1
        a.canonicalText() shouldBe b.canonicalText()
    }

    test("different attributes or values are still unequal") {
        call("zeta" to 1, "alpha" to 2) shouldNotBe call("zeta" to 1, "alpha" to 3)
        call("zeta" to 1) shouldNotBe call("zeta" to 1, "alpha" to 2)
        Call(NSID("f"), 1, 2) shouldNotBe Call(NSID("f"), 2, 1)    // values keep their order
    }

    test("replacing an attribute keeps its place; removing and adding puts it last") {
        val c = call("a" to 1, "b" to 2, "c" to 3)
        c.setAttribute("b", value = 20)
        c.toString() shouldBe "f(a=1, b=20, c=3)"
        c.attributes.remove(NSID("a"))
        c.setAttribute("a", value = 10)
        c.toString() shouldBe "f(b=20, c=3, a=10)"
    }

    test("attributes of a namespace come back in their order") {
        val c = call().apply {
            setAttribute("z", "lab", 1); setAttribute("x", "", 0); setAttribute("a", "lab", 2)
        }
        c.getAttributesInNamespace<Int>("lab").keys.toList() shouldBe listOf("z", "a")
    }
})
