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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Pattern;

// graphics
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
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
     * @param fileData1, fileData2 file tree data to compare
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
    String  findText;
    boolean findNamesFlag;
    boolean findContentFlag;
    boolean showAllRepositoriesFlag;
    boolean showHiddenFilesFlag;
    boolean quitFlag;

    Data()
    {
      this.fileNameFilterText      = CommandFindFiles.fileNameFilterText;
      this.findText                = "";
      this.findNamesFlag           = CommandFindFiles.findNamesFlag;
      this.findContentFlag         = CommandFindFiles.findContentFlag;
      this.showAllRepositoriesFlag = CommandFindFiles.showAllRepositoriesFlag;
      this.showHiddenFilesFlag     = CommandFindFiles.showHiddenFilesFlag;
      this.quitFlag                = false;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // stored settings
  private static String                  fileNameFilterText      = "";
  private static boolean                 findNamesFlag           = true;
  private static boolean                 findContentFlag         = false;
  private static boolean                 showAllRepositoriesFlag = false;
  private static boolean                 showHiddenFilesFlag     = false;

  // global variable references
  private final RepositoryTab            repositoryTab;
  private final Display                  display;

  private final RepositoryTab[]          thisRepositoryTabs;
  private final RepositoryTab[]          allRepositoryTabs;

  // dialog
  private final Data                     data = new Data();
  private final Shell                    dialog;

  // widgets
  private final Table                    widgetFiles;
  private final Text                     widgetFileNameFilterText;
  private final Text                     widgetFindText;
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

  /** view command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view
   * @param revision revision to view
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

    // get display
    display = shell.getDisplay();

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
            FindData findData = getSelectedFile();
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
            FindData findData = getSelectedFile();
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
        label = Widgets.newLabel(subComposite,"File patterns:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFileNameFilterText = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_SEARCH|SWT.ICON_CANCEL);
        widgetFileNameFilterText.setText(data.fileNameFilterText);
        widgetFileNameFilterText.setMessage("Enter filter patterns");
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
          }
        });

        label = Widgets.newLabel(subComposite,"Text patterns:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetFindText = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_SEARCH|SWT.ICON_CANCEL);
        widgetFindText.setMessage("Enter find text pattern");
        Widgets.layout(widgetFindText,1,1,TableLayoutData.WE);
        widgetFindText.setToolTipText("Find file by name/content patterns. Use * and ? as wildcards. Use space as separator.");
        widgetFindText.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
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
          FindData findData = getSelectedFile();
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
          FindData findData = getSelectedFile();
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
          FindData findData = getSelectedFile();
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
          FindData findData = getSelectedFile();
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
          // store settings
          fileNameFilterText      = data.fileNameFilterText;
          findNamesFlag           = data.findNamesFlag;
          findContentFlag         = data.findContentFlag;
          showAllRepositoriesFlag = data.showAllRepositoriesFlag;
          showHiddenFilesFlag     = data.showHiddenFilesFlag;

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
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          Widgets.setFocus(widgetFindText);
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
    widgetFindText.addKeyListener(keyListener);
    widgetFindNames.addKeyListener(keyListener);
    widgetFindContent.addKeyListener(keyListener);
    widgetShowAllRepositories.addKeyListener(keyListener);
    widgetShowHiddenFiles.addKeyListener(keyListener);
    widgetFindText.addKeyListener(new KeyListener()
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

    // start find files
    Background.run(new BackgroundRunnable()
    {
      String    fileNameFilterText       = "";
      Pattern[] fileNameFilterPatterns   = null;
      String    findText                 = "";
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
               || ((data.findText != null) && !data.findText.equals(findText))
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
          if (!findText.isEmpty())
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
                            nameMatchFlag =    fileName.toLowerCase().contains(findText)
                                            || matchPatterns(findNamePatterns,fileName);
                          }
                          if (   (!findNamesFlag || nameMatchFlag)
                              && findContentFlag
                             )
                          {
                            lineNumber = fileContains(file,findText,findContentPatterns);
                          }
  //Dprintf.dprintf("fileName=%s findNamesFlag=%s nameMatchFlag=%s findContentFlag=%s %d == %s",fileName,findNamesFlag,nameMatchFlag,findContentFlag,lineNumber);

                          if (   (!findNamesFlag   || nameMatchFlag)
                              && (!findContentFlag || (lineNumber > 0))
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
            if (data.findText != null)
            {
              // get new find text/pattern
              fileNameFilterText     = new String(data.fileNameFilterText);
              fileNameFilterPatterns = compileFileNamePatterns(data.fileNameFilterText);
              findText               = new String(data.findText);
              findNamePatterns       = compileFileNamePatterns(data.findText);
              findContentPatterns    = compileContentPatterns(data.findText);

              // clear existing text
              data.findText = null;
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
      Widgets.setFocus(widgetFindText);
      Dialogs.run(dialog,new DialogRunnable()
      {
        public void done(Object result)
        {
          // store values
          fileNameFilterText      = widgetFileNameFilterText.getText().trim();
          findNamesFlag           = widgetFindNames.getSelection();
          findContentFlag         = widgetFindContent.getSelection();
          showAllRepositoriesFlag = widgetShowAllRepositories.getSelection();
          showHiddenFilesFlag     = widgetShowHiddenFiles.getSelection();

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

      String findText = widgetFindText.getText().trim().toLowerCase();
      synchronized(data)
      {
        data.fileNameFilterText      = widgetFileNameFilterText.getText().trim();
        data.findText                = findText;
        data.findNamesFlag           = widgetFindNames.getSelection();
        data.findContentFlag         = widgetFindContent.getSelection();
        data.showAllRepositoriesFlag = widgetShowAllRepositories.getSelection();
        data.showHiddenFilesFlag     = widgetShowHiddenFiles.getSelection();
        data.notifyAll();

        if (!findText.isEmpty()) Widgets.modified(data);
      }
    }
  }

  /** get selected file data set
   * @return file data set
   */
  private HashSet<File> getSelectedFileSet()
  {
    HashSet<File> fileSet = new HashSet<File>();

    for (TableItem tableItem : widgetFiles.getSelection())
    {
      fileSet.add((File)tableItem.getData());
    }

    return (fileSet.size() > 0) ? fileSet : null;
  }

  /** get selected find data
   * @return find data or null
   */
  private FindData getSelectedFile()
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
   * @param findText find text string
   * @return file name pattern array
   */
  private Pattern[] compileFileNamePatterns(String findText)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findText))
    {
      patternList.add(Pattern.compile(StringUtils.globToRegex(pattern),Pattern.CASE_INSENSITIVE));
    }

    return patternList.toArray(new Pattern[patternList.size()]);
  }

  /** compile content patterns
   * @param findText find text string
   * @return file content pattern array
   */
  private Pattern[] compileContentPatterns(String findText)
  {
    ArrayList<Pattern> patternList = new ArrayList<Pattern>();
    for (String pattern : StringUtils.split(findText))
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
