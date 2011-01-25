/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Repository.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: repository
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.Serializable;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
//import java.io.PrintWriter;
//import java.io.FileWriter;
//import java.io.BufferedReader;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Serializable;

//import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
//import java.util.ListIterator;
//import java.util.StringTokenizer;

import javax.activation.MimetypesFileTypeMap;

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

/** file data
 */
class FileData
{
  // --------------------------- constants --------------------------------

  /** file types
   */
  enum Types
  {
    NONE,

    FILE,
    DIRECTORY,
    LINK,
    DEVICE,
    SPECIAL,
    FIFO,
    SOCKET,

    UNKNOWN
  };

  /** file states
   */
  enum States
  {
    NONE,

    OK,
    MODIFIED,
    MERGE,
    CONFLICT,
    REMOVED,
    UPDATE,
    CHECKOUT,
    UNKNOWN,
    NOT_EXISTS,
    WAITING,
    ADDED,
    ERROR;

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case OK:         return "ok";
        case MODIFIED:   return "modified";
        case MERGE:      return "merge";
        case CONFLICT:   return "conflict";
        case REMOVED:    return "removed";
        case UPDATE:     return "update";
        case CHECKOUT:   return "checkout";
        case NOT_EXISTS: return "not exists";
        case WAITING:    return "waiting";
        case ADDED:      return "added";
        case ERROR:      return "error";
        case UNKNOWN:
        default:         return "unknown";
      }
    }
  };

  /** file modes
   */
  enum Modes
  {
    NONE,

    TEXT,
    BINARY,

    UNKNOWN;

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case TEXT:   return "text";
        case BINARY: return "binary";
        default:     return "unknown";
      }
    }
  };

  // --------------------------- variables --------------------------------
  public final String title;
  public final Types  type;
  public       States state;
  public       Modes  mode;
  public       String workingRevision;
  public       String repositoryRevision;
  public       String branch;

  public final String name;
  public       long   size;
  public       long   datetime;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create file data
   * @param title title text
   * @param name file name
   * @param type file type (see FileData.Types)
   * @param state state (see FileData.States)
   * @param mode mode (see FileData.Modes)
   * @param size file size [bytes]
   * @param datetime file date/time [s]
   */
  public FileData(String title, String name, Types type, States state, Modes mode, long size, long datetime)
  {
    this.title              = title;
    this.type               = type;
    this.state              = state;
    this.mode               = mode;
    this.workingRevision    = "";
    this.repositoryRevision = "";
    this.branch             = "";
    this.name               = name;
    this.size               = size;
    this.datetime           = datetime;
  }

  /** create file data
   * @param title title text
   * @param name file name
   * @param type file type
   * @param size file size [bytes]
   * @param datetime file date/time [s]
   */
  public FileData(String title, String name, Types type, long size, long datetime)
  {
    this(title,name,type,States.UNKNOWN,Modes.UNKNOWN,size,datetime);
  }

  /** create file data
   * @param title title text
   * @param name file name
   * @param type file type
   */
  public FileData(String title, String name, Types type)
  {
    this(title,name,type,0L,0L);
  }

  /** get file name
   * @return file name
   */
  public String getFileName(String rootPath)
  {
    return rootPath+File.separator+name;
  }

  /** get file name
   * @return file name
   */
  public String getFileName()
  {
    return name;
  }

  /** get base name
   * @return base name
   */
  public String getBaseName()
  {
    return new File(name).getName();
  }

  /** get directory name
   * @return directory name
   */
  public String getDirectoryName()
  {
    String parent = new File(name).getParent();
    return (parent != null) ? parent : "";
  }

  /** get file mime type (use file name extensions)
   * @return mime type or null if no mime type found
   */
  public String getMimeType(String rootPath)
  {
    MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    // add additional mime types
    mimetypesFileTypeMap.addMimeTypes("text/x-c c cpp ++");
    mimetypesFileTypeMap.addMimeTypes("text/x-java java");

    return mimetypesFileTypeMap.getContentType(getFileName(rootPath));
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "FileData {"+name+", type: "+type+", state: "+state+", mode: "+mode+", revision: "+workingRevision+", branch: "+branch+"}";
  }
}

// ------------------------------------------------------------------------

/** branch data
 */
class BranchData
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final String         name;
  public final Date           date;
  public final String         author;            
  public final String[]       commitMessage;
  public final RevisionData[] revisionTree;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create branch data
   */
  public BranchData(String name, Date date, String author, String[] commitMessage, RevisionData[] revisionTree)
  {
    this.name          = name;
    this.date          = date;
    this.author        = author;
    this.commitMessage = commitMessage;
    this.revisionTree  = revisionTree;
  }

  /** create branch data
   */
  public BranchData(String revision, Date date, String author, String[] commitMessage)
  {
    this(revision,date,author,commitMessage,null);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "BranchData {"+name+", date: "+date+", author: "+author+", message: "+commitMessage+", revisions: "+revisionTree+"}";
  }
}

