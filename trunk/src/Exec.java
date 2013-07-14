/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: execute external command
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/****************************** Classes ********************************/

/** external command
 */
class Command
{
  /** hidden string class
   */
  class Hidden
  {
    private String string;
    private String printableString;

    /** create hidden string
     * @param string string
     * @param printableString printable string
     */
    Hidden(String string, String printableString)
    {
      this.string          = string;
      this.printableString = printableString;
    }

    /** create hidden string
     * @param string string
     */
    Hidden(String string)
    {
      this(string,null);
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return string;
    }
  };

  // --------------------------- constants --------------------------------
  private final String HIDDEN = "********";

  // --------------------------- variables --------------------------------
  private ArrayList<String> commandLine;           // command line arguments
  private ArrayList<String> printableCommandLine;  // command line arguments for printing

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create external command
   */
  Command()
  {
    commandLine          = new ArrayList<String>();
    printableCommandLine = new ArrayList<String>();
  }

  /** create external command
   * @param command command to execute
   * @param arguments arguments for command
   */
  Command(String command, String... arguments)
  {
    this();

    append(command);
    for (String argument : arguments)
    {
      append(argument);
    }
  }

  /** create external command
   * @param macro macro with command
   */
  Command(Macro macro)
  {
    this();

    for (Object value : macro.getValueArray())
    {
      if (value instanceof Macro.Hidden)
      {
        append(hidden(value.toString()));
      }
      else
      {
        append(value);
      }
    }
  }

  /** clear command array
   */
  public void clear()
  {
    commandLine.clear();
    printableCommandLine.clear();
  }

  /** append macro to command array
   * @param macro macro to append
   */
  public void append(Macro macro)
  {
    for (Object value : macro.getValueArray())
    {
      if (value instanceof Macro.Hidden)
      {
        append(hidden(value.toString()));
      }
      else
      {
        append(value);
      }
    }
  }

  /** append arguments to command array
   * @param arguments strings to append
   */
  public void append(Object... arguments)
  {
    for (Object object : arguments)
    {
      commandLine.add(object.toString());
      if (object instanceof Hidden)
      {
        printableCommandLine.add(HIDDEN);
      }
      else
      {
        printableCommandLine.add(object.toString());
      }
    }
  }

  /** append arguments to command array
   * @param strings strings to append
   */
  public void append(String[] strings)
  {
    for (String string : strings)
    {
      commandLine.add(string);
      printableCommandLine.add(string);
    }
  }

  /** concat arguments to last entry in command array
   * @param strings strings to concat
   */
  public void concat(Object... arguments)
  {
    for (Object object : arguments)
    {
      if (commandLine.size() > 0)
      {
        commandLine.set(commandLine.size()-1,
                        commandLine.get(commandLine.size()-1).concat(object.toString())
                       );
      }
      else
      {
        commandLine.add(object.toString());
      }
      if (printableCommandLine.size() > 0)
      {
        printableCommandLine.set(printableCommandLine.size()-1,
                                 printableCommandLine.get(printableCommandLine.size()-1).concat((object instanceof Hidden) ? HIDDEN : object.toString())
                                );
      }
      else
      {
        printableCommandLine.add((object instanceof Hidden) ? HIDDEN : object.toString());
      }
    }
  }

  /** concat arguments to last entry in command array
   * @param strings strings to concat
   */
  public void concat(String[] strings)
  {
    for (String string : strings)
    {
      if (commandLine.size() > 0)
      {
        commandLine.set(commandLine.size()-1,
                        commandLine.get(commandLine.size()-1).concat(string)
                       );
      }
      else
      {
        commandLine.add(string);
      }
      if (commandLine.size() > 0)
      {
        printableCommandLine.add(string);
      }
      else
      {
        printableCommandLine.add(string);
      }
    }
  }

  /** append first argument and concat other arguments to command array
   * @param strings strings to concat
   */
  public void appendConcat(Object... arguments)
  {
    if (arguments.length > 0)
    {
      append(arguments[0]);
      for (int i = 1; i < arguments.length; i++)
      {
        concat(arguments[i]);
      }
    }
  }

  /** append first argument and concat other arguments to command array
   * @param strings strings to concat
   */
  public void appendConcat(String[] strings)
  {
    if (strings.length > 0)
    {
      append(strings[0]);
      for (int i = 1; i < strings.length; i++)
      {
        concat(strings[i]);
      }
    }
  }

