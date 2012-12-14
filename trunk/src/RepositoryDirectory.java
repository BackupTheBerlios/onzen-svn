/***********************************************************************\
*
* $Revision: 1040 $
* $Date: 2012-04-16 11:28:09 +0200 (Mo, 16. Apr 2012) $
* $Author: trupp $
* Contents: repository functions for "directory"
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

/** directory repository
 */
class RepositoryDirectory extends Repository
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
 private final static RepositoryDirectory staticInstance = new RepositoryDirectory();

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get static instance
   * @return static instance
   */
  public final static RepositoryDirectory getInstance()
  {
    return staticInstance;
  }

  /** create repository
   * @param rootPath root path
   */
  RepositoryDirectory(String rootPath)
  {
    super(rootPath);
  }

  /** create repository
   */
  RepositoryDirectory()
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

  /** create new repository module
   * @param repositoryPath repository server
   * @param moduleName module name
   * @param importPath import directory
   */
  public void create(String repositoryPath, String moduleName, String importPath)
    throws RepositoryException
  {
  }

  /** checkout repository from server
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
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
    throws RepositoryException
  {
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.DIRECTORY;
  }

  /** get repository path
   * @return repository path
   */
  public String getRepositoryPath()
  {
    return "";
  }

  /** get first revision name
   * @return first revision name
   */
  public String getFirstRevision()
  {
    return "";
  }

  /** get last revision name
   * @return last revision name
   */
  public String getLastRevision()
  {
    return "";
  }

  /** get revision names of file
   * @param name file name or URL
   * @return array with revision names
   */
  public String[] getRevisionNames(String name)
    throws RepositoryException
  {
    return new String[0];
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
    return new RevisionData[0];
  }

  /** get file data (text lines)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public String[] getFileLines(FileData fileData, String revision)
    throws RepositoryException
  {
    return new String[0];
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public byte[] getFileBytes(FileData fileData, String revision)
    throws RepositoryException
  {
    return new byte[0];
  }

  /** get all changed/unknown files
   * @param stateSet state set
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles(EnumSet<FileData.States> stateSet)
    throws RepositoryException
  {
    return new HashSet<FileData>();
  }

  /** get diff of file
   * @param fileData file data
   * @param revision1,revision2 revisions to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String oldRevision, String newRevision)
    throws RepositoryException
  {
    return new DiffData[0];
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
  }

  /** get patch data for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @return patch data bytes
   */
  public byte[] getPatchBytes(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
    return new byte[0];
  }

  /** get log to file
   * @param fileData file data
   * @return log array
   */
  public LogData[] getLog(FileData fileData)
    throws RepositoryException
  {
    return new LogData[0];
  }

  /** get annotations to file
   * @param fileData file data
   * @param revision revision
   * @return annotation array
   */
  public AnnotationData[] getAnnotations(FileData fileData, String revision)
    throws RepositoryException
  {
    return new AnnotationData[0];
  }

  /** update file from respository
   * @param fileDataSet file data set or null
   * @param busyDialog busy dialog or null
   */
  public void update(HashSet<FileData> fileDataSet, BusyDialog busyDialog)
    throws RepositoryException
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
   * @param recursive true for recursive revert, false otherwise
   */
  public void revert(HashSet<FileData> fileDataSet, String revision, boolean recursive)
    throws RepositoryException
  {
Dprintf.dprintf("");
throw new Error("NYI");
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  public void rename(FileData fileData, String newName, CommitMessage commitMessage)
    throws RepositoryException
  {
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
  }

  /** set conflicts resolved
   * @param fileDataSet file data set or null for all files
   */
  public void resolve(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
  }

  /** get incoming changes list
   * @param masterRepository master repository or null
   */
  public LogData[] getIncomingChanges(String masterRepository)
    throws RepositoryException
  {
    return null;
  }

  /** get outgoing changes list
   * @param masterRepository master repository or null
   */
  public LogData[] getOutgoingChanges(String masterRepository)
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
   * @param masterRepository master repository or null
   */
  public void pullChanges(String masterRepository)
    throws RepositoryException
  {
  }

  /** push changes
   * @param masterRepository master repository or null
   */
  public void pushChanges(String masterRepository)
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
  }

  /** get default name of root
   * @return always null (no root name)
   */
  public String getDefaultRootName()
  {
    return null;
  }

  /** get default branch name
   * @return default branch name
   */
  public String getDefaultBranchName()
  {
    return "";
  }

  /** get names of existing branches
   * @return array with branch names
   */
  public String[] getBranchNames(String pathName)
    throws RepositoryException
  {
    return new String[0];
  }

  /** create new branch
   * @param rootName root name (source)
   * @param branchName branch name
   * @param commitMessage commit message
   * @param buysDialog busy dialog or null
   */
  public void newBranch(String rootName, String branchName, CommitMessage commitMessage, BusyDialog busyDialog)
    throws RepositoryException
  {
  }

  //-----------------------------------------------------------------------

}

/* end of file */
