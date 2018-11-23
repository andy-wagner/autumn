package norswap.lang.java;

import norswap.autumn.DSL;
import norswap.lang.java.ast.*;
import norswap.utils.Pair;

import static java.util.Collections.emptyList;
import static norswap.lang.java.LexUtils.*;

public final class Grammar extends DSL
{
    /// LEXICAL ====================================================================================

    // Whitespace ----------------------------------------------------------------------------------

    public Wrapper space_char          = cpred(Character::isWhitespace);
    public Wrapper not_line            = seq(str("\n").not(), any);
    public Wrapper line_comment        = seq("//", not_line.at_least(0), str("\n").opt());

    public Wrapper not_comment_term    = seq(str("*/").not(), any);
    public Wrapper multi_comment       = seq("/*", not_comment_term.at_least(0), "*/");

    {
        ws = choice(space_char, line_comment, multi_comment).at_least(0).get();
        ws.exclude_error = true;
    }

    // Keywords and Operators ----------------------------------------------------------------------

    public Wrapper _boolean        = word("boolean")      .token();
    public Wrapper _byte           = word("byte")         .token();
    public Wrapper _char           = word("char")         .token();
    public Wrapper _double         = word("double")       .token();
    public Wrapper _float          = word("float")        .token();
    public Wrapper _int            = word("int")          .token();
    public Wrapper _long           = word("long")         .token();
    public Wrapper _short          = word("short")        .token();
    public Wrapper _void           = word("void")         .token();
    public Wrapper _abstract       = word("abstract")     .token();
    public Wrapper _default        = word("default")      .token();
    public Wrapper _final          = word("final")        .token();
    public Wrapper _native         = word("native")       .token();
    public Wrapper _private        = word("private")      .token();
    public Wrapper _protected      = word("protected")    .token();
    public Wrapper _public         = word("public")       .token();
    public Wrapper _static         = word("static")       .token();
    public Wrapper _strictfp       = word("strictfp")     .token();
    public Wrapper _synchronized   = word("synchronized") .token();
    public Wrapper _transient      = word("transient")    .token();
    public Wrapper _volatile       = word("volatile")     .token();
    public Wrapper _assert         = word("assert")       .token();
    public Wrapper _break          = word("break")        .token();
    public Wrapper _case           = word("case")         .token();
    public Wrapper _catch          = word("catch")        .token();
    public Wrapper _class          = word("class")        .token();
    public Wrapper _const          = word("const")        .token();
    public Wrapper _continue       = word("continue")     .token();
    public Wrapper _do             = word("do")           .token();
    public Wrapper _else           = word("else")         .token();
    public Wrapper _enum           = word("enum")         .token();
    public Wrapper _extends        = word("extends")      .token();
    public Wrapper _finally        = word("finally")      .token();
    public Wrapper _for            = word("for")          .token();
    public Wrapper _goto           = word("goto")         .token();
    public Wrapper _if             = word("if")           .token();
    public Wrapper _implements     = word("implements")   .token();
    public Wrapper _import         = word("import")       .token();
    public Wrapper _interface      = word("interface")    .token();
    public Wrapper _instanceof     = word("instanceof")   .token();
    public Wrapper _new            = word("new")          .token();
    public Wrapper _package        = word("package")      .token();
    public Wrapper _return         = word("return")       .token();
    public Wrapper _super          = word("super")        .token();
    public Wrapper _switch         = word("switch")       .token();
    public Wrapper _this           = word("this")         .token();
    public Wrapper _throws         = word("throws")       .token();
    public Wrapper _throw          = word("throw")        .token();
    public Wrapper _try            = word("try")          .token();
    public Wrapper _while          = word("while")        .token();

    public Wrapper BANG            = word("!")            .token();
    public Wrapper BANGEQ          = word("!=")           .token();
    public Wrapper PERCENT         = word("%")            .token();
    public Wrapper PERCENTEQ       = word("%=")           .token();
    public Wrapper AMP             = word("&")            .token();
    public Wrapper AMPAMP          = word("&&")           .token();
    public Wrapper AMPEQ           = word("&=")           .token();
    public Wrapper LPAREN          = word("(")            .token();
    public Wrapper RPAREN          = word(")")            .token();
    public Wrapper STAR            = word("*")            .token();
    public Wrapper STAREQ          = word("*=")           .token();
    public Wrapper PLUS            = word("+")            .token();
    public Wrapper PLUSPLUS        = word("++")           .token();
    public Wrapper PLUSEQ          = word("+=")           .token();
    public Wrapper COMMA           = word(",")            .token();
    public Wrapper SUB             = word("-")            .token();
    public Wrapper SUBSUB          = word("--")           .token();
    public Wrapper SUBEQ           = word("-=")           .token();
    public Wrapper EQ              = word("=")            .token();
    public Wrapper EQEQ            = word("==")           .token();
    public Wrapper QUES            = word("?")            .token();
    public Wrapper CARET           = word("^")            .token();
    public Wrapper CARETEQ         = word("^=")           .token();
    public Wrapper LBRACE          = word("{")            .token();
    public Wrapper RBRACE          = word("}")            .token();
    public Wrapper BAR             = word("|")            .token();
    public Wrapper BARBAR          = word("||")           .token();
    public Wrapper BAREQ           = word("|=")           .token();
    public Wrapper TILDE           = word("~")            .token();
    public Wrapper MONKEYS_AT      = word("@")            .token();
    public Wrapper DIV             = word("/")            .token();
    public Wrapper DIVEQ           = word("/=")           .token();
    public Wrapper GT              = word(">")            .token();
    public Wrapper LT              = word("<")            .token();
    public Wrapper GTEQ            = word(">=")           .token();
    public Wrapper LTEQ            = word("<=")           .token();
    public Wrapper LTLT            = word("<<")           .token();
    public Wrapper LTLTEQ          = word("<<=")          .token();
    public Wrapper GTGTEQ          = word(">>=")          .token();
    public Wrapper GTGTGTEQ        = word(">>>=")         .token();
    public Wrapper LBRACKET        = word("[")            .token();
    public Wrapper RBRACKET        = word("]")            .token();
    public Wrapper ARROW           = word("->")           .token();
    public Wrapper COL             = word(":")            .token();
    public Wrapper COLCOL          = word("::")           .token();
    public Wrapper SEMI            = word(";")            .token();
    public Wrapper DOT             = word(".")            .token();
    public Wrapper ELLIPSIS        = word("...")          .token();