  /** get command array
   * @return command array
   */
  public String[] getCommandArray()
  {
    return commandLine.toArray(new String[commandLine.size()]);
  }

  /** create hidden argument
   * @param string argument
   * @return hidden argument
   */
  public Hidden hidden(String string)
  {
    return new Hidden(string);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    for (String string : printableCommandLine)
    {
      if (buffer.length() > 0) buffer.append(' ');
      if (string.isEmpty() || string.indexOf(' ') >= 0)
      {
        buffer.append(StringUtils.escape(string,'"'));
      }
      else
      {
        buffer.append(string);
      }
    }

    return buffer.toString();
  }
}

/** execute external command
 */
public class Exec
{
  // --------------------------- constants --------------------------------
  public final static long WAIT_FOREVER = -1;

  // --------------------------- variables --------------------------------
  private static HashSet<Process> processHash = new HashSet<Process>();

  private Process             process;
  private long                timeout;
  private PrintWriter         stdin = null;
  private BufferedReader      stdout = null;
  private DataInputStream     stdoutBinary = null;;
  private BufferedReader      stderr = null;
  private final Stack<String> stdoutStack = new Stack<String>();
  private final Stack<String> stderrStack = new Stack<String>();

  private ArrayList<String>   stdoutList = new ArrayList<String>();
  private ArrayList<String>   stderrList = new ArrayList<String>();
  private int                 exitCode = -1;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param commandArray command line array
   * @param printableCommandLine printable command line
   * @param timeout timeoout [ms] or WAIT_FOREVER
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, String subDirectory, String[] commandArray, String printableCommandLine, long timeout, boolean binaryFlag)
    throws IOException
  {
    // initialise variables
    this.timeout = timeout;

    if (Settings.debugFlag)
    {
      System.err.println("DEBUG execute "+path+((subDirectory != null) ? File.separator+subDirectory : "")+": "+printableCommandLine);
    }

    // get working directory
    File workingDirectory;
    if      ((path != null) && (subDirectory != null)) workingDirectory = new File(path,subDirectory);
    else if (path != null)                             workingDirectory = new File(path);
    else                                               workingDirectory = null;

    // start process. Note: set LANG=en_US.UTF-8 to force English output
    ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
    Map<String, String> environment = processBuilder.environment();
    environment.put("LANG","en_US.UTF-8");
    processBuilder.directory(workingDirectory);
    process = processBuilder.start();
    processHash.add(process);

    /*
    TODO: timeout for exec
    if (timeout != WAIT_FOREVER)
    {
      Timer timer = new Timer();
      timer.schedule(new TimerTask()
        {
          public void run()
          {
Dprintf.dprintf("");
process.destroy();
          }
        },
        timeout);
    }
    */
  }

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param command command to execute
   * @param timeout timeoout [ms] or WAIT_FOREVER
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, String subDirectory, Command command, long timeout, boolean binaryFlag)
    throws IOException
  {
    this(path,subDirectory,command.getCommandArray(),command.toString(),timeout,binaryFlag);
  }

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param command command to execute
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, String subDirectory, Command command, boolean binaryFlag)
    throws IOException
  {
    this(path,subDirectory,command.getCommandArray(),command.toString(),WAIT_FOREVER,binaryFlag);
  }

  /** execute external command
   * @param path working directory or null
   * @param command command to execute
   * @param timeout timeoout [ms] or WAIT_FOREVER
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, Command command, long timeout, boolean binaryFlag)
    throws IOException
  {
    this(path,null,command,timeout,binaryFlag);
  }

  /** execute external command
   * @param path working directory or null
   * @param command command to execute
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, Command command, boolean binaryFlag)
    throws IOException
  {
    this(path,null,command,binaryFlag);
  }

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param command command to execute
   * @param timeout timeoout [ms] or WAIT_FOREVER
   */
  public Exec(String path, String subDirectory, Command command, long timeout)
    throws IOException
  {
    this(path,subDirectory,command,timeout,false);
  }

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param command command to execute
   */
  public Exec(String path, String subDirectory, Command command)
    throws IOException
  {
    this(path,subDirectory,command,false);
  }

  /** execute external command
   * @param path working directory or null
   * @param command command to execute
   * @param timeout timeoout [ms] or WAIT_FOREVER
   */
  public Exec(String path, Command command, long timeout)
    throws IOException
  {
    this(path,command,timeout,false);
  }

  /** execute external command
   * @param path working directory or null
   * @param command command to execute
   */
  public Exec(String path, Command command)
    throws IOException
  {
    this(path,command,false);
  }

  /** execute external command
   * @param command command to execute
   * @param timeout timeoout [ms] or WAIT_FOREVER
   */
  public Exec(Command command, long timeout)
    throws IOException
  {
    this(null,command,timeout);
  }

  /** execute external command
   * @param command command to execute
   */
  public Exec(Command command)
    throws IOException
  {
    this(null,command);
  }

  /** done execute command
   * @return exit value or -1
   */
  public int done()
  {
    return waitFor();
  }

  /** get stdin stream
   * @return stream
   */
  public OutputStream getStdinStream()
  {
    return process.getOutputStream();
  }

  /** get stdout stream
   * @return stream
   */
  public InputStream getStdoutStream()
  {
    return process.getInputStream();
  }

  /** get stderr stream
   * @return stream
   */
  public InputStream getStderrStream()
  {
    return process.getErrorStream();
  }

  public void putStdin(String format, Object... arguments)
  {
    if (stdin == null)
    {
      stdin = new PrintWriter(getStdinStream());
    }

    stdin.println(String.format(format,arguments));
  }

  /** close stdin stream
   * @return stream
   */
  public void closeStdin()
  {
    try
    {
      getStdinStream().close();
    }
    catch (IOException exception)
    {
      // ignored
    }
  }

  /** get next line from stdout
   * @return line or null at EOF
   */
  public String getStdout()
  {
    String line = null;

    if (stdoutStack.isEmpty())
    {
      try
      {
        if (stdout == null)
        {
          stdout = new BufferedReader(new InputStreamReader(getStdoutStream()));
        }

        if (stdout != null)
        {
          line = stdout.readLine();
        }
      }
      catch (IOException exception)
      {
        /* ignored => no input */
      }
    }
    else
    {
      line = stdoutStack.pop();
    }

    return line;
  }

  /** push back line from stdout
   * @param  line
   */
  public void ungetStdout(String line)
  {
    if (line != null) stdoutStack.push(line);
  }

  /** poll next line from stdout
   * @param timeout timeout [ms]
   * @return line or null
   */
  public String pollStdout(int timeout)
  {
    String line = null;

    if (stdoutStack.isEmpty())
    {
      try
      {
        if (stdout == null)
        {
          stdout = new BufferedReader(new InputStreamReader(getStdoutStream()));
        }

        if (stdout != null)
        {
          if (stdout.ready())
          {
            line = stdout.readLine();
          }
          while ((line == null) && (timeout != 0))
          {
            if (line == null)
            {
              // wait a short time for data
              int n;
              if (timeout > 0)
              {
                n = Math.min(100,timeout);
                timeout -= n;
              }
              else
              {
                n = 100;
              }
              try { Thread.sleep(100); } catch (InterruptedException exception) { /* ignored */ }
            }

            // try to read data
            if (stdout.ready())
            {
              line = stdout.readLine();
            }
          }
        }
      }
      catch (IOException exception)
      {
        /* ignored => no input */
      }
    }
    else
    {
      line = stdoutStack.pop();
    }

    return line;
  }

  /** poll next line from stdout
   * @return line or null
   */
  public String pollStdout()
  {
    return pollStdout(250);
  }

  /** peek  (do not get) next line from stdout
   * @return line or null
   */
  public String peekStdout()
  {
    String line = getStdout();
    ungetStdout(line);
    return line;
  }

  /** discard next line from stdout
   */
  public void discardStdout()
  {
    getStdout();
  }

  /** check if EOF of stdout
   * @return true iff EOF
   */
  public boolean eofStdout()
  {
    String line = getStdout();
    ungetStdout(line);
    return line == null;
  }

  /** get next line from stderr
   * @return line or null
   */
  public String getStderr()
  {
    String line = null;

    if (stderrStack.isEmpty())
    {
      try
      {
        if (stderr == null)
        {
          stderr = new BufferedReader(new InputStreamReader(getStderrStream()));
        }

        if (stderr != null)
        {
          line = stderr.readLine();
        }
      }
      catch (IOException exception)
      {
        // ignored => no input
      }
    }
    else
    {
      line = stderrStack.pop();
    }

    return line;
  }

  /** push back line from stderr
   * @param  line
   */
  public void ungetStderr(String line)
  {
    if (line != null) stderrStack.push(line);
  }

  /** poll next line from stderr
   * @param timeout timeout [ms]
   * @return line or null
   */
  public String pollStderr(int timeout)
  {
    String line = null;

    if (stderrStack.isEmpty())
    {
      try
      {
        if (stderr == null)
        {
          stderr = new BufferedReader(new InputStreamReader(getStderrStream()));
        }

        if (stderr != null)
        {
          if (stderr.ready())
          {
            line = stderr.readLine();
          }
          while ((line == null) && (timeout != 0))
          {
            if (line == null)
            {
              // wait a short time for data
              int n;
              if (timeout > 0)
              {
                n = Math.min(100,timeout);
                timeout -= n;
              }
              else
              {
                n = 100;
              }
              try { Thread.sleep(100); } catch (InterruptedException exception) { /* ignored */ }
            }

            // try to read data
            if (stderr.ready())
            {
              line = stderr.readLine();
            }
          }
        }
      }
      catch (IOException exception)
      {
        // ignored => no input
      }
    }
    else
    {
      line = stderrStack.pop();
    }

    return line;
  }

  /** poll next line from stderr
   * @return line or null
   */
  public String pollStderr()
  {
    return pollStderr(250);
  }

  /** peek (do not get) next line from stderr
   * @return line or null
   */
  public String peekStderr()
  {
    String line = getStderr();
    ungetStderr(line);
    return line;
  }

  /** discard next line from stderr
   */
  public void discardStderr()
  {
    getStderr();
  }

  /** check if EOF of stdout
   * @return true iff EOF
   */
  public boolean eofStderr()
  {
    String line = getStderr();
    ungetStderr(line);
    return line == null;
  }

  /** read data from stdout
   * @return line or null
   */
  public int readStdout(byte[] buffer)
  {
    int n = 0;

    if (stdoutBinary != null)
    {
      stdoutBinary = new DataInputStream(getStdoutStream());
    }

    if (stdoutBinary != null)
    {
      try
      {
        n = stdoutBinary.read(buffer);
      }
      catch (IOException exception)
      {
        // ignored => no input
      }
    }

    return n;
  }

  /** wait until exec terminated
   * @return exit value or -1
   */
  public int waitFor()
  {
    // store stdout, stderr
    String line;
    while ((line = getStdout()) != null)
    {
      stdoutList.add(line);
    }
    while ((line = getStderr()) != null)
    {
      stderrList.add(line);
    }

    // wait for process
    try
    {
      exitCode = process.waitFor();
      processHash.remove(process);
    }
    catch (InterruptedException exception)
    {
      exitCode = -1;
    }

    return exitCode;
  }

  /** destroy process
   */
  public void destroy()
  {
    process.destroy();
  }

  /** check if external command terminated
   * @return true iff external command terminated, false otherwise
   */
  public boolean isTerminated()
  {
    try
    {
      process.exitValue();
      return true;
    }
    catch (IllegalThreadStateException exception)
    {
      return false;
    }
  }

  /** get extended error message lines
   * @return message lines array
   */
  public String[] getExtendedErrorMessage()
  {
    ArrayList<String> extendedErrorMessageList = new ArrayList<String>();

    for (String line : stderrList)
    {
      extendedErrorMessageList.add(line);
    }

    return extendedErrorMessageList.toArray(new String[extendedErrorMessageList.size()]);
  }

  /** print stdout output to stdout
   */
  public void printStdout()
  {
    if (stdoutList.size() > 0)
    {
      System.out.println("Stdout:");
      System.out.println("");
      for (String string : stdoutList)
      {
        System.out.println(string);
      }
    }
  }

  /** print stderr output to stdout
   */
  public void printStderr()
  {
    if (stderrList.size() > 0)
    {
      System.out.println("Stderr:");
      System.out.println("");
      for (String string : stderrList)
      {
        System.out.println(string);
      }
    }
  }

  //-----------------------------------------------------------------------
}

/* end of file */
