package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull

class CallTest : FunSpec({

    context("creation") {
        test("create with NSID") {
            val call = Call(NSID("myFunc"))
            call.nsid.name shouldBe "myFunc"
        }

        test("create with name string") {
            val call = Call("myFunc")
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe ""
        }

        test("create with name and namespace") {
            val call = Call("myFunc", "myNS")
            call.nsid.name shouldBe "myFunc"
            call.nsid.namespace shouldBe "myNS"
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

        test("call with attributes only") {
            val call = Call("config")
                .withAttribute("debug", value=true)
            call.toString() shouldBe "config(debug=true)"
        }

        test("call with values and attributes") {
            val call = Call("create")
                .withValue("item")
                .withAttribute("count", value=5)
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
    }

    context("equality") {
        test("equal calls") {
            val call1 = Call("func").withValue(1).withAttribute("a", value="b")
            val call2 = Call("func").withValue(1).withAttribute("a", value="b")
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
    }
})