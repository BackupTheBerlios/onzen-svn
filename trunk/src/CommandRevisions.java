/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command show file revisions tree
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;

// graphics
import org.eclipse.swt.dnd.Clipboard;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** view file revisions tree command
 */
class CommandRevisions
{
  /** drawing info
   */
  class DrawInfo
  {
    RevisionData revisionData;
    Rectangle    container;
    Rectangle    handle;

    /** create draw info
     * @param revisionData revision data
     * @param container container size
     * @param handle handle size
     */
    DrawInfo(RevisionData revisionData, Rectangle container, Rectangle handle)
    {
      this.revisionData = revisionData;
      this.container    = container;
      this.handle       = handle;
    }
  }

  /** dialog data
   */
  class Data
  {
    RevisionData[]      revisionDataTree;
    RevisionData[]      revisionData;
    Rectangle           view;
    Point               size;
    ArrayList<DrawInfo> drawInfoList;
    boolean             containerResizeFlag;
    Point               containerResizeStart;
    Point               containerResizeDelta;
    Rectangle           containerResizeRectangle;
    RevisionData        selectedRevisionData0,selectedRevisionData1;

    Data()
    {
      this.revisionDataTree         = null;
      this.revisionData             = null;
      this.view                     = new Rectangle(0,0,0,0);
      this.size                     = new Point(0,0);
      this.drawInfoList             = new ArrayList<DrawInfo>(128);
      this.containerResizeFlag      = false;
      this.containerResizeStart     = new Point(0,0);
      this.containerResizeDelta     = new Point(0,0);
      this.containerResizeRectangle = new Rectangle(0,0,0,0);
      this.selectedRevisionData0    = null;
      this.selectedRevisionData1    = null;
    }
  };

  // --------------------------- constants --------------------------------
  private final int MARGIN               = 10;   // margin left/top for containers in canvas
  private final int PADDING              = 20;   // padding between containers
  private final int CONTAINER_MARGIN     =  4;   // margin left/top for text in container
  private final int CONTAINER_MIN_WIDTH  = 20;   // min. width of container
  private final int CONTAINER_MIN_HEIGHT = 20;   // min. height of container
  private final int HANDLE_SIZE          =  8;   // size of resize-handle rectangle

  private final Color COLOR_CONTAINER            = Onzen.COLOR_GRAY;
  private final Color COLOR_HANDLE               = Onzen.COLOR_RED;
  private final Color COLOR_CONTAINER_CURRENT_   = Onzen.COLOR_GRAY;
  private final Color COLOR_CONTAINER_SELECTED0;
  private final Color COLOR_CONTAINER_SELECTED1;
  private final Color COLOR_LINES                = Onzen.COLOR_BLACK;

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
  private final Text          widgetSelectedRevision0;
  private final Text          widgetSelectedRevision1;
  private final Canvas        widgetRevisions;
  private final ScrollBar     widgetHorizontalScrollBar;
  private final ScrollBar     widgetVerticalScrollBar;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Button        widgetDiff;
  private final Button        widgetPatch;
  private final Text          widgetSelectedRevision;
  private final Button        widgetView;
  private final Button        widgetSave;
  private final Button        widgetRevert;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision view
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file data
   * @param revision revision to show or null
   */
  CommandRevisions(final Shell shell, final RepositoryTab repositoryTab, final FileData fileData, String revision)
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

    // init colors
    COLOR_CONTAINER_SELECTED0 = new Color(display,Settings.colorSelect0.background);
    COLOR_CONTAINER_SELECTED1 = new Color(display,Settings.colorSelect1.background);

