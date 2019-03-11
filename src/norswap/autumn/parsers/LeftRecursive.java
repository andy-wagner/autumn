package norswap.autumn.parsers;

import norswap.autumn.DSL;
import norswap.autumn.Parse;
import norswap.autumn.Parser;
import norswap.autumn.ParserVisitor;
import norswap.autumn.SideEffect;
import norswap.autumn.StackAction;
import norswap.autumn.util.ArrayStack;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * <b>Warning:</b> We strongly advise against using this — use {@link LeftAssoc} and {@link
 * RightAssoc} instead, via {@link DSL#left(Object, Object, StackAction.Push)} and {@link
 * DSL#right(Object, Object, StackAction.Push)} (and variants).
 *
 * A left-recursion capable parser. The child parser passed to this parser must left-recurse
 * only through a {@link LazyParser} reference to the {@link LeftRecursive} parser! The
 * methods {@link DSL#left_recursive(Function)} and {@link DSL#left_recursive_left_assoc(Function)}
 * can automate this setup.
 *
 * <p>Left-recursive rules that are also right-recursive may be parsed right-associatively (the
 * default) or left-associatively.
 *
 * <p>It is almost impossible to produce a satisfying definition of what "left-associative" means
 * in the context of PEG and Autumn (see my PhD thesis for more details). Instead, we choose to
 * define left-associative as "within a non-left recursion, no further recursion is permitted".
 * This ensures that non-left recursions an only ever match the (non-recursive) "base cases"
 * for the expression.
 *
 * <p>In brief, here is how left-recursion handling works:
 * <ol>
 * <li>The child parser is run. All left-recursive calls (i.e. nested calls to the parser at the
 * same input position) will fail immediately.</li>
 *
 * <li>After successfully parsing the child, we record its result (final input position, side
 * effects), then re-invoke it; but this time left-recursive calls will incur the result of the
 * previous successful invocation of the child.</li>
 *
 * <li>This process repeats itself until either the child fails, or the result's input position
 * stops growing.</li>
 *
 * <li>The final result will thus be that of the largest successful child parser invocation.</li>
 * </ol>
 *
 * <p>Also emember that if the parse is left-associative, further recursions during a non-left
 * recursions will fail.</p>
 */
public final class LeftRecursive extends Parser
{
    // ---------------------------------------------------------------------------------------------

    public final Parser child;

    // ---------------------------------------------------------------------------------------------

    public boolean left_associative;

    // ---------------------------------------------------------------------------------------------

    // TODO: not reusable by different threads! must create global state for each instance
    // - should state changes be logged? no -> they're fully local
    // - what about escape hatch -> same

    private final ArrayStack<Invocation> invocations = new ArrayStack<>();

    private int recursions = 0;

    // ---------------------------------------------------------------------------------------------

    public LeftRecursive (Parser child, boolean left_associative) {
        this.child = child;
        this.left_associative = left_associative;
    }

    // ---------------------------------------------------------------------------------------------

    @Override protected boolean doparse (Parse parse)
    {
        int pos0 = parse.pos;
        int log0 = parse.log.size();

        // left-associative expressions: block recursion from right-recursions
        if (left_associative && recursions == 2)
            return false;

        Invocation invoc = invocations.snoop();

        // if this is a left-recursion, a seed must exist at the current position
        if (invoc != null && invoc.pos0 == pos0)
        {
            // failed seed
            if (invoc.delta == null) return false;

            // seed match
            parse.pos = invoc.end_pos;
            parse.log.apply(invoc.delta);
            return true;
        }

        // left-associative expressions: this is a right-recursion, prevent further recursions
        if (left_associative && recursions == 1) {
            recursions = 2;
            return child.parse(parse);
        }

        // enter an initial failed seed
        invoc = new Invocation(pos0, -1, null);
        invocations.push(invoc);
        recursions = 1;

        // iteratively grow the seed
        while (child.parse(parse) && parse.pos > invoc.end_pos)
        {
            recursions = 1;
            invoc.end_pos = parse.pos;
            invoc.delta = parse.log.delta(log0);
            parse.pos = pos0;
            parse.log.rollback(log0);
        }

        recursions = 0;
        parse.pos = pos0;
        parse.log.rollback(log0);

        invocations.pop();
        if (invoc.delta == null)
            return false;

        parse.pos = invoc.end_pos;
        parse.log.apply(invoc.delta);
        return true;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void accept (ParserVisitor visitor) {
        visitor.visit(this);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Iterable<Parser> children () {
        return Collections.singletonList(child);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void set_rule (String name)
    {
        child.set_rule(name + "(leftrec child)");
        super.set_rule(name);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toStringFull ()
    {
        return child.rule() != null
            ? "left_recursive(" + child + ")"
            : rule() != null
                ? rule()
                : "anonymous left_recursive";
    }

    // ---------------------------------------------------------------------------------------------

    private final class Invocation
    {
        public final int pos0;
        public int end_pos;
        public List<SideEffect> delta;

        private Invocation (int pos0, int end_pos, List<SideEffect> delta)
        {
            this.pos0 = pos0;
            this.end_pos = end_pos;
            this.delta = delta;
        }
    }

    // ---------------------------------------------------------------------------------------------
}
