/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Patch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: patch function
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

/****************************** Classes ********************************/

/** patch
 */
class Patch
{
  // --------------------------- constants --------------------------------
  private static final String ONZEN_PATCHES_DATABASE_FILE_NAME = Settings.ONZEN_DIRECTORY+File.separator+"patches.db";
  private static final int    ONZEN_PATCHES_DATABASE_VERSION   = 1;

  private final int PATCH_ID_NONE = -1;

  public enum States
  {
    NONE,
    REVIEW,
    COMMITED,
    APPLIED,
    DISCARDED;

    /** convert ordinal value to enum
     * @param value ordinal value
     * @return enum
     */
    static States toEnum(int value)
    {
      States state;

      switch (value)
      {
        case 0:  return NONE;
        case 1:  return REVIEW;
        case 2:  return COMMITED;
        case 3:  return APPLIED;
        case 4:  return DISCARDED;
        default: return NONE;
      }
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case NONE:      return "none";
        case REVIEW:    return "review";
        case COMMITED:  return "commited";
        case APPLIED:   return "applied";
        case DISCARDED: return "discarded";
        default:        return "none";
      }
    }
  };

  // --------------------------- variables --------------------------------
  public  States         state;
  public  final String   text;
  public  final String[] fileNames;
  public  String         summary;
  public  String         message;

  private String         rootPath;
  private int            id;
  private File           tmpFile;
  private boolean        savedFlag;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get patches from database
   * @param rootPath root path of patches
   * @param filterStates states of patches
   * @param n max. number of patches to return
   * @return patches array
   */
  public static Patch[] getPatches(String rootPath, EnumSet<States> filterStates, int n)
  {
    ArrayList<Patch> patchList = new ArrayList<Patch>();

    try
    {
      Connection        connection;
      Statement         statement;
      PreparedStatement preparedStatement;
      ResultSet         resultSet1,resultSet2;

      // open database
      connection = openPatchesDatabase();

      // get patches (Note: there is no way to use prepared statements with variable "IN"-operator)
      statement = connection.createStatement();
      resultSet1 = statement.executeQuery("SELECT "+
                                          "  id, "+
                                          "  state, "+
                                          "  text, "+
                                          "  summary, "+
                                          "  message "+
                                          "FROM patches "+
                                          "WHERE     rootPath='"+rootPath+"'"+
                                          "      AND (   state="+Patch.States.NONE.ordinal()+" "+
                                          "           OR state IN ("+StringUtils.join(filterStates,",",true)+") "+
                                          "          )"+
                                          "LIMIT 0,"+n+
                                          ";"
                                         );

      preparedStatement = connection.prepareStatement("SELECT "+
                                                      "  fileName "+
                                                      "FROM files "+
                                                      "WHERE patchId=? "+
                                                      ";"
                                                     );
      while (resultSet1.next())
      {
        // get patch data
        int    id      = resultSet1.getInt("id"); 
        States state   = States.toEnum(resultSet1.getInt("state"));
        String text    = resultSet1.getString("text"); 
        String summary = resultSet1.getString("summary"); if (summary == null) summary = "";
        String message = resultSet1.getString("message"); if (message == null) message = "";
//Dprintf.dprintf("id=%d s=%s",id,summary);

        // get files
        ArrayList<String> fileList = new ArrayList<String>();
        preparedStatement.setInt(1,id);
        resultSet2 = preparedStatement.executeQuery();
        while (resultSet2.next())
        {
          fileList.add(resultSet2.getString("fileName"));
        }
        resultSet2.close();

        // add to list
        patchList.add(new Patch(rootPath,
                                id,
                                state,
                                text,
                                summary,
                                message,
                                fileList.toArray(new String[fileList.size()])
                               )
                         );
      }
      resultSet1.close();

      // close database
      closePatchesDatabase(connection);
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot patches from database (error: %s)",exception.getMessage());
      return null;
    }

    return patchList.toArray(new Patch[patchList.size()]);
  }

  /** create patch
   */
  Patch(String rootPath, int id, States state, String text, String summary, String message, String[] fileNames)
  {
    this.rootPath  = rootPath;
    this.id        = id;
    this.state     = state;
    this.text      = text;
    this.tmpFile   = null;
    this.fileNames = fileNames;
    this.summary   = summary;
    this.message   = message;
    this.savedFlag = false;
  }

  /** create patch
   * @param fileDataSet file data set
   * @param lines patch text
   */
  Patch(String rootPath, HashSet<FileData> fileDataSet, String text)
  {
    this.rootPath  = rootPath;
    this.id        = PATCH_ID_NONE;
    this.state     = States.NONE;
    this.text      = text;
    this.tmpFile   = null;
    this.message   = null;
    this.savedFlag = false;

    // set file names
    ArrayList<String> fileList = new ArrayList<String>();
    for (FileData fileData : fileDataSet)
    {
      fileList.add(fileData.getFileName());
    }
    this.fileNames = fileList.toArray(new String[fileList.size()]);
  }

  /** create patch
   * @param fileDataSet file data set
   * @param lines patch lines
   */
  Patch(String rootPath, HashSet<FileData> fileDataSet, String[] lines)
  {
    this.rootPath  = rootPath;
    this.id        = PATCH_ID_NONE;
    this.state     = States.NONE;
    this.tmpFile   = null;
    this.message   = null;
    this.savedFlag = false;

    // set text
    StringBuilder buffer = new StringBuilder();
    for (String line : lines)
    {
      buffer.append(line); buffer.append('\n');
    }
    this.text = buffer.toString();

    // set file names
    ArrayList<String> fileList = new ArrayList<String>();
    for (FileData fileData : fileDataSet)
    {
      fileList.add(fileData.getFileName());
    }
    this.fileNames = fileList.toArray(new String[fileList.size()]);
  }

  /** done patch
   */
  public void done()
  {
    // clean-up not used saved patch
    if ((id != PATCH_ID_NONE) && !savedFlag)
    {
      try
      {
        Connection        connection;
        PreparedStatement preparedStatement;

        // open database
        connection = openPatchesDatabase();

        // remove not saved patch
        preparedStatement = connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();
        preparedStatement = connection.prepareStatement("DELETE FROM patches WHERE id=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();

        // close database
        closePatchesDatabase(connection);

        id = -1;
      }
      catch (SQLException exception)
      {
        Onzen.printWarning("Cannot clean-up patches database (error: %s)",exception.getMessage());
        return;
      }
    }

    // delete tempory file
    if (tmpFile != null) tmpFile.delete();
  }

  /** get patch id
   * @return patch number or -1 if no patch number could be allocated
   */
  public int getId()
  {
    // reserve a patch number if required
    if (id == PATCH_ID_NONE)
    {
      try
      {
        reserve();
      }
      catch (SQLException exception)
      {
       return PATCH_ID_NONE;
      }
    }

    return id;
  }

  /** get patch file name
   * @return temporary file name or null
   */
  public String getFileName()
  {
    if (tmpFile == null)
    {
      // write temporary file
      try
      {
        this.tmpFile = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));

        PrintWriter output = new PrintWriter(new FileWriter(tmpFile.getPath()));
        output.println(text);
        output.close();
      }
      catch (IOException exception)
      {
  Dprintf.dprintf("");
      }
    }

    return (tmpFile != null) ? tmpFile.getAbsolutePath() : null;
  }

  /** save patch into database
   */
  public void save()
  {
    // store in database
    try
    {
      Connection        connection;
      Statement         statement;
      PreparedStatement preparedStatement;
      ResultSet         resultSet;

      // reserve if still not reserved
      if (id < 0)
      {
        reserve();
      }
      if (id < 0)
      {
        return;
      }

      // open database
      connection = openPatchesDatabase();

      // store patch data
      preparedStatement = connection.prepareStatement("UPDATE patches SET text=?,state=?,summary=?,message=? WHERE id=?;");
      preparedStatement.setString(1,text);
      preparedStatement.setInt(2,state.ordinal());
      preparedStatement.setString(3,summary);
      preparedStatement.setString(4,message);
      preparedStatement.setInt(5,id);
      preparedStatement.executeUpdate();

      // insert files
      preparedStatement = connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
      preparedStatement.setInt(1,id);
      preparedStatement.executeUpdate();
      preparedStatement = connection.prepareStatement("INSERT INTO files (patchId,fileName) VALUES (?,?);");
      for (String fileName : fileNames)
      {
        preparedStatement.setInt(1,id);
        preparedStatement.setString(2,fileName);
        preparedStatement.executeUpdate();
      }

      // close database
      closePatchesDatabase(connection);

      // mark this patch is saved
      this.savedFlag = true;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot store message into patches database (error: %s)",exception.getMessage());
      return;
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Patch {"+id+", summary: "+summary+", state: "+state.toString()+"}";
  }

  //-----------------------------------------------------------------------

  /** open history database
   * @return connection
   */
  private static Connection openPatchesDatabase()
    throws SQLException
  {
    Connection connection = null;

    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      // load SQLite driver class
      Class.forName("org.sqlite.JDBC");

      // open database
      connection = DriverManager.getConnection("jdbc:sqlite:"+ONZEN_PATCHES_DATABASE_FILE_NAME);
      connection.setAutoCommit(false);

      // create tables if needed
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS patches ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  rootPath TEXT, "+
                              "  state    INTEGER, "+
                              "  text     TEXT, "+
                              "  summary  TEXT "+
                              "  message  TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS files ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER, "+
                              "  fileName TEXT "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT name,value FROM meta;");
      if (!resultSet.next())
      {
        preparedStatement = connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
        preparedStatement.setString(1,Integer.toString(ONZEN_PATCHES_DATABASE_VERSION));
        preparedStatement.executeUpdate();
      }
      resultSet.close();
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      throw exception;
    }
    catch (ClassNotFoundException exception)
    {
      throw new SQLException("SQLite database driver not found");
    }

    return connection;
  }

  /** close history database
   * @param connection connection
   */
  private static void closePatchesDatabase(Connection connection)
    throws SQLException
  {
    connection.setAutoCommit(true);
    connection.close();

    // unlock database
//???
  }

  /** reserver patch in database
   */
  private void reserve()
    throws SQLException
  {
    Connection        connection;
    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    // open database
    connection = openPatchesDatabase();

    // insert empty patch to generated entry (Note: Java sqlite does not support getGeneratedKeys, thus use last_insert_rowid() direct)
    SQLException sqlException = null;
    int          retryCount   = 0;
    do
    {
      try
      {
        preparedStatement = connection.prepareStatement("INSERT INTO patches (rootPath,state) VALUES (?,?);");
        preparedStatement.setString(1,rootPath);
        preparedStatement.setInt(2,state.ordinal());
        preparedStatement.executeUpdate();

        preparedStatement = connection.prepareStatement("SELECT last_insert_rowid();");
        resultSet = preparedStatement.executeQuery();
        if (!resultSet.next())
        {
          throw new SQLException("no result");
        }
        id = resultSet.getInt(1);
        resultSet.close();
      }
      catch (SQLException exception)
      {
        sqlException = exception;
        retryCount++;

        try { Thread.sleep(250); } catch (InterruptedException interruptedException) { /* ignored */ }
      }
    }
    while ((id < 0) && (retryCount < 5));
    if (id < 0)
    {
      throw (sqlException != null) ? sqlException : new SQLException("cannot reserve patch in database");
    }

    // close database
    closePatchesDatabase(connection);
  }
}

/* end of file */
