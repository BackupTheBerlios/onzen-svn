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
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Pattern;

// graphics
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
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

    FindData(RepositoryTab repositoryTab, File file)
    {
      this.repositoryTab = repositoryTab;
      this.file          = file;
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
    String  findText;
    boolean showAllFlag;
    boolean showHiddenFlag;
    boolean quitFlag;

    Data()
    {
      this.findText       = "";
      this.showAllFlag    = false;
      this.showHiddenFlag = false;
      this.quitFlag       = false;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // stored settings
  private static boolean        showAllFlag    = false;
  private static boolean        showHiddenFlag = false;

  // global variable references
  private final RepositoryTab   repositoryTab;
  private final Display         display;

  private final ArrayList<File> thisDirectoryList;
  private final ArrayList<File> allDirectoryList;

  // dialog
  private final Data            data = new Data();
  private final Shell           dialog;

  // widgets
  private final Table           widgetFiles;
  private final Text            widgetFind;
  private final Button          widgetShowAll;
  private final Button          widgetShowHidden;

//  private final Button        widgetButtonUpdate;
//  private final Button        widgetButtonCommit;
//  private final Button        widgetButtonCreatePatch;
//  private final Button        widgetButtonAdd;
//  private final Button        widgetButtonRemove;
//  private final Button        widgetButtonRevert;
//  private final Button        widgetButtonDiff;
  private final Button        widgetButtonRevisions;
//  private final Button        widgetButtonSolve;

  private final Button          widgetClose;

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
    Composite composite,subComposite;
    Label       label;
    Button      button;
    TableColumn tableColumn;
    Listener    listener;

    // initialize variables
    this.repositoryTab    = repositoryTab;
    this.thisDirectoryList    = new ArrayList<File>();
    this.thisDirectoryList.add(new File(repositoryTab.repository.rootPath));
    this.allDirectoryList = new ArrayList<File>();
    for (Repository repository : repositoryTab.onzen.getRepositoryList())
    {
      this.allDirectoryList.add(new File(repository.rootPath));
    }

    // get display
    display = shell.getDisplay();


    // add files dialog
    dialog = Dialogs.open(shell,
                          showAllFlag
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
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumn.width);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find pattern:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_SEARCH|SWT.ICON_CANCEL);
        widgetFind.setMessage("Enter find file pattern");
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);
        widgetFind.setToolTipText("Find file pattern. Use * and ? as wildcards.");
        widgetFind.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            int index = widgetFiles.getSelectionIndex();
            if (index >= 0)
            {
              TableItem tableItem = widgetFiles.getItem(index);
              FindData  findData  = (FindData)tableItem.getData();

              String fileName = findData.file.getPath();
              String mimeType = Onzen.getMimeType(fileName);
              repositoryTab.openFile(fileName,mimeType);
            }
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
        label = Widgets.newLabel(subComposite,"Show:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetShowAll = Widgets.newCheckbox(subComposite,"all repositories");
        widgetShowAll.setSelection(showAllFlag);
        Widgets.layout(widgetShowAll,0,1,TableLayoutData.W);
        widgetShowAll.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            restartFindFiles();
          }
        });

        widgetShowHidden = Widgets.newCheckbox(subComposite,"show hidden");
        widgetShowHidden.setSelection(showHiddenFlag);
        Widgets.layout(widgetShowHidden,0,2,TableLayoutData.W);
        widgetShowHidden.addSelectionListener(new SelectionListener()
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
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonRevisions = Widgets.newButton(composite,"Revisions",Settings.keyRevisions);
      widgetButtonRevisions.setEnabled(false);
      Widgets.layout(widgetButtonRevisions,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
      Widgets.layout(widgetClose,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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

          String fileName = findData.file.getPath();
          String mimeType = Onzen.getMimeType(fileName);
          repositoryTab.openFile(fileName,mimeType);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Widgets.modified(data);
      }
    });

    widgetFiles.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          Widgets.setFocus(widgetFind);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
    widgetFind.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyRevisions))
        {
          Widgets.invoke(widgetButtonRevisions);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          Widgets.setFocus(widgetFind);
        }
        else if (keyEvent.keyCode == SWT.ARROW_UP)
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
    Dialogs.show(dialog,Settings.geometryFindFiles);

    // start find files
    Background.run(new BackgroundRunnable()
    {
      String  findText       = "";
      Pattern findPattern    = null;
      boolean showAllFlag    = false;
      boolean showHiddenFlag = false;

      boolean isDataModified()
      {
        return    data.quitFlag
               || ((data.findText != null) && !data.findText.equals(findText))
               || (data.showAllFlag != showAllFlag)
               || (data.showHiddenFlag != showHiddenFlag);
      }

      public void run()
      {
        while (!data.quitFlag)
        {
          // find files
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Widgets.removeAllTableEntries(widgetFiles);
            }
          });
          if (!findText.isEmpty())
          {
//Dprintf.dprintf("findPattern=%s",findPattern);
            for (File directory : (showAllFlag) ? allDirectoryList : thisDirectoryList)
            {
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
                    if (showHiddenFlag || !repositoryTab.repository.isHiddenFile(fileName))
                    {
                      final File file = new File(directory,fileName);
                      if     (file.isFile())
                      {
                        if (   fileName.toLowerCase().contains(findText)
                            || findPattern.matcher(fileName).matches()
                           )
                        {
                          display.syncExec(new Runnable()
                          {
                            public void run()
                            {
                              FindDataComparator findDataComparator = new FindDataComparator(widgetFiles);

                              Widgets.insertTableEntry(widgetFiles,
                                                       findDataComparator,
                                                       new FindData(repositoryTab,file),
                                                       file.getName(),
                                                       file.getParent(),
                                                       Onzen.DATETIME_FORMAT.format(file.lastModified()),
                                                       Long.toString(file.length())
                                                      );
                            }
                          });
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
              findText    = new String(data.findText);
              findPattern = Pattern.compile(StringUtils.globToRegex(data.findText),Pattern.CASE_INSENSITIVE);

              // clear existing text
              data.findText = null;
            }
            showAllFlag    = data.showAllFlag;
            showHiddenFlag = data.showHiddenFlag;
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
      Widgets.setFocus(widgetFind);
      Dialogs.run(dialog,new DialogRunnable()
      {
        public void done(Object result)
        {
          // store values
          showAllFlag    = widgetShowAll.getSelection();
          showHiddenFlag = widgetShowHidden.getSelection();

          Settings.geometryFindFiles       = dialog.getSize();
          Settings.geometryFindFilesColumn = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetFiles));

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
      dialog.setText(widgetShowAll.getSelection()
                       ? "Find files"
                       : "Find files in '"+repositoryTab.repository.rootPath+"'"
                    );

      String findText = widgetFind.getText().trim().toLowerCase();
      if (!findText.isEmpty())
      {
        synchronized(data)
        {
          data.findText       = findText;
          data.showAllFlag    = widgetShowAll.getSelection();
          data.showHiddenFlag = widgetShowHidden.getSelection();
          data.notifyAll();

          Widgets.modified(data);
        }
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
}

/* end of file */
