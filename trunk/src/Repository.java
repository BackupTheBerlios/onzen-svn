/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Repository.java,v $
* $Revision: 1.7 $
* $Author: torsten $
* Contents: repository
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;

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
    UNKNOWN,
    MODIFIED,
    CHECKOUT,
    UPDATE,
    MERGE,
    CONFLICT,
    ADDED,
    REMOVED,
    NOT_EXISTS,
    WAITING,
    ERROR;

    /** parse type string
     * @param string state string
     * @return state
     */
    static States parse(String string)
    {
      States state;

      if      (string.equalsIgnoreCase("ok"))
      {
        state = States.OK;
      }
      else if (string.equalsIgnoreCase("unknown"))
      {
        state = States.UNKNOWN;
      }
      else if (string.equalsIgnoreCase("modified"))
      {
        state = States.MODIFIED;
      }
      else if (string.equalsIgnoreCase("checkout"))
      {
        state = States.CHECKOUT;
      }
      else if (string.equalsIgnoreCase("update"))
      {
        state = States.UPDATE;
      }
      else if (string.equalsIgnoreCase("merge"))
      {
        state = States.MERGE;
      }
      else if (string.equalsIgnoreCase("conflict"))
      {
        state = States.CONFLICT;
      }
      else if (string.equalsIgnoreCase("added"))
      {
        state = States.ADDED;
      }
      else if (string.equalsIgnoreCase("removed"))
      {
        state = States.REMOVED;
      }
      else if (string.equalsIgnoreCase("not exists"))
      {
        state = States.NOT_EXISTS;
      }
      else if (string.equalsIgnoreCase("waiting"))
      {
        state = States.WAITING;
      }
      else if (string.equalsIgnoreCase("error"))
      {
        state = States.ERROR;
      }
      else
      {
        state = States.OK;
      }

      return state;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case OK:         return "ok";
        case UNKNOWN:    return "unknown";
        case MODIFIED:   return "modified";
        case CHECKOUT:   return "checkout";
        case UPDATE:     return "update";
        case MERGE:      return "merge";
        case CONFLICT:   return "conflict";
        case ADDED:      return "added";
        case REMOVED:    return "removed";
        case NOT_EXISTS: return "not exists";
        case WAITING:    return "waiting";
        case ERROR:      return "error";
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
  public       String name;
  public final Types  type;
  public       long   size;
  public       Date   datetime;
  public       States state;
  public       Modes  mode;
  public       String workingRevision;
  public       String repositoryRevision;
  public       String branch;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create file data
   * @param name file name
   * @param type file type (see FileData.Types)
   * @param state state (see FileData.States)
   * @param mode mode (see FileData.Modes)
   * @param size file size [bytes]
   * @param datetime file date/time
   * @param workingRevision working revsion
   * @param repositoryRevision repository revision
   * @param branch branch
   */
  public FileData(String name, Types type, States state, Modes mode, long size, Date datetime, String workingRevision, String repositoryRevision, String branch)
  {
    this.name               = name;
    this.type               = type;
    this.size               = size;
    this.datetime           = datetime;
    this.state              = state;
    this.mode               = mode;
    this.workingRevision    = workingRevision;
    this.repositoryRevision = repositoryRevision;
    this.branch             = branch;
  }

  /** create file data
   * @param name file name
   * @param type file type (see FileData.Types)
   * @param state state (see FileData.States)
   * @param mode mode (see FileData.Modes)
   * @param size file size [bytes]
   * @param datetime file date/time
   * @param workingRevision working revsion
   * @param repositoryRevision repository revision
   */
  public FileData(String name, Types type, States state, Modes mode, long size, Date datetime, String workingRevision, String repositoryRevision)
  {
    this(name,type,state,mode,size,datetime,workingRevision,repositoryRevision,"");
  }

  /** create file data
   * @param name file name
   * @param type file type (see FileData.Types)
   * @param state state (see FileData.States)
   * @param mode mode (see FileData.Modes)
   * @param size file size [bytes]
   * @param datetime file date/time
   */
  public FileData(String name, Types type, States state, Modes mode, long size, Date datetime)
  {
    this(name,type,state,mode,size,datetime,"","","");
  }

  /** create file data
   * @param name file name
   * @param type file type (see FileData.Types)
   * @param size file size [bytes]
   * @param datetime file date/time
   */
  public FileData(String name, Types type, long size, Date datetime)
  {
    this(name,type,States.UNKNOWN,Modes.UNKNOWN,size,datetime);
  }

  /** create file data
   * @param name file name
   * @param type file type (see FileData.Types)
   */
  public FileData(String name, Types type)
  {
    this(name,type,0L,null);
  }

  /** create file data
   * @param name file name
   * @param state state (see FileData.States)
   * @param mode mode (see FileData.Modes)
   */
  public FileData(String name, States state, Modes mode)
  {
    this(name,Types.FILE,state,mode,0L,null);
  }

  /** create file data
   * @param name file name
   * @param state file state
   */
  public FileData(String name, States state)
  {
    this(name,state,Modes.UNKNOWN);
  }

  /** get file name
   * @param rootPath root path
   * @return file name
   */
  public String getFileName(String rootPath)
  {
    return rootPath+File.separator+name;
  }

  /** get file name
   * @param repository repository
   * @return file name
   */
  public String getFileName(Repository repository)
  {
    return getFileName(repository.rootPath);
  }

  /** get file name
   * @return file name
   */
  public String getFileName()
  {
    return name;
  }

  /** set file name
   * @param name file name
   */
  public void setFileName(String name)
  {
    this.name = name;
  }

  /** get base name
   * @return base name
   */
  public String getBaseName()
  {
    return new File(name).getName();
  }

  /** get directory name
   * @param rootPath root path
   * @return directory name
   */
  public String getDirectoryName(String rootPath)
  {
    String parent = new File(rootPath,name).getParent();
    return (parent != null) ? parent : "";
  }

  /** get directory name
   * @param repository repository
   * @return directory name
   */
  public String getDirectoryName(Repository repository)
  {
    return getDirectoryName(repository.rootPath);
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
   * @param rootPath root path
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

  /** get file mime type (use file name extensions)
   * @param repository repository
   * @return mime type or null if no mime type found
   */
  public String getMimeType(Repository repository)
  {
    return getMimeType(repository.rootPath);
  }

  /** convert to set
   * @return file data set
   */
  public HashSet<FileData> toSet()
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(this);

    return fileDataSet;
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "FileData {name: "+name+", type: "+type+", state: "+state+", mode: "+mode+", revision: "+workingRevision+", branch: "+branch+"}";
  }
}

