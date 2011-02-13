/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Onzen.java,v $
* $Revision: 1.5 $
* $Author: torsten $
* Contents: Onzen
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Iterator;

// graphics
import org.eclipse.swt.custom.SashForm;
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
import org.eclipse.swt.graphics.Font;
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

/** units
 */
class Units
{
  /** get byte size string
   * @param n byte value
   * @return string
   */
  public static String getByteSize(double n)
  {
    if      (n >= 1024*1024*1024) return String.format(Locale.US,"%.1f",n/(1024*1024*1024));
    else if (n >=      1024*1024) return String.format(Locale.US,"%.1f",n/(     1024*1024));
    else if (n >=           1024) return String.format(Locale.US,"%.1f",n/(          1024));
    else                          return String.format(Locale.US,"%d"  ,(long)n           );
  }

  /** get byte size unit
   * @param n byte value
   * @return unit
   */
  public static String getByteUnit(double n)
  {
    if      (n >= 1024*1024*1024) return "GBytes";
    else if (n >=      1024*1024) return "MBytes";
    else if (n >=           1024) return "KBytes";
    else                          return "bytes";
  }

  /** get byte size short unit
   * @param n byte value
   * @return unit
   */
  public static String getByteShortUnit(double n)
  {
    if      (n >= 1024*1024*1024) return "G";
    else if (n >=      1024*1024) return "M";
    else if (n >=           1024) return "K";
    else                          return "";
  }

  /** parse byte size string
   * @param string string to parse (<n>.<n>(%|B|M|MB|G|GB)
   * @return byte value
   */
  public static long parseByteSize(String string)
    throws NumberFormatException
  {
    string = string.toUpperCase();

    // try to parse with default locale
    if      (string.endsWith("GB"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-2))*1024*1024*1024);
    }
    else if (string.endsWith("G"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-1))*1024*1024*1024);
    }
    else if (string.endsWith("MB"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-2))*1024*1024);
    }
    else if (string.endsWith("M"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-1))*1024*1024);
    }
    else if (string.endsWith("KB"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-2))*1024);
    }
    else if (string.endsWith("K"))
    {
      return (long)(Double.parseDouble(string.substring(0,string.length()-1))*1024);
    }
    else if (string.endsWith("B"))
    {
      return (long)Double.parseDouble(string.substring(0,string.length()-1));
    }
    else
    {
      return (long)Double.parseDouble(string);
    }
  }

  /** parse byte size string
   * @param string string to parse (<n>(%|B|M|MB|G|GB)
   * @param defaultValue default value if number cannot be parsed
   * @return byte value
   */
  public static long parseByteSize(String string, long defaultValue)
  {
    long n;

    try
    {
      n = Units.parseByteSize(string);
    }
    catch (NumberFormatException exception)
    {
      n = defaultValue;
    }

    return n;
  }

  /** format byte size
   * @param n byte value
   * @return string with unit
   */
  public static String formatByteSize(long n)
  {
    return getByteSize(n)+getByteShortUnit(n);
  }
}

/** Onzen
 */
public class Onzen
{
  /** status text
   */
  class StatusText
  {
    final Thread thread;
    final String text;

    /** create status text
     * @param format format
     * @param arguments optional arguments
     */
    StatusText(String format, Object... arguments)
    {
      this.thread = Thread.currentThread();
      this.text   = String.format(format,arguments);
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return text;
    }
  };

  /** dialog data
   */
  class Data
  {
    boolean       mouseDownFlag;
    boolean       dragTab;
    int           dragTabStartX;
    RepositoryTab dragTabRepositoryTab;
    TabItem[]     dragTabItems;
    TabItem       dragTabItem;
    boolean       dragTabDrawMarker;
    Point         dragTabMarker;
    int           dragTabMarkerHeight;

    Data()
    {
      this.dragTab              = false;
      this.dragTabStartX        = 0;
      this.dragTabRepositoryTab = null;
      this.dragTabItems         = null;
      this.dragTabItem          = null;
      this.dragTabDrawMarker    = false;
      this.dragTabMarker        = new Point(0,0);
      this.dragTabMarkerHeight  = 0;
    }
  };

  // --------------------------- constants --------------------------------

  // exit codes
  public static int              EXITCODE_OK             =   0;
  public static int              EXITCODE_INTERNAL_ERROR = 127;

  // colors
  public static Color            COLOR_BLACK;
  public static Color            COLOR_WHITE;
  public static Color            COLOR_GREEN;
  public static Color            COLOR_DARK_RED;
  public static Color            COLOR_RED;
  public static Color            COLOR_DARK_BLUE;
  public static Color            COLOR_BLUE;
  public static Color            COLOR_DARK_YELLOW;
  public static Color            COLOR_YELLOW;
  public static Color            COLOR_DARK_GRAY;
  public static Color            COLOR_GRAY;
  public static Color            COLOR_MAGENTA;
  public static Color            COLOR_BACKGROUND;

  // images
  public static Image            IMAGE_DIRECTORY;
  public static Image            IMAGE_FILE;
  public static Image            IMAGE_LINK;
  public static Image            IMAGE_ARRAY_UP;
  public static Image            IMAGE_ARRAY_DOWN;
  public static Image            IMAGE_ARROW_LEFT;
  public static Image            IMAGE_ARROW_RIGHT;

  // fonts
  public static Font             FONT_DIFF;
  public static Font             FONT_DIFF_LINE;

  // cursors
  public static Cursor           CURSOR_WAIT;

  // date/time format
  public static SimpleDateFormat DATE_FORMAT     = new SimpleDateFormat(Settings.dateFormat);
  public static SimpleDateFormat TIME_FORMAT     = new SimpleDateFormat(Settings.timeFormat);
  public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat(Settings.dateTimeFormat);


  // command line options
  private static final Option[] options =
  {
    new Option("--help",              "-h",Options.Types.BOOLEAN,    "helpFlag"),

    new Option("--debug",             null,Options.Types.BOOLEAN,    "debugFlag"),

    new Option("--cvs-prune-empty-directories",null,Options.Types.BOOLEAN,"cvsPruneEmtpyDirectories"),

    // ignored
    new Option("--swing",             null, Options.Types.BOOLEAN,   null),
  };

  // --------------------------- variables --------------------------------
  private Display                   display;
  private Shell                     shell;

