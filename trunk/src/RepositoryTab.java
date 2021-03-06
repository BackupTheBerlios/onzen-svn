/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: repository tab
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.WeakHashMap;

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
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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

/****************************** Classes ********************************/

/** repository tab
 */
class RepositoryTab
{
  /** file data comparator
   */
  class FileDataComparator implements Comparator<FileData>
  {
    // Note: enum in inner classes are not possible in Java, thus use the old way...
    private final static int SORTMODE_NAME = 0;
    private final static int SORTMODE_TYPE = 1;
    private final static int SORTMODE_SIZE = 2;
    private final static int SORTMODE_DATE = 3;

    private int sortMode;

    /** create file data comparator
     * @param sortMode sort mode
     */
    FileDataComparator(int sortMode)
    {
      this.sortMode = sortMode;
    }

    /** create file data comparator
     * @param tree file tree
     * @param sortColumn column to sort
     */
    FileDataComparator(Tree tree, TreeColumn sortColumn)
    {
      if      (tree.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
      else if (tree.getColumn(1) == sortColumn) sortMode = SORTMODE_TYPE;
      else if (tree.getColumn(2) == sortColumn) sortMode = SORTMODE_SIZE;
      else if (tree.getColumn(3) == sortColumn) sortMode = SORTMODE_DATE;
      else                                      sortMode = SORTMODE_NAME;
    }

    /** create file data comparator
     * @param tree file tree
     */
    FileDataComparator(Tree tree)
    {
      this(tree,tree.getSortColumn());
    }

    /** compare file tree data without taking care about type
     * @param fileData1, fileData2 file tree data to compare
     * @return -1 iff fileData1 < fileData2,
                0 iff fileData1 = fileData2,
                1 iff fileData1 > fileData2
     */
    private int compareWithoutType(FileData fileData1, FileData fileData2)
    {
      switch (sortMode)
      {
        case SORTMODE_NAME:
          return fileData1.name.compareTo(fileData2.name);
        case SORTMODE_TYPE:
          return fileData1.type.compareTo(fileData2.type);
        case SORTMODE_SIZE:
          if      (fileData1.size < fileData2.size) return -1;
          else if (fileData1.size > fileData2.size) return  1;
          else                                      return  0;
        case SORTMODE_DATE:
          if      (fileData1.date.before(fileData2.date)) return -1;
          else if (fileData1.date.after(fileData2.date))  return  1;
          else                                            return  0;
        default:
          return 0;
      }
    }

    /** compare file tree data
     * @param fileData1, fileData2 file tree data to compare
     * @return -1 iff fileData1 < fileData2,
                0 iff fileData1 = fileData2,
                1 iff fileData1 > fileData2
     */
    public int compare(FileData fileData1, FileData fileData2)
    {
      if ((fileData1 != null) && (fileData2 != null))
      {
        if (fileData1.type == FileData.Types.DIRECTORY)
        {
          if (fileData2.type == FileData.Types.DIRECTORY)
          {
            return compareWithoutType(fileData1,fileData2);
          }
          else
          {
            return -1;
          }
        }
        else
        {
          if (fileData2.type == FileData.Types.DIRECTORY)
          {
            return 1;
          }
          else
          {
            return compareWithoutType(fileData1,fileData2);
          }
        }
      }
      else
      {
        return 0;
      }
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "FileDataComparator {"+sortMode+"}";
    }
  }

  // --------------------------- constants --------------------------------

  // colors
  public static Color COLOR_OK;
  public static Color COLOR_UNKNOWN;
  public static Color COLOR_MODIFIED;
  public static Color COLOR_CHECKOUT;
  public static Color COLOR_UPDATE;
  public static Color COLOR_MERGE;
  public static Color COLOR_CONFLICT;
  public static Color COLOR_ADDED;
  public static Color COLOR_REMOVED;
  public static Color COLOR_NOT_EXISTS;
  public static Color COLOR_WAITING;
  public static Color COLOR_ERROR;
  public static Color COLOR_UPDATE_STATUS;
  public static Color COLOR_IGNORE;

  public enum ConvertWhitespacesResult
  {
    OK,
    ALL,
    FAIL,
    ABORTED
  };

  // --------------------------- variables --------------------------------
  // repository
  public  final Repository    repository;           // repository instance of tab

  // global variable references
  protected final Onzen       onzen;
  private final Shell         shell;
  private final Display       display;
  private final Clipboard     clipboard;

  // widgets
  private final TabFolder     widgetTabFolder;
  public  final Composite     widgetComposite;
  private final Tree          widgetFileTree;
  private final Menu          menuOpenFileWithCommands;
  private final Menu          menuShellCommands;

  // map file name -> tree item
  private WeakHashMap<String,TreeItem> fileNameMap = new WeakHashMap<String,TreeItem>();

  // last push/pull repository URL
  private RepositoryURL lastRepositoryURL = new RepositoryURL();

  // last copy destination directory
  private static String       lastDirectory = "";

  // last convert whitespaces dialog location
  private static Point        convertWhiteDialogLocation = new Point(SWT.DEFAULT,SWT.DEFAULT);

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository tab
   * @param onzen Onzen instance
   * @param parentTabFolder parent tab folder
   * @param leftRepositoryTab repository tab to left or null
   * @param repository repository
   */
  RepositoryTab(final Onzen onzen, TabFolder parentTabFolder, RepositoryTab leftRepositoryTab, Repository repository)
  {
    Menu       menu;
    MenuItem   menuItem;
    Composite  composite;
    Label      label;
    Button     button;
    TreeColumn treeColumn;
    TreeItem   treeItem;
    Text       text;

    // initialize variables
    this.onzen           = onzen;
    this.repository      = repository;
    this.widgetTabFolder = parentTabFolder;

    // get shell, display, clipboard
    shell     = parentTabFolder.getShell();
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // init colors
    COLOR_OK            = new Color(display,Settings.colorStatusOK.background          );
    COLOR_UNKNOWN       = new Color(display,Settings.colorStatusUnknown.background     );
    COLOR_MODIFIED      = new Color(display,Settings.colorStatusModified.background    );
    COLOR_CHECKOUT      = new Color(display,Settings.colorStatusCheckout.background    );
    COLOR_UPDATE        = new Color(display,Settings.colorStatusUpdate.background      );
    COLOR_MERGE         = new Color(display,Settings.colorStatusMerge.background       );
    COLOR_CONFLICT      = new Color(display,Settings.colorStatusConflict.background    );
    COLOR_ADDED         = new Color(display,Settings.colorStatusAdded.background       );
    COLOR_REMOVED       = new Color(display,Settings.colorStatusRemoved.background     );
    COLOR_NOT_EXISTS    = new Color(display,Settings.colorStatusNotExists.background   );
    COLOR_WAITING       = new Color(display,Settings.colorStatusWaiting.background     );
    COLOR_ERROR         = new Color(display,Settings.colorStatusError.background       );
    COLOR_UPDATE_STATUS = new Color(display,Settings.colorStatusUpdateStatus.foreground);
    COLOR_IGNORE        = new Color(display,Settings.colorStatusIgnore.foreground      );

    // create tab
    widgetComposite = Widgets.insertTab(parentTabFolder,
                                        (leftRepositoryTab != null)?leftRepositoryTab.widgetComposite:null,
                                        repository.title,
                                        this
                                       );
//Dprintf.dprintf("");
//widgetComposite.setBackground(Onzen.COLOR_YELLOW);
//Dprintf.dprintf("");
    widgetComposite.setLayout(new TableLayout(1.0,1.0,2));
    Widgets.layout(widgetComposite,0,0,TableLayoutData.NSWE);
    {
      // file tree
      widgetFileTree = Widgets.newTree(widgetComposite,SWT.MULTI);
      Widgets.layout(widgetFileTree,0,0,TableLayoutData.NSWE);
      SelectionListener fileTreeColumnSelectionListener = new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          TreeColumn         treeColumn         = (TreeColumn)selectionEvent.widget;
          FileDataComparator fileDataComparator = new FileDataComparator(widgetFileTree,treeColumn);

          synchronized(widgetFileTree)
          {
            Widgets.sortTreeColumn(widgetFileTree,treeColumn,fileDataComparator);
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      };
      Listener fileTreeColumnResizeListener = new Listener()
      {
        public void handleEvent(Event event)
        {
          Settings.geometryMainColumns = new Settings.ColumnSizes(Widgets.getTreeColumnWidth(widgetFileTree));
        }
      };
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Name",    SWT.LEFT,390,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.addListener(SWT.Resize,fileTreeColumnResizeListener);
      treeColumn.setToolTipText("Click to sort for name.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Status",  SWT.LEFT,160,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.addListener(SWT.Resize,fileTreeColumnResizeListener);
      treeColumn.setToolTipText("Click to sort for status.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Revision",SWT.LEFT,100,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.addListener(SWT.Resize,fileTreeColumnResizeListener);
      treeColumn.setToolTipText("Click to sort for revision.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Branch",  SWT.LEFT,100,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.addListener(SWT.Resize,fileTreeColumnResizeListener);
      treeColumn.setToolTipText("Click to sort for branch.");
      Widgets.setTreeColumnWidth(widgetFileTree,Settings.geometryMainColumns.width);
      widgetFileTree.addListener(SWT.PaintItem, new Listener()
      {
        public void handleEvent(Event event)
        {
          final int HEIGHT = Onzen.IMAGE_LOCK.getBounds().height;

          TreeItem item = (TreeItem)event.item;

          if (event.index == 1)
          {
            Object data = item.getData();
            if (data instanceof FileData)
            {
              FileData fileData = (FileData)item.getData();

              if (fileData.locked)
              {
                event.gc.drawImage(Onzen.IMAGE_LOCK, event.x+event.width, event.y+(event.height-HEIGHT)/2);
              }
            }
          }
        }
      });

      menu = Widgets.newPopupMenu(shell);
      {
        menuItem = Widgets.addMenuItem(menu,"Open file\u2026");
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
              openFile(fileData);
            }
          }
        });

        menuOpenFileWithCommands = Widgets.addMenu(menu,"Open file with");//,Settings.keyOpenFileWith);
        menuItem = Widgets.addMenuItem(menuOpenFileWithCommands,"\u2026");
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
              openFileWith(fileData);
            }
          }
        });

        menuShellCommands = Widgets.addMenu(menu,"Shell");
        Widgets.addMenuSeparator(menuShellCommands);
        menuItem = Widgets.addMenuItem(menuShellCommands,"Add new command\u2026");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            onzen.addShellCommand();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Copy to\u2026",Settings.keyCopyFilesTo);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            copyFilesTo();
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
            update();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Update all",Settings.keyUpdateAll);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            updateAll();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Commit\u2026",Settings.keyCommit);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            commit();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Create patch\u2026",Settings.keyCreatePatch);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            createPatch();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Revert\u2026",Settings.keyRevert);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            revert();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Resolve\u2026",Settings.keyResolve);
// NYI
menuItem.setEnabled(false);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
Dprintf.dprintf("NYI");
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Set resolved",Settings.keySetResolved);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            setResolved();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Lock",Settings.keyLock);
        menuItem.setEnabled(repository.supportLockUnlock());
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            lock();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Unlock",Settings.keyUnlock);
        menuItem.setEnabled(repository.supportLockUnlock());
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            unlock();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Add\u2026",Settings.keyAdd);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            add();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Remove\u2026",Settings.keyRemove);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            remove();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Copy\u2026",Settings.keyCopy);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            copy();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Rename/move\u2026",Settings.keyRename);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            rename();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Set file mode\u2026",Settings.keySetFileMode);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            setFileMode();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Revisions\u2026",Settings.keyRevisions);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            revisions();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Revision info\u2026",Settings.keyRevisionInfo);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            revisionInfo();
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
            diff();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Annotations\u2026",Settings.keyAnnotations);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            annotations();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"New file\u2026",Settings.keyNewFile);
