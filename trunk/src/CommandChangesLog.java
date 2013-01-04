/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command to view changes log
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** view changes log command
 */
class CommandChangesLog
{
  /** changes log comparator
   */
  class ChangeLogDataComparator extends LogDataComparator
  {
    /** create changes log comparator
     * @param table table
     * @param sortColumn column to sort
     */
    ChangeLogDataComparator(Table table, TableColumn sortColumn)
    {
      if      (table.getColumn(0) == sortColumn) setSortMode(LogDataComparator.SortModes.REVISION      );
      else if (table.getColumn(1) == sortColumn) setSortMode(LogDataComparator.SortModes.AUTHOR        );
      else if (table.getColumn(2) == sortColumn) setSortMode(LogDataComparator.SortModes.DATE          );
      else if (table.getColumn(3) == sortColumn) setSortMode(LogDataComparator.SortModes.COMMIT_MESSAGE);
      else                                       setSortMode(LogDataComparator.SortModes.REVISION      );
    }
  }

  /** dialog data
   */
  class Data
  {
    LogData[] logData;
    LogData   selectedLogData0,selectedLogData1;
    FileData  fileData;

    Data()
    {
      this.logData          = null;
      this.selectedLogData0 = null;
      this.selectedLogData1 = null;
      this.fileData         = null;
    }
  };

