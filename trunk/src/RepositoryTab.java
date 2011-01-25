/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryTab.java,v $
* $Revision: 1.2 $
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
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
import java.util.ListIterator;
//import java.util.StringTokenizer;
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

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  // repository
  public  String              name;
  public  Repository          repository;

  // global variable references
  private final Shell         shell;
  private final Display       display;

  // widgets
  private TabFolder           widgetTabFolder;
  public  Composite           widgetTab;
  private Tree                widgetFileTree;

  // widget variables
//  private WidgetVariable  archiveType             = new WidgetVariable(new String[]{"normal","full","incremental","differential"});
//  private WidgetVariable  archivePartSizeFlag     = new WidgetVariable(false);
//  private WidgetVariable  archivePartSize         = new WidgetVariable(0);

  // misc
  private WeakHashMap<FileData,TreeItem> fileDataMap = new WeakHashMap<FileData,TreeItem>();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository tab
   * @param parentTabFolder parent tab folder
   * @param name name of repository
   * @param repository repository
   */
  RepositoryTab(TabFolder parentTabFolder, Repository repository)
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
    this.repository      = repository;
    this.widgetTabFolder = parentTabFolder;

    // get shell, display
    shell   = parentTabFolder.getShell();
    display = shell.getDisplay();

    // create tab
    widgetTab = Widgets.addTab(parentTabFolder,repository.title,this);
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
                case FILE:      treeItem.setImage(Onzen.IMAGE_FILE_INCLUDED);      break;
                case DIRECTORY: treeItem.setImage(Onzen.IMAGE_DIRECTORY_INCLUDED); break;
                case LINK:      treeItem.setImage(Onzen.IMAGE_LINK_INCLUDED);      break;
                case DEVICE:    treeItem.setImage(Onzen.IMAGE_FILE_INCLUDED);      break;
                case SPECIAL:   treeItem.setImage(Onzen.IMAGE_FILE_INCLUDED);      break;
                case FIFO:      treeItem.setImage(Onzen.IMAGE_FILE_INCLUDED);      break;
                case SOCKET:    treeItem.setImage(Onzen.IMAGE_FILE_INCLUDED);      break;
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
    final Text   widgetTitle;
    final Text   widgetRootPath;
    final Text   widgetMasterRepository;
    final Button widgetAdd;
    composite = Widgets.newComposite(dialog,SWT.NONE,4);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Title:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetTitle = Widgets.newText(composite);
      widgetTitle.setText(name);
      Widgets.layout(widgetTitle,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Root path:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetRootPath = Widgets.newText(composite);
      widgetRootPath.setText(repository.rootPath);
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
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;
            entryType[0] = EntryTypes.FILE;
          }
        });
        button = Widgets.newRadio(subComposite,"image");
        button.setSelection(false);
        Widgets.layout(button,0,1,TableLayoutData.W);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;
            entryType[0] = EntryTypes.IMAGE;
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
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          name = widgetTitle.getText();

          Widgets.setTabTitle(widgetTabFolder,widgetTab,name);
          repository.rootPath = widgetRootPath.getText();

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
          Button widget = (Button)selectionEvent.widget;
          Dialogs.close(dialog,false);
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

  //-----------------------------------------------------------------------

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
        for (TreeItem treeItem : parentTreeItem.getItems())
        {
          fileDataSet.add((FileData)treeItem.getData());
        }
      }
    }

    // update file data
    repository.updateStates(fileDataSet,true);
    asyncUpdateFileStatus(fileDataSet);
  }

  /** update selected entries
   */
  public void update()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    try
    {
      repository.update(fileDataSet);
      updateFileStatus(fileDataSet);
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Update fail: %s",exception.getMessage());
      return;
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
    final HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    final String[]          history = Message.getHistory();

    // get commit message
    final Shell dialog = Dialogs.open(shell,"Commit files",Settings.geometryCommit.x,Settings.geometryCommit.y,new double[]{1.0,0.0},1.0);

    final List   widgetFiles;
    final List   widgetHistory;
    final Text   widgetMessage;
    final Button widgetCommit;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to commit.");

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,3,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Up/Down/Home/End to select message from history.\n\nUse Ctrl-Return to commit.");
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
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.message = widgetMessage.getText();

          Settings.geometryCommit = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetCommit.setToolTipText("Commit files.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog,false);
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
          asyncUpdateFileStatus(fileDataSet);

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
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      try
      {
        CommandAdd commandAdd = new CommandAdd(shell,repository,fileDataSet);
        if (commandAdd.run())
        {
          asyncUpdateFileStatus(fileDataSet);
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Add fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** remove selected entries
   */
  public void remove()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      try
      {
        CommandRemove commandRemove = new CommandRemove(shell,repository,fileDataSet);
        if (commandRemove.run())
        {
          asyncUpdateFileStatus(fileDataSet);
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Remove fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** revert selected entries
   */
  public void revert()
  {
    HashSet<FileData> fileDataSet = getSelectedFileDataSet();
    if (fileDataSet != null)
    {
      try
      {
        CommandRevert commandRevert = new CommandRevert(shell,repository,fileDataSet);
        if (commandRevert.run())
        {
          asyncUpdateFileStatus(fileDataSet);
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Revert fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** rename selected entry
   */
  public void rename()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      try
      {
        CommandRename commandRename = new CommandRename(shell,repository,fileData);
        if (commandRename.run())
        {
          asyncUpdateFileStatus(fileData);
        }
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Rename fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** view selected entry
   */
  public void view()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      try
      {
        CommandView commandView = new CommandView(shell,repository,fileData);
        commandView.run();
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"View fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** diff selected entry
   */
  public void diff()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      try
      {
        CommandDiff commandDiff = new CommandDiff(shell,repository,fileData);
        commandDiff.run();
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Diff fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** show revision tree of selected entry
   */
  public void revisions()
  {
    FileData fileData = getSelectedFileData();
    if (fileData != null)
    {
      try
      {
        CommandRevisions commandRevisions = new CommandRevisions(shell,repository,fileData);
        commandRevisions.run();
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(shell,"Show revisions fail: %s",exception.getMessage());
        return;
      }
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "RepositoryTab {"+name+"}";
  }

  //-----------------------------------------------------------------------

  /** add root directory
   */
  private void addRootDirectory()
  {
    FileData rootFileData = new FileData("/",
                                         "",
                                         FileData.Types.DIRECTORY
                                        );
    TreeItem rootTreeItem = Widgets.addTreeItem(widgetFileTree,rootFileData,true);
    rootTreeItem.setText("/");
    rootTreeItem.setImage(Onzen.IMAGE_DIRECTORY);
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
          switch (fileData.type)
          {
            case DIRECTORY:
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
              break;
            case FILE:
            case LINK:
              openFile(treeItem);
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

    // open root directory
    openFileTreeDirectory(rootTreeItem);
    rootTreeItem.setExpanded(true);
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

    // get file list data
    HashSet<FileData> fileDataSet = repository.listFiles(directoryData.name);

    // add sub-tree
    for (FileData fileData : fileDataSet)
    {
      // create tree item
      subTreeItem = Widgets.addTreeItem(treeItem,findFilesTreeIndex(treeItem,fileData),fileData,false);
      subTreeItem.setText(0,fileData.title);
      subTreeItem.setImage(getFileDataImage(fileData));
//Dprintf.dprintf("fileData=%s",fileData);

      // store tree item reference
      fileDataMap.put(fileData,subTreeItem);
    }

    // start update states
    asyncUpdateFileStatus(fileDataSet);
  }

  /** close file sub-tree
   * @param treeItem tree item to close
   */
  private void closeFileTreeDirectory(TreeItem treeItem)
  {
    treeItem.removeAll();
    new TreeItem(treeItem,SWT.NONE);
  }

  /** open file (with external command)
   * @param treeItem tree item
   */
  private void openFile(TreeItem treeItem)
  {
    FileData fileData = (FileData)treeItem.getData();

    // find editor command with file mime-type
    String command = null;
    String mimeType = fileData.getMimeType(repository.rootPath);
    for (Editor editor : Settings.editors)
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
        String mimeType;
        String command;

        Data()
        {
          this.mimeType = null;
          this.command  = null;
        }
      };

      final Data data = new Data();

      Composite composite,subComposite;
      Label     label;
      Button    button;

      data.mimeType = mimeType;

      // command selection dialog
      final Shell dialog = Dialogs.open(shell,"Select command to open file",300,200,new double[]{1.0,0.0},1.0);

      // create widgets
      final Table  widgetEditors;
      final Text   widgetMimeType;
      final Text   widgetCommand;
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

          widgetCommand = Widgets.newText(subComposite);
          Widgets.layout(widgetCommand,1,1,TableLayoutData.WE);
        }
      }

      // buttons
      composite = Widgets.newComposite(dialog,SWT.NONE,4);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetOpen = Widgets.newButton(composite,"Open");
        Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetOpen.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            data.mimeType = widgetMimeType.getText();
            data.command  = widgetCommand.getText();

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
            Button widget = (Button)selectionEvent.widget;
            Dialogs.close(dialog,false);
          }
        });
      }

      // listeners
      widgetEditors.addListener(SWT.MouseDoubleClick,new Listener()
      {
        public void handleEvent(final Event event)
        {
          int index = widgetEditors.getSelectionIndex();
          if (index >= 0)
          {
            TableItem tableItem = widgetEditors.getItem(index);

            Editor editor = (Editor)tableItem.getData();

            data.mimeType = null;
            data.command  = editor.command;

            Dialogs.close(dialog,true);
          }
        }
      });
      widgetMimeType.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
          widgetCommand.setFocus();
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
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

      // add editors
      for (Editor editor : Settings.editors)
      {
        TableItem tableItem = new TableItem(widgetEditors,SWT.NONE);
        tableItem.setData(editor);
        tableItem.setText(0,editor.mimeTypePattern);
        tableItem.setText(1,editor.command);
      }

      // run
      if ((Boolean)Dialogs.run(dialog))
      {
        if (!data.command.isEmpty())
        {
          if (data.mimeType != null)
          {
            // add editor
            Editor editor = new Editor(data.mimeType,data.command);
            Settings.editors = Arrays.copyOf(Settings.editors,Settings.editors.length+1);
            Settings.editors[Settings.editors.length-1] = editor;
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
      command = command.replace("%f",fileData.getFileName(repository.rootPath));

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

  /** get file data background color
   * @param fileData file data
   * @return color
   */
  private Color getFileDataBackground(FileData fileData)
  {
    Color color = null;
    switch (fileData.state)
    {
      case OK        : color = Onzen.COLOR_WHITE;       break;
      case MODIFIED  : color = Onzen.COLOR_GREEN;       break;
      case MERGE     : color = Onzen.COLOR_DARK_YELLOW; break;
      case CONFLICT  : color = Onzen.COLOR_RED;         break;
      case REMOVED   : color = Onzen.COLOR_DARK_BLUE;   break;
      case UPDATE    : color = Onzen.COLOR_YELLOW;      break;
      case CHECKOUT  : color = Onzen.COLOR_BLUE;        break;
      case UNKNOWN   : color = Onzen.COLOR_DARK_GRAY;   break;
      case NOT_EXISTS: color = Onzen.COLOR_DARK_GRAY;   break;
      case WAITING   : color = Onzen.COLOR_WHITE;       break;
      case ADDED     : color = Onzen.COLOR_MAGENTA;     break;
      case ERROR     : color = Onzen.COLOR_DARK_RED;    break;
      default        : color = Onzen.COLOR_GRAY;        break;
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
  private void updateFileStatus(TreeItem treeItem, FileData fileData)
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
  private void updateFileStatus(TreeItem treeItem)
  {
    updateFileStatus(treeItem,(FileData)treeItem.getData());
  }

  /** update file tree item
   * @param fileData file data
   */
  private void updateFileStatus(FileData fileData)
  {
    updateFileStatus(fileDataMap.get(fileData),fileData);
  }

  /** update file tree items
   * @param treeItems tree items to update
   */
  private void updateFileStatus(TreeItem[] treeItems)
  {
    for (TreeItem treeItem : treeItems)
    {
      updateFileStatus(treeItem,(FileData)treeItem.getData());
    }
  }

  /** update file tree items
   * @param fileDataSet file data set
   */
  private void updateFileStatus(HashSet<FileData> fileDataSet)
  {
    for (FileData fileData : fileDataSet)
    {
      updateFileStatus(fileData);
    }
  }

  private void asyncUpdateFileStatus(HashSet<FileData> fileDataSet)
  {
    Background.run(new BackgroundTask(repository,fileDataSet,fileDataMap)
    {
      public void run()
      {
        Repository repository                      = (Repository)                    userData[0];
        HashSet<FileData> fileDataSet              = (HashSet<FileData>)             userData[1];
        WeakHashMap<FileData,TreeItem> fileDataMap = (WeakHashMap<FileData,TreeItem>)userData[2];

        // update states
Dprintf.dprintf("");
        repository.updateStates(fileDataSet,true);
Dprintf.dprintf("");

        // update tree items
        for (final FileData fileData : fileDataSet)
        {
          final TreeItem treeItem = fileDataMap.get(fileData);
          if (!treeItem.isDisposed())
          {
//Dprintf.dprintf("fileData=%s",fileData);
            display.syncExec(new Runnable()
            {
              public void run()
              {
                updateFileStatus(treeItem,fileData);
              }
            });
          }
        }
      }
    });
  }

  private void asyncUpdateFileStatus(FileData fileData)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);
    asyncUpdateFileStatus(fileDataSet);
  }
}

/* end of file */
