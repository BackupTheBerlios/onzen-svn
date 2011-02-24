/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Message.java,v $
* $Revision: 1.3 $
* $Author: torsten $
* Contents: commit message functions
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

/****************************** Classes ********************************/

/** message broadcast receive thread
 */
class MessageReceiveBroadcast extends Thread
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private LinkedList<String[]> history;
  private MulticastSocket      socket;
  private DatagramPacket       packet;
  private byte[]               buffer = new byte[4*1024];

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create message broadcast handler
   * @param history history list
   * @param address UDP broadcasting address
   * @param port UDP broadcasting port
   */
  MessageReceiveBroadcast(LinkedList<String[]> history, InetAddress address, int port)
    throws UnknownHostException,SocketException,IOException
  {
    this.history  = history;
    this.socket   = new MulticastSocket(port);
    this.packet   = new DatagramPacket(buffer,buffer.length);
  }

  /** run broadcast handler
   */
  public void run()
  {
//    Object[] data = new Object[3];
    for (;;)
    {
      try
      {
        // wait for UDP message
        socket.receive(packet);
        String line = new String(packet.getData(),0,packet.getLength()).trim();
//Dprintf.dprintf("line=#%s#",line);

        // parse
        String[] data = line.split("\\s",3);
        if (data.length != 3)
        {
          continue;
        }
        String userName = data[0];
        String id       = data[1];
        String message  = data[2];

        // filter out own messages
        if (userName.equals(Message.USER_NAME) && id.equals(Message.ID))
        {
          continue;
        }

        // convert message
        ArrayList<String> lineList = new ArrayList<String>();
        for (String string : message.split("\\\\n"))
        {
          lineList.add(string.replace("\\\\","\\"));
        }
        String[] lines = lineList.toArray(new String[lineList.size()]);

        // add to history
        synchronized(history)
        {
          if (!Message.equalLines(lines,history.peekLast()))
          {
            // add to history
            history.addLast(lines);

            // shorten history
            while (history.size() > Settings.maxMessageHistory)
            {
              history.removeFirst();
            }
          }
        }
      }
      catch (IOException exception)
      {
        // ignored
      }
    }
  }
}

/** message
 */
class Message
{
  // --------------------------- constants --------------------------------
  protected final static String  USER_NAME = System.getProperty("user.name");
  protected final static String  ID        = String.format("%08x",System.currentTimeMillis());

  private static final String    HISTORY_DATABASE_NAME    = "history";
  private static final int       HISTORY_DATABASE_VERSION = 1;

  // --------------------------- variables --------------------------------
  private static                LinkedList<String[]> history = new LinkedList<String[]>();
  private static DatagramSocket socket;

  private final String[]        lines;
  private File                  tmpFile;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** load message history from database
   */
  public static void loadHistory()
  {
    Database database = null;
    try
    {
      PreparedStatement preparedStatement;
      ResultSet         resultSet;

      // open database
      database = openHistoryDatabase();

      synchronized(history)
      {
        // load history
        preparedStatement = database.connection.prepareStatement("SELECT message FROM messages ORDER BY datetime ASC;");
        resultSet = preparedStatement.executeQuery();
        history.clear();
        while (resultSet.next())
        {
          history.addLast(Database.dataToLines(resultSet.getString("message").trim()));
        }
        resultSet.close();

        // shorten history
        while (history.size() > Settings.maxMessageHistory)
        {
          history.removeFirst();
        }
      }

      // close database
      closeHistoryDatabase(database);
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot load message history from database (error: %s)",exception.getMessage());
      return;
    }
    finally
    {
      try { if (database != null) closeHistoryDatabase(database); } catch (SQLException exception) { /* ignored */ }
    }
  }

  /** start message broadcasting receiver
   */
  public static void startBroadcast()
  {
    try
    {
      // create UDP broadcasting socket
      InetAddress address = InetAddress.getByName(Settings.messageBroadcastAddress);
      socket = new DatagramSocket();
      socket.connect(address,Settings.messageBroadcastPort);
      socket.setBroadcast(true);

      // start broadcast receive thread
      MessageReceiveBroadcast messageReceiveBroadcast = new MessageReceiveBroadcast(history,address,Settings.messageBroadcastPort);
      messageReceiveBroadcast.setDaemon(true);
      messageReceiveBroadcast.start();
    }
    catch (UnknownHostException exception)
    {
      // ignored
    }
    catch (SocketException exception)
    {
      // ignored
    }
    catch (IOException exception)
    {
      // ignored
    }
  }

  /** get message history
   * @return message history array
   */
  public static String[][] getHistory()
  {
    synchronized(history)
    {
      return history.toArray(new String[history.size()][]);
    }
  }

