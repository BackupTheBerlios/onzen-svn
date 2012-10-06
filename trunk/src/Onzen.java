/***********************************************************************\
/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

import javax.activation.MimetypesFileTypeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

// graphics
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
  public static int                    EXITCODE_OK             =   0;
  public static int                    EXITCODE_RESTART        =  64;
  public static int                    EXITCODE_INTERNAL_ERROR = 127;

  // colors
  public static Color                  COLOR_BLACK;
  public static Color                  COLOR_WHITE;
  public static Color                  COLOR_GREEN;
  public static Color                  COLOR_DARK_RED;
  public static Color                  COLOR_RED;
  public static Color                  COLOR_DARK_BLUE;
  public static Color                  COLOR_BLUE;
  public static Color                  COLOR_DARK_YELLOW;
  public static Color                  COLOR_YELLOW;
  public static Color                  COLOR_DARK_GRAY;
  public static Color                  COLOR_GRAY;
  public static Color                  COLOR_MAGENTA;
  public static Color                  COLOR_BACKGROUND;

  // images
  public static Image                  IMAGE_DIRECTORY;
  public static Image                  IMAGE_FILE;
  public static Image                  IMAGE_LINK;
  public static Image                  IMAGE_ARROW_UP;
  public static Image                  IMAGE_ARROW_DOWN;
  public static Image                  IMAGE_ARROW_LEFT;
  public static Image                  IMAGE_ARROW_RIGHT;
  public static Image                  IMAGE_LOCK;
  public static Image                  IMAGE_EMPTY;

  // fonts
  public static Font                   FONT_DIFF;
  public static Font                   FONT_DIFF_LINE;
  public static Font                   FONT_CHANGES;

  // cursors
  public static Cursor                 CURSOR_WAIT;

  // date/time format
  public static SimpleDateFormat       DATE_FORMAT     = new SimpleDateFormat(Settings.dateFormat);
  public static SimpleDateFormat       TIME_FORMAT     = new SimpleDateFormat(Settings.timeFormat);
  public static SimpleDateFormat       DATETIME_FORMAT = new SimpleDateFormat(Settings.dateTimeFormat);

  public static MimetypesFileTypeMap   MIMETYPES_FILE_TYPE_MAP = new MimetypesFileTypeMap();
  public static HashMap<String,String> FILE_ASSOCIATION_MAP = new HashMap<String,String>();

  public static String                 ALL_FILE_EXTENSION        = isWindowsSystem() ? "*.*" : "*";
  public static String                 EXECUTABLE_FILE_EXTENSION = isWindowsSystem() ? "*.exe" : "*";

  // command line options
  private static final Option[] options =
  {
    new Option("--help",                       "-h",Options.Types.BOOLEAN, "helpFlag"),

    new Option("--debug",                      null,Options.Types.BOOLEAN, "debugFlag"),

    new Option("--cvs-prune-empty-directories",null,Options.Types.BOOLEAN, "cvsPruneEmtpyDirectories"),

    // ignored
    new Option("--swing",                      null, Options.Types.BOOLEAN,null),
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
  private MenuItem                          menuItemLock;
  private MenuItem                          menuItemUnlock;
  private MenuItem                          menuItemIncomingChanges;
  private MenuItem                          menuItemIncomingChangesFrom;
  private MenuItem                          menuItemOutgoingChanges;
  private MenuItem                          menuItemOutgoingChangesTo;
  private MenuItem                          menuItemPullChanges;
  private MenuItem                          menuItemPullChangesFrom;
  private MenuItem                          menuItemPushChanges;
  private MenuItem                          menuItemPushChangesTo;
  private MenuItem                          menuSetFileMode;

  private Menu                              menuShellCommands;
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

  // stored settingn of last checkout
  private static Repository                 lastCheckoutRepository      = RepositoryCVS.getInstance();
  private static String                     lastCheckoutRepositoryPath  = "";
  private static String                     lastCheckoutModuleName      = "";
  private static String                     lastCheckoutRevision        = "";
  private static String                     lastCheckoutDestinationPath = "";

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

  /** renice i/o exception (remove java.io.IOExcpetion text from exception)
   * @param exception i/o exception to renice
   * @return reniced exception
   */
  public static IOException reniceIOException(IOException exception)
  {
    final Pattern PATTERN = Pattern.compile("^(.*?)\\s*java.io.IOException: error=\\d+,\\s*(.*)$",Pattern.CASE_INSENSITIVE);

    Matcher matcher;
    if ((matcher = PATTERN.matcher(exception.getMessage())).matches())
    {
      exception = new IOException(matcher.group(1)+" "+matcher.group(2));
    }

    return exception;
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
      repositoryListName = loadRepositoryList(repositoryListName);
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

      if (repositoryListName != null)
      {
        // run
        exitcode = run();
      }
      else
      {
        exitcode = 0;
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
        if (!widgetStatus.isDisposed()) widgetStatus.setText(statusText.text);
        display.update();
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
        if (!widgetStatus.isDisposed()) widgetStatus.setText(string);
        display.update();
      }
    });
  }

  /** show error message in dialog
   */
  public void showError(final String message)
  {
    display.syncExec(new Runnable()
    {
      public void run()
      {
        Dialogs.error(shell,message);
      }
    });
  }

  /** get and decode password with master password
   * @param name name in database
   * @param inputFlag true to input missing password
   * @return password or null
   */
  public String getPassword(String name, boolean inputFlag)
  {
    String password = null;

    // read password from password database
    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      database = openPasswordDatabase();

      statement = database.createStatement();
      resultSet = null;
      try
      {
        preparedStatement = database.prepareStatement("SELECT data FROM passwords WHERE name=?;");
        preparedStatement.setString(1,name);
        resultSet = preparedStatement.executeQuery();

        if    (resultSet.next())
        {
          // read and decode password
          do
          {
            password = decodePassword((masterPassword != null)?masterPassword:"",resultSet.getBytes("data"));
            if (password == null)
            {
              if (masterPassword == null)
              {
                // get master password
                masterPassword = Dialogs.password(shell,"Master password","Master password:");
                if (masterPassword == null) return null;
              }
              else
              {
                // re-enter master password
                String reenteredMasterPassword = Dialogs.password(shell,"Master password","Cannot decrypt password. Wrong master password?","Re-enter master password:");
                if (reenteredMasterPassword != null)
                {
                  masterPassword = reenteredMasterPassword;
                }
                else
                {
                  return null;
                }
              }
            }
          }
          while (password == null);
        }
        else if (inputFlag)
        {
          // input password
          password = Dialogs.password(shell,"Password for: "+name);
          if (password == null) return null;

          // encode and store password
          byte[] encodedPassword = encodePassword(masterPassword,password);
          if (encodedPassword != null)
          {
            preparedStatement = database.prepareStatement("INSERT INTO passwords (name,data) VALUES (?,?);");
            preparedStatement.setString(1,name);
            preparedStatement.setBytes(2,encodedPassword);
            preparedStatement.executeUpdate();
          }
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

  /** get and decode password with master password
   * @param login login name
   * @param host host name
   * @param inputFlag true to input missing password
   * @return password or null
   */
  public String getPassword(String login, String host, boolean inputFlag)
  {
    return getPassword(login+"@"+host,inputFlag);
  }

  /** get and decode password with master password
   * @param login login name
   * @param host host name
   * @return password or null
   */
  public String getPassword(String login, String host)
  {
    return getPassword(login,host,true);
  }

  /** store password encode password with master password
   * @param name name
   * @param password to set
   */
  public void setPassword(String name, String password)
  {

    // get encoded password
    byte[] encodedPassword = encodePassword((masterPassword != null)?masterPassword:"",password);
    if (encodedPassword == null) return;

    // store password into password database
    Database database = null;
    try
    {
      PreparedStatement preparedStatement;

      database = openPasswordDatabase();

      // delete old password
      preparedStatement = database.prepareStatement("DELETE FROM passwords WHERE name=?;");
      preparedStatement.setString(1,name);
      preparedStatement.executeUpdate();

      // store password
      preparedStatement = database.prepareStatement("INSERT INTO passwords (name,data) VALUES (?,?);");
      preparedStatement.setString(1,name);
      preparedStatement.setBytes(2,encodedPassword);
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
   * @param login login name
   * @param host host name
   * @param password to set
   */
  public void setPassword(String login, String host, String password)
  {
    setPassword(login+"@"+host,password);
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
      PreparedStatement preparedStatement,preparedStatement1,preparedStatement2;

      database = openPasswordDatabase();

      // get encoded password
      statement = database.createStatement();
      resultSet = null;
      try
      {
        preparedStatement = database.prepareStatement("SELECT name,data FROM passwords;");
        resultSet = preparedStatement.executeQuery();

        boolean deleteAll = false;
        boolean skipAll   = false;
        preparedStatement1 = database.prepareStatement("UPDATE passwords SET data=? WHERE name=?;");
        preparedStatement2 = database.prepareStatement("DELETE FROM passwords WHERE name=?;");
        String name;
        String password;
        while (resultSet.next())
        {
          // read and decode with old password
          name     = resultSet.getString("name");
          password = decodePassword(oldMasterPassword,resultSet.getBytes("data"));
          if (password != null)
          {
            // encode with new password and store
            preparedStatement1.setBytes(1,encodePassword(newMasterPassword,password));
            preparedStatement1.setString(2,name);
            preparedStatement1.executeUpdate();
          }
          else if (deleteAll)
          {
            // delete password entry
            preparedStatement2.setString(1,name);
            preparedStatement2.executeUpdate();
          }
          else if (skipAll)
          {
            // skip password entry
          }
          else
          {
            switch (Dialogs.select(shell,"Confirmation","Cannot decode '"+name+"' with old master password!\nDelete password entry?",new String[]{"Delete","Delete all","Skip","Skip all","Cancel"},0))
            {
              case 0:
                // delete password entry
                preparedStatement2.setString(1,name);
                preparedStatement2.executeUpdate();
                break;
              case 1:
                // delete password entry
                preparedStatement2.setString(1,name);
                preparedStatement2.executeUpdate();

                deleteAll = true;
                break;
              case 2:
                // skip password entry
                break;
              case 3:
                // skip password entry
                skipAll = true;
                break;
              case 4:
                return;
            }
          }
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
      Dialogs.error(shell,"Cannot set new master password (error: %s).",exception.getMessage());
      return;
    }
    finally
    {
      if (database != null) closePasswordDatabase(database);
    }

    // set new master password
    masterPassword = newMasterPassword;
  }

  /** get repository list
   * @return repository list
   */
  public RepositoryList getRepositoryList()
  {
    return repositoryList;
  }

  /** get repository
   * @param rootPath repository root path
   * @return repository or null if not found
   */
  public Repository getRepository(String rootPath)
  {
    for (Repository repository : repositoryList)
    {
      if (repository.rootPath.equals(rootPath))
      {
        return repository;
      }
    }

    return null;
  }

  /** get repository tab
   * @param rootPath repository root path
   * @return repository tab or null if not found
   */
  public RepositoryTab getRepositoryTab(String rootPath)
  {
    return repositoryTabMap.get(getRepository(rootPath));
  }

  /** get all repository tabs
   * @return repository tabs
   */
  public RepositoryTab[] getRepositoryTabs()
  {
    Collection<RepositoryTab> repositoryTabs = repositoryTabMap.values();
    return repositoryTabs.toArray(new RepositoryTab[repositoryTabs.size()]);
  }

  /** get mime type of file
   * @param fileName file name
   * @return mime type or null
   */
  public static String getMimeType(String fileName)
  {
    return MIMETYPES_FILE_TYPE_MAP.getContentType(fileName);
  }

  /** get mime type of file
   * @param file file
   * @return mime type or null
   */
  public static String getMimeType(File file)
  {
    return MIMETYPES_FILE_TYPE_MAP.getContentType(file);
  }

  /** get file suffix association
   * @param suffix file suffix (Format: .xxx)
   * @return file suffix association or null
   */
  public static String getFileAssociation(String suffix)
  {
    return FILE_ASSOCIATION_MAP.get(suffix);
  }

   /** add new shell command to menu
   */
  public void addShellCommand()
  {
    /** dialog data
     */
    class Data
    {
     String name;
     String command;

      Data()
      {
        this.name    = null;
        this.command = null;
      }
    };

    final Data data = new Data();

    Composite composite;
    Label     label;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(shell,"Add shell command",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetName;
    final Text   widgetCommand;
    final Button widgetAddSave;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      widgetName = Widgets.newText(composite);
      Widgets.layout(widgetName,0,1,TableLayoutData.WE);
      widgetName.setToolTipText("Name of command");

      label = Widgets.newLabel(composite,"Command:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      widgetCommand = Widgets.newText(composite);
      Widgets.layout(widgetCommand,1,1,TableLayoutData.WE);
      widgetCommand.setToolTipText("Command to run.\nMacros:\n  %file% - file name\n  %% - %");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetAddSave = Widgets.newButton(composite,"Add");
      Widgets.layout(widgetAddSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetAddSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.name    = widgetName.getText().trim();
          data.command = widgetCommand.getText();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetCommand.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetCommand.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetAddSave.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // run dialog
    Widgets.setFocus(widgetName);
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // add shell command
      Settings.ShellCommand[] newShellCommands = new Settings.ShellCommand[Settings.shellCommands.length+1];
      System.arraycopy(Settings.shellCommands,0,newShellCommands,0,Settings.shellCommands.length);
      newShellCommands[newShellCommands.length-1] = new Settings.ShellCommand(data.name,data.command);
      Settings.shellCommands = newShellCommands;

      // sort
      Arrays.sort(Settings.shellCommands,new Comparator<Settings.ShellCommand>()
      {
        public int compare(Settings.ShellCommand shellCommand1, Settings.ShellCommand shellCommand2)
        {
          return shellCommand1.name.compareTo(shellCommand2.name);
        }
      });

      // update shell commands to menu
      updateShellCommands();
    }
  }

  //-----------------------------------------------------------------------

  /** static initializer
   */
  {
    // add known additional mime types
    MIMETYPES_FILE_TYPE_MAP.addMimeTypes("text/x-c c cpp c++");
    MIMETYPES_FILE_TYPE_MAP.addMimeTypes("text/x-h h hpp h++");
    MIMETYPES_FILE_TYPE_MAP.addMimeTypes("text/x-java java");

    // initialize file associations (Windows only)
    if (isWindowsSystem())
    {
      final Pattern PATTERN_ASSOC = Pattern.compile("^(.*)=(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_FTYPE = Pattern.compile("^(.*)=(.*)",Pattern.CASE_INSENSITIVE);

      Exec exec = null;
      try
      {
        Command command;
        String  line;
        Matcher matcher;

        // run Windows "assoc" command and collect file suffixes
        HashMap<String,String> fileSuffixMap = new HashMap<String,String>();
        command = new Command("cmd.exe","/C","assoc");
        exec = new Exec(command);
        while ((line = exec.getStdout()) != null)
        {
          if ((matcher = PATTERN_ASSOC.matcher(line)).matches())
          {
            String suffix = matcher.group(1).toLowerCase();
            String name   = matcher.group(2);
            fileSuffixMap.put(name,suffix);
          }
        }
        exec.done(); exec = null;
//for (String s : fileSuffixMap.keySet()) Dprintf.dprintf("%s -> %s",s,fileSuffixMap.get(s));

        // run Windows "ftype" command and collect associated programs
        command = new Command("cmd.exe","/C","ftype");
        exec = new Exec(command);
        while ((line = exec.getStdout()) != null)
        {
          if ((matcher = PATTERN_FTYPE.matcher(line)).matches())
          {
            String name        = matcher.group(1);
            String commandLine = matcher.group(2);
            if (fileSuffixMap.containsKey(name))
            {
              FILE_ASSOCIATION_MAP.put(fileSuffixMap.get(name),commandLine);
            }
          }
        }
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
Dprintf.dprintf("ex=%s",exception);
      }
      finally
      {
        if (exec != null) exec.done();
      }
//for (String s : FILE_ASSOCIATION_MAP.keySet()) Dprintf.dprintf("s=%s -> %s",s,FILE_ASSOCIATION_MAP.get(s));
    }
  }

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

  /** check if system is Windows system
   * @return TRUE iff Windows, FALSE otherwise
   */
  private static boolean isWindowsSystem()
  {
    String osName = System.getProperty("os.name").toLowerCase();

    return (osName.indexOf("win") >= 0);
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
    IMAGE_LOCK        = Widgets.loadImage(display,"lock.png");
    IMAGE_EMPTY       = Widgets.loadImage(display,"empty.png");

    // fonts
    FONT_CHANGES      = Widgets.newFont(display,Settings.fontChanges);
    FONT_DIFF         = Widgets.newFont(display,Settings.fontDiff);
    FONT_DIFF_LINE    = Widgets.newFont(display,Settings.fontDiffLine);

    // get cursors
    CURSOR_WAIT       = new Cursor(display,SWT.CURSOR_WAIT);
  }

  /** init loaded classes/JARs watchdog
   */
  private void initClassesWatchDog()
  {
    // get timestamp of all classes/JAR files
    final HashMap<File,Long> classModifiedMap = new HashMap<File,Long>();
    LinkedList<File> directoryList = new LinkedList<File>();
    for (String name : System.getProperty("java.class.path").split(File.pathSeparator))
    {
      File file = new File(name);
      if (file.isDirectory())
      {
        directoryList.add(file);
      }
      else
      {
        classModifiedMap.put(file,new Long(file.lastModified()));
      }
    }
    while (directoryList.size() > 0)
    {
      File directory = directoryList.removeFirst();
      File[] files = directory.listFiles();
      if (files != null)
      {
        for (File file : files)
        {
          if (file.isDirectory())
          {
            directoryList.add(file);
          }
          else
          {
            classModifiedMap.put(file,new Long(file.lastModified()));
          }
        }
      }
    }

    // periodically check timestamp of classes/JAR files
    Thread classWatchDogThread = new Thread()
    {
      public void run()
      {
        final long REMINDER_TIME = 5*60*1000;

        long            lastRemindedTimestamp = 0L;
        final boolean[] reminderFlag          = new boolean[]{true};

        for (;;)
        {
          // check timestamps, show warning dialog
          for (final File file : classModifiedMap.keySet())
          {
            if (   reminderFlag[0]
                && (file.lastModified() > classModifiedMap.get(file))
                && (System.currentTimeMillis() > (lastRemindedTimestamp+REMINDER_TIME))
               )
            {
//Dprintf.dprintf("file=%s %d -> %d",file,file.lastModified(),classModifiedMap.get(file));
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  switch (Dialogs.select(shell,"Warning","Class/JAR file '"+file.getName()+"' changed. Is is recommended to restart Onzen now.",new String[]{"Restart","Remind me again in 5min","Ignore"},0))
                  {
                    case 0:
                      // send close event with restart
                      Widgets.notify(shell,SWT.Close,EXITCODE_RESTART);
                      break;
                    case 1:
                      break;
                    case 2:
                      reminderFlag[0] = false;
                      break;
                  }
                }
              });
              lastRemindedTimestamp = System.currentTimeMillis();
            }
          }

          // sleep a short time
          try { Thread.sleep(30*1000); } catch (InterruptedException exception) { /* ignored */ }
        }
      }
    };
    classWatchDogThread.setDaemon(true);
    classWatchDogThread.start();
  }

  /** create main window
   */
  private void createWindow()
  {
    Composite composite;
    Button    button;
    Label     label;

    // create window
    shell = new Shell(display,SWT.SHELL_TRIM);
    shell.setText("Onzen");
    shell.setLayout(new TableLayout(new double[]{1.0,0.0,0.0},1.0));

    // create tab
    widgetTabFolder = Widgets.newTabFolder(shell);
    Widgets.layout(widgetTabFolder,0,0,TableLayoutData.NSWE);
    widgetTabFolder.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        TabFolder tabFolder = (TabFolder)mouseEvent.widget;

        /* Note: it is not possible to add a mouse-double-click handler to
          a tab-item nor a tab-folder and the correct tab-item is returned
          when the tab-items a scrolled. Thus use the following work-around:
            - get offset of first item = width of scroll buttons left and right
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
            RepositoryTab repositoryTab = (RepositoryTab)selectedTabItems[0].getData();
            if (editRepository(repositoryTab))
            {
              repositoryTab.updateStates();
            }
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
      widgetButtonUpdate = Widgets.newButton(composite,"Update",Settings.keyUpdate);
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

      widgetButtonCommit = Widgets.newButton(composite,"Commit",Settings.keyCommit);
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

      widgetButtonCreatePatch = Widgets.newButton(composite,"Patch",Settings.keyCreatePatch);
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

      widgetButtonAdd = Widgets.newButton(composite,"Add",Settings.keyAdd);
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

      widgetButtonRemove = Widgets.newButton(composite,"Remove",Settings.keyRemove);
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

      widgetButtonRevert = Widgets.newButton(composite,"Revert",Settings.keyRevert);
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

      widgetButtonDiff = Widgets.newButton(composite,"Diff",Settings.keyDiff);
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

      widgetButtonRevisions = Widgets.newButton(composite,"Revisions",Settings.keyRevisions);
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

      widgetButtonSolve = Widgets.newButton(composite,"Resolve",Settings.keyResolve);
// NYI
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
      menuItem = Widgets.addMenuItem(menu,"New repository\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          newRepository();
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Check-out repository\u2026",Settings.keyCheckoutRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          checkoutRepository();
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Open repository\u2026",Settings.keyOpenRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          String rootPath = Dialogs.directory(shell,
                                              "Open repository",
                                              (selectedRepositoryTab != null) ? selectedRepositoryTab.repository.rootPath : ""
                                             );
          if (rootPath != null)
          {
            openRepository(rootPath);
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Edit repository list\u2026",Settings.keyEditRepositoryList);
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

      menuItem = Widgets.addMenuItem(menu,"Edit repository\u2026",Settings.keyEditRepository);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            if (editRepository(selectedRepositoryTab))
            {
              selectedRepositoryTab.updateStates();
            }
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Close repository\u2026",Settings.keyCloseRepository);
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

      menuItem = Widgets.addMenuItem(menu,"New repository list\u2026");
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

      menuItem = Widgets.addMenuItem(menu,"Load repository list\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          repositoryListName = loadRepositoryList();
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
          Widgets.notify(shell,SWT.Close,EXITCODE_RESTART);
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Quit",Settings.keyQuit);
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

      menuItem = Widgets.addMenuItem(menu,"Update all",Settings.keyUpdateAll);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.updateAll();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Commit\u2026",Settings.keyCommit);
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

      menuItem = Widgets.addMenuItem(menu,"Create patch\u2026",Settings.keyCreatePatch);
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

      menuItem = Widgets.addMenuItem(menu,"Patches\u2026",Settings.keyPatches);
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

      menuItem = Widgets.addMenuItem(menu,"Revert\u2026",Settings.keyRevert);
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

      menuItem = Widgets.addMenuItem(menu,"Resolve\u2026",Settings.keyResolve);
// NYI
menuItem.setEnabled(false);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.resolve();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Set resolved",Settings.keySetResolved);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.setResolved();
          }
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

      menuItemLock = Widgets.addMenuItem(menu,"Lock",Settings.keyLock);
      menuItemLock.setEnabled(false);
      menuItemLock.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.lock();
          }
        }
      });

      menuItemUnlock = Widgets.addMenuItem(menu,"Unlock",Settings.keyUnlock);
      menuItemUnlock.setEnabled(false);
      menuItemUnlock.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.unlock();
          }
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItemIncomingChanges = Widgets.addMenuItem(menu,"Incoming changes",Settings.keyIncomingChanges);
      menuItemIncomingChanges.setEnabled(false);
      menuItemIncomingChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.incomingChanges();
          }
        }
      });

      menuItemIncomingChangesFrom = Widgets.addMenuItem(menu,"Incoming changes from\u2026",Settings.keyIncomingChangesFrom);
      menuItemIncomingChangesFrom.setEnabled(false);
      menuItemIncomingChangesFrom.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path for incoming changes",
                                            ((RepositoryHG)selectedRepositoryTab.repository).masterRepository
                                           );
            if (path != null)
            {
              selectedRepositoryTab.incomingChanges(path);
            }
          }
        }
      });

      menuItemOutgoingChanges = Widgets.addMenuItem(menu,"Outgoing changes",Settings.keyOutgoingChanges);
      menuItemOutgoingChanges.setEnabled(false);
      menuItemOutgoingChanges.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.outgoingChanges();
          }
        }
      });

      menuItemOutgoingChangesTo = Widgets.addMenuItem(menu,"Outgoing changes to\u2026",Settings.keyOutgoingChangesTo);
      menuItemOutgoingChangesTo.setEnabled(false);
      menuItemOutgoingChangesTo.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path for outgoing changes",
                                            ((RepositoryHG)selectedRepositoryTab.repository).masterRepository
                                           );
            if (path != null)
            {
              selectedRepositoryTab.outgoingChanges(path);
            }
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

      menuItemPullChangesFrom = Widgets.addMenuItem(menu,"Pull changes from\u2026",Settings.keyPullChangesFrom);
      menuItemPullChangesFrom.setEnabled(false);
      menuItemPullChangesFrom.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path for pull changes",
                                            ((RepositoryHG)selectedRepositoryTab.repository).masterRepository
                                           );
            if (path != null)
            {
              selectedRepositoryTab.pullChanges(path);
            }
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

      menuItemPushChangesTo = Widgets.addMenuItem(menu,"Push changes to\u2026",Settings.keyPushChangesTo);
      menuItemPushChangesTo.setEnabled(false);
      menuItemPushChangesTo.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path for push changes",
                                            ((RepositoryHG)selectedRepositoryTab.repository).masterRepository
                                           );
            if (path != null)
            {
              selectedRepositoryTab.pushChanges(path);
            }
          }
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Add\u2026",Settings.keyAdd);
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

      menuItem = Widgets.addMenuItem(menu,"Remove\u2026",Settings.keyRemove);
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

      menuItem = Widgets.addMenuItem(menu,"Rename\u2026",Settings.keyRename);
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

      menuSetFileMode = Widgets.addMenuItem(menu,"Set file mode\u2026",Settings.keySetFileMode);
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

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"New branch\u2026",Settings.keyNewBranch);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.newBranch();
          }
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"View");
    {
      menuItem = Widgets.addMenuItem(menu,"Revision info\u2026",Settings.keyRevisionInfo);
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

      menuItem = Widgets.addMenuItem(menu,"Revisions\u2026",Settings.keyRevisions);
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

      menuItem = Widgets.addMenuItem(menu,"Diff\u2026",Settings.keyDiff);
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

      menuItem = Widgets.addMenuItem(menu,"Changed files\u2026",Settings.keyChangedFiles);
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

      menuItem = Widgets.addMenuItem(menu,"Annotations\u2026",Settings.keyAnnotations);
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

      menuItem = Widgets.addMenuItem(menu,"Resolve\u2026",Settings.keyResolve);
// NYI
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

      menuItem = Widgets.addMenuItem(menu,"Set resolved",Settings.keySetResolved);
// NYI
menuItem.setEnabled(false);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
Dprintf.dprintf("NYI");
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"File/Directory");
    {
      menuItem = Widgets.addMenuItem(menu,"Open file\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.openFile();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Open file with\u2026",Settings.keyOpenFileWith);
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

      menuItem = Widgets.addMenuItem(menu,"New file\u2026",Settings.keyNewFile);
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

      menuItem = Widgets.addMenuItem(menu,"New directory\u2026",Settings.keyNewDirectory);
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

      menuItem = Widgets.addMenuItem(menu,"Rename local file/directory\u2026",Settings.keyRenameLocal);
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

      menuItem = Widgets.addMenuItem(menu,"Delete local files/directories\u2026",Settings.keyDeleteLocal);
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

      menuItem = Widgets.addMenuItem(menu,"Add to file ignore list",Settings.keyAddIgnoreFile);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.addIgnoreFiles();
            selectedRepositoryTab.updateStates();
          }
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Find files by name\u2026",Settings.keyFindFilesByName);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.findFilesByName();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Find in files\u2026",Settings.keyFindFilesByContent);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.findFilesByContent();
          }
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Copy to\u2026",Settings.keyCopyFilesTo);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          selectedRepositoryTab.copyFilesTo();
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Convert whitespaces\u2026",Settings.keyConvertWhitespaces);
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.convertWhitespaces();
          }
        }
      });
    }

    menuShellCommands = Widgets.addMenu(menuBar,"Shell");
    {
      Widgets.addMenuSeparator(menuShellCommands);
      menuItem = Widgets.addMenuItem(menuShellCommands,"Add new command\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          addShellCommand();
        }
      });
   }

    menuRepositories = Widgets.addMenu(menuBar,"Repositories");
    {
    }

    menu = Widgets.addMenu(menuBar,"Options");
    {
      menuItem = Widgets.addMenuItem(menu,"Preferences\u2026");
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

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"New master password\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editMasterPassword();
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

    // update shell commands in menu
    updateShellCommands();
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
    // init display
    initDisplay();

    // add watchdog for loaded classes/JARs
    initClassesWatchDog();

    // open main window
    createWindow();
    createMenu();
    createEventHandlers();

    // add empty repository tab
    addRepositoryTabEmpty();

    // init commit messages
    try
    {
      CommitMessage.init();
    }
    catch (SQLException exception)
    {
      Dialogs.warning(shell,"Cannot load commit message history from database (error: %s)",exception.getMessage());
    }
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
    shell.addListener(SWT.Close,new Listener()
    {
      public void handleEvent(Event event)
      {
        // store geometry
        Settings.geometryMain = shell.getSize();

        // save repository list
        saveRepositoryList();

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

  class DatabasePassword implements Serializable
  {
    String password;
    long   checkSum;

    DatabasePassword()
    {
    }

    DatabasePassword(String password)
    {
      Adler32 adler32 = new Adler32();
      adler32.update(password.getBytes());

      this.password = password;
      this.checkSum = adler32.getValue();
    }
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
      statement = database.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = database.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS passwords ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  name     TEXT, "+
                              "  data     BLOB "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = database.createStatement();
      resultSet = null;
      try
      {
        resultSet = statement.executeQuery("SELECT name,value FROM meta;");

        if (!resultSet.next())
        {
          preparedStatement = database.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
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
   * @param masterPassword master password
   * @param password password to encode
   * @return encoded password bytes or null
   */
  private byte[] encodePassword(String masterPassword, String password)
  {
    byte[] encodedPassword;

    try
    {
      // calculate password checksum
      Adler32 adler32 = new Adler32();
      adler32.update(password.getBytes());

      // serialize
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(password);
      objectOutputStream.writeObject(adler32.getValue());
      byte[] dataBytes = byteArrayOutputStream.toByteArray();

      if ((masterPassword != null) && !masterPassword.isEmpty())
      {
        // crypt with master password

        // get secret key
        byte[] masterPasswordBytes = masterPassword.getBytes();
        byte[] bytes = new byte[128/8];
        for (int z = 0; z < bytes.length; z++)
        {
          bytes[z] = masterPasswordBytes[z%masterPasswordBytes.length];
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(bytes,"AES");

        // get cipher
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec);

        // encrypt password data
        encodedPassword = cipher.doFinal(Arrays.copyOf(dataBytes,(dataBytes.length+15)/16*16));
      }
      else
      {
        // no encryption
        encodedPassword = dataBytes;
      }
    }
    catch (Exception exception)
    {
//Dprintf.dprintf("exception=%s",exception); exception.printStackTrace();
      return null;
    }

    return encodedPassword;
  }

  /** decode password with master password
   * @param masterPassword master password
   * @param encodedPassword encoded password bytes
   * @return decoded password or null
   */
  private String decodePassword(String masterPassword, byte[] encodedPassword)
  {
    String password;

    if (encodedPassword != null)
    {
      try
      {
        byte[] dataBytes;

        if ((masterPassword != null) && !masterPassword.isEmpty())
        {
          // decrypt with master password

          // get secret key
          byte[] masterPasswordBytes = masterPassword.getBytes();
          byte[] bytes = new byte[128/8];
          for (int z = 0; z < bytes.length; z++)
          {
            bytes[z] = masterPasswordBytes[z%masterPasswordBytes.length];
          }
          SecretKeySpec secretKeySpec = new SecretKeySpec(bytes,"AES");

          // get cipher
          Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
          cipher.init(Cipher.DECRYPT_MODE,secretKeySpec);

          // decrypt password data
          dataBytes = cipher.doFinal(encodedPassword);
        }
        else
        {
          // no encryption
          dataBytes = encodedPassword;
        }

        // deserialize password, checksum
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataBytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        password = (String)objectInputStream.readObject();
        long  checksum  = (Long)objectInputStream.readObject();

        // check if checksum is valid
        Adler32 adler32 = new Adler32();
        adler32.update(password.getBytes());
        if (adler32.getValue() != checksum) return null;
      }
      catch (Exception exception)
      {
//Dprintf.dprintf("exception=%s",exception); exception.printStackTrace();
        return null;
      }
    }
    else
    {
      password = "";
    }

    return password;
  }

  /** update shell commands to menu
   */
  private void updateShellCommands()
  {
    if (!menuShellCommands.isDisposed())
    {
      // remove old entries in shell command menu
      MenuItem[] menuItems = menuShellCommands.getItems();
      for (int i = 0; i < menuItems.length; i++)
      {
        menuItems[i].dispose();
      }

      // add new shell commands to menu
      for (Settings.ShellCommand shellCommand : Settings.shellCommands)
      {
        MenuItem menuItem = Widgets.addMenuItem(menuShellCommands,shellCommand.name);
        menuItem.setData(shellCommand);
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            MenuItem              widget       = (MenuItem)selectionEvent.widget;
            Settings.ShellCommand shellCommand = (Settings.ShellCommand)widget.getData();

            selectedRepositoryTab.executeShellCommand(shellCommand);
          }
        });
      }
      Widgets.addMenuSeparator(menuShellCommands);
      MenuItem menuItem = Widgets.addMenuItem(menuShellCommands,"Add new command\u2026");
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          addShellCommand();
        }
      });
    }

    // update context menu in repository tabs
    for (RepositoryTab repositoryTab : repositoryTabMap.values())
    {
      repositoryTab.updateShellCommands();
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

      // update context menu in repository tabs
      repositoryTab.updateShellCommands();
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

    // update repository menu entries
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

    if (selectedRepositoryTab != null)
    {
      // insert into list
      repositoryList.insert(selectedRepositoryTab.repository,repository);
    }
    else
    {
      // add to list
      repositoryList.add(repository);
    }

    // add tab, set default selected tab
    repositoryTab = new RepositoryTab(this,
                                      widgetTabFolder,
                                      selectedRepositoryTab,
                                      repository
                                     );
    repositoryTabMap.put(repository,repositoryTab);

    // update context menu in repository tabs
    repositoryTab.updateShellCommands();

    // update repository menu entries
    MenuItem menuItem = Widgets.addMenuItem(menuRepositories,repository.title);
    menuItem.setData(repositoryTab);
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
      Dialogs.error(shell,"Cannot store repository list (error: %s).",exception.getMessage());
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
    MenuItem[] menuItems = menuRepositories.getItems();
    for (MenuItem menuItem : menuRepositories.getItems())
    {
      if (menuItem.getData() == repositoryTab)
      {
        menuItem.dispose();
        break;
      }
    }

    // save list
    try
    {
      repositoryList.save();
    }
    catch (IOException exception)
    {
      Dialogs.error(shell,"Cannot store repository list (error: %s).",exception.getMessage());
    }
  }

  /** get repository list name
   * @param title title text
   * @param openButton true to display "open" button
   * @return repository list name or null
   */
  private String getRepositoryListName(String title, final boolean openButton)
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
    dialog = Dialogs.openModal(shell,title,300,300,new double[]{1.0,0.0},1.0);

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
        widgetNewName.setToolTipText("Name of repository list to create.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      int column = 0;

      if (openButton)
      {
        widgetOpen = Widgets.newButton(composite,"Open");
        widgetOpen.setEnabled(false);
        Widgets.layout(widgetOpen,0,column,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        widgetOpen.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            int index = widgetNames.getSelectionIndex();
            if (index >= 0)
            {
              data.name = data.names[index];

              Dialogs.close(dialog,true);
            }
          }
        });
        widgetOpen.setToolTipText("Open selected repository list.");
        column++;
      }
      else
      {
        widgetOpen = null;
      }

      widgetNew = Widgets.newButton(composite,"New");
      widgetNew.setEnabled(false);
      Widgets.layout(widgetNew,0,column,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetNew.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.name = widgetNewName.getText().trim();

          Dialogs.close(dialog,true);
        }
      });
      column++;

      widgetDelete = Widgets.newButton(composite,"Delete");
      widgetDelete.setEnabled(false);
      Widgets.layout(widgetDelete,0,column,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetDelete.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          int index = widgetNames.getSelectionIndex();
          if (index >= 0)
          {
            if (Dialogs.confirm(dialog,String.format("Really delete repository list '%s'?",data.names[index])))
            {
              try
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
                if (openButton) widgetOpen.setEnabled(false);
                widgetNew.setEnabled(false);
                widgetDelete.setEnabled(false);
              }
              catch (IOException exception)
              {
                Dialogs.error(shell,"Cannot delete repository list (error: "+exception.getMessage()+")");
              }
            }
          }
        }
      });
      column++;

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
      column++;
    }

    // listeners
    widgetNames.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          if (openButton)
          {
            Widgets.invoke(widgetOpen);
          }
          else
          {
            if (openButton) widgetOpen.setEnabled(true);
            if (data.names[index] != null) widgetNewName.setText(data.names[index]);
            Widgets.setFocus(widgetNewName);

            String  newName = widgetNewName.getText().trim();
            boolean newNameEmptyFlag = newName.isEmpty();
            boolean newNameExistsFlag = Arrays.asList(widgetNames.getItems()).contains(newName);
            widgetNew.setEnabled(!newNameEmptyFlag && !newNameExistsFlag);
            widgetDelete.setEnabled(newNameExistsFlag);
          }
        }
        else
        {
          if (openButton) widgetOpen.setEnabled(false);
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        List widget = (List)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          if (openButton) widgetOpen.setEnabled(true);
          if (data.names[index] != null) widgetNewName.setText(data.names[index]);
          Widgets.setFocus(widgetNewName);

          String  newName = widgetNewName.getText().trim();
          boolean newNameEmptyFlag = newName.isEmpty();
          boolean newNameExistsFlag = Arrays.asList(widgetNames.getItems()).contains(newName);
          widgetNew.setEnabled(!newNameEmptyFlag && !newNameExistsFlag);
          widgetDelete.setEnabled(newNameExistsFlag);
        }
        else
        {
          if (openButton) widgetOpen.setEnabled(false);
        }
      }
    });
    widgetNewName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = widgetNames.getSelectionIndex();
        if (index >= 0)
        {
          if (openButton)
          {
            data.name = data.names[index];
          }
          else
          {
            data.name = widgetNewName.getText().trim();
          }
          if (!data.name.isEmpty())
          {
            Dialogs.close(dialog,true);
          }
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetNewName.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (keyEvent.keyCode == SWT.ARROW_UP)
        {
          if (widgetNames.getItemCount() > 0)
          {
            int index = widgetNames.getSelectionIndex();
            if      (index < 0) index = 0;
            else if (index > 0) index--;
            widgetNames.setSelection(index,index);
            widgetOpen.setEnabled(true);
          }
          else
          {
            widgetOpen.setEnabled(false);
          }
        }
        else if (keyEvent.keyCode == SWT.ARROW_DOWN)
        {
          if (widgetNames.getItemCount() > 0)
          {
            int index = widgetNames.getSelectionIndex();
            if (index < widgetNames.getItemCount()-1) index++;
            widgetNames.setSelection(index,index);
          }
          else
          {
            widgetOpen.setEnabled(false);
          }
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        Text widget = (Text)keyEvent.widget;

        String  newName = widget.getText().trim();
        boolean newNameEmptyFlag = newName.isEmpty();
        boolean newNameExistsFlag = Arrays.asList(widgetNames.getItems()).contains(newName);
        widgetOpen.setEnabled(newNameExistsFlag);
        widgetNew.setEnabled(!newNameEmptyFlag && !newNameExistsFlag);
        widgetDelete.setEnabled(newNameExistsFlag);
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.setWindowLocation);

    // add names
    for (String name : data.names)
    {
      widgetNames.add(name);
    }

    // run
    widgetNewName.setFocus();
    if ((Boolean)Dialogs.run(dialog,false) && (data.name != null) && !data.name.isEmpty())
    {
      return data.name;
    }
    else
    {
      return null;
    }
  }

  /** get new repository list name
   * @return repository list name or null
   */
  private String getNewRepositoryListName()
  {
    return getRepositoryListName("New repository",false);
  }

  /** get repository list name
   * @return repository list name or null
   */
  private String getRepositoryListName()
  {
    return getRepositoryListName("Select repository list",true);
  }

  /** create new repository list
   */
  private void newRepositoryList()
  {
    String name = getNewRepositoryListName();
    if (repositoryListName != null)
    {
      repositoryListName = name;
      setRepositoryList(new RepositoryList(repositoryListName));
    }
  }

  /** load repository list from file
   * @param repositoryListName name of repository list
   * @return repository list name or null
   */
  private String loadRepositoryList(String repositoryListName)
  {
    // get repository list name (if not already specified)
    if (repositoryListName == null)
    {
      repositoryListName = getRepositoryListName();
    }

    // add repositories from list
    setRepositoryList(new RepositoryList(repositoryListName));

    return repositoryListName;
  }

  /** load repository list from file
   * @return repository list name or null
   */
  private String loadRepositoryList()
  {
    return loadRepositoryList(null);
  }

  /** save repository list to file
   */
  private void saveRepositoryList()
  {
    // get repository list name
    if ((repositoryListName == null) && (repositoryList.size() > 0))
    {
      boolean retryFlag = true;
      do
      {
        if (Dialogs.confirm(shell,"Respository list is not empty and not stored. Store it?",true))
        {
          repositoryListName = getNewRepositoryListName();
        }
        else
        {
          retryFlag = false;
        }
      }
      while ((repositoryListName == null) && retryFlag);
    }

    // store repository list
    if (repositoryListName != null)
    {
      try
      {
         repositoryList.save(repositoryListName);
      }
      catch (IOException exception)
      {
        printError("Cannot store repository list (error: %s)",exception.getMessage());
      }
    }
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
    dialog = Dialogs.openModal(shell,"Edit repository list",new double[]{1.0,0.0},1.0);

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
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,1.0,1.0,0.0,0.0}));
      Widgets.layout(subComposite,1,0,TableLayoutData.WE);
      {
        button = Widgets.newButton(subComposite,"Open");
        Widgets.layout(button,0,0,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String rootPath = Dialogs.directory(shell,
                                                "Open repository",
                                                (selectedRepositoryTab != null) ? selectedRepositoryTab.repository.rootPath : ""
                                               );
            if (rootPath != null)
            {
              Repository repository = openRepository(rootPath);

              widgetList.add(repository.title);
            }
          }
        });
        button.setToolTipText("Open repository.");

        button = Widgets.newButton(subComposite,"Edit");
        Widgets.layout(button,0,1,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if (index >= 0)
            {
              RepositoryTab repositoryTab = repositoryTabMap.get(repositoryList.get(index));
              if (editRepository(repositoryTab))
              {
                widgetList.setItem(index,repositoryTab.repository.title);
                repositoryTab.updateStates();
              }
            }
          }
        });
        button.setToolTipText("Edit repository settings.");

        button = Widgets.newButton(subComposite,"Close");
        Widgets.layout(button,0,2,TableLayoutData.WE,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            int index = widgetList.getSelectionIndex();
            if (index >= 0)
            {
              RepositoryTab repositoryTab = repositoryTabMap.get(repositoryList.get(index));
              closeRepository(repositoryTab);
            }
          }
        });
        button.setToolTipText("Close repository.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        Widgets.layout(button,0,3,TableLayoutData.NSW,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        Widgets.layout(button,0,4,TableLayoutData.NSW,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
      button = Widgets.newButton(composite,"Done");
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
        if (index >= 0)
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
    Dialogs.show(dialog,Settings.geometryEditRepositoryList,Settings.setWindowLocation);

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
      Dialogs.error(shell,"Cannot store repository list (error: %s).",exception.getMessage());
    }
  }

  /** create new repository
   */
  private void newRepository()
  {
    /** file data comparator
     */
    class FileDataComparator implements Comparator<FileData>
    {
      // Note: enum in inner classes are not possible in Java, thus use the old way...
      private final static int SORTMODE_NAME = 0;
      private final static int SORTMODE_TYPE = 1;
      private final static int SORTMODE_SIZE = 2;
      private final static int SORTMODE_DATE = 3;

      private int sortMode;

      /** create file data comparator
       * @param sortMode sort mode
       */
      FileDataComparator(int sortMode)
      {
        this.sortMode = sortMode;
      }

      /** create file data comparator
       * @param tree file tree
       * @param sortColumn column to sort
       */
      FileDataComparator(Tree tree, TreeColumn sortColumn)
      {
        if      (tree.getColumn(0) == sortColumn) sortMode = SORTMODE_NAME;
        else if (tree.getColumn(1) == sortColumn) sortMode = SORTMODE_TYPE;
        else if (tree.getColumn(2) == sortColumn) sortMode = SORTMODE_SIZE;
        else if (tree.getColumn(3) == sortColumn) sortMode = SORTMODE_DATE;
        else                                      sortMode = SORTMODE_NAME;
      }

      /** create file data comparator
       * @param tree file tree
       */
      FileDataComparator(Tree tree)
      {
        this(tree,tree.getSortColumn());
      }

      /** compare file tree data without taking care about type
       * @param fileData1, fileData2 file tree data to compare
       * @return -1 iff fileData1 < fileData2,
                  0 iff fileData1 = fileData2,
                  1 iff fileData1 > fileData2
       */
      private int compareWithoutType(FileData fileData1, FileData fileData2)
      {
        switch (sortMode)
        {
          case SORTMODE_NAME:
            return fileData1.name.compareTo(fileData2.name);
          case SORTMODE_TYPE:
            return fileData1.type.compareTo(fileData2.type);
          case SORTMODE_SIZE:
            if      (fileData1.size < fileData2.size) return -1;
            else if (fileData1.size > fileData2.size) return  1;
            else                                      return  0;
          case SORTMODE_DATE:
            if      (fileData1.date.before(fileData2.date)) return -1;
            else if (fileData1.date.after(fileData2.date))  return  1;
            else                                            return  0;
          default:
            return 0;
        }
      }

      /** compare file tree data
       * @param fileData1, fileData2 file tree data to compare
       * @return -1 iff fileData1 < fileData2,
                  0 iff fileData1 = fileData2,
                  1 iff fileData1 > fileData2
       */
      public int compare(FileData fileData1, FileData fileData2)
      {
        if ((fileData1 != null) && (fileData2 != null))
        {
          if (fileData1.type == FileData.Types.DIRECTORY)
          {
            if (fileData2.type == FileData.Types.DIRECTORY)
            {
              return compareWithoutType(fileData1,fileData2);
            }
            else
            {
              return -1;
            }
          }
          else
          {
            if (fileData2.type == FileData.Types.DIRECTORY)
            {
              return 1;
            }
            else
            {
              return compareWithoutType(fileData1,fileData2);
            }
          }
        }
        else
        {
          return 0;
        }
      }

      /** convert data to string
       * @return string
       */
      public String toString()
      {
        return "FileDataComparator {"+sortMode+"}";
      }
    }

    /** dialog data
     */
    class Data
    {
      Repository.Types type;
      String           repositoryPath;
      String           moduleName;
      String           destinationPath;
      boolean          quitFlag;
      String           importPath;
      String           excludePatterns;

      Data()
      {
        this.type            = Repository.Types.CVS;
        this.repositoryPath  = null;
        this.moduleName      = null;
        this.destinationPath = null;
        this.quitFlag        = false;
        this.importPath      = null;
        this.excludePatterns = null;
      }
    }

    Composite    composite,subComposite,subSubComposite;
    Button       button;
    Label        label;
    TreeColumn   treeColumn;

    final Data   data   = new Data();
    final Shell  dialog = Dialogs.openModal(shell,"Create new repository",500,500,new double[]{1.0,0.0},1.0);

    final Combo  widgetRepository;
    final Combo  widgetModuleName;
    final Text   widgetDestinationPath;
    final Text   widgetImportPath;
    final Tree   widgetFileTree;
    final Text   widgetExcludePatterns;
    final Button widgetCreate;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,1.0},new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Type:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,null));
      Widgets.layout(subComposite,0,1,TableLayoutData.W);
      {
        button = Widgets.newRadio(subComposite,"CVS");
        button.setSelection(true);
        Widgets.layout(button,0,0,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection()) data.type = Repository.Types.CVS;
          }
        });

        button = Widgets.newRadio(subComposite,"SVN");
        button.setSelection(false);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection()) data.type = Repository.Types.SVN;
          }
        });

        button = Widgets.newRadio(subComposite,"HG");
        button.setSelection(false);
        Widgets.layout(button,0,2,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection()) data.type = Repository.Types.HG;
          }
        });

        button = Widgets.newRadio(subComposite,"GIT");
        button.setSelection(false);
        Widgets.layout(button,0,3,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection()) data.type = Repository.Types.GIT;
          }
        });
      }

      label = Widgets.newLabel(composite,"Repository:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.NSWE);
      {
        widgetRepository = Widgets.newCombo(subComposite);
        widgetRepository.setText("file://");
        Widgets.layout(widgetRepository,0,0,TableLayoutData.WE);
        widgetRepository.setToolTipText("Respository path URI.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path",
                                            widgetRepository.getText()
                                           );
            if (path != null)
            {
              widgetRepository.setText(path);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Module:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetModuleName = Widgets.newCombo(composite);
      Widgets.layout(widgetModuleName,2,1,TableLayoutData.WE);
      widgetModuleName.setToolTipText("Module name in repository.");

      label = Widgets.newLabel(composite,"Destination:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,3,1,TableLayoutData.NSWE);
      {
        widgetDestinationPath = Widgets.newText(subComposite);
        Widgets.layout(widgetDestinationPath,0,0,TableLayoutData.WE);
        widgetDestinationPath.setToolTipText("Destination check-out directory path.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                           "Select destination directory path",
                                            widgetDestinationPath.getText()
                                           );
            if (path != null)
            {
              widgetDestinationPath.setText(path);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Import from:");
      Widgets.layout(label,4,0,TableLayoutData.NW);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0},1.0));
      Widgets.layout(subComposite,4,1,TableLayoutData.NSWE);
      {
        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.NSWE);
        {
          widgetImportPath = Widgets.newText(subSubComposite,SWT.SEARCH|SWT.ICON_CANCEL);
          Widgets.layout(widgetImportPath,0,0,TableLayoutData.WE);
          widgetImportPath.setToolTipText("Import directory path.");

          button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
          Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String path = Dialogs.directory(shell,
                                             "Select import directory path",
                                              widgetImportPath.getText()
                                             );
              if (path != null)
              {
                widgetImportPath.setText(path);
              }
            }
          });
        }

        widgetFileTree = Widgets.newTree(subComposite,SWT.MULTI);
        widgetFileTree.setBackground(Onzen.COLOR_GRAY);
        Widgets.layout(widgetFileTree,1,0,TableLayoutData.NSWE);
        SelectionListener selectionListener = new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            TreeColumn         treeColumn         = (TreeColumn)selectionEvent.widget;
            FileDataComparator fileDataComparator = new FileDataComparator(widgetFileTree,treeColumn);

            synchronized(widgetFileTree)
            {
              Widgets.sortTreeColumn(widgetFileTree,treeColumn,fileDataComparator);
            }
          }
        };
        treeColumn = Widgets.addTreeColumn(widgetFileTree,"Name",        SWT.LEFT, 390,true );
        treeColumn.addSelectionListener(selectionListener);
        treeColumn = Widgets.addTreeColumn(widgetFileTree,"Path",        SWT.LEFT, 160,true );
        treeColumn.addSelectionListener(selectionListener);
        treeColumn = Widgets.addTreeColumn(widgetFileTree,"Date",        SWT.LEFT, 100,false);
        treeColumn.addSelectionListener(selectionListener);
        treeColumn = Widgets.addTreeColumn(widgetFileTree,"Size [bytes]",SWT.RIGHT,100,false);
        treeColumn.addSelectionListener(selectionListener);
