package norswap.autumn;

import norswap.autumn.parsers.Collect;
import norswap.autumn.parsers.LeftAssoc;
import java.util.List;

/**
 * Functional interface for specifying actions on the value stack ({@link Parse#stack}).
 *
 * <p>The basic assumption is that stack actions are passed to some parsers. These parsers want to
 * run a child parser and do something with the items it pushed on the stack. (They can run
 * <i>multiple</i> child parsers, but we'll always refer to "the" child parser for simplicity's
 * sake).
 *
 * <p>It's important that any state change done by these actions be performed through {@link
 * Log#apply(SideEffect)} (or another such {@link Log} method).
 *
 * <p>The parsers that consume this interface are required to call <b>only</b> {@link #apply(Parse,
 * Object[], int, int)}, which is the most general method.
 *
 * <p>However, the actual method used for the functional notation is {@link #apply(Parse,
 * Object[])}, which is the most common form. Here is how you would implement a stack action in this
 * form and use it: {@code method_taking_stack_action((p,xs) -> { ... })}.
 *
 * <p>Other forms are available via the sub-interfaces {@link WithString} and {@link WithList}. They
 * give you access to the matched input (respectively when the input is a string and when it is a
 * list). To use these forms, you have to use a type cast:  {@code
 * method_taking_stack_action((StackAction.WithString) (p,str,xs) -> { ... })}.
 *
 * <p>A final form is available via the sub-interface {@link Push}: this form has to return
 * an object which is automatically pushed onto the stack.
 *
 * <p>Autumn itself supplies two consumers of stack actions: the {@link Collect} and {@link
 * LeftAssoc} parsers.
 */
@FunctionalInterface
public interface StackAction
{
    // ---------------------------------------------------------------------------------------------

    /**
     * The main method that consumer of stack actions will always call.
     *
     * @param items collected items from the stack, or null if the child parser failed.
     * @param pos0 the input position at which the child parser matched.
     * @param size0 the size of the stack before the child parser was called.
     */
    default void apply (Parse parse, Object[] items, int pos0, int size0)
    {
        apply(parse, items);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * @param items collected items from the stack, or null if the child parser failed.
     */
    void apply (Parse parse, Object[] items);

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link StackAction} to which the matched input list slice is provided.
     */
    @FunctionalInterface
    interface WithList extends StackAction
    {
        @Override default void apply (Parse parse, Object[] items, int pos0, int size0)
        {
            assert parse.list != null;
            apply(parse, items != null ? parse.list.subList(pos0, parse.pos) : null, items);
        }

        @Override default void apply (Parse parse, Object[] items) {
            throw new RuntimeException("Calling wrong apply overload.");
        }

        /**
         * @param match part of {@link Parse#list} matched by the child parser.
         * @param items collected items from the stack, or null if the child parser failed.
         */
        void apply (Parse parse, List<?> match, Object[] items);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link StackAction} to which the matched input string slice is provided.
     */
    @FunctionalInterface
    interface WithString extends StackAction
    {
        @Override default void apply (Parse parse, Object[] items, int pos0, int size0)
        {
            assert parse.string != null;
            apply(parse, items != null ? parse.string.substring(pos0, parse.pos) : null, items);
        }

        @Override default void apply (Parse parse, Object[] items) {
            throw new RuntimeException("Calling wrong apply overload.");
        }

        /**
         * @param match part of {@link Parse#string} matched by the child parser.
         * @param items collected items from the stack, or null if the child parser failed.
         */
        void apply (Parse parse, String match, Object[] items);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * A {@link StackAction} that automatically pushes a value on the value stack.
     */
    @FunctionalInterface
    interface Push extends StackAction
    {
        @Override default void apply (Parse parse, Object[] items, int pos0, int size0) {
            parse.stack.push(get(parse, items));
        }

        @Override default void apply (Parse parse, Object[] items) {
            throw new RuntimeException("Calling wrong apply overload.");
        }

        Object get (Parse parse, Object[] items);
    }

    // ---------------------------------------------------------------------------------------------
}