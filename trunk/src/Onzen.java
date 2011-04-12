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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

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
  public static Image            IMAGE_ARROW_UP;
  public static Image            IMAGE_ARROW_DOWN;
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
  private Display                           display;
  private Shell                             shell;

  private String                            repositoryListName = null;
  private RepositoryList                    repositoryList;
  private Composite                         repositoryTabEmpty = null;
  private HashMap<Repository,RepositoryTab> repositoryTabMap = new HashMap<Repository,RepositoryTab>();
  private RepositoryTab                     selectedRepositoryTab = null;
  private LinkedList<StatusText>            statusTextList = new LinkedList<StatusText>();
  private String                            masterPassword = null;

  private MenuItem                          menuItemApplyPatches;
  private MenuItem                          menuItemUnapplyPatches;
  private MenuItem                          menuItemPullChanges;
  private MenuItem                          menuItemPushChanges;
  private MenuItem                          menuSetFileMode;

  private Menu                              menuRepositories;

  private TabFolder                         widgetTabFolder;

  private Button                            widgetButtonUpdate;
  private Button                            widgetButtonCommit;
  private Button                            widgetButtonCreatePatch;
  private Button                            widgetButtonAdd;
  private Button                            widgetButtonRemove;
  private Button                            widgetButtonRevert;
  private Button                            widgetButtonDiff;
  private Button                            widgetButtonRevisions;
  private Button                            widgetButtonSolve;

  private Label                             widgetStatus;

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