menuItem.setEnabled(false);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            newFile();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"New directory\u2026",Settings.keyNewDirectory);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            newDirectory();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Rename/move local file/directory\u2026",Settings.keyRenameLocal);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            renameLocalFile();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Delete local files/directories\u2026",Settings.keyDeleteLocal);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            deleteLocalFiles();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Add to file ignore list",Settings.keyAddIgnoreFile);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            addIgnoreFiles();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Add to hidden list",Settings.keyAddHiddenFile);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            addHiddenFiles();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Convert whitespaces\u2026",Settings.keyConvertWhitespaces);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            convertWhitespaces();
          }
        });
      }
      widgetFileTree.setMenu(menu);
    }

    // add root directory
    addRootDirectory();
  }

  /** create repository tab
   * @param onzen Onzen instance
   * @param parentTabFolder parent tab folder
   * @param repository repository
   */
  RepositoryTab(Onzen onzen, TabFolder parentTabFolder, Repository repository)
  {
    this(onzen,parentTabFolder,null,repository);
  }

  /** get repository
   * @param rootPath repository root path
   * @return repository or null if not found
   */
  public Repository getRepository(String rootPath)
  {
    return onzen.getRepository(rootPath);
  }

  /** get repository tab
   * @param rootPath repository root path
   * @return repository tab or null if not found
   */
  public RepositoryTab getRepositoryTab(String rootPath)
  {
    return onzen.getRepositoryTab(rootPath);
  }

  /** get title
   */
  public String getTitle()
  {
    return repository.title;
  }

  /** set title
   * @param title title text to set
   */
  public void setTitle(String title)
  {
    repository.title = title;
    Widgets.setTabTitle(widgetTabFolder,widgetComposite,title);
  }

  /** close repository tab
   */
  public void close()
  {
    Widgets.removeTab(widgetTabFolder,widgetComposite);
  }

  /** move repository tab
   * @param index tab index (0..n-1)
   */
  public void move(int index)
  {
    Widgets.moveTab(widgetTabFolder,widgetComposite,index);
  }

  /** show repository tab
   */
  public void show()
  {
    Widgets.showTab(widgetTabFolder,widgetComposite);
  }

  /** open directory in file-tree
   * @param directory directory to open
   * @return true iff directory opened
   */
  public boolean openDirectory(String directory)
  {
    if (!widgetFileTree.isDisposed())
    {
      return openSubDirectory(widgetFileTree.getItems(),directory);
    }
    else
    {
      return false;
    }
  }

  /** open directory in file-tree
   * @param directory directory to open
   * @return true iff directory opened
   */
  public boolean openDirectory(File file)
  {
    return openDirectory(repository.getFileName(file.getParent()));
  }

  /** set status text
   * @param format format string
   * @param arguments optional arguments
   */
  public void setStatusText(String format, Object... arguments)
  {
    onzen.setStatusText(format,arguments);
  }

  /** clear status text
   */
  public void clearStatusText()
  {
    onzen.clearStatusText();
  }

  /** update open-file-with commands in context menu
   */
  public void updateOpenFileWithCommands()
  {
    MenuItem menuItem;

    if (!menuShellCommands.isDisposed())
    {
      // remove old entries in open-file-with command menu
      MenuItem[] menuItems = menuOpenFileWithCommands.getItems();
      for (int i = 0; i < menuItems.length-1; i++)
      {
        menuItems[0].dispose();
      }

      // add new open-file-with commands to menu
      HashMap<String,Settings.Editor> commandMap = new HashMap<String,Settings.Editor>();
      for (Settings.Editor editor : Settings.editors)
      {
        String name = !editor.name.isEmpty() ? editor.name : editor.commandLine;
        commandMap.put(name,editor);
      }

      String[] names = commandMap.keySet().toArray(new String[0]);
      Arrays.sort(names);
      for (int i = 0; i < names.length; i++)
      {
        menuItem = Widgets.addMenuItem(menuOpenFileWithCommands,names[i],i,SWT.NONE);
        menuItem.setData(commandMap.get(names[i]));
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            MenuItem        widget = (MenuItem)selectionEvent.widget;
            Settings.Editor editor = (Settings.Editor)widget.getData();

            openFileWith(editor.commandLine);
          }
        });
      }
    }
  }

  /** update shell commands in context menu
   */
  public void updateShellCommands()
  {
    MenuItem menuItem;

    if (!menuShellCommands.isDisposed())
    {
      // remove old entries in shell command menu
      MenuItem[] menuItems = menuShellCommands.getItems();
      for (int i = 0; i < menuItems.length; i++)
      {
        menuItems[i].dispose();
      }

      // add new shell commands to menu
      for (Settings.ShellCommand shellCommand : Settings.shellCommands)
      {
        menuItem = Widgets.addMenuItem(menuShellCommands,shellCommand.name);
        menuItem.setData(shellCommand);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            MenuItem              widget       = (MenuItem)selectionEvent.widget;
            Settings.ShellCommand shellCommand = (Settings.ShellCommand)widget.getData();

            executeShellCommand(shellCommand);
          }
        });
      }
      Widgets.addMenuSeparator(menuShellCommands);
      menuItem = Widgets.addMenuItem(menuShellCommands,"Add new command\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          onzen.addShellCommand();
        }
      });
    }
  }

  //-----------------------------------------------------------------------

  /** update states of selected entries/current directory
   */
  public void updateStates()
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    TreeItem[] selectedTreeItems = widgetFileTree.getSelection();
    if (selectedTreeItems.length > 0)
    {
      // get file data (selected and all in same directory)
      for (TreeItem selectedTreeItem : selectedTreeItems)
      {
        fileDataSet.add((FileData)selectedTreeItem.getData());

        TreeItem parentTreeItem = selectedTreeItem.getParentItem();
        if (parentTreeItem != null)
        {
          for (TreeItem treeItem : parentTreeItem.getItems())
          {
            fileDataSet.add((FileData)treeItem.getData());
          }
        }
      }
    }
    else
    {
      // get file data of all open directories
      for (TreeItem treeItem : Widgets.getTreeItems(widgetFileTree))
      {
        FileData fileData = (FileData)treeItem.getData();
//Dprintf.dprintf("treeItem=%s: %s",treeItem,(FileData)treeItem.getData());
        if (fileData != null) fileDataSet.add(fileData);
      }
    }

    // start update file data
    asyncUpdateFileStates(fileDataSet);
  }

  /** copy selected entries to directory
   */
  public void copyFilesTo()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();

    String directory = Dialogs.directory(shell,
                                         "Select destination directory","Copy "+fileDataSet.size()+" file(s)/directory to:",
                                         lastDirectory
                                        );
    if (directory != null)
    {
      final BusyDialog busyDialog = new BusyDialog(shell,
                                                   "Copy files to '"+directory+"'...",
                                                   400,200,
                                                   BusyDialog.LIST
                                                  );
      busyDialog.autoAnimate();
      try
      {
        for (FileData fileData : fileDataSet)
        {
          setStatusText("Copy file '%s' -> '%s'...",fileData.getFileName(),directory);
          try
          {
            File fromFile = new File(fileData.getFileName(repository));
            File toFile   = new File(directory);

            File newFile = new File(toFile,fromFile.getName());
            if (newFile.exists())
            {
              if (!Dialogs.confirm(shell,"Confirmation",String.format("File\n\n'%s'\n\nexists - overwrite?",newFile.getPath())))
              {
                return;
              }
            }

            FileUtils.copyFile(fromFile,
                               toFile,
                               new FileCopyConfirmation()
                               {
                                 public boolean confirm(File file)
                                 {
                                   busyDialog.updateList(file.getPath());
                                   return true;
                                 }
                               }
                              );
            if (busyDialog.isAborted()) break;
          }
          catch (IOException exception)
          {
            Dialogs.error(shell,"Copy file '"+fileData.getBaseName()+"' fail (error: %s)",exception.getMessage());
            return;
          }
          finally
          {
            clearStatusText();
          }
        }
      }
      finally
      {
        busyDialog.done();
      }

      // start update file data
      asyncUpdateSelectedFileData();

      lastDirectory = directory;
    }
  }

  /** update all open or selected entries
   */
  public void update()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();

    setStatusText("Update files...");
    try
    {
      repository.update(fileDataSet);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,exception.getExtendedMessage(),"Update fail (error: %s)",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
    }

    // start update file data
    asyncUpdateFileStates(fileDataSet);
  }

  /** update all or selected entries
   */
  public void updateAll()
  {
    final HashSet<FileData> fileDataSet = getAllFileDataSet();

    BusyDialog busyDialog = new BusyDialog(shell,
                                           "Update all directories and files...",
                                           400,200,
                                           BusyDialog.LIST
                                          );
    busyDialog.autoAnimate();

    Background.run(new BackgroundRunnable(repository,fileDataSet,busyDialog)
    {
      public void run(Repository repository, HashSet<FileData> fileDataSet, final BusyDialog busyDialog)
      {
        setStatusText("Update all directories and files...");
        try
        {
          repository.updateAll(busyDialog);
        }
        catch (RepositoryException exception)
        {
          final String   exceptionMessage         = exception.getMessage();
          final String[] exceptionExtendedMessage = exception.getExtendedMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(shell,exceptionExtendedMessage,"Update all fail (error: %s)",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              busyDialog.close();
            }
          });
          clearStatusText();
        }

        updateFileStates(fileDataSet);
      }
    });
  }

  /** commit selected entries
   */
  public void commit()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandCommit commandCommit = new CommandCommit(shell,this,fileDataSet);
      commandCommit.run();
    }
  }

  /** create patch for selected entries
   */
  public void createPatch()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandCreatePatch commandCreatePatch = new CommandCreatePatch(shell,this,fileDataSet);
      commandCreatePatch.run();
    }
  }

  /** show/edit patches
   */
  public void patches()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandPatches commandPatches = new CommandPatches(shell,this);
      commandPatches.run();
    }
  }

  /** add selected entries
   */
  public void add()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandAdd commandAdd = new CommandAdd(shell,this,fileDataSet);
      commandAdd.run();
    }
  }

  /** remove selected entries
   */
  public void remove()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandRemove commandRemove = new CommandRemove(shell,this,fileDataSet);
      commandRemove.run();
    }
  }

  /** copy selected entries
   */
  public void copy()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandCopy commandCopy = new CommandCopy(shell,this,fileDataSet);
      commandCopy.run();
    }
  }

  /** rename selected entry
   */
  public void rename()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandRename commandRename = new CommandRename(shell,this,fileData);
      commandRename.run();
    }
  }

  /** resolve conflicts
   */
  public void revert()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandRevert commandRevert = new CommandRevert(shell,this,fileDataSet);
      commandRevert.run();
    }
  }

  /** resolve conflicts
   */
  public void resolve()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
