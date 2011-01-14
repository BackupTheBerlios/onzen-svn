/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryTab.java,v $
* $Revision: 1.1 $
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

//import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
import java.util.ListIterator;
//import java.util.StringTokenizer;
import java.util.WeakHashMap;

// graphics
import org.eclipse.swt.custom.SashForm;
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
          if      (fileData1.datetime < fileData2.datetime) return -1;
          else if (fileData1.datetime > fileData2.datetime) return  1;
          else                                              return  0;
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

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "FileComparator {"+sortMode+"}";
    }
  }

  /** Background thread to get directory file size of tree items.
      This thread get the number of files and total size of a
      directories and update the file-tree widget entries. Requests
      are sorted by the depth of the directory and the timeout to
      read the contents. Requests with timeout are reinserted in
      the internal sorted list with an increasing timeout. This
      make sure short running requests are processed first.
   */
  class DirectoryInfoThread extends Thread
  {
    /** directory info request structure
     */
    class DirectoryInfoRequest
    {
      String   name;
      boolean  forceFlag;
      int      depth;
      int      timeout;
      TreeItem treeItem;

      /** create directory info request
       * @param name directory name
       * @param forceFlag true to force update size
       * @param treeItem tree item
       * @param timeout timeout [ms] or -1 for no timeout
       */
      DirectoryInfoRequest(String name, boolean forceFlag, TreeItem treeItem, int timeout)
      {
        this.name      = name;
        this.forceFlag = forceFlag;
//        this.depth     = StringUtils.split(name,BARServer.fileSeparator,true).length;
        this.timeout   = timeout;
        this.treeItem  = treeItem;
      }

      /** convert data to string
       * @return string
       */
      public String toString()
      {
      return "DirectoryInfoRequest {"+name+", "+forceFlag+", "+depth+", "+timeout+"}";
      }
    };

    /* timeouts to get directory information */
    private final int DEFAULT_TIMEOUT = 1*1000;
    private final int TIMEOUT_DETLA   = 2*1000;
    private final int MAX_TIMEOUT     = 5*1000;

    /* variables */
    private Display                          display;
    private LinkedList<DirectoryInfoRequest> directoryInfoRequestList;

    /** create tree item size thread
     * @param display display
     */
    DirectoryInfoThread(Display display)
    {
      this.display                  = display;
      this.directoryInfoRequestList = new LinkedList<DirectoryInfoRequest>();
      setDaemon(true);
    }

    /**
     * @param
     * @return
     */
    public void run()
    {
      for (;;)
      {
        // get next directory info request
        final DirectoryInfoRequest directoryInfoRequest;
        synchronized(directoryInfoRequestList)
        {
          /* get next request */
          while (directoryInfoRequestList.size() == 0)
          {
            try
            {
              directoryInfoRequestList.wait();
            }
            catch (InterruptedException exception)
            {
              /* ignored */
            }
          }
          directoryInfoRequest = directoryInfoRequestList.remove();
        }

        if (directorySizesFlag || directoryInfoRequest.forceFlag)
        {
          // check if disposed tree item
          final Object[] disposedData = new Object[]{null};
          display.syncExec(new Runnable()
          {
            public void run()
            {
              TreeItem treeItem = directoryInfoRequest.treeItem;
              disposedData[0] = (Boolean)treeItem.isDisposed();
            }
          });
          if ((Boolean)disposedData[0])
          {
            /* disposed -> skip */
            continue;
          }

          /* get file count, size */

          /* update view */
//Dprintf.dprintf("name=%s count=%d size=%d timeout=%s\n",directoryInfoRequest.name,count,size,timeoutFlag);
          display.syncExec(new Runnable()
          {
            public void run()
            {
              TreeItem treeItem = directoryInfoRequest.treeItem;
              if (!treeItem.isDisposed())
              {
//Dprintf.dprintf("update %s\n",treeItem.isDisposed());
//                treeItem.setText(2,Units.formatByteSize(size));
//                treeItem.setForeground(2,timeoutFlag?COLOR_RED:COLOR_BLACK);
              }
            }
          });
        }
      }
    }

    /** get index of directory info request in list
     * @param directoryInfoRequest directory info request
     * @return index or 0
     */
    private int getIndex(DirectoryInfoRequest directoryInfoRequest)
    {
//Dprintf.dprintf("find index %d: %s\n",directoryInfoRequestList.size(),directoryInfoRequest);
      /* find new position in list */
      ListIterator<DirectoryInfoRequest> listIterator = directoryInfoRequestList.listIterator();
      boolean                            foundFlag = false;
      int                                index = 0;
      while (listIterator.hasNext() && !foundFlag)
      {
        index = listIterator.nextIndex();

        DirectoryInfoRequest nextDirectoryInfoRequest = listIterator.next();
        foundFlag = (   (directoryInfoRequest.depth > nextDirectoryInfoRequest.depth)
                     || (directoryInfoRequest.timeout < nextDirectoryInfoRequest.timeout)
                    );
      }
//Dprintf.dprintf("found index=%d\n",index);

      return index;
    }

    /** add directory info request
     * @param directoryInfoRequest directory info request
     */
    private void add(DirectoryInfoRequest directoryInfoRequest)
    {
      synchronized(directoryInfoRequestList)
      {
        int index = getIndex(directoryInfoRequest);
        directoryInfoRequestList.add(index,directoryInfoRequest);
        directoryInfoRequestList.notifyAll();
      }
    }

    /** add directory info request
     * @param name path name
     * @param forceFlag true to force update
     * @param treeItem tree item
     * @param timeout timeout [ms]
     */
    public void add(String name, boolean forceFlag, TreeItem treeItem, int timeout)
    {
      DirectoryInfoRequest directoryInfoRequest = new DirectoryInfoRequest(name,forceFlag,treeItem,timeout);
      add(directoryInfoRequest);
    }

    /** add directory info request
     * @param name path name
     * @param treeItem tree item
     * @param timeout timeout [ms]
     */
    public void add(String name, TreeItem treeItem, int timeout)
    {
      DirectoryInfoRequest directoryInfoRequest = new DirectoryInfoRequest(name,false,treeItem,timeout);
      add(directoryInfoRequest);
    }

    /** add directory info request with default timeout
     * @param name path name
     * @param forceFlag true to force update
     * @param treeItem tree item
     */
    public void add(String name, boolean forceFlag, TreeItem treeItem)
    {
      add(name,forceFlag,treeItem,DEFAULT_TIMEOUT);
    }

    /** add directory info request with default timeout
     * @param name path name
     * @param treeItem tree item
     */
    public void add(String name, TreeItem treeItem)
    {
      add(name,false,treeItem,DEFAULT_TIMEOUT);
    }

    /** clear all directory info requests
     * @param treeItem tree item
     */
    public void clear()
    {
      synchronized(directoryInfoRequestList)
      {
        directoryInfoRequestList.clear();
      }
    }
  }

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  // repository
  public  String       name;
  public  Repository   repository;

  // colors
  private final Color  COLOR_BLACK;
  private final Color  COLOR_WHITE;
  private final Color  COLOR_GREEN;
  private final Color  COLOR_DARK_RED;
  private final Color  COLOR_RED;
  private final Color  COLOR_DARK_BLUE;
  private final Color  COLOR_BLUE;
  private final Color  COLOR_DARK_YELLOW;
  private final Color  COLOR_YELLOW;
  private final Color  COLOR_DARK_GRAY;
  private final Color  COLOR_GRAY;
  private final Color  COLOR_MAGENTA;

  // images
  private final Image  IMAGE_DIRECTORY;
  private final Image  IMAGE_FILE;
  private final Image  IMAGE_LINK;

  // date/time format
  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  // cursors
  private final Cursor        waitCursor;

  // global variable references
  private Shell               shell;

  // widgets
  private TabFolder           widgetTabFolder;
  public  Composite           widgetTab;
  private Tree                widgetFileTree;

  // widget variables
