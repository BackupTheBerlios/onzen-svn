/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandChangedFiles.java,v $
* $Revision: 1.6 $
* $Author: torsten $
* Contents: command show changed files
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.io.IOException;

import java.util.EnumSet;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.SashForm;
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

/** view changed files command
 */
class CommandChangedFiles
{
  /** dialog data
   */
  class Data
  {
    HashSet<FileData>        fileDataSet;
    EnumSet<FileData.States> showStates;
    FileDataComparator       fileDataComparator;

    Data()
    {
      this.fileDataSet        = null;
      this.showStates         = EnumSet.copyOf(Settings.changedFilesShowStates);
      this.fileDataComparator = new FileDataComparator();
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  public static Color COLOR_MODIFIED;
  public static Color COLOR_MERGE;
  public static Color COLOR_CONFLICT;
  public static Color COLOR_REMOVED;
  public static Color COLOR_UPDATE;
  public static Color COLOR_CHECKOUT;
  public static Color COLOR_UNKNOWN;
  public static Color COLOR_WAITING;
  public static Color COLOR_ADDED;

  // user events
  private final int USER_EVENT_FILTER = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text          widgetFilter;
  private final Table         widgetFiles;
  private final Button        widgetButtonUpdate;
  private final Button        widgetButtonCommit;
  private final Button        widgetButtonCreatePatch;
  private final Button        widgetButtonAdd;
  private final Button        widgetButtonRemove;
  private final Button        widgetButtonRevert;
  private final Button        widgetButtonDiff;
  private final Button        widgetButtonRevisions;
  private final Button        widgetButtonSolve;
  private final Button        widgetButtonReread;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** changed files command
   * @param shell shell
   * @param repositoryTab repository tab
   */
  CommandChangedFiles(final Shell shell, final RepositoryTab repositoryTab)
  {
    Composite   composite,subComposite;
    Label       label;
    Button      button;
    TableColumn tableColumn;
    Menu        menu;
    MenuItem    menuItem;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get display
    display = shell.getDisplay();

    // init colors
    COLOR_MODIFIED = new Color(display,Settings.colorStatusModified.background);
    COLOR_MERGE    = new Color(display,Settings.colorStatusMerge.background   );
    COLOR_CONFLICT = new Color(display,Settings.colorStatusConflict.background);
    COLOR_REMOVED  = new Color(display,Settings.colorStatusRemoved.background );
    COLOR_UPDATE   = new Color(display,Settings.colorStatusUpdate.background  );
    COLOR_CHECKOUT = new Color(display,Settings.colorStatusCheckout.background);
    COLOR_UNKNOWN  = new Color(display,Settings.colorStatusUnknown.background );
    COLOR_ADDED    = new Color(display,Settings.colorStatusAdded.background   );
    COLOR_REMOVED  = new Color(display,Settings.colorStatusRemoved.background );

    // changed files dialog
    dialog = Dialogs.open(shell,"Changed files",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Filter:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFilter = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        Widgets.layout(widgetFilter,0,1,TableLayoutData.WE);
        widgetFilter.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
        widgetFilter.addModifyListener(new ModifyListener()
        {
          public void modifyText(ModifyEvent modifyEvent)
          {
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });
        widgetFilter.setToolTipText("Filter name list. Enter multiple words which entires must contain to become listed.");
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,0.0));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Show:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        button = Widgets.newCheckbox(subComposite,"OK");
        button.setSelection(data.showStates.contains(FileData.States.OK));
        Widgets.layout(button,0,2,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.OK);
            }
            else
            {
              data.showStates.remove(FileData.States.OK);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"unknown");
        button.setSelection(data.showStates.contains(FileData.States.UNKNOWN));
        Widgets.layout(button,0,3,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.UNKNOWN);
            }
            else
            {
              data.showStates.remove(FileData.States.UNKNOWN);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"modified");
        button.setSelection(data.showStates.contains(FileData.States.MODIFIED));
        Widgets.layout(button,0,4,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.MODIFIED);
            }
            else
            {
              data.showStates.remove(FileData.States.MODIFIED);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"checkout");
        button.setSelection(data.showStates.contains(FileData.States.CHECKOUT));
        Widgets.layout(button,0,5,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.CHECKOUT);
            }
            else
            {
              data.showStates.remove(FileData.States.CHECKOUT);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"update");
        button.setSelection(data.showStates.contains(FileData.States.UPDATE));
        Widgets.layout(button,0,6,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.UPDATE);
            }
            else
            {
              data.showStates.remove(FileData.States.UPDATE);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"merge");
        button.setSelection(data.showStates.contains(FileData.States.MERGE));
        Widgets.layout(button,0,7,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.MERGE);
            }
            else
            {
              data.showStates.remove(FileData.States.MERGE);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"conflict");
        button.setSelection(data.showStates.contains(FileData.States.CONFLICT));
        Widgets.layout(button,0,8,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.CONFLICT);
            }
            else
            {
              data.showStates.remove(FileData.States.CONFLICT);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"added");
        button.setSelection(data.showStates.contains(FileData.States.ADDED));
        Widgets.layout(button,0,9,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.ADDED);
            }
            else
            {
              data.showStates.remove(FileData.States.ADDED);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });

        button = Widgets.newCheckbox(subComposite,"removed");
        button.setSelection(data.showStates.contains(FileData.States.REMOVED));
        Widgets.layout(button,0,10,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(FileData.States.REMOVED);
            }
            else
            {
              data.showStates.remove(FileData.States.REMOVED);
            };
            Widgets.notify(dialog,USER_EVENT_FILTER);
          }
        });
      }

      widgetFiles = Widgets.newTable(composite);
      Widgets.layout(widgetFiles,2,0,TableLayoutData.NSWE);
      SelectionListener selectionListener = new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          TableColumn tableColumn = (TableColumn)selectionEvent.widget;

          if      (tableColumn == widgetFiles.getColumn(0)) data.fileDataComparator.setSortMode(FileDataComparator.SortModes.NAME );
          else if (tableColumn == widgetFiles.getColumn(1)) data.fileDataComparator.setSortMode(FileDataComparator.SortModes.STATE);
          Widgets.sortTableColumn(widgetFiles,tableColumn,data.fileDataComparator);
        }
      };
      tableColumn = Widgets.addTableColumn(widgetFiles,0,"Name",  SWT.LEFT);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,1,"Status",SWT.LEFT);
      tableColumn.addSelectionListener(selectionListener);
      Widgets.sortTableColumn(widgetFiles,0,data.fileDataComparator);
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryChangedFilesColumn.width);
      widgetFiles.setToolTipText("Changed files.");

      menu = Widgets.newPopupMenu(dialog);
      {
        menuItem = Widgets.addMenuItem(menu,"Open file",Settings.keyUpdate);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            FileData fileData = getSelectedFileData();

            if (fileData != null)
            {
              repositoryTab.openFile(fileData);
            }
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Open file with...",Settings.keyUpdate);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            FileData fileData = getSelectedFileData();

            if (fileData != null)
            {
              repositoryTab.openFileWith(fileData);
            }
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Update",Settings.keyUpdate);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonUpdate);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Commit",Settings.keyCommit);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonCommit);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Create patch",Settings.keyCreatePatch);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonCreatePatch);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Add",Settings.keyAdd);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonAdd);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Remove",Settings.keyRemove);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonRemove);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Revert",Settings.keyRevert);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonRevert);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Diff",Settings.keyDiff);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonDiff);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Revisions",Settings.keyRevisions);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonRevisions);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Solve",Settings.keySolve);
menuItem.setEnabled(false);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonSolve);
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Reread",Settings.keyReread);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonReread);
          }
        });
      }
      widgetFiles.setMenu(menu);

      widgetFiles.setToolTipText("Changed files. Right-click to open context menu.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonUpdate = Widgets.newButton(composite,"Update");
      widgetButtonUpdate.setEnabled(false);
      Widgets.layout(widgetButtonUpdate,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonUpdate,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonUpdate.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            try
            {
              repositoryTab.repository.update(fileDataSet);
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
            catch (RepositoryException exception)
            {
              Dialogs.error(shell,"Update fail: %s",exception.getMessage());
              return;
            }
          }
        }
      });

      widgetButtonCommit = Widgets.newButton(composite,"Commit");
      widgetButtonCommit.setEnabled(false);
      Widgets.layout(widgetButtonCommit,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonCommit,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            CommandCommit commandCommit = new CommandCommit(shell,repositoryTab,fileDataSet);
            if (commandCommit.execute())
            {
              repositoryTab.repository.updateStates(fileDataSet);
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
          }
        }
      });

      widgetButtonCreatePatch = Widgets.newButton(composite,"Patch");
      widgetButtonCreatePatch.setEnabled(false);
      Widgets.layout(widgetButtonCreatePatch,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonCreatePatch,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonCreatePatch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            CommandCreatePatch commandCreatePatch = new CommandCreatePatch(shell,repositoryTab,fileDataSet);
            commandCreatePatch.run();
          }
        }
      });

      widgetButtonAdd = Widgets.newButton(composite,"Add");
      widgetButtonAdd.setEnabled(false);
      Widgets.layout(widgetButtonAdd,0,3,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonAdd,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            CommandAdd commandAdd = new CommandAdd(shell,repositoryTab,fileDataSet);
            if (commandAdd.execute())
            {
              repositoryTab.repository.updateStates(fileDataSet);
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
          }
        }
      });

      widgetButtonRemove = Widgets.newButton(composite,"Remove");
      widgetButtonRemove.setEnabled(false);
      Widgets.layout(widgetButtonRemove,0,4,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonRemove,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonRemove.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            CommandRemove commandRemove = new CommandRemove(shell,repositoryTab,fileDataSet);
            if (commandRemove.execute())
            {
              repositoryTab.repository.updateStates(fileDataSet);
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
          }
        }
      });

      widgetButtonRevert = Widgets.newButton(composite,"Revert");
      widgetButtonRevert.setEnabled(false);
      Widgets.layout(widgetButtonRevert,0,5,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonRevert,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonRevert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          HashSet<FileData> fileDataSet = getSelectedFileDataSet();
          if (fileDataSet != null)
          {
            CommandRevert commandRevert = new CommandRevert(shell,repositoryTab,fileDataSet);
            if (commandRevert.execute())
            {
              repositoryTab.repository.updateStates(fileDataSet);
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
          }
        }
      });

      widgetButtonDiff = Widgets.newButton(composite,"Diff");
      widgetButtonDiff.setEnabled(false);
      Widgets.layout(widgetButtonDiff,0,6,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonDiff,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonDiff.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          FileData fileData = getSelectedFileData();
          if (fileData != null)
          {
            CommandDiff commandDiff = new CommandDiff(shell,repositoryTab,fileData);
            commandDiff.run();
          }
        }
      });

      widgetButtonRevisions = Widgets.newButton(composite,"Revisions");
      widgetButtonRevisions.setEnabled(false);
      Widgets.layout(widgetButtonRevisions,0,7,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonRevisions,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonRevisions.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          FileData fileData = getSelectedFileData();
          if (fileData != null)
          {
            CommandRevisions commandRevisions = new CommandRevisions(shell,repositoryTab,fileData);
            commandRevisions.run();
          }
        }
      });

      widgetButtonSolve = Widgets.newButton(composite,"Solve");
