/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRename.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: command rename
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

/** rename command
 */
class CommandRename
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
  private final FileData   fileData;

  private final Display    display;
  private final Data       data = new Data();
  private final String[]   history;       

  // dialog
  private final Shell      dialog;        

  // widgets
  private final Text       widgetNewFileName;
  private final List       widgetHistory; 
  private final Text       widgetMessage; 
  private final Button     widgetRename;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** rename command
   * @param shell shell
   * @param repository repository
   * @param fileData file to rename
   */
  CommandRename(final Shell shell, final Repository repository, final FileData fileData)
    throws RepositoryException
  {

    Composite composite,subComposite;
    Label     label;
    Table     table;
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
    dialog = Dialogs.open(shell,"Rename file",Settings.geometryRename.x,Settings.geometryRename.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Old name:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        label = Widgets.newLabel(subComposite);
        label.setText(fileData.getFileName());
        Widgets.layout(label,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"New name:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetNewFileName = Widgets.newText(subComposite);
        Widgets.layout(widgetNewFileName,1,1,TableLayoutData.WE);
      }

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,2,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Up/Down/Home/End to select message from history.\n\nUse Ctrl-Return to rename file.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetRename = Widgets.newButton(composite,"Rename");
      Widgets.layout(widgetRename,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetRename.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.newFileName = widgetNewFileName.getText();
          data.message     = widgetMessage.getText();

          Settings.geometryRename = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetRename.setToolTipText("Rename file.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
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
    widgetNewFileName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetMessage.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          widgetMessage.setText(history[i]);
          widgetMessage.setFocus();
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetMessage.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        Text widget = (Text)keyEvent.widget;

        if ((keyEvent.stateMask & SWT.CTRL) != 0)
        {
          int i = widgetHistory.getSelectionIndex();

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i >= 0)
            {
              if (i < history.length-1)
              {
                widgetHistory.setSelection(i+1);
                widgetMessage.setText(history[i+1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i >= 0)
            {
              if (i > 0)
              {
                widgetHistory.setSelection(i-1);
                widgetMessage.setText(history[i-1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(0);
              widgetMessage.setText(history[0]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.END)
          {
            // last history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(history.length-1);
              widgetMessage.setText(history[history.length-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke add-button
            Widgets.invoke(widgetRename);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog);

/*
    // add files
    for (FileData fileData : fileDataSet)
    {
      widgetFiles.add(fileData.name);
    }
*/
Dprintf.dprintf("");

    // add history
    for (String string : history)
    {
      widgetHistory.add(string.replaceAll("\n","\\\\n"));
    }

    // update
  }

  /** run dialog
   */
  public boolean run()
    throws RepositoryException
  {
    widgetNewFileName.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      Message message = null;
      try
      {
        // rename file
        message = new Message(data.message);
        repository.rename(fileData,data.newFileName,message);//data.binaryFlag);

        // update states
        repository.updateStates(fileData);

        // store history
        Message.addHistory(data.message);
      }
      finally
      {
        message.done();
      }

      return true;
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
    return "CommandRename {}";
  }

  //-----------------------------------------------------------------------
}

/* end of file */
