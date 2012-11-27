/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command find files
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Pattern;

// graphics
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;


/****************************** Classes ********************************/

/** find files command
 */
public class CommandFindFiles
{
  enum Types
  {
    NAME,
    CONTENT,
    UNKNOWN
  };

  /** find data
   */
  class FindData
  {
    RepositoryTab repositoryTab;
    File          file;
    int           lineNumber;

    FindData(RepositoryTab repositoryTab, File file, int lineNumber)
    {
      this.repositoryTab = repositoryTab;
      this.file          = file;
      this.lineNumber    = lineNumber;
    }
  }

  /** find data comparator
   */
  class FindDataComparator implements Comparator<FindData>
  {
    // Note: enum in inner classes are not possible in Java, thus use the old way...
    private final static int SORTMODE_NAME        = 0;
    private final static int SORTMODE_LINE_NUMBER = 1;
    private final static int SORTMODE_PATH        = 2;
    private final static int SORTMODE_DATE        = 3;
    private final static int SORTMODE_SIZE        = 4;

    private int sortMode;

    /** create file data comparator
     * @param table table
     * @param sortColumn column to sort
     */
    FindDataComparator(Table table, TableColumn sortColumn)
    {
      if      (table.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
      else if (table.getColumn(1) == sortColumn) sortMode = SORTMODE_LINE_NUMBER;
      else if (table.getColumn(2) == sortColumn) sortMode = SORTMODE_PATH;
      else if (table.getColumn(3) == sortColumn) sortMode = SORTMODE_DATE;
      else if (table.getColumn(4) == sortColumn) sortMode = SORTMODE_SIZE;
      else                                       sortMode = SORTMODE_NAME;
    }

    /** create file data comparator
     * @param table table
     */
    FindDataComparator(Table table)
    {
      this(table,table.getSortColumn());
    }

    /** compare file tree data without taking care about type
     * @param findData1, findData2 file tree data to compare
     * @return -1 iff findData1 < findData2,
                0 iff findData1 = findData2,
                1 iff findData1 > findData2
     */
    public int compare(FindData findData1, FindData findData2)
    {
      final int[][] SORT_ORDERING = new int[][]{new int[]{SORTMODE_NAME,SORTMODE_LINE_NUMBER,SORTMODE_PATH,SORTMODE_DATE,SORTMODE_SIZE},
                                                new int[]{SORTMODE_LINE_NUMBER,SORTMODE_PATH,SORTMODE_NAME,SORTMODE_DATE,SORTMODE_SIZE},
                                                new int[]{SORTMODE_PATH,SORTMODE_LINE_NUMBER,SORTMODE_NAME,SORTMODE_DATE,SORTMODE_SIZE},
                                                new int[]{SORTMODE_DATE,SORTMODE_LINE_NUMBER,SORTMODE_NAME,SORTMODE_PATH,SORTMODE_SIZE},
                                                new int[]{SORTMODE_SIZE,SORTMODE_LINE_NUMBER,SORTMODE_NAME,SORTMODE_PATH,SORTMODE_DATE}
                                               };

      int result = 0;

      int z = 0;
      do
      {
        switch (SORT_ORDERING[sortMode][z])
        {
          case SORTMODE_NAME:
            String name1 = findData1.file.getName();
            String name2 = findData2.file.getName();
            result = name1.compareTo(name2);
            break;
          case SORTMODE_LINE_NUMBER:
            if      (findData1.lineNumber < findData2.lineNumber) result = -1;
            else if (findData1.lineNumber > findData2.lineNumber) result =  1;
            else                                                  result =  0;
            break;
          case SORTMODE_PATH:
            String path1 = findData1.file.getParent();
            String path2 = findData2.file.getParent();
            result = path1.compareTo(path2);
            break;
          case SORTMODE_DATE:
            long lastModified1 = findData1.file.lastModified();
            long lastModified2 = findData2.file.lastModified();
            if      (lastModified1 < lastModified2) result = -1;
            else if (lastModified1 > lastModified2) result =  1;
            else                                    result =  0;
            break;
          case SORTMODE_SIZE:
            long length1 = findData1.file.length();
            long length2 = findData2.file.length();
            if      (length1 < length2) result = -1;
            else if (length1 > length2) result =  1;
            else                        result =  0;
            break;
          default:
            break;
        }
        z++;
      }
      while ((z < SORT_ORDERING[sortMode].length) && (result == 0));

      return result;
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "FindDataComparator {"+sortMode+"}";
    }
  }

