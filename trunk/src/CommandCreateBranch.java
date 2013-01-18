/***********************************************************************\
*
* $Revision: 800 $
* $Date: 2012-01-28 10:49:16 +0100 (Sat, 28 Jan 2012) $
* $Author: trupp $
* Contents: command create branch
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** create branch command
 */
class CommandCreateBranch
{
  /** dialog data
   */
  class Data
  {
    String[] branchNames;
    String   rootName;
    String   branchName;
    String[] message;
    boolean  immediateCommitFlag;

    Data()
    {
      this.branchNames         = null;
      this.rootName            = null;
      this.branchName          = null;
      this.message             = null;
      this.immediateCommitFlag = Settings.immediateCommit;
    }
  };

  // --------------------------- constants --------------------------------

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
  private final Combo                widgetBranchName;
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

    // get default root/branch name
    String defaultRootName   = repositoryTab.repository.getDefaultRootName();
    String defaultBranchName = repositoryTab.repository.getDefaultBranchName();

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get shell, display
    this.shell   = shell;
    this.display = shell.getDisplay();

    // get commit history
    history = CommitMessage.getHistory();

    // add files dialog
    dialog = Dialogs.openModal(shell,"Create new branch",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,0.0,1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        if (defaultRootName != null)
        {
          label = Widgets.newLabel(subComposite,"Root:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetRootName = Widgets.newText(subComposite);
          if (defaultRootName != null) widgetRootName.setText(defaultRootName);
          Widgets.layout(widgetRootName,0,1,TableLayoutData.WE);
          widgetRootName.setToolTipText("Root for the new branch. For SVN, HG and GIT this is usually the main development fork 'trunk' or some directory name, usually below 'branches'.");

          label = Widgets.newLabel(subComposite,"Branch name:");
          Widgets.layout(label,1,0,TableLayoutData.W);

          widgetBranchName = Widgets.newCombo(subComposite);
          if (defaultBranchName != null) widgetBranchName.setText(defaultBranchName);
          Widgets.layout(widgetBranchName,1,1,TableLayoutData.WE);
          widgetBranchName.setToolTipText("Branch name. For CVS this is a tag name, for SVN, HG and GIT this is a directory name, usually below 'branches'.\n\nNote: HG use the term 'branch' different. Nevertheless a 'branch' follow here the common understanding: a branch is a fork of the checked-in files.");
        }
        else
        {
          widgetRootName = null;

          label = Widgets.newLabel(subComposite,"Branch name:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetBranchName = Widgets.newCombo(subComposite);
          widgetBranchName.setText(defaultBranchName);
          Widgets.layout(widgetBranchName,0,1,TableLayoutData.WE);
        }
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
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Up/Down/Home/End to select message from history.\n\nUse Ctrl-Return to create branch.");

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
          data.branchName          = widgetBranchName.getText().trim();
          data.message             = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
          data.immediateCommitFlag = widgetImmediateCommit.getSelection();

          createBranch();

          Settings.geometryCreateBranch = dialog.getSize();
          Settings.immediateCommit      = widgetImmediateCommit.getSelection();

          Dialogs.close(dialog,true);
        }
      });
      widgetCreateBranch.setToolTipText("Create new branch.");

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
    if (widgetRootName != null) Widgets.setNextFocus(widgetRootName,widgetBranchName);
    widgetBranchName.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Combo widget = (Combo)modifyEvent.widget;

        widgetCreateBranch.setEnabled(!widget.getText().trim().isEmpty());
      }
    });
    Widgets.setNextFocus(widgetBranchName,widgetMessage);
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          widgetMessage.setText(StringUtils.join(history.get(i),widgetMessage.DELIMITER));
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
          if (i < 0) i = history.size();

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i < history.size()-1)
            {
              widgetHistory.setSelection(i+1);
              display.update();
              widgetHistory.showSelection();
              widgetMessage.setText(StringUtils.join(history.get(i+1),widgetMessage.DELIMITER));
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i > 0)
            {
              widgetHistory.setSelection(i-1);
              display.update();
              widgetHistory.showSelection();
              widgetMessage.setText(StringUtils.join(history.get(i-1),widgetMessage.DELIMITER));
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.size() > 0)
            {
              widgetHistory.setSelection(0);
              display.update();
              widgetHistory.showSelection();
              widgetMessage.setText(StringUtils.join(history.getFirst(),widgetMessage.DELIMITER));
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.END)
          {
            // last history entry
            if (history.size() > 0)
            {
              widgetHistory.setSelection(history.size()-1);
              display.update();
              widgetHistory.showSelection();
              widgetMessage.setText(StringUtils.join(history.getLast(),widgetMessage.DELIMITER));
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke commit-button
            Widgets.invoke(widgetCreateBranch);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

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

    // add existing branch names
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
        // get branch names
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get branch names...");
        try
        {
          data.branchNames = repositoryTab.repository.getBranchNames();
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,String.format("Getting branch names fail: %s",exceptionMessage));
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

        // add to widget
        display.syncExec(new Runnable()
        {
          public void run()
          {
            for (String branchName : data.branchNames)
            {
              widgetBranchName.add(branchName);
            }
          }
        });
      }
    });

    // update
  }

  /** run dialog
   */
  public void run()
  {
    Widgets.setFocus(widgetBranchName);
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
   */
  private void createBranch()
  {
    if (!data.branchName.trim().isEmpty())
    {
      final BusyDialog busyDialog = new BusyDialog(dialog,
                                                   "Create new branch",
                                                   "Create new branch '"+
                                                   data.branchName.trim()+
                                                   "':",
                                                   BusyDialog.TEXT0
                                                  );
      busyDialog.autoAnimate();

      repositoryTab.setStatusText("Create branch '"+data.branchName.trim()+"'...");
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
        repositoryTab.repository.newBranch(data.rootName,data.branchName,commitMessage,busyDialog);

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

            Dialogs.error(dialog,extendedMessage,"Create branch fail:\n%s",exceptionMessage);
          }
        });
        Onzen.printStacktrace(exception);
        return;
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
  }
}

/* end of file */
