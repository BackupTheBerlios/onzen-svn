/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandAnnotations.java,v $
* $Revision: 1.6 $
* $Author: torsten $
* Contents: command view file annotations
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
//import java.io.IOException;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
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
import org.eclipse.swt.widgets.ScrollBar;
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

/** view annotations command
 */
class CommandAnnotations
{
  /** dialog data
   */
  class Data
  {
    String[]         revisionNames;      // revision names
    AnnotationData[] annotationData;     // annotation data

    Data()
    {
      this.revisionNames  = null;
      this.annotationData = null;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final Clipboard     clipboard;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetAnnotations;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Combo         widgetRevision;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view annotations
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   * @param revision to view annotions
   */
  CommandAnnotations(Shell shell, final RepositoryTab repositoryTab, FileData fileData, String revision)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // add files dialog
    dialog = Dialogs.open(shell,"Annotations: "+fileData.getFileName(),new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevision = Widgets.newSelect(subComposite);
        widgetRevision.setEnabled(false);
        Widgets.layout(widgetRevision,0,1,TableLayoutData.WE);
        Widgets.addModifyListener(new WidgetListener(widgetRevision,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionNames != null));
          }
        });
        widgetRevision.setToolTipText("Revision to view.");
      }

      widgetAnnotations = Widgets.newTable(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      widgetAnnotations.setEnabled(false);
      Widgets.layout(widgetAnnotations,1,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetListener(widgetAnnotations,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.annotationData != null));
        }
      });
      Widgets.addTableColumn(widgetAnnotations,0,"Revision",SWT.LEFT );
      Widgets.addTableColumn(widgetAnnotations,1,"Author",  SWT.LEFT );
      Widgets.addTableColumn(widgetAnnotations,2,"Date",    SWT.LEFT );
      Widgets.addTableColumn(widgetAnnotations,3,"Line Nb.",SWT.RIGHT);
      Widgets.addTableColumn(widgetAnnotations,4,"Line",    SWT.LEFT );
      Widgets.setTableColumnWidth(widgetAnnotations,Settings.geometryAnnotationsColumn.width);
      widgetAnnotations.setToolTipText("File annotations. Double-click to view revision of line.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setEnabled(false);
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);
        Widgets.addModifyListener(new WidgetListener(widgetFind,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.annotationData != null));
          }
        });

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.W);
        Widgets.addModifyListener(new WidgetListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.annotationData != null));
          }
        });

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.W);
        Widgets.addModifyListener(new WidgetListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.annotationData != null));
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Close");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryAnnotations       = dialog.getSize();
          Settings.geometryAnnotationsColumn = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetAnnotations));

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetRevision.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if ((index >= 0) && (index < data.revisionNames.length))
        {
          show(data.revisionNames[index]);
        }
      }
    });
    widgetAnnotations.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        Table widget = (Table)mouseEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          String revision = widget.getItem(index).getText(0);
          if (!revision.equals(widgetRevision.getText()))
          {
            if (Dialogs.confirm(dialog,String.format("Show revision '%s'?",revision)))
            {
              show(revision);
            }
          }
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetAnnotations.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if (((keyEvent.stateMask & SWT.CTRL) != 0) && (keyEvent.keyCode == 'c'))
        {
          TableItem[] tableItems = widgetAnnotations.getSelection();
          Widgets.setClipboard(clipboard,tableItems,4);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });
    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetFindPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findPrev(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
      }
    });
    widgetFindNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryAnnotations);

    // start get annotations of revision
    show(revision);

    // start add revisions
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(final FileData fileData, final String revision)
      {
        // get revisions
        repositoryTab.setStatusText("Get revisions for '%s'...",fileData.getFileName());
        try
        {
          data.revisionNames = repositoryTab.repository.getRevisionNames(fileData);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,String.format("Getting revisions fail: %s",exceptionMessage));
            }
          });
          return;
        }
        repositoryTab.clearStatusText();

        if (data.revisionNames.length > 0)
        {
          // add revisions
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (!widgetRevision.isDisposed())
                {
                  int selectedIndex = -1;
                  for (int z = 0; z < data.revisionNames.length; z++)
                  {
                    widgetRevision.add(data.revisionNames[z]);
                    if (data.revisionNames[z].equals(revision))
                    {
                      selectedIndex = z;
                    }
                  }
                  if (selectedIndex == -1) selectedIndex = data.revisionNames.length-1;
                  widgetRevision.select(selectedIndex);
                }

                // notify modification
                Widgets.modified(data);
              }
            });
          }
        }
      }
    });
  }

  /** view annotations
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   * @param revisionData revision data
   */
  CommandAnnotations(Shell shell, RepositoryTab repositoryTab, FileData fileData, RevisionData revisionData)
  {
    this(shell,repositoryTab,fileData,revisionData.revision);
  }

  /** view annotations of last revision
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   */
  CommandAnnotations(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData,repositoryTab.repository.getLastRevision());
  }

  /** run dialog
   */
  public void run()
  {
    widgetFind.setFocus();
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandAnnotations {}";
  }

  //-----------------------------------------------------------------------

  /** set annotation text
   * @param annotations annotation data
   * @param widgetAnnotations annotation table widget
   */
  private void setText(AnnotationData[] annotations,
                       Table            widgetAnnotations
                      )
  {
    if (!widgetAnnotations.isDisposed())
    {
      widgetAnnotations.removeAll();
      int lineNb = 1;
      for (AnnotationData annotation : annotations)
      {
        Widgets.addTableEntry(widgetAnnotations,
                              annotation,
                              annotation.revision,
                              annotation.author,
                              Onzen.DATE_FORMAT.format(annotation.date),
                              Integer.toString(lineNb),
                              annotation.line
                             );
        lineNb++;
      }
    }
  }

  /** search previous text in annotation
   * @param widgetAnnotations annotation table widget
   * @param widgetFind search text widget
   * @return line index or -1
   */
  private int findPrev(Table widgetAnnotations, Text widgetFind)
  {
    int index = -1;

    if (!widgetAnnotations.isDisposed() && !widgetFind.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get current line index or last line
        int i = widgetAnnotations.getSelectionIndex();
        if ((i < 0) || (i >= widgetAnnotations.getItemCount()))
        {
          i = widgetAnnotations.getItemCount()-1;
        }

        // find previous line index
        boolean foundFlag = false;
        if (i > 0)
        {
          do
          {
            i--;

            TableItem tableItem = widgetAnnotations.getItem(i);
            foundFlag =    tableItem.getText(1).toLowerCase().contains(findText)
                        || tableItem.getText(4).toLowerCase().contains(findText);
          }
          while (!foundFlag && (i > 0));
        }

        if (foundFlag)
        {
          index = i;
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }

    return index;
  }

  /** search next text in annotation
   * @param widgetAnnotations annotation table widget
   * @param widgetFind search text widget
   * @return line index or -1
   */
  private int findNext(Table widgetAnnotations, Text widgetFind)
  {
    int index = -1;

    if (!widgetFind.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get current line index or first line
        int i = widgetAnnotations.getSelectionIndex();
        if ((i < 0) || (i >= widgetAnnotations.getItemCount()))
        {
          i = 0;
        }

        // find next line index
        boolean foundFlag = false;
        if ((i >= 0) && (i < widgetAnnotations.getItemCount()-1))
        {
          do
          {
            i++;

            TableItem tableItem = widgetAnnotations.getItem(i);
            foundFlag =    tableItem.getText(1).toLowerCase().contains(findText)
                        || tableItem.getText(4).toLowerCase().contains(findText);
          }
          while (!foundFlag && (i < widgetAnnotations.getItemCount()-1));
        }

        if (foundFlag)
        {
          index = i;
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }

    return index;
  }

  /** show annotations
   * @param revision revision to show
   */
  private void show(String revision)
  {
    // clear
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          data.annotationData = null;
          Widgets.modified(data);
         }
      });
    }

    // start show annotations
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, final String revision)
      {
        // get annotations
        repositoryTab.setStatusText("Get annotations for '%s'...",fileData.getFileName());
        try
        {
          data.annotationData = repositoryTab.repository.getAnnotations(fileData,revision);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file annotations fail: %s",exceptionMessage);
            }
          });
          return;
        }
        repositoryTab.clearStatusText();

        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // set new text
              setText(data.annotationData,
                      widgetAnnotations
                     );

              // notify modification
              Widgets.modified(data);

              // focus text find
              widgetFind.setFocus();
            }
          });
        }
      }
    });
  }
}

/* end of file */