//  private WidgetVariable  archiveType             = new WidgetVariable(new String[]{"normal","full","incremental","differential"});
//  private WidgetVariable  archivePartSizeFlag     = new WidgetVariable(false);
//  private WidgetVariable  archivePartSize         = new WidgetVariable(0);

  // misc
  private DirectoryInfoThread directoryInfoThread;
  private boolean             directorySizesFlag     = false;

  private WeakHashMap<FileData,TreeItem> fileDataMap = new WeakHashMap<FileData,TreeItem>();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository tab
   * @param parentTabFolder parent tab folder
   * @param name name of repository
   * @param repository repository
   */
  RepositoryTab(TabFolder parentTabFolder, String name, Repository repository)
  {
    Display    display;
    Menu       menu;
    MenuItem   menuItem;
    Composite  composite;
    Label      label;
    Button     button;
    TreeColumn treeColumn;
    TreeItem   treeItem;
    Text       text;

    // initialize variables
    this.name            = name;
    this.repository      = repository;
    this.widgetTabFolder = parentTabFolder;

    // get shell, display
    shell   = parentTabFolder.getShell();
    display = shell.getDisplay();

    // get colors
    COLOR_BLACK       = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);
    COLOR_WHITE       = shell.getDisplay().getSystemColor(SWT.COLOR_WHITE);
    COLOR_GREEN       = shell.getDisplay().getSystemColor(SWT.COLOR_GREEN);
    COLOR_DARK_RED    = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
    COLOR_RED         = shell.getDisplay().getSystemColor(SWT.COLOR_RED);
    COLOR_DARK_BLUE   = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
    COLOR_BLUE        = shell.getDisplay().getSystemColor(SWT.COLOR_BLUE);
    COLOR_DARK_YELLOW = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
    COLOR_YELLOW      = shell.getDisplay().getSystemColor(SWT.COLOR_YELLOW);    
    COLOR_DARK_GRAY   = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
    COLOR_GRAY        = shell.getDisplay().getSystemColor(SWT.COLOR_GRAY);
    COLOR_MAGENTA     = new Color(null,0xFF,0xA0,0xA0);                         

    // get images
    IMAGE_DIRECTORY = Widgets.loadImage(display,"directory.png");
    IMAGE_FILE      = Widgets.loadImage(display,"file.png");
    IMAGE_LINK      = Widgets.loadImage(display,"link.png");

    // get cursors
    waitCursor = new Cursor(display,SWT.CURSOR_WAIT);

    // start tree item size thread
