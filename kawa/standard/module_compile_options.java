package kawa.standard;
import kawa.lang.*;
import gnu.expr.*;
import gnu.lists.*;

public class module_compile_options extends Syntax
{
  public boolean scanForDefinitions (Pair st, java.util.Vector forms,
                                     ScopeExp defs, Translator tr)
  {
    Object rest = with_compile_options.getOptions(st.cdr, null, this, tr);
    if (rest != LList.Empty)
      tr.error('e', getName() + " key must be a keyword");
    return true;
  }

  public Expression rewriteForm (Pair form, Translator tr)
  {
    return null;
  }
}
