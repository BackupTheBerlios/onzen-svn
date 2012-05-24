/***********************************************************************\
*
* $Revision: 800 $
* $Date: 2012-01-28 10:49:16 +0100 (Sa, 28 Jan 2012) $
* $Author: trupp $
* Contents: history database functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.LinkedList;

/****************************** Classes ********************************/

/** history database
 */
public abstract class HistoryDatabase<T>
{
  // --------------------------- constants --------------------------------
  public static final int HISTORY_LENGTH_INFINTE = -1;

  private static final String    HISTORY_DATABASE_NAME    = "history";
  private static final int       HISTORY_DATABASE_VERSION = 2;

  // --------------------------- variables --------------------------------
  private int historyId;
  private int maxHistoryLength;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** open history database
   * @param historyId unique history id
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   */
  public HistoryDatabase(int historyId, int maxHistoryLength)
  {
    this.historyId        = historyId;
    this.maxHistoryLength = maxHistoryLength;

    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      // open database
      database = new Database(HISTORY_DATABASE_NAME);

      // create tables if needed
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages ( "+
                              "  id        INTEGER PRIMARY KEY, "+
                              "  historyId INTEGER, "+
                              "  datetime  INTEGER DEFAULT (DATETIME('now')), "+
                              "  message   TEXT "+
                              ");"
                             );

      // upgrade tables if needed
      try
      {
        statement = database.connection.createStatement();
        statement.executeUpdate("ALTER TABLE messages ADD COLUMN historyId INTEGER;");

        preparedStatement = database.connection.prepareStatement("UPDATE messages SET historyId=? WHERE historyId IS NULL;");
        preparedStatement.setInt(1,historyId);
        preparedStatement.executeUpdate();
      }
      catch (SQLException exception)
      {
        // ignored
      }

      // init/update meta data
      statement = database.connection.createStatement();
      resultSet = statement.executeQuery("SELECT value FROM meta");
      if (resultSet.next())
      {
        if (Integer.parseInt(resultSet.getString("value")) != HISTORY_DATABASE_VERSION)
        {
          preparedStatement = database.connection.prepareStatement("UPDATE meta SET value=? WHERE name='version';");
          preparedStatement.setString(1,Integer.toString(HISTORY_DATABASE_VERSION));
          preparedStatement.executeUpdate();
        }
      }
      else
      {
        preparedStatement = database.connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
        preparedStatement.setString(1,Integer.toString(HISTORY_DATABASE_VERSION));
        preparedStatement.executeUpdate();
      }
      resultSet.close();

      database.close(); database = null;
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      throw new Error(exception);
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }
  }

  public HistoryDatabase(int historyId)
    throws SQLException
  {
    this(historyId,HISTORY_LENGTH_INFINTE);
  }

  /** close history database
   */
  public void close()
  {
  }

  /** convert data into string
   * @param data data to convert into string
   * @return string
   */
  abstract public String dataToString(T data);

  /** convert string into data
   * @param string string to convert into data
   * @return data
   */
  abstract public T stringToData(String string);

  /** compare entries
   * @param data0,data1 entries
   * @return true if entries are equal
   */
  public boolean dataEquals(T data0, T data1)
  {
Dprintf.dprintf("");
    return dataToString(data0).equals(dataToString(data1));
  }

  /** add to history
   * @param data history data to add
   */
  public synchronized void add(T data)
    throws SQLException
  {
    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    Database database = null;
    try
    {
      database = new Database(HISTORY_DATABASE_NAME);

      // check if equal to last entry
      T lastData = null;
      preparedStatement = database.connection.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime DESC LIMIT 0,1;");
      preparedStatement.setInt(1,historyId);
      resultSet = preparedStatement.executeQuery();
      if (resultSet.next())
      {
        lastData = stringToData(resultSet.getString("message"));
      }
      resultSet.close();
Dprintf.dprintf("lastData=%s",lastData);

      if ((lastData == null) || !dataEquals(data,lastData))
      {
        // add to history
Dprintf.dprintf("");
        preparedStatement = database.connection.prepareStatement("INSERT INTO messages (historyId,datetime,message) VALUES (?,DATETIME('now'),?);");
        preparedStatement.setInt(1,historyId);
        preparedStatement.setString(2,dataToString(data));
        preparedStatement.execute();
      }

      if (maxHistoryLength != HISTORY_LENGTH_INFINTE)
      {
        // shorten history
        boolean doneFlag;
        do
        {
          doneFlag = true;

          preparedStatement = database.connection.prepareStatement("SELECT id FROM messages WHERE historyId=? ORDER BY datetime DESC LIMIT ?,1;");
          preparedStatement.setInt(1,historyId);
          preparedStatement.setInt(2,maxHistoryLength);
          resultSet = preparedStatement.executeQuery();
          if (resultSet.next())
          {
            int id = resultSet.getInt("id");
//Dprintf.dprintf("delete id=%d",id);

            preparedStatement = database.connection.prepareStatement("DELETE FROM messages WHERE id=?;");
            preparedStatement.setInt(1,id);
            preparedStatement.execute();
            doneFlag = false;
          }
        }
        while (!doneFlag);
      }

      database.close(); database = null;
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }
  }

  /** get history from database
   * @return list with history data (ascending order)
   */
  public LinkedList<T> getHistory()
    throws SQLException
  {
    LinkedList<T> history = new LinkedList<T>();

    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    Database database = null;
    try
    {
      database = new Database(HISTORY_DATABASE_NAME);

      preparedStatement = database.connection.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime DESC LIMIT 0,?;");
      preparedStatement.setInt(1,historyId);
      preparedStatement.setInt(2,Settings.maxMessageHistory);
      resultSet = preparedStatement.executeQuery();
      while (resultSet.next())
      {
        history.addFirst(stringToData(resultSet.getString("message")));
      }
      resultSet.close();

      database.close(); database = null;
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }

    return history;
  }

   /** get history
   * @param array array to fill
   * @return array with history data
   */
  public T[] toArray(T[] array)
    throws SQLException
  {
    LinkedList<T> history = getHistory();

    return history.toArray(array);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "HistoryDatabase {id: "+historyId+"}";
  }

  //-----------------------------------------------------------------------

  /** compare lines if they are equal (ignore empty lines, space and case)
   * @param otherLines lines to compare with
   * @return true iff lines are equal  (ignore empty lines, space and case)
   */
  protected boolean equalLines(String[] lines0, String[] lines1)
  {
    boolean equal = true;

    if ((lines0 != null) && (lines1 != null))
    {
      int i0 = 0;
      int i1 = 0;
      while ((i0 < lines0.length) && (i1 < lines1.length) && equal)
      {
        // skip empty lines
        while ((i0 < lines0.length) && lines0[i0].trim().isEmpty())
        {
          i0++;
        }
        while ((i1 < lines1.length) && lines1[i1].trim().isEmpty())
        {
          i1++;
        }

        // compare lines, skipping spaces, ignore case
        if ((i0 < lines0.length) && (i1 < lines1.length))
        {
          String line0 = lines0[i0].replace("\\s","");
          String line1 = lines1[i1].replace("\\s","");
          if (!line0.equalsIgnoreCase(line1))
          {
            equal = false;
          }
        }

        // next line
        i0++;
        i1++;
      }
    }
    else
    {
      equal = false;
    }

    return equal;
  }
}

/* end of file */
