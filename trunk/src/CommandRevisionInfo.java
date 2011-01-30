/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRevisionInfo.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: command show file revision
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
import java.util.ListIterator;
//import java.util.StringTokenizer;
import java.util.WeakHashMap;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
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

/** view revision info command
 */
class CommandRevisionInfo
{
  /** dialog data
   */
  class Data
  {
    String  newFileName;
    String  message;
    boolean binaryFlag;

    Data()
    {
      this.newFileName = null;
      this.message     = "";
      this.binaryFlag  = false;
    }
  };

  // --------------------------- constants --------------------------------

  // colors

  // --------------------------- variables --------------------------------

  // global variable references
  private final Shell      shell;
  private final Repository repository;
  private final Display    display;
  private final FileData   fileData;

  // dialog
  private final Data       data = new Data();
  private final String[]   history;       
  private final Shell      dialog;        

  // widgets
//  private final Text       widgetNewFileName;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** rename command
   * @param shell shell
   * @param repository repository
   * @param fileData file to rename
   */
  CommandRevisionInfo(final Shell shell, final Repository repository, final FileData fileData)
    throws RepositoryException
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.shell      = shell;
    this.repository = repository;
    this.fileData   = fileData;

    // get display
    display = shell.getDisplay();

    // get history
    history = Message.getHistory();

    // add files dialog
    dialog = Dialogs.open(shell,"File revision: "+fileData.getFileName(),Settings.geometryRename.x,Settings.geometryRename.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        label = Widgets.newView(subComposite);
        label.setText(fileData.getFileName());
        Widgets.layout(label,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Date:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        label = Widgets.newView(subComposite);
        label.setText(Onzen.DATE_FORMAT.format(fileData.datetime));
        Widgets.layout(label,1,1,TableLayoutData.WE);
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Close");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog);
        }
      });
    }

    // listeners

    // show dialog
    Dialogs.show(dialog);

    // update
  }

  /** run dialog
   */
  public void run()
    throws RepositoryException
  {
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandRevisionInfo {}";
  }

  //-----------------------------------------------------------------------
}

/* end of file */
