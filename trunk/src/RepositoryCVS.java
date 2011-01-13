/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryCVS.java,v $
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/****************************** Classes ********************************/

/** Concurrent Versions System (CVS) repository
 */
class RepositoryCVS extends Repository
{
  // --------------------------- constants --------------------------------

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

  /** 
   * @param 
   * @return 
   */
  public void updateStates(HashSet<FileData> fileDataHashSet, HashSet<String> fileDirectoryHashSet, boolean addNewFlag)
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
    final Pattern PATTERN_TAGS                = Pattern.compile("^.*Existing Tags:.*",Pattern.CASE_INSENSITIVE);

    Exec            exec;                      
    String          line;                      
    Matcher         matcher;                   
    FileData        fileData;                  
    String          baseName           = null; 
    FileData.States state              = FileData.States.UNKNOWN;
    String          workingRevision    = "";
    String          repositoryRevision = "";
    String          branch             = "";
    HashSet<String> tags               = new HashSet<String>();
    for (String directory : fileDirectoryHashSet)
    {
      try
      {
        exec = new Exec(rootPath,"cvs status "+directory);
        while ((line = exec.readStdout()) != null)
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
            fileData = findFileData(fileDataHashSet,directory,baseName);
            if      (fileData != null)
            {
              fileData.state              = state;
              fileData.workingRevision    = workingRevision;
              fileData.repositoryRevision = repositoryRevision;
              fileData.branch             = branch;
            }
            else if (addNewFlag)
            {
              fileData = new FileData(baseName,
                                      directory+File.separator+baseName,
                                      FileData.Types.FILE
                                     );
//???
Dprintf.dprintf("");
            }

            baseName           = null;
            state              = FileData.States.UNKNOWN;
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

          // match tags
          if ((matcher = PATTERN_TAGS.matcher(line)).matches())
          {
            tags.clear();
          }
        }
        if (baseName != null)
        {
          fileData = findFileData(fileDataHashSet,directory,baseName);
          if      (fileData != null)
          {
            fileData.state              = state;
            fileData.workingRevision    = workingRevision;
            fileData.repositoryRevision = repositoryRevision;
            fileData.branch             = branch;
          }
          else if (addNewFlag)
          {
            fileData = new FileData(baseName,
                                    directory+File.separator+baseName,
                                    FileData.Types.FILE
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
    for (FileData fileData : fileDataHashSet)
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

  /** 
   * @param 
   * @return 
   */
  public void update(HashSet<FileData> fileDataHashSet)
    throws RepositoryException
  {
    try
    {
      StringBuffer command = new StringBuffer();
      int          exitCode;

      command.setLength(0);
      command.append("cvs update -d");
      if (Settings.cvsPruneEmtpyDirectories) command.append(" -P");
      command.append(" --");
      command.append(' ');
      command.append(getFileDataNames(fileDataHashSet));

      exitCode = new Exec(rootPath,command.toString()).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail (exit code: %d)",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** 
   * @param 
   * @return 
   */
  public void commit(HashSet<FileData> fileDataHashSet, Message commitMessage)
    throws RepositoryException
  {
    try
    {
      StringBuffer command = new StringBuffer();
      int          exitCode;

      command.setLength(0);
      command.append("cvs commit -F");
      command.append(' ');
      command.append(commitMessage.getFileName());
      command.append(" --");
      command.append(' ');
      command.append(getFileDataNames(fileDataHashSet));

      exitCode = new Exec(rootPath,command.toString()).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail (exit code: %d)",command.toString(),exitCode);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
  }

  /** 
   * @param 
   * @return 
   */
  public void add(HashSet<FileData> fileDataHashSet, Message commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    try
    {
      StringBuffer command = new StringBuffer();
      int          exitCode;

      command.setLength(0);
      command.append("cvs add");
      if (binaryFlag) command.append(" -k b");
      command.append(" --");
      command.append(' ');
      command.append(getFileDataNames(fileDataHashSet));

      exitCode = new Exec(rootPath,command.toString()).waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s' fail (exit code: %d)",command.toString(),exitCode);
      }

      if (commitMessage != null)
      {
        command.setLength(0);
        command.append("cvs commit -F");
        command.append(' ');
        command.append(commitMessage.getFileName());
        command.append(" --");
        command.append(' ');
        command.append(getFileDataNames(fileDataHashSet));

        exitCode = new Exec(rootPath,command.toString()).waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s' fail (exit code: %d)",command.toString(),exitCode);
        }
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(exception);
    }
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

  /** 
   * @param 
   * @return 
   */
  private FileData.States parseState(String string)
  {
    if      (string.equalsIgnoreCase("Up-to-date"      )) return FileData.States.OK;
    else if (string.equalsIgnoreCase("Locally Modified")) return FileData.States.MODIFIED;
    else if (string.equalsIgnoreCase("Needs Merge"     )) return FileData.States.MERGE;
    else if (string.equalsIgnoreCase("Conflict"        )) return FileData.States.CONFLICT;
    else if (string.equalsIgnoreCase("Removed"         )) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("Update"          )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs patch"     )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs update"    )) return FileData.States.UPDATE;
    else if (string.equalsIgnoreCase("Needs Checkout"  )) return FileData.States.CHECKOUT;
    else if (string.equalsIgnoreCase("Unknown"         )) return FileData.States.UNKNOWN;
    else if (string.equalsIgnoreCase("Entry invalid"   )) return FileData.States.REMOVED;
    else if (string.equalsIgnoreCase("Locally Added"   )) return FileData.States.ADDED;
    else                                                  return FileData.States.UNKNOWN;
  }
}

/* end of file */
