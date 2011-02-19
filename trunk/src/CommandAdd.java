/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandAdd.java,v $
* $Revision: 1.10 $
* $Author: torsten $
* Contents: command add files/directories
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;

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

/** add files/directories command
 */
class CommandAdd
{
  /** dialog data
   */
  class Data
  {
    String  message;
    boolean immediateCommitFlag;
    boolean binaryFlag;

    Data()
    {
      this.message             = "";
      this.immediateCommitFlag = false;
      this.binaryFlag          = false;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab     repositoryTab;
  private final HashSet<FileData> fileDataSet;
  private final Shell             shell;
  private final Display           display;
  private final String[]          history;       

  // dialog
  private final Data              data = new Data();
  private final Shell             dialog;        

  // widgets
  private final List              widgetFiles;   
  private final List              widgetHistory; 
  private final Text              widgetMessage; 
  private final Button            widgetImmediateCommit;  
  private final Button            widgetBinary;  
  private final Button            widgetAdd;     

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** add command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to add
   */
  CommandAdd(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileDataSet   = fileDataSet;
    this.shell         = shell;

    // get display
    display = shell.getDisplay();

    // get history
    history = Message.getHistory();

    // add files dialog
    dialog = Dialogs.open(shell,"Add files",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to add.");

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,3,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetMessage.setEnabled(Settings.immediateCommit);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetListener(widgetMessage,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.immediateCommitFlag);
        }
      });
      widgetMessage.setToolTipText("Commit message.\nCtrl-Up/Down/Home/End to select message from history.\nCtrl-Return to add files.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,1.0));
      Widgets.layout(subComposite,6,0,TableLayoutData.WE);
      {
        widgetBinary = Widgets.newCheckbox(subComposite,"binary");
        Widgets.layout(widgetBinary,0,0,TableLayoutData.W);
        widgetBinary.setToolTipText("Select this checkbox to add binary files.");

        widgetImmediateCommit = Widgets.newCheckbox(subComposite,"immediate commit");
        widgetImmediateCommit.setSelection(Settings.immediateCommit);
        Widgets.layout(widgetImmediateCommit,0,1,TableLayoutData.E);
        widgetImmediateCommit.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            data.immediateCommitFlag = widgetImmediateCommit.getSelection();
            Widgets.modified(data);
          }
        });
        widgetImmediateCommit.setToolTipText("Select this checkbox to commit added files immediately.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetAdd = Widgets.newButton(composite,"Add");
      Widgets.layout(widgetAdd,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.message             = widgetMessage.getText();
          data.immediateCommitFlag = widgetImmediateCommit.getSelection();
          data.binaryFlag          = widgetBinary.getSelection();

          Settings.geometryAdd     = dialog.getSize();
          Settings.immediateCommit = widgetImmediateCommit.getSelection();

          Dialogs.close(dialog,true);
        }
      });
      widgetAdd.setToolTipText("Add files.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
          if (i < 0) i = history.length;

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i < history.length-1)
            {
              widgetHistory.setSelection(i+1);
              widgetHistory.showSelection();
              widgetMessage.setText(history[i+1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i > 0)
            {
              widgetHistory.setSelection(i-1);
              widgetHistory.showSelection();
              widgetMessage.setText(history[i-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(0);
              widgetHistory.showSelection();
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
              widgetHistory.showSelection();
              widgetMessage.setText(history[history.length-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke commit-button
            Widgets.invoke(widgetAdd);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryAdd);

    // add files
    if (!widgetFiles.isDisposed())
    {
      for (FileData fileData : fileDataSet)
      {
        widgetFiles.add(fileData.getFileName());
      }
    }

    // add history
    if (!widgetHistory.isDisposed())
    {
      for (String string : history)
      {
        widgetHistory.add(string.replaceAll("\n","\\\\n"));
      }
      widgetHistory.setSelection(widgetHistory.getItemCount()-1);
      widgetHistory.showSelection();
      widgetHistory.deselectAll();
    }

    // update
    data.immediateCommitFlag = Settings.immediateCommit;
  }

  /** add command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to add
   */
  CommandAdd(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData.toSet());
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
          add();
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
      add();

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
    return "CommandAdd {}";
  }

  //-----------------------------------------------------------------------

  /** do add
   */
  private void add()
  {
    repositoryTab.setStatusText("Add files...");
    Message message = null;
    try
    {
      // add message to history
      if (data.immediateCommitFlag)
      {
        message = new Message(data.message);
        message.addToHistory();
      }

      // add files
      repositoryTab.repository.add(fileDataSet,message,data.binaryFlag);

      // update file states
      repositoryTab.repository.updateStates(fileDataSet);
      display.syncExec(new Runnable()
      {
        public void run()
        {
          repositoryTab.updateTreeItems(fileDataSet);
        }
      });
    }
    catch (RepositoryException exception)
    {
      final String exceptionMessage = exception.getMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,"Cannot add files (error: %s)",exceptionMessage);
        }
      });
      return;
    }
    finally
    {
      if (message != null) message.done();
      repositoryTab.clearStatusText();
    }
  }
}

/* end of file */
