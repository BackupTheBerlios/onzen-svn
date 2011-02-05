/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRevisionInfo.java,v $
* $Revision: 1.2 $
* $Author: torsten $
* Contents: command show file revision
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
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;
//import java.util.WeakHashMap;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
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

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;        

  // widgets
  private final Label         widgetRevision;
  private final Label         widgetAuthor;
  private final Text          widgetCommitMessage;
  private final Text          widgetLog;
  private final Button        widgetButtonClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** show revision command
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileData file to show revision information
   * @param revision revision to show or null
   */
  CommandRevisionInfo(final RepositoryTab repositoryTab, Shell shell, Repository repository, FileData fileData, String revision)
  {
    Composite         composite,subComposite;
    Label             label;
    ScrolledComposite scrolledComposite;
    Button            button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"File revision: "+fileData.getFileName(),Settings.geometryRevisionInfo.x,Settings.geometryRevisionInfo.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0},new double[]{0.0,1.0}));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(fileData.getFileName());
      Widgets.layout(label,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Revision:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetRevision = Widgets.newLabel(composite);
      Widgets.layout(widgetRevision,1,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Date:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(Onzen.DATETIME_FORMAT.format(fileData.datetime));
      Widgets.layout(label,2,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Size:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(Units.formatByteSize(fileData.size));
      Widgets.layout(label,3,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Mode:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      label = Widgets.newLabel(composite);
      label.setText(fileData.mode.toString());
      Widgets.layout(label,4,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Author:");
      Widgets.layout(label,5,0,TableLayoutData.W);

      widgetAuthor = Widgets.newLabel(composite);
      Widgets.layout(widgetAuthor,5,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Commit message:");
      Widgets.layout(label,6,0,TableLayoutData.NW);

      widgetCommitMessage = Widgets.newText(composite,SWT.MULTI|SWT.READ_ONLY|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetCommitMessage.setBackground(Onzen.COLOR_BACKGROUND);
      Widgets.layout(widgetCommitMessage,6,1,TableLayoutData.NSWE);

      label = Widgets.newLabel(composite,"Log:");
      Widgets.layout(label,7,0,TableLayoutData.NW);

      widgetLog = Widgets.newText(composite,SWT.MULTI|SWT.READ_ONLY|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      widgetLog.setBackground(Onzen.COLOR_BACKGROUND);
      Widgets.layout(widgetLog,7,1,TableLayoutData.NSWE);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetButtonClose,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      widgetButtonClose.addSelectionListener(new SelectionListener()
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
    Dialogs.show(dialog);

    if (revision != null)
    {
      // start get revision info
      Background.run(new BackgroundRunnable(fileData,revision)
      {
        public void run(FileData fileData, final String revision)
        {
          // get revision info
          repositoryTab.setStatusText("Get revisions for '%s'...",fileData.getFileName());
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
        repositoryTab.setStatusText("Get log for '%s'...",fileData.getFileName());
        try
        {
          data.logData = repositoryTab.repository.getLog(fileData);
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
        }

        // show
        if (!display.isDisposed())
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

  /** show revision command
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileData file to show revision information
   * @param revisionData revision to show
   */
  CommandRevisionInfo(RepositoryTab repositoryTab, Shell shell, Repository repository, FileData fileData, RevisionData revisionData)
  {
    this(repositoryTab,shell,repository,fileData,(String)null);
    data.revisionData = revisionData;
    show();
  }

  /** show revision command of last revision
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileData file to show revision information
   */
  CommandRevisionInfo(RepositoryTab repositoryTab, Shell shell, Repository repository, FileData fileData)
  {
    this(repositoryTab,shell,repository,fileData,repository.getLastRevision());
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      widgetButtonClose.setFocus();
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
    if (!display.isDisposed())
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

            if (!widgetRevision.isDisposed()) widgetRevision.setText(data.revisionData.revision);
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
