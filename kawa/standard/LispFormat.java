package kawa.standard;
import gnu.text.*;
import gnu.kawa.util.*;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParseException;
import java.io.Writer;
import gnu.math.*;
import gnu.mapping.OutPort;

/** A representation of a parsed Common Lisp-style format. */

public class LispFormat extends CompoundFormat
{
  public static final String paramFromList = "<from list>";
  public static final String paramFromCount = "<from count>";
  public static final String paramUnspecified = "<unspecified>";

  public LispFormat(char[] format, int offset, int length)
    throws ParseException
  {
    super(null, 0);
    // The index in spec of the most recent ~{, ~(, ~{ or ~[.
    int start_nesting = -1;
    int choices_seen = 0;  // Number of "~;" seen.

    StringBuffer litbuf = new StringBuffer(100);
    java.util.Stack stack = new java.util.Stack();

    int limit = offset + length;
    int i = offset;
    for (;;)
      {
	if ((i >= limit || format[i] == '~') && litbuf.length() > 0)
	  {
	    stack.push(new LiteralFormat(litbuf));
	    litbuf.setLength(0);
	  }
	if (i >= limit)
	  break;
	char ch = format[i++];
	if (ch != '~')
	  {
	    litbuf.append(ch);
	    continue;
	  }
	int speci = stack.size();
	ch = format[i++];
	for (;;)
	  {
	    if (ch == '#')
	      {
		stack.push(paramFromCount);
		ch = format[i++];
	      }
	    else if (ch == 'v' || ch == 'V')
	      {
		stack.push(paramFromList);
		ch = format[i++];
	      }
	    else if (ch == '-' || Character.digit(ch, 10) >= 0)
	      {
		boolean neg = (ch == '-');
		if (neg)
		  ch = format[i++];
		int val = 0;
		int start = i;
		for (;;)
		  {
		    int dig = Character.digit(ch, 10);
		    if (dig < 0)
		      break;
		    val = 10 * val + dig;
		    ch = format[i++];
		  }
		stack.push(i - start < 8 ? IntNum.make(neg ? - val : val)
			   : IntNum.valueOf(format, start, i-start, 10, neg));
	      }
	    else if (ch == '\'')
	      {
		stack.push(Char.make(format[i++]));
		ch = format[i++];
	      }
	    else if (ch == ',')
	      {
		stack.push(paramUnspecified);
	      }
	    else
	      break;
	    if (ch != ',')
	      break;
	    ch = format[i++];
	  }
	boolean seenColon = false;
	boolean seenAt = false;
	for (;;)
	  {
	    if (ch == ':')
	      seenColon = true;
	    else if (ch == '@')
	      seenAt = true;
	    else
	      break;
	    ch = format[i++];
	  }
	ch = Character.toUpperCase(ch);
	int numParams = stack.size() - speci;
	Format fmt;
	int minWidth, padChar, charVal, param1, param2, param3;
	switch (ch)
	  {
	  case 'R':  case 'D':  case 'O':  case 'B':  case 'X':
	    int argstart = speci;
	    int base;
	    if (ch == 'R')  base = getParam(stack, argstart++);
	    else if (ch == 'D')  base = 10;
	    else if (ch == 'O')  base = 8;
	    else if (ch == 'X')  base = 16; 
	    else base = 2;
	    minWidth = getParam(stack, argstart);
	    padChar = getParam(stack, argstart+1);
	    int commaChar = getParam(stack, argstart+2);
	    int commaInterval = getParam(stack, argstart+3);
            int flags = 0;
            if (seenColon)
              flags |= IntegerFormat.SHOW_GROUPS;
            if (seenAt)
              flags |= IntegerFormat.SHOW_PLUS;
	    fmt = IntegerFormat.getInstance(base, minWidth, padChar,
                                            commaChar, commaInterval, flags);
	    break;
	  case 'P':
	    fmt = LispPluralFormat.getInstance(seenColon, seenAt);
	    break;
	  case 'E':
	  case 'F':
	  case 'G':
	  case '$':
	    LispRealFormat dfmt = new LispRealFormat();
	    dfmt.op = ch;
	    dfmt.arg1 = getParam(stack, speci);
	    dfmt.arg2 = getParam(stack, speci+1);
	    dfmt.arg3 = getParam(stack, speci+2);
	    dfmt.arg4 = getParam(stack, speci+3);
	    if (ch != '$')
	      {
		dfmt.arg5 = getParam(stack, speci+4);
		if (ch == 'E' || ch == 'G')
		  {
		    dfmt.arg6 = getParam(stack, speci+5);
		    dfmt.arg7 = getParam(stack, speci+6);
		  }
	      }
	    dfmt.showPlus = seenAt;
	    dfmt.internalPad = seenColon;
	    if (dfmt.argsUsed == 0)
	      fmt = dfmt.resolve(null, 0);
	    else
	      fmt = dfmt;
	    break;
	  case 'A':  case 'S':
	    fmt = ObjectFormat.getInstance(ch == 'S');
	    if (numParams > 0)
	      {
		minWidth = getParam(stack, speci);
		int colInc = getParam(stack, speci+1);
		int minPad = getParam(stack, speci+2);
		padChar = getParam(stack, speci+3);
		fmt = new LispObjectFormat((ReportFormat) fmt,
					   minWidth, colInc, minPad,
					   padChar, seenAt ? 0 : 100);
	      }
	    break;
	  case 'C':
	    charVal = numParams > 0 ? getParam(stack, speci)
	      : PARAM_FROM_LIST;
	    fmt = LispCharacterFormat.getInstance(charVal, 1,
						  seenAt, seenColon);
	    break;
	  case '*':
	    fmt = new LispRepositionFormat(getParam(stack, speci),
					   seenColon, seenAt);
	    break;
	  case '(':
	    ch = seenColon ? (seenAt ? 'U' : 'C') : (seenAt ? 'T': 'L');
	    CaseConvertFormat cfmt = new CaseConvertFormat(null, ch);
	    stack.setSize(speci);
	    stack.push(cfmt);
	    stack.push(IntNum.make(start_nesting));
	    start_nesting = speci;
	    continue;
	  case ')':
	    if (start_nesting < 0
		|| ! (stack.elementAt(start_nesting)
		      instanceof CaseConvertFormat))
	      throw new ParseException("saw ~) without matching ~(", i);
	    cfmt = (CaseConvertFormat) stack.elementAt(start_nesting);
	    cfmt.setBaseFormat(popFormats(stack, start_nesting + 2, speci));
	    start_nesting = ((IntNum) stack.pop()).intValue();
	    continue;
	  case '?':
	    LispIterationFormat lfmt = new LispIterationFormat();
	    lfmt.seenAt = seenAt;
	    lfmt.maxIterations = 1;
	    lfmt.atLeastOnce = true;
	    fmt = lfmt;
	    break;
	  case '{':
	    lfmt = new LispIterationFormat();
	    lfmt.seenAt = seenAt;
	    lfmt.seenColon = seenColon;
	    lfmt.maxIterations = getParam(stack, speci);
	    stack.setSize(speci);
	    stack.push(lfmt);
	    stack.push(IntNum.make(start_nesting));
	    start_nesting = speci;
	    continue;
	  case '}':
	    if (start_nesting < 0
		|| ! (stack.elementAt(start_nesting)
		      instanceof LispIterationFormat))
	      throw new ParseException("saw ~} without matching ~{", i);
	    lfmt = (LispIterationFormat) stack.elementAt(start_nesting);
	    lfmt.atLeastOnce = seenColon;
	    if (speci > start_nesting + 2)
	      lfmt.body = popFormats(stack, start_nesting + 2, speci);
	    start_nesting = ((IntNum) stack.pop()).intValue();
	    continue;
	  case '[':
	    LispChoiceFormat afmt = new LispChoiceFormat();
	    afmt.param = getParam(stack, speci);
	    if (afmt.param == PARAM_UNSPECIFIED)
	      afmt.param = PARAM_FROM_LIST;
	    if (seenColon)
	      afmt.testBoolean = true;
	    if (seenAt)
	      afmt.skipIfFalse = true;
	    stack.setSize(speci);
	    stack.push(afmt);
	    stack.push(IntNum.make(start_nesting));
	    stack.push(IntNum.make(choices_seen));
	    start_nesting = speci;
	    choices_seen = 0;
	    continue;
	  case ';':
	    if (start_nesting >= 0)
	      {
		if (stack.elementAt(start_nesting)
		    instanceof LispChoiceFormat)
		  {
		    afmt = (LispChoiceFormat) stack.elementAt(start_nesting);
		    if (seenColon)
		      afmt.lastIsDefault = true;
		    fmt = popFormats(stack,
				     start_nesting + 3 + choices_seen, speci);
		    stack.push(fmt);
		    choices_seen++;
		    continue;
		  }
		// else if saw ~< ...
	      }
	    throw new ParseException("saw ~; without matching ~[ or ~<", i);
	  case ']':
	    if (start_nesting < 0
		|| ! (stack.elementAt(start_nesting)
                      instanceof LispChoiceFormat))
              throw new ParseException("saw ~] without matching ~[", i);
	    fmt = popFormats(stack, start_nesting + 3 + choices_seen, speci);
	    stack.push(fmt);
	    afmt = (LispChoiceFormat) stack.elementAt(start_nesting);
	    afmt.choices = getFormats(stack, start_nesting + 3, stack.size());
	    stack.setSize(start_nesting + 3);
	    choices_seen = ((IntNum) stack.pop()).intValue();
	    start_nesting = ((IntNum) stack.pop()).intValue();
	    continue;
	  case '^':
	    param1 = getParam(stack, speci);
	    param2 = getParam(stack, speci+1);
	    param3 = getParam(stack, speci+2);
	    fmt = new LispEscapeFormat(param1, param2, param3);
	    break;
	  case '\n':
	    if (seenAt)
	      litbuf.append(ch);
	    if (! seenColon)
	      {
		while (i < limit)
		  {
		    ch = format[i++];
		    if (! Character.isWhitespace(ch))
		      {
			i--;
			break;
		      }
		  }
	      }
	    continue;
	  case '!':
	    fmt = FlushFormat.getInstance();
	    break;
	  case 'T':
	    param1 = getParam(stack, speci);
	    param2 = getParam(stack, speci+1);
	    param3 = getParam(stack, speci+2);
	    fmt = new LispTabulateFormat(param1, param2, param3, seenAt);
	    break;
	  case '&':
	    param1 = getParam(stack, speci);
	    fmt = new LispFreshlineFormat(param1);
	    break;
	  case '_':  // FIXME actually conditional newline
	    param1 = getParam(stack, speci);
	    if (param1 == PARAM_UNSPECIFIED)
	      param1 = 1;
	    charVal = seenColon && seenAt ? '\n' : ' ';
	    fmt = LispCharacterFormat.getInstance(charVal, param1,
						  false, false);
	    break;
	  case '~':
	    if (numParams == 0)
	      {
		litbuf.append(ch);
		continue;
	      }
	    /* ... otherwise fall through ... */
	  case '|':
	  case '%':
	    int count = getParam(stack, speci);
	    if (count == PARAM_UNSPECIFIED)
	      count = 1;
	    // EXTENSION:  Allow repeating other characters than '~'.
	    charVal = getParam(stack, speci+1);
	    if (charVal == PARAM_UNSPECIFIED) 
	      charVal = ch == '|' ? '\f' : ch == '%' ? '\n' : '~';
	    fmt = LispCharacterFormat.getInstance(charVal, count,
						  false, false);
	    break;
          default:
	    throw new ParseException("unrecognized format specifier ~"+ch, i);
          }
	stack.setSize(speci);
	stack.push(fmt);
      }
    if (i > limit)
      throw new IndexOutOfBoundsException();
    if (start_nesting >= 0)
      {
	throw new ParseException("missing ~] or ~}", i);
      }
    this.length = stack.size();
    this.formats = new Format[this.length];
    stack.copyInto(this.formats);
  }