  private final Data                data = new Data();
  private String                    repositoryListName = null;
  private RepositoryList            repositoryList;
  private Composite                 repositoryTabEmpty = null;
  private LinkedList<RepositoryTab> repositoryTabList = new LinkedList<RepositoryTab>();
  private RepositoryTab             selectedRepositoryTab = null;
  private LinkedList<StatusText>    statusTextList = new LinkedList<StatusText>();

  private MenuItem                  menuItemApplyPatches;
  private MenuItem                  menuItemUnapplyPatches;
  private MenuItem                  menuItemPullChanges;
  private MenuItem                  menuItemPushChanges;
  private MenuItem                  menuSetFileMode;

  private Menu                      menuRepositories;

  private TabFolder                 widgetTabFolder;

  private Button                    widgetButtonUpdate;
  private Button                    widgetButtonCommit;
  private Button                    widgetButtonPatch;
  private Button                    widgetButtonAdd;
  private Button                    widgetButtonRemove;
  private Button                    widgetButtonRevert;
  private Button                    widgetButtonDiff;
  private Button                    widgetButtonRevisions;
  private Button                    widgetButtonSolve;

  private Label                     widgetStatus;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** print error to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printError(String format, Object... args)
  {
    System.err.println("ERROR: "+String.format(format,args));
  }

  /** print internal error to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printInternalError(String format, Object... args)
  {
    System.err.println("INTERNAL ERROR: "+String.format(format,args));
  }

  /** print internal error to stderr
   * @param throwable throwable
   * @param args optional arguments
   */
  public static void printInternalError(Throwable throwable, Object... args)
  {
    printInternalError(throwable.toString());
    if (Settings.debugFlag)
    {
      for (StackTraceElement stackTraceElement : throwable.getStackTrace())
      {
        System.err.println("  "+stackTraceElement);
      }
    }
  }

  /** print warning to stderr
   * @param format format string
   * @param args optional arguments
   */
  public static void printWarning(String format, Object... args)
  {
    System.err.print("Warning: "+String.format(format,args));
    if (Settings.debugFlag)
    {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      System.err.print(" at "+stackTrace[2].getFileName()+", "+stackTrace[2].getLineNumber());
    }
    System.err.println();
  }

  /** main
   * @param args command line arguments
   */
  public static void main(String[] args)
  {
    new Onzen(args);
  }

  /** onzen main
   * @param args command line arguments
   */
  Onzen(String[] args)
  {
    int exitcode = 255;
    try
    {
      // load settings
      Settings.load();

      // parse arguments
      parseArguments(args);

      // init
      initAll();

      // load repository list
      loadRepositoryList(repositoryListName);
/*
Repository repositoryX = new RepositoryCVS("/home/torsten/projects/onzen");
repositoryX.title = "Ooooonzen";
repositoryList.add(repositoryX);
RepositoryTab repositoryTab = new RepositoryTab(widgetTabFolder,repositoryX);
/**/

      // run
      exitcode = run();

      // save repository list
      if (repositoryList != null)
      {
        try
        {
          repositoryList.save();
        }
        catch (IOException exception)
        {
          Dialogs.error(shell,"Cannot store repository list (error: %s)",exception.getMessage());
        }
      }

      // done
      doneAll();

      // save settings
      Settings.save();
    }
    catch (org.eclipse.swt.SWTException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      System.err.println("ERROR graphics: "+exception.getCause());
      if (Settings.debugFlag)
      {
        for (StackTraceElement stackTraceElement : exception.getStackTrace())
        {
          System.err.println("  "+stackTraceElement);
        }
      }
    }
    catch (AssertionError assertionError)
    {
      printInternalError(assertionError);
      System.err.println("Please report this assertion error to torsten.rupp@gmx.net.");
    }
    catch (InternalError error)
    {
      printInternalError(error);
      System.err.println("Please report this internal error to torsten.rupp@gmx.net.");
    }
    catch (Error error)
    {
      printInternalError(error);
      System.err.println("Please report this error to torsten.rupp@gmx.net.");
    }

    System.exit(exitcode);
  }

  /** set status text
   * @param format format string
   * @param arguments optional arguments
   */
  public void setStatusText(final String format, final Object... arguments)
  {
    // create status text
    final StatusText statusText = new StatusText(format,arguments);

    // add to list
    synchronized(statusTextList)
    {
      statusTextList.add(statusText);
    }

    // show
    display.syncExec(new Runnable()
    {
      public void run()
      {
        widgetStatus.setText(statusText.text);
      }
    });
  }

  /** clear status text
   */
  public void clearStatusText()
  {
    // remove last status text of current thread
    synchronized(statusTextList)
    {
      // remove from status text list
      Iterator<StatusText> iterator = statusTextList.descendingIterator();
      while (iterator.hasNext())
      {
        StatusText statusText = iterator.next();
        if (statusText.thread == Thread.currentThread())
        {
          iterator.remove();
          break;
        }
      }
    }

    // get last text or ""
    final String string;
    synchronized(statusTextList)
    {
      string = (statusTextList.peekLast() != null) ? statusTextList.peekLast().text : "";
    }

    // show
    display.syncExec(new Runnable()
    {
      public void run()
      {
        widgetStatus.setText(string);
      }
    });
  }

  //-----------------------------------------------------------------------

  /** print program usage
   */
  private void printUsage()
  {
    System.out.println("onzen usage: <options> [--] [<repository list name>]");
    System.out.println("");
    System.out.println("Options: ");
    System.out.println("");
    System.out.println("         -h|--help                      - print this help");
    System.out.println("         --debug                        - enable debug mode");
  }

  /** parse arguments
   * @param args arguments
   */
  private void parseArguments(String[] args)
  {
    // parse arguments
    int z = 0;
    boolean endOfOptions = false;
    while (z < args.length)
    {
      if      (!endOfOptions && args[z].equals("--"))
      {
        endOfOptions = true;
        z++;
      }
      else if (!endOfOptions && (args[z].startsWith("--") || args[z].startsWith("-")))
      {
        int i = Options.parse(options,args,z,Settings.class);
        if (i < 0)
        {
          throw new Error("Unknown option '"+args[z]+"'!");
        }
        z = i;
      }
      else
      {
        repositoryListName = args[z];
        z++;
      }
    }

    // help
    if (Settings.helpFlag)
    {
      printUsage();
      System.exit(EXITCODE_OK);
    }

    // check arguments
  }

