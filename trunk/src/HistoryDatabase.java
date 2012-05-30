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
//import java.net.UnknownHostException;
//import java.net.SocketException;
//import java.net.InetAddress;
//import java.net.DatagramSocket;
//import java.net.DatagramPacket;
//import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/****************************** Classes ********************************/

/** history database
 */
public abstract class HistoryDatabase<T>
{
  // --------------------------- constants --------------------------------
  public enum Directions
  {
    ASCENDING,
    DESCENDING
  };

  public static final int HISTORY_LENGTH_INFINTE = -1;

  private static final String    HISTORY_DATABASE_NAME    = "history";
  private static final int       HISTORY_DATABASE_VERSION = 2;

  // --------------------------- variables --------------------------------
  private int        historyId;
  private int        maxHistoryLength;
  private Directions direction;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get history from database
   * @param historyId unique history id
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @return list with history data
   */
  public static LinkedList<String> getHistory(int historyId, int maxHistoryLength, Directions direction)
  {
    LinkedList<String> historyList = null;

    HistoryDatabase historyDatabase = null;
    try
    {
      historyDatabase = new HistoryDatabase<String>(historyId,maxHistoryLength,direction)
      {
        public String dataToString(String s) { return s; }
        public String stringToData(String s) { return s; }
      };

      historyList = historyDatabase.getHistory();

      historyDatabase.close(); historyDatabase = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot load history with id %d from database (error: %s)",historyId,exception.getMessage());
      return new LinkedList<String>();
    }
    finally
    {
      if (historyDatabase != null) historyDatabase.close();
    }

    return historyList;
  }

  /** get history from database
   * @param historyId unique history id
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @return list with history data
   */
  public static LinkedList<String> getHistory(int historyId, int maxHistoryLength)
  {
    return getHistory(historyId,maxHistoryLength,Directions.ASCENDING);
  }

  /** get history from database
   * @param historyId unique history id
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry entry to add or null
   */
  public static void putHistory(int historyId, LinkedList<String> historyList, int maxHistoryLength, Directions direction, String addEntry)
  {
    HistoryDatabase historyDatabase = null;
    try
    {
      historyDatabase = new HistoryDatabase<String>(historyId,maxHistoryLength,direction)
      {
        public String dataToString(String s) { return s; }
        public String stringToData(String s) { return s; }
      };

      historyDatabase.putHistory(historyList);
      if ((addEntry != null) && !addEntry.isEmpty()) historyDatabase.add(addEntry);

      historyDatabase.close(); historyDatabase = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot save history with id %d into database (error: %s)",historyId,exception.getMessage());
      return;
    }
    finally
    {
      if (historyDatabase != null) historyDatabase.close();
    }
  }

  /** get history from database
   * @param historyId unique history id
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   */
  public static void putHistory(int historyId, LinkedList<String> historyList, int maxHistoryLength, Directions direction)
  {
    putHistory(historyId,historyList,maxHistoryLength,direction,null);
  }

  /** get history from database
   * @param historyId unique history id
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param addEntry entry to add or null
   */
  public static void putHistory(int historyId, LinkedList<String> historyList, int maxHistoryLength, String addEntry)
  {
    putHistory(historyId,historyList,maxHistoryLength,Directions.ASCENDING,addEntry);
  }

  /** get history from database
   * @param historyId unique history id
   * @param history history array (ascending order)
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry entry to add or null
   */
  public static void putHistory(int historyId, String[] history, int maxHistoryLength, Directions direction, String addEntry)
  {
    putHistory(historyId,new LinkedList<String>(Arrays.asList(history)),maxHistoryLength,addEntry);
  }

  /** get history from database
   * @param historyId unique history id
   * @param historyList history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   */
  public static void putHistory(int historyId, String[] history, int maxHistoryLength, Directions direction)
  {
    putHistory(historyId,history,maxHistoryLength,direction,null);
  }

