package norswap.autumn.parsers;

import norswap.autumn.Parse;
import norswap.autumn.Parser;
import norswap.autumn.ParserCallFrame;
import norswap.autumn.ParserVisitor;
import java.util.ArrayDeque;
import java.util.Collections;

/**
 * Succeeds only if its child fails.
 */
public final class Not extends Parser
{
    // ---------------------------------------------------------------------------------------------

    public final Parser child;

    // ---------------------------------------------------------------------------------------------

    public Not (Parser child)
    {
        this.child = child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean doparse (Parse parse)
    {
        int err0 = parse.error;
        ArrayDeque<ParserCallFrame> stk0 = parse.error_call_stack_mutable();
        // if the child matches, #parse will undo its side effects
        boolean success = !child.parse(parse);
        // negated parsers should not count towards the furthest error
        parse.error = err0;
        parse.set_error_call_stack(stk0);
        return success;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void accept (ParserVisitor visitor) {
        visitor.visit(this);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Iterable<Parser> children() {
        return Collections.singletonList(child);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toStringFull()
    {
        return "not(" + child + ")";
    }

    // ---------------------------------------------------------------------------------------------
}
