package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull

class CallTest : FunSpec({

    context("creation - NSID-based constructors") {
        test("create with NSID only") {
            val call = Call(NSID("myFunc"))
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe ""
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe false
        }

        test("create with namespaced NSID") {
            val call = Call(NSID("myFunc", "myNS"))
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe "myNS"
        }

        test("create with NSID and vararg values") {
            val call = Call(NSID("add"), 1, 2, 3)
            call.nsid.name shouldBe "add"
            call.hasValues() shouldBe true
            call.valueCount shouldBe 3
            call[0] shouldBe 1
            call[1] shouldBe 2
            call[2] shouldBe 3
            call.hasAttributes() shouldBe false
        }

        test("create with NSID and single value") {
            val call = Call(NSID("greet"), "Hello")
            call.valueCount shouldBe 1
            call[0] shouldBe "Hello"
        }

        test("create with NSID and null value in varargs") {
            val call = Call(NSID("test"), "first", null, "third")
            call.valueCount shouldBe 3
            call[0] shouldBe "first"
            call[1] shouldBe null
            call[2] shouldBe "third"
        }

        test("create with NSID and attributes map") {
            val attrs = mapOf(
                NSID("debug") to true,
                NSID("level") to 5
            )
            val call = Call(NSID("config"), attrs)
            call.nsid.name shouldBe "config"
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe true
            call.attributeCount shouldBe 2
            call.getAttribute<Boolean>(NSID("debug")) shouldBe true
            call.getAttribute<Int>(NSID("level")) shouldBe 5
        }

        test("create with NSID and empty attributes map") {
            val call = Call(NSID("empty"), emptyMap())
            call.hasAttributes() shouldBe false
            call.attributeCount shouldBe 0
        }

        test("create with NSID, values list, and attributes map") {
            val values = listOf("item", 42)
            val attrs = mapOf(NSID("urgent") to true)
            val call = Call(NSID("create"), values, attrs)

            call.nsid.name shouldBe "create"
            call.hasValues() shouldBe true
            call.valueCount shouldBe 2
            call[0] shouldBe "item"
            call[1] shouldBe 42
            call.hasAttributes() shouldBe true
            call.getAttribute<Boolean>(NSID("urgent")) shouldBe true
        }

        test("create with NSID, empty values, and attributes") {
            val attrs = mapOf(NSID("key") to "value")
            val call = Call(NSID("test"), emptyList(), attrs)
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe true
        }

        test("create with NSID, values, and empty attributes") {
            val call = Call(NSID("test"), listOf(1, 2), emptyMap())
            call.hasValues() shouldBe true
            call.hasAttributes() shouldBe false
        }
    }

    context("creation - String-based constructors") {
        test("create with name only") {
            val call = Call("myFunc")
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe ""
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe false
        }

        test("create with name and namespace") {
            val call = Call("myFunc", "myNS")
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe "myNS"
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe false
        }

        test("create with name and values (using named parameter)") {
            val call = Call("add", values = listOf(1, 2, 3))
            call.nsid.name shouldBe "add"
            call.nsid.namespace shouldBe ""
            call.hasValues() shouldBe true
            call.valueCount shouldBe 3
            call[0] shouldBe 1
            call[1] shouldBe 2
            call[2] shouldBe 3
            call.hasAttributes() shouldBe false
        }

        test("create with name, namespace, and values") {
            val call = Call("method", "pkg", values = listOf("arg1", "arg2"))
            call.nsid.name shouldBe "method"
            call.nsid.namespace shouldBe "pkg"
            call.valueCount shouldBe 2
        }

        test("create with name and attributes only") {
            val attrs = mapOf(
                NSID("debug") to true,
                NSID("timeout") to 30
            )
            val call = Call("config", attributes = attrs)
            call.nsid.name shouldBe "config"
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe true
            call.attributeCount shouldBe 2
            call.getAttribute<Boolean>(NSID("debug")) shouldBe true
            call.getAttribute<Int>(NSID("timeout")) shouldBe 30
        }

        test("create with name, namespace, and attributes") {
            val attrs = mapOf(NSID("key") to "value")
            val call = Call("func", "ns", attributes = attrs)
            call.nsid.name shouldBe "func"
            call.nsid.namespace shouldBe "ns"
            call.hasAttributes() shouldBe true
        }

        test("create with name, values, and attributes") {
            val values = listOf("item", 100)
            val attrs = mapOf(NSID("priority") to "high")
            val call = Call("create", values = values, attributes = attrs)

            call.nsid.name shouldBe "create"
            call.hasValues() shouldBe true
            call.hasAttributes() shouldBe true
            call[0] shouldBe "item"
            call[1] shouldBe 100
            call.getAttribute<String>(NSID("priority")) shouldBe "high"
        }

        test("create with all parameters") {
            val values = listOf(1, 2, 3)
            val attrs = mapOf(NSID("sum") to 6, NSID("count") to 3)
            val call = Call("calculate", "math", values, attrs)

            call.nsid.name shouldBe "calculate"
            call.nsid.namespace shouldBe "math"
            call.valueCount shouldBe 3
            call.attributeCount shouldBe 2
        }

        test("create with null values parameter") {
            val call = Call("func", values = null)
            call.hasValues() shouldBe false
        }

        test("create with null attributes parameter") {
            val call = Call("func", attributes = null)
            call.hasAttributes() shouldBe false
        }

        test("create with empty values list") {
            val call = Call("func", values = emptyList())
            call.hasValues() shouldBe false
        }

        test("create with empty attributes map") {
            val call = Call("func", attributes = emptyMap())
            call.hasAttributes() shouldBe false
        }
    }

    context("values (indexed arguments)") {
        test("initially no values") {
            val call = Call("func")
            call.hasValues() shouldBe false
            call.valueCount shouldBe 0
        }

        test("add and access values") {
            val call = Call("func")
            call.values.add("hello")
            call.values.add(42)
            call.hasValues() shouldBe true
            call.valueCount shouldBe 2
            call[0] shouldBe "hello"
            call[1] shouldBe 42
        }

        test("set value by index") {
            val call = Call("func")
            call.values.add("original")
            call[0] = "modified"
            call[0] shouldBe "modified"
        }

        test("hasValue checks bounds") {
            val call = Call("func")
            call.values.add("test")
            call.hasValue(0) shouldBe true
            call.hasValue(1) shouldBe false
            call.hasValue(-1) shouldBe false
        }

        test("getValueOrDefault returns default for missing index") {
            val call = Call("func")
            call.values.add("exists")
            call.getValueOrDefault(0, "default") shouldBe "exists"
            call.getValueOrDefault(1, "default") shouldBe "default"
        }

        test("getValueOrNull returns null for missing index") {
            val call = Call("func")
            call.values.add("exists")
            call.getValueOrNull<String>(0) shouldBe "exists"
            call.getValueOrNull<String>(1).shouldBeNull()
        }

        test("value convenience property") {
            val call = Call("func")
            call.value.shouldBeNull()
            call.value = "first"
            call.value shouldBe "first"
            call.value = "updated"
            call.value shouldBe "updated"
            call.valueCount shouldBe 1
        }
    }

    context("attributes (named arguments)") {
        test("initially no attributes") {
            val call = Call("func")
            call.hasAttributes() shouldBe false
            call.attributeCount shouldBe 0
        }

        test("add and access attributes") {
            val call = Call("func")
            call.setAttribute("name", value = "John")
            call.setAttribute("age", value = 30)
            call.hasAttributes() shouldBe true
            call.attributeCount shouldBe 2
            call.getAttribute<String>("name") shouldBe "John"
            call.getAttribute<Int>("age") shouldBe 30
        }

        test("set attribute with NSID") {
            val call = Call("func")
            val nsid = NSID("attr", "ns")
            call.setAttribute(nsid, "value")
            call.getAttribute<String>(nsid) shouldBe "value"
        }

        test("operator access with name") {
            val call = Call("func")
            call["name"] = "Alice"
            call["name"] shouldBe "Alice"
        }

        test("hasAttribute checks existence") {
            val call = Call("func")
            call.setAttribute("exists", value = true)
            call.hasAttribute("exists") shouldBe true
            call.hasAttribute("missing") shouldBe false
        }

        test("getAttributeOrDefault returns default when missing") {
            val call = Call("func")
            call.setAttribute("exists", value = "value")
            call.getAttributeOrDefault("exists", "default") shouldBe "value"
            call.getAttributeOrDefault("missing", "default") shouldBe "default"
        }

        test("getAttributeOrNull returns null when missing") {
            val call = Call("func")
            call.setAttribute("exists", value = "value")
            call.getAttributeOrNull<String>("exists") shouldBe "value"
            call.getAttributeOrNull<String>("missing").shouldBeNull()
        }

        test("getAttributesInNamespace filters by namespace") {
            val call = Call("func")
            call.setAttribute("a", namespace = "ns1", value = 1)
            call.setAttribute("b", namespace = "ns1", value = 2)
            call.setAttribute("c", namespace = "ns2", value = 3)
            call.setAttribute("d", value = 4) // no namespace

            val ns1Attrs = call.getAttributesInNamespace<Int>("ns1")
            ns1Attrs.size shouldBe 2
            ns1Attrs["a"] shouldBe 1
            ns1Attrs["b"] shouldBe 2

            val noNsAttrs = call.getAttributesInNamespace<Int>("")
            noNsAttrs.size shouldBe 1
            noNsAttrs["d"] shouldBe 4
        }
    }

    context("fluent builders") {
        test("withValue adds and returns self") {
            val call = Call("func")
                .withValue("a")
                .withValue("b")
            call.valueCount shouldBe 2
            call[0] shouldBe "a"
            call[1] shouldBe "b"
        }

        test("withValues adds multiple values") {
            val call = Call("func")
                .withValues("a", "b", "c")
            call.valueCount shouldBe 3
        }

        test("withAttribute adds and returns self") {
            val call = Call("func")
                .withAttribute("x", value=1)
                .withAttribute("y", value=2)
            call.attributeCount shouldBe 2
            call.getAttribute<Int>("x") shouldBe 1
        }

        test("chaining values and attributes") {
            val call = Call("point")
                .withValue(10)
                .withValue(20)
                .withAttribute("label", value = "A")

            call.valueCount shouldBe 2
            call.attributeCount shouldBe 1
        }

        test("combine constructor and fluent builders") {
            val call = Call("func", values = listOf(1, 2))
                .withValue(3)
                .withAttribute("total", value = 6)

            call.valueCount shouldBe 3
            call.attributeCount shouldBe 1
            call[2] shouldBe 3
        }
    }

    context("toString formatting") {
        test("empty call") {
            Call("func").toString() shouldBe "func()"
        }

        test("call with values only") {
            val call = Call("add")
                .withValue(1)
                .withValue(2)
            call.toString() shouldBe "add(1, 2)"
        }

        test("call with values from constructor") {
            val call = Call(NSID("add"), 1, 2)
            call.toString() shouldBe "add(1, 2)"
        }

        test("call with attributes only") {
            val call = Call("config")
                .withAttribute("debug", value=true)
            call.toString() shouldBe "config(debug=true)"
        }

        test("call with attributes from constructor") {
            val call = Call(NSID("config"), mapOf(NSID("debug") to true))
            call.toString() shouldBe "config(debug=true)"
        }

        test("call with values and attributes") {
            val call = Call("create")
                .withValue("item")
                .withAttribute("count", value=5)
            call.toString() shouldBe "create(\"item\", count=5)"
        }

        test("call with values and attributes from constructor") {
            val call = Call(
                NSID("create"),
                listOf("item"),
                mapOf(NSID("count") to 5)
            )
            call.toString() shouldBe "create(\"item\", count=5)"
        }

        test("namespaced call") {
            val call = Call("method", "pkg")
            call.toString() shouldBe "pkg:method()"
        }

        test("null value formatted as nil") {
            val call = Call("test").withValue(null)
            call.toString() shouldBe "test(nil)"
        }

        test("null value in constructor varargs") {
            val call = Call(NSID("test"), null)
            call.toString() shouldBe "test(nil)"
        }
    }

    context("equality") {
        test("equal calls") {
            val call1 = Call("func").withValue(1).withAttribute("a", value="b")
            val call2 = Call("func").withValue(1).withAttribute("a", value="b")
            call1 shouldBe call2
        }

        test("equal calls created differently") {
            val call1 = Call(NSID("func"), 1, 2, 3)
            val call2 = Call("func", values = listOf(1, 2, 3))
            call1 shouldBe call2
        }

        test("equal calls with attributes from constructor") {
            val attrs = mapOf(NSID("x") to 1)
            val call1 = Call(NSID("func"), attrs)
            val call2 = Call("func", attributes = attrs)
            call1 shouldBe call2
        }

        test("different values not equal") {
            val call1 = Call("func").withValue(1)
            val call2 = Call("func").withValue(2)
            call1 shouldNotBe call2
        }

        test("different attributes not equal") {
            val call1 = Call("func").withAttribute("a", value=1)
            val call2 = Call("func").withAttribute("a", value=2)
            call1 shouldNotBe call2
        }

        test("hashCode consistent with equals") {
            val call1 = Call("func").withValue(1)
            val call2 = Call("func").withValue(1)
            call1.hashCode() shouldBe call2.hashCode()
        }

        test("hashCode consistent for differently constructed calls") {
            val call1 = Call(NSID("func"), 1, 2)
            val call2 = Call("func", values = listOf(1, 2))
            call1.hashCode() shouldBe call2.hashCode()
        }
    }

    context("lazy initialization") {
        test("values not initialized until accessed") {
            val call = Call("func")
            // hasValues should not trigger initialization
            call.hasValues() shouldBe false
            // Accessing values triggers initialization
            call.values.add("test")
            call.hasValues() shouldBe true
        }

        test("attributes not initialized until accessed") {
            val call = Call("func")
            // hasAttributes should not trigger initialization
            call.hasAttributes() shouldBe false
            // Accessing attributes triggers initialization
            call.attributes[NSID("key")] = "value"
            call.hasAttributes() shouldBe true
        }

        test("constructor with values initializes values list") {
            val call = Call(NSID("func"), 1, 2, 3)
            call.hasValues() shouldBe true
        }

        test("constructor with attributes initializes attributes map") {
            val call = Call(NSID("func"), mapOf(NSID("key") to "value"))
            call.hasAttributes() shouldBe true
        }

        test("constructor with empty collections does not initialize") {
            val call = Call("func", values = emptyList(), attributes = emptyMap())
            call.hasValues() shouldBe false
            call.hasAttributes() shouldBe false
        }
    }

    context("mixed type values and attributes") {
        test("heterogeneous values") {
            val call = Call(NSID("mixed"), "text", 42, 3.14, true, null)
            call.valueCount shouldBe 5
            call[0] shouldBe "text"
            call[1] shouldBe 42
            call[2] shouldBe 3.14
            call[3] shouldBe true
            call[4] shouldBe null
        }

        test("heterogeneous attributes") {
            val attrs = mapOf(
                NSID("name") to "Alice",
                NSID("age") to 30,
                NSID("active") to true,
                NSID("score") to 95.5
            )
            val call = Call(NSID("user"), attrs)
            call.getAttribute<String>(NSID("name")) shouldBe "Alice"
            call.getAttribute<Int>(NSID("age")) shouldBe 30
            call.getAttribute<Boolean>(NSID("active")) shouldBe true
            call.getAttribute<Double>(NSID("score")) shouldBe 95.5
        }
    }
})