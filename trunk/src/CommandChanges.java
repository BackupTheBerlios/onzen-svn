/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
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
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
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

  /** dialog data
   */
  class Data
  {
    LogData[]         changes;
    Rectangle         view;
    Point             size;
    RevisionData      selectedRevisionData;
    LogDataComparator logDataComparator;

    Data()
    {
      this.changes           = null;
      this.view              = new Rectangle(0,0,0,0);
      this.size              = new Point(0,0);
      this.logDataComparator = new LogDataComparator();
    }
  };

  // --------------------------- constants --------------------------------
  private final Color COLOR_BACKGROUND        = Onzen.COLOR_GRAY;
  private final Color COLOR_SEARCH_BACKGROUND = Onzen.COLOR_RED;
  private final Color COLOR_SEARCH_TEXT       = Onzen.COLOR_BLUE;

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
    Composite   composite;
    Label       label;
    Button      button;

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
    Widgets.setTableColumnWidth(widgetChanges,Settings.geometryChangesColumns.width);
    widgetChanges.setToolTipText("Incoming/outging changes. Double click to show changes for revision.");

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
          Settings.geometryChanges        = dialog.getSize();
          Settings.geometryChangesColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetChanges));

          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    widgetChanges.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        Table       table      = (Table)mouseEvent.widget;
        TableItem[] tableItems = table.getSelection();

        if (tableItems.length > 0)
        {
          LogData logData = (LogData)tableItems[0].getData();

          showLines(logData.revision);
        }
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

        closeChangesTooltip();

        if (tableItem != null)
        {
          LogData logData = (LogData)tableItem.getData();

          Rectangle bounds = tableItem.getBounds(0);
          Point point = table.toDisplay(mouseEvent.x-16,bounds.y);

          openChangesTooltip(shell,logData,point.x,point.y);

          widgetChangesToolTipMousePosition.x = mouseEvent.x;
          widgetChangesToolTipMousePosition.y = mouseEvent.y;
        }
      }
    });
    widgetChanges.addFocusListener(new FocusListener()
    {
      public void focusGained(FocusEvent focusEvent)
      {
      }

      public void focusLost(FocusEvent focusEvent)
      {
        closeChangesTooltip();
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryChanges,Settings.setWindowLocation);

    // showChanges
    showChanges(changesType);
  }

  /** run dialog
   */
  public void run()
  {
    widgetClose.setFocus();
    Dialogs.run(dialog,new DialogRunnable()
    {
      public void done(Object result)
      {
        closeChangesTooltip();
      }
    });
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
    if (!widgetChanges.isDisposed())
    {
      Rectangle bounds = widgetChangesToolTip.getClientArea();
      Point p = display.map(widgetChanges,widgetChangesToolTip,widgetChangesToolTipMousePosition);
      double d2 =  Math.pow(x-p.x,2)
                  +Math.pow(y-p.y,2);
      return (d2 < 100) || bounds.contains(x,y);
    }
    else
    {
      return true;
    }
  }

  /** open changes tool tip
   * @param shell shell
   * @param logData log data to show
   * @param x,y positin of tool tip
   */
  private void openChangesTooltip(Shell shell, LogData logData, int x, int y)
  {
    final Color COLOR_FORGROUND  = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
    final Color COLOR_BACKGROUND = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);

    Label label;
    Text  text;

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
          closeChangesTooltip();
        }
      }

      public void mouseHover(MouseEvent mouseEvent)
      {
      }
    });

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
    widgetChangesToolTip.setBounds(x,y,size.x,size.y);
    widgetChangesToolTip.setVisible(true);
  }

  /** close changes tooltip
   */
  private void closeChangesTooltip()
  {
    if (widgetChangesToolTip != null)
    {
      widgetChangesToolTip.dispose();
      widgetChangesToolTip = null;
    }
  }

  /** show changes: set canvas size and draw changes list
   * @param changesType
   */
  private void showChanges(ChangesTypes changesType)
  {
    Background.run(new BackgroundRunnable(changesType)
    {
      public void run(final ChangesTypes changesType)
      {
        // get revision tree
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
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
          final String   exceptionMessage = exception.getMessage();
          final String[] extendedMessage  = exception.getExtendedMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,extendedMessage,"Getting changes fail: %s",exceptionMessage);
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
        if (data.changes != null)
        {
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (data.changes.length > 0)
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
                else
                {
                  switch (changesType)
                  {
                    case INCOMING: Dialogs.info(dialog,"No incoming changes found."); break;
                    case OUTGOING: Dialogs.info(dialog,"No outgoing changes found."); break;
                  }

                  Widgets.invoke(widgetClose);
                }
              }
            });
          }
        }
      }
    });
  }

  /** search previous text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findPrev(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = -1;
        if (cursorIndex > 0)
        {
          String text = widgetText.getText(0,cursorIndex-1);
          offset = text.toLowerCase().lastIndexOf(findText);
        }
        if (offset >= 0)
        {
          int index = offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }
  }

  /** search next text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findNext(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = -1;
        if (cursorIndex >= 0)
        {
          String text = widgetText.getText();
          offset = (cursorIndex+1 < text.length()) ? text.substring(cursorIndex+1).toLowerCase().indexOf(findText) : -1;
        }
        if (offset >= 0)
        {
          int index = cursorIndex+1+offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }
  }

  /** show changed lines
   * @param revision revision to show changes for
   */
  private void showLines(String revision)
  {
    /** dialog data
     */
    class Data
    {
      String[] lines;

      Data()
      {
        this.lines = null;
      }
    };

    final Data       data = new Data();

    final StyledText widgetText;
    final Text       widgetFind;
    final Button     widgetFindPrev;
    final Button     widgetFindNext;

    Composite    composite,subComposite;
    Label        label;
    Button       button;

    // get changed lines
    repositoryTab.setStatusText("Get changes for revison "+revision+"...");
    try
    {
      data.lines = repositoryTab.repository.getChanges(revision);
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
    if (data.lines != null)
    {
      if (!dialog.isDisposed())
      {
        final Shell subDialog = Dialogs.open(dialog,"Changes revision "+revision,new double[]{1.0,0.0},1.0);

        composite = Widgets.newComposite(subDialog);
        composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,4));
        Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
        {
          widgetText = Widgets.newTextView(composite);
          Widgets.layout(widgetText,0,0,TableLayoutData.NSWE,0,0,4);

          subComposite = Widgets.newComposite(composite);
          subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
          Widgets.layout(subComposite,2,0,TableLayoutData.WE);
          {
            label = Widgets.newLabel(subComposite,"Find:",SWT.NONE,Settings.keyFind);
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
            widgetFind.setMessage("Enter text to find");
            Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

            widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
            Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
            widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

            widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
            Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
            widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");
          }
        }

        // buttons
        composite = Widgets.newComposite(subDialog);
        composite.setLayout(new TableLayout(0.0,new double[]{1.0}));
        Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
        {
          button = Widgets.newButton(composite,"Close");
          Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Settings.geometryChangesLines = subDialog.getSize();

              Dialogs.close(subDialog);
            }
          });
        }

        // listeners
        widgetText.addLineStyleListener(new LineStyleListener()
        {
          public void lineGetStyle(LineStyleEvent lineStyleEvent)
          {
             String findText = widgetFind.getText().toLowerCase();
             int    findTextLength = findText.length();
             if (findTextLength > 0)
             {
               ArrayList<StyleRange> styleRangeList = new ArrayList<StyleRange>();
               int                   index = 0;
               while ((index = lineStyleEvent.lineText.toLowerCase().indexOf(findText,index)) >= 0)
               {
                 styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_SEARCH_TEXT,COLOR_SEARCH_BACKGROUND));
                 index += findTextLength;
               }
               lineStyleEvent.styles = styleRangeList.toArray(new StyleRange[styleRangeList.size()]);
             }
             else
             {
               lineStyleEvent.styles = null;
             }
          }
        });
        widgetFind.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            findNext(widgetText,widgetFind);
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
        widgetFindPrev.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            findPrev(widgetText,widgetFind);
          }
        });
        widgetFindNext.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            findNext(widgetText,widgetFind);
          }
        });

        KeyListener keyListener = new KeyListener()
        {
          public void keyPressed(KeyEvent keyEvent)
          {
            if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
            {
              widgetFind.forceFocus();
            }
            else if (Widgets.isAccelerator(keyEvent,Settings.keyFindPrev))
            {
              Widgets.invoke(widgetFindPrev);
            }
            else if (Widgets.isAccelerator(keyEvent,Settings.keyFindNext))
            {
              Widgets.invoke(widgetFindNext);
            }
            else if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'c'))
            {
              String string = widgetText.getSelectionText();
              Widgets.setClipboard(clipboard,string);
            }
          }
          public void keyReleased(KeyEvent keyEvent)
          {
          }
        };
        widgetText.addKeyListener(keyListener);
        widgetFind.addKeyListener(keyListener);
        widgetFindPrev.addKeyListener(keyListener);
        widgetFindNext.addKeyListener(keyListener);

        // show dialog
        Dialogs.show(subDialog,Settings.geometryChangesLines,Settings.setWindowLocation);

        // add text
        StringBuilder buffer = new StringBuilder();
        for (String line : data.lines)
        {
          buffer.append(line); buffer.append('\n');
        }
        widgetText.setText(buffer.toString());

        Widgets.setFocus(widgetFind);
        Dialogs.run(subDialog);
      }
    }
  }
}

/* end of file */
