/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command commit files/directories
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;

import java.sql.SQLException;

// graphics
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
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
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
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
    String[] patchLinesNoWhitespaces;   // changed lines (without whitespace changes)
    String[] patchLines;                // changed lines
    String[] message;                   // commit message
    String   lastMessage;               // last set commit message
    boolean  messageEdited;             // true iff commit message edited

    Data()
    {
      this.patchLinesNoWhitespaces = null;
      this.patchLines              = null;
      this.message                 = null;
      this.lastMessage             = "";
      this.messageEdited           = false;
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
  private final StyledText           widgetChanges;
  private final ScrollBar            widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Button               widgetIgnoreWhitespaces;
  private final List                 widgetFiles;
  private final List                 widgetHistory;
  private final Text                 widgetMessage;
  private final Button               widgetCommit;

  private static String[]            lastMessage = new String[0];

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** commit command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to commit
   */
  CommandCommit(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet)
  {
    Composite composite,subComposite,subSubComposite;
    TabFolder tabFolder;
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

    // commit files dialog
    dialog = Dialogs.openModal(shell,"Commit files",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.5,0.0,0.15,0.0,0.35},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);
      {
        subComposite = Widgets.addTab(tabFolder,"Changes");
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetChanges = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
          widgetChanges.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetChanges,0,0,TableLayoutData.NSWE);
          widgetChanges.setToolTipText("Changes to commit.");
          widgetHorizontalScrollBar = widgetChanges.getHorizontalBar();
          widgetVerticalScrollBar   = widgetChanges.getVerticalBar();

          widgetIgnoreWhitespaces = Widgets.newCheckbox(subComposite,"Ignore whitespace changes");
          widgetIgnoreWhitespaces.setSelection(true);
          widgetIgnoreWhitespaces.setEnabled(false);
          Widgets.layout(widgetIgnoreWhitespaces,1,0,TableLayoutData.W);
          Widgets.addModifyListener(new WidgetModifyListener(widgetIgnoreWhitespaces,data)
          {
            public void modified(Control control)
            {
              Widgets.setEnabled(control,(data.patchLinesNoWhitespaces != null) && (data.patchLines != null));
            }
          });
          widgetIgnoreWhitespaces.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Button widget = (Button)selectionEvent.widget;

              String[] patchLines = widget.getSelection() ? data.patchLinesNoWhitespaces : data.patchLines;
              if (patchLines != null)
              {
                // set new text
                setChangesText(patchLines);
              }
            }
          });
        }

        subComposite = Widgets.addTab(tabFolder,"Files");
        subComposite.setLayout(new TableLayout(1.0,1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetFiles = Widgets.newList(subComposite);
          widgetFiles.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetFiles,0,0,TableLayoutData.NSWE);
          widgetFiles.setToolTipText("Files to commit.");
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
      widgetMessage.setText(StringUtils.join(lastMessage,widgetMessage.DELIMITER));
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n"+
                                   Widgets.acceleratorToText(Settings.keyPrevCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyFirstCommitMessage)+"/"+Widgets.acceleratorToText(Settings.keyLastCommitMessage)+"to select message from history,\n"+
                                   Widgets.acceleratorToText(Settings.keyFormatCommitMessage)+" to format message,\n"+
                                   Widgets.acceleratorToText(Settings.keyCommitMessageDone)+" to commit changed files."
                                  );
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetCommit,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // get message
          data.message = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);

          // check commit messaage
          CommitMessage commitMessage = new CommitMessage(data.message);
          if (!repositoryTab.repository.validCommitMessage(commitMessage))
          {
            if (!Dialogs.confirm(shell,"Confirmation","The commit message is probably too long or may not be accepted.\n\nCommit with the message anyway?"))
            {
              return;
            }
          }

          // close dialog
          Settings.geometryCommit = dialog.getSize();
          Dialogs.close(dialog,true);
        }
      });
      widgetCommit.setToolTipText("Commit files.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // get message
          data.message = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);

          // close dialog
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
          Widgets.notify(dialog,USER_EVENT_ADD_REPLACE_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
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
          Widgets.notify(dialog,USER_EVENT_ADD_REPLACE_MESSAGE,StringUtils.join(history.get(i),widgetMessage.DELIMITER).trim());
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
          // invoke commit-button
          Widgets.invoke(widgetCommit);
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
    Dialogs.show(dialog,Settings.geometryCommit,Settings.setWindowLocation);

    // add files
    if (!widgetFiles.isDisposed())
    {
      for (String fileName : FileData.toSortedFileNameArray(fileDataSet))
      {
        widgetFiles.add(fileName);
      }
    }

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

    // start getting changes
    Background.run(new BackgroundRunnable(fileDataSet)
    {
      public void run(HashSet<FileData> fileDataSet)
      {
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get patch...");
        try
        {
          // get patch without whitespace change
          data.patchLinesNoWhitespaces = repositoryTab.repository.getPatchLines(fileDataSet,true);
//Dprintf.dprintf("data.patchLinesNoWhitespaces=%d",data.patchLinesNoWhitespaces.length);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   !widgetIgnoreWhitespaces.isDisposed()
                    && (data.patchLinesNoWhitespaces != null)
                    && widgetIgnoreWhitespaces.getSelection()
                   )
                {
                  // set new text
                  setChangesText(data.patchLinesNoWhitespaces);
                }
              }
            });
          }

          // get patch
          data.patchLines = repositoryTab.repository.getPatchLines(fileDataSet,false);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   !widgetIgnoreWhitespaces.isDisposed()
                    && (data.patchLines != null)
                    && !widgetIgnoreWhitespaces.getSelection()
                   )
                {
                  // set new text
                  setChangesText(data.patchLines);
                }
              }
            });
          }

          // notify modification
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Widgets.modified(data);
              }
            });
          }
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(shell,"Getting file patch fail: %s",exceptionMessage);
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
      }
    });

    // update
  }

  /** commit command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to commit
   */
  CommandCommit(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData.toSet());
  }

  /** run dialog
   */
  public void run()
  {
    widgetMessage.setFocus();
    Dialogs.run(dialog,false,new DialogRunnable()
    {
      public void done(Object result)
      {
        if ((Boolean)result)
        {
          Background.run(new BackgroundRunnable()
          {
            public void run()
            {
              commit();
            }
          });

          lastMessage = new String[0];
        }
        else
        {
          lastMessage = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
        }
      }
    });
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    widgetMessage.setFocus();
    return (Boolean)Dialogs.run(dialog,false,new DialogRunnable()
    {
      public void done(Object result)
      {
        if ((Boolean)result)
        {
          commit();

          lastMessage = new String[0];
        }
        else
        {
          lastMessage = widgetMessage.getText().trim().split(widgetMessage.DELIMITER);
        }
      }
    });
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandCommit {}";
  }

  //-----------------------------------------------------------------------

  /** set changes text
   * @param lines changes lines
   */
  private void setChangesText(String[] lines)
  {
    if (   !widgetChanges.isDisposed()
        && !widgetVerticalScrollBar.isDisposed()
        && !widgetHorizontalScrollBar.isDisposed()
       )
    {
      StringBuilder text     = new StringBuilder();
      int           lineNb   = 1;
      int           maxWidth = 0;

      // get text
      for (String line : lines)
      {
        text.append(line); text.append('\n');

        maxWidth = Math.max(maxWidth,line.length());
      }

      // set text
      widgetChanges.setText(text.toString());

      // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
      widgetChanges.redraw();
      widgetChanges.update();

      // show top
      widgetChanges.setTopIndex(0);
      widgetChanges.setCaretOffset(0);
      widgetVerticalScrollBar.setSelection(0);
    }
  }

  /** do commit
   * @param data data
   * @param repositoryTab repository tab
   */
  private void commit()
  {
    repositoryTab.setStatusText("Commit files...");
    CommitMessage commitMessage = null;
    try
    {
      // get file names
      final String[] fileNames = FileData.toSortedFileNameArray(fileDataSet,repositoryTab.repository);

      // check for TABs/trailing whitespaces in files and convert/remove
      boolean allFlag = false;
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
            switch (result[0])
            {
              case OK:
                break;
              case ALL:
                allFlag = true;
                break;
              case FAIL:
                break;
              case ABORTED:
                return;
            }
          }

        }
      }

      // create message
      commitMessage = new CommitMessage(data.message);

      // add message to history
      commitMessage.addToHistory();

      // commit files
      repositoryTab.repository.commit(fileDataSet,commitMessage);

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
      final String   exceptionMessage = exception.getMessage();
      final String[] extendedMessage  = exception.getExtendedMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,extendedMessage,"Cannot commit files (error: %s)",exceptionMessage);
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
