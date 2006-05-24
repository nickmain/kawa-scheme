package kawa.standard;
import kawa.lang.*;
import gnu.mapping.*;
import gnu.expr.*;
import gnu.lists.*;
import gnu.kawa.functions.Setter;

/**
 * The Syntax transformer that re-writes the Scheme "set!" primitive.
 * @author	Per Bothner
 */

public class set_b extends Syntax implements Printable
{
  public static final set_b set = new set_b();
  static { set.setName("set!"); }

  public Expression rewriteForm (Pair form, Translator tr)
  {
    Object o1 = form.cdr;
    SyntaxForm syntax = null;
    while (o1 instanceof SyntaxForm)
      {
	syntax = (SyntaxForm) o1;
	o1 = syntax.form;
      }
    if (! (o1 instanceof Pair))
      return tr.syntaxError ("missing name");
    Pair p1 = (Pair) o1;
    Expression name = tr.rewrite_car(p1, syntax);
    Object o2 = p1.cdr;
    while (o2 instanceof SyntaxForm)
      {
	syntax = (SyntaxForm) o2;
	o2 = syntax.form;
      }
    Pair p2;
    if (! (o2 instanceof Pair)
	|| (p2 = (Pair) o2).cdr != LList.Empty)
      return tr.syntaxError ("missing or extra arguments to set!");
    Expression value = tr.rewrite_car(p2, syntax);

    if (name instanceof ApplyExp)
      {
	// rewrite (set! (proc . args) rhs) => ((setter proc) args ... rhs)

	ApplyExp aexp = (ApplyExp) name;
        Expression[] args = aexp.getArgs();
	int nargs = args.length;
        int skip = 0;
        Expression func = aexp.getFunction();
        if (args.length > 0 && func instanceof ReferenceExp
            && ((ReferenceExp) func).getBinding() == Scheme.applyFieldDecl)
          {
            skip = 1;
            nargs--;
            func = args[0];
          }
        Expression[] setterArgs = { func };
	Expression[] xargs = new Expression[nargs+1];
	System.arraycopy(args, skip, xargs, 0, nargs);
	xargs[nargs] = value;
	return new ApplyExp(new ApplyExp(new ReferenceExp(Setter.setterDecl),
                                         setterArgs), xargs);
      }
    else if (! (name instanceof ReferenceExp))
      return tr.syntaxError ("first set! argument is not a variable name");    

    ReferenceExp ref = (ReferenceExp) name;
    Declaration decl = ref.getBinding();
    SetExp sexp = new SetExp (ref.getSymbol(), value);
    sexp.setContextDecl(ref.contextDecl());
    if (decl != null)
      {
	sexp.setBinding(decl);
	decl = Declaration.followAliases(decl);
	if (decl != null)
	  decl.noteValue (value);
	if (decl.getFlag(Declaration.IS_CONSTANT))
	  return tr.syntaxError ("constant variable is set!");
      }
    return sexp;
  }
}