  /** dialog data
   */
  class Data
  {
    String  fileNameFilter;
    String  filter;
    boolean findByNameFlag;
    boolean findByContentFlag;
    boolean caseSensitiveFlag;
    boolean showAllHitsFlag;
    boolean showAllRepositoriesFlag;
    boolean showHiddenFilesFlag;
    boolean forceFlag;
    boolean quitFlag;

    Data()
    {
      this.fileNameFilter          = CommandFindFiles.fileNameFilter;
      this.filter                  = CommandFindFiles.filter;
      this.findByNameFlag          = false;
      this.findByContentFlag       = false;
      this.caseSensitiveFlag       = false;
      this.showAllHitsFlag         = CommandFindFiles.showAllHitsFlag;
      this.showAllRepositoriesFlag = CommandFindFiles.showAllRepositoriesFlag;
      this.showHiddenFilesFlag     = CommandFindFiles.showHiddenFilesFlag;
      this.forceFlag               = false;
      this.quitFlag                = false;
    }
  };

  // --------------------------- constants --------------------------------
  // unique history database ids
  private final int HISTORY_ID_FILENAME_FILTER = 10;
  private final int HISTORY_ID_FILTER          = 11;

  // --------------------------- variables --------------------------------

  // stored settings
  private static String                  fileNameFilter          = "";
  private static String                  filter                  = "";
  private static boolean                 caseSensitiveFlag       = false;
  private static boolean                 showAllHitsFlag         = false;
  private static boolean                 showAllRepositoriesFlag = false;
  private static boolean                 showHiddenFilesFlag     = false;

  // global variable references
  private final Types                    type;
  private final RepositoryTab            repositoryTab;
  private final Display                  display;
  private final Clipboard     clipboard;

  private final RepositoryTab[]          thisRepositoryTabs;
  private final RepositoryTab[]          allRepositoryTabs;

  // dialog
  private final Data                     data = new Data();
  private final Shell                    dialog;

  // widgets
  private final Table                    widgetFiles;
  private final Combo                    widgetFileNameFilter;
  private final Combo                    widgetFilter;
  private final Listener                 mouseWheelListener;
  private final Button                   widgetFindByName;
  private final Button                   widgetFindByContent;
  private final Button                   widgetCaseSensitive;
  private final Button                   widgetShowAllHits;
  private final Button                   widgetShowAllRepositories;
  private final Button                   widgetShowHiddenFiles;

  private final Button                   widgetButtonOpenDirectory;
  private final Button                   widgetButtonOpen;
  private final Button                   widgetButtonOpenWith;
//  private final Button         widgetButtonUpdate;
//  private final Button         widgetButtonCommit;
//  private final Button         widgetButtonCreatePatch;
//  private final Button         widgetButtonAdd;
//  private final Button         widgetButtonRemove;
//  private final Button         widgetButtonRevert;
//  private final Button        widgetButtonDiff;
  private final Button                   widgetButtonRevisions;
//  private final Button        widgetButtonSolve;