  static Format[] getFormats(java.util.Vector vector, int start, int end)
  {
    Format[] f = new Format[end - start];
    for (int i = start;  i < end;  i++)
      f[i - start] = (Format) vector.elementAt(i);
    return f;
  }

  static Format popFormats(java.util.Vector vector, int start, int end)
  {
    Format f;
    if (end == start + 1)
      f = (Format) vector.elementAt(start);
    else
      f = new CompoundFormat(getFormats(vector, start, end));
    vector.setSize(start);
    return f;
  }

  public LispFormat (String str)
    throws ParseException
  {
    this(str.toCharArray());
  }

  /*
  private void clearSpecs (int speci, int max)
  {
    int num = specs_length - speci - 1;
    for (int i = num;  i < max;  i++)
      addSpec(PARAM_UNSPECIFIED);
    specs_length = speci + 1;
  }
  */

  /*
  private void addSpec(Format fmt)
  {
    if (formats == null)
      formats = new Format[4];
    else
      {
	if (this.length == formats.length)
	  {
	    Format[] newformats = new Format[2 * this.length];
	    System.arraycopy(formats, 0, newformats, 0, this.length);
	    formats = newformats;
	  }
      }
    formats[this.length] = fmt;
    addSpec(this.length);
    this.length++;
  }
  */