    // These two are not tokens, because they would cause issue with nested generic types.
    // e.g. in List<List<String>>, you want ">>" to lex as [_GT, _GT]

    public Wrapper GTGT            = word(">>");
    public Wrapper GTGTGT          = word(">>>");

    public Wrapper _false          = word("false")          .as_val(false).token();
    public Wrapper _true           = word("true")           .as_val(true).token();
    public Wrapper _null           = word("null")           .as_val(Null.NULL).token();

    // Identifiers ---------------------------------------------------------------------------------

    public Wrapper id_start    = cpred(Character::isJavaIdentifierStart);
    public Wrapper id_part     = cpred(c -> c != 0 && Character.isJavaIdentifierPart(c));
    public Wrapper iden = seq(id_start, id_part.at_least(0))
        .collect_str((p,str,xs) -> p.push(Identifier.mk(str)))
        .word()
        .token();

    // Numerals - Common Parts ---------------------------------------------------------------------

    public Wrapper underscore  = str("_");
    public Wrapper dlit        = str(".");
    public Wrapper hex_prefix  = choice("0x", "0X");
    public Wrapper underscores = underscore.at_least(0);
    public Wrapper digits1     = digit.sep(1, underscores);
    public Wrapper digits0     = digit.sep(0, underscores);
    public Wrapper hex_digits  = hex_digit.sep(1, underscores);
    public Wrapper hex_num     = seq(hex_prefix, hex_digits);

    // Numerals - Floating Point -------------------------------------------------------------------

    public Wrapper hex_significand = choice(
        seq(hex_prefix, hex_digits.opt(), dlit, hex_digits),
        seq(hex_num, dlit.opt()));

    public Wrapper exp_sign_opt        = set("+-").opt();
    public Wrapper exponent            = seq(set("eE"), exp_sign_opt, digits1);
    public Wrapper binary_exponent     = seq(set("pP"), exp_sign_opt, digits1);
    public Wrapper float_suffix        = set("fFdD");
    public Wrapper float_suffix_opt    = float_suffix.opt();
    public Wrapper hex_float_lit       = seq(hex_significand, binary_exponent, float_suffix_opt);

    public Wrapper decimal_float_lit = choice(
        seq(digits1, dlit, digits0, exponent.opt(), float_suffix_opt),
        seq(dlit, digits1, exponent.opt(), float_suffix_opt),
        seq(digits1, exponent, float_suffix_opt),
        seq(digits1, exponent.opt(), float_suffix));

    public Wrapper float_literal = choice(hex_float_lit, decimal_float_lit)
        .collect_str((p,str,xs) -> p.push(parse_floating(str).unwrap()))
        .token();

    // Numerals - Integral -------------------------------------------------------------------------

    public Wrapper bit             = set("01");
    public Wrapper binary_prefix   = choice("0b", "0B");
    public Wrapper binary_num      = seq(binary_prefix, bit.at_least(1).sep(1, underscores));
    public Wrapper octal_num       = seq("0", seq(underscores, octal_digit).at_least(1));
    public Wrapper decimal_num     = choice("0", digits1);
    public Wrapper integer_num     = choice(hex_num, binary_num, octal_num, decimal_num);

    public Wrapper integer_literal = seq(integer_num, set("lL").opt())
        .collect_str((p,str,xs) -> p.push(parse_integer(str).unwrap()))
        .token();

    // Characters and Strings ----------------------------------------------------------------------

    public Wrapper octal_code_3    = seq(range('0', '3'), octal_digit, octal_digit);
    public Wrapper octal_code_2    = seq(octal_digit, octal_digit.opt());
    public Wrapper octal_code      = choice(octal_code_3, octal_code_2);
    public Wrapper unicode_code    = seq(str("u").at_least(1), hex_digit.repeat(4));
    public Wrapper escape_suffix   = choice(set("btnfr\"'\\"), octal_code, unicode_code);
    public Wrapper escape          = seq("\\", escape_suffix);
    public Wrapper naked_char      = choice(escape, seq(set("'\\\n\r").not(), any));
    public Wrapper nake_str_char   = choice(escape, seq(set("\"\\\n\r").not(), any));