//    directoryInfoThread = new DirectoryInfoThread(display);
//    directoryInfoThread.start();

    // create tab
    widgetTab = Widgets.addTab(parentTabFolder,name,this);
    widgetTab.setLayout(new TableLayout(1.0,1.0,2));
    Widgets.layout(widgetTab,0,0,TableLayoutData.NSWE);
    {
      // file tree
      widgetFileTree = Widgets.newTree(widgetTab,SWT.NONE);
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
/*
        menuItem = Widgets.addMenuItem(menu,"Open/Close");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            MenuItem widget = (MenuItem)selectionEvent.widget;

            TreeItem[] treeItems = widgetFileTree.getSelection();
            if (treeItems != null)
            {
              FileData fileData = (FileData)treeItems[0].getData();
              if (fileData.type == Types.DIRECTORY)
              {
                Event treeEvent = new Event();
                treeEvent.item = treeItems[0];
                if (treeItems[0].getExpanded())
                {
                  widgetFileTree.notifyListeners(SWT.Collapse,treeEvent);
                  treeItems[0].setExpanded(false);
                }
                else
                {
                  widgetFileTree.notifyListeners(SWT.Expand,treeEvent);
                  treeItems[0].setExpanded(true);
                }
              }
            }
          }
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
        });

        menuItem = Widgets.addMenuSeparator(menu);

        menuItem = Widgets.addMenuItem(menu,"Include");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            MenuItem widget = (MenuItem)selectionEvent.widget;

            for (TreeItem treeItem : widgetFileTree.getSelection())
            {
              FileData fileData = (FileData)treeItem.getData();
              includeAdd(EntryTypes.FILE,fileData.name);
              excludeRemove(fileData.name);
              switch (fileData.type)
              {
                case FILE:      treeItem.setImage(IMAGE_FILE_INCLUDED);      break;
                case DIRECTORY: treeItem.setImage(IMAGE_DIRECTORY_INCLUDED); break;
                case LINK:      treeItem.setImage(IMAGE_LINK_INCLUDED);      break;
                case DEVICE:    treeItem.setImage(IMAGE_FILE_INCLUDED);      break;
                case SPECIAL:   treeItem.setImage(IMAGE_FILE_INCLUDED);      break;
                case FIFO:      treeItem.setImage(IMAGE_FILE_INCLUDED);      break;
                case SOCKET:    treeItem.setImage(IMAGE_FILE_INCLUDED);      break;
              }
            }
          }
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
        });
*/
      }
      widgetFileTree.setMenu(menu);
      widgetFileTree.setToolTipText("Tree representation of files.\nDouble-click to open sub-directories, right-click to open context menu.");
    }
