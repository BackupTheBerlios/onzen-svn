/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
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

import org.eclipse.swt.widgets.Shell;

/****************************** Classes ********************************/

/** commit message broadcast receive thread
 */
class CommitMessageReceiveBroadcast extends Thread
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private MulticastSocket socket;
  private DatagramPacket  packet;
  private byte[]          buffer = new byte[4*1024];

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create commit message broadcast handler
   * @param history history list
   * @param address UDP broadcasting address
   * @param port UDP broadcasting port
   */
  CommitMessageReceiveBroadcast(InetAddress address, int port)
    throws UnknownHostException,SocketException,IOException
  {
    this.socket = new MulticastSocket(port);
    this.socket.joinGroup(address);
    this.packet = new DatagramPacket(buffer,buffer.length);
  }

  /** run broadcast handler
   */
  public void run()
  {
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
        String text     = data[2];

        // filter out own messages
        if (userName.equals(CommitMessage.USER_NAME) && id.equals(CommitMessage.ID))
        {
          continue;
        }

        // convert message
        ArrayList<String> lineList = new ArrayList<String>();
        for (String string : text.split("\\\\n"))
        {
          lineList.add(string.replace("\\\\","\\"));
        }
        String[] message = lineList.toArray(new String[lineList.size()]);

        // add to history
        CommitMessage.addToHistory(message);
      }
      catch (IOException unusedException)
      {
        // ignored
      }
    }
  }
}

/** commit message
 */
class CommitMessage
{
  // --------------------------- constants --------------------------------
  protected final static String  USER_NAME = System.getProperty("user.name");
  protected final static String  ID        = String.format("%08x",System.currentTimeMillis());

  // --------------------------- variables --------------------------------
  protected static HistoryDatabase      historyDatabase = null;
  private   static LinkedList<String[]> history;
  private   static DatagramSocket       socket;

  private   final String                summary;
  private   final String[]              message;
  private   File                        tmpFile;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** init commit message functions
   */
  public static void init()
    throws SQLException
  {
    // start message broadcast
    startBroadcast();

    // init database
    historyDatabase = new HistoryDatabase<String[]>("messages",Settings.maxMessageHistory)
    {
      public PreparedStatement prepareInit(Database database)
        throws SQLException
      {
        PreparedStatement preparedStatement = database.prepareStatement("CREATE TABLE IF NOT EXISTS messages ( "+
                                                                        "  id        INTEGER PRIMARY KEY, "+
                                                                        "  datetime  INTEGER DEFAULT (DATETIME('now')), "+
                                                                        "  message   TEXT "+
                                                                        ");"
                                                                       );

        return preparedStatement;
      }
      public PreparedStatement prepareInsert(Database database, String[] lines)
        throws SQLException
      {
        PreparedStatement preparedStatement =  database.prepareStatement("INSERT INTO messages (datetime,message) VALUES (DATETIME('now'),?);");
        preparedStatement.setString(1,Database.linesToData(lines));
        return preparedStatement;
      }
      public PreparedStatement prepareDelete(Database database, int id)
        throws SQLException
      {
        PreparedStatement preparedStatement =  database.prepareStatement("DELETE FROM messages WHERE id=?;");
        preparedStatement.setInt(1,id);
        return preparedStatement;
      }
      public String[] getResult(ResultSet resultSet)
        throws SQLException
      {
        return Database.dataToLines(resultSet.getString("message"));
      }
      public int dataCompareTo(String[] lines0, String[] lines1)
      {
        return compareLines(lines0,lines1);
      }
      @Override
      public boolean dataEquals(String s0[], String s1[]) { return equalLines(s0,s1); }
    };

    // load commit message history from database
    history = historyDatabase.getHistory();
  }

  /** get message history
   * @param shell shell (for error dialog) or null
   * @return message history array (as a copy!)
   */
  public static LinkedList<String[]> getHistory(Shell shell)
  {
    // load history if needed
    if (history == null)
    {
      try
      {
        if (historyDatabase == null) init();
        history = historyDatabase.getHistory();
      }
      catch (SQLException exception)
      {
Dprintf.dprintf("");
exception.printStackTrace();
        Onzen.printWarning("Cannot load commit message history from database (error: %s)",exception.getMessage());
        if (shell != null)
        {
          Dialogs.error(shell,"Cannot load commit message history from database (error: %s)",exception.getMessage());
        }

        history = new LinkedList<String[]>();
      }
    }

    return (LinkedList<String[]>)history.clone();
  }

  /** get message history
   * @return message history array
   */
  public static LinkedList<String[]> getHistory()
  {
    return getHistory(null);
  }

  /** format message with multiple lines
   * @param message to format
   * @param separator line separator
   * @return format message
   */
  public static String format(String message, String separator)
  {
    StringBuilder buffer = new StringBuilder();

    String[] lines = message.split(separator);
    if      (lines.length > 1)
    {
      for (String line : lines)
      {
        line = line.trim();
        if (!line.isEmpty())
        {
          if (Settings.autoCommitMessagePrefix != null)
          {
            if (!line.startsWith(Settings.autoCommitMessagePrefix)) line = Settings.autoCommitMessagePrefix+" "+line;
          }
          buffer.append(line);
          buffer.append(separator);
        }
      }
    }
    else if (lines.length == 1)
    {
      String line = lines[0].trim();
      if (!line.isEmpty())
      {
       buffer.append(line);
       buffer.append(separator);
      }
    }

    return buffer.toString();
  }


