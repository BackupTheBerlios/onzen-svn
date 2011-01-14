/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryHG.java,v $
* $Revision: 1.1 $
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

/** Mercurial (hg) repository
 */
class RepositoryHG extends Repository
{
  // --------------------------- constants --------------------------------

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
  public void revert(HashSet<FileData> fileDataHashSet)
    throws RepositoryException
  {
  }

  /** 
   * @param 
   * @return 
   */
  public void diff(FileData fileData)
    throws RepositoryException
  {
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
