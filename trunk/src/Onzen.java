/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Onzen.java,v $
* $Revision: 1.1 $
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
import java.security.KeyStore;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Locale;

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
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
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
  // --------------------------- constants --------------------------------
  private static final Option[] options =
  {

    new Option("--help",              "-h",Options.Types.BOOLEAN,    "helpFlag"),

    new Option("--debug",             null,Options.Types.BOOLEAN,    "debugFlag"),

    new Option("--cvs-prune-empty-directories",null,Options.Types.BOOLEAN,"cvsPruneEmtpyDirectories"),

    // ignored
    new Option("--swing",             null, Options.Types.BOOLEAN,   null),
  };

  // --------------------------- variables --------------------------------
  private Display       display;
  private Shell         shell;

  private TabFolder     widgetTabFolder;

  private Button        widgetButtonUpdate;
  private Button        widgetButtonCommit;
  private Button        widgetButtonAdd;

  private Label         widgetStatus;

  private RepositoryTab selectedRepositoryTab;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** print error to stderr
   * @param format format string
   * @param args optional arguments
   */
  private void printError(String format, Object... args)
  {
    System.err.println("ERROR: "+String.format(format,args));
  }

  /** print warning to stderr
   * @param format format string
   * @param args optional arguments
   */
  private void printWarning(String format, Object... args)
  {
    System.err.println("Warning: "+String.format(format,args));
  }

  /** print program usage
   * @param
   * @return
   */
  private void printUsage()
  {
    System.out.println("onzen usage: <options> --");
    System.out.println("");
    System.out.println("Options: ");
    System.out.println("");
    System.out.println("         -h|--help                      - print this help");
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
        z++;
      }
    }

    // help
    if (Settings.helpFlag)
    {
      printUsage();
      System.exit(0);
    }

    // check arguments
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
    widgetTabFolder.addSelectionListener(new SelectionListener()
    {
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        TabFolder tabFolder = (TabFolder)selectionEvent.widget;
        TabItem   tabItem = (TabItem)selectionEvent.item;

        selectedRepositoryTab = (RepositoryTab)tabItem.getData();;
      }
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
    });