  /** create message
   * @param line message lines
   */
  Message(String[] lines)
    throws IOException
  {
    // discard empty lines at beginning/end
    int i0 = 0;
    while ((i0 < lines.length) && lines[i0].trim().isEmpty())
    {
      i0++;
    }
    int i1 = lines.length-1;
    while ((i1 >= i0) && lines[i1].trim().isEmpty())
    {
      i1--;
    }
    this.lines = Arrays.copyOfRange(lines,i0,i1+1);

    // write to file
    tmpFile = File.createTempFile("msg",".tmp",new File(Settings.tmpDirectory));
    PrintWriter output = new PrintWriter(new FileWriter(tmpFile.getPath()));
    for (String line : this.lines)
    {
      output.println(line);
    }
    output.close();
  }

  /** done message
   */
  public void done()
  {
    tmpFile.delete();
  }

  /** add message to history
   * @param message message text
   */
  public void addToHistory()
  {
    if (lines.length > 0)
    {
      // broadcast message
      try
      {
        // convert message to string
        StringBuilder buffer = new StringBuilder();
        for (String line : lines)
        {
          buffer.append(line.replace("\\","\\\\"));
          buffer.append("\\n");
        }
        String string = buffer.toString();

        // broadcast message to other running instances of Onzen
        byte[] data = String.format("%s %s %s\n",USER_NAME,ID,string).getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data,data.length);
        socket.send(datagramPacket);
      }
      catch (IOException exception)
      {
        // ignored
      }

      // store in history
      storeHistory();
    }
  }

  /** check if message if empty
   * @return true iff message empty
   */
  public boolean isEmpty()
  {
    return lines.length == 0;
  }

  /** get message text
   * @param separator line separator
   * @return text
   */
  public String getMessage(String separator)
  {
    return StringUtils.join(lines,separator);
  }

  /** get message text
   * @return text (separated with \n, terminated with \n)
   */
  public String getMessage()
  {
    return getMessage("\n")+"\n";
  }

  /** get message file name
   * @return file name
   */
  public String getFileName()
  {
    return tmpFile.getAbsolutePath();
  }

  //-----------------------------------------------------------------------

  /** compare lines if they are equal (ignore empty lines, space and case)
   * @param otherLines lines to compare with
   * @return true iff lines are equal  (ignore empty lines, space and case)
   */
  protected static boolean equalLines(String[] lines0, String[] lines1)
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

  /** open history database
   * @return database
   */
  private static Database openHistoryDatabase()
    throws SQLException
  {
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
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  message  TEXT "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = database.connection.createStatement();
      resultSet = statement.executeQuery("SELECT name,value FROM meta");
      if (!resultSet.next())
      {
        preparedStatement = database.connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
        preparedStatement.setString(1,Integer.toString(HISTORY_DATABASE_VERSION));
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

    return database;
  }

  /** close history database
   * @param database database
   */
  private static void closeHistoryDatabase(Database database)
    throws SQLException
  {
    database.close();
  }

  /** store message into history database
   */
  private void storeHistory()
  {
    synchronized(history)
    {
      if (!equalLines(lines,history.peekLast()))
      {
        // add to history
        history.addLast(lines);

        // shorten history
        while (history.size() > Settings.maxMessageHistory)
        {
          history.removeFirst();
        }

        // store in database
        Database database = null;
        try
        {
          PreparedStatement preparedStatement;
          ResultSet         resultSet;

          // open database
          database = openHistoryDatabase();

          // add to database
          preparedStatement = database.connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
          preparedStatement.setString(1,Database.linesToData(lines));
          preparedStatement.execute();

          // shorten message table size
          boolean done = false;
          do
          {
            preparedStatement = database.connection.prepareStatement("SELECT id FROM messages ORDER BY datetime DESC LIMIT ?,1;");
            preparedStatement.setInt(1,Settings.maxMessageHistory);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next())
            {
              preparedStatement = database.connection.prepareStatement("DELETE FROM messages WHERE id=?;");
              preparedStatement.setLong(1,resultSet.getLong("id"));
              preparedStatement.execute();
            }
            else
            {
              done = true;
            }
            resultSet.close();
          }
          while (!done);

          // close database
          closeHistoryDatabase(database);
        }
        catch (SQLException exception)
        {
          Onzen.printWarning("Cannot store message into history database (error: %s)",exception.getMessage());
          return;
        }
        finally
        {
          try { if (database != null) closeHistoryDatabase(database); } catch (SQLException exception) { /* ignored */ }
        }
      }
    }
  }
}

/* end of file */
