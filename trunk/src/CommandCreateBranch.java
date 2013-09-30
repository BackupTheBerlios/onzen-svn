/***********************************************************************\
*
* $Revision: 800 $
* $Date: 2012-01-28 10:49:16 +0100 (Sat, 28 Jan 2012) $
* $Author: trupp $
* Contents: command create branch/tag
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.IOException;
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

/** create branch/tag command
 */
class CommandCreateBranch
{
  /** dialog data
   */
  class Data
  {
    String[] branchTagNames;
    String   rootName;
    String   branchTagName;             // branch/tag name
    String[] message;                   // commit message
    String   lastMessage;               // last set commit message
    boolean  messageEdited;             // true iff commit message edited
    boolean  immediateCommitFlag;       // true for immediate commit

    Data()
    {
      this.branchTagNames      = null;
      this.rootName            = null;
      this.branchTagName       = null;
      this.message             = null;
      this.lastMessage         = "";
      this.messageEdited       = false;
      this.immediateCommitFlag = Settings.immediateCommit;
    }
  };

  // --------------------------- constants --------------------------------
  // user events
  private final int USER_EVENT_NEW_MESSAGE         = 0xFFFF+0;
  private final int USER_EVENT_ADD_REPLACE_MESSAGE = 0xFFFF+1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab        repositoryTab;
  private final Shell                shell;
  private final Display              display;

  // dialog
  private final Data                 data = new Data();
  private final Shell                dialog;
  private final LinkedList<String[]> history;

  // widgets
  private final Text                 widgetRootName;
  private final Combo                widgetBranchTagNames;
  private final List                 widgetHistory;
  private final Text                 widgetMessage;
  private final Button               widgetImmediateCommit;
  private final Button               widgetCreateBranch;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** set file mode command
   * @param shell shell
   * @param repositoryTab repository tab
   */
  CommandCreateBranch(final Shell shell, final RepositoryTab repositoryTab)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // get default root, branch/tag name
    String defaultRootName      = "";
    String defaultBranchTagName = repositoryTab.repository.getDefaultBranchTagName();

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get shell, display
    this.shell   = shell;
    this.display = shell.getDisplay();

    // get commit history
    history = CommitMessage.getHistory();