/*
String p = "Hello";
String e = encodePassword(p);
String d = decodePassword(e);
Dprintf.dprintf("e=%s",e);
Dprintf.dprintf("d=%s",d);
*/

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
          printError("Cannot store repository list (error: %s)",exception.getMessage());
        }
      }

      // done
      doneAll();
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

  /** load and decode password with master password
   * @param password password to encode
   * @return encoded password or null
   */
  public String getPassword(String name)
  {
    String password = null;

    // get master password
    if (masterPassword == null)
    {
      masterPassword = Dialogs.password(shell,"Master password");
      if (masterPassword == null) return null;
    }

    // read password from password database
    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      database = openPasswordDatabase();

      statement = database.connection.createStatement();
      resultSet = null;
      try
      {
        preparedStatement = database.connection.prepareStatement("SELECT data FROM passwords WHERE name=?;");
        preparedStatement.setString(1,name);
        resultSet = preparedStatement.executeQuery();

        if (resultSet.next())
        {
          // read and decode password
          password = decodePassword(masterPassword,resultSet.getBytes("data"));
        }
        else
        {
          // input password
          password = Dialogs.password(shell,"Password for: "+name);
          if (password == null) return null;

          // encode and store password
          preparedStatement = database.connection.prepareStatement("INSERT INTO passwords (name,data) VALUES (?,?);");
          preparedStatement.setString(1,name);
          preparedStatement.setBytes(2,encodePassword(masterPassword,password));
          preparedStatement.executeUpdate();
        }

        resultSet.close(); resultSet = null;
      }
      finally
      {
        if (resultSet != null) resultSet.close();
      }

      // close datbase
      closePasswordDatabase(database); database = null;
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return null;
    }
    finally
    {
      if (database != null) closePasswordDatabase(database);
    }

    return password;
  }

  /** load and decode password with master password
   * @param password password to encode
   * @return encoded password or null
   */
  public String getPassword(String name, String host)
  {
    return getPassword(name+"@"+host);
  }

  /** store password encode password with master password
   * @param name name
   * @param password to set
   */
  public void setPassword(String name, String password)
  {
    // get master password
    if (masterPassword == null)
    {
      masterPassword = Dialogs.password(shell,"Master password");
      if (masterPassword == null) return;
    }

    // store password into password database
    Database database = null;
    try
    {
      PreparedStatement preparedStatement;

      database = openPasswordDatabase();

      // delete old password
      preparedStatement = database.connection.prepareStatement("DELETE FROM passwords WHERE name=?;");
      preparedStatement.setString(1,name);
      preparedStatement.executeUpdate();

      // store password
      preparedStatement = database.connection.prepareStatement("INSERT INTO passwords (name,data) VALUES (?,?);");
      preparedStatement.setString(1,name);
      preparedStatement.setBytes(2,encodePassword(masterPassword,password));
      preparedStatement.executeUpdate();

      // close datbase
      closePasswordDatabase(database); database = null;
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return;
    }
    finally
    {
      if (database != null) closePasswordDatabase(database);
    }
  }

  /** store password encode password with master password
   * @param name name
   * @param host host name
   * @param password to set
   */
  public void setPassword(String name, String host, String password)
  {
    setPassword(name+"@"+host,password);
  }

  /** set new master password
   * @param newMasterPassword new master password
   */
  public void setNewMasterPassword(String oldMasterPassword, String newMasterPassword)
  {
    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      database = openPasswordDatabase();

      // get encoded password
      statement = database.connection.createStatement();
      resultSet = null;
      try
      {
        preparedStatement = database.connection.prepareStatement("SELECT name,data FROM passwords;");
        resultSet = preparedStatement.executeQuery();

        preparedStatement = database.connection.prepareStatement("UPDATE passwords SET data=? WHERE name=?;");
        while (resultSet.next())
        {
          // read and decode password
          String name     = resultSet.getString("name");
          String password = decodePassword(oldMasterPassword,resultSet.getBytes("data"));

          // encode with new password and store
          preparedStatement.setBytes(1,encodePassword(newMasterPassword,password));
          preparedStatement.setString(2,name);
          preparedStatement.executeUpdate();
        }

        resultSet.close(); resultSet = null;
      }
      finally
      {
        if (resultSet != null) resultSet.close();
      }

      // close datbase
      closePasswordDatabase(database); database = null;
    }
    catch (SQLException exception)
    {
      Dialogs.error(shell,"Cannot set new master password (error: %s)",exception.getMessage());
      return;
    }
    finally
    {
      if (database != null) closePasswordDatabase(database);
    }

    // set new master password
    masterPassword = newMasterPassword;
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
    IMAGE_ARROW_UP    = Widgets.loadImage(display,"arrow-up.png");
    IMAGE_ARROW_DOWN  = Widgets.loadImage(display,"arrow-down.png");
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
    widgetTabFolder.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        TabFolder tabFolder = (TabFolder)mouseEvent.widget;

        /* Note: it is not possible to add a mouse-double-click handle to
          a tab-item nor a tab-folder and the correct tab-item is returned
          when the tab-items a scrolled. Thus use the following work-around:
            - get offset of first time = width of scroll buttons left and right
            - check mouse position with offset
            - currently selected tab-item is item with mouse-click
        */
        TabItem[] tabItems    = tabFolder.getItems();
        int scrollButtonWidth = (tabItems != null)?tabItems[0].getBounds().x:0;
        Rectangle bounds      = tabFolder.getBounds();
        if ((mouseEvent.x > bounds.x+scrollButtonWidth) && (mouseEvent.x < bounds.x+bounds.width-scrollButtonWidth))
        {
          TabItem[] selectedTabItems = tabFolder.getSelection();
          TabItem   tabItem          = ((selectedTabItems != null) && (selectedTabItems.length > 0))?selectedTabItems[0]:null;

          if (tabItem != null)
          {
            editRepository((RepositoryTab)selectedTabItems[0].getData());
          }
        }
      }

      public void mouseDown(final MouseEvent mouseEvent)
      {
      }

      public void mouseUp(final MouseEvent mouseEvent)
      {
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

        selectRepositoryTab((RepositoryTab)tabItem.getData());
      }
    });
    widgetTabFolder.setToolTipText("Repository tab.\nDouble-click to edit settings of repository.");

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
      widgetButtonUpdate.setToolTipText("Update selected entries with revisions from repository.");

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
      widgetButtonCommit.setToolTipText("Commit selected entries.");

      widgetButtonCreatePatch = Widgets.newButton(composite,"Patch");
      Widgets.layout(widgetButtonCreatePatch,0,2,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonCreatePatch.addSelectionListener(new SelectionListener()
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
      widgetButtonCreatePatch.setToolTipText("Create patch for selected entries.");

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
      widgetButtonAdd.setToolTipText("Add selected entries to repository.");

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
      widgetButtonRemove.setToolTipText("Remove selected entries from repository.");

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
      widgetButtonRevert.setToolTipText("Revert local changes of selected entires.");

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
      widgetButtonDiff.setToolTipText("Show differences of selected entry with revision in repository.");

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
      widgetButtonRevisions.setToolTipText("Show revisions of selected entry.");

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
Dprintf.dprintf("");
        }
      });
      widgetButtonSolve.setToolTipText("NYI");
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

      menuItem = Widgets.addMenuItem(menu,"Edit repository list...",Settings.keyEditRepositoryList);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editRepositoryList();
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
          if (selectedRepositoryTab != null)
          {
            editRepository(selectedRepositoryTab);
          }
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

          if (selectedRepositoryTab != null)
          {
            closeRepository(selectedRepositoryTab);
          }
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

      menuItem = Widgets.addMenuItem(menu,"Quit",SWT.CTRL+'Q');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // send close-event to shell
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
      menuItem = Widgets.addMenuItem(menu,"New master password...");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Composite composite;
          Label     label;
          Button    button;

          final Shell dialog = Dialogs.open(shell,"New master password",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

          final Text   widgetOldMasterPassword;
          final Text   widgetNewMasterPassword1,widgetNewMasterPassword2;
          final Button widgetSetPassword;

          composite = Widgets.newComposite(dialog);
          composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
          Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
          {
            label = Widgets.newLabel(composite,"Old password:");
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetOldMasterPassword = Widgets.newPassword(composite);
            Widgets.layout(widgetOldMasterPassword,0,1,TableLayoutData.WE);

            label = Widgets.newLabel(composite,"New password:");
            Widgets.layout(label,1,0,TableLayoutData.W);

            widgetNewMasterPassword1 = Widgets.newPassword(composite);
            Widgets.layout(widgetNewMasterPassword1,1,1,TableLayoutData.WE);

            label = Widgets.newLabel(composite,"Verify password:");
            Widgets.layout(label,2,0,TableLayoutData.W);

            widgetNewMasterPassword2 = Widgets.newPassword(composite);
            Widgets.layout(widgetNewMasterPassword2,2,1,TableLayoutData.WE);
          }

          // buttons
          composite = Widgets.newComposite(dialog);
          composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
          Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
          {
            widgetSetPassword = Widgets.newButton(composite,"Set password");
            Widgets.layout(widgetSetPassword,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            widgetSetPassword.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String oldMasterPassword  = widgetOldMasterPassword.getText();
                String newMasterPassword1 = widgetNewMasterPassword1.getText();
                String newMasterPassword2 = widgetNewMasterPassword2.getText();
                if (!newMasterPassword1.equals(newMasterPassword2))
                {
                  Dialogs.error(dialog,"New passwords differ!");
                  Widgets.setFocus(widgetNewMasterPassword1);
                  return;
                }

                setNewMasterPassword(oldMasterPassword,newMasterPassword1);

                Dialogs.close(dialog,true);
              }
            });

            button = Widgets.newButton(composite,"Cancel");
            Widgets.layout(button,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
          Widgets.setNextFocus(widgetOldMasterPassword,widgetNewMasterPassword1);
          Widgets.setNextFocus(widgetNewMasterPassword1,widgetNewMasterPassword2);
          Widgets.setNextFocus(widgetNewMasterPassword2,widgetSetPassword);

          // run dialog
          Widgets.setFocus(widgetOldMasterPassword);
          Dialogs.run(dialog,false);
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Preferences");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editPreferences();
        }
      });
      menuItem = Widgets.addMenuItem(menu,"Preferences save...");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // save settings
          boolean saveSettings = true;
          if (Settings.isFileModified())
          {
            saveSettings = Dialogs.confirm(shell,"Confirmation","Settings were modified externally.","Overwrite","Cancel",false);
          }
          if (saveSettings)
          {
            Settings.save();
          }
        }
      });
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
          Dialogs.info(shell,"About","Onzen "+Config.VERSION_MAJOR+"."+Config.VERSION_MINOR+" (revision "+Config.VERSION_REVISION+").\n\nWritten by Torsten Rupp.");
        }
      });
    }

    if (Settings.debugFlag)
    {
      menu = Widgets.addMenu(menuBar,"Debug");
      {
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
    CommitMessage.loadHistory();
    CommitMessage.startBroadcast();

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
        // save repository list
        if (repositoryList != null)
        {
          try
          {
            repositoryList.save();
          }
          catch (IOException exception)
          {
            if (!Dialogs.confirm(shell,"Confirmation",String.format("Cannot store repository list (error: %s).\n\nQuit anyway?",exception.getMessage())))
            {
              return;
            }
          }
        }

        // save settings
        boolean saveSettings = true;
        if (Settings.isFileModified())
        {
          switch (Dialogs.select(shell,"Confirmation","Settings were modified externally.",new String[]{"Overwrite","Just quit","Cancel"},0))
          {
            case 0:
              break;
            case 1:
              saveSettings = false;
              break;
            case 2:
              event.doit = false;
              return;
          }
        }
        if (saveSettings)
        {
          Settings.save();
        }

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

  /** open/create password database
   * @return database
   */
  private Database openPasswordDatabase()
  {
    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      database = new Database("passwords");

      // create tables if needed
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS passwords ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  name     TEXT, "+
                              "  data     BLOB "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = database.connection.createStatement();
      resultSet = null;
      try
      {
        resultSet = statement.executeQuery("SELECT name,value FROM meta;");

        if (!resultSet.next())
        {
          preparedStatement = database.connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
          preparedStatement.setString(1,"1");
          preparedStatement.executeUpdate();
        }

        resultSet.close(); resultSet = null;
      }
      finally
      {
        if (resultSet != null) resultSet.close();
      }
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return null;
    }

    return database;
  }

  /** close password database
   * @param database database
   */
  private void closePasswordDatabase(Database database)
  {
    try
    {
      database.close();
    }
    catch (SQLException exception)
    {
      // ignored
    }
  }

  /** encode password with master password
   * @param password password to encode
   * @return encoded password bytess or null
   */
  private byte[] encodePassword(String masterPassword, String password)
  {
    if ((masterPassword != null) && !masterPassword.isEmpty())
    {
      try
      {
        byte[] bytes;

        // get secret key
        byte[] masterPasswordBytes = masterPassword.getBytes();
        bytes = new byte[128/8];
        for (int z = 0; z < bytes.length; z++)
        {
          bytes[z] = masterPasswordBytes[z%masterPasswordBytes.length];
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes,"AES");

        // get cipher
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec);

        // encrypt password
        byte[] passwordBytes = password.getBytes();
        bytes = Arrays.copyOf(passwordBytes,(passwordBytes.length+15)/16*16);        
        return cipher.doFinal(bytes);
      }
      catch (Exception exception)
      {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
        return null;
      }
    }
    else
    {
      return null;
    }
  }

  /** decode password with master password
   * @param encodedPassword encoded password bytes
   * @return decoded password or null
   */
  private String decodePassword(String masterPassword, byte[] encodedPassword)
  {
    if (masterPassword == null)
    {
      masterPassword = Dialogs.password(shell,"Master password");
    }

    if ((masterPassword != null) && !masterPassword.isEmpty())
    {
      try
      {
        byte[] bytes;

        // get secret key
        byte[] masterPasswordBytes = masterPassword.getBytes();
        bytes = new byte[128/8];
        for (int z = 0; z < bytes.length; z++)
        {
          bytes[z] = masterPasswordBytes[z%masterPasswordBytes.length];
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes,"AES");

        // get cipher
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,secretKeySpec);

        // decrypt password
        return new String(cipher.doFinal(encodedPassword));
      }
      catch (Exception exception)
      {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
        return null;
      }     
    }
    else
    {
      return null;
    }
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
    for (RepositoryTab repositoryTab : repositoryTabMap.values())
    {
      repositoryTab.close();
    }
    repositoryTabMap.clear();
    addRepositoryTabEmpty();

    // remove entries in repository menu
    MenuItem[] menuItems = menuRepositories.getItems();
    for (MenuItem menuItem : menuRepositories.getItems())
    {
      menuItem.dispose();
    }
  }

  /** reset repository tab list (with possible new ordering)
   */
  private void setRepositoryList()
  {
    // clear repositories
    clearRepositories();

    // reset repository list
    for (Repository repository : repositoryList)
    {
      // add repository tab
      RepositoryTab repositoryTab = new RepositoryTab(this,widgetTabFolder,repository);
      repositoryTabMap.put(repository,repositoryTab);

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
    if (repositoryTabMap.size() > 0)
    {
      removeRepositoryTabEmpty();
    }

    // because SWT select first tab, process this selection now
    while (display.readAndDispatch())
    {
      // nothing
    }

    // reset repository menu entries
    for (Repository repository : repositoryList)
    {
      MenuItem menuItem = Widgets.addMenuItem(menuRepositories,repository.title);
      menuItem.setData(repositoryTabMap.get(repository));
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;

          selectRepositoryTab((RepositoryTab)widget.getData());
        }
      });
    }

    // set window title
    shell.setText("Onzen: "+repositoryList.name);
  }

  /** set repository tab list
   * @param repositoryList repository list to set
   */
  private void setRepositoryList(RepositoryList repositoryList)
  {
    // get selected repository
    Repository selectedRepository = null;
    for (Repository repository : repositoryList)
    {
      if (repository.selected)
      {
        selectedRepository = repository;
        break;
      }
    }

    // set list
    this.repositoryList = repositoryList;
    setRepositoryList();

    // select repository tab
    selectRepositoryTab(repositoryTabMap.get(selectedRepository));
  }

  /** reset repository tab list (with possible new ordering)
   */
  private void resetRepositoryList()
  {
    // save currently selected repository
    Repository selectedRepository = (selectedRepositoryTab != null)?selectedRepositoryTab.repository:null;

    // set list
    setRepositoryList();

    // select repository tab
    selectRepositoryTab(repositoryTabMap.get(selectedRepository));
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
    repositoryTabMap.put(repository,repositoryTab);
    if (repositoryTabMap.size() == 1)
    {
      // select repository tab
      selectRepositoryTab(repositoryTab);
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
    if (repositoryTabMap.size() <= 1)
    {
      addRepositoryTabEmpty();
    }

    // close tab, remove from repository list
    repositoryTab.close();
    repositoryTabMap.remove(repositoryTab.repository);

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
      Widgets.layout(widgetNew,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
      Widgets.layout(widgetDelete,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
      Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        Widgets.layout(widgetOpen,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        Widgets.layout(widgetNew,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        Widgets.layout(widgetDelete,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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

  /** edit repository list
   */
  private void editRepositoryList()
  {
    /** dialog data
     */
    class Data
    {
      RepositoryTab[] repositories;

      Data()
      {
        this.repositories     = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite,subSubComposite;
    Label       label;
    Button      button;

    // repository edit dialog
    dialog = Dialogs.open(shell,"Edit repository list",new double[]{1.0,0.0},1.0);

    final List   widgetList;
    final Button widgetSave;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      widgetList = Widgets.newList(composite);
      Widgets.layout(widgetList,0,0,TableLayoutData.NSWE);
      widgetList.setToolTipText("Repository title.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,1.0));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        button = Widgets.newButton(subComposite,"Open");
        Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String rootPath = Dialogs.directory(shell,"Open repository","");
            if (rootPath != null)
            {
              Repository repository = openRepository(rootPath);

              widgetList.add(repository.title);
            }
          }
        });
        button.setToolTipText("Open repository.");

        button = Widgets.newButton(subComposite,"Edit");
        Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if ((index >= 0) && (index < repositoryList.size()))
            {
              RepositoryTab repositoryTab = repositoryTabMap.get(repositoryList.get(index));
              editRepository(repositoryTab);
            }
          }
        });
        button.setToolTipText("Edit repository settings.");

        button = Widgets.newButton(subComposite,"Close");
        Widgets.layout(button,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if ((index >= 0) && (index < repositoryList.size()))
            {
              RepositoryTab repositoryTab = repositoryTabMap.get(repositoryList.get(index));
              closeRepository(repositoryTab);
            }
          }
        });
        button.setToolTipText("Close repository.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        Widgets.layout(button,0,3,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if (index > 0)
            {
              // move up
              repositoryList.move(repositoryList.get(index),index-1);

              // update view
              widgetList.removeAll();
              for (Repository repository : repositoryList)
              {
                widgetList.add(repository.title);
              }

              // set selection
              widgetList.setSelection(index-1);
            }
          }
        });
        button.setToolTipText("Open repository position up.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        Widgets.layout(button,0,4,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if (index < repositoryList.size()-1)
            {
              // move up
              repositoryList.move(repositoryList.get(index),index+1);

              // update view
              widgetList.removeAll();
              for (Repository repository : repositoryList)
              {
                widgetList.add(repository.title);
              }

              // set selection
              widgetList.setSelection(index+1);
            }
          }
        });
        button.setToolTipText("Move repository position down.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Close");
      Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
    widgetList.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = widgetList.getSelectionIndex();
        if ((index >= 0) && (index < repositoryList.size()))
        {
          RepositoryTab repositoryTab = repositoryTabMap.get(repositoryList.get(index));
          editRepository(repositoryTab);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryEditRepositoryList);

    // add repositories
    for (Repository repository : repositoryList)
    {
      widgetList.add(repository.title);
    }

    // run
    Widgets.setFocus(widgetList);
    Dialogs.run(dialog);

    // set data
    resetRepositoryList();

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

  /** open new repository tab
   * @param rootPath root path
   * @return repository or null
   */
  private Repository openRepository(String rootPath)
  {
    Repository repository = null;

    try
    {
      // open repository
      repository = Repository.newInstance(rootPath);

      // add and select new repository
      selectRepositoryTab(addRepositoryTab(repository));
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(shell,"Cannot open repository '%s' (error: %s)",rootPath,exception.getMessage());
      return null;
    }

    return repository;
  }

  /** edit repository
   */
  private void editRepository(final RepositoryTab repositoryTab)
  {
    /** dialog data
     */
    class Data
    {
      String   title;
      String   rootPath;
      String   masterRepository;
      String   mailSMTPHost;
      int      mailSMTPPort;
      boolean  mailSMTPSSL;
      String   mailLogin;
      String   mailPassword;
      String   mailFrom;
      String[] patchMailTests;
      String   patchMailTo;
      String   patchMailCC;
      String   patchMailSubject;
      String   patchMailText;

      Data()
      {
        this.title            = null;
        this.rootPath         = null;
        this.masterRepository = null;
        this.mailSMTPHost     = null;
        this.mailSMTPPort     = 0;
        this.mailSMTPSSL      = false;
        this.mailLogin        = null;
        this.mailPassword     = null;
        this.mailFrom         = null;
        this.patchMailTests   = null;
        this.patchMailTo      = null;
        this.patchMailCC      = null;
        this.patchMailSubject = null;
        this.patchMailSubject = null;
      }
    };

    final Data  data = new Data();
    final Shell dialog;
    Composite   composite,subComposite,subSubComposite,subSubSubComposite;
    TabFolder   tabFolder;
    Label       label;
    Button      button;
    Spinner     spinner;
    Text        text;
    Menu        menu,subMenu;
    MenuItem    menuItem;

    // repository edit dialog
    dialog = Dialogs.open(shell,"Edit repository",new double[]{1.0,0.0},1.0);

    final Text                  widgetTitle;
    final Text                  widgetRootPath;
    final HashMap<Field,Widget> widgetFieldMap = new HashMap<Field,Widget>();
    final Text                  widgetMailSMTPHost;
    final Spinner               widgetMailSMTPPort;
    final Button                widgetMailSMTPSSL;
    final Text                  widgetMailLogin;
    final Text                  widgetMailPassword;
    final Text                  widgetMailFrom;
    final List                  widgetPatchTests;
    final Text                  widgetPatchMailTo;
    final Text                  widgetPatchMailCC;
    final Text                  widgetPatchMailSubject;
    final Text                  widgetPatchMailText;
    final Button                widgetSave;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(1.0,1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);

      subComposite = Widgets.addTab(tabFolder,"Repository");
      subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0},2));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        // common values
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

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
        Widgets.layout(subSubComposite,2,1,TableLayoutData.WE);
        {
          widgetRootPath = Widgets.newText(subSubComposite);
          widgetRootPath.setText(repositoryTab.repository.rootPath);
          Widgets.layout(widgetRootPath,0,0,TableLayoutData.WE);

          button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
          Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String directoryPath = Dialogs.directory(shell,
                                                       "Select directory",
                                                       widgetRootPath.getText()
                                                      );
              if (directoryPath != null)
              {
                widgetRootPath.setText(directoryPath);
              }
            }
          });
        }

        // additional values
        int row = 3;
        for (final Field field : repositoryTab.repository.getClass().getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if (annotation instanceof RepositoryValue)
            {
              try
              {
                RepositoryValue repositoryValue = (RepositoryValue)annotation;

                label = Widgets.newLabel(subComposite,repositoryValue.title());
                Widgets.layout(label,row,0,TableLayoutData.W);

                Class type = field.getType();
                if      (type == int.class)
                {
                  // int value -> spinner input
                  int value = field.getInt(repositoryTab.repository);

                  spinner = Widgets.newSpinner(subComposite);
                  spinner.setSelection(value);
                  spinner.setMinimum(repositoryValue.min());
                  spinner.setMaximum(repositoryValue.max());
                  Widgets.layout(spinner,row,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
                  spinner.setToolTipText(repositoryValue.tooltip());
                  widgetFieldMap.put(field,spinner);
                }
                else if (type == long.class)
                {
                  // long value -> spinner input
                  long value = field.getLong(repositoryTab.repository);

                  spinner = Widgets.newSpinner(subComposite);
                  spinner.setSelection((int)value);
                  spinner.setMinimum(repositoryValue.min());
                  spinner.setMaximum(repositoryValue.max());
                  Widgets.layout(spinner,row,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
                  spinner.setToolTipText(repositoryValue.tooltip());
                  widgetFieldMap.put(field,spinner);
                }
                else if (type == float.class)
                {
throw new Error("Internal error: NYI");
                }
                else if (type == double.class)
                {
throw new Error("Internal error: NYI");
                }
                else if (type == boolean.class)
                {
                  // boolean value -> checkbox
                  boolean value = field.getBoolean(repositoryTab.repository);

                  button = Widgets.newCheckbox(subComposite,repositoryValue.text());
                  button.setSelection(value);
                  Widgets.layout(button,row,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
                  button.setToolTipText(repositoryValue.tooltip());
                  widgetFieldMap.put(field,button);
                }
                else if (type == String.class)
                {
                  // string value -> text input without/with path selector
                  String value = (type != null) ? (String)field.get(repositoryTab.repository) : repositoryValue.defaultValue();

                  if (repositoryValue.pathSelector())
                  {
                    subSubComposite = Widgets.newComposite(subComposite);
                    subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
                    Widgets.layout(subSubComposite,row,1,TableLayoutData.WE);
                    {
                      text = Widgets.newText(subSubComposite);
                      if (value != null) text.setText(value);
                      Widgets.layout(text,0,0,TableLayoutData.WE);
                      text.setToolTipText(repositoryValue.tooltip());
                      widgetFieldMap.put(field,text);

                      button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
                      Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
                      button.addSelectionListener(new SelectionListener()
                      {
                        public void widgetDefaultSelected(SelectionEvent selectionEvent)
                        {
                        }
                        public void widgetSelected(SelectionEvent selectionEvent)
                        {
                          Text widget = (Text)widgetFieldMap.get(field);

                          String directoryPath = Dialogs.directory(shell,
                                                                   "Select directory",
                                                                   widget.getText()
                                                                  );
                          if (directoryPath != null)
                          {
                            widget.setText(directoryPath);
                          }
                        }
                      });
                    }
                  }
                  else
                  {
                    text = Widgets.newText(subComposite);
                    text.setText(value);
                    Widgets.layout(text,row,1,TableLayoutData.WE);
                    text.setToolTipText(repositoryValue.tooltip());
                    widgetFieldMap.put(field,text);
                  }
                }
                else if (type.isEnum())
                {
throw new Error("Internal error: NYI");
                }
                else
                {
                  throw new Error("Internal error: unsupported type '"+type+"'");
                }

                row++;
              }
              catch (Exception exception)
              {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
              }
            }
          }
        }
      }

      subComposite = Widgets.addTab(tabFolder,"Mail");
      subComposite.setLayout(new TableLayout(new double[]{0.0,0.3,0.7},1.0));
      Widgets.layout(subComposite,0,1,TableLayoutData.NSWE);
      {
        subSubComposite = Widgets.newGroup(subComposite,"Mail server");
        subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.WE,0,0,2);
        {
          label = Widgets.newLabel(subSubComposite,"SMTP server:");
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{0.0,0.7,0.0,0.3,0.0}));
          Widgets.layout(subSubSubComposite,0,1,TableLayoutData.WE,0,0,2);
          {
            label = Widgets.newLabel(subSubSubComposite,"Name:");
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetMailSMTPHost = Widgets.newText(subSubSubComposite);
            widgetMailSMTPHost.setText((repositoryTab.repository.mailSMTPHost != null)?repositoryTab.repository.mailSMTPHost:Settings.mailSMTPHost);
            Widgets.layout(widgetMailSMTPHost,0,1,TableLayoutData.WE);
            widgetMailSMTPHost.setToolTipText("Mail SMTP server host name.");

            label = Widgets.newLabel(subSubSubComposite,"Port:");
            Widgets.layout(label,0,2,TableLayoutData.W);

            widgetMailSMTPPort = Widgets.newSpinner(subSubSubComposite,0,65535);
            widgetMailSMTPPort.setTextLimit(5);
            widgetMailSMTPPort.setSelection((repositoryTab.repository.mailSMTPPort != 0)?repositoryTab.repository.mailSMTPPort:Settings.mailSMTPPort);
            Widgets.layout(widgetMailSMTPPort,0,3,TableLayoutData.WE);
            widgetMailSMTPPort.setToolTipText("Mail SMTP server port number.");

            widgetMailSMTPSSL = Widgets.newCheckbox(subSubSubComposite,"SSL");
            widgetMailSMTPSSL.setSelection(repositoryTab.repository.mailSMTPSSL);
            Widgets.layout(widgetMailSMTPSSL,0,4,TableLayoutData.E);
            widgetMailSMTPSSL.setToolTipText("Use SMTP with SSL encryption.");
          }

          label = Widgets.newLabel(subSubComposite,"Login:");
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0,1.0}));
          Widgets.layout(subSubSubComposite,1,1,TableLayoutData.WE,0,0,2);
          {
            label = Widgets.newLabel(subSubSubComposite,"Name:");
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetMailLogin = Widgets.newText(subSubSubComposite);
            widgetMailLogin.setText((repositoryTab.repository.mailLogin != null)?repositoryTab.repository.mailLogin:Settings.mailLogin);
            Widgets.layout(widgetMailLogin,0,1,TableLayoutData.WE);
            widgetMailLogin.setToolTipText("Mail server login name.");

            label = Widgets.newLabel(subSubSubComposite,"Password:");
            Widgets.layout(label,0,2,TableLayoutData.W);

            widgetMailPassword = Widgets.newPassword(subSubSubComposite);
            String password = getPassword(repositoryTab.repository.mailLogin,repositoryTab.repository.mailSMTPHost);
            if (password != null) widgetMailPassword.setText(password);
            Widgets.layout(widgetMailPassword,0,3,TableLayoutData.WE);
            widgetMailPassword.setToolTipText("Mail server login password.");
          }

          label = Widgets.newLabel(subSubComposite,"From name:");
          Widgets.layout(label,3,0,TableLayoutData.W,0,0,2);

          widgetMailFrom = Widgets.newText(subSubComposite);
          widgetMailFrom.setText((repositoryTab.repository.mailFrom != null)?repositoryTab.repository.mailFrom:Settings.mailFrom);
          Widgets.layout(widgetMailFrom,3,1,TableLayoutData.WE,0,0,2);
          widgetMailFrom.setToolTipText("Mail from address.");
        }
        menu = Widgets.newPopupMenu(dialog);
        {
          subMenu = Widgets.addMenu(menu,"Copy from...");
          for (Repository repository : repositoryList)
          {
            if (repository != repositoryTab.repository)
            {
              menuItem = Widgets.addMenuItem(subMenu,repository.title);
              menuItem.setData(repository);
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  MenuItem   widget     = (MenuItem)selectionEvent.widget;
                  Repository repository = (Repository)widget.getData();

                  if (repository.mailSMTPHost != null) widgetMailSMTPHost.setText(repository.mailSMTPHost);
                  widgetMailSMTPPort.setSelection(repository.mailSMTPPort);
                  widgetMailSMTPSSL.setSelection(repository.mailSMTPSSL);
                  if (repository.mailLogin != null) widgetMailLogin.setText(repository.mailLogin);
                  widgetMailPassword.setText(getPassword(repository.mailLogin,repository.mailSMTPHost));
                  if (repository.mailFrom != null) widgetMailFrom.setText(repository.mailFrom);
                }
              });
            }
          }
        }
        subSubComposite.setMenu(menu);
        subSubComposite.setToolTipText("Mail server settings.\nRight-click to open context menu.");

        subSubComposite = Widgets.newGroup(subComposite,"Tests");
        subSubComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subSubComposite,1,0,TableLayoutData.NSWE);
        {
          widgetPatchTests = Widgets.newList(subSubComposite);
          if (repositoryTab.repository.patchMailTests != null)
          {
            for (String test : repositoryTab.repository.patchMailTests)
            {
              widgetPatchTests.add(test);
            }
          }
          Widgets.layout(widgetPatchTests,0,0,TableLayoutData.NSWE,0,0,2);
          widgetPatchTests.setToolTipText("List of test descriptions which can be selected when sending a patch mail.");

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubSubComposite,1,0,TableLayoutData.E,0,0,2);
          {
            button = Widgets.newButton(subSubSubComposite,Onzen.IMAGE_ARROW_UP);
            Widgets.layout(button,0,0,TableLayoutData.E);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetPatchTests.getSelectionIndex();
                if (index >= 0)
                {
                  if (index > 0)
                  {
                    String test0 = widgetPatchTests.getItem(index-1);
                    String test1 = widgetPatchTests.getItem(index  );
                    widgetPatchTests.setItem(index-1,test1);
                    widgetPatchTests.setItem(index  ,test0);
                    widgetPatchTests.setSelection(index-1);
                  }
                }
              }
            });
            button.setToolTipText("Move test description up.");

            button = Widgets.newButton(subSubSubComposite,Onzen.IMAGE_ARROW_DOWN);
            Widgets.layout(button,0,1,TableLayoutData.E);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetPatchTests.getSelectionIndex();
                if (index >= 0)
                {
                  if (index < widgetPatchTests.getItemCount()-1)
                  {
                    String test0 = widgetPatchTests.getItem(index  );
                    String test1 = widgetPatchTests.getItem(index+1);
                    widgetPatchTests.setItem(index  ,test1);
                    widgetPatchTests.setItem(index+1,test0);
                    widgetPatchTests.setSelection(index+1);
                  }
                }
              }
            });
            button.setToolTipText("Move test description down.");

            button = Widgets.newButton(subSubSubComposite,"Add");
            Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String test = Dialogs.string(dialog,"Edit test","Test:","","Add","Cancel");
                if (test != null)
                {
                  widgetPatchTests.add(test);
                }
              }
            });
            button.setToolTipText("Add new test description.");

            button = Widgets.newButton(subSubSubComposite,"Remove");
            Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetPatchTests.getSelectionIndex();
                if (index >= 0)
                {
                  String test = widgetPatchTests.getItem(index);
                  if (Dialogs.confirm(dialog,"Confirmation",String.format("Remove test '%s'?",test)))
                  {
                    widgetPatchTests.remove(index);
                  }
                }
              }
            });
            button.setToolTipText("Remove selected test description.");
          }
        }
        menu = Widgets.newPopupMenu(dialog);
        {
          subMenu = Widgets.addMenu(menu,"Copy from...");
          for (Repository repository : repositoryList)
          {
            if (repository != repositoryTab.repository)
            {
              menuItem = Widgets.addMenuItem(subMenu,repository.title);
              menuItem.setData(repository);
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  MenuItem   widget     = (MenuItem)selectionEvent.widget;
                  Repository repository = (Repository)widget.getData();

                  widgetPatchTests.removeAll();
                  if (repository.patchMailTests != null)
                  {
                    for (String test : repository.patchMailTests)
                    {
                      widgetPatchTests.add(test);
                    }
                  }
                }
              });
            }
          }
        }
        subSubComposite.setMenu(menu);
        subSubComposite.setToolTipText("Test description settings.\nRight-click to open context menu.");

        subSubComposite = Widgets.newGroup(subComposite,"Patch mail");
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,1.0},new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,2,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"To:");
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          widgetPatchMailTo = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailTo != null) widgetPatchMailTo.setText(repositoryTab.repository.patchMailTo);
          Widgets.layout(widgetPatchMailTo,0,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailTo.setToolTipText("Default to-address for patch mails.");

          label = Widgets.newLabel(subSubComposite,"CC:");
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);
          button.setToolTipText("Default CC-addresses for patch mails.");

          widgetPatchMailCC = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailCC != null) widgetPatchMailCC.setText(repositoryTab.repository.patchMailCC);
          Widgets.layout(widgetPatchMailCC,1,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailCC.setToolTipText("Patch mail carbon-copy address. Separate multiple addresses by spaces.");

          label = Widgets.newLabel(subSubComposite,"Subject:");
          Widgets.layout(label,2,0,TableLayoutData.W,0,0,2);

          widgetPatchMailSubject = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailSubject != null) widgetPatchMailSubject.setText(repositoryTab.repository.patchMailSubject);
          Widgets.layout(widgetPatchMailSubject,2,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailSubject.setToolTipText("Patch mail subject template.\nMacros:\n  ${n} - patch number\n  ${summary} - summary text");

          label = Widgets.newLabel(subSubComposite,"Text:");
          Widgets.layout(label,3,0,TableLayoutData.NW,0,0,2);

          widgetPatchMailText = Widgets.newText(subSubComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
          if (repositoryTab.repository.patchMailText != null) widgetPatchMailText.setText(repositoryTab.repository.patchMailText);
          Widgets.layout(widgetPatchMailText,3,1,TableLayoutData.NSWE,0,0,2);
          widgetPatchMailText.setToolTipText("Patch mail text template.\nMacros:\n  ${date} - date\n  ${time} - time\n  ${datetime} - date/time\n  ${message} - message\n  ${tests} - tests\n");
        }
        menu = Widgets.newPopupMenu(dialog);
        {
          subMenu = Widgets.addMenu(menu,"Copy from...");
          for (Repository repository : repositoryList)
          {
            if (repository != repositoryTab.repository)
            {
              menuItem = Widgets.addMenuItem(subMenu,repository.title);
              menuItem.setData(repository);
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  MenuItem   widget     = (MenuItem)selectionEvent.widget;
                  Repository repository = (Repository)widget.getData();

                  if (repository.patchMailTo != null) widgetPatchMailTo.setText(repository.patchMailTo);
                  if (repository.patchMailCC != null) widgetPatchMailCC.setText(repository.patchMailCC);
                  if (repository.patchMailSubject != null) widgetPatchMailSubject.setText(repository.patchMailSubject);
                  if (repository.patchMailText != null) widgetPatchMailText.setText(repository.patchMailText);
                }
              });
            }
          }
        }
        subSubComposite.setMenu(menu);
        subSubComposite.setToolTipText("Patch mail settings.\nRight-click to open context menu.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetSave = Widgets.newButton(composite,"Save");
      Widgets.layout(widgetSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
          data.mailSMTPHost     = widgetMailSMTPHost.getText();
          data.mailSMTPPort     = widgetMailSMTPPort.getSelection();
          data.mailSMTPSSL      = widgetMailSMTPSSL.getSelection();
          data.mailLogin        = widgetMailLogin.getText();
          data.mailPassword     = widgetMailPassword.getText();
          data.mailFrom         = widgetMailFrom.getText();
          data.patchMailTests   = widgetPatchTests.getItems();
          data.patchMailTo      = widgetPatchMailTo.getText();
          data.patchMailCC      = widgetPatchMailCC.getText();
          data.patchMailSubject = widgetPatchMailSubject.getText();
          data.patchMailText    = widgetPatchMailText.getText();

          for (final Field field : repositoryTab.repository.getClass().getDeclaredFields())
          {
            for (Annotation annotation : field.getDeclaredAnnotations())
            {
              if (annotation instanceof RepositoryValue)
              {
                try
                {
                  RepositoryValue repositoryValue = (RepositoryValue)annotation;

                  Class type = field.getType();
                  if      (type == int.class)
                  {
                    Spinner spinner = (Spinner)widgetFieldMap.get(field);
                    field.setInt(repositoryTab.repository,spinner.getSelection());
                  }
                  else if (type == long.class)
                  {
                    Spinner spinner = (Spinner)widgetFieldMap.get(field);
                    field.setLong(repositoryTab.repository,spinner.getSelection());
                  }
                  else if (type == float.class)
                  {
throw new Error("Internal error: NYI");
                  }
                  else if (type == double.class)
                  {
throw new Error("Internal error: NYI");
                  }
                  else if (type == boolean.class)
                  {
                    Button button = (Button)widgetFieldMap.get(field);
                    field.setBoolean(repositoryTab.repository,button.getSelection());
                  }
                  else if (type == String.class)
                  {
                    Text text = (Text)widgetFieldMap.get(field);
                    field.set(repositoryTab.repository,text.getText());
                  }
                  else if (type.isEnum())
                  {
throw new Error("Internal error: NYI");
                  }
                  else
                  {
                    throw new Error("Internal error: unsupported type '"+type+"'");
                  }
                }
                catch (Exception exception)
                {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
                }
              }
            }
          }


          Settings.geometryEditRepository = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });
      widgetSave.setToolTipText("Open selected repository list.");

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
    widgetPatchTests.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          String test = widget.getItem(index);
          test = Dialogs.string(dialog,"Edit test description","Test:",test,"Save","Cancel");
          if (test != null)
          {
            widget.setItem(index,test);
          }
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    Widgets.setNextFocus(widgetTitle,widgetRootPath);
    Widgets.setNextFocus(widgetRootPath,widgetSave);
    Widgets.setNextFocus(widgetPatchMailTo,widgetPatchMailCC);
    Widgets.setNextFocus(widgetPatchMailCC,widgetPatchMailSubject);
    Widgets.setNextFocus(widgetPatchMailSubject,widgetPatchMailText);

    // show dialog
    Dialogs.show(dialog,Settings.geometryEditRepository);

    // run
    Widgets.setFocus(widgetTitle);
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // set data
      repositoryTab.setTitle(data.title);
      repositoryTab.repository.rootPath         = data.rootPath;
      repositoryTab.repository.mailSMTPHost     = data.mailSMTPHost;
      repositoryTab.repository.mailSMTPPort     = data.mailSMTPPort;
      repositoryTab.repository.mailSMTPSSL      = data.mailSMTPSSL;
      repositoryTab.repository.mailLogin        = data.mailLogin;
      repositoryTab.repository.mailFrom         = data.mailFrom;
      repositoryTab.repository.patchMailTests   = data.patchMailTests;
      repositoryTab.repository.patchMailTo      = data.patchMailTo;
      repositoryTab.repository.patchMailCC      = data.patchMailCC;
      repositoryTab.repository.patchMailSubject = data.patchMailSubject;
      repositoryTab.repository.patchMailText    = data.patchMailText;
      setPassword(data.mailLogin,data.mailSMTPHost,data.mailPassword);

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

  /** close selected repository tab
   * @param repositoryTab repository tab to close
   */
  private void closeRepository(RepositoryTab repositoryTab)
  {
    if (Dialogs.confirm(shell,String.format("Really close repository '%s'?",repositoryTab.repository.title)))
    {
      // remove tab
      removeRepositoryTab(repositoryTab);

      if (repositoryTab == selectedRepositoryTab)
      {
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

  /** select repository tab
   * @param repositoryTab repository tab to select
   */
  private void selectRepositoryTab(RepositoryTab repositoryTab)
  {
    // deselect previous repository
    if (selectedRepositoryTab != null) selectedRepositoryTab.repository.selected = false;

    // select new repository
    if (repositoryTab != null)
    {
      // select
      repositoryTab.repository.selected = true;

      // show tab
      repositoryTab.show();

      // enable/disable menu entries
      menuItemApplyPatches.setEnabled(repositoryTab.repository.supportPatchQueues());
      menuItemUnapplyPatches.setEnabled(repositoryTab.repository.supportPatchQueues());
      menuItemPullChanges.setEnabled(repositoryTab.repository.supportPullPush());
      menuItemPushChanges.setEnabled(repositoryTab.repository.supportPullPush());
      menuSetFileMode.setEnabled(repositoryTab.repository.supportSetFileMode());
    }
    else
    {
      // disable menu entries
      menuItemApplyPatches.setEnabled(false);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemPullChanges.setEnabled(false);
      menuItemPushChanges.setEnabled(false);
      menuSetFileMode.setEnabled(false);
    }
    selectedRepositoryTab = repositoryTab;
  }

  /** reselect selected repository tab
   */
  private void reselectRepository()
  {
    if (selectedRepositoryTab != null)
    {
      // show tab
      selectedRepositoryTab.show();

      // enable/disable menu entries
      menuItemApplyPatches.setEnabled(selectedRepositoryTab.repository.supportPatchQueues());
      menuItemUnapplyPatches.setEnabled(selectedRepositoryTab.repository.supportPatchQueues());
      menuItemPullChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuItemPushChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuSetFileMode.setEnabled(selectedRepositoryTab.repository.supportSetFileMode());
    }
    else
    {
      // disable menu entries
      menuItemApplyPatches.setEnabled(false);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemPullChanges.setEnabled(false);
      menuItemPushChanges.setEnabled(false);
      menuSetFileMode.setEnabled(false);
    }
  }

  private void editPreferences()
  {
    Preferences preferences = new Preferences(shell,this);
    preferences.run();
  }
}

/* end of file */