    public Wrapper char_literal = seq("'", naked_char, "'")
        .collect_str((p,str,xs) -> p.push(parse_char(str).unwrap()))
        .token();

    public Wrapper string_literal = seq("\"", nake_str_char.at_least(0), "\"")
        .collect_str((p,str,xs) -> p.push(parse_string(str).unwrap()))
        .token();

    // Literal ----------------------------------------------------------------

    public Wrapper literal = seq(token_choice(
            integer_literal, string_literal, _null, float_literal, _true, _false, char_literal), ws)
        .push((p,xs) -> Literal.mk(xs[0]));

    // ---------------------------------------------------------------------------------------------
    {
        build_tokenizer();
    }
    // ---------------------------------------------------------------------------------------------

    /// ANNOTATIONS ================================================================================

    public Wrapper annotation_element = choice(
        lazy(() -> this.ternary),
        lazy(() -> this.annotation_element_list),
        lazy(() -> this.annotation));

    public Wrapper annotation_inner_list
        = lazy(() -> this.annotation_element).sep_trailing(0, COMMA);

    public Wrapper annotation_element_list
        = seq(LBRACE, annotation_inner_list, RBRACE)
        .push((p,xs) -> AnnotationElementList.mk(list(xs)));

    public Wrapper annotation_element_pair
        = seq(iden, EQ, annotation_element)
        .push((p,xs) -> new Pair<String, AnnotationElement>($(xs,0), $(xs,1)));

    public Wrapper normal_annotation_suffix
        = seq(LPAREN, annotation_element_pair.sep(1, COMMA), RPAREN)
        .push((p,xs) -> NormalAnnotation.mk($(p.pop()), list(xs)));

    public Wrapper single_element_annotation_suffix
        = seq(LPAREN, annotation_element, RPAREN)
        .lookback(1).push((p,xs) -> SingleElementAnnotation.mk($(xs,0), $(xs,1)));

    public Wrapper marker_annotation_suffix
        = seq(LPAREN, RPAREN).opt()
         .lookback(1).push((p,xs) -> MarkerAnnotation.mk($(xs,0)));

    public Wrapper annotation_suffix = choice(
        normal_annotation_suffix,
        single_element_annotation_suffix,
        marker_annotation_suffix);

    public Wrapper qualified_iden
        = iden.sep(1, DOT)
        .push((p, xs) -> this.<Identifier>list(xs));

    public Wrapper annotation
        = seq(MONKEYS_AT, qualified_iden, annotation_suffix);

    public Wrapper annotations
        = annotation.at_least(0)
        .push((p,xs) -> this.<TAnnotation>list(xs));

    // TODO temp
    public Wrapper dot_iden_temp
        = seq(DOT, iden)
        .lookback(1).push((p,xs) -> DotIden.mk($(xs,0), $(xs,1)));

    // TODO temp
    public Wrapper expr_qualified_iden
        = seq(iden, dot_iden_temp.repeat(0));

    // TODO placeholder
    public Wrapper ternary
        = choice(literal, expr_qualified_iden);

    /// TYPES ======================================================================================

    public Wrapper basic_type
        = token_choice(_byte, _short, _int, _long, _char, _float, _double, _boolean, _void)
        .collect_str((p,str,xs) -> p.push(BasicType.valueOf("_" + trim_trailing_whitespace(str))));

    public Wrapper primitive_type
        = seq(annotations, basic_type)
        .push((p,xs) -> PrimitiveType.mk($(xs,0), $(xs,1)));

    public Wrapper extends_bound
        = seq(_extends, lazy(() -> this.type))
        .push((p,xs) -> ExtendsBound.mk($(xs,0)));

    public Wrapper super_bound
        = seq(_super, lazy(() -> this.type))
        .push((p,xs) -> SuperBound.mk($(xs,0)));

    public Wrapper type_bound
        = choice(extends_bound, super_bound).maybe();

    public Wrapper wildcard
        = seq(annotations, QUES, type_bound)
        .push((p,xs) -> Wildcard.mk($(xs,0), $(xs,1)));

    public Wrapper type_args
        = seq(LT, choice(lazy(() -> this.type), wildcard).sep(0, COMMA), GT).opt()
        .push((p,xs) -> this.<TType>list(xs));

    public Wrapper class_type_part
        = seq(annotations, iden, type_args)
        .push((p,xs) -> ClassTypePart.mk($(xs,0), $(xs,1), $(xs,2)));

    public Wrapper class_type
        = class_type_part.sep(1, DOT)
        .push((p, xs) -> ClassType.mk(list(xs)));

    public Wrapper stem_type
        = choice(primitive_type, class_type);

    public Wrapper dim
        = seq(annotations, seq(LBRACKET, RBRACKET))
        .push((p,xs) -> Dimension.mk($(xs,0)));

    public Wrapper dims
        = dim.at_least(0)
        .push((p,xs) -> this.<Dimension>list(xs));

    public Wrapper dims1
        = dim.at_least(1)
        .push((p,xs) -> this.<Dimension>list(xs));

    public Wrapper type_dim_suffix
        = dims1
        .lookback(1).push((p,xs) -> ArrayType.mk($(xs,0), $(xs,1)));

