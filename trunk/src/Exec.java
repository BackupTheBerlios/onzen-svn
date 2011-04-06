/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Exec.java,v $
* $Revision: 1.4 $
* $Author: torsten $
* Contents: execute external command
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.Stack;

/****************************** Classes ********************************/

/** external command
 */
class Command
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private ArrayList<String> commandLine;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create external command
   */
  Command()
  {
    commandLine = new ArrayList<String>();
  }

  /** clear command array
   */
  public void clear()
  {
    commandLine.clear();
  }

  /** append arguments to command array
   * @param arguments strings to append
   */
  public void append(Object... arguments)
  {
    for (Object object : arguments)
    {
      commandLine.add(object.toString());
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
    }
  }

  /** get command array
   * @return command array
   */
  public String[] getCommandArray()
  {
    return commandLine.toArray(new String[commandLine.size()]);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    for (String string : commandLine)
    {
      if (buffer.length() > 0) buffer.append(' ');
      buffer.append(string);
    }

    return buffer.toString();
  }
}

/** execute external command
 */
class Exec
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private static HashSet<Process> processHash = new HashSet<Process>();

  private final BufferedWriter  stdin;
  private final BufferedReader  stdout;
  private final BufferedReader  stderr;
  private final Stack<String>   stdoutStack = new Stack<String>();
  private final Stack<String>   stderrStack = new Stack<String>();
  private final DataInputStream stdoutBinary;

  private Process               process;
  private int                   pid;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** execute external command
   * @param path working directory or null
   * @param subDirectory working subdirectory or null
   * @param command command to execute
   * @param binaryFlag true to read stdout in binary mode
   */
  public Exec(String path, String subDirectory, Command command, boolean binaryFlag)
    throws IOException
  {
    // get command array
    String[] commandArray = command.getCommandArray();
    if (Settings.debugFlag)
    {
      System.err.println("DEBUG execute "+path+((subDirectory != null) ? File.separator+subDirectory : "")+": "+StringUtils.join(commandArray));
    }

    // start process
    File workingDirectory;
    if      ((path != null) && (subDirectory != null)) workingDirectory = new File(path,subDirectory);
    else if (path != null)                             workingDirectory = new File(path);
    else                                               workingDirectory = null;
    process = Runtime.getRuntime().exec(commandArray,null,workingDirectory);
    processHash.add(process);
    stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    if (binaryFlag)
    {
      stdout       = null;
      stdoutBinary = new DataInputStream(process.getInputStream());
    }
    else
    {
      stdout       = new BufferedReader(new InputStreamReader(process.getInputStream()));
      stdoutBinary = null;
    }
    stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
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
   */
  public Exec(String path, String subDirectory, Command command)
    throws IOException
  {
    this(path,subDirectory,command,false);
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

  /** get next line from stdout
   * @return line or null
   */
  public String getStdout()
  {
    String line = null;

    if (stdoutStack.isEmpty())
    {
      if (stdout != null)
      {
        try
        {
          line = stdout.readLine();
        }
        catch (IOException exception)
        {
          /* ignored => no input */
        }
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
        line = stderr.readLine();
      }
      catch (IOException exception)
      {
        /* ignored => no input */
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

  /** read data from stdout
   * @return line or null
   */
  public int readStdout(byte[] buffer)
  {
    int n = 0;

    if (stdoutBinary != null)
    {
      try
      {
        n = stdoutBinary.read(buffer);
      }
      catch (IOException exception)
      {
        /* ignored => no input */
      }
    }

    return n;
  }

  /** wait until exec terminated
   * @return exit value or -1
   */
  public int waitFor()
  {
    int exitCode = -1;

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

  /** check if external command terminated
   * @return true iff external command terminated, false otherwise
   */
  public boolean terminated()
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

  //-----------------------------------------------------------------------
}

/* end of file */