widgetButtonSolve.setEnabled(false);
      Widgets.layout(widgetButtonSolve,0,8,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetButtonSolve,data)
      {
        public void modified(Control control)
        {
//          control.setEnabled(widgetFiles.getSelectionCount() > 0);
        }
      });
      widgetButtonSolve.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
Dprintf.dprintf("");
        }
      });

      widgetButtonReread = Widgets.newButton(composite,"Reread");
      Widgets.layout(widgetButtonReread,0,9,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonReread.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          try
          {
            data.fileDataSet = repositoryTab.repository.getChangedFiles();
          }
          catch (RepositoryException exception)
          {
            Dialogs.error(dialog,"Cannot get list of changed files (error: %s)",exception.getMessage());
            return;
          }
          Widgets.modified(data);
          Widgets.notify(dialog,USER_EVENT_FILTER);
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,10,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Settings.geometryChangedFiles       = dialog.getSize();
          Settings.geometryChangedFilesColumn = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetFiles));
          Settings.changedFilesShowStates     = data.showStates;

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetFiles.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        TableItem tableItem = (TableItem)selectionEvent.item;
        FileData  fileData  = (FileData)tableItem.getData();

        repositoryTab.openFile(fileData);
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        TableItem tableItem = (TableItem)selectionEvent.item;

        Widgets.modified(data);
      }
    });
    widgetFiles.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
