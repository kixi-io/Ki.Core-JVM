package io.kixi.types

import io.kixi.Call
import io.kixi.KiException
import io.kixi.NSID
import io.kixi.Type
import io.kixi.text.ParseException
import io.kixi.text.resolveEscapes
import java.util.Collections
import java.util.Locale
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** A bag that cannot be registered: a name taken, a bad name, a missing requirement. */
class TypeBagException(message: String) : KiException(message)

/**
 * The type bags available to KD, KS, KQ and RevDB: those added with [register],
 * and those discovered from `META-INF/services/io.kixi.types.TypeBag` in Ki.Core's
 * class loader and the context class loader of the first thread that uses the
 * registry, plus any loader passed to [discover].
 *
 * **Loading on reference** (type bags design, 13). Discovery reads each bag's
 * manifest (`META-INF/ki/bags/<bag class>.properties`: its name, the bags it
 * requires, its type, literal, function, constant and member names) and creates no
 * bag. A discovered bag is created and registered (activated) the first time
 * something refers to it: its literal (`.dna(…)`), a type name (`DNA`), a value of
 * one of its classes, a function or constant, its name (`byName`, `use ki.bio`, a
 * stored value), or a member it adds to another bag's type. The bags it requires are
 * activated first; nothing else is. A manifest that says `preload=true` (Ki's own
 * foundational bags) is activated at discovery. [all] is the active bags;
 * [available] names every bag, active or not. A bag stays active for the life of the
 * process: nothing undiscovers a loader, so a bag belongs on the host's classpath,
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

    /** Discovered bags not active yet, by every name that activates one. */
    private val catalog = AtomicReference(Catalog(emptyMap()))

    /** Discovered bags activated at discovery because their manifests say preload=true. */
    private val preloaded: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Discovered bags whose activation failed (reported in [problems]); they are not tried again. */
    private val failed: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** The bags this thread is activating, so a bag's own code asking for it sees the registry as it was before. */
    private val activating = ThreadLocal<MutableSet<String>>()

    /** Where a discovered bag's manifest is, for its class. */
    const val MANIFEST_DIR: String = "META-INF/ki/bags/"

    private const val SERVICES = "META-INF/services/io.kixi.types.TypeBag"

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
        val pending = catalog.get().pending[bag.name]
        if (pending != null && pending.className == bag.javaClass.name) {
            removePending(bag.name)      // the discovered bag, created by the caller: it is active from now on
        } else if (pending != null) {
            throw TypeBagException("Bag name '${bag.name}' is taken (${pending.className}, discovered, and ${bag.javaClass.name})")
        } else {
            catalog.get().conflict(Manifest.of(bag, bag.javaClass.classLoader ?: TypeBag::class.java.classLoader, preload = false))?.let { throw TypeBagException(it) }
        }
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
     * Reads the manifests of the bags [loader] lists in
     * `META-INF/services/io.kixi.types.TypeBag` and activates those that say
     * `preload=true` (with the bags they require); the others are activated when
     * first referred to. A bag with no manifest, a broken manifest, or a name another
     * bag has is skipped and described in [problems], so one broken library does not
     * hide the others. A loader is discovered once.
     */
    @JvmStatic
    fun discover(loader: ClassLoader) {
        current()
        discoverFrom(loader)
    }

    /** Every active bag, in registration order (a bag after the bags it requires). Discovered bags not referred to yet are not here; see [available]. */
    @JvmStatic
    val all: List<TypeBag> get() = current().bags

    /** The names of every bag: the active ones, then those discovered and not active yet. Creates nothing. */
    @JvmStatic
    val available: List<String> get() = current().byName.keys.toList() + catalog.get().pending.keys.sorted()

    /** True when the bag called [name] is active (registered, or discovered and referred to). */
    @JvmStatic
    fun isActive(name: String): Boolean = current().byName.containsKey(name)

    /**
     * The bag called [name], activating it (and the bags it requires) when it was
     * discovered and is not active yet; null when there is no such bag or it could not
     * be activated (the reason is in [problems]).
     */
    @JvmStatic
    fun activate(name: String): TypeBag? = byName(name)

    /** Activates every discovered bag (KS's `use ki.*` asks for them all). */
    @JvmStatic
    fun activateAll() {
        current()
        for (m in catalog.get().pending.values.sortedBy { it.name }) activate(m)
    }

    /**
     * True when a discovered bag that is not active yet has a member called [name] on
     * one of its types or as an extension, so a value it reads may have it. Creates
     * nothing (KQ's checker asks this for a target whose type is not known).
     */
    @JvmStatic
    fun hasInactiveMember(name: String): Boolean = name in catalog.get().memberNames

    /**
     * Every bag, active or not, with how it was loaded and what it offers, for a host
     * to show (RevNexus's RevDB Overview): the active bags in registration order, then
     * those not loaded yet by name. Creates nothing; [problems] lists the bags that
     * could not be loaded.
     */
    @JvmStatic
    fun describe(): List<TypeBagInfo> {
        val s = current()
        val out = ArrayList<TypeBagInfo>()
        for (e in s.entries) {
            val b = e.bag
            val status = when {
                e.explicit -> TypeBagInfo.Status.REGISTERED
                e.name in preloaded -> TypeBagInfo.Status.PRELOADED
                else -> TypeBagInfo.Status.LOADED_ON_REFERENCE
            }
            out.add(TypeBagInfo(e.name, status, e.requires, b.types.map { it.name }, b.types.flatMap { it.literals },
                b.functions.map { it.name }, b.constants.keys.toList()))
        }
        for (m in catalog.get().pending.values.sortedBy { it.name }) {
            out.add(TypeBagInfo(m.name, TypeBagInfo.Status.AVAILABLE, m.requires, m.types.keys.sorted(), m.literals.keys.sorted(),
                m.functions.sorted(), m.constants.sorted()))
        }
        return out
    }

    /** The names of every bag's functions and constants, active or not. Creates nothing. */
    @JvmStatic
    val valueNames: Set<String> get() = current().functions.keys + current().constants.keys + catalog.get().byValueName.keys

    /** The discovered bags that were skipped, and why. Hosts show these. */
    @JvmStatic
    val problems: List<String> get() { current(); return synchronized(problemSet) { problemSet.toList() } }

    // ------------------------------------------------------------------ lookups

    @JvmStatic
    fun byName(name: String): TypeBag? {
        current().byName[name]?.let { return it }
        val m = catalog.get().pending[name] ?: return null
        return activate(m)
    }

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
        val cat = catalog.get()
        if (s.classes.isEmpty() && cat.byClass.isEmpty()) return null
        val slot = classCache.get(c)
        val cached = slot.get()
        if (cached != null && cached.state === s && cached.catalog === cat) return cached.type
        val found = s.find(c)
        if (found == null && cat.byClass.isNotEmpty()) {
            // A value of a class a discovered bag declares: the bag is referred to.
            val m = cat.find(c)
            if (m != null && activate(m) != null) return current().find(c)
        }
        slot.set(Cached(s, cat, found))
        return found
    }

    /** The type whose literal is `.name(…)` (case is ignored), or null. */
    @JvmStatic
    fun literal(name: String): ValueType<Any>? {
        val key = name.lowercase(Locale.ROOT)
        current().literals[key]?.let { return it }
        val m = catalog.get().byLiteral[key] ?: return null
        activate(m)
        return current().literals[key]
    }

    /** The type called [name] (`"DNA"`), or null. */
    @JvmStatic
    fun typeNamed(name: String): ValueType<Any>? {
        current().types[name]?.let { return it }
        val m = catalog.get().byType[name] ?: return null
        activate(m)
        return current().types[name]
    }

    /** True when [name] has the form of a bag type's name (`[A-Z][A-Za-z0-9]*`) and is not reserved. */
    @JvmStatic
    fun isTypeName(name: String): Boolean = TYPE_NAME.matches(name) && name !in RESERVED_TYPE_NAMES

    /** The member [name] of [value]'s type: its own, then the extensions other bags add. Null when there is none. */
    @JvmStatic
    fun member(value: Any, name: String): Member<Any>? {
        val t = typeFor(value) ?: return null
        return member(t, name)
    }

    /**
     * The member [name] of [type]: its own, then the extensions of active bags, then
     * the extension a discovered bag adds under that name, which activates that bag.
     */
    @JvmStatic
    fun member(type: ValueType<*>, name: String): Member<Any>? {
        current().members[type.name]?.get(name)?.let { return it }
        val m = catalog.get().byExtension["${type.name}.$name"] ?: return null
        activate(m)
        return current().members[type.name]?.get(name)
    }

    /**
     * Every member of [type] (its own, then the extensions of active bags), by name, in
     * order. An extension of a bag not active yet is not listed; [member] finds it by name.
     */
    @JvmStatic
    fun members(type: ValueType<*>): Map<String, Member<Any>> = current().members[type.name] ?: emptyMap()

    /** A bag's function called [name] (activating a discovered bag that has it), or null. */
    @JvmStatic
    fun function(name: String): BagFunction? {
        current().functions[name]?.let { return it }
        val m = catalog.get().byValueName[name] ?: return null
        activate(m)
        return current().functions[name]
    }

    /** A bag's constant called [name] (activating a discovered bag that has it), or null. */
    @JvmStatic
    fun constant(name: String): Any? {
        current().constants[name]?.let { return it }
        val m = catalog.get().byValueName[name] ?: return null
        activate(m)
        return current().constants[name]
    }

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

    /**
     * Reads the manifests of the bags [loader] lists, adds them to the catalog (a name
     * another bag has is a problem, and that bag is skipped), and activates those that
     * say `preload=true`. No bag is created for any other.
     */
    private fun discoverFrom(loader: ClassLoader, again: Boolean = false) {
        if (!discoveredLoaders.add(loader) && !again) return
        val found = ArrayList<Manifest>()
        for (cls in serviceClassNames(loader)) {
            val url = loader.getResource("$MANIFEST_DIR$cls.properties")
            if (url == null) {
                problem("Type bag class $cls has no manifest ($MANIFEST_DIR$cls.properties), so it is not loaded; TypeBags.manifestOf(bag) writes one")
                continue
            }
            try {
                val props = Properties()
                url.openStream().reader(Charsets.UTF_8).use { props.load(it) }
                found.add(Manifest.read(cls, loader, props))
            } catch (e: Exception) {
                problem("Type bag class $cls was skipped: its manifest is not readable: ${e.message ?: e}")
            }
        }
        for (m in found) addToCatalog(m)
        for (m in found) if (m.preload && activate(m) != null) preloaded.add(m.name)
    }

    /** The bag classes the service files of [loader] list (`#` comments and blank lines skipped), without loading any. */
    private fun serviceClassNames(loader: ClassLoader): List<String> {
        val out = LinkedHashSet<String>()
        try {
            for (url in loader.getResources(SERVICES).toList()) {
                url.openStream().reader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) line.substringBefore('#').trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                }
            }
        } catch (e: java.io.IOException) {
            problem("The type bag service files could not be read: ${e.message}")
        }
        return out.toList()
    }

    /** Adds [m] to the catalog unless a bag of its name is active or available already, or one of its names is taken. */
    private fun addToCatalog(m: Manifest) {
        while (true) {
            val s = current()
            val cat = catalog.get()
            val active = s.byName[m.name]
            val pending = cat.pending[m.name]
            val same = active?.javaClass?.name ?: pending?.className
            if (same != null) {
                if (same != m.className) problem("Type bag '${m.name}' (${m.className}) was skipped: a bag of that name is registered ($same)")
                return
            }
            if (m.name in failed) return
            val conflict = conflictWithActive(m, s) ?: cat.conflict(m)
            if (conflict != null) {
                problem("Type bag '${m.name}' (${m.className}) was skipped: $conflict")
                return
            }
            if (catalog.compareAndSet(cat, Catalog(cat.pending + (m.name to m)))) return
        }
    }

    /** Why [m] cannot join the active bags of [s], or null. */
    private fun conflictWithActive(m: Manifest, s: State): String? {
        for ((t, _) in m.types) s.types[t]?.let { return "Type name '$t' is taken (bags '${s.owner[it.name]!!.name}' and '${m.name}')" }
        for ((t, c) in m.types) s.classes.entries.firstOrNull { it.key.name == c }?.let { return "Class $c has two types (${s.owner[it.value.name]!!.name}.${it.value.name} and ${m.name}.$t)" }
        for ((l, t) in m.literals) s.literals[l]?.let { return "Literal name '$l' is taken (${s.owner[it.name]!!.name}.${it.name} and ${m.name}.$t)" }
        for (v in m.functions + m.constants) {
            val other = s.functions[v]?.let { f -> s.bags.first { b -> b.functions.any { it === f } }.name }
                ?: if (v in s.constants) s.bags.first { v in it.constants }.name else null
            if (other != null) return "The name '$v' is taken (bags '$other' and '${m.name}')"
        }
        return null
    }

    private fun removePending(name: String) {
        while (true) {
            val cat = catalog.get()
            if (name !in cat.pending) return
            if (catalog.compareAndSet(cat, Catalog(cat.pending - name))) return
        }
    }

    /**
     * Creates and registers the discovered bag [m] describes, after the bags it
     * requires; null when that fails (the reason goes to [problems], and the bag is
     * not tried again). Nothing is held while bag code runs: two threads may both
     * create the bag, and the second finds it registered.
     */
    private fun activate(m: Manifest): TypeBag? {
        current().byName[m.name]?.let { return it }
        if (m.name in failed) return null
        val busy = activating.get() ?: HashSet<String>().also { activating.set(it) }
        if (!busy.add(m.name)) return null
        try {
            for (r in m.requires) {
                if (current().byName[r] != null) continue
                val rm = catalog.get().pending[r]
                if (rm == null || activate(rm) == null) return fail(m, "it requires '$r', which is not available")
            }
            val bag = try {
                Class.forName(m.className, true, m.loader).getDeclaredConstructor().newInstance() as TypeBag
            } catch (e: Exception) {
                return fail(m, (if (e is java.lang.reflect.InvocationTargetException) e.targetException ?: e else e).toString())
            } catch (e: LinkageError) {
                return fail(m, e.toString())
            }
            val differences = try {
                m.differences(Manifest.of(bag, m.loader, m.preload))
            } catch (e: RuntimeException) {
                return fail(m, e.toString())
            }
            if (differences.isNotEmpty()) return fail(m, "its manifest does not match the bag (${differences.joinToString("; ")})")
            removePending(m.name)
            tryRegisterDiscovered(bag)
            return current().byName[m.name] ?: run { failed.add(m.name); null }
        } finally {
            busy.remove(m.name)
            if (busy.isEmpty()) activating.remove()
        }
    }

    private fun fail(m: Manifest, why: String): TypeBag? {
        problem("Type bag '${m.name}' (${m.className}) was skipped: $why")
        failed.add(m.name)
        removePending(m.name)
        return null
    }

    /** Registers one discovered bag after its manifest was checked. Every failure is a problem, not an exception. */
    private fun tryRegisterDiscovered(bag: TypeBag) {
        val cls = bag.javaClass.name
        try {
            val name = bag.name
            while (true) {
                val s = ref.get()
                val same = s.byName[name]
                if (same != null) {
                    if (same.javaClass.name != cls) {
                        problem("Type bag '$name' ($cls) was skipped: a bag of that name is registered (${same.javaClass.name})")
                    }
                    return
                }
                val next = State(s.entries + Entry(bag, explicit = false))
                if (ref.compareAndSet(s, next)) return
            }
        } catch (e: TypeBagException) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: ${e.message}")
        } catch (e: RuntimeException) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: $e")
        } catch (e: LinkageError) {
            problem("Type bag '${safeName(bag)}' ($cls) was skipped: $e")
        }
    }

    private fun safeName(bag: TypeBag): String = try { bag.name } catch (e: RuntimeException) { "?" }

    // ------------------------------------------------------------------ manifests

    /**
     * The manifest of [bag], as `META-INF/ki/bags/<bag class>.properties` holds it:
     * what discovery reads instead of creating the bag. [preload] is the manifest's
     * own choice; everything else comes from the bag, and activation refuses a bag
     * whose manifest no longer matches it. Any bag may preload, but it is generally a
     * bad idea: a preloaded bag costs every host its memory whether or not it is used.
     * Keep it for a small bag that is used frequently (Ki.Math, Ki.Sci, Ki.Bio); every
     * other bag should load when first referred to.
     */
    @JvmStatic
    @JvmOverloads
    fun manifestOf(bag: TypeBag, preload: Boolean = false): String =
        Manifest.of(bag, bag.javaClass.classLoader ?: TypeBag::class.java.classLoader, preload).text()

    /**
     * What is wrong with [bag]'s manifest on its class path, or nothing: a missing
     * manifest, or names that differ from the bag's. A bag's own tests should call it,
     * after [check]. The messages end with the manifest the bag needs.
     */
    @JvmStatic
    fun checkManifest(bag: TypeBag): List<String> {
        val loader = bag.javaClass.classLoader ?: TypeBag::class.java.classLoader
        val path = "$MANIFEST_DIR${bag.javaClass.name}.properties"
        val url = loader.getResource(path)
        val props = Properties()
        url?.openStream()?.reader(Charsets.UTF_8)?.use { props.load(it) }
        val expected = Manifest.of(bag, loader, props.getProperty("preload")?.trim() == "true")
        val out = ArrayList<String>()
        if (url == null) {
            out.add("${bag.name}: no manifest at $path")
        } else {
            val diffs = Manifest.read(bag.javaClass.name, loader, props).differences(expected)
            for (d in diffs) out.add("${bag.name}: manifest $d")
        }
        if (out.isNotEmpty()) out.add("${bag.name}: the manifest should be:\n" + expected.text())
        return out
    }

    /** What a discovered bag's manifest says, read without creating the bag. */
    private class Manifest(
        val className: String,
        val loader: ClassLoader,
        val name: String,
        val requires: List<String>,
        val preload: Boolean,
        /** Type name to Kotlin class name. */
        val types: Map<String, String>,
        /** Literal name (lowercase) to type name. */
        val literals: Map<String, String>,
        val functions: Set<String>,
        val constants: Set<String>,
        /** Member names of the bag's own types. */
        val members: Set<String>,
        /** `Type.member` for each member the bag adds to another bag's type. */
        val extensions: Set<String>,
    ) {
        /** The differences between this manifest and [actual] (made from the bag), preload aside. */
        fun differences(actual: Manifest): List<String> {
            val out = ArrayList<String>()
            fun cmp(what: String, a: Any, b: Any) { if (a != b) out.add("$what is $a, the bag has $b") }
            cmp("name", name, actual.name)
            cmp("requires", requires.toSet(), actual.requires.toSet())
            cmp("types", types, actual.types)
            cmp("literals", literals, actual.literals)
            cmp("functions", functions, actual.functions)
            cmp("constants", constants, actual.constants)
            cmp("members", members, actual.members)
            cmp("extensions", extensions, actual.extensions)
            return out
        }

        fun text(): String {
            val b = StringBuilder()
            b.append("# The type bag ").append(name).append(" (").append(className).append("): read by Ki.Core's TypeBags\n")
            b.append("# instead of creating the bag, which is created when first referred to. TypeBags.manifestOf(bag) writes it.\n")
            b.append("name=").append(name).append('\n')
            b.append("requires=").append(requires.joinToString(" ")).append('\n')
            b.append("preload=").append(preload).append('\n')
            for ((t, cls) in types.toSortedMap()) {
                val ls = literals.filterValues { it == t }.keys.sorted()
                b.append("type.").append(t).append('=').append(cls)
                for (l in ls) b.append(' ').append(l)
                b.append('\n')
            }
            b.append("functions=").append(functions.sorted().joinToString(" ")).append('\n')
            b.append("constants=").append(constants.sorted().joinToString(" ")).append('\n')
            b.append("members=").append(members.sorted().joinToString(" ")).append('\n')
            val ext = extensions.groupBy({ it.substringBefore('.') }, { it.substringAfter('.') })
            for ((t, ms) in ext.toSortedMap()) b.append("extends.").append(t).append('=').append(ms.sorted().joinToString(" ")).append('\n')
            return b.toString()
        }

        companion object {
            private fun words(v: String?): List<String> = v?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()

            fun read(cls: String, loader: ClassLoader, p: Properties): Manifest {
                val name = p.getProperty("name")?.trim() ?: throw IllegalArgumentException("no name")
                require(BAG_NAME.matches(name)) { "the name '$name' is not a lowercase identifier" }
                val types = LinkedHashMap<String, String>()
                val literals = LinkedHashMap<String, String>()
                val ext = LinkedHashSet<String>()
                for (k in p.stringPropertyNames()) {
                    when {
                        k.startsWith("type.") -> {
                            val t = k.removePrefix("type.")
                            require(TYPE_NAME.matches(t)) { "the type name '$t' is not a capitalized identifier" }
                            val ws = words(p.getProperty(k))
                            require(ws.isNotEmpty()) { "type $t has no class" }
                            types[t] = ws[0]
                            for (l in ws.drop(1)) {
                                require(LITERAL_NAME.matches(l)) { "the literal name '$l' is not letters starting with a lowercase letter" }
                                literals[l.lowercase(Locale.ROOT)] = t
                            }
                        }
                        k.startsWith("extends.") -> for (mbr in words(p.getProperty(k))) ext.add(k.removePrefix("extends.") + "." + mbr)
                        k in KEYS -> {}
                        else -> throw IllegalArgumentException("unknown key '$k'")
                    }
                }
                return Manifest(cls, loader, name, words(p.getProperty("requires")), p.getProperty("preload")?.trim() == "true",
                    types, literals, words(p.getProperty("functions")).toSet(), words(p.getProperty("constants")).toSet(),
                    words(p.getProperty("members")).toSet(), ext)
            }

            /** The manifest [bag] needs (reads its names, and so loads its types' classes). */
            fun of(bag: TypeBag, loader: ClassLoader, preload: Boolean): Manifest {
                val types = LinkedHashMap<String, String>()
                val literals = LinkedHashMap<String, String>()
                val members = LinkedHashSet<String>()
                for (t in bag.types) {
                    types[t.name] = t.kotlinClass.java.name
                    for (l in t.literals) literals[l.lowercase(Locale.ROOT)] = t.name
                    for (mbr in t.members) members.add(mbr.name)
                }
                val ext = LinkedHashSet<String>()
                for ((t, ms) in bag.extensions) for (mbr in ms) ext.add("$t.${mbr.name}")
                return Manifest(bag.javaClass.name, loader, bag.name, bag.requires.toList(), preload, types, literals,
                    bag.functions.map { it.name }.toSet(), bag.constants.keys.toSet(), members, ext)
            }

            private val KEYS = setOf("name", "requires", "preload", "functions", "constants", "members")
        }
    }

    /** The discovered bags not active yet, indexed by every name that refers to one. Immutable. */
    private class Catalog(val pending: Map<String, Manifest>) {
        val byLiteral = HashMap<String, Manifest>()
        val byType = HashMap<String, Manifest>()
        val byClass = HashMap<String, Manifest>()
        val byValueName = HashMap<String, Manifest>()
        val byExtension = HashMap<String, Manifest>()
        val memberNames = HashSet<String>()

        init {
            for (m in pending.values) {
                for (l in m.literals.keys) byLiteral[l] = m
                for ((t, c) in m.types) { byType[t] = m; byClass[c] = m }
                for (v in m.functions + m.constants) byValueName[v] = m
                for (e in m.extensions) { byExtension[e] = m; memberNames.add(e.substringAfter('.')) }
                memberNames.addAll(m.members)
            }
        }

        /** Why [m] cannot join these bags, or null. */
        fun conflict(m: Manifest): String? {
            for (t in m.types.keys) byType[t]?.let { if (it.name != m.name) return "Type name '$t' is taken (bags '${it.name}' and '${m.name}')" }
            for ((t, c) in m.types) byClass[c]?.let { if (it.name != m.name) return "Class $c has two types (${it.name}.${it.types.entries.first { e -> e.value == c }.key} and ${m.name}.$t)" }
            for ((l, t) in m.literals) byLiteral[l]?.let { if (it.name != m.name) return "Literal name '$l' is taken (${it.name}.${it.literals[l]} and ${m.name}.$t)" }
            for (v in m.functions + m.constants) byValueName[v]?.let { if (it.name != m.name) return "The name '$v' is taken (bags '${it.name}' and '${m.name}')" }
            return null
        }

        /** The bag that declares [c], a superclass of it or an interface it implements, as [State.find] walks them. */
        fun find(c: Class<*>): Manifest? {
            var k: Class<*>? = c
            while (k != null) { byClass[k.name]?.let { return it }; k = k.superclass }
            val queue = ArrayDeque<Class<*>>()
            k = c
            while (k != null) { queue.addAll(k.interfaces); k = k.superclass }
            val seen = HashSet<Class<*>>()
            while (queue.isNotEmpty()) {
                val i = queue.removeFirst()
                if (!seen.add(i)) continue
                byClass[i.name]?.let { return it }
                queue.addAll(i.interfaces)
            }
            return null
        }
    }

    // ------------------------------------------------------------------ the class cache

    /** What [typeForClass] found for a class, for one snapshot and catalog. Held per class, so it goes when the class does. */
    private class Cached(val state: State, val catalog: Catalog, val type: ValueType<Any>?)

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
