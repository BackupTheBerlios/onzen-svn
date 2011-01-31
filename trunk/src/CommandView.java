/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandView.java,v $
* $Revision: 1.2 $
* $Author: torsten $
* Contents: command view file
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
//import java.util.Arrays;
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

/** view file command
 */
class CommandView
{
  /** dialog data
   */
  class Data
  {
    String   revision;
    String[] revisions;
    String[] lines;

    Data()
    {
      this.revisions = null;
      this.lines     = null;
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  private final Color COLOR_DIFF_NONE         = Onzen.COLOR_WHITE;
  private final Color COLOR_DIFF_SEARCH_TEXT  = Onzen.COLOR_BLUE;

  // user events
  private final int USER_EVENT_NEW_REVISION   = 0xFFFF+0;
  private final int USER_EVENT_REFRESH_COLORS = 0xFFFF+1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final Repository repository;
  private final Display    display;
  private final FileData   fileData;

  // dialog
  private final Data       data = new Data();
  private final Shell      dialog;

  // widgets
  private final Text       widgetLineNumbers;
  private final StyledText widgetText;
  private final ScrollBar  widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Combo      widgetRevision;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view command
   * @param shell shell
   * @param repository repository
   * @param fileData file to view
   * @param revision revision to view
   */
  CommandView(final Shell shell, final Repository repository, final FileData fileData, final String revision)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // initialize variables
    this.repository = repository;
    this.fileData   = fileData;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"View: "+fileData.getFileName(),Settings.geometryView.x,Settings.geometryView.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Mode:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        label = Widgets.newLabel(subComposite);
        label.setText(fileData.mode.toString());
        Widgets.layout(label,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Size:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        label = Widgets.newLabel(subComposite);
        label.setText(Units.formatByteSize(fileData.size));
        Widgets.layout(label,1,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Date:");
        Widgets.layout(label,2,0,TableLayoutData.W);

        label = Widgets.newLabel(subComposite);
        label.setText(Onzen.DATE_FORMAT.format(fileData.datetime));
        Widgets.layout(label,2,1,TableLayoutData.WE);
      }

      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
      {
        widgetLineNumbers = Widgets.newText(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetLineNumbers,0,0,TableLayoutData.NS,0,0,0,0,60,SWT.DEFAULT);

        widgetText = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetText,0,1,TableLayoutData.NSWE);
      }
      widgetHorizontalScrollBar = subComposite.getHorizontalBar();
      widgetVerticalScrollBar   = subComposite.getVerticalBar();

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevision = Widgets.newSelect(subComposite);
        widgetRevision.setEnabled(false);
        Widgets.layout(widgetRevision,0,1,TableLayoutData.WE);
        widgetRevision.setToolTipText("Revision to view.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Close");
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
    widgetHorizontalScrollBar.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();

        // sync text widget
        widgetText.setHorizontalIndex(index);
      }
    });
    widgetVerticalScrollBar.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();
