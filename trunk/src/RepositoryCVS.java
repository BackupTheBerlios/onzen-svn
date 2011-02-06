/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryCVS.java,v $
* $Revision: 1.7 $
* $Author: torsten $
* Contents: CVS repository functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
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
  /** revision info data
   */
  class RevisionInfo
  {
    final String   revision;
    final int[]    revisionNumbers;
    final String   symbolicName;
    final Date     date;
    final String   author;
    final String[] commitMessage;
    final String[] branchRevisions;

    /** create revision info
     * @param revision revision string
     * @param revisionNumbers revision numbers
     * @param symbolicName symbolic name of revision
     * @param date date
     * @param author author name
     * @param branchRevisions branch revisions
     * @param commitMessage commit message lines
     */
    RevisionInfo(String                revision,
                 AbstractList<Integer> revisionNumbers,
                 String                symbolicName,
                 Date                  date,
                 String                author,
                 AbstractList<String>  branchRevisions,
                 AbstractList<String>  commitMessage
                )
    {
      this.revision        = revision;
      this.revisionNumbers = new int[revisionNumbers.size()];
      for (int z = 0; z < revisionNumbers.size(); z++)
      {
        this.revisionNumbers[z] = revisionNumbers.get(z).intValue();
      }
      this.symbolicName    = symbolicName;
      this.date            = date;
      this.author          = author;
      this.branchRevisions = branchRevisions.toArray(new String[branchRevisions.size()]);
      this.commitMessage   = commitMessage.toArray(new String[commitMessage.size()]);
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "Revision info {revision: "+revision+", name: "+symbolicName+", date: "+date+", author: "+author+", message: "+commitMessage+"}";
    }
  }

  // --------------------------- constants --------------------------------
  private final String CVS_COMMAND        = "cvs";
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

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.CVS;
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectoryHashSet directory set to check for new/missing files
   * @param addNewFlag add missing files
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, boolean addNewFlag)
  {
    final Pattern PATTERN_COMPLETE1           = Pattern.compile("^\\s*File:.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_COMPLETE2           = Pattern.compile("^\\?\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE_STATUS1        = Pattern.compile("^\\s*File:\\s*no\\s+file\\s+(.*?)\\s+Status:\\s*(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE_STATUS2        = Pattern.compile("^\\s*File:\\s*(.*?)\\s+Status:\\s*(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_WORKING_REVISION1   = Pattern.compile("^.*Working revision:\\s*no.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_WORKING_REVISION2   = Pattern.compile("^.*Working revision:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_REPOSITORY_REVISION = Pattern.compile("^.*Repository revision:\\s*(\\S*).*",Pattern.CASE_INSENSITIVE);
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
        command.append(CVS_COMMAND,"status");
        command.append("--");
        if (directory != null) command.append(directory);
        exec = new Exec(rootPath,command);

        // parse status data
        while ((line = exec.getStdout()) != null)
        {
//Dprintf.dprintf("line=%s",line);
          // check if one entry is complete
          if (   (   PATTERN_COMPLETE1.matcher(line).matches()
                  || PATTERN_COMPLETE2.matcher(line).matches()
//                  || exec.eof()
                 )
              && (baseName != null)
             )
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
            else if (addNewFlag)
            {
              // get file type, size, date/time
              File file = new File(rootPath,baseName);
              FileData.Types type     = getFileType(file);
              long           size     = file.length();
              long           datetime = file.lastModified();

              // create file data
              fileData = new FileData(directory+File.separator+baseName,
                                      type,
                                      state,
                                      mode,
                                      size,
                                      datetime
                                     );
//???
//Dprintf.dprintf("");
            }

            baseName           = null;
            state              = FileData.States.UNKNOWN;
            mode               = FileData.Modes.UNKNOWN;
            workingRevision    = "";
            repositoryRevision = "";
            branch             = "";
          }

          // match name, state
          if      ((matcher = PATTERN_FILE_STATUS1.matcher(line)).matches())
          {
            state = parseState(matcher.group(1));
          }
          else if ((matcher = PATTERN_FILE_STATUS2.matcher(line)).matches())
          {
            baseName = matcher.group(1);
            state    = parseState(matcher.group(2));
          }

          // match working revision
          if      ((matcher = PATTERN_WORKING_REVISION1.matcher(line)).matches())
          {
            workingRevision = "";
          }
          else if ((matcher = PATTERN_WORKING_REVISION2.matcher(line)).matches())
          {
            workingRevision = matcher.group(1);
          }

          // match repository revision
          if ((matcher = PATTERN_REPOSITORY_REVISION.matcher(line)).matches())
          {
            repositoryRevision = matcher.group(1);
          }

          // match sticky tag/date (branch)
          if      ((matcher = PATTERN_STICKY_TAG.matcher(line)).matches())
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
            else
            {
              branch = "";
            }
          }

          // match sticky options
          if ((matcher = PATTERN_STICKY_OPTIONS.matcher(line)).matches())
          {
            if      (matcher.group(1).equals("-kb" )) mode = FileData.Modes.BINARY;
            else if (matcher.group(1).equals("-ko" )) mode = FileData.Modes.BINARY;
            else if (matcher.group(1).equals("-kvv")) mode = FileData.Modes.TEXT;
            else                                      mode = FileData.Modes.TEXT;
          }

          // match tags
          if ((matcher = PATTERN_EXISTING_TAGS.matcher(line)).matches())
          {
            tags.clear();
          }
        }

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
          else if (addNewFlag)
          {
            // get file type, size, date/time
            File file = new File(rootPath,baseName);
            FileData.Types type     = getFileType(file);
            long           size     = file.length();
            long           datetime = file.lastModified();

            // create file data
            fileData = new FileData(directory+File.separator+baseName,
                                    type,
                                    state,
                                    mode,
                                    size,
                                    datetime
                                   );
//???
Dprintf.dprintf("");
          }
        }
      }
      catch (IOException exception)
      {
        // ignored
      }
    } 