/** file data comparator
 */
class FileDataComparator implements Comparator<FileData>
{
  // --------------------------- constants --------------------------------
  enum SortModes
  {
    NAME,
    TYPE,
    SIZE,
    DATETIME,
    STATE,
    MODE,
    WORKING_REVISION,
    REPOSITORY_REVISION,
    BRANCH
  };

  // --------------------------- variables --------------------------------
  private SortModes sortMode;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create file data comparator
   * @param sortMode sort mode
   */
  FileDataComparator(SortModes sortMode)
  {
    this.sortMode = sortMode;
  }

  /** create file data comparator
   */
  FileDataComparator()
  {
    this(SortModes.NAME);
  }

  /** get sort mode
   * @return sort mode
   */
  public SortModes getSortMode()
  {
    return sortMode;
  }

  /** set sort mode
   * @param sortMode sort mode
   */
  public void setSortMode(SortModes sortMode)
  {
    this.sortMode = sortMode;
  }

  /** compare file data
   * @param fileData0, fileData1 file data to compare
   * @return -1 iff fileData0 < fileData1,
              0 iff fileData0 = fileData1,
              1 iff fileData0 > fileData1
   */
  public int compare(FileData fileData0, FileData fileData1)
  {
    switch (sortMode)
    {
      case NAME:
        return fileData0.name.compareTo(fileData1.name);
      case TYPE:
        return fileData0.type.compareTo(fileData1.type);
      case SIZE:
        if      (fileData0.size < fileData1.size) return -1;
        else if (fileData0.size > fileData1.size) return  1;
        else                                      return  0;
      case DATETIME:
        if      (fileData0.datetime.before(fileData1.datetime)) return -1;
        else if (fileData0.datetime.after(fileData1.datetime))  return  1;
        else                                                    return  0;
      case STATE:
        return fileData0.state.compareTo(fileData1.state);
      case MODE:
        return fileData0.type.compareTo(fileData1.type);
      case WORKING_REVISION:
        return fileData0.workingRevision.compareTo(fileData1.workingRevision);
      case REPOSITORY_REVISION:
        return fileData0.repositoryRevision.compareTo(fileData1.repositoryRevision);
      case BRANCH:
        return fileData0.branch.compareTo(fileData1.branch);
      default:
        return 0;
    }
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
  enum Types
  {
    NONE,

    KEEP,
    ADDED,
    DELETED,
    CHANGED,
    CHANGED_WHITESPACES,

    UNKNOWN;

    /** parse type string
     * @param string state string
     * @return state
     */
    static Types parse(String string)
    {
      Types type;

      if      (string.equalsIgnoreCase("keep"))
      {
        type = Types.KEEP;
      }
      else if (string.equalsIgnoreCase("added"))
      {
        type = Types.ADDED;
      }
      else if (string.equalsIgnoreCase("deleted"))
      {
        type = Types.DELETED;
      }
      else if (string.equalsIgnoreCase("changed"))
      {
        type = Types.CHANGED;
      }
      else if (string.equalsIgnoreCase("changedWhitespaces"))
      {
        type = Types.CHANGED_WHITESPACES;
      }
      else
      {
        type = Types.UNKNOWN;
      }

      return type;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case KEEP:                return "ok";
        case ADDED:               return "added";
        case DELETED:             return "deleted";
        case CHANGED:             return "changed";
        case CHANGED_WHITESPACES: return "changedWhitespaces";
        default:                  return null;
      }
    }
  };

