/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: Mecurial repository functions
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Stack;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/****************************** Classes ********************************/

/** Mercurial (hg) repository
 */
@XmlType(propOrder={"masterRepository"})
@XmlAccessorType(XmlAccessType.NONE)
class RepositoryHG extends Repository
{
  /** HG revision data
   */
  class RevisionDataHG extends RevisionData
  {
    private String changeSet;

    /** create HG revision data
     * @param revision revision
     * @param changeSet change-set id
     * @param tagList list with tags
     * @param date date
     * @param author author name
     * @param commitMessageList commit message
     */
    public RevisionDataHG(String revision, String changeSet, AbstractList<String> tagList, Date date, String author, AbstractList<String> commitMessageList)
    {
      super(revision,tagList,date,author,commitMessageList);
      this.changeSet = changeSet;
    }

    /** create HG revision data
     * @param revision revision
     */
    public RevisionDataHG(String revision)
    {
      super(revision);
    }

    /** get revision text for display
     * @return revision text
     */
    public String getRevisionText()
    {
      return revision+" ("+changeSet+")";
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "HG revision data {revision: "+revision+", date: "+date+", author: "+author+", message: "+commitMessage+"}";
    }
  }

  /** parent data
   */
  class ParentData
  {
    private int revision1,revision2;

    ParentData(int revision1, int revision2)
    {
      this.revision1 = revision1;
      this.revision2 = revision2;
    }

    ParentData(int revision1)
    {
      this(revision1,0);
    }

    public String toString()
    {
      if (revision2 != 0)
      {
        return Integer.toString(revision1)+","+Integer.toString(revision2);
      }
      else
      {
        return Integer.toString(revision1);
      }
    }
  }

  // --------------------------- constants --------------------------------
  private final String LAST_REVISION_NAME = "tip";

  /* log format template:
    <rev> <changeset> <date> <time> <timezone> <author>
    <parents>
    <branches>
    <tags>
    <description>
    -----
    ...
  */
  private final String LOG_TEMPLATE = "{rev} {node|short} {date|isodate} {author}\\n{parents}\\n{branches}\\n{tags}\\n{desc}\\n-----\\n";

  // --------------------------- variables --------------------------------
  @XmlElement(name = "masterRepository")
  @RepositoryValue(title = "Master repository:", pathSelector=true, tooltip="Path to master repository.")
  public String masterRepository;

  private HashMap<Integer,ParentData> parentMap = null;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository
   * @param rootPath root path
   */
  RepositoryHG(String rootPath)
  {
    super(rootPath);

    // start getting parent map
    Background.run(new BackgroundRunnable()
    {
      public void run()
      {
// NYI: is the global parent map needed?
        parentMap = getParentMap();
//for (int i : parentMap.keySet()) Dprintf.dprintf("%d -> %s",i,parentMap.get(i));
      }
    });
  }

  /** create repository
   */
  RepositoryHG()
  {
    this(null);
  }

  /** check if repository support incoming/outgoing commands
   * @return true iff incoming/outgoing commands are supported
   */
  public boolean supportIncomingOutgoing()
  {
    return true;
  }

  /** check if repository support pull/push commands
   * @return true iff pull/push commands are supported
   */
  public boolean supportPullPush()
  {
    return true;
  }

  /** check if repository support patch queues
   * @return true
   */
  public boolean supportPatchQueues()
  {
    return true;
  }

