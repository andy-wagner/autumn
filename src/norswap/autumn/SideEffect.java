package norswap.autumn;

/**
 * TODO
 */
public final class SideEffect
{
    // ---------------------------------------------------------------------------------------------

    public final Runnable apply;

    // ---------------------------------------------------------------------------------------------

    public final Runnable undo;

    // ---------------------------------------------------------------------------------------------

    public SideEffect (Runnable apply, Runnable undo)
    {
        this.apply = apply;
        this.undo = undo;
    }

    // ---------------------------------------------------------------------------------------------
}