package kawa.standard;
import kawa.lang.*;
import gnu.expr.*;
import gnu.bytecode.ClassType;
import gnu.bytecode.Method;
import gnu.kawa.util.*;

public class define_syntax extends Syntax
{
  static ClassType typeMacro = ClassType.make("kawa.lang.Macro");
  static Method makeMethod = typeMacro.getDeclaredMethod("make", 2);
  static Method setExpanderMethod
    = typeMacro.getDeclaredMethod("setExpander", 1);

  public Expression rewriteForm (Pair form, Translator tr)
  {
    Pair pair;
    if (! (form.cdr instanceof Pair)
        || ! ((pair = (Pair) form.cdr).car instanceof String
              || pair.car instanceof Declaration))
      return tr.syntaxError("Missing macro name for "+form.car);
    String name;
    Macro macro;
    if (pair.car instanceof String)
      {
        name = (String) pair.car;
        macro = null;
      }
    else
      {
        macro = (Macro) pair.car;
        name = macro.getName();
      }
    if (! (pair.cdr instanceof Pair))
      return tr.syntaxError("Missing transformation for "+form.car);
    pair = (Pair) pair.cdr;
    Expression rule = tr.rewrite(pair.car);
    macro.expander = rule;
    if (! (macro.context instanceof ModuleExp))
      {
	return QuoteExp.voidExp;
      }
    else
      {
        // Add rule to translation environment.
        tr.addGlobal(name, macro);

        // Add rule to execution environment.
	if (tr.immediate)
	  {
	    if (! (rule instanceof QuoteExp))
	      {
		Expression args[] = new Expression[2];
		args[0] = new QuoteExp(macro);
		args[1] = rule;
		return new ApplyExp(new PrimProcedure(setExpanderMethod), args);
	      }
	    return QuoteExp.voidExp;
	  }
	if (! (rule instanceof QuoteExp)
	    || ! (((QuoteExp) rule).getValue() instanceof java.io.Externalizable))
	  {
	    Expression args[] = new Expression[2];
	    args[0] = new QuoteExp(name);
	    args[1] = rule;
	    rule = new ApplyExp(new PrimProcedure(makeMethod), args);
	  }
	else
	  rule = new QuoteExp(macro);
        SetExp result = new SetExp (macro, rule);
                        
        result.setDefining (true);
	macro.noteValue(rule);
        return result;
      }
  }

  public boolean scanForDefinitions (Pair st, java.util.Vector forms,
                                     ScopeExp defs, Translator tr)
  {
    if (! (st.cdr instanceof Pair)
        || ! (((Pair) st.cdr).car instanceof String))
      return super.scanForDefinitions(st, forms, defs, tr);
    Pair p = (Pair) st.cdr;
    Object name = p.car;
    if (! (p.car instanceof String)
        || ! (p.cdr instanceof Pair)
        || (p = (Pair) p.cdr).cdr != LList.Empty)
      {
        forms.addElement(tr.syntaxError("invalid syntax for define-syntax"));
        return false;
      }
    Macro macro = new Macro((String) name);
    defs.addDeclaration(macro);
    p = tr.makePair(st, this, new Pair(macro, p));
    forms.addElement (p);
    return true;
  }
}
