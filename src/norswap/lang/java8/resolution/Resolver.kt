package norswap.lang.java8.resolution
import norswap.lang.java8.classes
import norswap.lang.java8.java_virtual_node
import norswap.lang.java8.scopes.BytecodeClassScope
import norswap.lang.java8.scopes.Scope
import norswap.lang.java8.scopes.ClassScope
import norswap.lang.java8.typing.MemberInfo
import norswap.uranium.Attribute
import norswap.uranium.Continue
import norswap.uranium.Reaction
import norswap.uranium.Context
import norswap.utils.attempt
import org.apache.bcel.classfile.ClassParser
import java.net.URL
import java.net.URLClassLoader

/**
 * ## Lexicon
 *
 * Simple class name: the unqualified class name (single identifier).
 *
 * Full class name: the fully qualified class name (with outer classes and package), separated with
 * dots.
 *
 * Canonical class name: the fully qualified class name (with outer classes and package). Each
 * class is separated from its inner classe by a dollar, while package components are separated by
 * dots.
 *
 * Full class chain: the list of components of a full class name.
 *
 * Class chain: a list of class name components that may or may not include the package part and/or
 * outer classes. Hence full class chains are a subset of all class chains.
 */
object Resolver
{
    // ---------------------------------------------------------------------------------------------

    /**
     * Bogus class-like object used to represent "failures to load" in the caches.
     * Never escapes the resolver.
     */
    private object Miss: ClassScope()
    {
        override val super_type           = null
        override val timestamp            = 0L
        override val name           get() = throw NotImplementedError()
        override val canonical_name get() = throw NotImplementedError()
        override val kind           get() = throw NotImplementedError()
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Maps canonical class names to their class information.
     *
     * A null value indicates that the class could not be loaded from .class files or through
     * reflection, but could be defined later by a source file.
     */
    private val class_cache = HashMap<String, ClassScope>()

    // ---------------------------------------------------------------------------------------------

    /**
     * Maps full class chains to their class information.
     * This is important because the canonical name of a class chain is ambiguous.
     */
    private val chain_cache = HashMap<List<String>, ClassScope>()

    // ---------------------------------------------------------------------------------------------

    private val syscl = ClassLoader.getSystemClassLoader() as URLClassLoader

    // ---------------------------------------------------------------------------------------------

    private val urls: Array<URL> = syscl.urLs

    // ---------------------------------------------------------------------------------------------

    private val loader = URLClassLoader(urls)

    // ---------------------------------------------------------------------------------------------