  // --------------------------- variables --------------------------------
  public final Types    type;               // diff block type
  public final String[] keepLines;          // lines not changed
  public final String[] addedLines;         // add lines
  public final String[] deletedLines;       // deleted lines

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create diff data
   * @param type block type
   * @param addedLines lines to add
   * @param deletedLines lines to delete
   */
  public DiffData(Types type, String[] addedLines, String[] deletedLines)
  {
    this.type = type;
    switch (type)
    {
      case KEEP:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case ADDED:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case DELETED:
        throw new Error("INTERNAL ERROR: invalid block type!");
      case CHANGED:
      case CHANGED_WHITESPACES:
        this.keepLines    = null;
        this.addedLines   = addedLines;
        this.deletedLines = deletedLines;
        break;
      default:
        throw new Error("INTERNAL ERROR: invalid block type!");
    }
  }

  /** create diff data
   * @param type block type
   * @param addedLines lines to add
   * @param deletedLines lines to delete
   */
  public DiffData(Types type, AbstractList<String> addedLinesList, AbstractList<String> deletedLinesList)
  {
    this(type,
         addedLinesList.toArray(new String[addedLinesList.size()]),
         deletedLinesList.toArray(new String[deletedLinesList.size()])
        );
  }

  /** create diff data
   * @param type block type
   * @param addedLines lines to add
   * @param deletedLinesList lines to delete
   */
  public DiffData(Types type, String[] addedLines, AbstractList<String> deletedLinesList)
  {
    this(type,
         addedLines,
         deletedLinesList.toArray(new String[deletedLinesList.size()])
        );
  }
  /** create diff data
   * @param type block type
   * @param addedLines lines to add
   * @param deletedLines lines to delete
   */
  public DiffData(Types type, AbstractList<String> addedLinesList, String[] deletedLines)
  {
    this(type,
         addedLinesList.toArray(new String[addedLinesList.size()]),
         deletedLines
        );
  }


  /** create diff data
   * @param type block type
   * @param lines lines to keep/add/delete
   */
  public DiffData(Types type, String[] lines)
  {
    this.type = type;
    switch (type)
    {
      case KEEP:
        this.keepLines    = lines;
        this.addedLines   = null;
        this.deletedLines = null;
        break;
      case ADDED:
        this.keepLines    = null;
        this.addedLines   = lines;
        this.deletedLines = null;
        break;
      case DELETED:
        this.keepLines    = null;
        this.addedLines   = null;
        this.deletedLines = lines;
        break;
      case CHANGED:
      case CHANGED_WHITESPACES:
        throw new Error("INTERNAL ERROR: invalid block type!");
      default:
        throw new Error("INTERNAL ERROR: invalid block type!");
    }
  }

