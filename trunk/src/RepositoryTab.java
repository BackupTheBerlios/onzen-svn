/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryTab.java,v $
* $Revision: 1.8 $
* $Author: torsten $
* Contents: repository tab
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.WeakHashMap;

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

import org.eclipse.swt.widgets.ScrollBar;

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
    private final static int SORTMODE_NAME     = 0;
    private final static int SORTMODE_TYPE     = 1;
    private final static int SORTMODE_SIZE     = 2;
    private final static int SORTMODE_DATETIME = 3;

    private int sortMode;

    /** create file data comparator
     * @param tree file tree
     * @param sortColumn column to sort
     */
    FileDataComparator(Tree tree, TreeColumn sortColumn)
    {
      if      (tree.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
      else if (tree.getColumn(1) == sortColumn) sortMode = SORTMODE_TYPE;
      else if (tree.getColumn(2) == sortColumn) sortMode = SORTMODE_SIZE;
      else if (tree.getColumn(3) == sortColumn) sortMode = SORTMODE_DATETIME;
      else                                      sortMode = SORTMODE_NAME;
    }

    /** create file data comparator
     * @param tree file tree
     */
    FileDataComparator(Tree tree)
    {
      this(tree,tree.getSortColumn());
    }

    /** compare file tree data without take care about type
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
        case SORTMODE_DATETIME:
          if      (fileData1.datetime.before(fileData2.datetime)) return -1;
          else if (fileData1.datetime.after(fileData2.datetime))  return  1;
          else                                                    return  0;
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
      return "FileComparator {"+sortMode+"}";
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

  // --------------------------- variables --------------------------------
  // repository
  public  final Repository    repository;           // repository instance of tab

  // global variable references
  protected final Onzen       onzen;
  private final Shell         shell;
  private final Display       display;

  // widgets
  private final TabFolder     widgetTabFolder;
  public  final Composite     widgetTab;
  private final Tree          widgetFileTree;

  // map file name -> tree item
  private WeakHashMap<String,TreeItem> fileNameMap = new WeakHashMap<String,TreeItem>();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository tab
   * @param onzen Onzen instance
   * @param parentTabFolder parent tab folder
   * @param repository repository
   */
  RepositoryTab(Onzen onzen, TabFolder parentTabFolder, Repository repository)
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

    // get shell, display
    shell   = parentTabFolder.getShell();
    display = shell.getDisplay();

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

    // create tab
    widgetTab = Widgets.addTab(parentTabFolder,repository.title,this);
//Dprintf.dprintf("");
//widgetTab.setBackground(Onzen.COLOR_YELLOW);
//Dprintf.dprintf("");
    widgetTab.setLayout(new TableLayout(1.0,1.0,2));
    Widgets.layout(widgetTab,0,0,TableLayoutData.NSWE);
    {
      // file tree
      widgetFileTree = Widgets.newTree(widgetTab,SWT.MULTI);
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
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Name",    SWT.LEFT,390,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.setToolTipText("Click to sort for name.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Status",  SWT.LEFT,160,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.setToolTipText("Click to sort for status.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Revision",SWT.LEFT,100,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.setToolTipText("Click to sort for revision.");
      treeColumn = Widgets.addTreeColumn(widgetFileTree,"Branch",  SWT.LEFT,100,true);
      treeColumn.addSelectionListener(fileTreeColumnSelectionListener);
      treeColumn.setToolTipText("Click to sort for branch.");

      menu = Widgets.newPopupMenu(shell);
      {
        menuItem = Widgets.addMenuItem(menu,"Update");
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

        menuItem = Widgets.addMenuItem(menu,"Commit...");
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

        menuItem = Widgets.addMenuItem(menu,"Create patch...");
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

        menuItem = Widgets.addMenuItem(menu,"Patches...");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            patches();
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Revert...");
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

        menuItem = Widgets.addMenuItem(menu,"Add...");
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

        menuItem = Widgets.addMenuItem(menu,"Remove...");
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

        menuItem = Widgets.addMenuItem(menu,"Rename...");
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

        menuItem = Widgets.addMenuItem(menu,"Set file mode...");
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

        menuItem = Widgets.addMenuItem(menu,"Revision info...");
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

        menuItem = Widgets.addMenuItem(menu,"Revisions...");
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

        menuItem = Widgets.addMenuItem(menu,"Diff...");
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

        menuItem = Widgets.addMenuItem(menu,"Annotations...");
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

        menuItem = Widgets.addMenuItem(menu,"Solve...");
menuItem.setEnabled(false);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
Dprintf.dprintf("");
//            solve();
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Open...");
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

        menuItem = Widgets.addMenuItem(menu,"Open with...");
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

        menuItem = Widgets.addMenuItem(menu,"New directory...");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            newDirectory(getSelectedFileData());
          }
        });

        menuItem = Widgets.addMenuItem(menu,"Rename local...");
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

        menuItem = Widgets.addMenuItem(menu,"Delete local...");
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
      }
      widgetFileTree.setMenu(menu);
    }

    // add root directory
    addRootDirectory();
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
    Widgets.setTabTitle(widgetTabFolder,widgetTab,title);
  }

  /** close repository tab
   */
  public void close()
  {
    Widgets.removeTab(widgetTabFolder,widgetTab);
  }

  /** move repository tab
   * @param index tab index (0..n-1)
   */
  public void move(int index)
  {
    Widgets.moveTab(widgetTabFolder,widgetTab,index);
  }

  /** show repository tab
   */
  public void show()
  {
    Widgets.showTab(widgetTabFolder,widgetTab);
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

  //-----------------------------------------------------------------------

  /** update states of selected entries
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

  /** update selected entries
   */
  public void update()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    try
    {
      repository.update(fileDataSet);
      asyncUpdateFileStates(fileDataSet);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Update fail (error: %s)",exception.getMessage());
      return;
    }
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

  /** revert selected entries
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

  /** pull changes
   */
  public void pullChanges()
  {
    setStatusText("Pull changes...");
    try
    {
      repository.pullChanges();
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Pulling changes fail: %s",exception.getMessage());
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
    setStatusText("Push changes...");
    try
    {
      repository.pushChanges();
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Pushing changes fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
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
      Dialogs.error(shell,"Apply patches fail: %s",exception.getMessage());
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
      Dialogs.error(shell,"Unapply patches fail: %s",exception.getMessage());
      return;
    }
    finally
    {
      clearStatusText();
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

  /** open file with external program
   * @param fileData file data
   */
  public void openFileWith(FileData fileData)
  {
    /** dialog data
     */
    class Data
    {
      String command;

      Data()
      {
        this.command = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite;
    Label       label;
    Button      button;

    // new directory dialog
    dialog = Dialogs.open(shell,"New directory",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetCommand;
    final Button widgetOpen;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Command:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetCommand = Widgets.newText(composite);
      Widgets.layout(widgetCommand,0,1,TableLayoutData.WE);
      widgetCommand.setToolTipText("Command to open file with.\nMacros:\n%file% - file name");

      button = Widgets.newButton(composite,Onzen.IMAGE_DIRECTORY);
      Widgets.layout(button,0,2,TableLayoutData.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String fileName = Dialogs.fileOpen(shell,
                                             "Select program",
                                             widgetCommand.getText(),
                                             new String[]{"All files","*",
                                                         }
                                            );
          if (fileName != null)
          {
            widgetCommand.setText(fileName);
          }
        }
      });
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetOpen = Widgets.newButton(composite,"Open");
      widgetOpen.setEnabled(false);
      Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetOpen.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.command = widgetCommand.getText();

          Dialogs.close(dialog,true);
        }
      });
      widgetOpen.setToolTipText("Open file with command.");

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
    widgetCommand.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        widgetOpen.setEnabled(!widget.getText().trim().isEmpty());
      }
    });
    widgetCommand.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetOpen.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog);

    // run
    widgetCommand.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // expand command
      String command = data.command;
      command = (command.indexOf("%file%")>=0)
                  ?command.replace("%file%",fileData.getFileName(repository.rootPath))
                  :command+" "+fileData.getFileName(repository.rootPath);

      // run command
      try
      {
        Runtime.getRuntime().exec(command);
      }
      catch (IOException exception)
      {
        Dialogs.error(shell,"Execute external command fail: \n\n'%s'\n\n (error: %s)",command,exception.getMessage());
        return;
      }
    }
  }

  /** open file with external program
   */
  public void openFileWith()
  {
    final FileData fileData = getSelectedFileData();
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
    Composite   composite,subComposite;
    Label       label;
    Button      button;

    if (fileData != null)
    {
      data.fileName = fileData.getFileName();
    }

    // new directory dialog
    dialog = Dialogs.open(shell,"New file",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

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
          else if (!file.getParentFile().canWrite())
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
    Dialogs.show(dialog);

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
Dprintf.dprintf("");
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
      data.path = fileData.getDirectoryName();
    }

    // new directory dialog
    dialog = Dialogs.open(shell,"New directory",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetPath;
    final Button widgetCreate;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Path:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetPath = Widgets.newText(composite);
      if (data.path != null) widgetPath.setText(data.path);
      widgetPath.setSelection(data.path.length(),data.path.length());
      Widgets.layout(widgetPath,1,1,TableLayoutData.WE);
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
          File file = new File(widgetPath.getText());
          if      (file.exists())
          {
            Dialogs.error(shell,"File or directory '%s' already exists!",file.getPath());
            return;
          }
          else if (!file.getParentFile().canWrite())
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
    Dialogs.show(dialog);

    // run
    widgetPath.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // create directory
      File file = new File(data.path);
      if (!file.mkdirs())
      {
        Dialogs.error(shell,"Cannot create new directory '%s'",data.path);
        return;
      }

      // update file list
Dprintf.dprintf("");
    }
  }

  /** create new directory
   */
  public void newDirectory()
  {
    newDirectory(null);
  }

  /** rename local file/directory
   */
  public void renameLocalFile()
  {
    final FileData fileData = getSelectedFileData();
    if (fileData != null)
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
      Composite   composite,subComposite;
      Label       label;
      Button      button;

      // rename file/directory dialog
      dialog = Dialogs.open(shell,"Rename file/directory",new double[]{1.0,0.0},1.0);

      final Text   widgetNewFileName;
      final Button widgetRename;
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},4));
      Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
      {
        label = Widgets.newLabel(composite,"Old file name:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        label = Widgets.newView(composite);
        label.setText(fileData.getFileName());
        Widgets.layout(label,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(composite,"New file name:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetNewFileName = Widgets.newText(composite);
        Widgets.layout(widgetNewFileName,1,1,TableLayoutData.WE);
      }

      // buttons
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetRename = Widgets.newButton(composite,"Rename");
        widgetRename.setEnabled(false);
        Widgets.layout(widgetRename,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
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
        widgetRename.setToolTipText("Rename file.");

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
      widgetNewFileName.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent modifyEvent)
        {
          Text widget = (Text)modifyEvent.widget;

          widgetRename.setEnabled(!widget.getText().equals(fileData.getFileName()));
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
      Dialogs.show(dialog,Settings.geometryRenameLocalFile.x,SWT.DEFAULT);

      // update
      data.newFileName = fileData.getFileName();
      widgetNewFileName.setText(data.newFileName);

      // run
      Widgets.setFocus(widgetNewFileName);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        // create directory
        File oldFile = new File(fileData.getFileName(repository));
        File newFile = new File(repository.rootPath,data.newFileName);
        if (!oldFile.renameTo(newFile))
        {
          Dialogs.error(shell,"Cannot rename file/directory\n\n'%s'\n\nto\n\n%s",fileData.getFileName(),data.newFileName);
          return;
        }
        fileData.setFileName(data.newFileName);

        // start update file data
        asyncUpdateFileStates(fileData);
      }
    }
  }

  /** delete local files/directories
   */
  public void deleteLocalFiles()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      final Shell dialog;
      Composite   composite,subComposite;
      Label       label;
      Button      button;

      // delete dialog
      dialog = Dialogs.open(shell,"Delete files/directories",new double[]{1.0,0.0},1.0);

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
      Dialogs.show(dialog,Settings.geometryDeleteLocalFiles);

      // add files
      for (FileData fileData : fileDataSet)
      {
        widgetFiles.add(fileData.name);
      }

      // run
      widgetCancel.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
        // delete files/directories
        boolean deleteAll  = false;
        boolean skipErrors = false;
        for (FileData fileData : fileDataSet)
        {
          File file = new File(fileData.getFileName(repository));

          // delete file/directory
          boolean deleted = false;
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
            deleted = deleteDirectory(file);
          }
          else
          {
            deleted = file.delete();
          }

          // check if deleted
          if (!deleted && !skipErrors)
          {
            switch (Dialogs.select(shell,
                                   "Error",
                                   String.format("Cannot delete file/directory '%s'.\n\nContinue?",file.getPath()),
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

        // start update file data
        asyncUpdateFileStates(fileDataSet);
      }
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "RepositoryTab {"+repository.title+"}";
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
    rootTreeItem.setText("/");
    rootTreeItem.setImage(Onzen.IMAGE_DIRECTORY);
    widgetFileTree.addListener(SWT.Expand,new Listener()
    {
      public void handleEvent(Event event)
      {
        TreeItem treeItem = (TreeItem)event.item;
        FileData fileData = (FileData)treeItem.getData();
        if (fileData.type == FileData.Types.DIRECTORY)
        {
          openFileTreeItem(treeItem);
        }
      }
    });
    widgetFileTree.addListener(SWT.Collapse,new Listener()
    {
      public void handleEvent(Event event)
      {
        TreeItem treeItem = (TreeItem)event.item;
        closeFileTreeItem(treeItem);
      }
    });
    widgetFileTree.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
//Dprintf.dprintf("mouseEvent=%s",mouseEvent);
        TreeItem treeItem = widgetFileTree.getItem(new Point(mouseEvent.x,mouseEvent.y));
        if (treeItem != null)
        {
          FileData fileData = (FileData)treeItem.getData();
          switch (fileData.type)
          {
            case DIRECTORY:
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

        if (directory.startsWith(fileData.getFileName()))
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

  /** open file (with external command)
   * @param fileData file to open
   */
  private void openFile(FileData fileData)
  {
    // find editor command with file mime-type
    String command = null;
    String mimeType = fileData.getMimeType(repository.rootPath);
    for (Settings.Editor editor : Settings.editors)
    {
      if (editor.pattern.matcher(mimeType).matches())
      {
        command = editor.command;
        break;
      }
    }

    // if no editor command found -> ask for command
    if (command == null)
    {
      /** dialog data
       */
      class Data
      {
        String  mimeType;
        String  command;
        boolean addNewCommand;

        Data()
        {
          this.mimeType      = null;
          this.command       = null;
          this.addNewCommand = false;
        }
      };

      final Data data = new Data();

      Composite composite,subComposite,subSubComposite;
      Label     label;
      Button    button;

      data.mimeType = mimeType;

      // command selection dialog
      final Shell dialog = Dialogs.open(shell,"Select command to open file",300,200,new double[]{1.0,0.0},1.0);

      // create widgets
      final Table  widgetEditors;
      final Text   widgetMimeType;
      final Text   widgetCommand;
      final Button widgetAddNewCommand;
      final Button widgetOpen;
      composite = Widgets.newComposite(dialog,SWT.NONE,4);
      composite.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,4));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
      {
        widgetEditors = Widgets.newTable(composite);
        Widgets.layout(widgetEditors,0,0,TableLayoutData.NSWE);
        Widgets.addTableColumn(widgetEditors,0,"Mime type",SWT.LEFT,100,false);
        Widgets.addTableColumn(widgetEditors,1,"Command",  SWT.LEFT,250,true );

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Mime type:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetMimeType = Widgets.newText(subComposite);
          widgetMimeType.setText(mimeType);
          Widgets.layout(widgetMimeType,0,1,TableLayoutData.WE);

          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,1,0,TableLayoutData.W);

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,1,1,TableLayoutData.WE);
          {
            widgetCommand = Widgets.newText(subSubComposite);
            Widgets.layout(widgetCommand,0,0,TableLayoutData.WE);
            widgetCommand.setToolTipText("Command to open file with.\nMacros:\n%file% - file name");

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
                                                   widgetCommand.getText(),
                                                   new String[]{"All files","*",
                                                               }
                                                  );
                if (fileName != null)
                {
                  widgetCommand.setText(fileName);
                }
              }
            });
          }

          widgetAddNewCommand = Widgets.newCheckbox(subComposite,"add as new command");
          Widgets.layout(widgetAddNewCommand,2,1,TableLayoutData.W);
        }
      }

      // buttons
      composite = Widgets.newComposite(dialog,SWT.NONE,4);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetOpen = Widgets.newButton(composite,"Open");
        widgetOpen.setEnabled(false);
        Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetOpen.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            data.mimeType      = widgetMimeType.getText();
            data.command       = widgetCommand.getText();
            data.addNewCommand = widgetAddNewCommand.getSelection();

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

            widgetCommand.setText(editor.command);
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

            data.mimeType      = null;
            data.command       = editor.command;
            data.addNewCommand = widgetAddNewCommand.getSelection();

            Dialogs.close(dialog,true);
          }
        }
      });
      Widgets.setNextFocus(widgetMimeType,widgetCommand);
      widgetCommand.addModifyListener(new ModifyListener()
      {
        public void modifyText(ModifyEvent modifyEvent)
        {
          Text widget = (Text)modifyEvent.widget;

          widgetOpen.setEnabled(!widget.getText().trim().isEmpty());
        }
      });
      Widgets.setNextFocus(widgetCommand,widgetOpen);

      // add editors
      for (Settings.Editor editor : Settings.editors)
      {
        TableItem tableItem = new TableItem(widgetEditors,SWT.NONE);
        tableItem.setData(editor);
        tableItem.setText(0,editor.mimeTypePattern);
        tableItem.setText(1,editor.command);
      }

      // run
      widgetCommand.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
        if (!data.command.isEmpty())
        {
          if (data.mimeType != null)
          {
            if (data.addNewCommand)
            {
              // add editor
              Settings.Editor editor = new Settings.Editor(data.mimeType,data.command);
              Settings.editors = Arrays.copyOf(Settings.editors,Settings.editors.length+1);
              Settings.editors[Settings.editors.length-1] = editor;
            }
          }

          // get command
          command = data.command;
        }
      }
    }
