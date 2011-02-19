/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandCommit.java,v $
* $Revision: 1.6 $
* $Author: torsten $
* Contents: command commit files/directories
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
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
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

/** commit files/directories command
 */
class CommandCommit
{
  /** dialog data
   */
  class Data
  {
    String[] patchLinesNoWhitespaces;   // changed lines (without whitespace changes)
    String[] patchLines;                // changed lines
    String   message;                   // commit message

    Data()
    {
      this.patchLinesNoWhitespaces = null;
      this.patchLines              = null;
      this.message                 = "";
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab     repositoryTab;
  private final HashSet<FileData> fileDataSet;
  private final Shell             shell;
  private final Display           display;
  private final String[]          history;       

  // dialog
  private final Data              data = new Data();
  private final Shell             dialog;        

  // widgets
  private final StyledText        widgetChanges;
  private final ScrollBar         widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final Button            widgetIgnoreWhitespaces;
  private final List              widgetFiles;
  private final List              widgetHistory; 
  private final Text              widgetMessage; 
  private final Button            widgetCommit;     

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** commit command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet files to commit
   */
  CommandCommit(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet)
  {
    Composite composite,subComposite,subSubComposite;
    TabFolder tabFolder;
    Label     label;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileDataSet   = fileDataSet;
    this.shell         = shell;

    // get display
    display = shell.getDisplay();

    // get history
    history = Message.getHistory();

    // commit files dialog
    dialog = Dialogs.open(shell,"Commit files",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.5,0.0,0.15,0.0,0.35},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);
      {
        subComposite = Widgets.addTab(tabFolder,"Changes");
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetChanges = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
          widgetChanges.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetChanges,0,0,TableLayoutData.NSWE);
          widgetChanges.setToolTipText("Changes to commit.");

          widgetHorizontalScrollBar = widgetChanges.getHorizontalBar();
          widgetVerticalScrollBar   = widgetChanges.getVerticalBar();

          widgetIgnoreWhitespaces = Widgets.newCheckbox(subComposite,"Ignore whitespace changes");
          widgetIgnoreWhitespaces.setSelection(true);
          widgetIgnoreWhitespaces.setEnabled(false);
          Widgets.layout(widgetIgnoreWhitespaces,1,0,TableLayoutData.W);
          Widgets.addModifyListener(new WidgetListener(widgetIgnoreWhitespaces,data)
          {
            public void modified(Control control)
            {
              Widgets.setEnabled(control,(data.patchLinesNoWhitespaces != null) && (data.patchLines != null));
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

              String[] patchLines = widget.getSelection() ? data.patchLinesNoWhitespaces : data.patchLines;
              if (patchLines != null)
              {
                // set new text
                setPatchText(patchLines,
                             widgetChanges,
                             widgetHorizontalScrollBar,
                             widgetVerticalScrollBar
                            );
              }
            }
          });

        }

        subComposite = Widgets.addTab(tabFolder,"Files");
        subComposite.setLayout(new TableLayout(1.0,1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetFiles = Widgets.newList(subComposite);
          widgetFiles.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetFiles,0,0,TableLayoutData.NSWE);
          widgetFiles.setToolTipText("Files to commit.");
        }
      }

