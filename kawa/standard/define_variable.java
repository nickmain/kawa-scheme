package kawa.standard;
import kawa.lang.*;
import gnu.mapping.*;
import gnu.expr.*;
import gnu.lists.*;

/** "define-variable" is like define, but ignored if variable already bound. */

public class define_variable extends Syntax implements Printable
{
  public boolean scanForDefinitions (Pair st, java.util.Vector forms,
                                     ScopeExp defs, Translator tr)
  {
    if (! (st.cdr instanceof Pair))
      return super.scanForDefinitions(st, forms, defs, tr);
    Pair p = (Pair) st.cdr;
    if (p.car instanceof String || p.car instanceof Symbol)
      {
	Object sym = p.car;
	if (! (defs instanceof ModuleExp))
	  tr.error('e', getName() + " must be at module level");
	Declaration decl = defs.lookup(sym);
	if (decl != null)
	  tr.error('e', "duplicate declaration for '"+sym+"'");
	ModuleExp mod = defs.currentModule();
	decl = mod.addDeclaration(sym);
	tr.push(decl);
	decl.setSimple(false);
	decl.setPrivate(true);
	if (mod.isStatic())
	  decl.setFlag(Declaration.STATIC_SPECIFIED);
	decl.setCanRead(true);
	decl.setCanWrite(true);
	decl.setIndirectBinding(true);
	p = tr.makePair(p, decl, p.cdr);
	st = tr.makePair(st, this, p);
      }
    forms.addElement (st);
    return true;
  }

  public Expression rewriteForm (Pair form, Translator tr)
  {
    Object obj = form.cdr;
    Expression value = null;
    Declaration decl = null;

    if (obj instanceof Pair)
      {
	Pair p1 = (Pair) obj;
	obj = p1.car;
	if (obj instanceof String || obj instanceof Symbol)
	  return tr.syntaxError(getName() + " is only allowed in a <body>");
	if (obj instanceof Declaration)
	  {
	    decl = (Declaration) p1.car;
	    obj = p1.cdr;
	    if (obj instanceof Pair
		&& (p1 = (Pair) obj).cdr == LList.Empty)
	      value = tr.rewrite (p1.car);
	    else if (obj != LList.Empty)
	      decl = null;
	  }
      }
    if (decl == null)
      return tr.syntaxError ("invalid syntax for "+getName());
    if (value == null)
      return QuoteExp.voidExp;
    SetExp sexp = new SetExp (decl, value);
    sexp.setDefining (true);
    sexp.setSetIfUnbound(true);
    
    if (decl != null)
      {
	sexp.binding = decl;
	if (decl.context instanceof ModuleExp
	    && decl.getCanWrite())
	  value = null;
	decl.noteValue(value);
      }
    return sexp;
  }
}