  /*
  private void addSpec(int val)
  {
    //System.err.println("addSpec("+val+") at:"+specs_length);
    int old_size = specs.length;
    if (specs_length >= old_size)
      {
	int[] new_specs = new int[2 * old_size];
	System.arraycopy(specs, 0, new_specs, 0, old_size);
	specs = new_specs;
      }
    specs[specs_length++] = val;
  }
  */

  public LispFormat(char[] format)
    throws ParseException
  {
    this(format, 0, format.length);
  }

  public static int getParam(java.util.Vector vec, int index)
  {
    if (index >= vec.size())
      return PARAM_UNSPECIFIED;
    Object arg = vec.elementAt(index);
    if (arg == paramFromList)
      return PARAM_FROM_LIST;
    if (arg == paramFromCount)
      return PARAM_FROM_COUNT;
    if (arg == paramUnspecified)
      return PARAM_UNSPECIFIED;
    return getParam(arg, PARAM_UNSPECIFIED);
  }

  /** Convert sequence (or Object[]) to Object[].
   * Return null if not a valid Sequence. */
  public static Object[] asArray (Object arg)
  {
    if (arg instanceof Object[])
      return (Object[]) arg;
    if (!(arg instanceof Sequence))
      return null;
    int count = ((Sequence) arg).length();
    Object[] arr = new Object[count];
    int i = 0;
    while (arg instanceof Pair)
      {
	Pair pair = (Pair) arg;
	arr[i++] = pair.car;
	arg = pair.cdr;
      }
    if (i < count)
      {
	if (! (arg instanceof Sequence))
	  return null;
	int npairs = i;
	Sequence seq = (Sequence) arg;
	for (; i < count; i++)
	  arr[i] = seq.elementAt(npairs + i);
      }
    return arr;
  }
}