/** revision data
 */
class RevisionData
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final String       revision;
  public final Date         date;
  public final String       author;
  public final String[]     commitMessage;
  public       BranchData[] branches;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision data
   * @param 
   * @return 
   */
  public RevisionData(String revision, Date date, String author, String[] commitMessage, BranchData[] branches)
  {
    this.revision      = revision;
    this.date          = date;
    this.author        = author;
    this.commitMessage = commitMessage;
    this.branches      = branches;
  }

  /** create revision data
   * @param 
   * @return 
   */
  public RevisionData(String revision, Date date, String author, String[] commitMessage, BranchData branchData)
  {
    this(revision,date,author,commitMessage,new BranchData[]{branchData});
  }

  /** create revision data
   * @param 
   * @return 
   */
  public RevisionData(String revision, Date date, String author, String[] commitMessage)
  {
    this(revision,date,author,commitMessage,new BranchData[]{});
  }

  /** add branch
   * @param 
   * @return 
   */
  public void addBranch(String name, Date date, String author, String[] commitMessage, RevisionData[] subRevisionTree)
  {
    // create branch data
    BranchData branchData = new BranchData(name,date,author,commitMessage,subRevisionTree);

    // add to branches array
    branches = Arrays.copyOf(branches,branches.length+1);
    branches[branches.length-1] = branchData;
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "RevisionData {"+revision+", date: "+date+", author: "+author+", message: "+commitMessage+", branches: "+branches+"}";
  }
}

// ------------------------------------------------------------------------

/** diff data
 */
class DiffData
{
  // --------------------------- constants --------------------------------

  /** diff block types
   */
  enum BlockTypes
  {
    NONE,

    KEEP,
    ADD,
    DELETE,
    CHANGE,

    UNKNOWN
  };

  // --------------------------- variables --------------------------------
  public final BlockTypes blockType;          // diff block type
  public final String[]   keepLines;          // lines not changed
  public final String[]   addedLines;         // add lines
  public final String[]   deletedLines;       // deleted lines

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create diff data
   * @param blockType block type
   * @param addedLines lines to add
   * @param deletedLines lines to delete
   */
  public DiffData(BlockTypes blockType, String[] addedLines, String[] deletedLines)
  {
    this.blockType    = blockType;
    switch (blockType)
    {
      case KEEP:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case ADD:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case DELETE:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case CHANGE:
        this.keepLines    = null;
        this.addedLines   = addedLines;
        this.deletedLines = deletedLines;
        break;
      default:
        throw new Error("INTERNAL ERROR: invalid block type!");
    }
  }

  /** create diff data
   * @param blockType block type
   * @param addedLines lines to add
   * @param deletedLines lines to delete
   */
  public DiffData(BlockTypes blockType, ArrayList<String> addedLines, ArrayList<String> deletedLines)
  {
    this(blockType,
         addedLines.toArray(new String[addedLines.size()]),
         deletedLines.toArray(new String[deletedLines.size()])
        );
  }

  /** create diff data
   * @param blockType block type
   * @param lines lines to keep/add/delete
   */
  public DiffData(BlockTypes blockType, String[] lines)
  {
    this.blockType = blockType;
    switch (blockType)
    {
      case KEEP:
        this.keepLines    = lines;
        this.addedLines   = null;
        this.deletedLines = null;
        break;
      case ADD:
        this.keepLines    = null;
        this.addedLines   = lines;
        this.deletedLines = null;
        break;
      case DELETE:
        this.keepLines    = null;
        this.addedLines   = null;
        this.deletedLines = lines;
        break;
      case CHANGE:
        throw new Error("INTERNAL ERROR: invalid block type!");
      default:
        throw new Error("INTERNAL ERROR: invalid block type!");
    }
  }

  /** create diff data
   * @param blockType block type
   * @param lines lines to keep/add/delete
   */
  public DiffData(BlockTypes blockType, ArrayList<String> lines)
  {
    this(blockType,
         lines.toArray(new String[lines.size()])
        );
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "DiffData {"+blockType+", keep: "+StringUtils.join(keepLines,"\\n")+", added: "+StringUtils.join(addedLines,"\\n")+" deleted: "+StringUtils.join(deletedLines,"\\n")+"}";
  }
}

// ------------------------------------------------------------------------

/** repository exception
 */
