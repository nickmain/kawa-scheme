package kawa.lang;
import gnu.mapping.*;
import gnu.expr.*;
import java.io.*;
import gnu.lists.*;
import java.util.Vector;

/** This encodes a pattern from a Scheem syntax-case or syntax-rules. */

public class SyntaxPattern extends Pattern implements Externalizable
{
  /** An encoding of the pattern in a compact form.
   * This is a sequence of "matching instructions".  These have a 3-bit
   * "opcode", which is one of the <code>MATCH_XXX</code> cosntants.
   * The leaves 13 bits available as an operand; if that isn't enough the
   * <code>MATCH_WIDE</code> "instruction" can be used to modify the
   * following instruction. */
  String program;

  /** This 3-bit "opcode" is used for shorter operand-less instructions. */
  static final int MATCH_MISC = 0;

  /** Matches <code>List.Empty</code>. */
  static final int MATCH_NIL = (1<<3)+MATCH_MISC;

  /** Matches a vector (FVector).
   * Matches a vecetor v if the following list pattern (at pc+1)
   * matches (vector->list v). */
  static final int MATCH_VECTOR = (2<<3)+MATCH_MISC;

  /** Match anything and ignoe it. */
  static final int MATCH_IGNORE = (3<<3)+MATCH_MISC;

  /** The instruction <code>8*i+MATCH_WIDE</code> is a prefix.
   * It causes <code>i&lt;&lt;13</code> to be added to the parameter
   * (<code>i</code>) of the following instruction. */
  static final int MATCH_WIDE = 1;

  /** The instruction <code>8*i+MATCH_EQUALS</code> matches the literal values literals[i]. */
  static final int MATCH_EQUALS = 2;

  static final int MATCH_ANY = 3;

  /** The instruction <code>8*i+MATCH_PAIR</code> matches a Pair.
   * Its <code>car</code> must match the pattern at <code>pc+1</code>, while
   * its <code>cdr</code> must match the mattern at <code>pc+1+i</code>. */
  static final int MATCH_PAIR = 4;

  /** The instruction <code>8*i+MATCH_LREPEAT</code> matches a repeated
   * pattern.  Followed by followed by var_first, var_count,
   * then ...
   * Old: The pattern which is matched against a variable
   * number of list head is at <code>pc+1</code>.
   * The tail is matchesd <code>pc+1+i</ode>, which must be
   * a <code>MATCH_NIL</code> or start with a <code>MATCH_LENGTH</code>. */
  static final int MATCH_LREPEAT = 5;

  /** The instruction <code>8*i+MATCH_LENGTH</code> matches a pure list
   * of length <code>2*i</code> or an impure list <code>2*i+1</code> pair.
   * It is followed by a pattern which must also match. */
  static final int MATCH_LENGTH = 6;

  Object[] literals;
  int varCount;

  public int varCount() { return varCount; }

  public boolean match (Object obj, Object[] vars, int start_vars)
  {
    return match(obj, vars, start_vars, 0, null);
  }

  public SyntaxPattern (String program, Object[] literals, int varCount)
  {
    this.program = program;
    this.literals = literals;
    this.varCount = varCount;
  }

  public SyntaxPattern (Object pattern,
			Object[] literal_identifiers, Translator tr)
  {
    this(new StringBuffer(), pattern,
	 null, literal_identifiers, tr);
    /*
    StringBuffer programbuf = new StringBuffer();
    this
    translate(pattern, programbuf,
	      literal_identifiers, 0, literalsbuf, null, tr);
    program = programbuf.toString();
    literals = new Object[literalsbuf.size()];
    literalsbuf.copyInto(literals);
    varCount = tr.patternScope.pattern_names.size();
    */
  }

  SyntaxPattern (StringBuffer programbuf, Object pattern,
		 SyntaxForm syntax, Object[] literal_identifiers,
		 Translator tr)
  {
    Vector literalsbuf = new Vector();
    translate(pattern, programbuf,
	      literal_identifiers, 0, literalsbuf, null, tr);
    program = programbuf.toString();
    literals = new Object[literalsbuf.size()];
    literalsbuf.copyInto(literals);
    varCount = tr.patternScope.pattern_names.size();
  }

  public void disassemble ()
  {
    disassemble(OutPort.errDefault(), (Translator) Compilation.getCurrent(),
		0, program.length());
  }