    public Wrapper type
        = seq(stem_type, type_dim_suffix.opt());

    public Wrapper type_union_syntax
        = lazy(() -> this.type).sep(1, AMP);

    public Wrapper type_union
        = type_union_syntax
        .push((p,xs) -> this.<TType>list(xs));

    public Wrapper type_bounds
        = seq(_extends, type_union_syntax).opt()
        .push((p,xs) -> this.<TType>list(xs));

    public Wrapper type_param
        = seq(annotations, iden, type_bounds)
        .push((p,xs) -> TypeParam.mk($(xs,0), $(xs,1), $(xs,2)));

    public Wrapper type_params
        = seq(LT, type_param.sep(0, COMMA), GT).opt()
        .push((p,xs) -> this.<TypeParam>list(xs));
 
    /// MODIFIERS ==================================================================================

    public Wrapper keyword_modifier =
        token_choice(
            _public, _protected, _private, _abstract, _static, _final, _synchronized,
            _native, _strictfp, _default, _transient, _volatile)
        .collect_str((p,str,xs) -> p.push(Keyword.valueOf("_" + trim_trailing_whitespace(str))));

    public Wrapper modifier =
        choice(annotation, keyword_modifier);

    public Wrapper modifiers =
        modifier.at_least(0)
        .push((p,xs) -> this.<Modifier>list(xs));

    /// PARAMETERS =================================================================================

    public Wrapper args =
        seq(LPAREN, lazy(() -> this.expr).sep(0, COMMA), RPAREN)
        .push((p,xs) -> this.<Expression>list(xs));

    public Wrapper this_parameter_qualifier =
        seq(iden, DOT).at_least(0)
        .push((p, xs) -> this.<String>list(xs));

    public Wrapper this_param_suffix =
        seq(this_parameter_qualifier, _this)
        .push((p,xs) -> ThisParameter.mk($(xs,0), $(xs,1), $(xs,2)));

    public Wrapper iden_param_suffix =
        seq(iden, dims)
        .push((p,xs) -> IdenParameter.mk($(xs,0), $(xs,1), $(xs,2), $(xs,3)));

//    Wrapper variadic_param_suffix =
//        seq(annotations, ELLIPSIS, iden)
//        .push((p, xs) -> new VariadicParameter($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    Wrapper formal_param_suffix =
//        choice(iden_param_suffix, this_param_suffix, variadic_param_suffix);
//
//    Wrapper formal_param =
//        seq(modifiers, type, formal_param_suffix);
//
//    Wrapper formal_params =
//        formal_param.sep(0, COMMA).bracketed("()")
//        .push((p,xs) -> new FormalParameters(it.list()));
//
//    Wrapper untyped_params =
//        iden.sep(1, COMMA).bracketed("()")
//        .push((p,xs) -> new UntypedParameters(it.list()));
//
//    Wrapper single_param =
//        iden
//        .push((p,xs) -> new UntypedParameters(this.<String>list(xs)));
//
//    Wrapper lambda_params =
//        choice(formal_params, untyped_params, single_param);
//
//    /// NON-TYPE DECLARATIONS ======================================================================

    Wrapper var_init =
        choice(lazy(() -> this.expr), lazy(() -> this.array_init));

