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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

/****************************** Classes ********************************/

/** Git repository
 */
class RepositoryGit extends Repository
{
  // --------------------------- constants --------------------------------
  private final String LAST_REVISION_NAME = "HEAD";

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create repository
   * @param rootPath root path
   */
  RepositoryGit(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositoryGit()
  {
    this(null);
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.GIT;
  }

  /** checkout repository from server
   * @param repositoryPath repository server
   * @param rootPath root path
   */
  public void checkout(String repositoryPath, String rootPath)
    throws RepositoryException
  {
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
  {
    final Pattern PATTERN_STATUS = Pattern.compile("^\\s*(\\S+)\\s+(.*?)\\s*",Pattern.CASE_INSENSITIVE);

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
        command.append(Settings.gitCommand,"status","-s","--porcelain");
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
        command.append(Settings.gitCommand,"identify","-t");
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
        command.append(Settings.gitCommand,"branch");
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
    return null;
  }

  /** get revision data
   * @param fileData file data
   * @param revision revision
   * @return revision data
   */
  public RevisionData getRevisionData(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException
  {
    return null;
  }

  /** get file data (text lines)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public String[] getFileLines(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public byte[] getFileBytes(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** get all changed/unknown files
   * @param stateSet state set
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles(EnumSet<FileData.States> stateSet)
    throws RepositoryException
  {
    return null;
  }

  /** get diff of file
   * @param fileData file data
   * @param revision revision to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String oldRevision, String newRevision)
    throws RepositoryException
  {
    return null;
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
  }

  /** get patch data for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @return patch data bytes
   */
  public byte[] getPatchBytes(HashSet<FileData> fileDataSet, String revision1, String revision2)
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
    return null;
  }

  /** get annotations to file
   * @param fileData file data
   * @param revision revision
   * @return annotation array
   */
  public AnnotationData[] getAnnotations(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** update file from respository
   * @param fileDataSet file data set
   */
  public void update(HashSet<FileData> fileDataSet)
  {
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void commit(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(HashSet<FileData> fileDataSet, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
  }

  /** revert files
   * @param fileDataSet file data set or null for all files
   * @param revision revision to revert to
   */
  public void revert(HashSet<FileData> fileDataSet, String revision)
    throws RepositoryException
  {
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  public void rename(FileData fileData, String newName, CommitMessage commitMessage)
    throws RepositoryException
  {
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
  }

  //-----------------------------------------------------------------------

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

}

/* end of file */