  /** init display variables
   */
  private void initDisplay()
  {
    display = new Display();

    // get colors
    COLOR_BLACK       = display.getSystemColor(SWT.COLOR_BLACK);
    COLOR_WHITE       = display.getSystemColor(SWT.COLOR_WHITE);
    COLOR_GREEN       = display.getSystemColor(SWT.COLOR_GREEN);
    COLOR_DARK_RED    = display.getSystemColor(SWT.COLOR_DARK_RED);
    COLOR_RED         = display.getSystemColor(SWT.COLOR_RED);
    COLOR_DARK_BLUE   = display.getSystemColor(SWT.COLOR_DARK_BLUE);
    COLOR_BLUE        = display.getSystemColor(SWT.COLOR_BLUE);
    COLOR_DARK_YELLOW = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
    COLOR_YELLOW      = display.getSystemColor(SWT.COLOR_YELLOW);    
    COLOR_DARK_GRAY   = display.getSystemColor(SWT.COLOR_DARK_GRAY);
    COLOR_GRAY        = display.getSystemColor(SWT.COLOR_GRAY);
    COLOR_MAGENTA     = new Color(null,0xFF,0xA0,0xA0);                         
    COLOR_BACKGROUND  = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

    // get images
    IMAGE_DIRECTORY   = Widgets.loadImage(display,"directory.png");
    IMAGE_FILE        = Widgets.loadImage(display,"file.png");
    IMAGE_LINK        = Widgets.loadImage(display,"link.png");
    IMAGE_ARRAY_UP    = Widgets.loadImage(display,"arrow-up.png");
    IMAGE_ARRAY_DOWN  = Widgets.loadImage(display,"arrow-down.png");
    IMAGE_ARROW_LEFT  = Widgets.loadImage(display,"arrow-left.png");
    IMAGE_ARROW_RIGHT = Widgets.loadImage(display,"arrow-right.png");

    // fonts
    FONT_DIFF         = Widgets.newFont(display,Settings.fontDiff);
    FONT_DIFF_LINE    = Widgets.newFont(display,Settings.fontDiffLine);

    // get cursors
    CURSOR_WAIT       = new Cursor(display,SWT.CURSOR_WAIT);
  }

  /** create main window
   */
  private void createWindow()
  {
    Composite composite;
    Button    button;
    Label     label;

    // create window
    shell = new Shell(display);
    shell.setText("Onzen");
    shell.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));

    // create tab
    widgetTabFolder = Widgets.newTabFolder(shell);
    Widgets.layout(widgetTabFolder,0,0,TableLayoutData.NSWE);
    widgetTabFolder.addPaintListener(new PaintListener()
    {
      public void paintControl(PaintEvent paintEvent)
      {
        if (data.dragTab && data.dragTabDrawMarker)
        {
          int[] marker = new int[2*7];
          marker[0*2+0] = data.dragTabMarker.x+2; marker[0*2+1] = data.dragTabMarker.y+0;
          marker[1*2+0] = data.dragTabMarker.x+2; marker[1*2+1] = data.dragTabMarker.y+0+data.dragTabMarkerHeight-5;
          marker[2*2+0] = data.dragTabMarker.x+5; marker[2*2+1] = data.dragTabMarker.y+0+data.dragTabMarkerHeight-5;
          marker[3*2+0] = data.dragTabMarker.x+0; marker[3*2+1] = data.dragTabMarker.y+5+data.dragTabMarkerHeight;
          marker[4*2+0] = data.dragTabMarker.x-5; marker[4*2+1] = data.dragTabMarker.y+0+data.dragTabMarkerHeight-5;
          marker[5*2+0] = data.dragTabMarker.x-2; marker[5*2+1] = data.dragTabMarker.y+0+data.dragTabMarkerHeight-5;
          marker[6*2+0] = data.dragTabMarker.x-2; marker[6*2+1] = data.dragTabMarker.y+0;
          paintEvent.gc.setAntialias(SWT.OFF);
          paintEvent.gc.setBackground(Onzen.COLOR_BLUE);
          paintEvent.gc.setForeground(Onzen.COLOR_MAGENTA);
          paintEvent.gc.fillPolygon(marker);
          paintEvent.gc.drawPolygon(marker);
          paintEvent.gc.setAntialias(SWT.DEFAULT);
        }
      }
    });
    widgetTabFolder.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        assert selectedRepositoryTab != null;

        editRepository(selectedRepositoryTab);
      }

      public void mouseDown(final MouseEvent mouseEvent)
      {
        assert selectedRepositoryTab != null;

        TabFolder tabFolder = (TabFolder)mouseEvent.widget;
        TabItem   tabItem = tabFolder.getItem(new Point(mouseEvent.x,mouseEvent.y));

        if (tabItem != null)
        {
          // start dragging tab
          data.dragTab              = true;
          data.dragTabStartX        = mouseEvent.x;
          data.dragTabRepositoryTab = (RepositoryTab)tabItem.getData();
          data.dragTabItems         = tabFolder.getItems();
          data.dragTabItem          = tabItem;
        }
      }

      public void mouseUp(final MouseEvent mouseEvent)
      {
        assert selectedRepositoryTab != null;

        if (data.dragTab)
        {
          TabFolder tabFolder = (TabFolder)mouseEvent.widget;
          TabItem   tabItem   = tabFolder.getItem(new Point(mouseEvent.x,mouseEvent.y));

          if (   (tabItem != null)
              && (data.dragTabItem != null)
              && (tabItem != data.dragTabItem)
              && (Math.abs(mouseEvent.x-data.dragTabStartX) > 20)
             )
          {
            // get new tab index
            int newTabIndex = tabFolder.indexOf(tabItem);
            assert newTabIndex != -1;

            // re-order tabs
            Widgets.moveTab(widgetTabFolder,data.dragTabItem,newTabIndex);
            repositoryList.move(data.dragTabRepositoryTab.repository,newTabIndex);

            // set selected repository
            selectRepository(data.dragTabRepositoryTab);
          }

          // stop dragging tab
          data.dragTab       = false;
          data.dragTabItems  = null;
          data.dragTabItem   = null;
          widgetTabFolder.redraw();
        }
      }
    });
    widgetTabFolder.addMouseMoveListener(new MouseMoveListener()
    {
      public void mouseMove(MouseEvent mouseEvent)
      {
        if (data.dragTab)
        {
          TabFolder tabFolder = (TabFolder)mouseEvent.widget;
          TabItem   tabItem   = tabFolder.getItem(new Point(mouseEvent.x,mouseEvent.y));

          // check if dragging (mouse is over another tab than drag tab and moved some way)
          data.dragTabDrawMarker =    (tabItem != null)
                                   && (data.dragTabItem != null)
                                   && (tabItem != data.dragTabItem)
                                   && (Math.abs(mouseEvent.x-data.dragTabStartX) > 20);

          // get marker drawing position and height
          if (data.dragTabDrawMarker)
          {
            // get new tab index
            int newTabIndex = tabFolder.indexOf(tabItem);
            assert newTabIndex != -1;

            Rectangle bounds = data.dragTabItems[newTabIndex].getBounds();
            if (mouseEvent.x > data.dragTabStartX)
            {
              // right
              if (newTabIndex < data.dragTabItems.length-1)
              {
                Rectangle boundsNext = data.dragTabItems[newTabIndex+1].getBounds();
                data.dragTabMarker.x = (bounds.x+bounds.width+boundsNext.x)/2;
              }
              else
              {
                data.dragTabMarker.x = bounds.x+bounds.width+4;
              }
            }
            else
            {
              // left
              if (newTabIndex > 0)
              {
                Rectangle boundsPrev = data.dragTabItems[newTabIndex-1].getBounds();
                data.dragTabMarker.x = (boundsPrev.x+boundsPrev.width+bounds.x)/2;
              }
              else
              {
                data.dragTabMarker.x = bounds.x-4;
              }
            }
            data.dragTabMarker.y     = 2;
            data.dragTabMarkerHeight = bounds.height;
          }

          // redraw
          widgetTabFolder.redraw();
        }
      }
    });
    widgetTabFolder.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        TabFolder tabFolder = (TabFolder)selectionEvent.widget;
        TabItem   tabItem   = (TabItem)selectionEvent.item;

        selectRepository((RepositoryTab)tabItem.getData());
      }
    });

    // create buttons
    composite = Widgets.newComposite(shell);
    composite.setLayout(new TableLayout(0.0,1.0,2));
    Widgets.layout(composite,1,0,TableLayoutData.WE);
    {
      widgetButtonUpdate = Widgets.newButton(composite,"Update");
      Widgets.layout(widgetButtonUpdate,0,0,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonUpdate.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.update();
          }
        }
      });

      widgetButtonCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetButtonCommit,0,1,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.commit();
          }
        }
      });

      widgetButtonPatch = Widgets.newButton(composite,"Create patch");
      Widgets.layout(widgetButtonPatch,0,2,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonPatch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.createPatch();
          }
        }
      });

      widgetButtonPatch = Widgets.newButton(composite,"Patches");
      Widgets.layout(widgetButtonPatch,0,2,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonPatch.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.patches();
          }
        }
      });

      widgetButtonAdd = Widgets.newButton(composite,"Add");
      Widgets.layout(widgetButtonAdd,0,3,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.add();
          }
        }
      });

      widgetButtonRemove = Widgets.newButton(composite,"Remove");
      Widgets.layout(widgetButtonRemove,0,4,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonRemove.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.remove();
          }
        }
      });

      widgetButtonRevert = Widgets.newButton(composite,"Revert");
      Widgets.layout(widgetButtonRevert,0,5,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonRevert.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.revert();
          }
        }
      });

      widgetButtonDiff = Widgets.newButton(composite,"Diff");
      Widgets.layout(widgetButtonDiff,0,6,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonDiff.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.diff();
          }
        }
      });

      widgetButtonRevisions = Widgets.newButton(composite,"Revisions");
      Widgets.layout(widgetButtonRevisions,0,7,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT,SWT.DEFAULT,SWT.DEFAULT);
      widgetButtonRevisions.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.revisions();
          }
        }
      });

      widgetButtonSolve = Widgets.newButton(composite,"Solve");