    Wrapper array_init =
        seq(LBRACE, var_init.sep_trailing(0, COMMA), RBRACE)
        .push((p,xs) -> ArrayInitializer.mk(list(xs)));

//    Wrapper var_declarator_id =
//        seq(iden, dims)
//        .push((p,xs) -> new VarDeclaratorID($(xs,0), $(xs,1)));
//
//    Wrapper var_declarator =
//        seq(var_declarator_id, seq(EQ, var_init).maybe())
//        .push((p,xs) -> new VarDeclarator($(xs,0), $(xs,1)));
//
//    Wrapper var_decl_no_semi =
//        seq(type, var_declarator.sep(1, COMMA))
//        .push((p,xs) -> new VarDecl($(xs,0), $(xs,1), it.list(2)));
//
//    Wrapper var_decl_suffix =
//        seq(var_decl_no_semi, SEMI);
//
//    Wrapper var_decl =
//        seq(modifiers, var_decl_suffix);
//
//    Wrapper throws_clause =
//        seq(throws, type.sep(1, COMMA)).opt()
//        .push((p,xs) -> this.<Type>list(xs));
//
//    Wrapper block_or_semi =
//        choice(lazy(() -> this.block), SEMI.as_val(null));
//
//    Wrapper method_decl_suffix =
//        seq(type_params, type, iden, formal_params, dims, throws_clause, block_or_semi)
//        .push((p,xs) -> new MethodDecl($(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5), $(xs,6), $(xs,7)));
//
//    Wrapper constructor_decl_suffix =
//        seq(type_params, iden, formal_params, throws_clause, lazy(() -> this.block))
//        .push((p,xs) -> new ConstructorDecl($(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5)));
//
//    Wrapper init_block =
//        seq(static.as_bool(), lazy(() -> this.block))
//        .push((p,xs) -> new InitBlock($(xs,0), $(xs,1)));
//
//    /// TYPE DECLARATIONS ==========================================================================
//
//    // Common -----------------------------------------------------------------
//
//    Wrapper extends_clause =
//        seq(extends, type.sep(0, COMMA)).opt()
//        .push((p,xs) -> this.<Type>list(xs));
//
//    Wrapper implements_clause =
//        seq(implements, type.sep(0, COMMA)).opt()
//        .push((p,xs) -> this.<Type>list(xs));
//
//    Wrapper type_sig =
//        seq(iden, type_params, extends_clause, implements_clause);
//
//    Wrapper class_modified_decl =
//        seq(modifiers, choice(var_decl_suffix, method_decl_suffix, constructor_decl_suffix, lazy(() -> this.type_decl_suffix)));
//
//    Wrapper class_body_decl =
//        choice(class_modified_decl, init_block, SEMI);
//
//    Wrapper class_body_decls =
//        class_body_decl.at_least(0)
//        .push((p,xs) -> this.<Decl>list(xs));
//
//    Wrapper type_body =
//        class_body_decls.bracketed("{}");
//
//    // Enum -------------------------------------------------------------------
//
//    Wrapper enum_constant =
//        seq(annotations, iden, args.maybe(), type_body.maybe())
//        .push((p,xs) -> new EnumConstant($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    Wrapper enum_class_decls =
//        seq(SEMI, class_body_decl.at_least(0)).opt()
//        .push((p, xs) -> this.<Decl>list(xs));
//
//    Wrapper enum_constants =
//        enum_constant.sep(1, COMMA).opt()
//        .push((p,xs) -> this.<EnumConstant>list(xs));
//
        // TODO peek-only
//    Wrapper enum_body =
//        seq(enum_constants, enum_class_decls).bracketed("{}").collect((p,xs) -> stack
//        .push($(xs,1)) ; stack
//        .push($(xs,0)) /* swap */);
//
//    Wrapper enum_decl =
//        seq(enum, type_sig, enum_body)
//        .push((p,xs) -> new val td = TypeDecl(input, ENUM, $(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5))
//    EnumDecl(td, $(xs,6)));
//
//    // Annotations ------------------------------------------------------------
//
//    Wrapper annot_default_clause =
//        seq(default, annotation_element)
//        .push((p,xs) -> {$(xs,1));
//
//    Wrapper annot_elem_decl =
//        seq(modifiers, type, iden, seq(LPAREN, RPAREN), dims, annot_default_clause.maybe(), SEMI)
//        .push((p, xs) -> new AnnotationElemDecl($(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4)));
//
//    Wrapper annot_body_decls =
//        choice(annot_elem_decl, class_body_decl).at_least(0)
//        .push((p,xs) -> this.<Decl>list(xs));
//
//    Wrapper annotation_decl =
//        seq(MONKEYS_AT, _interface, type_sig, annot_body_decls.bracketed("{}"))
//        .push((p,xs) -> new TypeDecl(input, ANNOTATION, $(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5)));
//
//    //// ------------------------------------------------------------------------
//
//    Wrapper class_decl =
//        seq(_class, type_sig, type_body)
//        .push((p,xs) -> new TypeDecl(input, CLASS, $(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5)));
//
//    Wrapper interface_declaration =
//        seq(_interface, type_sig, type_body)
//        .push((p,xs) -> new TypeDecl(input, INTERFACE, $(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4), $(xs,5)));
//
//    Wrapper type_decl_suffix =
//        choice(class_decl, interface_declaration, enum_decl, annotation_decl);
//
//    Wrapper type_decl =
//        seq(modifiers, type_decl_suffix);
//
//    Wrapper type_decls =
//        choice(type_decl, SEMI).at_least(0)
//        .push((p, xs) -> this.<Decl>list(xs));
//
    /// EXPRESSIONS ================================================================================

    // Array Constructor ------------------------------------------------------

    Wrapper dim_expr =
        seq(annotations, LBRACKET, lazy(() -> this.expr), RBRACKET)
        .push((p,xs) -> DimExpression.mk($(xs,0), $(xs,1)));

    Wrapper dim_exprs =
        dim_expr.at_least(1)
        .push((p,xs) -> this.<DimExpression>list(xs));

    Wrapper dim_expr_array_creator =
        seq(stem_type, dim_exprs, dims)
        .push((p,xs) -> ArrayConstructorCall.mk($(xs,0), $(xs,1), $(xs,2), null));

    Wrapper init_array_creator =
        seq(stem_type, dims1, array_init)
        .push((p,xs) -> ArrayConstructorCall.mk($(xs,0), emptyList(), $(xs,1), $(xs,2)));

    Wrapper array_ctor_call =
        seq(_new, choice(dim_expr_array_creator, init_array_creator));

    // Lambda Expression ------------------------------------------------------

//    Wrapper lambda =
//        seq(lambda_params, ARROW, choice(lazy(() -> this.block), lazy(() -> this.expr)))
//        .push((p, xs) -> new Lambda($(xs,0), $(xs,1)));

    // Expression - Primary ---------------------------------------------------

    public Wrapper par_expr =
        seq(LPAREN, lazy(() -> this.expr), RPAREN)
        .push((p,xs) -> ParenExpression.mk($(xs,0)));

    // TODO (type_body undefined yet)
    public Wrapper ctor_call =
        // seq(_new, type_args, stem_type, args, type_body.maybe())
        seq(_new, type_args, stem_type, args, str("").push((p, xs) -> null))
            .push((p,xs) -> ConstructorCall.mk($(xs,0), $(xs,1), $(xs,2), $(xs,3)));

