package norswap.utils
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.Arrays

// -------------------------------------------------------------------------------------------------

/**
 * Shorthand for [StringBuilder.append].
 */
operator fun StringBuilder.plusAssign(o: Any?) { append(o) }

// -------------------------------------------------------------------------------------------------

/**
 * Use this to enable Kotlin smart casts at no cost: include a value cast as the parameter to
 * this function, and the code that follows will be able to assume that the value is of the
 * type it was casted to.
 *
 * e.g. `proclaim (vehicle as Truck)`
 */
@Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
inline fun proclaim (cast: Any)
    = Unit

// -------------------------------------------------------------------------------------------------

/**
 * Shorthand for [Arrays.toString].
 */
inline val <T> Array<T>.str: String
    get() = Arrays.toString(this)

// -------------------------------------------------------------------------------------------------

/**
 * Converts this alphanum CamelCase string to snake_case.
 * @author http://stackoverflow.com/questions/10310321
 */
fun String.camel_to_snake(): String
{
    return this
        .replace("([a-z0-9])([A-Z]+)".toRegex(), "$1_$2")
        .toLowerCase()
}

// -------------------------------------------------------------------------------------------------

/**
 * Converts this alphanum snake_case string to CamelCase.
 */
fun String.snake_to_camel(): String
{
    return this
        .split('_')
        .map(String::capitalize)
        .joinToString()
}

// -------------------------------------------------------------------------------------------------

/**
 * Reads a complete file and returns its contents as a string.
 * @throws IOException see [Files.readAllBytes]
 * @throws InvalidPathException see [Paths.get]
 */
fun read_file (file: String)
    = String(Files.readAllBytes(Paths.get(file)))

// -------------------------------------------------------------------------------------------------

/**
 * Returns a list of all the paths that match the given glob pattern within the given directory.
 *
 * The pattern syntax is described in the doc of [FileSystem.getPathMatcher] --
 * the "glob:" part should be omitted.
 */
@Throws(IOException::class)
fun glob (pattern: String, directory: Path): List<Path>
{
    val matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern)

    val result = ArrayList<Path>()

    Files.walkFileTree (directory, object : SimpleFileVisitor<Path>()
    {
        override fun visitFile (file: Path, attrs: BasicFileAttributes): FileVisitResult
        {
            if (matcher.matches(file)) result.add(file)
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed (file: Path, exc: IOException?)
            = FileVisitResult.CONTINUE
    })

    return result
}

// -------------------------------------------------------------------------------------------------

/**
 * True iff the receiver is a subtype of (i.e. is assignable to) the parameter.
 */
infix fun Class<*>.extends(other: Class<*>): Boolean
    = other.isAssignableFrom(this)

/**
 * True iff the receiver is a supertype of (i.e. can be assigned from) the parameter.
 */
infix fun Class<*>.supers (other: Class<*>): Boolean
    = this.isAssignableFrom(other)

/**
 * True iff the type parameter is a subtype of (i.e. is assignable to) the parameter.
 */
inline fun <reified T: Any> extends (other: Class<*>): Boolean
    = T::class.java extends other

/**
 * True iff the type parameter is a supertype of (i.e. can be assigned from) the parameter.
 */
inline fun <reified T: Any> supers (other: Class<*>): Boolean
    = T::class.java supers other

// -------------------------------------------------------------------------------------------------

// TODO: limitation of three next functions? mutation?

/**
 * Returns an array of the given size, populated with nulls, but casted to an array of non-nullable
 * items. This is unsafe, but handy when an array has to be allocated just to be populated
 * immediately, but the map-style [Array] constructor is not convenient. It also helps construct
 * array of nulls for non-reifiable types.
 */
fun <T> arrayOfSize (size: Int): Array<T>
{
    @Suppress("UNCHECKED_CAST")
    return arrayOfNulls<Any>(size) as Array<T>
}

// -------------------------------------------------------------------------------------------------

/**
 * Inexplicably missing standard library function.
 */
fun <T> Sequence<T>.toArray(): Array<T>
{
    @Suppress("UNCHECKED_CAST")
    return toCollection(ArrayList<T>()).toArray() as Array<T>
}

// -------------------------------------------------------------------------------------------------

/**
 * Maps a sequence to an array.
 */
