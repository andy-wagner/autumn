package norswap.autumn;

import norswap.autumn.memo.*;
import norswap.autumn.parsers.*;
import norswap.utils.NArrays;
import norswap.utils.Slot;
import norswap.utils.Util;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This class implements a domain specific language (DSL) for creating parsers. It's just
 * a nicer API than having to piece together parser constructors.
 *
 * <p>This class features methods that return a {@link rule} object wrapping a parser.
 * Methods can be called on this wrapper to create further wrappers. e.g.:
 *
 * <pre>
 * {@code
 * Parser arith = digit().at_least(1).sep(1, choice("+", "-")).get();
 * }
 * </pre>
 *
 * <p><b>Usage:</b> To use the DSL, create a class (the <b>grammar class</b>) that extends this class
 * (recommended). It's also possible to instantiate this class and to call methods on it.
 *
 * <p><b>Automatic conversion:</b> Most DSL methods take instances of {@code Object} instead of
 * {@link Parser}. Parsers passed like this are simply passed through. Parsers are extracted out
 * of {@link rule} instances, and {@code String} instances are replaced by calling {@link #str}
 * with the string.
 *
 * <p><b>Whitespace handling:</b> set {@link #ws} to skip whitespace after matching certain parser
 * (most importantly, when using {@link #word}).
 */
public class DSL
{
    // =============================================================================================
    // Public Properties and Constructors
    // =============================================================================================

    /**
     * The token factory used by the grammar.
     */
    public final Tokens tokens;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new instance using the default memoization strategy for tokens (currently: an
     * 8-slot cache).
     */
    public DSL () {
        this.tokens = new Tokens(() -> new MemoCache(8, false));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new instance using a custom memoization strategy for tokens.
     */
    public DSL (Supplier<Memoizer> token_memo) {
        this.tokens = new Tokens(token_memo);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Change this to specify the whitespace parser used for {@link #word} and {@link rule#word} and
     * used after automatically converted string literals.
     *
     * <p>This parser <b>must</b> always succeed, meaning it must be able to succeed matching
     * the empty string.
     *
     * <p>null by default, meaning no whitespace will be matched by {@link #word}, {@link rule#word}
     * and automatically converted string literals.
     *
     * <p>Both {@link #word} and {@link rule#word} capture the value of this field when called, so
     * setting the value of this field should be one of the first thing you do in your grammar.
     *
     * <p>If {@link #exclude_ws_errors} is set, its {@link Parser#exclude_errors} field will be
     * automatically set as long as {@link #word(String)} or {@link rule#word()} is called at least
     * once (otherwise you'll have to set it yourself if you use {@code ws} explicitly).
     */
    public rule ws = null;

    // ---------------------------------------------------------------------------------------------

    private Parser ws() {
        if (ws == null)
            return empty.get();
        Parser p = ws.get();
        if (!p.exclude_errors && exclude_ws_errors)
            p.exclude_errors = true;
        return p;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Whether to exclude errors inside whitespace ({@link #ws}) from counting against the furthest
     * parse error ({@link Parse#error}). True by default.
     */
    public boolean exclude_ws_errors = true;

    // =============================================================================================
    // Auto Conversion
    // =============================================================================================

    private Parser compile (Object item)
    {
        if (item instanceof rule)
            return ((rule) item).get();

        if (item instanceof Parser)
            return (Parser) item;

        if (item instanceof String)
            return new StringMatch((String) item, null);

        throw new Error("unknown item type " + item.getClass());
    }

    // =============================================================================================
    // Misc Utilities
    // =============================================================================================

    /**
     * Wraps the given parser into a {@link rule}.
     */
    public rule rule (Parser parser) {
        return new rule(parser);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the given object, casted to type {@code T}.
     *
     * <p>The target type {@code T} can be inferred from the assignment target.
     * e.g. {@code Object x = "hello"; String y = $(x);}
     */
    public <T> T $ (Object object)
    {
        //noinspection unchecked
        return (T) object;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the array item at the given index, casted to type {@code T}.
     *
     * @see #$
     */
    public <T> T $ (Object[] array, int index)
    {
        //noinspection unchecked
        return (T) array[index];
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new empty list of type T.
     */
    public <T> List<T> list ()
    {
        return Collections.emptyList();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new list wrapping the given array after casting it to to an array of type {@code T}.
     *
     * <p>Use the {@code this.<T>list(array)} form to specify the type {@code T}.
     */
    public <T> List<T> list (Object... array)
    {
        //noinspection unchecked
        return Arrays.asList((T[]) array);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new list wrapping the slice {@code [start, length[} of {@code array} after casting
     * it to to an array of type {@code T}.
     *
     * <p>Use the {@code this.<T>list(array)} form to specify the type {@code T}.
     */
    public <T> List<T> list (int start, Object[] array)
    {
        //noinspection unchecked
        return Arrays.asList(Arrays.copyOfRange((T[]) array, start, array.length));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new list wrapping the slice {@code [start, end[} of {@code array} after casting it
     * to to an array of type {@code T}.
     *
     * <p>Use the {@code this.<T>list(array)} form to specify the type {@code T}.
     */
    public <T> List<T> list (int start, int end, Object[] array)
    {
        //noinspection unchecked
        return Arrays.asList(Arrays.copyOfRange((T[]) array, start, end));
    }

    // =============================================================================================
    // Rule Naming
    // =============================================================================================

    /**
     * Fetches all the fields declared in the class of this object (i.e. {@code this.getClass()}),
     * and for those that are of type {@link rule} or {@link Parser}, sets the rule name to the name
     * of the field, if no rule name has been set already.
     */
    public void make_rule_names ()
    {
        make_rule_names(this.getClass());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Fetches all the fields declared in {@code klass}, and for those that are of type {@link rule}
     * or {@link Parser}, sets the rule name to the name of the field, if no rule name has been set
     * already.
     */
    public void make_rule_names (Class<?> klass)
    {
        make_rule_names(DSL.class.getFields());
        make_rule_names(klass.getDeclaredFields());
    }

    // ---------------------------------------------------------------------------------------------

    // Note: supresses warning on `f.isAccessible()` deprecated after Java 8 in favor of
    // `f.canAccess(this)`. Language level 8 with a later JDK will yield a warning while we
    // can't use `canAccess` yet.
    @SuppressWarnings("deprecation")
    private void make_rule_names (Field[] fields)
    {
        try {
            for (Field f : fields) {
                if (!Modifier.isPublic(f.getModifiers()) && !f.isAccessible())
                    f.setAccessible(true);

                if (f.getType().equals(rule.class)) {
                    rule w = (rule) f.get(this);
                    if (w == null) continue;
                    Parser p = w.get();
                    if (p.rule() == null)
                        p.set_rule(f.getName());
                }
                else if (f.getType().equals(Parser.class)) {
                    Parser p = (Parser) f.get(this);
                    if (p == null) continue;
                    if (p.rule() == null)
                        p.set_rule(f.getName());
                }
            }
        }
        // Should always be a security exception: illegal access prevented by `setAccessible`.
        catch (SecurityException e) {
            throw new RuntimeException(
                "The security policy does not allow Autumn to access private or protected fields "
                    + "in the grammar. Either make all the fields containing grammar rules public, "
                    + "or amend the security policy by granting: "
                    + "permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // =============================================================================================
    // Pre-Defined Rules
    // =============================================================================================

    /**
     * A parser that always succeeds.
     */
    public rule empty = new rule(new Empty());

    // ---------------------------------------------------------------------------------------------

    /**
     * A parser that always fails.
     */
    public rule fail = new rule(new Fail());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} parser that matches any character.
     */
    public rule any = new rule(CharPredicate.any());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single ASCII alphabetic character.
     */
    public rule alpha = new rule(CharPredicate.alpha());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single ASCII alpha-numeric character.
     */
    public rule alphanum = new rule(CharPredicate.alphanum());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single decimal digit.
     */
    public rule digit = new rule(CharPredicate.digit());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single hexadecimal digit (for letters, both
     * the lowercase and uppercase forms are allowed).
     */
    public rule hex_digit = new rule(CharPredicate.hex_digit());

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single octal digit.
     */
    public rule octal_digit = new rule(CharPredicate.octal_digit());

    // ---------------------------------------------------------------------------------------------

    /**
     * A rule that matches zero or more of the usual whitespace characters (spaces, tabs (\t), line
     * return (\n) and carriage feed (\r)). Fit to be assigned to {@link #ws}.
     */
    public rule usual_whitespace = set(" \t\n\r").at_least(0);

    // =============================================================================================
    // Simple Parsers
    // =============================================================================================

    /**
     * Returns a {@link Sequence} of the given parsers.
     */
    public rule seq (Object... parsers) {
        return new rule(new Sequence(NArrays.map(parsers, new Parser[0], this::compile)));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link Choice} between the given parsers.
     */
    public rule choice (Object... parsers) {
        return new rule(new Choice(NArrays.map(parsers, new Parser[0], this::compile)));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link Longest} match choice between the given parsers.
     */
    public rule longest (Object... parsers) {
        return new rule(new Longest(NArrays.map(parsers, new Parser[0], this::compile)));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link StringMatch} parser for the given string.
     */
    public rule str (String string) {
        return new rule(new StringMatch(string, null));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link StringMatch} parser with post whitespace matching dependent on {@link
     * #ws}.
     */
    public rule word (String string) {
        return new rule(new StringMatch(string, ws()));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link CharPredicate} that matches a single character.
     */
    public rule character (char character) {
        return new rule(CharPredicate.single(character));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link CharPredicate} parser that matches an (inclusive) range of characters.
     */
    public rule range (char start, char end) {
        return new rule(CharPredicate.range(start, end));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link CharPredicate} parser that matches a set of characters.
     */
    public rule set (String string) {
        return new rule(CharPredicate.set(string));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link CharPredicate} parser that matches a set of characters.
     */
    public rule set (char... chars) {
        return new rule(CharPredicate.set(chars));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link CharPredicate} parser with name "cpred".
     */
    public rule cpred (IntPredicate predicate) {
        return new rule(new CharPredicate("cpred", predicate));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an {@link ObjectPredicate} parser with name "opred".
     */
    public rule opred (Predicate<Object> predicate) {
        return new rule(new ObjectPredicate("opred", predicate));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link ContextPredicate} parsed with name "context".
     */
    public rule context (Predicate<Parse> predicate) {
        return new rule (new ContextPredicate("context", predicate));
    }

    // =============================================================================================
    // Token Choices
    // =============================================================================================

    /**
     * Returns a {@link TokenChoice} parser that selects between the passed token parsers or base
     * token parsers. These tokens must have been defined previously (using {@link rule#token()},
     * <b>lazy references won't work.</b>
     */
    public rule token_choice (Object... parsers)
    {
        Parser[] compiled_parsers = new Parser[parsers.length];

        for (int i = 0; i < parsers.length; ++i)
        {
            if (parsers[i] instanceof String)
                throw new Error("Token choice requires exact parser reference and does not work "
                    + "with automatic string conversion. String:" + parsers[i]);

            compiled_parsers[i] = compile(parsers[i]);
        }

        return new rule(tokens.token_choice(compiled_parsers));
    }

    // =========================================================================================
    // Expression parsers
    // =========================================================================================

    /**
     * Returns a {@link LeftExpressionBuilder} that helps build a {@link LeftExpression} parser.
     */
    public LeftExpressionBuilder left_expression() {
        return new LeftExpressionBuilder();
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightExpressionBuilder} that helps build a {@link RightExpression}
     * parser.
     */
    public RightExpressionBuilder right_expression() {
        return new RightExpressionBuilder();
    }

    // =============================================================================================
    // Lazy, Recursive and Associative Parsers
    // =============================================================================================

    /**
     * Returns a {@link LazyParser} using the given supplier.
     */
    public rule lazy_parser (Supplier<Parser> supplier) {
        return new rule(new LazyParser(supplier));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LazyParser} using the given supplier.
     */
    public rule lazy (Supplier<rule> supplier) {
        return new rule(new LazyParser(() -> supplier.get().parser));
    }

    // ---------------------------------------------------------------------------------------------

    private rule recursive_parser (Function<rule, Parser> f)
    {
        Slot<Parser> slot = new Slot<>();
        slot.x = f.apply(new rule(new LazyParser(() -> slot.x)));
        return new rule(slot.x);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the parser returned by {@code f}, which takes as parameter a {@link LazyParser} able
     * to recursively invoke the parser {@code f} will return, but *not* in left position.
     */
    public rule recursive (Function<rule, rule> f)
    {
        return recursive_parser(r -> f.apply(r).get());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the parser returned by {@code f}, which takes as parameter a {@link LazyParser} able
     * to recursively invoke the parser {@code f} will return, including in left position.
     * If the parser is both left- and right-recursive, the result will be right-associative.
     *
     * <p>In general, prefer using {@link #right_fold(Object, Object, StackAction.Push)} or one of
     * its variants.
     */
    public rule left_recursive (Function<rule, rule> f) {
        return recursive_parser(r -> new LeftRecursive(f.apply(r).get(), false));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the parser returned by {@code f}, which takes as parameter a {@link LazyParser} able
     * to recursively invoke the parser {@code f} will return, including in left position.
     * If the parser is both left- and right-recursive, the result will be left-associative.
     *
     * <p>In general, prefer using {@link #left_fold(Object, Object, StackAction.Push)} or one of
     * its variants.
     */
    public rule left_recursive_left_assoc (Function<rule, rule> f) {
        return recursive_parser(r -> new LeftRecursive(f.apply(r).get(), true));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that allows left-only matches.
     */
    public rule left_fold (Object left, Object operator, Object right, StackAction.Push step) {
        return new rule(
            new LeftFold(compile(left), compile(operator), compile(right), false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that allows left-only matches, and with no step
     * action performed.
     */
    public rule left_fold (Object left, Object operator, Object right) {
        return new rule(
            new LeftFold(compile(left), compile(operator), compile(right), false, null));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that allows left-only matches, with the same
     * operand on both sides.
     */
    public rule left_fold (Object operand, Object operator, StackAction.Push step) {
        Parser coperand = compile(operand);
        return new rule(new LeftFold(coperand, compile(operator), coperand, false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that does not allow left-only matches.
     */
    public rule left_fold_full (Object left, Object operator, Object right, StackAction.Push step) {
        return new rule(
            new LeftFold(compile(left), compile(operator), compile(right), true, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that does not allow left-only matches, with the same
     * operand on both sides.
     */
    public rule left_fold_full (Object operand, Object operator, StackAction.Push step) {
        Parser coperand = compile(operand);
        return new rule(new LeftFold(coperand, compile(operator), coperand, true, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that allows left-only matches.
     */
    public rule right_fold (Object left, Object operator, Object right, StackAction.Push step) {
        return new rule(
            new RightFold(compile(left), compile(operator), compile(right), false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that allows left-only matches, with the same
     * operand on both sides.
     */
    public rule right_fold (Object operand, Object operator, StackAction.Push step) {
        Parser coperand = compile(operand);
        return new rule(new RightFold(coperand, compile(operator), coperand, false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that does not allow left-only matches.
     */
    public rule right_fold_full (Object left, Object operator, Object right, StackAction.Push step) {
        return new rule(
            new RightFold(compile(left), compile(operator), compile(right), true, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that does not allow left-only matches, with the same
     * operand on both sides.
     */
    public rule right_fold_full (Object operand, Object operator, StackAction.Push step) {
        Parser coperand = compile(operand);
        return new rule(new RightFold(coperand, compile(operator), coperand, true, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that matches a postfix expression (the right-hand
     * side matches nothing). Allows left-only matches.
     */
    public rule postfix (Object operand, Object operator, StackAction.Push step) {
        return new rule(
            new LeftFold(compile(operand), compile(operator), empty.get(), false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link LeftFold} parser that matches a postfix expression (the right-hand
     * side matches nothing). Does not allow left-only matches.
     */
    public rule postfix_full (Object operand, Object operator, StackAction.Push step) {
        return new rule(
            new LeftFold(compile(operand), compile(operator), empty.get(), true, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that matches a prefix expression (the left-hand
     * side matches nothing). Allows right-only matches.
     */
    public rule prefix (Object operator, Object operand, StackAction.Push step) {
        return new rule(
            new RightFold(empty.get(), compile(operand), compile(operator), false, step));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link RightFold} parser that matches a prefix expression (the left-hand
     * side matches nothing). Does not allow right-only matches.
     */
    public rule prefix_full (Object operator, Object operand, StackAction.Push step) {
        return new rule(
            new RightFold(empty.get(), compile(operand), compile(operator), true, step));
    }

    // =============================================================================================
    // `StackAction.Push` Type Hints
    // =============================================================================================

    /**
     * Hints that a lambda represents a {@link StackAction.PushWithParse} action, so it
     * can be used with DSL methods that except a {@link StackAction.Push}.
     */
    public StackAction.PushWithParse with_parse (StackAction.PushWithParse action) {
        return action;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Hints that a lambda represents a {@link StackAction.PushWithString} action, so it
     * can be used with DSL methods that except a {@link StackAction.Push}.
     */
    public StackAction.PushWithString with_string (StackAction.PushWithString action) {
        return action;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Hints that a lambda represents a {@link StackAction.PushWithList} action, so it
     * can be used with DSL methods that except a {@link StackAction.Push}.
     */
    public StackAction.PushWithList with_list (StackAction.PushWithList action) {
        return action;
    }

    // =============================================================================================
    // =============================================================================================
    // =============================================================================================

    /**
     * Wraps a {@link Parser} to enable builder-style parser construction.
     *
     * <p>Functionally, this is a parser wrapper, but it is called "rule" to prettify grammar
     * definitions (where each rule is a field declaration whose type is "rule").
     *
     * <p>Extract the parser using {@link #get()}.
     */
    public final class rule
    {
        // -----------------------------------------------------------------------------------------

        private final Parser parser;

        // -----------------------------------------------------------------------------------------

        private rule (Parser parser) {
            this.parser = parser;
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns the DSL instance this rule belongs to.
         */
        public DSL dsl() {
            return DSL.this;
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns this wrapper, after setting the name of the parser to the given name. Only works
         * for parsers with a name property: {@link Collect}, {@link CharPredicate} and {@link
         * ObjectPredicate}.
         */
        public rule named (String name)
        {
            /**/ if (parser instanceof Collect)
                ((Collect) parser).name = name;
            else if (parser instanceof CharPredicate)
                ((CharPredicate) parser).name = name;
            else if (parser instanceof ObjectPredicate)
                ((ObjectPredicate) parser).name = name;
            else if (parser instanceof ContextPredicate)
                ((ContextPredicate) parser).name = name;
            else
                throw new Error("Wrapped parser doesn't have a name property: " + this);

            return this;
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns the wrapped parser.
         */
        public Parser get() {
            return parser;
        }

        // -----------------------------------------------------------------------------------------

        @Override public String toString() {
            return parser.toString();
        }

        // =========================================================================================
        // Simple Combinators
        // =========================================================================================

        /**
         * Returns a negation ({@link Not}) of the parser.
         */
        public rule not() {
            return new rule(new Not(parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a lookahead version ({@link Lookahead}) of the parser.
         */
        public rule ahead() {
            return new rule(new Lookahead(parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns an optional version ({@link Optional}) of the parser.
         */
        public rule opt() {
            return new rule(new Optional(parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a repetition ({@link Repeat}) of exactly {@code n} times the parser.
         */
        public rule repeat (int n) {
            return new rule(new Repeat(n, true, parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a repetition ({@link Repeat}) of at least {@code min} times the parser.
         */
        public rule at_least (int min) {
            return new rule(new Repeat(min, false, parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns an {@link Around} parser that matches at least {@code min} repetition
         * of the parser, separated by the {@code separator} parser.
         */
        public rule sep (int min, Object separator) {
            return new rule(new Around(min, false, false, parser, compile(separator)));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns an {@link Around} parser that matches exactly {@code n} repetition
         * of the parser, separated by the {@code separator} parser.
         */
        public rule sep_exact (int n, Object separator) {
            return new rule(new Around(n, true, false, parser, compile(separator)));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns an {@link Around} parser that matches at least {@code min} repetition of the
         * parser, separated by the {@code separator} parser, and allowing for a trailing separator.
         */
        public rule sep_trailing (int min, Object separator) {
            return new rule(new Around(min, false, true, parser, compile(separator)));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Sequence} composed of the parser followed by the whitespace parser
         * {@link #ws}.
         */
        public rule word() {
            return new rule(new Sequence(parser, ws()));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link GuardedRecursion} wrapping the parser.
         */
        public rule guarded() {
            return new rule(new GuardedRecursion(parser));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new {@link TokenParser} wrapping the parser, adding it as a possible token
         * kind. The underlying parser will have its {@link Parser#exclude_errors} flag set to true.
         */
        public rule token() {
            return new rule(tokens.token_parser(parser));
        }

        // =========================================================================================
        // `Collect` parsers
        // =========================================================================================

        /**
         * Returns a {@link CollectBuilder} that lets you customize and build a {@link Collect}
         * parser.
         *
         * <p>By default: has no lookback, pops the items off the stack on success and does nothing
         * in case of failure.
         */
        public CollectBuilder collect() {
            return new CollectBuilder(parser, 0, false, false);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser, performing a simple pushing collect
         * action ({@link StackAction.Push}).
         *
         * <p>Shorthand for {@code this.collect().push(action)}, using the default parameters (no
         * lookback, items popped of the stack upon success, nothing done upon failure).
         */
        public rule push (StackAction.Push action) {
            return collect().push(action);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a peek-only {@link Collect} parser wrapping the parser. The returned parser
         * pushes true or false on the stack depending on whether the underlying parser succeeds or
         * fails. The returned parser always succeeds.
         */
        public rule as_bool()
        {
            return new rule(new Collect("as_bool", new Optional(parser), 0, true, false,
                (StackAction.PushWithParse) (p, xs) -> xs != null));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a peek-only {@link Collect} parser wrapping the parser. The returned parser
         * pushes the supplied value on the stack if the underlying parser is successful.
         */
        public rule as_val (Object value)
        {
            return new rule(new Collect("as_val", parser, 0, false, false,
                (StackAction.PushWithParse) (p, xs) -> value));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a peek-only {@link Collect} parser wrapping the parser. The returned parser
         * pushes null on the stack if and only if the underlying parser fails. The returned parser
         * always succeeds.
         */
        public rule maybe()
        {
            return new rule(new Collect("maybe", parser, 0, true, false,
                (StackAction.ActionWithParse)
                    (p,xs) -> { if (xs == null) p.stack.push((Object) null); }));
        }

        // =========================================================================================
        // Memoization
        // =========================================================================================

        /**
         * Returns a new {@link Memo} parser wrapping the parser. The parse results will be memoized
         * in a {@link MemoTable}.
         */
        public rule memo() {
            return memo((Function<Parse, Object>) null);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new context-sensitive {@link Memo} parser wrapping the parser. The parse
         * results will be memoized in a {@link MemoTable}. {@code extractor} will be used to
         * extract and compare the relevant context (see {@link Memo} for details).
         */
        public rule memo (Function<Parse, Object> extractor)
        {
            ParseState<Memoizer> memoizer
                = new ParseState<>(new Slot<>(parser), () -> new MemoTable(false));

            return new rule(new Memo(parser, memoizer, extractor));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new {@link Memo} parser wrapping the parser. The parse results will be memoized
         * in a {@link MemoCache} with {@code n} slots (must be strictly positive).
         */
        public rule memo (int n) {
            return memo(n, null);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new context-sensitive {@link Memo} parser wrapping the parser. The parse
         * results will be memoized in a {@link MemoCache} with {@code n} slots (must be strictly
         * positive). {@code extractor} will be used to extract and compare the relevant context
         * (see {@link Memo} for details).
         */
        public rule memo (int n, Function<Parse, Object> extractor)
        {
            if (n <= 0) throw new IllegalArgumentException
                ("A memo cache must have a strictly positive number of entries.");

            ParseState<Memoizer> memoizer
                = new ParseState<>(new Slot<>(parser), () -> new MemoCache(n, false));

            return new rule(new Memo(parser, memoizer, extractor));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new {@link Memo} wrapping the parser. The parse results will be memoized using
         * the supplied memoizer. This form is useful when you want to share a single memoizer
         * amongst multiple parsers.
         */
        public rule memo (ParseState<Memoizer> memoizer) {
            return new rule(new Memo(parser, memoizer, null));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a new context-sensitive {@link Memo} wrapping the parser. The parse results will
         * be memoized using the supplied memoizer. This form is useful when you want to share a
         * single memoizer amongst multiple parsers. {@code extractor} will be used to extract and
         * compare the relevant context (see {@link Memo} for details).
         */
        public rule memo (ParseState<Memoizer> memoizer, Function<Parse, Object> extractor) {
            return new rule(new Memo(parser, memoizer, extractor));
        }
    }

    // =============================================================================================
    // =============================================================================================
    // =============================================================================================

    /**
     * Lets you customize and build a {@link Collect} parser.
     *
     * <p>By default: has no lookback, pops the items off the stack on success and does nothing in
     * case of failure.
     */
    public final class CollectBuilder
    {
        // -----------------------------------------------------------------------------------------

        private final Parser parser;
        private final int lookback;
        private final boolean peek_only;
        private final boolean collect_on_fail;

        // -----------------------------------------------------------------------------------------

        CollectBuilder (Parser parser, int lookback, boolean peek_only, boolean collect_on_fail)
        {
            this.parser = parser;
            this.lookback = lookback;
            this.peek_only = peek_only;
            this.collect_on_fail = collect_on_fail;
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Indicates that the {@link Collect} should apply the given lookback before calling the
         * action (i.e. pass (and potentially pop) this many more items from the stack (compared to
         * the amount of items pushed by child parser) to the action).
         */
        public CollectBuilder lookback (int lookback)
        {
            if (this.lookback != 0) throw new IllegalStateException(
                "Trying to redefine the lookback on rule wrapper holding: " + parser);

            return new CollectBuilder(this.parser, lookback, this.peek_only, this.collect_on_fail);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Indicates that the items pushed on the stack by the child parser should not be popped
         * off the stack before calling the action (the items pushed on the stack by the child are
         * still passed as an array to the action, however).
         */
        public CollectBuilder peek_only()
        {
            if (peek_only) throw new IllegalStateException(
                "Attempting to set the peek_only property twice on rule wrapper holding: "
                    + parser);

            return new CollectBuilder(this.parser, lookback, true, this.collect_on_fail);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Indicates that the {@link Collect} parser should also be run even if its child parser
         * fails (meaning it always succeeds).
         */
        public CollectBuilder also_on_fail ()
        {
            if (collect_on_fail) throw new IllegalStateException(
                "Attempting to set the collect_on_fail property twice on rule wrapper holding: "
                    + parser);

            return new CollectBuilder(this.parser, lookback, this.peek_only, true);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser, performing a simple collect
         * action ({@link StackAction.ActionWithParse}).
         */
        public rule action (StackAction.ActionWithParse action)
        {
            return new rule(new Collect("collect", parser, lookback, collect_on_fail,
                !peek_only, action));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser, performing a string-capturing
         * collect action ({@link StackAction.ActionWithString}).
         */
        public rule action_with_string (StackAction.ActionWithString action)
        {
            return new rule(new Collect("collect_with_string", parser, lookback, collect_on_fail,
                !peek_only, action));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser, performing a list-capturing
         * collect action ({@link StackAction.ActionWithList}).
         */
        public rule action_with_list (StackAction.ActionWithList action)
        {
            return new rule(new Collect("collect_with_list", parser, lookback, collect_on_fail,
                !peek_only, action));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser, performing a simple pushing collect
         * action ({@link StackAction.Push}).
         */
        public rule push (StackAction.Push action)
        {
            return new rule(new Collect("push", parser, lookback, collect_on_fail,
                !peek_only, action));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser that pushes the string matched
         * by the parser onto the value stack.
         */
        public rule push_string_match () {
            return new rule(new Collect("push_string_match", parser, lookback, collect_on_fail,
                !peek_only, (StackAction.ActionWithString) (p, xs, str) -> p.stack.push(str)));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser that pushes the sublist matched
         * by the parser onto the value stack.
         */
        public rule push_list_match ()
        {
            return new rule(new Collect("push_list_match", parser, lookback, collect_on_fail,
                !peek_only, (StackAction.ActionWithString) (p, xs, lst) -> p.stack.push(lst)));
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Returns a {@link Collect} parser wrapping the parser. The action consists of pushing a
         * list of all collected items onto the stack, casted to the type denoted by {@code klass}.
         */
        public <T> rule as_list(Class<T> klass) {
            return new rule(new Collect("as_list", parser, lookback, collect_on_fail, !peek_only,
                (StackAction.PushWithParse) (p, xs) -> Arrays.asList(Util.<T[]>cast(xs))));
        }
    }

    // =============================================================================================
    // =============================================================================================
    // =============================================================================================

    /**
     * Base class for {@link LeftExpressionBuilder} and {@link RightExpressionBuilder}.
     */
    public abstract class ExpressionBuilder <Self extends ExpressionBuilder<Self>>
    {
        // -----------------------------------------------------------------------------------------

        final boolean left_associative;
        final boolean require_operator;
        final Parser left;
        final Parser right;
        final Parser[] infixes;
        final StackAction[] infix_steps;
        final Parser[] affixes;
        final StackAction[] affix_steps;

        // -----------------------------------------------------------------------------------------

        ExpressionBuilder (
            boolean left_associative, boolean require_operator,
            Parser left, Parser right,
            Parser[] infixes, StackAction[] infix_steps,
            Parser[] affixes, StackAction[] affix_steps)
        {
            this.left_associative = left_associative;
            this.left = left;
            this.right = right;
            this.infixes = infixes;
            this.infix_steps = infix_steps;
            this.affixes = affixes;
            this.affix_steps = affix_steps;
            this.require_operator = require_operator;
        }

        // -----------------------------------------------------------------------------------------

        ExpressionBuilder (boolean left_associative) {
            this(
                left_associative,
                false, null, null,
                new Parser[0], new StackAction[0],
                new Parser[0], new StackAction[0]
            );
        }

        // -----------------------------------------------------------------------------------------

        abstract Self copy (
            boolean require_other_side,
            Parser right, Parser left,
            Parser[] infixes, StackAction[] infix_steps,
            Parser[] affixes, StackAction[] affix_steps);


        // -----------------------------------------------------------------------------------------

        /**
         * Construct the parser and returns a {@link rule} wrapping it.
         */
        public abstract rule get();

        // -----------------------------------------------------------------------------------------

        /**
         * Define the left and right operand.
         */
        public Self operand (rule op)
        {
            if (this.left != null)
                throw new IllegalStateException("Trying to redefine the left operand.");
            if (this.right != null)
                throw new IllegalStateException("Trying to redefine the right operand.");

            return copy(
                require_operator, op.get(), op.get(), infixes, infix_steps, affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define an infix operator, along with the corresponding step action.
         */
        public Self infix (rule op, StackAction.Push step)
        {
            Parser[] ops = NArrays.append(this.infixes, op.get());
            StackAction[] op_steps = NArrays.append(this.infix_steps, step);
            return copy(require_operator, right, left, ops, op_steps, affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        Self _left (rule left)
        {
            if (this.left != null)
                throw new IllegalStateException("Trying to redefine the left operand.");

            return copy(
                require_operator, right, left.get(), infixes, infix_steps, affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        Self _right (rule right)
        {
            if (this.right != null)
                throw new IllegalStateException("Trying to redefine the right operand.");

            return copy(
                require_operator, right.get(), left, infixes, infix_steps, affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        Self affix (rule op, StackAction.Push step)
        {
            Parser[] affixes = NArrays.append(this.affixes, op.get());
            StackAction[] affix_steps = NArrays.append(this.affix_steps, step);
            return copy(require_operator, right, left, infixes, infix_steps, affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        Self require_operator()
        {
            if (require_operator)
                throw new IllegalStateException("Specifiying that an operator is required twice.");

            return copy(true, right, left, infixes, infix_steps, affixes, affix_steps);
        }
    }

    // =============================================================================================

    /**
     * Helps build a {@link LeftExpression} parser.
     */
    public final class LeftExpressionBuilder extends ExpressionBuilder<LeftExpressionBuilder>
    {
        LeftExpressionBuilder () {
            super(true);
        }

        // -----------------------------------------------------------------------------------------

        LeftExpressionBuilder (
            boolean left_associative,
            Parser left, Parser right,
            Parser[] ops, StackAction[] op_steps,
            Parser[] affixes, StackAction[] affix_steps,
            boolean require_other_side)
        {
            super(
                left_associative,
                require_other_side, left, right,
                ops, op_steps,
                affixes, affix_steps);
        }

        // -----------------------------------------------------------------------------------------

        @Override LeftExpressionBuilder copy (
            boolean require_other_side,
            Parser right, Parser left,
            Parser[] infixes, StackAction[] infix_steps,
            Parser[] affixes, StackAction[] affix_steps)
        {
            return new LeftExpressionBuilder(
                left_associative,
                left, right,
                infixes, infix_steps,
                affixes, affix_steps,
                require_other_side);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define the left operand.
         */
        public LeftExpressionBuilder left (rule left) {
            return _left(left);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define the right operand.
         */
        public LeftExpressionBuilder right (rule right) {
            return _right(right);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define a suffix operator, along with the corresponding step action.
         */
        public LeftExpressionBuilder suffix (rule op, StackAction.Push step) {
            return affix(op, step);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Specifies that an operator match is required, so a left operand cannot match on its own.
         */
        @Override public LeftExpressionBuilder require_operator() {
            return super.require_operator();
        }

        // -----------------------------------------------------------------------------------------

        @Override public rule get()
        {
            if (left == null)
                throw new IllegalStateException(
                    "No left operand specified for a left-associative expression.");

            if (right == null && infixes.length > 0)
                throw new IllegalStateException(
                    "No right operand specified for a left-associative expression, "
                        + "but operators have been defined.");

            if (require_operator && infixes.length == 0 && affixes.length == 0)
                throw new IllegalStateException(
                    "Right-side required but no prefix or operator has been defined.");

            if (infixes.length == 1 && affixes.length == 0)
                return new rule(new LeftFold(
                    left, infixes[0], right, require_operator, infix_steps[0]));

            if (infixes.length == 0 && affixes.length == 1)
                return new rule(new LeftFold(
                    left, affixes[0], empty.get(), require_operator, affix_steps[0]));

            return rule(new LeftExpression(
                left, right, infixes, infix_steps, affixes, affix_steps, require_operator));
        }
    }

    // =============================================================================================

    /**
     * Helps build a {@link RightExpression} parser.
     */
    public final class RightExpressionBuilder extends ExpressionBuilder<RightExpressionBuilder>
    {
        RightExpressionBuilder () {
            super(false);
        }

        // -----------------------------------------------------------------------------------------

        RightExpressionBuilder (
            boolean left_associative,
            Parser left, Parser right,
            Parser[] ops, StackAction[] op_steps,
            Parser[] affixes, StackAction[] affix_steps,
            boolean require_other_side)
        {
            super(
                left_associative,
                require_other_side, left, right,
                ops, op_steps,
                affixes, affix_steps
            );
        }

        // -----------------------------------------------------------------------------------------

        @Override RightExpressionBuilder copy (
            boolean require_other_side, Parser right, Parser left,
            Parser[] infixes, StackAction[] infix_steps,
            Parser[] affixes, StackAction[] affix_steps)
        {
            return new RightExpressionBuilder(
                left_associative,
                left, right,
                infixes, infix_steps,
                affixes, affix_steps,
                require_other_side);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define the left operand.
         *
         * <p>Beware that defining different parsers for the left and right operands that may
         * nonetheless call the same parser(s) may cause significant parse performance degradation.
         *
         * <p>Prefer using {@link #operand(rule)}, or call this method with a parser that memoizes
         * the repeated parser(s).
         */
        public RightExpressionBuilder _maybe_slow_left (rule left) {
            return _left(left);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define the right operand.
         *
         * <p>Beware that defining different parsers for the left and right operands that may
         * nonetheless call the same parser(s) may cause significant parse performance degradation.
         *
         * <p>Prefer using {@link #operand(rule)}, or call this method with a parser that memoizes
         * the repeated parser(s).
         */
        public RightExpressionBuilder _maybe_slow_right (rule right) {
            return _right(right);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Define a prefix operator, along with the corresponding step action.
         */
        public RightExpressionBuilder prefix (rule op, StackAction.Push step) {
            return affix(op, step);
        }

        // -----------------------------------------------------------------------------------------

        /**
         * Specifies that an operator match is required, so a right operand cannot match on its own.
         */
        @Override public RightExpressionBuilder require_operator() {
            return super.require_operator();
        }

        // -----------------------------------------------------------------------------------------

        @Override public rule get()
        {
            if (right == null)
                throw new IllegalStateException(
                    "No right operand specified for a right-associative expression.");

            if (left == null && infixes.length > 0)
                throw new IllegalStateException(
                    "No left operand specified for a right-associative expression, "
                        + "but operators have been defined.");

            if (require_operator && infixes.length == 0 && affixes.length == 0)
                throw new IllegalStateException(
                    "Left-side required but no prefix or operator has been defined.");

            if (infixes.length == 1 && affixes.length == 0)
                return new rule(new RightFold(
                    left, infixes[0], right, require_operator, infix_steps[0]));

            if (infixes.length == 0 && affixes.length == 1)
                return new rule(new RightFold(
                    empty.get(), affixes[0], right, require_operator, affix_steps[0]));

            return rule(new RightExpression(
                left, right, infixes, infix_steps, affixes, affix_steps, require_operator));
        }
    }

    // =============================================================================================
    // =============================================================================================
    // =============================================================================================
}
