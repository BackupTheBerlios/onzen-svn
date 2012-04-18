/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command show file revision
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base

// graphics
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
    RevisionData revisionData;
    LogData[]    logData;

    Data()
    {
      this.revisionData = null;
      this.logData      = null;
    }
  };

  // --------------------------- constants --------------------------------
  private final int USER_EVENT_SELECT = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text          widgetRevision;
  private final Text          widgetDate;
  private final Text          widgetAuthor;
  private final Text          widgetCommitMessage;
  private final Text          widgetLog;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** show revision command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to show revision information
   * @param revision revision to show or null
   */
  CommandRevisionInfo(Shell shell, final RepositoryTab repositoryTab, final FileData fileData, final String revision)
  {
    Composite         composite,subComposite;
    Label             label;
    Text              text;
    ScrolledComposite scrolledComposite;
    Button            button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"File revision: "+fileData.getFileName(),new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0},new double[]{0.0,1.0}));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      text = Widgets.newStringView(composite,SWT.LEFT);
      text.setText(fileData.getFileName());
      Widgets.layout(text,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Revision:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetRevision = Widgets.newStringView(composite,SWT.LEFT);
      widgetRevision.setBackground(composite.getBackground());
      Widgets.layout(widgetRevision,1,1,TableLayoutData.WE);
      widgetRevision.addMouseListener(new MouseListener()
      {
        public void mouseDoubleClick(MouseEvent mouseEvent)
        {
          Text widget = (Text)mouseEvent.widget;

          Event event = new Event();
          event.widget = widget;
          widget.notifyListeners(USER_EVENT_SELECT,event);
        }
        public void mouseDown(MouseEvent mouseEvent)
        {
        }
        public void mouseUp(MouseEvent mouseEvent)
        {
        }
      });
      widgetRevision.addListener(USER_EVENT_SELECT,new Listener()
      {
        public void handleEvent(Event event)
        {
          Text   widget    = (Text)event.widget;
          String text      = widget.getText();
          Point  selection = widget.getSelection();

          while ((selection.x > 0) && Character.isLetterOrDigit(text.charAt(selection.x-1)))
          {
            selection.x--;
          }
          while ((selection.y < text.length()) && Character.isLetterOrDigit(text.charAt(selection.y)))
          {
            selection.y++;
          }
          widget.setSelection(selection);
        }
      });

      label = Widgets.newLabel(composite,"Date:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetDate = Widgets.newStringView(composite,SWT.LEFT);
      Widgets.layout(widgetDate,2,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Size:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(Units.formatByteSize(fileData.size)+" ("+fileData.size+"bytes)");
      Widgets.layout(label,3,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Permissions:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(fileData.getPermissions(repositoryTab.repository));
      Widgets.layout(label,4,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Mode:");
      Widgets.layout(label,5,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(fileData.mode.toString());
      Widgets.layout(label,5,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Author:");
      Widgets.layout(label,6,0,TableLayoutData.W);

      widgetAuthor = Widgets.newStringView(composite,SWT.LEFT);
      Widgets.layout(widgetAuthor,6,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Commit message:");
      Widgets.layout(label,7,0,TableLayoutData.NW);

      widgetCommitMessage = Widgets.newText(composite,SWT.MULTI|SWT.READ_ONLY|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetCommitMessage.setBackground(Onzen.COLOR_BACKGROUND);
      Widgets.layout(widgetCommitMessage,7,1,TableLayoutData.NSWE);

      label = Widgets.newLabel(composite,"Log:");
      Widgets.layout(label,8,0,TableLayoutData.NW);

      widgetLog = Widgets.newText(composite,SWT.MULTI|SWT.READ_ONLY|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetLog.setBackground(Onzen.COLOR_BACKGROUND);
      Widgets.layout(widgetLog,8,1,TableLayoutData.NSWE);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"View");
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          CommandView commandView = new CommandView(dialog,repositoryTab,fileData,revision);
          commandView.run();
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryRevisionInfo = dialog.getSize();

          Dialogs.close(dialog);
        }
      });
    }

    // listeners

    // show dialog
    Dialogs.show(dialog,Settings.geometryRevisionInfo,Settings.setWindowLocation);

    if (revision != null)
    {
      // start get revision info
      Background.run(new BackgroundRunnable(fileData,revision)
      {
        public void run(FileData fileData, final String revision)
        {
          // get revision info
          Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
          repositoryTab.setStatusText("Get revision '%s' for '%s'...",revision,fileData.getFileName());
          try
          {
            data.revisionData = repositoryTab.repository.getRevisionData(fileData,revision);
          }
          catch (RepositoryException exception)
          {
            final String exceptionMessage = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(dialog,"Getting file revision '%s' fail: %s",revision,exceptionMessage);
              }
            });
            return;
          }
          finally
          {
            repositoryTab.clearStatusText();
            Widgets.resetCursor(dialog);
          }

          // show
          show();
        }
      });
    }

    // start get log
    Background.run(new BackgroundRunnable(data,repositoryTab,fileData)
    {
      public void run()
      {
        final Data          data          = (Data)         userData[0];
        final RepositoryTab repositoryTab = (RepositoryTab)userData[1];
        final FileData      fileData      = (FileData)     userData[2];

        // get log
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get log for '%s'...",fileData.getFileName());
        try
        {
          data.logData = repositoryTab.repository.getLog(fileData);
for (LogData l : data.logData)
{
Dprintf.dprintf("-----");
for (String s : l.commitMessage) Dprintf.dprintf("s=#%s#",s);

 }

          }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting log of file fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
          Widgets.resetCursor(dialog);
        }

        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              if (!widgetLog.isDisposed())
              {
                StringBuilder buffer = new StringBuilder();
                for (LogData logData : data.logData)
                {
                  buffer.append("Revision: "); buffer.append(logData.revision); buffer.append('\n');
                  buffer.append("---"); buffer.append('\n');
                  for (String line : logData.commitMessage)
                  {
                    buffer.append(line); buffer.append('\n');
                  }
                  buffer.append('\n');
                }
                widgetLog.setText(buffer.toString());
              }
            }
          });
        }
      }
    });
  }

  /** show revision command of last revision
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to show revision information
   */
  CommandRevisionInfo(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData,repositoryTab.repository.getLastRevision());
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      widgetClose.setFocus();
      Dialogs.run(dialog);
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandRevisionInfo {}";
  }

  //-----------------------------------------------------------------------

  /** show revision info
   */
  private void show()
  {
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          if (data.revisionData != null)
          {
            StringBuilder buffer = new StringBuilder();
            for (String line : data.revisionData.commitMessage)
            {
              buffer.append(line); buffer.append('\n');
            }

            if (!widgetRevision.isDisposed()) widgetRevision.setText(data.revisionData.getRevisionText());
            if (!widgetDate.isDisposed()) widgetDate.setText(Onzen.DATETIME_FORMAT.format(data.revisionData.date));
            if (!widgetAuthor.isDisposed()) widgetAuthor.setText(data.revisionData.author);
            if (!widgetCommitMessage.isDisposed()) widgetCommitMessage.setText(buffer.toString());

            // notify modification
            Widgets.modified(data);
          }
        }
      });
    }
  }
}

/* end of file */