  /** create commit message
   * @param summary summary line
   * @param message message lines
   */
  CommitMessage(String summary, String[] message)
  {
    // discard empty lines at beginning/end of message
    int i0 = 0;
    while ((i0 < message.length) && message[i0].trim().isEmpty())
    {
      i0++;
    }
    int i1 = message.length-1;
    while ((i1 >= i0) && message[i1].trim().isEmpty())
    {
      i1--;
    }

    this.summary = summary;
    this.message = Arrays.copyOfRange(message,i0,i1+1);
  }

  /** create commit message
   * @param message message lines
   */
  CommitMessage(String[] message)
  {
    this(null,message);
  }

  /** done commit message
   */
  public void done()
  {
    if (tmpFile != null) tmpFile.delete();
  }

  /** add message to history
   */
  public void addToHistory()
  {
    if (message.length > 0)
    {
      // broadcast message
      try
      {
        // convert message to string
        StringBuilder buffer = new StringBuilder();
        for (String line : message)
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
      catch (IOException unusedException)
      {
        // ignored
      }

      // store into history
      addToHistory(message);
    }
  }

  /** check if message if empty
   * @return true iff message empty
   */
  public boolean isEmpty()
  {
    return message.length == 0;
  }


  /** get summary
   * @param separator line separator
   * @return summary
   */
  public String getSummary()
  {
    return summary;
  }

  /** get message text
   * @param separator line separator
   * @return text
   */
  public String getMessage(String separator)
  {
    StringBuilder buffer = new StringBuilder();

    if (summary != null)
    {
      buffer.append(summary);
      buffer.append(separator);
    }
    buffer.append(StringUtils.join(message,separator));

    return buffer.toString();
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
    throws IOException
  {
    if (tmpFile == null)
    {
      // write to file
      tmpFile = File.createTempFile("msg",".tmp",new File(Settings.tmpDirectory));
      PrintWriter output = new PrintWriter(new FileWriter(tmpFile.getPath()));
      if (summary != null)
      {
        output.println(summary);
      }
      for (String line : this.message)
      {
        output.println(line);
      }
      output.close();
    }

    return tmpFile.getAbsolutePath();
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommitMessage {summary: "+summary+", message: "+getMessage()+"}";
  }

  //-----------------------------------------------------------------------

  /** start message broadcasting receiver
   */
  private static void startBroadcast()
  {
    try
    {
      // create UDP broadcasting socket
      InetAddress address = InetAddress.getByName(Settings.messageBroadcastAddress);
      socket = new DatagramSocket();
      socket.connect(address,Settings.messageBroadcastPort);
      socket.setBroadcast(true);

      // start commit message broadcast receive thread
      CommitMessageReceiveBroadcast commitMessageReceiveBroadcast = new CommitMessageReceiveBroadcast(address,Settings.messageBroadcastPort);
      commitMessageReceiveBroadcast.setDaemon(true);
      commitMessageReceiveBroadcast.start();
    }
    catch (UnknownHostException unusedException)
    {
      // ignored
    }
    catch (SocketException unusedException)
    {
      // ignored
    }
    catch (IOException unusedException)
    {
      // ignored
    }
  }

  /** compare lines (ignore empty lines, space and case)
   * @param lines0, lines1 lines to compare
   * @return -1 if lines0 < lines1, 0 if lines0 == lines1, 1 if lines0 > lines1
   */
  private static int compareLines(String[] lines0, String[] lines1)
  {
    int result = 0;

    if ((lines0 != null) && (lines1 != null))
    {
      int i0 = 0;
      int i1 = 0;
      while ((i0 < lines0.length) && (i1 < lines1.length) && (result == 0))
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
          result = line0.compareToIgnoreCase(line1);
        }

        // next line
        i0++;
        i1++;
      }
      if (result == 0)
      {
        if      (i0 < i1) result = -1;
        else if (i0 > i1) result =  1;
      }
    }
    else
    {
      result = 0;
    }

    return result;
  }

  /** compare lines if they are equal (ignore empty lines, space and case)
   * @param lines0, lines1 lines to compare
   * @return true iff lines are equal
   */
  private static boolean equalLines(String[] lines0, String[] lines1)
  {
    return compareLines(lines0,lines1) == 0;
  }

  /** add message to history
   * @param message message text
   */
  protected static void addToHistory(String[] message)
  {
    // load history if needed
    if (history == null)
    {
      try
      {
        if (historyDatabase == null) init();
        history = historyDatabase.getHistory();
      }
      catch (SQLException exception)
      {
        Onzen.printWarning("Cannot load commit message history from database (error: %s)",exception.getMessage());
        return;
      }
    }

    synchronized(history)
    {
      if (!equalLines(message,history.peekLast()))
      {
        // add to history list
        history.addLast(message);

        // shorten history list
        while (history.size() > Settings.maxMessageHistory)
        {
          history.removeFirst();
        }

        // store into history database
        try
        {
          if (historyDatabase == null) init();
          historyDatabase.add(message);
        }
        catch (SQLException exception)
        {
          Onzen.printWarning("Cannot store message into history database (error: %s)",exception.getMessage());
exception.printStackTrace();
          return;
        }
      }
    }
  }
}

/* end of file */