widgetButtonSolve.setEnabled(false);
      Widgets.layout(widgetButtonSolve,0,8,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonSolve.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
//          Dialogs.close(dialog,false);
        }
      });
    }

    // create status line
    composite = Widgets.newComposite(shell);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,1.0},2));
    Widgets.layout(composite,2,0,TableLayoutData.WE);
    {
      label = Widgets.newLabel(composite,"Status:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetStatus = Widgets.newView(composite,"",SWT.NONE);
      Widgets.layout(widgetStatus,0,1,TableLayoutData.WE);
    }

    // window listener
    display.addFilter(SWT.KeyDown,new Listener()
    {
      public void handleEvent(Event event)
      {
//        if (event.stateMaks & SWT.CTRL)
        switch (event.keyCode)
        {
          case SWT.F1:
//            Widgets.showTab(widgetTabFolder,tabStatus.widgetTab);
            event.doit = false;
            break;
          case SWT.F2:
//            Widgets.showTab(widgetTabFolder,tabJobs.widgetTab);
            event.doit = false;
            break;
          case SWT.F3:
//            Widgets.showTab(widgetTabFolder,tabRestore.widgetTab);
            event.doit = false;
            break;
          case SWT.F5:
            if (selectedRepositoryTab != null)
            {
              selectedRepositoryTab.updateStates();
            }
            event.doit = false;
            break;
          default:
            break;
        }
      }
    });
  }

  /** create menu
   */
  private void createMenu()
  {
    Menu     menuBar;
    Menu     menu,subMenu;
    MenuItem menuItem;

    // create menu
    menuBar = Widgets.newMenuBar(shell);

    menu = Widgets.addMenu(menuBar,"Program");
    {
      menuItem = Widgets.addMenuItem(menu,"New repository...");
menuItem.setEnabled(false);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
Dprintf.dprintf("");
//          newRepository(rootPath);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Open repository...",Settings.keyOpenRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String rootPath = Dialogs.directory(shell,"Open repository","");
          if (rootPath != null)
          {
            openRepository(rootPath);
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Edit repository...",Settings.keyEditRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editRepository(selectedRepositoryTab);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Close repository...",Settings.keyCloseRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;

          closeRepository();
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"New repository list...");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          newRepositoryList();
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Load repository list...");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          loadRepositoryList();
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Quit",SWT.CTRL+'Q');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close-evemnt to shell
          Widgets.notify(shell,SWT.Close,0);
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"Command");
    {
      menuItem = Widgets.addMenuItem(menu,"Status",Settings.keyStatus);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.updateStates();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Update",Settings.keyUpdate);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonUpdate);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Commit...",Settings.keyCommit);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonCommit);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Create patch...",Settings.keyCreatePatch);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.createPatch();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Patches...",Settings.keyPatches);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.patches();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Revert...",Settings.keyRevert);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonRevert);
        }
      });

      menuItemApplyPatches = Widgets.addMenuItem(menu,"Apply patches",Settings.keyApplyPatches);
      menuItemApplyPatches.setEnabled(false);
      menuItemApplyPatches.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.applyPatches();
          }
        }
      });

      menuItemUnapplyPatches = Widgets.addMenuItem(menu,"Unapply patches",Settings.keyUnapplyPatches);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemUnapplyPatches.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.unapplyPatches();
          }
        }
      });

      menuItemPullChanges = Widgets.addMenuItem(menu,"Pull changes",Settings.keyPullChanges);
      menuItemPullChanges.setEnabled(false);
      menuItemPullChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.pullChanges();
          }
        }
      });

      menuItemPushChanges = Widgets.addMenuItem(menu,"Push changes",Settings.keyPushChanges);
      menuItemPushChanges.setEnabled(false);
      menuItemPushChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.pushChanges();
          }
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Add...",Settings.keyAdd);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonAdd);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Remove...",Settings.keyRemove);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonRemove);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Rename...",Settings.keyRename);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.rename();
          }
        }
      });

      menuSetFileMode = Widgets.addMenuItem(menu,"Set file mode...",Settings.keySetFileMode);
      menuSetFileMode.setEnabled(false);
      menuSetFileMode.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.setFileMode();
          }
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"View");
    {
      menuItem = Widgets.addMenuItem(menu,"Revision info...",Settings.keyRevisionInfo);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.revisionInfo();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Revisions...",Settings.keyRevisions);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonRevisions);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Diff...",Settings.keyDiff);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonDiff);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Changed files...",Settings.keyChangedFiles);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.changedFiles();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Annotations...",Settings.keyAnnotations);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.annotations();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"View/Solve conflict...",Settings.keySolve);