/** Add plural suffixes ("s" or "y/ies") of English words.
 * Used to implement the Common Lisp ~P ('Plural') format operator. */

class LispPluralFormat extends ReportFormat
{
  boolean backup;
  boolean y;

  public static LispPluralFormat getInstance (boolean backup, boolean y)
  {
    LispPluralFormat fmt = new LispPluralFormat();
    fmt.backup = backup;
    fmt.y = y;
    return fmt;
  }

  public int format(Object[] args, int start, 
		    Writer dst, FieldPosition fpos) 
    throws java.io.IOException
  {
    if (backup)
      start--;
    Object arg = args[start++];
    boolean plural = arg != IntNum.one();
    if (y)
      print(dst, plural ? "ies" : "y");
    else if (plural)
      dst.write('s');
    return start;
  }
}


/** Handle formatting of characters.
 * Used to implement the Common List ~C (Character) and ~~ (Tilde)
 * format operators. */

class LispCharacterFormat extends ReportFormat
{
  boolean seenAt;
  boolean seenColon;
  int count;
  int charVal;

  public static LispCharacterFormat
  getInstance(int charVal, int count, boolean seenAt, boolean seenColon)
  {
    LispCharacterFormat fmt = new LispCharacterFormat();
    fmt.count = count;
    fmt.charVal = charVal;
    fmt.seenAt = seenAt;
    fmt.seenColon = seenColon;
    return fmt;
  }

  public int format(Object[] args, int start, 
		    Writer dst, FieldPosition fpos) 
    throws java.io.IOException
  {
    int count = getParam(this.count, 1, args, start);
    if (this.count == LispFormat.PARAM_FROM_LIST)  start++;
    int charVal = getParam(this.charVal, '?', args, start);
    if (this.charVal == LispFormat.PARAM_FROM_LIST)  start++;
    while (--count >= 0)
      printChar (charVal, seenAt, seenColon, dst);
    return start;
  }

