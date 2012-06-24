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
  public final static int ID_NONE = -1;

  // --------------------------- variables --------------------------------
  private final Connection connection;

  private String          name;
  private boolean         open;

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
      connection = DriverManager.getConnection("jdbc:sqlite:"+Settings.ONZEN_DIRECTORY+File.separator+name+".db");
      connection.setAutoCommit(false);

      this.open = true;
    }
    catch (ClassNotFoundException exception)
    {
      throw new SQLException("SQLite database driver not found");
    }
  }

  /** create new statement
   * @return statement
   */
  public Statement createStatement()
    throws java.sql.SQLException
  {
    return connection.createStatement();
  }

  /** create new prepared statement
   * @param sqlCommand SQL command
   * @return prepared statement
   */
  public PreparedStatement prepareStatement(String sqlCommand)
    throws java.sql.SQLException
  {
//Dprintf.dprintf("sqlCommand=%s",sqlCommand);
    return connection.prepareStatement(sqlCommand);
  }

  /** commit transaction
   */
  public void commit()
    throws SQLException
  {
    if (open)
    {
      connection.commit();
    }
  }


  /** close database access
   */
  public void close()
    throws SQLException
  {
    if (open)
    {
      connection.setAutoCommit(true);
      connection.close();

      open = false;
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