Dprintf.dprintf("");
    Repository repository = new RepositoryCVS("/home/torsten/projects/onzen");
    RepositoryTab repositoryTab = new RepositoryTab(widgetTabFolder,"Oonzen",repository);

    // create buttons
    composite = Widgets.newComposite(shell);
    composite.setLayout(new TableLayout(0.0,1.0,2));
    Widgets.layout(composite,1,0,TableLayoutData.W);
    {
      widgetButtonUpdate = Widgets.newButton(composite,"Update");
      Widgets.layout(widgetButtonUpdate,0,0,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetButtonUpdate.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.update();
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      widgetButtonCommit = Widgets.newButton(composite,"Commit");
      Widgets.layout(widgetButtonCommit,0,1,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetButtonCommit.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.commit();
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Patch");
button.setEnabled(false);
      Widgets.layout(button,0,2,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          if (selectedRepositoryTab != null)
          {
//            selectedRepositoryTab.patch();
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      widgetButtonAdd = Widgets.newButton(composite,"Add");
      Widgets.layout(widgetButtonAdd,0,3,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      widgetButtonAdd.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
          if (selectedRepositoryTab != null)
          {
            selectedRepositoryTab.add();
          }
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Remove");
button.setEnabled(false);
      Widgets.layout(button,0,4,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
//          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Revert");
button.setEnabled(false);
      Widgets.layout(button,0,5,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
//          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Diff");
button.setEnabled(false);
      Widgets.layout(button,0,6,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
//          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      button = Widgets.newButton(composite,"Solve");
button.setEnabled(false);
      Widgets.layout(button,0,7,TableLayoutData.W,0,0,0,0,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
//          Dialogs.close(dialog,false);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
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
          default:
            break;
          case SWT.F5:
            if (selectedRepositoryTab != null)
            {
              selectedRepositoryTab.updateStates();
            }
            event.doit = false;
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
      menuItem = Widgets.addMenuItem(menu,"Open repository...",SWT.CTRL+'O');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          openRepository();
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Close repository...",SWT.CTRL+'W');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          closeRepository();
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Edit repository...",SWT.CTRL+'E');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          editRepository();
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      Widgets.addMenuSeparator(menu);

      menuItem = Widgets.addMenuItem(menu,"Quit",SWT.CTRL+'Q');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;

          // send close-evemnt to shell
          Event event = new Event();
          shell.notifyListeners(SWT.Close,event);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"Command");
    {
      menuItem = Widgets.addMenuItem(menu,"Update",SWT.CTRL+'U');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          Widgets.invoke(widgetButtonUpdate);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Commit...",'#');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          Widgets.invoke(widgetButtonCommit);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });

      menuItem = Widgets.addMenuItem(menu,"Add...",'+');
      menuItem.addSelectionListener(new SelectionListener()
      {
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          Widgets.invoke(widgetButtonAdd);
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }

    menu = Widgets.addMenu(menuBar,"View");
    {
    }

    menu = Widgets.addMenu(menuBar,"File/Directory");
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
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          MenuItem widget = (MenuItem)selectionEvent.widget;
          Dialogs.info(shell,"About","Onzen "+Config.VERSION_MAJOR+"."+Config.VERSION_MINOR+".\n\nWritten by Torsten Rupp.");
        }
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
      });
    }

    if (Settings.debugFlag)
    {
      menu = Widgets.addMenu(menuBar,"Debug");
      {
      }
    }
  }

  /** run application
   */
  private void run()
  {
    // set window size, manage window (approximate height according to height of a text line)
    shell.setSize(840,600+5*(Widgets.getTextHeight(shell)+4));
    shell.open();

    // add close listener
    shell.addListener(SWT.Close,new Listener()
    {
      public void handleEvent(Event event)
      {
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
  }

  /** open new repository tab
   * @param rootPath root path
   */
  private void openRepository(String rootPath)
  {
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
        return;
    }

    RepositoryTab repositoryTab = new RepositoryTab(widgetTabFolder,rootPath,repository);
  }

  /** open new repository tab
   */
  private void openRepository()
  {
    String rootPath = Dialogs.directory(shell,"Open repository","");
    if (rootPath != null)
    {
      openRepository(rootPath);
    }
  }

  /** close selected repository tab
   */
  private void closeRepository()
  {
    if (selectedRepositoryTab != null)
    {
      RepositoryTab repositoryTab = selectedRepositoryTab;

      selectedRepositoryTab = null;
      repositoryTab.close();
    }
  }

  /** edit selected repository tab
   */
  private void editRepository()
  {
    if (selectedRepositoryTab != null)
    {
      RepositoryTab repositoryTab = selectedRepositoryTab;

      repositoryTab.edit();
    }
  }

  /** onzen main
   * @param args command line arguments
   */
  Onzen(String[] args)
  {
    selectedRepositoryTab = null;

    try
    {
      // load settings
      Settings.load();

      // parse arguments
      parseArguments(args);

      // start message broadcast
      Message.startBroadcast();
Message.addHistory("Hello 1");
Message.addHistory("Tral\nlala");
Message.addHistory("Und nun?");

      // init display
      display = new Display();

      // open main window
      createWindow();
      createMenu();

      // run
      run();

      // save settings
      Settings.save();
    }
    catch (org.eclipse.swt.SWTException exception)
    {
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
      System.err.println("INTERNAL ERROR: "+assertionError.toString());
      for (StackTraceElement stackTraceElement : assertionError.getStackTrace())
      {
        System.err.println("  "+stackTraceElement);
      }
      System.err.println("");
      System.err.println("Please report this assertion error to torsten.rupp@gmx.net.");
    }
    catch (InternalError error)
    {
      System.err.println("INTERNAL ERROR: "+error.getMessage());
    }
    catch (Error error)
    {
      System.err.println("ERROR: "+error.getMessage());
      if (Settings.debugFlag)
      {
        for (StackTraceElement stackTraceElement : error.getStackTrace())
        {
          System.err.println("  "+stackTraceElement);
        }
      }
    }
  }

  /** main
   * @param args command line arguments
   */
  public static void main(String[] args)
  {
    new Onzen(args);
  }
}

/* end of file */