  /** create diff data
   * @param type block type
   * @param lines lines to keep/add/delete
   */
  public DiffData(Types type, AbstractList<String> linesList)
  {
    this(type,
         linesList.toArray(new String[linesList.size()])
        );
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "DiffData {"+type+", keep "+((keepLines != null) ? keepLines.length : 0)+": '"+StringUtils.join(keepLines,"\\n")+"', added "+((addedLines != null) ? addedLines.length : 0)+": '"+StringUtils.join(addedLines,"\\n")+"' deleted "+((deletedLines != null) ? deletedLines.length : 0)+": '"+StringUtils.join(deletedLines,"\\n")+"'}";
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
  public final RevisionData[] revisionDataTree;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create branch data
   * @param revision revision
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param revisionDataTree branch revision data tree
   */
  public BranchData(String name, Date date, String author, String[] commitMessage, RevisionData[] revisionDataTree)
  {
    this.name             = name;
    this.date             = date;
    this.author           = author;
    this.commitMessage    = commitMessage;
    this.revisionDataTree = revisionDataTree;
  }

  /** create branch data
   * @param revision revision
   * @param date date
   * @param author author name
   * @param commitMessage commit message
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
    return "BranchData {"+name+", date: "+date+", author: "+author+", message: "+commitMessage+", revisions: "+revisionDataTree+"}";
  }
}

// ------------------------------------------------------------------------

/** revision data
 */
class RevisionData
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final String       revision;
  public final String       symbolicName;
  public final Date         date;
  public final String       author;
  public final String[]     commitMessage;
  public       BranchData[] branches;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision data
   * @param revision revision
   * @param symbolicName symbolic name of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branches branches
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, String[] commitMessage, BranchData[] branches)
  {
    this.revision      = revision;
    this.symbolicName  = symbolicName;
    this.date          = date;
    this.author        = author;
    this.commitMessage = commitMessage;
    this.branches      = branches;
  }

  /** create revision data
   * @param revision revision
   * @param symbolicName symbolic name of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branches branches
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, AbstractList<String> commitMessageList, BranchData[] branches)
  {
    this(revision,symbolicName,date,author,commitMessageList.toArray(new String[commitMessageList.size()]),branches);
  }

  /** create revision data
   * @param revision revision
   * @param symbolicName symbolic name of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branch branch
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, String[] commitMessage, BranchData branchData)
  {
    this(revision,symbolicName,date,author,commitMessage,new BranchData[]{branchData});
  }

  /** create revision data
   * @param revision revision
   * @param symbolicName symbolic name of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branch branch
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, AbstractList<String> commitMessageList, BranchData branchData)
  {
    this(revision,symbolicName,date,author,commitMessageList,new BranchData[]{branchData});
  }

  /** create revision data
   * @param revision revision
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, String[] commitMessage)
  {
    this(revision,symbolicName,date,author,commitMessage,new BranchData[]{});
  }

  /** create revision data
   * @param revision revision
   * @param date date
   * @param author author name
   * @param commitMessageList commit message
   */
  public RevisionData(String revision, String symbolicName, Date date, String author, AbstractList<String> commitMessageList)
  {
    this(revision,symbolicName,date,author,commitMessageList,new BranchData[]{});
  }

  /** get revision text for display
   * @return revision text
   */
  public String getRevisionText()
  {
    StringBuilder buffer = new StringBuilder();

    buffer.append(revision);
    if (symbolicName != null)
    {
      buffer.append(" (");
      buffer.append(symbolicName);
      buffer.append(")");
    }

    return buffer.toString();
  }

