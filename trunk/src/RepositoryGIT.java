/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: repository
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.DateFormat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/****************************** Classes ********************************/

/** Git repository
 */
class RepositoryGIT extends Repository
{
  // --------------------------- constants --------------------------------
  public final static String[] DEFAULT_REVISION_NAMES = new String[]{"HEAD"};
  public final static String[] DEFAULT_BRANCH_NAMES   = new String[]{"master","branches"};
  public final static String   DEFAULT_ROOT_NAME      = "master";
  public final static String   DEFAULT_BRANCHES_NAME  = "branches";
  public final static String   FIRST_REVISION_NAME    = "0";
  public final static String   LAST_REVISION_NAME     = "HEAD";

  // --------------------------- variables --------------------------------
  private final static RepositoryGIT staticInstance = new RepositoryGIT();

  @XmlElement(name = "userName")
  @RepositoryValue(title = "User name:", tooltip="GIT server user login name.")
  public String userName;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get static instance
   * @return static instance
   */
  public final static RepositoryGIT getInstance()
  {
    return staticInstance;
  }

  /** create repository
   * @param rootPath root path
   */
  RepositoryGIT(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositoryGIT()
  {
    this(null);
  }

  /** check if repository support pull/push commands
   * @return true iff pull/push commands are supported
   */
  public boolean supportPullPush()
  {
    return true;
  }

  /** create new repository module
   * @param repositoryPath repository server
   * @param moduleName module name
   * @param importPath import directory
   */
  public void create(String repositoryPath, String moduleName, String importPath)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** checkout repository
   * @param repositoryPath repository server
   * @param moduleName module name
   * @param revision revision to checkout
   * @param userName user name or ""
   * @param password password or ""
   * @param destinationPath destination path
   * @param busyDialog busy dialog or null
   */
  public void checkout(String repositoryPath, String moduleName, String revision, String userName, String password, String destinationPath, BusyDialog busyDialog)
    throws RepositoryException
  {
    final Pattern PATTERN_URI1 = Pattern.compile("^([^:/]+)://([^:]*):([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI2 = Pattern.compile("^([^:/]+)://([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI3 = Pattern.compile("^([^:/]+)://(.*)",Pattern.CASE_INSENSITIVE);

    File tmpDirectory = null;
    Exec exec         = null;
    try
    {
      Command command = new Command();

      // create temporary directory
      tmpDirectory = FileUtils.createTempDirectory("git",".tmp",new File(destinationPath).getParentFile());

      // checkout
      command.clear();
      command.append(Settings.gitCommand,"clone","--verbose","--progress");
      if ((revision != null) && !revision.isEmpty()) command.append("--branch",revision);
      Matcher matcher;
      if      ((matcher = PATTERN_URI1.matcher(repositoryPath)).matches())
      {
        command.appendConcat(matcher.group(1),
                             "://",
                             matcher.group(2),
                             ":",
                             command.hidden(matcher.group(3)),
                             "@",
                             matcher.group(4),
                             "/",
                             moduleName
                            );
      }
      else if ((matcher = PATTERN_URI2.matcher(repositoryPath)).matches())
      {
        if (password.isEmpty())
        {
          throw new RepositoryException("no password given");
        }
        command.appendConcat(matcher.group(1),
                             "://",
                             matcher.group(2),
                             ":",
                             command.hidden(password),
                             "@",
                             matcher.group(3),
                             "/",
                             moduleName
                            );
      }
      else if ((matcher = PATTERN_URI3.matcher(repositoryPath)).matches())
      {
        if (userName.isEmpty())
        {
          userName = System.getProperty("user.name");
        }
        if (password.isEmpty())
        {
          throw new RepositoryException("no password given");
        }
        command.appendConcat(matcher.group(1),
                             "://",
                             userName,
                             ":",
                             command.hidden(password),
                             "@",
                             matcher.group(2),
                             "/",
                             moduleName
                            );
      }
      else
      {
        command.append("file://"+repositoryPath+"/"+moduleName);
      }
      command.append(tmpDirectory);
      exec = new Exec(destinationPath,command,10*1000);

      // read output
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && !exec.isTerminated()
            )
      {
        String line;

        // read stdout
        line = exec.pollStdout();
        if (line != null)
        {
//Dprintf.dprintf("out: %s",line);
          if (busyDialog != null) busyDialog.updateList(line);
        }
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        // wait for termination
Dprintf.dprintf("");
        int exitCode = exec.waitFor();
Dprintf.dprintf("");
        if (exitCode == 0)
        {
          try
          {
Dprintf.dprintf("");
            if (busyDialog != null) busyDialog.updateList("Move files into '"+destinationPath+"'...");
Dprintf.dprintf("");
            FileUtils.moveDirectoryTree(tmpDirectory,new File(destinationPath));
Dprintf.dprintf("");
          }
          catch (IOException exception)
          {
Dprintf.dprintf("");
            throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
          }
        }
        else
        {
Dprintf.dprintf("");
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }
      }
      else
      {
        // abort
        exec.destroy();
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      try { FileUtils.deleteDirectoryTree(destinationPath); } catch (IOException ignoredException) { /* ignored */ }
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
      if (tmpDirectory != null) try { FileUtils.deleteDirectoryTree(tmpDirectory); } catch (IOException ignoredException) { /* ignored */ }
    }
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
    throws RepositoryException
  {
    final Pattern PATTERN_STATUS         = Pattern.compile("^\\s*(\\S+)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_CURRENT_BRANCH = Pattern.compile("^\\*\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

    // all not reported files have state OK
    for (FileData fileData : fileDataSet)
    {
      fileData.state = FileData.States.OK;
    }

    // get repository absolute path
    String absolutePath = getAbsolutePath();

    // get status of files
    Command         command            = new Command();
    Exec            exec;
    String          line;
    Matcher         matcher;
    String          name               = null;
    FileData.States state              = FileData.States.UNKNOWN;
    FileData.Modes  mode               = FileData.Modes.UNKNOWN;
    String          workingRevision    = "";
    String          repositoryRevision = "";
    for (String fileDirectory : fileDirectorySet)
    {
      exec = null;
      try
      {
        // get status
        command.clear();
        command.append(Settings.gitCommand,"status","-s","--porcelain","--untracked-files=all");
        command.append("--");
        command.append(!fileDirectory.isEmpty() ? fileDirectory : ".");
        exec = new Exec(rootPath,command);

        // parse status data
        String directory = new File(rootPath,fileDirectory).getPath();
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // match state, name
          if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
          {
            state = parseState(matcher.group(1));
            name  = StringUtils.unescape(matcher.group(2));
//Dprintf.dprintf("name=%s",name);

            FileData fileData;
            fileData = findFileData(fileDataSet,name);
            if      (fileData != null)
            {
              fileData.state              = state;
              fileData.mode               = mode;
              fileData.workingRevision    = workingRevision;
              fileData.repositoryRevision = repositoryRevision;
            }
            else if (   (newFileDataSet != null)
                     && !isHiddenFile(name)
                    )
            {
              // check if file is not in sub-directory (git list all files :-()
              String subDirectory = new File(absolutePath,name).getParent();
//Dprintf.dprintf("directory=%s new File(absolutePath,name)=%s subDirectory=%s",directory,new File(absolutePath,name),subDirectory);
              if (   ((subDirectory == null) && fileDirectory.isEmpty())
                  || ((subDirectory != null) && subDirectory.equals(directory))
                 )
              {
                // get file type, size, date/time
                File file               = new File(rootPath,name);
                FileData.Types type     = getFileType(file);
                long           size     = file.length();
                Date           datetime = new Date(file.lastModified());

                // create file data
Dprintf.dprintf("add new %s",name);
                newFileDataSet.add(new FileData(name,
                                                type,
                                                state,
                                                mode,
                                                false,
                                                size,
                                                datetime,
                                                workingRevision,
                                                repositoryRevision
                                               )
                                  );
              }
            }
          }
          else
          {
            // unknown line
            Onzen.printWarning("No match for update states line '%s'",line);
          }
        }

        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
        // ignored
      }
      finally
      {
        if (exec != null) exec.done();
      }

      // get branch
      exec = null;
      try
      {
        command.clear();
        command.append(Settings.gitCommand,"branch");
        command.append("--");
        exec = new Exec(rootPath,command);
        // parse branch
        while ((line = exec.getStdout()) != null)
        {
          if ((matcher = PATTERN_CURRENT_BRANCH.matcher(line)).matches())
          {
            name = matcher.group(1);

            for (FileData fileData : fileDataSet)
            {
              fileData.branch = name;
            }
          }
        }

        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
        // ignored
      }
      finally
      {
        if (exec != null) exec.done();
      }
    }
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.GIT;
  }

  /** get repository root URL
   * @return repository root URL
   */
  public String getRepositoryRootURL()
  {
    String repositoryPath = "";

    // get root
    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;

      command.clear();
      command.append(Settings.gitCommand,"rev-parse","--show-toplevel");
      command.append("--");
      exec = new Exec(rootPath,command);

      repositoryPath = exec.getStdout();

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return repositoryPath;
  }

  /** get first revision name
   * @return first revision name
   */
  public String getFirstRevision()
  {
    return FIRST_REVISION_NAME;
  }

  /** get last revision name
   * @return last revision name
   */
  public String getLastRevision()
  {
    return LAST_REVISION_NAME;
  }

  /** get revision names of file
   * @param name file name or URL
   * @return array with revision names
   */
  public String[] getRevisionNames(String name)
    throws RepositoryException
  {
    final Pattern PATTERN_COMMIT = Pattern.compile("^commit\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<String> revisionList = new ArrayList<String>();

    // get revision info list
    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;
      Matcher matcher;

      // get log
      command.clear();
      command.append(Settings.gitCommand,"log");
      command.append("--");
      if (name != null) command.append(name);
      exec = new Exec(rootPath,command);

      // parse revisions in log output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_COMMIT.matcher(line)).matches())
        {
          // add log info entry
          revisionList.add(matcher.group(1));
        }
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert to array and sort
    String[] revisions = revisionList.toArray(new String[revisionList.size()]);
    Arrays.sort(revisions);

    return revisions;
  }

  /** get revision data
   * @param fileData file data
   * @param revision revision
   * @return revision data
   */
  public RevisionData getRevisionData(FileData fileData, String revision)
    throws RepositoryException
  {
    RevisionData revisionData = null;

    // get revision data
    Exec exec = null;
    try
    {
      Command command = new Command();

      // get single log entry
      command.clear();
      command.append(Settings.gitCommand,"log","-1");
      if (revision != null) command.append(revision);
      command.append("--");
      exec = new Exec(rootPath,command);

      // parse data
      revisionData = parseLogData(exec);

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return revisionData;
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException
  {
    LinkedList<RevisionData> revisionDataList = new LinkedList<RevisionData>();

    // get revision info list
    Exec exec = null;
    try
    {
      Command command = new Command();

      // get log
      command.clear();
      command.append(Settings.gitCommand,"log");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse data
      RevisionData revisionData;
      while ((revisionData = parseLogData(exec)) != null)
      {
        // add revision info entry
        revisionDataList.addFirst(revisionData);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
//for (RevisionData revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

    // create revision data tree (=list of revisions)
    return revisionDataList.toArray(new RevisionData[revisionDataList.size()]);
  }

  /** get file data (text lines)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public String[] getFileLines(FileData fileData, String revision)
    throws RepositoryException
  {
    ArrayList<String> lineList = new ArrayList<String>();

    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;

      // get file
      command.clear();
      command.append(Settings.gitCommand,"show");
      command.append("--");
      command.append(((revision != null) ? revision+":" : "")+getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // read file data
      while ((line = exec.getStdout()) != null)
      {
        lineList.add(line);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return lineList.toArray(new String[lineList.size()]);
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public byte[] getFileBytes(FileData fileData, String revision)
    throws RepositoryException
  {
    ByteArrayOutputStream output = new ByteArrayOutputStream(64*1024);

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get file data
      command.clear();
      command.append(Settings.gitCommand,"show");
      command.append("--");
      command.append(((revision != null) ? revision+":" : "")+getFileDataName(fileData));
      exec = new Exec(rootPath,command,true);

      // read file bytes into byte array stream
      while ((n = exec.readStdout(buffer)) > 0)
      {
        output.write(buffer,0,n);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert byte array stream into array
    return output.toByteArray();
  }

  /** get all changed/unknown files
   * @param stateSet state set
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles(EnumSet<FileData.States> stateSet)
    throws RepositoryException
  {
    final Pattern PATTERN_STATUS         = Pattern.compile("^\\s*(\\S+)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_CURRENT_BRANCH = Pattern.compile("^\\*\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    Exec exec = null;
    try
    {
      Command         command            = new Command();
      String          line;
      Matcher         matcher;
      String          name               = null;
      FileData.States state              = FileData.States.UNKNOWN;
      String          workingRevision    = "";
      String          repositoryRevision = "";
      String          author             = "";

      // get list of files which may be updated or which are locally changed
      command.clear();
      command.append(Settings.gitCommand,"status","-s","--porcelain","--untracked-files=all");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read list
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
        {
          state = parseState(matcher.group(1));
          name  = StringUtils.unescape(matcher.group(2));

          File file = new File(rootPath,name);
          if (   !isIgnoreFile(file)
              && file.isFile()
              && stateSet.contains(state)
             )
          {
            fileDataSet.add(new FileData(name,
                                         state,
                                         FileData.Modes.BINARY,
                                         false
                                        )
                           );
          }
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for changed files line '%s'",line);
        }
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return fileDataSet;
  }

  /** get diff of file
   * @param fileData file data
   * @param revision revision to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String oldRevision, String newRevision)
    throws RepositoryException
  {
    final Pattern PATTERN_DIFF_START1   = Pattern.compile("^\\-\\-\\-.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_START2   = Pattern.compile("^\\+\\+\\+.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_OLD_MODE = Pattern.compile("^old mode\\s+.*$",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_NEW_MODE = Pattern.compile("^new mode\\s+.*$",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_INDEX    = Pattern.compile("^index\\s+.*$",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF          = Pattern.compile("^@@\\s+\\-([\\d,]*)\\s+\\+([\\d,]*)\\s+@@(.*)$",Pattern.CASE_INSENSITIVE);

    ArrayList<DiffData> diffDataList = new ArrayList<DiffData>();

    String[] newFileLines = null;
    if (newRevision != null)
    {
      Exec exec = null;
      try
      {
        Command command = new Command();
        Matcher matcher;
        String  line;

        // check out new revision
        command.clear();
        command.append(Settings.gitCommand,"show");
        command.append(newRevision+":"+getFileDataName(fileData));
        exec = new Exec(rootPath,command);

        // read content
        ArrayList<String> newFileLineList = new ArrayList<String>();
        while ((line = exec.getStdout()) != null)
        {
          newFileLineList.add(line);
        }

        // done
        exec.done(); exec = null;

        // convert to lines array
        newFileLines = newFileLineList.toArray(new String[newFileLineList.size()]);
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
      finally
      {
        if (exec != null) exec.done();
      }
    }
    else
    {
      // use local revision
      try
      {
        // open file
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));

        // read content
        ArrayList<String> newFileLineList = new ArrayList<String>();
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
          newFileLineList.add(line);
        }

        // close file
        bufferedReader.close();

        newFileLines = newFileLineList.toArray(new String[newFileLineList.size()]);
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
    }

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      String  line;

      // diff file
      command.clear();
      command.append(Settings.gitCommand,"diff");
      if (oldRevision != null) command.append("-r",oldRevision);
      if (newRevision != null) command.append("-r",newRevision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      /* skip diff header (1 line)
           diff ...
      */
      exec.getStdout();

      /* parse diff output
           Format:
             @@ -<i> +<j> @@
      */
      int               lineNb = 1;
      DiffData          diffData;
      ArrayList<String> keepLinesList = new ArrayList<String>();
      ArrayList<String> addedLinesList = new ArrayList<String>();
      ArrayList<String> deletedLinesList = new ArrayList<String>();
      String[]          lines;
      while ((line = exec.getStdout()) != null)
      {
        if      ((matcher = PATTERN_DIFF_START1.matcher(line)).matches())
        {
          // skipped
        }
        else if ((matcher = PATTERN_DIFF_START2.matcher(line)).matches())
        {
          // skipped
        }
        else if ((matcher = PATTERN_DIFF_OLD_MODE.matcher(line)).matches())
        {
          // skipped
        }
        else if ((matcher = PATTERN_DIFF_NEW_MODE.matcher(line)).matches())
        {
          // skipped
        }
        else if ((matcher = PATTERN_DIFF_INDEX.matcher(line)).matches())
        {
          // skipped
        }
        else if ((matcher = PATTERN_DIFF.matcher(line)).matches())
        {
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

          // git is odd: the first line of the diff is appended to the diff-header line...
          exec.ungetStdout(matcher.group(3));

          // git is odd: the first line of the diff is appended to the diff-header line...
          exec.ungetStdout(matcher.group(3));

          // read until @@ is found
          while (   ((line = exec.peekStdout()) != null)
                 && !line.startsWith("@@")
                )
          {
//Dprintf.dprintf("line=#%s#",line);
            // skip unknown lines
            while (   ((line = exec.getStdout()) != null)
                   && !line.isEmpty()
                   && !line.startsWith(" ")
                   && !line.startsWith("-")
                   && !line.startsWith("+")
                  )
            {
//Dprintf.dprintf("skip=#%s#",line);
            }
            exec.ungetStdout(line);

            // get keep lines
            keepLinesList.clear();
            while ((lineNb < newIndex[0]) && (lineNb <= newFileLines.length))
            {
              keepLinesList.add(newFileLines[lineNb-1]);
              lineNb++;
            }
            while (   ((line = exec.getStdout()) != null)
                   && (line.isEmpty() || line.startsWith(" "))
                  )
            {
//Dprintf.dprintf("keep=#%s#",);
              if (lineNb <= newFileLines.length)
              {
                keepLinesList.add(newFileLines[lineNb-1]);
              }
              else
              {
                keepLinesList.add("???");
              }
              lineNb++;
            }
            exec.ungetStdout(line);
            diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
            diffDataList.add(diffData);

            // get deleted lines
            deletedLinesList.clear();
            while (   ((line = exec.getStdout()) != null)
                   && line.startsWith("-")
                  )
            {
              deletedLinesList.add(line.substring(1));
            }
            exec.ungetStdout(line);

            // get added lines
            addedLinesList.clear();
            while (   ((line = exec.getStdout()) != null)
                   && line.startsWith("+")
                  )
            {
              addedLinesList.add(line.substring(1));
              lineNb++;
            }
            exec.ungetStdout(line);

            int deletedLinesCount = deletedLinesList.size();
            int addedLinesCount   = addedLinesList.size();
            if      (deletedLinesCount < addedLinesCount)
            {
              // changed
              if (deletedLinesCount > 0)
              {
                lines = new String[deletedLinesCount];
                for (int z = 0; z < deletedLinesCount; z++)
                {
                  lines[z] = addedLinesList.get(z);
                }
                diffData = new DiffData(DiffData.Types.CHANGED,lines,deletedLinesList);
                diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",lines.length,deletedLinesCount);
              }

              // added
              lines = new String[addedLinesCount-deletedLinesCount];
              for (int z = 0; z < addedLinesCount-deletedLinesCount; z++)
              {
                 lines[z] = addedLinesList.get(deletedLinesCount+z);
              }
              diffData = new DiffData(DiffData.Types.ADDED,lines);
              diffDataList.add(diffData);
//Dprintf.dprintf("a %d",lines.length);
            }
            else if (deletedLinesCount > addedLinesCount)
            {
              // changed
              if (addedLinesCount > 0)
              {
                lines = new String[addedLinesCount];
                for (int z = 0; z < addedLinesCount; z++)
                {
                  lines[z] = deletedLinesList.get(z);
                }
                diffData = new DiffData(DiffData.Types.CHANGED,addedLinesList,lines);
                diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",addedLinesCount,lines.length);
              }

              // deleted
              lines = new String[deletedLinesCount-addedLinesCount];
              for (int z = 0; z < deletedLinesCount-addedLinesCount; z++)
              {
                 lines[z] = deletedLinesList.get(addedLinesCount+z);
              }
              diffData = new DiffData(DiffData.Types.DELETED,lines);
              diffDataList.add(diffData);
//Dprintf.dprintf("d %d",lines.length);
            }
            else if ((deletedLinesCount > 0) && (addedLinesCount > 0))
            {
              // changed
              diffData = new DiffData(DiffData.Types.CHANGED,addedLinesList,deletedLinesList);
              diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",addedLinesCount,deletedLinesCount);
            }
          }
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for diff line '%s'",line);
        }
      }

      // get rest of keep lines
      if (lineNb <= newFileLines.length)
      {
        keepLinesList.clear();
        while (lineNb <= newFileLines.length)
        {
          keepLinesList.add(newFileLines[lineNb-1]);
          lineNb++;
        }
        diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
        diffDataList.add(diffData);
      }
//Dprintf.dprintf("diffData=%s",diffData);

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
/*
int lineNb=1;
for (DiffData d : diffDataList)
{
Dprintf.dprintf("%s %d: %d %d %d",d.blockType,
lineNb,
(d.keepLines!=null)?d.keepLines.length:0,
(d.addedLines!=null)?d.addedLines.length:0,
(d.deletedLines!=null)?d.deletedLines.length:0
);
if (d.blockType==DiffData.Types.CHANGE) for (int z = 0; z < d.deletedLines.length; z++) Dprintf.dprintf("%s -----> %s",d.deletedLines[z],d.addedLines[z]);
if (d.blockType==DiffData.Types.KEEP) lineNb += d.keepLines.length;
if (d.blockType==DiffData.Types.ADDED) lineNb += d.addedLines.length;
}
*/

    return diffDataList.toArray(new DiffData[diffDataList.size()]);
  }

  /** get patch for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @param output patch output or null
   * @param lineList patch data lines or null
   */
  public void getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces, PrintWriter output, ArrayList<String> lineList)
    throws RepositoryException
  {
    final Pattern PATTERN_OLD_FILE = Pattern.compile("^\\-\\-\\-\\s+(a[/\\\\])(.*?)(\\t.*){0,1}",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_NEW_FILE = Pattern.compile("^\\+\\+\\+\\s+(b[/\\\\])(.*?)(\\t.*){0,1}",Pattern.CASE_INSENSITIVE);

    // get existing/new files
    HashMap<String,FileData> existingFileDataMap = new HashMap<String,FileData>();
    HashSet<FileData>        newFileDataSet      = new HashSet<FileData>();
    if (fileDataSet != null)
    {
      for (FileData fileData : fileDataSet)
      {
        if (fileData.state != FileData.States.UNKNOWN)
        {
          existingFileDataMap.put(getFileDataName(fileData),fileData);
        }
        else
        {
          newFileDataSet.add(fileData);
        }
      }
    }

    // get patch for existing files
    if ((fileDataSet == null) || (existingFileDataMap.size() > 0))
    {
      Exec exec = null;
      try
      {
        Command command = new Command();
        String  line;

        // get patch
        command.clear();
        command.append(Settings.gitCommand,"diff","--patch");
        if (ignoreWhitespaces)
        {
          command.append("-w","-b","--ignore-space-at-eol");
        }
        else
        {
        }
        if (revision1 != null) command.append("-r",revision1);
        if (revision2 != null) command.append("-r",revision2);
        command.append("--");
        if (fileDataSet != null) command.append(getFileDataNames(existingFileDataMap));
        exec = new Exec(rootPath,command);

        // read patch data
        HashSet<FileData> patchFileDataSet = new HashSet<FileData>();
        Matcher matcher;
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // fix +++/--- lines: check if absolute path and convert or remove prefixes
          if      ((matcher = PATTERN_OLD_FILE.matcher(line)).matches())
          {
            String prefix     = matcher.group(1);
            String name       = matcher.group(2);
            String dateString = matcher.group(3);

            // store
            patchFileDataSet.add(existingFileDataMap.get(name));

            // get file name
            String fileName;
            if      (name.startsWith(rootPath+File.separator))
            {
              // absolute path -> convert to relative path to root path
              fileName = name.substring(rootPath.length()+1);
            }
            else
            {
              // use original name
              fileName = prefix+name;
            }

            line = "--- "+fileName+((dateString != null)?"\t"+dateString:"");
          }
          else if ((matcher = PATTERN_NEW_FILE.matcher(line)).matches())
          {
            String prefix     = matcher.group(1);
            String name       = matcher.group(2);
            String dateString = matcher.group(3);

            // store
            patchFileDataSet.add(existingFileDataMap.get(name));

            // get file name
            String fileName;
            if      (name.startsWith(rootPath+File.separator))
            {
              // absolute path -> convert to relative path to root path
              fileName = name.substring(rootPath.length()+1);
            }
            else
            {
              // use original name
              fileName = prefix+name;
            }

            line = "+++ "+fileName+((dateString != null)?"\t"+dateString:"");
          }

          if      (output   != null) output.println(line);
          else if (lineList != null) lineList.add(line);
        }

        // get file names for not modified files
        for (FileData fileData : existingFileDataMap.values())
        {
          if (!patchFileDataSet.contains(fileData))
          {
            if      (output   != null) output.println("File: "+getFileDataName(fileData));
            else if (lineList != null) lineList.add("File: "+getFileDataName(fileData));
          }
        }

        // done
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
      finally
      {
        if (exec != null) exec.done();
      }
    }

    // get complete patches for new files
    for (FileData fileData : newFileDataSet)
    {
      try
      {
        BufferedReader bufferedReader;
        String         line;

        // count number of lines in file
        int lineCount = 0;
        bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));
        while ((line = bufferedReader.readLine()) != null)
        {
          lineCount++;
        }
        bufferedReader.close();

        // add as patch
        bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));
        String dateString = DateFormat.getDateInstance().format(new Date());
        if      (output   != null)
        {
          output.println(String.format("diff -u %s",fileData.getFileName()));
          output.println(String.format("--- /dev/null\t%s",dateString));
          output.println(String.format("+++ %s\t%s",fileData.getFileName(),dateString));
          output.println(String.format("@@ -1,%d +1,%d @@",lineCount,lineCount));
          while ((line = bufferedReader.readLine()) != null)
          {
            output.println("+"+line);
          }
        }
        else if (lineList != null)
        {
          lineList.add(String.format("diff -u %s",fileData.getFileName()));
          lineList.add(String.format("--- /dev/null\t%s",dateString));
          lineList.add(String.format("+++ %s\t%s",fileData.getFileName(),dateString));
          lineList.add(String.format("@@ -1,%d +1,%d @@",lineCount,lineCount));
          while ((line = bufferedReader.readLine()) != null)
          {
            lineList.add("+"+line);
          }
        }
        bufferedReader.close();
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
    }
  }

  /** get patch data for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @return patch data bytes
   */
  public byte[] getPatchBytes(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
Dprintf.dprintf("");
    return null;
  }

  /** get log to file
   * @param fileData file data
   * @return log array
   */
  public LogData[] getLog(FileData fileData)
    throws RepositoryException
  {
    ArrayList<LogData> logDataList = new ArrayList<LogData>();

    Exec exec = null;
    try
    {
      // get log
      Command command = new Command();
      command.clear();
      command.append(Settings.gitCommand,"log");
      command.append("--");
      if (fileData != null) command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse data
      RevisionData revisionData;
      while ((revisionData = parseLogData(exec)) != null)
      {
        // add log info entry
        logDataList.add(new LogData(revisionData.revision,
                                    revisionData.date,
                                    revisionData.author,
                                    revisionData.commitMessage,
                                    revisionData.fileNames
                                   )
                       );
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
//for (LogData logData : logDataList) Dprintf.dprintf("logData=%s",logData);

    return logDataList.toArray(new LogData[logDataList.size()]);
  }

  /** get annotations to file
   * @param fileData file data
   * @param revision revision
   * @return annotation array
   */
  public AnnotationData[] getAnnotations(FileData fileData, String revision)
    throws RepositoryException
  {
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^(\\S+)\\s+\\((\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+\\d+\\)\\s(.*)",Pattern.CASE_INSENSITIVE);
//    final Pattern PATTERN_COMMIT = Pattern.compile("^(\\S+)\\s+(\\d+)\\s+(\\d+)\\s(\\d+)",Pattern.CASE_INSENSITIVE);
//    final Pattern PATTERN_AUTHOR = Pattern.compile("^author\\s+(.+)",Pattern.CASE_INSENSITIVE);
//    final Pattern PATTERN_DATE   = Pattern.compile("^commit-date\\s+(.+)",Pattern.CASE_INSENSITIVE);
//    final Pattern PATTERN_LINE   = Pattern.compile("^\\t(.*)",Pattern.CASE_INSENSITIVE);

    ArrayList<AnnotationData> annotationDataList = new ArrayList<AnnotationData>();

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      String  line;

      // get annotations
      command.clear();
      command.append(Settings.gitCommand,"blame","--date","iso");
      if (revision != null) command.append(revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      /* parse annotation output
           Format:
             <revision> (<author> <date> <time> <time zone> <line nb>) <line>
      */
      while ((line = exec.getStdout()) != null)
      {
        if      ((matcher = PATTERN_ANNOTATION.matcher(line)).matches())
        {
          annotationDataList.add(new AnnotationData(matcher.group(1),
                                                    matcher.group(2),
                                                    parseDate(matcher.group(3)),
                                                    matcher.group(4))
                                                   );
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for annotations line '%s'",line);
        }
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return annotationDataList.toArray(new AnnotationData[annotationDataList.size()]);
  }

  /** update file from respository
   * @param fileDataSet file data set or null
   * @param busyDialog busy dialog or null
   */
  public void update(HashSet<FileData> fileDataSet, BusyDialog busyDialog)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();

      // update files
      command.clear();
      command.append(Settings.gitCommand,"pull");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read output
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && !exec.isTerminated()
            )
      {
        String line;

        // read stdout
        line = exec.pollStdout();
        if ((line != null) && line.startsWith("getting "))
        {
//Dprintf.dprintf("out: %s",line);
          if (busyDialog != null) busyDialog.updateList(line);
        }

        // discard stderr
        line = exec.pollStderr();
        if (line != null)
        {
//Dprintf.dprintf("err1: %s",line);
        }
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
        }
      }
      else
      {
        // abort
        exec.destroy();
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void commit(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // commit files
      command.clear();
      command.append(Settings.gitCommand,"commit","-F",commitMessage.getFileName());
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(HashSet<FileData> fileDataSet, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // add files
      command.clear();
      command.append(Settings.gitCommand,"add");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // immediate commit when message is given
      if (commitMessage != null)
      {
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      // remove from repository
      Command command = new Command();

      // remove files
      command.clear();
      command.append(Settings.gitCommand,"rm");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // immediate commit when message is given
      if (commitMessage != null)
      {
        // commit removed files
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** revert files
   * @param fileDataSet file data set or null for all files
   * @param revision revision to revert to
   * @param recursive true for recursive revert, false otherwise
 */
  public void revert(HashSet<FileData> fileDataSet, String revision, boolean recursive)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      if (fileDataSet.size() > 0)
      {
        // revert files
        command.clear();
        command.append(Settings.gitCommand,"checkout");
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exec = new Exec(rootPath,command);

        // wait for termination
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  public void rename(FileData fileData, String newName, CommitMessage commitMessage)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // rename file
      command.clear();
      command.append(Settings.gitCommand,"mv");
      command.append("--");
      command.append(fileData.getFileName());
      command.append(newName);
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // immediate commit when message is given
      if (commitMessage != null)
      {
        commit(fileData,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** set conflicts resolved
   * @param fileDataSet file data set or null for all files
   */
  public void resolve(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
throw new Error("NYI");
  }

  /** get incoming changes list
   * @param masterRepository master repository or null
   */
  public LogData[] getIncomingChanges(String masterRepository)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
//    return null;
  }

  /** get outgoing changes list
   * @param masterRepository master repository or null
   */
  public LogData[] getOutgoingChanges(String masterRepository)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
//    return null;
  }

  /** get incoming/outgoing changes lines
   * @param revision revision to get changes lines for
   */
  public String[] getChanges(String revision)
    throws RepositoryException
  {
    return null;
  }

  /** pull changes
   * @param masterRepositoryPath master repository path
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pullChanges(String masterRepositoryPath, String moduleName, String userName, String password)
    throws RepositoryException
  {
    final Pattern PATTERN_URI1 = Pattern.compile("^([^:/]+)://([^:]*):([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI2 = Pattern.compile("^([^:/]+)://([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI3 = Pattern.compile("^([^:/]+)://(.*)",Pattern.CASE_INSENSITIVE);

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      int     exitCode;

      command.clear();
      command.append(Settings.gitCommand,"pull");
      command.append("--");
      if ((masterRepositoryPath != null) && !masterRepositoryPath.isEmpty())
      {
        if      ((matcher = PATTERN_URI1.matcher(masterRepositoryPath)).matches())
        {
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(matcher.group(3)),
                               "@",
                               matcher.group(4),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI2.matcher(masterRepositoryPath)).matches())
        {
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(3),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI3.matcher(masterRepositoryPath)).matches())
        {
          if (userName.isEmpty())
          {
            userName = System.getProperty("user.name");
          }
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               userName,
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(2),
                               "/",
                               moduleName
                              );
        }
        else
        {
          command.append("file://"+masterRepositoryPath+"/"+moduleName);
        }
      }
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** push changes
   * @param masterRepositoryPath master repository path
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pushChanges(String masterRepositoryPath, String moduleName, String userName, String password)
    throws RepositoryException
  {
    final Pattern PATTERN_URI1 = Pattern.compile("^([^:/]+)://([^:]*):([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI2 = Pattern.compile("^([^:/]+)://([^@]*)@(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_URI3 = Pattern.compile("^([^:/]+)://(.*)",Pattern.CASE_INSENSITIVE);

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;
      String  line;
      Matcher matcher;

      // push changes
      command.clear();
      command.append(Settings.gitCommand,"push");
      command.append("--");
      if ((masterRepositoryPath != null) && !masterRepositoryPath.isEmpty())
      {
        if      ((matcher = PATTERN_URI1.matcher(masterRepositoryPath)).matches())
        {
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(matcher.group(3)),
                               "@",
                               matcher.group(4),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI2.matcher(masterRepositoryPath)).matches())
        {
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(3),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI3.matcher(masterRepositoryPath)).matches())
        {
          if (userName.isEmpty())
          {
            userName = System.getProperty("user.name");
          }
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               userName,
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(2),
                               "/",
                               moduleName
                              );
        }
        else
        {
          command.append("file://"+masterRepositoryPath+"/"+moduleName);
        }
      }
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // push tags
      command.clear();
      command.append(Settings.gitCommand,"push","--tags");
      command.append("--");
      if ((masterRepositoryPath != null) && !masterRepositoryPath.isEmpty())
      {
        if      ((matcher = PATTERN_URI1.matcher(masterRepositoryPath)).matches())
        {
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(matcher.group(3)),
                               "@",
                               matcher.group(4),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI2.matcher(masterRepositoryPath)).matches())
        {
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               matcher.group(2),
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(3),
                               "/",
                               moduleName
                              );
        }
        else if ((matcher = PATTERN_URI3.matcher(masterRepositoryPath)).matches())
        {
          if (userName.isEmpty())
          {
            userName = System.getProperty("user.name");
          }
          if (password.isEmpty())
          {
            throw new RepositoryException("no password given");
          }
          command.appendConcat(matcher.group(1),
                               "://",
                               userName,
                               ":",
                               command.hidden(password),
                               "@",
                               matcher.group(2),
                               "/",
                               moduleName
                              );
        }
        else
        {
          command.append("file://"+masterRepositoryPath+"/"+moduleName);
        }
      }
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** apply patches
   */
  public void applyPatches()
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** unapply patches
   */
  public void unapplyPatches()
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** lock files
   * @param fileDataSet file data set
   */
  public void lock(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** unlock files
   * @param fileDataSet file data set
   */
  public void unlock(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, CommitMessage commitMessage)
    throws RepositoryException
  {
Dprintf.dprintf("NYI");
throw new RepositoryException("NYI");
  }

  /** get default name of root
   * @return default root name
   */
  public String getDefaultRootName()
  {
    return DEFAULT_ROOT_NAME;
  }

  /** get default branch/tag name
   * @return default branch/tag name
   */
  public String getDefaultBranchTagName()
  {
    return DEFAULT_BRANCHES_NAME;
  }

  /** get names of existing branches/tags
   * @param pathName path name
   * @return array with branch/tag names
   */
  public String[] getBranchTagNames(String pathName)
    throws RepositoryException
  {
    HashSet<String> branchTagNameSet = new HashSet<String>();

    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;

      // list tags
      command.clear();
      command.append(Settings.gitCommand,"tag","-l");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read output
      while ((line = exec.getStdout()) != null)
      {
        branchTagNameSet.add(line);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert to array and sort
    String[] branchTagNames = branchTagNameSet.toArray(new String[branchTagNameSet.size()]);
    Arrays.sort(branchTagNames);

    return branchTagNames;
  }

  /** create new branch
   * @param rootName root name (source)
   * @param branchTagName branch/tag name
   * @param commitMessage commit message
   * @param buysDialog busy dialog or null
   */
  public void newBranch(String rootName, String branchTagName, CommitMessage commitMessage, BusyDialog busyDialog)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;
      int     exitCode;

      // create branch
      command.clear();
      command.append(Settings.gitCommand,"tag","-a",branchTagName,"-m",commitMessage.getMessage());
      command.append("--");
      exec = new Exec(rootPath,command);

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  //-----------------------------------------------------------------------

  /** get absolute local repository path
   * @return absolute path
   */
  public String getAbsolutePath()
  {
    String absolutePath = "";

    // get root
    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;

      command.clear();
      command.append(Settings.gitCommand,"rev-parse","--show-toplevel");
      command.append("--");
      exec = new Exec(rootPath,command);

      absolutePath = exec.getStdout();

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return absolutePath;
  }

  /** parse git state string
   * @param string state string
   * @return state
   */
  private FileData.States parseState(String string)
  {
    if      (string.equalsIgnoreCase(" " )) return FileData.States.OK;
    else if (string.equalsIgnoreCase("M" )) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("A" )) return FileData.States.ADDED;
    else if (string.equalsIgnoreCase("D" )) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("R" )) return FileData.States.RENAMED;
    else if (string.equalsIgnoreCase("C" )) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("U" )) return FileData.States.CHECKOUT;
    else if (string.equalsIgnoreCase("??")) return FileData.States.UNKNOWN;
    else                                    return FileData.States.OK;
  }

  /** parse log data
   * @param exec exec command
   * @return revision info or null
   */
  private RevisionData parseLogData(Exec exec)
    throws IOException
  {
    final Pattern PATTERN_COMMIT = Pattern.compile("^commit\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_AUTHOR = Pattern.compile("^Author:\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DATE   = Pattern.compile("^Date:\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_MERGE  = Pattern.compile("^Merge:\\s+(.*)\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    RevisionData       revisionData      = null;

    boolean            dataDone          = false;
    Matcher            matcher;
    String             line;
    String             revision          = null;
    String             changeSet         = null;
    Date               date              = null;
    String             author            = null;
    ArrayList<String>  fileNameList      = new ArrayList<String>();
    LinkedList<String> commitMessageList = new LinkedList<String>();
    while (   !dataDone
           && ((line = exec.getStdout()) != null)
          )
    {
//Dprintf.dprintf("line=%s",line);
      if      ((matcher = PATTERN_COMMIT.matcher(line)).matches())
      {
        revision  = matcher.group(1);
        changeSet = matcher.group(1);
      }
      else if ((matcher = PATTERN_AUTHOR.matcher(line)).matches())
      {
        author = matcher.group(1);
      }
      else if ((matcher = PATTERN_DATE.matcher(line)).matches())
      {
        date = parseDate(matcher.group(1));
      }
      else if ((matcher = PATTERN_MERGE.matcher(line)).matches())
      {
      }
      else if (line.isEmpty())
      {
        // get commit message lines
        commitMessageList.clear();
        while (   ((line = exec.getStdout()) != null)
               && line.startsWith("    ")
              )
        {
          commitMessageList.add(line.substring(4));
        }
        while (   (commitMessageList.peekFirst() != null)
               && commitMessageList.peekFirst().trim().isEmpty()
              )
        {
          commitMessageList.removeFirst();
        }
        while (   (commitMessageList.peekLast() != null)
               && commitMessageList.peekLast().trim().isEmpty()
              )
        {
          commitMessageList.removeLast();
        }

        // skip empty lines
        while (   ((line = exec.getStdout()) != null)
               && line.isEmpty()
              )
        {
        }
        exec.ungetStdout(line);

        // create log info entry
        revisionData = new RevisionData(revision,
                                        (String)null,
                                        date,
                                        author,
                                        commitMessageList,
//??? file list?
                                        fileNameList
                                       );

        dataDone = true;
      }
      else
      {
        // unknown line
        Onzen.printWarning("No match for log line '%s'",line);
      }
    }
//Dprintf.dprintf("revisionData=%s",revisionData);

    return revisionData;
  }

  /** parse HG diff index
   * @param string diff index string
   * @return index array [lineNb,length] or null
   */
  private int[] parseDiffIndex(String string)
  {
    Object[] data = new Object[2];
    if      (StringParser.parse(string,"%d,%d",data))
    {
      return new int[]{(Integer)data[0],(Integer)data[1]-(Integer)data[0]-1};
    }
    else if (StringParser.parse(string,"%d",data))
    {
      return new int[]{(Integer)data[0],1};
    }
    else
    {
      return null;
    }
  }
}

/* end of file */
