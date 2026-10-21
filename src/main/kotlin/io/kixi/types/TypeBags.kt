package io.kixi.types

import io.kixi.Call
import io.kixi.KiException
import io.kixi.NSID
import io.kixi.Type
import io.kixi.text.ParseException
import io.kixi.text.resolveEscapes
import java.util.Collections
import java.util.Locale
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** A bag that cannot be registered: a name taken, a bad name, a missing requirement. */
class TypeBagException(message: String) : KiException(message)

/**
 * The type bags available to KD, KS, KQ and RevDB: those added with [register],
 * and those discovered with `ServiceLoader` (`META-INF/services/io.kixi.types.TypeBag`)
 * from Ki.Core's class loader and the context class loader of the first thread that
 * uses the registry, plus any loader passed to [discover]. Bags live for the life of
 * the process: nothing undiscovers a loader, so a bag belongs on the host's classpath,
 * not in a plugin that is unloaded.
 *
 * Lookups are lock-free from any thread: the registry is an immutable snapshot,
 * replaced as a whole (compare-and-set) when a bag is added or removed. No bag code
 * runs under a lock, so a bag's constructor or getters may use Ki.Core freely, and
 * may call back into the registry (they then see it as it was before their bag).
 */
object TypeBags {

    /** Dot-literal names KD and Ki.Core use themselves, or will (`measure`, `vec`). */
    @JvmField
    val RESERVED_LITERALS: Set<String> = setOf("blob", "geo", "coordinate", "id", "ref", "grid", "snip", "schema", "measure", "vec")

    /** Type names Ki.Core, KD and KS use themselves. */
    @JvmField
    val RESERVED_TYPE_NAMES: Set<String> = Type.entries.map { it.name }.toSet() +
            setOf("Call", "Tag", "Annotation", "NSID", "Currency", "Regex", "MatchResult", "Measure", "ExtLiteral",
                "Function", "Lambda", "Type", "Nil", "Object", "Unknown")

    private val BAG_NAME = Regex("[a-z][a-z0-9]*")
    private val TYPE_NAME = Regex("[A-Z][A-Za-z0-9]*")
    private val LITERAL_NAME = Regex("[a-z][A-Za-z]*")
    private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

    private val ref = AtomicReference(State(emptyList()))

    @Volatile
    private var defaultsDiscovered = false

    /** True on a thread running the default discovery, so lookups from bag code there do not start it again. */
    private val discoveringHere = ThreadLocal<Boolean>()