    public Wrapper new_ref_suffix =
        _new
        .lookback(2).push((p,xs) -> NewReference.mk($(xs,0), $(xs,1)));

    public Wrapper method_ref_suffix =
        iden
        .lookback(2).push((p,xs) -> TypeMethodReference.mk($(xs,0), $(xs,1), $(xs,2)));

    public Wrapper ref_suffix =
        seq(COLCOL, type_args, choice(new_ref_suffix, method_ref_suffix));

    public Wrapper class_expr_suffix =
        seq(DOT, _class)
        .lookback(1).push((p, xs) -> ClassExpression.mk($(xs,0)));

    public Wrapper type_suffix_expr =
        seq(type, choice(ref_suffix, class_expr_suffix));

    public Wrapper iden_or_method_expr =
        seq(iden, args.maybe())
        .push((p,xs) -> $(xs,1) == null ? $(xs,0) : MethodCall.mk(null, list(), $(xs,0), $(xs,1)));

    public Wrapper this_expr =
        seq(_this, args.maybe())
        .push((p,xs) -> $(xs,0) == null ? This.mk() : ThisCall.mk($(xs,0)));

    public Wrapper super_expr =
        seq(_super, args.maybe())
        .push((p,xs) -> $(xs,0) == null ? Super.mk() : SuperCall.mk($(xs,0)));

    public Wrapper primary_expr = choice(
        par_expr, array_ctor_call, ctor_call, type_suffix_expr, iden_or_method_expr,
        this_expr, super_expr, literal);

    // Expression - Postfix ---------------------------------------------------

    public Wrapper dot_this =
        _this
        .lookback(1).push((p,xs) -> DotThis.mk($(xs,0)));

    public Wrapper dot_super =
        _super
        .lookback(1).push((p,xs) -> DotSuper.mk($(xs,0)));

    public Wrapper dot_iden =
        iden
        .lookback(1).push((p,xs) -> DotIden.mk($(xs,0), $(xs,1)));

    public Wrapper dot_new =
        ctor_call
        .lookback(1).push((p,xs) -> DotNew.mk($(xs,0), $(xs,1)));

    public Wrapper dot_method =
        seq(type_args, iden, args)
        .lookback(1).push((p,xs) -> MethodCall.mk($(xs,0), $(xs,1), $(xs,2), $(xs,3)));

    public Wrapper dot_postfix =
        choice(dot_method, dot_iden, dot_this, dot_super, dot_new);

    public Wrapper ref_postfix =
        seq(COLCOL, type_args, iden)
        .lookback(1).push((p, xs) -> BoundMethodReference.mk($(xs,0), $(xs,1), $(xs,2)));

    public Wrapper array_postfix =
        seq(LBRACKET, lazy(() -> this.expr), RBRACKET)
        .lookback(1).push((p,xs) -> ArrayAccess.mk($(xs,0), $(xs,1)));

    public Wrapper inc_suffix =
        PLUSPLUS
        .lookback(1).push((p,xs) -> PostIncrement.mk($(xs,0)));

    public Wrapper dec_suffix =
        SUBSUB
        .lookback(1).push((p,xs) -> PostDecrement.mk($(xs,0)));

    public Wrapper postfix =
        choice(seq(DOT, dot_postfix), array_postfix, inc_suffix, dec_suffix, ref_postfix);

    public Wrapper postfix_expr =
        seq(primary_expr, postfix.at_least(0));

    public Wrapper inc_prefix =
        seq(PLUSPLUS, lazy(() -> this.prefix_expr))
        .push((p,xs) -> PreIncrement.mk($(xs,0)));

    public Wrapper dec_prefix =
        seq(SUBSUB, lazy(() -> this.prefix_expr))
        .push((p,xs) -> PreDecrement.mk($(xs,0)));

    public Wrapper unary_plus =
        seq(PLUS, lazy(() -> this.prefix_expr))
        .push((p,xs) -> UnaryPlus.mk($(xs,0)));

    public Wrapper unary_minus =
        seq(SUB, lazy(() -> this.prefix_expr))
        .push((p,xs) -> UnaryMinus.mk($(xs,0)));

    public Wrapper complement =
        seq(TILDE, lazy(() -> this.prefix_expr))
        .push((p,xs) -> Complement.mk($(xs,0)));

    public Wrapper not =
        seq(BANG, lazy(() -> this.prefix_expr))
        .push((p,xs) -> Negate.mk($(xs,0)));

    // TODO lambda not defined yet
    public Wrapper cast =
        //seq(LPAREN, type_union, RPAREN, choice(lambda, lazy(() -> this.prefix_expr)))
        seq(LPAREN, type_union, RPAREN, choice(lazy(() -> this.prefix_expr)))
        .push((p,xs) -> Cast.mk($(xs,0), $(xs,1)));

