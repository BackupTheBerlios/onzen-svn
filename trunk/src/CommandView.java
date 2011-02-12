/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandView.java,v $
* $Revision: 1.5 $
* $Author: torsten $
* Contents: command view file
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

import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.BitSet;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
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
    String[] revisionNames;      // revision names
    String[] lines;              // file lines

    Data()
    {
      this.revisionNames = null;
      this.lines         = null;
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  private final Color COLOR_VIEW_NONE        = Onzen.COLOR_WHITE;
  private final Color COLOR_VIEW_SEARCH_TEXT = Onzen.COLOR_BLUE;

  // user events
  private final int USER_EVENT_NEW_REVISION  = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Combo         widgetRevision;
  private final Text          widgetLineNumbers;
  private final StyledText    widgetText;
  private final ScrollBar     widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Button        widgetButtonClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view
   * @param revision revision to view
   */
  CommandView(final Shell shell, final RepositoryTab repositoryTab, final FileData fileData, final String revision)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;
    Listener  listener;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"View: "+fileData.getFileName(),new double[]{1.0,0.0},1.0);

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
        widgetRevision.setToolTipText("Revision to view.");
      }

      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
      {
        widgetLineNumbers = Widgets.newText(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        widgetLineNumbers.setForeground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetLineNumbers,0,0,TableLayoutData.NS,0,0,0,0,60,SWT.DEFAULT);
        Widgets.addModifyListener(new WidgetListener(widgetLineNumbers,data)
        {
          public void modified(Control control)
          {
            control.setForeground((data.lines != null) ? null : Onzen.COLOR_GRAY);
          }
        });

        widgetText = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        widgetText.setForeground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetText,0,1,TableLayoutData.NSWE);
        Widgets.addModifyListener(new WidgetListener(widgetText,data)
        {
          public void modified(Control control)
          {
            control.setForeground((data.lines != null) ? null : Onzen.COLOR_GRAY);
          }
        });
      }
      widgetHorizontalScrollBar = subComposite.getHorizontalBar();
      widgetVerticalScrollBar   = subComposite.getVerticalBar();

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.NSWE);
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
            Widgets.setEnabled(control,(data.lines != null));
          }
        });

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.W);
        Widgets.addModifyListener(new WidgetListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.lines != null));
          }
        });

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.W);
        Widgets.addModifyListener(new WidgetListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.lines != null));
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetButtonClose,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryView = dialog.getSize();

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
        if ((data.revisionNames != null) && (index >= 0) && (index < data.revisionNames.length))
        {
          show(data.revisionNames[index]);
        }
      }
    });

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        Text widget = (Text)event.widget;
        int  topIndex = widget.getTopIndex();
//Dprintf.dprintf("%d %d",widget.getTopPixel(),widgetText.getTopPixel());

        widgetText.setTopIndex(topIndex);
//        widgetText.setTopPixel(widget.getTopPixel());
      }
    };
    widgetLineNumbers.addListener(SWT.KeyDown,listener);
    widgetLineNumbers.addListener(SWT.KeyUp,listener);
    widgetLineNumbers.addListener(SWT.MouseDown,listener);
    widgetLineNumbers.addListener(SWT.MouseUp,listener);
    widgetLineNumbers.addListener(SWT.MouseMove,listener);
    widgetLineNumbers.addListener(SWT.Resize,listener);

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        StyledText widget = (StyledText)event.widget;
        int        topIndex = widget.getTopIndex();
