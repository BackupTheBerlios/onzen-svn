/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandView.java,v $
* $Revision: 1.5 $
* $Author: torsten $
* Contents: command find files
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.IOException;

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
    boolean quitFlag;

    Data()
    {
      this.findText = "";
      this.quitFlag = false;
    }
  };

  // --------------------------- constants --------------------------------
  private static final int MAX_HISTORY_LENGTH = 20;

  // --------------------------- variables --------------------------------

  // find history
  private static String[]       findHistory = new String[0];

  // global variable references
  private final RepositoryTab   repositoryTab;
  private final Display         display;

  // dialog
  private final Data            data = new Data();
  private final Shell           dialog;

  // widgets
  private final Table           widgetFiles;
  private final Combo           widgetFind;
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
    this.repositoryTab = repositoryTab;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Find files in '"+repositoryTab.repository.rootPath+"'",new double[]{1.0,0.0},1.0);

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
      tableColumn = Widgets.addTableColumn(widgetFiles,0,"Name",SWT.LEFT,true);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,1,"Date",SWT.LEFT);
      tableColumn.addSelectionListener(selectionListener);
      tableColumn = Widgets.addTableColumn(widgetFiles,2,"Size [bytes]",SWT.RIGHT);
      tableColumn.addSelectionListener(selectionListener);
      Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumn.width);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find pattern:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newCombo(subComposite);
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);
        widgetFind.setToolTipText("Find file pattern. Use * and ? as wildcards.");
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
          findHistory = widgetFind.getItems();

          Settings.geometryView = dialog.getSize();

          Dialogs.close(dialog,false);
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

    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        // find files
        findFiles(widgetFind);

        // add to history
        widgetFind.add(widgetFind.getText().trim(),0);
        while (widgetFind.getItemCount() > MAX_HISTORY_LENGTH)
        {
          widgetFind.remove(MAX_HISTORY_LENGTH,MAX_HISTORY_LENGTH);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    KeyListener keyListener = new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          widgetFind.forceFocus();
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        findFiles(widgetFind);
      }
    };
    widgetFiles.addKeyListener(keyListener);
    widgetFind.addKeyListener(keyListener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryFindFiles);

    // add history
    for (String string : findHistory)
    {
      widgetFind.add(string);
    }

    // start find files
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
        String  findText    = "";
        Pattern findPattern = null;
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
            LinkedList<File> directoryList = new LinkedList<File>();
            directoryList.add(new File(repositoryTab.repository.rootPath));
            while (directoryList.size() > 0)
            {
              // check for quit/restart find
              if (data.quitFlag || ((data.findText != null) && !data.findText.equals(findText))) break;

              File directory = directoryList.removeFirst();
//Dprintf.dprintf("directory=%s",directory);
              for (final File file : directory.listFiles())
              {
                // check for quit/restart find
                if (data.quitFlag || ((data.findText != null) && !data.findText.equals(findText))) break;

                if     (file.isFile())
                {
                  final String fileName = repositoryTab.repository.getFileName(file);
//Dprintf.dprintf("file=%s %s %s",fileName,file.isHidden(),repositoryTab.repository.isHiddenFile(fileName));

                  if (   !repositoryTab.repository.isHiddenFile(fileName)
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

          // wait for new filter pattern/quit
          synchronized(data)
          {
            while (!data.quitFlag && ((data.findText == null) || data.findText.equals(findText)))
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
            if (data.findText != null)
            {
              // get new find text/pattern
              findText    = new String(data.findText);
              findPattern = Pattern.compile(StringUtils.globToRegex(data.findText),Pattern.CASE_INSENSITIVE);

              // clear existing text
              data.findText = null;
            }
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
      widgetFind.setFocus();
      Dialogs.run(dialog,new DialogRunnable()
      {
        public void done(Object result)
        {
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

  /** find files matching filter
   * @param widgetFind filter pattern widget
   */
  private void findFiles(Combo widgetFind)
  {
    if (!widgetFind.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        synchronized(data)
        {
          data.findText = findText;
          data.notifyAll();
        }
      }
    }
  }
}

/* end of file */