// NYI
Dprintf.dprintf("NYI");
//      CommandRename commandRename = new CommandRename(shell,this,fileData);
//      commandRename.run();
    }
  }

  /** set conflicts resolved
   */
  public void setResolved()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      setStatusText("Set conflicts resolved...");
      try
      {
        repository.resolve(fileDataSet);
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,exception.getExtendedMessage(),"Set conflicts resolved fail: %s",exception.getMessage());
        return;
      }
      finally
      {
        clearStatusText();
      }

      // get all entries in directories for update (files may be added or disappear after set solved)
      HashSet<FileData> updateFileDataSet = new HashSet<FileData>();
      for (FileData fileData : fileDataSet)
      {
        TreeItem parentTreeItem = fileNameMap.get(fileData.getFileName()).getParentItem();

        for (TreeItem treeItem : parentTreeItem.getItems())
        {
          FileData subFileData = (FileData)treeItem.getData();
           if (subFileData != null) updateFileDataSet.add(subFileData);
        }
      }

      // start update file data
      asyncUpdateFileStates(updateFileDataSet);
    }
  }

  /** apply patches
   */
  public void applyPatches()
  {
    setStatusText("Apply patches...");
    try
    {
      repository.applyPatches();
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,exception.getExtendedMessage(),"Apply patches fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
    }
  }

  /** unapply patches
   */
  public void unapplyPatches()
  {
    setStatusText("Unapply patches...");
    try
    {
      repository.unapplyPatches();
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,exception.getExtendedMessage(),"Unapply patches fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
    }
  }

  /** show incoming changes
   * @param masterRepository master repository or ""
   */
  public void incomingChanges(String masterRepository)
  {
    CommandChanges commandChanges = new CommandChanges(shell,this,masterRepository,CommandChanges.ChangesTypes.INCOMING);
    commandChanges.run();
  }

  /** show incoming changes
   */
  public void incomingChanges()
  {
//TODO
    incomingChanges(repository.getMasterRepositoryURL().path);
  }

  /** show outgoing changes
   * @param masterRepository master repository or ""
   */
  public void outgoingChanges(String masterRepository)
  {
    CommandChanges commandChanges = new CommandChanges(shell,this,masterRepository,CommandChanges.ChangesTypes.OUTGOING);
    commandChanges.run();
  }

  /** show outgoing changes
   */
  public void outgoingChanges()
  {
//TODO
    outgoingChanges(repository.getMasterRepositoryURL().path);
  }

  /** pull changes
   * @param masterRepository master repository or ""
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pullChanges(String masterRepository, String moduleName, String userName, String password)
  {
    setStatusText("Pull changes...");
    try
    {
      RepositoryURL repositoryURL = new RepositoryURL(RepositoryURL.Types.MANUAL,
                                                      repository.getType(),
                                                      masterRepository
                                                     );
      repositoryURL.addToHistory();

      repository.pullChanges(masterRepository,moduleName,userName,password);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,exception.getExtendedMessage(),"Pulling changes fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
    }
  }

  /** pull changes
   */
  public void pullChanges()
  {
    /** dialog data
     */
    class Data
    {
      ArrayList<RepositoryURL> repositoryURLList;
      String                   path;
      String                   moduleName;
      String                   userName;
      String                   password;

      Data()
      {
        this.repositoryURLList = new ArrayList<RepositoryURL>();
        this.path              = null;
        this.moduleName        = "";
        this.userName          = "";
        this.password          = null;

        repositoryURLList.add(new RepositoryURL("file://"));
        repositoryURLList.add(new RepositoryURL("ssh://"));
        repositoryURLList.add(new RepositoryURL("http://"));
        repositoryURLList.add(new RepositoryURL("https://"));
        repositoryURLList.add(new RepositoryURL("rsync://"));
        for (RepositoryURL repositoryURL : RepositoryURL.getHistoryURLs())
        {
          repositoryURLList.add(repositoryURL);
        }
        for (RepositoryURL repositoryURL : Settings.repositoryURLs)
        {
          repositoryURLList.add(repositoryURL);
        }
      }
    }

    Composite composite,subComposite;
    Label     label;
    Button    button;

    final Data  data   = new Data();
    final Shell dialog = Dialogs.openModal(shell,"Select repository for pull changes",500,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Combo  widgetPath;
    final Combo  widgetModuleName;
    final Text   widgetUserName;
    final Button widgetPullChanges;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Repository:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,0,1,TableLayoutData.NSWE);
      {
        widgetPath = Widgets.newCombo(subComposite);
        Widgets.layout(widgetPath,0,0,TableLayoutData.WE);
        widgetPath.setToolTipText("Respository path URI.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                            "Select repository path",
                                            widgetPath.getText()
                                           );
            if (path != null)
            {
              widgetPath.setText(path);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Module:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      widgetModuleName = Widgets.newCombo(composite);
      widgetModuleName.setText(data.moduleName);
      Widgets.layout(widgetModuleName,1,1,TableLayoutData.WE);
      widgetModuleName.setToolTipText("Module name in repository.");
      Widgets.addModifyListener(new WidgetModifyListener(widgetModuleName,data)
      {
        public void modified(Combo combo)
        {
          combo.setText(data.moduleName);
        }
      });

      label = Widgets.newLabel(composite,"User name:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      widgetUserName = Widgets.newText(composite);
      widgetUserName.setText(data.userName);
      Widgets.layout(widgetUserName,2,1,TableLayoutData.WE);
      widgetUserName.setToolTipText("Check-out user name.");
      Widgets.addModifyListener(new WidgetModifyListener(widgetUserName,data)
      {
        public void modified(Text text)
        {
          text.setText(data.userName);
        }
      });
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetPullChanges = Widgets.newButton(composite,"Pull changes");
      widgetPullChanges.setEnabled(!widgetPath.getText().trim().isEmpty());
      Widgets.layout(widgetPullChanges,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetPullChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String path     = widgetPath.getText().trim();
          String userName = widgetUserName.getText();
          String password = onzen.getPassword(userName,path,true);
          if (password != null)
          {
            data.path       = path;
            data.moduleName = widgetModuleName.getText().trim();
            data.userName   = userName;
            data.password   = password;

            lastRepositoryURL = new RepositoryURL(data.path,data.moduleName,data.userName);

            Dialogs.close(dialog,true);
          }
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetPath.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetPullChanges.setEnabled(!widgetPath.getText().trim().isEmpty());
      }
    });
    widgetPath.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          RepositoryURL repositoryURL = (RepositoryURL)Widgets.getComboEntry(widget,index);

          synchronized(data)
          {
            widgetPath.setText(repositoryURL.path);
            widgetModuleName.setText(repositoryURL.moduleName);
            widgetUserName.setText(repositoryURL.userName);

            data.path       = repositoryURL.path;
            data.moduleName = repositoryURL.moduleName;
            data.userName   = repositoryURL.userName;
            Widgets.modified(data);
          }
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          RepositoryURL repositoryURL = (RepositoryURL)Widgets.getComboEntry(widget,index);

          synchronized(data)
          {
            widgetPath.setText(repositoryURL.path);
            widgetModuleName.setText(repositoryURL.moduleName);
            widgetUserName.setText(repositoryURL.userName);

            data.path       = repositoryURL.path;
            data.moduleName = repositoryURL.moduleName;
            data.userName   = repositoryURL.userName;
            Widgets.modified(data);
          }
        }
      }
    });

    // focus traversal
    Widgets.setNextFocus(widgetPath,
                         widgetModuleName,
                         widgetUserName,
                         widgetPullChanges
                        );

    // add repository URLs
    for (RepositoryURL repositoryURL : data.repositoryURLList)
    {
      Widgets.addComboEntry(widgetPath,repositoryURL,repositoryURL.path);
    }
    widgetPath.setText(lastRepositoryURL.path);
    widgetModuleName.setText(lastRepositoryURL.moduleName);
    widgetUserName.setText(lastRepositoryURL.userName);

    // run dialog
    if ((Boolean)Dialogs.run(dialog,false))
    {
      pullChanges(data.path,data.moduleName,data.userName,data.password);
    }
  }

  /** push changes
   * @param masterRepository master repository or ""
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pushChanges(String masterRepository, String moduleName, String userName, String password)
  {
    setStatusText("Push changes...");
    try
    {
      RepositoryURL repositoryURL = new RepositoryURL(repository.getType(),
                                                      masterRepository,
                                                      moduleName,
                                                      userName
                                                     );
      repositoryURL.addToHistory();

      repository.pushChanges(masterRepository,moduleName,userName,password);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,exception.getExtendedMessage(),"Pushing changes fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
    }
  }

  /** push changes
   */
  public void pushChanges()
  {
    /** dialog data
     */
    class Data
    {
      ArrayList<RepositoryURL> repositoryURLList;
      String                   path;
      String                   moduleName;
      String                   userName;
      String                   password;

      Data()
      {
        this.repositoryURLList = new ArrayList<RepositoryURL>();
        this.path              = null;
        this.moduleName        = "";
        this.userName          = "";
        this.password          = null;

        repositoryURLList.add(new RepositoryURL("file://"));
        repositoryURLList.add(new RepositoryURL("ssh://"));
        repositoryURLList.add(new RepositoryURL("http://"));
        repositoryURLList.add(new RepositoryURL("https://"));
        repositoryURLList.add(new RepositoryURL("rsync://"));
        for (RepositoryURL repositoryURL : RepositoryURL.getHistoryURLs())
        {
          repositoryURLList.add(repositoryURL);
        }
        for (RepositoryURL repositoryURL : Settings.repositoryURLs)
        {
          repositoryURLList.add(repositoryURL);
        }
      }
    }

    Composite composite,subComposite;
    Label     label;
    Button    button;

    final Data  data   = new Data();
    final Shell dialog = Dialogs.openModal(shell,"Select repository for push changes",500,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Combo  widgetPath;
    final Combo  widgetModuleName;
    final Text   widgetUserName;
    final Button widgetPushChanges;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Repository:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,0,1,TableLayoutData.NSWE);
      {
        widgetPath = Widgets.newCombo(subComposite);
        Widgets.layout(widgetPath,0,0,TableLayoutData.WE);
        widgetPath.setToolTipText("Respository path URI.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                            "Select repository path",
                                            widgetPath.getText()
                                           );
            if (path != null)
            {
              widgetPath.setText(path);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Module:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      widgetModuleName = Widgets.newCombo(composite);
      widgetModuleName.setText(data.moduleName);
      Widgets.layout(widgetModuleName,1,1,TableLayoutData.WE);
      widgetModuleName.setToolTipText("Module name in repository.");
      Widgets.addModifyListener(new WidgetModifyListener(widgetModuleName,data)
      {
        public void modified(Combo combo)
        {
          combo.setText(data.moduleName);
        }
      });

      label = Widgets.newLabel(composite,"User name:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      widgetUserName = Widgets.newText(composite);
      widgetUserName.setText(data.userName);
      Widgets.layout(widgetUserName,2,1,TableLayoutData.WE);
      widgetUserName.setToolTipText("Check-out user name.");
      Widgets.addModifyListener(new WidgetModifyListener(widgetUserName,data)
      {
        public void modified(Text text)
        {
          text.setText(data.userName);
        }
      });
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetPushChanges = Widgets.newButton(composite,"Push changes");
      widgetPushChanges.setEnabled(!widgetPath.getText().trim().isEmpty());
      Widgets.layout(widgetPushChanges,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetPushChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String path     = widgetPath.getText().trim();
          String userName = widgetUserName.getText();
          String password = onzen.getPassword(userName,path,true);
          if (password != null)
          {
            data.path       = path;
            data.moduleName = widgetModuleName.getText().trim();
            data.userName   = userName;
            data.password   = password;

            lastRepositoryURL = new RepositoryURL(data.path,data.moduleName,data.userName);

            Dialogs.close(dialog,true);
          }
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetPath.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetPushChanges.setEnabled(!widgetPath.getText().trim().isEmpty());
      }
    });
    widgetPath.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          RepositoryURL repositoryURL = (RepositoryURL)Widgets.getComboEntry(widget,index);

          synchronized(data)
          {
            widgetPath.setText(repositoryURL.path);
            widgetModuleName.setText(repositoryURL.moduleName);
            widgetUserName.setText(repositoryURL.userName);

            data.path       = repositoryURL.path;
            data.moduleName = repositoryURL.moduleName;
            data.userName   = repositoryURL.userName;
            Widgets.modified(data);
          }
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          RepositoryURL repositoryURL = (RepositoryURL)Widgets.getComboEntry(widget,index);

          synchronized(data)
          {
            widgetPath.setText(repositoryURL.path);
            widgetModuleName.setText(repositoryURL.moduleName);
            widgetUserName.setText(repositoryURL.userName);

            data.path       = repositoryURL.path;
            data.moduleName = repositoryURL.moduleName;
            data.userName   = repositoryURL.userName;
            Widgets.modified(data);
          }
        }
      }
    });

    // focus traversal
    Widgets.setNextFocus(widgetPath,
                         widgetModuleName,
                         widgetUserName,
                         widgetPushChanges
                        );

    // add repository URLs
    for (RepositoryURL repositoryURL : data.repositoryURLList)
    {
      Widgets.addComboEntry(widgetPath,repositoryURL,repositoryURL.path);
    }
    widgetPath.setText(lastRepositoryURL.path);
    widgetModuleName.setText(lastRepositoryURL.moduleName);
    widgetUserName.setText(lastRepositoryURL.userName);

    // run dialog
    if ((Boolean)Dialogs.run(dialog,false))
    {
      pushChanges(data.path,data.moduleName,data.userName,data.password);
    }
  }

  /** lock file
   */
  public void lock()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      setStatusText("Lock...");
      try
      {
        repository.lock(fileDataSet);
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,exception.getExtendedMessage(),"Lock fail: %s",exception.getMessage());
        return;
      }
      finally
      {
        clearStatusText();
      }

      // start update file data
      asyncUpdateSelectedFileData();
    }
  }

  /** unlock file
   */
  public void unlock()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      setStatusText("Unlock...");
      try
      {
        repository.unlock(fileDataSet);
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,exception.getExtendedMessage(),"Unlock fail: %s",exception.getMessage());
        return;
      }
      finally
      {
        clearStatusText();
      }

      // start update file data
      asyncUpdateSelectedFileData();
    }
  }

  /** set file mode of selected entries
   */
  public void setFileMode()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      CommandSetFileMode commandSetFileMode = new CommandSetFileMode(shell,this,fileDataSet);
      commandSetFileMode.run();
    }
  }

  /** create new branch/tag
   */
  public void newBranchTag()
  {
    CommandCreateBranch commandCreateBranch = new CommandCreateBranch(shell,this);
    commandCreateBranch.run();
  }

  /** view selected entry
   */
  public void view()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandView commandView = new CommandView(shell,this,fileData);
      commandView.run();
    }
  }

  /** diff selected entry
   */
  public void diff()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandDiff commandDiff = new CommandDiff(shell,this,fileData);
      commandDiff.run();
    }
  }

  /** show changes log
   */
  public void changesLog()
  {
    CommandChangesLog commandChangesLog = new CommandChangesLog(shell,this);
    commandChangesLog.run();
  }

  /** show revision tree of selected entry
   */
  public void revisions()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandRevisions commandRevisions = new CommandRevisions(shell,this,fileData);
      commandRevisions.run();
    }
  }

  /** show revision info of selected entry
   */
  public void revisionInfo()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandRevisionInfo commandRevisionInfo = new CommandRevisionInfo(shell,this,fileData);
      commandRevisionInfo.run();
    }
  }

  /** show list of all changed files
   */
  public void changedFiles()
  {
    CommandChangedFiles commandChangedFiles = new CommandChangedFiles(shell,this);
    commandChangedFiles.run();
  }

  /** show annotatiosn of file
   */
  public void annotations()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      CommandAnnotations commandAnnotations = new CommandAnnotations(shell,this,fileData);
      commandAnnotations.run();
    }
  }

  /** open file (with external command)
   * @param file file
   * @param mimeType mime type pattern
   * @param lineNumber line number
   */
  public void openFile(File file, String mimeType, int lineNumber)
  {
    // find editor command with file mime-type or file name pattern
    String commandLine = null;
    if (commandLine == null)
    {
      if ((mimeType != null) && !mimeType.isEmpty() && !mimeType.equals("application/octet-stream"))
      {
        for (Settings.Editor editor : Settings.editors)
        {
          if (editor.mimeTypePattern.matcher(mimeType).matches())
          {
            commandLine = editor.commandLine;
            break;
          }
        }
      }
    }
    if (commandLine == null)
    {
      String fileName = file.getPath();
      String baseName = file.getName();
      for (Settings.Editor editor : Settings.editors)
      {
        if (   editor.fileNamePattern.matcher(fileName).matches()
            || editor.fileNamePattern.matcher(baseName).matches()
           )
        {
          commandLine = editor.commandLine;
          break;
        }
      }
    }

    // if no editor command found -> ask for command
    if (commandLine == null)
    {
      commandLine  = getFileOpenCommand(file,mimeType);
    }

    // execute external command
    if (commandLine != null)
    {
      // expand command
      Macro macro = new Macro(StringUtils.split(commandLine,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS),Macro.PATTERN_PERCENTAGE);
      if (!macro.contains("files") && !macro.contains("file")) macro.add("file");
      macro.expand("files",     new String[]{file.getPath()});
      macro.expand("file",      file.getPath());
      macro.expand("f",         file.getPath());
      macro.expand("lineNumber",lineNumber);
      macro.expand("n",         lineNumber);
      macro.expand("%%",        "%");
      Command command = new Command(macro);
//Dprintf.dprintf("command=%s",command);

      // run command
      Background.run(new BackgroundRunnable(command)
      {
        public void run(final Command command)
        {
          try
          {
            // start process (Note: use Runtime.exec() because it is a background process without i/o here)
            Process process = Runtime.getRuntime().exec(command.getCommandArray());

            // collect stderr output
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final ArrayList<String> stderrList = new ArrayList<String>();
            String line;
            while ((line = stderr.readLine()) != null)
            {
//Dprintf.dprintf("line=%s",line);
              stderrList.add(line);
            }

            // wait for exit
            final int exitCode = process.waitFor();
            if (exitCode != 0)
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  Dialogs.error(shell,
                                stderrList,
                                "Execute external command fail: \n\n'%s'\n\n (exitcode: %d)",
                                command,
                                exitCode
                               );
                }
              });
              return;
            }
          }
          catch (InterruptedException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
          catch (IOException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
        }
      });
    }
  }

  /** open file (with external command)
   * @param file file to open
   * @param mimeType mime type pattern
   */
  public void openFile(File file, String mimeType)
  {
    openFile(file,mimeType,0);
  }

  /** open file (with external command)
   * @param file file to open
   */
  public void openFile(File file)
  {
    openFile(file,Onzen.getMimeType(file));
  }

  /** open file (with external command)
   * @param fileName file to open
   * @param mimeType mime type pattern
   * @param lineNumber line number
   */
  public void openFile(String fileName, String mimeType, int lineNumber)
  {
    openFile(new File(fileName),mimeType,lineNumber);
  }

  /** open file (with external command)
   * @param fileName file name
   * @param mimeType mime type pattern
   */
  public void openFile(String fileName, String mimeType)
  {
    openFile(fileName,mimeType,0);
  }

  /** open file (with external command)
   * @param fileData file to open
   */
  public void openFile(FileData fileData)
  {
    openFile(fileData.getFileName(repository.rootPath),
             fileData.getMimeType(repository.rootPath)
            );
  }

  /** open file (with external command)
   */
  public void openFile()
  {
    final FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      openFileWith(fileData);
    }
  }

  /** open file with external program
   * @param commandLine command line
   * @param fileName file name
   * @param lineNumber line number
   */
  public void openFileWith(String commandLine, String fileName, int lineNumber)
  {
    if (commandLine != null)
    {
      // expand command
      Macro macro = new Macro(StringUtils.split(commandLine,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS),Macro.PATTERN_PERCENTAGE);
      if (!macro.contains("files") && !macro.contains("file")) macro.add("file");
      macro.expand("files",     new String[]{fileName});
      macro.expand("file",      fileName);
      macro.expand("f",         fileName);
      macro.expand("lineNumber",lineNumber);
      macro.expand("n",         lineNumber);
      macro.expand("%%",        "%");
      Command command = new Command(macro);
//Dprintf.dprintf("command=%s",command);

      // run command
      Background.run(new BackgroundRunnable(command)
      {
        public void run(final Command command)
        {
          try
          {
            // start process (Note: use Runtime.exec() because it is a background process without i/o here)
            Process process = Runtime.getRuntime().exec(command.getCommandArray());

            // collect stderr output
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final ArrayList<String> stderrList = new ArrayList<String>();
            String line;
            while ((line = stderr.readLine()) != null)
            {
//Dprintf.dprintf("line=%s",line);
              stderrList.add(line);
            }

            // wait for exit
            final int exitCode = process.waitFor();
            if (exitCode != 0)
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  Dialogs.error(shell,
                                stderrList,
                                "Execute external command fail: \n\n'%s'\n\n (exitcode: %d)",
                                command,
                                exitCode
                               );
                }
              });
              return;
            }
          }
          catch (InterruptedException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
          catch (IOException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
        }
      });
    }
  }

  /** open file with external program
   * @param fileName file name
   * @param lineNumber line number
   */
  public void openFileWith(String fileName, int lineNumber)
  {
    String commandLine = getFileOpenCommand(fileName,
                                            Onzen.getMimeType(fileName)
                                           );
    openFileWith(commandLine,fileName,lineNumber);
  }

  /** open file with external program
   * @param commandLine command line
   * @param fileName file name
   */
  public void openFileWith(String commandLine, String fileName)
  {
    openFileWith(commandLine,fileName,0);
  }

  /** open file with external program
   * @param commandLine command line
   * @param file file
   * @param lineNumber line number
   */
  public void openFileWith(String commandLine, File file, int lineNumber)
  {
    openFileWith(commandLine,file.getPath(),lineNumber);
  }

  /** open file with external program
   * @param file file
   * @param lineNumber line number
   */
  public void openFileWith(File file, int lineNumber)
  {
    openFileWith(file.getPath(),lineNumber);
  }

  /** open file with external program
   * @param commandLine command line
   * @param fileData file data
   */
  public void openFileWith(String commandLine, FileData fileData)
  {
    openFileWith(commandLine,fileData.getFileName(repository.rootPath));
  }

  /** open file with external program
   * @param fileData file data
   */
  public void openFileWith(FileData fileData)
  {
    String fileName = fileData.getFileName(repository.rootPath);
    String commandLine = getFileOpenCommand(fileName,
                                            Onzen.getMimeType(fileName)
                                           );
    openFileWith(commandLine,fileName);
  }

  /** open file with external program
   * @param commandLine command line
   * @param file file
   */
  public void openFileWith(String commandLine, File file)
  {
    openFileWith(commandLine,file,0);
  }

  /** open file with external program
   * @param file file
   */
  public void openFileWith(File file)
  {
    openFileWith(file,0);
  }

  /** open file with external program
   * @param commandLine command line
   */
  public void openFileWith(String commandLine)
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      openFileWith(commandLine,fileData);
    }
  }


  /** open file with external program
   */
  public void openFileWith()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      openFileWith(fileData);
    }
  }

  /** create new file
   * @param fileData file data
   */
  public void newFile(FileData fileData)
  {
    /** dialog data
     */
    class Data
    {
      String fileName;

      Data()
      {
        this.fileName = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite;
    Label       label;
    Button      button;

    if (fileData != null)
    {
      data.fileName = fileData.getFileName(repository.rootPath);
    }

    // new directory dialog
    dialog = Dialogs.openModal(shell,"New file",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetFileName;
    final Button widgetCreate;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"File name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFileName = Widgets.newText(composite);
      if (data.fileName != null) widgetFileName.setText(data.fileName);
      widgetFileName.setSelection(data.fileName.length(),data.fileName.length());
      Widgets.layout(widgetFileName,1,1,TableLayoutData.WE);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCreate = Widgets.newButton(composite,"Create");
      Widgets.layout(widgetCreate,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetCreate.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          File file = new File(widgetFileName.getText());
          if      (file.exists())
          {
            Dialogs.error(shell,"File or directory '%s' already exists!",file.getPath());
            return;
          }
          else if ((file.getParentFile() != null) && !file.getParentFile().canWrite())
          {
            Dialogs.error(shell,"Permission denied for parent directory '%s'!",file.getParentFile().getPath());
            return;
          }
          else
          {
            data.fileName = widgetFileName.getText();
            Dialogs.close(dialog,true);
          }
        }
      });
      widgetCreate.setToolTipText("Create new file.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetFileName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetCreate.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.setWindowLocation);

    // run
    widgetFileName.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // create file
      File file = new File(data.fileName);
Dprintf.dprintf("");
/*
      if (!file.mkdirs())
      {
        Dialogs.error(shell,"Cannot create new file '%s'",data.fileName);
        return;
      }
*/

      // update file list
      asyncUpdateSelectedFileData();
    }
  }

  /** create new file
   */
  public void newFile()
  {
    newFile(null);
  }

  /** create new directory
   * @param fileData file data for pre-set directory name
   */
  public void newDirectory(FileData fileData)
  {
    /** dialog data
     */
    class Data
    {
      String path;

      Data()
      {
        this.path = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite;
    Label       label;
    Button      button;

    if (fileData != null)
    {
      if (fileData.type == FileData.Types.DIRECTORY)
      {
        data.path = fileData.getFileName();
      }
      else
      {
        data.path = fileData.getDirectoryName();
      }
    }

    // new directory dialog
    dialog = Dialogs.openModal(shell,"New directory in: "+repository.rootPath,300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetPath;
    final List   widgetHistory;
    final Text   widgetMessage;
    final Button widgetCreate;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Sub-directory:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetPath = Widgets.newText(subComposite);
        if (data.path != null)
        {
          widgetPath.setText(data.path+File.separator);
          widgetPath.setSelection(data.path.length()+File.separator.length(),data.path.length()+File.separator.length());
        }
        Widgets.layout(widgetPath,0,1,TableLayoutData.WE);

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,2,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                           "Select path",
                                            widgetPath.getText()
                                           );
            if (path != null)
            {
              widgetPath.setText(path);
            }
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,2,0,TableLayoutData.WE,0,0,4);
    {
      widgetCreate = Widgets.newButton(composite,"Create");
      Widgets.layout(widgetCreate,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetCreate.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          File file = new File(repository.rootPath,widgetPath.getText());
          if      (file.exists())
          {
            Dialogs.error(shell,"File or directory '%s' already exists!",file.getPath());
            return;
          }
          else if ((file.getParentFile() != null) && !file.getParentFile().canWrite())
          {
            Dialogs.error(shell,"Permission denied for parent directory '%s'!",file.getParentFile().getPath());
            return;
          }
          else
          {
            data.path = widgetPath.getText();
            Dialogs.close(dialog,true);
          }
        }
      });
      widgetCreate.setToolTipText("Create new directory.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetPath.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetCreate.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.setWindowLocation);

    // run
    widgetPath.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // create directory
      File file = new File(repository.rootPath,data.path);
      if (!file.mkdirs())
      {
        Dialogs.error(shell,"Cannot create new directory '%s'",data.path);
        return;
      }

      // start update file data
      HashSet<FileData> fileDataSet = new HashSet<FileData>();
      fileDataSet.add(new FileData(new File(data.path).getParent(),FileData.Types.DIRECTORY));
      asyncUpdateFileStates(fileDataSet);
    }
  }

  /** create new directory
   */
  public void newDirectory()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      newDirectory(fileData);
      asyncUpdateSelectedFileData();
    }
  }

  /** rename local file/directory
   * @param fileData file to rename
   */
  public void renameLocalFile(final String fileName)
  {
    /** dialog data
     */
    class Data
    {
      String newFileName;

      Data()
      {
        this.newFileName = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite;
    Label       label;
    Text        text;
    Button      button;

    // rename file/directory dialog
    dialog = Dialogs.openModal(shell,"Rename/move file/directory",new double[]{1.0,0.0},1.0);

    final Text   widgetNewFileName;
    final Button widgetRename;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Old name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      text = Widgets.newStringView(composite);
      text.setText(fileName);
      Widgets.layout(text,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"New name:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetNewFileName = Widgets.newText(composite);
      Widgets.layout(widgetNewFileName,1,1,TableLayoutData.WE);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetRename = Widgets.newButton(composite,"Rename/Move");
      widgetRename.setEnabled(false);
      Widgets.layout(widgetRename,0,0,TableLayoutData.W,0,0,0,0,80,SWT.DEFAULT);
      widgetRename.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.newFileName                 = widgetNewFileName.getText();
          Settings.geometryRenameLocalFile = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetRename.setToolTipText("Rename/move file.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,80,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetNewFileName.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        widgetRename.setEnabled(!widget.getText().equals(fileName));
      }
    });
    widgetNewFileName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetRename.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryRenameLocalFile.x,SWT.DEFAULT,Settings.setWindowLocation);

    // update
    data.newFileName = fileName;
    widgetNewFileName.setText(data.newFileName);

    // run
    Widgets.setFocus(widgetNewFileName);
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // rename local file/directory
      File oldFile = new File(repository.rootPath,fileName);
      File newFile = new File(repository.rootPath,data.newFileName);

      if (!oldFile.renameTo(newFile))
      {
        Dialogs.error(shell,"Cannot rename file/directory\n\n'%s'\n\nto\n\n'%s'",oldFile.getName(),newFile.getName());
        return;
      }

      // update tree
      TreeItem subTreeItem = fileNameMap.get(fileName);
      FileData fileData = (FileData)subTreeItem.getData();
      fileNameMap.remove(fileName);
      fileData.setFileName(data.newFileName);
      fileNameMap.put(fileData.getFileName(),subTreeItem);

      // start update file data
      asyncUpdateFileStates(fileData);
    }
  }

  /** rename local file/directory
   * @param fileData file to rename
   */
  public void renameLocalFile(FileData fileData)
  {
    renameLocalFile(fileData.getFileName());
  }

  /** rename local file/directory
   * @param file file to rename
   */
  public void renameLocalFile(File file)
  {
    renameLocalFile(file.getPath());
  }

  /** rename local file/directory
   */
  public void renameLocalFile()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      renameLocalFile(fileData);
      asyncUpdateSelectedFileData();
    }
  }

  /** delete local files/directories
   * @param fileNameSet files to delete
   */
  public void deleteLocalFiles(HashSet<FileData> fileDataSet)
  {
    if (fileDataSet.size() > 0)
    {
      final Shell dialog;
      Composite   composite;
      Button      button;

      // delete dialog
      dialog = Dialogs.openModal(shell,"Delete local files/directories",new double[]{1.0,0.0},1.0);

      final List   widgetFiles;
      final Button widgetDelete;
      final Button widgetCancel;
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(1.0,1.0,4));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
      {
        widgetFiles = Widgets.newList(composite);
        widgetFiles.setBackground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetFiles,0,0,TableLayoutData.NSWE);
        widgetFiles.setToolTipText("Local files/directories to delete.");
      }

      // buttons
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetDelete = Widgets.newButton(composite,"Delete");
        Widgets.layout(widgetDelete,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetDelete.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Settings.geometryDeleteLocalFiles = dialog.getSize();

            Dialogs.close(dialog,true);
          }
        });
        widgetDelete.setToolTipText("Delete local files/directories.");

        widgetCancel = Widgets.newButton(composite,"Cancel");
        Widgets.layout(widgetCancel,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
        widgetCancel.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Dialogs.close(dialog,false);
          }
        });
      }

      // listeners

      // show dialog
      Dialogs.show(dialog,Settings.geometryDeleteLocalFiles,Settings.setWindowLocation);

      // add files
      for (FileData fileData : fileDataSet)
      {
        widgetFiles.add(fileData.getFileName());
      }

      // run
      widgetCancel.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
              HashSet<FileData> updateFileDataSet = new HashSet<FileData>();

        boolean deleteAll  = false;
        boolean skipErrors = false;
        for (FileData fileData : fileDataSet)
        {
          File file = new File(fileData.getFileName(repository.rootPath));

          // delete local file/directory
          try
          {
            if (file.isDirectory())
            {
              // confirm deleting directory
              if (!deleteAll)
              {
                switch (Dialogs.select(shell,
                                       "Error",
                                       String.format("'%s' is a directory.\n\nDelete directory?",file.getPath()),
                                       new String[]{"Yes","Yes, always","No"},
                                       2
                                      )
                       )
                {
                  case 0:
                    break;
                  case 1:
                    deleteAll = true;
                    break;
                  case 2:
                    continue;
                }
              }

              // delete local directory tree
              FileUtils.deleteDirectoryTree(file);
            }
            else
            {
              // delete local file
              FileUtils.deleteFile(file);
            }

            // store for refresh
            updateFileDataSet.add(fileData);
            FileData parentFileData = fileData.getParent();
            if (parentFileData != null) updateFileDataSet.add(parentFileData);
          }
          catch (IOException exception)
          {
            if (!skipErrors)
            {
              switch (Dialogs.select(shell,
                                     "Error",
                                     String.format("Cannot delete local file/directory '%s'.\n\nContinue?",file.getPath()),
                                     new String[]{"Yes","Yes, always","No"},
                                     2
                                    )
                     )
              {
                case 0:
                  break;
                case 1:
                  skipErrors = true;
                  break;
                case 2:
                  return;
              }
            }
          }
        }

        // start update file data
        asyncUpdateFileStates(updateFileDataSet);
      }
    }
  }

  /** delete local file/directory
   */
  public void deleteLocalFile(String fileName)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(new FileData(fileName));
    deleteLocalFiles(fileDataSet);
  }

  /** delete local file/directory
   */
  public void deleteLocalFile(File file)
  {
    deleteLocalFile(file.getPath());
  }

  /** delete selected local files/directories
   */
  public void deleteLocalFiles()
  {
    deleteLocalFiles(getSelectedFileDataSet());
    asyncUpdateSelectedFileData();
  }

  /** add to ignore file list
   * @param fileNameSet files to add to ignore liste
   */
  public void addIgnoreFiles(HashSet<FileData> fileDataSet)
  {
    // add files
    for (FileData fileData : fileDataSet)
    {
      repository.addIgnoreFilePattern(fileData.getFileName(repository));
    }

    // refresh
    asyncUpdateFileStates(fileDataSet);
  }

  /** add selected files to ignore file list
   * @param fileNameSet files to add to ignore liste
   */
  public void addIgnoreFiles()
  {
    addIgnoreFiles(getSelectedFileDataSet());
    asyncUpdateSelectedFileData();
  }

  /** add to hidden list
   * @param fileNameSet files to add to ignore liste
   */
  public void addHiddenFiles(HashSet<FileData> fileDataSet)
  {
    // get pattern maps
    LinkedHashMap<String,Settings.FilePattern> hiddenFilePatternsMap = new LinkedHashMap<String,Settings.FilePattern>();
    for (Settings.FilePattern hiddenFilePattern : Settings.hiddenFilePatterns)
    {
      hiddenFilePatternsMap.put(hiddenFilePattern.string,hiddenFilePattern);
    }
    LinkedHashMap<String,Settings.FilePattern> hiddenDirectoryPatternsMap = new LinkedHashMap<String,Settings.FilePattern>();
    for (Settings.FilePattern hiddenDirectoryPattern : Settings.hiddenDirectoryPatterns)
    {
      hiddenDirectoryPatternsMap.put(hiddenDirectoryPattern.string,hiddenDirectoryPattern);
    }

    // add patterns
    for (FileData fileData : fileDataSet)
    {
      if (fileData.type == FileData.Types.DIRECTORY)
      {
        hiddenDirectoryPatternsMap.put(fileData.getFileName(),new Settings.FilePattern(fileData.getFileName()));
      }
      else
      {
        hiddenFilePatternsMap.put(fileData.getFileName(),new Settings.FilePattern(fileData.getFileName()));
      }
    }

    // store new pattern maps
    Settings.hiddenFilePatterns      = hiddenFilePatternsMap.values().toArray(new Settings.FilePattern[hiddenFilePatternsMap.size()]);
    Settings.hiddenDirectoryPatterns = hiddenDirectoryPatternsMap.values().toArray(new Settings.FilePattern[hiddenDirectoryPatternsMap.size()]);

    // refresh
//TODO refresh parent directories
    asyncUpdateFileStates(fileDataSet);
  }

  /** add selected files to hidden list
   * @param fileNameSet files to add to ignore liste
   */
  public void addHiddenFiles()
  {
    addHiddenFiles(getSelectedFileDataSet());
    asyncUpdateSelectedFileData();
  }

  /** find files by name
   */
  public void findFilesByName()
  {
    CommandFindFiles commandFindFiles = new CommandFindFiles(shell,this,CommandFindFiles.Types.NAME);
    commandFindFiles.run();
  }

  /** find files by content
   */
  public void findFilesByContent()
  {
    CommandFindFiles commandFindFiles = new CommandFindFiles(shell,this,CommandFindFiles.Types.CONTENT);
    commandFindFiles.run();
  }

  /** convert whitespaces in selected files
   */
  public void convertWhitespaces()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      /** dialog data
       */
      class Data
      {
        boolean convertTABs;
        int     spacesPerTAB;
        boolean removeTrailingWhitespaces;

        Data()
        {
          this.convertTABs               = true;
          this.spacesPerTAB              = Settings.convertSpacesPerTAB;
          this.removeTrailingWhitespaces = true;
        }
      };

      final Data data = new Data();

      Composite composite,subComposite;
      Control   control;
      Label     label;
      Button    button;

      // convert dialog
      final Shell dialog = Dialogs.openModal(shell,"Convert whitespaces",new double[]{1.0,0.0},1.0);

      final List    widgetFiles;
      final Spinner widgetSpacesPerTAB;
      final Button  widgetConvertTABs;
      final Button  widgetRemoveTrailingWhitespaces;
      final Button  widgetConvert;
      final Button  widgetCancel;
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,4));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
      {
        widgetFiles = Widgets.newList(composite);
        widgetFiles.setBackground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetFiles,0,0,TableLayoutData.NSWE);
        widgetFiles.setToolTipText("Files to convert TABs/whitespaces.");

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.W);
        {
          widgetConvertTABs = Widgets.newCheckbox(subComposite,"Convert TABs to spaces:");
          widgetConvertTABs.setSelection(data.spacesPerTAB > 0);
          Widgets.layout(widgetConvertTABs,0,0,TableLayoutData.W);

          widgetSpacesPerTAB = Widgets.newSpinner(subComposite);
          widgetSpacesPerTAB.setMinimum(1);
          widgetSpacesPerTAB.setMaximum(255);
          widgetSpacesPerTAB.setEnabled(data.spacesPerTAB > 0);
          widgetSpacesPerTAB.setSelection(data.spacesPerTAB);
          Widgets.layout(widgetSpacesPerTAB,0,1,TableLayoutData.W,0,0,0,0,80,SWT.DEFAULT);
        }

        widgetRemoveTrailingWhitespaces = Widgets.newCheckbox(composite,"Remove trailing whitespaces");
        widgetRemoveTrailingWhitespaces.setSelection(data.removeTrailingWhitespaces);
        Widgets.layout(widgetRemoveTrailingWhitespaces,2,0,TableLayoutData.W);
      }

      // buttons
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetConvert = Widgets.newButton(composite,"Convert");
        Widgets.layout(widgetConvert,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetConvert.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            data.convertTABs               = widgetConvertTABs.getSelection();
            data.spacesPerTAB              = widgetSpacesPerTAB.getSelection();
            data.removeTrailingWhitespaces = widgetRemoveTrailingWhitespaces.getSelection();

            convertWhiteDialogLocation          = dialog.getLocation();
            Settings.geometryConvertWhitespaces = dialog.getSize();

            Dialogs.close(dialog,true);
          }
        });
        widgetConvert.setToolTipText("Convert whitespaces in files.");

        widgetCancel = Widgets.newButton(composite,"Cancel");
        Widgets.layout(widgetCancel,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
        widgetCancel.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Dialogs.close(dialog,false);
          }
        });
      }

      // listeners

      // show dialog
      Dialogs.show(dialog,convertWhiteDialogLocation,Settings.geometryConvertWhitespaces,Settings.setWindowLocation);

      // add files
      for (FileData fileData : fileDataSet)
      {
        widgetFiles.add(fileData.name);
      }

      // run
      widgetCancel.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
        // get file names
        LinkedList<String> fileNameList = new LinkedList<String>();
        for (FileData fileData : fileDataSet)
        {
          fileNameList.add(fileData.getFileName(repository));
        }

        // convert TABs/whitespaces in files
        boolean convertAll = false;
        while (!fileNameList.isEmpty())
        {
          String fileName = fileNameList.removeFirst();
          File   file     = new File(fileName);

          // convert  TABs/whitespaces in file
          boolean converted  = false;
          boolean skipErrors = false;
          if (file.isDirectory())
          {
            // confirm convert all files in directory
            if (!convertAll)
            {
              switch (Dialogs.select(shell,
                                     "Error",
                                     String.format("'%s' is a directory.\n\nConvert all files in directory?",fileName),
                                     new String[]{"Yes","Yes, always","No"},
                                     2
                                    )
                     )
              {
                case 0:
                  break;
                case 1:
                  convertAll = true;
                  break;
                case 2:
                  continue;
              }
            }

            // add files in directory for checking for whitespaces
            for (String directoryFileName : file.list())
            {
              fileNameList.add(directoryFileName);
            }
          }
          else
          {
            // convert whitespaces in file
            try
            {
              convertWhitespaces(fileName,
                                 data.convertTABs,
                                 data.removeTrailingWhitespaces,
                                 data.spacesPerTAB
                                );
            }
            catch (IOException exception)
            {
              if (!skipErrors)
              {
                switch (Dialogs.select(shell,
                                       "Error",
                                       String.format("Cannot convert file\n\n'%s'\n\n(error: %s).\n\nContinue?",file.getPath(),exception.getMessage()),
                                       new String[]{"Yes","Yes, always","No"},
                                       2
                                      )
                       )
                {
                  case 0:
                    break;
                  case 1:
                    skipErrors = true;
                    break;
                  case 2:
                    return;
                }
              }
            }
          }
        }

        // start update file data
        asyncUpdateFileStates(fileDataSet);
      }
    }
  }

  /** convert whitespaces in file dialog
   * @param fileName file name
   * @param fileNames all file names
   * @param convertTabs true to convert tabs
   * @param removeTrailingWhitespaces true to remove trailing whitespaces
   * @return true iff OK, false for abort
   */
  public ConvertWhitespacesResult convertWhitespaces(String   fileName,
                                                     String[] fileNames,
                                                     String   message,
                                                     boolean  convertTabs,
                                                     boolean  removeTrailingWhitespaces
                                                    )
  {
    /** dialog data
     */
    class Data
    {
      boolean convertTABs;
      int     spacesPerTAB;
      boolean removeTrailingWhitespaces;
      boolean convertAll;

      Data()
      {
        this.convertTABs               = true;
        this.spacesPerTAB              = Settings.convertSpacesPerTAB;
        this.removeTrailingWhitespaces = true;
        this.convertAll                = false;
      }
    };

    final Data data = new Data();

    Composite composite,subComposite;
    Control   control;
    Label     label;
    Button    button;

    // convert dialog
    final Shell dialog = Dialogs.openModal(shell,"Convert whitespaces",new double[]{1.0,0.0},1.0);

    final Spinner widgetSpacesPerTAB;
    final Button  widgetConvertTABs;
    final Button  widgetRemoveTrailingWhitespaces;
    final Button  widgetConvert;
    final Button  widgetKeep;
    final Button  widgetConvertAll;
    final Button  widgetKeepAll;
    final Button  widgetAbort;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,message);
      Widgets.layout(label,0,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,0.0));
      Widgets.layout(subComposite,1,0,TableLayoutData.W);
      {
        widgetConvertTABs = Widgets.newCheckbox(subComposite,"Convert TABs to spaces:");
        if (convertTabs)
        {
          widgetConvertTABs.setSelection(data.spacesPerTAB > 0);
        }
        else
        {
          widgetConvertTABs.setEnabled(false);
        }
        Widgets.layout(widgetConvertTABs,0,0,TableLayoutData.W);

        widgetSpacesPerTAB = Widgets.newSpinner(subComposite);
        widgetSpacesPerTAB.setMinimum(1);
        widgetSpacesPerTAB.setMaximum(255);
        widgetSpacesPerTAB.setSelection(data.spacesPerTAB);
        widgetSpacesPerTAB.setEnabled(convertTabs && (data.spacesPerTAB > 0));
        Widgets.layout(widgetSpacesPerTAB,0,1,TableLayoutData.W,0,0,0,0,80,SWT.DEFAULT);
      }

      widgetRemoveTrailingWhitespaces = Widgets.newCheckbox(composite,"Remove trailing whitespaces");
      widgetRemoveTrailingWhitespaces.setSelection(data.removeTrailingWhitespaces);
      widgetRemoveTrailingWhitespaces.setEnabled(removeTrailingWhitespaces);
      Widgets.layout(widgetRemoveTrailingWhitespaces,2,0,TableLayoutData.W);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetConvert = Widgets.newButton(composite,"Convert");
      Widgets.layout(widgetConvert,0,0,TableLayoutData.W,0,0,0,0,140,SWT.DEFAULT);
      widgetConvert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.convertTABs               = widgetConvertTABs.getSelection();
          data.spacesPerTAB              = widgetSpacesPerTAB.getSelection();
          data.removeTrailingWhitespaces = widgetRemoveTrailingWhitespaces.getSelection();
          data.convertAll                = false;

          convertWhiteDialogLocation          = dialog.getLocation();
          Settings.geometryConvertWhitespaces = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetConvert.setToolTipText("Convert whitespaces in file.");

      widgetKeep = Widgets.newButton(composite,"Keep");
      Widgets.layout(widgetKeep,0,1,TableLayoutData.W,0,0,0,0,140,SWT.DEFAULT);
      widgetKeep.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.convertTABs               = false;
          data.removeTrailingWhitespaces = false;
          data.convertAll                = false;

          convertWhiteDialogLocation          = dialog.getLocation();
          Settings.geometryConvertWhitespaces = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetKeep.setToolTipText("Keep whitespaces in file.");

      widgetConvertAll = Widgets.newButton(composite,"Convert in all files");
      Widgets.layout(widgetConvertAll,0,2,TableLayoutData.W,0,0,0,0,140,SWT.DEFAULT);
      widgetConvertAll.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.convertTABs               = widgetConvertTABs.getSelection();
          data.spacesPerTAB              = widgetSpacesPerTAB.getSelection();
          data.removeTrailingWhitespaces = widgetRemoveTrailingWhitespaces.getSelection();
          data.convertAll                = true;

          convertWhiteDialogLocation          = dialog.getLocation();
          Settings.geometryConvertWhitespaces = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetConvertAll.setToolTipText("Convert whitespaces in all files.");

      widgetKeepAll = Widgets.newButton(composite,"Keep in all files");
      Widgets.layout(widgetKeepAll,0,3,TableLayoutData.W,0,0,0,0,140,SWT.DEFAULT);
      widgetKeepAll.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.convertTABs               = false;
          data.removeTrailingWhitespaces = false;
          data.convertAll                = true;

          convertWhiteDialogLocation          = dialog.getLocation();
          Settings.geometryConvertWhitespaces = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetKeepAll.setToolTipText("Keep whitespaces in all files.");

      widgetAbort = Widgets.newButton(composite,"Abort");
      Widgets.layout(widgetAbort,0,4,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      widgetAbort.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners

    // show dialog
    Dialogs.show(dialog,convertWhiteDialogLocation,Settings.geometryConvertWhitespaces,Settings.setWindowLocation);

    // run
    widgetAbort.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // convert whitespaces in files
      if (data.convertAll)
      {
        try
        {
          convertWhitespaces(fileNames,
                             data.convertTABs,
                             data.removeTrailingWhitespaces,
                             data.spacesPerTAB
                            );
        }
        catch (IOException exception)
        {
          Dialogs.error(shell,
                        String.format("Cannot convert whitespaces in files\n(error: %s).",exception.getMessage())
                       );
          return ConvertWhitespacesResult.FAIL;
        }

        if (data.convertTABs && data.removeTrailingWhitespaces)
        {
          return ConvertWhitespacesResult.ALL;
        }
        else if (data.convertTABs)
        {
          return ConvertWhitespacesResult.ALL;
        }
        else if (data.removeTrailingWhitespaces)
        {
          return ConvertWhitespacesResult.ALL;
        }
        else
        {
          return ConvertWhitespacesResult.OK;
        }
      }
      else
      {
        try
        {
          convertWhitespaces(fileName,
                             data.convertTABs,
                             data.removeTrailingWhitespaces,
                             data.spacesPerTAB
                            );
        }
        catch (IOException exception)
        {
          if (!Dialogs.confirm(shell,
                               "Error",
                               String.format("Cannot convert whitespaces in file\n\n'%s'\n\n(error: %s).\n\nContinue?",fileName,exception.getMessage())
                              )
             )
          {
            return ConvertWhitespacesResult.FAIL;
          }
        }
        return ConvertWhitespacesResult.OK;
      }
    }
    else
    {
      return ConvertWhitespacesResult.ABORTED;
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "RepositoryTab {"+repository.title+"}";
  }

  /** execute shell command
   * @param fileDataSet files for shell command
   */
  public void executeShellCommand(final Settings.ShellCommand shellCommand, HashSet<FileData> fileDataSet)
  {
    if (fileDataSet != null)
    {
      FileData[] fileDataArray = fileDataSet.toArray(new FileData[fileDataSet.size()]);

      // expand command
      Macro macro = new Macro(StringUtils.split(shellCommand.commandLine,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS),Macro.PATTERN_PERCENTAGE);
      if (   !macro.contains("files")
          && !macro.contains("file1") && !macro.contains("directory1")
          && !macro.contains("file2") && !macro.contains("directory2")
          && !macro.contains("file3") && !macro.contains("directory3")
          && !macro.contains("file4") && !macro.contains("directory4")
          && !macro.contains("file5") && !macro.contains("directory5")
          && !macro.contains("file6") && !macro.contains("directory6")
          && !macro.contains("file7") && !macro.contains("directory7")
          && !macro.contains("file8") && !macro.contains("directory8")
          && !macro.contains("file9") && !macro.contains("directory9")
          && !macro.contains("file") && !macro.contains("directory")
         )
      {
        macro.add("file");
      }
      macro.expand("files",fileDataSet,'"'," ");
      macro.expand("file1",     (fileDataArray.length >= 1) ? fileDataArray[0].getFileName(repository.rootPath)      : "");
      macro.expand("directory1",(fileDataArray.length >= 1) ? fileDataArray[0].getDirectoryName(repository.rootPath) : "");
      macro.expand("file2",     (fileDataArray.length >= 2) ? fileDataArray[1].getFileName(repository.rootPath)      : "");
      macro.expand("directory2",(fileDataArray.length >= 2) ? fileDataArray[1].getDirectoryName(repository.rootPath) : "");
      macro.expand("file3",     (fileDataArray.length >= 3) ? fileDataArray[2].getFileName(repository.rootPath)      : "");
      macro.expand("directory3",(fileDataArray.length >= 3) ? fileDataArray[2].getDirectoryName(repository.rootPath) : "");
      macro.expand("file4",     (fileDataArray.length >= 4) ? fileDataArray[3].getFileName(repository.rootPath)      : "");
      macro.expand("directory4",(fileDataArray.length >= 4) ? fileDataArray[3].getDirectoryName(repository.rootPath) : "");
      macro.expand("file5",     (fileDataArray.length >= 5) ? fileDataArray[4].getFileName(repository.rootPath)      : "");
      macro.expand("directory5",(fileDataArray.length >= 5) ? fileDataArray[4].getDirectoryName(repository.rootPath) : "");
      macro.expand("file6",     (fileDataArray.length >= 6) ? fileDataArray[5].getFileName(repository.rootPath)      : "");
      macro.expand("directory6",(fileDataArray.length >= 6) ? fileDataArray[5].getDirectoryName(repository.rootPath) : "");
      macro.expand("file7",     (fileDataArray.length >= 7) ? fileDataArray[6].getFileName(repository.rootPath)      : "");
      macro.expand("directory7",(fileDataArray.length >= 7) ? fileDataArray[6].getDirectoryName(repository.rootPath) : "");
      macro.expand("file8",     (fileDataArray.length >= 8) ? fileDataArray[7].getFileName(repository.rootPath)      : "");
      macro.expand("directory8",(fileDataArray.length >= 8) ? fileDataArray[7].getDirectoryName(repository.rootPath) : "");
      macro.expand("file9",     (fileDataArray.length >= 9) ? fileDataArray[8].getFileName(repository.rootPath)      : "");
      macro.expand("directory9",(fileDataArray.length >= 9) ? fileDataArray[8].getDirectoryName(repository.rootPath) : "");
      macro.expand("file",      (fileDataArray.length >= 1) ? fileDataArray[0].getFileName(repository.rootPath)      : "");
      macro.expand("f",         (fileDataArray.length >= 1) ? fileDataArray[0].getFileName(repository.rootPath)      : "");
      macro.expand("directory", (fileDataArray.length >= 1) ? fileDataArray[0].getDirectoryName(repository.rootPath) : "");
      macro.expand("d",         (fileDataArray.length >= 1) ? fileDataArray[0].getDirectoryName(repository.rootPath) : "");
      macro.expand("rootPath",  repository.rootPath);
      macro.expand("r",         repository.rootPath);
      macro.expand("",          "%");
      Command command = new Command(macro);

      // run command
      Background.run(new BackgroundRunnable(command)
      {
        public void run(final Command command)
        {
          try
          {
            // start process (Note: use Runtime.exec() because it is a background process without i/o here)
            Process process = Runtime.getRuntime().exec(command.getCommandArray());

            // collect stderr output
            BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final ArrayList<String> stderrList = new ArrayList<String>();
            String line;
            while ((line = stderr.readLine()) != null)
            {
              stderrList.add(line);
            }

            // wait for exit
            final int exitCode = process.waitFor();
            if (exitCode > shellCommand.validExitcode)
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  Dialogs.error(shell,
                                stderrList,
                                "Execute external shell command fail: \n\n'%s'\n\n (exitcode: %d)",
                                command,
                                exitCode
                               );
                }
              });
              return;
            }
          }
          catch (InterruptedException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external shell command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
          catch (IOException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              "Execute external shell command fail: \n\n'%s'\n\n (error: %s)",
                              command,
                              message
                             );
              }
            });
            return;
          }
        }
      });
    }
  }

  /** run shell command
   */
  public void executeShellCommand(Settings.ShellCommand shellCommand)
  {
    executeShellCommand(shellCommand,getSelectedFileDataSet());
    asyncUpdateSelectedFileData();
  }

  //-----------------------------------------------------------------------

  /** add root directory
   */
  private void addRootDirectory()
  {
    FileData rootFileData = new FileData("",
                                         FileData.Types.DIRECTORY
                                        );
    TreeItem rootTreeItem = Widgets.addTreeItem(widgetFileTree,rootFileData,true);
    rootTreeItem.setText(repository.rootPath);
    rootTreeItem.setImage(Onzen.IMAGE_DIRECTORY);
    widgetFileTree.addTreeListener(new TreeListener()
    {
      public void treeCollapsed(TreeEvent treeEvent)
      {
        TreeItem treeItem = (TreeItem)treeEvent.item;
        closeFileTreeItem(treeItem);
      }
      public void treeExpanded(TreeEvent treeEvent)
      {
        TreeItem treeItem = (TreeItem)treeEvent.item;
        FileData fileData = (FileData)treeItem.getData();
        if (fileData.type == FileData.Types.DIRECTORY)
        {
          openFileTreeItem(treeItem);
        }
      }
    });
    widgetFileTree.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        TreeItem treeItem = widgetFileTree.getItem(new Point(mouseEvent.x,mouseEvent.y));
        if (treeItem != null)
        {
          FileData fileData = (FileData)treeItem.getData();
          switch (fileData.type)
          {
            case DIRECTORY:
              /* On Linux a double-click does not open directory by default. Send a
                 event to initiate this behavior on Linux.
              */
              if (System.getProperty("os.name").toLowerCase().matches("linux"))
              {
                Event treeEvent = new Event();
                treeEvent.item = treeItem;
                if (treeItem.getExpanded())
                {
                  widgetFileTree.notifyListeners(SWT.Collapse,treeEvent);
                }
                else
                {
                  widgetFileTree.notifyListeners(SWT.Expand,treeEvent);
                }
              }
              break;
            case FILE:
            case LINK:
              openFile(fileData);
              break;
            default:
              break;
          }
        }
      }

      public void mouseDown(final MouseEvent mouseEvent)
      {
      }

      public void mouseUp(final MouseEvent mouseEvent)
      {
      }
    });
    /* Note: do not use SelectionListener, because after a mouse-click
       the selection listener is executed, too! (this cause open and
       closing the sub-tree immediately)
    */
    widgetFileTree.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
