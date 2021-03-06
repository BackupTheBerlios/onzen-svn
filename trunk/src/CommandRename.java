/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command rename/move file/directory
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.IOException;
import java.io.File;
import java.util.AbstractList;
import java.util.HashSet;
import java.util.LinkedList;

// graphics
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
    String[] message;                   // commit message
    String   lastMessage;               // last set commit message
    boolean  messageEdited;             // true iff commit message edited
    boolean  immediateCommitFlag;       // true for immediate commit

    Data()
    {
      this.newFileName         = null;
      this.message             = null;
      this.lastMessage         = "";
      this.messageEdited       = false;
      this.immediateCommitFlag = Settings.immediateCommit;;
    }
  };

  // --------------------------- constants --------------------------------
  // user events
  private final int USER_EVENT_NEW_MESSAGE         = 0xFFFF+0;
  private final int USER_EVENT_ADD_REPLACE_MESSAGE = 0xFFFF+1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab        repositoryTab;
  private final FileData             fileData;
  private final Shell                shell;
  private final Display              display;
  private final LinkedList<String[]> history;

  // dialog
  private final Data                 data = new Data();
  private final Shell                dialog;

  // widgets
  private final Text                 widgetNewFileName;
  private final List                 widgetHistory;
  private final Text                 widgetMessage;
  private final Button               widgetImmediateCommit;
  private final Button               widgetRename;

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
    Text      text;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;
    this.shell         = shell;

    // get display
    display = shell.getDisplay();

    // get commit history
    history = CommitMessage.getHistory();

    // rename file dialog
    dialog = Dialogs.openModal(shell,"Rename/move file",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Old name:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        text = Widgets.newStringView(subComposite);
        text.setText(fileData.getFileName());
        Widgets.layout(text,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"New name:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetNewFileName = Widgets.newText(subComposite);
        Widgets.layout(widgetNewFileName,1,1,TableLayoutData.WE);

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,1,2,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            File file = new File(repositoryTab.repository.rootPath,widgetNewFileName.getText());

            String newDirectory = Dialogs.directory(shell,
                                                    "Select directory",
                                                    file.getParentFile().getPath()
                                                   );
            if (newDirectory != null)
            {
              File newFile = new File(newDirectory,file.getName());
              widgetNewFileName.setText(repositoryTab.repository.getFileName(newFile));
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,2,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.WRAP|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetMessage.setEnabled(data.immediateCommitFlag);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetMessage,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.immediateCommitFlag);
          if (data.immediateCommitFlag) control.setFocus();
        }
      });
      widgetMessage.setToolTipText("Commit message. Use:\n"+
                                   Widgets.acceleratorToText(Settings.keyPrevCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyFirstCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"to select message from history,\n"+
                                   Widgets.acceleratorToText(Settings.keyFormatCommitMessage)+" to format message,\n"+
                                   Widgets.acceleratorToText(Settings.keyCommitMessageDone)+" to rename files."
                                  );

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,1.0));
      Widgets.layout(subComposite,6,0,TableLayoutData.WE);
      {
        widgetImmediateCommit = Widgets.newCheckbox(subComposite,"immediate commit");
        widgetImmediateCommit.setSelection(data.immediateCommitFlag);
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
      widgetRename = Widgets.newButton(composite,"Rename/Move");
      Widgets.layout(widgetRename,0,0,TableLayoutData.W,0,0,0,0,80,SWT.DEFAULT);
      widgetRename.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.newFileName         = widgetNewFileName.getText();
          data.message             = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
          data.immediateCommitFlag = widgetImmediateCommit.getSelection();

          Settings.geometryRename  = dialog.getSize();
          Settings.immediateCommit = widgetImmediateCommit.getSelection();

          Dialogs.close(dialog,true);
        }
      });
      widgetRename.setToolTipText("Rename/move file.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,80,SWT.DEFAULT);
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
    widgetHistory.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        if (data.immediateCommitFlag)
        {
          int i = widget.getSelectionIndex();
          if (i >= 0)
          {
            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
          }
        }
      }
    });
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        if (data.immediateCommitFlag)
        {
          int i = widget.getSelectionIndex();
          if (i >= 0)
          {
            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
          }
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
        int i = widgetHistory.getSelectionIndex();
        if (i < 0) i = history.size();

        if      (Widgets.isAccelerator(keyEvent,Settings.keyPrevCommitMessage))
        {
          // previous history entry
          if (i > 0)
          {
            widgetHistory.setSelection(i-1);
            display.update();
            widgetHistory.showSelection();

            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i-1),widgetMessage.DELIMITER).trim());
          }
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyNextCommitMessage))
        {
          // next history entry
          if (i < history.size()-1)
          {
            widgetHistory.setSelection(i+1);
            display.update();
            widgetHistory.showSelection();

            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i+1),widgetMessage.DELIMITER).trim());
          }
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFirstCommitMessage))
        {
          // first history entry
          if (history.size() > 0)
          {
            widgetHistory.setSelection(0);
            display.update();
            widgetHistory.showSelection();

            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.getFirst(),widgetMessage.DELIMITER).trim());
          }
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyLastCommitMessage))
        {
          // last history entry
          if (history.size() > 0)
          {
            widgetHistory.setSelection(history.size()-1);
            display.update();
            widgetHistory.showSelection();

            Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.getLast(),widgetMessage.DELIMITER).trim());
          }
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFormatCommitMessage))
        {
          // format message
          String oldMessage = widgetMessage.getText().trim();
          String newMessage = CommitMessage.format(oldMessage,widgetMessage.DELIMITER);
          if (!newMessage.equals(oldMessage))
          {
            widgetMessage.setText(newMessage);
            Widgets.setFocus(widgetMessage);
          }
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyCommitMessageDone))
        {
          // invoke rename-button
          Widgets.invoke(widgetRename);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
    Listener listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        String  currentMessage = widgetMessage.getText().trim();

        String newMessage;
        if (   (!currentMessage.isEmpty() && !currentMessage.equals(data.lastMessage))
            || (!currentMessage.isEmpty() && (event.type == USER_EVENT_ADD_REPLACE_MESSAGE))
            || data.messageEdited
           )
        {
          switch (Dialogs.select(dialog,"Confirmation","Replace or add message to existing commit message?",new String[]{"Replace","Add","Cancel"},1))
          {
            case 0:
              // replace
              newMessage         = event.text;
              data.lastMessage   = newMessage;
              data.messageEdited = false;
              break;
            case 1:
              // add
              newMessage         = CommitMessage.format(currentMessage+widgetMessage.DELIMITER+event.text,widgetMessage.DELIMITER);
              data.messageEdited = true;
              break;
            default:
              return;
          }
        }
        else
        {
          newMessage       = event.text;
          data.lastMessage = newMessage;
        }

        widgetMessage.setText(newMessage);
        Widgets.setFocus(widgetMessage);
      }
    };
    dialog.addListener(USER_EVENT_NEW_MESSAGE,listener);
    dialog.addListener(USER_EVENT_ADD_REPLACE_MESSAGE,listener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryRename,Settings.setWindowLocation);

    // add history
    if (!widgetHistory.isDisposed())
    {
      for (String[] lines : history)
      {
        widgetHistory.add(StringUtils.join(lines,", "));
      }
      widgetHistory.setSelection(widgetHistory.getItemCount()-1);
      display.update();
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
    repositoryTab.setStatusText("Rename/move file '%s' -> '%s'...",fileData.getFileName(),data.newFileName);
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
                                   fileData.locked,
                                   fileData.size,
                                   fileData.date
                                  )
                     );
      repositoryTab.updateFileStates(fileData.toSet());

      // free resources
      commitMessage.done(); commitMessage = null;
    }
    catch (final RepositoryException exception)
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,
                        exception.getExtendedMessage(),
                        "Cannot rename file\n\n'%s'\n\ninto\n\n'%s'\n\n(error: %s)",
                        fileData.getFileName(),
                        data.newFileName,
                        exception.getMessage()
                       );
        }
      });
      Onzen.printStacktrace(exception);
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