//        Widgets.setTableColumnWidth(widgetFiles,Settings.geometryFindFilesColumn.width);
        treeColumn.setToolTipText("Files to import.");

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,3,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subSubComposite,"Exclude:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetExcludePatterns = Widgets.newText(subSubComposite,SWT.SEARCH|SWT.ICON_CANCEL);
          Widgets.layout(widgetExcludePatterns,0,1,TableLayoutData.WE);
          widgetExcludePatterns.setToolTipText("Import exclude patterns. Separate patterns by space and use * and ? for wildcards.");
        }
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCreate = Widgets.newButton(composite,"Create");
      widgetCreate.setEnabled(false);
      Widgets.layout(widgetCreate,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCreate.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.repositoryPath  = widgetRepository.getText();
          data.moduleName      = widgetModuleName.getText().trim();
          data.destinationPath = widgetDestinationPath.getText();
          data.importPath      = widgetImportPath.getText();
          data.excludePatterns = widgetExcludePatterns.getText().trim();

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
    widgetRepository.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetCreate.setEnabled(   !widgetRepository.getText().trim().isEmpty()
                                && !widgetModuleName.getText().trim().isEmpty()
                               );
      }
    });
    widgetModuleName.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetCreate.setEnabled(   !widgetRepository.getText().trim().isEmpty()
                                && !widgetModuleName.getText().trim().isEmpty()
                               );
      }
    });
    Widgets.setNextFocus(widgetRepository,widgetModuleName);
    Widgets.setNextFocus(widgetModuleName,widgetDestinationPath);
    Widgets.setNextFocus(widgetDestinationPath,widgetCreate);

    // add existing repository paths
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
        // get repository paths
        ArrayList<String> repositoryPathArray = new ArrayList<String>();
        for (Repository repository : repositoryList)
        {
          String repositoryPath = repository.getRepositoryPath();
          if (!repositoryPath.isEmpty()) repositoryPathArray.add(repositoryPath);
        }
        final String[] repositoryPaths = repositoryPathArray.toArray(new String[repositoryPathArray.size()]);

        // sort
        Arrays.sort(repositoryPaths);

        // add to widget
        display.syncExec(new Runnable()
        {
          public void run()
          {
            for (String repositoryPath : repositoryPaths)
            {
              widgetRepository.add(repositoryPath);
            }
          }
        });
      }
    });

    // start find files
    Background.run(new BackgroundRunnable()
    {
      String             excludePatterns    = "";
      ArrayList<Pattern> excludePatternList = new ArrayList<Pattern>();

      boolean matchExcludePatterns(String fileName)
      {
Dprintf.dprintf("");
        return false;
      }

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isDataModified()
      {
        return    data.quitFlag
               || ((data.excludePatterns != null) && !data.excludePatterns.equals(excludePatterns));
      }

      /** run method
       */
      public void run()
      {
        while (!data.quitFlag)
        {
          // find files
          display.syncExec(new Runnable()
          {
            public void run()
            {
Dprintf.dprintf("");
//              Widgets.removeAllTableEntries(widgetFiles);
            }
          });
          if (!excludePatterns.isEmpty())
          {
//Dprintf.dprintf("findPattern=%s",findPattern);
            File directory = new File(data.importPath);

            // check for modified data
            if (isDataModified()) break;
//Dprintf.dprintf("directory=%s",directory);

            LinkedList<File> directoryList = new LinkedList<File>();
            do
            {
//Dprintf.dprintf("directory=%s",directory);
              String[] fileNames = directory.list();
              if (fileNames != null)
              {
                for (String fileName : fileNames)
                {
                  // check for modified data
                  if (isDataModified()) break;

//Dprintf.dprintf("fileName=%s %s",fileName,repositoryTab.repository.isHiddenFile(fileName));
                  if (true) //!repositoryTab.repository.isHiddenFile(fileName))
                  {
                    final File file = new File(directory,fileName);
                    if     (file.isFile())
                    {
                      if (!matchExcludePatterns(fileName))
                      {
                        display.syncExec(new Runnable()
                        {
                          public void run()
                          {
                            FileDataComparator fileDataComparator = new FileDataComparator(widgetFileTree);

/*
                            Widgets.insertTableEntry(widgetFileTree,
                                                     fileDataComparator,
null,//                                                       new FindData(repositoryTab,file),
                                                     file.getName(),
                                                     file.getParent(),
                                                     Onzen.DATETIME_FORMAT.format(file.lastModified()),
                                                     Long.toString(file.length())
                                                    );
*/
Dprintf.dprintf("");

                          }
                        });
                      }
                    }
                    else if (file.isDirectory())
                    {
//Dprintf.dprintf("add dir=%s %s",fileName,file.getPath());
                      directoryList.add(file);
                    }
                  }
                }
              }
            }
            while ((directory = directoryList.pollFirst()) != null);
          }

          // wait for new filter pattern/quit
          synchronized(data)
          {
            // wait for new find data
            while (!isDataModified())
            {
              try
              {
                data.wait();
              }
              catch (InterruptedException exception)
              {
                // ignored
              }
            }

            // get new exclude pattners
            if (data.excludePatterns != null)
            {
              // get new exclude patterns
              excludePatterns = new String(data.excludePatterns);
              excludePatternList.clear();
              for (String excludePattern : StringUtils.split(excludePatterns))
              {
                excludePatternList.add(Pattern.compile(StringUtils.globToRegex(excludePattern),Pattern.CASE_INSENSITIVE));
              }

              // clear existing text
              data.excludePatterns = null;
            }
          }
        }
      }
    });

    // run dialog
    Widgets.setFocus(widgetRepository);
    if ((Boolean)Dialogs.run(dialog,false))
    {
      BusyDialog busyDialog = new BusyDialog(shell,
                                             "Create new repository",
                                             "Create new repository '" + data.repositoryPath + "'...",
                                             BusyDialog.TEXT0
                                            );
      busyDialog.autoAnimate();

      Background.run(new BackgroundRunnable(this,busyDialog)
      {
        public void run(final Onzen onzen, final BusyDialog busyDialog)
        {
          setStatusText("Create new repository '" + data.repositoryPath + "'...");
          try
          {
            // create directory
            File file = new File(data.destinationPath);
            if      (!file.exists())
            {
              if (!file.mkdirs())
              {
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    Dialogs.error(shell,"Cannot create new directory '%s'",data.destinationPath);
                    }
                });
                return;
              }
            }
            else if (!file.isDirectory())
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  Dialogs.error(shell,"'" + data.destinationPath +"' is not a directory");
                  }
              });
              return;
            }

            final Repository repository = Repository.newInstance(data.type,data.destinationPath);;
