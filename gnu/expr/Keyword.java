package gnu.expr;
import gnu.bytecode.*;
import gnu.mapping.Printable;
import java.io.*;

public class Keyword extends Object implements Printable, Compilable, Externalizable
{
  // Does not include final ':'.
  private String name;

  public Keyword()
  {
  }

  private Keyword (String n)
  {
    name = new String(n);
  }

  private static java.util.Hashtable keywordTable = new java.util.Hashtable ();

  public int hashCode () { return name.hashCode (); }

  /**
   * Create or find a Keyword with a given name (without final ':').
   * @param name the print-name of the desired Keyword
   * @return a Keyword with the given name, newly created iff none such exist
   */
  static public Keyword make (String name)
  {
    Keyword keyword = (Keyword) keywordTable.get (name);
    if (keyword == null) {
      keyword = new Keyword (name);
      keywordTable.put (name, keyword);
    }
    return keyword;
  }

  /*
  public FString toSchemeString()
  {
    return new FString(name);
  }
  */

  public static boolean isKeyword (Object obj)
  {
    return obj instanceof Keyword;
  }

  public final String toString()
  {
    return name+':';
  }

  public void print(java.io.PrintWriter ps)
  {
    Symbol.print(name, ps);
    ps.print(':');
  }

  /**
   * Search vals[0:offset-1] for a keyword.
   * Each key at vals[i] is followed by a value at keys[i+1].
   * (This is used to search for a keyword parameter in an argument list.)
   * @param vals the list to search in
   * @param offset the index in vals to start the search at
   * @param keyword the keyword to search for
   * @return vals[i+1] such that vals[i]==keyword (and (i-offset) is even
   * and non-negative);  if there is no such i, return Special.dfault.
   */
  public static Object searchForKeyword (Object[] vals,
					 int offset, Object keyword)
  {
    for (int i = offset;  i < vals.length;  i += 2)
      {
	if (vals[i] == keyword)
	  return vals[i+1];
      }
    return Special.dfault;
  }

  /**
   * Search vals[0:offset-1] for a keyword.
   * Each key at vals[i] is followed by a value at keys[i+1].
   * (This is used to search for a keyword parameter in an argument list.)
   * @param vals the list to search in
   * @param offset the index in vals to start the search at
   * @param keyword the keyword to search for
   * @param dfault the value to return if there is no match
   * @return vals[i+1] such that vals[i]==keyword (and (i-offset) is even
   * and non-negative);  if there is no such i, return dfault.
   */
  public static Object searchForKeyword (Object[] vals,
					 int offset, Object keyword,
					 Object dfault)
  {
    for (int i = offset;  i < vals.length;  i += 2)
      {
	if (vals[i] == keyword)
	  return vals[i+1];
      }
    return dfault;
  }

  public final String getName() { return name; }
  
  static Method makeKeywordMethod;

  /**
   * @serialData Write the keyword name (without colons) using writeUTF.
   */

  public void writeExternal(ObjectOutput out) throws IOException
  {
    out.writeUTF(name);
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException
  {
    name = in.readUTF();
  }

  public Keyword readResolve() throws ObjectStreamException
  {
    return make(name);
  }

  public Literal makeLiteral (Compilation comp)
  {
    if (makeKeywordMethod == null)
      {
	Type[] applyargs = new Type[1];
	applyargs[0] = comp.javaStringType;
	makeKeywordMethod =
	  comp.scmKeywordType.addMethod ("make", applyargs,
					  comp.scmKeywordType,
					  Access.PUBLIC|Access.STATIC);
      }
    return new Literal (this, comp.scmKeywordType, comp);
  }

  public void emit (Literal literal, Compilation comp)
  {
    gnu.bytecode.CodeAttr code = comp.getCode();
    code.emitPushString(((Keyword) literal.value).name);
    code.emitInvokeStatic(makeKeywordMethod);
  }
}