  public void disassemble (java.io.PrintWriter ps, Translator tr)
  {
    disassemble(ps, tr, 0, program.length());
  }

  void disassemble (java.io.PrintWriter ps, Translator tr, int start, int limit)
  {
    Vector pattern_names = null;
    if (tr != null && tr.patternScope != null)
      pattern_names = tr.patternScope.pattern_names;
    int value = 0;
    for (int i = start;  i < limit;  )
      {
	char ch = program.charAt(i);
	ps.print(" " + i + ": " + (int)ch);
	i++;
	int opcode = ch & 7;
	value = (value << 13) | (ch >> 3);
	switch (opcode)
	  {
	  case MATCH_WIDE:
	    ps.println(" - WIDE "+value);
	    continue;
	  case MATCH_EQUALS:
	    ps.print(" - EQUALS["+value+"]");
	    if (literals != null && value >= 0 && value < literals.length)
	      ps.print(literals[value]);
	    ps.println();
	    break;
	  case MATCH_ANY:
	    ps.print(" - ANY["+value+"]");
	    if (pattern_names != null
		&& value >= 0 && value < pattern_names.size())
	      ps.print(pattern_names.elementAt(value));
	    ps.println();
	    break;
	  case MATCH_PAIR:
	    ps.println(" - PAIR["+value+"]");
	    break;
	  case MATCH_LREPEAT:
	    ps.println(" - LREPEAT["+value+"]");
	    disassemble(ps, tr, i, i+value);
	    i += value;
	    ps.println(" " + i + ": - repeat first var:"+(program.charAt(i++)>>3));
	    ps.println(" " + i + ": - repeast nested vars:"+(program.charAt(i++)>>3));
	    break;
	  case MATCH_LENGTH:
	    ps.println(" - LENGTH "+(value>>1)+" pairs. "
		       + (((value&1)==0?"pure list":"impure list")));
	    break;
	  case MATCH_MISC:
	    ps.print("[misc ch:"+(int)ch+" n:"+(int)(MATCH_NIL)+"]");
	    if (ch == MATCH_NIL)
	      {
		ps.println(" - NIL");
		break;
	      }
	    if (ch == MATCH_VECTOR)
	      {
		ps.println(" - VECTOR");
		break;
	      }
	    if (ch == MATCH_IGNORE)
	      {
		ps.println(" - IGNORE");
		break;
	      }
	  default:
	    ps.println(" - "+opcode+'/'+value);
	    break;
	  }
	value = 0;
      }
  }