class RepositoryException extends Exception
{
  /** create repository exception
   * @param message message
   * @param cause exception cause
   * @param arguments optional format arguments for message
   */
  RepositoryException(String message, Exception cause, Object... arguments)
  {
    super(String.format(message,arguments),cause);
  }

  /** create repository exception
   * @param cause exception cause
   */
  RepositoryException(Exception cause)
  {
    this(cause.getMessage(),cause);
  }

  /** create repository exception
   * @param message message
   * @param arguments optional format arguments for message
   */
  RepositoryException(String message, Object... arguments)
  {
    this(message,null,arguments);
  }
}

// ------------------------------------------------------------------------

/** repository
 */
@XmlType(propOrder={"title","rootPath"})
@XmlSeeAlso({RepositoryCVS.class,RepositorySVN.class,RepositoryHG.class,RepositoryGit.class})
@XmlAccessorType(XmlAccessType.NONE)
abstract class Repository implements Serializable
{
  private File file;

  // --------------------------- constants --------------------------------
  enum Types
  {
    CVS,
    SVN,
    HG,
    GIT,

    UNKNOWN
  };

  // --------------------------- variables --------------------------------
  @XmlAttribute(name = "title", required=true)
  public String title;

  @XmlAttribute(name = "rootPath", required=true)
  public String rootPath;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get type of repository in path
   * @param rootPath root path
   * @return repository type or UNKNOWN
   */
  public static Types getType(String rootPath)
  {
    File file;

    file = new File(rootPath,"CVS");
    if (file.exists() && file.isDirectory())
    {
      return Types.CVS;
    }
    file = new File(rootPath,".svn");
    if (file.exists() && file.isDirectory())
    {
      return Types.SVN;
    }
    file = new File(rootPath,".hg");
    if (file.exists() && file.isDirectory())
    {
      return Types.HG;
    }
    file = new File(rootPath,".git");
    if (file.exists() && file.isDirectory())
    {
      return Types.GIT;
    }

    return Types.UNKNOWN;
  }

  /** get file type
   * @param file file
   * @return file type
   */
  public FileData.Types getFileType(File file)
  {
    FileData.Types type = FileData.Types.UNKNOWN;

    try
    {
      if      (file.exists() && !file.getCanonicalFile().equals(file.getAbsoluteFile())) type = FileData.Types.LINK;
      else if (file.isDirectory())                                                       type = FileData.Types.DIRECTORY;
      else                                                                               type = FileData.Types.FILE;
    }
    catch (IOException exception)
    {
      // ignored
    }

    return type;
  }

  /** get file type
   * @param name file name
   * @return file type
   */
  public FileData.Types getFileType(String name)
  {
    return getFileType(new File(rootPath,name));
  }

  /** create repository
   * @param rootPath root path
   */
  Repository(String rootPath)
  {
    this.title    = rootPath;
    this.rootPath = rootPath;   
  }

  /** create repository
   */
  Repository()
  {
    this(null);
  }

  /** get list of files
   * @return subDirectory sub-directory
   * @return hash with file data
   */
  public HashSet<FileData> listFiles(String subDirectory)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    File directory = new File(rootPath,subDirectory);
    for (File file : directory.listFiles())
    {
      // detect file type
      FileData.Types type = FileData.Types.UNKNOWN;
      try
      {
        if      (file.exists() && !file.getCanonicalFile().equals(file.getAbsoluteFile())) type = FileData.Types.LINK;
        else if (file.isDirectory())                                                       type = FileData.Types.DIRECTORY;
        else                                                                               type = FileData.Types.FILE;
      }
      catch (IOException exception)
      {
        // ignored
      }

      // add file data
      FileData fileData = new FileData(file.getName(),
                                       (!subDirectory.equals("")?subDirectory+File.separator:"")+file.getName(),
                                       type,
                                       file.length(),
                                       file.lastModified()
                                      );
      fileDataSet.add(fileData);
    }

