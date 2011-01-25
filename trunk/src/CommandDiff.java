/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandDiff.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: command diff
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;

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

/** diff command
 */
class CommandDiff
{
  /** dialog data
   */
  class Data
  {
    DiffData[] diffData;
    BitSet     diffLines;

    Data()
    {
      this.diffData  = null;
      this.diffLines = null;
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  private final Color COLOR_DIFF_NONE                = Onzen.COLOR_WHITE;
  private final Color COLOR_DIFF_ADDED               = Onzen.COLOR_GREEN;
  private final Color COLOR_DIFF_DELETED             = Onzen.COLOR_MAGENTA;
  private final Color COLOR_DIFF_CHANGED             = Onzen.COLOR_RED;
  private final Color COLOR_DIFF_CHANGED_WHITESPACES = Onzen.COLOR_DARK_YELLOW;
  private final Color COLOR_DIFF_SEARCH_TEXT         = Onzen.COLOR_BLUE;

  // user events
  private final int   USER_EVENT_NEW_REVISION   = 0xFFFF+0;
  private final int   USER_EVENT_SYNC           = 0xFFFF+1;
  private final int   USER_EVENT_REFRESH_COLORS = 0xFFFF+2;
  private final int   USER_EVENT_REFRESH_BAR    = 0xFFFF+3;

  // --------------------------- variables --------------------------------

  // global variable references
  private final Shell         shell;
  private final Repository    repository;

  private final Display       display;
  private final Data          data = new Data();

  // dialog
  private final Shell         dialog;

  // widgets
  private    final Label      widgetRevisionLeft;
  private    final Label      widgetRevisionRight;
  private    final Text       widgetLineNumbersLeft;
  private    final Text       widgetLineNumbersRight;
  private    final StyledText widgetTextLeft;
  private    final StyledText widgetTextRight;
  private    final ScrollBar  widgetHorizontalScrollBarLeft,widgetVerticalScrollBarLeft;
  private    final ScrollBar  widgetHorizontalScrollBarRight,widgetVerticalScrollBarRight;
  private    final Canvas     widgetBar;
  private    final Text       widgetFindLeft;
  private    final Text       widgetFindRight;
  private    final Button     widgetFindLeftPrev;
  private    final Button     widgetFindLeftNext;
  private    final Button     widgetFindRightPrev;
  private    final Button     widgetFindRightNext;
  private    final StyledText widgetLineDiff;
  private    final Button     widgetSync;
  private    final Button     widgetAdded;
  private    final Button     widgetDeleted;
  private    final Button     widgetChanged;
  private    final Button     widgetChangedWhitespaces;
  private    final Combo      widgetRevision;
  private    final Button     widgetRevisionPrev;
  private    final Button     widgetRevisionNext;
  private    final Button     widgetPatch;
  private    final Button     widgetPrev;
  private    final Button     widgetNext;
  private    final Button     widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** diff command
   * @param shell shell
   * @param repository repository
   * @param fileData file to show diff for
   */
  CommandDiff(final Shell shell, final Repository repository, final FileData fileData, String revisionLeft, String revisionRight)
    throws RepositoryException
  {
    final String[] revisions;

    Composite      composite,subComposite;
    Label          label;
    Button         button;
    Listener       listener;

    // initialize variables
    this.shell           = shell;
    this.repository      = repository;

    // get display
    display = shell.getDisplay();

    // get revisions (if not selected revision left and right)
    if ((revisionLeft == null) && (revisionRight == null))
    {
      try
      {
        revisions = repository.getRevisions(fileData);
      }
      catch (RepositoryException exception)
      {
        throw new RepositoryException("Getting revisions fail",exception);
      }
    }
    else
    {
      revisions = null;
    }

    // get diff
    try
    {
      if (revisions != null)
      {
        String revision = (revisions.length > 0) ? revisions[revisions.length-1] : repository.getLastRevision();
        data.diffData = repository.diff(fileData,revision);
      }
      else
      {
        data.diffData = repository.diff(fileData,revisionLeft,revisionRight);
      }
    }
    catch (RepositoryException exception)
    {
      throw new RepositoryException("Getting diff fail",exception);
    }

    // show diff
    dialog = Dialogs.open(shell,"Diff: "+fileData.getFileName(),Settings.geometryDiff.x,Settings.geometryDiff.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,0.0},new double[]{1.0,0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevisionLeft = Widgets.newLabel(subComposite);
        Widgets.layout(widgetRevisionLeft,0,1,TableLayoutData.WE);
        if (revisionLeft != null) widgetRevisionLeft.setText(revisionLeft);
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,2,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevisionRight = Widgets.newLabel(subComposite);
        Widgets.layout(widgetRevisionRight,0,1,TableLayoutData.WE);
        if (revisionRight != null) widgetRevisionRight.setText(revisionRight);
      }

      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
      {
        widgetLineNumbersLeft = Widgets.newText(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetLineNumbersLeft,0,0,TableLayoutData.NS,0,0,0,0,60,SWT.DEFAULT);

        widgetTextLeft = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetTextLeft,0,1,TableLayoutData.NSWE);
      }
      widgetHorizontalScrollBarLeft = subComposite.getHorizontalBar();
      widgetVerticalScrollBarLeft   = subComposite.getVerticalBar();

      widgetBar = Widgets.newCanvas(composite,SWT.BORDER);
      Widgets.layout(widgetBar,1,1,TableLayoutData.NS,0,0,0,0,20,SWT.DEFAULT);

      subComposite = Widgets.newComposite(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,2,TableLayoutData.NSWE);
      {
        widgetLineNumbersRight = Widgets.newText(subComposite,SWT.RIGHT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetLineNumbersRight,0,0,TableLayoutData.NS,0,0,0,0,60,SWT.DEFAULT);

        widgetTextRight = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.READ_ONLY);
        Widgets.layout(widgetTextRight,0,1,TableLayoutData.NSWE);
      }
      widgetHorizontalScrollBarRight = subComposite.getHorizontalBar();
      widgetVerticalScrollBarRight   = subComposite.getVerticalBar();

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFindLeft = Widgets.newText(subComposite);
        Widgets.layout(widgetFindLeft,0,1,TableLayoutData.WE);

        widgetFindLeftPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_UP);
        Widgets.layout(widgetFindLeftPrev,0,2,TableLayoutData.W);

        widgetFindLeftNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_DOWN);
        Widgets.layout(widgetFindLeftNext,0,3,TableLayoutData.W);
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,2,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFindRight = Widgets.newText(subComposite);
        Widgets.layout(widgetFindRight,0,1,TableLayoutData.WE);

        widgetFindRightPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_UP);
        Widgets.layout(widgetFindRightPrev,0,2,TableLayoutData.W);

        widgetFindRightNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARRAY_DOWN);
        Widgets.layout(widgetFindRightNext,0,3,TableLayoutData.W);
      }

      widgetLineDiff = Widgets.newStyledText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL);
      Widgets.layout(widgetLineDiff,3,0,TableLayoutData.WE,0,3,0,0,SWT.DEFAULT,60);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetSync = Widgets.newCheckbox(composite,"Sync");
      widgetSync.setSelection(true);
      Widgets.layout(widgetSync,0,0,TableLayoutData.W);
      widgetSync.setToolTipText("Sync left and right text.");

      widgetAdded = Widgets.newCheckbox(composite,"Added");
      widgetAdded.setSelection(true);
      widgetAdded.setBackground(COLOR_DIFF_ADDED);
      Widgets.layout(widgetAdded,0,1,TableLayoutData.W);
      widgetAdded.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Widgets.notify(dialog,USER_EVENT_REFRESH_COLORS);
        }
      });
      widgetAdded.setToolTipText("Show added lines.");

      widgetDeleted = Widgets.newCheckbox(composite,"Deleted");
      widgetDeleted.setSelection(true);
      widgetDeleted.setBackground(COLOR_DIFF_DELETED);
      Widgets.layout(widgetDeleted,0,2,TableLayoutData.W);
      widgetDeleted.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Widgets.notify(dialog,USER_EVENT_REFRESH_COLORS);
        }
      });
      widgetDeleted.setToolTipText("Show deleted lines.");

      widgetChanged = Widgets.newCheckbox(composite,"Changed");
      widgetChanged.setSelection(true);
      widgetChanged.setBackground(COLOR_DIFF_CHANGED);
      Widgets.layout(widgetChanged,0,3,TableLayoutData.W);
      widgetChanged.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Widgets.notify(dialog,USER_EVENT_REFRESH_COLORS);
        }
      });
      widgetChanged.setToolTipText("Show changed lines.");

      widgetChangedWhitespaces = Widgets.newCheckbox(composite,"Changed spaces");
      widgetChangedWhitespaces.setSelection(false);
      widgetChangedWhitespaces.setBackground(COLOR_DIFF_CHANGED_WHITESPACES);
      Widgets.layout(widgetChangedWhitespaces,0,4,TableLayoutData.W);
      widgetChangedWhitespaces.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Widgets.notify(dialog,USER_EVENT_REFRESH_COLORS);
        }
      });
      widgetChangedWhitespaces.setToolTipText("Show lines with white-space changes.");

      widgetRevisionPrev = Widgets.newButton(composite,Onzen.IMAGE_ARROW_LEFT);
      widgetRevisionPrev.setEnabled(revisions != null);
      Widgets.layout(widgetRevisionPrev,0,5,TableLayoutData.W);

      widgetRevision = Widgets.newSelect(composite);
      widgetRevision.setEnabled(revisions != null);
      Widgets.layout(widgetRevision,0,6,TableLayoutData.WE);
      widgetRevision.setToolTipText("Revisions to show.");

      widgetRevisionNext = Widgets.newButton(composite,Onzen.IMAGE_ARROW_RIGHT);
      widgetRevisionNext.setEnabled(revisions != null);
      Widgets.layout(widgetRevisionNext,0,7,TableLayoutData.W);

      widgetPatch = Widgets.newButton(composite,"Patch");
      Widgets.layout(widgetPatch,0,8,TableLayoutData.W);

      widgetPrev = Widgets.newButton(composite,"Prev");
      Widgets.layout(widgetPrev,0,9,TableLayoutData.E);

      widgetNext = Widgets.newButton(composite,"Next");
      Widgets.layout(widgetNext,0,10,TableLayoutData.E);

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,11,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryDiff = dialog.getSize();

          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        Text widget = (Text)event.widget;
        int topIndex = widget.getTopIndex();
//Dprintf.dprintf("widget=%s: %d",widget,widget.getTopIndex());

        // sync left text widget
        if (widgetTextLeft.getTopIndex() != topIndex)
        {
          widgetTextLeft.setTopIndex(topIndex);
          widgetBar.redraw();
        }

        // sync to right
        if (widgetSync.getSelection())
        {
          widgetLineNumbersRight.setTopIndex(topIndex);
          widgetTextRight.setTopIndex(topIndex);
        }
      }
    };
    widgetLineNumbersLeft.addListener(SWT.KeyDown,listener);
    widgetLineNumbersLeft.addListener(SWT.KeyUp,listener);
    widgetLineNumbersLeft.addListener(SWT.MouseDown,listener);
    widgetLineNumbersLeft.addListener(SWT.MouseUp,listener);
    widgetLineNumbersLeft.addListener(SWT.MouseMove,listener);
    widgetLineNumbersLeft.addListener(SWT.Resize,listener);

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        Text widget = (Text)event.widget;
        int topIndex = widget.getTopIndex();
//Dprintf.dprintf("widget=%s: %d",widget,widget.getTopIndex());

        // sync right text widget
        widgetTextRight.setTopIndex(topIndex);

        // sync to left
        if (widgetSync.getSelection())
        {
          widgetLineNumbersLeft.setTopIndex(topIndex);
          if (widgetTextLeft.getTopIndex() != topIndex)
          {
            widgetTextLeft.setTopIndex(topIndex);
            widgetBar.redraw();
          }
        }
      }
    };
    widgetLineNumbersRight.addListener(SWT.KeyDown,listener);
    widgetLineNumbersRight.addListener(SWT.KeyUp,listener);
    widgetLineNumbersRight.addListener(SWT.MouseDown,listener);
    widgetLineNumbersRight.addListener(SWT.MouseUp,listener);
    widgetLineNumbersRight.addListener(SWT.MouseMove,listener);
    widgetLineNumbersRight.addListener(SWT.Resize,listener);

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        StyledText widget = (StyledText)event.widget;
        int        topIndex = widget.getTopIndex();
