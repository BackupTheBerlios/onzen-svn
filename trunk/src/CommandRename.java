/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRename.java,v $
* $Revision: 1.6 $
* $Author: torsten $
* Contents: command rename file/directory
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.IOException;

import java.util.AbstractList;
import java.util.HashSet;

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
    String   newFileName;
    String[] message;
    boolean  immediateCommitFlag;

    Data()
    {
      this.newFileName         = null;
      this.message             = null;
      this.immediateCommitFlag = false;
    }
  };

  // --------------------------- constants --------------------------------

  // colors

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final FileData      fileData;
  private final Shell         shell;
  private final Display       display;
  private final String[][]    history;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text          widgetNewFileName;
  private final List          widgetHistory;
  private final Text          widgetMessage;
  private final Button        widgetImmediateCommit;
  private final Button        widgetRename;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** rename command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to rename
   */
  CommandRename(final Shell shell, final RepositoryTab repositoryTab, final FileData fileData)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;
    this.shell         = shell;

    // get display
    display = shell.getDisplay();

    // get history
    history = CommitMessage.getHistory();

    // rename files dialog
    dialog = Dialogs.open(shell,"Rename file",new double[]{1.0,0.0},1.0);

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
      widgetMessage.setEnabled(Settings.immediateCommit);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetListener(widgetMessage,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.immediateCommitFlag);
        }
      });
      widgetMessage.setToolTipText("Commit message.\nCtrl-Up/Down/Home/End to select message from history.\nCtrl-Return to rename file.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,1.0));
      Widgets.layout(subComposite,6,0,TableLayoutData.WE);
      {
        widgetImmediateCommit = Widgets.newCheckbox(subComposite,"immediate commit");
        widgetImmediateCommit.setSelection(Settings.immediateCommit);
        Widgets.layout(widgetImmediateCommit,0,0,TableLayoutData.E);
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

          data.newFileName         = widgetNewFileName.getText();
          data.message             = StringUtils.split(widgetMessage.getText(),widgetMessage.DELIMITER);
          data.immediateCommitFlag = widgetImmediateCommit.getSelection();

          Settings.geometryRename  = dialog.getSize();
          Settings.immediateCommit = widgetImmediateCommit.getSelection();

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
    widgetNewFileName.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        widgetRename.setEnabled(!widget.getText().equals(fileData.getFileName()));
      }
    });
    Widgets.setNextFocus(widgetNewFileName,widgetMessage);
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          widgetMessage.setText(StringUtils.join(history[i],widgetMessage.DELIMITER));
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
              widgetMessage.setText(StringUtils.join(history[i+1],widgetMessage.DELIMITER));
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
              widgetMessage.setText(StringUtils.join(history[i-1],widgetMessage.DELIMITER));
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
              widgetMessage.setText(StringUtils.join(history[0],widgetMessage.DELIMITER));
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
              widgetMessage.setText(StringUtils.join(history[history.length-1],widgetMessage.DELIMITER));
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke commit-button
            Widgets.invoke(widgetRename);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryRename);

    // add history
    if (!widgetHistory.isDisposed())
    {
      for (String[] lines : history)
      {
        widgetHistory.add(StringUtils.join(lines,", "));
      }
      widgetHistory.setSelection(widgetHistory.getItemCount()-1);
      widgetHistory.showSelection();
      widgetHistory.deselectAll();
    }

    // update
    data.newFileName         = fileData.getFileName();
    data.immediateCommitFlag = Settings.immediateCommit;
    widgetNewFileName.setText(data.newFileName);
  }

  /** run dialog
   */
  public void run()
  {
    widgetNewFileName.setFocus();
    widgetNewFileName.setSelection(data.newFileName.length(),data.newFileName.length());
    if ((Boolean)Dialogs.run(dialog,false))
    {
      Background.run(new BackgroundRunnable()
      {
        public void run()
        {
          rename();
        }
      });
    }
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    widgetNewFileName.setFocus();
    widgetNewFileName.setSelection(data.newFileName.length(),data.newFileName.length());
    if ((Boolean)Dialogs.run(dialog,false))
    {
      rename();

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

  /** do rename
   */
  private void rename()
  {
    repositoryTab.setStatusText("Rename file '%s' -> '%s'...",fileData.getFileName(),data.newFileName);
    CommitMessage commitMessage = null;
    try
    {
      // create and add message to history
      if (data.immediateCommitFlag)
      {
        commitMessage = new CommitMessage(data.message);
        commitMessage.addToHistory();
      }

      // rename file
      repositoryTab.repository.rename(fileData,data.newFileName,commitMessage);

      // update file states
      HashSet<FileData> fileDataSet = fileData.toSet();
      fileDataSet.add(new FileData(data.newFileName,
                                   fileData.type,
                                   fileData.state,
                                   fileData.mode,
                                   fileData.size,
                                   fileData.datetime
                                  )
                     );
      repositoryTab.updateFileStates(fileData.toSet());

      // free resources
      commitMessage.done(); commitMessage = null;
    }
    catch (IOException exception)
    {
      final String exceptionMessage = exception.getMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,
                        "Cannot rename file\n\n'%s'\n\ninto\n\n'%s'\n\n(error: %s)",
                        fileData.getFileName(),
                        data.newFileName,
                        exceptionMessage
                       );
        }
      });
      return;
    }
    catch (RepositoryException exception)
    {
      final String exceptionMessage = exception.getMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,
                        "Cannot rename file\n\n'%s'\n\ninto\n\n'%s'\n\n(error: %s)",
                        fileData.getFileName(),
                        data.newFileName,
                        exceptionMessage
                       );
        }
      });
      return;
    }
    finally
    {
      if (commitMessage != null) commitMessage.done();
      repositoryTab.clearStatusText();
    }
  }
}

/* end of file */
