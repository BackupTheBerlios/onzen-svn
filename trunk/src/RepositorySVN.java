/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositorySVN.java,v $
* $Revision: 1.7 $
* $Author: torsten $
* Contents: Apache Subversion (SVN) repository
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/****************************** Classes ********************************/

/** Apache Subversion (SVN) repository
 */
class RepositorySVN extends Repository
{
  /** revision info data
   */
  class RevisionInfo
  {
    final String   revision;
    final Date     date;
    final String   author;
    final String[] commitMessage;
    final String[] fileNames;

    /** create revision info
     * @param revision revision string
     * @param date date
     * @param author author name
     * @param commitMessage commit message lines
     * @param fileNameList file name list
     */
    RevisionInfo(String               revision,
                 Date                 date,
                 String               author,
                 AbstractList<String> commitMessage,
                 AbstractList<String> fileNameList
                )
    {
      this.revision      = revision;
      this.date          = date;
      this.author        = author;
      this.commitMessage = commitMessage.toArray(new String[commitMessage.size()]);
      this.fileNames     = fileNameList.toArray(new String[fileNameList.size()]);
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "Revision info {revision: "+revision+", date: "+date+", author: "+author+", message: "+commitMessage+"}";
    }
  }

  // --------------------------- constants --------------------------------
  private final String SVN_COMMAND        = "svn";
  private final String LAST_REVISION_NAME = "HEAD";

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository
   * @param rootPath root path
   */
  RepositorySVN(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositorySVN()
  {
    this(null);
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.SVN;
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectoryHashSet directory set to check for new/missing files
   * @param addNewFlag add missing files
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, boolean addNewFlag)
  {
    final Pattern PATTERN_STATUS         = Pattern.compile("^(.)......(.)\\s+(\\d+?)\\s+(\\d+?)\\s+(\\S+?)\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_UNKNOWN        = Pattern.compile("^\\?.......\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STATUS_AGAINST = Pattern.compile("^Status against revision:.*",Pattern.CASE_INSENSITIVE);

    Command         command            = new Command();
    Exec            exec;                      
    String          line;                      
    Matcher         matcher;                   
    FileData        fileData;                  
    String          name               = null;
    FileData.States state              = FileData.States.UNKNOWN;
    String          workingRevision    = "";
    String          repositoryRevision = "";
    String          author             = "";
    for (String directory : fileDirectorySet)
    {
      try
      {
        // get status
        command.clear();
        command.append(SVN_COMMAND,"status","-uvN");
        command.append("--");
        if (directory != null) command.append(directory);
        exec = new Exec(rootPath,command);

        // parse status data
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // match name, state
          if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
          {
            state              = parseState(matcher.group(1),matcher.group(2));
            workingRevision    = matcher.group(3);
            repositoryRevision = matcher.group(4);
            author             = matcher.group(5);
            name               = matcher.group(6);

            fileData = findFileData(fileDataSet,name);
            if      (fileData != null)
            {
              fileData.state              = state;
              fileData.mode               = FileData.Modes.BINARY;
              fileData.workingRevision    = workingRevision;
              fileData.repositoryRevision = repositoryRevision;
            }
            else if (addNewFlag)
            {
              // get file type, size, date/time
              File file = new File(rootPath,name);
              FileData.Types type     = getFileType(file);
              long           size     = file.length();
              long           datetime = file.lastModified();

              // create file data
              fileData = new FileData(name,
                                      FileData.Types.FILE,
                                      state,
                                      FileData.Modes.BINARY,
                                      size,
                                      datetime
                                     );
//???
Dprintf.dprintf("");
            }
          }
          else if ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
          {
            name = matcher.group(1);

            if (addNewFlag)
            {
              // get file type, size, date/time
              File file = new File(rootPath,name);
              FileData.Types type     = getFileType(file);
              long           size     = file.length();
              long           datetime = file.lastModified();

              // create file data
              fileData = new FileData(name,
                                      FileData.Types.FILE,
                                      state,
                                      FileData.Modes.BINARY,
                                      size,
                                      datetime
                                     );
//???
Dprintf.dprintf("");
            }
          }
          else if (PATTERN_STATUS_AGAINST.matcher(line).matches())
          {
          // ignore "Pattern status agaist:..."
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
        // ignored
Dprintf.dprintf("exception=%s",exception);
      }
    } 

/*
    for (FileData fileData : fileDataSet)
    {
      try
      {
        Exec exec = new Exec(rootPath,"svn status "+fileData.name);
        while ((line = exec.getNextLineStdout()) != null)
        {
Dprintf.dprintf("line=%s",line);
          if      ((matcher = filePattern1.matcher(line)).matches())
          {
Dprintf.dprintf("file 1");
            fileData.state = parseState(matcher.group(1));
          }
          else if ((matcher = filePattern2.matcher(line)).matches())
          {
Dprintf.dprintf("file 2 %s",matcher.group(2));
            fileData.state = parseState(matcher.group(2));
          }
        }
      }
      catch (IOException exception)
      {
        fileData.state = FileData.States.ERROR;
      }
Dprintf.dprintf("file=%s",fileData);
    }
    */
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
    final Pattern PATTERN_REVSION = Pattern.compile("^r(\\d+).*",Pattern.CASE_INSENSITIVE);

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
      command.append(SVN_COMMAND,"log","-r","HEAD:0","--verbose");
      command.append("--");
      command.append(fileData.getFileName());
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
      command.append(SVN_COMMAND,"log","-r",revision+":PREV","--verbose");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionInfo revisionInfo = parseLogData(exec);
        if (revisionInfo != null)
        {
          revisionData = new RevisionData(revisionInfo.revision,
                                          "",
                                          revisionInfo.date,
                                          revisionInfo.author,
                                          revisionInfo.commitMessage
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

    return revisionData;
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException
  {
    LinkedList<RevisionInfo> revisionInfoList = new LinkedList<RevisionInfo>();

    // get revision info list
    Command command = new Command(); 
    Exec    exec;                    
    try
    {
      // get log
      command.clear();
      command.append(SVN_COMMAND,"log","-r","HEAD:0","--verbose");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionInfo revisionInfo;
        while ((revisionInfo = parseLogData(exec)) != null)
        {
          // add log info entry
          revisionInfoList.add(revisionInfo);
        }
      }

      // done
      exec.done();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
//for (RevisionInfo revisionInfo : revisionInfoList) Dprintf.dprintf("revisionInfo=%s",revisionInfo);

    // create revision data list
    ArrayList<RevisionData> revisionDataList = new ArrayList<RevisionData>();
    for (RevisionInfo revisionInfo : revisionInfoList)
    {
       revisionDataList.add(new RevisionData(revisionInfo.revision,
                                             "",
                                             revisionInfo.date,
                                             revisionInfo.author,
                                             revisionInfo.commitMessage
                                            )
                           );
    }

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
      command.append(SVN_COMMAND,"cat");
      if (revision != null) command.append("--revision",revision);
      command.append("--");
      command.append(fileData.getFileName());
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
      command.append(SVN_COMMAND,"cat");
      if (revision != null) command.append("--revision",revision);
      command.append("--");
      command.append(fileData.getFileName());
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
    final Pattern PATTERN_STATUS         = Pattern.compile("^(.)......(.)\\s+(\\d+?)\\s+(\\d+?)\\s+(\\S+?)\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_UNKNOWN        = Pattern.compile("^\\?.......\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STATUS_AGAINST = Pattern.compile("^Status against revision:.*",Pattern.CASE_INSENSITIVE);

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
      command.append(SVN_COMMAND,"status","-uv");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read list
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
        {
          state              = parseState(matcher.group(1),matcher.group(2));
          workingRevision    = matcher.group(3);
          repositoryRevision = matcher.group(4);
          author             = matcher.group(5);
          name               = matcher.group(6);

          fileDataSet.add(new FileData(name,
                                       state,
                                       FileData.Modes.BINARY
                                      )
                         );
        }
        else if ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
        {
          name = matcher.group(1);

          fileDataSet.add(new FileData(name,FileData.States.UNKNOWN));
        }
        else if (PATTERN_STATUS_AGAINST.matcher(line).matches())
        {
          // ignore "Pattern status agaist:..."
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
        command.append(SVN_COMMAND,"cat","--revision",newRevision);
        command.append("--");
        command.append(fileData.getFileName());
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
      command.append(SVN_COMMAND,"diff","--revision",((oldRevision != null) ? oldRevision : getLastRevision())+((newRevision != null) ? ":"+newRevision : ""));
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // skip diff header
      while ((line = exec.getStdout()) != null)
      {
        if (PATTERN_DIFF_START.matcher(line).matches()) break;
      }

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
if (d.blockType==DiffData.Types.CHANGED) for (int z = 0; z < d.deletedLines.length; z++) Dprintf.dprintf("%s -----> %s",d.deletedLines[z],d.addedLines[z]);
if (d.blockType==DiffData.Types.KEEP) lineNb += d.keepLines.length;
if (d.blockType==DiffData.Types.ADDED) lineNb += d.addedLines.length;
}
*/

    return diffDataList.toArray(new DiffData[diffDataList.size()]);
  }

  /** get patch for file
   * @param fileData file data
   * @return patch data
   */
  public void getPatch(FileData fileData)
    throws RepositoryException
  {
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
    HashMap<String,String> symbolicNamesMap = new HashMap<String,String>();
    Command                command = new Command(); 
    Exec                   exec;                    
    try
    {
      // get log
      command.clear();
      command.append(SVN_COMMAND,"log","-r","HEAD:0","--verbose");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionInfo revisionInfo;
        while ((revisionInfo = parseLogData(exec)) != null)
        {
          // add log info entry
          logDataList.add(new LogData(revisionInfo.revision,
                                      revisionInfo.date,
                                      revisionInfo.author,
                                      revisionInfo.commitMessage
                                     )
                         );
        }
      }

      // done
      exec.done();
    }
    catch (Exception exception)
    {
Dprintf.dprintf("xxxxxxxxxxxxxxxxxx");
      throw new RepositoryException(exception);
    }
//for (RevisionInfo revisionInfo : revisionInfoList) Dprintf.dprintf("revisionInfo=%s",revisionInfo);

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
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^\\s*(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+\\([^\\)]*\\)\\s(.*?)\\s*",Pattern.CASE_INSENSITIVE);
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
      command.append(SVN_COMMAND,"blame","-v");
      if (revision != null) command.append("-r",revision);
      command.append("--");
      command.append(fileData.getFileName());
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

      // update files
      command.clear();
      command.append(SVN_COMMAND,"update","-N");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
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
      command.append(SVN_COMMAND,"commit","-F",commitMessage.getFileName());
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
      }
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
      command.append(SVN_COMMAND,"add");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
      }

      if (commitMessage != null)
      {
        // commit added files
        command.clear();
        command.append(SVN_COMMAND,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
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
      command.append(SVN_COMMAND,"remove");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
      }

      if (commitMessage != null)
      {
        // commit removed files
        command.clear();
        command.append(SVN_COMMAND,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
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
      command.append(SVN_COMMAND,"update","-r",revision);
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
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

      // copy file
      command.clear();
      command.append(SVN_COMMAND,"copy");
      command.append("--");
      command.append(getFileDataName(fileData));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
      }

      // commit
      if (commitMessage != null)
      {
        // commit remove/add (=rename) file
        command.clear();
        command.append(SVN_COMMAND,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataName(fileData));
        command.append((!rootPath.isEmpty()) ? rootPath+File.separator+newName : newName);
        exitCode = new Exec(rootPath,command).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
        }
      }

      // remove old file
      command.clear();
      command.append(SVN_COMMAND,"delete");
      command.append("--");
      command.append(getFileDataName(fileData));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
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

  /** parse SVN state string
   * @param string state string
   * @return state
   */
  private FileData.States parseState(String stateFlag, String updateFlag)
  {
    if      (stateFlag.equalsIgnoreCase(" "))
    {
      if (updateFlag.equalsIgnoreCase("*"))
      {
        return FileData.States.UPDATE;
      }
      else
      {
        return FileData.States.OK;
      }
    }
    else if (stateFlag.equalsIgnoreCase("A")) return FileData.States.ADDED;
    else if (stateFlag.equalsIgnoreCase("C")) return FileData.States.CONFLICT;
    else if (stateFlag.equalsIgnoreCase("D")) return FileData.States.REMOVED;
    else if (stateFlag.equalsIgnoreCase("M"))
    {
      if (updateFlag.equalsIgnoreCase("*"))
      {
        return FileData.States.MERGE;
      }
      else
      {
        return FileData.States.MODIFIED;
      }
    }
    else if (stateFlag.equalsIgnoreCase("!")) return FileData.States.CHECKOUT;
    else                                      return FileData.States.UNKNOWN;
  }

  /** parse SVN diff index
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
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec)
    throws IOException
  {
    // parse header
    boolean headerDone = false;
    String  line;
    while (   !headerDone
           && ((line = exec.getStdout()) != null)
          )
    {
      if (line.startsWith("-----"))
      {
        headerDone = true;
      }
    }

    return headerDone;
  }

  /** parse log data
   * @param exec exec command
   * @param symbolicNamesMap symbolic names map
   * @return revision info or null
   */
  private RevisionInfo parseLogData(Exec exec)
    throws IOException
//    throws RepositoryException

  {
    final Pattern PATTERN_REVISION = Pattern.compile("^r(\\d+)\\s*\\|\\s*(\\S*)\\s*\\|\\s*(\\S*\\s+\\S*\\s+\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE     = Pattern.compile("^\\s*?\\s+(.*)\\s*",Pattern.CASE_INSENSITIVE);

    RevisionInfo       revisionInfo = null;

    boolean            dataDone      = false;
    Matcher            matcher;
    String             line;                  
    String             revision      = null;
    Date               date          = null;
    String             author        = null;
    ArrayList<String>  fileNameList  = new ArrayList<String>();
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
        revision = matcher.group(1);
        author   = matcher.group(2);
        date     = parseDate(matcher.group(3));

        // skip line "Changed paths:"
        exec.getStdout();

        // get files
        while (   ((line = exec.getStdout()) != null)
               && (matcher = PATTERN_FILE.matcher(line)).matches()
              )
        {
          fileNameList.add(matcher.group(1));
        }

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
        revisionInfo = new RevisionInfo(revision,
                                        date,
                                        author,
                                        commitMessage,
                                        fileNameList
                                       );
        dataDone = true;
      }
    }
//Dprintf.dprintf("revisionInfo=%s",revisionInfo);

    return revisionInfo;
  }
}

/* end of file */