  public static void printChar(int ch, boolean seenAt, boolean seenColon,
			       Writer dst)
    throws java.io.IOException
  {
    if (seenAt)
      {
	print(dst, Char.toScmReadableString(ch));
      }
    else if (seenColon)
      {
	if (ch < ' ')
	  {
	    dst.write('^');
	    dst.write(ch + 0x40);
	  }
	else if (ch >= 0x7f)
	  {
	    print(dst, "#\\");
	    print(dst, Integer.toString(ch, 8));
	  }
	else
	  dst.write(ch);
      }
    else
      {
	// if (ch > 0xFFFF) print surrogate chars; else
	dst.write(ch);
      }
  }
}

/** Perform general padding.
 * Used to implement the Common Lisp ~A (Ascii) and ~ (S-expression),
 * format operators, unless they have no parameters. */

class LispObjectFormat extends ReportFormat
{
  int minWidth;
  int colInc;
  int minPad;
  int padChar;
  int where;
  ReportFormat base;

  public LispObjectFormat(ReportFormat base,
			  int minWidth, int colInc, int minPad, int padChar,
			  int where)
  {
    this.base = base;
    this.minWidth = minWidth;
    this.colInc = colInc;
    this.minPad = minPad;
    this.padChar = padChar;
    this.where = where;
  }

  public int format(Object[] args, int start, 
		    Writer dst, FieldPosition fpos) 
    throws java.io.IOException
  {
    int minWidth = getParam(this.minWidth, 0, args, start);
    if (this.minWidth == LispFormat.PARAM_FROM_LIST)  start++;
    int colInc = getParam(this.colInc, 1, args, start);
    if (this.colInc == LispFormat.PARAM_FROM_LIST)  start++;
    int minPad = getParam(this.minPad, 0, args, start);
    if (this.minPad == LispFormat.PARAM_FROM_LIST)  start++;
    char padChar = getParam(this.padChar, ' ', args, start);
    if (this.padChar == LispFormat.PARAM_FROM_LIST)  start++;
    return gnu.text.PadFormat.format(base, args, start, dst,
				     padChar, minWidth, colInc, minPad,
				     where, fpos);
  }
}

class LispEscapeFormat extends ReportFormat 
{
  int param1;
  int param2;
  int param3;
  boolean escapeAll;

  public final static LispEscapeFormat alwaysTerminate
  = new LispEscapeFormat(0, LispFormat.PARAM_UNSPECIFIED);

  public LispEscapeFormat(int param1, int param2)
  {
    this.param1 = param1;
    this.param2 = param2;
    this.param3 = LispFormat.PARAM_UNSPECIFIED;
  }

  public LispEscapeFormat(int param1, int param2, int param3)
  {
    this.param1 = param1;
    this.param2 = param2;
    this.param3 = param3;
  }

  static Numeric getParam(int param, Object[] args, int start)
  {
    if (param == LispFormat.PARAM_FROM_COUNT)
      return IntNum.make(args.length - start);
    if (param == LispFormat.PARAM_FROM_LIST)
      {
	Object arg = args[start];
	if (arg instanceof Numeric)
	  return (Numeric) arg;
	if (arg instanceof Number)
	  {
	    if (arg instanceof Float || arg instanceof Double)
	      return new DFloNum(((Number) arg).doubleValue());
	    return IntNum.make(((Number) arg).longValue());
	  }
	if (arg instanceof Char)
	  return new IntNum(((Char) arg).intValue());
	if (arg instanceof Character)
	  return new IntNum((int) ((Character) arg).charValue());
	return new DFloNum(Double.NaN);
      }
    return IntNum.make(param);
  }

  /** WRONG: Tests if we should exit the the surrounding format.
   * Returns 2*ARGS_USED+(DO_TERMINATE?1:0), where ARGS_USED is the
   * number of arguments consumed by the specification, and
   * DO_TERMINATE is true if we actually should exit.
   */
  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)
    throws java.io.IOException
  {
    int orig_start = start;
    boolean do_terminate;
    if (param1 == LispFormat.PARAM_UNSPECIFIED)
      do_terminate = start == args.length;
    else if (param2 == LispFormat.PARAM_UNSPECIFIED && param1 == 0)
      do_terminate = true;  // Efficiency hack
    else
      {
	Numeric arg1 = getParam(param1, args, start);
	if (param1 == LispFormat.PARAM_FROM_LIST)  start++;
	if (param2 == LispFormat.PARAM_UNSPECIFIED)
	  {
	    do_terminate = arg1.isZero();
	  }
	else
	  {
	    Numeric arg2 = getParam(param2, args, start);
	    if (param2 == LispFormat.PARAM_FROM_LIST)  start++;
	    if (param3 == LispFormat.PARAM_UNSPECIFIED)
	      {
		do_terminate = arg1.equals(arg2);
	      }
	    else
	      {
		Numeric arg3 = getParam(param3, args, start);
		if (param3 == LispFormat.PARAM_FROM_LIST)  start++;
		do_terminate = arg2.geq(arg1) && arg3.geq(arg2);
	      }
	  }
      }
    return result(! do_terminate ? 0 : escapeAll ? ESCAPE_ALL : ESCAPE_NORMAL,
		  start);
  }

  public final static int ESCAPE_NORMAL = 0xF1;
  public final static int ESCAPE_ALL = 0xF2;
}

