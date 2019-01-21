package norswap.autumn;

import norswap.autumn.util.ArrayStack;
import norswap.utils.Vanilla;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of side-effects that have been applied during this parse. New side-effects
 * are appended at the end.
 *
 * <p>Usually, this is only modified through the {@link #apply} methods. Parsers automatically
 * undo side-effects on failure through {@link #rollback}. A list of recently applied
 * side-effects can be acquired through {@link #delta}.
 */
public final class Log extends ArrayStack<SideEffect.Applied>
{
    // ---------------------------------------------------------------------------------------------

    Log () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Applies the given side-effect and adds it to the log of applied side effects.
     */
    public void apply (SideEffect effect)
    {
        add(effect.apply());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Applies a list of side-effects in order. Usually the list was obtained by a previous call to
     * {@link #delta}.
     */
    public void apply (List<SideEffect> delta)
    {
        delta.forEach(this::apply);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Rollback logged side effects in reverse order of application until the log size is {@code
     * log_target_size}.
     */
    public void rollback (int log_target_size)
    {
        for (int i = size(); i > log_target_size; --i)
            pop().undo.run();
    }

    // ---------------------------------------------------------------------------------------------


    /**
     * Returns a list of side effects (without undo functions!) whose index {@code i} are such that
     * {@code log_start_index <= i < log.size()}, in increasing index order.
     */
    public List<SideEffect> delta (int log_start_index)
    {
        return Vanilla.map(from(log_start_index), it -> it.effect);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a list of applied side effects (with undo function) whose index {@code i} are such
     * that {@code log_start_index <= i < log.size()}, in increasing index order.
     */
    public List<SideEffect.Applied> delta_applied (int log_start_index)
    {
        return new ArrayList<>(from(log_start_index));
    }

    // ---------------------------------------------------------------------------------------------
}
