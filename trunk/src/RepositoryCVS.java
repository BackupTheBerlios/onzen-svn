/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: CVS repository functions
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
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/****************************** Classes ********************************/

/** Concurrent Versions System (CVS) repository
 */
class RepositoryCVS extends Repository
{
  /** CVS revision data
   */
  class RevisionDataCVS extends RevisionData
  {
    final int[]    revisionNumbers;
    final String[] branchRevisions;

    /** create CVS revision data
     * @param revision revision string
     * @param revisionNumberList list of revision numbers
     * @param tag tag name or null
     * @param date date
     * @param author author name
     * @param branchRevisionList branch revisions
     * @param commitMessageList commit message lines
     */
    RevisionDataCVS(String                revision,
                    AbstractList<Integer> revisionNumberList,
                    String                tag,
                    Date                  date,
                    String                author,
                    AbstractList<String>  branchRevisionList,
                    AbstractList<String>  commitMessageList
                   )
    {
      super(revision,tag,date,author,commitMessageList);

      this.revisionNumbers = new int[revisionNumberList.size()];
      for (int z = 0; z < revisionNumberList.size(); z++)
      {
        this.revisionNumbers[z] = revisionNumberList.get(z).intValue();
      }
      this.branchRevisions = branchRevisionList.toArray(new String[branchRevisionList.size()]);
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "CVS revision info {revision: "+revision+", tags: "+tags+", date: "+date+", author: "+author+", message: "+commitMessage+"}";
    }
  }

