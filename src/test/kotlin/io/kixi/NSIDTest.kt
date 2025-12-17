package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import io.kixi.text.ParseException

class NSIDTest : FunSpec({

    context("creation") {
        test("create with name only") {
            val nsid = NSID("tag")
            nsid.name shouldBe "tag"
            nsid.namespace shouldBe ""
            nsid.hasNamespace shouldBe false
        }

        test("create with name and namespace") {
            val nsid = NSID("tag", "my")
            nsid.name shouldBe "tag"
            nsid.namespace shouldBe "my"
            nsid.hasNamespace shouldBe true
        }

        test("create anonymous NSID") {
            val nsid = NSID("", "")
            nsid.isAnonymous shouldBe true
            nsid shouldBe NSID.ANONYMOUS
        }

        test("throws on namespace without name") {
            shouldThrow<ParseException> {
                NSID("", "namespace")
            }
        }

        test("throws on invalid name characters") {
            shouldThrow<ParseException> {
                NSID("invalid-name")  // hyphen not allowed
            }
        }

        test("throws on invalid namespace characters") {
            shouldThrow<ParseException> {
                NSID("name", "invalid-namespace")
            }
        }

        test("allows underscore in name") {
            val nsid = NSID("my_tag")
            nsid.name shouldBe "my_tag"
        }

        test("allows dollar sign in name") {
            val nsid = NSID("\$special")
            nsid.name shouldBe "\$special"
        }

        test("allows unicode letters in name") {
            val nsid = NSID("日本語")
            nsid.name shouldBe "日本語"
        }
    }

    context("parsing") {
        test("parse simple name") {
            val nsid = NSID.parse("tag")
            nsid.name shouldBe "tag"
            nsid.namespace shouldBe ""
        }

        test("parse namespaced name") {
            val nsid = NSID.parse("my:tag")
            nsid.name shouldBe "tag"
            nsid.namespace shouldBe "my"
        }

        test("parse empty string returns ANONYMOUS") {
            val nsid = NSID.parse("")
            nsid shouldBe NSID.ANONYMOUS
        }

        test("parse throws on multiple colons") {
            shouldThrow<ParseException> {
                NSID.parse("ns:name:with:colons")
            }
        }
    }

    context("toString") {
        test("name only") {
            NSID("tag").toString() shouldBe "tag"
        }

        test("with namespace") {
            NSID("tag", "my").toString() shouldBe "my:tag"
        }

        test("anonymous") {
            NSID.ANONYMOUS.toString() shouldBe ""
        }
    }

    context("equality and comparison") {
        test("equal NSIDs") {
            val nsid1 = NSID("tag", "my")
            val nsid2 = NSID("tag", "my")
            nsid1 shouldBe nsid2
        }

        test("different names not equal") {
            val nsid1 = NSID("tag1", "my")
            val nsid2 = NSID("tag2", "my")
            nsid1 shouldNotBe nsid2
        }

        test("different namespaces not equal") {
            val nsid1 = NSID("tag", "ns1")
            val nsid2 = NSID("tag", "ns2")
            nsid1 shouldNotBe nsid2
        }

        test("comparison uses string ordering") {
            val nsid1 = NSID("alpha")
            val nsid2 = NSID("beta")
            (nsid1 < nsid2) shouldBe true
        }

        test("namespaced comparison") {
            val nsid1 = NSID("tag", "a")
            val nsid2 = NSID("tag", "b")
            (nsid1 < nsid2) shouldBe true
        }
    }

    context("data class features") {
        test("copy with modifications") {
            val original = NSID("tag", "ns")
            val copied = original.copy(name = "newTag")
            copied.name shouldBe "newTag"
            copied.namespace shouldBe "ns"
        }

        test("destructuring") {
            val (name, namespace) = NSID("tag", "ns")
            name shouldBe "tag"
            namespace shouldBe "ns"
        }

        test("hashCode consistent with equals") {
            val nsid1 = NSID("tag", "ns")
            val nsid2 = NSID("tag", "ns")
            nsid1.hashCode() shouldBe nsid2.hashCode()
        }
    }

    context("dot-path style names") {
        test("allows dots in name for KD-style paths") {
            // Note: dots are specifically allowed for KD-style paths
            val nsid = NSID("path.to.element")
            nsid.name shouldBe "path.to.element"
        }
    }
})