  /** check if commit message is valid and acceptable
   * @return true iff commit message accepted
   */
  public boolean validCommitMessage(CommitMessage commitMessage)
  {
    return    !Settings.hgSingleLineCommitMessages
           || (Settings.hgSingleLineMaxCommitMessageLength == 0)
           || (commitMessage.getMessage(", ").length() <= Settings.hgSingleLineMaxCommitMessageLength);
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
      // checkout
      command.clear();
      command.append(Settings.hgCommand,(Settings.hgUseForestExtension) ? "fclone" : "clone","-v");
      if (!revision.isEmpty()) command.append("-r",revision);
      command.append(repositoryPath+File.separator+moduleName,destinationPath);
      Exec exec = new Exec(rootPath,command);

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
          if (busyDialog != null) busyDialog.updateText(line.substring(8));
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
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
  {
    final Pattern PATTERN_STATUS = Pattern.compile("^\\s*(.)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

    Command         command            = new Command();
    Exec            exec;
    String          line;
    Matcher         matcher;
    String          name               = null;
    FileData.States state              = FileData.States.UNKNOWN;
    FileData.Modes  mode               = FileData.Modes.UNKNOWN;
    String          workingRevision    = "";
    String          repositoryRevision = "";
    for (String directory : fileDirectorySet)
    {
      exec = null;
      try
      {
        // get status
        command.clear();
        command.append(Settings.hgCommand,"-y","status","-mardcu");
        command.append("--");
        if (!directory.isEmpty()) command.append(directory);
        exec = new Exec(rootPath,command);

        // parse status data
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // check if one entry is complete
          // match name, state
          if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
          {
            state = parseState(matcher.group(1));
            name  = matcher.group(2);

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
              // check if file not in sub-directory (hg list all files :-()
              String parentDirectory = new File(name).getParent();
              if (   ((parentDirectory == null) && directory.isEmpty())
                  || ((parentDirectory != null) && parentDirectory.equals(directory))
                 )
              {
                // get file type, size, date/time
                File file = new File(rootPath,name);
                FileData.Types type     = getFileType(file);
                long           size     = file.length();
                Date           datetime = new Date(file.lastModified());

                // create file data
                newFileDataSet.add(new FileData(name,
                                                type,
                                                state,
                                                mode,
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
            Onzen.printWarning("No match for line '%s'",line);
          }
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

      exec = null;
      try
      {
        // get revision (identity)
        command.clear();
        command.append(Settings.hgCommand,"identify","-t");
        command.append("--");
        exec = new Exec(rootPath,command);
        if ((line = exec.getStdout()) != null)
        {
          for (FileData fileData : fileDataSet)
          {
            fileData.workingRevision = line;
          }
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

      exec = null;
      try
      {
        // get branch
        command.clear();
        command.append(Settings.hgCommand,"branch");
        command.append("--");
        exec = new Exec(rootPath,command);
        if ((line = exec.getStdout()) != null)
        {
          for (FileData fileData : fileDataSet)
          {
            fileData.branch = line;
          }
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
    return Types.HG;
  }

  /** get repository path
   * @return repository path
   */
  public String getRepositoryPath()
  {
    String repositoryPath = "";

    // get root
    Command command = new Command();
    Exec    exec;
    String  line;
    try
    {
      command.clear();
      command.append(Settings.hgCommand,"root");
      command.append("--");
      exec = new Exec(rootPath,command);

      repositoryPath = exec.getStdout();

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      // ignored
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
    final Pattern PATTERN_REVSION = Pattern.compile("^(\\d+).*",Pattern.CASE_INSENSITIVE);

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
      command.append(Settings.hgCommand,"-y","-v","log","--template","{rev} {node|short}\\n");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse revisions in log output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_REVSION.matcher(line)).matches())
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
    Arrays.sort(revisions,new Comparator<String>()
    {
      public int compare(String revision1, String revision2)
      {
        int number1 = Integer.parseInt(revision1);
        int number2 = Integer.parseInt(revision2);
        if      (number1 < number2)
        {
          return -1;
        }
        else if (number1 > number2)
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
    RevisionData revisionData = null;

    // get revision data
    Exec exec = null;
    try
    {
      Command command = new Command();

      // get single log entry
      command.clear();
      command.append(Settings.hgCommand,"-y","-v","log");
      if (revision != null) command.append(Settings.hgCommand,"-r",revision);
      command.append(Settings.hgCommand,"-l","1","--template",LOG_TEMPLATE);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        revisionData = parseLogData(exec);
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

    return revisionData;
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException
  {
    LinkedList<RevisionDataHG> revisionDataList = new LinkedList<RevisionDataHG>();

    // get revision info list
    Exec exec = null;
    try
    {
      Command command = new Command();

      // get log
      command.clear();
      command.append(Settings.hgCommand,"-y","-v","log","--template",LOG_TEMPLATE);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        HashMap<Integer,RevisionData> revisionDataMap = new HashMap<Integer,RevisionData>();
        HashMap<RevisionData,ParentData> fileParentMap = new HashMap<RevisionData,ParentData>();
        RevisionDataHG revisionData;
        while ((revisionData = parseLogData(exec,revisionDataMap,fileParentMap)) != null)
        {
          // add revision info entry
          revisionDataList.addFirst(revisionData);
        }

        // set parents
        for (RevisionData parentRevisionData : fileParentMap.keySet())
        {
          ParentData parentData = fileParentMap.get(parentRevisionData);

          RevisionData parentRevisionData1 = getParentRevisionData(parentData.revision1,revisionDataMap,fileParentMap);
          RevisionData parentRevisionData2 = getParentRevisionData(parentData.revision2,revisionDataMap,fileParentMap);

/*
if ((parentData.revision1 != 0) && (parentRevisionData1==null))
{
Dprintf.dprintf("parent not found %s",parentData.revision1);
//System.exit(1);
}
if ((parentData.revision2 != 0) && (parentRevisionData2==null))
{
Dprintf.dprintf("parent not found %s",parentData.revision2);
//System.exit(1);
}
*/
          if      ((parentRevisionData1 != null) && (parentRevisionData2 != null))
          {
            parentRevisionData.parents = new RevisionData[]{parentRevisionData1,parentRevisionData2};
          }
          else if (parentRevisionData1 != null)
          {
            parentRevisionData.parents = new RevisionData[]{parentRevisionData1};
          }
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
//for (RevisionDataHG revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

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
      command.append(Settings.hgCommand,"-y","cat");
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
      command.append(Settings.hgCommand,"-y","cat");
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
    final Pattern PATTERN_STATUS = Pattern.compile("^\\s*(.)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

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
      command.append(Settings.hgCommand,"-y","status","-mardu");
      command.append("--");
      command.append("glob:**");
      exec = new Exec(rootPath,command);

      // read list
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // check if one entry is complete
        // match name, state
        if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
        {
          state = parseState(matcher.group(1));
          name  = matcher.group(2);

          if (stateSet.contains(state))
          {
            fileDataSet.add(new FileData(name,
                                         state,
                                         FileData.Modes.BINARY
                                        )
                           );
          }
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for line '%s'",line);
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
    final Pattern PATTERN_DIFF_START  = Pattern.compile("^\\+\\+\\+.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF        = Pattern.compile("^@@\\s+\\-([\\d,]*)\\s+\\+([\\d,]*)\\s+@@$",Pattern.CASE_INSENSITIVE);

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
        command.append(Settings.hgCommand,"-y","cat","-r",newRevision);
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
      command.append(Settings.hgCommand,"-y","diff");
      if (oldRevision != null) command.append("-r",oldRevision);
      if (newRevision != null) command.append("-r",newRevision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      /* skip diff header (3 lines)
           diff -r ...
           --- ...
           +++ ...
      */
      exec.getStdout();
      exec.getStdout();
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
        if      ((matcher = PATTERN_DIFF.matcher(line)).matches())
        {
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

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
              keepLinesList.add(newFileLines[lineNb-1]);
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
          Onzen.printWarning("No match for line '%s'",line);
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

  /** get patch lines for file
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

        // get sub-directory relative to hg root path
        String subDirectory = getSubDirectory();

        // get patch
        command.clear();
        command.append(Settings.hgCommand);
        if (!Settings.hgDiffCommand.isEmpty())
        {
          // use external diff command
          command.append("extdiff","-p",Settings.hgDiffCommand);
          if (ignoreWhitespaces)
          {
            if (!Settings.hgDiffCommandOptionsIgnoreWhitespaces.isEmpty())
            {
              command.append("-o",Settings.hgDiffCommandOptionsIgnoreWhitespaces);
            }
          }
          else
          {
            if (!Settings.hgDiffCommandOptions.isEmpty())
            {
              command.append("-o",Settings.hgDiffCommandOptions);
            }
          }
        }
        else
        {
          // use internal diff
          command.append("diff");
          if (ignoreWhitespaces)
          {
            command.append("-w","-b","-B","--git");
          }
          else
          {
            command.append("--git");
          }
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
            else if (Settings.hgRelativePatchPaths)
            {
              // relative hg path -> convert to relative path to root path
              fileName = name.startsWith(subDirectory+File.separator)?name.substring(subDirectory.length()+1):name;
            }
            else
            {
              // use original name
              fileName = prefix+name;
            }

            // remove sub-directory if needed
            if ((subDirectory.length() > 0) && (fileName.length() > subDirectory.length()) && fileName.startsWith(subDirectory)) fileName = fileName.substring(subDirectory.length()+1);

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
            else if (Settings.hgRelativePatchPaths)
            {
              // relative hg path -> convert to relative path to root path
              fileName = name.startsWith(subDirectory+File.separator)?name.substring(subDirectory.length()+1):name;
            }
            else
            {
              // use original name
              fileName = prefix+name;
            }

            // remove sub-directory if needed
            if ((subDirectory.length() > 0) && (fileName.length() > subDirectory.length()) && fileName.startsWith(subDirectory)) fileName = fileName.substring(subDirectory.length()+1);

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
    ByteArrayOutputStream output = new ByteArrayOutputStream(64*1024);

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get patch
      command.clear();
      command.append(Settings.hgCommand,"diff");
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
      command.append(Settings.hgCommand,"-y","-v","log","--template",LOG_TEMPLATE);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionDataHG revisionData;
        while ((revisionData = parseLogData(exec)) != null)
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
//for (RevisionDataHG revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);

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
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s+(.*):\\s(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_EMPTY      = Pattern.compile("^\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<AnnotationData> annotationDataList = new ArrayList<AnnotationData>();

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      String  line;

      // get annotations
      command.clear();
      command.append(Settings.hgCommand,"blame","-n","-u","-d");
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
          annotationDataList.add(new AnnotationData(matcher.group(2),
                                                    matcher.group(1),
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
          Onzen.printWarning("No match for line '%s'",line);
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
   * @param fileDataSet file data set
   */
  public void update(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      String[] trees;
      if (Settings.hgUseForestExtension)
      {
        // get trees of forest (if forest extension is enabled)
        trees = getTrees();
      }
      else
      {
        // only single tree
        trees = new String[]{rootPath};
      }

      // unapply patches for all trees in forest/single repository
      for (String tree : trees)
      {
        command.clear();
        command.append(Settings.hgCommand,"qpop","-a");
        command.append("--");
        exec = new Exec(tree,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
        exec.done(); exec = null;
      }

      // update files (Note: hg does not allow to update single files, thus update all files)
      if      (Settings.hgUpdateWithFetch)
      {
        StoredFiles   storedFiles   = null;
        StoredChanges storedChanges = null;
        try
        {
          // create files backup of all local changes (in case there occur some error)
          storedFiles = new StoredFiles(rootPath,getChangedFiles());

          if (Settings.hgSafeUpdate)
          {
            // do 'safe' update when fetch is used: store local changes into patch
            storedChanges = new StoredChanges();
          }

          // revert all local changes
          command.clear();
          command.append(Settings.hgCommand,"revert","--no-backup","-a");
          command.append("--");
          exec = new Exec(rootPath,command);
          exitCode = exec.waitFor();
          if (exitCode != 0)
          {
            if (Settings.hgUpdateWithFetch && Settings.hgSafeUpdate)
            {
              storedFiles.restore(); storedFiles = null;
            }
            throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
          }
          exec.done(); exec = null;

          // fetch+fpush
          command.clear();
          command.append(Settings.hgCommand,"fetch");
          command.append("--");
          if ((masterRepository != null) && !masterRepository.isEmpty()) command.append(masterRepository);
          exec = new Exec(rootPath,command);
          exitCode = exec.waitFor();
          if (exitCode != 0)
          {
            if (Settings.hgUpdateWithFetch && Settings.hgSafeUpdate)
            {
              storedFiles.restore(); storedFiles = null;
            }
            throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
          }
          exec.done(); exec = null;

          command.clear();
          command.append(Settings.hgCommand,"fpush");
          command.append("--");
          if ((masterRepository != null) && !masterRepository.isEmpty()) command.append(masterRepository);
          exec = new Exec(rootPath,command);
          exitCode = exec.waitFor();
          if (exitCode != 0)
          {
            if (Settings.hgUpdateWithFetch && Settings.hgSafeUpdate)
            {
              storedFiles.restore(); storedFiles = null;
            }
            throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
          }
          exec.done(); exec = null;

          if (Settings.hgSafeUpdate)
          {
            // do 'safe' update when fetch is used: restore local changes and merge if needed
            if (!storedChanges.restore())
            {
              throw new RepositoryException("restore local changes fail");
            }

            // discard changes patch
            storedChanges.discard(); storedChanges = null;
          }

          // discard files backup
          storedFiles.discard(); storedFiles = null;
        }
        finally
        {
          if (storedChanges != null) storedChanges.discard();
          if (storedFiles != null) storedFiles.restore();
        }
      }
      else if (Settings.hgUseForestExtension)
      {
        command.clear();
        command.append(Settings.hgCommand,"fupdate");
        command.append("--");
        exec = new Exec(rootPath,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
        exec.done(); exec = null;
      }
      else
      {
        command.clear();
        command.append(Settings.hgCommand,"update");
        command.append("--");
        exec = new Exec(rootPath,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
        exec.done(); exec = null;
      }

      // apply patches for all trees in forest/single repository
      for (String tree : trees)
      {
        command.clear();
        command.append(Settings.hgCommand,"qpush","-a");
        command.append("--");
        exec = new Exec(tree,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
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
      command.append(Settings.hgCommand,"commit");
      if (!commitMessage.isEmpty())
      {
        if (Settings.hgSingleLineCommitMessages)
        {
          command.append("-m",commitMessage.getMessage(", "));
        }
        else
        {
          command.append("-l",commitMessage.getFileName());
        }
      }
      else
      {
        command.append("-m","empty");
      }
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
      exec.done(); exec = null;

      // immediate push changes when configured
      if (Settings.hgImmediatePush)
      {
        pushChanges();
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
      command.append(Settings.hgCommand,"add");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
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
      int     exitCode;

      // remove files
      command.clear();
      command.append(Settings.hgCommand,"remove");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
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

  /** revert files
   * @param fileDataSet file data set or null for all files
   * @param revision revision to revert to
   */
  public void revert(HashSet<FileData> fileDataSet, String revision)
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // revert files
      command.clear();
      command.append(Settings.hgCommand,"revert","--no-backup");
      // Note: hg revert to last pulled repository version when "tip" is given, not the local version
      if (!revision.equals(LAST_REVISION_NAME)) command.append(Settings.hgCommand,"-r",revision);
      if (fileDataSet != null)
      {
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
      }
      else
      {
        command.append("-a");
        command.append("--");
      }
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
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
      command.append(Settings.hgCommand,"rename");
      command.append("--");
      command.append(fileData.getFileName());
      command.append(newName);
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
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

  /** get incoming changes list
   */
  public LogData[] getIncomingChanges()
    throws RepositoryException
  {
    final Pattern PATTERN_CHANGE = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<LogData> logDataList = new ArrayList<LogData>();

    Exec exec = null;
    try
    {
      Command            command           = new Command();
      String             line;
      Matcher            matcher;
      String             revision          = null;
      String             changeSet         = null;
      Date               date              = null;
      String             author            = null;
      ArrayList<String>  fileList          = new ArrayList<String>();
      LinkedList<String> commitMessageList = new LinkedList<String>();

      // get log
      command.clear();
      command.append(Settings.hgCommand,"-y","-v","incoming","--template","{rev} {node|short} {date|isodate} {author|person}\\n{files}\\n-----\\n{desc}\\n-----\\n");
      command.append("--");
      exec = new Exec(rootPath,command);

      // parse revisions in log output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match revision, node, date author
        if      ((matcher = PATTERN_CHANGE.matcher(line)).matches())
        {
          revision  = matcher.group(1);
          changeSet = matcher.group(2);
          date      = parseDate(matcher.group(3));
          author    = matcher.group(4);

          // get files
          fileList.clear();
          while (  ((line = exec.getStdout()) != null)
                 && !line.startsWith("-----")
                )
          {
            fileList.add(line);
          }

          // get commit message
          commitMessageList.clear();
          while (  ((line = exec.getStdout()) != null)
                 && !line.startsWith("-----")
                )
          {
            commitMessageList.add(line);
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

          logDataList.add(new LogData(revision,
                                      date,
                                      author,
                                      commitMessageList.toArray(new String[commitMessageList.size()])
                                     )
                         );
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

    return logDataList.toArray(new LogData[logDataList.size()]);
  }

  /** get outgoing changes list
   */
  public LogData[] getOutgoingChanges()
    throws RepositoryException
  {
    final Pattern PATTERN_CHANGE = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<LogData> logDataList = new ArrayList<LogData>();

    Exec exec = null;
    try
    {
      Command            command           = new Command();
      String             line;
      Matcher            matcher;
      String             revision          = null;
      String             changeSet         = null;
      Date               date              = null;
      String             author            = null;
      ArrayList<String>  fileList          = new ArrayList<String>();
      LinkedList<String> commitMessageList = new LinkedList<String>();

      // get log
      command.clear();
      command.append(Settings.hgCommand,"-y","-v","outgoing","--template","{rev} {node|short} {date|isodate} {author|person}\\n{files}\\n-----\\n{desc}\\n-----\\n");
      command.append("--");
      exec = new Exec(rootPath,command);

      // parse revisions in log output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match revision, node, date author
        if      ((matcher = PATTERN_CHANGE.matcher(line)).matches())
        {
          revision  = matcher.group(1);
          changeSet = matcher.group(2);
          date      = parseDate(matcher.group(3));
          author    = matcher.group(4);

          // get files
          fileList.clear();
          while (  ((line = exec.getStdout()) != null)
                 && !line.startsWith("-----")
                )
          {
            fileList.add(line);
          }

          // get commit message
          commitMessageList.clear();
          while (  ((line = exec.getStdout()) != null)
                 && !line.startsWith("-----")
                )
          {
            commitMessageList.add(line);
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

          logDataList.add(new LogData(revision,
                                      date,
                                      author,
                                      commitMessageList.toArray(new String[commitMessageList.size()])
                                     )
                         );
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

    return logDataList.toArray(new LogData[logDataList.size()]);
  }

  /** pull changes
   */
  public void pullChanges()
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      command.clear();
      command.append(Settings.hgCommand,Settings.hgUseForestExtension?"fpull":"pull");
      command.append("--");
      if ((masterRepository != null) && !masterRepository.isEmpty()) command.append(masterRepository);
      exec = new Exec(rootPath,command);
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
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
   */
  public void pushChanges()
    throws RepositoryException
  {
    final Pattern PATTERN_ABORT = Pattern.compile("^\\s*abort:.*",Pattern.CASE_INSENSITIVE);

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;
      String  line;
      Matcher matcher;

      command.clear();
      command.append(Settings.hgCommand,Settings.hgUseForestExtension?"fpush":"push");
      command.append("--");
      if ((masterRepository != null) && !masterRepository.isEmpty()) command.append(masterRepository);
      exec = new Exec(rootPath,command);

      // bug in hg: hg does not report an exitcode != 0 if something go wrong => check error output
      exitCode = 0;
      while ((line = exec.getStderr()) != null)
      {
        if ((matcher = PATTERN_ABORT.matcher(line)).matches())
        {
          exitCode = 1;
        }
      }
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }
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
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      String[] trees;
      if (Settings.hgUseForestExtension)
      {
        // get trees of forest (if forest extension is enabled)
        trees = getTrees();
      }
      else
      {
        // only single tree
        trees = new String[]{rootPath};
      }

      // unapply patches for all trees in forest/single repository
      for (String tree : trees)
      {
        command.clear();
        command.append(Settings.hgCommand,"qpush","-a");
        command.append("--");
        exec = new Exec(tree,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
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

  /** unapply patches
   */
  public void unapplyPatches()
    throws RepositoryException
  {
    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      String[] trees;
      if (Settings.hgUseForestExtension)
      {
        // get trees of forest (if forest extension is enabled)
        trees = getTrees();
      }
      else
      {
        // only single tree
        trees = new String[]{rootPath};
      }

      // unapply patches for all trees in forest/single repository
      for (String tree : trees)
      {
        command.clear();
        command.append(Settings.hgCommand,"qpop","-a");
        command.append("--");
        exec = new Exec(tree,command);
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
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

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, CommitMessage commitMessage)
    throws RepositoryException
  {
  }

  /** create new branch
   * @param name branch name
   * @param commitMessage commit message
   * @param buysDialog busy dialog or null
   */
  public void newBranch(String name, CommitMessage commitMessage, BusyDialog busyDialog)
    throws RepositoryException
  {
  }

  /** post to review server/update review server
   */
  protected String postReview(String password, String reference, HashSet<FileData> fileDataSet, CommitMessage commitMessage, LinkedHashSet<String> testSet)
    throws RepositoryException
  {
    final Pattern PATTERN_REFERENCE = Pattern.compile("^review\\s+.*:\\s+.*/r/(\\d+.)/",Pattern.CASE_INSENSITIVE);

    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;
      Matcher matcher;
      int     exitCode;

      // post review for files
      command.clear();
      command.append(Settings.hgCommand,"lpostreview");
      if (reviewServerHost != null) command.append("--server",reviewServerHost);
      if (reviewServerLogin != null) command.append("--username",reviewServerLogin);
      if (password != null) command.append("--password",command.hidden(password));
      if (reviewServerRepository != null) command.append("--repoid",reviewServerRepository);
      if (!reference.isEmpty()) command.append("--existing",reference);
      command.append("--publish");
      command.append("--summary",commitMessage.getSummary());
      command.append("--description",commitMessage.getMessage());
      command.append("--tests",StringUtils.join(testSet,", "));
      if (reviewServerGroups != null) command.append("--target_groups",reviewServerGroups);
      if (reviewServerPersons != null) command.append("--target_people",reviewServerPersons);
//command.append("--debug");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));

      // execute
      exec = new Exec(rootPath,command);
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("stdout %s",line);
        if ((matcher = PATTERN_REFERENCE.matcher(line)).matches())
        {
          reference = matcher.group(1);
        }
      }
      while ((line = exec.getStderr()) != null)
      {
//Dprintf.dprintf("stderr %s",line);
      }
      exitCode = exec.done(); exec = null;
      if (exitCode != 0)
      {
        throw new RepositoryException("Cannot post patch to review server (exitcode: "+exitCode+")");
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

    return reference;
  }

  //-----------------------------------------------------------------------

  /** parse hg state string
   * @param string state string
   * @return state
   */
  private FileData.States parseState(String string)
  {
    if      (string.equalsIgnoreCase(" ")) return FileData.States.OK;
    else if (string.equalsIgnoreCase("C")) return FileData.States.OK;
    else if (string.equalsIgnoreCase("A")) return FileData.States.ADDED;
    else if (string.equalsIgnoreCase("R")) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("M")) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("!")) return FileData.States.CHECKOUT;
    else if (string.equalsIgnoreCase("I")) return FileData.States.UNKNOWN;
    else if (string.equalsIgnoreCase("?")) return FileData.States.UNKNOWN;
    else                                   return FileData.States.UNKNOWN;
  }

  /** get HG root path
   * @return root path
   */
  private String getHGRootPath()
    throws IOException,RepositoryException
  {
    String hgRootPath;

    Command command = new Command();
    Exec    exec;
    String  line;

    // get forest trees
    command.clear();
    command.append(Settings.hgCommand,"root");
    command.append("--");
    exec = new Exec(rootPath,command);

    // read
    hgRootPath = exec.getStdout();

    // done
    exec.done(); exec = null;

    return hgRootPath;
  }

  /** get sub-directory based on hg root path
   * @return sub-directory or ""
   */
  private String getSubDirectory()
    throws IOException,RepositoryException
  {
    // get hg root path
    String hgRootPath = getHGRootPath();

    // get sub-directory
    return ((rootPath.length() > hgRootPath.length()) && rootPath.startsWith(hgRootPath))
             ?rootPath.substring(hgRootPath.length()+1)
             :"";
  }

  /** get trees of hg forest
   * @return tree names
   */
  private String[] getTrees()
    throws IOException,RepositoryException
  {
    ArrayList<String> treeList = new ArrayList<String>();

    Command command = new Command();
    Exec    exec;
    String  line;

    // get forest trees
    command.clear();
    command.append(Settings.hgCommand,"ftree");
    command.append("--");
    exec = new Exec(rootPath,command);

    // parse
    while ((line = exec.getStdout()) != null)
    {
      treeList.add(line);
    }

    // done
    exec.done(); exec = null;

    return treeList.toArray(new String[treeList.size()]);
  }

// NYI: is the global parent map needed?
  private HashMap<Integer,ParentData> getParentMap()
  {
    HashMap<Integer,ParentData> parentMap = new HashMap<Integer,ParentData>();

    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;

      // get parents
      command.clear();
      command.append(Settings.hgCommand,"-y","log","--template","{rev} {parents}\\n");
      command.append("--");
      exec = new Exec(rootPath,command);

      // parse
      Object[] data = new Object[5];
      while ((line = exec.getStdout()) != null)
      {
        if      (StringParser.parse(line,"%d %d:%s %d:%s",data))
        {
          // node with two parents: merge
          int revision        = (Integer)data[0];
          int parentRevision1 = (Integer)data[1];
          int parentRevision2 = (Integer)data[3];

          parentMap.put(revision,new ParentData(parentRevision1,parentRevision2));
        }
        else if (StringParser.parse(line,"%d %d:%s",data))
        {
// NYI: what is a single parent? a commit?
          // node with single parent: commit?
          int revision        = (Integer)data[0];
          int parentRevision1 = (Integer)data[1];

          parentMap.put(revision,new ParentData(parentRevision1));
        }
        else
        {
          // nodes with no parents are ignored
        }
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      // ignored
    }

    return parentMap;
  }

// get parent revision data
  private RevisionData getParentRevisionData(int revision, HashMap<Integer,RevisionData> revisionDataMap, HashMap<RevisionData,ParentData> fileParentMap)
  {
    RevisionData revisionData = null;

    if ((revision != 0) && (parentMap != null))
    {
      synchronized(parentMap)
      {
      Stack<Integer> revisionStack = new Stack<Integer>();
      do
      {
        // check if revision exists
        revisionData = revisionDataMap.get(revision);

        if (revisionData == null)
        {
          // push parents for check
          ParentData parentData = fileParentMap.get(revision);
          if (parentData != null)
          {
            if (parentData.revision2 != 0) revisionStack.push(parentData.revision2);

            // get next revision to check
            revision = parentData.revision1;
          }
          else
          {
            revision = revision-1;
          }
        }
      }
      while ((revisionData == null) && (revision != 0));
      }
    }

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

  /** parse log header (currently nothing to do)
   * @param exec exec command
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec)
    throws IOException
  {
    return true;
  }

  /** parse log data
   * @param exec exec command
   * @return revision info or null
   */
  private RevisionDataHG parseLogData(Exec exec, HashMap<Integer,RevisionData> revisionDataMap, HashMap<RevisionData,ParentData> fileParentMap)
    throws IOException
  {
    final Pattern PATTERN_REVISION = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    RevisionDataHG          revisionData      = null;

    boolean                 dataDone          = false;
    Matcher                 matcher;
    String                  line;
    String                  revision          = null;
    String                  changeSet         = null;
    Date                    date              = null;
    String                  author            = null;
    ArrayList<RevisionData> parentList        = new ArrayList<RevisionData>();
    int                     parentRevision1   = 0;
    int                     parentRevision2   = 0;
    ArrayList<String>       tagList           = new ArrayList<String>();
    String                  branches          = null;
    LinkedList<String>      commitMessageList = new LinkedList<String>();
    while (   !dataDone
           && ((line = exec.getStdout()) != null)
          )
    {
//Dprintf.dprintf("line=%s",line);
      if      (line.startsWith("-----"))
      {
        dataDone = true;
      }
      else if ((matcher = PATTERN_REVISION.matcher(line)).matches())
      {
        // revision
        revision  = matcher.group(1);
        changeSet = matcher.group(2);
        date      = parseDate(matcher.group(3));
        author    = matcher.group(4);

        // get parents
        if ((line = exec.getStdout()) != null)
        {
          Object[] data = new Object[4];
          if      (StringParser.parse(line,"%d:%s %d:%s",data))
          {
            // node with two parents: merge
            parentRevision1 = (Integer)data[0];
            parentRevision2 = (Integer)data[2];
          }
          else if (StringParser.parse(line,"%d:%s",data))
          {
// NYI: what is a single parent? a commit?
            // node with single parent: commit?
            parentRevision1 = (Integer)data[0];
          }
        }

        // get branches
        if ((line = exec.getStdout()) != null)
        {
          branches = line;
        }

        // get tags
        if ((line = exec.getStdout()) != null)
        {
          for (String tag : StringUtils.split(line,StringUtils.WHITE_SPACES,false))
          {
            tagList.add(tag);
          }
        }

        // get commit message lines
        while (  ((line = exec.getStdout()) != null)
               && !line.startsWith("-----")
              )
        {
          commitMessageList.add(line);
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
        revisionData = new RevisionDataHG(revision,
                                          changeSet,
                                          tagList,
                                          date,
                                          author,
                                          commitMessageList
                                         );
        if (revisionDataMap != null) revisionDataMap.put(Integer.parseInt(revision),revisionData);
        if (fileParentMap != null) fileParentMap.put(revisionData,new ParentData(parentRevision1,parentRevision2));

        dataDone = true;
      }
    }
//Dprintf.dprintf("revisionData=%s",revisionData);

    return revisionData;
  }

  /** parse log data
   * @param exec exec command
   * @return revision info or null
   */
  private RevisionDataHG parseLogData(Exec exec)
    throws IOException
  {
    return parseLogData(exec,null,null);
  }
}

/* end of file */