  private final Button                   widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** find file command
   * @param shell shell
   * @param repositoryTab repository tab
   */
  CommandFindFiles(final Shell shell, final RepositoryTab repositoryTab, Types type)
  {
    Composite   composite,subComposite;
    Menu        menu;
    MenuItem    menuItem;
    Label       label;
    Button      button;
    TableColumn tableColumn;
    Listener    listener;

    // initialize variables
    this.type               = type;
    this.repositoryTab      = repositoryTab;
    this.thisRepositoryTabs = new RepositoryTab[]{repositoryTab};
    this.allRepositoryTabs  = repositoryTab.onzen.getRepositoryTabs();

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // add files dialog
    dialog = Dialogs.open(shell,
                          showAllRepositoriesFlag
                            ? "Find files"
                            : "Find files in '"+repositoryTab.repository.rootPath+"'",
                          new double[]{1.0,0.0},
                          1.0
                         );

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      widgetFiles = Widgets.newTable(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,0,0,TableLayoutData.NSWE);
      SelectionListener selectionListener = new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          TableColumn        tableColumn    = (TableColumn)selectionEvent.widget;
          FindDataComparator findDataComparator = new FindDataComparator(widgetFiles,tableColumn);

          synchronized(widgetFiles)
          {
            Widgets.sortTableColumn(widgetFiles,tableColumn,findDataComparator);
          }
        }
      };
      tableColumn = Widgets.addTableColumn(widgetFiles,0,"Name",        SWT.LEFT, true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,1,"#",           SWT.RIGHT,true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,2,"Path",        SWT.LEFT, true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,3,"Date",        SWT.LEFT, false);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,4,"Size [bytes]",SWT.RIGHT,false);
      tableColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumns.width);

      menu = Widgets.newPopupMenu(dialog);
      {
        menuItem = Widgets.addMenuItem(menu,"Open directory");
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonOpenDirectory);
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Revisions\u2026");
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
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

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Open file\u2026");
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonOpen);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Open file with\u2026",Settings.keyOpenFileWith);
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Widgets.invoke(widgetButtonOpenWith);
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Rename local file/directory\u2026",Settings.keyRenameLocal);
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            FindData findData = getSelectedFindData();
            if (findData != null)
            {
              findData.repositoryTab.renameLocalFile(findData.file);
              dialog.setActive();
            }
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Delete local files/directories\u2026",Settings.keyDeleteLocal);
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            FindData findData = getSelectedFindData();
            if (findData != null)
            {
              findData.repositoryTab.deleteLocalFile(findData.file);
              dialog.setActive();
            }
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Convert whitespaces\u2026",Settings.keyDeleteLocal);
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetModifyListener(menuItem,data)
        {
          public void modified(MenuItem menuItem)
          {
//            menuItem.setEnabled((widgetFiles.getSelectionCount() > 0));
          }
        });
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
Dprintf.dprintf("");
//  public boolean convertWhitespaces(String fileName, String[] fileNames, String message)
//            convertWhitespaces();
          }
        });
      }
      widgetFiles.setMenu(menu);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"File patterns:",SWT.NONE,Settings.keyFileNameFilter);
        Widgets.layout(label,0,0,TableLayoutData.W);
        widgetFileNameFilter = Widgets.newCombo(subComposite);
        Widgets.layout(widgetFileNameFilter,0,1,TableLayoutData.WE);
        widgetFileNameFilter.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });
        widgetFileNameFilter.setToolTipText("File name/text patterns. Use * and ? as wildcards. Use space as separator.");
        for (String pattern : HistoryDatabase.getHistory(HISTORY_ID_FILENAME_FILTER,20,HistoryDatabase.Directions.SORTED))
        {
          widgetFileNameFilter.add(pattern);
        }
        widgetFileNameFilter.setText(CommandFindFiles.fileNameFilter);

        label = Widgets.newLabel(subComposite,"Text patterns:",SWT.NONE,Settings.keyFilter);
        Widgets.layout(label,1,0,TableLayoutData.W);
        widgetFilter = Widgets.newCombo(subComposite);
        Widgets.layout(widgetFilter,1,1,TableLayoutData.WE);
        widgetFilter.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });
        widgetFilter.setToolTipText("File name/text patterns. Use * and ? as wildcards. Use space as separator.");
        String[] history;
        for (String pattern : HistoryDatabase.getHistory(HISTORY_ID_FILTER,20,HistoryDatabase.Directions.SORTED))
        {
          widgetFilter.add(pattern);
        }
        widgetFilter.setText(CommandFindFiles.filter);
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,0.0));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        widgetFindByName = Widgets.newCheckbox(subComposite,"find by name",SWT.F5);
        widgetFindByName.setSelection(type == Types.NAME);
        Widgets.layout(widgetFindByName,0,0,TableLayoutData.W);
        widgetFindByName.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetFindByContent = Widgets.newCheckbox(subComposite,"find by content",SWT.F6);
        widgetFindByContent.setSelection(type == Types.CONTENT);
        Widgets.layout(widgetFindByContent,0,1,TableLayoutData.W);
        widgetFindByContent.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetCaseSensitive = Widgets.newCheckbox(subComposite,"case sensitive",SWT.F7);
        widgetCaseSensitive.setSelection(data.caseSensitiveFlag);
        Widgets.layout(widgetCaseSensitive,0,2,TableLayoutData.W);
        widgetCaseSensitive.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetShowAllHits = Widgets.newCheckbox(subComposite,"show all hits",SWT.F8);
        widgetShowAllHits.setSelection(data.showAllRepositoriesFlag);
        Widgets.layout(widgetShowAllHits,0,3,TableLayoutData.W);
        widgetShowAllHits.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetShowAllRepositories = Widgets.newCheckbox(subComposite,"show all repositories",SWT.F9);
        widgetShowAllRepositories.setSelection(data.showAllRepositoriesFlag);
        Widgets.layout(widgetShowAllRepositories,0,4,TableLayoutData.W);
        widgetShowAllRepositories.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetShowHiddenFiles = Widgets.newCheckbox(subComposite,"show hidden files",SWT.F10);
        widgetShowHiddenFiles.setSelection(data.showHiddenFilesFlag);
        Widgets.layout(widgetShowHiddenFiles,0,5,TableLayoutData.W);
        widgetShowHiddenFiles.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonOpenDirectory = Widgets.newButton(composite,"Open directory");
      widgetButtonOpenDirectory.setEnabled(false);
      Widgets.layout(widgetButtonOpenDirectory,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetButtonOpenDirectory,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonOpenDirectory.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          FindData findData = getSelectedFindData();
          if (findData != null)
          {
            findData.repositoryTab.onzen.selectRepositoryTab(findData.repositoryTab);
            findData.repositoryTab.openDirectory(findData.file);
          }
        }
      });

      widgetButtonOpen = Widgets.newButton(composite,"Open file");
      widgetButtonOpen.setEnabled(false);
      Widgets.layout(widgetButtonOpen,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetButtonOpen,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonOpen.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          FindData findData = getSelectedFindData();
          if (findData != null)
          {
            findData.repositoryTab.openFile(findData.file,
                                            Onzen.getMimeType(findData.file),
                                            findData.lineNumber
                                           );
          }
        }
      });

      widgetButtonOpenWith = Widgets.newButton(composite,"Open file with...",Settings.keyOpenFileWith);
      widgetButtonOpenWith.setEnabled(false);
      Widgets.layout(widgetButtonOpenWith,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetButtonOpenWith,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(widgetFiles.getSelectionCount() > 0));
        }
      });
      widgetButtonOpenWith.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          FindData findData = getSelectedFindData();
          if (findData != null)
          {
            findData.repositoryTab.openFileWith(findData.file,findData.lineNumber);
          }
        }
      });

      widgetButtonRevisions = Widgets.newButton(composite,"Revisions",Settings.keyRevisions);
      widgetButtonRevisions.setEnabled(false);
      Widgets.layout(widgetButtonRevisions,0,3,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetButtonRevisions,data)
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
          FindData findData = getSelectedFindData();
          if (findData != null)
          {
            CommandRevisions commandRevisions = new CommandRevisions(shell,findData.repositoryTab,new FileData(findData.file));
            commandRevisions.run();
          }
        }
      });

      button = Widgets.newButton(composite,"Reread");
      Widgets.layout(button,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          restartFindFiles(true);
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,5,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    widgetFiles.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        Table widget = (Table)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          TableItem tableItem = widget.getItem(index);
          FindData  findData  = (FindData)tableItem.getData();

          repositoryTab.openFile(findData.file,
                                 Onzen.getMimeType(findData.file),
                                 findData.lineNumber
                                );
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Widgets.modified(data);
      }
    });

    KeyListener keyListener = new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFileNameFilter))
        {
          Widgets.setFocus(widgetFileNameFilter);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFilter))
        {
          Widgets.setFocus(widgetFilter);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyOpenFileWith))
        {
          Widgets.invoke(widgetButtonOpenWith);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyRevisions))
        {
          Widgets.invoke(widgetButtonRevisions);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F5))
        {
          Widgets.invoke(widgetFindByName);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F6))
        {
          Widgets.invoke(widgetFindByContent);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F7))
        {
          Widgets.invoke(widgetCaseSensitive);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F8))
        {
          Widgets.invoke(widgetShowAllHits);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F9))
        {
          Widgets.invoke(widgetShowAllRepositories);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F10))
        {
          Widgets.invoke(widgetShowHiddenFiles);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'a'))
        {
          widgetFiles.selectAll();
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'c'))
        {
          HashSet<FindData> findDataSet = getSelectedFindDataSet();
          if (findDataSet != null)
          {
            StringBuilder buffer = new StringBuilder();
            for (FindData findData : findDataSet)
            {
              buffer.append(findData.file.getAbsolutePath()); buffer.append('\n');
            }
            Widgets.setClipboard(clipboard,buffer.toString());
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    };
    widgetFiles.addKeyListener(keyListener);
    widgetFileNameFilter.addKeyListener(keyListener);
    widgetFilter.addKeyListener(keyListener);
    widgetFindByName.addKeyListener(keyListener);
    widgetFindByContent.addKeyListener(keyListener);
    widgetCaseSensitive.addKeyListener(keyListener);
    widgetShowAllRepositories.addKeyListener(keyListener);
    widgetShowHiddenFiles.addKeyListener(keyListener);
    widgetFilter.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (keyEvent.keyCode == SWT.ARROW_UP)
        {
          int index = widgetFiles.getSelectionIndex();
          if (index > 0) index--;
          widgetFiles.setSelection(index,index);
        }
        else if (keyEvent.keyCode == SWT.ARROW_DOWN)
        {
          int index = widgetFiles.getSelectionIndex();
          if (index < widgetFiles.getItemCount()-1) index++;
          widgetFiles.setSelection(index,index);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        restartFindFiles();
      }
    });
    // filter mouse-wheel event to avoid scrolling in file name/content-combo
    mouseWheelListener = new Listener()
    {
      public void handleEvent(Event event)
      {
        if (   (event.widget == widgetFileNameFilter)
            || (event.widget == widgetFilter)
           )
        {
Dprintf.dprintf("event=%s",event);
          event.doit = false;

/*
??? scroll widgetFiles
          widgetFiles

          Event redirectedEvent = new Event();
          redirectedEvent.type   = SWT.MouseWheel;
          redirectedEvent.widget = widgetFiles;
          redirectedEvent.time   = event.time;
          redirectedEvent.x      = event.x;
          redirectedEvent.y      = event.y;
          redirectedEvent.detail = event.detail;
          redirectedEvent.count  = event.count;
          widgetFiles.notifyListeners(SWT.MouseWheel,redirectedEvent);
*/
        }
      }
    };
    display.addFilter(SWT.MouseWheel,mouseWheelListener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryFindFiles,Settings.setWindowLocation);

    // find files
    Background.run(new BackgroundRunnable()
    {
      String    fileNameFilter          = "";
      Pattern[] fileNameFilterPatterns  = null;
      String    filter                  = "";
      Pattern[] fileNamePatterns        = null;
      Pattern[] contentPatterns         = null;
      boolean   findByNameFlag          = data.findByNameFlag;
      boolean   findByContentFlag       = data.findByContentFlag;
      boolean   caseSensitiveFlag       = data.caseSensitiveFlag;
      boolean   showAllHitsFlag         = data.showAllHitsFlag;
      boolean   showAllRepositoriesFlag = data.showAllRepositoriesFlag;
      boolean   showHiddenFilesFlag     = data.showHiddenFilesFlag;

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isDataModified()
      {
        return    data.quitFlag
               || ((data.fileNameFilter != null) && !data.fileNameFilter.equals(fileNameFilter))
               || ((data.filter != null) && !data.filter.equals(filter))
               || (data.findByNameFlag != findByNameFlag)
               || (data.findByContentFlag != findByContentFlag)
               || (data.caseSensitiveFlag != caseSensitiveFlag)
               || (data.showAllHitsFlag != showAllHitsFlag)
               || (data.showAllRepositoriesFlag != showAllRepositoriesFlag)
               || (data.showHiddenFilesFlag != showHiddenFilesFlag)
               || data.forceFlag;
      }

      /** run method
       */
      public void run()
      {
        HashSet<String> findDataKeySet = new HashSet<String>();
 
        while (!data.quitFlag)
        {
          // clearfiles
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Widgets.removeAllTableEntries(widgetFiles);
              }
            });
          }
          findDataKeySet.clear();

          // find files
          if (!filter.isEmpty())
          {
//Dprintf.dprintf("findNamePatterns=%s findContentPatterns=%s",findNamePatterns,findContentPatterns);
            // start find files
            if (!dialog.isDisposed())
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
                  widgetFiles.setForeground(Onzen.COLOR_DARK_GRAY);
                }
              });
            }

            // find files
            for (final RepositoryTab repositoryTab : (showAllRepositoriesFlag) ? allRepositoryTabs : thisRepositoryTabs)
            {
              File directory = new File(repositoryTab.repository.rootPath);

              // check for modified data
              if (isDataModified()) break;
//Dprintf.dprintf("directory=%s",directory);

              LinkedList<File> directoryList = new LinkedList<File>();
              do
              {
//Dprintf.dprintf("directory=%s",directory);
                String[] fileNames = directory.list();
                if (fileNames != null)
                {
                  for (String fileName : fileNames)
                  {
                    // check for modified data
                    if (isDataModified()) break;

//Dprintf.dprintf("fileName=%s %s",fileName,repositoryTab.repository.isHiddenFile(fileName));
                    if (showHiddenFilesFlag || !repositoryTab.repository.isHiddenFile(fileName))
                    {
                      final File file = new File(directory,fileName);
                      if     (file.isFile())
                      {
                        if ((fileNameFilterPatterns.length == 0) || matchPatterns(fileName,fileNameFilterPatterns))
                        {
                          // initialise text search
                          TextSearch textSearch = new TextSearch(file,filter,contentPatterns,data.caseSensitiveFlag);

                          // find by name or content
                          boolean nameMatchFlag = false;
                          int     lineNumber    = 0;
                          if (findByNameFlag)
                          {
                            nameMatchFlag =    (data.caseSensitiveFlag && fileName.contains(filter))
                                            || (!data.caseSensitiveFlag && fileName.toLowerCase().contains(filter))
                                            || matchPatterns(fileName,fileNamePatterns);
                          }
                          if (findByContentFlag)
                          {
                            if (!nameMatchFlag)
                            {
                              lineNumber = textSearch.findNext();
                            }
                          }

                          if (   nameMatchFlag
                              || (lineNumber > 0)
                             )
                          {
                            do
                            {
                              String findDataKey = file.getAbsolutePath()+Integer.toString(lineNumber);
                              if (!findDataKeySet.contains(findDataKey))
                              {
//Dprintf.dprintf("file=%s n=%d x=%s",file.getAbsolutePath(),lineNumber,findDataKeySet.contains(findDataKey));
                                // add find data to list
                                final FindData findData = new FindData(repositoryTab,file,lineNumber);
                                display.syncExec(new Runnable()
                                {
                                  public void run()
                                  {
                                    if (!dialog.isDisposed())
                                    {
                                      Widgets.insertTableEntry(widgetFiles,
                                                               new FindDataComparator(widgetFiles),
                                                               findData,
                                                               file.getName(),
                                                               Integer.toString(findData.lineNumber),
                                                               file.getParent(),
                                                               Onzen.DATETIME_FORMAT.format(file.lastModified()),
                                                               Long.toString(file.length())
                                                              );
                                    }
                                  }
                                });

                                findDataKeySet.add(findDataKey);
                              }

                              // check for modified data
                              if (isDataModified()) break;

                              // find next text line
                              if (data.showAllHitsFlag && (lineNumber > 0))
                              {
                                lineNumber = textSearch.findNext();
                              }
                            }
                            while (data.showAllHitsFlag && (lineNumber > 0));
                          }

                          // free resources
                          textSearch.close();
                        }
                      }
                      else if (file.isDirectory())
                      {
//Dprintf.dprintf("add dir=%s %s",fileName,file.getPath());
                        directoryList.add(file);
                      }
                    }
                  }
                }
              }
              while ((directory = directoryList.pollFirst()) != null);
            }

            // stop find files
            if (!dialog.isDisposed())
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  widgetFiles.setForeground(Onzen.COLOR_BLACK);
                  Widgets.resetCursor(dialog);
                }
              });
            }
          }

          // wait for new filter pattern/quit
          synchronized(data)
          {
            // wait for new find data
            while (!isDataModified())
            {
              try
              {
                data.wait();
              }
              catch (InterruptedException exception)
              {
                // ignored
              }
            }

            // get new find data, clear data
            if (data.filter != null)
            {
              // get new find text/pattern
              fileNameFilter         = new String(data.fileNameFilter);
              fileNameFilterPatterns = compileFileNamePatterns(data.fileNameFilter,data.caseSensitiveFlag);
              filter                 = data.caseSensitiveFlag ? new String(data.filter) : new String(data.filter).toLowerCase();
              fileNamePatterns       = compileFileNamePatterns(data.filter,data.caseSensitiveFlag);
              contentPatterns        = compileContentPatterns(data.filter,data.caseSensitiveFlag);

              data.filter = null;
            }
            findByNameFlag          = data.findByNameFlag;
            findByContentFlag       = data.findByContentFlag;
            caseSensitiveFlag       = data.caseSensitiveFlag;
            showAllHitsFlag         = data.showAllHitsFlag;
            showAllRepositoriesFlag = data.showAllRepositoriesFlag;
            showHiddenFilesFlag     = data.showHiddenFilesFlag;

            data.forceFlag = false;
          }
        }
      }
    });
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      Widgets.setFocus(widgetFilter);
      Dialogs.run(dialog,new DialogRunnable()
      {
        public void done(Object result)
        {
          // store values
          HistoryDatabase.putHistory(HISTORY_ID_FILENAME_FILTER,
                                     widgetFileNameFilter.getItems(),
                                     20,
                                     HistoryDatabase.Directions.SORTED,
                                     widgetFileNameFilter.getText().trim()
                                    );
          HistoryDatabase.putHistory(HISTORY_ID_FILTER,
                                     widgetFilter.getItems(),
                                     20,
                                     HistoryDatabase.Directions.SORTED,
                                     widgetFilter.getText().trim()
                                    );
          CommandFindFiles.fileNameFilter          = widgetFileNameFilter.getText().trim();
          CommandFindFiles.filter                  = widgetFilter.getText().trim();
          CommandFindFiles.caseSensitiveFlag       = widgetCaseSensitive.getSelection();
          CommandFindFiles.showAllHitsFlag         = widgetShowAllHits.getSelection();
          CommandFindFiles.showAllRepositoriesFlag = widgetShowAllRepositories.getSelection();
          CommandFindFiles.showHiddenFilesFlag     = widgetShowHiddenFiles.getSelection();

          Settings.geometryFindFiles        = dialog.getSize();
          Settings.geometryFindFilesColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetFiles));

          // signal quit to find thread
          synchronized(data)
          {
            data.quitFlag = true;
            data.notifyAll();
          }

          // remove mouse-wheel filter
          display.removeFilter(SWT.MouseWheel,mouseWheelListener);
        }
      });
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandFindFiles {}";
  }

  //-----------------------------------------------------------------------

  class TextSearch
  {
    private BufferedReader input;
    private String         content;
    private Pattern[]      contentPatterns;
    private boolean        caseSensitiveFlag;
    private int            lineNumber;

    /** create text search
     * @param
     */
    TextSearch(File file, String content, Pattern[] contentPatterns, boolean caseSensitiveFlag)
    {
      this.input             = null;
      this.content           = (caseSensitiveFlag) ? content : content.toLowerCase();
      this.contentPatterns   = contentPatterns;
      this.caseSensitiveFlag = caseSensitiveFlag;
      this.lineNumber        = 0;

      // open file
      try
      {
        input = new BufferedReader(new FileReader(file));
      }
      catch (IOException exception)
      {
        input = null;
      }
    }

    /** create text search
     * @param
     */
    public void close()
    {
      try
      {
        if (input != null) input.close();
      }
      catch (IOException exception)
      {
        // ignored
      }
    }

    /** find next line with search text
     * @return line number or -1 if not found
     */
    int findNext()
    {
      int n = -1;

      if (input != null)
      {
        try
        {
          // read file and find text
          String line;
          while ((line = input.readLine()) != null)
          {
            lineNumber++;
            if (   (caseSensitiveFlag && line.contains(content))
                || (!caseSensitiveFlag && line.toLowerCase().contains(content))
                || matchPatterns(line,contentPatterns)
               )
            {
              n = lineNumber;
              break;
            }
          }
        }
        catch (IOException exception)
        {
          // not found
        }
      }

      return n;
    }
  }

  /** restart find files matching filters
   * @param forceFlag true to force restart find files
   */
  private void restartFindFiles(boolean forceFlag)
  {
    if (!dialog.isDisposed())
    {
      dialog.setText(widgetShowAllRepositories.getSelection()
                       ? "Find files"
                       : "Find files in '"+repositoryTab.repository.rootPath+"'"
                    );

      synchronized(data)
      {
        data.fileNameFilter          = widgetFileNameFilter.getText().trim();
        data.filter                  = widgetFilter.getText().trim();
        data.findByNameFlag          = widgetFindByName.getSelection();
        data.findByContentFlag       = widgetFindByContent.getSelection();
        data.caseSensitiveFlag       = widgetCaseSensitive.getSelection();
        data.showAllHitsFlag         = widgetShowAllHits.getSelection();
        data.showAllRepositoriesFlag = widgetShowAllRepositories.getSelection();
        data.showHiddenFilesFlag     = widgetShowHiddenFiles.getSelection();
        data.forceFlag               = forceFlag;
        data.notifyAll();

        if (!data.filter.isEmpty()) Widgets.modified(data);
      }
    }
  }

  /** restart find files matching filters
   */
  private void restartFindFiles()
  {
    restartFindFiles(false);
  }

  /** get selected find data set
   * @return find data set
   */
  private HashSet<FindData> getSelectedFindDataSet()
  {
    HashSet<FindData> findDataSet = new HashSet<FindData>();

    for (TableItem tableItem : widgetFiles.getSelection())
    {
      findDataSet.add((FindData)tableItem.getData());
    }

    return (findDataSet.size() > 0) ? findDataSet : null;
  }

  /** get selected file data
   * @return file data or null
   */
  private FindData getSelectedFindData()
  {
    int index = widgetFiles.getSelectionIndex();
    if (index >= 0)
    {
      return (FindData)widgetFiles.getItem(index).getData();
    }
    else
    {
      return null;
    }
  }

  /** compile file patterns
   * @param findTextPattern find text string
   * @param caseSensitiveFlag true for case-sensitive matching
   * @return file name pattern array
   */
  private Pattern[] compileFileNamePatterns(String findTextPattern, boolean caseSensitiveFlag)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findTextPattern,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS))
    {
      patternList.add(Pattern.compile(StringUtils.globToRegex(pattern),caseSensitiveFlag ? 0 : Pattern.CASE_INSENSITIVE));
    }

    return patternList.toArray(new Pattern[patternList.size()]);
  }

  /** compile content patterns
   * @param findTextPattern find text string
   * @param caseSensitiveFlag true for case-sensitive matching
   * @return file content pattern array
   */
  private Pattern[] compileContentPatterns(String findTextPattern, boolean caseSensitiveFlag)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findTextPattern,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS))
    {
      patternList.add(Pattern.compile(".*"+StringUtils.globToRegex(pattern)+".*",caseSensitiveFlag ? 0 : Pattern.CASE_INSENSITIVE));
    }

    return patternList.toArray(new Pattern[patternList.size()]);
  }

  /** match patterns
   * @param string string to match
   * @param patterns patterns to match
   * @return true iff one pattern match with string
   */
  private boolean matchPatterns(String string, Pattern[] patterns)
  {
    for (Pattern pattern : patterns)
    {
      if (pattern.matcher(string).matches()) return true;
    }

    return false;
  }

  /** check if file contains text
   * @param file file to check
   * @param content text to find
   * @param contentPatterns content patterns
   * @param caseSensitiveFlag true for case-sensitive matching
   * @return line number or -1 if not found
   */
  private int fileContains(File file, String content, Pattern[] contentPatterns, boolean caseSensitiveFlag)
  {
    int lineNumber = -1;

    BufferedReader input = null;
    try
    {
      // open file
      input = new BufferedReader(new FileReader(file));

      // read file and find text
      if (!caseSensitiveFlag) content = content.toLowerCase();
      String line;
      int    n = 0;
      while ((line = input.readLine()) != null)
      {
        n++;
        if (   (caseSensitiveFlag && line.contains(content))
            || (!caseSensitiveFlag && line.toLowerCase().contains(content))
            || matchPatterns(line,contentPatterns)
           )
        {
          lineNumber = n;
          break;
        }
      }

      // close file
      input.close(); input = null;
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      try
      {
        if (input != null) input.close();
      }
      catch (IOException exception)
      {
        // ignored
      }
    }

    return lineNumber;
  }
}

/* end of file */