    // create dialog
    dialog = Dialogs.open(shell,"Revisions: "+fileData.getFileName(),new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      // tree view
      widgetRevisions = Widgets.newCanvas(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      widgetRevisions.setBackground(Onzen.COLOR_WHITE);
      Widgets.layout(widgetRevisions,0,0,TableLayoutData.NSWE,0,0,4);
      widgetHorizontalScrollBar = widgetRevisions.getHorizontalBar();
      widgetVerticalScrollBar   = widgetRevisions.getVerticalBar();

      // find
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setMessage("Enter text to find");
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetModifyListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionDataTree != null) && (data.revisionData != null));
          }
        });
        widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetModifyListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionDataTree != null) && (data.revisionData != null));
          }
        });
        widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,0.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"For");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetSelectedRevision0 = Widgets.newStringView(composite);
      widgetSelectedRevision0.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedRevision0,0,1,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedRevision0,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision0.isDisposed()) widgetSelectedRevision0.setText((data.selectedRevisionData0 != null) ? data.selectedRevisionData0.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"->");
      Widgets.layout(label,0,2,TableLayoutData.W);

      widgetSelectedRevision1 = Widgets.newStringView(composite);
      widgetSelectedRevision1.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedRevision1,0,3,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedRevision1,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision1.isDisposed()) widgetSelectedRevision1.setText((data.selectedRevisionData1 != null) ? data.selectedRevisionData1.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,4,TableLayoutData.W);

      widgetDiff = Widgets.newButton(composite,"Diff",Settings.keyDiff);
      widgetDiff.setEnabled(false);
      Widgets.layout(widgetDiff,0,5,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetDiff,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData0 != null) && (data.selectedRevisionData1 != null));
        }
      });
      widgetDiff.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          CommandDiff commandDiff = new CommandDiff(dialog,
                                                    repositoryTab,
                                                    fileData,
                                                    data.selectedRevisionData0.revision,
                                                    (data.selectedRevisionData1 != null) ? data.selectedRevisionData1.revision : null
                                                   );
          commandDiff.run();
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      widgetPatch = Widgets.newButton(composite,"Patch");
      widgetPatch.setEnabled(false);
      Widgets.layout(widgetPatch,0,6,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetPatch,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData0 != null) && (data.selectedRevisionData1 != null));
        }
      });
      widgetPatch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          if (data.selectedRevisionData1 != null)
          {
            CommandCreatePatch commandCreatePatch;
            if (data.selectedRevisionData0 != null)
            {
              commandCreatePatch = new CommandCreatePatch(dialog,
                                                          repositoryTab,
                                                          fileData.toSet(),
                                                          data.selectedRevisionData0.revision,
                                                          data.selectedRevisionData1.revision,
                                                          false
                                                         );
            }
            else
            {
              commandCreatePatch = new CommandCreatePatch(dialog,
                                                          repositoryTab,
                                                          fileData.toSet(),
                                                          data.selectedRevisionData1.revision,
                                                          false
                                                         );
            }
            commandCreatePatch.run();
          }
        }
      });

      label = Widgets.newLabel(composite,"For");
      Widgets.layout(label,0,7,TableLayoutData.W);

      widgetSelectedRevision = Widgets.newStringView(composite);
      widgetSelectedRevision.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetSelectedRevision,0,8,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSelectedRevision,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision.isDisposed()) widgetSelectedRevision.setText((data.selectedRevisionData1 != null) ? data.selectedRevisionData1.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,9,TableLayoutData.W);

      widgetView = Widgets.newButton(composite,"View");
      widgetView.setEnabled(false);
      Widgets.layout(widgetView,0,10,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetView,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled(data.selectedRevisionData1 != null);
        }
      });
      widgetView.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          if (data.selectedRevisionData1 != null)
          {
            CommandView commandView = new CommandView(dialog,repositoryTab,fileData,data.selectedRevisionData1.revision);
            commandView.run();
          }
        }
      });

      widgetSave = Widgets.newButton(composite,"Save...");
      widgetSave.setEnabled(false);
      Widgets.layout(widgetSave,0,11,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetSave,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData1 != null));
        }
      });
      widgetSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          if (data.selectedRevisionData1 != null)
          {
            try
            {
              // get file
              byte[] fileDataBytes =  repositoryTab.repository.getFileBytes(fileData,data.selectedRevisionData1.revision);

              // save to file
              String fileName = Dialogs.fileSave(dialog,"Save file");
              if (fileName != null)
              {
                try
                {
                  DataOutputStream output = new DataOutputStream(new FileOutputStream(fileName));
                  output.write(fileDataBytes);
                  output.close();
                }
                catch (IOException exception)
                {
                  Dialogs.error(dialog,"Cannot save file '%s' (error: %s)",fileData.getFileName(),exception.getMessage());
                }
              }
            }
            catch (RepositoryException exception)
            {
              Dialogs.error(dialog,"Cannot save file '%s' (error: %s)",fileData.getFileName(),exception.getMessage());
            }
          }
        }
      });

      widgetRevert = Widgets.newButton(composite,"Revert...",Settings.keyRevert);
      widgetRevert.setEnabled(false);
      Widgets.layout(widgetRevert,0,12,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetModifyListener(widgetRevert,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData1 != null));
        }
      });
      widgetRevert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          if (data.selectedRevisionData1 != null)
          {
            if (Dialogs.confirm(dialog,String.format("Revert file '%s' to revision %s?",fileData.getFileName(),data.selectedRevisionData1.revision)))
            {
              try
              {
                // revert file
                repositoryTab.repository.revert(fileData,data.selectedRevisionData1.revision);

                // update state
                repositoryTab.repository.updateStates(fileData);
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    if (!repositoryTab.widgetComposite.isDisposed()) repositoryTab.updateTreeItem(fileData);
                  }
                });
              }
              catch (RepositoryException exception)
              {
                Dialogs.error(dialog,"Cannot revert file '%s' (error: %s)",fileData.getFileName(),exception.getMessage());
              }
            }
          }
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,13,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryRevisions = dialog.getSize();

          Dialogs.close(dialog);
        }
      });
    }

    // listeners
    widgetRevisions.addPaintListener(new PaintListener()
    {
      public void paintControl(PaintEvent paintEvent)
      {
        redraw();
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

        // scroll
        int n = widget.getSelection();
        int dx = -n-data.view.x;
        widgetRevisions.scroll(dx,0,0,0,data.size.x,data.size.y,false);
        widgetRevisions.redraw();

        // save origin
        data.view.x = -n;
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

        // scroll
        int n = widget.getSelection();
        int dy = -n-data.view.y;
        widgetRevisions.scroll(0,dy,0,0,data.size.x,data.size.y,false);
        widgetRevisions.redraw();

        // save origin
        data.view.y = -n;
      }
    });
    widgetRevisions.addListener(SWT.Resize,new Listener()
    {
      public void handleEvent(Event event)
      {
        Rectangle clientArea = widgetRevisions.getClientArea ();

        data.view.width  = clientArea.width;
        data.view.height = clientArea.height;

        widgetHorizontalScrollBar.setThumb(Math.min(data.view.width,data.size.x));
        widgetVerticalScrollBar.setThumb(Math.min(data.view.height,data.size.y));
        widgetRevisions.redraw();
      }
    });
    widgetRevisions.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        for (DrawInfo drawInfo : data.drawInfoList)
        {
          if (drawInfo.container.contains(mouseEvent.x,mouseEvent.y))
          {
            CommandRevisionInfo commandRevisionInfo = new CommandRevisionInfo(dialog,repositoryTab,fileData,drawInfo.revisionData.revision);
            commandRevisionInfo.run();
            break;
          }
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
        if (!data.containerResizeFlag)
        {
          for (DrawInfo drawInfo : data.drawInfoList)
          {
            if      (drawInfo.container.contains(mouseEvent.x,mouseEvent.y))
            {
              // selected revision
              data.selectedRevisionData0 = data.selectedRevisionData1;
              data.selectedRevisionData1 = drawInfo.revisionData;

              // notify modification
              Widgets.modified(data);

              widgetRevisions.redraw();
              break;
            }
            else if (drawInfo.handle.contains(mouseEvent.x,mouseEvent.y))
            {
              // start resize container
              data.containerResizeStart.x          = mouseEvent.x;
              data.containerResizeStart.y          = mouseEvent.y;
              data.containerResizeDelta.x          = 0;
              data.containerResizeDelta.y          = 0;
              data.containerResizeRectangle.x      = drawInfo.container.x;
              data.containerResizeRectangle.y      = drawInfo.container.y;
              data.containerResizeRectangle.width  = drawInfo.container.width;
              data.containerResizeRectangle.height = drawInfo.container.height;
              data.containerResizeFlag             = true;
              break;
            }
          }
        }
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
        if (data.containerResizeFlag)
        {
          // stop resize container
          data.containerResizeDelta.x = mouseEvent.x-data.containerResizeStart.x;
          data.containerResizeDelta.y = mouseEvent.y-data.containerResizeStart.y;
          data.containerResizeFlag    = false;

          // set canvas size
          Settings.geometryRevisionBox.x = Math.max(Settings.geometryRevisionBox.x+data.containerResizeDelta.x,CONTAINER_MIN_WIDTH );
          Settings.geometryRevisionBox.y = Math.max(Settings.geometryRevisionBox.y+data.containerResizeDelta.y,CONTAINER_MIN_HEIGHT);
          setSize();

          // redraw
          widgetRevisions.redraw();
        }
      }
    });
    widgetRevisions.addMouseMoveListener(new MouseMoveListener()
    {
      public void mouseMove(MouseEvent mouseEvent)
      {
        if (data.containerResizeFlag)
        {
          data.containerResizeDelta.x = mouseEvent.x-data.containerResizeStart.x;
          data.containerResizeDelta.y = mouseEvent.y-data.containerResizeStart.y;
          redraw(data.containerResizeDelta.x,data.containerResizeDelta.y);
        }
      }
    });
    widgetRevisions.addKeyListener(new KeyListener()
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
    });
    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        RevisionData revisionData = findNext(widgetFind);
        if (revisionData != null)
        {
          // select revision and show
          data.selectedRevisionData0 = null;
          data.selectedRevisionData1 = revisionData;
          scrollTo(revisionData);

          // notify modification
          Widgets.modified(data);
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
        RevisionData revisionData = findPrev(widgetFind);
        if (revisionData != null)
        {
          // select revision and show
          data.selectedRevisionData0 = null;
          data.selectedRevisionData1 = revisionData;
          scrollTo(revisionData);

          // notify modification
          Widgets.modified(data);
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
        RevisionData revisionData = findNext(widgetFind);
        if (revisionData != null)
        {
          // select revision and show
          data.selectedRevisionData0 = null;
          data.selectedRevisionData1 = revisionData;
          scrollTo(revisionData);

          // notify modification
          Widgets.modified(data);
        }
      }
    });

    KeyListener keyListener = new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (((keyEvent.stateMask & SWT.MODIFIER_MASK) == SWT.NONE) && (keyEvent.keyCode == SWT.PAGE_UP))
        {
          // goto one page up
          int pageIncrement = widgetVerticalScrollBar.getPageIncrement();
          int n = widgetVerticalScrollBar.getSelection();
          n = (n-pageIncrement > 0) ? n-pageIncrement : 0;
          widgetVerticalScrollBar.setSelection(n);

          // save origin
          data.view.y = -n;

          // redraw
          widgetRevisions.redraw();
        }
        else if (((keyEvent.stateMask & SWT.MODIFIER_MASK) == SWT.NONE) && (keyEvent.keyCode == SWT.PAGE_DOWN))
        {
          // goto one page down
          int pageIncrement = widgetVerticalScrollBar.getPageIncrement();
          int maximum = widgetVerticalScrollBar.getMaximum();
          int n = widgetVerticalScrollBar.getSelection();
          n = (n+pageIncrement < maximum-pageIncrement) ? n+pageIncrement : maximum-pageIncrement;
          widgetVerticalScrollBar.setSelection(n);

          // save origin
          data.view.y = -n;

          // redraw
          widgetRevisions.redraw();
        }
        else if (((keyEvent.stateMask & SWT.MODIFIER_MASK) == SWT.NONE) && (keyEvent.keyCode == SWT.HOME))
        {
          // goto top
          widgetVerticalScrollBar.setSelection(0);

          // save origin
          data.view.y = 0;

          // redraw
          widgetRevisions.redraw();
        }
        else if (((keyEvent.stateMask & SWT.MODIFIER_MASK) == SWT.NONE) && (keyEvent.keyCode == SWT.END))
        {
          // goto bottom
          int pageIncrement = widgetVerticalScrollBar.getPageIncrement();
          int n = widgetVerticalScrollBar.getMaximum()-pageIncrement;
          widgetVerticalScrollBar.setSelection(n);

          // save origin
          data.view.y = -n;

          // redraw
          widgetRevisions.redraw();
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFind))
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
        else if (Widgets.isAccelerator(keyEvent,Settings.keyDiff))
        {
          Widgets.invoke(widgetDiff);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyRevert))
        {
          Widgets.invoke(widgetRevert);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    };
    widgetRevisions.addKeyListener(keyListener);
    widgetFind.addKeyListener(keyListener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryRevisions,Settings.setWindowLocation);

    // show
    show(revision);
  }

  /** create revision view
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file data
   */
  CommandRevisions(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData,fileData.workingRevision);
  }

  /** set scroll value
   * @param scrollX,scrollY scroll values
   */
  public void setScroll(int scrollX, int scrollY)
  {
    widgetRevisions.getHorizontalBar().setSelection(scrollX);
    widgetRevisions.getVerticalBar().setSelection(scrollY);
    widgetRevisions.redraw();
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
    return "CommandRevisions {}";
  }

  //-----------------------------------------------------------------------

  /** get size of revision tree
   * @param revisionDataTree revision data tree
   * @return size of revision tree (in pixel)
   */
  private Point getSize(RevisionData[] revisionDataTree)
  {
    final int ENTRY_WIDTH  = Settings.geometryRevisionBox.x;
    final int ENTRY_HEIGHT = Settings.geometryRevisionBox.y;

    Point size = new Point(0,0);

    for (RevisionData revisionData : revisionDataTree)
    {
      size.y += PADDING;

      size.x = Math.max(size.x,ENTRY_WIDTH);
      size.y += ENTRY_HEIGHT;
//Dprintf.dprintf("size=%s",size);

      if (revisionData.branches != null)
      {
        Point maxSubSize = new Point(0,0);
        for (BranchData branchData : revisionData.branches)
        {
          Point subSize = getSize(branchData.revisionDataTree);
//Dprintf.dprintf("subSize=%s",subSize);
          maxSubSize.x = Math.max(maxSubSize.x,subSize.x);
          maxSubSize.y = Math.max(maxSubSize.y,subSize.y);
        }
//Dprintf.dprintf("maxSubSize=%s",maxSubSize);

        // next column, get max. dy
        size.x = Math.max(size.x,ENTRY_WIDTH+PADDING+maxSubSize.x);
        size.y = size.y+maxSubSize.y;
      }
    }

    return size;
  }

  /** get closest revision in revision tree
   * @param revisionDataTree revision data tree
   * @param revision revision
   * @return closest revision data or null
   */
  private RevisionData getClosestRevision(RevisionData[] revisionDataTree, String revision)
  {
    RevisionData closestRevisionData = null;

    if (revision.equals(repositoryTab.repository.getLastRevision()))
    {
      closestRevisionData = (revisionDataTree.length > 0) ? revisionDataTree[revisionDataTree.length-1] : null;
    }
    else
    {
      for (RevisionData revisionData : revisionDataTree)
      {
        if (   (revisionData.revision.compareTo(revision) <= 0)
            && ((closestRevisionData == null) || (revisionData.revision.compareTo(closestRevisionData.revision) > 0))
           )
        {
          closestRevisionData = revisionData;
        }
        else
        {
          if (revisionData.branches != null)
          {
            for (BranchData branchData : revisionData.branches)
            {
              RevisionData subRevisionData = getClosestRevision(branchData.revisionDataTree,revision);
              if (subRevisionData != null)
              {
                closestRevisionData = subRevisionData;
              }
            }
          }
        }
      }
    }

    return closestRevisionData;
  }