menuItem.setEnabled(false);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Widgets.invoke(widgetButtonSolve);
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"File/Directory");
    {
      menuItem = Widgets.addMenuItem(menu,"Open file with...",Settings.keyOpenFileWith);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
Dprintf.dprintf("");
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.openFileWith();
          }
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"New file...",Settings.keyNewFile);
menuItem.setEnabled(false);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
Dprintf.dprintf("");
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.newFile();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"New directory...",Settings.keyNewDirectory);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.newDirectory();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Rename local file/directory...",Settings.keyRenameLocal);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.renameLocalFile();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Delete local files/directories...",Settings.keyDeleteLocal);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.deleteLocalFiles();
          }
        }
      });
    }

    menuRepositories = Widgets.addMenu(menuBar,"Repositories");
    {
    }

    menu = Widgets.addMenu(menuBar,"Options");
    {
    }

    menu = Widgets.addMenu(menuBar,"Help");
    {
      menuItem = Widgets.addMenuItem(menu,"About");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.info(shell,"About","Onzen "+Config.VERSION_MAJOR+"."+Config.VERSION_MINOR+".\n\nWritten by Torsten Rupp.");
        }
      });
    }

    if (Settings.debugFlag)
    {
      menu = Widgets.addMenu(menuBar,"Debug");
      {
        menuItem = Widgets.addMenuItem(menu,"Restart");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            // send close event with restart
            Widgets.notify(shell,SWT.Close,64);
          }
        });