  /** add branch
   * @param name branch name
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param subRevisionDataTree branch sub-revision data tree
   */
  public void addBranch(String name, Date date, String author, String[] commitMessage, RevisionData[] subRevisionDataTree)
  {
    // create branch data
    BranchData branchData = new BranchData(name,date,author,commitMessage,subRevisionDataTree);

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

/** log data
 */
class LogData
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final String   revision;
  public final Date     date;
  public final String   author;
  public final String[] commitMessage;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create log data
   * @param revision revision
   * @param author author name
   * @param date date
   * @param commitMessage commit message
   */
  public LogData(String revision, Date date, String author, String[] commitMessage)
  {
    this.revision      = revision;
    this.author        = author;
    this.date          = date;
    this.commitMessage = commitMessage;
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "LogData {"+revision+", author: "+author+", date: "+date+", commit message: "+commitMessage+"}";
  }
}

/** annotation data
 */
class AnnotationData
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public final String revision;
  public final String author;
  public final Date   date;
  public final String line;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create annotation data
   * @param revision revision
   * @param author author name
   * @param date date
   * @param line text line
   */
  public AnnotationData(String revision, String author, Date date, String line)
  {
    this.revision = revision;
    this.author   = author;
    this.date     = date;
    this.line     = line;
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "AnnotationData {"+revision+", author: "+author+", date: "+date+", line: "+line+"}";
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
@XmlType(propOrder={"title","rootPath","openDirectories","patchMailTo","patchMailCC","patchMailSubject","patchMailText"})
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
  @XmlAttribute(name = "selected")
  public boolean selected;

  @XmlElement(name = "rootPath")
  public String rootPath;

  @XmlElementWrapper(name = "openDirectories")
  @XmlElement(name = "path")
  private HashSet<String> openDirectories;

  @XmlElement(name = "patchMailTo")
  public String patchMailTo;
  @XmlElement(name = "patchMailCC")
  public String patchMailCC;
  @XmlElement(name = "patchMailSubject")
  public String patchMailSubject;
  @XmlElement(name = "patchMailText")
  public String patchMailText;

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

  /** create repository
   * @param rootPath root path
   */
  Repository(String rootPath)
  {
    int z;

    this.title           = rootPath;
    this.rootPath        = rootPath;
    this.openDirectories = new HashSet<String>();
  }

  /** create repository
   */
  Repository()
  {
    this(null);
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.UNKNOWN;
  }

  /** check if repository support setting file mode
   * @return true iff file modes are supported
   */
  public boolean supportSetFileMode()
  {
    return false;
  }

  /** check if repository support pull/push commands
   * @return true iff pull/push commands are supported
   */
  public boolean supportPullPush()
  {
    return false;
  }

  /** check if repository support patch queues
   * @return true iff patch queues are supported
   */
  public boolean supportPatchQueues()
  {
    return false;
  }

  /** get open directories
   * @return open directories array
   */
  public String[] getOpenDirectories()
  {
    synchronized(openDirectories)
    {
      return openDirectories.toArray(new String[openDirectories.size()]);
    }
  }

  /** open sub-directory
   * @param directory sub-directory to open
   * @return file data set of sub-directory
   */
  public HashSet<FileData> openDirectory(String directory)
  {
    // list files
    HashSet<FileData> fileDataSet = listFiles(directory);

    // add sub-directory to list with open directories
    synchronized(openDirectories)
    {
      openDirectories.add(directory);
    }

    return fileDataSet;
  }

  /** close sub-directory
   * @param directory sub-directory to close
   */
  public void closeDirectory(String directory)
  {
    synchronized(openDirectories)
    {
      // remove directory and sub-directories from list with open directories
      String[] subDirectories = openDirectories.toArray(new String[openDirectories.size()]);
      for (String subDirectory : subDirectories)
      {
        if (subDirectory.startsWith(directory))
        {
          openDirectories.remove(subDirectory);
        }
      }
    }
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

  /** check if file is hidden
   * @param fileName file name
   * @param type file type; see FileData.Types
   * @return true iff hidden
   */
  public boolean isHiddenFile(String fileName, FileData.Types type)
  {
    boolean hiddenFlag = false;
    switch (type)
    {
      case DIRECTORY:
        for (Settings.FilePattern filePattern : Settings.hiddenDirectoryPatterns)
        {
          if (filePattern.pattern.matcher(fileName).matches())
          {
            hiddenFlag = true;
            break;
          }
        }
        break;
      case FILE:
      case LINK:
        for (Settings.FilePattern filePattern : Settings.hiddenFilePatterns)
        {
          if (filePattern.pattern.matcher(fileName).matches())
          {
            hiddenFlag = true;
            break;
          }
        }
        break;
      default:
        break;
    }

    return hiddenFlag;
  }

  /** check if file is hidden
   * @param file file
   * @param type file type; see FileData.Types
   * @return true iff hidden
   */
  public boolean isHiddenFile(File file, FileData.Types type)
  {
    return isHiddenFile(file.getName(),type);
  }

  /** check if file is hidden
  * @param directory directory
   * @param baseName file base name
   * @return true iff hidden
   */
  public boolean isHiddenFile(String directory, String baseName)
  {
    String fileName = !directory.isEmpty() ? directory+File.separator+baseName : baseName;
    return isHiddenFile(fileName,getFileType(fileName));
  }

  /** check if file is hidden
   * @param fileName file name
   * @return true iff hidden
   */
  public boolean isHiddenFile(String fileName)
  {
    return isHiddenFile(fileName,getFileType(fileName));
  }

  /** get list of files (exclude hidden files)
   * @return subDirectory sub-directory
   * @return hash with file data
   */
  public HashSet<FileData> listFiles(String subDirectory)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    File directory = !subDirectory.isEmpty() ? new File(rootPath,subDirectory) : new File(rootPath);
    File[] files = directory.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        // detect file type
        FileData.Types type = getFileType(file);

        // add file data
        if (!isHiddenFile(file,type))
        {
          FileData fileData = new FileData((!subDirectory.isEmpty()?subDirectory+File.separator:"")+file.getName(),
                                           type,
                                           file.length(),
                                           new Date(file.lastModified())
                                          );
          fileDataSet.add(fileData);
        }
      }
    }

