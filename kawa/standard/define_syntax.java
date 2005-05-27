package kawa.standard;
import kawa.lang.*;
import gnu.expr.*;
import gnu.bytecode.ClassType;
import gnu.mapping.*;
import gnu.lists.*;

public class define_syntax extends Syntax
{
  public static final define_syntax define_macro
    = new define_syntax("%define-macro", false);

  public static final define_syntax define_syntax
    = new define_syntax("%define-syntax", true);

  public define_syntax ()
  {
    this.hygienic = true;
  }

  public define_syntax (Object name, boolean hygienic)
  {
    super(name);
    this.hygienic = hygienic;
  }

  static ClassType typeMacro = ClassType.make("kawa.lang.Macro");
  static PrimProcedure makeHygienic
    = new PrimProcedure(typeMacro.getDeclaredMethod("make", 3));
  static PrimProcedure makeNonHygienic
    = new PrimProcedure(typeMacro.getDeclaredMethod("makeNonHygienic", 3));

  boolean hygienic;

  public Expression rewriteForm (Pair form, Translator tr)
  {
    return tr.syntaxError("define-syntax not in a body");
  }

  public void scanForm (Pair st, ScopeExp defs, Translator tr)
  {
    SyntaxForm syntax = null;
    Object st_cdr = st.cdr;
    while (st_cdr instanceof SyntaxForm)
      {
	syntax = (SyntaxForm) st_cdr;
	st_cdr = syntax.form;
      }
    Object p = st_cdr;
    Object name;
    if (p instanceof Pair)
      {
	Pair pp = (Pair) p;
	name = pp.car;
	p = pp.cdr;
      }
    else
      name = null;
    SyntaxForm nameSyntax = syntax;
    while (name instanceof SyntaxForm)
      {
	nameSyntax = (SyntaxForm) name;
	name = nameSyntax.form;
      }
    if (! (name instanceof String || name instanceof Symbol))
      {
        tr.formStack.addElement(tr.syntaxError("missing macro name for "+Translator.safeCar(st)));
        return;
      }
    if (p == null || Translator.safeCdr(p) != LList.Empty)
      {
        tr.formStack.addElement(tr.syntaxError("invalid syntax for "+getName()));
        return;
      }

    Declaration decl = tr.define(name, nameSyntax, defs);
    decl.setType(typeMacro); 
    tr.push(decl);

    Macro savedMacro = tr.currentMacroDefinition;
    Macro macro = Macro.make(decl);
    macro.setHygienic(hygienic);
    tr.currentMacroDefinition = macro;
    Expression rule = tr.rewrite_car((Pair) p, syntax);
    tr.currentMacroDefinition = savedMacro;
    macro.expander = rule;

    Object expander;
    if (rule instanceof LambdaExp)
      ((LambdaExp) rule).setFlag(LambdaExp.NO_FIELD);
    Expression args[] = new Expression[3];
    args[0] = new QuoteExp(name);
    args[1] = rule;
    args[2] = ThisExp.makeGivingContext(defs);
    rule = new ApplyExp(hygienic ? makeHygienic : makeNonHygienic,
			args);
    decl.noteValue(rule);
    decl.setProcedureDecl(true);
  
    if (decl.context instanceof ModuleExp)
      {
	SetExp result = new SetExp (decl, rule);
        result.setDefining (true);
	if (tr.getLanguage().hasSeparateFunctionNamespace())
	  result.setFuncDef(true);

	tr.formStack.addElement(result);
      }
  }
}