//Dprintf.dprintf("widget=%s: %d",widget,widget.getTopIndex());

        // sync left number text widget, vertical scrollbar
        widgetLineNumbersLeft.setTopIndex(topIndex);
        widgetVerticalScrollBarLeft.setSelection(topIndex);

        // sync to right
        if (widgetSync.getSelection())
        {
          widgetLineNumbersRight.setTopIndex(topIndex);
          widgetTextRight.setTopIndex(topIndex);
          widgetVerticalScrollBarRight.setSelection(topIndex);
        }
      }
    };
    widgetTextLeft.addListener(SWT.KeyDown,listener);
    widgetTextLeft.addListener(SWT.KeyUp,listener);
    widgetTextLeft.addListener(SWT.MouseDown,listener);
    widgetTextLeft.addListener(SWT.MouseUp,listener);
    widgetTextLeft.addListener(SWT.MouseMove,listener);
    widgetTextLeft.addListener(SWT.Resize,listener);
    widgetTextLeft.addListener(USER_EVENT_SYNC,listener);
    widgetTextLeft.addLineStyleListener(new LineStyleListener()
    {
      public void lineGetStyle(LineStyleEvent lineStyleEvent)
      {
//Dprintf.dprintf("x %d %s",lineStyleEvent.lineOffset,lineStyleEvent.lineText);
         String findText = widgetFindLeft.getText();
         int    findTextLength = findText.length();
         if (findTextLength > 0)
         {
           ArrayList<StyleRange> styleRangeList = new ArrayList<StyleRange>();
           int                   index = 0;
           while ((index = lineStyleEvent.lineText.indexOf(findText,index)) >= 0)
           {
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_DIFF_SEARCH_TEXT,null));
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
    widgetTextLeft.addCaretListener(new CaretListener()
    {
      public void caretMoved(CaretEvent caretEvent)
      {
        int lineIndex = widgetTextLeft.getLineAtOffset(caretEvent.caretOffset);

        String line1 = widgetTextLeft.getLine(lineIndex);
        String line2 = widgetTextRight.getLine(lineIndex);
        setLine(widgetLineDiff,
                line1,
                line2
               );
        updateLineColors(widgetLineDiff,
                         widgetAdded.getSelection(),
                         widgetDeleted.getSelection(),
                         widgetChanged.getSelection(),
                         widgetChangedWhitespaces.getSelection(),
                         COLOR_DIFF_NONE,
                         COLOR_DIFF_CHANGED
                        );
      }
    });
    widgetTextRight.addCaretListener(new CaretListener()
    {
      public void caretMoved(CaretEvent caretEvent)
      {
        int lineIndex = widgetTextRight.getLineAtOffset(caretEvent.caretOffset);

        String line1 = widgetTextLeft.getLine(lineIndex);
        String line2 = widgetTextRight.getLine(lineIndex);
        setLine(widgetLineDiff,
                line1,
                line2
               );
        updateLineColors(widgetLineDiff,
                         widgetAdded.getSelection(),
                         widgetDeleted.getSelection(),
                         widgetChanged.getSelection(),
                         widgetChangedWhitespaces.getSelection(),
                         COLOR_DIFF_NONE,
                         COLOR_DIFF_CHANGED
                        );
      }
    });

    listener = new Listener()
    {
      public void handleEvent(Event event)
      {
        StyledText widget = (StyledText)event.widget;
        int        topIndex = widget.getTopIndex();
//Dprintf.dprintf("widget=%s: %d",widget,widget.getTopIndex());

        // sync right number text widget
        widgetLineNumbersRight.setTopIndex(topIndex);
        widgetVerticalScrollBarRight.setSelection(topIndex);

        // sync to left
        if (widgetSync.getSelection())
        {
          widgetLineNumbersLeft.setTopIndex(topIndex);
          if (widgetTextLeft.getTopIndex() != topIndex)
          {
            widgetTextLeft.setTopIndex(topIndex);
            widgetBar.redraw();
          }
          widgetVerticalScrollBarLeft.setSelection(topIndex);
        }
      }
    };
    widgetTextRight.addListener(SWT.KeyDown,listener);
    widgetTextRight.addListener(SWT.KeyUp,listener);
    widgetTextRight.addListener(SWT.MouseDown,listener);
    widgetTextRight.addListener(SWT.MouseUp,listener);
    widgetTextRight.addListener(SWT.MouseMove,listener);
    widgetTextRight.addListener(SWT.Resize,listener);
    widgetTextRight.addListener(USER_EVENT_SYNC,listener);
    widgetTextRight.addLineStyleListener(new LineStyleListener()
    {
      public void lineGetStyle(LineStyleEvent lineStyleEvent)
      {
//Dprintf.dprintf("x %d %s",lineStyleEvent.lineOffset,lineStyleEvent.lineText);
         String findText = widgetFindRight.getText();
         int    findTextLength = findText.length();
         if (findTextLength > 0)
         {
           ArrayList<StyleRange> styleRangeList = new ArrayList<StyleRange>();
           int                   index = 0;
           while ((index = lineStyleEvent.lineText.indexOf(findText,index)) >= 0)
           {
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_DIFF_SEARCH_TEXT,null));
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

    widgetHorizontalScrollBarLeft.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();

        // sync left text widget
        widgetTextLeft.setHorizontalIndex(index);

        // sync to right
        if (widgetSync.getSelection())
        {
          widgetTextRight.setHorizontalIndex(index);
          widgetHorizontalScrollBarRight.setSelection(index);
        }
      }
    });
    widgetVerticalScrollBarLeft.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();
//Dprintf.dprintf("widget=%s: %d %d %d",widget,widget.getSelection(),widget.getMinimum(),widget.getMaximum());

        // sync left number text widget, text widget
        widgetLineNumbersLeft.setTopIndex(index);
        if (widgetTextLeft.getTopIndex() != index)
        {
          widgetTextLeft.setTopIndex(index);
          widgetBar.redraw();
        }

        // sync to right
        if (widgetSync.getSelection())
        {
          widgetLineNumbersRight.setTopIndex(index);
          widgetTextRight.setTopIndex(index);
          widgetVerticalScrollBarRight.setSelection(index);
        }
      }
    });

    widgetHorizontalScrollBarRight.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();