inline fun <T, Out> Sequence<T>.mapToArray (f: (T) -> Out): Array<Out>
{
    @Suppress("UNCHECKED_CAST")
    return mapTo(ArrayList<Out>(), f).toArray() as Array<Out>
}

// -------------------------------------------------------------------------------------------------

/**
 * Prints the list with each item on a single line
 */
fun List<*>.lines(): String
    =  joinToString(separator = ",\n ", prefix = "[", postfix = "]")

// -------------------------------------------------------------------------------------------------

/**
 * Like `this.flatten.forEach(f)`, but without the memory overheads.
 */
inline fun <T> Iterable<Iterable<T>>.flat_foreach (f: (T) -> Unit) {
    forEach { it.forEach { f(it) } }
}

// -------------------------------------------------------------------------------------------------

/**
 * Casts the receiver to type T (unsafe).
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <T> Any?.cast(): T
    = this as T

// -------------------------------------------------------------------------------------------------

/**
 * Returns a view of the list without its first [n] items (default: 1).
 */
fun <T> List<T>.rest (n: Int = 1): List<T>
    = subList(n, size)

// -------------------------------------------------------------------------------------------------

/**
 * Returns a view of the list without its last [n] items.
 */
fun <T> List<T>.except (n: Int): List<T>
    = subList(0, size - n)

// -------------------------------------------------------------------------------------------------

/**
 * Tries to run [f], returning its return value if successful and null if an exception is thrown.
 */
inline fun <T: Any> attempt (f: () -> T): T?
    = try { f() } catch (_: Exception) { null }

// -------------------------------------------------------------------------------------------------

/**
 * Returns a list wrapping [item] if not null or an empty list otherwise.
 */
fun <T: Any> maybe_list (item: T?): List<T>
    = if (item == null) emptyList() else listOf(item)

// -------------------------------------------------------------------------------------------------

/**
 * Returns [list] if not null or an empty list otherwise.
 */
fun <T> maybe_list (list: List<T>?): List<T>
    = list ?: emptyList()

// -------------------------------------------------------------------------------------------------

/**
 * Returns the nth type argument to the superclass of the class of [obj].
 */
fun nth_superclass_targ (obj: Any, n: Int): Class<*>?
{
    val zuper = obj::class.java.genericSuperclass as? ParameterizedType ?: return null
    if (zuper.actualTypeArguments.size < n) return null
    val ntype = zuper.actualTypeArguments[n - 1]
    if (ntype is ParameterizedType)
        return ntype.rawType as Class<*>
    else
        return ntype as Class<*>
}

// -------------------------------------------------------------------------------------------------

/**
 * Returns of first instance of [T] from the iterable, or throws an exception.
 */
inline fun <reified T> Iterable<*>.first_instance(): T
    = filter { it is T } as T

// -------------------------------------------------------------------------------------------------

/**
 * Syntactic sugar for [then].
 *
 * Enables two cute things:
 *
 * - An if that evaluates to true on the else branch: `(<condition>) .. { <body> }`
 * - The ternary operator: `(<condition>) .. { <if-true> } ?: <if-false>`
 */
inline operator fun <T> Boolean.rangeTo (f: () -> T): T?
    = then(f)

// -------------------------------------------------------------------------------------------------

/**
 * Enables two cute things:
 *
 * - An if that evaluates to true on the else branch: `(<condition>) .. { <body> }`
 * - The ternary operator: `(<condition>) .. { <if-true> } ?: <if-false>`
 */
inline fun <T> Boolean.then (f: () -> T): T?
    = if (this) f() else null

// -------------------------------------------------------------------------------------------------

/**
<<<<<<< HEAD
 * If the receiver can be cast to [T], run [f] over it and return the result. Else return null.
 *
 * You typically have to specify the type of the parameter in the lambda, because Kotlin type
 * inference is dumb.
 */
inline fun <reified T, R> Any?.when_is (f: (T) -> R): R?
    = if (this is T) f(this) else null

// -------------------------------------------------------------------------------------------------

/**
 * Applies [f] to the receiver if it is non-null, else return null.
 */
inline fun <T: Any, R: Any> T?.inn (f: (T) -> R): R?
    = if (this != null) f(this) else null

// -------------------------------------------------------------------------------------------------