    public Wrapper prefix_expr =
        choice(inc_prefix, dec_prefix, unary_plus, unary_minus, complement, not, cast, postfix_expr);

//    // Expression - Binary ----------------------------------------------------
//
//    public Wrapper mult_expr =
//        AssocLeft(this) {
//    operands = prefix_expr
//    op(STAR, { Product($(xs,0), $(xs,1)) })
//    op(DIV, { Division($(xs,0), $(xs,1)) })
//    op(PERCENT, { Remainder($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper add_expr =
//        AssocLeft(this) {
//    operands = mult_expr
//    op(PLUS, { Sum($(xs,0), $(xs,1)) })
//    op(SUB, { Diff($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper shift_expr =
//        AssocLeft(this) {
//    operands = add_expr
//    op(LTLT, { ShiftLeft($(xs,0), $(xs,1)) })
//    op(GTGT, { ShiftRight($(xs,0), $(xs,1)) })
//    op(GTGTGT, { BinaryShiftRight($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper order_expr =
//        AssocLeft(this) {
//    operands = shift_expr
//    op(LT, { Lower($(xs,0), $(xs,1)) })
//    op(LTEQ, { LowerEqual($(xs,0), $(xs,1)) })
//    op(GT, { Greater($(xs,0), $(xs,1)) })
//    op(GTEQ, { GreaterEqual($(xs,0), $(xs,1)) })
//    postfix(seq(instanceof, type), { Instanceof($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper eq_expr =
//        AssocLeft(this) {
//    operands = order_expr
//    op(EQEQ, { Equal($(xs,0), $(xs,1)) })
//    op(BANGEQ, { NotEqual($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper binary_and_expr =
//        AssocLeft(this) {
//    operands = eq_expr
//    op(AMP, { BinaryAnd($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper xor_expr =
//        AssocLeft(this) {
//    operands = binary_and_expr
//    op(CARET, { Xor($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper binary_or_expr =
//        AssocLeft(this) {
//    operands = xor_expr
//    op(BAR, { BinaryOr($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper and_expr =
//        AssocLeft(this) {
//    operands = binary_or_expr
//    op(AMPAMP, { And($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper or_expr =
//        AssocLeft(this) {
//    operands = and_expr
//    op(BARBAR, { Or($(xs,0), $(xs,1)) })
//);
//
//    public Wrapper ternary_suffix =
//        seq(QUES, lazy(() -> this.expr), COL, lazy(() -> this.expr))
//        .push((p, xs) -> new Ternary($(xs,0), $(xs,1), $(xs,2)));
//
//    public Wrapper ternary =
//        seq(or_expr, ternary_suffix.opt());
//
//    public Wrapper assignment_suffix =
//        choice(seq(EQ, lazy(() -> this.expr))
//        .push((p,xs) -> new Assign($(xs,0), $(xs,1), "=")}, seq(PLUSEQ, lazy(() -> this.expr))
//        .push((p,xs) -> new Assign($(xs,0), $(xs,1), "+=")}, seq(SUBEQ, lazy(() -> this.expr))
//        .push((p,xs) -> new Assign($(xs,0), $(xs,1), "-=")}, seq(STAREQ, lazy(() -> this.expr))
//        .push((p,xs) -> new Assign($(xs,0), $(xs,1), "*=")}, seq(DIVEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "/=")}, seq(PERCENTEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "%=")}, seq(LTLTEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "<<=")}, seq(GTGTEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), ">>=")}, seq(GTGTGTEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), ">>>=")}, seq(AMPEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "&=")}, seq(CARETEQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "^=")}, seq(BAREQ, lazy(() -> this.expr))
//        .push((p, xs) -> new Assign($(xs,0), $(xs,1), "|=")});
//
//    public Wrapper assignment =
//        seq(ternary, assignment_suffix.opt());
//
    // TODO
    public Wrapper expr =
        prefix_expr;
    //    choice(lambda, assignment);
//
//    /// STATEMENTS =================================================================================
//
//    public Wrapper if_stmt =
//        seq(_if, par_expr, lazy(() -> this.stmt), seq(_else, lazy(() -> this.stmt)).maybe())
//        .push((p,xs) -> new If($(xs,0), $(xs,1), $(xs,2)));
//
//    public Wrapper expr_stmt_list =
//        expr.sep(0, COMMA)
//        .push((p,xs) -> this.<Stmt>list(xs));
//
//    public Wrapper for_init_decl =
//        seq(modifiers, var_decl_no_semi)
//        .push((p,xs) -> this.<Stmt>list(xs));
//
//    public Wrapper for_init =
//        choice(for_init_decl, expr_stmt_list);
//
//    public Wrapper basic_for_paren_part =
//        seq(for_init, SEMI, expr.maybe(), SEMI, expr_stmt_list.opt());
//
//    public Wrapper basic_for_stmt =
//        seq(_for, basic_for_paren_part.bracketed("()"), lazy(() -> this.stmt))
//        .push((p,xs) -> new BasicFor($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    public Wrapper for_val_decl =
//        seq(modifiers, type, var_declarator_id, COL, expr);
//
//    public Wrapper enhanced_for_stmt =
//        seq(_for, for_val_decl.bracketed("()"), lazy(() -> this.stmt))
//        .push((p,xs) -> new EnhancedFor($(xs,0), $(xs,1), $(xs,2), $(xs,3), $(xs,4)));
//
//    public Wrapper while_stmt =
//        seq(_while, par_expr, lazy(() -> this.stmt))
//        .push((p,xs) -> new WhileStmt($(xs,0), $(xs,1)));
//
//    public Wrapper do_while_stmt =
//        seq(_do, lazy(() -> this.stmt), _while, par_expr, SEMI)
//        .push((p, xs) -> new DoWhileStmt($(xs,0), $(xs,1)));
//
//    public Wrapper catch_parameter_types =
//        type.sep(0, BAR)
//        .push((p,xs) -> this.<Type>list(xs));
//
//    public Wrapper catch_parameter =
//        seq(modifiers, catch_parameter_types, var_declarator_id);
//
//    public Wrapper catch_clause =
//        seq(catch, catch_parameter.bracketed("()"), lazy(() -> this.block))
//        .push((p,xs) -> new CatchClause($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    public Wrapper catch_clauses =
//        catch_clause.at_least(0)
//        .push((p,xs) -> this.<CatchClause>list(xs));
//
//    public Wrapper finally_clause =
//        seq(finally, lazy(() -> this.block));
//
//    public Wrapper resource =
//        seq(modifiers, type, var_declarator_id, EQ, expr)
//        .push((p,xs) -> new TryResource($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    public Wrapper resources =
//        resource.sep(1, SEMI).bracketed("()").opt()
//        .push((p, xs) -> this.<TryResource>list(xs));
//
//    public Wrapper try_stmt =
//        seq(_try, resources, lazy(() -> this.block), catch_clauses, finally_clause.maybe())
//        .push((p,xs) -> new TryStmt($(xs,0), $(xs,1), $(xs,2), $(xs,3)));
//
//    public Wrapper default_label =
//        seq(default, COL)
//        .push((p, xs) -> new DefaultLabel);
//
//    public Wrapper case_label =
//        seq(case, expr, COL)
//        .push((p, xs) -> new CaseLabel($(xs,0)));
//
//    public Wrapper switch_label =
//        choice(case_label, default_label);
//
//    public Wrapper switch_clause =
//        seq(switch_label, lazy(() -> this.stmts))
//        .push((p,xs) -> new SwitchClause($(xs,0), $(xs,1)));
//
//    public Wrapper switch_stmt =
//        seq(switch, par_expr, switch_clause.at_least(0).bracketed("{}"))
//        .push((p,xs) -> new SwitchStmt($(xs,0), it.list(1)));
//
//    public Wrapper synchronized_stmt =
//        seq(synchronized, par_expr, lazy(() -> this.block))
//        .push((p,xs) -> new SynchronizedStmt($(xs,1), $(xs,2)));
//
//    public Wrapper return_stmt =
//        seq(_return, expr.maybe(), SEMI)
//        .push((p, xs) -> new ReturnStmt($(xs,0)));
//
//    public Wrapper throw_stmt =
//        seq(_throw, expr, SEMI)
//        .push((p, xs) -> new ThrowStmt($(xs,0)));
//
//    public Wrapper break_stmt =
//        seq(_break, iden.maybe(), SEMI)
//        .push((p, xs) -> new BreakStmt($(xs,0)));
//
//    public Wrapper continue_stmt =
//        seq(_continue, iden.maybe(), SEMI)
//        .push((p, xs) -> new ContinueStmt($(xs,0)));
//
//    public Wrapper assert_stmt =
//        seq(assert, expr, seq(COL, expr).maybe(), semi)
//        .push((p, xs) -> new AssertStmt($(xs,0), $(xs,1)));
//
//    public Wrapper semi_stmt =
//        SEMI
//        .push((p, xs) -> new SemiStmt);
//
//    public Wrapper expr_stmt =
//        seq(expr, SEMI);
//
//    public Wrapper labelled_stmt =
//        seq(iden, COL, lazy(() -> this.stmt))
//        .push((p, xs) -> new LabelledStmt($(xs,0), $(xs,1)));
//
//    public Wrapper stmt =
//        choice(lazy(() -> this.block), if_stmt, basic_for_stmt, enhanced_for_stmt, while_stmt, do_while_stmt, try_stmt, switch_stmt, synchronized_stmt, return_stmt, throw_stmt, break_stmt, continue_stmt, assert_stmt, semi_stmt, expr_stmt, labelled_stmt, var_decl, type_decl);
//
//    public Wrapper block =
//        stmt.at_least(0).bracketed("{}")
//        .push((p,xs) -> new Block(it.list()));
//
//    public Wrapper stmts =
//        stmt.at_least(0)
//        .push((p,xs) -> this.<Stmt>list(xs));
//
//    /// TOP-LEVEL ==================================================================================
//
//    public Wrapper package_decl =
//        seq(annotations, _package, qualified_iden, SEMI)
//        .push((p, xs) -> new Package($(xs,0), $(xs,1)));
//
//    public Wrapper import_decl =
//        seq(import, static.as_bool(), qualified_iden, seq(DOT, STAR).as_bool(), semi)
//        .push((p, xs) -> new Import($(xs,0), $(xs,1), $(xs,2)));
//
//    public Wrapper import_decls =
//        import_decl.at_least(0)
//        .push((p,xs) -> this.<Import>list(xs));
//
//    public Wrapper root =
//        seq(lazy(() -> this.whitespace), package_decl.maybe(), import_decls, type_decls)
//        .push((p,xs) -> new File(input, $(xs,0), $(xs,1), $(xs,2)));

    public Grammar()
    {
        make_rule_names(this);
    }
}

