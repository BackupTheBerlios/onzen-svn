/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: database functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.StringTokenizer;

/****************************** Classes ********************************/

/** database
 */
class Database
{
  // --------------------------- constants --------------------------------
  public  final static int ID_NONE = -1;

  private final int        TIMEOUT = 10; // timeout [s]

  // --------------------------- variables --------------------------------
  // use a single connection for all database accesses
  private static Object     connectionLock  = new Object();
  private static int        connectionCount = 0;
  private static Connection connection;

  private String           name;
  private boolean          openFlag;
//static int openCount=0;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create database access
   * @param name name
   */
  Database(String name)
    throws SQLException
  {
    this.name = name;

    // create directory
    File directory = new File(Settings.ONZEN_DIRECTORY);
    if ((directory != null) && !directory.exists()) directory.mkdirs();

    try
    {
      // load SQLite driver class
      Class.forName("org.sqlite.JDBC");

      // open database
      synchronized(connectionLock)
      {
        connectionCount++;
        if (connectionCount == 1)
        {
          try
          {
            DriverManager.setLoginTimeout(TIMEOUT);
            connection = DriverManager.getConnection("jdbc:sqlite:"+Settings.ONZEN_DIRECTORY+File.separator+name+".db");
            connection.setAutoCommit(false);
          }
          catch (SQLException exception)
          {
            connectionCount--;
            try { connection.close(); } catch (SQLException unusedException) { /* ignored */ }
            throw exception;
          }
//openCount++; Dprintf.dprintf("open openCount=%d",openCount); new Throwable().printStackTrace();
        }
      }

      this.openFlag = true;
    }
    catch (ClassNotFoundException exception)
    {
      throw new SQLException("SQLite database driver not found");
    }
  }

  /** close database access
   */
  public void close()
    throws SQLException
  {
    if (openFlag)
    {
      // close database
      synchronized(connectionLock)
      {
        if (connectionCount <= 0)
        {
          throw new Error("Internal error: close database which is not open");
        }

        connectionCount--;
        if (connectionCount == 0)
        {
          try { connection.setAutoCommit(true); } catch (SQLException unusedException) { /* ignored */ }
          connection.close();
//openCount--; Dprintf.dprintf("close openCount=%d",openCount);
        }
      }

      openFlag = false;
    }
  }

  /** create new statement
   * @return statement
   */
  public Statement createStatement()
    throws java.sql.SQLException
  {
    if (!openFlag) throw new SQLException("database is closed");

    Statement statement = connection.createStatement();
    statement.setQueryTimeout(TIMEOUT);

    return statement;
  }

  /** create new prepared statement
   * @param sqlCommand SQL command
   * @return prepared statement
   */
  public PreparedStatement prepareStatement(String sqlCommand)
    throws java.sql.SQLException
  {
    if (!openFlag) throw new SQLException("database is closed");

//Dprintf.dprintf("sqlCommand=%s",sqlCommand);
    PreparedStatement preparedStatement = connection.prepareStatement(sqlCommand);
    preparedStatement.setQueryTimeout(TIMEOUT*1000);

    return preparedStatement;
  }

  /** commit transaction
   */
  public void commit()
    throws SQLException
  {
    if (openFlag)
    {
      connection.commit();
    }
  }

  /** get id of last inserted row
   * @return id or ID_NONE
   */
  public int getLastInsertId()
    throws SQLException
  {
    int id = ID_NONE;

    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    if (!openFlag) throw new SQLException("database is closed");

    // Note: Java sqlite does not support getGeneratedKeys, thus use last_insert_rowid() direct.
    preparedStatement = connection.prepareStatement("SELECT last_insert_rowid();");
    resultSet = null;
    try
    {
      resultSet = preparedStatement.executeQuery();
      if (!resultSet.next())
      {
        throw new SQLException("no result");
      }

      id = resultSet.getInt(1);

      resultSet.close(); resultSet = null;
    }
    finally
    {
      if (resultSet != null) resultSet.close();
    }

    return id;
  }

  /** convert data to lines
   * @param data data (lines separated by \n) or null
   * @return lines
   */
  public static String[] dataToLines(String data)
  {
    ArrayList<String> lineList = new ArrayList<String>();

    if (data != null)
    {
      StringTokenizer stringTokenizer = new StringTokenizer(data,"\n");
      while (stringTokenizer.hasMoreTokens())
      {
        lineList.add(stringTokenizer.nextToken());
      }
    }

    return lineList.toArray(new String[lineList.size()]);
  }

  /** convert lines to data
   * @param lines lines
   * @return data (lines separated by \n)
   */
  public static String linesToData(String[] lines)
  {
    StringBuilder buffer = new StringBuilder();
    if (lines != null)
    {
      for (String line : lines)
      {
        buffer.append(line); buffer.append('\n');
      }
    }

    return buffer.toString();
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Database {name: "+name+"}";
  }

  //-----------------------------------------------------------------------

}

/* end of file */