//Dprintf.dprintf("widget=%s: %d %d %d",widget,widget.getSelection(),widget.getMinimum(),widget.getMaximum());

        // sync  number text widget, text widget
        widgetLineNumbers.setTopIndex(index);
        widgetText.setTopIndex(index);
      }
    });

    widgetRevision.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        Widgets.notify(dialog,USER_EVENT_NEW_REVISION,widget.getSelectionIndex());
      }
    });

    dialog.addListener(USER_EVENT_NEW_REVISION,new Listener()
    {
      public void handleEvent(Event event)
      {
        if ((event.index >= 0) && (event.index < data.revisions.length))
        {
          // get new revision
          try
          {
            data.lines = repository.getFile(fileData,data.revisions[event.index]);
          }
          catch (RepositoryException exception)
          {
            Dialogs.error(dialog,"Getting file data fail: %s",exception.getMessage());
          }
Dprintf.dprintf("");

          // set new text
          setText(data.lines,
                  widgetLineNumbers,
                  widgetText,
                  widgetHorizontalScrollBar,
                  widgetVerticalScrollBar
                 );
        }
      }
    });

    // show dialog
    Dialogs.show(dialog);

    // update
    data.revision = revision;

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
                    int selectIndex = -1;

                    widgetRevision.removeAll();
                    for (int z = 0; z < data.revisions.length; z++)
                    {
                      widgetRevision.add(data.revisions[z]);
                      if ((data.revision != null) && data.revision.equals(data.revisions[z])) selectIndex = z;
                    }
                    widgetRevision.add(repository.getLastRevision());
                    if ((data.revision != null) && data.revision.equals(repository.getLastRevision())) selectIndex = data.revisions.length;
                    if (selectIndex < 0) selectIndex = data.revisions.length;

                    widgetRevision.select(selectIndex);
                    widgetRevision.setEnabled(true);

                    Widgets.notify(dialog,USER_EVENT_NEW_REVISION,data.revisions.length);
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

  /** view command
   * @param shell shell
   * @param repository repository
   * @param fileData file to view
   */
  CommandView(final Shell shell, final Repository repository, final FileData fileData)
  {
    this(shell,repository,fileData,null);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      widgetRevision.setFocus();
      Dialogs.run(dialog);
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandView {}";
  }

  //-----------------------------------------------------------------------

  /** set text
   * @param lines lines
   * @param widgetLineNumbers number text widget
   * @param widgetText text widget
   * @param widgetHorizontalScrollBar horizontal scrollbar widget
   * @param widgetVerticalScrollBar horizontal scrollbar widget
   */
  private void setText(String[]   lines,
                       Text       widgetLineNumbers,
                       StyledText widgetText,
                       ScrollBar  widgetHorizontalScrollBar,
                       ScrollBar  widgetVerticalScrollBar
                      )
  {
    StringBuilder lineNumbers = new StringBuilder();
    StringBuilder text        = new StringBuilder();
    int           lineNb      = 1;
    int           maxWidth    = 0;

    // get text
    for (String line : lines)
    {
      lineNumbers.append(String.format("%d\n",lineNb)); lineNb++;
      text.append(line); text.append('\n');

      maxWidth = Math.max(maxWidth,line.length());
    }

    // set text
    widgetLineNumbers.setText(lineNumbers.toString());
    widgetText.setText(text.toString());

    // set scrollbars
    widgetHorizontalScrollBar.setMinimum(0);
    widgetHorizontalScrollBar.setMaximum(maxWidth);
    widgetVerticalScrollBar.setMinimum(0);
    widgetVerticalScrollBar.setMaximum(lineNb-1);

    // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
    widgetLineNumbers.redraw();
    widgetText.redraw();
    widgetLineNumbers.update();
    widgetText.update();

    // show top
    widgetLineNumbers.setTopIndex(0);
    widgetLineNumbers.setSelection(0);
    widgetText.setTopIndex(0);
    widgetText.setCaretOffset(0);
    widgetVerticalScrollBar.setSelection(0);
  }

  /** search previous text in diff
   * @param widgetText text widget
   * @param widgetFind search text widget
   * @return
   */
  private int findPrev(StyledText widgetText, Text widgetFind)
  {
    int index = -1;

    String findText = widgetFind.getText();
    if (!findText.isEmpty())
    {
      // get cursor position, text before cursor
      int cursorIndex = widgetText.getCaretOffset();

      int offset = (cursorIndex > 0) ? widgetText.getText(0,cursorIndex-1).lastIndexOf(findText) : -1;
      if (offset >= 0)
      {
        index = offset;

        widgetText.setCaretOffset(index);
        widgetText.setSelection(index);
        widgetText.redraw();
      }
      else
      {
        Widgets.flash(widgetFind);
      }
    }

    return index;
  }

  /** search next text in diff
   * @param widgetText text widget
   * @param widgetFind search text widget
   * @return
   */
  private int findNext(StyledText widgetText, Text widgetFind)
  {
    int index = -1;

    String findText = widgetFind.getText();
    if (!findText.isEmpty())
    {
      // get cursor position, text before cursor
      int cursorIndex = widgetText.getCaretOffset();
  //Dprintf.dprintf("cursorIndex=%d: %s",cursorIndex,widgetText.getText().substring(cursorIndex+1).substring(0,100));

      // search
      int offset = widgetText.getText().substring(cursorIndex+1).indexOf(findText);
      if (offset >= 0)
      {
        index = cursorIndex+1+offset;

        widgetText.setCaretOffset(index);
        widgetText.setSelection(index);
        widgetText.redraw();
      }
      else
      {
        Widgets.flash(widgetFind);
      }
    }

    return index;
  }
}

/* end of file */
