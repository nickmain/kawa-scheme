//This is free software;  for terms and warranty disclaimer see ./COPYING.

package gnu.jemacs.swt;

import gnu.jemacs.buffer.Buffer;
import gnu.jemacs.buffer.EKeymap;
import gnu.jemacs.buffer.EWindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Christian Surlykke
 *         11-07-2004
 */
public class SwtWindow extends EWindow implements VerifyKeyListener, FocusListener, KeyListener, MouseListener
{
  private StyledText styledText;
  private SwtBuffer swtBuffer; 
  
  public SwtWindow(Buffer buffer) {
    this(buffer, true);
  }

  public SwtWindow(Buffer buffer, boolean wantModeLine) {
    super(buffer);
    this.swtBuffer = (SwtBuffer) buffer;
  }
  
  /**
   * 
   */
  public void getReadyToShow(Composite parent, int firstVisibleLine)
  {
    styledText = SwtHelper.newStyledText(parent, 
                                         SWT.V_SCROLL | SWT.H_SCROLL,
                                         swtBuffer.getBufferContent(), 
                                         this, 
                                         firstVisibleLine);
  }

  
  /**
   * @see gnu.jemacs.buffer.EWindow#setBuffer(gnu.jemacs.buffer.Buffer)
   */
  public void setBuffer(Buffer buffer)
  {
    super.setBuffer(buffer);
    SwtHelper.setContent(styledText, ((SwtBuffer) buffer).getBufferContent());
  }

  
  /**
   * The purpose of this method is to emulate the 'toInt' method of SwingWindow
   * so as to transform Swt KeyEvents into the same int's as equivalent awt KeyEvents.
   * 
   * TODO: Elaborate this method so that all KeyEvents work (e.g. enter (!))
   * 
   * I've been thinkin it perhaps would be better to make EKeymap abstract with implementors 
   * for each toolkit, and then lookup commands by Swt events directly when running
   * Swt and Swing events when running swing. Must be considered more... 
   * 
   *  
   * @param swtKeyCode
   * @param stateMask
   * @param additionalFlags
   * @return
   */
  private int transFormKeyKode(int swtKeyCode, int stateMask, int additionalFlags)
  {
    int characterPart = Character.toUpperCase((char) (swtKeyCode & 0xFFFF));
    int modifierPart = (stateMask & swtModifiers) >> 1; // awt modifiers seem to be displaced
                                                        // one bit to the left relative to 
                                                        // swt modifiers.
    return characterPart | modifierPart | (additionalFlags << 16);
  }

  private final static int swtModifiers = SWT.SHIFT | SWT.CTRL | SWT.ALT;

  public void handleKey (int code)
  {
    Object command = lookupKey(code);
    if (command == null )
    {
      return;
    }
    pushPrefix(code);
    pendingLength--;
    handleCommand (command);
  }


  
  public void handleCommand(Object command)
  {
    int oldDot = getBuffer().getDot();
    super.handleCommand(command);
    SwtHelper.redraw(styledText);
    if (oldDot != getBuffer().getDot())
    {
      styledText.showSelection();
    }
  }
  
  /**
   * @see gnu.jemacs.buffer.EWindow#setSelected()
   */
  public void setSelected()
  {
    super.setSelected();
    buffer.pointMarker.sequence = null;
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#unselect()
   */
  public void unselect()
  {
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#getPoint()
   */
  public int getPoint()
  {
    return SwtHelper.getCaretOffset(styledText);
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#setDot(int)
   */
  public void setDot(int offset)
  {
    SwtHelper.setCaretOffset(styledText, offset);
  }
  
  public EWindow split(Buffer buffer, int lines, boolean horizontal)
  {
    SwtWindow newWindow = new SwtWindow(buffer);
    newWindow.frame = this.frame;
    linkSibling(newWindow, horizontal);
    
    int firstVisibleLine = buffer == this.buffer ? SwtHelper.getTopIndex(styledText) : 0;
    int visibleLines = SwtHelper.getArea(styledText).height / SwtHelper.getLineHeight(styledText);
    
    int[] weights = null;
    if (!horizontal && lines > 0 && visibleLines > 1)
    {
      weights = new int[2];
      lines = Math.min(lines, visibleLines - 1);
      weights[0] = lines;
      weights[1] = visibleLines - lines;
      System.out.println("lines = " + lines);
      System.out.println("visible lines = " + visibleLines);
      System.out.println("weights = {" + weights[0] + ", " + weights[1] + "}");
    }
    
    SwtHelper.injectSashFormAsParent(styledText, horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
    newWindow.getReadyToShow(SwtHelper.getParent(styledText), firstVisibleLine);
    if (weights != null)
      SwtHelper.setWeights(((SashForm) SwtHelper.getParent(styledText)), weights);
    SwtHelper.layout(SwtHelper.getParent(SwtHelper.getParent(styledText)));
    
    
    return newWindow;
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#getCharSize()
   */
  protected void getCharSize()
  {
    // TODO
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#getWidth()
   */
  public int getWidth()
  {
    return SwtHelper.getArea(styledText).width;
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#getHeight()
   */
  public int getHeight()
  {
    return SwtHelper.getArea(styledText).height;
  }

  /**
   * @see gnu.jemacs.buffer.EWindow#tooLong(int)
   */
  public Object tooLong(int pendingLength)
  {
    // TODO Something more subtle here
    return null;
  }
  
  // ---------------------------- Listener methods ------------------------------------
  
  // --- VerifyKeyListener
  public void verifyKey(VerifyEvent event)
  {
    handleKey(transFormKeyKode(event.keyCode, event.stateMask, EKeymap.PRESSED));
    if (event.character != 0)
    {
      handleKey(event.character);
    }
    SwtHelper.setCaretOffset(styledText, buffer.getDot());
    event.doit = false;
  }

  // --- FocusListener ---
  public void focusGained(FocusEvent e)
  {
    setSelected();
  }
  
  public void focusLost(FocusEvent e)
  {
    unselect();
  }
  
  // --- KeyListener ---
  public void keyPressed(KeyEvent e)
  {
  }
  
  public void keyReleased(KeyEvent e)
  {
    handleKey(transFormKeyKode(e.keyCode, e.stateMask, EKeymap.RELEASED));
  }
  
  // --- MouseListener ---
  public void mouseDoubleClick(MouseEvent e)
  {
  }

  public void mouseDown(MouseEvent e)
  {
    if (EWindow.getSelected() == this)  // Is this nessecary - aren't we always selected when this event arrives?
    {
      buffer.setDot(SwtHelper.getCaretOffset(styledText));
      SwtHelper.showSelection(styledText);
    }
  }

  public void mouseUp(MouseEvent e)
  {
  }


  
  
  /**
   * @param e
   */
  private void show(KeyEvent e)
  {
    System.out.println("keyCode:   " + Integer.toBinaryString(e.keyCode));
    System.out.println("character: " + Integer.toBinaryString(e.character));
    System.out.println("stateMask: " + Integer.toBinaryString(e.stateMask));
  }

}