  // --------------------------- constants --------------------------------
//  private final Color COLOR_CURRENT   = Onzen.COLOR_YELLOW;
  private final Color COLOR_SELECTED0;
  private final Color COLOR_SELECTED1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final Clipboard     clipboard;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text          widgetSelectedLog0;
  private final Text          widgetSelectedLog1;
  private final Table         widgetChangesLog;
  private final TableColumn   widgetChangesLogColumn;
  private final List          widgetFiles;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Button        widgetDiff;
  private final Button        widgetPatch;
  private final Text          widgetSelectedLog;
  private final Button        widgetView;
  private final Button        widgetSave;
//  private final Button        widgetRevert;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision view
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file data
   * @param revision revision to show or null
   */
  CommandChangesLog(final Shell shell, final RepositoryTab repositoryTab)
  {
    Composite   composite,subComposite;
    Label       label;
    TableColumn tableColumn;
    Menu        menu;
    MenuItem    menuItem;
    Button      button;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // init colors
    COLOR_SELECTED0 = new Color(display,Settings.colorSelect0.background);
    COLOR_SELECTED1 = new Color(display,Settings.colorSelect1.background);

    // create dialog
    dialog = Dialogs.open(shell,"Changes log",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.7,0.0,0.3},1.0));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      // changes log
      widgetChangesLog = Widgets.newTable(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      widgetChangesLog.setBackground(Onzen.COLOR_WHITE);
      Widgets.layout(widgetChangesLog,0,0,TableLayoutData.NSWE,0,0,4);
      SelectionListener selectionListener = new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          TableColumn             tableColumn             = (TableColumn)selectionEvent.widget;
          ChangeLogDataComparator changeLogDataComparator = new ChangeLogDataComparator(widgetChangesLog,tableColumn);

          synchronized(widgetChangesLog)
          {
            Widgets.sortTableColumn(widgetChangesLog,tableColumn,changeLogDataComparator);
          }
        }
      };
      tableColumn = Widgets.addTableColumn(widgetChangesLog,0,"Revision",SWT.RIGHT);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetChangesLog,1,"Author",  SWT.LEFT );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetChangesLog,2,"Date",    SWT.LEFT );
      tableColumn.addSelectionListener(selectionListener);
      widgetChangesLogColumn = Widgets.addTableColumn(widgetChangesLog,3,"Message", SWT.LEFT);
      widgetChangesLogColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetChangesLog,Settings.geometryChangesLogColumns.width);
      menu = Widgets.newPopupMenu(dialog);
      {
/*
        menuItemGotoRevision = Widgets.addMenuItem(menu,"Goto revision");
        menuItemGotoRevision.setEnabled(false);
        menuItemGotoRevision.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            show(data.selectedLog);
          }
        });
*/
      }
      widgetChangesLog.setMenu(menu);
      widgetChangesLog.setToolTipText("Changes log. Double-click to view revision info.");

      // find
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setMessage("Enter text to find");
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetModifyListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.logData != null) && (data.logData != null));
          }
        });
        widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetModifyListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.logData != null) && (data.logData != null));
          }
        });
        widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");
      }

      // files
      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,2,0,TableLayoutData.NSWE);
      menu = Widgets.newPopupMenu(dialog);
      {
        menuItem = Widgets.addMenuItem(menu,"Revision info\u2026",Settings.keyRevisionInfo);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            CommandRevisionInfo commandRevisionInfo = new CommandRevisionInfo(shell,repositoryTab,data.fileData,data.selectedLogData1.revision);
            commandRevisionInfo.run();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Diff\u2026",Settings.keyDiff);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetDiff);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Patch\u2026",Settings.keyDiff);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetPatch);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Save\u2026",Settings.keyDiff);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetSave);
          }
        });
      }
      widgetFiles.setMenu(menu);
      widgetFiles.setToolTipText("Changed files.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"For");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetSelectedLog0 = Widgets.newStringView(composite);
      widgetSelectedLog0.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedLog0,0,1,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedLog0,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedLog0.isDisposed()) widgetSelectedLog0.setText((data.selectedLogData0 != null) ? data.selectedLogData0.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"->");
      Widgets.layout(label,0,2,TableLayoutData.W);

      widgetSelectedLog1 = Widgets.newStringView(composite);
      widgetSelectedLog1.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedLog1,0,3,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedLog1,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedLog1.isDisposed()) widgetSelectedLog1.setText((data.selectedLogData1 != null) ? data.selectedLogData1.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,4,TableLayoutData.W);

      widgetDiff = Widgets.newButton(composite,"Diff",Settings.keyDiff);
      widgetDiff.setEnabled(false);
      Widgets.layout(widgetDiff,0,5,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetDiff,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedLogData0 != null) && (data.selectedLogData1 != null) && (data.fileData != null));
        }
      });
      widgetDiff.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if ((data.selectedLogData1 != null) && (data.fileData != null))
          {
            CommandDiff commandDiff = new CommandDiff(dialog,
                                                      repositoryTab,
                                                      data.fileData,
                                                      (data.selectedLogData0 != null) ? data.selectedLogData0.revision : repositoryTab.repository.getFirstRevision(),
                                                      (data.selectedLogData1 != null) ? data.selectedLogData1.revision : null
                                                     );
            commandDiff.run();
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      widgetPatch = Widgets.newButton(composite,"Patch");
      widgetPatch.setEnabled(false);
      Widgets.layout(widgetPatch,0,6,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetPatch,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedLogData0 != null) && (data.selectedLogData1 != null) && (data.fileData != null));
        }
      });
      widgetPatch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if ((data.selectedLogData1 != null) && (data.fileData != null))
          {
            CommandCreatePatch commandCreatePatch;
            if (data.selectedLogData0 != null)
            {
              commandCreatePatch = new CommandCreatePatch(dialog,
                                                          repositoryTab,
                                                          data.fileData.toSet(),
                                                          data.selectedLogData0.revision,
                                                          data.selectedLogData1.revision,
                                                          false
                                                         );
            }
            else
            {
              commandCreatePatch = new CommandCreatePatch(dialog,
                                                          repositoryTab,
                                                          data.fileData.toSet(),
                                                          data.selectedLogData1.revision,
                                                          false
                                                         );
            }
            commandCreatePatch.run();
          }
        }
      });

      label = Widgets.newLabel(composite,"For");
      Widgets.layout(label,0,7,TableLayoutData.W);

      widgetSelectedLog = Widgets.newStringView(composite);
      widgetSelectedLog.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedLog,0,8,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedLog,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedLog.isDisposed()) widgetSelectedLog.setText((data.selectedLogData1 != null) ? data.selectedLogData1.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,9,TableLayoutData.W);

      widgetView = Widgets.newButton(composite,"View");
      widgetView.setEnabled(false);
      Widgets.layout(widgetView,0,10,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetView,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedLogData1 != null) && (data.fileData != null));
        }
      });
      widgetView.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if ((data.selectedLogData1 != null) && (data.fileData != null))
          {
            CommandView commandView = new CommandView(dialog,repositoryTab,data.fileData,data.selectedLogData1.revision);
            commandView.run();
          }
        }
      });

      widgetSave = Widgets.newButton(composite,"Save...");
      widgetSave.setEnabled(false);
      Widgets.layout(widgetSave,0,11,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSave,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedLogData1 != null) && (data.fileData != null));
        }
      });
      widgetSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          if ((data.selectedLogData1 != null) && (data.fileData != null))
          {
            try
            {
              // get file
              byte[] fileDataBytes =  repositoryTab.repository.getFileBytes(data.fileData,data.selectedLogData1.revision);

              // save to file
              String fileName = Dialogs.fileSave(dialog,"Save file");
              if (fileName != null)
              {
                try
                {
                  DataOutputStream output = new DataOutputStream(new FileOutputStream(fileName));
                  output.write(fileDataBytes);
                  output.close();
                }
                catch (IOException exception)
                {
                  Dialogs.error(dialog,"Cannot save file '%s' (error: %s)",data.fileData.getFileName(),exception.getMessage());
                }
              }
            }
            catch (RepositoryException exception)
            {
              Dialogs.error(dialog,"Cannot save file '%s' (error: %s)",data.fileData.getFileName(),exception.getMessage());
              Onzen.printStacktrace(exception);
            }
          }
        }
      });