    // add files dialog
    dialog = Dialogs.openModal(shell,"Create new branch/tag",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0,1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        int row = 0;
        if (defaultRootName != null)
        {
          label = Widgets.newLabel(subComposite,"Root:");
          Widgets.layout(label,row,0,TableLayoutData.W);
          widgetRootName = Widgets.newText(subComposite);
          if (defaultRootName != null) widgetRootName.setText(defaultRootName);
          Widgets.layout(widgetRootName,row,1,TableLayoutData.WE);
          widgetRootName.setToolTipText("Root for the new branch/tag. For SVN, HG and GIT this is usually the main development fork 'trunk' or some directory name, usually below 'branches' or 'tags'.");
          row++;
        }
        else
        {
          widgetRootName = null;
        }

        label = Widgets.newLabel(subComposite,"Branch/tag name:");
        Widgets.layout(label,row,0,TableLayoutData.W);
        widgetBranchTagNames = Widgets.newCombo(subComposite);
        if (defaultBranchTagName != null) widgetBranchTagNames.setText(defaultBranchTagName);
        Widgets.layout(widgetBranchTagNames,row,1,TableLayoutData.WE);
        widgetBranchTagNames.setToolTipText("Branch or tag name. For CVS this is a tag name, for SVN, HG and GIT this is a directory name, usually below 'branches' or 'tags'.\n\nNote: HG use the term 'branch' different. Nevertheless a 'branch' follow here the common understanding: a branch is a fork of the checked-in files.");
        row++;
      }

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,2,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetMessage.setEnabled(Settings.immediateCommit);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
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
                                   Widgets.acceleratorToText(Settings.keyCommitMessageDone)+" to create branch/tag."
                                  );

      widgetImmediateCommit = Widgets.newCheckbox(composite,"immediate commit");
      widgetImmediateCommit.setSelection(Settings.immediateCommit);
      Widgets.layout(widgetImmediateCommit,5,0,TableLayoutData.E);
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

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCreateBranch = Widgets.newButton(composite,"Create");
      widgetCreateBranch.setEnabled(false);
      Widgets.layout(widgetCreateBranch,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCreateBranch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.rootName            = (widgetRootName != null) ? widgetRootName.getText().trim() : null;
          data.branchTagName       = widgetBranchTagNames.getText().trim();
          data.message             = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
          data.immediateCommitFlag = widgetImmediateCommit.getSelection();

          if (createBranchTag())
          {
            Settings.geometryCreateBranch = dialog.getSize();
            Settings.immediateCommit      = widgetImmediateCommit.getSelection();

            Dialogs.close(dialog,true);
          }
        }
      });
      widgetCreateBranch.setToolTipText("Create new branch/tag.");

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
    if (widgetRootName != null) Widgets.setNextFocus(widgetRootName,widgetBranchTagNames);
    widgetBranchTagNames.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Combo widget = (Combo)modifyEvent.widget;

        widgetCreateBranch.setEnabled(!widget.getText().trim().isEmpty());
      }
    });
    Widgets.setNextFocus(widgetBranchTagNames,widgetMessage);
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
          // invoke create branch-button
          Widgets.invoke(widgetCreateBranch);
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
Dprintf.dprintf("lastMessage=#%s#",data.lastMessage);
Dprintf.dprintf("currentMessage=#%s#",currentMessage);
Dprintf.dprintf("event.text=#%s#",event.text);
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
    Dialogs.show(dialog,Settings.geometryCreateBranch,Settings.setWindowLocation);

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

    // add existing branch/tag names
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
        // get branch names
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get branch/tag names...");
        try
        {
          data.branchTagNames = repositoryTab.repository.getBranchTagNames();
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,String.format("Getting branch/tag names fail: %s",exceptionMessage));
            }
          });
          Onzen.printStacktrace(exception);
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
          Widgets.resetCursor(dialog);
        }

        // add branch names to widget
        display.syncExec(new Runnable()
        {
          public void run()
          {
            for (String branchTagName : data.branchTagNames)
            {
              widgetBranchTagNames.add(branchTagName);
            }
          }
        });
      }
    });
  }

  /** run dialog
   */
  public void run()
  {
    Widgets.setFocus(widgetBranchTagNames);
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandCreateBranch {}";
  }

  //-----------------------------------------------------------------------

  /** create branch
   * @return true iff branch/tag created
   */
  private boolean createBranchTag()
  {
    if (!data.branchTagName.trim().isEmpty())
    {
      final BusyDialog busyDialog = new BusyDialog(dialog,
                                                   "Create new branch/tag",
                                                   "Create new branch/tag '"+
                                                   data.branchTagName.trim()+
                                                   "':",
                                                   BusyDialog.TEXT0
                                                  );
      busyDialog.autoAnimate();

      repositoryTab.setStatusText("Create branch/tag '"+data.branchTagName.trim()+"'...");
      CommitMessage commitMessage = null;
      try
      {
        // create and add message to history
        if (data.immediateCommitFlag)
        {
          commitMessage = new CommitMessage(data.message);
          commitMessage.addToHistory();
        }

        // create new branch
        repositoryTab.repository.newBranch(data.rootName,data.branchTagName,commitMessage,busyDialog);

        // free resources
        commitMessage.done(); commitMessage = null;
      }
      catch (RepositoryException exception)
      {
        final String   exceptionMessage = exception.getMessage();
        final String[] extendedMessage  = exception.getExtendedMessage();
        display.syncExec(new Runnable()
        {
          public void run()
          {
            busyDialog.close();

            Dialogs.error(dialog,extendedMessage,"Create branch/tag fail:\n%s",exceptionMessage);
          }
        });
        Onzen.printStacktrace(exception);
        return false;
      }
      finally
      {
        if (commitMessage != null) commitMessage.done();
        display.syncExec(new Runnable()
        {
          public void run()
          {
            busyDialog.close();
          }
        });
        repositoryTab.clearStatusText();
      }
    }

    return true;
  }
}

/* end of file */
