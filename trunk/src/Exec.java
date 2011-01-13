/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Exec.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: execute external command
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;

/****************************** Classes ********************************/

/** execute external command
 */
class Exec
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final BufferedWriter stdin;
  public final BufferedReader stdout;
  public final BufferedReader stderr;

  private Process      process;
  private int          pid;
  private StringBuffer stdoutLine = new StringBuffer();
  private StringBuffer stderrLine = new StringBuffer();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** execute external command
   * @param path working directory or null
   * @param commandLine command line
   */
  public Exec(String path, String commandLine)
    throws IOException
  {
    // get shell
    String shell;
    if (Settings.hostSystem == Settings.HostSystems.WINDOWS)
    {
      shell = "bash.exe";
    }
    else
    {
      shell = "/bin/sh";
    }

    /* Workaround for a bug/incompatibility off Unix and Windows.
       The call of exec("FOO1=\"\"","FOO2=\"\"") become on
       Unix: FOO1="" FOO2=""
       Windows: FOO1=" FOO2="
       Someone seems to eat up the double ".
       Fix: escape " on Windows
    */
    if (Settings.hostSystem == Settings.HostSystems.WINDOWS)
    {
      commandLine = StringUtils.escape(commandLine,false,'"');
    }    

    // create command array
    String[] cmdArray = new String[]{shell,"-c","echo $$;"+commandLine};
    if (Settings.debugFlag)
    {
      System.err.println("DEBUG Execute: "+commandLine);
/*
      System.err.print("Execute:");
      for (String string :cmdArray)
      {
        System.err.print(" ");
        System.err.print(string);
      }
      System.err.println();
*/
    }

    // start process
    process = Runtime.getRuntime().exec(cmdArray,null,(path != null) ? new File(path) : null);
    stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
    stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String line = stdout.readLine();
    try
    {
      pid = Integer.parseInt(line);
    }
    catch (NumberFormatException exception)
    {
      throw new IOException("Cannot execute '"+commandLine+"' (error: "+exception.getMessage()+")");
    }
  }

  /** execute external command
   * @param path working directory or null
   * @param commandLine command line
   */
  public Exec(String path, StringBuffer commandLine)
    throws IOException
  {
    this(path,commandLine.toString());
  }

  /** execute external command
   * @param commandLine command line
   */
  public Exec(String commandLine)
    throws IOException
  {
    this(null,commandLine);
  }

  /** execute external command
   * @param commandLine command line
   */
  public Exec(StringBuffer commandLine)
    throws IOException
  {
    this(null,commandLine);
  }

  /** get next line from stdout
   * @return line or null
   */
  public String getNextLineStdout()
  {
    String line = null;

    try
    {
      line = stdout.readLine();
    }
    catch (IOException exception)
    {
      /* ignored => no input */
    }

    return line;
  }

  public String xgetNextLineStdout()
  {
    String line = null;

    /* Note: do not use readline(). This blocks until the line
             is complete even in non-blocking mode of the used
             file! Instead read char and check for EOL.
    */
    try
    {
      boolean eolFlag = false;
      while (stdout.ready() && !eolFlag)
      {
        char ch = (char)stdout.read();
        switch (ch)
        {
          case '\n':
            eolFlag = true;
            break;
          case '\r':
            break;
          default:
            stdoutLine.append(ch);
            break;
        }
      }
      if (eolFlag)
      {
        line = stdoutLine.toString();
        stdoutLine.setLength(0);
      }
    }
    catch (IOException exception)
    {
      /* ignored => no input */
    }
Dprintf.dprintf("line=%s",line);

    return line;
  }

  /** get next line from stderr
   * @return line or null
   */
  public String getNextLineStderr()
  {
    String line = null;

    /* Note: do not use readline(). This blocks until the line
             is complete even in non-blocking mode of the used
             file! Instead read char and check for EOL.
    */
    try
    {
      boolean eolFlag = false;
      while (stderr.ready() && !eolFlag)
      {
        char ch = (char)stderr.read();
        switch (ch)
        {
          case '\n':
            eolFlag = true;
            break;
          case '\r':
            break;
          default:
            stderrLine.append(ch);
            break;
        }
      }
      if (eolFlag)
      {
        line = stderrLine.toString();
        stderrLine.setLength(0);
      }
    }
    catch (IOException exception)
    {
      /* ignored => no input */
    }

    return line;
  }

  /** wait until exec terminated
   * @return exit value or -1
   */
  public int waitFor()
  {
    try
    {
      return process.waitFor();
    }
    catch (InterruptedException exception)
    {
      return -1;
    }
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

  /** suspend external command execution
   */
  public void suspend()
  {
    sendSignal("STOP");
  }

  /** resume external command execution
   */
  public void resume()
  {
    sendSignal("CONT");
  }

  /** cancel external command
   */
  public void cancel()
  {
    sendSignal("STOP");
    sendSignal("KILL");
  }

  //-----------------------------------------------------------------------

  /** get all PIDs of external command
   * @return array with PIDs
   */
  private int[] getAllPIDs()
  {
    HashSet<Integer> pidHashSet = new HashSet<Integer>();
    Process          process    = null;
    BufferedReader   stdout     = null;

    // get pids
    pidHashSet.add(pid);
    switch (Settings.hostSystem)
    {
      case LINUX:
      case SOLARIS:
      case MACOS:
      case QNX:
        try
        {
          process = Runtime.getRuntime().exec(new String[]{"ps","-A","-o","pid,ppid"});
          stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

          // ignore title line
          stdout.readLine();

          // read pids 
          String line;
          while ((line = stdout.readLine()) != null)
          {
            Object[] data = new Object[2];
            if (!StringParser.parse(line,"%d %d",data))
            {
              return null;
            }
            int pid  = (Integer)data[0];
            int ppid = (Integer)data[1];

            if (pidHashSet.contains(ppid)) pidHashSet.add(pid);
          }
        }
        catch (IOException exception)
        {
          return null;
        }
        finally
        {
          try
          {
            if (stdout != null) stdout.close();
            if (process != null) process.destroy();
          }
          catch (IOException exception)
          {
            // ignored
          }
        }
        break;
      case WINDOWS:
        break;
    }

    // convert to int-array
    int[] pids = new int[pidHashSet.size()];
    int z = 0;
    for (Integer pid : pidHashSet)
    {
      pids[z] = (int)pid;
      z++;
    }

    return pids;
  }

  /** send signal to external command and all sub-processes
   * @param signal signal to send
   */
  private void sendSignal(String signal)
  {
    // get all pids of sub-processes
    int[] pids = getAllPIDs();
//for (int pid : pids) Dprintf.dprintf("send %s to %d\n",signal,pid);
//Dprintf.dprintf("pids=%s\n",pids);

    // build command
    String[] cmdArray = new String[1+2+pids.length];
    cmdArray[0] = "kill";
    cmdArray[1] = "-s";
    cmdArray[2] = signal;
    for (int z = 0; z < pids.length; z++)
    {
      cmdArray[3+z] = Integer.toString(pids[z]);
    }

    // execute
    try
    {
      Process process = Runtime.getRuntime().exec(cmdArray);
      process.waitFor();
    }
    catch (IOException exception)
    {
      /* ignored */
    }
    catch (InterruptedException exception)
    {
      /* ignored */
    }
  }
}

/* end of file */
