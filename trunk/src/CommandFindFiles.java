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
class CommandFindFiles
{
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
    private final static int SORTMODE_NAME = 0;
    private final static int SORTMODE_PATH = 1;
    private final static int SORTMODE_DATE = 2;
    private final static int SORTMODE_SIZE = 3;

    private int sortMode;

    /** create file data comparator
     * @param table table
     * @param sortColumn column to sort
     */
    FindDataComparator(Table table, TableColumn sortColumn)
    {
      if      (table.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
      else if (table.getColumn(1) == sortColumn) sortMode = SORTMODE_PATH;
      else if (table.getColumn(2) == sortColumn) sortMode = SORTMODE_DATE;
      else if (table.getColumn(3) == sortColumn) sortMode = SORTMODE_SIZE;
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
      final int[][] SORT_ORDERING = new int[][]{new int[]{SORTMODE_NAME,SORTMODE_PATH,SORTMODE_DATE,SORTMODE_SIZE},
                                                new int[]{SORTMODE_PATH,SORTMODE_NAME,SORTMODE_DATE,SORTMODE_SIZE},
                                                new int[]{SORTMODE_DATE,SORTMODE_NAME,SORTMODE_PATH,SORTMODE_SIZE},
                                                new int[]{SORTMODE_SIZE,SORTMODE_NAME,SORTMODE_PATH,SORTMODE_DATE}
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
    String  fileNameFilterText;
    String  findTextPattern;
    boolean findNamesFlag;
    boolean findContentFlag;
    boolean showAllRepositoriesFlag;
    boolean showHiddenFilesFlag;
    boolean quitFlag;

    Data()
    {
      this.fileNameFilterText      = CommandFindFiles.fileNameFilterText;
      this.findTextPattern         = "";
      this.findNamesFlag           = CommandFindFiles.findNamesFlag;
      this.findContentFlag         = CommandFindFiles.findContentFlag;
      this.showAllRepositoriesFlag = CommandFindFiles.showAllRepositoriesFlag;
      this.showHiddenFilesFlag     = CommandFindFiles.showHiddenFilesFlag;
      this.quitFlag                = false;
    }
  };

  // --------------------------- constants --------------------------------
  // unique history database ids
  private final int HISTORY_ID_FILENAME_FILTER_PATTERNS = 10;
  private final int HISTORY_ID_TEXT_FIND_PATTERNS       = 11;

  // --------------------------- variables --------------------------------

  // stored settings
  private static String                  fileNameFilterText      = "";
  private static String                  fileTextFindPattern     = "";
  private static boolean                 findNamesFlag           = true;
  private static boolean                 findContentFlag         = false;
  private static boolean                 showAllRepositoriesFlag = false;
  private static boolean                 showHiddenFilesFlag     = false;

  // global variable references
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
  private final Combo                    widgetFileNameFilterText;
  private final Combo                    widgetTextFindPatterns;
  private final Button                   widgetFindNames;
  private final Button                   widgetFindContent;
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
  CommandFindFiles(final Shell shell, final RepositoryTab repositoryTab)
  {
    Composite   composite,subComposite;
    Menu        menu;
    MenuItem    menuItem;
    Label       label;
    Button      button;
    TableColumn tableColumn;
    Listener    listener;

    // initialize variables
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
      tableColumn = Widgets.addTableColumn(widgetFiles,1,"Path",        SWT.LEFT, true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,2,"Date",        SWT.LEFT, false);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,3,"Size [bytes]",SWT.RIGHT,false);
      tableColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumns.width);

      menu = Widgets.newPopupMenu(dialog);
      {
        menuItem = Widgets.addMenuItem(menu,"Open directory");
        menuItem.setEnabled(false);
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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
        Widgets.addModifyListener(new WidgetListener(menuItem,data)
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

        widgetFileNameFilterText = Widgets.newCombo(subComposite);
        Widgets.layout(widgetFileNameFilterText,0,1,TableLayoutData.WE);
        widgetFileNameFilterText.setToolTipText("File name filter patterns. Use * and ? as wildcards. Use space as separator.");
        widgetFileNameFilterText.addSelectionListener(new SelectionListener()
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
        widgetFileNameFilterText.setToolTipText("Find file patterns. Use * and ? as wildcards. Use space as separator.");
        for (String fileNameFilterText : HistoryDatabase.getHistory(HISTORY_ID_FILENAME_FILTER_PATTERNS,20,HistoryDatabase.Directions.DESCENDING))
        {
          widgetFileNameFilterText.add(fileNameFilterText);
        }
        widgetFileNameFilterText.setText(data.fileNameFilterText);

        label = Widgets.newLabel(subComposite,"Text patterns:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetTextFindPatterns = Widgets.newCombo(subComposite);
        Widgets.layout(widgetTextFindPatterns,1,1,TableLayoutData.WE);
        widgetTextFindPatterns.addSelectionListener(new SelectionListener()
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
        for (String textFindPattern : HistoryDatabase.getHistory(HISTORY_ID_TEXT_FIND_PATTERNS,20,HistoryDatabase.Directions.DESCENDING))
        {
          widgetTextFindPatterns.add(textFindPattern);
        }
        widgetTextFindPatterns.setToolTipText("Find file by name/content patterns. Use * and ? as wildcards. Use space as separator.");
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,0.0));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        widgetFindNames = Widgets.newCheckbox(subComposite,"find names",SWT.F5);
        widgetFindNames.setSelection(data.findNamesFlag);
        Widgets.layout(widgetFindNames,0,0,TableLayoutData.W);
        widgetFindNames.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetFindContent = Widgets.newCheckbox(subComposite,"find content",SWT.F6);
        widgetFindContent.setSelection(data.findContentFlag);
        Widgets.layout(widgetFindContent,0,1,TableLayoutData.W);
        widgetFindContent.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetShowAllRepositories = Widgets.newCheckbox(subComposite,"show all repositories",SWT.F7);
        widgetShowAllRepositories.setSelection(data.showAllRepositoriesFlag);
        Widgets.layout(widgetShowAllRepositories,0,2,TableLayoutData.W);
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

        widgetShowHiddenFiles = Widgets.newCheckbox(subComposite,"show hidden files",SWT.F8);
        widgetShowHiddenFiles.setSelection(data.showHiddenFilesFlag);
        Widgets.layout(widgetShowHiddenFiles,0,3,TableLayoutData.W);
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
      Widgets.addModifyListener(new WidgetListener(widgetButtonOpenDirectory,data)
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
      Widgets.addModifyListener(new WidgetListener(widgetButtonOpen,data)
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
      Widgets.addModifyListener(new WidgetListener(widgetButtonOpenWith,data)
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
          FindData findData = getSelectedFindData();
          if (findData != null)
          {
            CommandRevisions commandRevisions = new CommandRevisions(shell,findData.repositoryTab,new FileData(findData.file));
            commandRevisions.run();
          }
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
          Widgets.setFocus(widgetFileNameFilterText);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          Widgets.setFocus(widgetTextFindPatterns);
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
          Widgets.invoke(widgetFindNames);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F6))
        {
          Widgets.invoke(widgetFindContent);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F7))
        {
          Widgets.invoke(widgetShowAllRepositories);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.F8))
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
    widgetFileNameFilterText.addKeyListener(keyListener);
    widgetFileNameFilterText.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        restartFindFiles();
      }
    });
    widgetTextFindPatterns.addKeyListener(keyListener);
    widgetFindNames.addKeyListener(keyListener);
    widgetFindContent.addKeyListener(keyListener);
    widgetShowAllRepositories.addKeyListener(keyListener);
    widgetShowHiddenFiles.addKeyListener(keyListener);
    widgetTextFindPatterns.addKeyListener(new KeyListener()
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

    // show dialog
    Dialogs.show(dialog,Settings.geometryFindFiles,Settings.setWindowLocation);

    // find files
    Background.run(new BackgroundRunnable()
    {
      String    fileNameFilterText       = "";
      Pattern[] fileNameFilterPatterns   = null;
      String    findTextPattern          = "";
      Pattern[] findNamePatterns         = null;
      Pattern[] findContentPatterns      = null;
      boolean   findNamesFlag            = data.findNamesFlag;
      boolean   findContentFlag          = data.findContentFlag;
      boolean   showAllRepositoriesFlag  = data.showAllRepositoriesFlag;
      boolean   showHiddenFilesFlag      = data.showHiddenFilesFlag;

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isDataModified()
      {
        return    data.quitFlag
               || ((data.fileNameFilterText != null) && !data.fileNameFilterText.equals(fileNameFilterText))
               || ((data.findTextPattern != null) && !data.findTextPattern.equals(findTextPattern))
               || (data.findNamesFlag != findNamesFlag)
               || (data.findContentFlag != findContentFlag)
               || (data.showAllRepositoriesFlag != showAllRepositoriesFlag)
               || (data.showHiddenFilesFlag != showHiddenFilesFlag);
      }

      /** run method
       */
      public void run()
      {
        while (!data.quitFlag)
        {
          // find files
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
          if (!findTextPattern.isEmpty())
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
                        if ((fileNameFilterPatterns.length == 0) || matchPatterns(fileNameFilterPatterns,fileName))
                        {
                          boolean nameMatchFlag = false;
                          int     lineNumber    = 0;
                          if (findNamesFlag)
                          {
                            nameMatchFlag =    fileName.toLowerCase().contains(findTextPattern)
                                            || matchPatterns(findNamePatterns,fileName);
                          }
                          if (   !nameMatchFlag
                              && findContentFlag
                             )
                          {
                            lineNumber = fileContains(file,findTextPattern,findContentPatterns);
                          }
//Dprintf.dprintf("fileName=%s findNamesFlag=%s nameMatchFlag=%s findContentFlag=%s %d",fileName,findNamesFlag,nameMatchFlag,findContentFlag,lineNumber);

                          if (   nameMatchFlag
                              || (lineNumber > 0)
                             )
                          {
                            if (!dialog.isDisposed())
                            {
                              final FindData findData = new FindData(repositoryTab,file,lineNumber);
                              display.syncExec(new Runnable()
                              {
                                public void run()
                                {
                                  FindDataComparator findDataComparator = new FindDataComparator(widgetFiles);

                                  Widgets.insertTableEntry(widgetFiles,
                                                           findDataComparator,
                                                           findData,
                                                           file.getName(),
                                                           file.getParent(),
                                                           Onzen.DATETIME_FORMAT.format(file.lastModified()),
                                                           Long.toString(file.length())
                                                          );
                                }
                              });
                            }
                          }
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

            // get new find data
            if (data.findTextPattern != null)
            {
              // get new find text/pattern
              fileNameFilterText     = new String(data.fileNameFilterText);
              fileNameFilterPatterns = compileFileNamePatterns(data.fileNameFilterText);
              findTextPattern        = new String(data.findTextPattern);
              findNamePatterns       = compileFileNamePatterns(data.findTextPattern);
              findContentPatterns    = compileContentPatterns(data.findTextPattern);

              // clear existing text
              data.findTextPattern = null;
            }
            findNamesFlag           = data.findNamesFlag;
            findContentFlag         = data.findContentFlag;
            showAllRepositoriesFlag = data.showAllRepositoriesFlag;
            showHiddenFilesFlag     = data.showHiddenFilesFlag;
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
      Widgets.setFocus(widgetTextFindPatterns);
      Dialogs.run(dialog,new DialogRunnable()
      {
        public void done(Object result)
        {
          // store values
          HistoryDatabase.putHistory(HISTORY_ID_FILENAME_FILTER_PATTERNS,
                                     widgetFileNameFilterText.getItems(),
                                     20,
                                     HistoryDatabase.Directions.DESCENDING,
                                     widgetFileNameFilterText.getText().trim()
                                    );
          HistoryDatabase.putHistory(HISTORY_ID_TEXT_FIND_PATTERNS,
                                     widgetTextFindPatterns.getItems(),
                                     20,
                                     HistoryDatabase.Directions.DESCENDING,
                                     widgetTextFindPatterns.getText().trim()
                                    );
          CommandFindFiles.fileNameFilterText      = widgetFileNameFilterText.getText().trim();
          CommandFindFiles.findNamesFlag           = widgetFindNames.getSelection();
          CommandFindFiles.findContentFlag         = widgetFindContent.getSelection();
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

  /** restart find files matching filters
   */
  private void restartFindFiles()
  {
    if (!dialog.isDisposed())
    {
      dialog.setText(widgetShowAllRepositories.getSelection()
                       ? "Find files"
                       : "Find files in '"+repositoryTab.repository.rootPath+"'"
                    );

      String findTextPattern = widgetTextFindPatterns.getText().trim().toLowerCase();
      synchronized(data)
      {
        data.fileNameFilterText      = widgetFileNameFilterText.getText().trim();
        data.findTextPattern         = findTextPattern;
        data.findNamesFlag           = widgetFindNames.getSelection();
        data.findContentFlag         = widgetFindContent.getSelection();
        data.showAllRepositoriesFlag = widgetShowAllRepositories.getSelection();
        data.showHiddenFilesFlag     = widgetShowHiddenFiles.getSelection();
        data.notifyAll();

        if (!findTextPattern.isEmpty()) Widgets.modified(data);
      }
    }
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
   * @return file name pattern array
   */
  private Pattern[] compileFileNamePatterns(String findTextPattern)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findTextPattern,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS))
    {
      patternList.add(Pattern.compile(StringUtils.globToRegex(pattern),Pattern.CASE_INSENSITIVE));
    }

    return patternList.toArray(new Pattern[patternList.size()]);
  }

  /** compile content patterns
   * @param findTextPattern find text string
   * @return file content pattern array
   */
  private Pattern[] compileContentPatterns(String findTextPattern)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findTextPattern,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS))
    {
      patternList.add(Pattern.compile(".*"+StringUtils.globToRegex(pattern)+".*",Pattern.CASE_INSENSITIVE));
    }

    return patternList.toArray(new Pattern[patternList.size()]);
  }

  /** match patterns
   * @param patterns patterns to match
   * @param string string to match
   * @return true iff one pattern match with string
   */
  private boolean matchPatterns(Pattern[] patterns, String string)
  {
    for (Pattern pattern : patterns)
    {
      if (pattern.matcher(string).matches()) return true;
    }

    return false;
  }

  /** get selected find data
   * @param file file to check
   * @param string text to find
   * @param findContentPatterns content patterns
   * @return line number or -1 if not found
   */
  private int fileContains(File file, String string, Pattern[] findContentPatterns)
  {
    int lineNumber = -1;

    BufferedReader input = null;
    try
    {
      // open file
      input = new BufferedReader(new FileReader(file));

      // read file and find text
      string = string.toLowerCase();
      String line;
      int    n = 0;
      while ((line = input.readLine()) != null)
      {
        n++;
        if (   line.toLowerCase().contains(string)
            || matchPatterns(findContentPatterns,line)
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