//Dprintf.dprintf("keyEvent=%s",keyEvent);
        if ((keyEvent.stateMask == 0) && (keyEvent.keyCode == SWT.CR))
        {
// NYI: handling more than one item?
          TreeItem[] treeItems = widgetFileTree.getSelection();
          if ((treeItems != null) && (treeItems.length > 0) && (treeItems[0] != null))
          {
            FileData fileData = (FileData)treeItems[0].getData();
            switch (fileData.type)
            {
              case DIRECTORY:
                Event treeEvent = new Event();
                treeEvent.item = treeItems[0];
                if (treeItems[0].getExpanded())
                {
                  widgetFileTree.notifyListeners(SWT.Collapse,treeEvent);
                }
                else
                {
                  widgetFileTree.notifyListeners(SWT.Expand,treeEvent);
                }
                break;
              case FILE:
              case LINK:
                openFile(fileData);
                break;
              default:
                break;
            }
          }
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'c'))
        {
          TreeItem[] treeItems = widgetFileTree.getSelection();
          if (treeItems != null)
          {
            Widgets.setClipboard(clipboard,treeItems,0);
          }
        }
      }

      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
  }

  /** clear file tree, close all sub-directories
   */
  private void clearFileTree()
  {
    // close all directories
    synchronized(widgetFileTree)
    {
      for (TreeItem treeItem : widgetFileTree.getItems())
      {
        treeItem.removeAll();
        new TreeItem(treeItem,SWT.NONE);
      }
    }
  }

  /** find tree item
   * @param name name of tree item
   * @return tree item or null if not found
   */
  private TreeItem findTreeItem(TreeItem treeItems[], String name)
  {
    for (TreeItem treeItem : treeItems)
    {
      FileData fileData = (FileData)treeItem.getData();

      if ((fileData != null) && fileData.name.equals(name))
      {
        return treeItem;
      }
    }

    return null;
  }

  /** find index for insert of tree item in sorted list of tree items
   * @param treeItem tree item
   * @param fileData data of tree item
   * @return index in tree item
   */
  private int findFilesTreeIndex(TreeItem treeItem, FileData fileData)
  {
    int index = 0;

    synchronized(widgetFileTree)
    {
      TreeItem           subTreeItems[]     = treeItem.getItems();
      FileDataComparator fileDataComparator = new FileDataComparator(widgetFileTree);

      while (   (index < subTreeItems.length)
             && (fileDataComparator.compare(fileData,(FileData)subTreeItems[index].getData()) > 0)
            )
      {
        index++;
      }
    }

    return index;
  }

  /** open file sub-tree item
   * @param treeItem tree item to open
   */
  private void openFileTreeItem(TreeItem treeItem)
  {
    FileData directoryData = (FileData)treeItem.getData();

    if (!treeItem.getExpanded())
    {
      // remove existing sub-items
      treeItem.removeAll();

      // open directory and get file list data
      HashSet<FileData> fileDataSet = repository.openDirectory(directoryData.name);

      // add sub-tree
      for (FileData fileData : fileDataSet)
      {
        // create tree item
        TreeItem subTreeItem = Widgets.addTreeItem(treeItem,findFilesTreeIndex(treeItem,fileData),fileData,false);
        subTreeItem.setText(0,fileData.getBaseName());
        subTreeItem.setImage(getFileDataImage(fileData));
        if (fileData.type == FileData.Types.DIRECTORY) new TreeItem(subTreeItem,SWT.NONE);

        // store tree item reference
        fileNameMap.put(fileData.getFileName(),subTreeItem);
      }
      treeItem.setExpanded(true);

      // start update states
      asyncUpdateFileStates(fileDataSet,directoryData.getFileName(repository));
    }
  }

  /** close file sub-tree
   * @param treeItem tree item to close
   */
  private void closeFileTreeItem(TreeItem treeItem)
  {
    FileData directoryData = (FileData)treeItem.getData();

    // close directory
    repository.closeDirectory(directoryData.name);

    // remove sub-tree
    treeItem.setExpanded(false);
    treeItem.removeAll();
    new TreeItem(treeItem,SWT.NONE);
  }

  /** open sub-directory
   * @param treeItems tree items
   * @param directory directory to open
   * @return true iff sub-directory opened
   */
  private boolean openSubDirectory(TreeItem[] treeItems, String directory)
  {
    for (TreeItem treeItem : treeItems)
    {
      if (!treeItem.isDisposed())
      {
        FileData fileData = (FileData)treeItem.getData();
        String   fileName = fileData.getFileName();
//Dprintf.dprintf("fileData.getFileName()=#%s# e=#%s#",fileName,directory);

        if (   fileName.isEmpty()
            || directory.equals(fileName)
            || directory.startsWith(fileName+File.separator)
           )
        {
          // open this tree item
          openFileTreeItem(treeItem);

          if (!directory.equals(fileData.getFileName()))
          {
            // go down to sub-items
            return openSubDirectory(treeItem.getItems(),directory);
          }
          else
          {
            // done
            return true;
          }
        }
      }
    }

    return false;
  }

  /** get file data image
   * @param fileData file data
   * @return image
   */
  private Image getFileDataImage(FileData fileData)
  {
    Image image = null;
    switch (fileData.type)
    {
      case DIRECTORY: image = Onzen.IMAGE_DIRECTORY; break;
      case FILE:      image = Onzen.IMAGE_FILE;      break;
      case LINK:      image = Onzen.IMAGE_LINK;      break;
      default:        image = Onzen.IMAGE_FILE;      break;
    }

    return image;
  }

  /** get file data state string
   * @param fileData file data
   * @return string
   */
  private String getFileDataStateString(FileData fileData)
  {
    String string = null;
    if (!repository.isIgnoreFile(fileData.getFileName(repository)))
    {
      switch (fileData.type)
      {
        case DIRECTORY:
          string = "";
          break;
        case FILE:
        case LINK:
          string = fileData.state.toString();
          break;
        default:
          string = "";
          break;
      }
    }
    else
    {
      string = "";
    }

    return string;
  }

  /** get file data foreground color
   * @param fileData file data
   * @return color
   */
  private Color getFileDataForeground(FileData fileData)
  {
    Color color = null;
    if (!repository.isIgnoreFile(fileData.getFileName(repository)))
    {
      color = Onzen.COLOR_BLACK;
    }
    else
    {
      color = COLOR_IGNORE;
    }

    return color;
  }

  /** get file data background color
   * @param fileData file data
   * @return color
   */
  private Color getFileDataBackground(FileData fileData)
  {
    Color color = null;
    if (!repository.isIgnoreFile(fileData.getFileName(repository)))
    {
      switch (fileData.type)
      {
        case DIRECTORY:
        case FILE:
        case LINK:
          switch (fileData.state)
          {
            case OK        : color = COLOR_OK;         break;
            case UNKNOWN   : color = COLOR_UNKNOWN;    break;
            case MODIFIED  : color = COLOR_MODIFIED;   break;
            case CHECKOUT  : color = COLOR_CHECKOUT;   break;
            case UPDATE    : color = COLOR_UPDATE;     break;
            case MERGE     : color = COLOR_MERGE;      break;
            case CONFLICT  : color = COLOR_CONFLICT;   break;
            case ADDED     : color = COLOR_ADDED;      break;
            case REMOVED   : color = COLOR_REMOVED;    break;
            case NOT_EXISTS: color = COLOR_NOT_EXISTS; break;
            case WAITING   : color = COLOR_WAITING;    break;
            case ERROR     : color = COLOR_ERROR;      break;
            default        : color = COLOR_UNKNOWN;    break;
          }
          break;
        default:
          color = COLOR_OK;
          break;
      }
    }
    else
    {
      color = COLOR_OK;
    }

    return color;
  }

  /** get selected file data
   * @return selected file data (first)
   */
  private FileData getSelectedFileData()
  {
    TreeItem[] selectedTreeItems = widgetFileTree.getSelection();

    return (selectedTreeItems.length > 0) ? (FileData)selectedTreeItems[0].getData() : null;
  }

  /** get selected file data set
   * @return selected file data hash set
   */
  private HashSet<FileData> getSelectedFileDataSet()
  {
    TreeItem[] selectedTreeItems = widgetFileTree.getSelection();

    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (TreeItem selectedTreeItem : selectedTreeItems)
    {
      fileDataSet.add((FileData)selectedTreeItem.getData());
    }

    return fileDataSet;
  }

  /** asynchronous update of selected file data set
   */
  private void asyncUpdateSelectedFileData()
  {
    ArrayList<TreeItem> updateTreeItemList = new ArrayList<TreeItem>();

    LinkedList<TreeItem> treeItemList = new LinkedList<TreeItem>();
    for (TreeItem selectedTreeItem : widgetFileTree.getSelection())
    {
      updateTreeItemList.add(selectedTreeItem);
    }
    asyncUpdateFileStates(updateTreeItemList.toArray(new TreeItem[updateTreeItemList.size()]));
  }

  /** get all file data set
   * @return all file data hash set
   */
  private HashSet<FileData> getAllFileDataSet()
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    LinkedList<TreeItem> treeItems = new LinkedList<TreeItem>();
    for (TreeItem treeItem : widgetFileTree.getItems())
    {
      treeItems.add(treeItem);
    }
    while (!treeItems.isEmpty())
    {
      TreeItem treeItem = treeItems.removeFirst();
      if (treeItem != null)
      {
        FileData fileData = (FileData)treeItem.getData();
        if (fileData != null) fileDataSet.add(fileData);

        for (TreeItem subTreeItem : treeItem.getItems())
        {
          treeItems.add(subTreeItem);
        }
      }
    }

    return fileDataSet;
  }

  //-----------------------------------------------------------------------

  /** update file tree item
   * @param treeItem tree item to update
   * @param fileData file data
   */
  private void updateTreeItem(TreeItem treeItem, FileData fileData)
  {
    if (!treeItem.isDisposed())
    {
      // set text, colors
      treeItem.setText(0,fileData.getBaseName());
//treeItem.setImage(1,(fileData.state == FileData.States.OK) ? Onzen.IMAGE_LOCK : Onzen.IMAGE_EMPTY);
      treeItem.setText(1,getFileDataStateString(fileData));
      treeItem.setText(2,fileData.workingRevision);
      treeItem.setText(3,fileData.branch);
      treeItem.setForeground(getFileDataForeground(fileData));
      treeItem.setBackground(getFileDataBackground(fileData));

      // refresh widgegt state
//Dprintf.dprintf("updateTreeItem filedata=%s treeitem=%s %s %s",fileData,treeItem,treeItem.getExpanded(),treeItem.getItemCount());
      if (fileData.type == FileData.Types.DIRECTORY)
      {
        if (!treeItem.getExpanded() || (treeItem.getItemCount() <= 0))
        {
          closeFileTreeItem(treeItem);
        }
      }
//Dprintf.dprintf("treeItem=%s %d %s %s",treeItem,treeItem.getItemCount(),fileData.type,treeItem.getExpanded());
/*
      if ((fileData.type == FileData.Types.DIRECTORY) && !treeItem.getExpanded() && (treeItem.getItemCount() != 1))
      {
        treeItem.removeAll();
        new TreeItem(treeItem,SWT.NONE);
      }
*/
    }
  }

  /** update file tree item
   * @param treeItem tree item to update
   */
  private void updateTreeItem(TreeItem treeItem)
  {
    if (!treeItem.isDisposed())
    {
      updateTreeItem(treeItem,(FileData)treeItem.getData());
    }
  }

  /** update file tree items
   * @param treeItems tree items to update
   */
  private void updateTreeItems(TreeItem[] treeItems)
  {
    for (TreeItem treeItem : treeItems)
    {
      if (!treeItem.isDisposed())
      {
        updateTreeItem(treeItem,(FileData)treeItem.getData());
      }
    }
  }

  /** update file tree item
   * @param fileData file data
   */
  protected void updateTreeItem(FileData fileData)
  {
    TreeItem treeItem = fileNameMap.get(fileData.getFileName());
    if (treeItem != null)
    {
      updateTreeItem(treeItem,fileData);
    }
  }

  /** update file tree items
   * @param fileDataSet file data set
   */
  protected void updateTreeItems(HashSet<FileData> fileDataSet)
  {
    for (FileData fileData : fileDataSet)
    {
      updateTreeItem(fileData);
    }
  }

  /** update file states
   * @param fileDataSet file data set to update states
   * @param title title to show in status line or null
   */
  protected void updateFileStates(HashSet<FileData> fileDataSet, String title)
  {
    setStatusText("Update status of '%s'...",(title != null) ? title : repository.title);
    try
    {
      // update tree items: status update in progress
      for (final FileData fileData : fileDataSet)
      {
        final TreeItem treeItem = fileNameMap.get(fileData.getFileName());
        if (treeItem != null)
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              if (!treeItem.isDisposed()) treeItem.setForeground(COLOR_UPDATE_STATUS);
            }
          });
        }
      }

      // update states
      HashSet<FileData> newFileDataSet = new HashSet<FileData>();
      try
      {
        repository.updateStates(fileDataSet,newFileDataSet);
//Dprintf.dprintf("--------------------"); for (FileData f : newFileDataSet) Dprintf.dprintf("newFileDataSet=%s",f.getFileName());
      }
      catch (final RepositoryException exception)
      {
        synchronized(Settings.showUpdateStatusErrors)
        {
          if (Settings.showUpdateStatusErrors)
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,
                              Dialogs.booleanFieldUpdater(Settings.class,"showUpdateStatusErrors"),
                              exception.getExtendedMessage(),
                              "Getting file states fail:\n\n%s",
                              exception.getMessage()
                             );
              }
            });
          }
        }
      }

      // update tree items: set status color, remove not existing entries
      FileData[] fileDataArray = fileDataSet.toArray(new FileData[fileDataSet.size()]);
      for (final FileData fileData : fileDataArray)
      {
        if ((new File(fileData.getFileName(repository.rootPath)).exists()))
        {
          // file exists -> show state
          final TreeItem treeItem = fileNameMap.get(fileData.getFileName());
          if (treeItem != null)
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (!treeItem.isDisposed()) updateTreeItem(treeItem,fileData);
              }
            });
          }
        }
        else
        {
          // file disappeared -> remove tree entry
          fileDataSet.remove(fileData);
//Dprintf.dprintf("removed %s %s",fileData,fileData.getFileName());

          final TreeItem treeItem = fileNameMap.get(fileData.getFileName());
          if (treeItem != null)
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (!treeItem.isDisposed())
                {
                  TreeItem parentTreeItem = treeItem.getParentItem();

                  if ((parentTreeItem != null) && (parentTreeItem.getItemCount() <= 1))
                  {
                    parentTreeItem.setExpanded(false);
                  }
                  treeItem.dispose();
                  if ((parentTreeItem != null) && (parentTreeItem.getItemCount() <= 0))
                  {
                    closeFileTreeItem(parentTreeItem);
                  }
                }
              }
            });
          }
        }
      }

      // add new tree items
      for (final FileData newFileData : newFileDataSet)
      {
        TreeItem treeItem = fileNameMap.get(newFileData.getFileName());
        if ((treeItem == null) || treeItem.isDisposed())
        {
//Dprintf.dprintf("newFileData=%s newFileData.getDirectoryName=#%s#",newFileData,newFileData.getDirectoryName());
          // add new tree item
          display.syncExec(new Runnable()
          {
            public void run()
            {
              synchronized(widgetFileTree)
              {
                if (!widgetFileTree.isDisposed())
                {
                  // find parent tree item to insert new tree item
                  TreeItem treeItem = null;
                  for (TreeItem rootTreeItem : Widgets.getTreeItems(widgetFileTree))
                  {
                    FileData fileData = (FileData)rootTreeItem.getData();
//if (fileData != null) Dprintf.dprintf("  fileData.getFileName=#%s#",fileData.getFileName());
                    if ((fileData != null) && fileData.getFileName().equals(newFileData.getDirectoryName()))
                    {
//Dprintf.dprintf("    found treeItem=%s fileData=%s",rootTreeItem,fileData);
                      treeItem = rootTreeItem;
                      break;
                    }
                  }

                  if ((treeItem != null) && !treeItem.isDisposed())
                  {
                    // create tree item
                    TreeItem newTreeItem = Widgets.addTreeItem(treeItem,findFilesTreeIndex(treeItem,newFileData),newFileData,false);
                    newTreeItem.setText(0,newFileData.getBaseName());
                    newTreeItem.setImage(getFileDataImage(newFileData));
                    updateTreeItem(newTreeItem,newFileData);

                    // store tree item reference
                    fileNameMap.put(newFileData.getFileName(),newTreeItem);
                  }
//else { for (TreeItem i : fileNameMap.values()) Dprintf.dprintf("i=%s",i); }
                }
              }
            }
          });
        }
      }
    }
    finally
    {
      clearStatusText();
    }
  }

  /** update file states
   * @param fileDataSet file data set to update states
   */
  protected void updateFileStates(HashSet<FileData> fileDataSet)
  {
    if (fileDataSet != null)
    {
      updateFileStates(fileDataSet,null);
    }
  }

  /** update file state
   * @param fileData file data
   * @param title title to show in status line or null
   */
  protected void updateFileStates(final FileData fileData, String title)
  {
    updateFileStates(fileData.toSet(),title);
  }

  /** update file state
   * @param fileData file data
   */
  protected void updateFileStates(final FileData fileData)
  {
    updateFileStates(fileData,null);
  }

  /** update file state
   * @param treeItems tree items
   * @param title title to show in status line or null
   */
  protected void updateFileStates(final TreeItem[] treeItems, String title)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (TreeItem treeItem : treeItems)
    {
      fileDataSet.add((FileData)treeItem.getData());
    }
    updateFileStates(fileDataSet,title);
  }

  /** update file state
   * @param treeItems tree items
   */
  protected void updateFileStates(final TreeItem[] treeItems)
  {
    updateFileStates(treeItems,null);
  }

  /** update file state
   * @param treeItem tree item
   */
  protected void updateFileStates(final TreeItem treeItem)
  {
    updateFileStates((FileData)treeItem.getData());
  }

  /** asyncronous update file states
   * @param fileDataSet file data set to update states
   * @param title title to show in status line or null
   */
  protected void asyncUpdateFileStates(HashSet<FileData> fileDataSet, String title)
  {
    if (fileDataSet != null)
    {
      Background.run(new BackgroundRunnable(repository,fileDataSet,title)
      {
        public void run(Repository repository, HashSet<FileData> fileDataSet, String title)
        {
          updateFileStates(fileDataSet,(title != null) ? title : repository.title);
        }
      });
    }
  }

  /** asyncronous update file states
   * @param fileDataSet file data set to update states
   */
  protected void asyncUpdateFileStates(HashSet<FileData> fileDataSet)
  {
    asyncUpdateFileStates(fileDataSet,null);
  }

  /** asyncronous update file state
   * @param fileData file data
   */
  protected void asyncUpdateFileStates(FileData fileData)
  {
    Background.run(new BackgroundRunnable(repository,fileData)
    {
      public void run(Repository repository, final FileData fileData)
      {
//Dprintf.dprintf("fileData=%s",fileData);
        updateFileStates(fileData,fileData.getFileName(repository.rootPath));
      }
    });
  }

    /** update file state
   * @param treeItems tree items
   * @param title title to show in status line or null
   */
  protected void asyncUpdateFileStates(final TreeItem[] treeItems, String title)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (TreeItem treeItem : treeItems)
    {
      fileDataSet.add((FileData)treeItem.getData());
    }
    asyncUpdateFileStates(fileDataSet,title);
  }

  /** update file state
   * @param treeItems tree items
   */
  protected void asyncUpdateFileStates(final TreeItem[] treeItems)
  {
    asyncUpdateFileStates(treeItems,null);
  }

  /** update file state
   * @param treeItem tree item
   */
  protected void asyncUpdateFileStates(final TreeItem treeItem)
  {
    asyncUpdateFileStates((FileData)treeItem.getData());
  }

  //-----------------------------------------------------------------------

  /** get file open command
   * @param fileName file name
   * @param mimeType mime type pattern
   * @return command or null
   */
  private String getFileOpenCommand(File file, String mimeType)
  {
    /** dialog data
     */
    class Data
    {
      String  name;
      String  mimeType;
      String  fileName;
      String  commandLine;
      boolean addNewCommand;

      Data()
      {
        this.name          = "";
        this.mimeType      = null;
        this.fileName      = null;
        this.commandLine   = null;
        this.addNewCommand = false;
      }
    };

    String     commandLine = null;
    final Data data        = new Data();

    Composite composite,subComposite,subSubComposite;
    Label     label;
    Button    button;

    data.fileName = file.getName();
    data.mimeType = mimeType;

    // get file name pattern and command from file associations if possible
    String suffix = (data.fileName.lastIndexOf('.') >= 0) ? data.fileName.substring(data.fileName.lastIndexOf('.')).toLowerCase() : null;
    if (suffix != null)
    {
      data.fileName    = "*"+suffix;
      data.commandLine = Onzen.getFileAssociation(suffix);
    }

    // command selection dialog
    final Shell dialog = Dialogs.openModal(shell,"Select command to open file '"+file.getPath()+"'",300,200,new double[]{1.0,0.0},1.0);

    // create widgets
    final Table  widgetEditors;
    final Text   widgetName;
    final Text   widgetMimeType;
    final Text   widgetFileName;
    final Text   widgetCommandLine;
    final Button widgetAddNewCommand;
    final Button widgetOpen;
    composite = Widgets.newComposite(dialog,SWT.NONE,4);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      widgetEditors = Widgets.newTable(composite);
      Widgets.layout(widgetEditors,0,0,TableLayoutData.NSWE);
      Widgets.addTableColumn(widgetEditors,0,"Name",     SWT.LEFT,100,false);
      Widgets.addTableColumn(widgetEditors,1,"Mime type",SWT.LEFT,100,false);
      Widgets.addTableColumn(widgetEditors,2,"File name",SWT.LEFT,100,false);
      Widgets.addTableColumn(widgetEditors,3,"Command",  SWT.LEFT,400,true );
      Widgets.setTableColumnWidth(widgetEditors,Settings.geometryOpenFileColumns.width);
      widgetEditors.setToolTipText("Edit commands.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Name:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetName = Widgets.newText(subComposite);
        Widgets.layout(widgetName,0,1,TableLayoutData.WE);
        widgetName.setToolTipText("Name of editor or empty.\n");

        label = Widgets.newLabel(subComposite,"Mime type:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetMimeType = Widgets.newText(subComposite);
        widgetMimeType.setText(mimeType);
        Widgets.layout(widgetMimeType,1,1,TableLayoutData.WE);
        widgetMimeType.setToolTipText("Mime type pattern. Format: <type>/<sub-type>\n");

        label = Widgets.newLabel(subComposite,"File name:");
        Widgets.layout(label,2,0,TableLayoutData.W);

        widgetFileName = Widgets.newText(subComposite);
        if (data.fileName != null) widgetFileName.setText(data.fileName);
        Widgets.layout(widgetFileName,2,1,TableLayoutData.WE);
        widgetFileName.setToolTipText("Simple file file name pattern, e. g. *.pdf.\n");

        label = Widgets.newLabel(subComposite,"Command:");
        Widgets.layout(label,3,0,TableLayoutData.W);

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
        Widgets.layout(subSubComposite,3,1,TableLayoutData.WE);
        {
          widgetCommandLine = Widgets.newText(subSubComposite);
          if (data.commandLine != null) widgetCommandLine.setText(data.commandLine);
          Widgets.layout(widgetCommandLine,0,0,TableLayoutData.WE);
          widgetCommandLine.setToolTipText("Command to open file with.\nMacros:\n  %file% - file name\n  %n% - line number\n  %% - %");

          button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
          Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String fileName = Dialogs.fileOpen(shell,
                                                 "Select program",
                                                 widgetCommandLine.getText(),
                                                 new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                              "Scripts",    "*.sh",
                                                              "Batch files","*.cmd",
                                                              "Executables","*.exe"
                                                             },
                                                 Onzen.EXECUTABLE_FILE_EXTENSION
                                                );
              if (fileName != null)
              {
                widgetCommandLine.setText(FileUtils.escape(fileName));
              }
            }
          });
        }

        widgetAddNewCommand = Widgets.newCheckbox(subComposite,"add as new command");
        Widgets.layout(widgetAddNewCommand,4,1,TableLayoutData.W);
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog,SWT.NONE,4);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetOpen = Widgets.newButton(composite,"Open");
      widgetOpen.setEnabled(!widgetCommandLine.getText().trim().isEmpty());
      Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
      widgetOpen.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.name          = widgetName.getText().trim();
          data.mimeType      = widgetMimeType.getText().trim();
          data.fileName      = widgetFileName.getText().trim();
          data.commandLine   = widgetCommandLine.getText();
          data.addNewCommand = widgetAddNewCommand.getSelection();

          Settings.geometryOpenFile        = dialog.getSize();
          Settings.geometryOpenFileColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetEditors));

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetEditors.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetEditors.getSelectionIndex();
        if (index >= 0)
        {
          TableItem       tableItem = widgetEditors.getItem(index);
          Settings.Editor editor    = (Settings.Editor)tableItem.getData();

          widgetName.setText(editor.name);
          widgetCommandLine.setText(editor.commandLine);
        }
      }
    });
    widgetEditors.addListener(SWT.MouseDoubleClick,new Listener()
    {
      public void handleEvent(final Event event)
      {
        int index = widgetEditors.getSelectionIndex();
        if (index >= 0)
        {
          TableItem       tableItem = widgetEditors.getItem(index);
          Settings.Editor editor    = (Settings.Editor)tableItem.getData();

          data.name          = editor.name;
          data.mimeType      = null;
          data.fileName      = null;
          data.commandLine   = editor.commandLine;
          data.addNewCommand = widgetAddNewCommand.getSelection();

          Settings.geometryOpenFile        = dialog.getSize();
          Settings.geometryOpenFileColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetEditors));

          Dialogs.close(dialog,true);
        }
      }
    });

    widgetCommandLine.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        widgetOpen.setEnabled(!widget.getText().trim().isEmpty());
      }
    });

    // focus traversal
    Widgets.setNextFocus(widgetMimeType,
                         widgetFileName,
                         widgetOpen
                        );

    // add editors
    for (Settings.Editor editor : Settings.editors)
    {
      TableItem tableItem = new TableItem(widgetEditors,SWT.NONE);
      tableItem.setData(editor);
      tableItem.setText(0,editor.name);
      tableItem.setText(1,editor.mimeType);
      tableItem.setText(2,editor.fileName);
      tableItem.setText(3,editor.commandLine);
    }

    // show dialog
    Dialogs.show(dialog,Settings.geometryOpenFile,Settings.setWindowLocation);

    // run
    widgetCommandLine.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      if (!data.commandLine.trim().isEmpty())
      {
        if (data.mimeType != null)
        {
          if (data.addNewCommand)
          {
            // add editor
            Settings.Editor editor = new Settings.Editor(data.name,
                                                         data.mimeType,
                                                         data.fileName,
                                                         data.commandLine
                                                        );
            Settings.editors = Arrays.copyOf(Settings.editors,Settings.editors.length+1);
            Settings.editors[Settings.editors.length-1] = editor;
          }
        }

        // get command line
        commandLine = data.commandLine;
      }
    }

    return commandLine;
  }

  /** get file open command
   * @param file file
   * @param mimeType mime type pattern
   * @return command or null
   */
  private String getFileOpenCommand(String fileName, String mimeType)
  {
    return getFileOpenCommand(new File(fileName),mimeType);
  }

  /** check if file contain TABs/trailing whitespaces
   * @param fileName file name
   * @param checkTABs TRUE to check for TABs
   * @param checkTrailingWhitespace TRUE to check for trailing whitespaces
   * @return TRUE iff file contain TABs or trailing whitespaces
   */
  private boolean containWhitespaces(String fileName, boolean checkTABs, boolean checkTrailingWhitespace)
  {
    boolean        trailingWhitespaces = false;
    boolean        trailingEmptyLines  = false;

    BufferedReader input = null;
    try
    {
      // open
      input = new BufferedReader(new FileReader(fileName));

      // check
      String        line;
      StringBuilder buffer = new StringBuilder();
      while ((line = input.readLine()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        int n = line.length();

        if (checkTrailingWhitespace)
        {
          // check if trailing whitespaces in line
          if  ((n > 0) && Character.isWhitespace(line.charAt(n-1)))
          {
            trailingWhitespaces = true;
          }
        }

        if (checkTABs)
        {
          // check for TABs
          for (int z = 0; z < n; z++)
          {
            char ch = line.charAt(z);
            if (ch == '\t')
            {
              trailingWhitespaces = true;
            }
          }
        }

        if (checkTrailingWhitespace)
        {
          // check for empty lines at end of file
          trailingEmptyLines = line.isEmpty();
        }
      }

      // close
      input.close(); input = null;
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      if (input != null) try { input.close(); } catch (IOException exception) { /* ignored */ }
    }

    return trailingWhitespaces || trailingEmptyLines;
  }

  /** check if file contain TABs
   * @param fileName file name
   * @return TRUE iff file contain TABs
   */
  protected boolean containTABs(String fileName)
  {
    return containWhitespaces(fileName,true,false);
  }

  /** check if file contain trailing whitespaces
   * @param fileName file name
   * @return TRUE iff file contain trailing whitespaces
   */
  protected boolean containTrailingWhitespaces(String fileName)
  {
    return containWhitespaces(fileName,false,true);
  }

  /** convert whitespaces in files
   * @param fileNames file name array
   * @param convertTabs TRUE iff TABs could be converted
   * @param removeTrailingWhitespaces TRUE iff trailing white spaces should be removed
   * @param spacesPerTAB number of spaces per TAB
   */
  private void convertWhitespaces(String[] fileNames, boolean convertTabs, boolean removeTrailingWhitespaces, int spacesPerTAB)
    throws IOException
  {
    if (   removeTrailingWhitespaces
        || (spacesPerTAB > 0)
       )
    {
      final String EOL = Settings.eolType.get();

      for (String fileName : fileNames)
      {
        File           tmpFile = null;
        BufferedReader input   = null;
        PrintWriter    output  = null;
        try
        {
          // open
          File file = new File(fileName);
          tmpFile = File.createTempFile("convert-",".tmp",file.getParentFile());
          input  = new BufferedReader(new FileReader(file));
          output = new PrintWriter(new FileWriter(tmpFile));

          // convert
          int           trailingEmptyLineCount = 0;
          String        line;
          StringBuilder buffer = new StringBuilder();
          while ((line = input.readLine()) != null)
          {
//Dprintf.dprintf("line=#%s#",line);
            // get length of line
            int n = line.length();

            if (removeTrailingWhitespaces)
            {
              // get length of line without trailing whitespaces
              while ((n > 0) && Character.isWhitespace(line.charAt(n-1)))
              {
                n--;
              }
            }

            // convert TABs
            buffer.setLength(0);
            for (int z = 0; z < n; z++)
            {
              char ch = line.charAt(z);
              if (convertTabs && (spacesPerTAB > 0) && (ch == '\t'))
              {
                for (int i = 0; i < spacesPerTAB; i++)
                {
                  buffer.append(' ');
                }
              }
              else
              {
                buffer.append(ch);
              }
            }

            // check if empty line
            if (removeTrailingWhitespaces && (buffer.length() == 0))
            {
              trailingEmptyLineCount++;
            }
            else
            {
              // output previous collected empty lines
              while (trailingEmptyLineCount > 0)
              {
                output.write(EOL);
                trailingEmptyLineCount--;
              }

              // output line
              output.write(buffer.toString());
              output.write(EOL);
            }
          }

          // close
          output.close(); output = null;
          input.close(); input = null;

          // rename (keep executable flag, last modified)
          boolean isExecutable = file.canExecute();
          long lastModified = file.lastModified();
          File backupFile = new File(fileName+Settings.backupFileSuffix);
          backupFile.delete();
          if (!file.renameTo(backupFile))
          {
            throw new IOException("create backup file fail");
          }
          if (!tmpFile.renameTo(file))
          {
            throw new IOException("rename temporary file fail");
          }
          tmpFile = null;
          file.setExecutable(isExecutable);
          file.setLastModified(lastModified);
        }
        finally
        {
          if (output != null) output.close();
          if (input != null) try { input.close(); } catch (IOException exception) { /* ignored */ }
          if (tmpFile != null) tmpFile.delete();
        }
      }
    }
  }

  /** convert whitespaces in file
   * @param fileName file name
   * @param convertTabs TRUE iff TABs could be converted
   * @param removeTrailingWhitespaces TRUE iff trailing white spaces should be removed
   * @param spacesPerTAB number of spaces per TAB; 0 for not converting TABs
   */
  private void convertWhitespaces(String fileName, boolean convertTabs, boolean removeTrailingWhitespaces, int spacesPerTAB)
    throws IOException
  {
    convertWhitespaces(new String[]{fileName},convertTabs,removeTrailingWhitespaces,spacesPerTAB);
  }
}

/* end of file */