    return fileDataSet;
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  abstract public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet);

  /** update file states
   * @param fileDataSet file data set to update
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<FileData> newFileDataSet)
  {
    // get directories
    HashSet<String> fileDirectoryHashSet = new HashSet<String>();
    for (FileData fileData : fileDataSet)
    {
      String directory = fileData.getDirectoryName();
      fileDirectoryHashSet.add(directory);
    }

    // udpate states
    updateStates(fileDataSet,fileDirectoryHashSet,newFileDataSet);
  }

  /** update files states
   * @param fileDataSet file data set to update
   */
  public void updateStates(HashSet<FileData> fileDataSet)
  {
    updateStates(fileDataSet,(HashSet<FileData>)null);
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

  /** get last revision name
   * @return last revision name
   */
  abstract public String getLastRevision();

  /** get revision names of file
   * @param fileData file data
   * @return array with revision names
   */
  abstract public String[] getRevisionNames(FileData fileData)
    throws RepositoryException;

  /** get revision data
   * @param fileData file data
   * @param revision revision
   * @return revision data
   */
  abstract public RevisionData getRevisionData(FileData fileData, String revision)
    throws RepositoryException;

  /** get revision data of last revision
   * @param fileData file data
   * @return revision data
   */
  public RevisionData getRevisionData(FileData fileData)
    throws RepositoryException
  {
    return getRevisionData(fileData,getLastRevision());
  }

  /** get revision data tree
   * @param fileData file data
   * @return revision data tree
   */
  abstract public RevisionData[] getRevisionDataTree(FileData fileData)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of lines)
   */
  abstract public String[] getFileLines(FileData fileData, String revision)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @return file data (array of lines)
   */
  public String[] getFileLines(FileData fileData)
    throws RepositoryException
  {
    return getFileLines(fileData,null);
  }

  /** get file data (byte array)
   * @param fileData file data
   * @param revision revision to get
   * @return file data (array of bytes)
   */
  abstract public byte[] getFileBytes(FileData fileData, String revision)
    throws RepositoryException;

  /** get file data (text lines)
   * @param fileData file data
   * @return file data (array of lines)
   */
  public byte[] getFileBytes(FileData fileData)
    throws RepositoryException
  {
    return getFileBytes(fileData,null);
  }

  /** get all changed files
   * @return fileDataSet file data set with modified files
   */
  abstract public HashSet<FileData> getChangedFiles()
    throws RepositoryException;

  /** get diff of file
   * @param fileData file data
   * @param revision1,revision2 revisions to get diff for
   * @return diff data
   */
  abstract public DiffData[] getDiff(FileData fileData, String revision1, String revision2)
    throws RepositoryException;

  /** get diff of file
   * @param fileData file data
   * @param revision revision to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String revision)
    throws RepositoryException
  {
    return getDiff(fileData,revision,null);
  }

  /** get patch for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  abstract public String[] getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException;

  /** get patch for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
    return getPatch(fileDataSet,revision1,revision2,false);
  }

  /** get patch for file
   * @param fileData file data
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatch(FileData fileData, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    return getPatch(fileDataSet,revision1,revision2,false);
  }

  /** get patch for file
   * @param fileData file data
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatch(FileData fileData, String revision1, String revision2)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    return getPatch(fileDataSet,revision1,revision2,false);
  }

  /** get log to file
   * @param fileData file data
   * @return log array
   */
  abstract public LogData[] getLog(FileData fileData)
    throws RepositoryException;

  /** get annotations to file
   * @param fileData file data
   * @param revision revision
   * @return annotation array
   */
  abstract public AnnotationData[] getAnnotations(FileData fileData, String revision)
    throws RepositoryException;

  /** get annotations to file of last revision
   * @param fileData file data
   * @return annotation array
   */
  public AnnotationData[] getAnnotations(FileData fileData)
    throws RepositoryException
  {
    return getAnnotations(fileData,getLastRevision());
  }

  /** update file from respository
   * @param fileDataSet file data set
   */
  abstract public void update(HashSet<FileData> fileDataSet)
    throws RepositoryException;

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  abstract public void commit(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException;

  /** commit file
   * @param fileData file data
   * @param commitMessage commit message
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
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  abstract public void add(HashSet<FileData> fileDataSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException;

  /** add file
   * @param fileData file data
   * @param commitMessage commit message
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
   * @param commitMessage commit message
   */
  abstract public void remove(HashSet<FileData> fileDataSet, Message commitMessage)
    throws RepositoryException;

  /** remove file
   * @param fileData file data
   * @param commitMessage commit message
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

  /** revert files
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
   * @param commitMessage commit message
   */
  abstract public void rename(FileData fileData, String newName, Message commitMessage)
    throws RepositoryException;

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  abstract public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, Message commitMessage)
    throws RepositoryException;

  /** set files mode
   * @param fileData file data
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(FileData fileData, FileData.Modes mode, Message commitMessage)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    setFileMode(fileDataSet,mode,commitMessage);
  }

  /** pull changes
   */
  abstract public void pullChanges()
    throws RepositoryException;

  /** push changes
   */
  abstract public void pushChanges()
    throws RepositoryException;

  /** apply patches
   */
  abstract public void applyPatches()
    throws RepositoryException;

  /** unapply patches
   */
  abstract public void unapplyPatches()
    throws RepositoryException;

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Repository {"+rootPath+"}";
  }

  //-----------------------------------------------------------------------

  /** 
   * @param 
   * @return 
   */
  protected Date parseDate(String string)
  {
    final String[] FORMATS = new String[]
    {
      "dd-MMM-yy",                   // 25-Jan-11
      "yyyy-MM-dd HH:mm:ss Z",       // 2011-01-25 15:19:06 +0900
      "yyyy-MM-dd HH:mm Z",          // 2011-01-25 15:19 +0900
      "yyyy/MM/dd HH:mm:ss",         // 2010/05/13 07:09:36
      "EEE MMM dd HH:mm:ss yyyy Z",  // Wed Dec 17 15:41:19 2008 +0100
    };

    Date date;
    
    for (String format : FORMATS)
    {
      try
      {
        date = new SimpleDateFormat(format).parse(string);
        return date;
      }
      catch (ParseException exception)
      {
        // ignored, try next format
      }
    }
    if (Settings.debugFlag)
    {
      Onzen.printWarning("Cannot parse date '%s'",string);
    }

    return new Date();
  }

  /** find file data
   * @param fileDataSet file data set
   * @param directory directory
   * @param baseName base name
   * @return file data or null
   */
  protected FileData findFileData(HashSet<FileData> fileDataSet, String directory, String baseName)
  {
    return findFileData(fileDataSet,!directory.isEmpty() ? directory+File.separator+baseName : baseName);
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

  /** check if file data set contain file data
   * @param fileDataSet file data set
   * @param file data
   * @return true iff found
   */
  protected boolean containFileData(HashSet<FileData> fileDataSet, FileData fileData)
  {
    return findFileData(fileDataSet,fileData.getFileName()) != null;
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
    return fileData.getFileName();
  }
}

/* end of file */