    fun register_class (klass: ClassScope)
    {
        var old = class_cache.putIfAbsent(klass.canonical_name, klass)
        Context.classes[klass.canonical_name] = klass

        if (old != null) {
            // TODO must handle class conflicts
            // - javac: -Xprefer:newer (default) and -Xprefer:source
            // - how to update everything when class is redefined
            // > must have a reaction depending on classes node
            // - ConflictingClassDefinitions(klass.canonical_name)
        }

        if (!klass.inner) return

        val chain = klass.chain
        old = chain_cache[chain]
        if (old == null) return

        val reactor    = Context.reactor
        val chain_name = chain.joinToString(".")

        val i1 = klass.canonical_name.indexOf('$')
        val i2 = old  .canonical_name.indexOf('$')

        if (i1 < i2) {
            val chain_node = reactor.java_virtual_node.chains
            chain_node[chain_name] = klass
            chain_cache[chain] = klass
        }

        if (!old.ambiguous_chain) {
            reactor.register_error(AmbiguousClassDefinitions(chain_name))
            klass.ambiguous_chain = true
            old  .ambiguous_chain = true
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the pass to the class designated by the given canonical name using the given class
     * loader.
     */
    fun find_class_path (loader: URLClassLoader, cano_name: String): URL?
        = loader.findResource(cano_name.replace('.', '/') + ".class")

    // ---------------------------------------------------------------------------------------------

    private fun load_from_url (class_url: URL): ClassScope?
    {
        try {
            val cparser = ClassParser(class_url.openStream(), class_url.toString())
            val bclass = cparser.parse()
            return BytecodeClassScope(bclass)
        } catch (e: Exception) {
            // TODO register error
            return null
        }
    }

    // ---------------------------------------------------------------------------------------------

    private fun load_from_classfile (cano_name: String): ClassScope?
    {
        return find_class_path(loader, cano_name)
            ?. let { load_from_url(it) }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Some core classes have no associated .class files, search for those through reflection.
     * e.g. `java.lang.Object`
     *
     * However it seems that sometimes these classes have associated class files in `rt.jar`.
     */
    private fun load_reflectively (cano_name: String): ClassScope?
    {
        if (cano_name.startsWith("java.") || cano_name.startsWith("javax."))
            return attempt { syscl.loadClass(cano_name) } ?. let(::ReflectionClassLike)
        else
            return null
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Try to load a class from .class files or reflectively, using its canonical name, without
     * reading or writing the class cache.
     */
    private fun load_class (cano_name: String): ClassScope?
    {
        return load_from_classfile(cano_name) ?: load_reflectively(cano_name)
    }

    // ---------------------------------------------------------------------------------------------

    private fun load_class_reaction (cano_name: String, name: String, scope: Scope.Node)
    {
        Reaction (scope) {
            _optional = true // TODO because may not provide attribute -- keep?
            _provided = listOf(Attribute(scope, name))
            _trigger  = {
                val klass = load_class(cano_name)
                if (klass != null) scope[name] = klass
                class_cache[cano_name] = klass ?: Miss
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Input: the canonical name of a class, as well as a scope node and the name under which
     * the class should be registered within that scope.
     *
     * If we attempted loading the class earlier, the result (successful or not (null)) is returned.
     *
     * Otherwise we create a reaction tasked with loading the class, caching the result and
     * setting the scope appropriately; then we return null.
     */
    fun klass (cano_name: String, name: String, scope: Scope.Node): ClassScope?
    {
        val cached = class_cache[cano_name]
        if (cached == Miss) return null

        if (cached != null) {
            scope[name] = cached
            return cached
        }

        load_class_reaction(cano_name, name, scope)
        return null
    }

    // ---------------------------------------------------------------------------------------------

    fun klass (cano_name: String, scope: Scope.Node): ClassScope?
    {
        return klass(cano_name, cano_to_simple_name(cano_name), scope)
    }

    // ---------------------------------------------------------------------------------------------

    fun klass (cano_name: String): ClassScope?
    {
        val reactor    = Context.reactor
        val klass_node = reactor.java_virtual_node.classes

        return klass(cano_name, cano_name, klass_node)
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Loads the named class eagerly (from the cache or by reading class files).
     * This function may block while class files are being read.
     * The cache may be updated as a result.
     * Throws an error if the class cannot be loaded in this way.
     *
     * This is used to load "well-known" Java classes (e.g. `java.lang.Object`).
     */
    fun eagerly (cano_name: String): ClassScope
    {
        val cached = class_cache[cano_name]
        if (cached != null && cached != Miss) return cached

        val klass = load_class(cano_name) ?: throw Error("could not load class: $cano_name")
        register_class(klass)
        return klass
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Derives all possible canonical names for the chain.
     */
    private fun cano_names (chain: List<String>): List<String>
    {
        val size = chain.size
        return chain.indices.map {
            val prefix = chain.subList(0, size - it).joinToString(".")
            if (it == 0) prefix
            else         prefix + chain.subList(size - it, size).joinToString("$", prefix = "$")
        }
    }

    // ---------------------------------------------------------------------------------------------

    // fully qualified chains
    fun full_chain (chain: List<String>): ClassScope?
    {
        val cached = chain_cache[chain]
        if (cached == Miss) return null
        if (cached != null) return cached

        val cano_names = cano_names(chain)
        val chain_name = cano_names[0] // no static classes
        val reactor    = Context.reactor
        val class_node = reactor.java_virtual_node.classes
        val chain_node = reactor.java_virtual_node.classes
        val chain_attr = Attribute(chain_node, chain_name)

        val required = cano_names.map {
            class_cache[it] ?: load_class_reaction(it, it, class_node)
            Attribute(class_node, it)
        }

        Reaction (chain_node) {
            _consumed = required
            _provided = listOf(chain_attr)
            _trigger  = {
                val available = consumed.filter { it.get() != Attribute.None }
                val klass = available.lastOrNull()?.get() as ClassScope?

                if (available.size > 1) {
                    klass?.ambiguous_chain = true
                    reactor.register_error(AmbiguousClassDefinitions(chain_name))
                }

                chain_cache[chain]      = klass ?: Miss
                chain_node[chain_name]  = klass ?: Attribute.None
            }
        }

        // Redo the current reaction when the chain becomes available,
        // or when its value changes (for a more specific one).
        Reaction (chain_node) {
            _consumed = listOf(chain_attr)
            _pushed = Context.reaction
        }

        return null

        // TODO
        // - all raw node access must check for None
    }

    // ---------------------------------------------------------------------------------------------

    fun klass_chain (scope: Scope, chain: List<String>): ClassScope?
    {
        // TODO
        // - will hang on every component to know whether it exists or not?
        // - no: will return null if not present
        // - problem: need to load static classes at some point...
        // - override lookup function in binary thingy to throw a continue

        var cur_scope: Scope? = scope
        var last = 0
        var cont: Continue? = null

        try {
            for (item in chain) {
                val next = cur_scope?.class_like(item)
                cur_scope = next
                if (next == null) break
                ++last
            }
        }
        catch (e: Continue) { cont = e }

        // TODO if full chain returns but we have a continuation?
        // - reaction is registered on instantiation
        // - but continued_in/from only in reactor
        //      - hard to set when instantiating: we don't know from!
        //      - expand context?
        //      - alternative: add dependency on +self to all rules
        //          - issue: must propagate self down
        // - triggered = false in reactor

        if (last == chain.size)
            return cur_scope as ClassScope
        if (last == 0)
            return full_chain(chain)
        if (cont != null)
            throw cont
        else
            throw Error() //Fail(ClassNotFoundScopeError())
    }

    // ---------------------------------------------------------------------------------------------

    fun resolve_members (cano_name: String, member: String): Collection<MemberInfo>
    {
        val klass = klass(cano_name)
        val members = klass?.member(member) ?: emptyList() // TODO hackfix
        if (members.isEmpty())
            throw Error() //Fail(MemberNotFoundScopeError())
        return members
    }
}