  /** get history from database
   * @param historyId unique history id
   * @param historyList history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param addEntry entry to add or null
   */
  public static void putHistory(int historyId, String[] history, int maxHistoryLength, String addEntry)
  {
    putHistory(historyId,history,maxHistoryLength,Directions.ASCENDING,addEntry);
  }

  /** convert string array into history
   * @param history string array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param addEntry entry to add or null
   * @return list with history data (ascending order)
   */
  public static LinkedList<String> asList(String[] history, int maxHistoryLength, String addEntry)
  {
    LinkedList<String> historyList = new LinkedList<String>(Arrays.asList(history));
    if (!addEntry.equals(historyList.getFirst()))
    {
      historyList.addFirst(addEntry);
    }
    if (maxHistoryLength != HISTORY_LENGTH_INFINTE)
    {
      while (historyList.size() > maxHistoryLength)
      {
        historyList.removeLast();
      }
    }

    return historyList;
  }

  /** convert string array into history
   * @param history string array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @return list with history data (ascending order)
   */
  public static LinkedList<String> asList(String[] history, int maxHistoryLength)
  {
    return asList(history,maxHistoryLength,null);
  }

  /** convert string array into history
   * @param history string array
   * @return list with history data (ascending order)
   */
  public static LinkedList<String> asList(String[] history)
  {
    return asList(history,HISTORY_LENGTH_INFINTE);
  }

  /** open history database
   * @param historyId unique history id
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   */
  public HistoryDatabase(int historyId, int maxHistoryLength, Directions direction)
  {
    this.historyId        = historyId;
    this.maxHistoryLength = maxHistoryLength;
    this.direction        = direction;

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

  /** open history database
   * @param historyId unique history id
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   */
  public HistoryDatabase(int historyId, int maxHistoryLength)
  {
    this(historyId,maxHistoryLength,Directions.ASCENDING);
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
      preparedStatement = database.connection.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime,id DESC LIMIT 0,1;");
      preparedStatement.setInt(1,historyId);
      resultSet = preparedStatement.executeQuery();
      if (resultSet.next())
      {
        lastData = stringToData(resultSet.getString("message"));
      }
      resultSet.close();

      if ((lastData == null) || !dataEquals(data,lastData))
      {
        // add to history
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
    LinkedList<T> historyList = new LinkedList<T>();

    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    Database database = null;
    try
    {
      database = new Database(HISTORY_DATABASE_NAME);

      preparedStatement = database.connection.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime DESC LIMIT 0,?;");
      preparedStatement.setInt(1,historyId);
      preparedStatement.setInt(2,maxHistoryLength);
      resultSet = preparedStatement.executeQuery();
      while (resultSet.next())
      {
        T data = stringToData(resultSet.getString("message"));

        switch (direction)
        {
          case ASCENDING:
            historyList.add(data);
            break;
          case DESCENDING:
            historyList.addFirst(data);
            break;
        }
      }
      resultSet.close();

      database.close(); database = null;
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }

    return historyList;
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

  /** get history from database
   * @return list with history data (ascending order)
   */
  public void putHistory(LinkedList<T> historyList)
    throws SQLException
  {
    PreparedStatement preparedStatement;
    ResultSet         resultSet;

    Database database = null;
    try
    {
      database = new Database(HISTORY_DATABASE_NAME);

      preparedStatement = database.connection.prepareStatement("DELETE FROM messages WHERE historyId=?;");
      preparedStatement.setInt(1,historyId);
      preparedStatement.executeUpdate();

      Iterator<T> iterator = null;
      switch (direction)
      {
        case ASCENDING : iterator = historyList.descendingIterator(); break;
        case DESCENDING: iterator = historyList.iterator();           break;
      }
      while (iterator.hasNext())
      {
        T data = iterator.next();

        preparedStatement = database.connection.prepareStatement("INSERT INTO messages (historyId,datetime,message) VALUES (?,DATETIME('now'),?);");
        preparedStatement.setInt(1,historyId);
        preparedStatement.setString(2,dataToString(data));
        preparedStatement.execute();
      }

      database.close(); database = null;
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }
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