//Dprintf.dprintf("keyEvent=%s %x %x",keyEvent,keyEvent.stateMask & SWT.KEY_MASK,keyEvent.stateMask & SWT.MODIFIER_MASK);
        if      (Widgets.isAccelerator(keyEvent,Settings.keyUpdate     )) Widgets.invoke(widgetButtonUpdate     );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyCommit     )) Widgets.invoke(widgetButtonCommit     );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyCreatePatch)) Widgets.invoke(widgetButtonCreatePatch);
        else if (Widgets.isAccelerator(keyEvent,Settings.keyAdd        )) Widgets.invoke(widgetButtonAdd        );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyRemove     )) Widgets.invoke(widgetButtonRemove     );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyRevert     )) Widgets.invoke(widgetButtonRevert     );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyDiff       )) Widgets.invoke(widgetButtonDiff       );
        else if (Widgets.isAccelerator(keyEvent,Settings.keyRevisions  )) Widgets.invoke(widgetButtonRevisions  );
//        else if (Widgets.isAccelerator(keyEvent,Settings.keyCommit)) Wiggets.invoke(widgetButtonReread);
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    dialog.addListener(USER_EVENT_FILTER,new Listener()
    {
      public void handleEvent(Event event)
      {
        // get filter words
        String[] filterWords = widgetFilter.getText().toLowerCase().split("\\s");

        // update table
        widgetFiles.removeAll();
        if (data.fileDataSet != null)
        {
          for (FileData fileData : data.fileDataSet)
          {
            // check state filter
            if (data.showStates.contains(fileData.state))
            {
              String fileName = fileData.getFileName().toLowerCase();

              // match name
              boolean match = true;
              for (String filterWord : filterWords)
              {
                if (!fileName.contains(filterWord))
                {
                  match = false;
                  break;
                }
              }

              if (match)
              {
                TableItem tableItem = Widgets.insertTableEntry(widgetFiles,
                                                               data.fileDataComparator,
                                                               fileData,
                                                               fileData.name,
                                                               fileData.state.toString()
                                                              );
                tableItem.setBackground(getFileDataBackground(fileData));
              }
            }
          }
        }
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryChangedFiles);

    // start getting changed files
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
        // get changed files
        repositoryTab.setStatusText("Get changed files for '%s'...",repositoryTab.repository.title);
        try
        {
          data.fileDataSet = repositoryTab.repository.getChangedFiles();
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file revisions fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
        }

        // notity changed data
        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Widgets.notify(dialog,USER_EVENT_FILTER);
            }
          });
        }
      }
    });
  }

  /** run dialog
   */
  public boolean run()
  {
    if (!dialog.isDisposed())
    {
      widgetFilter.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
        return true;
      }
      else
      {
        return false;
      }
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
    return "CommandChangedFiles {}";
  }

  //-----------------------------------------------------------------------

  /** get file data background color
   * @param fileData file data
   * @return color
   */
  private Color getFileDataBackground(FileData fileData)
  {
    Color color = null;
    switch (fileData.type)
    {
      case DIRECTORY:
        color = COLOR_UNKNOWN;
        break;
      case FILE:
      case LINK:
        switch (fileData.state)
        {
          case OK      : color = null;             break;
          case MODIFIED: color = COLOR_MODIFIED;   break;
          case MERGE   : color = COLOR_MERGE;      break;
          case CONFLICT: color = COLOR_CONFLICT;   break;
          case REMOVED : color = COLOR_REMOVED;    break;
          case UPDATE  : color = COLOR_UPDATE;     break;
          case CHECKOUT: color = COLOR_CHECKOUT;   break;
          case WAITING : color = COLOR_WAITING;    break;
          case ADDED   : color = COLOR_ADDED;      break;
          default      : color = COLOR_UNKNOWN;    break;
        }
        break;
      default:
        color = COLOR_UNKNOWN;
        break;
    }

    return color;
  }

  /** get selected file data set
   * @return file data set
   */
  private HashSet<FileData> getSelectedFileDataSet()
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    for (TableItem tableItem : widgetFiles.getSelection())
    {
      fileDataSet.add((FileData)tableItem.getData());
    }

    return (fileDataSet.size() > 0) ? fileDataSet : null;
  }

  /** get selected file data set
   * @return file data set
   */
  private FileData getSelectedFileData()
  {
    int index = widgetFiles.getSelectionIndex();
    if (index >= 0)
    {
      return (FileData)widgetFiles.getItem(index).getData();
    }
    else
    {
      return null;
    }
  }
}

/* end of file */