    private val problemSet: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet())
    private val discoveredLoaders: MutableSet<ClassLoader> = ConcurrentHashMap.newKeySet()

    // ------------------------------------------------------------------ registration

    /**
     * Adds [bag]. Registering the same bag object again does nothing. Throws
     * [TypeBagException] when a name is taken (the message names both bags), a name is
     * malformed or reserved, a class overlaps another bag's, or a required bag is
     * missing.
     */
    @JvmStatic
    fun register(bag: TypeBag) {
        current()
        while (true) {
            val s = ref.get()
            if (s.entries.any { it.bag === bag }) return
            val next = State(s.entries + Entry(bag, explicit = true))
            if (ref.compareAndSet(s, next)) return
        }
    }

    /**
     * Removes a bag added with [register]. Discovered bags stay. Throws
     * [TypeBagException] when another registered bag requires it.
     */
    @JvmStatic
    fun unregister(name: String) {
        current()
        while (true) {
            val s = ref.get()
            val entry = s.entries.firstOrNull { it.bag.name == name && it.explicit } ?: return
            s.entries.firstOrNull { name in it.requires }?.let {
                throw TypeBagException("Cannot remove bag '$name': bag '${it.name}' requires it")
            }
            if (ref.compareAndSet(s, State(s.entries - entry))) return
        }
    }

    /**
     * Registers the bags [loader] provides through `ServiceLoader`, in `requires`
     * order. A bag that cannot be loaded or registered is skipped and described in
     * [problems], so one broken library does not hide the others. A loader is
     * discovered once.
     */
    @JvmStatic
    fun discover(loader: ClassLoader) {
        current()
        discoverFrom(loader)
    }

    /** Every registered bag, in registration order (a bag after the bags it requires). */
    @JvmStatic
    val all: List<TypeBag> get() = current().bags

    /** The discovered bags that were skipped, and why. Hosts show these. */
    @JvmStatic
    val problems: List<String> get() { current(); return synchronized(problemSet) { problemSet.toList() } }

    // ------------------------------------------------------------------ lookups

    @JvmStatic
    fun byName(name: String): TypeBag? = current().byName[name]

    /** The bag [type] belongs to. Throws [IllegalArgumentException] for a type no registered bag has. */
    @JvmStatic
    fun bagOf(type: ValueType<*>): TypeBag =
        current().owner[type.name] ?: throw IllegalArgumentException("${type.name} is not a type of a registered bag")

    /**
     * The type of [value]: the type registered for its class, else for its nearest
     * superclass, else for an interface it implements. Null for anything else,
     * including Ki.Core's own types. Pass the result only values of that type.
     */
    @JvmStatic
    fun typeFor(value: Any): ValueType<Any>? = typeForClass(value.javaClass)

    /** As [typeFor], for a class. */
    @JvmStatic
    fun typeForClass(c: Class<*>): ValueType<Any>? {
        val s = current()
        if (s.classes.isEmpty()) return null
        val slot = classCache.get(c)
        val cached = slot.get()
        if (cached != null && cached.state === s) return cached.type
        val found = s.find(c)
        slot.set(Cached(s, found))
        return found
    }

    /** The type whose literal is `.name(…)` (case is ignored), or null. */
    @JvmStatic
    fun literal(name: String): ValueType<Any>? = current().literals[name.lowercase(Locale.ROOT)]

    /** The type called [name] (`"DNA"`), or null. */
    @JvmStatic
    fun typeNamed(name: String): ValueType<Any>? = current().types[name]

    /** True when [name] has the form of a bag type's name (`[A-Z][A-Za-z0-9]*`) and is not reserved. */
    @JvmStatic
    fun isTypeName(name: String): Boolean = TYPE_NAME.matches(name) && name !in RESERVED_TYPE_NAMES

    /** The member [name] of [value]'s type: its own, then the extensions other bags add. Null when there is none. */
    @JvmStatic
    fun member(value: Any, name: String): Member<Any>? {
        val t = typeFor(value) ?: return null
        return current().members[t.name]?.get(name)
    }

    /** Every member of [type] (its own, then extensions), by name, in order. */
    @JvmStatic
    fun members(type: ValueType<*>): Map<String, Member<Any>> = current().members[type.name] ?: emptyMap()

    /** A registered bag's function called [name], or null. */
    @JvmStatic
    fun function(name: String): BagFunction? = current().functions[name]

    /** A registered bag's constant called [name], or null. */
    @JvmStatic
    fun constant(name: String): Any? = current().constants[name]

    /** The literal of a registered value, or null when [value] is not one or its type has no literal. */
    @JvmStatic
    fun format(value: Any): String? {
        val t = typeFor(value) ?: return null
        if (t.literals.isEmpty()) return null
        return t.format(value)
    }

    /**
     * What is wrong with [bag], or nothing: whether it could be registered now
     * (names, conflicts, requirements; skipped when it is registered already), and,
     * over every type's [ValueType.samples] (a type with a literal and no samples is
     * a problem), that `parse(format(v)) == v` with the same hash, that `format`
     * writes one of the type's literal names, that `decode(encode(v), version) == v`,
     * and that sort keys are equal exactly when the values are. A bag's own tests
     * should call it; RevDB's storage test does.
     */
    @JvmStatic
    fun check(bag: TypeBag): List<String> {
        val out = ArrayList<String>()
        val s = current()
        if (s.entries.none { it.bag === bag }) {
            try { State(s.entries + Entry(bag, explicit = true)) } catch (e: TypeBagException) { out.add(e.message) }
        }
        for (t0 in bag.types) {
            @Suppress("UNCHECKED_CAST")
            val t = t0 as ValueType<Any>
            fun fail(what: String, v: Any?, e: Throwable? = null) {
                out.add("${bag.name}.${t.name}: $what for sample $v" + (e?.let { ": $it" } ?: ""))
            }
            val samples = t.samples
            if (samples.isEmpty() && t.literals.isNotEmpty()) out.add("${bag.name}.${t.name}: no samples, so nothing was checked")
            for (v in samples) {
                if (!t.kotlinClass.isInstance(v)) { fail("not an instance of ${t.kotlinClass.java.name}", v); continue }
                if (t.literals.isNotEmpty()) {
                    try {
                        val text = t.format(v)
                        val name = text.drop(1).takeWhile { it.isLetter() }
                        if (!text.startsWith(".") || t.literals.none { it.equals(name, ignoreCase = true) }) {
                            fail("format writes $text, whose name is not one of ${t.literals}", v)
                        }
                        val back = t.parse(text)
                        if (back != v) fail("parse(format(v)) != v ($text)", v)
                        else if (back.hashCode() != v.hashCode()) fail("parse(format(v)) is equal but hashes differently ($text)", v)
                    } catch (e: Exception) { fail("parse(format(v)) failed", v, e) }
                }
                try { if (t.decode(t.encode(v), bag.version) != v) fail("decode(encode(v)) != v", v) }
                catch (e: Exception) { fail("decode(encode(v)) failed", v, e) }
            }
            if (!t.ordered) continue
            val keys = try { samples.map { t.sortKey(it) } } catch (e: Exception) { out.add("${bag.name}.${t.name}: sortKey failed: $e"); continue }
            for (i in samples.indices) for (j in samples.indices) {
                if ((samples[i] == samples[j]) != keys[i].contentEquals(keys[j])) {
                    out.add("${bag.name}.${t.name}: samples ${samples[i]} and ${samples[j]} are " +
                            (if (samples[i] == samples[j]) "equal but their sort keys differ" else "unequal but share a sort key"))
                }
            }
        }
        return out
    }

    // ------------------------------------------------------------------ literal helpers

    /** The text between a literal's first `(` and its closing `)`: `"ATG"` for `.dna("ATG")`. */
    @JvmStatic
    fun inside(literal: String): String {
        val t = literal.trimEnd()
        val open = t.indexOf('(')
        if (open < 0 || !t.endsWith(")")) throw ParseException("Not a .name(…) literal: $literal")
        return t.substring(open + 1, t.length - 1)
    }

    /**
     * The inside of a literal read as one KD string (`"…"` with escapes, `"""…"""`,
     * `@"…"` or `` `…` `` raw) or, when it is not quoted, as a bare token, trimmed.
     * Throws [ParseException] for an empty inside, an unterminated string or text
     * after it.
     */
    @JvmStatic
    fun stringOrToken(inside: String): String {
        val t = inside.trim()
        if (t.isEmpty()) throw ParseException("Empty literal")
        return when {
            t.startsWith("\"\"\"") -> {
                val end = t.indexOf("\"\"\"", 3)
                if (end < 0) throw ParseException("Unterminated string: $t")
                if (end != t.length - 3) throw ParseException("Unexpected text after the string: $t")
                t.substring(3, end)
            }
            t[0] == '"' -> {
                var i = 1
                while (i < t.length && t[i] != '"') i += if (t[i] == '\\') 2 else 1
                if (i >= t.length) throw ParseException("Unterminated string: $t")
                if (i != t.length - 1) throw ParseException("Unexpected text after the string: $t")
                t.substring(1, i).resolveEscapes('"')
            }
            t.startsWith("@\"") -> raw(t, 2, '"')
            t[0] == '`' -> raw(t, 1, '`')
            else -> t
        }
    }

    private fun raw(t: String, from: Int, quote: Char): String {
        val end = t.indexOf(quote, from)
        if (end < 0) throw ParseException("Unterminated string: $t")
        if (end != t.length - 1) throw ParseException("Unexpected text after the string: $t")
        return t.substring(from, end)
    }

    // ------------------------------------------------------------------ discovery

    private fun current(): State {
        if (!defaultsDiscovered && discoveringHere.get() != true) discoverDefaults()
        return ref.get()
    }

    /**
     * Discovers Ki.Core's and the calling thread's context class loader. Nothing is
     * held while bag code runs: two threads arriving together may both discover (the
     * second finds every bag registered already), and neither waits on the other.
     */
    private fun discoverDefaults() {
        discoveringHere.set(true)
        try {
            val loaders = listOfNotNull(TypeBag::class.java.classLoader, Thread.currentThread().contextClassLoader).distinct()
            for (l in loaders) discoverFrom(l, again = true)
        } finally {
            discoveringHere.remove()
            defaultsDiscovered = true
        }
    }

    private fun problem(text: String) { problemSet.add(text) }

    private fun discoverFrom(loader: ClassLoader, again: Boolean = false) {
        if (!discoveredLoaders.add(loader) && !again) return
        val found = ArrayList<TypeBag>()
        val it = ServiceLoader.load(TypeBag::class.java, loader).iterator()
        var failures = 0
        while (failures < 100) {
            try {
                if (!it.hasNext()) break
                found.add(it.next())
            } catch (e: ServiceConfigurationError) {
                failures++
                problem("A type bag could not be loaded: ${e.message}")
            } catch (e: Exception) {
                failures++
                problem("A type bag could not be created: $e")
            } catch (e: LinkageError) {
                failures++
                problem("A type bag could not be created: $e")
            }
        }
        // Register in requires order: repeat passes until nothing more can be added.
        var pending: List<TypeBag> = found
        while (pending.isNotEmpty()) {
            val next = ArrayList<TypeBag>()
            var progress = false
            for (bag in pending) {
                when (tryRegisterDiscovered(bag)) {
                    Outcome.WAITING -> next.add(bag)
                    Outcome.DONE -> progress = true
                }
            }
            if (!progress) {
                for (bag in next) {
                    val missing = try { bag.requires.filter { it !in ref.get().byName } } catch (e: RuntimeException) { emptyList() }
                    problem("Type bag '${safeName(bag)}' (${bag.javaClass.name}) was skipped: it requires " +
                            missing.joinToString { "'$it'" } + ", which is not available")
                }
                break
            }
            pending = next
        }
    }

    private enum class Outcome { DONE, WAITING }

    /** Registers one discovered bag, or says it must wait for a bag it requires. Every failure is a problem, not an exception. */
    private fun tryRegisterDiscovered(bag: TypeBag): Outcome {
        val cls = bag.javaClass.name
        try {
            val name = bag.name
            val requires = bag.requires
            while (true) {
                val s = ref.get()
                val same = s.byName[name]
                if (same != null) {
                    if (same.javaClass.name != cls) {
                        problem("Type bag '$name' ($cls) was skipped: a bag of that name is registered (${same.javaClass.name})")
                    }
                    return Outcome.DONE
                }
                if (requires.any { it !in s.byName }) return Outcome.WAITING
                val next = State(s.entries + Entry(bag, explicit = false))
                if (ref.compareAndSet(s, next)) return Outcome.DONE
            }
        } catch (e: TypeBagException) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: ${e.message}")
        } catch (e: RuntimeException) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: $e")
        } catch (e: LinkageError) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: $e")
        }
        return Outcome.DONE
    }

    private fun safeName(bag: TypeBag): String = try { bag.name } catch (e: RuntimeException) { "?" }

    // ------------------------------------------------------------------ the class cache

    /** What [typeForClass] found for a class, for one snapshot. Held per class, so it goes when the class does. */
    private class Cached(val state: State, val type: ValueType<Any>?)

    private val classCache = object : ClassValue<AtomicReference<Cached?>>() {
        override fun computeValue(type: Class<*>): AtomicReference<Cached?> = AtomicReference(null)
    }

    // ------------------------------------------------------------------ the snapshot

    /** A registered bag, with what the snapshot read from it once. */
    private class Entry(val bag: TypeBag, val explicit: Boolean) {
        val name: String = bag.name
        val requires: List<String> = bag.requires.toList()
    }

    /** One immutable registry; the constructor checks every rule and throws [TypeBagException]. */
    private class State(val entries: List<Entry>) {
        val bags: List<TypeBag> = entries.map { it.bag }
        val byName = LinkedHashMap<String, TypeBag>()
        val types = HashMap<String, ValueType<Any>>()
        val literals = HashMap<String, ValueType<Any>>()
        val classes = LinkedHashMap<Class<*>, ValueType<Any>>()
        /** The bag of each type, by type name (unique), so a bag that builds its types anew each time still works. */
        val owner = HashMap<String, TypeBag>()
        val functions = HashMap<String, BagFunction>()
        val constants = HashMap<String, Any>()
        val members = HashMap<String, Map<String, Member<Any>>>()

        /** Which bag claimed a function or constant name. */
        private val valueNames = HashMap<String, String>()

        init {
            for (e in entries) add(e)
            for (e in entries) addExtensions(e)
        }

        private fun fail(message: String): Nothing = throw TypeBagException(message)

        @Suppress("UNCHECKED_CAST")
        private fun add(e: Entry) {
            val bag = e.bag
            val b = e.name
            if (!BAG_NAME.matches(b)) fail("Bag name '$b' is not a lowercase identifier ([a-z][a-z0-9]*)")
            byName[b]?.let { fail("Bag name '$b' is taken (${it.javaClass.name} and ${bag.javaClass.name})") }
            if (bag.version < 1) fail("Bag '$b': version must be at least 1, not ${bag.version}")
            for (r in e.requires) if (r !in byName) fail("Bag '$b' requires bag '$r', which is not registered")
            byName[b] = bag

            for (t0 in bag.types) {
                val t = t0 as ValueType<Any>
                val n = t.name
                if (!TYPE_NAME.matches(n)) fail("Bag '$b': type name '$n' is not a capitalized identifier ([A-Z][A-Za-z0-9]*)")
                if (n in RESERVED_TYPE_NAMES) fail("Bag '$b': type name '$n' is reserved")
                types[n]?.let { fail("Type name '$n' is taken (bags '${owner[it.name]!!.name}' and '$b')") }
                val c = t.kotlinClass.java
                coreClassProblem(c)?.let { fail("Bag '$b': type $n's class ${c.name} $it") }
                classes[c]?.let { fail("Class ${c.name} has two types (${owner[it.name]!!.name}.${it.name} and $b.$n)") }
                // Another bag's class may not be a supertype or subtype of this one: a value would belong to both.
                for ((k, other) in classes) {
                    val o = owner[other.name]!!
                    if (o !== bag && (k.isAssignableFrom(c) || c.isAssignableFrom(k))) {
                        fail("Bag '$b': type $n's class ${c.name} overlaps ${o.name}.${other.name} (${k.name}); add members to another bag's type with extensions instead")
                    }
                }
                val seen = HashSet<String>()
                for (l in t.literals) {
                    if (!LITERAL_NAME.matches(l)) fail("Bag '$b': literal name '$l' of $n is not letters starting with a lowercase letter")
                    val key = l.lowercase(Locale.ROOT)
                    if (key in RESERVED_LITERALS) fail("Bag '$b': literal name '$l' of $n is reserved")
                    if (!seen.add(key)) fail("Bag '$b': $n lists literal '$l' twice")
                    literals[key]?.let { fail("Literal name '$l' is taken (${owner[it.name]!!.name}.${it.name} and $b.$n)") }
                }
                val ms = LinkedHashMap<String, Member<Any>>()
                for (m in t.members) {
                    if (!IDENTIFIER.matches(m.name)) fail("Bag '$b': member name '${m.name}' of $n is not an identifier")
                    checkArity(m.arity, "$b: member ${m.name} of $n")
                    if (ms.put(m.name, m as Member<Any>) != null) fail("Bag '$b': $n has two members called '${m.name}'")
                }
                types[n] = t
                for (l in t.literals) literals[l.lowercase(Locale.ROOT)] = t
                classes[c] = t
                owner[n] = bag
                members[n] = Collections.unmodifiableMap(ms)
            }

            for (f in bag.functions) {
                claim(f.name, b)
                checkArity(f.arity, "$b: function ${f.name}")
                functions[f.name] = f
            }
            for ((k, v) in bag.constants) {
                claim(k, b)
                constants[k] = v
            }
        }

        private fun claim(name: String, bag: String) {
            if (!IDENTIFIER.matches(name)) fail("Bag '$bag': '$name' is not an identifier")
            valueNames[name]?.let { fail(if (it == bag) "Bag '$bag' has two functions or constants called '$name'" else "The name '$name' is taken (bags '$it' and '$bag')") }
            valueNames[name] = bag
        }

        private fun checkArity(r: IntRange?, what: String) {
            if (r != null && (r.first < 0 || r.isEmpty())) fail("$what: arity $r is not a range of argument counts")
        }

        @Suppress("UNCHECKED_CAST")
        private fun addExtensions(e: Entry) {
            val bag = e.bag
            val reach = reachable(e)
            for ((typeName, list) in bag.extensions) {
                val t = types[typeName] ?: fail("Bag '${e.name}' extends type '$typeName', which no registered bag has")
                val o = owner[typeName]!!
                if (o.name !in reach) fail("Bag '${e.name}' extends ${o.name}.$typeName but does not require bag '${o.name}'")
                val merged = LinkedHashMap(members[t.name]!!)
                for (m in list) {
                    if (!IDENTIFIER.matches(m.name)) fail("Bag '${e.name}': member name '${m.name}' is not an identifier")
                    checkArity(m.arity, "${e.name}: member ${m.name} of $typeName")
                    if (merged.put(m.name, m as Member<Any>) != null) fail("Bag '${e.name}' adds member '${m.name}' to $typeName, which already has one")
                }
                members[t.name] = Collections.unmodifiableMap(merged)
            }
        }

        /** The names of the bags [e] requires, directly or not. */
        private fun reachable(e: Entry): Set<String> {
            val requiresOf = entries.associate { it.name to it.requires }
            val out = HashSet<String>()
            val stack = ArrayDeque(e.requires)
            while (stack.isNotEmpty()) {
                val n = stack.removeLast()
                if (out.add(n)) requiresOf[n]?.let { stack.addAll(it) }
            }
            return out
        }

        fun find(c: Class<*>): ValueType<Any>? {
            var k: Class<*>? = c
            while (k != null) { classes[k]?.let { return it }; k = k.superclass }
            val queue = ArrayDeque<Class<*>>()
            k = c
            while (k != null) { queue.addAll(k.interfaces); k = k.superclass }
            val seen = HashSet<Class<*>>()
            while (queue.isNotEmpty()) {
                val i = queue.removeFirst()
                if (!seen.add(i)) continue
                classes[i]?.let { return it }
                queue.addAll(i.interfaces)
            }
            return null
        }
    }

    /** The classes Ki.Core already gives a type (or that KS and Ki.format treat first), by name. */
    private val CORE_CLASSES: List<Pair<String, Class<*>>> =
        Type.entries.filter { it != Type.Any && it != Type.nil && it != Type.Ext }
            .flatMap { listOf(it.name to it.kclass.java, it.name to it.kclass.javaObjectType) }.distinct() +
                listOf("Call" to Call::class.java, "NSID" to NSID::class.java, "ExtLiteral" to ExtLiteral::class.java,
                    "Collection" to java.util.Collection::class.java, "Throwable" to Throwable::class.java)

    /**
     * Why [c] cannot be a bag type's class: it is (or extends) a class Ki.Core already
     * gives a type, or one of those classes is a [c] (`CharSequence`, `Comparable`), so
     * that `typeFor` would claim core values. Every layer tests the core classes first,
     * so such a type could never be told apart.
     */
    private fun coreClassProblem(c: Class<*>): String? {
        if (c == Any::class.java) return "is Any"
        for ((name, k) in CORE_CLASSES) {
            if (k.isAssignableFrom(c)) return "is a Ki.Core $name"
            if (c.isAssignableFrom(k)) return "is a supertype of Ki.Core's $name (${k.name})"
        }
        return null
    }
}