/*
      widgetRevert = Widgets.newButton(composite,"Revert...",Settings.keyRevert);
      widgetRevert.setEnabled(false);
      Widgets.layout(widgetRevert,0,12,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetRevert,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedLogData1 != null));
        }
      });
      widgetRevert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if ((data.selectedLogData1 != null) && (data.fileData != null))
          {
            if (Dialogs.confirm(dialog,String.format("Revert file '%s' to revision %s?",fileData.getFileName(),data.selectedLogData1.revision)))
            {
              try
              {
                // revert file
                repositoryTab.repository.revert(fileData,data.selectedLogData1.revision);

                // update state
                repositoryTab.repository.updateStates(fileData);
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    if (!repositoryTab.widgetComposite.isDisposed()) repositoryTab.updateTreeItem(fileData);
                  }
                });
              }
              catch (RepositoryException exception)
              {
                Dialogs.error(dialog,"Cannot revert file '%s' (error: %s)",fileData.getFileName(),exception.getMessage());
                Onzen.printStacktrace(exception);
              }
            }
          }
        }
      });
*/

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,13,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryChangesLog        = dialog.getSize();
          Settings.geometryChangesLogColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetChangesLog));

          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    widgetChangesLog.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
        Table       widget     = (Table)mouseEvent.widget;
        TableItem[] tableItems = widgetChangesLog.getSelection();

        if (tableItems.length > 0)
        {
          // selected revision
          selectRevision((LogData)tableItems[0].getData());

          // notify modification
          Widgets.modified(data);
        }
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetChangesLog.addKeyListener(new KeyListener()
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
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
    widgetFiles.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        List     widget    = (List)selectionEvent.widget;
        String[] fileNames = widget.getSelection();

        if ((data.selectedLogData1 != null) && (data.fileData != null))
        {
          CommandDiff commandDiff = new CommandDiff(dialog,
                                                    repositoryTab,
                                                    data.fileData,
                                                    (data.selectedLogData0 != null) ? data.selectedLogData0.revision : repositoryTab.repository.getFirstRevision(),
                                                    (data.selectedLogData1 != null) ? data.selectedLogData1.revision : null
                                                   );
          commandDiff.run();
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        List     widget    = (List)selectionEvent.widget;
        String[] fileNames = widget.getSelection();

        if (fileNames.length > 0)
        {
          data.fileData = new FileData(fileNames[0]);

          Widgets.modified(data);
        }
      }
    });
    widgetFiles.addKeyListener(new KeyListener()
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
        else if (Widgets.isAccelerator(keyEvent,Settings.keyDiff))
        {
          Widgets.invoke(widgetDiff);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyCreatePatch))
        {
          Widgets.invoke(widgetPatch);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        LogData logData = findNext(widgetFind);
        if (logData != null)
        {
          // select revision and show
          selectRevision(logData);
          scrollTo(logData);

          // notify modification
          Widgets.modified(data);
        }
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
        LogData logData = findPrev(widgetFind);
        if (logData != null)
        {
          // select revision and show
          selectRevision(logData);
          scrollTo(logData);

          // notify modification
          Widgets.modified(data);
        }
      }
    });
    widgetFindNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        LogData logData = findNext(widgetFind);
        if (logData != null)
        {
          // select revision and show
          selectRevision(logData);
          scrollTo(logData);

          // notify modification
          Widgets.modified(data);
        }
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
          TableItem[] tableItems = widgetChangesLog.getSelection();
          Widgets.setClipboard(clipboard,tableItems,4);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    };
    widgetChangesLog.addKeyListener(keyListener);
    widgetFind.addKeyListener(keyListener);
    widgetFindPrev.addKeyListener(keyListener);
    widgetFindNext.addKeyListener(keyListener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryChangesLog,Settings.setWindowLocation);

    // show
    show();
  }

  /** set scroll value
   * @param scrollX,scrollY scroll values
   */
  public void setScroll(int scrollX, int scrollY)
  {
    widgetChangesLog.getHorizontalBar().setSelection(scrollX);
    widgetChangesLog.getVerticalBar().setSelection(scrollY);
    widgetChangesLog.redraw();
  }

  /** run dialog
   */
  public void run()
  {
    widgetFind.setFocus();
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandChangesLog {}";
  }

  //-----------------------------------------------------------------------

  /** scroll to revision
   * @param logData revision to show
   */
  private void scrollTo(final LogData logData)
  {
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {

        }
      });
    }
  }

  /** show revisions
   * @param revision revision to show
   */
  private void show(String revision)
  {
    // clear
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          data.logData = null;
          Widgets.modified(data);
         }
      });
    }

    // start show changes log
    Background.run(new BackgroundRunnable(revision)
    {
      public void run(final String revision)
      {
        // get log
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get changes log...");
        try
        {
          data.logData = repositoryTab.repository.getLog();
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Get changes log fail: %s",exceptionMessage);
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

        // show
        if (!widgetChangesLog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              widgetChangesLog.removeAll();
              int maxWidth = 0;
              int lineNb   = 1;
              for (LogData logData : data.logData)
              {
                Widgets.addTableEntry(widgetChangesLog,
                                      logData,
                                      logData.revision+StringUtils.repeat('\n',logData.commitMessage.length-1),
                                      logData.author+StringUtils.repeat('\n',logData.commitMessage.length-1),
                                      Onzen.DATE_FORMAT.format(logData.date)+StringUtils.repeat('\n',logData.commitMessage.length-1),
                                      StringUtils.join(logData.commitMessage,'\n')
                                     );
                maxWidth = Math.max(maxWidth,Widgets.getTextWidth(widgetChangesLog,logData.commitMessage));
                lineNb++;
              }
              widgetChangesLogColumn.setWidth(maxWidth);
            }
          });
        }
      }
    });
  }

  /** show last revision
   */
  private void show()
  {
    show(repositoryTab.repository.getLastRevision());
  }

  /** select revision
   * @param logData0,logData1 logData
   */
  private void selectRevision(LogData logData0, LogData logData1)
  {
    if (data.selectedLogData0 != null) Widgets.setTableEntryColor(widgetChangesLog,data.selectedLogData0,null);
    if (data.selectedLogData0 != null) Widgets.setTableEntryColor(widgetChangesLog,data.selectedLogData1,null);
    data.selectedLogData0 = logData0;
    data.selectedLogData1 = logData1;
    if (data.selectedLogData0 != null) Widgets.setTableEntryColor(widgetChangesLog,data.selectedLogData0,COLOR_SELECTED0);
    if (data.selectedLogData1 != null) Widgets.setTableEntryColor(widgetChangesLog,data.selectedLogData1,COLOR_SELECTED1);
    data.fileData = null;
Dprintf.dprintf("data.selectedLogData0=%s %s",data.selectedLogData0,COLOR_SELECTED0);
Dprintf.dprintf("data.selectedLogData1=%s %s",data.selectedLogData1,COLOR_SELECTED1);

    if (data.selectedLogData1 != null)
    {
      if (!dialog.isDisposed())
      {
        display.syncExec(new Runnable()
        {
          public void run()
          {
            // select log entry, show it
            TableItem[] tableItems = widgetChangesLog.getItems();
            for (TableItem tableItem : widgetChangesLog.getItems())
            {
              if ((LogData)tableItem.getData() == data.selectedLogData1)
              {
                widgetChangesLog.setSelection(tableItem);
                widgetChangesLog.showSelection();
                break;
              }
            }

            // set changed files
            widgetFiles.removeAll();
            for (String fileName : data.selectedLogData1.fileNames)
            {
              widgetFiles.add(fileName);
            }

            if (data.selectedLogData1.fileNames.length > 0)
            {
              // select first file
              widgetFiles.select(0);
              data.fileData = new FileData(data.selectedLogData1.fileNames[0]);
            }
          }
        });
      }
    }
  }

  /** select revision
   * @param logData log data to select
   */
  private void selectRevision(LogData logData)
  {
    selectRevision(data.selectedLogData1,logData);
  }

  /** select revision
   * @param index index
   */
  private void selectRevision(final int index)
  {
Dprintf.dprintf("");
/*
    data.selectedLog = data.revisionNames[index];
    data.prevRevision     = getPrevRevision(data.revisionNames[index]);
    data.nextRevision     = getNextRevision(data.revisionNames[index]);

    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          widgetRevision.select(index);

          menuItemGotoRevision.setText("Goto revision "+((data.selectedLog != null)?data.selectedLog:""));
          menuItemGotoRevision.setEnabled(data.selectedLog != null);

          menuItemPrevRevision.setText("Goto previous revision "+((data.prevRevision != null)?data.prevRevision:""));
          menuItemPrevRevision.setEnabled(data.prevRevision != null);

          menuItemNextRevision.setText("Goto next revision "+((data.nextRevision != null)?data.nextRevision:""));
          menuItemNextRevision.setEnabled(data.nextRevision != null);

          menuItemShowRevision.setText("Show revision "+((data.selectedLog != null)?data.selectedLog:""));
          menuItemShowRevision.setEnabled(data.selectedLog != null);
        }
      });
    };
*/
  }

  /** select revision
   * @param revision revision to select
   */
  private void selectRevision(String revision)
  {
    for (LogData logData : data.logData)
    {
      if (logData.revision.equals(revision))
      {
Dprintf.dprintf("");
        selectRevision(logData);
        break;
      }
    }
  }

  /** search previous revision
   * @param widgetFind search text widget
   * @return revision data or null
   */
  private LogData findPrev(Text widgetFind)
  {
    LogData logData = null;

    if (!widgetFind.isDisposed() && (data.logData != null))
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get selected/first revision
        LogData selectedLogData = (data.selectedLogData1 != null) ? data.selectedLogData1 : data.logData[0];

        // find current revision
        int z = 0;
        while ((z < data.logData.length) && (data.logData[z] != selectedLogData))
        {
          z++;
        }

        // find previous matching revision
        do
        {
Dprintf.dprintf("data.logData[z]=%s",data.logData[z]);
          z--;
        }
        while (   (z >= 0)
               && !data.logData[z].match(findText)
              );

        if (z >= 0)
        {
Dprintf.dprintf("data.logData[z]=%s",data.logData[z]);
          // select previsous revision
          logData = data.logData[z];
        }
        else
        {
          // not found
          Widgets.flash(widgetFind);
        }
      }
    }
Dprintf.dprintf("prev %s",logData);

    return logData;
  }

  /** search next revision
   * @param widgetFind search text widget
   * @return revision data or null
   */
  private LogData findNext(Text widgetFind)
  {
    LogData logData = null;

    if (!widgetFind.isDisposed() && (data.selectedLogData1 != null))
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // find current revision index or -1 if non selected
        int z;
        if (data.selectedLogData1 != null)
        {
          z = 0;
          while ((z < data.logData.length) && (data.logData[z] != data.selectedLogData1))
          {
            z++;
          }
        }
        else
        {
          z = -1;
        }

        // find next matching revision
        do
        {
          z++;
        }
        while (   (z < data.logData.length)
               && !data.logData[z].match(findText)
              );

        if (z < data.logData.length)
        {
          // select next revision
          logData = data.logData[z];
        }
        else
        {
          // not found
          Widgets.flash(widgetFind);
        }
      }
    }
Dprintf.dprintf("next %s",logData);

    return logData;
  }
}

/* end of file */
