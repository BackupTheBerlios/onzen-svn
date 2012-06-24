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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    DESCENDING,
    SORTED
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

      if ((addEntry != null) && !addEntry.isEmpty()) add(historyList,maxHistoryLength,direction,addEntry);
      historyDatabase.putHistory(historyList);

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
   * @param history history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry entry to add or null
   */
  public static void putHistory(int historyId, String[] history, int maxHistoryLength, Directions direction, String addEntry)
  {
    putHistory(historyId,new LinkedList<String>(Arrays.asList(history)),maxHistoryLength,direction,addEntry);
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
      statement = database.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = database.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages ( "+
                              "  id        INTEGER PRIMARY KEY, "+
                              "  historyId INTEGER, "+
                              "  datetime  INTEGER DEFAULT (DATETIME('now')), "+
                              "  message   TEXT "+
                              ");"
                             );
      database.commit();

      // upgrade tables if needed
      try
      {
        statement = database.createStatement();
        statement.executeUpdate("ALTER TABLE messages ADD COLUMN historyId INTEGER;");

        preparedStatement = database.prepareStatement("UPDATE messages SET historyId=? WHERE historyId IS NULL;");
        preparedStatement.setInt(1,historyId);
        preparedStatement.executeUpdate();

        database.commit();
      }
      catch (SQLException exception)
      {
        // ignored
      }

      // init/update meta data
      statement = database.createStatement();
      resultSet = statement.executeQuery("SELECT value FROM meta");
      if (resultSet.next())
      {
        String value = resultSet.getString("value");
        resultSet.close();

        if (Integer.parseInt(value) != HISTORY_DATABASE_VERSION)
        {
          preparedStatement = database.prepareStatement("UPDATE meta SET value=? WHERE name='version';");
          preparedStatement.setString(1,Integer.toString(HISTORY_DATABASE_VERSION));
          preparedStatement.executeUpdate();
          database.commit();
        }
      }
      else
      {
        resultSet.close();

        preparedStatement = database.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
        preparedStatement.setString(1,Integer.toString(HISTORY_DATABASE_VERSION));
        preparedStatement.executeUpdate();
        database.commit();
      }


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
   * @return -1,0,-1 if data0 < data1, data0 == data1, data0 > data1
   */
  public int dataCompareTo(T data0, T data1)
  {
    return dataToString(data0).compareTo(dataToString(data1));
  }

  /** compare entries
   * @param data0,data1 entries
   * @return true if entries are equal
   */
  public boolean dataEquals(T data0, T data1)
  {
    return dataCompareTo(data0,data1) == 0;
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

      // check if entry already exists (depending on sort mode)
      boolean existsFlag = false;
      switch (direction)
      {
        case ASCENDING:
          preparedStatement = database.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime DESC,id DESC LIMIT 0,1;");
          preparedStatement.setInt(1,historyId);
          resultSet = preparedStatement.executeQuery();
          if (resultSet.next())
          {
            T existingData = stringToData(resultSet.getString("message"));
            existsFlag = (existingData != null) && dataEquals(data,existingData);
          }
          resultSet.close();
          break;
        case DESCENDING:
          preparedStatement = database.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime ASC,id ASC LIMIT 0,1;");
          preparedStatement.setInt(1,historyId);
          resultSet = preparedStatement.executeQuery();
          if (resultSet.next())
          {
            T existingData = stringToData(resultSet.getString("message"));
            existsFlag = (existingData != null) && dataEquals(data,existingData);
          }
          resultSet.close();
          break;
        case SORTED:
          preparedStatement = database.prepareStatement("SELECT message FROM messages WHERE historyId=?;");
          preparedStatement.setInt(1,historyId);
          resultSet = preparedStatement.executeQuery();
          while (!existsFlag && resultSet.next())
          {
            T existingData = stringToData(resultSet.getString("message"));
            existsFlag = (existingData != null) && dataEquals(data,existingData);
          }
          resultSet.close();
          break;
      }

      if (!existsFlag)
      {
        // add to history
        preparedStatement = database.prepareStatement("INSERT INTO messages (historyId,datetime,message) VALUES (?,DATETIME('now'),?);");
        preparedStatement.setInt(1,historyId);
        preparedStatement.setString(2,dataToString(data));
        preparedStatement.executeUpdate();
        database.commit();
      }

      if (maxHistoryLength != HISTORY_LENGTH_INFINTE)
      {
        // shorten history
        int id;
        do
        {
          preparedStatement = database.prepareStatement("SELECT id FROM messages WHERE historyId=? ORDER BY datetime DESC,id DESC LIMIT ?,1;");
          preparedStatement.setInt(1,historyId);
          preparedStatement.setInt(2,maxHistoryLength);
          resultSet = preparedStatement.executeQuery();
          if (resultSet.next())
          {
            id = resultSet.getInt("id");
          }
          else
          {
            id = -1;
          }
          resultSet.close();;

          if (id >= 0)
          {
//Dprintf.dprintf("delete id=%d",id);

            preparedStatement = database.prepareStatement("DELETE FROM messages WHERE id=?;");
            preparedStatement.setInt(1,id);
            preparedStatement.executeUpdate();
            database.commit();
          }
        }
        while (id >= 0);
      }

      database.close(); database = null;
    }
    finally
    {
      if (database != null) try { database.close(); } catch (SQLException exception) { /* ignored */ }
    }
  }

  /** get history from database
   * @return list with history data
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

      preparedStatement = database.prepareStatement("SELECT message FROM messages WHERE historyId=? ORDER BY datetime ASC,id ASC LIMIT 0,?;");
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
          case SORTED:
            historyList.add(data);
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

    if (direction == Directions.SORTED)
    {
      Collections.sort(historyList,new Comparator<T>()
                       {
                          public int compare(T data0, T data1)
                          {
                            return dataCompareTo(data0,data1);
                          }
                       }
                      );
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

      preparedStatement = database.prepareStatement("DELETE FROM messages WHERE historyId=?;");
      preparedStatement.setInt(1,historyId);
      preparedStatement.executeUpdate();
      database.commit();

      Iterator<T> iterator = null;
      switch (direction)
      {
        case ASCENDING :
          iterator = historyList.descendingIterator();
          break;
        case DESCENDING:
          iterator = historyList.iterator();
          break;
        case SORTED:
          Collections.sort(historyList,new Comparator<T>()
                           {
                              public int compare(T data0, T data1)
                              {
                                return dataCompareTo(data0,data1);
                              }
                           }
                          );
          iterator = historyList.iterator();
          break;
      }
      while (iterator.hasNext())
      {
        T data = iterator.next();

        preparedStatement = database.prepareStatement("INSERT INTO messages (historyId,datetime,message) VALUES (?,DATETIME('now'),?);");
        preparedStatement.setInt(1,historyId);
        preparedStatement.setString(2,dataToString(data));
        preparedStatement.executeUpdate();
      }
      database.commit();

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

  private static void add(LinkedList<String> historyList, int maxHistoryLength, Directions direction, String addEntry)
  {
    switch (direction)
    {
      case ASCENDING:
        {
          String lastEntry = historyList.peekLast();
          if ((lastEntry == null) || !lastEntry.equals(addEntry))
          {
            historyList.add(addEntry);
            while (historyList.size() > maxHistoryLength)
            {
              historyList.removeFirst();
            }
          }
        }
        break;
      case DESCENDING:
        {
          String firstEntry = historyList.peekFirst();
          if ((firstEntry == null) || !firstEntry.equals(addEntry))
          {
            historyList.addFirst(firstEntry);
            while (historyList.size() > maxHistoryLength)
            {
              historyList.removeLast();
            }
          }
        }
        break;
      case SORTED:
        {
          boolean existsFlag = false;
          for (String entry : historyList)
          {
            if (entry.equals(addEntry))
            {
              existsFlag = true;
              break;
            }
          }
          if (!existsFlag)
          {
            historyList.addFirst(addEntry);
            Collections.sort(historyList,new Comparator<String>()
                             {
                                public int compare(String data0, String data1)
                                {
                                  return data0.compareTo(data1);
                                }
                             }
                            );
            while (historyList.size() > maxHistoryLength)
            {
              historyList.removeLast();
            }
          }
        }
        break;
    }
  }

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
