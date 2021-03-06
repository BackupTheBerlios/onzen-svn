/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command add files/directories
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
    String[] message;                   // commit message
    String   lastMessage;               // last set commit message
    boolean  messageEdited;             // true iff commit message edited
    boolean  immediateCommitFlag;       // true for immediate commit
    boolean  binaryFlag;                // true iff binary file

    Data()
    {
      this.message             = null;
      this.lastMessage         = "";
      this.messageEdited       = false;
      this.immediateCommitFlag = false;
      this.binaryFlag          = false;
    }
  };

  // --------------------------- constants --------------------------------
  // user events
  private final int USER_EVENT_NEW_MESSAGE         = 0xFFFF+0;
  private final int USER_EVENT_ADD_REPLACE_MESSAGE = 0xFFFF+1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab        repositoryTab;
  private final HashSet<FileData>    fileDataSet;
  private final Shell                shell;
  private final Display              display;
  private final LinkedList<String[]> history;

  // dialog
  private final Data                 data = new Data();
  private final Shell                dialog;

  // widgets
  private final List                 widgetFiles;
  private final List                 widgetHistory;
  private final Text                 widgetMessage;
  private final Button               widgetImmediateCommit;
  private final Button               widgetBinary;
  private final Button               widgetAdd;

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

    // get commit history
    history = CommitMessage.getHistory(shell);

    // init data
    data.immediateCommitFlag = Settings.immediateCommit;

    // add files dialog
    dialog = Dialogs.openModal(shell,"Add files",new double[]{1.0,0.0},1.0);

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

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetMessage.setEnabled(Settings.immediateCommit);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetMessage,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.immediateCommitFlag);
        }
      });
      widgetMessage.setToolTipText("Commit message. Use:\n"+
                                   Widgets.acceleratorToText(Settings.keyPrevCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyFirstCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"to select message from history,\n"+
                                   Widgets.acceleratorToText(Settings.keyFormatCommitMessage)+" to format message,\n"+
                                   Widgets.acceleratorToText(Settings.keyCommitMessageDone)+" to add files."
                                  );

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,1.0));
      Widgets.layout(subComposite,6,0,TableLayoutData.WE);
      {
        widgetBinary = Widgets.newCheckbox(subComposite,"binary");
        widgetBinary.setVisible(repositoryTab.repository.supportSetFileMode());
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

          data.message             = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
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
    widgetHistory.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
        }
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
          Widgets.notify(dialog,USER_EVENT_NEW_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
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
          // invoke add-button
          Widgets.invoke(widgetAdd);
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
        String currentMessage = widgetMessage.getText().trim();

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
    Dialogs.show(dialog,Settings.geometryAdd,Settings.setWindowLocation);

    // add files
    if (!widgetFiles.isDisposed())
    {
      for (String fileName : FileData.toSortedFileNameArray(fileDataSet))
      {
        widgetFiles.add(fileName);
      }
    }

    // add history
    if (!widgetHistory.isDisposed() && (history != null))
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
    CommitMessage commitMessage = null;
    try
    {
      if (!data.binaryFlag)
      {
        // get file names
        final String[] fileNames = FileData.toSortedFileNameArray(fileDataSet,repositoryTab.repository);

        // check for TABs/trailing whitespaces in files and convert/remove
        for (final String fileName : fileNames)
        {
          if (!repositoryTab.repository.isSkipWhitespaceCheckFile(fileName))
          {
            // check for TABs/trailing whitespaces in file
            final boolean containTABs                = Settings.checkTABs                && repositoryTab.containTABs(fileName);
            final boolean containTrailingWhitespaces = Settings.checkTrailingWhitespaces && repositoryTab.containTrailingWhitespaces(fileName);

            // convert TABs, remove trailing whitespaces
            if (containTABs || containTrailingWhitespaces)
            {
              final RepositoryTab.ConvertWhitespacesResult[] result = new RepositoryTab.ConvertWhitespacesResult[1];
              final String                                   message;
              if      (containTABs && containTrailingWhitespaces) message = "The following file contain TABs and trailing whitespaces:\n\n"+fileName;
              else if (containTABs)                               message = "The following file contain TABs or:\n\n"+fileName;
              else /*if (containTrailingWhitespaces)*/            message = "The following file contain trailing whitespaces:\n\n"+fileName;
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  result[0] = repositoryTab.convertWhitespaces(fileName,
                                                               fileNames,
                                                               message,
                                                               containTABs,
                                                               containTrailingWhitespaces
                                                              );
                }
              });
              if (result[0] == RepositoryTab.ConvertWhitespacesResult.ABORTED) return;
            }
          }
        }
      }

      // create and add message to history
      if (data.immediateCommitFlag)
      {
        commitMessage = new CommitMessage(data.message);
        commitMessage.addToHistory();
      }

      // add files
      repositoryTab.repository.add(fileDataSet,commitMessage,data.binaryFlag);

      // update file states
      repositoryTab.repository.updateStates(fileDataSet);
      display.syncExec(new Runnable()
      {
        public void run()
        {
          if (!repositoryTab.widgetComposite.isDisposed()) repositoryTab.updateTreeItems(fileDataSet);
        }
      });

      // free resources
      commitMessage.done(); commitMessage = null;
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
