/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: Apache Subversion (SVN) repository
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/****************************** Classes ********************************/

/** Apache Subversion (SVN) repository
 */
@XmlType(propOrder={"userName","logPrefix"})
@XmlAccessorType(XmlAccessType.NONE)
class RepositorySVN extends Repository
{
  // --------------------------- constants --------------------------------
  public final static String[] DEFAULT_REVISION_NAMES = new String[]{"HEAD"};
  public final static String[] DEFAULT_BRANCH_NAMES   = new String[]{"trunk","tags","branches"};
  public final static String   DEFAULT_ROOT_NAME      = "trunk";
  public final static String   DEFAULT_BRANCH_NAME    = "branches";
  public final static String   FIRST_REVISION_NAME    = "1";
  public final static String   LAST_REVISION_NAME     = "HEAD";

  // --------------------------- variables --------------------------------
  private final static RepositorySVN staticInstance = new RepositorySVN();

  @XmlElement(name = "userName")
  @RepositoryValue(title = "User name:", tooltip="SVN server user login name.")
  public String userName = "";

  @XmlElement(name = "logPrefix")
  @RepositoryValue(title = "Log prefix:", defaultValue="/trunk", tooltip="Prefix of repository log path.")
  public String logPrefix;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get static instance
   * @return static instance
   */
  public final static RepositorySVN getInstance()
  {
    return staticInstance;
  }

  /** create repository
   * @param rootPath root path
   */
  RepositorySVN(String rootPath, String userName, PasswordHandler passwordHandler, String comment)
  {
    super(rootPath,passwordHandler,comment);
    this.userName = userName;
  }

  /** create repository
   */
  RepositorySVN()
  {
    super();
  }

  /** check if repository support lock/unlock
   * @return true iff lock/unlock is supported
   */
  public boolean supportLockUnlock()
  {
    return true;
  }

  /** get special commands for repository
   * @return array with special commands
   */
  public SpecialCommand[] getSpecialCommands()
  {
    return new SpecialCommand[]
    {
      new SpecialCommand("Cleanup")
      {
        public void run(BusyDialog busyDialog)
          throws RepositoryException
        {
          cleanup(this,busyDialog);
        }
      }
    };
  }