/*
menuItem = Widgets.addMenuItem(menu,"XXXXX");
menuItem.addSelectionListener(new SelectionListener()
{
  public void widgetDefaultSelected(SelectionEvent selectionEvent)
  {
  }
  public void widgetSelected(SelectionEvent selectionEvent)
  {
  }
});
*/
      }
    }
  }

  /** create event handlers
   */
  private void createEventHandlers()
  {
  }

  /** init all
   */
  private void initAll()
  {
    // load message history, start message broadcast
    Message.loadHistory();
    Message.startBroadcast();
/*
new Message("Hello 1").addToHistory();
new Message("Tral\nlala").addToHistory();
new Message("Und nun?").addToHistory();
*/

    // init display
    initDisplay();

    // open main window
    createWindow();
    createMenu();
    createEventHandlers();

    // add empty repository tab
    addRepositoryTabEmpty();
  }

  /** done all
   */
  private void doneAll()
  {
    // shutdown running background tasks
    Background.executorService.shutdownNow();
  }

  /** run application
   * @return exit code
   */
  private int run()
  {
    final int[] result = new int[1];

    // set window size, manage window
    shell.setSize(Settings.geometryMain.x,Settings.geometryMain.y);
    shell.open();

    // listener
    shell.addListener(SWT.Resize,new Listener()
    {
      public void handleEvent(Event event)
      {
        Settings.geometryMain = shell.getSize();
      }
    });
    shell.addListener(SWT.Close,new Listener()
    {
      public void handleEvent(Event event)
      {
        // store exitcode
        result[0] = event.index;

        // close
        shell.dispose();
      }
    });

    // SWT event loop
    while (!shell.isDisposed())
    {
//System.err.print(".");
      if (!display.readAndDispatch())
      {
        display.sleep();
      }
    }

    return result[0];
  }

  /** add empty "New" tab repository
   */
  private void addRepositoryTabEmpty()
  {
    // create empty tab (workaround for a problem when creating first tab which is not shown - bug in SWT?)
    if (repositoryTabEmpty != null) Widgets.removeTab(widgetTabFolder,repositoryTabEmpty);
    repositoryTabEmpty = Widgets.addTab(widgetTabFolder,"New",null);
    repositoryTabEmpty.setLayout(new TableLayout(1.0,1.0,2));
    Widgets.layout(repositoryTabEmpty,0,0,TableLayoutData.NSWE);
    {
      Label label = Widgets.newLabel(repositoryTabEmpty,"Open a repository list or add a repository from the menu.");
      Widgets.layout(label,0,0,TableLayoutData.NONE);
    }
  }

  /** remove empty "New" tab repository
   */
  private void removeRepositoryTabEmpty()
  {
    if (repositoryTabEmpty != null)
    {
      Widgets.removeTab(widgetTabFolder,repositoryTabEmpty);
      repositoryTabEmpty = null;
    }
  }

  /** clear repository tab list
   */
  private void clearRepositories()
  {
    // deselect repository
    if (selectedRepositoryTab != null) selectedRepositoryTab.repository.selected = false;
    selectedRepositoryTab = null;

    // remove old tabs (Note: empty tab is workaround for a problem when creating first tab which is not shown - bug in SWT?)
    for (RepositoryTab repositoryTab : repositoryTabList)
    {
      repositoryTab.close();
    }
    repositoryTabList.clear();
    addRepositoryTabEmpty();

    // remove entries in repository menu
    MenuItem[] menuItems = menuRepositories.getItems();
    for (MenuItem menuItem : menuRepositories.getItems())
    {
      menuItem.dispose();
    }
  }

  /** set repository tab list
   * @param repositoryList repository list to set
   */
  private void setRepositoryList(RepositoryList repositoryList)
  {
    RepositoryTab newSelectedRepositoryTab = null;

    // clear repositories
    clearRepositories();

    // set repository list
    this.repositoryList = repositoryList;
    for (Repository repository : repositoryList)
    {
      // add repository tab
      RepositoryTab repositoryTab = new RepositoryTab(this,widgetTabFolder,repository);
      repositoryTabList.add(repositoryTab);

      // save select repository
      if (repository.selected)
      {
        newSelectedRepositoryTab = repositoryTab;
      }

      // open sub-directories in repository tab, remove unknown sub-directories
      for (String directory : repository.getOpenDirectories())
      {
        if (!repositoryTab.openDirectory(directory))
        {
          repository.closeDirectory(directory);
        }
      }
    }

    // remove empty tab if repository list is not empty
    if (repositoryTabList.size() > 0)
    {
      removeRepositoryTabEmpty();
    }

    // select new tab
    if (newSelectedRepositoryTab != null)
    {
      selectRepository(newSelectedRepositoryTab);
    }

    // set repository menu
    for (RepositoryTab repositoryTab : repositoryTabList)
    {
      MenuItem menuItem = Widgets.addMenuItem(menuRepositories,repositoryTab.repository.title);
      menuItem.setData(repositoryTab);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;

          selectRepository((RepositoryTab)widget.getData());
        }
      });
    }
  }

  /** add a repository tab to the repository tab list
   * @param repository repository to add
   * @return added repository tab
   */
  private RepositoryTab addRepositoryTab(Repository repository)
  {
    RepositoryTab repositoryTab = null;

    // add to list
    repositoryList.add(repository);

    // add tab, set default selected tab
    repositoryTab = new RepositoryTab(this,widgetTabFolder,repository);
    repositoryTabList.add(repositoryTab);
    if (repositoryTabList.size() == 1)
    {
      // select repository tab
      selectRepository(repositoryTab);
    }

    // remove empty tab, set default selected tab
    removeRepositoryTabEmpty();

    // save list
    try
    {
      repositoryList.save();
    }
    catch (IOException exception)
    {
      Dialogs.error(shell,"Cannot store repository list (error: %s)",exception.getMessage());
    }

    return repositoryTab;
  }

  /** remove a repository tab fro9m the repository tab list
   * @param repositoryTab repository tab to remove
   */
  private void removeRepositoryTab(RepositoryTab repositoryTab)
  {
    // remove from list
    repositoryList.remove(repositoryTab.repository);

    // add empty tab if list will become empty
    if (repositoryTabList.size() <= 1)
    {
      addRepositoryTabEmpty();
    }

    // close tab, remove from repository list
    repositoryTab.close();
    repositoryTabList.remove(repositoryTab);

    // save list
    try
    {
      repositoryList.save();
    }
    catch (IOException exception)
    {
      Dialogs.error(shell,"Cannot store repository list (error: %s)",exception.getMessage());
    }
  }

  private void newRepositoryList()
  {
    /** dialog data
     */
    class Data
    {
      String[] names;
      String   newName;

      Data()
      {
        this.newName = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite;
    Label       label;
    Button      button;

    // get names
    data.names = RepositoryList.listNames();

    // name dialog
    dialog = Dialogs.open(shell,"New repository list",300,300,new double[]{1.0,0.0},1.0);

    final List   widgetNames;
    final Text   widgetNewName;
    final Button widgetNew;
    final Button widgetDelete;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      widgetNames = Widgets.newList(composite);
      widgetNames.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetNames,0,0,TableLayoutData.NSWE);
      widgetNames.setToolTipText("Repository list names.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"New:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetNewName = Widgets.newText(subComposite);
        Widgets.layout(widgetNewName,0,1,TableLayoutData.WE);
        widgetNewName.setToolTipText("Name of repository list to create.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetNew = Widgets.newButton(composite,"New");
      widgetNew.setEnabled(false);
      Widgets.layout(widgetNew,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetNew.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          data.newName = widgetNewName.getText().trim();

          Dialogs.close(dialog,true);
        }
      });

      widgetDelete = Widgets.newButton(composite,"Delete");
      widgetDelete.setEnabled(false);
      Widgets.layout(widgetDelete,0,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetDelete.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          int index = widgetNames.getSelectionIndex();
          if ((index >= 0) && (index < data.names.length))
          {
            if (Dialogs.confirm(dialog,String.format("Really delete repository list '%s'?",data.names[index])))
            {
              // delete repository list
              RepositoryList.delete(data.names[index]);

              // remove name from array
              String[] newNames = new String[data.names.length-1];
              System.arraycopy(data.names,0,newNames,0,index);
              System.arraycopy(data.names,index+1,newNames,0,data.names.length-1-index);
              data.names = newNames;

              // update widgets
              widgetNames.remove(index);
              widgetDelete.setEnabled(false);
            }
          }
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
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
    widgetNames.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if ((index >= 0) && (index < data.names.length))
        {
          widgetNewName.setText(data.names[index]);
          widgetNewName.setFocus();
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        widgetNew.setEnabled((index >= 0) && (index < data.names.length));
        widgetDelete.setEnabled((index >= 0) && (index < data.names.length));
      }
    });
    widgetNewName.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        Text widget = (Text)keyEvent.widget;

        widgetNew.setEnabled(!widget.getText().isEmpty());
      }
    });

    // show dialog
    Dialogs.show(dialog);

    // add names
    for (String name : data.names)
    {
      widgetNames.add(name);
    }

    // run
    widgetNewName.setFocus();
    if ((Boolean)Dialogs.run(dialog,false) && !data.newName.isEmpty())
    {
      repositoryListName = data.newName;
      setRepositoryList(new RepositoryList(repositoryListName));
    }
  }

  /** load repository list from file
   * @param repositoryListName name of repository list
   */
  private void loadRepositoryList(String repositoryListName)
  {
    if (repositoryListName == null)
    {
      /** dialog data
       */
      class Data
      {
        String[] names;
        String   name;

        Data()
        {
          this.name = null;
        }
      };

      final Data  data = new Data();
      final Shell dialog;
      Composite   composite,subComposite;
      Label       label;
      Button      button;

      // get names
      data.names = RepositoryList.listNames();

      // name dialog
      dialog = Dialogs.open(shell,"Select repository list",300,300,new double[]{1.0,0.0},1.0);

      final List   widgetNames;
      final Text   widgetNewName;
      final Button widgetOpen;
      final Button widgetNew;
      final Button widgetDelete;
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,4));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
      {
        widgetNames = Widgets.newList(composite);
        widgetNames.setBackground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetNames,0,0,TableLayoutData.NSWE);
        widgetNames.setToolTipText("Repository list names.");

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"New:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetNewName = Widgets.newText(subComposite);
          Widgets.layout(widgetNewName,0,1,TableLayoutData.WE);
          widgetNewName.setToolTipText("New repository list name.");
        }
      }

      // buttons
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetOpen = Widgets.newButton(composite,"Open");
        widgetOpen.setEnabled(false);
        Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetOpen.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            int index = widgetNames.getSelectionIndex();
            if ((index >= 0) && (index < data.names.length))
            {
              data.name = data.names[index];

              Dialogs.close(dialog,true);
            }
          }
        });
        widgetOpen.setToolTipText("Open selected repository list.");

        widgetNew = Widgets.newButton(composite,"New");
        widgetNew.setEnabled(false);
        Widgets.layout(widgetNew,0,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetNew.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            data.name = widgetNewName.getText();

            Dialogs.close(dialog,true);
          }
        });

        widgetDelete = Widgets.newButton(composite,"Delete");
        widgetDelete.setEnabled(false);
        Widgets.layout(widgetDelete,0,2,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetDelete.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            int index = widgetNames.getSelectionIndex();
            if ((index >= 0) && (index < data.names.length))
            {
              if (Dialogs.confirm(dialog,String.format("Really delete repository list '%s'?",data.names[index])))
              {
                // delete repository list
                RepositoryList.delete(data.names[index]);

                // remove name from array
                String[] newNames = new String[data.names.length-1];
                System.arraycopy(data.names,0,newNames,0,index);
                System.arraycopy(data.names,index+1,newNames,0,data.names.length-1-index);
                data.names = newNames;

                // update widgets
                widgetNames.remove(index);
                widgetOpen.setEnabled(false);
                widgetDelete.setEnabled(false);
              }
            }
          }
        });

        button = Widgets.newButton(composite,"Cancel");
        Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
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
      widgetNames.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
          List widget = (List)selectionEvent.widget;

          int index = widget.getSelectionIndex();
          if ((index >= 0) && (index < data.names.length))
          {
            Widgets.invoke(widgetOpen);
          }
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          List widget = (List)selectionEvent.widget;

          int index = widget.getSelectionIndex();
          widgetOpen.setEnabled((index >= 0) && (index < data.names.length));
          widgetDelete.setEnabled((index >= 0) && (index < data.names.length));
        }
      });
      widgetNewName.addKeyListener(new KeyListener()
      {
        public void keyPressed(KeyEvent keyEvent)
        {
        }
        public void keyReleased(KeyEvent keyEvent)
        {
          Text widget = (Text)keyEvent.widget;

          widgetNew.setEnabled(!widget.getText().isEmpty());
        }
      });

      // show dialog
      Dialogs.show(dialog);

      // add names
      for (String name : data.names)
      {
        widgetNames.add(name);
      }

      // run
      widgetNames.setFocus();
      if ((Boolean)Dialogs.run(dialog,false))
      {
        repositoryListName = data.name;
      }
    }

    if (repositoryListName != null)
    {
      // add repositories from list
      setRepositoryList(new RepositoryList(repositoryListName));
    }
  }

  /** load repository list from file
   */
  private void loadRepositoryList()
  {
    loadRepositoryList(null);
  }

  /** open new repository tab
   * @param rootPath root path
   */
  private void openRepository(String rootPath)
  {
    // open repository
    Repository repository = null;
    switch (Repository.getType(rootPath))
    {
      case CVS:
        repository = new RepositoryCVS(rootPath);
        break;
      case SVN:
        repository = new RepositorySVN(rootPath);
        break;
      case HG:
        repository = new RepositoryHG(rootPath);
        break;
      case GIT:
        repository = new RepositoryGit(rootPath);
        break;
      default:
        break;
    }

    if (repository != null)
    {
      // add and select new repository
      selectRepository(addRepositoryTab(repository));
    }
    else
    {
// NYI: add git
      Dialogs.error(shell,"'%s'\n\ndoes not contain a known repository (CVS,SVN,HG)",rootPath);
      return;
    }
  }

  /** edit repository
   */
  private void editRepository(RepositoryTab repositoryTab)
  {
    if (selectedRepositoryTab != null)
    {
      /** dialog data
       */
      class Data
      {
        String title;
        String rootPath;
        String masterRepository;
        String patchMailTo;
        String patchMailCC;
        String patchMailSubject;
        String patchMailText;

        Data()
        {
          this.title            = null;
          this.rootPath         = null;
          this.masterRepository = null;
          this.patchMailTo      = null;
          this.patchMailCC      = null;
          this.patchMailSubject = null;
          this.patchMailSubject = null;
        }
      };

      final Data  data = new Data();
      final Shell dialog;
      Composite   composite,subComposite;
      Label       label;
      Button      button;

      // repository edit dialog
      dialog = Dialogs.open(shell,"Edit repository",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

      final Text   widgetTitle;
      final Text   widgetRootPath;
//      final Text   widgetMasterRepository;
      final Text   widgetPatchMailTo;
      final Text   widgetPatchMailCC;
      final Text   widgetPatchMailSubject;
      final Text   widgetPatchMailText;
      final Button widgetSave;
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(new double[]{0.0,1.0},1.0,4));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
      {
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,0,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Type:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          label = Widgets.newLabel(subComposite);
          label.setText(repositoryTab.repository.getType().toString());
          Widgets.layout(label,0,1,TableLayoutData.W);

          label = Widgets.newLabel(subComposite,"Title:");
          Widgets.layout(label,1,0,TableLayoutData.W);

          widgetTitle = Widgets.newText(subComposite);
          widgetTitle.setText(repositoryTab.repository.title);
          Widgets.layout(widgetTitle,1,1,TableLayoutData.WE);
          widgetTitle.setToolTipText("Repository title.");

          label = Widgets.newLabel(subComposite,"Root path:");
          Widgets.layout(label,2,0,TableLayoutData.W);

          widgetRootPath = Widgets.newText(subComposite);
          widgetRootPath.setText(repositoryTab.repository.rootPath);
          Widgets.layout(widgetRootPath,2,1,TableLayoutData.WE);
        }

        subComposite = Widgets.newGroup(composite,"Patch mail");
        subComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,1.0},new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subComposite,"To:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetPatchMailTo = Widgets.newText(subComposite);
          if (repositoryTab.repository.patchMailTo != null) widgetPatchMailTo.setText(repositoryTab.repository.patchMailTo);
          Widgets.layout(widgetPatchMailTo,0,1,TableLayoutData.WE);

          label = Widgets.newLabel(subComposite,"CC:");
          Widgets.layout(label,1,0,TableLayoutData.W);

          widgetPatchMailCC = Widgets.newText(subComposite);
          if (repositoryTab.repository.patchMailCC != null) widgetPatchMailCC.setText(repositoryTab.repository.patchMailCC);
          Widgets.layout(widgetPatchMailCC,1,1,TableLayoutData.WE);

          label = Widgets.newLabel(subComposite,"Subject:");
          Widgets.layout(label,2,0,TableLayoutData.W);

          widgetPatchMailSubject = Widgets.newText(subComposite);
          if (repositoryTab.repository.patchMailSubject != null) widgetPatchMailSubject.setText(repositoryTab.repository.patchMailSubject);
          Widgets.layout(widgetPatchMailSubject,2,1,TableLayoutData.WE);

          label = Widgets.newLabel(subComposite,"Text:");
          Widgets.layout(label,3,0,TableLayoutData.NW);

          widgetPatchMailText = Widgets.newText(subComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
          if (repositoryTab.repository.patchMailText != null) widgetPatchMailText.setText(repositoryTab.repository.patchMailText);
          Widgets.layout(widgetPatchMailText,3,1,TableLayoutData.NSWE);
          widgetPatchMailText.setToolTipText("Patch mail template.\n\nMacros:\n  %date% - date\n  %time% - time\n  %datetime% - date/time\n");
        }
      }

      // buttons
      composite = Widgets.newComposite(dialog);
      composite.setLayout(new TableLayout(0.0,1.0));
      Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
      {
        widgetSave = Widgets.newButton(composite,"Save");
        Widgets.layout(widgetSave,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
        widgetSave.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            data.title            = widgetTitle.getText();
            data.rootPath         = widgetRootPath.getText();
//            data.masterRepository = widgetMasterRepository.getText();
            data.patchMailTo      = widgetPatchMailTo.getText();
            data.patchMailCC      = widgetPatchMailCC.getText();
            data.patchMailSubject = widgetPatchMailSubject.getText();
            data.patchMailText    = widgetPatchMailText.getText();

            Dialogs.close(dialog,true);
          }
        });
        widgetSave.setToolTipText("Open selected repository list.");

        button = Widgets.newButton(composite,"Cancel");
        Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,70,SWT.DEFAULT);
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
      widgetTitle.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
          widgetRootPath.setFocus();
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
        }
      });
      widgetRootPath.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
          widgetSave.setFocus();
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
        }
      });

      // show dialog
      Dialogs.show(dialog);

      // set title
      widgetTitle.setText(selectedRepositoryTab.repository.title);

      // run
      Widgets.setFocus(widgetTitle);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        // set data
        repositoryTab.setTitle(data.title);
        repositoryTab.repository.rootPath         = data.rootPath;
        repositoryTab.repository.patchMailTo      = data.patchMailTo;
        repositoryTab.repository.patchMailCC      = data.patchMailCC;
        repositoryTab.repository.patchMailSubject = data.patchMailSubject;
        repositoryTab.repository.patchMailText    = data.patchMailText;

        // save list
        try
        {
          repositoryList.save();
        }
        catch (IOException exception)
        {
          Dialogs.error(shell,"Cannot store repository list (error: %s)",exception.getMessage());
        }
      }
    }
  }

  /** close selected repository tab
   */
  private void closeRepository()
  {
    if (selectedRepositoryTab != null)
    {
      if (Dialogs.confirm(shell,String.format("Really close repository '%s'?",selectedRepositoryTab.repository.title)))
      {
        // remove tab
        removeRepositoryTab(selectedRepositoryTab);

        // deselect repository
        selectedRepositoryTab.repository.selected = false;
        selectedRepositoryTab = null;

        // select new repository
        int index = widgetTabFolder.getSelectionIndex();
        if (index >= 0)
        {
          TabItem tabItem = widgetTabFolder.getItem(index);
          selectedRepositoryTab = (RepositoryTab)tabItem.getData();
          selectedRepositoryTab.repository.selected = true;
        }
      }
    }
  }

  /** select repository
   * @param repositoryTab repository tab to select
   */
  private void selectRepository(RepositoryTab repositoryTab)
  {
    // deselect previous repository
    if (selectedRepositoryTab != null) selectedRepositoryTab.repository.selected = false;

    // select new repository
    selectedRepositoryTab = repositoryTab;
    if (repositoryTab != null)
    {
      // select
      selectedRepositoryTab.repository.selected = true;

      // show tab
      selectedRepositoryTab.show();
      menuItemApplyPatches.setEnabled(selectedRepositoryTab.repository.supportPatchQueues());
      menuItemUnapplyPatches.setEnabled(selectedRepositoryTab.repository.supportPatchQueues());
      menuItemPullChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuItemPushChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuSetFileMode.setEnabled(selectedRepositoryTab.repository.supportSetFileMode());
    }
    else
    {
      menuItemApplyPatches.setEnabled(false);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemPullChanges.setEnabled(false);
      menuItemPushChanges.setEnabled(false);
      menuSetFileMode.setEnabled(false);
    }
  }
}

/* end of file */
