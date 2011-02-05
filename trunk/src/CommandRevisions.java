/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandRevisions.java,v $
* $Revision: 1.3 $
* $Author: torsten $
* Contents: command show file revisions tree
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.BufferedReader;
import java.io.DataOutputStream;
//import java.io.File;
import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
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
import org.eclipse.swt.custom.ScrolledComposite;
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
    RevisionData[] revisionDataTree;
    DrawInfo[]     drawInfos;
    boolean        containerResizeFlag;
    Point          containerResizeStart;
    Point          containerResizeDelta;
    Rectangle      containerResizeRectangle;
    RevisionData   selectedRevisionData0,selectedRevisionData1;
    RevisionData   selectedRevisionData;

    Data()
    {
      this.revisionDataTree         = null;
      this.drawInfos                = null;
      this.containerResizeFlag      = false;
      this.containerResizeStart     = new Point(0,0);
      this.containerResizeDelta     = new Point(0,0); 
      this.containerResizeRectangle = new Rectangle(0,0,0,0);
      this.selectedRevisionData0    = null;
      this.selectedRevisionData1    = null;
    }
  };

  // --------------------------- constants --------------------------------
  private final int MARGIN               = 10;
  private final int PADDING              = 20;
  private final int CONTAINER_MARGIN     =  4;
  private final int CONTAINER_MIN_WIDTH  = 20;
  private final int CONTAINER_MIN_HEIGHT = 20;
  private final int HANDLE_SIZE          =  8;

  private final Color COLOR_CONTAINER            = Onzen.COLOR_GRAY;
  private final Color COLOR_HANDLE               = Onzen.COLOR_RED;
  private final Color COLOR_CONTAINER_CURRENT_   = Onzen.COLOR_GRAY;
  private final Color COLOR_CONTAINER_SELECTED0  = Onzen.COLOR_BLUE;
  private final Color COLOR_CONTAINER_SELECTED1  = Onzen.COLOR_GREEN;
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
  private final Label         widgetSelectedRevision0;
  private final Label         widgetSelectedRevision1;
  private final Canvas        widgetRevisions;
  private final Button        widgetDiff;
  private final Button        widgetPatch;
  private final Label         widgetSelectedRevision;
  private final Button        widgetView;
  private final Button        widgetSave;
  private final Button        widgetRevert;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision view
   * @param repositoryTab repository tab
   * @param shell shell
   * @param repository repository
   * @param fileData file data
   */
  CommandRevisions(final RepositoryTab repositoryTab, final Shell shell, final Repository repository, final FileData fileData)
  {
    Composite         composite,subComposite;
    ScrolledComposite scrolledComposite;
    Label             label;
    Button            button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // create dialog
    dialog = Dialogs.open(shell,"Revisions: "+fileData.getFileName(),Settings.geometryRevisions.x,Settings.geometryRevisions.y,new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(1.0,1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      // create scrolled canvas
      scrolledComposite = Widgets.newScrolledComposite(composite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
      scrolledComposite.setBackground(Onzen.COLOR_WHITE);
      scrolledComposite.setLayout(new TableLayout(1.0,1.0,4));
      Widgets.layout(scrolledComposite,0,0,TableLayoutData.NSWE); //,0,0,4);
      {
        // Note: do not set canvas size here; it will increase the widget to the size of the tree!
        widgetRevisions = Widgets.newCanvas(scrolledComposite);
        widgetRevisions.setBackground(Onzen.COLOR_WHITE);
        Widgets.layout(widgetRevisions,0,0,TableLayoutData.NONE);
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
      Widgets.layout(widgetSelectedRevision0,0,1,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetListener(widgetSelectedRevision0,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision0.isDisposed()) widgetSelectedRevision0.setText((data.selectedRevisionData0 != null) ? data.selectedRevisionData0.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"->");
      Widgets.layout(label,0,2,TableLayoutData.W);

      widgetSelectedRevision1 = Widgets.newStringView(composite);
      Widgets.layout(widgetSelectedRevision1,0,3,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetListener(widgetSelectedRevision1,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision1.isDisposed()) widgetSelectedRevision1.setText((data.selectedRevisionData1 != null) ? data.selectedRevisionData1.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,4,TableLayoutData.W);

      widgetDiff = Widgets.newButton(composite,"Diff");
      widgetDiff.setEnabled(false);
      Widgets.layout(widgetDiff,0,5,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetDiff,data)
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
          CommandDiff commandDiff = new CommandDiff(repositoryTab,
                                                    dialog,
                                                    repository,
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
      Widgets.layout(widgetPatch,0,6,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetPatch,data)
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

          if (data.selectedRevisionData != null)
          {
            try
            {
Dprintf.dprintf("");
throw new RepositoryException("NYI");
            }
            catch (RepositoryException exception)
            {
              Dialogs.error(dialog,"Cannot create patch for file '%s' (error: %s)",fileData.getFileName(),exception.getMessage());
            }
          }
        }
      });

      label = Widgets.newLabel(composite,"For");
      Widgets.layout(label,0,7,TableLayoutData.W);

      widgetSelectedRevision = Widgets.newStringView(composite);
      Widgets.layout(widgetSelectedRevision,0,8,TableLayoutData.WE);
      Widgets.addModifyListener(new WidgetListener(widgetSelectedRevision,data)
      {
        public void modified(Control control)
        {
          if (!widgetSelectedRevision.isDisposed()) widgetSelectedRevision.setText((data.selectedRevisionData != null) ? data.selectedRevisionData.revision : "");
        }
      });

      label = Widgets.newLabel(composite,"do:");
      Widgets.layout(label,0,9,TableLayoutData.W);

      widgetView = Widgets.newButton(composite,"View");
      widgetView.setEnabled(false);
      Widgets.layout(widgetView,0,10,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetView,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((fileData.mode == FileData.Modes.TEXT) && (data.selectedRevisionData != null));
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

          if (data.selectedRevisionData != null)
          {
            CommandView commandView = new CommandView(repositoryTab,dialog,repository,fileData,data.selectedRevisionData.revision);
            commandView.run();
          }
        }
      });

      widgetSave = Widgets.newButton(composite,"Save");
      widgetSave.setEnabled(false);
      Widgets.layout(widgetSave,0,11,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetSave,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData != null));
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

          if (data.selectedRevisionData != null)
          {
            try
            {
              // get file
              byte[] fileDataBytes =  repositoryTab.repository.getFileBytes(fileData,data.selectedRevisionData.revision);

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

      widgetRevert = Widgets.newButton(composite,"Revert");
      widgetRevert.setEnabled(false);
      Widgets.layout(widgetRevert,0,12,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetRevert,data)
      {
        public void modified(Control control)
        {
          if (!control.isDisposed()) control.setEnabled((data.selectedRevisionData != null));
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

          if (data.selectedRevisionData != null)
          {
            if (Dialogs.confirm(dialog,String.format("Revert file '%s' to revision %s?",fileData.getFileName(),data.selectedRevisionData.revision)))
            {
              try
              {
                // revert file
                repositoryTab.repository.revert(fileData,data.selectedRevisionData.revision);

                // update state
                repositoryTab.repository.updateStates(fileData);
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    repositoryTab.updateFileStatus(fileData);
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
      Widgets.layout(widgetClose,0,13,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
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
    widgetRevisions.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        for (DrawInfo drawInfo : data.drawInfos)
        {
          if (drawInfo.container.contains(mouseEvent.x,mouseEvent.y))
          {
Dprintf.dprintf("");
            CommandRevisionInfo rommandRevisionInfo = new CommandRevisionInfo(repositoryTab,dialog,repository,fileData,drawInfo.revisionData);
            rommandRevisionInfo.run();
            break;
          }
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
        if (!data.containerResizeFlag)
        {          
          for (DrawInfo drawInfo : data.drawInfos)
          {
            if      (drawInfo.container.contains(mouseEvent.x,mouseEvent.y))
            {
              // selected revision
              data.selectedRevisionData0 = data.selectedRevisionData1;
              data.selectedRevisionData1 = drawInfo.revisionData;
              data.selectedRevisionData  = drawInfo.revisionData;

              // notify modification
              Widgets.modified(data);

              redraw();
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

          // set size and redraw
          Settings.geometryRevisionBox.x = Math.max(Settings.geometryRevisionBox.x+data.containerResizeDelta.x,CONTAINER_MIN_WIDTH );
          Settings.geometryRevisionBox.y = Math.max(Settings.geometryRevisionBox.y+data.containerResizeDelta.y,CONTAINER_MIN_HEIGHT);
          setSize();
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

    // show dialog
    Dialogs.show(dialog);

    // show
    show();
  }

  /** set scroll value
   * @param scrollX,scrollY scroll values
   */
  public void setScroll(int scrollX, int scrollY)
  {
    widgetRevisions.getHorizontalBar().setSelection(scrollX);
    widgetRevisions.getVerticalBar().setSelection(scrollY);
    redraw();
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

  /** redraw revision tree
   * @param revisionDataTree revision data tree
   * @param x,y base position
   * @param rectanglesList rectangle coordinates list
   * @param handles handles coordinates list
   */
  private void redraw(RevisionData[]      revisionDataTree,
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

    boolean firstFlag    = true;
    int     widthColumn0 = Widgets.getTextWidth(widgetRevisions,new String[]{"Revision:","Date:","Autor:"});
    Image   image        = new Image(display,ENTRY_WIDTH,ENTRY_HEIGHT);
    GC      imageGC      = new GC(image);
    GC      gc           = new GC(widgetRevisions);
    int     prevY        = y;
    int     dx,dy;
    for (RevisionData revisionData : revisionDataTree)
    {
      dy = 0;

//Dprintf.dprintf("revisionData=%s",revisionData.revision);
      // draw connection line
      if (!firstFlag)
      {
        gc.setLineWidth(1);
        gc.setForeground(COLOR_LINES);
        gc.drawLine(x+ENTRY_WIDTH/2,prevY+ENTRY_HEIGHT,
                    x+ENTRY_WIDTH/2,y
                   );
      }
      dy += PADDING;

      // draw box
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
        imageGC.drawString("Revision:",CONTAINER_MARGIN+0,CONTAINER_MARGIN+0*FONT_HEIGHT,true); imageGC.drawString(revisionData.revision,       CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+0*FONT_HEIGHT,true);
        imageGC.drawString("Date:",    CONTAINER_MARGIN+0,CONTAINER_MARGIN+1*FONT_HEIGHT,true); imageGC.drawString(revisionData.date.toString(),CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+1*FONT_HEIGHT,true);
        imageGC.drawString("Autor:",   CONTAINER_MARGIN+0,CONTAINER_MARGIN+2*FONT_HEIGHT,true); imageGC.drawString(revisionData.author,         CONTAINER_MARGIN+widthColumn0+4,CONTAINER_MARGIN+2*FONT_HEIGHT,true);
      }
      gc.drawImage(image,x,y);
      dy += ENTRY_HEIGHT;

      // draw handle
      gc.setBackground(COLOR_HANDLE);
      gc.fillRectangle(x+ENTRY_WIDTH-1,y+ENTRY_HEIGHT-1,HANDLE_SIZE,HANDLE_SIZE);
      gc.setForeground(Onzen.COLOR_BLACK);
      gc.drawRectangle(x+ENTRY_WIDTH-1,y+ENTRY_HEIGHT-1,HANDLE_SIZE,HANDLE_SIZE);

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
      for (BranchData branchData : revisionData.branches)
      {
//Dprintf.dprintf("branchData=%s",branchData);
        // get size of sub-tree
        Point subSize = getSize(branchData.revisionDataTree);

        // draw connection L-line
        gc.setLineWidth(1);
        gc.setForeground(COLOR_LINES);
        gc.drawLine(x+ENTRY_WIDTH,                      y+ENTRY_HEIGHT/2,
                    x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+ENTRY_HEIGHT/2
                   );
        gc.drawLine(x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+ENTRY_HEIGHT/2,
                    x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2,y+PADDING+ENTRY_HEIGHT
                   );

        // draw branch name
        gc.setForeground(Onzen.COLOR_BLACK);
        gc.drawString(branchData.name,x+ENTRY_WIDTH+PADDING+ENTRY_WIDTH/2+4,y+PADDING+ENTRY_HEIGHT/2,true);
//Dprintf.dprintf("%d %d %s",x+width,y+height/2,width,height,branchData.name);

        redraw(branchData.revisionDataTree,
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

      // next row
      prevY = y;
      y += dy;
      firstFlag = false;
    }
  }

  /** redraw revision tree and update draw info
   */
  private void redraw()
  {
    ArrayList<DrawInfo> drawInfoList = new ArrayList<DrawInfo>();

    // clear
    Rectangle bounds = widgetRevisions.getBounds();
    GC gc = new GC(widgetRevisions);
    gc.setBackground(Onzen.COLOR_WHITE);
    gc.fillRectangle(0,0,bounds.width,bounds.height);
    gc.dispose();

    // redraw
    redraw(data.revisionDataTree,MARGIN,MARGIN,0,0,drawInfoList);

    // get container, handles coordinates
    data.drawInfos = drawInfoList.toArray(new DrawInfo[drawInfoList.size()]);
  }

  /** redraw revision tree with container delta width/height (no update of draw infos)
   */
  private void redraw(int containerDeltaWidth, int containerDeltaHeight)
  {
    // clear
    Rectangle bounds = widgetRevisions.getBounds();
    GC gc = new GC(widgetRevisions);
    gc.setBackground(Onzen.COLOR_WHITE);
    gc.fillRectangle(0,0,bounds.width,bounds.height);
    gc.dispose();

    // redraw
    redraw(data.revisionDataTree,MARGIN,MARGIN,containerDeltaWidth,containerDeltaHeight,null);
  }

  /** set canvas size and redraw
   */
  private void setSize()
  {
    if (!widgetRevisions.isDisposed())
    {
      // set canvas size
      Point size = getSize(data.revisionDataTree);
      size.x += 2*MARGIN;
      size.y += 2*MARGIN;
      widgetRevisions.setSize(size);

      // redraw
      redraw();
    }
  }

  /** show revisions: set canvas size and draw revision tree
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
          data.revisionDataTree = null;
          Widgets.modified(data);
         }
      });
    }

    // start show revision tree
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, String revision)
      {
        // get revision tree
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
        }
//printRevisionDataTree(data.revisionDataTree);

        // show
        if (!display.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // set canvas size and redraw
              setSize();

              // notify modification
              Widgets.modified(data);
            }
          });
        }
      }
    });
  }

  /** show revisions: set canvas size and draw revision tree
   */
  private void show()
  {
    show(repositoryTab.repository.getLastRevision());
  }

  /** print revision tree (for debugging)
   * @param revisionDataTree revision data tree to print
   * @param indent indentation
   */
  private void printRevisionDataTree(RevisionData[] revisionDataTree, int indent)
  {
    for (RevisionData revisionData : revisionDataTree)
    {
      for (int z = 0; z < indent; z++) System.out.print(' ');
      System.out.println(revisionData.revision+": "+revisionData.date);
      for (BranchData branchData : revisionData.branches)
      {
        printRevisionDataTree(branchData.revisionDataTree,indent+2);
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