/*
    Widgets.addEventListener(new WidgetEventListener(widgetTabFolder,selectJobEvent)
    {
      public void trigger(Control control)
      {
        Widgets.setEnabled(control,selectedJobId != 0);
      }
    });
*/

    // add root devices
    addDirectoryRoot();
  }

  /** close repository tab
   */
  public void close()
  {
    Widgets.removeTab(widgetTabFolder,widgetTab);
  }

  /** edit repository tab
   */
  public void edit()
  {
    Composite composite;
    Label     label;
    Button    button;

    // create dialog
    final Shell dialog = Dialogs.open(shell,"Edit repository",300,70,new double[]{1.0,0.0},1.0);

    // create widgets
    final Text   widgetName;
    final Text   widgetRootPath;
    final Text   widgetMasterRepository;
    final Button widgetAdd;
    composite = Widgets.newComposite(dialog,SWT.NONE,4);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetName = Widgets.newText(composite);
      widgetName.setText(name);
      Widgets.layout(widgetName,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Root path:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetRootPath = Widgets.newText(composite);
      widgetRootPath.setText(repository.getRootPath());
      Widgets.layout(widgetRootPath,1,1,TableLayoutData.WE);

/*
      label = Widgets.newLabel(composite,"Type:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(0.0,0.0));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        button = Widgets.newRadio(subComposite,"file");
        button.setSelection(true);
        Widgets.layout(button,0,0,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;
            entryType[0] = EntryTypes.FILE;
          }
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
        });
        button = Widgets.newRadio(subComposite,"image");
        button.setSelection(false);
        Widgets.layout(button,0,1,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;
            entryType[0] = EntryTypes.IMAGE;
          }
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
        });
      }
*/
    }

    // buttons
    composite = Widgets.newComposite(dialog,SWT.NONE,4);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save");
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          name = widgetName.getText();
          Widgets.setTabTitle(widgetTabFolder,widgetTab,name);
          repository.setRootPath(widgetRootPath.getText());

          Dialogs.close(dialog,true);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }
