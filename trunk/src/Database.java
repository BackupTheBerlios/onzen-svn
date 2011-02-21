/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Patch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
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

/****************************** Classes ********************************/

/** database
 */
class Database
{
  // --------------------------- constants --------------------------------
  public final static int ID_NONE = -1;

  // --------------------------- variables --------------------------------
  public final Connection connection;

  private boolean         open;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create database access
   * @param name name
   */
  Database(String name)
    throws SQLException
  {
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

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Database {}";
  }

  //-----------------------------------------------------------------------

}

/* end of file */