      label = Widgets.newLabel(composite,"History:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetHistory = Widgets.newList(composite);
      widgetHistory.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetHistory,2,0,TableLayoutData.NSWE);
      widgetHistory.setToolTipText("Commit message history.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\nCtrl-Up/Down/Home/End to select message from history.\nCtrl-Return to commit changed files.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetCommit,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        if ((keyEvent.stateMask & SWT.CTRL) != 0)
        {
          int i = widgetHistory.getSelectionIndex();
          if (i < 0) i = history.length;

          if (keyEvent.keyCode == SWT.ARROW_DOWN)
          {
            // next history entry
            if (i < history.length-1)
            {
              widgetHistory.setSelection(i+1);
              widgetHistory.showSelection();
              widgetMessage.setText(history[i+1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.ARROW_UP)
          {
            // previous history entry
            if (i > 0)
            {
              widgetHistory.setSelection(i-1);
              widgetHistory.showSelection();
              widgetMessage.setText(history[i-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.keyCode == SWT.HOME)
          {
            // first history entry
            if (history.length > 0)
            {
              widgetHistory.setSelection(0);
              widgetHistory.showSelection();
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
              widgetHistory.showSelection();
              widgetMessage.setText(history[history.length-1]);
              widgetMessage.setFocus();
            }
          }
          else if (keyEvent.character == SWT.CR)
          {
            // invoke commit-button
            Widgets.invoke(widgetCommit);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryCommit);

    // add files
    if (!widgetFiles.isDisposed())
    {
      for (FileData fileData : fileDataSet)
      {
        widgetFiles.add(fileData.getFileName());
      }
    }

    // add history
    if (!widgetHistory.isDisposed())
    {
      for (String string : history)
      {
        widgetHistory.add(string.trim().replaceAll("\n",", "));
      }
      widgetHistory.setSelection(widgetHistory.getItemCount()-1);
      widgetHistory.showSelection();
      widgetHistory.deselectAll();
    }

    // start getting changes
    Background.run(new BackgroundRunnable(fileDataSet)
    {
      public void run(HashSet<FileData> fileDataSet)
      {
        repositoryTab.setStatusText("Get patch...");
        try
        {
          // get patch without whitespace change
          data.patchLinesNoWhitespaces = repositoryTab.repository.getPatchLines(fileDataSet,true);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   (data.patchLinesNoWhitespaces != null)
                    && widgetIgnoreWhitespaces.getSelection()
                   )
                {
                  // set new text
                  setPatchText(data.patchLinesNoWhitespaces,
                               widgetChanges,
                               widgetHorizontalScrollBar,
                               widgetVerticalScrollBar
                              );
                }
              }
            });
          }

          // get patch
          data.patchLines = repositoryTab.repository.getPatchLines(fileDataSet,false);

          // show
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                if (   (data.patchLines != null)
                    && !widgetIgnoreWhitespaces.getSelection()
                   )
                {
                  // set new text
                  setPatchText(data.patchLines,
                               widgetChanges,
                               widgetHorizontalScrollBar,
                               widgetVerticalScrollBar
                              );

                }
              }
            });
          }

          // notify modification
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Widgets.modified(data);
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
              Dialogs.error(shell,"Getting file patch fail: %s",exceptionMessage);
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

    // update
  }

  /** commit command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to commit
   */
  CommandCommit(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData.toSet());
  }

  /** run dialog
   */
  public void run()
  {
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {      
      Background.run(new BackgroundRunnable()
      {
        public void run()
        {
          commit();
        }
      });
    }
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    widgetMessage.setFocus();
    if ((Boolean)Dialogs.run(dialog,false))
    {
      commit();

      return true;
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
    return "CommandCommit {}";
  }

  //-----------------------------------------------------------------------

  /** set patch text
   * @param lines lines
   * @param widgetText text widget
   * @param widgetHorizontalScrollBar horizontal scrollbar widget
   * @param widgetVerticalScrollBar horizontal scrollbar widget
   */
  private void setPatchText(String[]   lines,
                            StyledText widgetText,
                            ScrollBar  widgetHorizontalScrollBar,
                            ScrollBar  widgetVerticalScrollBar
                           )
  {
    if (   !widgetText.isDisposed()
        && !widgetVerticalScrollBar.isDisposed()
        && !widgetHorizontalScrollBar.isDisposed()
       )
    {
      StringBuilder text     = new StringBuilder();
      int           lineNb   = 1;
      int           maxWidth = 0;

      // get text
      for (String line : lines)
      {
        text.append(line); text.append('\n');

        maxWidth = Math.max(maxWidth,line.length());
      }

      // set text
      widgetText.setText(text.toString());

/*
      // set scrollbars
      widgetHorizontalScrollBar.setMinimum(0);
      widgetHorizontalScrollBar.setMaximum(maxWidth);
      widgetVerticalScrollBar.setMinimum(0);
      widgetVerticalScrollBar.setMaximum(lineNb-1);
*/

      // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
      widgetText.redraw();
      widgetText.update();

      // show top
      widgetText.setTopIndex(0);
      widgetText.setCaretOffset(0);
      widgetVerticalScrollBar.setSelection(0);
    }
  }

  /** do commit
   * @param data data
   * @param repositoryTab repository tab
   */
  private void commit()
  {
    repositoryTab.setStatusText("Commit files...");
    Message message = null;
    try
    {
      // add message to history
      message = new Message(data.message);
      message.addToHistory();

      // commit files
      repositoryTab.repository.commit(fileDataSet,message);

      // update file states
      repositoryTab.repository.updateStates(fileDataSet);
      display.syncExec(new Runnable()
      {
        public void run()
        {
          repositoryTab.updateTreeItems(fileDataSet);
        }
      });
    }
    catch (RepositoryException exception)
    {
      final String exceptionMessage = exception.getMessage();
      display.syncExec(new Runnable()
      {
        public void run()
        {
          Dialogs.error(shell,"Cannot commit files (error: %s)",exceptionMessage);
        }
      });
      return;
    }
    finally
    {
      message.done();
      repositoryTab.clearStatusText();
    }
  }
}

/* end of file */
