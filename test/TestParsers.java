import norswap.autumn.Parse;
import norswap.autumn.ParseResult;
import norswap.autumn.Parser;
import norswap.autumn.StackAction;
import norswap.autumn.parsers.*;
import norswap.utils.Slot;
import org.testng.annotations.Test;

import java.util.Arrays;

import static norswap.utils.Vanilla.list;
import static norswap.utils.Vanilla.peek_index;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public final class TestParsers
{
    // ---------------------------------------------------------------------------------------------

    private Parser parser;

    // ---------------------------------------------------------------------------------------------

    private ParseResult result;

    // ---------------------------------------------------------------------------------------------

    private void success (String string)
    {
        prefix(string, string.length());
    }

    // ---------------------------------------------------------------------------------------------

    private void success (String string, Object top)
    {
        result = Parse.run(parser, string, null);
        assertTrue(result.full_match);
        assertEquals(result.value_stack.size(), 1);
        assertEquals(result.top_value(), top);
    }

    // ---------------------------------------------------------------------------------------------

    private void prefix (String string)
    {
        result = Parse.run(parser, string, null);
        assertTrue(result.success);
    }

    // ---------------------------------------------------------------------------------------------

    private void prefix (String string, int size)
    {
        result = Parse.run(parser, string, null);
        assertTrue(result.success);
        assertEquals(result.match_size, size);
    }

    // ---------------------------------------------------------------------------------------------

    private void failure (String string)
    {
        result = Parse.run(parser, string, null);
        assertFalse(result.success);
    }

    // ---------------------------------------------------------------------------------------------

    private void failure (String string, int position)
    {
        result = Parse.run(parser, string, null);
        assertFalse(result.success);
        assertEquals(result.error_position, position);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void char_predicate()
    {
        parser = CharPredicate.any();
        success("a");
        success("_");
        failure("\0");

        parser = CharPredicate.single('a');
        success("a");
        failure("b");

        parser = CharPredicate.alpha();
        success("a");
        success("A");
        failure("1");
        
        parser = CharPredicate.alphanum();
        success("a");
        success("1");
        failure("_");

        parser = CharPredicate.digit();
        success("1");
        failure("a");

        parser = CharPredicate.octal_digit();
        success("0");
        success("7");
        failure("8");
        failure("a");

        parser = CharPredicate.hex_digit();
        success("a");
        success("f");
        success("F");
        failure("g");
        failure("G");
        success("1");

        parser = CharPredicate.range('a', 'z');
        success("a");
        failure("1");

        parser = CharPredicate.set('a', 'b');
        success("a");
        success("b");
        failure("c");

        parser = CharPredicate.set("ab");
        success("a");
        success("b");
        failure("c");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void object_predicate()
    {
        parser = ObjectPredicate.any();

        assertTrue(Parse.run(parser, list(new Object()), null).full_match);
        assertFalse(Parse.run(parser, list((Object) null), null).success);

        parser = ObjectPredicate.instance(String.class);

        assertTrue(Parse.run(parser, list(""), null).full_match);
        assertFalse(Parse.run(parser, list(3), null).success);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void optional()
    {
        parser = new Optional(CharPredicate.alpha());
        success("a");
        success("");
        prefix("_", 0);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void string_match()
    {
        parser = new StringMatch("foo", null);
        success("foo");
        prefix("foobar", 3);
        failure("bar");

        parser = new StringMatch("foo", new Optional(CharPredicate.single(' ')));
        success("foo");
        success("foo ");
        failure("bar");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void sequence()
    {
        parser = new Sequence(
            CharPredicate.single('a'),
            CharPredicate.single('b'),
            CharPredicate.single('c'));
        success("abc");
        failure("bbc", 0);
        failure("aac", 1);
        failure("aba", 2);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void repeat()
    {
        parser = new Repeat(0, false, CharPredicate.single('a'));
        success("");
        success("a");
        success("aaa");
        prefix("", 0);
        prefix("b", 0);
        prefix("aab", 2);

        parser = new Repeat(1, false, CharPredicate.single('a'));
        success("a");
        success("aaa");
        failure("");
        failure("b");
        prefix("aab", 2);

        parser = new Repeat(3, false, CharPredicate.single('a'));
        success("aaa");
        success("aaaa");
        failure("");
        failure("aa", 2);

        parser = new Repeat(3, true, CharPredicate.single('a'));
        success("aaa");
        failure("");
        failure("aa", 2);
        prefix("aaaa", 3);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void around()
    {
        parser = new Around(0, false, false, CharPredicate.single('a'), CharPredicate.single(','));
        success("");
        success("a");
        success("a,a");
        prefix("a,", 1);
        prefix("b", 0);

        parser = new Around(1, false, false, CharPredicate.single('a'), CharPredicate.single(','));
        success("a");
        success("a,a");
        failure("");
        prefix("a,", 1);
        prefix("a,b", 1);

        parser = new Around(0, false, true, CharPredicate.single('a'), CharPredicate.single(','));
        success("");
        success("a");
        success("a,");
        success("a,a");
        success("a,a,");
        success(",");
        prefix("b", 0);
        prefix("a,b", 2);

        parser = new Around(1, false, true, CharPredicate.single('a'), CharPredicate.single(','));
        success("a");
        success("a,");
        success("a,a");
        success("a,a,");
        failure("");
        failure(",");
        prefix("a,b", 2);

        parser = new Around(3, false, false, CharPredicate.single('a'), CharPredicate.single(','));
        success("a,a,a");
        success("a,a,a,a");
        failure("a,a", 3);
        failure("a,a,", 4);

        parser = new Around(3, true, false, CharPredicate.single('a'), CharPredicate.single(','));
        success("a,a,a");
        prefix("a,a,a,a", 5);
        failure("a,a", 3);
        failure("a,a,", 4);

        parser = new Around(3, true, true, CharPredicate.single('a'), CharPredicate.single(','));
        success("a,a,a");
        success("a,a,a,");
        prefix("a,a,a,a", 6);
        failure("a,a", 3);
        failure("a,a,", 4);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void choice()
    {
        parser = new Choice(CharPredicate.single('a'), CharPredicate.single('b'));
        success("a");
        success("b");
        failure("");
        failure("c", 0);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void longest()
    {
        Parser enda = new Sequence(
            CharPredicate.single('a'),
            new Repeat(1, false, new StringMatch("ba", null)));

        Parser endb = new Repeat(1, false, new StringMatch("ab", null));

        parser = new Longest(enda, endb);
        success("ab");
        success("aba");
        success("abab");
        failure("");
        failure("a", 1);
        prefix("abc", 2);
        prefix("abac", 3);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void lookahead()
    {
        parser = new Sequence(
            new Lookahead(CharPredicate.single('a')),
            CharPredicate.single('a'));
        success("a");
        prefix("a", 1);
        failure("");

        parser = new Sequence(
            new Lookahead(new StringMatch("ab", null)),
            CharPredicate.single('a'),
            CharPredicate.single('b'),
            CharPredicate.single('c'));
        success("abc");
        failure("ab", 2);
        failure("ac", 0);

        parser = new Lookahead(CharPredicate.single('a'));
        prefix("a", 0);
        failure("");
        failure("b");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void not()
    {
        parser = new Sequence(new Not(CharPredicate.single('a')), CharPredicate.single('b'));
        success("b");
        failure("a");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void lazy_parser()
    {
        Parser alpha = CharPredicate.alpha();
        parser = new LazyParser(() -> alpha);
        success("a");
        success("A");
        failure("1");
    }

    // ---------------------------------------------------------------------------------------------

    private static Parser a =
        new Collect("A", CharPredicate.single('a'), 0, false, true,
            (p,xs) -> p.push("a"));

    private static Parser b =
        new Collect("B", CharPredicate.single('b'), 0, false, true,
            (p,xs) -> p.push("b"));

    private static Parser aa =
        new Collect("AA", new StringMatch("aa", null), 0, false, true,
            (p, xs) -> p.push("aa"));

    // ---------------------------------------------------------------------------------------------

    private static void pair_concat (Parse parse, Object[] items) {
        parse.push("(" + items[0] + "," + items[1] + ")");
    }

    // ---------------------------------------------------------------------------------------------

    private static void pair_concat2 (Parse parse, String string, Object[] items) {
        parse.push("(" + string + ")");
    }

    // ---------------------------------------------------------------------------------------------

    private static void concat (Parse parse, Object[] items) {
        parse.push(Arrays.toString(items));
    }

    // ---------------------------------------------------------------------------------------------

    private static Object peek (ParseResult result, int index) {
        return peek_index(result.value_stack, index);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void collect()
    {
        parser = a;
        success("a", "a");

        parser = new Collect("as",
            new Sequence(a, CharPredicate.single(','), a),
            0, false, true,
            TestParsers::pair_concat);
        success("a,a", "(a,a)");

        parser = new Collect("as",
            new Sequence(a, CharPredicate.single(','), a),
            0, false, false,
            TestParsers::pair_concat);
        success("a,a");
        assertEquals(result.value_stack.size(), 3);
        assertEquals(result.top_value(), "(a,a)");
        assertEquals(peek(result, 1), "a");
        assertEquals(peek(result, 2), "a");

        parser = new Collect("as",
            new Sequence(a, CharPredicate.single(','), a),
            0, false, false,
            (StackAction.WithString) TestParsers::pair_concat2);
        success("a,a");
        assertEquals(result.value_stack.size(), 3);
        assertEquals(peek(result, 0), "(a,a)");
        assertEquals(peek(result, 1), "a");
        assertEquals(peek(result, 2), "a");

        // tests that a push is properly undone
        parser = new Sequence(
            new Collect("as",
                new Sequence(a, CharPredicate.single(','), a),
                0, false, true,
                TestParsers::pair_concat),
            new Choice());

        failure("a,a", 3);
        assertEquals(result.value_stack.size(), 0);

        // tests that pop is properly undone
        parser = new Sequence(
            a,
            new Optional(new Sequence(
                new Collect("ca", new Sequence(), 0, false, false,
                    (p,xs) -> p.pop()),
                new Choice())));

        success("a");
        assertEquals(result.value_stack.size(), 1);
        assertEquals(peek(result, 0), "a");

        // test lookback
        parser = new Sequence(
            new Collect("xxx", new StringMatch("xxx", null), 0, false, true,
                (p,xs) -> p.push("xxx")),
            new Collect("yyy", new StringMatch("yyy", null), 1, false, true,
                (p,xs) -> p.push(xs[0] + "yyy")));
        success("xxxyyy");
        assertEquals(result.value_stack.size(), 1);
        assertEquals(peek(result, 0), "xxxyyy");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void left_assoc()
    {
        parser = new LeftAssoc(
            b, CharPredicate.single(','), a, true,
            (p,xs) -> p.push("(" + xs[0] + "," + xs[1] + ")"));
        success("b,a", "(b,a)");
        success("b,a,a", "((b,a),a)");
        success("b,a,a,a", "(((b,a),a),a)");
        failure("");
        failure("b");
        failure("a");

        parser =  new LeftAssoc(
            b, CharPredicate.single(','), a, false,
            (p,xs) -> p.push("(" + xs[0] + "," + xs[1] + ")"));
        success("b", "b");
        success("b,a,a,a", "(((b,a),a),a)");
        failure("");
        failure("a");

        // check that side-effects from an operator are properly undone
        parser = new Collect("baba",
            new Sequence(
                new LeftAssoc(b, a, b, false, (p,xs) -> p.push("bab")),
                a),
            0, false, true,
            (p,xs) -> p.push("" + xs[0] + xs[1]));
        success("baba", "baba");
        success("ba", "ba");
        failure("bab");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void right_assoc()
    {
        parser = new RightAssoc(
            a, CharPredicate.single(','), b, true,
            (p,xs) -> p.push("(" + xs[0] + "," + xs[1] + ")"));
        success("a,b", "(a,b)");
        success("a,a,b", "(a,(a,b))");
        success("a,a,a,b", "(a,(a,(a,b)))");
        failure("");
        failure("b");
        failure("a");

        parser =  new RightAssoc(
            a, CharPredicate.single(','), b, false,
            (p,xs) -> p.push("(" + xs[0] + "," + xs[1] + ")"));
        success("b", "b");
        success("a,a,a,b", "(a,(a,(a,b)))");
        failure("");
        failure("a");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void backtracking()
    {
        parser = new Choice(
            new Sequence(a, a),
            new Sequence(CharPredicate.single('a'), b));
        success("ab", "b");

        parser = new Choice(
            new Repeat(4, false, a),
            new Sequence(new Repeat(0, false, CharPredicate.single('a')), b));
        success("aaab", "b");
    }

    // ---------------------------------------------------------------------------------------------

    // Test for the token cache.
    // Commented, as this API is not supposed to be publicly accessible.

//    @Test public void token_cache()
//    {
//        HashMap<Integer, TokenResult> map = new HashMap<>();
//        TokenCache cache = new TokenCache();
//        int N = 1000_000;
//        int RANGE = 10_000;
//        int NTOKENS = 100;
//        int SPAN = 100;
//        Random random = new Random();
//
//        for (int i = 0; i < N; ++i)
//        {
//            int pos = random.nextInt(RANGE);
//            TokenResult r = cache.get(pos);
//            assertEquals(r, map.get(pos));
//
//            if (r == null) {
//                TokenResult res = new TokenResult(
//                    random.nextInt(NTOKENS), pos, pos + random.nextInt(SPAN),
//                    Collections.emptyList());
//                cache.put(pos, res);
//                map.put(pos, res);
//            }
//        }
//    }

    // ---------------------------------------------------------------------------------------------

    @Test public void tokens()
    {
        Tokens tokens = new Tokens(a, b, aa);
        Parser a_  = tokens.token_parser(a);
        Parser b_  = tokens.token_parser(b);
        Parser aa_ = tokens.token_parser(aa);

        parser = new Collect("AABAB", new Sequence(aa_, b_, a_, b_), 0, false, true,
            TestParsers::concat);
        success("aabab", "[aa, b, a, b]");

        parser = new Sequence(a_, a_);
        failure("aa");

        parser = tokens.token_choice(a, b, aa);
        success("aa", "aa");
        success("b", "b");
        failure("c");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void left_recursive()
    {
        Slot<Parser> slot = new Slot<>();
        parser = new LeftRecursive(new Choice(
            new Sequence(new LazyParser(() -> slot.x), a),
            a));
        slot.x = parser;

        success("a");
        success("aa");
        success("aaa");

        failure("b", 0);
        failure("", 0);

        parser = new LeftRecursive(new Choice(
            new Sequence(new LazyParser(() -> parser), b),
            slot.x));

        success("ab");
        success("aaab");
        success("abbb");
        success("aaabbb");
        failure("b", 0);
    }

    // ---------------------------------------------------------------------------------------------
}
