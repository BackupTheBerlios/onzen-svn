/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRevert.java,v $
* $Revision: 1.4 $
* $Author: torsten $
* Contents: command revert files
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.io.IOException;

//import java.text.SimpleDateFormat;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.BitSet;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;
//import java.util.WeakHashMap;

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

/****************************** Classes ********************************/

/** revert command
 */
class CommandRevert
{
  /** dialog data
   */
  class Data
  {
    HashSet<FileData> fileDataSet;
    String[]          revisions;
    String            revision;

    Data()
    {
      this.fileDataSet = new HashSet<FileData>();
      this.revisions   = null;
      this.revision    = null;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final Onzen      onzen;
  private final Repository repository;
  private final Display    display;

  // dialog
  private final Data       data = new Data();
  private final Shell      dialog;        

  // widgets
  private final List       widgetFiles;   
  private final Combo      widgetRevision; 
  private final Button     widgetRevert;  

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** revert command
   * @param onzen onzen instance
   * @param shell shell
   * @param repository repository
   */
  CommandRevert(Onzen onzen, final Shell shell, final Repository repository)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.onzen      = onzen;
    this.repository = repository;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Revert files",Settings.geometryRevert.x,Settings.geometryRevert.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetFiles = Widgets.newList(composite);
      widgetFiles.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFiles,1,0,TableLayoutData.NSWE);
      widgetFiles.setToolTipText("Files to revert.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevision = Widgets.newSelect(subComposite);
        widgetRevision.setEnabled(false);
        Widgets.layout(widgetRevision,0,1,TableLayoutData.WE);
        widgetRevision.setToolTipText("Revision to revert to.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetRevert = Widgets.newButton(composite,"Revert");
      Widgets.layout(widgetRevert,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetRevert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.revision = widgetRevision.getText();

          Settings.geometryRevert = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetRevert.setToolTipText("Revert files.");

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

    // show dialog
    Dialogs.show(dialog);

    // update
    widgetRevision.add(repository.getLastRevision());
    widgetRevision.select(0);
  }

  /** revert command
   * @param onzen onzen instance
   * @param shell shell
   * @param repository repository
   * @param fileDataSet files to revert
   */
  CommandRevert(Onzen onzen, final Shell shell, final Repository repository, HashSet<FileData> fileDataSet)
  {
    this(onzen,shell,repository);

    // add files
    for (FileData fileData : fileDataSet)
    {
      data.fileDataSet.add(fileData);
      widgetFiles.add(fileData.name);
    }

    if (fileDataSet.size() == 1)
    {
      // get file data
      FileData fileData = fileDataSet.toArray(new FileData[1])[0];

      // start add revisions (only if single file is seleccted)
      Background.run(new BackgroundTask(data,repository,fileData)
      {
        public void run()
        {
          final Data       data       = (Data)      userData[0];
          final Repository repository = (Repository)userData[1];
          final FileData   fileData   = (FileData)  userData[2];

          try
          {
            // get revisions
            data.revisions = repository.getRevisions(fileData);
            if (data.revisions.length > 0)
            {
              // add revisions
              if (!display.isDisposed())
              {
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    if (!widgetRevision.isDisposed())
                    {
                      widgetRevision.removeAll();
                      for (String revision : data.revisions)
                      {
                        widgetRevision.add(revision);
                      }
                      widgetRevision.add(repository.getLastRevision());
                      widgetRevision.select(data.revisions.length);
                      widgetRevision.setEnabled(true);
                    }
                  }
                });
              }
            }
          }
          catch (RepositoryException exception)
          {
            Dialogs.error(dialog,String.format("Getting revisions fail: %s",exception));
          }
        }
      });
    }
  }

  /** revert command
   * @param onzen onzen instance
   * @param shell shell
   * @param repository repository
   * @param fileData file to revert
   */
  CommandRevert(Onzen onzen, final Shell shell, final Repository repository, FileData fileData)
  {
    this(onzen,shell,repository);

    // add file
    data.fileDataSet.add(fileData);
    widgetFiles.add(fileData.name);

    // start add revisions (only if single file is seleccted)
    Background.run(new BackgroundTask(data,repository,fileData)
    {
      public void run()
      {
        final Data data             = (Data)      userData[0];
        final Repository repository = (Repository)userData[1];
        final FileData fileData     = (FileData)  userData[2];

        try
        {
          // get revisions
          data.revisions = repository.getRevisions(fileData);
          if (data.revisions.length > 0)
          {
            // add revisions
            if (!display.isDisposed())
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  if (!widgetRevision.isDisposed())
                  {
                    widgetRevision.removeAll();
                    for (String revision : data.revisions)
                    {
                      widgetRevision.add(revision);
                    }
                    widgetRevision.add(repository.getLastRevision());
                    widgetRevision.select(data.revisions.length);
                    widgetRevision.setEnabled(true);
                  }
                }
              });
            }
          }
        }
        catch (RepositoryException exception)
        {
          Dialogs.error(dialog,String.format("Getting revisions fail: %s",exception));
        }
      }
    });
  }

  /** run dialog
   */
  public boolean run()
  {
    widgetRevision.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      try
      {
        // revert files
        repository.revert(data.fileDataSet,data.revision);

        return true;
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(dialog,
                      String.format("Cannot revert files (error: %s)",
                                    exception.getMessage()
                                   )
                     );
        return false;
      }
    }
    else
    {
      return false;
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandRevert {}";
  }

  //-----------------------------------------------------------------------
}

/* end of file */