  /** create new repository module
   * @param repositoryURL repository server URL
   * @param moduleName module name
   * @param importPath import directory
   */
  public void create(String repositoryURL, String moduleName, String importPath)
    throws RepositoryException
  {
    final Pattern PATTERN_URI = Pattern.compile("^[^:/]+://.*",Pattern.CASE_INSENSITIVE);

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // get full repository path
      String path = (PATTERN_URI.matcher(repositoryURL).matches()
                       ? repositoryURL
                       : "file://"+repositoryURL
                    )+"/"+moduleName;

      // create repository
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("import",importPath,path,"-m","initial");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** checkout repository
   * @param repositoryURL repository server URL
   * @param moduleName module name
   * @param revision revision to checkout
   * @param userName user name or ""
   * @param destinationPath destination path
   * @param busyDialog busy dialog or null
   */
  public void checkout(String repositoryURL, String moduleName, String revision, String userName, String destinationPath, BusyDialog busyDialog)
    throws RepositoryException
  {
    final Pattern PATTERN_URI = Pattern.compile("^[^:/]+://.*",Pattern.CASE_INSENSITIVE);

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,repositoryURL) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // get full repository path
      String path = (PATTERN_URI.matcher(repositoryURL).matches()
                       ? repositoryURL
                       : "file://"+repositoryURL
                    )+"/"+moduleName;

      // checkout
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("checkout");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      if ((revision != null) && !revision.isEmpty()) command.append("--revision",revision);
      command.append(path,destinationPath);
      exec = new Exec(destinationPath,command);
      exec.closeStdin();

      // read output
      int n = destinationPath.length();
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && !exec.isTerminated()
            )
      {
        String line;

        // read stdout
        line = exec.pollStdout();
        if (line != null)
        {
//Dprintf.dprintf("out: %s",line);
          if (busyDialog != null) busyDialog.updateList(line);
        }
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          try { FileUtils.deleteDirectoryTree(destinationPath); } catch (IOException ignoredException) { /* ignored */ }
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }
      }
      else
      {
        // abort
        exec.destroy();
        try { FileUtils.deleteDirectoryTree(destinationPath); } catch (IOException ignoredException) { /* ignored */ }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      try { FileUtils.deleteDirectoryTree(destinationPath); } catch (IOException ignoredException) { /* ignored */ }
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** update file states
   * @param fileDataSet file data set to update
   * @param fileDirectorySet directory set to check for new/missing files
   * @param newFileDataSet new file data set or null
   */
  public void updateStates(HashSet<FileData> fileDataSet, HashSet<String> fileDirectorySet, HashSet<FileData> newFileDataSet)
    throws RepositoryException
  {
    final Pattern PATTERN_STATUS         = Pattern.compile("^(.).(.)..(.).\\s(.)\\s+(\\d+?)\\s+(\\d+?)\\s+(\\S+?)\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_UNKNOWN        = Pattern.compile("^\\?.......\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STATUS_AGAINST = Pattern.compile("^Status against revision:.*",Pattern.CASE_INSENSITIVE);

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Command command = new Command();
    for (String directory : fileDirectorySet)
    {
      Exec exec = null;
      try
      {
        // get status
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
        if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("status","-uvN","--xml");
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        command.append("--");
        exec = new Exec(rootPath,directory,command);
        exec.closeStdin();
//Dprintf.dprintf("command=%s directory=%s",command,directory);

        try
        {
          DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
          Document document = documentBuilder.parse(exec.getStdoutStream());
          document.getDocumentElement().normalize();

          NodeList entryList = document.getElementsByTagName("entry");
          NodeList nodeList;
          for (int z = 0; z < entryList.getLength(); z++)
          {
            Element entryElement = (Element)entryList.item(z);

            String          line;
            Matcher         matcher;
            String          name;
            String          path;
            String          fileName;
            FileData.States state              = FileData.States.UNKNOWN;
            boolean         locked             = false;
            String          workingRevision    = "";
            String          repositoryRevision = "";
            String          author             = "";

            name     = entryElement.getAttribute("path");
            fileName = (!directory.isEmpty() ? directory+File.separator : "")+name;
//Dprintf.dprintf("fileName=%s",fileName);

            nodeList = entryElement.getElementsByTagName("wc-status");
            if (nodeList.getLength() > 0)
            {
              Element wcStatusElement = (Element)nodeList.item(0);

              state = parseState(wcStatusElement);
//Dprintf.dprintf("state=%s %s",state,wcStatusElement.getAttribute("item"));
              workingRevision = wcStatusElement.getAttribute("revision");

              nodeList = wcStatusElement.getElementsByTagName("commit");
              if (nodeList.getLength() > 0)
              {
                Element commitElement = (Element)nodeList.item(0);

                repositoryRevision = commitElement.getAttribute("revision");

                nodeList = commitElement.getElementsByTagName("author");
                if (nodeList.getLength() > 0)
                {
                  Element authorElement = (Element)nodeList.item(0);
                  author = authorElement.getFirstChild().getNodeValue();
                }
              }
            }

            FileData fileData = findFileData(fileDataSet,fileName);
            if      (fileData != null)
            {
              fileData.state              = state;
              fileData.mode               = FileData.Modes.BINARY;
              fileData.mode               = FileData.Modes.BINARY;
              fileData.locked             = locked;
              fileData.repositoryRevision = repositoryRevision;
            }
            else if (   (newFileDataSet != null)
                     && !name.equals(".")
                     && !isHiddenFile(fileName)
                     && !isIgnoreFile(fileName)
                    )
            {
              // get file type, size, date/time
              File file = new File(rootPath,fileName);
              FileData.Types type     = getFileType(file);
              long           size     = file.length();
              Date           datetime = new Date(file.lastModified());

              // create file data
              newFileDataSet.add(new FileData(fileName,
                                              type,
                                              state,
                                              FileData.Modes.BINARY,
                                              locked,
                                              size,
                                              datetime,
                                              workingRevision,
                                              repositoryRevision
                                             )
                                );
            }
//Dprintf.dprintf("fileName=%s",fileName);
//Dprintf.dprintf("state=%s",state);
//Dprintf.dprintf("workingRevision=%s",workingRevision);
//Dprintf.dprintf("repositoryRevision=%s",repositoryRevision);
//Dprintf.dprintf("author=%s",author);
//Dprintf.dprintf("");
          }
        }
        catch (org.xml.sax.SAXException exception)
        {
          // ignored
        }
        catch (javax.xml.parsers.ParserConfigurationException exception)
        {
          // ignored
        }

        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
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

      // add unknown files
      if (newFileDataSet != null)
      {
        for (FileData fileData : listFiles(directory))
        {
          String fileName = fileData.getFileName();
          if (   !containFileData(fileDataSet,fileData)
              && !fileName.equals(".")
              && !isHiddenFile(fileName)
              && !isIgnoreFile(fileName)
             )
          {
            newFileDataSet.add(fileData);
          }
        }
      }
    }
  }

  /** get repository type
   * @return repository type
   */
  public Types getType()
  {
    return Types.SVN;
  }

  /** get repository root URL
   * @return repository root URL
   */
  public String getRepositoryRootURL()
  {
    final Pattern PATTERN_REPOSITORTY_URL = Pattern.compile("^Repository Root:\\s*(.+)",Pattern.CASE_INSENSITIVE);

    String repositoryURL = "";

    Exec exec = null;
    try
    {
      // get info
      Command command = new Command();
      String  line;
      Matcher matcher;

      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      command.append("info");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse info output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_REPOSITORTY_URL.matcher(line)).matches())
        {
          repositoryURL = matcher.group(1);
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

    return repositoryURL;
  }

  /** get repository URL
   * @return repository URL
   */
  public String getRepositoryURL()
  {
    final Pattern PATTERN_REPOSITORTY_URL = Pattern.compile("^URL:\\s*(.+)",Pattern.CASE_INSENSITIVE);

    String repositoryURL = "";

    Exec exec = null;
    try
    {
      // get info
      Command command = new Command();
      String  line;
      Matcher matcher;

      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      command.append("info");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse info output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_REPOSITORTY_URL.matcher(line)).matches())
        {
          repositoryURL = matcher.group(1);
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

    return repositoryURL;
  }

  /** get first revision name
   * @return first revision name
   */
  public String getFirstRevision()
  {
    return FIRST_REVISION_NAME;
  }

  /** get last revision name
   * @return last revision name
   */
  public String getLastRevision()
  {
    return LAST_REVISION_NAME;
  }

  /** get revision names of file
   * @param name file name or URL
   * @return array with revision names
   */
  public String[] getRevisionNames(String name)
    throws RepositoryException
  {
    final Pattern PATTERN_REVSION = Pattern.compile("^r(\\d+).*",Pattern.CASE_INSENSITIVE);

    ArrayList<String> revisionList = new ArrayList<String>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      // get revision info list
      Command command = new Command();
      String  line;
      Matcher matcher;

      // get log
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("log","-r","HEAD:0","--verbose");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (name != null) command.append(name);
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse revisions in log output
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_REVSION.matcher(line)).matches())
        {
          // add log info entry
          revisionList.add(matcher.group(1));
        }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert to array and sort
    String[] revisions = revisionList.toArray(new String[revisionList.size()]);
    Arrays.sort(revisions,new Comparator<String>()
    {
      public int compare(String revision1, String revision2)
      {
        int number1 = Integer.parseInt(revision1);
        int number2 = Integer.parseInt(revision2);
        if      (number1 < number2)
        {
          return -1;
        }
        else if (number1 > number2)
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

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      // get revision data
      Command command = new Command();

      // get single log entry
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("log","-r",revision+":PREV","--verbose");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (fileData != null) command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        revisionData = parseLogData(exec);
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
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
    LinkedList<RevisionData> revisionDataList = new LinkedList<RevisionData>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      // get revision info list
      Command command = new Command();

      // get log
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("log","-r","HEAD:0","--verbose");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionData revisionData;
        while ((revisionData = parseLogData(exec)) != null)
        {
          // add revision info entry
          revisionDataList.addFirst(revisionData);
        }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
//for (RevisionDataSVN revisionData : revisionDataList) Dprintf.dprintf("revisionData=%s",revisionData);
    finally
    {
      if (exec != null) exec.done();
    }

    // create revision data tree (=list of revisions)
    return revisionDataList.toArray(new RevisionData[revisionDataList.size()]);
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

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      String  line;

      // get file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("cat");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      if (revision != null) command.append("--revision",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read file data
      while ((line = exec.getStdout()) != null)
      {
        lineList.add(line);
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
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

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get file data
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("cat");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      if (revision != null) command.append("--revision",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command,true);
      exec.closeStdin();

      // read file bytes into byte array stream
      while ((n = exec.readStdout(buffer)) > 0)
      {
        output.write(buffer,0,n);
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert byte array stream into array
    return output.toByteArray();
  }

  /** get all changed/unknown files
   * @param stateSet state set
   * @return fileDataSet file data set with modified files
   */
  public HashSet<FileData> getChangedFiles(EnumSet<FileData.States> stateSet)
    throws RepositoryException
  {
    final Pattern PATTERN_STATUS         = Pattern.compile("^(.).(.)..(.).\\s(.)\\s+(\\d+?)\\s+(\\d+?)\\s+(\\S+?)\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_UNKNOWN        = Pattern.compile("^\\?.......\\s+(.*?)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_STATUS_AGAINST = Pattern.compile("^Status against revision:.*",Pattern.CASE_INSENSITIVE);

    HashSet<FileData> fileDataSet = new HashSet<FileData>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command         command            = new Command();
      String          line;
      Matcher         matcher;
      String          name               = null;
      FileData.States state              = FileData.States.UNKNOWN;
      boolean         locked             = false;
      String          workingRevision    = "";
      String          repositoryRevision = "";
      String          author             = "";

      // get list of files which may be updated or which are locally changed
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("status","-uv");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read list
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        // match name, state
        if      ((matcher = PATTERN_STATUS.matcher(line)).matches())
        {
          state              = parseState(matcher.group(1),matcher.group(4));
          locked             = matcher.group(2).equals("K") || matcher.group(3).equals("L");
          workingRevision    = matcher.group(5);
          repositoryRevision = matcher.group(6);
          author             = matcher.group(7);
          name               = matcher.group(8);

          File file = new File(rootPath,name);
          if (   !isIgnoreFile(file)
              && file.isFile()
              && stateSet.contains(state)
             )
          {
            fileDataSet.add(new FileData(name,
                                         state,
                                         FileData.Modes.BINARY,
                                         locked
                                        )
                           );
          }
        }
        else if ((matcher = PATTERN_UNKNOWN.matcher(line)).matches())
        {
          name = matcher.group(1);

          File file = new File(rootPath,name);
          if (   !isIgnoreFile(file)
              && file.isFile()
              && stateSet.contains(FileData.States.UNKNOWN)
             )
          {
            fileDataSet.add(new FileData(name,FileData.States.UNKNOWN));
          }
        }
        else if (PATTERN_STATUS_AGAINST.matcher(line).matches())
        {
          // ignore "Pattern status against:..."
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for changed files line '%s'",line);
        }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return fileDataSet;
  }

  /** get diff of file
   * @param fileData file data
   * @param revision revision to get diff for
   * @return diff data
   */
  public DiffData[] getDiff(FileData fileData, String oldRevision, String newRevision)
    throws RepositoryException
  {
    final Pattern PATTERN_DIFF_START  = Pattern.compile("^\\+\\+\\+.*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_DIFF        = Pattern.compile("^@@\\s+\\-([\\d,]*)\\s+\\+([\\d,]*)\\s+@@$",Pattern.CASE_INSENSITIVE);

    ArrayList<DiffData> diffDataList = new ArrayList<DiffData>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      String  line;

      String[] newFileLines = null;
      if (newRevision != null)
      {
        // check out new revision
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
        if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("cat","--revision",newRevision);
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        command.append("--");
        if (fileData != null) command.append(getFileDataName(fileData));
        exec = new Exec(rootPath,command);
        exec.closeStdin();

        // read content
        ArrayList<String> newFileLineList = new ArrayList<String>();
        while ((line = exec.getStdout()) != null)
        {
          newFileLineList.add(line);
        }

        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;

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
          throw new RepositoryException(Onzen.reniceIOException(exception));
        }
      }

      // diff file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("diff","--revision",((oldRevision != null) ? oldRevision : getLastRevision())+((newRevision != null) ? ":"+newRevision : ""));
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (fileData != null) command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // skip diff header
      while ((line = exec.getStdout()) != null)
      {
        if (PATTERN_DIFF_START.matcher(line).matches()) break;
      }

      /* parse diff output
           Format:
             @@ -<i> +<j> @@
      */
      int               lineNb = 1;
      DiffData          diffData;
      ArrayList<String> keepLinesList = new ArrayList<String>();
      ArrayList<String> addedLinesList = new ArrayList<String>();
      ArrayList<String> deletedLinesList = new ArrayList<String>();
      String[]          lines;
      while ((line = exec.getStdout()) != null)
      {
//Dprintf.dprintf("line=%s",line);
        if      ((matcher = PATTERN_DIFF.matcher(line)).matches())
        {
          int[] oldIndex = parseDiffIndex(matcher.group(1));
          int[] newIndex = parseDiffIndex(matcher.group(2));
//Dprintf.dprintf("oldIndex=%d,%d",oldIndex[0],oldIndex[1]);
//Dprintf.dprintf("newIndex=%d,%d",newIndex[0],newIndex[1]);

          while (   ((line = exec.getStdout()) != null)
                 && !line.startsWith("@@")
                )
          {
            exec.ungetStdout(line);
//Dprintf.dprintf("line=%s",line);

            // get keep lines
            keepLinesList.clear();
            while ((lineNb < newIndex[0]) && (lineNb <= newFileLines.length))
            {
              keepLinesList.add(newFileLines[lineNb-1]);
              lineNb++;
            }
            while (   ((line = exec.getStdout()) != null)
                   && (line.isEmpty() || line.startsWith(" "))
                  )
            {
              keepLinesList.add(newFileLines[lineNb-1]);
              lineNb++;
            }
            exec.ungetStdout(line);
            diffData = new DiffData(DiffData.Types.KEEP,keepLinesList);
            diffDataList.add(diffData);

            // get deleted lines
            deletedLinesList.clear();
            while (   ((line = exec.getStdout()) != null)
                   && line.startsWith("-")
                  )
            {
              deletedLinesList.add(line.substring(1));
            }
            exec.ungetStdout(line);

            // get added lines
            addedLinesList.clear();
            while (   ((line = exec.getStdout()) != null)
                   && line.startsWith("+")
                  )
            {
              addedLinesList.add(line.substring(1));
              lineNb++;
            }
            exec.ungetStdout(line);

            int deletedLinesCount = deletedLinesList.size();
            int addedLinesCount   = addedLinesList.size();
            if      (deletedLinesCount < addedLinesCount)
            {
              // changed
              if (deletedLinesCount > 0)
              {
                lines = new String[deletedLinesCount];
                for (int z = 0; z < deletedLinesCount; z++)
                {
                  lines[z] = addedLinesList.get(z);
                }
                diffData = new DiffData(DiffData.Types.CHANGED,lines,deletedLinesList);
                diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",lines.length,deletedLinesCount);
              }

              // added
              lines = new String[addedLinesCount-deletedLinesCount];
              for (int z = 0; z < addedLinesCount-deletedLinesCount; z++)
              {
                 lines[z] = addedLinesList.get(deletedLinesCount+z);
              }
              diffData = new DiffData(DiffData.Types.ADDED,lines);
              diffDataList.add(diffData);
//Dprintf.dprintf("a %d",lines.length);
            }
            else if (deletedLinesCount > addedLinesCount)
            {
              // changed
              if (addedLinesCount > 0)
              {
                lines = new String[addedLinesCount];
                for (int z = 0; z < addedLinesCount; z++)
                {
                  lines[z] = deletedLinesList.get(z);
                }
                diffData = new DiffData(DiffData.Types.CHANGED,addedLinesList,lines);
                diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",addedLinesCount,lines.length);
              }

              // deleted
              lines = new String[deletedLinesCount-addedLinesCount];
              for (int z = 0; z < deletedLinesCount-addedLinesCount; z++)
              {
                 lines[z] = deletedLinesList.get(addedLinesCount+z);
              }
              diffData = new DiffData(DiffData.Types.DELETED,lines);
              diffDataList.add(diffData);
//Dprintf.dprintf("d %d",lines.length);
            }
            else if ((deletedLinesCount > 0) && (addedLinesCount > 0))
            {
              // changed
              diffData = new DiffData(DiffData.Types.CHANGED,addedLinesList,deletedLinesList);
              diffDataList.add(diffData);
//Dprintf.dprintf("c %d %d",addedLinesCount,deletedLinesCount);
            }
            else
            {
              // unknown -> stop
              break;
            }
          }
          exec.ungetStdout(line);
        }
        else if (line.startsWith("Property "))
        {
          // skip "Property ..."
          do
          {
            lineNb++;
          }
          while (   ((line = exec.getStdout()) != null)
                 && !line.startsWith("@@")
                );
          exec.ungetStdout(line);
        }
        else
        {
          // unknown line
          Onzen.printWarning("No match for diff line '%s'",line);
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

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
/*
int lineNb=1;
for (DiffData d : diffDataList)
{
Dprintf.dprintf("%s %d: %d %d %d",d.blockType,
lineNb,
(d.keepLines!=null)?d.keepLines.length:0,
(d.addedLines!=null)?d.addedLines.length:0,
(d.deletedLines!=null)?d.deletedLines.length:0
);
if (d.blockType==DiffData.Types.CHANGED) for (int z = 0; z < d.deletedLines.length; z++) Dprintf.dprintf("%s -----> %s",d.deletedLines[z],d.addedLines[z]);
if (d.blockType==DiffData.Types.KEEP) lineNb += d.keepLines.length;
if (d.blockType==DiffData.Types.ADDED) lineNb += d.addedLines.length;
}
*/

    return diffDataList.toArray(new DiffData[diffDataList.size()]);
  }

  /** get patch lines for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @param ignoreWhitespaces true to ignore white spaces
   * @param output patch output or null
   * @param lineList patch data lines or null
   */
  public void getPatch(HashSet<FileData> fileDataSet, String revision1, String revision2, boolean ignoreWhitespaces, PrintWriter output, ArrayList<String> lineList)
    throws RepositoryException
  {
    final Pattern PATTERN_OLD_FILE = Pattern.compile("^\\-\\-\\-\\s+(.*)",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_NEW_FILE = Pattern.compile("^\\+\\+\\+\\s+(.*)",Pattern.CASE_INSENSITIVE);

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    // get existing/new files
    HashSet<FileData> existFileDataSet = new HashSet<FileData>();
    HashSet<FileData> newFileDataSet   = new HashSet<FileData>();
    if (fileDataSet != null)
    {
      for (FileData fileData : fileDataSet)
      {
        if (fileData.state != FileData.States.UNKNOWN)
        {
          existFileDataSet.add(fileData);
        }
        else
        {
          newFileDataSet.add(fileData);
        }
      }
    }

    // get patch for existing files
    if ((fileDataSet == null) || (existFileDataSet.size() > 0))
    {
      Exec exec = null;
      try
      {
        Command command = new Command();
        String  line;

        // get patch
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
        if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("diff");
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        if (!Settings.svnDiffCommand.isEmpty())
        {
          // use external diff command
          command.append("--diff-cmd",Settings.svnDiffCommand);
          if (ignoreWhitespaces)
          {
            if (!Settings.svnDiffCommandOptionsIgnoreWhitespaces.isEmpty())
            {
              command.append("-x",Settings.svnDiffCommandOptionsIgnoreWhitespaces);
            }
          }
          else
          {
            if (!Settings.svnDiffCommandOptions.isEmpty())
            {
              command.append("-x",Settings.svnDiffCommandOptions);
            }
          }
        }
        else
        {
          // use internal diff
          if (ignoreWhitespaces)
          {
            command.append("-x","-ubw");
          }
          else
          {
            command.append("-x","-u");
          }
        }
        command.append("--revision",((revision1 != null) ? revision1 : getLastRevision())+((revision2 != null) ? ":"+revision2 : ""));
        command.append("--");
        if (fileDataSet != null) command.append(getFileDataNames(existFileDataSet));
        exec = new Exec(rootPath,command);
        exec.closeStdin();

        // read patch data
        Matcher matcher;
        while ((line = exec.getStdout()) != null)
        {
          // fix +++/--- lines: strip out first part in name, e. g. "a/"/"b/", check if absolute path and convert
          if      ((matcher = PATTERN_OLD_FILE.matcher(line)).matches())
          {
            String fileName = matcher.group(1);
            if (fileName.startsWith(rootPath))
            {
              // absolute path -> convert to relative path
              fileName = matcher.group(1).substring(rootPath.length()+1);
            }
            else
            {
              // relative path -> strip out first path in name
              int index = fileName.indexOf(File.separator);
              fileName = (index >= 0) ? fileName.substring(index+1) : fileName;
            }
            line = "--- "+fileName;
          }
          else if ((matcher = PATTERN_NEW_FILE.matcher(line)).matches())
          {
            String fileName = matcher.group(1);
            if (fileName.startsWith(rootPath))
            {
              // absolute path -> convert to relative path
              fileName = matcher.group(1).substring(rootPath.length()+1);
            }
            else
            {
              // relative path -> strip out first path in name
              int index = fileName.indexOf(File.separator);
              fileName = (index >= 0) ? fileName.substring(index+1) : fileName;
            }
            line = "+++ "+fileName;
          }

          if      (output   != null) output.println(line);
          else if (lineList != null) lineList.add(line);
        }

        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
      finally
      {
        if (exec != null) exec.done();
      }
    }

    // get complete patches for new files
    for (FileData fileData : newFileDataSet)
    {
      try
      {
        BufferedReader bufferedReader;
        String         line;

        // count number of lines in file
        int lineCount = 0;
        bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));
        while ((line = bufferedReader.readLine()) != null)
        {
          lineCount++;
        }
        bufferedReader.close();

        // add as patch
        bufferedReader = new BufferedReader(new FileReader(fileData.getFileName(rootPath)));
        String dateString = DateFormat.getDateInstance().format(new Date());
        if      (output   != null)
        {
          output.println(String.format("diff -u %s",fileData.getFileName()));
          output.println(String.format("--- /dev/null\t%s",dateString));
          output.println(String.format("+++ %s\t%s",fileData.getFileName(),dateString));
          output.println(String.format("@@ -1,%d +1,%d @@",lineCount,lineCount));
          while ((line = bufferedReader.readLine()) != null)
          {
            output.println("+"+line);
          }
        }
        else if (lineList != null)
        {
          lineList.add(String.format("diff -u %s",fileData.getFileName()));
          lineList.add(String.format("--- /dev/null\t%s",dateString));
          lineList.add(String.format("+++ %s\t%s",fileData.getFileName(),dateString));
          lineList.add(String.format("@@ -1,%d +1,%d @@",lineCount,lineCount));
          while ((line = bufferedReader.readLine()) != null)
          {
            lineList.add("+"+line);
          }
        }
        bufferedReader.close();
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
    }
  }

  /** get patch data for file
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions to get patch for
   * @return patch data bytes
   */
  public byte[] getPatchBytes(HashSet<FileData> fileDataSet, String revision1, String revision2)
    throws RepositoryException
  {
    ByteArrayOutputStream output = new ByteArrayOutputStream(64*1024);

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     n;
      byte[]  buffer  = new byte[64*1024];

      // get patch
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("diff","--revision",((revision1 != null) ? revision1 : getLastRevision())+((revision2 != null) ? ":"+revision2 : ""));
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (fileDataSet != null) command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read patch bytes into byte array stream
      while ((n = exec.readStdout(buffer)) > 0)
      {
        output.write(buffer,0,n);
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    // convert byte array stream into array
    return output.toByteArray();
  }

  /** get log to file
   * @param fileData file data
   * @return log array
   */
  public LogData[] getLog(FileData fileData)
    throws RepositoryException
  {
    ArrayList<LogData> logDataList = new ArrayList<LogData>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      // get revision info list
      HashMap<String,String> symbolicNamesMap = new HashMap<String,String>();
      Command                command = new Command();

      // get log
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("log","-r","HEAD:0","--verbose");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (fileData != null) command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // parse header
      if (parseLogHeader(exec))
      {
        // parse data
        RevisionData revisionData;
        while ((revisionData = parseLogData(exec)) != null)
        {
          // add log info entry
          logDataList.add(new LogData(revisionData.revision,
                                      revisionData.date,
                                      revisionData.author,
                                      revisionData.commitMessage,
                                      revisionData.fileNames
                                     )
                         );
        }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (Exception exception)
    {
      throw new RepositoryException(exception);
    }
    finally
    {
      if (exec != null) exec.done();
    }
  //for (LogData logData : logDataList) Dprintf.dprintf("logData=%s",logData);

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
    final Pattern PATTERN_ANNOTATION = Pattern.compile("^\\s*(\\d+)\\s+(\\S+)\\s+(\\S+\\s+\\S+\\s+\\S+)\\s+\\([^\\)]*\\)\\s(.*?)\\s*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_EMPTY      = Pattern.compile("^\\s*",Pattern.CASE_INSENSITIVE);

    ArrayList<AnnotationData> annotationDataList = new ArrayList<AnnotationData>();

    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      Matcher matcher;
      String  line;

      // get annotations
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("blame","-v");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      if (revision != null) command.append("-r",revision);
      command.append("--");
      command.append(getFileDataName(fileData));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

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
          Onzen.printWarning("No match for annotations line '%s'",line);
        }
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }

    return annotationDataList.toArray(new AnnotationData[annotationDataList.size()]);
  }

  /** update file from respository
   * @param fileDataSet file data set or null
   * @param busyDialog busy dialog or null
   */
  public void update(HashSet<FileData> fileDataSet, BusyDialog busyDialog)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // update files
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("update","--accept","postpone");
      if (fileDataSet != null) command.append("--depth","immediates");
//      command.append(Settings.svnCommand,"--non-interactive","merge","--dry-run","-r","BASE:HEAD");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      if (fileDataSet != null) command.append(getFileDataNames(fileDataSet));
//      command.append(".");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read output
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && !exec.isTerminated()
            )
      {
        String line;

        line = exec.pollStdout();
        if (line != null)
        {
          if (busyDialog != null) busyDialog.updateList(line);
        }
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }
      }
      else
      {
        // abort
        exec.destroy();
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** commit files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void commit(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // commit files
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("commit","-F",commitMessage.getFileName());
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** add files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   * @param binaryFlag true to add file as binary files, false otherwise
   */
  public void add(HashSet<FileData> fileDataSet, CommitMessage commitMessage, boolean binaryFlag)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // add files
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("add");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // immediate commit when message is given
      if (commitMessage != null)
      {
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** remove files
   * @param fileDataSet file data set
   * @param commitMessage commit message
   */
  public void remove(HashSet<FileData> fileDataSet, CommitMessage commitMessage)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      // remove from repository
      Command command = new Command();

      // remove files
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("remove");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // immediate commit when message is given
      if (commitMessage != null)
      {
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** copy files
   * @param fileDataSet files to copy
   * @param destination destination
   * @param commitMessage commit message
   */
  public void copy(HashSet<FileData> fileDataSet, String destination, CommitMessage commitMessage)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // copy file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("copy");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      command.append(destination);
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // commit
      if (commitMessage != null)
      {
        fileDataSet.add(new FileData(destination));
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** rename file
   * @param fileData file data to rename
   * @param newName new name
   * @param commitMessage commit message
   */
  public void rename(FileData fileData, String newName, CommitMessage commitMessage)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // copy file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("rename");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(fileData.getFileName());
      command.append(newName);
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      // commit
      if (commitMessage != null)
      {
        HashSet<FileData> fileDataSet = FileData.toSet(getFileDataName(fileData),
                                                       (!rootPath.isEmpty()) ? rootPath+File.separator+newName : newName
                                                      );
        commit(fileDataSet,commitMessage);
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** revert files
   * @param fileDataSet file data set
   * @param revision revision to revert to
   * @param recursive true for recursive revert, false otherwise
   */
  public void revert(HashSet<FileData> fileDataSet, String revision, boolean recursive)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();
      int     exitCode;

      // delete local files
      for (FileData fileData : fileDataSet)
      {
        new File(fileData.getFileName(rootPath)).delete();
      }

      // revert files
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("revert");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      if (recursive) command.append("-R");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;

      if (revision != null)
      {
        // update to specifc revision
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("update","-r",revision);
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        command.append("--");
        command.append(getFileDataNames(fileDataSet));
        exec = new Exec(rootPath,command);
        exec.closeStdin();

        // wait for termination
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** set conflicts resolved
   * @param fileDataSet file data set or null for all files
   */
  public void resolve(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && (userName != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // copy file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("resolve","--accept","working");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
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

  /** get outgoing changes list
   */
  public String[] getChanges(String revision)
    throws RepositoryException
  {
    return null;
  }

  /** pull changes
   * @param masterRepositoryPath master repository path
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pullChanges(String masterRepositoryPath, String moduleName, String userName, String password)
    throws RepositoryException
  {
  }

  /** push changes
   * @param masterRepositoryPath master repository path
   * @param moduleName module name
   * @param userName user name or ""
   * @param password password or ""
   */
  public void pushChanges(String masterRepositoryPath, String moduleName, String userName, String password)
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
    String password = ((passwordHandler != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // copy file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("lock");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  /** unlock files
   * @param fileDataSet file data set
   */
  public void unlock(HashSet<FileData> fileDataSet)
    throws RepositoryException
  {
    String password = ((passwordHandler != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // copy file
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("unlock");
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("--");
      command.append(getFileDataNames(fileDataSet));
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
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
   * @return default root name
   */
  public String getDefaultRootName()
  {
    return DEFAULT_ROOT_NAME;
  }

  /** get default branch/tag name
   * @return default branch/tag name
   */
  public String getDefaultBranchTagName()
  {
    return DEFAULT_BRANCH_NAME+"/";
  }

  /** get names of existing branches/tags
   * @param pathName path name
   * @return array with branch/tag names
   */
  public String[] getBranchTagNames(String pathName)
    throws RepositoryException
  {
    HashSet<String> branchTagNameSet = new HashSet<String>();

    String password = ((passwordHandler != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    for (String branchName : DEFAULT_BRANCH_NAMES)
    {
      branchTagNameSet.add(branchName);
    }
    if (!pathName.isEmpty())
    {
      Exec exec = null;
      try
      {
        Command command = new Command();
        String  line;
        int     exitCode;

        // list branches
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
        if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("list",pathName+"/branches");
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        command.append("--");
        exec = new Exec(rootPath,command);
        exec.closeStdin();

        // read output
        while ((line = exec.getStdout()) != null)
        {
          branchTagNameSet.add("branches/"+StringUtils.trimEnd(line,"/\\"));
        }

        // wait for termination
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;

        // list tags
        command.clear();
        command.append(Settings.svnCommand,"--non-interactive");
        if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
        if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
        command.append("list",pathName+"/tags");
        if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
        command.append("--");
        exec = new Exec(rootPath,command);
        exec.closeStdin();

        // read output
        while ((line = exec.getStdout()) != null)
        {
          branchTagNameSet.add("tags/"+StringUtils.trimEnd(line,"/\\"));
        }

        // wait for termination
        exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }

        // done
        exec.done(); exec = null;
      }
      catch (IOException exception)
      {
        throw new RepositoryException(Onzen.reniceIOException(exception));
      }
      finally
      {
        if (exec != null) exec.done();
      }
    }

    // convert to array and sort
    String[] branchTagNames = branchTagNameSet.toArray(new String[branchTagNameSet.size()]);
    Arrays.sort(branchTagNames);

    return branchTagNames;
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
    String repositoryURL     = getRepositoryURL();
    String repositoryRootURL = getRepositoryRootURL();

    String password = ((passwordHandler != null) && !userName.isEmpty()) ? passwordHandler.getPassword(userName,getRepositoryRootURL()) : null;

    Exec exec = null;
    try
    {
      Command command = new Command();

      // create branch
      command.clear();
      command.append(Settings.svnCommand,"--non-interactive");
      if ((userName != null) && !userName.isEmpty()) command.append("--username",userName);
      if ((password != null) && !password.isEmpty()) command.append("--password",command.hidden(password));
      command.append("copy",repositoryURL+"/"+rootName,repositoryRootURL+"/"+branchName);
      if (Settings.svnAlwaysTrustServerCertificate) command.append("--trust-server-cert");
      command.append("-F",commitMessage.getFileName());
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read output
      String line;
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && ((line = exec.getStdout()) != null)
            )
      {
//Dprintf.dprintf("line=%s",line);
        if (busyDialog != null) busyDialog.updateText(line);
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }
      }
      else
      {
        exec.destroy();
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }

  //-----------------------------------------------------------------------

  /** parse SVN state string
   * @param stateFlag state flag string
   * @param updateFlag update flag string
   * @return state
   */
  private FileData.States parseState(String stateFlag, String updateFlag)
  {
    if      (stateFlag.equalsIgnoreCase(" "))
    {
      if (updateFlag.equalsIgnoreCase("*"))
      {
        return FileData.States.UPDATE;
      }
      else
      {
        return FileData.States.OK;
      }
    }
    else if (stateFlag.equalsIgnoreCase("A")) return FileData.States.ADDED;
    else if (stateFlag.equalsIgnoreCase("C")) return FileData.States.CONFLICT;
    else if (stateFlag.equalsIgnoreCase("D")) return FileData.States.REMOVED;
    else if (stateFlag.equalsIgnoreCase("M"))
    {
      if (updateFlag.equalsIgnoreCase("*"))
      {
        return FileData.States.MERGE;
      }
      else
      {
        return FileData.States.MODIFIED;
      }
    }
    else if (stateFlag.equalsIgnoreCase("!")) return FileData.States.CHECKOUT;
    else                                      return FileData.States.UNKNOWN;
  }

  /** parse SVN XML state string
   * @param element status element
   * @return state
   */
  private FileData.States parseState(Element element)
  {
    String item           = element.getAttribute("item");
    String treeConflicted = element.getAttribute("tree-conflicted");

    if      (item.equalsIgnoreCase("normal")           ) return FileData.States.OK;
    else if (item.equalsIgnoreCase("update")           ) return FileData.States.UPDATE;
    else if (item.equalsIgnoreCase("modified")         ) return FileData.States.MODIFIED;
    else if (item.equalsIgnoreCase("added")            ) return FileData.States.ADDED;
    else if (item.equalsIgnoreCase("conflict")         ) return FileData.States.CONFLICT;
    else if (   item.equalsIgnoreCase("conflicted")
             || treeConflicted.equalsIgnoreCase("true")) return FileData.States.CONFLICT;
    else if (item.equalsIgnoreCase("removed")          ) return FileData.States.REMOVED;
    else if (item.equalsIgnoreCase("deleted")          ) return FileData.States.REMOVED;
    else if (item.equalsIgnoreCase("merge")            ) return FileData.States.MERGE;
    else if (item.equalsIgnoreCase("checkout")         ) return FileData.States.CHECKOUT;
    else if (item.equalsIgnoreCase("missing")          ) return FileData.States.CHECKOUT;
    else if (item.equalsIgnoreCase("unversioned")      ) return FileData.States.UNKNOWN;
    else                                                 return FileData.States.UNKNOWN;
  }

  /** parse SVN diff index
   * @param string diff index string
   * @return index array [lineNb,length] or null
   */
  private int[] parseDiffIndex(String string)
  {
    Object[] data = new Object[2];
    if      (StringParser.parse(string,"%d,%d",data))
    {
      return new int[]{(Integer)data[0],(Integer)data[1]-(Integer)data[0]-1};
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
   * @return true iff header parsed
   */
  private boolean parseLogHeader(Exec exec)
    throws IOException
  {
    // parse header
    boolean headerDone = false;
    String  line;
    while (   !headerDone
           && ((line = exec.getStdout()) != null)
          )
    {
      if (line.startsWith("-----"))
      {
        headerDone = true;
      }
    }

    return headerDone;
  }

  /** parse log data
   * @param exec exec command
   * @param symbolicNamesMap symbolic names map
   * @return revision info or null
   */
  private RevisionData parseLogData(Exec exec)
    throws IOException
  {
    final Pattern PATTERN_REVISION = Pattern.compile("^r(\\d+)\\s*\\|\\s*(\\S*)\\s*\\|\\s*(\\S*\\s+\\S*\\s+\\S*).*",Pattern.CASE_INSENSITIVE);
    final Pattern PATTERN_FILE     = Pattern.compile("^\\s*.\\s+(.+)\\s*",Pattern.CASE_INSENSITIVE);

    RevisionData       revisionData      = null;

    boolean            dataDone          = false;
    Matcher            matcher;
    String             line;
    String             revision          = null;
    Date               date              = null;
    String             author            = null;
    ArrayList<String>  fileNameList      = new ArrayList<String>();
    LinkedList<String> commitMessageList = new LinkedList<String>();
    while (   !dataDone
           && ((line = exec.getStdout()) != null)
          )
    {
//Dprintf.dprintf("line=%s",line);
      if      (line.startsWith("-----"))
      {
        dataDone = true;
      }
      else if ((matcher = PATTERN_REVISION.matcher(line)).matches())
      {
        // revision
        revision = matcher.group(1);
        author   = matcher.group(2);
        date     = parseDate(matcher.group(3));

        // skip line "Changed paths:"
        exec.getStdout();

        // get files
        while (   ((line = exec.getStdout()) != null)
               && (matcher = PATTERN_FILE.matcher(line)).matches()
              )
        {
          String fileName = matcher.group(1);
          if ((logPrefix != null) && fileName.startsWith(logPrefix)) fileName = fileName.substring(logPrefix.length());
          fileNameList.add(fileName);
        }

        // get commit message lines
        commitMessageList.clear();
        while (   ((line = exec.getStdout()) != null)
               && !line.startsWith("-----")
              )
        {
          commitMessageList.add(line);
        }
        while (   (commitMessageList.peekFirst() != null)
               && commitMessageList.peekFirst().trim().isEmpty()
              )
        {
          commitMessageList.removeFirst();
        }
        while (   (commitMessageList.peekLast() != null)
               && commitMessageList.peekLast().trim().isEmpty()
              )
        {
          commitMessageList.removeLast();
        }

        // add log info entry
        revisionData = new RevisionData(revision,
                                        (String)null,
                                        date,
                                        author,
                                        commitMessageList,
                                        fileNameList
                                       );
        dataDone = true;
      }
    }
//Dprintf.dprintf("revisionData=%s",revisionData);

    return revisionData;
  }

  /** cleanup command
   * @param specialCommand special command
   * @param busyDialog busy dialog or null
   */
  private void cleanup(SpecialCommand specialCommand, BusyDialog busyDialog)
    throws RepositoryException
  {
    final Pattern PATTERN_URI = Pattern.compile("^[^:/]+://.*",Pattern.CASE_INSENSITIVE);

    Exec exec = null;
    try
    {
      Command command = new Command();

      // cleanup
      command.append(Settings.svnCommand,"cleanup");
      exec = new Exec(rootPath,command);
      exec.closeStdin();

      // read output
      while (   ((busyDialog == null) || !busyDialog.isAborted())
             && !exec.isTerminated()
            )
      {
        String line;

        // read stdout
        line = exec.pollStdout();
        if (line != null)
        {
//Dprintf.dprintf("out: %s",line);
          if (busyDialog != null) busyDialog.updateList(line);
        }
      }
      if ((busyDialog == null) || !busyDialog.isAborted())
      {
        // wait for termination
        int exitCode = exec.waitFor();
        if (exitCode != 0)
        {
          throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
        }
      }
      else
      {
        // abort
        exec.destroy();
      }

      // wait for termination
      int exitCode = exec.waitFor();
      if (exitCode != 0)
      {
        throw new RepositoryException("'%s', exit code: %d",exec.getExtendedErrorMessage(),command.toString(),exitCode);
      }

      // done
      exec.done(); exec = null;
    }
    catch (IOException exception)
    {
      throw new RepositoryException(Onzen.reniceIOException(exception));
    }
    finally
    {
      if (exec != null) exec.done();
    }
  }
}

/* end of file */