    return fileDataSet;
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectoryHashSet directory set to check for new/missing files
   * @param addNewFlag add missing file
   */
  abstract public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectoryHashSet, boolean addNewFlag);

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectoryHashSet directory set to check for new/missing files
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectoryHashSet)
  {
    updateStates(fileDataSet,fileDirectoryHashSet,false);
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param addNewFlag add missing file
   */
  public void updateStates(HashSet<FileData> fileDataSet, boolean addNewFlag)
  {
    HashSet<String> fileDirectoryHashSet = new HashSet<String>();
    for (FileData fileData : fileDataSet)
    {
      String directory = fileData.getDirectoryName();
      fileDirectoryHashSet.add(!directory.isEmpty() ? directory : null);
    }

    updateStates(fileDataSet,fileDirectoryHashSet,addNewFlag);
  }

  /** update files states
   * @param fileDataSet file data set to update
   */
  public void updateStates(HashSet<FileData> fileDataSet)
  {
    updateStates(fileDataSet,false);
  }

  /** update file state
   * @param fileDataSet file data to update
   */
  public void updateStates(FileData fileData)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    updateStates(fileDataSet);
  }

  /** update file data
   * @param fileDataSet file data set
   */
  abstract public void update(HashSet<FileData> fileDataSet)
    throws RepositoryException;

  /** get last revision name
   * @return last revision name
   */
  abstract public String getLastRevision()
    throws RepositoryException;

  /** get revisions of file
   * @param fileData file data
   * @return array with revision names
   */
  abstract public String[] getRevisions(FileData fileData)
    throws RepositoryException;

  /** get revision tree
   * @param fileData file data
   * @return revision tree
   */
  abstract public RevisionData[] getRevisionTree(FileData fileData)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  abstract public String[] getFile(FileData fileData, String revision)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @return file data (array of lines)
   */
  public String[] getFile(FileData fileData)
    throws RepositoryException
  {
    return getFile(fileData,null);
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of bytes)
   */
  abstract public byte[] getFileData(FileData fileData, String revision)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @return file data (array of lines)
   */
  public byte[] getFileData(FileData fileData)
    throws RepositoryException
  {
    return getFileData(fileData,null);
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessagec commit message
   */
  abstract public void commit(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException;

  /** commit file
   * @param fileData file data
   * @param commitMessagec commit message
   */
  public void commit(FileData fileData, Message commitMessage)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    commit(fileDataSet,commitMessage);
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessagec commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  abstract public void add(HashSet<FileData> fileDataSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException;

  /** add file
   * @param fileData file data
   * @param commitMessagec commit message
   * @param binaryFlag true to add file as binary file, false otherwise
   */
  public void add(FileData fileData, Message commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    add(fileDataSet,commitMessage,binaryFlag);
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessagec commit message
   */
  abstract public void remove(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException;

  /** remove file
   * @param fileData file data
   * @param commitMessagec commit message
   */
  public void remove(FileData fileData, Message commitMessage)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    remove(fileDataSet,commitMessage);
  }

  /** revert files
   * @param fileDataSet file data set
   * @param revision revision to revert to
   */
  abstract public void revert(HashSet<FileData> fileDataSet, String revision)
    throws RepositoryException;

  /** revert file
   * @param fileData file data
   * @param revision revision to revert to
   */
  public void revert(FileData fileData, String revision)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    revert(fileDataSet,revision);
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessagec commit message
   */
  abstract public void rename(FileData fileData, String newName, Message commitMessage)
    throws RepositoryException;

  /** diff file
   * @param fileData file data
   * @param revision1,revision2 revisions to get diff for
   * @return diff data
   */
  abstract public DiffData[] diff(FileData fileData, String revision1, String revision2)
    throws RepositoryException;

  /** diff file
   * @param fileData file data
   * @param revision revision to get diff for
   * @return diff data
   */
  public DiffData[] diff(FileData fileData, String revision)
    throws RepositoryException
  {
    return diff(fileData,revision,null);
  }

  /** 
   * @param 
   * @return 
   */
  abstract public void patch(FileData fileData)
    throws RepositoryException;

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Repository {"+rootPath+"}";
  }

  //-----------------------------------------------------------------------

  /** find file data
   * @param fileDataSet file data set
   * @param directory directory
   * @param baseName base name
   * @return file data or null
   */
  protected FileData findFileData(HashSet<FileData> fileDataSet, String directory, String baseName)
  {
    return findFileData(fileDataSet,(directory != null) ? directory+File.separator+baseName : baseName);
  }

  /** find file data
   * @param fileDataSet file data set
   * @param name name
   * @return file data or null
   */
  protected FileData findFileData(HashSet<FileData> fileDataSet, String name)
  {
    for (FileData fileData : fileDataSet)
    {
      if (name.equals(fileData.name)) return fileData;
    }
    return null;
  }

  /** get file names from file data set
   * @param fileDataSet file data set
   * @return file names array
   */
  protected String[] getFileDataNames(HashSet<FileData> fileDataSet)
  {
    ArrayList<String> fileNameList = new ArrayList<String>(fileDataSet.size());

    for (FileData fileData : fileDataSet)
    {
      fileNameList.add(fileData.name);
    }

    return fileNameList.toArray(new String[fileNameList.size()]);
  }

  /** get file name from file data
   * @param fileData file data
   * @return file name
   */
  protected String getFileDataName(FileData fileData)
  {
    return fileData.name;
  }
}

/* end of file */
