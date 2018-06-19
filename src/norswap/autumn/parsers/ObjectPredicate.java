package norswap.autumn.parsers;

import norswap.autumn.Parse;
import norswap.autumn.Parser;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Matches a single object that satisfies a predicate, within {@link Parse#list}.
 */
public final class ObjectPredicate extends Parser
{
    // ---------------------------------------------------------------------------------------------

    /**
     * The display name for this parser.
     */
    public final String name;

    // ---------------------------------------------------------------------------------------------

    public final Predicate<Object> predicate;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single object that satisfies {@code predicate}.
     * {@code name} is used as display name for this parser.
     */
    public ObjectPredicate (String name, Predicate<Object> predicate)
    {
        this.name = name;
        this.predicate = predicate;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean doparse (Parse parse)
    {
        assert parse.list != null;
        if (predicate.test(parse.object_at(parse.pos))) {
            ++ parse.pos;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single object with class {@code klass}.
     */
    public static ObjectPredicate instance (Class<?> klass)
    {
        return new ObjectPredicate("<is? " + klass.getSimpleName() + ">", klass::isInstance);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches any single non-null object.
     */
    public static ObjectPredicate any()
    {
        return new ObjectPredicate("<any object>", Objects::nonNull);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toStringFull()
    {
        return name;
    }

    // ---------------------------------------------------------------------------------------------
}
