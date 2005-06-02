// Copyright (c) 1999, 2005  Per M.A. Bothner.
// This is free software;  for terms and warranty disclaimer see ./COPYING.

package gnu.expr;
import gnu.bytecode.*;
import gnu.mapping.*;

/**
 * This class represents a variable reference (an identifier).
 * @author	Per Bothner
 */

public class ReferenceExp extends AccessExp
{
  static int counter;
  /** Unique id number, to ease print-outs and debugging. */
  int id = ++counter;

  /** If binding has a non-static field and no base, use this instead of base.
   *  This is used for aliases of imported module declarations. */
  private Declaration context;
  public final Declaration contextDecl ()
  { return context; }
  public final void setContextDecl(Declaration decl)
  { context = decl; }

  public static final int DONT_DEREFERENCE = NEXT_AVAIL_FLAG;
  public static final int PROCEDURE_NAME = NEXT_AVAIL_FLAG << 1;
  public static final int PREFER_BINDING2 = NEXT_AVAIL_FLAG << 2;
  /** Create a FieldLocation referencing the binding. */
  public static final int CREATE_FIELD_REFERENCE = NEXT_AVAIL_FLAG << 3;

  /* If true, must have binding.isIndirectBinding().  Don't dereference it. */
  public final boolean getDontDereference()
  {
    return (flags & DONT_DEREFERENCE) != 0;
  }

  public final void setDontDereference(boolean setting)
  { setFlag(setting, DONT_DEREFERENCE); }

  /** True if this identifier appears in "function call position".
   * If so, it should be interpreted as a function name, which makes a
   * difference for languages (like Common Lisp) that have two name spaces. */
  public final boolean isProcedureName()
  {
    return (flags & PROCEDURE_NAME) != 0;
  }

  /** Note if this identifier appears in "function call position". */
  public final void setProcedureName(boolean setting)
  {
    setFlag(setting, PROCEDURE_NAME);
  }

  public ReferenceExp (Object symbol)
  {
    this.symbol = symbol;
  }

  public ReferenceExp (Object symbol, Declaration binding)
  {
    this.symbol = symbol;
    this.binding = binding;
  }

  public ReferenceExp (Declaration binding) 
  {
    this(binding.getSymbol(), binding);
  }

  public Object eval (Environment env)
  {
    if (binding != null)
      {
        // This isn't just an optimization; it's needed for module imports.
        if (binding.value instanceof QuoteExp
            && binding.value != QuoteExp.undefined_exp
            && (! getDontDereference() || binding.isIndirectBinding()))
          {
            Object value = binding.getConstantValue();
            if (binding.isIndirectBinding())
              return ((gnu.mapping.Location) value).get();
            return value;
          }

        if (binding.field != null && binding.field.getStaticFlag()
            && (! getDontDereference() || binding.isIndirectBinding()))
          {
            try
              {
                Object value = binding.field.getReflectField().get(null);
                if (binding.isIndirectBinding())
                  return ((gnu.mapping.Location) value).get();
                return value;
              }
            catch (Exception ex)
              {
                throw WrappedException.wrapIfNeeded(ex);
              }
          }
        if ( ! (binding.context instanceof ModuleExp && ! binding.isPrivate()))
          throw new Error("internal error: ReferenceExp.eval on lexical binding");
      }
    Symbol sym = symbol instanceof Symbol ? (Symbol) symbol
      : env.getSymbol(symbol.toString());
    Object property = getFlag(PREFER_BINDING2) && isProcedureName()
      ? EnvironmentKey.FUNCTION
      : null;
    if (getDontDereference())
      return env.getLocation(sym, property);
    Object unb = gnu.mapping.Location.UNBOUND;
    Object val = env.get(sym, property, unb);
    if (val == unb)
      throw new UnboundLocationException(sym);
    return val;
  }

  public void compile (Compilation comp, Target target)
  {
    binding.load(contextDecl(), flags, comp, target);
  }

  protected Expression walk (ExpWalker walker)
  {
    return walker.walkReferenceExp(this);
  }

  public void print (OutPort ps)
  {
    ps.print("(Ref/");
    ps.print(id);
    if (symbol != null
	&& (binding == null || symbol.toString() != binding.getName()))
      {
	ps.print('/');
	ps.print(symbol);
      }
    if (binding != null)
      {
	ps.print('/');
	ps.print(binding);
      }
    ps.print(")");
  }

  public gnu.bytecode.Type getType()
  {
    return (binding == null || binding.isFluid()) ? Type.pointer_type
      : getDontDereference() ? Compilation.typeLocation
      : Declaration.followAliases(binding).getType();
  }

  public String toString()
  {
    return "RefExp/"+symbol+'/'+id+'/';
  }
}
