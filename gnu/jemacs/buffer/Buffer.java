package gnu.jemacs.buffer;
import javax.swing.text.*;
import java.io.*;

public class Buffer
{
  String name;
  String filename;
  //boolean modified;

  static javax.swing.text.StyleContext styles
  = new javax.swing.text.StyleContext();
  Style inputStyle = styles.addStyle("input", null);

  /** Value of point (0-orgin), when curPosition is null. */
  int point;
  Caret curPosition = null;

  BufferContent content;
  DefaultStyledDocument document;
  DefaultStyledDocument modelineDocument;
  public final BufferKeymap keymap = new BufferKeymap(this);

  /** Map buffer names to buffer.s */
  public static java.util.Hashtable buffers
  = new java.util.Hashtable(100);

  /** Map file names to buffer.s */
  public static java.util.Hashtable fileBuffers
  = new java.util.Hashtable(100);

  public String getName() { return name; }

  public String getFileName() { return filename; }

  public void setFileName(String fname)
  {
    if (filename != null && fileBuffers.get(filename) == this)
      fileBuffers.remove(filename);
    if (name != null && buffers.get(name) == this)
      buffers.remove(name);
    filename = fname;
    name = generateNewBufferName(new java.io.File(fname).getName());
    buffers.put(name, this);
    fileBuffers.put(filename, this);
    redrawModeline();
  }

  public static Buffer findFile(String fname)
  {
    Buffer buffer = (Buffer) fileBuffers.get(fname);
    if (buffer == null)
      {
        buffer = new Buffer(null);
        buffer.setFileName(fname);
        try
          {
            Reader in = new FileReader(fname);
            buffer.insertFile(in);
            in.close();
          }
        catch (java.io.FileNotFoundException ex)
          {
            Signal.message("New file");
          }
        catch (Exception ex)
          {
            throw new RuntimeException("error reading file \"" + fname
                                       + "\": " + ex);
          }
      }
    return buffer;
  }

  public static Buffer getBuffer(String name)
  {
    return (Buffer) buffers.get(name);
  }

  public static Buffer coerceBuffer(Object buf)
  {
    if (buf instanceof Buffer)
      return (Buffer) buf;
    return getBuffer(buf.toString());
  }

  public static String generateNewBufferName(String start)
  {
    Buffer buf = getBuffer(start);
    if (buf == null)
      return start;
    int len = start.length();
    StringBuffer sbuf = new StringBuffer(len + 5);
    sbuf.append(start);
    sbuf.append('<');
    for (int i = 2;  ;  i++)
      {
	sbuf.append(i);
	sbuf.append('>');
	String name = sbuf.toString();
	buf = getBuffer(name);
	if (buf == null)
	  return name;
	sbuf.setLength(len+1);
      }
  }

  public void redrawModeline()
  {
    try
      {
        modelineDocument.remove(0, modelineDocument.getLength());
        modelineDocument.insertString(0, "---JEmacs: " + getName() + " ---", null);
      }
    catch (javax.swing.text.BadLocationException ex)
      {
        throw new Error("internal error in redraw-modeline- "+ex);
      }
  }

  public Buffer(String name)
  {
    this.name = name;
    content = new BufferContent();
    document = new javax.swing.text.DefaultStyledDocument(content, styles);
    modelineDocument
      = new javax.swing.text.DefaultStyledDocument(new javax.swing.text.StringContent(), styles);
    redrawModeline();
  }

  public final int getDot()
  {
    return curPosition == null ? point : curPosition.getDot();
  }

  public int getPoint()
  {
    return 1 + getDot();
  }

  public final void setDot(int i)
  {
    point = i;
    if (curPosition != null)
      curPosition.setDot(i);
  }

  public final void setPoint(int i)
  {
    setDot(i - 1);
  }

  public void forwardChar(int i)
  {
    if (curPosition != null)
      point = curPosition.getDot();
    point += i;
    if (curPosition != null)
      curPosition.setDot(point);
  }

  public void backwardChar(int i)
  {
    if (curPosition != null)
      point = curPosition.getDot();
    point -= i;
    if (point < 0)
      Signal.signal("Beginning of buffer");
    if (curPosition != null)
      curPosition.setDot(point);
  }

  public String toString()
  {
    return "#<buffer \"" + name + "\">";
  }

  public void insert (String string, Style style)
  {
    pointMarker.insert(string, style);
  }

  /** Insert count copies of ch at point. */
  public void insert (char ch, int count, Style style)
  {
    pointMarker.insert(ch, count, style);
  }

  Marker pointMarker = makePointMarker();

  private Marker makePointMarker ()
  {
    Marker marker = new Marker();
    marker.buffer = this;
    marker.index = Marker.POINT_POSITION_INDEX;
    return marker;
  }

  public Marker getPointMarker (boolean share)
  {
    return share ? pointMarker : new Marker(pointMarker);
  }

  public void save(Writer out)
    throws java.io.IOException, javax.swing.text.BadLocationException
  {
    int length = document.getLength();
    int todo = length;
    Segment segment = new Segment();
    int offset = 0;
    while (offset < length)
      {
        int count = length;
        if (count > 4096)
          count = 4096;
        document.getText(offset, count, segment);
        out.write(segment.array, segment.offset, segment.count);
        offset += count;
      }
  }

  public void save()
  {
    try
      {
        Writer out = new FileWriter(filename);
        save(out);
        out.close();
      }
    catch (Exception ex)
      {
        throw new RuntimeException("error save-buffer: "+ex);
      }
  }

  public void insertFile(Reader in)
    throws java.io.IOException, javax.swing.text.BadLocationException
  {
    char[] buffer = new char[2048];
    int offset = getDot();
    for (;;)
      {
        int count = in.read(buffer, 0, buffer.length);
        if (count <= 0)
          break;
        document.insertString(offset, new String(buffer, 0, count), null);
        offset += count;
      }
    setDot(offset);
  }

  public void insertFile(String filename)
  {
    try
      {
        Reader in = new FileReader(filename);
        insertFile(in);
        in.close();
      }
    catch (Exception ex)
      {
        throw new RuntimeException("error reading file \""+filename+"\": "+ex);
      }
  }

  /*
  public insertFileContents(String name)
  {
  }
  */
}