class LispIterationFormat extends ReportFormat
{
  int maxIterations;
  boolean seenAt;
  boolean seenColon;
  boolean atLeastOnce;

  Format body;

  public static int format(Format body, int maxIterations,
			   Object[] args, int start, 
			   Writer dst, boolean seenColon, boolean atLeastOnce)
    throws java.io.IOException
  {
    for (int i = 0; ; i++)
      {
	//System.err.println("it:"+i+" start:"+start+" maxIt:"+maxIterations);
	if (i == maxIterations && maxIterations != -1)
	  break;
	if (start == args.length && (i > 0 || ! atLeastOnce))
	  break;
	//System.err.println("loop:i:"+i+ " start:"+start);
	if (seenColon)
	  {
	    Object curArg = args[start];
	    Object[] curArr = LispFormat.asArray(curArg);
	    if (curArr == null)
	      { // ?
	      }
	    int result = ReportFormat.format(body, curArr, 0, dst, null);
	    start++;
	    if (ReportFormat.resultCode(result) == LispEscapeFormat.ESCAPE_ALL)
	      break;
	  }
	else
	  {
	    start = ReportFormat.format(body, args, start, dst, null);
	    if (start < 0)
	      {
		start = ReportFormat.nextArg(start);
		break;
	      }
	  }
      }
    return start;
  }

  public int format(Object[] args, int start,
                    Writer dst, FieldPosition fpos)  
    throws java.io.IOException
  {
    int maxIterations = getParam(this.maxIterations, -1,
					    args, start);
    if (this.maxIterations == LispFormat.PARAM_FROM_LIST)  start++;

    Format body = this.body;
    if (body == null)
      {
	// from args
	Object arg = args[start++];
	if (arg instanceof java.text.Format)
	  body = (java.text.Format) arg;
	else
	  {
	    try
	      {
		body = new LispFormat(arg.toString());
	      }
	    catch (Exception ex)
	      {
		print(dst, "<invalid argument for \"~{~}\" format>");
		return args.length; // FIXME
	      }
	  }
      }
    if (seenAt)
      {
	return format(body, maxIterations, args, start,
		      dst, seenColon, atLeastOnce);
      }
    else
      {
	Object arg = args[start];
	Object[] curArgs = LispFormat.asArray(arg);
	if (curArgs == null)
	  dst.write("{"+arg+"}".toString());
	else
	  format(body, maxIterations, curArgs, 0, 
		 dst, seenColon, atLeastOnce);
	//System.err.println("after-start:"+(start+1)+" len:"+args.length);
	return start + 1;
      }
    /*
    Object[] curArgs;
    int curStart;
    if (seenAt)
      {
	curArgs = args;
	curStart = start;
      }
    else
      {
	Object arg = args[start];
	curArgs = LispFormat.asArray(arg);
	if (curArgs == null)
	  {
	    dst.write("{"+arg+"}".toString());
	    maxIterations = 0;
	  }
	curStart = 0;
      }      
    */
    /*
    for (int i = 0; ; i++)
      {
	//System.err.println("it:"+i+" curStart:"+curStart);
	if (i == maxIterations && maxIterations != -1)
	  break;
	if (curStart == curArgs.length && (i > 0 || ! atLeastOnce))
	  break;
	//System.err.println("loop:i:"+i+ " curStart:"+curStart);
	if (seenColon)
	  {
	    Object curArg = curArgs[curStart];
	    Object[] curArr = LispFormat.asArray(curArg);
	    if (curArr == null)
	      { // ?
	      }
	    ReportFormat.format(body, curArr, 0, dst, fpos);
	    curStart++;
	  }
	else
	  curStart = ReportFormat.format(body, curArgs, curStart, dst, fpos);
	if (curStart < 0)
	  break;
      }
    return seenAt ? curStart : start+1;
    */
  }
}

