package io.kixi.types

import io.kixi.Ki
import io.kixi.Type
import io.kixi.text.ParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

private fun bag(
    name: String, types: List<ValueType<*>> = emptyList(), functions: List<BagFunction> = emptyList(),
    constants: Map<String, Any> = emptyMap(), requires: List<String> = emptyList(),
    extensions: Map<String, List<Member<*>>> = emptyMap(), version: Int = 1,
): TypeBag = object : TypeBag {
    override val name = name
    override val version = version
    override val requires = requires
    override val types = types
    override val functions = functions
    override val constants = constants
    override val extensions = extensions
}

private class Other(val x: Int)

/** A subclass of the sample bag's Shape, for the overlap check. */
class FancyShape : Shape("circle")

private fun type(name: String, literals: List<String> = emptyList(), cls: kotlin.reflect.KClass<*> = Other::class,
                 members: List<Member<Any>> = emptyList()): ValueType<*> = object : ValueType<Any> {
    override val name = name
    @Suppress("UNCHECKED_CAST")
    override val kotlinClass = cls as kotlin.reflect.KClass<Any>
    override val literals = literals
    override fun parse(literal: String): Any = Other(0)
    override fun format(value: Any) = ".x(0)"
    override fun encode(value: Any) = ByteArray(0)
    override fun decode(bytes: ByteArray, version: Int): Any = Other(0)
    override val members = members
}

