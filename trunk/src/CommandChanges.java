/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRevisions.java,v $
* $Revision: 1.5 $
* $Author: torsten $
* Contents: command to show incoming/outgoing file changes
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
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
import org.eclipse.swt.widgets.ScrollBar;
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

/** view incoming/outgoing file changes
 */
class CommandChanges
{
  enum ChangesTypes
  {
    INCOMING,
    OUTGOING
  };

  /** drawing info
   */
  class DrawInfo
  {
    RevisionData revisionData;
    Rectangle    container;
    Rectangle    handle;

    /** create draw info
     * @param revisionData revision data
     * @param container container size
     * @param handle handle size
     */
    DrawInfo(RevisionData revisionData, Rectangle container, Rectangle handle)
    {
      this.revisionData = revisionData;
      this.container    = container;
      this.handle       = handle;
    }
  }

  /** dialog data
   */
  class Data
  {
    LogData[]         changes;
    Rectangle         view;
    Point             size;
    DrawInfo[]        drawInfos;
    boolean           containerResizeFlag;
    Point             containerResizeStart;
    Point             containerResizeDelta;
    Rectangle         containerResizeRectangle;
    RevisionData      selectedRevisionData0,selectedRevisionData1;
    RevisionData      selectedRevisionData;
    LogDataComparator logDataComparator;

    Data()
    {
      this.changes                  = null;
      this.view                     = new Rectangle(0,0,0,0);
      this.size                     = new Point(0,0);
      this.drawInfos                = null;
      this.containerResizeFlag      = false;
      this.containerResizeStart     = new Point(0,0);
      this.containerResizeDelta     = new Point(0,0);
      this.containerResizeRectangle = new Rectangle(0,0,0,0);
      this.selectedRevisionData0    = null;
      this.selectedRevisionData1    = null;
      this.logDataComparator        = new LogDataComparator();
    }
  };

  // --------------------------- constants --------------------------------
  private final int MARGIN               = 10;
  private final int PADDING              = 20;
  private final int CONTAINER_MARGIN     =  4;
  private final int CONTAINER_MIN_WIDTH  = 20;
  private final int CONTAINER_MIN_HEIGHT = 20;
  private final int HANDLE_SIZE          =  8;

  private final Color COLOR_CONTAINER            = Onzen.COLOR_GRAY;
  private final Color COLOR_HANDLE               = Onzen.COLOR_RED;
  private final Color COLOR_CONTAINER_CURRENT_   = Onzen.COLOR_GRAY;
  private final Color COLOR_CONTAINER_SELECTED0  = Onzen.COLOR_BLUE;
  private final Color COLOR_CONTAINER_SELECTED1  = Onzen.COLOR_GREEN;
  private final Color COLOR_LINES                = Onzen.COLOR_BLACK;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final Clipboard     clipboard;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetChanges;
  private Shell               widgetChangesToolTip              = null;
  private Point               widgetChangesToolTipMousePosition = new Point(0,0);
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create changes list view
   * @param shell shell
   * @param repositoryTab repository tab
   * @param changesType
   */
  CommandChanges(final Shell shell, final RepositoryTab repositoryTab, ChangesTypes changesType)
  {
    TableColumn tableColumn;
    Composite         composite;
    ScrolledComposite scrolledComposite;
    Label             label;
    Button            button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = null;

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // create dialog
    String title = "";
    switch (changesType)
    {
      case INCOMING: title = "Incoming changes"; break;
      case OUTGOING: title = "Outgoing changes"; break;
    }
    dialog = Dialogs.open(shell,title,new double[]{1.0,0.0},1.0);

    widgetChanges = Widgets.newTable(dialog,SWT.H_SCROLL|SWT.V_SCROLL);
    Widgets.layout(widgetChanges,0,0,TableLayoutData.NSWE,0,0,4);
    SelectionListener selectionListener = new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        TableColumn tableColumn = (TableColumn)selectionEvent.widget;

        if      (tableColumn == widgetChanges.getColumn(0)) data.logDataComparator.setSortMode(LogDataComparator.SortModes.REVISION      );
        else if (tableColumn == widgetChanges.getColumn(1)) data.logDataComparator.setSortMode(LogDataComparator.SortModes.DATE          );
        else if (tableColumn == widgetChanges.getColumn(2)) data.logDataComparator.setSortMode(LogDataComparator.SortModes.AUTHOR        );
        else if (tableColumn == widgetChanges.getColumn(3)) data.logDataComparator.setSortMode(LogDataComparator.SortModes.COMMIT_MESSAGE);
        Widgets.sortTableColumn(widgetChanges,tableColumn,data.logDataComparator);
      }
    };
    tableColumn = Widgets.addTableColumn(widgetChanges,0,"Revision",SWT.RIGHT);
    tableColumn.addSelectionListener(selectionListener);
    tableColumn = Widgets.addTableColumn(widgetChanges,1,"Date",SWT.LEFT);
    tableColumn.addSelectionListener(selectionListener);
    tableColumn = Widgets.addTableColumn(widgetChanges,2,"Autor",SWT.LEFT);
    tableColumn.addSelectionListener(selectionListener);
    tableColumn = Widgets.addTableColumn(widgetChanges,3,"Message",SWT.LEFT);
    tableColumn.addSelectionListener(selectionListener);
    Widgets.sortTableColumn(widgetChanges,0,data.logDataComparator);
    Widgets.setTableColumnWidth(widgetChanges,Settings.geometryChangesColumn.width);
