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
import java.util.List;
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
  private static Object lock = new Object();

  private String           name;
  private int              maxHistoryLength;
  private Directions       direction;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get history from database
   * @param name history name
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @return list with history data
   */
  public static LinkedList<String> getHistory(String name, int maxHistoryLength, Directions direction)
  {
    LinkedList<String> historyList = null;

    HistoryDatabase historyDatabase = null;
    try
    {
      // open database
      historyDatabase = openStringHistoryDatabase(name,maxHistoryLength,direction);

      // get history
      historyList = historyDatabase.getHistory();

      // close database
      historyDatabase.close(); historyDatabase = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot load history '%s' from database (error: %s)",name,exception.getMessage());
      return new LinkedList<String>();
    }
    finally
    {
      if (historyDatabase != null) historyDatabase.close();
    }

    return historyList;
  }

  /** get history from database
   * @param name history name
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @return list with history data
   */
  public static LinkedList<String> getHistory(String name, int maxHistoryLength)
  {
    return getHistory(name,maxHistoryLength,Directions.ASCENDING);
  }

  /** put history into database
   * @param name history name
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry entry to add or null
   */
  public static void setHistory(String name, LinkedList<String> historyList, int maxHistoryLength, Directions direction, String addEntry)
  {
    HistoryDatabase historyDatabase = null;
    try
    {
      // open database
      historyDatabase = openStringHistoryDatabase(name,maxHistoryLength,direction);

      // set history
      historyDatabase.setHistory(historyList);

      // add entry
      if ((addEntry != null) && !addEntry.isEmpty()) add(historyList,maxHistoryLength,direction,addEntry);

      // close database
      historyDatabase.close(); historyDatabase = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot save history '%s' into database (error: %s)",name,exception.getMessage());
      return;
    }
    finally
    {
      if (historyDatabase != null) historyDatabase.close();
    }
  }

  /** set history in database
   * @param name history name
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   */
  public static void setHistory(String name, LinkedList<String> historyList, int maxHistoryLength, Directions direction)
  {
    setHistory(name,historyList,maxHistoryLength,direction,null);
  }

  /** set history in database
   * @param name history name
   * @param historyList history list
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param addEntry entry to add or null
   */
  public static void setHistory(String name, LinkedList<String> historyList, int maxHistoryLength, String addEntry)
  {
    setHistory(name,historyList,maxHistoryLength,Directions.ASCENDING,addEntry);
  }

  /** set history in database
   * @param name history name
   * @param history history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry entry to add or null
   */
  public static void setHistory(String name, String[] history, int maxHistoryLength, Directions direction, String addEntry)
  {
    setHistory(name,new LinkedList<String>(Arrays.asList(history)),maxHistoryLength,direction,addEntry);
  }

  /** set history in database
   * @param name history name
   * @param historyList history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   */
  public static void setHistory(String name, String[] history, int maxHistoryLength, Directions direction)
  {
    setHistory(name,history,maxHistoryLength,direction,null);
  }

  /** set history in database
   * @param name history name
   * @param historyList history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param addEntry entry to add or null
   */
  public static void setHistory(String name, String[] history, int maxHistoryLength, String addEntry)
  {
    setHistory(name,history,maxHistoryLength,Directions.ASCENDING,addEntry);
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
   * @param name history name
   * @param historyList history array
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   */
  public HistoryDatabase(String name, int maxHistoryLength, Directions direction)
    throws SQLException
  {
    this.name             = name;
    this.maxHistoryLength = maxHistoryLength;
    this.direction        = direction;

    // create tables
    synchronized(lock)
    {
      Database database = null;
      try
      {
        PreparedStatement preparedStatement;
        ResultSet         resultSet;

        // open database
        database = new Database(HISTORY_DATABASE_NAME);

        // create tables if needed
        preparedStatement = database.prepareStatement("CREATE TABLE IF NOT EXISTS meta ( "+
                                                      "  name  TEXT, "+
                                                      "  value TEXT "+
                                                      ");"
                                                     );
        preparedStatement.executeUpdate();
        preparedStatement = prepareInit(database);
        preparedStatement.executeUpdate();
        database.commit();

        // upgrade tables if needed
        try
        {
//todo
        preparedStatement = database.prepareStatement("ALTER TABLE messages ADD COLUMN historyId INTEGER;");
          preparedStatement.executeUpdate();

          preparedStatement = database.prepareStatement("UPDATE messages SET historyId=? WHERE historyId IS NULL;");
//          preparedStatement.setInt(1,historyId);
          preparedStatement.executeUpdate();

          database.commit();
        }
        catch (SQLException unusedException)
        {
          // ignored
        }

        // init/update meta data
        preparedStatement = database.prepareStatement("SELECT value FROM meta");
        resultSet = preparedStatement.executeQuery();
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

        // close database
        database.close(); database = null;
      }
      finally
      {
        if (database != null) try { database.close(); } catch (SQLException unusedException) { /* ignored */ }
      }
    }
  }

  /** open history database
   * @param name history name
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   */
  public HistoryDatabase(String name, int maxHistoryLength)
    throws SQLException
  {
    this(name,maxHistoryLength,Directions.ASCENDING);
  }

  public HistoryDatabase(String name)
    throws SQLException
  {
    this(name,HISTORY_LENGTH_INFINTE);
  }

  /** close history database
   */
  public void close()
  {
  }

  /** prepare table init statement
   * @param database database
   * @return prepared statment
   */
  abstract public PreparedStatement prepareInit(Database database)
    throws SQLException;

    /** prepare table insert statement
   * @param database database
   * @return prepared statment
   */
  abstract public PreparedStatement prepareInsert(Database database, T data)
    throws SQLException;

    /** prepare table delete statement
   * @param database database
   * @return prepared statment
   */
  abstract public PreparedStatement prepareDelete(Database database, int id)
    throws SQLException;

    /** get result
   * @param resultSet result set
   * @return data
   */
  abstract public T getResult(ResultSet resultSet)
    throws SQLException;

  /** compare entries
   * @param data0,data1 entries
   * @return -1,0,-1 if data0 < data1, data0 == data1, data0 > data1
   */
  abstract public int dataCompareTo(T data0, T data1);

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

    synchronized(lock)
    {
      Database database = null;
      try
      {
        database = new Database(HISTORY_DATABASE_NAME);

        // check if entry already exists (depending on sort mode)
        boolean existsFlag = false;
        switch (direction)
        {
          case ASCENDING:
          case DESCENDING:
            // get most recent entry
            preparedStatement = database.prepareStatement("SELECT * FROM "+name+" ORDER BY datetime DESC,id DESC LIMIT 0,1;");
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next())
            {
              // check if equals
              T existingData = getResult(resultSet);
              existsFlag = (existingData != null) && dataEquals(data,existingData);
            }
            resultSet.close();
            break;
          case SORTED:
            // check if exists
            preparedStatement = database.prepareStatement("SELECT * FROM "+name);
            resultSet = preparedStatement.executeQuery();
            while (!existsFlag && resultSet.next())
            {
              T existingData = getResult(resultSet);
              existsFlag = (existingData != null) && dataEquals(data,existingData);
            }
            resultSet.close();
            break;
        }

        if (!existsFlag)
        {
          // add to history
          preparedStatement = prepareInsert(database,data);
          preparedStatement.executeUpdate();
          database.commit();
        }

        if (maxHistoryLength != HISTORY_LENGTH_INFINTE)
        {
          // shorten history
          int id;
          do
          {
            preparedStatement = database.prepareStatement("SELECT id FROM "+name+" ORDER BY datetime DESC,id DESC LIMIT ?,1;");
            preparedStatement.setInt(1,maxHistoryLength);
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
              preparedStatement = prepareDelete(database,id);
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
        if (database != null) try { database.close(); } catch (SQLException unusedException) { /* ignored */ }
      }
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

    synchronized(lock)
    {
      Database database = null;
      try
      {
        database = new Database(HISTORY_DATABASE_NAME);

        preparedStatement = database.prepareStatement("SELECT * FROM "+name+" ORDER BY datetime ASC,id ASC LIMIT 0,?;");
        preparedStatement.setInt(1,maxHistoryLength);
        resultSet = preparedStatement.executeQuery();
        while (resultSet.next())
        {
          T data = getResult(resultSet);

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
        if (database != null) try { database.close(); } catch (SQLException unusedException) { /* ignored */ }
      }
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

  /** set history in database
   * @param list list with history data
   */
  public void setHistory(LinkedList<T> list)
    throws SQLException
  {
    PreparedStatement preparedStatement;

    synchronized(lock)
    {
      Database database = null;
      try
      {
        // open database
        database = new Database(HISTORY_DATABASE_NAME);

        // delete old history
        preparedStatement = database.prepareStatement("DELETE FROM "+name);
        preparedStatement.executeUpdate();
        database.commit();

        // add new history
        Iterator<T> iterator = null;
        switch (direction)
        {
          case ASCENDING :
            iterator = list.descendingIterator();
            break;
          case DESCENDING:
            iterator = list.iterator();
            break;
          case SORTED:
            Collections.sort(list,new Comparator<T>()
                             {
                                public int compare(T data0, T data1)
                                {
                                  return dataCompareTo(data0,data1);
                                }
                             }
                            );
            iterator = list.iterator();
            break;
        }
        while (iterator.hasNext())
        {
          T data = iterator.next();

          preparedStatement = prepareInsert(database,data);
          preparedStatement.executeUpdate();
        }
        database.commit();

        // close database
        database.close(); database = null;
      }
      finally
      {
        if (database != null) try { database.close(); } catch (SQLException unusedException) { /* ignored */ }
      }
    }
  }

  /** set history in database
   * @param list list with history data
   */
  public void setHistory(T[] array)
    throws SQLException
  {
    PreparedStatement preparedStatement;

    synchronized(lock)
    {
      Database database = null;
      try
      {
        // open database
        database = new Database(HISTORY_DATABASE_NAME);

        // delete old history
        preparedStatement = database.prepareStatement("DELETE FROM "+name);
        preparedStatement.executeUpdate();
        database.commit();

        // add new history
        switch (direction)
        {
          case ASCENDING :
            for (int i = array.length-1; i >= 0; i--)
            {
              preparedStatement = prepareInsert(database,array[i]);
              preparedStatement.executeUpdate();
            }
            break;
          case DESCENDING:
            for (int i = 0; i < array.length; i++)
            {
              preparedStatement = prepareInsert(database,array[i]);
              preparedStatement.executeUpdate();
            }
            break;
          case SORTED:
            Arrays.sort(array,new Comparator<T>()
                        {
                           public int compare(T data0, T data1)
                           {
                             return dataCompareTo(data0,data1);
                           }
                        }
                       );
            for (int i = 0; i < array.length; i++)
            {
              preparedStatement = prepareInsert(database,array[i]);
              preparedStatement.executeUpdate();
            }
            break;
        }
        database.commit();

        // close database
        database.close(); database = null;
      }
      finally
      {
        if (database != null) try { database.close(); } catch (SQLException unusedException) { /* ignored */ }
      }
    }
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
    return "HistoryDatabase {id: "+name+"}";
  }

  //-----------------------------------------------------------------------

  /** open string history database
   * @param name name
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @return history database
   */
  private static HistoryDatabase openStringHistoryDatabase(final String name, int maxHistoryLength, Directions direction)
    throws SQLException
  {
    return new HistoryDatabase<String>(name,maxHistoryLength,direction)
    {
      public PreparedStatement prepareInit(Database database)
        throws SQLException
      {
        PreparedStatement preparedStatement = database.prepareStatement("CREATE TABLE IF NOT EXISTS "+name+" ( "+
                                                                        "  id        INTEGER PRIMARY KEY, "+
                                                                        "  datetime  INTEGER DEFAULT (DATETIME('now')), "+
                                                                        "  data      TEXT "+
                                                                        ");"
                                                                       );

        return preparedStatement;
      }
      public PreparedStatement prepareInsert(Database database, String string)
        throws SQLException
      {
        PreparedStatement preparedStatement =  database.prepareStatement("INSERT INTO "+name+" (datetime,data) VALUES (DATETIME('now'),?);");
        preparedStatement.setString(1,string);
        return preparedStatement;
      }
      public PreparedStatement prepareDelete(Database database, int id)
        throws SQLException
      {
        PreparedStatement preparedStatement =  database.prepareStatement("DELETE FROM "+name+" WHERE id=?;");
        preparedStatement.setInt(1,id);
        return preparedStatement;
      }
      public String getResult(ResultSet resultSet)
        throws SQLException
      {
        return resultSet.getString("data");
      }
      public int dataCompareTo(String string0, String string1)
      {
        return string0.compareTo(string1);
      }
    };
  }

  /** add
   * @param historyList
   * @param maxHistoryLength max. length of history or HISTORY_LENGTH_INFINTE
   * @param direction history direction order
   * @param addEntry
   */
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