  void translate (Object pattern, StringBuffer program,
		  Object[] literal_identifiers, int nesting,
		  Vector literals, SyntaxForm syntax,
		  Translator tr)
  {
    PatternScope patternScope = tr.patternScope;
    Vector patternNames = patternScope.pattern_names;
    for (;;)
      {
	while (pattern instanceof SyntaxForm)
	  {
	    syntax = (SyntaxForm) pattern;
	    pattern = syntax.form;
	  }
	if (pattern instanceof Pair)
	  {
	    Object savePos = tr.pushPositionOf(pattern);
	    try
	      {
		int start_pc = program.length();
		program.append((char) MATCH_PAIR);
		Pair pair = (Pair) pattern;
		Object next = pair.cdr;
		while (next instanceof SyntaxForm)
		  {
		    syntax = (SyntaxForm) next;
		    next = syntax.form;
		  }
		boolean repeat = false;
		if (next instanceof Pair
		    // FIXME - hygiene!
		    && ((Pair) next).car == SyntaxRule.dots3)
		  {
		    repeat = true;
		    next = ((Pair) next).cdr;
		    while (next instanceof SyntaxForm)
		      {
			syntax = (SyntaxForm) next;
			next = syntax.form;
		      }
		  }

		int subvar0 = patternNames.size();
		translate(pair.car, program, literal_identifiers,
			  repeat ? nesting + 1 : nesting,
			  literals, syntax, tr);
		int subvarN = patternNames.size() - subvar0;
		int width = ((program.length() - start_pc - 1) << 3)
		  | (repeat ? MATCH_LREPEAT : MATCH_PAIR);
		if (width > 0xFFFF)
		  start_pc += insertInt(start_pc, program,
					(width >> 13) + MATCH_WIDE);
		program.setCharAt(start_pc, (char) width);

		int restLength = Translator.listLength(next);
		if (restLength == Integer.MIN_VALUE)
		  {
		    tr.syntaxError("cyclic pattern list");
		    return;
		  }

		if (repeat)
		  {
		    addInt(program, subvar0 << 3);
		    addInt(program, subvarN << 3);
		    if (next == LList.Empty)
		      {
			program.append((char) MATCH_NIL);
			return;
		      }
		    else
		      {
			// Map a signed int to an unsigned.
			restLength = restLength >= 0 ? restLength << 1
			  : ((-restLength) << 1) + 1;
			addInt(program, (restLength << 3) | MATCH_LENGTH);
		      }
		  }

		pattern = next;
		continue;
	      }
	    finally
	      {
		tr.popPositionOf(savePos);
	      }
	  }
	else if (pattern instanceof String || pattern instanceof Symbol)
	  {
	    for (int j = literal_identifiers.length;  --j >= 0; )
	      {
		// NOTE - should also generate check that the binding of the
		// pattern at macro definition time matches that at macro
		// application type. FIXME.
		if (literal_identifiers[j] == pattern)
		  {
		    int i = SyntaxTemplate.indexOf(literals, pattern);
		    if (i < 0)
		      {
			i = literals.size();
			literals.addElement(pattern);
		      }
		    addInt(program, (i << 3) | MATCH_EQUALS);
		    return;
		  }
	      }
	    if (patternNames.contains(pattern))
	      tr.syntaxError("duplicated pattern variable " + pattern);
	    int i = patternNames.size();
	    patternNames.addElement(pattern);
	    patternScope.pattern_nesting.append((char) nesting);
	    tr.push(patternScope.addDeclaration(pattern));
	    addInt(program, (i << 3) | MATCH_ANY);
	    return;
	  }
	else if (pattern == LList.Empty)
	  {
	    program.append((char) MATCH_NIL);
	    return;
	  }
	else if (pattern instanceof FVector)
	  {
	    program.append((char) MATCH_VECTOR);
	    pattern = LList.makeList((FVector) pattern);
	    continue;
	  }
	else
	  {
	    int i = SyntaxTemplate.indexOf(literals, pattern);
	    if (i < 0)
	      {
		i = literals.size();
		literals.addElement(pattern);
	      }
	    addInt(program, (i << 3) | MATCH_EQUALS);
	    return;
	  }
      }
  }

  private static void addInt (StringBuffer sbuf, int val)
  {
    if (val > 0xFFFF)
      addInt(sbuf, (val << 13) + MATCH_WIDE);
    sbuf.append((char) (val));
  }

  private static int insertInt (int offset, StringBuffer sbuf, int val)
  {
    if (val > 0xFFFF)
      offset += insertInt(offset, sbuf, (val << 13) + MATCH_WIDE);
    sbuf.insert(offset, (char) (val));
    return offset+1;
  }