class LispChoiceFormat extends ReportFormat
{
  int param;
  boolean lastIsDefault;
  boolean testBoolean;  // choice[0] is selected if arg is false.
  boolean skipIfFalse;
  Format[] choices;

  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)  
    throws java.io.IOException 
  {
    Format fmt;
    if (testBoolean)  // Handles ~:[false~;true~]
      {
	fmt = choices[args[start] == Boolean.FALSE ? 0 : 1];
	start++;
      }
    else if (! skipIfFalse)
      {
	int index = getParam(this.param, LispFormat.PARAM_FROM_LIST,
					args, start);
	if (param == LispFormat.PARAM_FROM_LIST)  start++;
	if (index < 0 || index >= choices.length)
	  {
	    if (lastIsDefault)
	      index = choices.length - 1;
	    else
	      return start;
	  }
	fmt = choices[index];
      }
    else
      {
	if (args[start] == Boolean.FALSE)
	  return start + 1;
	fmt = choices[0];
      }
    return ReportFormat.format(fmt, args, start, dst, fpos);
  }
}

class LispRepositionFormat extends ReportFormat
{
  boolean backwards;
  boolean absolute;
  int count;

  public LispRepositionFormat(int count, boolean backwards, boolean absolute)
  {
    this.count = count;
    this.backwards = backwards;
    this.absolute = absolute;
  }

  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)  
    throws java.io.IOException 
  {
    int count = getParam(this.count, absolute ? 0 : 1,
				    args, start);
    if (!absolute)
      {
	if (backwards)
	  count = -count;
	count += start;
      }
    return count < 0 ? 0 : count > args.length ? args.length : count;
  }
}

class LispFreshlineFormat  extends ReportFormat
{
  int count;

  public LispFreshlineFormat (int count)
  {
    this.count = count;
  }

  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)  
    throws java.io.IOException 
  {
    int count = getParam(this.count, 1, args, start);
    if (this.count == LispFormat.PARAM_FROM_LIST)  start++;
    int column = -1;
    if (dst instanceof OutPort)
      column = ((OutPort) dst).getColumnNumber();
    if (column == 0)
      count--;
    while (--count >= 0)
      dst.write('\n');
    return start; 
  }
}

class LispTabulateFormat extends ReportFormat
{
  boolean relative;
  int colnum;
  int colinc;
  int padChar;

  public LispTabulateFormat(int colnum, int colinc,
			    int padChar, boolean relative)
  {
    this.colnum = colnum;
    this.colinc = colinc;
    this.relative = relative;
    this.padChar = padChar;
  }

  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)  
    throws java.io.IOException 
  {
    int colnum = getParam(this.colnum, 1, args, start);
    if (this.colnum == LispFormat.PARAM_FROM_LIST)  start++;
    int colinc = getParam(this.colinc, 1, args, start);
    if (this.colinc == LispFormat.PARAM_FROM_LIST)  start++;
    // Extension from SLIB:
    char padChar = getParam(this.padChar, ' ', args, start); 
    if (this.padChar == LispFormat.PARAM_FROM_LIST)  start++; 
    int column = -1;
    if (dst instanceof OutPort)
      column = ((OutPort) dst).getColumnNumber();
    int spaces;
    if (column >= 0)
      {
	if (! relative)
	  {
	    if (column < colnum)
	      spaces = colnum - column;
	    else if (colinc <= 0)
	      spaces = 0;
	    else
	      spaces = colinc - (column - colnum) % colinc;
	  }
	else
	  {
	    spaces = colnum + colinc - (column + colnum) % colinc;
	  }
      }
    else
      {
	spaces = relative ? colnum : 2;
      }
    while (--spaces >= 0)
      dst.write(padChar);
    return start;
  }
}

/* Support for ~F, ~$, ~E, ~G. */

class LispRealFormat extends ReportFormat
{
  char op;
  int arg1;
  int arg2;
  int arg3;
  int arg4;
  int arg5;
  int arg6;
  int arg7;
  boolean showPlus;
  boolean internalPad;
  /** Twice the number of args consumed; odd if any arg is PARAM_FROM_COUNT. */
  int argsUsed;

