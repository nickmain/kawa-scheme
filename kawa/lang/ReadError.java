package kawa.lang;

/**
 * A syntax error during read.
 */

public class ReadError extends SyntaxError
{
  InPort inport;

  public ReadError (InPort inport, String message)
  {
    super (construct_message (inport, message));
    this.inport = inport;
  }

  public InPort getPort ()
  {
    return inport;
  }

  static private String construct_message (InPort port, String message)
  {
    StringBuffer buffer = new StringBuffer ();
    String name = port.getName ();
    if (name == null)
      name = "<unknown>";
    buffer.append (name);
    buffer.append (':');
    buffer.append (port.getLineNumber ());
    int column = port.getColumnNumber ();
    if (column >= 0)
      {
	buffer.append (':');
	buffer.append (column);
      }
    buffer.append (": ");
    buffer.append (message);
    return buffer.toString ();
  }
}