//Dprintf.dprintf("widget=%s: %d %d %d",widget,widget.getSelection(),widget.getMinimum(),widget.getMaximum());

        // sync right number text widget
        widgetTextRight.setHorizontalIndex(index);

        // sync to left
        if (widgetSync.getSelection())
        {
          widgetTextLeft.setHorizontalIndex(index);
          widgetHorizontalScrollBarLeft.setSelection(index);
        }
      }
    });
    widgetVerticalScrollBarRight.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        ScrollBar widget = (ScrollBar)selectionEvent.widget;
        int       index = widget.getSelection();
//Dprintf.dprintf("widget=%s: %d %d %d",widget,widget.getSelection(),widget.getMinimum(),widget.getMaximum());

        // sync right number text widget, text widget
        widgetLineNumbersRight.setTopIndex(index);
        widgetTextRight.setTopIndex(index);

        // sync to left
        if (widgetSync.getSelection())
        {
          widgetLineNumbersLeft.setTopIndex(index);
          if (widgetTextLeft.getTopIndex() != index)
          {
            widgetTextLeft.setTopIndex(index);
            widgetBar.redraw();
          }
          widgetVerticalScrollBarLeft.setSelection(index);
        }
      }
    });

    widgetBar.addPaintListener(new PaintListener()
    {
      public void paintControl(PaintEvent paintEvent)
      {
        drawBar(data.diffData,
                widgetTextLeft.getClientArea().height/widgetTextLeft.getLineHeight(),
                widgetTextLeft.getTopIndex(),
                widgetTextLeft.getLineCount(),
                widgetBar,
                widgetAdded.getSelection(),
                widgetDeleted.getSelection(),
                widgetChanged.getSelection(),
                widgetChangedWhitespaces.getSelection(),
                COLOR_DIFF_ADDED,
                COLOR_DIFF_DELETED,
                COLOR_DIFF_CHANGED,
                COLOR_DIFF_CHANGED_WHITESPACES
               );
      }
    });

    widgetPatch.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