class TypeBagsTest : FunSpec({

    context("registry") {

        test("a registered bag's types, literals, functions, constants and members are found") {
            withSampleBags {
                TypeBags.byName("tb") shouldBe SampleBag
                TypeBags.literal("seq") shouldBe SeqType
                TypeBags.literal("sq") shouldBe SeqType
                TypeBags.typeNamed("Seq") shouldBe SeqType
                TypeBags.typeFor(Seq("ACGT")) shouldBe SeqType
                TypeBags.bagOf(SeqType) shouldBe SampleBag
                TypeBags.function("revcomp")!!.invoke(listOf(Seq("AACG"))) shouldBe Seq("CGTT")
                TypeBags.constant("answer") shouldBe 42
                TypeBags.member(Seq("GGAT"), "gc")!!.invoke(Seq("GGAT"), emptyList()) shouldBe 0.5
                TypeBags.member(Seq(""), "gc")!!.invoke(Seq(""), emptyList()).shouldBeNull()      // a member may be nil
                TypeBags.member(Seq("A"), "nope").shouldBeNull()
                TypeBags.all.map { it.name } shouldContain "tb"
            }
            TypeBags.byName("tb").shouldBeNull()
            TypeBags.literal("seq").shouldBeNull()
            TypeBags.typeFor(Seq("A")).shouldBeNull()
        }

        test("a value of a subclass, or of a class implementing a registered interface, is of that type") {
            withSampleBags {
                TypeBags.typeFor(object : Shape("circle") {}) shouldBe ShapeType
                TypeBags.typeFor(PairImpl(1, 2)) shouldBe PairishType
                TypeBags.typeFor("text").shouldBeNull()
                TypeBags.typeFor(Other(1)).shouldBeNull()
            }
        }

        test("an extension bag adds members after the type's own, in order") {
            withSampleBags {
                TypeBags.member(Seq("ACGT"), "isPalindrome")!!.invoke(Seq("ACGT"), emptyList()) shouldBe true
                TypeBags.members(SeqType).keys.toList() shouldContainExactly
                        listOf("length", "gc", "slice", "reverse", "contains", "trim", "isPalindrome")
            }
            withSampleBags(extension = false) {
                TypeBags.member(Seq("ACGT"), "isPalindrome").shouldBeNull()
            }
        }

        test("registering the same bag twice does nothing") {
            withSampleBags {
                TypeBags.register(SampleBag)
                TypeBags.all.count { it.name == "tb" } shouldBe 1
            }
        }

        test("every name is checked, and a conflict names both bags") {
            withSampleBags {
                fun refused(b: TypeBag): String = shouldThrow<TypeBagException> { TypeBags.register(b) }.message
                refused(bag("tb")) shouldContain "Bag name 'tb' is taken"
                refused(bag("x1", listOf(type("Seq")))) shouldContain "Type name 'Seq' is taken (bags 'tb' and 'x1')"
                refused(bag("x1", listOf(type("Seq2", listOf("seq"))))) shouldContain "Literal name 'seq' is taken (tb.Seq and x1.Seq2)"
                refused(bag("x1", listOf(type("Seq2", cls = Seq::class)))) shouldContain "has two types (tb.Seq and x1.Seq2)"
                refused(bag("x1", functions = listOf(BagFunction("revcomp", 1..1) { null }))) shouldContain "'revcomp' is taken (bags 'tb' and 'x1')"
                refused(bag("x1", constants = mapOf("concat" to 1))) shouldContain "'concat' is taken (bags 'tb' and 'x1')"
                refused(bag("x1", listOf(type("Q", listOf("id"))))) shouldContain "literal name 'id' of Q is reserved"
                refused(bag("x1", listOf(type("String")))) shouldContain "type name 'String' is reserved"
                refused(bag("x1", listOf(type("Txt", cls = String::class)))) shouldContain "is a Ki.Core String"
                refused(bag("x1", listOf(type("Lst", cls = ArrayList::class)))) shouldContain "is a Ki.Core List"
                refused(bag("x1", listOf(type("Num", cls = java.util.concurrent.atomic.AtomicInteger::class)))) shouldContain "is a Ki.Core Number"
                refused(bag("x1", listOf(type("Chars", cls = CharSequence::class)))) shouldContain "is a supertype of Ki.Core's String"
                refused(bag("x1", listOf(type("Cmp", cls = Comparable::class)))) shouldContain "is a supertype of Ki.Core's"
                refused(bag("x1", listOf(type("Q", listOf("SEQ"))))) shouldContain "not letters starting with a lowercase letter"
                refused(bag("x1", listOf(type("Q", listOf("sEQ"))))) shouldContain "Literal name 'sEQ' is taken (tb.Seq and x1.Q)"
                refused(bag("x1", listOf(type("Function")))) shouldContain "type name 'Function' is reserved"
                refused(bag("x1", listOf(type("Bag", cls = java.util.HashSet::class)))) shouldContain "is a Ki.Core Collection"
                refused(bag("x1", listOf(type("Fancy", cls = FancyShape::class)))) shouldContain "overlaps tb.Shape (io.kixi.types.Shape)"
                refused(bag("x1", listOf(type("Pairs", cls = PairImpl::class)))) shouldContain "overlaps tb.Pairish"
                // Another bag object with a taken name is refused, even of the same class.
                refused(bag("tb")) shouldContain "Bag name 'tb' is taken"
                refused(bag("X1")) shouldContain "not a lowercase identifier"
                refused(bag("x1", listOf(type("lower")))) shouldContain "not a capitalized identifier"
                refused(bag("x1", listOf(type("Q", listOf("Dna"))))) shouldContain "not letters starting with a lowercase letter"
                refused(bag("x1", listOf(type("Q", listOf("dna2"))))) shouldContain "not letters starting with a lowercase letter"
                refused(bag("x1", listOf(type("Q", listOf("q", "q"))))) shouldContain "lists literal 'q' twice"
                refused(bag("x1", version = 0)) shouldContain "version must be at least 1"
                refused(bag("x1", requires = listOf("bio"))) shouldContain "requires bag 'bio', which is not registered"
                refused(bag("x1", listOf(type("Q", members = listOf(Member("a", null) { _, _ -> 1 }, Member("a", 0..0) { _, _ -> 2 }))))) shouldContain "two members called 'a'"
                refused(bag("x1", functions = listOf(BagFunction("f", 2..1) { null }))) shouldContain "is not a range of argument counts"
                refused(bag("x1", extensions = mapOf("Nope" to emptyList()))) shouldContain "extends type 'Nope', which no registered bag has"
                refused(bag("x1", extensions = mapOf("Seq" to emptyList()))) shouldContain "does not require bag 'tb'"
                refused(bag("x1", requires = listOf("tb"), extensions = mapOf("Seq" to listOf(Member<Seq>("gc", null) { _, _ -> 0 })))) shouldContain
                        "adds member 'gc' to Seq, which already has one"
                // A refused bag leaves nothing behind.
                TypeBags.byName("x1").shouldBeNull()
                TypeBags.typeNamed("Q").shouldBeNull()
            }
        }

        test("a bag another bag requires cannot be removed first") {
            withSampleBags {
                shouldThrow<TypeBagException> { TypeBags.unregister("tb") }.message shouldContain "bag 'tbx' requires it"
                TypeBags.byName("tb") shouldBe SampleBag
            }
        }

        test("discovered bags are read from their manifests; broken ones are skipped and reported") {
            TypeBags.literal("tally")!!.parse(".tally(7)").let { (it as Tally).n } shouldBe 7      // refers to disc
            TypeBags.byName("disc").shouldNotBeNull()
            TypeBags.byName("clash").shouldBeNull()
            TypeBags.byName("needy").shouldBeNull()
            TypeBags.byName("throwing").shouldBeNull()
            TypeBags.literal("buoy").shouldBeNull()
            val problems = TypeBags.problems.joinToString("\n")
            problems shouldContain "Type bag class io.kixi.types.DoesNotExist has no manifest (META-INF/ki/bags/io.kixi.types.DoesNotExist.properties)"
            problems shouldContain "Type bag 'clash' (io.kixi.types.ClashingBag) was skipped: Literal name 'tally' is taken (disc.Tally and clash.Clash)"
            problems shouldContain "Type bag 'needy' (io.kixi.types.NeedyBag) was skipped: it requires 'nowhere', which is not available"
            problems shouldContain "Type bag 'throwing' (io.kixi.types.ThrowingBag) was skipped: java.lang.IllegalStateException: no name today"
            problems shouldContain "Type bag 'stale' (io.kixi.types.StaleBag) was skipped: its manifest does not match the bag (literals is {buoy=Buoy, float=Buoy}, the bag has {buoy=Buoy})"
            // Discovered bags stay; unregister is for bags added with register.
            TypeBags.unregister("disc")
            TypeBags.byName("disc").shouldNotBeNull()
            // Discovering the same loader again changes nothing.
            TypeBags.discover(TypeBag::class.java.classLoader)
            TypeBags.all.count { it.name == "disc" } shouldBe 1
        }

        test("a discovered bag is created when first referred to, with the bags it requires and no other") {
            fun active(n: String) = TypeBags.isActive(n)
            // preload=true: created at discovery.
            active("preloaded").shouldBeTrue()
            Created.counts["preloaded"] shouldBe 1
            // The others are available, not created.
            TypeBags.available.containsAll(listOf("lazybase", "lazytop", "lazyother")).shouldBeTrue()
            listOf("lazybase", "lazytop", "lazyother").forEach { active(it).shouldBeFalse(); Created.counts[it].shouldBeNull() }
            TypeBags.all.none { it.name.startsWith("lazy") }.shouldBeTrue()
            // Questions about other names create nothing; the catalog answers what a lazy bag offers.
            TypeBags.literal("nothing").shouldBeNull()
            TypeBags.typeNamed("Nothing").shouldBeNull()
            TypeBags.function("nothing").shouldBeNull()
            TypeBags.typeFor("text").shouldBeNull()
            TypeBags.hasInactiveMember("stacked").shouldBeTrue()
            TypeBags.hasInactiveMember("nothing").shouldBeFalse()
            ("windy" in TypeBags.valueNames).shouldBeTrue()
            listOf("lazybase", "lazytop", "lazyother").forEach { Created.counts[it].shouldBeNull() }
            // A value of a bag's class refers to that bag; the bag that extends it is not needed yet.
            TypeBags.typeFor(Pebble(3))!!.name shouldBe "Pebble"
            active("lazybase").shouldBeTrue()
            active("lazytop").shouldBeFalse()
            TypeBags.member(Pebble(3), "size")!!.invoke(Pebble(3), emptyList()) shouldBe 3
            active("lazytop").shouldBeFalse()
            // A member another bag adds refers to that bag.
            TypeBags.member(Pebble(3), "stacked")!!.invoke(Pebble(3), emptyList()) shouldBe 6
            active("lazytop").shouldBeTrue()
            (Ki.parse(".cairn(2)") as Cairn).n shouldBe 2
            // Unrelated bags stay as they were.
            active("lazyother").shouldBeFalse()
            Created.counts["lazyother"].shouldBeNull()
            TypeBags.constant("windy") shouldBe 7
            active("lazyother").shouldBeTrue()
            listOf("lazybase", "lazytop", "lazyother", "preloaded").forEach { Created.counts[it] shouldBe 1 }
        }

        test("a literal or function refers to its bag, which brings the bags it requires") {
            // lazytop requires lazybase: asking for lazytop by name makes both (they may be active already).
            TypeBags.activate("lazytop").shouldNotBeNull()
            TypeBags.isActive("lazybase").shouldBeTrue()
            TypeBags.function("pebbles")!!.invoke(listOf(4)) shouldBe Pebble(4)
            TypeBags.activate("nowhere").shouldBeNull()
        }

        test("describe: every bag, how it was loaded, its names; creates nothing") {
            withSampleBags {
                val before = Created.counts.toMap()
                val info = TypeBags.describe().associateBy { it.name }
                info["preloaded"]!!.status shouldBe TypeBagInfo.Status.PRELOADED
                info["lazybase"]!!.status shouldBe TypeBagInfo.Status.LOADED_ON_REFERENCE
                info["lazybase"]!!.literals shouldBe listOf("pebble")
                info["lazybase"]!!.functions shouldBe listOf("pebbles")
                info["lazytop"]!!.requires shouldBe listOf("lazybase")
                info["tb"]!!.status shouldBe TypeBagInfo.Status.REGISTERED
                info["tb"]!!.status.loaded.shouldBeTrue()
                (info["clash"] == null).shouldBeTrue()
                Created.counts.toMap() shouldBe before
            }
        }

        test("manifests: what discovery reads, checked against the bag") {
            val top = TypeBags.byName("lazytop")!!
            TypeBags.checkManifest(top).shouldBeEmpty()
            val text = TypeBags.manifestOf(top)
            text shouldContain "name=lazytop\n"
            text shouldContain "requires=lazybase\n"
            text shouldContain "type.Cairn=io.kixi.types.Cairn cairn\n"
            text shouldContain "extends.Pebble=stacked\n"
            TypeBags.checkManifest(TypeBags.byName("lazybase")!!).shouldBeEmpty()
            val missing = TypeBags.checkManifest(bag("nomanifest")).joinToString("\n")
            missing shouldContain "nomanifest: no manifest at META-INF/ki/bags/"
            missing shouldContain "name=nomanifest"
        }

        test("lookups from many threads while bags come and go") {
            val failure = AtomicReference<Throwable?>(null)
            val start = CountDownLatch(1)
            val readers = (1..6).map {
                thread {
                    start.await()
                    try {
                        repeat(20_000) {
                            val t = TypeBags.typeFor(Seq("AC"))
                            if (t != null && t !== SeqType) throw AssertionError("wrong type $t")
                            Type.typeOf(Seq("A"))
                            Ki.format(Seq("A"))
                        }
                    } catch (e: Throwable) { failure.compareAndSet(null, e) }
                }
            }
            start.countDown()
            repeat(200) { withSampleBags { TypeBags.typeFor(Seq("A")) shouldBe SeqType } }
            readers.forEach { it.join() }
            failure.get().shouldBeNull()
        }
    }

    context("Ki.Core's types") {

        test("Type.typeOf, forName and of know extension values without making Ext a type name") {
            withSampleBags {
                Type.typeOf(Seq("A")) shouldBe Type.Ext
                Type.typeOf(object : Shape("square") {}) shouldBe Type.Ext
                Type.of(Seq::class.java) shouldBe Type.Ext
                Type.of(PairImpl::class.java) shouldBe Type.Ext
            }
            Type.typeOf(Seq("A")).shouldBeNull()
            Type.typeOf(ExtLiteral(".dna(\"ATG\")")) shouldBe Type.Ext
            Type.of(ExtLiteral::class.java) shouldBe Type.Ext
            Type.typeOf(Other(1)).shouldBeNull()
            Type.forName("Ext").shouldBeNull()
            Type.forName("Seq").shouldBeNull()
            Type.of(String::class.java) shouldBe Type.String
            Type.of(java.lang.Integer::class.java) shouldBe Type.Int
            Type.of(ArrayList::class.java) shouldBe Type.List
            Type.Any.isAssignableFrom(Type.Ext).shouldBeTrue()
            Type.Ext.isNumber().shouldBeFalse()
        }

        test("Ki.format writes a bag value's literal; Ki.parse reads registered literals only") {
            withSampleBags {
                Ki.format(Seq("ACGT")) shouldBe ".seq(\"ACGT\")"
                Ki.format(listOf(Seq("A"), Shape("circle"))) shouldBe "[.seq(\"A\"), .shape(circle)]"
                Ki.parse(".sq(\"GA\")") shouldBe Seq("GA")
                Ki.parse(".SEQ(\"GA\")") shouldBe Seq("GA")                              // literal names ignore case, as .ID( does
                TypeBags.literal("Sq") shouldBe SeqType
                Ki.parse(".shape( circle )") shouldBe Shape("circle")
                shouldThrow<ParseException> { Ki.parse(".seq(\"AX\")") }.message shouldContain "Not a Seq: AX"
                Ki.format(PairImpl(1, 2)) shouldBe PairImpl(1, 2).toString()          // no literal: not a KD value, printed as is
            }
            Ki.format(ExtLiteral(".dna(\"ATG\")")) shouldBe ".dna(\"ATG\")"
            shouldThrow<ParseException> { Ki.parse(".seq(\"A\")") }.message shouldContain "Unknown literal .seq(…)"   // no bag: Ki.parse stays strict
        }

        test("ExtLiteral keeps the literal as written and compares by it") {
            val a = ExtLiteral(".dna( \"ATG\" )")
            a.name shouldBe "dna"
            a.toString() shouldBe ".dna( \"ATG\" )"
            a shouldBe ExtLiteral(".dna( \"ATG\" )")
            setOf(a, ExtLiteral(".dna( \"ATG\" )")).size shouldBe 1
            (a == ExtLiteral(".dna(\"ATG\")")).shouldBeFalse()
            shouldThrow<IllegalArgumentException> { ExtLiteral("dna(1)") }
            shouldThrow<IllegalArgumentException> { ExtLiteral(".(1)") }
        }
    }

    context("bag code calling back") {

        test("a bag's getters may use the registry while it is being registered") {
            val curious = object : TypeBag {
                override val name = "cur"
                override val version = 1
                override val types: List<ValueType<*>> get() {
                    TypeBags.typeNamed("Seq")                // the registry as it is so far, no deadlock
                    Type.typeOf(Other(1))
                    return emptyList()
                }
            }
            withSampleBags {
                TypeBags.register(curious)
                TypeBags.byName("cur") shouldBe curious
                TypeBags.unregister("cur")
            }
        }

        test("a grid of bag values is written without a type header, so it reads back") {
            withSampleBags {
                io.kixi.Grid.of(2, 1, Seq("A")).toString() shouldStartWith ".grid {"
            }
        }
    }

    context("check") {

        test("the sample bags pass; each broken rule is reported") {
            TypeBags.check(SampleBag) shouldBe emptyList()
            withSampleBags(extension = false) {
                TypeBags.check(SampleBag) shouldBe emptyList()        // registered: the names are not rechecked
                TypeBags.check(SampleExtensionBag) shouldBe emptyList()
            }
            TypeBags.check(SampleExtensionBag).single() shouldContain "requires bag 'tb'"

            data class Box(val n: Int)
            val sloppy = object : ValueType<Box> {
                override val name = "Box"
                override val kotlinClass = Box::class
                override val literals = listOf("box")
                override fun parse(literal: String) = Box(0)                                  // loses the number
                override fun format(value: Box) = ".box(${value.n})"
                override fun encode(value: Box) = byteArrayOf(value.n.toByte())
                override fun decode(bytes: ByteArray, version: Int) = Box(bytes[0].toInt())
                override val ordered = true
                override fun sortKey(value: Box) = byteArrayOf((value.n / 2).toByte())       // 2 and 3 share a key
                override val samples = listOf(Box(0), Box(2), Box(3))
            }
            val problems = TypeBags.check(bag("sl", listOf(sloppy))).joinToString("\n")
            problems shouldContain "sl.Box: parse(format(v)) != v (.box(2)) for sample Box(n=2)"
            problems shouldContain "sl.Box: samples Box(n=2) and Box(n=3) are unequal but share a sort key"

            val misnamed = object : ValueType<Box> by sloppy {
                override fun format(value: Box) = ".crate(${value.n})"
                override val samples = listOf(Box(0))
            }
            TypeBags.check(bag("mn", listOf(misnamed))).joinToString() shouldContain "format writes .crate(0), whose name is not one of [box]"
            val bare = object : ValueType<Box> by sloppy { override val samples = emptyList<Box>() }
            TypeBags.check(bag("nb", listOf(bare))).single() shouldContain "nb.Box: no samples, so nothing was checked"
        }
    }

    context("literal helpers") {

        test("inside and stringOrToken read the usual forms") {
            TypeBags.inside(".dna(\"ATG\")") shouldBe "\"ATG\""
            TypeBags.inside(".f(a(b))") shouldBe "a(b)"
            TypeBags.stringOrToken(" \"A\\tB\\\"C\" ") shouldBe "A\tB\"C"
            TypeBags.stringOrToken("@\"C:\\dir\"") shouldBe "C:\\dir"
            TypeBags.stringOrToken("`raw \\n`") shouldBe "raw \\n"
            TypeBags.stringOrToken("  Na ") shouldBe "Na"
            TypeBags.stringOrToken("13C") shouldBe "13C"
            shouldThrow<ParseException> { TypeBags.inside(".dna") }
            shouldThrow<ParseException> { TypeBags.stringOrToken("  ") }
            shouldThrow<ParseException> { TypeBags.stringOrToken("\"open") }
            shouldThrow<ParseException> { TypeBags.stringOrToken("\"a\" \"b\"") }.message shouldContain "after the string"
            shouldThrow<ParseException> { TypeBags.stringOrToken("`a` x") }
            TypeBags.stringOrToken("\"\"\"ATG\"\"\"") shouldBe "ATG"
            shouldThrow<ParseException> { TypeBags.inside(".dna(\"A\") x") }
        }
    }

    context("the sample bag itself") {

        test("Seq encodes in both versions, orders by its key, and its members work") {
            val s = Seq("ACGTTGCA")
            SeqType.decode(SeqType.encode(s), 2) shouldBe s
            SeqType.decode(SeqType.encodeV1(s), 1) shouldBe s
            SeqType.decode(SeqType.encode(Seq("")), 2) shouldBe Seq("")
            SeqType.parse(SeqType.format(s)) shouldBe s
            val keys = listOf("", "A", "AC", "C", "GA", "T").map { SeqType.sortKey(Seq(it)).toList() }
            keys shouldBe keys.sortedWith { a, b -> java.util.Arrays.compareUnsigned(a.toByteArray(), b.toByteArray()) }
            SeqType.members.first { it.name == "slice" }.invoke(s, listOf(2, 5)) shouldBe Seq("GTT")
        }
    }
})

private fun <T> T?.shouldNotBeNull(): T { if (this == null) throw AssertionError("expected a value"); return this }