/*
    // add selection listeners
    widgetPattern.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetAdd.forceFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
throw new Error("NYI");
      }
    });
    widgetAdd.addSelectionListener(new SelectionListener()
    {
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Button widget = (Button)selectionEvent.widget;
        pattern[0] = widgetPattern.getText().trim();
        Dialogs.close(dialog,true);
      }
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
    });
*/

    Dialogs.run(dialog);
  }

  /** update states of selected entries
   */
  public void updateStates()
  {
    TreeItem[] selectedTreeItems = widgetFileTree.getSelection();

    // get file data (selected and all in same directory)
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (TreeItem selectedTreeItem : selectedTreeItems)
    {
      fileDataSet.add((FileData)selectedTreeItem.getData());

      TreeItem parentTreeItem = selectedTreeItem.getParentItem();
      if (parentTreeItem != null)
      {
Dprintf.dprintf("");
        for (TreeItem treeItem : parentTreeItem.getItems())
        {
          fileDataSet.add((FileData)treeItem.getData());
        }
      }
    }

    // update file data
    repository.updateStates(fileDataSet,true);

    // update tree
    for (TreeItem selectedTreeItem : selectedTreeItems)
    {
      updateFileTree(selectedTreeItem);

      TreeItem parentTreeItem = selectedTreeItem.getParentItem();
      if (parentTreeItem != null)
      {
        updateFileTree(parentTreeItem.getItems());
      }
    }
  }

  /** update selected entries
   */
  public void update()
  {
    HashSet<FileData> fileDataSet = getSelectedFileData();
    try
    {
      repository.update(fileDataSet);
      updateFileTree(fileDataSet);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Update fail: %s",exception.getMessage());
    }
  }

  /** update selected entries
   */
  public void commit()
  {
    // dialog data
    class Data
    {
      String message;

      Data()
      {
        this.message = "";
      }
    };

    // variables
    final Data              data = new Data();
    Composite               composite;
    Label                   label;
    Table                   table;
    Button                  button;
    final HashSet<FileData> fileDataSet = getSelectedFileData();
    final String[]          history = Message.getHistory();

    // get commit message
    final Shell dialog = Dialogs.open(shell,"Commit files",Settings.commitGeometry.x,Settings.commitGeometry.y,new double[]{1.0,0.0},1.0);

    final Text   widgetMessage;
    final List   widgetFiles;
    final List   widgetHistory;
    final Button widgetCommit;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to commit.");

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(COLOR_GRAY);
      Widgets.layout(widgetHistory,3,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newTextArea(composite);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message. Use Ctrl-Up/Down/Home/End to select message from history. Use Ctrl-Return to commit.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetCommit,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.message = widgetMessage.getText();

          Settings.commitGeometry = dialog.getSize();

          Dialogs.close(dialog,true);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
      widgetCommit.setToolTipText("Commit files.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }

    // listeners
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          widgetMessage.setText(history[i]);
          widgetMessage.setFocus();
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetMessage.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        Text widget = (Text)keyEvent.widget;

        if ((keyEvent.stateMask & SWT.CTRL) != 0)
        {
          int i = widgetHistory.getSelectionIndex();

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i >= 0)
            {
              if (i < history.length-1)
              {
                widgetHistory.setSelection(i+1);
                widgetMessage.setText(history[i+1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i >= 0)
            {
              if (i > 0)
              {
                widgetHistory.setSelection(i-1);
                widgetMessage.setText(history[i-1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(0);
              widgetMessage.setText(history[0]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.END)
          {
            // last history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(history.length-1);
              widgetMessage.setText(history[history.length-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke add-button
            Widgets.invoke(widgetCommit);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // add files
    for (FileData fileData : fileDataSet)
    {
      widgetFiles.add(fileData.name);
    }

    // add history
    for (String string : history)
    {
      widgetHistory.add(string.replaceAll("\n","\\\\n"));
    }

    // run
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      try
      {
        Message message = null;
        try
        {
          // commit files
          message = new Message(data.message);
          repository.commit(fileDataSet,message);

          // update view
          updateFileTree(fileDataSet);

          // store history
          Message.addHistory(data.message);
        }
        finally
        {
          message.done();
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Commit fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** add selected entries
   */
  public void add()
  {
    // dialog data
    class Data
    {
      String  message;
      boolean binaryFlag;

      Data()
      {
        this.message    = "";
        this.binaryFlag = false;
      }
    };

    // variables
    final Data              data = new Data();
    Composite               composite;
    Label                   label;
    Table                   table;
    Button                  button;
    final HashSet<FileData> fileDataSet = getSelectedFileData();
    final String[]          history = Message.getHistory();

    // get add message
    final Shell dialog = Dialogs.open(shell,"Add files",Settings.addGeometry.x,Settings.addGeometry.y,new double[]{1.0,0.0},1.0);

    final Text   widgetMessage;
    final List   widgetFiles;
    final List   widgetHistory;
    final Button widgetBinary;
    final Button widgetAdd;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to add.");

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(COLOR_GRAY);
      Widgets.layout(widgetHistory,3,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newTextArea(composite);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message. Use Ctrl-Up/Down/Home/End to select message from history. Use Ctrl-Return to commit.");

      widgetBinary = Widgets.newCheckbox(composite,"binary");
      Widgets.layout(widgetBinary,6,0,TableLayoutData.W);
      widgetBinary.setToolTipText("Select this checkbox to add binary files.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetAdd = Widgets.newButton(composite,"Add");
      Widgets.layout(widgetAdd,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.message    = widgetMessage.getText();
          data.binaryFlag = widgetBinary.getSelection();

          Settings.addGeometry = dialog.getSize();

          Dialogs.close(dialog,true);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
      widgetAdd.setToolTipText("Add files.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }

    // listeners
    widgetHistory.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        List widget = (List)mouseEvent.widget;

        int i = widget.getSelectionIndex();
        if (i >= 0)
        {
          widgetMessage.setText(history[i]);
          widgetMessage.setFocus();
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetMessage.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        Text widget = (Text)keyEvent.widget;

        if ((keyEvent.stateMask & SWT.CTRL) != 0)
        {
          int i = widgetHistory.getSelectionIndex();

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i >= 0)
            {
              if (i < history.length-1)
              {
                widgetHistory.setSelection(i+1);
                widgetMessage.setText(history[i+1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i >= 0)
            {
              if (i > 0)
              {
                widgetHistory.setSelection(i-1);
                widgetMessage.setText(history[i-1]);
                widgetMessage.setFocus();
              }
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(0);
              widgetMessage.setText(history[0]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.END)
          {
            // last history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(history.length-1);
              widgetMessage.setText(history[history.length-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke add-button
            Widgets.invoke(widgetAdd);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // add files
    for (FileData fileData : fileDataSet)
    {
      widgetFiles.add(fileData.name);
    }

    // add history
    for (String string : history)
    {
      widgetHistory.add(string.replaceAll("\n","\\\\n"));
    }

    // run
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      try
      {
        Message message = null;
        try
        {
          // add files
          message = new Message(data.message);
          repository.add(fileDataSet,message,data.binaryFlag);

          // update view
          repository.updateStates(fileDataSet);
          updateFileTree(fileDataSet);

          // store history
          Message.addHistory(data.message);
        }
        finally
        {
          message.done();
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Add fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "{RepositoryTab "+name+"}";
  }

  //-----------------------------------------------------------------------

  /** add directory root
   */
  private void addDirectoryRoot()
  {
    FileData rootFileData = new FileData("/",
                                         "",
                                         FileData.Types.DIRECTORY
                                        );
    TreeItem rootTreeItem = Widgets.addTreeItem(widgetFileTree,rootFileData,true);
    rootTreeItem.setText("/");
    rootTreeItem.setImage(IMAGE_DIRECTORY);
    widgetFileTree.addListener(SWT.Expand,new Listener()
    {
      public void handleEvent(final Event event)
      {
        final TreeItem treeItem = (TreeItem)event.item;
        FileData       fileData = (FileData)treeItem.getData();
        if (fileData.type == FileData.Types.DIRECTORY)
        {
          openFileTreeDirectory(treeItem);
        }
      }
    });
    widgetFileTree.addListener(SWT.Collapse,new Listener()
    {
      public void handleEvent(final Event event)
      {
        final TreeItem treeItem = (TreeItem)event.item;
        closeFileTreeDirectory(treeItem);
      }
    });
    widgetFileTree.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(final MouseEvent mouseEvent)
      {
        TreeItem treeItem = widgetFileTree.getItem(new Point(mouseEvent.x,mouseEvent.y));
        if (treeItem != null)
        {
          FileData fileData = (FileData)treeItem.getData();
          if (fileData.type == FileData.Types.DIRECTORY)
          {
            Event treeEvent = new Event();
            treeEvent.item = treeItem;
            if (treeItem.getExpanded())
            {
              widgetFileTree.notifyListeners(SWT.Collapse,treeEvent);
              treeItem.setExpanded(false);
            }
            else
            {
              widgetFileTree.notifyListeners(SWT.Expand,treeEvent);
              treeItem.setExpanded(true);
            }
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

    // open root
    rootTreeItem.setExpanded(true);
    openFileTreeDirectory(rootTreeItem);
    /*
    Event treeEvent = new Event();
    treeEvent.item = rootTreeItem;
    widgetFileTree.notifyListeners(SWT.Expand,treeEvent);
    */
  }

  /** clear file tree, close all sub-directories
   */
  private void clearFileTree()
  {
    // close all directories
    for (TreeItem treeItem : widgetFileTree.getItems())
    {
      treeItem.removeAll();
      new TreeItem(treeItem,SWT.NONE);
    }

    // clear directory info requests
//    directoryInfoThread.clear();
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
    TreeItem           subTreeItems[]     = treeItem.getItems();
    FileDataComparator fileDataComparator = new FileDataComparator(widgetFileTree);

    int index = 0;
    while (   (index < subTreeItems.length)
           && (fileDataComparator.compare(fileData,(FileData)subTreeItems[index].getData()) > 0)
          )
    {
      index++;
    }

    return index;
  }

  /** open file sub-tree
   * @param treeItem parent tree item
   */
  private void openFileTreeDirectory(TreeItem treeItem)
  {
    FileData directoryData = (FileData)treeItem.getData();
    TreeItem subTreeItem;

    // remove existing sub-items
    treeItem.removeAll();

    // get file data
    HashSet<FileData> fileDataSet = repository.listFiles(directoryData.name);

    // update states
    repository.updateStates(fileDataSet,true);

    // add sub-tree
    for (FileData fileData : fileDataSet)
    {
      // create tree item
      subTreeItem = Widgets.addTreeItem(treeItem,findFilesTreeIndex(treeItem,fileData),fileData,false);
      subTreeItem.setText(0,fileData.title);
      subTreeItem.setImage(getFileDataImage(fileData));
//Dprintf.dprintf("fileData=%s",fileData);
      updateFileTree(subTreeItem,fileData);

      // store tree item reference
      fileDataMap.put(fileData,subTreeItem);
    }
  }

  /** close file sub-tree
   * @param treeItem tree item to close
   */
  private void closeFileTreeDirectory(TreeItem treeItem)
  {
    treeItem.removeAll();
    new TreeItem(treeItem,SWT.NONE);
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
      case DIRECTORY: image = IMAGE_DIRECTORY; break;
      case FILE:      image = IMAGE_FILE;      break;
      case LINK:      image = IMAGE_LINK;      break;
      default:        image = IMAGE_FILE;      break;
    }

    return image;
  }

  /** get file data background color
   * @param fileData file data
   * @return color
   */
  private Color getFileDataBackground(FileData fileData)
  {
    Color color = null;
    switch (fileData.state)
    {
      case OK        : color = COLOR_WHITE;       break;
      case MODIFIED  : color = COLOR_GREEN;       break;
      case MERGE     : color = COLOR_DARK_YELLOW; break;
      case CONFLICT  : color = COLOR_RED;         break;
      case REMOVED   : color = COLOR_DARK_BLUE;   break;
      case UPDATE    : color = COLOR_YELLOW;      break;
      case CHECKOUT  : color = COLOR_BLUE;        break;
      case UNKNOWN   : color = COLOR_DARK_GRAY;   break;
      case NOT_EXISTS: color = COLOR_DARK_GRAY;   break;
      case WAITING   : color = COLOR_WHITE;       break;
      case ADDED     : color = COLOR_MAGENTA;     break;
      case ERROR     : color = COLOR_DARK_RED;    break;
      default        : color = COLOR_GRAY;        break;
    }

    return color;
  }

  /** get selected file data
   * @return selected file data hash
   */
  private HashSet<FileData> getSelectedFileData()
  {
    TreeItem[] selectedTreeItems = widgetFileTree.getSelection();

    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (TreeItem selectedTreeItem : selectedTreeItems)
    {
      fileDataSet.add((FileData)selectedTreeItem.getData());
    }

    return fileDataSet;
  }

  /** update file tree item
   * @param treeItem tree item to update
   * @param fileData file data
   */
  private void updateFileTree(TreeItem treeItem, FileData fileData)
  {
    treeItem.setText(1,fileData.state.toString());
    treeItem.setText(2,fileData.workingRevision);
//    treeItem.setText(3,simpleDateFormat.format(new Date(fileData.datetime*1000)));
//      subTreeItem.setText(2,Units.formatByteSize(fileData.size));
    treeItem.setText(3,fileData.branch);
    treeItem.setBackground(getFileDataBackground(fileData));
  }

  /** update file tree item
   * @param treeItem tree item to update
   */
  private void updateFileTree(TreeItem treeItem)
  {
    updateFileTree(treeItem,(FileData)treeItem.getData());
  }

  /** update file tree item
   * @param fileData file data
   */
  private void updateFileTree(FileData fileData)
  {
    updateFileTree(fileDataMap.get(fileData),fileData);
  }

  /** update file tree items
   * @param treeItems tree items to update
   */
  private void updateFileTree(TreeItem[] treeItems)
  {
    for (TreeItem treeItem : treeItems)
    {
      updateFileTree(treeItem,(FileData)treeItem.getData());
    }
  }

  /** update file tree items
   * @param fileDataSet file data set
   */
  private void updateFileTree(HashSet<FileData> fileDataSet)
  {
    for (FileData fileData : fileDataSet)
    {
      updateFileTree(fileData);
    }
  }
}

/* end of file */