//    widgetChanges.setToolTipText("List of changes.");

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryChanges       = dialog.getSize();
          Settings.geometryChangesColumn = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetChanges));

          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    widgetChanges.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
/*
        if (data.drawInfos != null)
        {
          for (DrawInfo drawInfo : data.drawInfos)
          {
            if (drawInfo.container.contains(mouseEvent.x,mouseEvent.y))
            {
              CommandRevisionInfo rommandRevisionInfo = new CommandRevisionInfo(dialog,repositoryTab,fileData,drawInfo.revisionData);
              rommandRevisionInfo.run();
              break;
            }
          }
        }
*/
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetChanges.addMouseTrackListener(new MouseTrackListener()
    {
      public void mouseEnter(MouseEvent mouseEvent)
      {
      }

      public void mouseExit(MouseEvent mouseEvent)
      {
      }

      public void mouseHover(MouseEvent mouseEvent)
      {
        Table     table     = (Table)mouseEvent.widget;
        TableItem tableItem = table.getItem(new Point(mouseEvent.x,mouseEvent.y));

        if (widgetChangesToolTip != null)
        {
          widgetChangesToolTip.dispose();
          widgetChangesToolTip = null;
        }

        if (tableItem != null)
        {
          LogData logData = (LogData)tableItem.getData();
          Label   label;
          Text    text;

          final Color COLOR_FORGROUND  = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
          final Color COLOR_BACKGROUND = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);

          widgetChangesToolTip = new Shell(shell,SWT.ON_TOP|SWT.NO_FOCUS|SWT.TOOL);
          widgetChangesToolTip.setBackground(COLOR_BACKGROUND);
          widgetChangesToolTip.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,1.0},new double[]{0.0,1.0},2));
          Widgets.layout(widgetChangesToolTip,0,0,TableLayoutData.NSWE);
          widgetChangesToolTip.addMouseTrackListener(new MouseTrackListener()
          {
            public void mouseEnter(MouseEvent mouseEvent)
            {
            }

            public void mouseExit(MouseEvent mouseEvent)
            {
              if (!isInsideChangesTooltip(mouseEvent.x,mouseEvent.y))
              {
                widgetChangesToolTip.dispose();
                widgetChangesToolTip = null;
              }
            }

            public void mouseHover(MouseEvent mouseEvent)
            {
            }
          });
          widgetChangesToolTipMousePosition.x = mouseEvent.x;
          widgetChangesToolTipMousePosition.y = mouseEvent.y;

          label = Widgets.newLabel(widgetChangesToolTip,"Revision:");
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,0,0,TableLayoutData.W);

          label = Widgets.newLabel(widgetChangesToolTip,logData.revision);
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,0,1,TableLayoutData.WE);

          label = Widgets.newLabel(widgetChangesToolTip,"Date:");
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,1,0,TableLayoutData.W);

          label = Widgets.newLabel(widgetChangesToolTip,Onzen.DATETIME_FORMAT.format(logData.date));
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,1,1,TableLayoutData.WE);

          label = Widgets.newLabel(widgetChangesToolTip,"Author:");
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,2,0,TableLayoutData.W);

          label = Widgets.newLabel(widgetChangesToolTip,logData.author);
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,2,1,TableLayoutData.WE);

          label = Widgets.newLabel(widgetChangesToolTip,"Commit message:");
          label.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(label,3,0,TableLayoutData.NW);

          text = Widgets.newText(widgetChangesToolTip,SWT.LEFT|SWT.V_SCROLL|SWT.H_SCROLL|SWT.MULTI|SWT.WRAP);
          text.setText(StringUtils.join(logData.commitMessage,text.DELIMITER));
          text.setBackground(Onzen.COLOR_WHITE);
          Widgets.layout(text,3,1,TableLayoutData.WE,0,0,0,0,300,100);

          Point size = widgetChangesToolTip.computeSize(SWT.DEFAULT,SWT.DEFAULT);
          Rectangle bounds = tableItem.getBounds(0);
          Point point = table.toDisplay(mouseEvent.x-16,bounds.y);
          widgetChangesToolTip.setBounds(point.x,point.y,size.x,size.y);
          widgetChangesToolTip.setVisible(true);
        }
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryChanges);

    // show
    show(changesType);
  }

  /** run dialog
   */
  public void run()
  {
    widgetClose.setFocus();
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandChanges {}";
  }

  //-----------------------------------------------------------------------

  /** check if mouse position is inside changes tooltip
   * @param x,y mouse position
   * @return true iff inside changes tooltip
   */
  private boolean isInsideChangesTooltip(int x, int y)
  {
    Rectangle bounds = widgetChangesToolTip.getClientArea();
    Point p = display.map(widgetChanges,widgetChangesToolTip,widgetChangesToolTipMousePosition);
    double d2 =  Math.pow(x-p.x,2)
                +Math.pow(y-p.y,2);
//Dprintf.dprintf("x=%d y=%d p=%s d2=%f bounds=%s",x,y,p,d2,bounds);
    return (d2 < 100) || bounds.contains(x,y);
  }

  /** show changes: set canvas size and draw changes list
   * @param changesType
   */
  private void show(ChangesTypes changesType)
  {
    Background.run(new BackgroundRunnable(changesType)
    {
      public void run(ChangesTypes changesType)
      {
        // get revision tree
        switch (changesType)
        {
          case INCOMING: repositoryTab.setStatusText("Get incoming changes..."); break;
          case OUTGOING: repositoryTab.setStatusText("Get outgoing changes..."); break;
        }
        try
        {
          switch (changesType)
          {
            case INCOMING: data.changes = repositoryTab.repository.getIncomingChanges(); break;
            case OUTGOING: data.changes = repositoryTab.repository.getOutgoingChanges(); break;
          }
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting changes fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
        }

        // show
        if (data.changes != null)
        {
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                for (LogData logData : data.changes)
                {
                  Widgets.insertTableEntry(widgetChanges,
                                           data.logDataComparator,
                                           logData,
                                           logData.revision,
                                           Onzen.DATETIME_FORMAT.format(logData.date),
                                           logData.author,
                                           logData.commitMessage[0]
                                          );
                }
              }
            });
          }
        }
      }
    });
  }
}

/* end of file */