/*
    for (FileData fileData : fileDataSet)
    {
      try
      {
        Exec exec = new Exec(rootPath,"cvs status "+fileData.name);
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
    ArrayList<String> revisionList = new ArrayList<String>();

    // get revision info list
    HashMap<String,String> symbolicNamesMap = new HashMap<String,String>();
    Command                command          = new Command(); 
    Exec                   exec;                    
    try
    {
      // get log
      command.clear();
      command.append(CVS_COMMAND,"log");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,symbolicNamesMap))
      {
        // parse data
        RevisionInfo revisionInfo;
        while ((revisionInfo = parseLogData(exec,symbolicNamesMap)) != null)
        {
          // add log info entry
          revisionList.add(revisionInfo.revision);
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
    RevisionData revisionData = null;

    // get revision data
    Command                command = new Command(); 
    Exec                   exec;                    
    HashMap<String,String> symbolicNamesMap = new HashMap<String,String>();
    try
    {
      // get single log entry
      command.clear();
      command.append(CVS_COMMAND,"log","-r",revision);
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,symbolicNamesMap))
      {
        // parse data
        RevisionInfo revisionInfo = parseLogData(exec,symbolicNamesMap);
        if (revisionInfo != null)
        {
          revisionData = new RevisionData(revisionInfo.revision,
                                          symbolicNamesMap.get(revisionInfo.revision),
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
    HashMap<String,String> symbolicNamesMap = new HashMap<String,String>();
    Command                command = new Command(); 
    Exec                   exec;                    
    try
    {
      // get log
      command.clear();
      command.append(CVS_COMMAND,"log");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,symbolicNamesMap))
      {
        // parse data
        RevisionInfo revisionInfo;
        while ((revisionInfo = parseLogData(exec,symbolicNamesMap)) != null)
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

    // create revision tree
    return createRevisionTree(revisionInfoList,symbolicNamesMap);
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
      command.append(CVS_COMMAND,"up","-p");
      if (revision != null) command.append("-r",revision);
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
      command.append(CVS_COMMAND,"up","-p");
      if (revision != null) command.append("-r",revision);
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
      Matcher matcher;
      String  line;

      // get list of files which may be updated or which are locally changed
      command.clear();
      command.append(CVS_COMMAND,"-n","-q","update","-d");
      command.append("--");
      exec = new Exec(rootPath,command);

      // read list
      boolean mergeFlag = false;
      while ((line = exec.getStdout()) != null)
      {
        if      ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
        {
          fileDataSet.add(new FileData(matcher.group(1),FileData.States.UNKNOWN));
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_UPDATE.matcher(line)).matches())
        {
          fileDataSet.add(new FileData(matcher.group(1),FileData.States.UPDATE));
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_MODIFIED.matcher(line)).matches())
        {
          fileDataSet.add(new FileData(matcher.group(1),FileData.States.MODIFIED));
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
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.MERGE));
          }
          else
          {
            fileDataSet.add(new FileData(matcher.group(1),FileData.States.CONFLICT));
          }
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_ADDED.matcher(line)).matches())
        {
          fileDataSet.add(new FileData(matcher.group(1),FileData.States.ADDED));
          mergeFlag = false;
        }
        else if ((matcher = PATTERN_REMOVED.matcher(line)).matches())
        {
          fileDataSet.add(new FileData(matcher.group(1),FileData.States.REMOVED));
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
      throw new RepositoryException(exception);
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
      Matcher matcher;
      String  line;

      String[] newFileLines = null;
      if (newRevision != null)
      {
        // check out new revision
        command.clear();
        command.append(CVS_COMMAND,"up","-p","-r",newRevision);
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
      command.append(CVS_COMMAND,"diff");
      if (oldRevision != null) command.append("-r",oldRevision);
      if (newRevision != null) command.append("-r",newRevision);
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
      throw new RepositoryException(exception);
    }

    return diffDataList.toArray(new DiffData[diffDataList.size()]);
  }

  /** 
   * @param 
   * @return 
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
    Command                command          = new Command(); 
    Exec                   exec;                    
    try
    {
      // get log
      command.clear();
      command.append(CVS_COMMAND,"log");
      command.append("--");
      command.append(fileData.getFileName());
      exec = new Exec(rootPath,command);

      // parse header
      if (parseLogHeader(exec,symbolicNamesMap))
      {
        // parse data
        RevisionInfo revisionInfo;
        while ((revisionInfo = parseLogData(exec,symbolicNamesMap)) != null)
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
    catch (IOException exception)
    {
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
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^\\s*([\\.\\d]+)\\s+\\((\\S+)\\s+(\\S+)\\):\\s(.*)\\s*",Pattern.CASE_INSENSITIVE);
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
      command.append(CVS_COMMAND,"annotate");
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
Dprintf.dprintf("unknown %s",line);
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
      command.append(CVS_COMMAND,"update","-d");
      if (Settings.cvsPruneEmtpyDirectories) command.append("-P");
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
      command.append(CVS_COMMAND,"commit","-F",commitMessage.getFileName());
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
      command.append(CVS_COMMAND,"add");
      if (binaryFlag) command.append("-k","b");
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
        command.append(CVS_COMMAND,"commit","-F",commitMessage.getFileName());
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
      command.append(CVS_COMMAND,"remove");
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
        command.append(CVS_COMMAND,"commit","-F",commitMessage.getFileName());
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
      command.append(CVS_COMMAND,"update","-r",revision);
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

      // rename local file
      File oldFile = new File(rootPath,fileData.getFileName());
      File newFile = new File(rootPath,newName);
      if (!newFile.exists())
      {
        if (!oldFile.renameTo(newFile))
        {
          throw new RepositoryException("Cannot rename file '%s' to '%s'",oldFile.getName(),newFile.getName());
        }
      }
      else
      {
        throw new RepositoryException("File '%s' already exists",newFile.getName());
      }

      // add new file
      command.clear();
      command.append(CVS_COMMAND,"add");
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
      command.append(getFileDataName(fileData));
      exitCode = new Exec(rootPath,command).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("Command fail:\n\n%s\n\n(exit code: %d)",command.toString(),exitCode);
      }

      // remove old file
      command.clear();
      command.append(CVS_COMMAND,"remove");
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
        command.append(CVS_COMMAND,"commit","-F",commitMessage.getFileName());
        command.append("--");
        command.append(getFileDataName(fileData));
        command.append((!rootPath.isEmpty()) ? rootPath+File.separator+newName : newName);
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

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Repository {type: CVS, root: "+rootPath+"}";
  }

  //-----------------------------------------------------------------------

  /** parse CVS state string
   * @param string state string
   * @return state
   */
  private FileData.States parseState(String string)
  {
    if      (string.equalsIgnoreCase("Up-to-date"      )) return FileData.States.OK;
    else if (string.equalsIgnoreCase("Unknown"         )) return FileData.States.UNKNOWN;
    else if (string.equalsIgnoreCase("Update"          )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs patch"     )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs update"    )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs Checkout"  )) return FileData.States.CHECKOUT;
    else if (string.equalsIgnoreCase("Locally Modified")) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("Needs Merge"     )) return FileData.States.MERGE;
    else if (string.equalsIgnoreCase("Conflict"        )) return FileData.States.CONFLICT;
    else if (string.equalsIgnoreCase("Locally Added"   )) return FileData.States.ADDED;
    else if (string.equalsIgnoreCase("Entry invalid"   )) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("Removed"         )) return FileData.States.REMOVED;
    else                                                  return FileData.States.UNKNOWN;
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
   * @param symbolicNamesMap symbolic names map to fill
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec, HashMap<String,String> symbolicNamesMap)
    throws IOException
  {
    final Pattern PATTERN_HEAD           = Pattern.compile("^\\s*head:\\s*(.*)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_SYMBOLIC_NAMES = Pattern.compile("^\\s*symbolic names:.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_SYMBOLIC_NAME  = Pattern.compile("^\\s+(.*)\\s*:\\s*(.*)\\s*",Pattern.CASE_INSENSITIVE);

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
        // get head revision
        symbolicNamesMap.put(matcher.group(1),LAST_REVISION_NAME);
      }
      else if (PATTERN_SYMBOLIC_NAMES.matcher(line).matches())
      {
        // get symbolic tag/branch names
        while (   ((line = exec.getStdout()) != null)
               && (matcher = PATTERN_SYMBOLIC_NAME.matcher(line)).matches()
              )
        {
          symbolicNamesMap.put(matcher.group(2),matcher.group(1));
        }
        exec.ungetStdout(line);
//for (String s : symbolicNamesMap.keySet()) Dprintf.dprintf("symbol %s=%s",s,symbolicNamesMap.get(s));
      }
    }

    return headerDone;
  }

  /** parse log data
   * @param exec exec command
   * @param symbolicNamesMap symbolic names map
   * @return revision info or null
   */
  private RevisionInfo parseLogData(Exec exec, HashMap<String,String> symbolicNamesMap)
    throws IOException
  {
    final Pattern PATTERN_REVISION       = Pattern.compile("^\\s*revision\\s+(\\S+).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DATE_AUTHOR    = Pattern.compile("^\\s*date:\\s+([-\\d/]*\\s+[\\d: +]*);\\s*author:\\s*(\\S*);.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_BRANCES        = Pattern.compile("^\\s*branches:\\s*(.*).*",Pattern.CASE_INSENSITIVE);

    RevisionInfo       revisionInfo = null;

    boolean            dataDone        = false;
    Matcher            matcher;
    String             line;                    
    String             revision        = null;
    ArrayList<Integer> revisionNumbers = new ArrayList<Integer>();
    Date               date            = null;
    String             author          = null;
    ArrayList<String>  branchRevisions = new ArrayList<String>();
    LinkedList<String> commitMessage   = new LinkedList<String>();
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
          revisionNumbers.add(Integer.parseInt(string));
        }
      }
      else if ((matcher = PATTERN_DATE_AUTHOR.matcher(line)).matches())
      {
        // date, author
        date   = parseDate(matcher.group(1));
        author = matcher.group(2);

        // get branch names (if any)
        branchRevisions.clear();
        if ((line = exec.getStdout()) != null)
        {
          if ((matcher = PATTERN_BRANCES.matcher(line)).matches())
          {
            for (String branchName : matcher.group(1).split(";"))
            {
              branchRevisions.add(branchName.trim());
            }
          }
          else
          {
            exec.ungetStdout(line);
          }
        }

        // get commit message lines
        commitMessage.clear();
        while (  ((line = exec.getStdout()) != null)
               && !line.startsWith("-----")
               && !line.startsWith("=====")
              )
        {
          if (!line.equals("*** empty log message ***"))
          {
            commitMessage.add(line);
          }
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
                                        revisionNumbers,
                                        symbolicNamesMap.get(revision),
                                        date,
                                        author,
                                        branchRevisions,
                                        commitMessage
                                       );
        dataDone = true;
      }
    }

    return revisionInfo;
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

  /** create revision tree
   * @param revisionInfoList revision info list
   * @param symbolicNamesMap symbolic tag/branch names
   * @param branchesMap branches
   * @param branchRevisionNumbers branch revision numbers
   * @return revision tree
   */
  private RevisionData[] createRevisionTree(LinkedList<RevisionInfo>     revisionInfoList,
                                            HashMap<String,String>       symbolicNamesMap,
                                            HashMap<String,RevisionData> branchesMap,
                                            int[]                        branchRevisionNumbers
                                           )
  {
    LinkedList<RevisionData> revisionDataList = new LinkedList<RevisionData>();

    boolean  branchDone = false;  
    while (!revisionInfoList.isEmpty() && !branchDone)
    {
      RevisionInfo revisionInfo = revisionInfoList.getFirst();
//Dprintf.dprintf("revisionInfo=%s -- %d %d",revisionInfo,revisionInfo.revisionNumbers.length,branchRevisionNumbers.length);

      // get branch id this revision belongs to
      String branchId = getId(revisionInfo.revisionNumbers,-1);

      // add revision
      if      (revisionInfo.revisionNumbers.length-2 > branchRevisionNumbers.length)
      {
        // create branch sub-tree
        RevisionData[] subRevisionTree = createRevisionTree(revisionInfoList,
                                                            symbolicNamesMap,
                                                            branchesMap,
                                                            getSubNumbers(revisionInfo.revisionNumbers,-2)
                                                           );

        // find revision data to add/create revision data
        RevisionData branchRevisionData = branchesMap.get(branchId);
        if (branchRevisionData == null)
        {
Dprintf.dprintf("xxxxxxxxxxxxxxxxx %s",branchId);
          branchRevisionData = new RevisionData(revisionInfo.revision,
                                                symbolicNamesMap.get(revisionInfo.revision),
                                                revisionInfo.date,
                                                revisionInfo.author,
                                                revisionInfo.commitMessage
                                               );
          revisionDataList.add(branchRevisionData);

          // store branches of this revision
          branchesMap.put(branchId,branchRevisionData);
          if (revisionInfo.branchRevisions != null)
          {
            for (String branchRevision : revisionInfo.branchRevisions)
            {
              branchesMap.put(branchRevision,branchRevisionData);
            }
          }
        }

        // add branch
        String branchName = symbolicNamesMap.get(branchId);
        branchRevisionData.addBranch((branchName != null) ? branchName : branchId,
                                     revisionInfo.date,
                                     revisionInfo.author,
                                     revisionInfo.commitMessage,
                                     subRevisionTree
                                    );
      }
      else if (revisionInfo.revisionNumbers.length-2 == branchRevisionNumbers.length)
      {
        // add revision
        RevisionData revisionData = new RevisionData(revisionInfo.revision,
                                                     symbolicNamesMap.get(revisionInfo.revision),
                                                     revisionInfo.date,
                                                     revisionInfo.author,
                                                     revisionInfo.commitMessage
                                                    );
        revisionDataList.addLast(revisionData);

        // store branches of this revision
        if (revisionInfo.branchRevisions != null)
        {
          for (String branchRevision : revisionInfo.branchRevisions)
          {
            branchesMap.put(branchRevision,revisionData);
          }
        }

        // done this revision
        revisionInfoList.removeFirst();
      }
      else
      {
        // branch is complete
        branchDone = true;
      }
    }

    return revisionDataList.toArray(new RevisionData[revisionDataList.size()]);
  }

  /** create revision tree
   * @param revisionInfoList revision info list
   * @param symbolicNamesMap symbolic tag/branch names
   * @return revision tree
   */
  private RevisionData[] createRevisionTree(LinkedList<RevisionInfo> revisionInfoList,
                                            HashMap<String,String>   symbolicNamesMap
                                           )
  {
    return createRevisionTree(revisionInfoList,
                              symbolicNamesMap,
                              new HashMap<String,RevisionData>(),
                              new int[]{}
                             );
  }
}

/* end of file */
