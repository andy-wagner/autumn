package norswap.autumn.parsers;

import norswap.autumn.Parse;
import norswap.autumn.Parser;
import norswap.autumn.ParserVisitor;
import norswap.autumn.StackAction;
import norswap.utils.ArrayListInt;
import java.util.Arrays;

/**
 * Matches a right-associative binary expression. See {@link #RightAssoc}.
 */
public final class RightAssoc extends Parser
{
    // ---------------------------------------------------------------------------------------------

    public final Parser left;

    // ---------------------------------------------------------------------------------------------

    public final Parser right;

    // ---------------------------------------------------------------------------------------------

    public final Parser operator;

    // ---------------------------------------------------------------------------------------------

    public final boolean operator_required;

    // ---------------------------------------------------------------------------------------------

    public final StackAction step;

    // ---------------------------------------------------------------------------------------------

    /**
     * Matches a right-associative binary expression (specified by {@code left}, {@code right} and
     * {@code operator}).
     *
     * @param operator_required specifies whether at least one operator should be present or if a
     * right-hand side alone is admissible.
     *
     * @param step is applied iteratively after the whole expression has been matched, with the
     * expected input for a right-associative parse: the input position and stack size are those
     * recorded when before parsing each left-hand side, in right-to-left order. If {@code step} is
     * null, no action is taken (though we should point out that using RightAssoc is uterly useless
     * in that case).
     */
    public RightAssoc (Parser left, Parser operator, Parser right,
                      boolean operator_required, StackAction step)
    {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.operator_required = operator_required;
        this.step = step;
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    protected boolean doparse (Parse parse)
    {
        // Stores alternate pairs of position and stack size recorded
        // before parsing a left-hand side.
        ArrayListInt stack = new ArrayListInt();
        stack.push(parse.pos);
        stack.push(parse.stack.size());

        int log0 = parse.log.size();

        while (left.parse(parse))
        {
            if (!operator.parse(parse)) {
                // rollback left operand
                parse.pos = stack.back(1);
                parse.rollback(log0);
                break;
            }

            log0 = parse.log.size();
            stack.push(parse.pos);
            stack.push(parse.stack.size());
        }

        // Always pop the last entry (the last operand is not a left-hand-side).
        stack.pop(2);

        if (operator_required && stack.size() == 0)
            return false;

        if (!right.parse(parse))
            return false;

        while (stack.size() > 0) {
            int size0 = stack.pop();
            int pos0  = stack.pop();
            step.apply(parse, parse.pop_from(size0), pos0, size0);
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void accept (ParserVisitor visitor) {
        visitor.visit(this);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Order: left, operator, right.
     */
    @Override public Iterable<Parser> children () {
        return Arrays.asList(left, operator, right);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toStringFull ()
    {
        StringBuilder b = new StringBuilder();
        b.append("right_assoc(");
        b.append(left)      .append(", ");
        b.append(operator)  .append(", ");
        b.append(right);
        if (operator_required)
            b.append(", operator_required");
        b.append(")");
        return b.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