Dprintf.dprintf("NYI");
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
          widgetRevision.select(index-1);
          Widgets.notify(dialog,USER_EVENT_NEW_REVISION,index-1);
        }
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
    widgetRevisionNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetRevision.getSelectionIndex();
        if (index < revisions.length-1)
        {
          widgetRevision.select(index+1);
          Widgets.notify(dialog,USER_EVENT_NEW_REVISION,index+1);
        }
      }
    });

    widgetPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int topIndex = widgetTextLeft.getTopIndex();

        while ((topIndex >= 0) && data.diffLines.get(topIndex))
        {
          topIndex--;
        }
        while ((topIndex >= 0) && !data.diffLines.get(topIndex))
        {
          topIndex--;
        }

        if (topIndex >= 0)
        {
          // set left text widget
          widgetLineNumbersLeft.setTopIndex(topIndex);
          widgetTextLeft.setTopIndex(topIndex);
          widgetBar.redraw();

          // sync to right
          if (widgetSync.getSelection())
          {
            widgetLineNumbersRight.setTopIndex(topIndex);
            widgetTextRight.setTopIndex(topIndex);
          }
        }
      }
    });
    widgetNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int topIndex = widgetTextLeft.getTopIndex();

        while ((topIndex < data.diffLines.size()) && data.diffLines.get(topIndex))
        {
          topIndex++;
        }
        while ((topIndex < data.diffLines.size()) && !data.diffLines.get(topIndex))
        {
          topIndex++;
        }

        if (topIndex < data.diffLines.size())
        {
          // set left text widget
          widgetLineNumbersLeft.setTopIndex(topIndex);
          widgetTextLeft.setTopIndex(topIndex);
          widgetBar.redraw();

          // sync to right
          if (widgetSync.getSelection())
          {
            widgetLineNumbersRight.setTopIndex(topIndex);
            widgetTextRight.setTopIndex(topIndex);
          }
        }
      }
    });

    dialog.addListener(USER_EVENT_NEW_REVISION,new Listener()
    {
      public void handleEvent(Event event)
      {
        if ((event.index >= 0) && (event.index < revisions.length))
        {
          // get new diff
          String     revisionLeft  = null;
          String     revisionRight = null;
          DiffData[] diffData      = null;
          try
          {
            if (event.index < revisions.length-1)
            {
              revisionLeft  = revisions[event.index+0];
              revisionRight = revisions[event.index+1];
              diffData      = repository.diff(fileData,revisions[event.index+0],revisions[event.index+1]);
            }
            else
            {
              revisionLeft  = revisions[event.index];
              revisionRight = "local";
              diffData      = repository.diff(fileData,revisions[event.index]);
            }
          }
          catch (RepositoryException exception)
          {
            Dialogs.error(dialog,"Getting diff fail: %s",exception.getMessage());
          }

          // set new diff
          if (diffData != null)
          {
            widgetRevisionLeft.setText(revisionLeft);
            widgetRevisionRight.setText(revisionRight);
            data.diffData  = diffData;
            data.diffLines = setText(diffData,
                                     widgetLineNumbersLeft,
                                     widgetLineNumbersRight,
                                     widgetTextLeft,
                                     widgetTextRight,
                                     widgetHorizontalScrollBarLeft,
                                     widgetVerticalScrollBarLeft,
                                     widgetHorizontalScrollBarRight,
                                     widgetVerticalScrollBarRight
                                    );
            Widgets.notify(dialog,USER_EVENT_REFRESH_COLORS);
          }
        }
      }
    });
    dialog.addListener(USER_EVENT_REFRESH_COLORS,new Listener()
    {
      public void handleEvent(Event event)
      {
        updateTextColors(data.diffData,
                         widgetTextLeft,
                         widgetTextRight,
                         widgetAdded.getSelection(),
                         widgetDeleted.getSelection(),
                         widgetChanged.getSelection(),
                         widgetChangedWhitespaces.getSelection(),
                         COLOR_DIFF_NONE,
                         COLOR_DIFF_ADDED,
                         COLOR_DIFF_DELETED,
                         COLOR_DIFF_CHANGED,
                         COLOR_DIFF_CHANGED_WHITESPACES
                        );
        drawBar(data.diffData,
                widgetTextLeft.getClientArea().height/widgetTextLeft.getLineHeight(),
                widgetTextLeft.getTopIndex(),
                widgetTextLeft.getLineCount(),
                widgetBar,
                widgetAdded.getSelection(),
                widgetDeleted.getSelection(),
                widgetChanged.getSelection(),
                widgetChangedWhitespaces.getSelection(),
                COLOR_DIFF_ADDED,
                COLOR_DIFF_DELETED,
                COLOR_DIFF_CHANGED,
                COLOR_DIFF_CHANGED_WHITESPACES
               );
        updateLineColors(widgetLineDiff,
                         widgetAdded.getSelection(),
                         widgetDeleted.getSelection(),
                         widgetChanged.getSelection(),
                         widgetChangedWhitespaces.getSelection(),
                         COLOR_DIFF_NONE,
                         COLOR_DIFF_CHANGED
                        );
      }
    });
    dialog.addListener(USER_EVENT_REFRESH_BAR,new Listener()
    {
      public void handleEvent(Event event)
      {
        drawBar(data.diffData,
                widgetTextLeft.getClientArea().height/widgetTextLeft.getLineHeight(),
                widgetTextLeft.getTopIndex(),
                widgetTextLeft.getLineCount(),
                widgetBar,
                widgetAdded.getSelection(),
                widgetDeleted.getSelection(),
                widgetChanged.getSelection(),
                widgetChangedWhitespaces.getSelection(),
                COLOR_DIFF_ADDED,
                COLOR_DIFF_DELETED,
                COLOR_DIFF_CHANGED,
                COLOR_DIFF_CHANGED_WHITESPACES
               );
      }
    });

    widgetFindLeft.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetTextLeft,widgetFindLeft);
        if (index >= 0)
        {
          Widgets.notify(widgetTextLeft,USER_EVENT_SYNC);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetFindLeftPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findPrev(widgetTextLeft,widgetFindLeft);
        if (index >= 0)
        {
          Widgets.notify(widgetTextLeft,USER_EVENT_SYNC);
        }
      }
    });
    widgetFindLeftNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetTextLeft,widgetFindLeft);
        if (index >= 0)
        {
          Widgets.notify(widgetTextLeft,USER_EVENT_SYNC);
        }
      }
    });

    widgetFindRight.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetTextRight,widgetFindRight);
        if (index >= 0)
        {
          Widgets.notify(widgetTextRight,USER_EVENT_SYNC);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetFindRightPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findPrev(widgetTextRight,widgetFindRight);
        if (index >= 0)
        {
          Widgets.notify(widgetTextRight,USER_EVENT_SYNC);
        }
      }
    });
    widgetFindRightNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetTextRight,widgetFindRight);
        if (index >= 0)
        {
          Widgets.notify(widgetTextRight,USER_EVENT_SYNC);
        }
      }
    });

    // show dialog
    Dialogs.show(dialog);

    // add revisions, get selected revision index
    int selectedRevisionIndex = -1;
    if (revisions != null)
    {
      for (int z = 0; z < revisions.length; z++)
      {
        String revision = (z+1 < revisions.length) ? revisions[z+1] : "local";
        widgetRevision.add(String.format("%s -> %s",revisions[z],revision));
        if (revisionLeft != null)
        {
          if (revision.equals(revisionLeft)) selectedRevisionIndex = z;
        }
      }
      if (selectedRevisionIndex < 0) selectedRevisionIndex = revisions.length-1;
    }

    // update
    if (selectedRevisionIndex >= 0)
    {
      widgetRevision.select(selectedRevisionIndex);
      Widgets.notify(dialog,USER_EVENT_NEW_REVISION,selectedRevisionIndex);
    }
  }

  /** diff command
   * @param shell shell
   * @param repository repository
   * @param fileData file to show diff for
   */
  CommandDiff(final Shell shell, final Repository repository, final FileData fileData, String revision)
    throws RepositoryException
  {
    this(shell,repository,fileData,revision,null);
  }

  /** diff command
   * @param shell shell
   * @param repository repository
   * @param fileData file to show diff for
   */
  CommandDiff(final Shell shell, final Repository repository, final FileData fileData)
    throws RepositoryException
  {
    this(shell,repository,fileData,null);
  }

  /** run dialog
   */
  public void run()
  {
    widgetClose.setFocus();
    Dialogs.run(dialog);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandDiff {}";
  }

  //-----------------------------------------------------------------------

  /** set diff text
   * @param diffData_ diff data
   * @param widgetLineNumbersLeft left number text widget
   * @param widgetLineNumbersRight right number text widget
   * @param widgetTextLeft left text widget
   * @param widgetTextRight right text widget
   * @param widgetHorizontalScrollBarLeft left horizontal scrollbar widget
   * @param widgetVerticalScrollBarLeft left horizontal scrollbar widget
   * @param widgetHorizontalScrollBarRight right vertical scrollbar widget
   * @param widgetVerticalScrollBarRight right vertical scrollbar widget
   */
  private BitSet setText(DiffData[] diffData_,
                         Text       widgetLineNumbersLeft,
                         Text       widgetLineNumbersRight,
                         StyledText widgetTextLeft,
                         StyledText widgetTextRight,
                         ScrollBar  widgetHorizontalScrollBarLeft,
                         ScrollBar  widgetVerticalScrollBarLeft,
                         ScrollBar  widgetHorizontalScrollBarRight,
                         ScrollBar  widgetVerticalScrollBarRight
                        )
  {
    StringBuffer lineNumbersLeft  = new StringBuffer(); 
    StringBuffer lineNumbersRight = new StringBuffer(); 
    StringBuffer textLeft         = new StringBuffer(); 
    StringBuffer textRight        = new StringBuffer(); 
    int          index            = 0;                  
    int          lineNbLeft       = 1;                  
    int          lineNbRight      = 1;                  
    int          maxWidth         = 0;                  
    BitSet       diffLines = new BitSet();

    // get text
    for (DiffData diffData : diffData_)
    {
//Dprintf.dprintf("diffData=%s",diffData);
      switch (diffData.blockType)
      {
        case KEEP:
          assert(diffData.keepLines != null);

          for (String line : diffData.keepLines)
          {
            lineNumbersLeft.append(String.format("%d\n",lineNbLeft)); lineNbLeft++;
            textLeft.append(line); textLeft.append('\n');

            lineNumbersRight.append(String.format("%d\n",lineNbRight)); lineNbRight++;
            textRight.append(line); textRight.append('\n');

            maxWidth = Math.max(maxWidth,line.length());
          }
          diffLines.clear(index,index+diffData.keepLines.length);

          index += diffData.keepLines.length;
          break;
        case ADD:
          assert(diffData.addedLines != null);

          for (String line : diffData.addedLines)
          {
            lineNumbersLeft.append("\n");
            textLeft.append('\n');

            lineNumbersRight.append(String.format("%d\n",lineNbRight)); lineNbRight++;
            textRight.append(line); textRight.append('\n');

            maxWidth = Math.max(maxWidth,line.length());
          }
          diffLines.set(index,index+diffData.addedLines.length);

          index += diffData.addedLines.length;
          break;
        case DELETE:
          assert(diffData.deletedLines != null);

          for (String line : diffData.deletedLines)
          {
            lineNumbersLeft.append(String.format("%d\n",lineNbLeft)); lineNbLeft++;
            textLeft.append(line); textLeft.append('\n');

            lineNumbersRight.append('\n');
            textRight.append('\n');

            maxWidth = Math.max(maxWidth,line.length());
          }
          diffLines.set(index,index+diffData.deletedLines.length);

          index += diffData.deletedLines.length;
          break;
        case CHANGE:
          assert(diffData.addedLines != null);
          assert(diffData.deletedLines != null);

          for (String line : diffData.deletedLines)
          {
            lineNumbersLeft.append(String.format("%d\n",lineNbLeft)); lineNbLeft++;
            textLeft.append(line); textLeft.append('\n');

            maxWidth = Math.max(maxWidth,line.length());
          }
          for (int z = diffData.deletedLines.length; z < diffData.addedLines.length; z++)
          {
            lineNumbersLeft.append('\n');
            textLeft.append('\n');
          }

          for (String line : diffData.addedLines)
          {
            lineNumbersRight.append(String.format("%d\n",lineNbRight)); lineNbRight++;
            textRight.append(line); textRight.append('\n');

            maxWidth = Math.max(maxWidth,line.length());
          }
          for (int z = diffData.addedLines.length; z < diffData.deletedLines.length; z++)
          {
            lineNumbersRight.append('\n');
            textRight.append('\n');
          }

          diffLines.set(index,index+Math.max(diffData.deletedLines.length,diffData.addedLines.length));
          index += Math.max(diffData.deletedLines.length,diffData.addedLines.length);
          break;
        default:
          break;
      }
    }

    // set text
    widgetLineNumbersLeft.setText(lineNumbersLeft.toString());
    widgetLineNumbersRight.setText(lineNumbersRight.toString());
    widgetTextLeft.setText(textLeft.toString());
    widgetTextRight.setText(textRight.toString());
    // number of lines in text widgets must now be equal to index+1
    assert(widgetLineNumbersLeft.getLineCount() == index+1);
    assert(widgetLineNumbersRight.getLineCount() == index+1);
    assert(widgetTextLeft.getLineCount() == index+1);
    assert(widgetTextRight.getLineCount() == index+1);

    // set scrollbars
    widgetHorizontalScrollBarLeft.setMinimum(0);
    widgetHorizontalScrollBarLeft.setMaximum(maxWidth);
    widgetHorizontalScrollBarRight.setMinimum(0);
    widgetHorizontalScrollBarRight.setMaximum(maxWidth);
    widgetVerticalScrollBarLeft.setMinimum(0);
    widgetVerticalScrollBarLeft.setMaximum(index);
    widgetVerticalScrollBarRight.setMinimum(0);
    widgetVerticalScrollBarRight.setMaximum(index);

    // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
    widgetLineNumbersLeft.redraw();
    widgetLineNumbersRight.redraw();
    widgetTextLeft.redraw();
    widgetTextRight.redraw();
    widgetLineNumbersLeft.update();
    widgetLineNumbersRight.update();
    widgetTextLeft.update();
    widgetTextRight.update();

    // show top
    widgetLineNumbersLeft.setTopIndex(0);
    widgetLineNumbersLeft.setSelection(0);
    widgetLineNumbersRight.setTopIndex(0);
    widgetLineNumbersRight.setSelection(0);
    widgetTextLeft.setTopIndex(0);
    widgetTextLeft.setCaretOffset(0);
    widgetTextRight.setTopIndex(0);
    widgetTextRight.setCaretOffset(0);
    widgetVerticalScrollBarLeft.setSelection(0);
    widgetVerticalScrollBarRight.setSelection(0);

    return diffLines;
  }

  /** update diff text coloring
   * @param diffData_ diff data
   * @param widgetTextLeft left text widget
   * @param widgetTextRight right text widget
   * @param widgetBar bar widget
   * @param showAdded show added lines
   * @param showDeleted show deleted lines
   * @param showChanged show changed lines
   * @param showChangedWhitespaces show whitespace changes
   * @param backgroundDiffNone background color of not shown added/deleted/changed lines
   * @param backgroundDiffAdded background color added lines
   * @param backgroundDiffDeleted background color deleted lines
   * @param backgroundDiffChanged background color changed lines
   * @param backgroundDiffChangedWhitespaces background color of lines with whitespace changes only
   */
  private void updateTextColors(DiffData[] diffData_,
                                StyledText widgetTextLeft,
                                StyledText widgetTextRight,
                                boolean    showAdded,
                                boolean    showDeleted,
                                boolean    showChanged,
                                boolean    showChangedWhitespaces,
                                Color      backgroundDiffNone,
                                Color      backgroundDiffAdd,
                                Color      backgroundDiffDelete,
                                Color      backgroundDiffChange,
                                Color      backgroundDiffChangeWhitespaces
                               )
  {
    int index = 0;
    int n,m;

    for (DiffData diffData : diffData_)
    {
//Dprintf.dprintf("diffData=%s",diffData);
      switch (diffData.blockType)
      {
        case KEEP:
          assert(diffData.keepLines != null);

          n = diffData.keepLines.length;

          widgetTextRight.setLineBackground(index,n,backgroundDiffNone);

          index += n;
          break;
        case ADD:
          assert(diffData.addedLines != null);

          n = diffData.addedLines.length;

          if (showAdded)
          {
            widgetTextRight.setLineBackground(index,n,backgroundDiffAdd);
          }
          else
          {
            widgetTextRight.setLineBackground(index,n,backgroundDiffNone);
          }

          index += n;
          break;
        case DELETE:
          assert(diffData.deletedLines != null);

          n = diffData.deletedLines.length;

          if (showDeleted)
          {
            widgetTextRight.setLineBackground(index,n,backgroundDiffDelete);
          }
          else
          {
            widgetTextRight.setLineBackground(index,n,backgroundDiffNone);
          }

          index += n;
          break;
        case CHANGE:
          assert(diffData.addedLines != null);
          assert(diffData.deletedLines != null);

          n = Math.max(diffData.deletedLines.length,diffData.addedLines.length);
          m = Math.min(diffData.deletedLines.length,diffData.addedLines.length);

          if (showChanged || showChangedWhitespaces)
          {
            String line1,line2;
            for (int z = 0; z < m; z++)
            {
              line1 = diffData.deletedLines[z].replaceAll("\\s*","");
              line2 = diffData.addedLines[z].replaceAll("\\s*","");
              if (!line1.equals(line2))
              {
                // non-whitespace changes
                if (showChanged)
                {
                  // show changes
                  widgetTextLeft.setLineBackground(index,1,backgroundDiffChange);
                  widgetTextRight.setLineBackground(index,1,backgroundDiffChange);
                }
                else
                {
                  // do not show changes
                  widgetTextLeft.setLineBackground(index,1,backgroundDiffNone);
                  widgetTextRight.setLineBackground(index,1,backgroundDiffNone);
                }               
              }
              else
              {
                // whitespace changes
                if (showChangedWhitespaces)
                {
                  // show whitespace changes
                  widgetTextLeft.setLineBackground(index,n,backgroundDiffChangeWhitespaces);
                  widgetTextRight.setLineBackground(index,n,backgroundDiffChangeWhitespaces);
                }
                else
                {
                  // do not show whitespace changes
                  widgetTextLeft.setLineBackground(index,1,backgroundDiffNone);
                  widgetTextRight.setLineBackground(index,1,backgroundDiffNone);
                }
              }
            }
            widgetTextLeft.setLineBackground(index+m,n-m,backgroundDiffChange);
            widgetTextRight.setLineBackground(index+m,n-m,backgroundDiffChange);
          }
          else
          {
            // do not show any changes
            widgetTextLeft.setLineBackground(index,n,backgroundDiffNone);
            widgetTextRight.setLineBackground(index,n,backgroundDiffNone);
          }

          index += n;
          break;
        default:
          break;
      }
    }
  }

  /** set line text
   * @param widgetLineDiff line diff widget
   * @param line1,line2 lines
   */
  private void setLine(StyledText widgetLineDiff,
                       String     line1,
                       String     line2
                      )
  {
    widgetLineDiff.setText("");
    widgetLineDiff.append(line1); widgetLineDiff.append("\n");
    widgetLineDiff.append(line2); widgetLineDiff.append("\n");
  }

  /** 
   * @param widgetLineDiff line diff widget
   * @param showAdded show added lines
   * @param showDeleted show deleted lines
   * @param showChanged show changed lines
   * @param showChangedWhitespaces show whitespace changes
   * @param backgroundDiffNone background color of not shown added/deleted/changed lines
   * @param backgroundDiffChanged background color changed lines
   */
  private void updateLineColors(StyledText widgetLineDiff,
                                boolean    showAdded,
                                boolean    showDeleted,
                                boolean    showChanged,
                                boolean    showChangedWhitespaces,
                                Color      backgroundDiffNone,
                                Color      backgroundDiffChanged
                               )
  {
    if      (widgetLineDiff.getLineCount() >= 2)
    {
      int    lineIndex1 = widgetLineDiff.getOffsetAtLine(0);
      int    lineIndex2 = widgetLineDiff.getOffsetAtLine(1);
      String line1 = widgetLineDiff.getLine(0);
      String line2 = widgetLineDiff.getLine(1);

      int z = 0;
      while (z < Math.min(line1.length(),line2.length()))
      {
        if (line1.charAt(z) != line2.charAt(z))
        {
          widgetLineDiff.setStyleRange(new StyleRange(lineIndex1+z,1,null,backgroundDiffChanged));
          widgetLineDiff.setStyleRange(new StyleRange(lineIndex2+z,1,null,backgroundDiffChanged));
        }
        else
        {
          widgetLineDiff.setStyleRange(new StyleRange(lineIndex1+z,1,null,null));
          widgetLineDiff.setStyleRange(new StyleRange(lineIndex2+z,1,null,null));
        }
        z++;
      }
      for (int i = z; i < line1.length(); i++)
      {
        widgetLineDiff.setStyleRange(new StyleRange(lineIndex1+i,1,null,backgroundDiffChanged));
        widgetLineDiff.setStyleRange(new StyleRange(lineIndex2+i,1,null,backgroundDiffChanged));
      }
      for (int i = z; i < line2.length(); i++)
      {
        widgetLineDiff.setStyleRange(new StyleRange(lineIndex1+i,1,null,backgroundDiffChanged));
        widgetLineDiff.setStyleRange(new StyleRange(lineIndex2+i,1,null,backgroundDiffChanged));
      }
    }
    else if (widgetLineDiff.getLineCount() >= 1)
    {
      String line = widgetLineDiff.getLine(0);

      widgetLineDiff.setStyleRange(new StyleRange(0,line.length(),null,backgroundDiffChanged));
    }  
  }

  /** draw diff bar
   * @param diffData_ diff data
   * @param textWindowHeight text window height (in lines)
   * @param textTopIndex text top index [0..n]
   * @param textLineCount text line count
   * @param widgetBar bar widget
   * @param showAdded show added lines
   * @param showDeleted show deleted lines
   * @param showChanged show changed lines
   * @param showChangedWhitespaces show whitespace changes
   * @param backgroundDiffAdded background color added lines
   * @param backgroundDiffDeleted background color deleted lines
   * @param backgroundDiffChanged background color changed lines
   * @param backgroundDiffChangedWhitespaces background color of lines with whitespace changes only
   */
  private void drawBar(DiffData[] diffData_,
                       int        textWindowHeight,
                       int        textTopIndex,
                       int        textLineCount,
                       Canvas     widgetBar,
                       boolean    showAdded,
                       boolean    showDeleted,
                       boolean    showChanged,
                       boolean    showChangedWhitespaces,
                       Color      backgroundDiffAdded,
                       Color      backgroundDiffDeleted,
                       Color      backgroundDiffChanged,
                       Color      backgroundDiffChangedWhitespaces
                      )
  {
    GC        gc;
    Rectangle size = widgetBar.getClientArea();
    int       index = 0;
    int       n,m;
    int       y;
    int       width,height;

    // create gc
    gc = new GC(widgetBar);

    // clear
    widgetBar.drawBackground(gc,0,0,size.width,size.height);

    // draw bars
    width = size.width-1;
    for (DiffData diffData : diffData_)
    {
      switch (diffData.blockType)
      {
        case KEEP:
          assert(diffData.keepLines != null);

          n = diffData.keepLines.length;

          index += n;
          break;
        case ADD:
          assert(diffData.addedLines != null);

          n = diffData.addedLines.length;

          // draw bar for added lines
          if (showAdded)
          {
            y      = (((size.height-1)*index)+textLineCount-1)/textLineCount;
            height = Math.max(((size.height-1)*n+textLineCount-1)/textLineCount,1);
//Dprintf.dprintf("y=%d h=%d size.height=%d",y,height,size.height);

            gc.setBackground(backgroundDiffAdded);
            gc.fillRectangle(0,y,width,height);
          }

          index += n;
          break;
        case DELETE:
          assert(diffData.deletedLines != null);

          n = diffData.deletedLines.length;

          // draw bar for deleted lines
          if (showDeleted)
          {
            y      = (((size.height-1)*index)+textLineCount-1)/textLineCount;
            height = Math.max(((size.height-1)*n+textLineCount-1)/textLineCount,1);
//Dprintf.dprintf("y=%d h=%d size.height=%d",y,height,size.height);

            gc.setBackground(backgroundDiffDeleted);
            gc.fillRectangle(0,y,width,height);
          }

          index += n;
          break;
        case CHANGE:
          assert(diffData.addedLines != null);
          assert(diffData.deletedLines != null);

          n = Math.max(diffData.deletedLines.length,diffData.addedLines.length);
          m = Math.min(diffData.deletedLines.length,diffData.addedLines.length);

          if (showChanged || showChangedWhitespaces)
          {
            // draw bar for replaced lines
            String line1,line2;
            for (int z = 0; z < m; z++)
            {
              line1 = diffData.deletedLines[z].replaceAll("\\s*","");
              line2 = diffData.addedLines[z].replaceAll("\\s*","");
              if (!line1.equals(line2))
              {
                // non-whitespace changes
                if (showChanged)
                {
                  // show changes
                  y      = (((size.height-1)*(index+z))+textLineCount-1)/textLineCount;
                  height = Math.max(((size.height-1)+textLineCount-1)/textLineCount,1);
//Dprintf.dprintf("y=%d h=%d size.height=%d",y,height,size.height);

                  gc.setBackground(backgroundDiffChanged);
                  gc.fillRectangle(0,y,width,height);
                }
              }
              else
              {
                // whitespace changes
                if (showChangedWhitespaces)
                {
                  // show whitespace changes
                  y      = (((size.height-1)*(index+z))+textLineCount-1)/textLineCount;
                  height = Math.max(((size.height-1)+textLineCount-1)/textLineCount,1);
//Dprintf.dprintf("y=%d h=%d size.height=%d",y,height,size.height);

                  gc.setBackground(backgroundDiffChangedWhitespaces);
                  gc.fillRectangle(0,y,width,height);
                }
              }
            }

            // draw bar for added/deleted lines
            y      = (((size.height-1)*(index+m))+textLineCount-1)/textLineCount;
            height = Math.max(((size.height-1)*(n-m)+textLineCount-1)/textLineCount,1);
//Dprintf.dprintf("y=%d h=%d size.height=%d",y,height,size.height);

            gc.setBackground(backgroundDiffChanged);
            gc.fillRectangle(0,y,width,height);
          }

          index += n;
          break;
        default:
          break;
      }
    }

    // draw slider
    y      = ((size.height-1)*textTopIndex)/textLineCount;
    height = Math.min(((size.height-1)*textWindowHeight)/textLineCount,size.height-1);
//Dprintf.dprintf("textWindowHeight=%d textTopIndex=%d textLineCount=%d height=%d",textWindowHeight,textTopIndex,textLineCount,height);
    gc.setForeground(Onzen.COLOR_BLACK);
    gc.drawRectangle(0,y,width-1,height-1);

    // free resources
    gc.dispose();
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
