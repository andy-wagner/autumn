package norswap.autumn.parsers;

import norswap.autumn.Parse;
import norswap.autumn.Parser;
import norswap.autumn.ParserVisitor;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.IntPredicate;

import static norswap.autumn.AutumnUtil.replace_closing_square_brackets;

/**
 * Matches a single character that satisfies a predicate, within {@link Parse#string}.
 */
public final class CharPredicate extends Parser
{
    // ---------------------------------------------------------------------------------------------

    /**
     * The display name for this parser.
     */
    public final String name;

    // ---------------------------------------------------------------------------------------------

    public final IntPredicate predicate;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single character that satisfies {@code predicate}.
     * {@code name} is used as display name for this parser.
     */
    public CharPredicate (String name, IntPredicate predicate)
    {
        this.name = name;
        this.predicate = predicate;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean doparse (Parse parse)
    {
        assert parse.string != null;
        if (predicate.test(parse.char_at(parse.pos))) {
            ++ parse.pos;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void accept (ParserVisitor visitor) {
        visitor.visit(this);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Iterable<Parser> children() {
        return Collections.emptyList();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toStringFull()
    {
        return name;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches any single character except the nul ('\0') character.
     */
    public static CharPredicate any ()
    {
        return new CharPredicate("<any char>", it -> it != 0);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single {@code c} character.
     */
    public static CharPredicate single (char c)
    {
        return new CharPredicate("["+ c + "]", it -> it == c);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single character in the [start-end] range.
     */
    public static CharPredicate range (char start, char end)
    {
        String name = replace_closing_square_brackets("[" + start + "-" + end + "]");
        return new CharPredicate(name, it ->
            start <= it && it <= end);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single character contains in {@code chars}.
     */
    public static CharPredicate set (String chars)
    {
        String name = replace_closing_square_brackets("[" + chars + "]");
        return new CharPredicate(name, it ->
            chars.indexOf(it) >= 0);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single character contains in {@code chars}.
     */
    public static CharPredicate set (char... chars)
    {
        String name = replace_closing_square_brackets(Arrays.toString(chars));
        return new CharPredicate(name, it -> {
            for (char c: chars) if (c == it) return true;
            return false;
        });
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single ASCII alphabetic character.
     */
    public static CharPredicate alpha()
    {
        return new CharPredicate("<alpha>", it ->
            'a' <= it && it <= 'z' || 'A' <= it && it <= 'Z');
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single ASCII alpha-numeric character.
     */
    public static CharPredicate alphanum()
    {
        return new CharPredicate("<alpha>", it ->
            'a' <= it && it <= 'z' || 'A' <= it && it <= 'Z' || '0' <= it && it <= '9');
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single decimal digit.
     */
    public static CharPredicate digit()
    {
        return new CharPredicate("<digit>", it ->
            '0' <= it && it <= '9');
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single hexadecimal digit (for letters, both the
     * lowercase and uppercase forms are allowed).
     */
    public static CharPredicate hex_digit()
    {
        return new CharPredicate("<hex digit>", it ->
            '0' <= it && it <= '9' || 'a' <= it && it <= 'f' || 'A' <= it && it <= 'F');
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new parser that matches a single octal digit.
     */
    public static CharPredicate octal_digit()
    {
        return new CharPredicate("<octal digit>", it ->
            '0' <= it && it <= '7');
    }

    // ---------------------------------------------------------------------------------------------
}