  public boolean match (Object obj, Object[] vars, int start_vars,
			int pc, SyntaxForm syntax)
  {
    int value = 0;
    Pair p;
    for (;;)
      {
	while (obj instanceof SyntaxForm)
	  {
	    syntax = (SyntaxForm) obj;
	    obj = syntax.form;
	  }
	char ch = program.charAt(pc++);
	int opcode = ch & 7;
	value = (value << 13) | (ch >> 3);
	switch (opcode)
	  {
	  case MATCH_WIDE:
	    continue;
	  case MATCH_MISC:
	    if (ch == MATCH_NIL)
	      return obj == LList.Empty;
	    else if (ch == MATCH_VECTOR)
	      {
		if (! (obj instanceof FVector))
		  return false;
		return match(LList.makeList((FVector) obj),
			     vars, start_vars, pc, syntax);
	      }
	    else if (ch == MATCH_IGNORE)
	      return true;
	    else
	      throw new Error("unknwon pattern opcode");
	  case MATCH_NIL:
	    return obj == LList.Empty;
	  case MATCH_LENGTH:
	    int npairs = value>>1;
	    Object o = obj;
	    for (int i = 0;;i++)
	      {
		while (o instanceof SyntaxForm)
		  o = ((SyntaxForm) o).form;
		if (i == npairs)
		  {
		    if ((value&1) == 0)
		      {
			if (o == LList.Empty)
			  break;
			return false;
		      }
		    else
		      {
			return false;  // FIXME
		      }
		  }
		else if (! (o instanceof Pair))
		  return false;
		o = ((Pair) o).cdr;
	      }
	    value = 0;
	    continue;
	  case MATCH_PAIR:
	    if (! (obj instanceof Pair))
	      return false;
	    p = (Pair) obj;

	    /*
	    opcode = (ch = program.charAt(pc++)) >> 3;
	    while ((ch & 0x7) == MATCH_WIDE)
	      opcode = (opcode << 13) | ((ch = program.charAt(pc++)) >> 3);

	    if ((ch & 0x7) == MATCH_ANY)
	      {
	      }
	    */

	    if (! match(p.car, vars, start_vars, pc, syntax))
	      return false;
	    pc += value;
	    value = 0;
	    obj = p.cdr;
	    continue;	
	  case MATCH_LREPEAT:
	    int repeat_pc = pc;
	    pc += value;
	    int subvar0 = (ch = program.charAt(pc++)) >> 3;
	    while ((ch & 0x7) == MATCH_WIDE)
	      subvar0 = (subvar0 << 13) | ((ch = program.charAt(pc++)) >> 3);
	    subvar0 += start_vars;
	    int subvarN = program.charAt(pc++) >> 3;
	    while ((ch & 0x7) == MATCH_WIDE)
	      subvarN = (subvarN << 13) | ((ch = program.charAt(pc++)) >> 3);

	    ch = program.charAt(pc++);
	    boolean listRequired = true;
	    int pairsRequired;
	    if (ch == MATCH_NIL)
	      {
		pairsRequired = 0;
	      }
	    else
	      {
		value = ch >> 3;
		while ((ch & 0x7) == MATCH_WIDE)
		  value = (value << 13) | ((ch = program.charAt(pc++)) >> 3);
		if ((value & 1) != 0)
		  {
		    listRequired = false;
		    value--;
		  }
		pairsRequired = value >> 1;
		value = (value & 1) == 0 ? value >> 1
		  : (1 - value) >> 1;
	      }
	    int pairsValue = Translator.listLength(obj);
	    boolean listValue;

	    if (pairsValue >= 0)
	      listValue = true;
	    else
	      {
		listValue = false;
		pairsValue = -pairsValue;
	      }
	    if (listValue != listRequired || pairsValue < pairsRequired)
	      return false;
	    int repeat_count = pairsValue - pairsRequired;
	    Object[][] arrays = new Object[subvarN][];

	    for (int j = 0;  j < subvarN;  j++)
	      arrays[j] = new Object[repeat_count];
	    for (int i = 0;  i < repeat_count;  i++)
	      {
		while (obj instanceof SyntaxForm)
		  {
		    syntax = (SyntaxForm) obj;
		    obj = syntax.form;
		  }
		p = (Pair) obj;
		if (! match (p.car, vars, start_vars, repeat_pc, syntax))
		  return false;
		obj = p.cdr;
		for (int j = 0;  j < subvarN;  j++)
		  arrays[j][i] = vars[subvar0+j];
	      }
	    for (int j = 0;  j < subvarN;  j++)
	      vars[subvar0+j] = arrays[j];
	    value = 0;
	    if (pairsRequired == 0)
	      return true;
	    continue;
	  case MATCH_EQUALS:
	    Object lit = literals[value];
	    // We should be using Translator's matches routine, but the current
	    // Translator isn't available, so here is a special-purpose kludge.
	    if (lit instanceof String && obj instanceof Symbol)
	      obj = ((Symbol) obj).getName();
	    return lit.equals(obj);
	  case MATCH_ANY:
	    if (syntax != null)
	      obj = syntax.fromDatum(obj);
	    vars[start_vars + value] = obj;
	    return true;
	  default:
	    throw new Error("unrecognized pattern opcode");
	  }
      }
  }
  
  public void writeExternal(ObjectOutput out) throws IOException
  {
    out.writeObject(program);
    out.writeObject(literals);
    out.writeInt(varCount);
  }

  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException
  {
    literals = (Object[]) in.readObject();
    program = (String)  in.readObject();
    varCount = in.readInt();
  }
}
