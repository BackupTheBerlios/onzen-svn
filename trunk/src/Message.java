/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Message.java,v $
* $Revision: 1.3 $
* $Author: torsten $
* Contents: message functions
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

import java.util.HashSet;
import java.util.LinkedList;

/****************************** Classes ********************************/

/** message broadcast receive thread
 */
class MessageReceiveBroadcast extends Thread
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private LinkedList<String> history;
  private MulticastSocket    socket;
  private DatagramPacket     packet;
  private byte[]             buffer = new byte[4*1024];

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create message broadcast handler
   * @param history history list
   * @param address UDP broadcasting address
   * @param port UDP broadcasting port
   */
  MessageReceiveBroadcast(LinkedList<String> history, InetAddress address, int port)
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
    String   line;
    Object[] data = new Object[3];
    String   userName,id,string,message;

    for (;;)
    {
      try
      {
        // wait for UDP message
        socket.receive(packet);
        line = new String(packet.getData(),0,packet.getLength()).trim();
//Dprintf.dprintf("line=#%s#",line);

        // parse
        if (!StringParser.parse(line,"%s %s % s",data))
        {
          continue;
        }
        userName = (String)data[0];
        id       = (String)data[1];
        string   = (String)data[2];

        // filter out own messages
        if (userName.equals(Message.USER_NAME) && id.equals(Message.ID))
        {
          continue;
        }

        // convert message
        message  = string.replace("\\n","\n").
                          replace("\\\\","\\");

        // add to history
        synchronized(history)
        {
          if (!history.peekLast().equals(message))
          {
            // add to history
            history.addLast(message);

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

  private static final String ONZEN_HISTORY_DATABASE_FILE_NAME = Settings.ONZEN_DIRECTORY+File.separator+"history.db";
  private static final int    ONZEN_HISTORY_DATABASE_VERSION   = 1;

  // --------------------------- variables --------------------------------
  private static                LinkedList<String> history = new LinkedList<String>();
  private static DatagramSocket socket;

  private final String message;
  private File         tmpFile;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** load message history from database
   */
  public static void loadHistory()
  {
    Connection connection = null;
    try
    {
      PreparedStatement preparedStatement;
      ResultSet         resultSet;

      // open database
      connection = openHistoryDatabase();

      synchronized(history)
      {
        // load history
        preparedStatement = connection.prepareStatement("SELECT message FROM messages ORDER BY datetime ASC;");
        resultSet = preparedStatement.executeQuery();
        history.clear();
        while (resultSet.next())
        {
          history.addLast(resultSet.getString("message").trim()+"\n");
        }
        resultSet.close();

        // shorten history
        while (history.size() > Settings.maxMessageHistory)
        {
          history.removeFirst();
        }
      }

      // close database
      closeHistoryDatabase(connection); connection = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot load message history from database (error: %s)",exception.getMessage());
      return;
    }
    finally
    {
      try { if (connection != null) closeHistoryDatabase(connection); } catch (SQLException exception) { /* ignored */ }
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
  public static String[] getHistory()
  {
    synchronized(history)
    {
      return history.toArray(new String[history.size()]);
    }
  }

  /** add message to history
   * @param message message text
   */
  public void addToHistory()
  {
    if (!message.isEmpty())
    {
      // broadcast message
      try
      {
        // convert message
        String string = message.replace("\\","\\\\").
                                replace("\n","\\n");

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

  /** create message
   * @param message message text
   */
  Message(String message)
  {
    this.message = message.trim();
    try
    {
      tmpFile = File.createTempFile("msg",".tmp",new File(Settings.tmpDirectory));

      PrintWriter output = new PrintWriter(new FileWriter(tmpFile.getPath()));
      output.println(message);
      output.close();
    }
    catch (IOException exception)
    {
Dprintf.dprintf("exception=%s",exception);
    }
  }

  /** done message
   */
  public void done()
  {
    tmpFile.delete();
  }

  /** check if message if empty
   * @return true iff message empty
   */
  public boolean isEmpty()
  {
    return message.isEmpty();
  }

  /** get message text
   * @return text
   */
  public String getMessage()
  {
    return message+"\n";
  }

  /** get message file name
   * @return file name
   */
  public String getFileName()
  {
    return tmpFile.getAbsolutePath();
  }

  //-----------------------------------------------------------------------

  /** open history database
   * @return connection
   */
  private static Connection openHistoryDatabase()
    throws SQLException
  {
    Connection connection;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      // load SQLite driver class
      Class.forName("org.sqlite.JDBC");

      // open database
      connection = DriverManager.getConnection("jdbc:sqlite:"+ONZEN_HISTORY_DATABASE_FILE_NAME);

      // create tables if needed
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS messages ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  message  TEXT "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT name,value FROM meta");
      if (!resultSet.next())
      {
        preparedStatement = connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
        preparedStatement.setString(1,Integer.toString(ONZEN_HISTORY_DATABASE_VERSION));
        preparedStatement.executeUpdate();
      }
      resultSet.close();
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
  private static void closeHistoryDatabase(Connection connection)
    throws SQLException
  {
    connection.setAutoCommit(true);
    connection.close();
  }

  /** store message into history database
   */
  private void storeHistory()
  {
    synchronized(history)
    {
      if (!history.peekLast().equals(message))
      {
        // add to history
        history.addLast(message);

        // shorten history
        while (history.size() > Settings.maxMessageHistory)
        {
          history.removeFirst();
        }

        // store in database
        Connection connection = null;
        try
        {
          PreparedStatement preparedStatement;
          ResultSet         resultSet;

          // open database
          connection = openHistoryDatabase();

          // add to database
          preparedStatement = connection.prepareStatement("INSERT INTO messages (message) VALUES (?);");
          preparedStatement.setString(1,message);
          preparedStatement.execute();

          // shorten message table size
          boolean done = false;
          do
          {
            preparedStatement = connection.prepareStatement("SELECT id FROM messages ORDER BY datetime DESC LIMIT ?,1;");
            preparedStatement.setInt(1,Settings.maxMessageHistory);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next())
            {
              preparedStatement = connection.prepareStatement("DELETE FROM messages WHERE id=?;");
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
          closeHistoryDatabase(connection); connection = null;
        }
        catch (SQLException exception)
        {
          Onzen.printWarning("Cannot store message into history database (error: %s)",exception.getMessage());
          return;
        }
        finally
        {
          try { if (connection != null) closeHistoryDatabase(connection); } catch (SQLException exception) { /* ignored */ }
        }
      }
    }
  }
}

/* end of file */
