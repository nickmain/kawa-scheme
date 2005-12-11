package gnu.mapping;
import java.io.*;

public class InPort extends gnu.text.LineBufferedReader implements Printable
{
  public InPort (Reader in)
  {
    super (in);
  }

  public InPort (Reader in, String name)
  {
    this (in);
    setName(name);
  }

  public InPort (InputStream in)
  {
    super (in);
  }

  public InPort (InputStream in, String name)
  {
    this (in);
    setName(name);
  }

  public static Reader convertToReader (InputStream in, Object conv)
  {
    if (conv != null && conv != Boolean.TRUE)
      {
	String enc = (conv == Boolean.FALSE ? "8859_1" : conv.toString());
	try
	  {
	    return new java.io.InputStreamReader(in, enc);
	  }
	catch (java.io.UnsupportedEncodingException ex)
	  {
	    throw new RuntimeException("unknown character encoding: "+enc);
	  }
      }
    return new java.io.InputStreamReader(in);
  }

  public InPort (InputStream in, String name, Object conv)
    throws java.io.UnsupportedEncodingException
  {
    this (convertToReader(in, conv), name);
    if (conv == Boolean.FALSE)
      {
	// Use a fixed-size buffer.  This prevents really-long "lines"
	// from causing the buffer to grow to accomodate them.
	try
	  {
	    setBuffer(new char[2048]);
	  }
	catch (java.io.IOException ex) { /* ignored */ }
      }
    else
      setConvertCR(true);
  }

  private static InPort systemInPort
    = new TtyInPort (System.in, "<stdin>", OutPort.outInitial);
  public static final ThreadLocation inLocation
    = new ThreadLocation("in-default");
  static { inLocation.setGlobal(systemInPort); }

  static public InPort inDefault ()
  {
    return (InPort) inLocation.get();
  }

  static public void setInDefault (InPort in)
  {
    inLocation.set(in);
  }

  /** Tests if a URL has a scheme.
   * For convenience, we treat a 1-character "scheme" as an
   * MS-DOS-style "drive letter" - i.e. not a scheme. */
  public static boolean uriSchemeSpecified (String name)
  {
    return uriSchemeLength(name) > 1;
  }

  /** Helper routine to get the scheme part of a URI.
   * The scheme part is "http:" or "file:" or "ftp:" most commonly.
   * This functions searches for the first ':' that doesn't follow a '/'.
   * @return The length of the scheme component, not counting the colon,
   * (or alternatively the index of the colon,), or -1 if the is no scheme.
   */
  public static int uriSchemeLength (String uri)
  {
    int len = uri.length();
    for (int i = 0;  i < len;  i++)
      {
	char ch = uri.charAt(i);
	if (ch == ':')
	  return i;
	if (ch == '/')
	  return -1;
      }
    return -1;
  }

  public static InPort openFile(String fname)
    throws java.io.IOException
  {
    java.io.InputStream strm;
    if (uriSchemeSpecified(fname))
      strm = new java.net.URL(fname).openConnection().getInputStream();
    else
      strm = new java.io.FileInputStream(fname);
    strm = new java.io.BufferedInputStream(strm);
    return openFile(strm, fname);
  }

  public static InPort openFile(InputStream strm, String fname)
    throws java.io.UnsupportedEncodingException
  {
    return new InPort(strm, fname,
		      Environment.user().get("port-char-encoding"));
  }

  public void print(java.io.PrintWriter ps)
  {
    ps.print ("#<input-port");
    String name = getName();
    if (name != null)
      {
	ps.print (' ');
	ps.print (name);
      }
    ps.print ('>');
  }
}
