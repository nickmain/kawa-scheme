package gnu.mapping;

/**
 * An environment contains (name->value) bindings.
 * Names are Strings that are compared by ==, not equal.
 * @author	Per Bothner
 */

public class Environment extends NameMap
{
  private Binding[] table;
  private int log2Size;
  private int mask;
  int num_bindings;

  private static Environment global;

  Environment previous;

  protected TrivialConstraint trivialConstraint = new TrivialConstraint(this);
  protected Constraint unboundConstraint = new UnboundConstraint(this);

  public static Environment user () { return current(); }

  public static Object lookup_global (String name)
       throws UnboundSymbol
  {
    Binding binding = user().lookup(name);
    if (binding == null)
      throw new UnboundSymbol(name);
    return binding.get ();
  }

  /** Define name (interned) to have a given value. */
  public static void define_global (String name, Object new_value)
  {
    user().define (name, new_value);
  }

  /** Define name (interned) to have a given value. */
  public static void put_global (String name, Object new_value)
  {
    user().put (name, new_value);
  }

  /**
    * @deprecated
    */
  public static Environment current () { return getCurrent(); }
  public static Environment getCurrent ()
  {
    Thread thread = Thread.currentThread ();
    if (thread instanceof Future)
      return ((Future)thread).environment;
    return global;
  }

  public static void setCurrent (Environment env)
  {
    Thread thread = Thread.currentThread ();
    if (thread instanceof Future)
      ((Future) thread).environment = env;
    else
      global = env;
  }

  public Environment ()
  {
    this(64);
  }

  public Environment (int capacity)
  {
    log2Size = 4;
    while (capacity > (1 << log2Size))
      log2Size++;
    capacity = 1 << log2Size;
    table = new Binding[capacity];
    mask = capacity - 1;
  }

  public Environment (Environment previous)
  {
    this ();
    this.previous = previous;
  }

  public Binding getBinding (String name)
  {
    Binding binding = lookup(name);
    if (binding != null)
      return binding;
    binding = addBinding(name, null);
    binding.constraint = unboundConstraint;
    return binding;
  }

  public static Binding getCurrentBinding (String name)
  {
    return getCurrent().getBinding(name);
  }

  /**
   * Search for a variable binding by name.
   * @param sym the (interned) name of the binding to search for
   * @return the value of the binding, or null if not found
   */

  public Binding lookup (String name)
  {
    return lookup(name, System.identityHashCode(name));
  }

  private Binding lookup (String name, int hash)
  {
    for (Environment env = this;  env != null;  env = env.previous)
      {
	int index = Binding.hashSearch(env.table, env.log2Size, env.mask,
				       name, hash);
	Binding element = env.table[index];
	if (element != null && element != Binding.hashDELETED)
	  return element;
      }
    return null;
  }

  public Binding define (String name, Object value)
  {
    Binding binding = getBinding(name);
    binding.set(value);
    return binding;
  }

  public void addBinding(Binding binding)
  {
    // Rehash if over 2/3 full.
    if (3 * num_bindings >= 2 * table.length)
      rehash();
    if (Binding.hashSet(table, log2Size, binding) == null)
      num_bindings++;
  }

  public Binding addBinding (String name, Object value)
  {
    Binding binding = new Binding(name);
    binding.constraint = trivialConstraint;
    binding.value = value;
    addBinding(binding);
    return binding;
  }

  void rehash ()
  {
    int new_capacity = 2 * table.length;
    Binding[] new_table = new Binding[new_capacity];

    Binding.hashInsertAll(new_table, log2Size + 1,
			  table, log2Size);
    table = new_table;
    log2Size++;
    mask = (mask << 1) | 1;
  }

  public Object remove (String name)
  {
    Environment env = this;
    for ( ; ;  env = env.previous)
      {
	if (env == null)
	  return null;
	Binding[] env_tab = env.table;
	Named old = Binding.hashDelete(env.table, env.log2Size, name);
	if (old != null)
	  return old;
      }
  }

  public Object remove (Object name)
  {
    return remove((String) name);
  }

  public void remove (Binding binding)
  {
    Binding.hashDelete(table, log2Size, binding.sym_name);
  }

  /** Get the value bound to the given name.
   * @exception gnu.mapping.UnboundSymbol the name has no binding
   * @see Environment#get(Object)
   */
  public Object getChecked(String name)
  {
    Binding binding = lookup (name);
    if (binding == null)
      throw new UnboundSymbol(name);
    return binding.get ();
  }

  public Object put (/* interned */ String name, Object value)
  {
    Binding binding = lookup (name);
    if (binding == null)
      {
	define (name, value);
	return null;
      }
    else if (! binding.isBound())
      {
	binding.set (value);
        return null;
      }
    else
      {
	Object old_value = binding.get ();
	binding.set (value);
	return old_value;
      }
  }

  public Object put (Object name, Object value)
  {
    return put ((String) name, value);
  }

  public String toString ()
  {
    String name = getName();
    if (name == null)
      name = super.toString ();
    return "#<environment " + name + '>';
  }

  /**
   * Evaluate an expression in this Environment.
   */
  /*
  final public Object
  eval (Expression expr)
  {
    return expr.eval (this);
  }
  */
}