// ???
Dprintf.dprintf("NYI");
            repository.create(data.repositoryPath,data.moduleName,data.importPath);

            display.syncExec(new Runnable()
            {
              public void run()
              {
                // add and select new repository
                selectRepositoryTab(addRepositoryTab(repository));
              }
            });
          }
          catch (RepositoryException exception)
          {
            final String message = exception.getMessage();
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,"Cannot create new repository '%s' in:\n\n'%s'\n\n(error: %s).",data.moduleName,data.repositoryPath,message);
              }
            });
            return;
          }
          finally
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                busyDialog.close();
              }
            });
            clearStatusText();
          }
        }
      });
    }
  }

  /** checkout and open repository
   */
  private void checkoutRepository()
  {
    /** dialog data
     */
    class Data
    {
      Repository repository;
      String     repositoryPath;
      String     moduleName;
      String     revision;
      String     destinationPath;
      boolean    quitFlag;

      Data()
      {
        this.repository      = lastCheckoutRepository;
        this.repositoryPath  = lastCheckoutRepositoryPath;
        this.moduleName      = lastCheckoutModuleName;
        this.revision        = lastCheckoutRevision;
        this.destinationPath = lastCheckoutDestinationPath;
        this.quitFlag        = false;
      }
    }

    Composite composite,subComposite;
    Button    button;
    Label     label;

    final Data  data   = new Data();
    final Shell dialog = Dialogs.openModal(shell,"Checkout repository",500,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Combo  widgetRepository;
    final Combo  widgetModuleName;
    final Combo  widgetRevision;
    final Text   widgetDestinationPath;
    final Button widgetCheckout;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Type:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,null));
      Widgets.layout(subComposite,0,1,TableLayoutData.W);
      {
        button = Widgets.newRadio(subComposite,"CVS");
        button.setSelection(data.repository == RepositoryCVS.getInstance());
        Widgets.layout(button,0,0,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              synchronized(data)
              {
                data.repository = RepositoryCVS.getInstance();
                data.notifyAll();
              }
            }
          }
        });

        button = Widgets.newRadio(subComposite,"SVN");
        button.setSelection(data.repository == RepositorySVN.getInstance());
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              synchronized(data)
              {
                data.repository = RepositorySVN.getInstance();
                data.notifyAll();
              }
            }
          }
        });

        button = Widgets.newRadio(subComposite,"HG");
        button.setSelection(data.repository == RepositoryHG.getInstance());
        Widgets.layout(button,0,2,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              synchronized(data)
              {
                data.repository = RepositoryHG.getInstance();
                data.notifyAll();
              }
            }
          }
        });

        button = Widgets.newRadio(subComposite,"GIT");
        button.setSelection(data.repository == RepositoryGIT.getInstance());
        Widgets.layout(button,0,3,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              synchronized(data)
              {
                data.repository = RepositoryGIT.getInstance();
                data.notifyAll();
              }
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Repository:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.NSWE);
      {
        widgetRepository = Widgets.newCombo(subComposite);
        widgetRepository.setText(data.repositoryPath);
        Widgets.layout(widgetRepository,0,0,TableLayoutData.WE);
        widgetRepository.setToolTipText("Respository path URI.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                           "Select repository path",
                                            widgetRepository.getText()
                                           );
            if (path != null)
            {
              widgetRepository.setText(path);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Module:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetModuleName = Widgets.newCombo(composite);
      widgetModuleName.setText(data.moduleName);
      Widgets.layout(widgetModuleName,2,1,TableLayoutData.WE);
      widgetModuleName.setToolTipText("Module name in repository.");

      label = Widgets.newLabel(composite,"Revision:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetRevision = Widgets.newCombo(composite);
      widgetRevision.setText(data.revision);
      Widgets.layout(widgetRevision,3,1,TableLayoutData.WE);
      widgetRevision.setToolTipText("Revision to check-out.");

      label = Widgets.newLabel(composite,"Destination:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,4,1,TableLayoutData.WE);
      {
        widgetDestinationPath = Widgets.newText(subComposite);
        widgetDestinationPath.setText(data.destinationPath);
        Widgets.layout(widgetDestinationPath,0,0,TableLayoutData.WE);
        widgetDestinationPath.setToolTipText("Destination check-out directory path.");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String path = Dialogs.directory(shell,
                                           "Select destination directory path",
                                            widgetDestinationPath.getText()
                                           );
            if (path != null)
            {
              widgetDestinationPath.setText(path);
            }
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetCheckout = Widgets.newButton(composite,"Check-out");
      widgetCheckout.setEnabled(   !widgetRepository.getText().trim().isEmpty()
                                && !widgetDestinationPath.getText().trim().isEmpty()
                               );
      Widgets.layout(widgetCheckout,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCheckout.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          // get data
          data.repositoryPath  = widgetRepository.getText();
          data.moduleName      = widgetModuleName.getText().trim();
          data.revision        = widgetRevision.getText().trim();
          data.destinationPath = widgetDestinationPath.getText();

          // store repository path into checkout history
          boolean flag = false;
          for (String checkoutHistoryPath : Settings.checkoutHistoryPaths)
          {
            flag |= checkoutHistoryPath.equals(data.repositoryPath);
          }
          if (!flag)
          {
            String[] newCheckoutHistoryPaths = new String[Math.min(Settings.checkoutHistoryPaths.length+1,20)];
            newCheckoutHistoryPaths[0] = data.repositoryPath;
            System.arraycopy(Settings.checkoutHistoryPaths,0,newCheckoutHistoryPaths,1,Math.min(Settings.checkoutHistoryPaths.length,20-1));
            Settings.checkoutHistoryPaths = newCheckoutHistoryPaths;
          }

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
    widgetRepository.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetCheckout.setEnabled(   !widgetRepository.getText().trim().isEmpty()
                                  && !widgetDestinationPath.getText().trim().isEmpty()
                                 );
      }
    });
    widgetRepository.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        synchronized(data)
        {
          data.repositoryPath = widget.getText().trim();
          data.notifyAll();
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        synchronized(data)
        {
          data.repositoryPath = widget.getText().trim();
          data.notifyAll();
        }
      }
    });
    widgetModuleName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        synchronized(data)
        {
          data.moduleName = widget.getText().trim();
          data.notifyAll();
        }
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        synchronized(data)
        {
          data.moduleName = widget.getText().trim();
          data.notifyAll();
        }
      }
    });
    widgetDestinationPath.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetCheckout.setEnabled(   !widgetRepository.getText().trim().isEmpty()
                                  && !widgetDestinationPath.getText().trim().isEmpty()
                                 );
      }
    });

    Widgets.setNextFocus(widgetRepository,widgetModuleName);
    Widgets.setNextFocus(widgetModuleName,widgetRevision);
    Widgets.setNextFocus(widgetRevision,widgetDestinationPath);
    Widgets.setNextFocus(widgetDestinationPath,widgetCheckout);

    // set type, add checkout history paths
    widgetRepository.add("file://");
    widgetRepository.add("ssh://");
    widgetRepository.add("http://");
    widgetRepository.add("https://");
    widgetRepository.add("rsync://");
    for (String checkoutHistoryPath : Settings.checkoutHistoryPaths)
    {
      widgetRepository.add(checkoutHistoryPath);
    }

    // update module/branch names, revision names
    Background.run(new BackgroundRunnable()
    {
      boolean    repositoryModifiedFlag = false;
      Repository repository             = RepositoryCVS.getInstance();
      String     repositoryPath         = "";
      boolean    moduleNameModifiedFlag = false;
      String     moduleName             = "";

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isRepositoryModified()
      {
        return    (repository != data.repository)
               || !repositoryPath.equals(data.repositoryPath);
      }

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isModuleNameModified()
      {
        return !moduleName.equals(data.moduleName);
      }

      /** check if data is modified
       * @return true iff data is modified
       */
      boolean isDataModified()
      {
        return    data.quitFlag
               || isRepositoryModified()
               || isModuleNameModified();
      }

      /** run method
       */
      public void run()
      {
        while (!data.quitFlag)
        {
          // reset module/branch names, store current selection
          final String[] result = new String[2];
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                result[0] = widgetModuleName.getText().trim();
                result[1] = widgetRevision.getText().trim();
              }
            });
          }
          final String selectedModuleName   = result[0];
          final String selectedRevisionName = result[1];

          if (repositoryModifiedFlag)
          {
            // update module/branch names
            try
            {
              final String[] branchNames = repository.getBranchNames(repositoryPath);
              if (branchNames != null)
              {
//Dprintf.dprintf("branchNames.length=%d",branchNames.length);
                if (!dialog.isDisposed())
                {
                  display.syncExec(new Runnable()
                  {
                    public void run()
                    {
                      widgetModuleName.removeAll();
                      widgetModuleName.add("");
                      for (String branchName : branchNames)
                      {
                        widgetModuleName.add(branchName);
                      }
                      if      (!selectedModuleName.isEmpty()) widgetModuleName.setText(selectedModuleName);
                      else if (repository.getDefaultRootName() != null) widgetModuleName.setText(repository.getDefaultRootName());
                    }
                  });
                }
              }
            }
            catch (RepositoryException exception)
            {
Dprintf.dprintf("exception=%s",exception);
              // ignored
            }
          }

          if (repositoryModifiedFlag || moduleNameModifiedFlag)
          {
            // update revisions
            try
            {
              final String[] revisionNames = repository.getRevisionNames(repositoryPath);
              if (revisionNames != null)
              {
//Dprintf.dprintf("revisionNames.length=%d",revisionNames.length);
                if (!dialog.isDisposed())
                {
                  display.syncExec(new Runnable()
                  {
                    public void run()
                    {
                      widgetRevision.removeAll();
                      widgetRevision.add("");
                      for (String revisionName : revisionNames)
                      {
                        widgetRevision.add(revisionName,0);
                      }
                      widgetRevision.setText(!selectedRevisionName.isEmpty() ? selectedRevisionName : repository.getLastRevision());
                    }
                  });
                }
              }
            }
            catch (RepositoryException exception)
            {
              // ignored
Dprintf.dprintf("exception=%s",exception);
            }
          }

          // wait for new filter pattern/quit
          synchronized(data)
          {
            // wait for new data
            while (!isDataModified())
            {
              try
              {
                data.wait();
              }
              catch (InterruptedException exception)
              {
                // ignored
              }
            }

            // get new update data
            repositoryModifiedFlag = isRepositoryModified();
            moduleNameModifiedFlag = isModuleNameModified();
            repository             = data.repository;
            repositoryPath         = new String(data.repositoryPath);
            moduleName             = new String(data.moduleName);
          }
        }
      }
    });

    // run dialog
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // store last data
      lastCheckoutRepository      = data.repository;
      lastCheckoutRepositoryPath  = data.repositoryPath;
      lastCheckoutModuleName      = data.moduleName;
      lastCheckoutRevision        = data.revision;
      lastCheckoutDestinationPath = data.destinationPath;

      // checkout
      BusyDialog busyDialog = new BusyDialog(shell,
                                             "Checkout repository",
                                             "Checkout repository '"+
                                             data.repositoryPath+
                                             (!data.moduleName.isEmpty() ? "/"+data.moduleName : "")+
                                             (!data.revision.isEmpty() ? ":"+data.revision : "")+
                                             "' into '"+
                                             data.destinationPath+
                                             "':",
                                             BusyDialog.LIST
                                            );
      busyDialog.autoAnimate();

      Background.run(new BackgroundRunnable(this,busyDialog)
      {
        public void run(final Onzen onzen, final BusyDialog busyDialog)
        {
          setStatusText("Checkout repository '"+
                        data.repositoryPath+
                        (!data.moduleName.isEmpty() ? "/"+data.moduleName : "")+
                        (!data.revision.isEmpty() ? ":"+data.revision : "")+
                        "' into '"+
                        data.destinationPath+
                        "'"
                       );
          try
          {
            // create directory
            if (!busyDialog.isAborted())
            {
              File file = new File(data.destinationPath);
              if      (!file.exists())
              {
                if (!file.mkdirs())
                {
                  display.syncExec(new Runnable()
                  {
                    public void run()
                    {
                      Dialogs.error(shell,"Cannot create new directory '%s'",data.repositoryPath);
                    }
                  });
                  return;
                }
              }
              else if (!file.isDirectory())
              {
                display.syncExec(new Runnable()
                {
                  public void run()
                  {
                    Dialogs.error(shell,"'" + data.repositoryPath +"' is not a directory");
                    }
                });
                return;
              }
            }

            final Repository repository = Repository.newInstance(data.repository.getType(),data.destinationPath);
            repository.checkout(data.repositoryPath,data.moduleName,data.revision,data.destinationPath,busyDialog);

            if (!busyDialog.isAborted())
            {
              display.syncExec(new Runnable()
              {
                public void run()
                {
                  // add repository tab
                  RepositoryTab repositoryTab = addRepositoryTab(repository);

                  // select repository tab
                  selectRepositoryTab(repositoryTab);
                }
              });
            }
          }
          catch (final RepositoryException exception)
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                Dialogs.error(shell,exception.getExtendedMessage(),"Cannot checkout repository\n\n'%s'\n\n(error: %s).",data.repositoryPath,exception.getMessage());
              }
            });
            return;
          }
          finally
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                busyDialog.close();
              }
            });
            clearStatusText();
          }
        }
      });

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
      Dialogs.error(shell,"Cannot open repository '%s' (error: %s).",rootPath,exception.getMessage());
      return null;
    }

    return repository;
  }

  /** edit repository
   * @param repositoryTab repository tab to edit
   * @return true iff edited, FALSE otherwise
   */
  private boolean editRepository(final RepositoryTab repositoryTab)
  {
    /** dialog data
     */
    class Data
    {
      String   title;
      String   rootPath;
      String   masterRepository;
      String[] ignorePatterns;
      String[] patchTests;
      String   mailSMTPHost;
      int      mailSMTPPort;
      boolean  mailSMTPSSL;
      String   mailLogin;
      String   mailPassword;
      String   mailFrom;
      String   patchMailTo;
      String   patchMailCC;
      String   patchMailSubject;
      String   patchMailText;
      String   reviewServerHost;
      String   reviewServerLogin;
      String   reviewServerPassword;
      String   reviewServerSummary;
      String   reviewServerRepository;
      String   reviewServerGroups;
      String   reviewServerPersons;
      String   reviewServerDescription;

      Data()
      {
        this.title                   = null;
        this.rootPath                = null;
        this.masterRepository        = null;
        this.ignorePatterns          = null;
        this.patchTests              = null;
        this.mailSMTPHost            = null;
        this.mailSMTPPort            = 0;
        this.mailSMTPSSL             = false;
        this.mailLogin               = null;
        this.mailPassword            = null;
        this.mailFrom                = null;
        this.patchMailTo             = null;
        this.patchMailCC             = null;
        this.patchMailSubject        = null;
        this.patchMailText           = null;
        this.reviewServerHost        = null;
        this.reviewServerLogin       = null;
        this.reviewServerPassword    = null;
        this.reviewServerSummary     = null;
        this.reviewServerRepository  = null;
        this.reviewServerGroups      = null;
        this.reviewServerPersons     = null;
        this.reviewServerDescription = null;
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
    Menu        menu,menu1,menu2,subMenu;
    MenuItem    menuItem;

    // repository edit dialog
    dialog = Dialogs.openModal(shell,"Edit repository",new double[]{1.0,0.0},1.0);

    final Text                  widgetTitle;
    final Text                  widgetRootPath;
    final List                  widgetIgnorePatterns;
    final HashMap<Field,Widget> widgetFieldMap = new HashMap<Field,Widget>();
    final List                  widgetPatchTests;
    final Text                  widgetMailSMTPHost;
    final Spinner               widgetMailSMTPPort;
    final Button                widgetMailSMTPSSL;
    final Text                  widgetMailLogin;
    final Text                  widgetMailPassword;
    final Text                  widgetMailFrom;
    final Text                  widgetPatchMailTo;
    final Text                  widgetPatchMailCC;
    final Text                  widgetPatchMailSubject;
    final Text                  widgetPatchMailText;
    final Text                  widgetReviewServerHost;
    final Text                  widgetReviewServerLogin;
    final Text                  widgetReviewServerPassword;
    final Text                  widgetReviewServerSummary;
    final Text                  widgetReviewServerRepository;
    final Text                  widgetReviewServerGroups;
    final Text                  widgetReviewServerPersons;
    final Text                  widgetReviewServerDescription;
    final Button                widgetSave;
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(1.0,1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);

      subComposite = Widgets.addTab(tabFolder,"Repository");
      subComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,1.0,0.0},new double[]{0.0,1.0},2));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        // common values
        label = Widgets.newLabel(subComposite,"Type:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        label = Widgets.newLabel(subComposite);
        label.setText(repositoryTab.repository.getType().toString());
        Widgets.layout(label,0,1,TableLayoutData.W);

        label = Widgets.newLabel(subComposite,"Repository path:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        text = Widgets.newStringView(subComposite,SWT.LEFT);
        text.setText(repositoryTab.repository.getRepositoryPath());
        Widgets.layout(text,1,1,TableLayoutData.W);
        text.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
            Text widget = (Text)mouseEvent.widget;

            widget.setSelection(0,widget.getText().length());
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
          }
        });

        label = Widgets.newLabel(subComposite,"Title:");
        Widgets.layout(label,2,0,TableLayoutData.W);

        widgetTitle = Widgets.newText(subComposite);
        widgetTitle.setText(repositoryTab.repository.title);
        Widgets.layout(widgetTitle,2,1,TableLayoutData.WE);
        widgetTitle.setToolTipText("Repository title.");

        label = Widgets.newLabel(subComposite,"Root path:");
        Widgets.layout(label,3,0,TableLayoutData.W);

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
        Widgets.layout(subSubComposite,3,1,TableLayoutData.WE);
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

        label = Widgets.newLabel(subComposite,"Ignore patterns:");
        Widgets.layout(label,4,0,TableLayoutData.NW);

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subSubComposite,4,1,TableLayoutData.NSWE);
        {
          widgetIgnorePatterns = Widgets.newList(subSubComposite);
          for (String pattern : repositoryTab.repository.getIgnorePatterns())
          {
            widgetIgnorePatterns.add(pattern);
          }
          Widgets.layout(widgetIgnorePatterns,0,0,TableLayoutData.NSWE);
          widgetIgnorePatterns.setToolTipText("List of file patterns to ignore.");

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,0.0));
          Widgets.layout(subSubSubComposite,1,0,TableLayoutData.WE,0,0,2);
          {
            button = Widgets.newButton(subSubSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String pattern = Dialogs.path(dialog,"Add ignore pattern","Pattern:","","Add","Cancel");
                if (pattern != null)
                {
                  widgetIgnorePatterns.add(pattern);
                }
              }
            });

            button = Widgets.newButton(subSubSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetIgnorePatterns.getSelectionIndex();
                if (index >= 0)
                {
                  widgetIgnorePatterns.remove(index);
                }
              }
            });
          }
        }

        // additional repository values
        int row = 5;
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

      subComposite = Widgets.addTab(tabFolder,"Patch tests");
      subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
      Widgets.layout(subComposite,0,1,TableLayoutData.NSWE);
      {
        menu = Widgets.newPopupMenu(dialog);

        widgetPatchTests = Widgets.newList(subComposite);
        if (repositoryTab.repository.patchTests != null)
        {
          for (String patchTest : repositoryTab.repository.patchTests)
          {
            widgetPatchTests.add(patchTest);
          }
        }
        Widgets.layout(widgetPatchTests,0,0,TableLayoutData.NSWE,0,0,2);
        widgetPatchTests.setMenu(menu);
        widgetPatchTests.setToolTipText("List of test descriptions which can be selected when sending a patch for review.");

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(null,null));
        Widgets.layout(subSubComposite,1,0,TableLayoutData.WE,0,0,2);
        {
          button = Widgets.newButton(subSubComposite,"Add");
          Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String patchTest = Dialogs.string(dialog,"Edit patch test","Test:","","Add","Cancel");
              if (patchTest != null)
              {
                widgetPatchTests.add(patchTest);
              }
            }
          });
          button.setToolTipText("Add new test description.");

          button = Widgets.newButton(subSubComposite,"Remove");
          Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
                if (Dialogs.confirm(dialog,"Confirmation",String.format("Remove patch test '%s'?",test)))
                {
                  widgetPatchTests.remove(index);
                }
              }
            }
          });
          button.setToolTipText("Remove selected test description.");

          button = Widgets.newButton(subSubComposite,"Sort\u2026");
          Widgets.layout(button,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              if (Dialogs.confirm(dialog,"Really sort test descriptions?"))
              {
                Widgets.sortList(widgetPatchTests);
              }
            }
          });
          button.setToolTipText("Sort test descriptions.");

          button = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_UP);
          Widgets.layout(button,0,3,TableLayoutData.NSW,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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

          button = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_DOWN);
          Widgets.layout(button,0,4,TableLayoutData.NSW,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
        }
        subComposite.setMenu(menu);
        subComposite.setToolTipText("Test description settings.\nRight-click to open context menu.");

        {
          subMenu = Widgets.addMenu(menu,"Copy from\u2026");
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
                  if (repository.patchTests != null)
                  {
                    for (String patchTest : repository.patchTests)
                    {
                      widgetPatchTests.add(patchTest);
                    }
                  }
                }
              });
            }
          }
        }
      }

      subComposite = Widgets.addTab(tabFolder,"Mail");
      subComposite.setLayout(new TableLayout(new double[]{0.0,0.3,0.7},1.0));
      Widgets.layout(subComposite,0,2,TableLayoutData.NSWE);
      {
        menu = Widgets.newPopupMenu(dialog);

        subSubComposite = Widgets.newGroup(subComposite,"Mail server");
        subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.WE,0,0,2);
        {
          label = Widgets.newLabel(subSubComposite,"SMTP server:");
          label.setMenu(menu);
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{0.7,0.0,0.3,0.0}));
          Widgets.layout(subSubSubComposite,0,1,TableLayoutData.WE,0,0,2);
          {
            widgetMailSMTPHost = Widgets.newText(subSubSubComposite);
            widgetMailSMTPHost.setText((repositoryTab.repository.mailSMTPHost != null)?repositoryTab.repository.mailSMTPHost:Settings.mailSMTPHost);
            Widgets.layout(widgetMailSMTPHost,0,0,TableLayoutData.WE);
            widgetMailSMTPHost.setToolTipText("Mail SMTP server host name.");

            label = Widgets.newLabel(subSubSubComposite,"Port:");
            label.setMenu(menu);
            Widgets.layout(label,0,1,TableLayoutData.W);

            widgetMailSMTPPort = Widgets.newSpinner(subSubSubComposite,0,65535);
            widgetMailSMTPPort.setTextLimit(5);
            widgetMailSMTPPort.setSelection((repositoryTab.repository.mailSMTPPort != 0)?repositoryTab.repository.mailSMTPPort:Settings.mailSMTPPort);
            Widgets.layout(widgetMailSMTPPort,0,2,TableLayoutData.WE);
            widgetMailSMTPPort.setToolTipText("Mail SMTP server port number.");

            widgetMailSMTPSSL = Widgets.newCheckbox(subSubSubComposite,"SSL");
            widgetMailSMTPSSL.setSelection(repositoryTab.repository.mailSMTPSSL);
            Widgets.layout(widgetMailSMTPSSL,0,3,TableLayoutData.E);
            widgetMailSMTPSSL.setToolTipText("Use SMTP with SSL encryption.");
          }

          label = Widgets.newLabel(subSubComposite,"Login:");
          label.setMenu(menu);
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,1.0}));
          Widgets.layout(subSubSubComposite,1,1,TableLayoutData.WE,0,0,2);
          {
            widgetMailLogin = Widgets.newText(subSubSubComposite);
            widgetMailLogin.setText((repositoryTab.repository.mailLogin != null)?repositoryTab.repository.mailLogin:Settings.mailLogin);
            Widgets.layout(widgetMailLogin,0,0,TableLayoutData.WE);
            widgetMailLogin.setToolTipText("Mail server login name.");

            label = Widgets.newLabel(subSubSubComposite,"Password:");
            label.setMenu(menu);
            Widgets.layout(label,0,1,TableLayoutData.W);

            widgetMailPassword = Widgets.newPassword(subSubSubComposite);
            String password = getPassword(repositoryTab.repository.mailLogin,repositoryTab.repository.mailSMTPHost,false);
            if (password != null) widgetMailPassword.setText(password);
            Widgets.layout(widgetMailPassword,0,2,TableLayoutData.WE);
            widgetMailPassword.setToolTipText("Mail server login password.");
          }

          label = Widgets.newLabel(subSubComposite,"From name:");
          label.setMenu(menu);
          Widgets.layout(label,3,0,TableLayoutData.W,0,0,2);

          widgetMailFrom = Widgets.newText(subSubComposite);
          widgetMailFrom.setText((repositoryTab.repository.mailFrom != null)?repositoryTab.repository.mailFrom:Settings.mailFrom);
          Widgets.layout(widgetMailFrom,3,1,TableLayoutData.WE,0,0,2);
          widgetMailFrom.setToolTipText("Mail from address.");
        }
        subSubComposite.setMenu(menu);
        subSubComposite.setToolTipText("Mail server settings.\nRight-click to open context menu.");

        {
          subMenu = Widgets.addMenu(menu,"Copy from\u2026");
          menuItem = Widgets.addMenuItem(subMenu,"default");
          menuItem.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String password = getPassword(Settings.mailLogin,Settings.mailSMTPHost,false);

              if (Settings.mailSMTPHost != null) widgetMailSMTPHost.setText(Settings.mailSMTPHost);
              widgetMailSMTPPort.setSelection(Settings.mailSMTPPort);
              widgetMailSMTPSSL.setSelection(Settings.mailSMTPSSL);
              if (Settings.mailLogin != null) widgetMailLogin.setText(Settings.mailLogin);
              if (password != null) widgetMailPassword.setText(password);
              if (Settings.mailFrom != null) widgetMailFrom.setText(Settings.mailFrom);
            }
          });
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
                  String     password   = getPassword(repository.mailLogin,repository.mailSMTPHost,false);

                  if (repository.mailSMTPHost != null) widgetMailSMTPHost.setText(repository.mailSMTPHost);
                  widgetMailSMTPPort.setSelection(repository.mailSMTPPort);
                  widgetMailSMTPSSL.setSelection(repository.mailSMTPSSL);
                  if (repository.mailLogin != null) widgetMailLogin.setText(repository.mailLogin);
                  if (password != null) widgetMailPassword.setText(password);
                  if (repository.mailFrom != null) widgetMailFrom.setText(repository.mailFrom);
                }
              });
            }
          }
        }

        menu1 = Widgets.newPopupMenu(dialog);
        menu2 = Widgets.newPopupMenu(dialog);

        subSubComposite = Widgets.newGroup(subComposite,"Patch mail");
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,1.0},new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,1,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"To:");
          label.setMenu(menu1);
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          widgetPatchMailTo = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailTo != null) widgetPatchMailTo.setText(repositoryTab.repository.patchMailTo);
          Widgets.layout(widgetPatchMailTo,0,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailTo.setToolTipText("Default to-address for patch mails.");

          label = Widgets.newLabel(subSubComposite,"CC:");
          label.setMenu(menu1);
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);
          button.setToolTipText("Default CC-addresses for patch mails.");

          widgetPatchMailCC = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailCC != null) widgetPatchMailCC.setText(repositoryTab.repository.patchMailCC);
          Widgets.layout(widgetPatchMailCC,1,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailCC.setToolTipText("Patch mail carbon-copy address. Separate multiple addresses by spaces.");

          label = Widgets.newLabel(subSubComposite,"Subject:");
          label.setMenu(menu1);
          Widgets.layout(label,2,0,TableLayoutData.W,0,0,2);

          widgetPatchMailSubject = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.patchMailSubject != null) widgetPatchMailSubject.setText(repositoryTab.repository.patchMailSubject);
          Widgets.layout(widgetPatchMailSubject,2,1,TableLayoutData.WE,0,0,2);
          widgetPatchMailSubject.setToolTipText("Patch mail subject template.\nMacros:\n  ${n} - patch number\n  ${summary} - summary text");

          label = Widgets.newLabel(subSubComposite,"Text:");
          label.setMenu(menu2);
          Widgets.layout(label,3,0,TableLayoutData.NSW,0,0,2);

          widgetPatchMailText = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.H_SCROLL|SWT.V_SCROLL);
          if (repositoryTab.repository.patchMailText != null) widgetPatchMailText.setText(repositoryTab.repository.patchMailText);
          Widgets.layout(widgetPatchMailText,3,1,TableLayoutData.NSWE,0,0,2);
          widgetPatchMailText.setToolTipText("Patch mail text template.\nMacros:\n  ${date} - date\n  ${time} - time\n  ${datetime} - date/time\n  ${message} - message\n  ${tests} - tests\n  ${comment} - comments\n");
        }
        subSubComposite.setMenu(menu1);
        subSubComposite.setToolTipText("Patch mail settings.\nRight-click to open context menu.");

        {
          subMenu = Widgets.addMenu(menu1,"Copy from\u2026");
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
                }
              });
            }
          }
        }
        {
          subMenu = Widgets.addMenu(menu2,"Copy from\u2026");
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

                  if (repository.patchMailText != null) widgetPatchMailText.setText(repository.patchMailText);
                }
              });
            }
          }
        }
      }

      subComposite = Widgets.addTab(tabFolder,"Post review");
      subComposite.setLayout(new TableLayout(new double[]{0.0,0.3,0.7},1.0));
      Widgets.layout(subComposite,0,3,TableLayoutData.NSWE);
      {
        menu = Widgets.newPopupMenu(dialog);

        subSubComposite = Widgets.newGroup(subComposite,"Review server");
        subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.WE,0,0,2);
        {
          label = Widgets.newLabel(subSubComposite,"Name:");
          label.setMenu(menu);
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          widgetReviewServerHost = Widgets.newText(subSubComposite);
          widgetReviewServerHost.setText((repositoryTab.repository.reviewServerHost != null)?repositoryTab.repository.reviewServerHost:Settings.reviewServerHost);
          Widgets.layout(widgetReviewServerHost,0,1,TableLayoutData.WE);
          widgetReviewServerHost.setToolTipText("Review server name.");

          label = Widgets.newLabel(subSubComposite,"Login:");
          label.setMenu(menu);
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);

          subSubSubComposite = Widgets.newComposite(subSubComposite);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,1.0}));
          Widgets.layout(subSubSubComposite,1,1,TableLayoutData.WE,0,0,2);
          {
            widgetReviewServerLogin = Widgets.newText(subSubSubComposite);
            widgetReviewServerLogin.setText((repositoryTab.repository.reviewServerLogin != null)?repositoryTab.repository.reviewServerLogin:Settings.reviewServerLogin);
            Widgets.layout(widgetReviewServerLogin,0,0,TableLayoutData.WE);
            widgetReviewServerLogin.setToolTipText("Review server login name.");

            label = Widgets.newLabel(subSubSubComposite,"Password:");
            label.setMenu(menu);
            Widgets.layout(label,0,1,TableLayoutData.W);

            widgetReviewServerPassword = Widgets.newPassword(subSubSubComposite);
            String password = getPassword(repositoryTab.repository.reviewServerLogin,repositoryTab.repository.reviewServerHost,false);
            if (password != null) widgetReviewServerPassword.setText(password);
            Widgets.layout(widgetReviewServerPassword,0,2,TableLayoutData.WE);
            widgetReviewServerPassword.setToolTipText("Review server login password.");
          }
        }
        subSubComposite.setMenu(menu);
        subSubComposite.setToolTipText("Review server settings.\nRight-click to open context menu.");

        {
          subMenu = Widgets.addMenu(menu,"Copy from\u2026");
          menuItem = Widgets.addMenuItem(subMenu,"default");
          menuItem.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String password = getPassword(Settings.reviewServerLogin,Settings.reviewServerHost,false);

              if (Settings.reviewServerHost != null) widgetReviewServerHost.setText(Settings.reviewServerHost);
              if (Settings.reviewServerLogin != null) widgetReviewServerLogin.setText(Settings.reviewServerLogin);
              if (password != null) widgetReviewServerPassword.setText(password);
            }
          });
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
                  String     password   = getPassword(repository.reviewServerLogin,repository.reviewServerHost,false);

                  if (repository.reviewServerHost != null) widgetReviewServerHost.setText(repository.reviewServerHost);
                  if (repository.reviewServerLogin != null) widgetReviewServerLogin.setText(repository.reviewServerLogin);
                  if (password != null) widgetReviewServerPassword.setText(password);
                }
              });
            }
          }
        }

        menu1 = Widgets.newPopupMenu(dialog);
        menu2 = Widgets.newPopupMenu(dialog);

        subSubComposite = Widgets.newGroup(subComposite,"Review info");
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,1.0},new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,1,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"Summary:");
          label.setMenu(menu1);
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          widgetReviewServerSummary = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.reviewServerSummary != null) widgetReviewServerSummary.setText(repositoryTab.repository.reviewServerSummary);
          Widgets.layout(widgetReviewServerSummary,0,1,TableLayoutData.WE,0,0,2);
          widgetReviewServerSummary.setToolTipText("Review summary template.\nMacros:\n  ${n} - patch number\n  ${summary} - summary text");

          label = Widgets.newLabel(subSubComposite,"Repository:");
          label.setMenu(menu1);
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);

          widgetReviewServerRepository = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.reviewServerRepository != null) widgetReviewServerRepository.setText(repositoryTab.repository.reviewServerRepository);
          Widgets.layout(widgetReviewServerRepository,1,1,TableLayoutData.WE,0,0,2);
          widgetReviewServerRepository.setToolTipText("Repository id or name.");

          label = Widgets.newLabel(subSubComposite,"Groups:");
          label.setMenu(menu1);
          Widgets.layout(label,2,0,TableLayoutData.W,0,0,2);

          widgetReviewServerGroups = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.reviewServerGroups != null) widgetReviewServerGroups.setText(repositoryTab.repository.reviewServerGroups);
          Widgets.layout(widgetReviewServerGroups,2,1,TableLayoutData.WE,0,0,2);
          widgetReviewServerGroups.setToolTipText("Review groups names.");

          label = Widgets.newLabel(subSubComposite,"Persons:");
          label.setMenu(menu1);
          Widgets.layout(label,3,0,TableLayoutData.W,0,0,2);

          widgetReviewServerPersons = Widgets.newText(subSubComposite);
          if (repositoryTab.repository.reviewServerPersons != null) widgetReviewServerPersons.setText(repositoryTab.repository.reviewServerPersons);
          Widgets.layout(widgetReviewServerPersons,3,1,TableLayoutData.WE,0,0,2);
          widgetReviewServerPersons.setToolTipText("Review person names.");

          label = Widgets.newLabel(subSubComposite,"Description:");
          label.setMenu(menu2);
          Widgets.layout(label,4,0,TableLayoutData.NSW,0,0,2);

          widgetReviewServerDescription = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.H_SCROLL|SWT.V_SCROLL);
          if (repositoryTab.repository.reviewServerDescription != null) widgetReviewServerDescription.setText(repositoryTab.repository.reviewServerDescription);
          Widgets.layout(widgetReviewServerDescription,4,1,TableLayoutData.NSWE,0,0,2);
          widgetReviewServerDescription.setToolTipText("Review description template.\nMacros:\n  ${date} - date\n  ${time} - time\n  ${datetime} - date/time\n  ${message} - message\n  ${tests} - tests\n  ${comment} - comments\n");
        }
        subSubComposite.setMenu(menu1);

        {
          subMenu = Widgets.addMenu(menu1,"Copy from\u2026");
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

                  if (repository.reviewServerSummary != null) widgetReviewServerSummary.setText(repository.reviewServerSummary);
                  if (repository.reviewServerRepository != null) widgetReviewServerRepository.setText(repository.reviewServerRepository);
                  if (repository.reviewServerGroups != null) widgetReviewServerGroups.setText(repository.reviewServerGroups);
                  if (repository.reviewServerPersons != null) widgetReviewServerPersons.setText(repository.reviewServerPersons);
                }
              });
            }
          }
        }

        {
          subMenu = Widgets.addMenu(menu2,"Copy from\u2026");
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

                  if (repository.reviewServerDescription != null) widgetReviewServerDescription.setText(repository.reviewServerDescription);
                }
              });
            }
          }
        }

        subSubComposite.setToolTipText("Review message settings.\nRight-click to open context menu.");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetSave = Widgets.newButton(composite,"Save",SWT.CTRL+'S');
      Widgets.layout(widgetSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          // get base data
          data.title                   = widgetTitle.getText().trim();
          data.rootPath                = widgetRootPath.getText().trim();
          data.ignorePatterns          = widgetIgnorePatterns.getItems();
          data.patchTests              = widgetPatchTests.getItems();
          data.mailSMTPHost            = widgetMailSMTPHost.getText().trim();
          data.mailSMTPPort            = widgetMailSMTPPort.getSelection();
          data.mailSMTPSSL             = widgetMailSMTPSSL.getSelection();
          data.mailLogin               = widgetMailLogin.getText().trim();
          data.mailPassword            = widgetMailPassword.getText();
          data.mailFrom                = widgetMailFrom.getText().trim();
          data.patchMailTo             = widgetPatchMailTo.getText().trim();
          data.patchMailCC             = widgetPatchMailCC.getText().trim();
          data.patchMailSubject        = widgetPatchMailSubject.getText().trim();
          data.patchMailText           = widgetPatchMailText.getText().trim();
          data.reviewServerHost        = widgetReviewServerHost.getText().trim();
          data.reviewServerLogin       = widgetReviewServerLogin.getText().trim();
          data.reviewServerPassword    = widgetReviewServerPassword.getText().trim();
          data.reviewServerSummary     = widgetReviewServerSummary.getText().trim();
          data.reviewServerRepository  = widgetReviewServerRepository.getText().trim();
          data.reviewServerGroups      = widgetReviewServerGroups.getText().trim();
          data.reviewServerPersons     = widgetReviewServerPersons.getText().trim();
          data.reviewServerDescription = widgetReviewServerDescription.getText().trim();

          // get additional data
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
    Dialogs.show(dialog,Settings.geometryEditRepository,Settings.setWindowLocation);

    // run
    Widgets.setFocus(widgetTitle);
    if ((Boolean)Dialogs.run(dialog,false))
    {
      // set data
      repositoryTab.setTitle(data.title);
      repositoryTab.repository.rootPath                = data.rootPath;
      repositoryTab.repository.setIgnorePatterns(data.ignorePatterns);
      repositoryTab.repository.patchTests              = data.patchTests;
      repositoryTab.repository.mailSMTPHost            = data.mailSMTPHost;
      repositoryTab.repository.mailSMTPPort            = data.mailSMTPPort;
      repositoryTab.repository.mailSMTPSSL             = data.mailSMTPSSL;
      repositoryTab.repository.mailLogin               = data.mailLogin;
      repositoryTab.repository.mailFrom                = data.mailFrom;
      repositoryTab.repository.patchMailTo             = data.patchMailTo;
      repositoryTab.repository.patchMailCC             = data.patchMailCC;
      repositoryTab.repository.patchMailSubject        = data.patchMailSubject;
      repositoryTab.repository.patchMailText           = data.patchMailText;
      repositoryTab.repository.reviewServerHost        = data.reviewServerHost;
      repositoryTab.repository.reviewServerLogin       = data.reviewServerLogin;
      repositoryTab.repository.reviewServerSummary     = data.reviewServerSummary;
      repositoryTab.repository.reviewServerRepository  = data.reviewServerRepository;
      repositoryTab.repository.reviewServerGroups      = data.reviewServerGroups;
      repositoryTab.repository.reviewServerPersons     = data.reviewServerPersons;
      repositoryTab.repository.reviewServerDescription = data.reviewServerDescription;
      setPassword(data.mailLogin,data.mailSMTPHost,data.mailPassword);
      setPassword(data.reviewServerLogin,data.reviewServerHost,data.reviewServerPassword);

      // save list
      try
      {
        repositoryList.save();
      }
      catch (IOException exception)
      {
        Dialogs.error(shell,"Cannot store repository list (error: %s).",exception.getMessage());
      }

      return true;
    }
    else
    {
      return false;
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
  public void selectRepositoryTab(RepositoryTab repositoryTab)
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
      menuItemLock.setEnabled(repositoryTab.repository.supportLockUnlock());
      menuItemUnlock.setEnabled(repositoryTab.repository.supportLockUnlock());
      menuItemIncomingChanges.setEnabled(repositoryTab.repository.supportIncomingOutgoing());
      menuItemIncomingChangesFrom.setEnabled(repositoryTab.repository.supportIncomingOutgoing());
      menuItemOutgoingChanges.setEnabled(repositoryTab.repository.supportIncomingOutgoing());
      menuItemOutgoingChangesTo.setEnabled(repositoryTab.repository.supportIncomingOutgoing());
      menuItemPullChanges.setEnabled(repositoryTab.repository.supportPullPush());
      menuItemPullChangesFrom.setEnabled(repositoryTab.repository.supportPullPush());
      menuItemPushChanges.setEnabled(repositoryTab.repository.supportPullPush());
      menuItemPushChangesTo.setEnabled(repositoryTab.repository.supportPullPush());
      menuSetFileMode.setEnabled(repositoryTab.repository.supportSetFileMode());
    }
    else
    {
      // disable menu entries
      menuItemApplyPatches.setEnabled(false);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemLock.setEnabled(false);
      menuItemUnlock.setEnabled(false);
      menuItemIncomingChanges.setEnabled(false);
      menuItemIncomingChangesFrom.setEnabled(false);
      menuItemOutgoingChanges.setEnabled(false);
      menuItemOutgoingChangesTo.setEnabled(false);
      menuItemPullChanges.setEnabled(false);
      menuItemPullChangesFrom.setEnabled(false);
      menuItemPushChanges.setEnabled(false);
      menuItemPushChangesTo.setEnabled(false);
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
      menuItemLock.setEnabled(selectedRepositoryTab.repository.supportLockUnlock());
      menuItemUnlock.setEnabled(selectedRepositoryTab.repository.supportLockUnlock());
      menuItemIncomingChanges.setEnabled(selectedRepositoryTab.repository.supportIncomingOutgoing());
      menuItemIncomingChangesFrom.setEnabled(selectedRepositoryTab.repository.supportIncomingOutgoing());
      menuItemOutgoingChanges.setEnabled(selectedRepositoryTab.repository.supportIncomingOutgoing());
      menuItemOutgoingChangesTo.setEnabled(selectedRepositoryTab.repository.supportIncomingOutgoing());
      menuItemPullChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuItemPullChangesFrom.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuItemPushChanges.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuItemPushChangesTo.setEnabled(selectedRepositoryTab.repository.supportPullPush());
      menuSetFileMode.setEnabled(selectedRepositoryTab.repository.supportSetFileMode());
    }
    else
    {
      // disable menu entries
      menuItemApplyPatches.setEnabled(false);
      menuItemUnapplyPatches.setEnabled(false);
      menuItemLock.setEnabled(false);
      menuItemUnlock.setEnabled(false);
      menuItemIncomingChanges.setEnabled(false);
      menuItemIncomingChangesFrom.setEnabled(false);
      menuItemOutgoingChanges.setEnabled(false);
      menuItemOutgoingChangesTo.setEnabled(false);
      menuItemPullChanges.setEnabled(false);
      menuItemPullChangesFrom.setEnabled(false);
      menuItemPushChanges.setEnabled(false);
      menuItemPushChangesTo.setEnabled(false);
      menuSetFileMode.setEnabled(false);
    }
  }

  /** edit preferences
   */
  private void editPreferences()
  {
    // edit preferences
    Preferences preferences = new Preferences(shell,this);
    preferences.run();

    // update shell commands to menu
    updateShellCommands();
  }

  /** set new master password
   */
  private void editMasterPassword()
  {
    Composite composite;
    Label     label;
    Button    button;

    final Shell dialog = Dialogs.openModal(shell,"New master password",300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

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
}

/* end of file */
