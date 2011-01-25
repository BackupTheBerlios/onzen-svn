/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositorySVN.java,v $
* $Revision: 1.2 $
* $Author: torsten $
* Contents: repository
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
//import java.io.FileReader;
//import java.io.BufferedReader;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;


/****************************** Classes ********************************/

/** Apache Subversion (SVN) repository
 */
class RepositorySVN extends Repository
{
  // --------------------------- constants --------------------------------

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

  /** 
   * @param 
   * @return 
   */
  public void updateStates(HashSet<FileData> fileDataHashSet, HashSet<String> fileDirectoryHashSet, boolean addNewFlag)
  {
  }

  /** 
   * @param 
   * @return 
   */
  public void update(HashSet<FileData> fileDataHashSet)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public String getLastRevision()
    throws RepositoryException
  {
    return "???";
  }

  /** 
   * @param 
   * @return 
   */
  public String[] getRevisions(FileData fileData)
    throws RepositoryException
  {
    return null;
  }

  /** 
   * @param 
   * @return 
   */
  public RevisionData[] getRevisionTree(FileData fileData)
    throws RepositoryException
  {
    return null;
  }

  /** 
   * @param 
   * @return 
   */
  public String[] getFile(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  public byte[] getFileData(FileData fileData, String revision)
    throws RepositoryException
  {
    return null;
  }

  /** 
   * @param 
   * @return 
   */
  public void commit(HashSet<FileData> fileDataHashSet, Message commitMessage)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public void add(HashSet<FileData> fileDataHashSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public void remove(HashSet<FileData> fileDataHashSet, Message commitMessage)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public void revert(HashSet<FileData> fileDataHashSet, String revision)
    throws RepositoryException
  {
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   */
  public void rename(FileData fileData, String newName, Message commitMessage)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public DiffData[] diff(FileData fileData, String revision1, String revision2)
    throws RepositoryException
  {
    return null;
  }

  /** 
   * @param 
   * @return 
   */
  public void patch(FileData fileData)
    throws RepositoryException
  {
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "{Repository "+rootPath+"}";
  }

  //-----------------------------------------------------------------------

}

/* end of file */
