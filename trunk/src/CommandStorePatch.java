/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandStorePatch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: command store patch in internal database
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** store patch command
 */
class CommandStorePatch
{
  /** dialog data
   */
  class Data
  {
    String[]        revisionNames;        // revision names
    String[] lines;                // patch lines
    String[] linesNoWhitespaces;   // patch lines (without whitespaces)
    String   summary;              // summary for patch
    String[] message;              // message for patch

    Data()
    {
      this.revisionNames      = null;
      this.lines              = null;
      this.linesNoWhitespaces = null;
      this.summary            = null;
      this.message            = null;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public String               summary;
  public String[]             message;

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Patch         patch;
  private final Date          date;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text          widgetPatch;
  private final Text          widgetSummary;
  private final Text          widgetMessage;
  private final Button        widgetStore;
  private final Button        widgetCancel;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** store patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet file data set
   * @param patch patch
   * @param summary summary text
   * @param message message text lines
   */
  CommandStorePatch(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, final Patch patch, String summary, String[] message)
  {
    Composite         composite,subComposite,subSubComposite,subSubSubComposite;
    Label             label;
    TabFolder         tabFolder;
    Button            button;
    ScrolledComposite scrolledComposite;
    SelectionListener selectionListener;
    Listener          listener;

    // initialize variables
    this.summary       = summary;
    this.message       = message;
    this.repositoryTab = repositoryTab;
    this.patch         = patch;
    this.date          = new Date();

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Store patch",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Patch:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetPatch = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
      widgetPatch.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetPatch,1,0,TableLayoutData.NSWE);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(new double[]{0.0,1.0},new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(subComposite,"Summary:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetSummary = Widgets.newText(subComposite);
        widgetSummary.setText(summary);
        Widgets.layout(widgetSummary,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Message:");
        Widgets.layout(label,1,0,TableLayoutData.NW);

        widgetMessage = Widgets.newText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
        widgetMessage.setText(StringUtils.join(message,widgetMessage.DELIMITER));
        Widgets.layout(widgetMessage,1,1,TableLayoutData.NSWE);
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetStore = Widgets.newButton(composite,"Store");
      Widgets.layout(widgetStore,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetStore.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.summary = widgetSummary.getText();
          data.message = StringUtils.split(widgetMessage.getText(),widgetMessage.DELIMITER);

          Settings.geometryStorePatch = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });

      widgetCancel = Widgets.newButton(composite,"Cancel");
      Widgets.layout(widgetCancel,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCancel.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners

    // show dialog
    Dialogs.show(dialog,Settings.geometryStorePatch);

    // update
    widgetPatch.setText(StringUtils.join(patch.getLines(),widgetPatch.DELIMITER));
  }

  /** store patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet file data set
   * @param patch patch
   */
  CommandStorePatch(Shell shell, RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, Patch patch)
  {
    this(shell,repositoryTab,fileDataSet,patch,"",null);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      Widgets.setFocus(widgetSummary);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        summary = data.summary;
        message = data.message;
      }
    }
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    if (!dialog.isDisposed())
    {
      Widgets.setFocus(widgetSummary);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        summary = data.summary;
        message = data.message;

        return true;
      }
      else
      {
        return false;
      }
    }
    else
    {
      return false;
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandStorePatch {}";
  }

  //-----------------------------------------------------------------------
}

/* end of file */
