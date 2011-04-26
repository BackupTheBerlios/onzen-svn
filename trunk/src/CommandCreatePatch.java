/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandCreatePatch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: create patch command
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashSet;

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

/** create patch command
 */
class CommandCreatePatch
{
  /** dialog data
   */
  class Data
  {
    String[] revisionNames;        // revision names
    Patch    patch;
    String[] linesNoWhitespaces;   // patch lines (without whitespace changes)
    String[] lines;                // patch lines
    String   message;

    Data()
    {
      this.revisionNames      = null;
      this.patch              = null;
      this.linesNoWhitespaces = null;
      this.lines              = null;
      this.message            = null;
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  private final Color COLOR_TEXT;
  private final Color COLOR_FIND_TEXT;

  // user events
  private final int USER_EVENT_FILTER_PATCHES  = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab     repositoryTab;
  private final Display           display;
  private final HashSet<FileData> fileDataSet;
  private final String            revision1,revision2;

  // dialog
  private final Data              data = new Data();
  private final Shell             dialog;

  // widgets
  private final Text              widgetLineNumbers;
  private final StyledText        widgetText;
  private final ScrollBar         widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Text              widgetFind;
  private final Button            widgetFindPrev;
  private final Button            widgetFindNext;
  private final Button            widgetIgnoreWhitespaces;
  private final Button            widgetButtonClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to add
   * @param revision1,revisions2 patch revision
   */
  CommandCreatePatch(final Shell shell, final RepositoryTab repositoryTab, final HashSet<FileData> fileDataSet, final String revision1, final String revision2)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;
    Listener  listener;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileDataSet   = fileDataSet;
    this.revision1     = revision1;
    this.revision2     = revision2;

    // get display
    display = shell.getDisplay();

    // colors
    COLOR_TEXT      = new Color(display,Settings.colorInactive.background);
    COLOR_FIND_TEXT = new Color(display,Settings.colorFindText.foreground);

    // add files dialog
    dialog = Dialogs.open(shell,"Create patch",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        widgetLineNumbers = Widgets.newText(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        widgetLineNumbers.setBackground(COLOR_TEXT);
//        widgetLineNumbers.setForeground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetLineNumbers,0,0,TableLayoutData.NS,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,60,SWT.DEFAULT);
        Widgets.addModifyListener(new WidgetListener(widgetLineNumbers,data)
        {
          public void modified(Control control)
          {
            if (!control.isDisposed()) control.setForeground(((data.linesNoWhitespaces != null) || (data.lines != null)) ? null : COLOR_TEXT);
          }
        });

        widgetText = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        widgetText.setBackground(COLOR_TEXT);
//        widgetText.setForeground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetText,0,1,TableLayoutData.NSWE);
        Widgets.addModifyListener(new WidgetListener(widgetText,data)
        {
          public void modified(Control control)
          {
            if (!control.isDisposed()) control.setForeground(((data.linesNoWhitespaces != null) || (data.lines != null)) ? null : COLOR_TEXT);
          }
        });
      }
      widgetHorizontalScrollBar = subComposite.getHorizontalBar();
      widgetVerticalScrollBar   = subComposite.getVerticalBar();

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
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

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.W);
        Widgets.addModifyListener(new WidgetListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.lines != null));
          }
        });

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
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

      widgetIgnoreWhitespaces = Widgets.newCheckbox(composite,"Ignore whitespace changes");
      widgetIgnoreWhitespaces.setSelection(true);
      Widgets.layout(widgetIgnoreWhitespaces,2,0,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(widgetIgnoreWhitespaces,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.linesNoWhitespaces != null) && (data.lines != null));
        }
      });
      widgetIgnoreWhitespaces.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          String[] lines = widget.getSelection() ? data.linesNoWhitespaces : data.lines;
          if (lines != null)
          {
            // set new text
            setText(lines,
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

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save as file");
      button.setEnabled(false);
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.lines != null);
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String[] patchLines = widgetIgnoreWhitespaces.getSelection() ? data.linesNoWhitespaces : data.lines;
          if (patchLines != null)
          {
            // get file name
            String fileName = Dialogs.fileSave(dialog,"Save patch","",new String[]{".patch","*"});
            if (fileName == null)
            {
              return;
            }

            // check if file exists: overwrite or append
            File file = new File(fileName);
            if (file.exists())
            {
              switch (Dialogs.select(dialog,"Confirm",String.format("File '%s' already exists.",fileName),new String[]{"Overwrite","Append","Cancel"},2))
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

            // create patch
            Patch patch = new Patch(repositoryTab.repository.rootPath,
                                    fileDataSet,
                                    revision1,
                                    revision2,
                                    widgetIgnoreWhitespaces.getSelection(),
                                    patchLines
                                   );
            try
            {
              try
              {
                // write patch to file
                patch.write(fileName);

                // close dialog
                Dialogs.close(dialog,true);
              }
              catch (IOException exception)
              {
                Dialogs.error(dialog,"Cannot save patch file! (error: %s)",exception.getMessage());
                return;
              }
            }
            finally
            {
              patch.done();
            }
          }
        }
      });
      button.setToolTipText("Save patch to file.");

      button = Widgets.newButton(composite,"Store");
      button.setEnabled(false);
      Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.lines != null);
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String[] patchLines = widgetIgnoreWhitespaces.getSelection() ? data.linesNoWhitespaces : data.lines;

          if (patchLines != null)
          {
            Patch patch = null;
            try
            {
              // create patch
              patch = new Patch(repositoryTab.repository.rootPath,
                                fileDataSet,
                                patchLines
                               );

              // mail patch
              CommandStorePatch commandStorePatch = new CommandStorePatch(dialog,
                                                                          repositoryTab,
                                                                          fileDataSet,
                                                                          patch
                                                                         );
              if (commandStorePatch.execute())
              {
                try
                {
                  // save patch into database
                  patch.state   = Patch.States.NONE;
                  patch.summary = commandStorePatch.summary;
                  patch.message = commandStorePatch.message;
                  patch.save();

                  // close dialog
                  Dialogs.close(dialog,true);
                }
                catch (SQLException exception)
                {
                  Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                  return;
                }
              }

              // free resources
              patch.done(); patch = null;
            }
            finally
            {
              if (patch != null) patch.done();
            }
          }
        }
      });
      button.setToolTipText("Store patch into database.");

      button = Widgets.newButton(composite,"Mail for review");
      button.setEnabled(false);
      Widgets.layout(button,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.lines != null);
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String[] patchLines = widgetIgnoreWhitespaces.getSelection() ? data.linesNoWhitespaces : data.lines;

          if (patchLines != null)
          {
            Patch patch = null;
            try
            {
              // create patch
              patch = new Patch(repositoryTab.repository.rootPath,
                                fileDataSet,
                                patchLines
                               );

              // mail patch
              CommandMailPatch commandMailPatch = new CommandMailPatch(dialog,
                                                                       repositoryTab,
                                                                       fileDataSet,
                                                                       patch
                                                                      );
              if (commandMailPatch.execute())
              {
                try
                {
                  // save patch into database
                  patch.state   = Patch.States.REVIEW;
                  patch.summary = commandMailPatch.summary;
                  patch.message = commandMailPatch.message;
                  patch.save();

                  // close dialog
                  Dialogs.close(dialog,true);
                }
                catch (SQLException exception)
                {
                  Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                  return;
                }
              }

              // free resources
              patch.done(); patch = null;
            }
            finally
            {
              if (patch != null) patch.done();
            }
          }
        }
      });
      button.setToolTipText("Mail patch for reviewing.");

      button = Widgets.newButton(composite,"Commit");
      button.setEnabled(false);
      Widgets.layout(button,0,3,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
//          if (!control.isDisposed()) control.setEnabled(repositoryTab.repository.supportPatchQueues() && (data.lines != null));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
Dprintf.dprintf("");

          // close dialog
          Dialogs.close(dialog,true);
        }
      });
      button.setToolTipText("NYI");

      widgetButtonClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetButtonClose,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Settings.geometryCreatePatch = dialog.getSize();

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
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
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_FIND_TEXT,null));
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

    // show dialog
    Dialogs.show(dialog,Settings.geometryCreatePatch);

    // start show file
    show(revision1,revision2);
  }

  /** patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to add
   * @param revision revisions patch revision
   */
  CommandCreatePatch(Shell shell, RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, String revision)
  {
    this(shell,repositoryTab,fileDataSet,revision,null);
  }

  /** create patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to add
   */
  CommandCreatePatch(Shell shell, RepositoryTab repositoryTab, HashSet<FileData> fileDataSet)
  {
    this(shell,repositoryTab,fileDataSet,null);
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
  public String ring()
  {
    return "CommandCreatePatch {}";
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

  /** show patch
   * @param revision1,revision2 patch to view
   */
  private void show(String revision1, String revision2)
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

    // get and show patch
    Background.run(new BackgroundRunnable(fileDataSet,revision1,revision2)
    {
      public void run(HashSet<FileData> fileDataSet, String revision1, String revision2)
      {
        repositoryTab.setStatusText("Get patch...");
        try
        {
          // get patch without whitespace change
          data.linesNoWhitespaces = repositoryTab.repository.getPatchLines(fileDataSet,revision1,revision2,true);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   (data.linesNoWhitespaces != null)
                    && widgetIgnoreWhitespaces.getSelection()
                   )
                {
                  // set new text
                  setText(data.linesNoWhitespaces,
                          widgetLineNumbers,
                          widgetText,
                          widgetHorizontalScrollBar,
                          widgetVerticalScrollBar
                         );
                }

                // notify modification
                Widgets.modified(data);

                // focus find
                if (!widgetFind.isDisposed()) widgetFind.setFocus();
              }
            });
          }

          // get patch
          data.lines = repositoryTab.repository.getPatchLines(fileDataSet,revision1,revision2,false);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   (data.lines != null)
                    && !widgetIgnoreWhitespaces.getSelection()
                   )
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

                // focus find
                if (!widgetFind.isDisposed()) widgetFind.setFocus();
              }
            });
          }
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file patch fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
        }
      }
    });
  }

  /** show patch last revision/local revision
   */
  private void show()
  {
    show(null,null);
  }
}

/* end of file */
