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
  /** file comparator
   */
  class FileComparator implements Comparator<File>
  {
    // Note: enum in inner classes are not possible in Java, thus use the old way...
    private final static int SORTMODE_NAME = 0;
    private final static int SORTMODE_DATE = 1;
    private final static int SORTMODE_SIZE = 2;

    private int sortMode;

    /** create file data comparator
     * @param table table
     * @param sortColumn column to sort
     */
    FileComparator(Table table, TableColumn sortColumn)
    {
      if      (table.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
      else if (table.getColumn(1) == sortColumn) sortMode = SORTMODE_DATE;
      else if (table.getColumn(2) == sortColumn) sortMode = SORTMODE_SIZE;
      else                                       sortMode = SORTMODE_NAME;
    }

    /** create file data comparator
     * @param table table
     */
    FileComparator(Table table)
    {
      this(table,table.getSortColumn());
    }

    /** compare file tree data without taking care about type
     * @param fileData1, fileData2 file tree data to compare
     * @return -1 iff file1 < file2,
                0 iff file1 = file2,
                1 iff file1 > file2
     */
    public int compare(File file1, File file2)
    {
      switch (sortMode)
      {
        case SORTMODE_NAME:
          String name1 = repositoryTab.repository.getFileName(file1);
          String name2 = repositoryTab.repository.getFileName(file2);
          return name1.compareTo(name2);
        case SORTMODE_DATE:
          long lastModified1 = file1.lastModified();
          long lastModified2 = file2.lastModified();
          if      (lastModified1 < lastModified2) return -1;
          else if (lastModified1 > lastModified2) return  1;
          else                                    return  0;
        case SORTMODE_SIZE:
          long length1 = file1.length();
          long length2 = file2.length();
          if      (length1 < length2) return -1;
          else if (length1 > length2) return  1;
          else                        return  0;
        default:
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
  private static String         findText       = "";
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
          TableColumn    tableColumn    = (TableColumn)selectionEvent.widget;
          FileComparator fileComparator = new FileComparator(widgetFiles,tableColumn);

          synchronized(widgetFiles)
          {
            Widgets.sortTableColumn(widgetFiles,tableColumn,fileComparator);
          }
        }
      };
      tableColumn = Widgets.addTableColumn(widgetFiles,0,"Name",        SWT.LEFT, true );
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,1,"Date",        SWT.LEFT, false);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,2,"Size [bytes]",SWT.RIGHT,false);
      tableColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumn.width);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find pattern:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setText(findText);
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
              File      file      = (File)tableItem.getData();

              String fileName = file.getPath();
              String mimeType = repositoryTab.onzen.getMimeType(fileName);
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
          File      file      = (File)tableItem.getData();

          String fileName = file.getPath();
          String mimeType = repositoryTab.onzen.getMimeType(fileName);
          repositoryTab.openFile(fileName,mimeType);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
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
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
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

              LinkedList<File> directoryList = new LinkedList<File>();
              do
              {
//Dprintf.dprintf("directory=%s",directory);
                File[] files = directory.listFiles();
                if (files != null)
                {
                  for (final File file : files)
                  {
                    // check for modified data
                    if (isDataModified()) break;

                    if     (file.isFile())
                    {
                      final String fileName = repositoryTab.repository.getFileName(file);
    //Dprintf.dprintf("file=%s %s %s",fileName,file.isHidden(),repositoryTab.repository.isHiddenFile(fileName));

                      if (   (showHiddenFlag || !repositoryTab.repository.isHiddenFile(fileName))
                          && (   fileName.toLowerCase().contains(findText)
                              || findPattern.matcher(fileName).matches()
                             )
                         )
                      {
                        display.syncExec(new Runnable()
                        {
                          public void run()
                          {
                            FileComparator fileComparator = new FileComparator(widgetFiles);

                            Widgets.insertTableEntry(widgetFiles,
                                                     fileComparator,
                                                     file,
                                                     fileName,
                                                     Onzen.DATETIME_FORMAT.format(file.lastModified()),
                                                     Long.toString(file.length())
                                                    );
                          }
                        });
                      }
                    }
                    else if (file.isDirectory())
                    {
                      directoryList.add(file);
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
          findText       = widgetFind.getText().trim();
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
        }
      }
    }
  }
}

/* end of file */
