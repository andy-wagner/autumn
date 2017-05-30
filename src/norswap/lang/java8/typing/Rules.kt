package norswap.lang.java8.typing
import norswap.uranium.*
import kotlin.collections.listOf as list
import norswap.lang.java8.ast.*
import norswap.lang.java8.resolution.resolved

// =================================================================================================

fun Reactor.install_java8_typing_rules()
{
    add_visitor(LiteralRule)
    add_visitor(NotRule)
    add_visitor(ComplementRule)
    add_visitor(UnaryArithRule)
    add_visitor(BinaryArithRule)
    add_visitor(ShiftRule)
    add_visitor(OrderingRule)
    add_visitor(InstanceofRule)
    add_visitor(EqualRule)
    add_visitor(BitwiseRule)
    add_visitor(LogicalRule)
}

// =================================================================================================

abstract class TypingRule <N: Node>: Rule<N>()
{
    override fun provided (node: N)
        = list(Attribute(node, "type"))
}

// -------------------------------------------------------------------------------------------------

abstract class UnaryTypingRule <N: UnaryOp> : TypingRule<N>()
{
    override fun consumed (node: N) = list(Attribute(node.operand, "type"))
}

// -------------------------------------------------------------------------------------------------

abstract class BinaryOpRule: TypingRule<BinaryOp>()
{
    override fun consumed (node: BinaryOp) = list(
        Attribute(node.left,  "type"),
        Attribute(node.right, "type"))
}

// =================================================================================================

object LiteralRule: TypingRule<Literal>()
{
    override val domain
        = list(Literal::class.java)

    override fun Reaction<Literal>.compute()
    {
        node.typea = when (node.value) {
            is String   -> TString
            is Int      -> TInt
            is Long     -> TLong
            is Float    -> TFloat
            is Double   -> TDouble
            is Char     -> TChar
            is Boolean  -> TBool
            else        -> throw Error("unknown literal type")
        }
    }
}

// -------------------------------------------------------------------------------------------------

object NotRule: UnaryTypingRule<Not>()
{
    override val domain = list(Not::class.java)

    override fun Reaction<Not>.compute()
    {
        val op_type = node.operand.typea.unboxed

        if (op_type === TBool)
            node.typea = TBool
        else
            report(NotTypeError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object ComplementRule: UnaryTypingRule<Complement>()
{
    override val domain = list(Complement::class.java)

    override fun Reaction<Complement>.compute()
    {
        val op_type = node.operand.typea.unboxed

        if (op_type is IntegerType)
            node.typea = unary_promotion(op_type)
        else
            report(ComplementTypeError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object UnaryArithRule: UnaryTypingRule<UnaryOp>()
{
    override val domain = list(
        UnaryPlus    ::class.java,
        UnaryMinus   ::class.java)

    override fun Reaction<UnaryOp>.compute()
    {
        val op_type = node.operand.typea.unboxed

        if (op_type is NumericType)
            node.typea = unary_promotion(op_type)
        else
            report(UnaryArithTypeError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object BinaryArithRule: BinaryOpRule()
{
    override val domain = list(
        Product         ::class.java,
        Division        ::class.java,
        Remainder       ::class.java,
        Sum             ::class.java,
        Diff            ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea.unboxed
        val rt = node.right.typea.unboxed

        if (node is Sum && (lt === TString || rt === TString))
            return run { node.typea = TString }

        if (lt is NumericType && rt is NumericType)
            node.typea = binary_promotion(lt, rt)
        else
            report(BinaryArithTypeError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object ShiftRule: BinaryOpRule()
{
    override val domain = list(
        ShiftLeft           ::class.java,
        ShiftRight          ::class.java,
        BinaryShiftRight    ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea.unboxed
        val rt = node.right.typea.unboxed

        if (lt is IntegerType && rt is IntegerType)
            node.typea = unary_promotion(lt)
        else
            report(ShiftTypeError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object OrderingRule: BinaryOpRule()
{
    override val domain = list(
        Greater         ::class.java,
        GreaterEqual    ::class.java,
        Lower           ::class.java,
        LowerEqual      ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea.unboxed
        val rt = node.right.typea.unboxed

        if (lt !is NumericType || rt !is NumericType)
            report(OrderingTypeError(node))
        else
            node.typea = TBool
    }
}

// -------------------------------------------------------------------------------------------------

object InstanceofRule: TypingRule<Instanceof>()
{
    override val domain = list(Instanceof::class.java)

    override fun consumed(node: Instanceof) = list(
        Attribute(node.op,   "type"),
        Attribute(node.type, "resolved"))

    override fun Reaction<Instanceof>.compute()
    {
        val op_type = node.op.typea
        val type    = node.type.resolved

        if (op_type !is RefType)
            return report(InstanceofValueError(node))
        if (type !is RefType)
            return report(InstanceofTypeError(node))
        if (!type.reifiable())
            return report(InstanceofReifiableError(node))

        if (cast_compatible(op_type, type))
            node.typea = TBool
        else
            report(InstanceofCompatError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object EqualRule: BinaryOpRule()
{
    override val domain = list(
        Equal       ::class.java,
        NotEqual    ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea
        val rt = node.right.typea
        val ltu = lt.unboxed
        val rtu = rt.unboxed

        if (ltu is NumericType && rtu is NumericType)
            return run { node.typea = TBool }

        if (ltu === TBool && rtu === TBool)
            return run { node.typea = TBool }

        if (ltu is PrimitiveType && rtu is PrimitiveType)
            return report(EqualNumBoolError(node))

        if (lt is PrimitiveType || rt is PrimitiveType)
            return report(EqualPrimRefError(node))

        if (cast_compatible(lt, rt))
            node.typea = TBool
        else
            report(EqualCompatError(node))
    }
}

// -------------------------------------------------------------------------------------------------

object BitwiseRule: BinaryOpRule()
{
    override val domain = list(
        BinaryAnd   ::class.java,
        Xor         ::class.java,
        BinaryOr    ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea.unboxed
        val rt = node.right.typea.unboxed

        if (lt === TBool && rt === TBool)
            return run { node.typea = TBool }

        if (lt === TBool || rt === TBool)
            return report(BitwiseMixedError(node))

        if (lt !is IntegerType || rt !is IntegerType)
            return report(BitwiseRefError(node))

        node.typea = binary_promotion(lt, rt)
    }
}

// -------------------------------------------------------------------------------------------------

object LogicalRule: BinaryOpRule()
{
    override val domain = list(
        And         ::class.java,
        Or          ::class.java)

    override fun Reaction<BinaryOp>.compute()
    {
        val lt = node.left .typea.unboxed
        val rt = node.right.typea.unboxed

        if (lt !== TBool || rt !== TBool)
            report(LogicalTypeError(node))
        else
            node.typea = TBool
    }
}

// -------------------------------------------------------------------------------------------------