  LispRealFormat()
  {
    argsUsed = (arg1 == LispFormat.PARAM_FROM_COUNT
		|| arg2 == LispFormat.PARAM_FROM_COUNT
		|| arg3 == LispFormat.PARAM_FROM_COUNT
		|| arg4 == LispFormat.PARAM_FROM_COUNT
		|| arg5 == LispFormat.PARAM_FROM_COUNT
		|| arg6 == LispFormat.PARAM_FROM_COUNT
		|| arg7 == LispFormat.PARAM_FROM_COUNT) ? 1 : 0;
    if (arg1 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg2 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg3 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg4 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg5 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg6 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
    if (arg7 == LispFormat.PARAM_FROM_LIST) argsUsed += 2;
  }

  public Format resolve (Object[] args, int start)
  {
    if (op == '$')
      {
	FixedRealFormat mfmt = new FixedRealFormat();
	int decimals = getParam(this.arg1, 2, args, start);
	if (this.arg1 == LispFormat.PARAM_FROM_LIST)  start++;
	int digits = getParam(this.arg2, 1, args, start);
	if (this.arg2 == LispFormat.PARAM_FROM_LIST)  start++;
	int width = getParam(this.arg3, 0, args, start);
	if (this.arg3 == LispFormat.PARAM_FROM_LIST)  start++;
	char padChar = getParam(this.arg4, ' ', args, start);
	if (this.arg4 == LispFormat.PARAM_FROM_LIST)  start++;

	mfmt.setMaximumFractionDigits(decimals);
	mfmt.setMinimumIntegerDigits(digits);
	mfmt.width = width;
	mfmt.padChar = padChar;
	mfmt.internalPad = internalPad;
	mfmt.showPlus = showPlus;
	return mfmt;
      }
    else if (op == 'F')
      {
	FixedRealFormat mfmt = new FixedRealFormat();
	int width = getParam(this.arg1, 0, args, start);
	if (this.arg1 == LispFormat.PARAM_FROM_LIST)  start++;
	int decimals = getParam(this.arg2, -1, args, start);
	if (this.arg2 == LispFormat.PARAM_FROM_LIST)  start++;
	int scale = getParam(this.arg3, 0, args, start);
	if (this.arg3 == LispFormat.PARAM_FROM_LIST)  start++;
	mfmt.overflowChar = getParam(this.arg4, '\0', args, start);
	if (this.arg4 == LispFormat.PARAM_FROM_LIST)  start++;
	char padChar = getParam(this.arg5, ' ', args, start);
	if (this.arg5 == LispFormat.PARAM_FROM_LIST)  start++;
	mfmt.setMaximumFractionDigits(decimals);
	mfmt.setMinimumIntegerDigits(0);
	mfmt.width = width;
	mfmt.scale = scale;
	mfmt.padChar = padChar;
	mfmt.internalPad = internalPad;
	mfmt.showPlus = showPlus;
	return mfmt;
      }
    else // if (op == 'E' || op == 'G')
      {
	ExponentialFormat efmt = new ExponentialFormat();
	efmt.width = getParam(this.arg1, 0, args, start);
	if (this.arg1 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.fracDigits = getParam(this.arg2, -1, args, start);
	if (this.arg2 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.expDigits = getParam(this.arg3, 0, args, start);
	if (this.arg3 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.intDigits = getParam(this.arg4, 1, args, start);
	if (this.arg4 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.overflowChar = getParam(this.arg5, '\0', args, start);
	if (this.arg5 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.padChar = getParam(this.arg6, ' ', args, start);
	if (this.arg6 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.exponentChar = getParam(this.arg7, 'E', args, start);
	if (this.arg7 == LispFormat.PARAM_FROM_LIST)  start++;
	efmt.general = op == 'G';
	efmt.showPlus = showPlus;
	return efmt;
      }
  }

  public int format(Object[] args, int start, Writer dst, FieldPosition fpos)
    throws java.io.IOException
  {
    StringBuffer sbuf = new StringBuffer(100);
    Format fmt = resolve(args, start);
    start += argsUsed >> 1;
    RealNum value = (RealNum) args[start++];
    fmt.format(value, sbuf, fpos);
    dst.write(sbuf.toString());
    return start;
  }
}
