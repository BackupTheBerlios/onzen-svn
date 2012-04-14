/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command view file
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

// graphics
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
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
  private final Color COLOR_VIEW_NONE              = Onzen.COLOR_WHITE;
  private final Color COLOR_VIEW_SEARCH_BACKGROUND = Onzen.COLOR_RED;
  private final Color COLOR_VIEW_SEARCH_TEXT       = Onzen.COLOR_BLUE;

  // user events
  private final int USER_EVENT_NEW_REVISION  = 0xFFFF+0;

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
  private final Combo         widgetRevision;
  private final Button        widgetRevisionPrev;
  private final Button        widgetRevisionNext;
  private final StyledText    widgetLineNumbers;
  private final StyledText    widgetText;
  private final ScrollBar     widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Button        widgetClose;

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
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

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
        Widgets.addModifyListener(new WidgetListener(widgetRevision,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionNames != null));
          }
        });
        widgetRevision.setToolTipText("Revision to view.");

        widgetRevisionPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_LEFT);
        widgetRevisionPrev.setEnabled(false);
        Widgets.layout(widgetRevisionPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetRevisionPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionNames != null) && (widgetRevision.getSelectionIndex() > 0));
          }
        });
        widgetRevisionPrev.setToolTipText("Show previous revision.");

        widgetRevisionNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_RIGHT);
        widgetRevisionNext.setEnabled(false);
        Widgets.layout(widgetRevisionNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetRevisionNext,data)
        {
          public void modified(Control control)
          {

            Widgets.setEnabled(control,(data.revisionNames != null) && (widgetRevision.getSelectionIndex() < data.revisionNames.length-1));
          }
        });
        widgetRevisionNext.setToolTipText("Show next revision.");
      }

      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
      {
        widgetLineNumbers = Widgets.newTextView(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI);
        widgetLineNumbers.setForeground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetLineNumbers,0,0,TableLayoutData.NS,0,0,0,0,60,SWT.DEFAULT);
        Widgets.addModifyListener(new WidgetListener(widgetLineNumbers,data)
        {
          public void modified(Control control)
          {
            control.setForeground((data.lines != null) ? null : Onzen.COLOR_GRAY);
          }
        });

        widgetText = Widgets.newTextView(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI);
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
        label = Widgets.newLabel(subComposite,"Find:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setMessage("Enter text to find");
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.lines != null));
          }
        });
        widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.lines != null));
          }
        });
        widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save as...");
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // get file name
          String fileName = Dialogs.fileSave(dialog,"Save file","",new String[]{"*"});
          if (fileName == null)
          {
            return;
          }

          // check if file exists: overwrite or append
          File file = new File(fileName);
          if (file.exists())
          {
            switch (Dialogs.select(dialog,"Confirmation",String.format("File '%s' already exists.",fileName),new String[]{"Overwrite","Append","Cancel"},2))
            {
              case 0:
                if (!file.delete())
                {
                  Dialogs.error(dialog,"Cannot delete file!");
                  return;
                }
              case 1:
                break;
              case 2:
                return;
            }
          }

          PrintWriter output = null;
          try
          {
            // open file
            output = new PrintWriter(new FileWriter(file,true));

            // write/append file
            for (String line : data.lines)
            {
              output.println(line);
            }

            // close file
            output.close();
          }
          catch (IOException exception)
          {
            Dialogs.error(dialog,"Cannot write file '"+file.getName()+"' (error: "+exception.getMessage());
            return;
          }
          finally
          {
            if (output != null) output.close();
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
    widgetRevisionPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetRevision.getSelectionIndex();
        if (index > 0)
        {
          show(data.revisionNames[index-1]);
        }
      }
    });
    widgetRevisionNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetRevision.getSelectionIndex();
        if ((data.revisionNames != null) && (index < data.revisionNames.length-1))
        {
          show(data.revisionNames[index+1]);
        }
      }
    });

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        StyledText widget   = (StyledText)event.widget;
        int        topIndex = widget.getTopIndex();
//Dprintf.dprintf("%d %d",widget.getTopPixel(),widgetText.getTopPixel());

        widgetText.setTopIndex(topIndex);
        widgetText.setCaretOffset(widgetText.getOffsetAtLine(topIndex));
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
        widgetVerticalScrollBar.setSelection(topIndex);
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
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_VIEW_SEARCH_TEXT,COLOR_VIEW_SEARCH_BACKGROUND));
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
    widgetText.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'c'))
        {
          Widgets.setClipboard(clipboard,widgetText.getSelectionText());
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
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
    widgetFind.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        updateViewFindText(widgetText,widgetFind);
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

    KeyListener keyListener = new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          widgetFind.forceFocus();
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFindPrev))
        {
          Widgets.invoke(widgetFindPrev);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFindNext))
        {
          Widgets.invoke(widgetFindNext);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    };
    widgetText.addKeyListener(keyListener);
    widgetFind.addKeyListener(keyListener);
    widgetFindPrev.addKeyListener(keyListener);
    widgetFindNext.addKeyListener(keyListener);

    dialog.addListener(USER_EVENT_NEW_REVISION,new Listener()
    {
      public void handleEvent(Event event)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryView,Settings.setWindowLocation);

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
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (!widgetRevision.isDisposed())
                {
                  int selectIndex = -1;
                  for (int z = 0; z < data.revisionNames.length; z++)
                  {
                    widgetRevision.add(data.revisionNames[z]);
                    if ((revision != null) && revision.equals(data.revisionNames[z]))
                    {
                      selectIndex = z;
                    }
                  }
                  widgetRevision.add(repositoryTab.repository.getLastRevision());
                  if ((revision != null) && revision.equals(repositoryTab.repository.getLastRevision())) selectIndex = data.revisionNames.length;
                  if (selectIndex == -1) selectIndex = data.revisionNames.length;
                  widgetRevision.select(selectIndex);
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
        widgetClose.setFocus();
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
                       StyledText widgetLineNumbers,
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
      widgetText.setCaretOffset(0);

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

  /** update view find text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void updateViewFindText(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();;
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = widgetText.getText().toLowerCase().substring(cursorIndex).indexOf(findText);
        if (offset >= 0)
        {
          widgetText.redraw();
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
      else
      {
        widgetText.redraw();
      }
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
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = -1;
        if (cursorIndex > 0)
        {
          String text = widgetText.getText(0,cursorIndex-1);
          offset = text.toLowerCase().lastIndexOf(findText);
        }
        if (offset >= 0)
        {
          int index = offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();

          int topIndex = widgetText.getTopIndex();

          widgetLineNumbers.setTopIndex(topIndex);
          widgetVerticalScrollBar.setSelection(topIndex);
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
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = -1;
        if (cursorIndex >= 0)
        {
          String text = widgetText.getText();
          offset = (cursorIndex+1 < text.length()) ? text.substring(cursorIndex+1).toLowerCase().indexOf(findText) : -1;
        }
        if (offset >= 0)
        {
          int index = cursorIndex+1+offset;

          widgetText.setCaretOffset(index);
          widgetText.setSelection(index);
          widgetText.redraw();

          int topIndex = widgetText.getTopIndex();

          widgetLineNumbers.setTopIndex(topIndex);
          widgetVerticalScrollBar.setSelection(topIndex);
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
    if (!dialog.isDisposed())
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
      public void run(FileData fileData, final String revision)
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

        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // set new revision
              widgetRevision.setText(revision);

              if (data.lines != null)
              {
                // set new text
                setText(data.lines,
                        widgetLineNumbers,
                        widgetText,
                        widgetHorizontalScrollBar,
                        widgetVerticalScrollBar
                       );
              }

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