//Dprintf.dprintf("widget=%s: %d",widget,widget.getTopIndex());

        widgetLineNumbers.setTopIndex(topIndex);
      }
    };
    widgetText.addListener(SWT.KeyDown,listener);
    widgetText.addListener(SWT.KeyUp,listener);
    widgetText.addListener(SWT.MouseDown,listener);
    widgetText.addListener(SWT.MouseUp,listener);
    widgetText.addListener(SWT.MouseMove,listener);
    widgetText.addListener(SWT.Resize,listener);
    widgetText.addLineStyleListener(new LineStyleListener()
    {
      public void lineGetStyle(LineStyleEvent lineStyleEvent)
      {
//Dprintf.dprintf("x %d %s",lineStyleEvent.lineOffset,lineStyleEvent.lineText);
         String findText = widgetFind.getText().toLowerCase();
         int    findTextLength = findText.length();
         if (findTextLength > 0)
         {
           ArrayList<StyleRange> styleRangeList = new ArrayList<StyleRange>();
           int                   index = 0;
           while ((index = lineStyleEvent.lineText.toLowerCase().indexOf(findText,index)) >= 0)
           {
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_VIEW_SEARCH_TEXT,null));
             index += findTextLength;
           }
           lineStyleEvent.styles = styleRangeList.toArray(new StyleRange[styleRangeList.size()]);
//Dprintf.dprintf("lineStyleEvent.styles=%d",lineStyleEvent.styles.length);
         }
         else
         {
           lineStyleEvent.styles = null;
         }
      }
    });
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

    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        findNext(widgetText,widgetFind);
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
        findPrev(widgetText,widgetFind);
      }
    });
    widgetFindNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        findNext(widgetText,widgetFind);
      }
    });

    dialog.addListener(USER_EVENT_NEW_REVISION,new Listener()
    {
      public void handleEvent(Event event)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryView);

    // start show file
    show(revision);

    // start add revisions (only if single file is seleccted)
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, final String revision)
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
        finally
        {
          repositoryTab.clearStatusText();
        }

        if (data.revisionNames.length > 0)
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
                  for (int z = 0; z < data.revisionNames.length; z++)
                  {
                    widgetRevision.add(data.revisionNames[z]);
                    if ((revision != null) && revision.equals(data.revisionNames[z])) selectIndex = z;
                  }
                  widgetRevision.add(repositoryTab.repository.getLastRevision());
                  if ((revision != null) && revision.equals(repositoryTab.repository.getLastRevision())) selectIndex = data.revisionNames.length;
                  if (selectIndex < 0) selectIndex = data.revisionNames.length;

                  widgetRevision.select(selectIndex);
                  widgetRevision.setEnabled(true);
                }
              }
            });
          }
        }
      }
    });
  }

  /** view command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view
   */
  CommandView(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData,null);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      if (data.lines != null)
      {
        widgetFind.setFocus();
      }
      else
      {
        widgetButtonClose.setFocus();
      }
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
    if (   !widgetLineNumbers.isDisposed()
        && !widgetText.isDisposed()
        && !widgetVerticalScrollBar.isDisposed()
        && !widgetHorizontalScrollBar.isDisposed()
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
  }

  /** search previous text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findPrev(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText();
      if (!findText.isEmpty())
      {
        // get cursor position, text before cursor
        int cursorIndex = widgetText.getCaretOffset();

        int offset = (cursorIndex > 0) ? widgetText.getText(0,cursorIndex-1).lastIndexOf(findText) : -1;
        if (offset >= 0)
        {
          int index = offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();

          widgetLineNumbers.setTopIndex(widgetText.getTopIndex());
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }
  }

  /** search next text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findNext(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText();
      if (!findText.isEmpty())
      {
        // get cursor position, text before cursor
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = widgetText.getText().substring(cursorIndex+1).indexOf(findText);
        if (offset >= 0)
        {
          int index = cursorIndex+1+offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();

          widgetLineNumbers.setTopIndex(widgetText.getTopIndex());
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }
  }

  /** show file
   * @param revision revision to view
   */
  private void show(String revision)
  {
    // clear
    if (!display.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          data.lines = null;
          Widgets.modified(data);
         }
      });
    }

    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, String revision)
      {
        // get new revision
        repositoryTab.setStatusText("Get file '%s'...",fileData.getFileName());
        try
        {
          data.lines = repositoryTab.repository.getFileLines(fileData,revision);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file data fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
        }

        if (!display.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              if (data.lines != null)
              {
                // set new text
                setText(data.lines,
                        widgetLineNumbers,
                        widgetText,
                        widgetHorizontalScrollBar,
                        widgetVerticalScrollBar
                       );

                // notify modification
                Widgets.modified(data);

                // focus find
                if (!widgetFind.isDisposed()) widgetFind.setFocus();
              }
            }
          });
        }
      }
    });
  }
}

/* end of file */
