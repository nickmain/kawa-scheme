package gnu.jemacs.buffer;
import javax.swing.*;
import javax.swing.text.*;
import gnu.jemacs.swing.SwingWindow;

/** A pseudo-action to wrap an integer. */

public class TooLongAction extends javax.swing.AbstractAction
{
  int maxValid;

  public TooLongAction(int maxValid)
  {
    //super(Integer.toString(maxValid));
    this.maxValid = maxValid;
  }

  public int getMaxValid() { return maxValid; }

  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    // Should never happen - ignore.
    SwingWindow.getWindow(event).pendingLength = 0;
  }

}
