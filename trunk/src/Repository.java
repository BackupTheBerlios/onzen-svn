/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Repository.java,v $
* $Revision: 1.8 $
* $Author: torsten $
* Contents: repository
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

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

  final static EnumSet<States> STATES_ALL   = EnumSet.allOf(States.class);
  final static EnumSet<States> STATES_KNOWN = EnumSet.of(States.OK,
                                                         States.MODIFIED,
                                                         States.CHECKOUT,
                                                         States.UPDATE,
                                                         States.MERGE,
                                                         States.CONFLICT,
                                                         States.ADDED,
                                                         States.REMOVED
                                                        );
  final static EnumSet<States> STATES_NONE  = EnumSet.allOf(States.class);

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

  /** create file data
   * @param name file name
   */
  public FileData(String name)
  {
    this(name,States.UNKNOWN);
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

  /** convert this to set
   * @return file data set
   */
  public HashSet<FileData> toSet()
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(this);

    return fileDataSet;
  }

  /** convert to set
   * @param fileNames file name array
   * @return file data set
   */
  static public HashSet<FileData> toSet(String[] fileNames)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    for (String fileName : fileNames)
    {
      fileDataSet.add(new FileData(fileName));
    }

    return fileDataSet;
  }

  /** convert to set
   * @param fileNameSet file name set
   * @return file data set
   */
  static public HashSet<FileData> toSet(HashSet<String> fileNameSet)
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    for (String fileName : fileNameSet)
    {
      fileDataSet.add(new FileData(fileName));
    }

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
  public String         revision;
  public RevisionData[] parents;
  public String[]       tags;
  public Date           date;
  public String         author;
  public String[]       commitMessage;
  public BranchData[]   branches;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create revision data
   * @param revision revision
   * @param parents list with parents of this revision
   * @param tags tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branches branches
   */
  public RevisionData(String revision, RevisionData[] parents, String[] tags, Date date, String author, String[] commitMessage, BranchData[] branches)
  {
    this.revision      = revision;
    this.parents       = parents;
    this.tags          = tags;
    this.date          = date;
    this.author        = author;
    this.commitMessage = commitMessage;
    this.branches      = branches;
  }

  /** create revision data
   * @param revision revision
   * @param parent parent of this revision
   * @param tags tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branches branches
   */
  public RevisionData(String revision, RevisionData parent, String[] tags, Date date, String author, String[] commitMessage, BranchData[] branches)
  {
    this(revision,
         new RevisionData[]{parent},
         tags,
         date,
         author,
         commitMessage,
         branches
        );
  }

  /** create revision data
   * @param revision revision
   * @param parents list with parents of this revision
   * @param tagList tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessageList commit message
   * @param branches branches
   */
  public RevisionData(String revision, AbstractList<RevisionData> parentList, AbstractList<String> tagList, Date date, String author, AbstractList<String> commitMessageList, BranchData[] branches)
  {
    this(revision,
         (parentList != null)?parentList.toArray(new RevisionData[parentList.size()]):(RevisionData[])null,
         (tagList != null)?tagList.toArray(new String[tagList.size()]):(String[])null,
         date,
         author,
         (commitMessageList != null)?commitMessageList.toArray(new String[commitMessageList.size()]):(String[])null,
         branches
        );
  }

  /** create revision data
   * @param revision revision
   * @param tags tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessage commit message
   * @param branch branch
   */
  public RevisionData(String revision, RevisionData[] parents, String[] tags, Date date, String author, String[] commitMessage, BranchData branchData)
  {
    this(revision,
         parents,
         tags,
         date,
         author,
         commitMessage,
         new BranchData[]{branchData}
        );
  }

  /** create revision data
   * @param revision revision
   * @param parents list with parents of this revision
   * @param tagList tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessageList commit message
   * @param branch branch
   */
  public RevisionData(String revision, AbstractList<RevisionData> parentList, AbstractList<String> tagList, Date date, String author, AbstractList<String> commitMessageList, BranchData branchData)
  {
    this(revision,
         parentList,
         tagList,
         date,
         author,
         commitMessageList,
         new BranchData[]{branchData}
        );
  }

  /** create revision data
   * @param revision revision
   * @param parents list with parents of this revision
   * @param tag tag name or null
   * @param date date
   * @param author author name
   * @param commitMessageList commit message
   */
  public RevisionData(String revision, AbstractList<RevisionData> parentList, String tag, Date date, String author, AbstractList<String> commitMessageList)
  {
    this(revision,
         (parentList != null)?parentList.toArray(new RevisionData[parentList.size()]):(RevisionData[])null,
         (tag != null)?new String[]{tag}:(String[])null,
         date,
         author,
         (commitMessageList != null)?commitMessageList.toArray(new String[commitMessageList.size()]):(String[])null,
         new BranchData[]{}
        );
  }

  /** create revision data
   * @param revision revision
   * @param parents list with parents of this revision
   * @param tagList tag names of this revision or null
   * @param date date
   * @param author author name
   * @param commitMessageList commit message
   */
  public RevisionData(String revision, AbstractList<RevisionData> parentList, AbstractList<String> tagList, Date date, String author, AbstractList<String> commitMessageList)
  {
    this(revision,
         parentList,
         tagList,
         date,
         author,
         commitMessageList,
         new BranchData[]{}
        );
  }

  /** create revision data
   * @param revision revision
   */
  public RevisionData(String revision)
  {
    this(revision,
         (AbstractList<RevisionData>)null,
         (AbstractList<String>)null,
         null,
         null,
         null
        );
  }

  /** get revision text for display
   * @return revision text
   */
  public String getRevisionText()
  {
    StringBuilder buffer = new StringBuilder();

    buffer.append(revision);
    if (tags != null)
    {
      buffer.append(" (");
      buffer.append(StringUtils.join(tags));
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

//-----------------------------------------------------------------------

/** store local files into database
 */
class StoredFiles
{
  // --------------------------- constants --------------------------------
  private static final String FILES_DATABASE_NAME    = "files";
  private static final int    FILES_DATABASE_VERSION = 1;

  // --------------------------- variables --------------------------------
  private String rootPath;
  private File   file;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create storage of files
   * @param rootPath root path
   * @param fileDataSet file data set to store
   */
  public StoredFiles(String rootPath, HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    this.rootPath = rootPath;
    this.file     = null;

    ZipOutputStream output = null;
    try
    {
      if (fileDataSet.size() > 0)
      {
        // create temporary file
        file = File.createTempFile("onzen",".zip",new File(Settings.tmpDirectory));

        // create ZIP storage
        output = new ZipOutputStream(new FileOutputStream(file));
        output.setLevel(ZipOutputStream.STORED);

        // store files
        byte[] buffer = new byte[64*1024];
        for (FileData fileData : fileDataSet)
        {

          // add ZIP entry to output stream.
          output.putNextEntry(new ZipEntry(fileData.getFileName(rootPath)));

          // store file data
          FileInputStream input = new FileInputStream(fileData.getFileName(rootPath));
          int n;
          while ((n = input.read(buffer)) > 0)
          {
            output.write(buffer,0,n);
          }
          input.close();

          // close ZIP entry
          output.closeEntry();
        }

        // close ZIP storage
        output.close(); output = null;
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
    finally
    {
      try { if (output != null) output.close(); } catch (IOException exception) { /* ignored */ }
    }
  }

  /** restore files stored in database
   * @return true iff changes restored
   */
  protected boolean restore()
    throws RepositoryException
  {
    ZipFile zipFile = null;
    try
    {
      if (file != null)
      {
        // open ZIP storage
        zipFile = new ZipFile(file);

        // read entries
        byte[] buffer = new byte[64*1024];
        Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements())
        {
          // get entry
          ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();

          // restore file data
          InputStream      input  = zipFile.getInputStream(zipEntry);
          FileOutputStream output = new FileOutputStream(zipEntry.getName());
          int n;
          while ((n = input.read(buffer)) > 0)
          {
            output.write(buffer,0,n);
          }
          input.close();
        }

        // close ZIP storage
        zipFile.close();
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
    finally
    {
      try { if (zipFile != null) zipFile.close(); } catch (IOException exception) { /* ignored */ }
    }

    return true;
  }

  /** discard stored files in database
   */
  protected void discard()
    throws RepositoryException
  {
    if (file != null)
    {
      file.delete();
    }
  }
}

// ------------------------------------------------------------------------

/** config value annotation
 */
@Target({TYPE,FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface RepositoryValue
{
  String  name()         default "";                 // name of value
  String  defaultValue() default "";                 // default value
  Class   type()         default DEFAULT.class;      // adapter class
  boolean pathSelector() default false;              // true for path selector widget
  int     min()          default Integer.MIN_VALUE;  // min. value
  int     max()          default Integer.MAX_VALUE;  // max. value
  String  title()        default "";                 // title of value
  String  text()         default "";                 // text of value (boolean)
  String  tooltip()      default "";                 // tooltip for value

  static final class DEFAULT
  {
  }
}

/** repository
 */
@XmlType(propOrder={"title",
                    "rootPath",
                    "openDirectories",

                    "patchTests",

                    "mailSMTPHost",
                    "mailSMTPPort",
                    "mailSMTPSSL",
                    "mailLogin",
                    "mailFrom",

                    "patchMailFlag",
                    "patchMailTo",
                    "patchMailCC",
                    "patchMailSubject",
                    "patchMailText",

                    "reviewServerFlag",
                    "reviewServerHost",
                    "reviewServerLogin",
                    "reviewServerSummary",
                    "reviewServerRepository",
                    "reviewServerGroups",
                    "reviewServerPersons",
                    "reviewServerDescription"
                   }
        )
@XmlSeeAlso({RepositoryCVS.class,RepositorySVN.class,RepositoryHG.class,RepositoryGit.class})
@XmlAccessorType(XmlAccessType.NONE)
abstract class Repository implements Serializable
{
  /** store local changes into into a temporary patch file
   */
  class StoredChanges
  {
    // --------------------------- constants --------------------------------

    // --------------------------- variables --------------------------------
    private File patchFile;

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

    /** create stored chnages of local changes
     * @param fileDataSet file data set to store changes
     */
    public StoredChanges(HashSet<FileData> fileDataSet)
      throws RepositoryException
    {
      patchFile = getPatchFile(fileDataSet);
    }

    /** create stored chnages of all local changes
     */
    public StoredChanges()
      throws RepositoryException
    {
      this(getChangedFiles(FileData.STATES_KNOWN));
    }

    /** restore changes stored in database
     * @return true iff changes restored
     */
    public boolean restore()
      throws RepositoryException
    {
      // create patch from file
      Patch patch = new Patch(rootPath,patchFile);

      // apply patch to restore changes
      if (!patch.apply())
      {
        return false;
      }

      return true;
    }

    /** restore changes stored in database
     */
    public void discard()
      throws RepositoryException
    {
      if (patchFile != null)
      {
        patchFile.delete(); patchFile = null;
      }
    }
  }

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

  @XmlElementWrapper(name = "patchTests")
  @XmlElement(name = "patchTests", defaultValue = "")
  public String[] patchTests = new String[0];

  @XmlElement(name = "mailSMTPHost")
  public String  mailSMTPHost;
  @XmlElement(name = "mailSMTPPort")
  public int     mailSMTPPort;
  @XmlElement(name = "mailSMTPSSL")
  public boolean mailSMTPSSL;
  @XmlElement(name = "mailLogin")
  public String  mailLogin;
  @XmlElement(name = "mailFrom")
  public String  mailFrom;

  @XmlElement(name = "patchMail")
  public boolean patchMailFlag;
  @XmlElement(name = "patchMailTo", defaultValue = "")
  public String patchMailTo = "";
  @XmlElement(name = "patchMailCC", defaultValue = "")
  public String patchMailCC = "";
  @XmlElement(name = "patchMailSubject", defaultValue = "Patch #${n %04d}: ${summary}")
  public String patchMailSubject = "Patch #${n %04d}: ${summary}";
  @XmlElement(name = "patchMailText", defaultValue = "${message}\n\n${tests - %s}")
  public String patchMailText = "${message}\n\n${tests - %s}";

  @XmlElement(name = "reviewServer")
  public boolean reviewServerFlag;
  @XmlElement(name = "reviewServerHost")
  public String  reviewServerHost;
  @XmlElement(name = "reviewServerLogin")
  public String  reviewServerLogin;
  @XmlElement(name = "reviewServerSummary", defaultValue = "Patch #${n %04d}: ${summary}")
  public String reviewServerSummary = "Patch #${n %04d}: ${summary}";
  @XmlElement(name = "reviewServerRepository")
  public String  reviewServerRepository;
  @XmlElement(name = "reviewServerGroups")
  public String  reviewServerGroups;
  @XmlElement(name = "reviewServerPersons")
  public String  reviewServerPersons;
  @XmlElement(name = "reviewServerDescription", defaultValue = "${message}\n\n${tests - %s}")
  public String reviewServerDescription = "${message}\n\n${tests - %s}";

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get type of repository in path
   * @param rootPath root path
   * @return repository type or UNKNOWN
   */
  public static Types getType(String rootPath)
  {
    File directory = new File(rootPath);
    while (directory != null)
    {
      File file;

      file = new File(directory,"CVS");
      if (file.exists() && file.isDirectory())
      {
        return Types.CVS;
      }
      file = new File(directory,".svn");
      if (file.exists() && file.isDirectory())
      {
        return Types.SVN;
      }
      file = new File(directory,".hg");
      if (file.exists() && file.isDirectory())
      {
        return Types.HG;
      }
      file = new File(directory,".git");
      if (file.exists() && file.isDirectory())
      {
        return Types.GIT;
      }

      directory = directory.getParentFile();
    }

    return Types.UNKNOWN;
  }

  /** create new repository instance
   * @param rootPath root path
   * @return repository or null if no repository found
   */
  public static Repository newInstance(String rootPath)
    throws RepositoryException
  {
    switch (Repository.getType(rootPath))
    {
      case CVS: return new RepositoryCVS(rootPath);
      case SVN: return new RepositorySVN(rootPath);
      case HG:  return new RepositoryHG(rootPath);
      case GIT: return new RepositoryGit(rootPath);
      default:  throw new RepositoryException("no repository (CVS/SVN/HG/Git) found");
    }
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

  /** check if repository support posting reviews
   * @return true iff posting reviews is supported
   */
  public boolean supportPostReview()
  {
    return false;
  }

  /** check if commit message is valid and acceptable
   * @return true iff commit message accepted
   */
  public boolean validCommitMessage(CommitMessage commitMessage)
  {
Dprintf.dprintf("");
    return true;
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
      // get canonical path, file name
      String path     = file.getParentFile().getCanonicalPath();
      String fileName = file.getName();

      // check if link, directory, file
      if      (file.exists() && !new File(path,fileName).getCanonicalFile().equals(new File(path,fileName).getAbsoluteFile())) type = FileData.Types.LINK;
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

  /** get all changed/unknown files
   * @param stateSet state set
   * @return fileDataSet file data set with modified files
   */
  abstract public HashSet<FileData> getChangedFiles(EnumSet<FileData.States> stateSet)
    throws RepositoryException;

  /** get all changed/unknown files
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles()
    throws RepositoryException
  {
    return getChangedFiles(FileData.STATES_ALL);
  }

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

  /** get unified patch lines for files
   * Note: must return unified patch data with file names relative to rootPath
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @param output patch output or null
   * @param lineLine patch data lines or null
   */
  abstract public void getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces, PrintWriter output, ArrayList<String> lineList)
    throws RepositoryException;

  /** get unified patch lines for files
   * Note: must return unified patch data with file names relative to rootPath
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return file with patch
   */
  public File getPatchFile(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    File file = null;
    try
    {
      file = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));
      PrintWriter output = new PrintWriter(new FileWriter(file));
      getPatch(fileDataSet,revision1,revision2,ignoreWhitespaces,output,null);
      output.close();
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }

    return file;
  }

  /** get unified patch lines for file
   * @param fileNameSet file name set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return file with patch
   */
  public File getPatchFile(Collection<String> fileNameSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNameSet)
    {
      fileDataSet.add(new FileData(fileName));
    }
    updateStates(fileDataSet);

    return getPatchFile(fileDataSet,revision1,revision2,ignoreWhitespaces);
  }

  /** get unified patch lines for file
   * @param fileNames file names
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return file with patch
   */
  public File getPatchFile(String[] fileNames, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNames)
    {
      fileDataSet.add(new FileData(fileName));
    }
    updateStates(fileDataSet);

    return getPatchFile(fileDataSet,revision1,revision2,ignoreWhitespaces);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return file with patch
   */
  public File getPatchFile(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
    return getPatchFile(fileDataSet,revision1,revision2,false);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @param ignoreWhitespaces true to ignore white spaces
   * @return file with patch
   */
  public File getPatchFile(HashSet<FileData> fileDataSet, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    return getPatchFile(fileDataSet,getLastRevision(),null,ignoreWhitespaces);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @return file with patch
   */
  public File getPatchFile(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    return getPatchFile(fileDataSet,false);
  }

  /** get patch for all changed files
   * @return file with patch
   */
  public File getPatchFile()
    throws RepositoryException
  {
    return getPatchFile((HashSet<FileData>)null);
  }

  /** get unified patch lines for file
   * Note: must return unified patch data with file names relative to rootPath
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    ArrayList<String> lineList = new ArrayList<String>();
    getPatch(fileDataSet,revision1,revision2,ignoreWhitespaces,null,lineList);
    return lineList.toArray(new String[lineList.size()]);
  }

  /** get unified patch lines for file
   * @param fileNameSet file name set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(Collection<String> fileNameSet, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNameSet)
    {
      fileDataSet.add(new FileData(fileName));
    }
    updateStates(fileDataSet);

    return getPatchLines(fileDataSet,revision1,revision2,ignoreWhitespaces);
  }

  /** get unified patch lines for file
   * @param fileNames file names
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(String[] fileNames, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNames)
    {
      fileDataSet.add(new FileData(fileName));
    }
    updateStates(fileDataSet);

    return getPatchLines(fileDataSet,revision1,revision2,ignoreWhitespaces);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
    return getPatchLines(fileDataSet,revision1,revision2,false);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(HashSet<FileData> fileDataSet, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    return getPatchLines(fileDataSet,getLastRevision(),null,ignoreWhitespaces);
  }

  /** get unified patch for file
   * @param fileDataSet file data set
   * @return patch data lines
   */
  public String[] getPatchLines(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    return getPatchLines(fileDataSet,false);
  }

  /** get patch for all changed files
   * @return patch data lines
   */
  public String[] getPatchLines()
    throws RepositoryException
  {
    return getPatchLines((HashSet<FileData>)null);
  }

  /** get unified patch for file
   * @param fileData file data
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(FileData fileData, String revision1, String revision2, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    return getPatchLines(fileDataSet,revision1,revision2,false);
  }

  /** get unified patch for file
   * @param fileData file data
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(FileData fileData, String revision1, String revision2)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    return getPatchLines(fileDataSet,revision1,revision2,false);
  }

  /** get unified patch for file
   * @param fileData file data
   * @param ignoreWhitespaces true to ignore white spaces
   * @return patch data lines
   */
  public String[] getPatchLines(FileData fileData, boolean ignoreWhitespaces)
    throws RepositoryException
  {
    return getPatchLines(fileData,getLastRevision(),null,ignoreWhitespaces);
  }

  /** get unified patch for file
   * @param fileData file data
   * @return patch data lines
   */
  public String[] getPatchLines(FileData fileData)
    throws RepositoryException
  {
    return getPatchLines(fileData,false);
  }

// NYI: is getPatchBytes() obsolete?
  /** get patch data for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @return patch data bytes
   */
  abstract public byte[] getPatchBytes(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException;

  /** get patch data for file
   * @param fileDataSet file data set
   * @return patch data bytes
   */
  public byte[] getPatchBytes(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    return getPatchBytes(fileDataSet,null,null);
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
  abstract public void commit(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException;

  /** commit file
   * @param fileData file data
   * @param commitMessage commit message
   */
  public void commit(FileData fileData, CommitMessage commitMessage)
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
  abstract public void add(HashSet<FileData> fileDataSet, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException;

  /** add files
   * @param fileNameSet file name set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(Collection<String> fileNameSet, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNameSet)
    {
Dprintf.dprintf("fileName=%s",fileName);
      fileDataSet.add(new FileData(fileName));
    }

    add(fileDataSet,commitMessage,binaryFlag);
  }

  /** add files
   * @param fileNames file names
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(String[] fileNames, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : fileNames)
    {
      fileDataSet.add(new FileData(fileName));
    }

    add(fileDataSet,commitMessage,binaryFlag);
  }

  /** add file
   * @param fileData file data
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary file, false otherwise
   */
  public void add(FileData fileData, CommitMessage commitMessage, boolean binaryFlag)
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
  abstract public void remove(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException;

  /** remove file
   * @param fileData file data
   * @param commitMessage commit message
   */
  public void remove(FileData fileData, CommitMessage commitMessage)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    remove(fileDataSet,commitMessage);
  }

  /** revert files
   * @param fileDataSet file data set or null for all files
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

  /** revert files
   * @param fileDataSet file data set or null for all files
   */
  public void revert(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
   revert(fileDataSet,getLastRevision());
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  abstract public void rename(FileData fileData, String newName, CommitMessage commitMessage)
    throws RepositoryException;

  /** pull changes
   */
  abstract public void pullChanges()
    throws RepositoryException;

  /** push changes
   */
  abstract public void pushChanges()
    throws RepositoryException;

  /** apply patch queue patches
   */
  abstract public void applyPatches()
    throws RepositoryException;

  /** unapply patch queue patches
   */
  abstract public void unapplyPatches()
    throws RepositoryException;

  /** set files mode
   * @param fileDataSet file data set
   * @param mode file mode
   * @param commitMessage commit message
   */
  abstract public void setFileMode(HashSet<FileData> fileDataSet, FileData.Modes mode, CommitMessage commitMessage)
    throws RepositoryException;

  /** set files mode
   * @param fileData file data
   * @param mode file mode
   * @param commitMessage commit message
   */
  public void setFileMode(FileData fileData, FileData.Modes mode, CommitMessage commitMessage)
    throws RepositoryException
  {
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    fileDataSet.add(fileData);

    setFileMode(fileDataSet,mode,commitMessage);
  }

  /** post to review server
   */
  public void postReview(String password, HashSet<FileData> fileDataSet, CommitMessage commitMessage, LinkedHashSet<String> testSet)
    throws RepositoryException
  {
    if (!Settings.commandPostReviewServer.isEmpty())
    {
Dprintf.dprintf("");
      File patchFile = null;
      try
      {
        // get patch file
        patchFile = getPatchFile(fileDataSet);
Dprintf.dprintf("");

        // create command
        Macro macro = new Macro(StringUtils.split(Settings.commandPostReviewServer,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS,false));
        macro.expand("server",     reviewServerHost);
        macro.expand("login",      reviewServerLogin);
        macro.expand("password",   (password != null) ? password : "");
        macro.expand("summary",    commitMessage.getSummary());
        macro.expand("groups",     reviewServerGroups);
        macro.expand("persons",    reviewServerPersons);
        macro.expand("description",commitMessage.getMessage());
        macro.expand("tests",      testSet,",");
        macro.expand("file",       patchFile.getAbsolutePath());
        String[] commandArray = macro.getValueArray();
    //for (String s : commandArray) Dprintf.dprintf("command=%s",s);
Dprintf.dprintf("");

        Exec exec = null;
        try
        {
          // execute
          exec = new Exec(commandArray);
          String line;
          while ((line = exec.getStdout()) != null)
          {
Dprintf.dprintf("stdout %s",line);
          }
          while ((line = exec.getStderr()) != null)
          {
Dprintf.dprintf("stderr %s",line);
          }
          int exitcode = exec.waitFor();
          if (exitcode != 0)
          {
            throw new RepositoryException("Cannot post patch to review server (exitcode: "+exitcode+")");
          }
          exec.done(); exec = null;
        }
        catch (IOException exception)
        {
          throw new RepositoryException(exception);
        }
        finally
        {
          if (exec != null) exec.done();
        }

        patchFile.delete(); patchFile = null;
      }
      finally
      {
        if (patchFile != null) patchFile.delete();
      }
    }
    else
    {
      throw new RepositoryException("No review server post command configured.");
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Repository {type: "+getType()+", root: "+rootPath+", selected: "+selected+"}";
  }

  //-----------------------------------------------------------------------

  /** parse date
   * @param string date/time string
   * @return date/time or current date/time
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

  /** get file names from file data map
   * @param fileDataMap file data map
   * @return file names array
   */
  protected String[] getFileDataNames(HashMap<String,FileData> fileDataMap)
  {
    ArrayList<String> fileNameList = new ArrayList<String>(fileDataMap.size());

    for (FileData fileData : fileDataMap.values())
    {
      fileNameList.add(fileData.name);
    }

    return fileNameList.toArray(new String[fileNameList.size()]);
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

  //-----------------------------------------------------------------------
}

/* end of file */