  // --------------------------- constants --------------------------------
  private final String LAST_REVISION_NAME = "HEAD";

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository
   * @param rootPath root path
   */
  RepositoryCVS(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositoryCVS()
  {
    this(null);
  }

  /** check if repository support setting file mode
   * @return true iff file modes are supported
   */
  public boolean supportSetFileMode()
  {
    return true;
  }

  /** checkout repository from server
   * @param repositoryPath repository server
   * @param moduleName module name
   * @param revision revision to checkout
   * @param destinationPath destination path
   * @param busyDialog busy dialog or null
   */
  public void checkout(String repositoryPath, String moduleName, String revision, String destinationPath, BusyDialog busyDialog)
    throws RepositoryException
  {
    Command command = new Command();
    try
    {
      // create temporary directory for check-out
      File tmpDirectory = createTempDirectory(destinationPath);

      // checkout
      command.clear();
      command.append(Settings.cvsCommand,"-d",repositoryPath,"co","-d",tmpDirectory.getName());
      if (!revision.isEmpty()) command.append("-r",revision);
      command.append(moduleName);
      Exec exec = new Exec(rootPath,command);

      // read output
      int n = tmpDirectory.getName().length();
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
          if (busyDialog != null) busyDialog.updateText(line.substring(1+1+n+1));
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
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
      else
      {
        exec.destroy();
      }

      // done
      exec.done();

      // move files from temporary directory to destination directory
      File[] files = tmpDirectory.listFiles();
      if (files != null)
      {
        for (File file : files)
        {
          file.renameTo(new File(destinationPath,file.getName()));
        }
      }

      // delete temporary directory
      tmpDirectory.delete();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
  {
    final Pattern PATTERN_UNKNOWN             = Pattern.compile("^\\?\\s+.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_COMPLETE            = Pattern.compile("^\\s*File:.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE_STATUS1        = Pattern.compile("^\\s*File:\\s*no\\s+file\\s+(.*?)\\s+Status:\\s*(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE_STATUS2        = Pattern.compile("^\\s*File:\\s*(.*?)\\s+Status:\\s*(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_WORKING_REVISION1   = Pattern.compile("^.*Working revision:\\s*no.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_WORKING_REVISION2   = Pattern.compile("^.*Working revision:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_REPOSITORY_REVISION = Pattern.compile("^.*Repository revision:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_COMMIT_IDENTIFIER   = Pattern.compile("^.*Commit Identifier:.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STICKY_TAG          = Pattern.compile("^.*Sticky Tag:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STICKY_DATE1        = Pattern.compile("^.*Sticky Date:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STICKY_DATE2        = Pattern.compile("^.*(\\d*\\.\\d*\\.\\d*)\\.(\\d*\\.\\d*\\.\\d*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STICKY_OPTIONS      = Pattern.compile("^.*Sticky Options:\\s*(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_EXISTING_TAGS       = Pattern.compile("^.*Existing Tags:.*",Pattern.CASE_INSENSITIVE);

    Command         command            = new Command();
    Exec            exec;
    String          line;
    Matcher         matcher;
    FileData        fileData;
    String          baseName           = null;
    FileData.States state              = FileData.States.UNKNOWN;
    FileData.Modes  mode               = FileData.Modes.UNKNOWN;
    String          workingRevision    = "";
    String          repositoryRevision = "";
    String          branch             = "";
    HashSet<String> tags               = new HashSet<String>();
    for (String directory : fileDirectorySet)
    {
      try
      {
        // get status
        command.clear();
        command.append(Settings.cvsCommand,"status","-l");
        command.append("--");
        if (!directory.isEmpty()) command.append(directory);
        exec = new Exec(rootPath,command);

        // parse status data
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // check if one entry is complete
          if (PATTERN_COMPLETE.matcher(line).matches())
          {
            if (baseName != null)
            {
              fileData = findFileData(fileDataSet,directory,baseName);
              if      (fileData != null)
              {
                fileData.state              = state;
                fileData.mode               = mode;
                fileData.workingRevision    = workingRevision;
                fileData.repositoryRevision = repositoryRevision;
                fileData.branch             = branch;
              }
              else if (   (newFileDataSet != null)
                       && !isHiddenFile(directory,baseName)
                      )
              {
                // get file type, size, date/time
                File file = new File(rootPath,baseName);
                FileData.Types type     = getFileType(file);
                long           size     = file.length();
                Date           datetime = new Date(file.lastModified());

                // create file data
                newFileDataSet.add(new FileData((!directory.isEmpty()?directory+File.separator:"")+baseName,
                                                type,
                                                state,
                                                mode,
                                                size,
                                                datetime,
                                                workingRevision,
                                                repositoryRevision,
                                                branch
                                               )
                                  );
              }
            }

            baseName           = null;
            state              = FileData.States.UNKNOWN;
            mode               = FileData.Modes.UNKNOWN;
            workingRevision    = "";
            repositoryRevision = "";
            branch             = "";
          }

          // unknown file
          if      ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
          {
            // ignored
          }

          // match name, state
          else if ((matcher = PATTERN_FILE_STATUS1.matcher(line)).matches())
          {
            state = parseState(matcher.group(1));
          }
          else if ((matcher = PATTERN_FILE_STATUS2.matcher(line)).matches())
          {
            baseName = matcher.group(1);
            state    = parseState(matcher.group(2));
          }

          // match working revision
          else if ((matcher = PATTERN_WORKING_REVISION1.matcher(line)).matches())
          {
            workingRevision = "";
          }
          else if ((matcher = PATTERN_WORKING_REVISION2.matcher(line)).matches())
          {
            workingRevision = matcher.group(1);
          }

          // match repository revision
          else if ((matcher = PATTERN_REPOSITORY_REVISION.matcher(line)).matches())
          {
            repositoryRevision = matcher.group(1);
          }

          // commit idenitifier
          else if ((matcher = PATTERN_COMMIT_IDENTIFIER.matcher(line)).matches())
          {
            // ignored
          }

          // match sticky tag/date (branch)
          else if ((matcher = PATTERN_STICKY_TAG.matcher(line)).matches())
          {
            branch = (!matcher.group(1).equals("(none)")) ? matcher.group(1) : "";
          }
          else if ((matcher = PATTERN_STICKY_DATE1.matcher(line)).matches())
          {
            if      (!matcher.group(1).equals("(none)"))
            {
              branch = matcher.group(1);
            }
            else if ((matcher = PATTERN_STICKY_DATE2.matcher(line)).matches())
            {
              branch = matcher.group(1).replace(".","-")+" "+matcher.group(2).replace(".",":");
            }
          }

          // match sticky options
          else if ((matcher = PATTERN_STICKY_OPTIONS.matcher(line)).matches())
          {
            if      (matcher.group(1).equals("-kb" )) mode = FileData.Modes.BINARY;
            else if (matcher.group(1).equals("-ko" )) mode = FileData.Modes.BINARY;
            else if (matcher.group(1).equals("-kvv")) mode = FileData.Modes.TEXT;
            else                                      mode = FileData.Modes.TEXT;
          }

          // match tags
          else if ((matcher = PATTERN_EXISTING_TAGS.matcher(line)).matches())
          {
            tags.clear();
          }

          else if (line.isEmpty())
          {
            // ignored
          }
          else if (line.startsWith("====="))
          {
            // ignored
          }

          else
          {
            // unknown line
            Onzen.printWarning("No match for line '%s'",line);
          }
        }
//while ((line = exec.getStderr()) != null) Dprintf.dprintf("err %s",line);

        // done
        exec.done();

        if (baseName != null)
        {
          fileData = findFileData(fileDataSet,directory,baseName);
          if      (fileData != null)
          {
            fileData.state              = state;
            fileData.mode               = mode;
            fileData.workingRevision    = workingRevision;
            fileData.repositoryRevision = repositoryRevision;
            fileData.branch             = branch;
          }
          else if (   (newFileDataSet != null)
                   && !isHiddenFile(directory,baseName)
                  )
          {
            // get file type, size, date/time
            File file = new File(rootPath,baseName);
            FileData.Types type     = getFileType(file);
            long           size     = file.length();
            Date           datetime = new Date(file.lastModified());

            // create file data
            newFileDataSet.add(new FileData((!directory.isEmpty()?directory+File.separator:"")+baseName,
                                            type,
                                            state,
                                            mode,
                                            size,
                                            datetime,
                                            workingRevision,
                                            repositoryRevision,
                                            branch
                                           )
                              );
          }
        }

        if (newFileDataSet != null)
        {
          // find new files
          for (FileData newFileData : listFiles(directory))
          {

            if (   !isHiddenFile(newFileData.getFileName())
                && !containFileData(fileDataSet,newFileData)
                && !containFileData(newFileDataSet,newFileData)
               )
            {
              newFileDataSet.add(newFileData);
            }
          }
        }
      }
      catch (IOException exception)
      {
        // ignored
      }
    }
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.CVS;
  }

  /** get repository path
   * @return repository path
   */
  public String getRepositoryPath()
  {
    String repositoryPath = "";

    File file = new File(rootPath,"CVS"+File.separator+"Root");

    BufferedReader bufferedReader = null;
    try
    {
      // open file
      bufferedReader = new BufferedReader(new FileReader(file));

      // read repository path
      String line;
      if ((line = bufferedReader.readLine()) != null)
      {
        repositoryPath = line;
      }

      // close file
      bufferedReader.close(); bufferedReader = null;
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      try { if (bufferedReader != null) bufferedReader.close(); } catch (IOException exception) { /* ignored */ }
    }

    return repositoryPath;
  }

  /** get last revision name
   * @return last revision name
   */
  public String getLastRevision()
  {
    return LAST_REVISION_NAME;
  }

  /** get revision names of file
   * @param fileData file data
   * @return array with revision names
   */
  public String[] getRevisionNames(FileData fileData)
    throws RepositoryException
  {
    ArrayList<String> revisionList = new ArrayList<String>();

    // get revision info list
    HashMap<String,String> branchNamesMap = new HashMap<String,String>();
    Command                command        = new Command();
    Exec                   exec;
    try
    {
      // get log
      command.clear();
      command.append(Settings.cvsCommand,"log");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,branchNamesMap))
      {
        // parse data
        RevisionDataCVS revisionData;
        while ((revisionData = parseLogData(exec,branchNamesMap)) != null)
        {
          // add log info entry
          revisionList.add(revisionData.revision);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
//for (RevisionDataCVS revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

    // convert to array and sort
    String[] revisions = revisionList.toArray(new String[revisionList.size()]);
    Arrays.sort(revisions,new Comparator<String>()
    {
      public int compare(String revision1, String revision2)
      {
        String numbers1[] = revision1.split("\\.");
        String numbers2[] = revision2.split("\\.");
        for (int z = 0; z < Math.min(numbers1.length,numbers2.length); z++)
        {
          if      (Integer.parseInt(numbers1[z]) < Integer.parseInt(numbers2[z]))
          {
            return -1;
          }
          else if (Integer.parseInt(numbers1[z]) > Integer.parseInt(numbers2[z]))
          {
            return 1;
          }
        }
        if      (numbers1.length < numbers2.length)
        {
          return -1;
        }
        else if (numbers1.length < numbers2.length)
        {
          return 1;
        }
        else
        {
          return 0;
        }
      }
    });

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
    RevisionDataCVS revisionDataCVS = null;

    // get revision data
    HashMap<String,String> branchNamesMap = new HashMap<String,String>();
    Command                command        = new Command();
    Exec                   exec;
    try
    {
      // get single log entry
      command.clear();
      command.append(Settings.cvsCommand,"log","-r",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,branchNamesMap))
      {
        // parse data
        revisionDataCVS = parseLogData(exec,branchNamesMap);
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }

    return revisionDataCVS;
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException
  {
    LinkedList<RevisionDataCVS> revisionDataList = new LinkedList<RevisionDataCVS>();

    // get revision info list
    HashMap<String,String> branchNamesMap = new HashMap<String,String>();
    Command                command        = new Command();
    Exec                   exec;
    try
    {
      // get log
      command.clear();
      command.append(Settings.cvsCommand,"log");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,branchNamesMap))
      {
        // parse data
        RevisionDataCVS revisionData;
        while ((revisionData = parseLogData(exec,branchNamesMap)) != null)
        {
          // add log info entry
          revisionDataList.add(revisionData);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
//for (RevisionDataCVS revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

    // create revision data tree from list
    return createRevisionDataTree(revisionDataList,branchNamesMap);
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
    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;

      // get file
      command.clear();
      command.append(Settings.cvsCommand,"up","-p");
      if (revision != null) command.append("-r",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // read file data
      while ((line = exec.getStdout()) != null)
      {
        lineList.add(line);
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
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
    try
    {
      Command command = new Command();
      Exec    exec;
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get file data
      command.clear();
      command.append(Settings.cvsCommand,"up","-p");
      if (revision != null) command.append("-r",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command,true);

      // read file bytes into byte array stream
      while ((n = exec.readStdout(buffer)) > 0)
      {
        output.write(buffer,0,n);
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
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
    final Pattern PATTERN_UNKNOWN  = Pattern.compile("^\\?\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_UPDATE   = Pattern.compile("^U\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_MODIFIED = Pattern.compile("^M\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_CONFLICT = Pattern.compile("^C\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_MERGE    = Pattern.compile("^Merging differences.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_ADDED    = Pattern.compile("^A\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_REMOVED  = Pattern.compile("^R\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_EMPTY    = Pattern.compile("^\\s*",Pattern.CASE_INSENSITIVE);

    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;
      Matcher matcher;

      // get list of files which may be updated or which are locally changed
      command.clear();
      command.append(Settings.cvsCommand,"-n","-q","update","-d");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read list
      boolean mergeFlag = false;
      while ((line = exec.getStdout()) != null)
      {
        if      ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
        {
          if (stateSet.contains(FileData.States.UNKNOWN))
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.UNKNOWN));
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_UPDATE.matcher(line)).matches())
        {
          if (stateSet.contains(FileData.States.UPDATE))
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.UPDATE));
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_MODIFIED.matcher(line)).matches())
        {
          if (stateSet.contains(FileData.States.MODIFIED))
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.MODIFIED));
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_MERGE.matcher(line)).matches())
        {
          mergeFlag = true;
        }
        else if ((matcher = PATTERN_CONFLICT.matcher(line)).matches())
        {
          if (mergeFlag)
          {
            if (stateSet.contains(FileData.States.MERGE))
            {
              fileDataSet.add(new FileData(matcher.group(1),FileData.States.MERGE));
            }
          }
          else
          {
            if (stateSet.contains(FileData.States.CONFLICT))
            {
              fileDataSet.add(new FileData(matcher.group(1),FileData.States.CONFLICT));
            }
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_ADDED.matcher(line)).matches())
        {
          if (stateSet.contains(FileData.States.ADDED))
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.ADDED));
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_REMOVED.matcher(line)).matches())
        {
          if (stateSet.contains(FileData.States.REMOVED))
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.REMOVED));
          }
          mergeFlag = false;
        }
        else if (PATTERN_EMPTY.matcher(line).matches())
        {
          // skip empty lines
        }
        else
        {
          // unknown line
Dprintf.dprintf("unknown %s",line);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }

    return fileDataSet;
  }

  /** get diff of file
   * @param fileData file data
   * @param revision1,revision2 revisions to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String oldRevision, String newRevision)
    throws RepositoryException
  {
    final Pattern PATTERN_DIFF_START   = Pattern.compile("^diff.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_ADDED   = Pattern.compile("^([\\d,]+)a([\\d,]+)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_DELETED = Pattern.compile("^([\\d,]+)d([\\d,]+)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF_CHANGED = Pattern.compile("^([\\d,]+)c([\\d,]+)",Pattern.CASE_INSENSITIVE);

    ArrayList<DiffData> diffDataList = new ArrayList<DiffData>();

    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;
      Matcher matcher;

      String[] newFileLines = null;
      if (newRevision != null)
      {
        // check out new revision
        command.clear();
        command.append(Settings.cvsCommand,"up","-p","-r",newRevision);
        command.append("--");
        command.append(getFileDataName(fileData));
        exec = new Exec(rootPath,command);

        // read content
        ArrayList<String> newFileLineList = new ArrayList<String>();
        while ((line = exec.getStdout()) != null)
        {
          newFileLineList.add(line);
        }

        // done
        exec.done();

        // convert to lines array
        newFileLines = newFileLineList.toArray(new String[newFileLineList.size()]);
      }
      else
      {
        // use local revision
        BufferedReader bufferedReader = null;
        try
        {
          // open file
          bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));

          // read content
          ArrayList<String> newFileLineList = new ArrayList<String>();
          while ((line = bufferedReader.readLine()) != null)
          {
            newFileLineList.add(line);
          }

          // close file
          bufferedReader.close(); bufferedReader = null;

          // get lines array
          newFileLines = newFileLineList.toArray(new String[newFileLineList.size()]);
        }
        catch (IOException exception)
        {
          throw new RepositoryException(Onzen.reniceIOException(exception));
        }
        finally
        {
          try { if (bufferedReader != null) bufferedReader.close(); } catch (IOException exception) { /* ignored */ }
        }
      }

      // diff file
      command.clear();
      command.append(Settings.cvsCommand,"diff");
      if (oldRevision != null) command.append("-r",oldRevision);
      if (newRevision != null) command.append("-r",newRevision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // skip diff header
      while ((line = exec.getStdout()) != null)
      {
        if (PATTERN_DIFF_START.matcher(line).matches()) break;
      }

      /* parse diff output
           Format:
             <i>a<j> - lines added
             <i>d<j> - lines delete
             <i>c<j> - lines changed
      */
      int                lineNb = 1;
      DiffData           diffData;
      ArrayList<String> keepLinesList = new ArrayList<String>();
      ArrayList<String> addedLinesList = new ArrayList<String>();
      ArrayList<String> deletedLinesList = new ArrayList<String>();
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        if      ((matcher = PATTERN_DIFF_ADDED.matcher(line)).matches())
        {
          // add lines
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

          // get keep lines
          keepLinesList.clear();
          while ((lineNb < newIndex[0]) && (lineNb <= newFileLines.length))
          {
            keepLinesList.add(newFileLines[lineNb-1]);
            lineNb++;
          }
          diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);

          // get added lines
          addedLinesList.clear();
          for (int z = 0; z < newIndex[1]; z++)
          {
            line = exec.getStdout();
            if (!line.startsWith(">")) throw new RepositoryException("Invalid add diff output: '"+line+"'");
            addedLinesList.add(line.substring(2));
            lineNb++;
          }
          diffData = new DiffData(DiffData.Types.ADDED,addedLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);
        }
        else if ((matcher = PATTERN_DIFF_DELETED.matcher(line)).matches())
        {
          // delete lines
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));

          // get keep lines
          keepLinesList.clear();
          while ((lineNb <= newIndex[0]) && (lineNb <= newFileLines.length))
          {
            keepLinesList.add(newFileLines[lineNb-1]);
            lineNb++;
          }
          diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);

          // get deleted lines
          deletedLinesList.clear();
          for (int z = 0; z < oldIndex[1]; z++)
          {
            line = exec.getStdout();
            if (!line.startsWith("<")) throw new RepositoryException("Invalid delete diff output: '"+line+"'");
            deletedLinesList.add(line.substring(2));
          }
          diffData = new DiffData(DiffData.Types.DELETED,deletedLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);
        }
        else if ((matcher = PATTERN_DIFF_CHANGED.matcher(line)).matches())
        {
          // change lines
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

          // get keep lines
          keepLinesList.clear();
          while ((lineNb < newIndex[0]) && (lineNb <= newFileLines.length))
          {
            keepLinesList.add(newFileLines[lineNb-1]);
            lineNb++;
          }
          diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);

          // get delete lines
          deletedLinesList.clear();
          for (int z = 0; z < oldIndex[1]; z++)
          {
            line = exec.getStdout();
            if (!line.startsWith("<")) throw new RepositoryException("Invalid change diff output: '"+line+"'");
            deletedLinesList.add(line.substring(2));
          }

          // skip possible "\ No newline at end of file"
          line = exec.getStdout();
          if (!line.startsWith("\\ No newline at end of file")) exec.ungetStdout(line);

          // skip separator "---"
          line = exec.getStdout();
          if (!line.startsWith("---")) throw new RepositoryException("Invalid diff output: expected separator, got '"+line+"'");

          // get added lines
          addedLinesList.clear();
          for (int z = 0; z < newIndex[1]; z++)
          {
            line = exec.getStdout();
            if (!line.startsWith(">")) throw new RepositoryException("Invalid change diff output: '"+line+"'");
            addedLinesList.add(line.substring(2));
            lineNb++;
          }

          diffData = new DiffData(DiffData.Types.CHANGED,addedLinesList,deletedLinesList);
          diffDataList.add(diffData);
//Dprintf.dprintf("diffData=%s",diffData);
        }
else {
//Dprintf.dprintf("line=%s",line);
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }

    return diffDataList.toArray(new DiffData[diffDataList.size()]);
  }

  /** get unified patch lines for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @param output patch output or null
   * @param lineList patch data lines or null
   */
  public void getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces, PrintWriter output, ArrayList<String> lineList)
    throws RepositoryException
  {
    // get existing/new files
    HashSet<FileData> existFileDataSet = new HashSet<FileData>();
    HashSet<FileData> newFileDataSet   = new HashSet<FileData>();
    if (fileDataSet != null)
    {
      for (FileData fileData : fileDataSet)
      {
        if (fileData.state != FileData.States.UNKNOWN)
        {
          existFileDataSet.add(fileData);
        }
        else
        {
          newFileDataSet.add(fileData);
        }
      }
    }

    // get patch for existing files
    if ((fileDataSet == null) || (existFileDataSet.size() > 0))
    {
      try
      {
        Command command = new Command();
        Exec    exec;
        String  line;

        // get patch
        command.clear();
        if (ignoreWhitespaces)
        {
          command.append(Settings.cvsCommand,"diff","-u","-w","-b","-B");
        }
        else
        {
          command.append(Settings.cvsCommand,"diff","-u");
        }
        if (revision1 != null) command.append("-r",revision1);
        if (revision2 != null) command.append("-r",revision2);
        command.append("--");
        if (fileDataSet != null) command.append(getFileDataNames(existFileDataSet));
        exec = new Exec(rootPath,command);

        // read patch data
        while ((line = exec.getStdout()) != null)
        {
          if      (output   != null) output.println(line);
          else if (lineList != null) lineList.add(line);
        }

        // done
        exec.done();
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
    }

    // get complete patches for new files
    for (FileData fileData : newFileDataSet)
    {
      BufferedReader bufferedReader = null;
      try
      {
        String line;

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
        bufferedReader.close(); bufferedReader = null;
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
      finally
      {
        try { if (bufferedReader != null) bufferedReader.close(); } catch (IOException exception) { /* ignored */ }
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
    ByteArrayOutputStream output = new ByteArrayOutputStream(64*1024);
    try
    {
      Command command = new Command();
      Exec    exec;
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get patch
      command.clear();
      command.append(Settings.cvsCommand,"diff","-u");
      if (revision1 != null) command.append("-r",revision1);
      if (revision2 != null) command.append("-r",revision2);
      command.append("--");
      if (fileDataSet != null) command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);

      // read patch bytes into byte array stream
      while ((n = exec.readStdout(buffer)) > 0)
      {
        output.write(buffer,0,n);
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }

    // convert byte array stream into array
    return output.toByteArray();
  }

  /** get log to file
   * @param fileData file data
   * @return log array
   */
  public LogData[] getLog(FileData fileData)
    throws RepositoryException
  {
    ArrayList<LogData> logDataList = new ArrayList<LogData>();

    // get revision info list
    HashMap<String,String> branchNamesMap = new HashMap<String,String>();
    Command                command        = new Command();
    Exec                   exec;
    try
    {
      // get log
      command.clear();
      command.append(Settings.cvsCommand,"log");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,branchNamesMap))
      {
        // parse data
        RevisionDataCVS revisionData;
        while ((revisionData = parseLogData(exec,branchNamesMap)) != null)
        {
          // add log info entry
          logDataList.add(new LogData(revisionData.revision,
                                      revisionData.date,
                                      revisionData.author,
                                      revisionData.commitMessage
                                     )
                         );
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
//for (RevisionDataCVS revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

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
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^\\s*([\\.\\d]+)\\s+\\((\\S+)\\s+(\\S+)\\):\\s(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_EMPTY      = Pattern.compile("^\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<AnnotationData> annotationDataList = new ArrayList<AnnotationData>();

    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;
      Matcher matcher;

      // get annotations
      command.clear();
      command.append(Settings.cvsCommand,"annotate");
      if (revision != null) command.append("-r",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      /* parse annotation output
           Format:
             <revision> <author> <date> <line>
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
        else if (PATTERN_EMPTY.matcher(line).matches())
        {
          // skip empty lines
        }
        else
        {
          // unknown line
Dprintf.dprintf("unknown %s",line);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }

    return annotationDataList.toArray(new AnnotationData[annotationDataList.size()]);
  }

  /** update file from respository
   * @param fileDataSet file data set
   */
  public void update(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // update files
      command.clear();
      command.append(Settings.cvsCommand,"update","-d");
      if (Settings.cvsPruneEmtpyDirectories) command.append("-P");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void commit(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // commit files
      command.clear();
      command.append(Settings.cvsCommand,"commit","-F",commitMessage.getFileName());
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
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
    try
    {
      Command command = new Command();
      int     exitCode;

      // add files
      command.clear();
      command.append(Settings.cvsCommand,"add");
      if (binaryFlag) command.append("-k","b");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      if (commitMessage != null)
      {
        // commit added files
        command.clear();
        command.append(Settings.cvsCommand,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    try
    {
      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      // remove from repository
      Command command = new Command();
      int     exitCode;

      // remove files
      command.clear();
      command.append(Settings.cvsCommand,"remove");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      if (commitMessage != null)
      {
        // commit removed files
        command.clear();
        command.append(Settings.cvsCommand,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** revert files
   * @param fileDataSet file data set or null for all files
   * @param revision revision to revert to
   */
  public void revert(HashSet<FileData> fileDataSet, String revision)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      // revert files
      command.clear();
      command.append(Settings.cvsCommand,"update");
      if (revision.equals(LAST_REVISION_NAME))
      {
        command.append("-A");
      }
      else
      {
        command.append("-r",revision);
      }
      command.append("--");
      if (fileDataSet != null) command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
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
    try
    {
      Command command = new Command();
      int     exitCode;

      // rename local file
      File oldFile = new File(rootPath,fileData.getFileName());
      File newFile = new File(rootPath,newName);
      if (!newFile.exists())
      {
        if (!oldFile.renameTo(newFile))
        {
          throw new RepositoryException("Rename file '%s' to '%s' fail",oldFile.getName(),newFile.getName());
        }
      }
      else
      {
        throw new RepositoryException("File '%s' already exists",newFile.getName());
      }

      // add new file
      command.clear();
      command.append(Settings.cvsCommand,"add");
      switch (fileData.mode)
      {
        case TEXT:
          break;
        case BINARY:
          command.append("-k","b");
          break;
        default:
          break;
      }
      command.append("--");
      command.append(newFile.getName());
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        newFile.renameTo(oldFile);
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      // remove old file
      command.clear();
      command.append(Settings.cvsCommand,"remove");
      command.append("--");
      command.append(oldFile.getName());
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        newFile.renameTo(oldFile);
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      // commit
      if (commitMessage != null)
      {
        // commit remove/add (=rename) file
        command.clear();
        command.append(Settings.cvsCommand,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(oldFile.getName());
        command.append(newFile.getName());
        command.append((!rootPath.isEmpty()) ? rootPath+File.separator+newName : newName);
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          newFile.renameTo(oldFile);
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** get incoming changes list
   */
  public LogData[] getIncomingChanges()
    throws RepositoryException
  {
    return null;
  }

  /** get outgoing changes list
   */
  public LogData[] getOutgoingChanges()
    throws RepositoryException
  {
    return null;
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
   */
  public void pullChanges()
    throws RepositoryException
  {
  }

  /** push changes
   */
  public void pushChanges()
    throws RepositoryException
  {
  }

  /** apply patches
   */
  public void applyPatches()
    throws RepositoryException
  {
  }

  /** unapply patches
   */
  public void unapplyPatches()
    throws RepositoryException
  {
  }

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, CommitMessage commitMessage)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // set files modes
      command.clear();
      command.append(Settings.cvsCommand,"admin");
      switch (mode)
      {
        case TEXT:
          command.append("-kkv");
          break;
        case BINARY:
          command.append("-kb");
          break;
        default:
          break;
      }
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      // update files
      command.clear();
      command.append(Settings.cvsCommand,"update","-d","-A");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** create new branch
   * @param name branch name
   * @param commitMessage commit message
   * @param buysDialog busy dialog or null
   */
  public void newBranch(String name, CommitMessage commitMessage, BusyDialog busyDialog)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();

      // create branch
      command.clear();
      command.append(Settings.cvsCommand,"tag","-b",name);
      Exec exec = new Exec(rootPath,command);

      // read output
      String line;
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && ((line = exec.getStdout()) != null)
            )
      {
//Dprintf.dprintf("line=%s",line);
        if (busyDialog != null) busyDialog.updateText(line);
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
      else
      {
        exec.destroy();
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  //-----------------------------------------------------------------------

  /** create temporary directory
   * @param path path
   * @return temporary directory
   */
  private File createTempDirectory(String path)
    throws IOException
  {
    final int MAX_TRIES = 10000;

    for (int i = 0; i < MAX_TRIES; i++)
    {
      File tmpDirectory = new File(path,String.format("tmp-%06x",i));
      if (tmpDirectory.mkdirs())
      {
        return tmpDirectory;
      }
    }

    throw new IOException("too many retries when creating temporary directory");
  }

  private File createTempDirectory()
    throws IOException
  {
    return createTempDirectory(System.getProperty("java.io.tmpdir"));
  }

  /** parse CVS state string
   * @param string state string
   * @return state
   */
  private FileData.States parseState(String string)
  {
    if      (string.equalsIgnoreCase("Up-to-date"         )) return FileData.States.OK;
    else if (string.equalsIgnoreCase("Unknown"            )) return FileData.States.UNKNOWN;
    else if (string.equalsIgnoreCase("Update"             )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs patch"        )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs update"       )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs Checkout"     )) return FileData.States.CHECKOUT;
    else if (string.equalsIgnoreCase("Locally Modified"   )) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("Needs Merge"        )) return FileData.States.MERGE;
    else if (string.equalsIgnoreCase("Unresolved Conflict")) return FileData.States.CONFLICT;
    else if (string.equalsIgnoreCase("Conflict"           )) return FileData.States.CONFLICT;
    else if (string.equalsIgnoreCase("Locally Added"      )) return FileData.States.ADDED;
    else if (string.equalsIgnoreCase("Entry invalid"      )) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("Removed"            )) return FileData.States.REMOVED;
    else                                                     return FileData.States.UNKNOWN;
  }

  /** parse CVS diff index
   * @param string diff index string
   * @return index array [lineNb,length] or null
   */
  private int[] parseDiffIndex(String string)
  {
    Object[] data = new Object[2];
    if      (StringParser.parse(string,"%d,%d",data))
    {
      return new int[]{(Integer)data[0],(Integer)data[1]-(Integer)data[0]+1};
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

  /** parse log header
   * @param exec exec command
   * @param branchNamesMap branch names map to fill or null (map revision -> name)
   * @param tagNamesMap tag names map to fill or null (map name -> revision)
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec, HashMap<String,String> branchNamesMap, HashMap<String,String> tagNamesMap)
    throws IOException
  {
    final Pattern PATTERN_HEAD           = Pattern.compile("^\\s*head:\\s*(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_SYMBOLIC_NAMES = Pattern.compile("^\\s*symbolic names:.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_SYMBOLIC_NAME  = Pattern.compile("^\\s+(.*)\\s*:\\s*(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_BRANCH_NAME    = Pattern.compile("^\\s+(.*)\\s*:\\s*([\\d\\.]+)\\.0\\.(\\d+)\\s*",Pattern.CASE_INSENSITIVE);

    // parse header
    boolean headerDone = false;
    Matcher matcher;
    String  line;
    while (   !headerDone
           && ((line = exec.getStdout()) != null)
          )
    {
      if      (line.startsWith("-----"))
      {
        headerDone = true;
      }
      else if ((matcher = PATTERN_HEAD.matcher(line)).matches())
      {
        // get head revision tag
        if (tagNamesMap != null) tagNamesMap.put(LAST_REVISION_NAME,matcher.group(1));
      }
      else if (PATTERN_SYMBOLIC_NAMES.matcher(line).matches())
      {
        // get symbolic tag/branch names
        boolean symbolicNamesDone = false;
        while (   ((line = exec.getStdout()) != null)
               && !symbolicNamesDone
              )
        {
          if      ((matcher = PATTERN_BRANCH_NAME.matcher(line)).matches())
          {
            // branch name
            if (branchNamesMap != null) branchNamesMap.put(matcher.group(2)+"."+matcher.group(3),matcher.group(1));
            if (tagNamesMap != null) tagNamesMap.put(LAST_REVISION_NAME,matcher.group(1));
          }
          else if ((matcher = PATTERN_SYMBOLIC_NAME.matcher(line)).matches())
          {
            // tag name
            if (tagNamesMap != null) tagNamesMap.put(matcher.group(1),matcher.group(2));
          }
          else
          {
            symbolicNamesDone = true;
          }
        }
        exec.ungetStdout(line);
//for (String s : branchNamesMap.keySet()) Dprintf.dprintf("symbol %s=%s",s,branchNamesMap.get(s));
      }
    }

    return headerDone;
  }

  /** parse log header
   * @param exec exec command
   * @param branchNamesMap branch names map to fill or null (map revision -> name)
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec, HashMap<String,String> branchNamesMap)
    throws IOException
  {
    return parseLogHeader(exec,branchNamesMap,null);
  }

  /** parse log data
   * @param exec exec command
   * @param branchNamesMap branch names map or null (map revision -> name)
   * @param tagNamesMap tag names map or null (map name -> revision)
   * @return revision data or null
   */
  private RevisionDataCVS parseLogData(Exec exec, HashMap<String,String> branchNamesMap, HashMap<String,String> tagNamesMap)
    throws IOException
  {
    final Pattern PATTERN_REVISION    = Pattern.compile("^\\s*revision\\s+(\\S+).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DATE_AUTHOR = Pattern.compile("^\\s*date:\\s+([-\\d/]*\\s+[\\d: +]*);\\s*author:\\s*(\\S*);.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_BRANCES     = Pattern.compile("^\\s*branches:\\s*(.*).*",Pattern.CASE_INSENSITIVE);

    RevisionDataCVS    revisionData       = null;

    boolean            dataDone           = false;
    Matcher            matcher;
    String             line;
    String             revision           = null;
    ArrayList<Integer> revisionNumberList = new ArrayList<Integer>();
    Date               date               = null;
    String             author             = null;
    ArrayList<String>  branchRevisionList = new ArrayList<String>();
    LinkedList<String> commitMessageList  = new LinkedList<String>();
    while (   !dataDone
           && ((line = exec.getStdout()) != null)
          )
    {
      if      (line.startsWith("-----") || line.startsWith("====="))
      {
        // ignored
      }
      else if ((matcher = PATTERN_REVISION.matcher(line)).matches())
      {
        // revision number
        revision = matcher.group(1);
        for (String string : revision.split("\\."))
        {
          revisionNumberList.add(Integer.parseInt(string));
        }
      }
      else if ((matcher = PATTERN_DATE_AUTHOR.matcher(line)).matches())
      {
        // date, author
        date   = parseDate(matcher.group(1));
        author = matcher.group(2);

        // get branch names (if any)
        if ((line = exec.getStdout()) != null)
        {
          if ((matcher = PATTERN_BRANCES.matcher(line)).matches())
          {
            for (String branchName : matcher.group(1).split(";"))
            {
              branchRevisionList.add(branchName.trim());
            }
          }
          else
          {
            exec.ungetStdout(line);
          }
        }

        // get commit message lines
        while (  ((line = exec.getStdout()) != null)
               && !line.startsWith("-----")
               && !line.startsWith("=====")
              )
        {
          if (!line.equals("*** empty log message ***"))
          {
            commitMessageList.add(line);
          }
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

        // add log info entry
        revisionData = new RevisionDataCVS(revision,
                                           revisionNumberList,
                                           (branchNamesMap != null) ? branchNamesMap.get(revision) : "",
                                           date,
                                           author,
                                           branchRevisionList,
                                           commitMessageList
                                          );
        dataDone = true;
      }
    }

    return revisionData;
  }

  /** parse log data
   * @param exec exec command
   * @param branchNamesMap branch names map or null (map revision -> name)
   * @return revision data or null
   */
  private RevisionDataCVS parseLogData(Exec exec, HashMap<String,String> branchNamesMap)
    throws IOException
  {
    return parseLogData(exec,branchNamesMap,null);
  }

  /** get sub-numbers from numbers
   * @param numbers numbers
   * @param delta delta (<=0)
   * @return numbers [0..numbers.length+delta]
   */
  private int[] getSubNumbers(int[] numbers, int delta)
  {
    assert numbers.length+delta >= 0 : numbers.length+", "+delta;

    return Arrays.copyOfRange(numbers,0,numbers.length+delta);
  }

  /** get id from revision numbers
   * @param numbers revision numbers
   * @param delta delta (<=0)
   * @return revision id string with numbers [0..numbers.length+delta]
   */
  private String getId(int[] numbers, int delta)
  {
    assert numbers.length+delta >= 0 : numbers.length+", "+delta;

    StringBuilder buffer = new StringBuilder();
    for (int z = 0; z < numbers.length+delta; z++)
    {
      if (buffer.length() > 0) buffer.append('.');
      buffer.append(Integer.toString(numbers[z]));
    }
    return buffer.toString();
  }

  /** get id from revision numbers
   * @param numbers revision numbers
   * @param delta delta (<=0)
   * @return revision id string
   */
  private String getId(int[] numbers)
  {
    return getId(numbers,0);
  }

  /** compare revision numbers
   * @param numbers1,numbers2 numbers to compare
   * @param delta1,delta2 numbers to ignore at end (<= 0!)
   * @return true iff revision numbers are equal
   */
  private boolean equalsRevisionNumbers(int[] numbers1, int delta1, int[] numbers2, int delta2)
  {
    boolean equal = true;

    assert numbers1.length+delta1 >= 0;
    assert numbers2.length+delta2 >= 0;

    if ((numbers1.length+delta1) == (numbers2.length+delta2))
    {
      for (int z = 0; z < numbers1.length+delta1; z++)
      {
        if (numbers1[z] != numbers2[z])
        {
          equal = false;
          break;
        }
      }
    }
    else
    {
      equal = false;
    }

    return equal;
  }

  /**
   * @param
   * @return
   */
  private boolean equalsRevisionNumbers(int[] numbers1, int delta1, int[] numbers2)
  {
    return equalsRevisionNumbers(numbers1,delta1,numbers2,0);
  }

  /** create revision tree
   * @param revisionDataList revision info list
   * @param branchNamesMap branch names map (map revision -> name)
   * @param branchesMap branches
   * @param branchRevisionNumbers branch revision numbers
   * @return revision tree
   */
  private RevisionDataCVS[] createRevisionDataTree(LinkedList<RevisionDataCVS>     revisionDataList,
                                                   HashMap<String,String>          branchNamesMap,
                                                   HashMap<String,RevisionDataCVS> branchesMap,
                                                   int[]                           branchRevisionNumbers
                                                  )
  {
    LinkedList<RevisionDataCVS> revisionDataTreeList = new LinkedList<RevisionDataCVS>();

    boolean  branchDone = false;
    while (!revisionDataList.isEmpty() && !branchDone)
    {
      RevisionDataCVS revisionData = revisionDataList.getFirst();
//Dprintf.dprintf("revisionData=%s -- %d %d",revisionData,revisionData.revisionNumbers.length,branchRevisionNumbers.length);

      // get branch id this revision belongs to (n-1 numbers of revision)
      String branchId = getId(revisionData.revisionNumbers,-1);

      // add revision
      if      (revisionData.revisionNumbers.length-2 > branchRevisionNumbers.length)
      {
        // found something like <...>.x.y -> create branch sub-tree
        RevisionDataCVS[] subRevisionTree = createRevisionDataTree(revisionDataList,
                                                                   branchNamesMap,
                                                                   branchesMap,
                                                                   getSubNumbers(revisionData.revisionNumbers,-2)
                                                                  );

        // find revision data to add/create revision data
        RevisionDataCVS branchRevisionData = branchesMap.get(branchId);
        if (branchRevisionData == null)
        {
          // still not a known branch -> add to revision tree and create branch
          revisionDataTreeList.addFirst(revisionData);

          // store branches of this revision
          branchesMap.put(branchId,revisionData);
          if (revisionData.branchRevisions != null)
          {
            for (String branchRevision : revisionData.branchRevisions)
            {
              branchesMap.put(branchRevision,revisionData);
            }
          }

          branchRevisionData = revisionData;
        }

        // get branch name
        String branchName = branchNamesMap.get(branchId);
        if (branchName == null) branchName = branchId;

        // add branch
        branchRevisionData.addBranch(branchName,
                                     revisionData.date,
                                     revisionData.author,
                                     revisionData.commitMessage,
                                     subRevisionTree
                                    );
      }
      else if (   (revisionData.revisionNumbers.length-2 == branchRevisionNumbers.length)
               && equalsRevisionNumbers(revisionData.revisionNumbers,-2,branchRevisionNumbers)
              )
      {
        // add revision to tree
        revisionDataTreeList.addFirst(revisionData);

        // store branches of this revision
        if (revisionData.branchRevisions != null)
        {
          for (String branchRevision : revisionData.branchRevisions)
          {
            branchesMap.put(branchRevision,revisionData);
          }
        }

        // done this revision
        revisionDataList.removeFirst();
      }
      else
      {
        // branch is complete
        branchDone = true;
      }
    }

    return revisionDataTreeList.toArray(new RevisionDataCVS[revisionDataTreeList.size()]);
  }

  /** create revision tree
   * @param revisionDataList revision info list
   * @param branchNamesMap branch names map (map revision -> name)
   * @return revision tree
   */
  private RevisionData[] createRevisionDataTree(LinkedList<RevisionDataCVS> revisionDataList,
                                                HashMap<String,String>      branchNamesMap
                                               )
  {
    return createRevisionDataTree(revisionDataList,
                                  branchNamesMap,
                                  new HashMap<String,RevisionDataCVS>(),
                                  new int[]{}
                                 );
  }
}

/* end of file */