/** get closest revision in revision tree
   * @param revisionDataTree revision data tree
   * @param revision revision
   * @return closest revision data or null
   */
  private RevisionData getNextRevision(RevisionData[] revisionDataTree, RevisionData revisionData)
  {
    RevisionData nextRevisionData = null;

    for (int z = 0; z < revisionDataTree.length; z++)
    {
      if (revisionDataTree[z] == revisionData)
      {
        nextRevisionData = ((z+1) < revisionDataTree.length) ? revisionDataTree[z+1] : null;
        break;
      }
      else
      {
        if (revisionData.branches != null)
        {
          for (BranchData branchData : revisionData.branches)
          {
            RevisionData subRevisionData = getNextRevision(branchData.revisionDataTree,revisionData);
            if (subRevisionData != null)
            {
              nextRevisionData = subRevisionData;
              break;
            }
          }
        }
      }
    }

    return nextRevisionData;
  }

  /** get origin (x0,y0) of revision in revision tree
   * @param revisionDataTree revision data tree
   * @param revision revision
   * @return point (in pixel) or null
   */
  private Point getRevisionX0Y0(RevisionData[] revisionDataTree, String revision)
  {
    final int ENTRY_WIDTH  = Settings.geometryRevisionBox.x;
    final int ENTRY_HEIGHT = Settings.geometryRevisionBox.y;

    Point point = new Point(0,0);

    boolean firstFlag = true;
    for (RevisionData revisionData : revisionDataTree)
    {
      if (revisionData.revision.equals(revision))
      {
        return point;
      }
      else
      {
        point.y += PADDING;

        point.y += ENTRY_HEIGHT;
//Dprintf.dprintf("size=%s",size);

        if (revisionData.branches != null)
        {
          Point maxSubSize = new Point(0,0);
          for (BranchData branchData : revisionData.branches)
          {
            Point subPoint = getRevisionX0Y0(branchData.revisionDataTree,revision);
            if (subPoint != null)
            {
              point.x += ENTRY_WIDTH+PADDING+subPoint.x;
              point.y += subPoint.y;

              return point;
            }
            else
            {
              Point subSize = getSize(branchData.revisionDataTree);

              maxSubSize.x = Math.max(maxSubSize.x,PADDING+subSize.x);
              maxSubSize.y = Math.max(maxSubSize.y,subSize.y);
            }
          }
//Dprintf.dprintf("maxSubSize=%s",maxSubSize);

          // next column, get max. dy
//          point.x += Math.max(point.x,ENTRY_WIDTH+PADDING+maxSubSize.x);
          point.y += maxSubSize.y;
        }

        firstFlag = false;
      }
    }

    return null;
  }

  /** get origin (x0,y0) of revision in revision tree
   * @param revision revision
   * @return point (in pixel) or null
   */
  private Point getRevisionX0Y0(String revision)
  {
    return getRevisionX0Y0(data.revisionDataTree,revision);
  }

  /** get origin (x0,y0) of revision in revision tree
   * @param revisionData revision data
   * @return point (in pixel) or null
   */
  private Point getRevisionX0Y0(RevisionData revisionData)
  {
    return getRevisionX0Y0(revisionData.revision);
  }

  /** redraw revision data tree
   * @param view draw bounds
   * @param gc graphics context
   * @param image image object
   * @param imageGC image graphics context
   * @param revisionDataTree revision data tree
   * @param x,y base position
   * @param containerDeltaWidth,containerDeltaHeight container delta for resize containers or 0,0
   * @param drawInfoList draw info list
   */
  private void redraw(Rectangle           view,
                      GC                  gc,
                      Image               image,
                      GC                  imageGC,
                      RevisionData[]      revisionDataTree,
                      int                 x,
                      int                 y,
                      int                 containerDeltaWidth,
                      int                 containerDeltaHeight,
                      ArrayList<DrawInfo> drawInfoList
                     )
  {
    final int ENTRY_WIDTH  = Math.max(Settings.geometryRevisionBox.x+containerDeltaWidth, CONTAINER_MIN_WIDTH );
    final int ENTRY_HEIGHT = Math.max(Settings.geometryRevisionBox.y+containerDeltaHeight,CONTAINER_MIN_HEIGHT);
    final int FONT_HEIGHT  = Widgets.getTextHeight(widgetRevisions);

    final int Y_MIN = -ENTRY_HEIGHT;
    final int Y_MAX = -view.y+view.height;

    boolean firstFlag    = true;
    int     widthColumn0 = Widgets.getTextWidth(widgetRevisions,new String[]{"Revision:","Date:","Autor:"});
    int     prevY        = y;
    int     dx,dy;
    for (RevisionData revisionData : revisionDataTree)
    {
      dy = 0;

//Dprintf.dprintf("revisionData=%s",revisionData.revision);
      // draw connection line
      if (!firstFlag)
      {
        if (   (prevY+ENTRY_HEIGHT >= Y_MIN)
            && (y >= Y_MIN)
           )
        {
          gc.setLineWidth(1);
          gc.setForeground(COLOR_LINES);
          gc.drawLine(x+ENTRY_WIDTH/2,prevY+ENTRY_HEIGHT,
                      x+ENTRY_WIDTH/2,y
                     );
        }
      }
      dy += PADDING;

      // draw box
      if (y >= Y_MIN)
      {
        {
          imageGC.setBackground(COLOR_CONTAINER);
          imageGC.fillRectangle(0,0,ENTRY_WIDTH,ENTRY_HEIGHT);

          if      (revisionData == data.selectedRevisionData0)
          {
            imageGC.setLineWidth(3);
            imageGC.setForeground(COLOR_CONTAINER_SELECTED0);
          }
          else if (revisionData == data.selectedRevisionData1)
          {
            imageGC.setLineWidth(3);
            imageGC.setForeground(COLOR_CONTAINER_SELECTED1);
          }
          else
          {
            imageGC.setLineWidth(1);
            imageGC.setForeground(Onzen.COLOR_BLACK);
          }
          imageGC.drawRectangle(0,0,ENTRY_WIDTH-1,ENTRY_HEIGHT-1);

          imageGC.setForeground(Onzen.COLOR_BLACK);
          imageGC.drawString("Revision:",CONTAINER_MARGIN+0,CONTAINER_MARGIN+0*FONT_HEIGHT,true); imageGC.drawString(revisionData.getRevisionText(),                 CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+0*FONT_HEIGHT,true);
          imageGC.drawString("Date:",    CONTAINER_MARGIN+0,CONTAINER_MARGIN+1*FONT_HEIGHT,true); imageGC.drawString(Onzen.DATETIME_FORMAT.format(revisionData.date),CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+1*FONT_HEIGHT,true);
          imageGC.drawString("Autor:",   CONTAINER_MARGIN+0,CONTAINER_MARGIN+2*FONT_HEIGHT,true); imageGC.drawString(revisionData.author,                            CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+2*FONT_HEIGHT,true);
          imageGC.drawString("Message:", CONTAINER_MARGIN+0,CONTAINER_MARGIN+3*FONT_HEIGHT,true); imageGC.drawText(StringUtils.join(revisionData.commitMessage,"\n"),CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+3*FONT_HEIGHT,true);
        }
        gc.drawImage(image,x,y);
      }
      dy += ENTRY_HEIGHT;
//Dprintf.dprintf("revisionData.getRevisionText()=%s %d %d",revisionData.getRevisionText(),x,y);

      // draw handle
      if (y >= Y_MIN)
      {
        gc.setBackground(COLOR_HANDLE);
        gc.fillRectangle(x+ENTRY_WIDTH-1,y+ENTRY_HEIGHT-1,HANDLE_SIZE,HANDLE_SIZE);
        gc.setForeground(Onzen.COLOR_BLACK);
        gc.drawRectangle(x+ENTRY_WIDTH-1,y+ENTRY_HEIGHT-1,HANDLE_SIZE,HANDLE_SIZE);
//xxx=(xxx+1)%100;
//gc.drawString(String.format("y=%d time=%d",y,System.currentTimeMillis()),x+ENTRY_WIDTH+30+xxx,y+ENTRY_HEIGHT+12);
      }

      if (drawInfoList != null)
      {
        // add container, handle coordinates info
        drawInfoList.add(new DrawInfo(revisionData,
                                      new Rectangle(x,y,ENTRY_WIDTH,ENTRY_HEIGHT),
                                      new Rectangle(x+ENTRY_WIDTH-1,y+ENTRY_HEIGHT-1,HANDLE_SIZE,HANDLE_SIZE)
                                     )
                        );
      }

      // draw branches
      dx = 0;
      if (revisionData.branches != null)
      {
        for (BranchData branchData : revisionData.branches)
        {
//Dprintf.dprintf("branchData=%s",branchData);
          // get size of sub-tree
          Point subSize = getSize(branchData.revisionDataTree);

          // draw connection L-line
          if (y+ENTRY_HEIGHT/2 >= Y_MIN)
          {
            gc.setLineWidth(1);
            gc.setForeground(COLOR_LINES);
            gc.drawLine(x+ENTRY_WIDTH,                      y+ENTRY_HEIGHT/2,
                        x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+ENTRY_HEIGHT/2
                       );
            gc.drawLine(x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+ENTRY_HEIGHT/2,
                        x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+PADDING+ENTRY_HEIGHT
                       );
          }

          // draw branch name
          if (y+PADDING+ENTRY_HEIGHT/2 >= Y_MIN)
          {
            gc.setForeground(Onzen.COLOR_BLACK);
            gc.drawString(branchData.name,x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2+4,y+PADDING+ENTRY_HEIGHT/2,true);
//Dprintf.dprintf("%d %d %s",x+width,y+height/2,width,height,branchData.name);
          }

          redraw(view,
                 gc,
                 image,
                 imageGC,
                 branchData.revisionDataTree,
                 x+dx+PADDING+ENTRY_WIDTH,
                 y+   PADDING+ENTRY_HEIGHT,
                 containerDeltaWidth,
                 containerDeltaHeight,
                 drawInfoList
                );

          // next column, get max. dy
          dx += PADDING+subSize.x;
          dy = Math.max(dy,PADDING+ENTRY_HEIGHT+subSize.y);
        }
      }

      // next row
      prevY = y;
      y += dy;
      firstFlag = false;

//Dprintf.dprintf("%d %d",y,view.y+view.height);
      // stop when rest is invisible
      if (y > Y_MAX) break;
    }
  }

  /** redraw revision tree with container delta width/height (no update of draw infos)
   */
  private void redraw(int containerDeltaWidth, int containerDeltaHeight)
  {
    // redraw
    if (data.revisionDataTree != null)
    {
      final int ENTRY_WIDTH  = Math.max(Settings.geometryRevisionBox.x+containerDeltaWidth ,CONTAINER_MIN_WIDTH );
      final int ENTRY_HEIGHT = Math.max(Settings.geometryRevisionBox.y+containerDeltaHeight,CONTAINER_MIN_HEIGHT);

      GC    gc      = new GC(widgetRevisions);
      Image image   = new Image(display,ENTRY_WIDTH,ENTRY_HEIGHT);
      GC    imageGC = new GC(image);

      // clear
      gc.setBackground(Onzen.COLOR_WHITE);
      gc.fillRectangle(0,0,data.view.width,data.view.height);

      // redraw
      redraw(data.view,
             gc,
             image,
             imageGC,
             data.revisionDataTree,
             data.view.x+MARGIN,
             data.view.y+MARGIN,
             containerDeltaWidth,
             containerDeltaHeight,
             null
            );

      gc.dispose();
    }
  }

  /** redraw revision tree and update draw info
   * @param clearFlag true to clear background
   */
  private void redraw()
  {
    if (data.revisionDataTree != null)
    {
      final int ENTRY_WIDTH  = Math.max(Settings.geometryRevisionBox.x,CONTAINER_MIN_WIDTH );
      final int ENTRY_HEIGHT = Math.max(Settings.geometryRevisionBox.y,CONTAINER_MIN_HEIGHT);

      data.drawInfoList.clear();

      Rectangle clientArea = widgetRevisions.getClientArea();
      GC        gc         = new GC(widgetRevisions);
      Image     image      = new Image(display,ENTRY_WIDTH,ENTRY_HEIGHT);
      GC        imageGC    = new GC(image);

      // clear
      gc.setBackground(Onzen.COLOR_WHITE);
      gc.fillRectangle(0,0,clientArea.width,clientArea.height);

      // draw
      redraw(data.view,
             gc,
             image,
             imageGC,
             data.revisionDataTree,
             data.view.x+MARGIN,
             data.view.y+MARGIN,
             0,
             0,
             data.drawInfoList
            );

      gc.dispose();
    }
  }

  /** set canvas size from revision tree size
   */
  private void setSize()
  {
    if (!widgetRevisions.isDisposed())
    {
      // get size
      data.size = getSize(data.revisionDataTree);
      data.size.x += 2*MARGIN;
      data.size.y += 2*MARGIN;

      // set scroll bars
      Rectangle clientArea = widgetRevisions.getClientArea();
      widgetHorizontalScrollBar.setMaximum(data.size.x);
      widgetVerticalScrollBar.setMaximum(data.size.y);
      widgetHorizontalScrollBar.setThumb(Math.min(clientArea.width,data.size.x));
      widgetVerticalScrollBar.setThumb(Math.min(clientArea.height,data.size.y));

      widgetHorizontalScrollBar.setIncrement(Settings.geometryRevisionBox.x+PADDING);
      widgetHorizontalScrollBar.setPageIncrement(clientArea.width);
      widgetVerticalScrollBar.setIncrement(Settings.geometryRevisionBox.y+PADDING);
      widgetVerticalScrollBar.setPageIncrement(clientArea.height);
    }
  }

  /** append revision data to revision data array
   * @param revisionDataArray array
   * @param revisionData_ revision data to append
   */
  private void revisionDataTreeToArray(ArrayList<RevisionData> revisionDataArray, RevisionData[] revisionData_)
  {
    for (RevisionData revisionData : revisionData_)
    {
      revisionDataArray.add(revisionData);
      if (revisionData.branches != null)
      {
        for (BranchData branchData : revisionData.branches)
        {
          revisionDataTreeToArray(revisionDataArray,branchData.revisionDataTree);
        }
      }
    }
  }

  /** scroll to revision
   * @param revisionData revision to show
   */
  private void scrollTo(final RevisionData revisionData)
  {
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          final int ENTRY_WIDTH  = Settings.geometryRevisionBox.x;
          final int ENTRY_HEIGHT = Settings.geometryRevisionBox.y;

          // get x,y-offset of revision
          Point point = getRevisionX0Y0(revisionData);
          point.x += ENTRY_WIDTH /2;
          point.y += ENTRY_HEIGHT/2;
//Dprintf.dprintf("point=%s",point);

          // scroll
          Rectangle clientArea = widgetRevisions.getClientArea();
          clientArea.x = widgetHorizontalScrollBar.getSelection();
          clientArea.y = widgetVerticalScrollBar.getSelection();
//Dprintf.dprintf("clientArea=%s",clientArea);
          if ((point.x < clientArea.x) || (point.x > clientArea.x+clientArea.width )) data.view.x = -(point.x-ENTRY_WIDTH /2);
          if ((point.y < clientArea.y) || (point.y > clientArea.y+clientArea.height)) data.view.y = -(point.y-ENTRY_HEIGHT/2);
Dprintf.dprintf("data.view=%s",data.view);

//Dprintf.dprintf("revisionData=%s",revisionData);
          RevisionData nextRevisionData = getNextRevision(data.revisionDataTree,revisionData);;
          int          verticalSpace    = clientArea.height-(PADDING+ENTRY_HEIGHT);
          while ((verticalSpace > 0) && (nextRevisionData != null))
          {
            nextRevisionData = getNextRevision(data.revisionDataTree,nextRevisionData);
//Dprintf.dprintf("nextRevisionData=%s",nextRevisionData);
            verticalSpace -= (PADDING+ENTRY_HEIGHT);
          }
data.view.y += verticalSpace;
//Dprintf.dprintf("data.view=%s",data.view);

          widgetRevisions.scroll(-data.view.x,-data.view.y,0,0,data.size.x,data.size.y,false);
          widgetRevisions.redraw();
          widgetHorizontalScrollBar.setSelection(-data.view.x);
          widgetVerticalScrollBar.setSelection(-data.view.y);
        }
      });
    }
  }

  /** show revisions
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
          data.revisionDataTree = null;
          Widgets.modified(data);
         }
      });
    }

    // start show revision tree
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, final String revision)
      {
        // get revision tree
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get revision data for '%s'...",fileData.getFileName());
        try
        {
          data.revisionDataTree = repositoryTab.repository.getRevisionDataTree(fileData);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file revisions fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
          Widgets.resetCursor(dialog);
        }
//Dprintf.dprintf(""); printRevisionDataTree(data.revisionDataTree);

        // convert tree into an array for find function
        ArrayList<RevisionData> revisionDataArray = new ArrayList<RevisionData>();
        revisionDataTreeToArray(revisionDataArray,data.revisionDataTree);
        data.revisionData = revisionDataArray.toArray(new RevisionData[revisionDataArray.size()]);

        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              final int ENTRY_WIDTH  = Settings.geometryRevisionBox.x;
              final int ENTRY_HEIGHT = Settings.geometryRevisionBox.y;

              // set canvas size
              setSize();

              // redraw/scroll to selected revision
              data.selectedRevisionData1 = getClosestRevision(data.revisionDataTree,revision);
              if (data.selectedRevisionData1 != null)
              {
                scrollTo(data.selectedRevisionData1);
              }
              else
              {
                widgetRevisions.redraw();
              }

              // notify modification
              Widgets.modified(data);
            }
          });
        }
      }
    });
  }

  /** show last revision
   */
  private void show()
  {
    show(repositoryTab.repository.getLastRevision());
  }

  /** search previous revision
   * @param widgetFind search text widget
   * @return revision data or null
   */
  private RevisionData findPrev(Text widgetFind)
  {
    RevisionData revisionData = null;

    if (!widgetFind.isDisposed() && (data.revisionData != null))
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get selected/first revision
        RevisionData selectedRevisionData = (data.selectedRevisionData1 != null) ? data.selectedRevisionData1 : data.revisionData[0];

        // find current revision
        int z = 0;
        while ((z < data.revisionData.length) && (data.revisionData[z] != selectedRevisionData))
        {
          z++;
        }

        // find previous matching revision
        do
        {
//Dprintf.dprintf("data.revisionData[z]=%s",data.revisionData[z]);
          z--;
        }
        while (   (z >= 0)
               && !data.revisionData[z].match(findText)
              );

        if (z >= 0)
        {
//Dprintf.dprintf("data.revisionData[z]=%s",data.revisionData[z]);
          // select previsous revision
          revisionData = data.revisionData[z];
        }
        else
        {
          // not found
          Widgets.flash(widgetFind);
        }
      }
    }

    return revisionData;
  }

  /** search next revision
   * @param widgetFind search text widget
   * @return revision data or null
   */
  private RevisionData findNext(Text widgetFind)
  {
    RevisionData revisionData = null;

    if (!widgetFind.isDisposed() && (data.revisionData != null))
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // find current revision index or -1 if non selected
        int z;
        if (data.selectedRevisionData1 != null)
        {
          z = 0;
          while ((z < data.revisionData.length) && (data.revisionData[z] != data.selectedRevisionData1))
          {
            z++;
          }
        }
        else
        {
          z = -1;
        }

        // find next matching revision
        do
        {
          z++;
        }
        while (   (z < data.revisionData.length)
               && !data.revisionData[z].match(findText)
              );

        if (z < data.revisionData.length)
        {
          // select next revision
          revisionData = data.revisionData[z];
        }
        else
        {
          // not found
          Widgets.flash(widgetFind);
        }
      }
    }

    return revisionData;
  }

  /** print revision tree (for debugging)
   * @param revisionDataTree revision data tree to print
   * @param indent indentation
   */
  private void printRevisionDataTree(RevisionData[] revisionDataTree, int indent)
  {
    for (RevisionData revisionData : revisionDataTree)
    {
      System.out.print(StringUtils.repeat(' ',indent));
      System.out.println(revisionData.revision+": "+revisionData.date);
      if (revisionData.parents != null)
      {
        for (RevisionData parentRevisionData : revisionData.parents)
        {
          System.out.print(StringUtils.repeat(' ',indent+2)+"parent ");
          System.out.println(parentRevisionData.revision);
        }
      }

      if (revisionData.branches != null)
      {
        System.out.print(StringUtils.repeat(' ',indent+2)+"branch:");
        for (BranchData branchData : revisionData.branches)
        {
          printRevisionDataTree(branchData.revisionDataTree,indent+4);
        }
      }
    }
  }

  /** print revision tree (for debugging)
   * @param revisionDataTree revision data tree to print
   */
  private void printRevisionDataTree(RevisionData[] revisionDataTree)
  {
    System.out.println("Revision tree:");
    printRevisionDataTree(revisionDataTree,2);
  }
}

/* end of file */
