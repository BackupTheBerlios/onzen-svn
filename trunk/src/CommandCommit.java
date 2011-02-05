/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandCommit.java,v $
* $Revision: 1.3 $
* $Author: torsten $
* Contents: command commit files/directories
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.io.IOException;

//import java.text.SimpleDateFormat;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.BitSet;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;
//import java.util.WeakHashMap;

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

/** commit files/directories command
 */
class CommandCommit
{
  /** dialog data
   */
  class Data
  {
    HashSet<FileData> fileDataSet;
    String            message;
    boolean           binaryFlag;

    Data()
    {
      this.fileDataSet = new HashSet<FileData>();
      this.message     = "";
      this.binaryFlag  = false;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final String[]      history;       
  private final Shell         dialog;        

  // widgets
  private final List          widgetFiles;   
  private final List          widgetHistory; 
  private final Text          widgetMessage; 
  private final Button        widgetCommit;     

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** commit command
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   */
  CommandCommit(RepositoryTab repositoryTab, final Shell shell, final Repository repository)
  {
    Composite composite;
    Label     label;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get display
    display = shell.getDisplay();

    // get history
    history = Message.getHistory();

    // commit files dialog
    dialog = Dialogs.open(shell,"Commit files",Settings.geometryCommit.x,Settings.geometryCommit.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to commit.");

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,3,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Up/Down/Home/End to select message from history.\n\nUse Ctrl-Return to commit files.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetCommit,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.message = widgetMessage.getText();

          Settings.geometryCommit = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetCommit.setToolTipText("Commit files.");

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
            // invoke commit-button
            Widgets.invoke(widgetCommit);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog);

    // add history
    for (String string : history)
    {
      widgetHistory.add(string.replaceAll("\n","\\\\n"));
    }

    // update
  }

  /** commit command
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileDataSet files to commit
   */
  CommandCommit(RepositoryTab repositoryTab, Shell shell, Repository repository, HashSet<FileData> fileDataSet)
  {
    this(repositoryTab,shell,repository);

    // add files
    for (FileData fileData : fileDataSet)
    {
      data.fileDataSet.add(fileData);
      widgetFiles.add(fileData.name);
    }
  }

  /** commit command
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileData file to commit
   */
  CommandCommit(RepositoryTab repositoryTab, Shell shell, Repository repository, FileData fileData)
  {
    this(repositoryTab,shell,repository);

    // add file
    data.fileDataSet.add(fileData);
    widgetFiles.add(fileData.name);
  }

  /** run dialog
   */
  public void run()
  {
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {      
      Background.run(new BackgroundRunnable()
      {
        public void run()
        {
          commit(data,repositoryTab);
        }
      });
    }
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      commit(data,repositoryTab);

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
    return "CommandCommit {}";
  }

  //-----------------------------------------------------------------------

  /** do commit
   * @param data data
   * @param repositoryTab repository tab
   */
  private void commit(final Data data, final RepositoryTab repositoryTab)
  {
    repositoryTab.setStatusText("Commit files...");
    Message message = null;
    try
    {
      // commit files
      message = new Message(data.message);
      repositoryTab.repository.commit(data.fileDataSet,message);

      // add message to history
      message.addToHistory();

      // update file states
Dprintf.dprintf("");
      display.syncExec(new Runnable()
      {
        public void run()
        {
          repositoryTab.updateFileStatus(data.fileDataSet);
        }
      });
Dprintf.dprintf("");
    }
    catch (RepositoryException exception)
    {
      final String exceptionMessage = exception.getMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(dialog,"Cannot commit files (error: %s)",exceptionMessage);
        }
      });
      return;
    }
    finally
    {
      message.done();
      repositoryTab.clearStatusText();
    }
  }
}

/* end of file */
