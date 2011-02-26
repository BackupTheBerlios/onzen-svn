/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryGit.java,v $
* $Revision: 1.9 $
* $Author: torsten $
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

/****************************** Classes ********************************/

/** Git repository
 */
class RepositoryGit extends Repository
{
  // --------------------------- constants --------------------------------

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

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
  {
  }

  /** get last revision name
   * @return last revision name
   */
  public String getLastRevision()
  {
    return "???";
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
   * @param lineLine patch data lines or null
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
  public void commit(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException
  {
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(HashSet<FileData> fileDataSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, Message commitMessage)
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
  public void rename(FileData fileData, String newName, Message commitMessage)
    throws RepositoryException
  {
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

  //-----------------------------------------------------------------------

}

/* end of file */