//Dprintf.dprintf("command=%s",command);

    // execute external command
    if (command != null)
    {
      // expand command
      command = (command.indexOf("%file%") >= 0)
                  ?command.replace("%file%",fileData.getFileName(repository.rootPath))
                  :command+" "+fileData.getFileName(repository.rootPath);

      // run command
      try
      {
        Runtime.getRuntime().exec(command);
      }
      catch (IOException exception)
      {
        Dialogs.error(shell,"Execute external command fail: \n\n'%s'\n\n (error: %s)",command,exception.getMessage());
        return;
      }
    }
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

    return string;
  }

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
        color = COLOR_OK;
        break;
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

  //-----------------------------------------------------------------------

  /** update file tree item
   * @param treeItem tree item to update
   * @param fileData file data
   */
  private void updateTreeItem(TreeItem treeItem, FileData fileData)
  {
    if (!treeItem.isDisposed())
    {
      treeItem.setText(1,getFileDataStateString(fileData));
      treeItem.setText(2,fileData.workingRevision);
      treeItem.setText(3,fileData.branch);
      treeItem.setForeground(Onzen.COLOR_BLACK);
      treeItem.setBackground(getFileDataBackground(fileData));
    }
  }

  /** update file tree item
   * @param treeItem tree item to update
   */
  private void updateTreeItem(TreeItem treeItem)
  {
    updateTreeItem(treeItem,(FileData)treeItem.getData());
  }

  /** update file tree items
   * @param treeItems tree items to update
   */
  private void updateTreeItems(TreeItem[] treeItems)
  {
    for (TreeItem treeItem : treeItems)
    {
      updateTreeItem(treeItem,(FileData)treeItem.getData());
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

  /** asyncronous update file state
   * @param fileDataSet file data set to update states
   */
  protected void updateFileStates(HashSet<FileData> fileDataSet)
  {
    // update tree items: status update in progress
    for (final FileData fileData : fileDataSet)
    {
//Dprintf.dprintf("fileData=%s",fileData);
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
    repository.updateStates(fileDataSet,newFileDataSet);

    // update tree items: set status color, remove not existing entries
    FileData[] fileDataArray = fileDataSet.toArray(new FileData[fileDataSet.size()]);
    for (final FileData fileData : fileDataArray)
    {
      if ((new File(fileData.getFileName(repository.rootPath)).exists()))
      {
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
        fileDataSet.remove(fileData);
//Dprintf.dprintf("removed %s",fileData);

        final TreeItem treeItem = fileNameMap.get(fileData.getFileName());
        if (treeItem != null)
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              if (!treeItem.isDisposed()) treeItem.dispose();
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
//Dprintf.dprintf("newFileData=%s",newFileData);
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
    //Dprintf.dprintf("#%s# - #%s# --- %s",fileData.getFileName(),newFileData.getDirectoryName(),fileData.getFileName().equals(newFileData.getDirectoryName()));
                  if ((fileData != null) && fileData.getFileName().equals(newFileData.getDirectoryName()))
                  {
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

  /** asyncronous update file state
   * @param fileData file data
   */
  protected void updateFileStates(final FileData fileData)
  {
    // update tree items: status update in progress
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

    // update states: set status color
    HashSet<FileData> newFileDataSet = new HashSet<FileData>();
    repository.updateStates(fileData.toSet());

    // update tree items
//Dprintf.dprintf("fileData=%s",fileData);
    display.syncExec(new Runnable()
    {
      public void run()
      {
        if (!treeItem.isDisposed()) updateTreeItem(treeItem,fileData);
      }
    });
  }

  /** asyncronous update file state
   * @param fileDataSet file data set to update states
   * @param title title to show in status line or null
   */
  protected void asyncUpdateFileStates(HashSet<FileData> fileDataSet, String title)
  {
    Background.run(new BackgroundRunnable(repository,fileDataSet,title)
    {
      public void run(Repository repository, HashSet<FileData> fileDataSet, String title)
      {
        try
        {
          onzen.setStatusText("Update status of '%s'...",(title != null) ? title : repository.title);
          updateFileStates(fileDataSet);
        }
        finally
        {
          onzen.clearStatusText();
        }
      }
    });
  }

  /** asyncronous update file state
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
        try
        {
          onzen.setStatusText("Update states of '%s'...",fileData.getFileName(repository.rootPath));
          updateFileStates(fileData);
        }
        finally
        {
          onzen.clearStatusText();
        }
      }
    });
  }

  /** delete directory tree
   * @param directory directory to delete
   * @return true if deleted, false otherwise
   */
  private boolean deleteDirectory(File directory)
  {
    File[] files = directory.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          if (!deleteDirectory(file)) return false;
        }
        else
        {
          if (!file.delete()) return false;
        }
      }
    }

    return directory.delete();
  }
}

/* end of file */
