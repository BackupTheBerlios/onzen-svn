/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryHG.java,v $
* $Revision: 1.11 $
* $Author: torsten $
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;

/****************************** Classes ********************************/

/** Mercurial (hg) repository
 */
class RepositoryHG extends Repository
{
  /** HG revision data
   */
  class RevisionDataHG extends RevisionData
  {
    private String changeSet;

    /** create HG revision data
     * @param revision revision
     * @param date date
     * @param author author name
     * @param commitMessageList commit message
     */
    public RevisionDataHG(String revision, String changeSet, String symbolicName, Date date, String author, AbstractList<String> commitMessageList)
    {
      super(revision,symbolicName,date,author,commitMessageList);
      this.changeSet = changeSet;
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

  // --------------------------- constants --------------------------------
  private final String HG_COMMAND         = "hg";
  private final String LAST_REVISION_NAME = "tip";

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository
   * @param rootPath root path
   */
  RepositoryHG(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositoryHG()
  {
    this(null);
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.HG;
  }

  /** check if repository support patch queues
   * @return true
   */
  public boolean supportPatchQueues()
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
      try
      {
        // get status
        command.clear();
        command.append(HG_COMMAND,"-y","status","-mardcu");
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
        exec.done();

        // get revision (identity)
        command.clear();
        command.append(HG_COMMAND,"identify","-t");
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
        exec.done();

        // get branch
        command.clear();
        command.append(HG_COMMAND,"branch");
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
        exec.done();
      }
      catch (IOException exception)
      {
        // ignored
      }
    } 
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
    Command command = new Command(); 
    Exec    exec;                    
    String  line;                      
    Matcher matcher;                   
    try
    {
      // get log
      command.clear();
      command.append(HG_COMMAND,"-y","-v","log","--template","{rev} {node|short}\\n");
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }

    // convert to array and sort
    String[] revisions = revisionList.toArray(new String[revisionList.size()]);
    Arrays.sort(revisions,new Comparator<String>()
    {
      public int compare(String revision1, String revision2)
      {
        int number1 = Integer.parseInt(revision1.substring(1));
        int number2 = Integer.parseInt(revision2.substring(1));
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
    Command command = new Command(); 
    Exec    exec;                    
    try
    {
      // get single log entry
      command.clear();
      command.append(HG_COMMAND,"-y","-v","log","-l","1","--template","{rev} {node|short} {date|isodate} {author|user} {branches}\\n{desc}\\n-----\\n");
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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
    Command command = new Command(); 
    Exec    exec;                    
    try
    {
      // get log
      command.clear();
      command.append(HG_COMMAND,"-y","-v","log","--template","{rev} {node|short} {date|isodate} {author|user} {branches}\\n{desc}\\n-----\\n");
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
          // add revision info entry
          revisionDataList.add(revisionData);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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
    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;

      // get file
      command.clear();
      command.append(HG_COMMAND,"-y","cat");
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
      throw new RepositoryException(exception);
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
      byte[]  buffer  = new byte[4*1024];

      // get file data
      command.clear();
      command.append(HG_COMMAND,"-y","cat");
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
      throw new RepositoryException(exception);
    }

    // convert byte array stream into array
    return output.toByteArray();
  }

  /** get all changed files
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles()
    throws RepositoryException
  {
    final Pattern PATTERN_STATUS = Pattern.compile("^\\s*(.)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    try
    {
      Command         command            = new Command();
      Exec            exec;
      String          line;                  
      Matcher         matcher;
      String          name               = null;
      FileData.States state              = FileData.States.UNKNOWN;
      String          workingRevision    = "";
      String          repositoryRevision = "";
      String          author             = "";

      // get list of files which may be updated or which are locally changed
      command.clear();
      command.append(HG_COMMAND,"-y","status","-mardu");
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

          fileDataSet.add(new FileData(name,
                                       state,
                                       FileData.Modes.BINARY
                                      )
                         );
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for line '%s'",line);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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

    try
    {
      Command command = new Command();
      Exec    exec;
      Matcher matcher;
      String  line;

      String[] newFileLines = null;
      if (newRevision != null)
      {
        // check out new revision
        command.clear();
        command.append(HG_COMMAND,"-y","cat","-r",newRevision);
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
        try
        {
          // open file
          BufferedReader bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));

          // read content
          ArrayList<String> newFileLineList = new ArrayList<String>();
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
          throw new RepositoryException(exception);
        }
      }

      // diff file
      command.clear();
      command.append(HG_COMMAND,"-y","diff");
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
//Dprintf.dprintf("line=%s",line);
        if      ((matcher = PATTERN_DIFF.matcher(line)).matches())
        {
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

          while (   ((line = exec.getStdout()) != null)
                 && !line.startsWith("@@")
                )
          {
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
          exec.ungetStdout(line);
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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
   * @return patch data lines
   */
  public String[] getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
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

    // get revision info list
    Command command = new Command(); 
    Exec    exec;                    
    try
    {
      // get log
      command.clear();
      command.append(HG_COMMAND,"-y","-v","log","--template","{rev} {node|short} {date|isodate} {author|user} {branches}\\n{desc}\\n-----\\n");
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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

    try
    {
      Command command = new Command();
      Exec    exec;
      Matcher matcher;
      String  line;

      // get annotations
      command.clear();
      command.append(HG_COMMAND,"blame","-n","-u","-d");
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
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
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

      // unapply patches for all trees in forest
      for (String tree : getTrees())
      {
        command.clear();
        command.append(HG_COMMAND,"qpop","-a");
        command.append("--");
        exitCode = new Exec(tree,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }

      // push ???

      // update files
      command.clear();
      command.append(HG_COMMAND,Settings.hgForest?"fupdate":"update");
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
      throw new RepositoryException(exception);
    }
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void commit(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // commit files
      command.clear();
      command.append(HG_COMMAND,"commit");
      if (!commitMessage.isEmpty())
      {
        command.append("-F",commitMessage.getFileName());
      }
      else
      {
        command.append("-m","empty");
      }
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      immediatePush();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(HashSet<FileData> fileDataSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // add files
      command.clear();
      command.append(HG_COMMAND,"add");
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
        command.append(HG_COMMAND,"commit");
        if (!commitMessage.isEmpty())
        {
          command.append("-F",commitMessage.getFileName());
        }
        else
        {
          command.append("-m","empty");
        }
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
      throw new RepositoryException(exception);
    }
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, Message commitMessage)
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
      command.append(HG_COMMAND,"remove");
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
        command.append(HG_COMMAND,"commit");
        if (!commitMessage.isEmpty())
        {
          command.append("-F",commitMessage.getFileName());
        }
        else
        {
          command.append("-m","empty");
        }
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
      throw new RepositoryException(exception);
    }
  }

  /** revert files
   * @param fileDataSet file data set
   * @param revision revision to revert to
   */
  public void revert(HashSet<FileData> fileDataSet, String revision)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // revert files
      command.clear();
      command.append(HG_COMMAND,"revert","-r",revision);
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
      throw new RepositoryException(exception);
    }
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  public void rename(FileData fileData, String newName, Message commitMessage)
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // rename file
      command.clear();
      command.append(HG_COMMAND,"rename");
      command.append("--");
      command.append(fileData.getFileName());
      command.append(newName);
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }

      // commit
      if (commitMessage != null)
      {
        // commit remove/add (=rename) file
        command.clear();
        command.append(HG_COMMAND,"commit");
        if (!commitMessage.isEmpty())
        {
          command.append("-F",commitMessage.getFileName());
        }
        else
        {
          command.append("-m","empty");
        }
        command.append("--");
        command.append(fileData.getFileName());
        command.append(newName);
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, Message commitMessage)
    throws RepositoryException
  {
  }

  /** pull changes
   */
  public void pullChanges()
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      command.clear();
      command.append(HG_COMMAND,Settings.hgForest?"fpull":"pull");
      command.append("--");
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** push changes
   */
  public void pushChanges()
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      command.clear();
      command.append(HG_COMMAND,Settings.hgForest?"fpush":"push");
      command.append("--");
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** apply patches
   */
  public void applyPatches()
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // unapply patches for all trees in forest
      for (String tree : getTrees())
      {
        command.clear();
        command.append(HG_COMMAND,"qpush","-a");
        command.append("--");
        exitCode = new Exec(tree,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** unapply patches
   */
  public void unapplyPatches()
    throws RepositoryException
  {
    try
    {
      Command command = new Command();
      int     exitCode;

      // unapply patches for all trees in forest
      for (String tree : getTrees())
      {
        command.clear();
        command.append(HG_COMMAND,"qpop","-a");
        command.append("--");
        exitCode = new Exec(tree,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "{Repository "+rootPath+"}";
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

  /** get trees of hg forest
   * @return tree names
   */
  private String[] getTrees()
    throws RepositoryException
  {
    ArrayList<String> treeList = new ArrayList<String>();

    try
    {
      Command command = new Command();
      Exec    exec;
      String  line;

      // get forest trees
      command.clear();
      command.append(HG_COMMAND,"ftree");
      command.append("--");
      exec = new Exec(rootPath,command);

      // parse
      while ((line = exec.getStdout()) != null)
      {
        treeList.add(line);
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }

    return treeList.toArray(new String[treeList.size()]);
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

  /** parse log header
   * @param exec exec command
   * @param symbolicNamesMap symbolic names map to fill
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec)
    throws IOException
  {
    return true;
  }

  /** parse log data
   * @param exec exec command
   * @param symbolicNamesMap symbolic names map
   * @return revision info or null
   */
  private RevisionDataHG parseLogData(Exec exec)
    throws IOException
  {
    final Pattern PATTERN_REVISION = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+(\\S+)\\s+(.*)",Pattern.CASE_INSENSITIVE);

    RevisionDataHG     revisionData = null;

    boolean            dataDone      = false;
    Matcher            matcher;
    String             line;                  
    String             revision      = null;
    String             changeSet     = null;
    Date               date          = null;
    String             author        = null;
    String             branches      = null;
    LinkedList<String> commitMessage = new LinkedList<String>();
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
        branches  = matcher.group(4);

        // get commit message lines
        while (  ((line = exec.getStdout()) != null)
               && !line.startsWith("-----")
              )
        {
          commitMessage.add(line);
        }
        while (   (commitMessage.peekFirst() != null)
               && commitMessage.peekFirst().trim().isEmpty()
              )
        {
          commitMessage.removeFirst();
        }
        while (   (commitMessage.peekLast() != null)
               && commitMessage.peekLast().trim().isEmpty()
              )
        {
          commitMessage.removeLast();
        }

        // add log info entry
        revisionData = new RevisionDataHG(revision,
                                          changeSet,
                                          "",
                                          date,
                                          author,
                                          commitMessage
                                         );
        dataDone = true;
      }
    }
//Dprintf.dprintf("revisionData=%s",revisionData);

    return revisionData;
  }

  private void immediatePush()
    throws IOException,RepositoryException
  {
    if (Settings.hgImmediatePush)
    {
      Command command = new Command();
      int     exitCode;

      // push changes to master repository
      command.clear();
      command.append(HG_COMMAND,Settings.hgForest?"fpush":"push");
      command.append("--");
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail, exit code: %d",command.toString(),exitCode);
      }
    }
  }
}

/* end of file */
