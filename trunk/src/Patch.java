/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Patch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: patch function
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileReader;
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/****************************** Classes ********************************/

/** patch
 */
class Patch
{
  // --------------------------- constants --------------------------------
  private static final String ONZEN_PATCHES_DATABASE_FILE_NAME = Settings.ONZEN_DIRECTORY+File.separator+"patches.db";
  private static final int    ONZEN_PATCHES_DATABASE_VERSION   = 1;

  private final int PATCH_NUMBER_NONE = -1;

  public enum States
  {
    NONE,
    REVIEW,
    COMMITED,
    APPLIED,
    DISCARDED;

    /** convert ordinal value to enum
     * @param value ordinal value
     * @return enum
     */
    static States toEnum(int value)
    {
      States state;

      switch (value)
      {
        case 0:  return NONE;
        case 1:  return REVIEW;
        case 2:  return COMMITED;
        case 3:  return APPLIED;
        case 4:  return DISCARDED;
        default: return NONE;
      }
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      switch (this)
      {
        case NONE:      return "none";
        case REVIEW:    return "review";
        case COMMITED:  return "commited";
        case APPLIED:   return "applied";
        case DISCARDED: return "discarded";
        default:        return "none";
      }
    }
  };

  // --------------------------- variables --------------------------------
  public  States   state;
  public  String[] fileNames;
  public  String   summary;
  public  String   message;
  public  String[] lines;

  private String   rootPath;
  private int      id;
  private int      number;
  private File     tmpFile;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get patches from database
   * @param rootPath root path of patches
   * @param filterStates states of patches
   * @param n max. number of patches to return
   * @return patches array
   */
  public static Patch[] getPatches(String rootPath, EnumSet<States> filterStates, int n)
  {
    ArrayList<Patch> patchList = new ArrayList<Patch>();

    Connection connection = null;
    try
    {
      Statement         statement;
      PreparedStatement preparedStatement;
      ResultSet         resultSet1,resultSet2;

      // open database
      connection = openPatchesDatabase();

      // get patches (Note: there is no way to use prepared statements with variable "IN"-operator)
      statement = connection.createStatement();
      resultSet1 = null;
      try
      {
        resultSet1 = statement.executeQuery("SELECT "+
                                            "  id, "+
                                            "  state, "+
                                            "  summary, "+
                                            "  message, "+
                                            "  data "+
                                            "FROM patches "+
                                            "WHERE     rootPath='"+rootPath+"'"+
                                            "      AND (   state="+Patch.States.NONE.ordinal()+" "+
                                            "           OR state IN ("+StringUtils.join(filterStates,",",true)+") "+
                                            "          )"+
                                            "LIMIT 0,"+n+
                                            ";"
                                           );

        preparedStatement = connection.prepareStatement("SELECT "+
                                                        "  fileName "+
                                                        "FROM files "+
                                                        "WHERE patchId=? "+
                                                        ";"
                                                       );
        while (resultSet1.next())
        {
          // get patch data
          int      id      = resultSet1.getInt("id"); 
          States   state   = States.toEnum(resultSet1.getInt("state"));
          String   summary = resultSet1.getString("summary"); if (summary == null) summary = "";
          String   message = resultSet1.getString("message"); if (message == null) message = "";
          String[] lines   = resultSet1.getString("data").split("\n"); 
  //Dprintf.dprintf("id=%d s=%s",id,summary);

          // get file names
          ArrayList<String> fileNameList = new ArrayList<String>();
          preparedStatement.setInt(1,id);
          resultSet2 = null;
          try
          {
            resultSet2 = preparedStatement.executeQuery();
            while (resultSet2.next())
            {
              fileNameList.add(resultSet2.getString("fileName"));
            }
            resultSet2.close(); resultSet2 = null;
          }
          finally
          {
            if (resultSet2 != null) resultSet2.close();
          }
          String[] fileNames = fileNameList.toArray(new String[fileNameList.size()]);

          // add to list
          patchList.add(new Patch(rootPath,
                                  id,
                                  state,
                                  summary,
                                  message,
                                  lines,
                                  fileNames
                                 )
                           );
        }

        resultSet1.close(); resultSet1 = null;
      }
      finally
      {
        if (resultSet1 != null) resultSet1.close();
      }

      // close database
      closePatchesDatabase(connection); connection = null;
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot get patches from database (error: %s)",exception.getMessage());
      return null;
    }
    finally
    {
      try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
    }

    return patchList.toArray(new Patch[patchList.size()]);
  }

  /** create patch
   * @param rootPath root path
   * @param id patch id
   * @param state state
   * @param summary summary text
   * @param message message text
   * @param lines patch lines
   * @param fileNames file names
   */
  Patch(String rootPath, int id, States state, String summary, String message, String[] lines, String[] fileNames)
  {
    this.state     = state;
    this.summary   = summary;
    this.message   = message;
    this.fileNames = fileNames;

    this.rootPath  = rootPath;
    this.id        = id;
    this.number    = -1;
    this.lines     = lines;
    this.tmpFile   = null;
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param lines patch lines
   */
  Patch(String rootPath, HashSet<FileData> fileDataSet, String[] lines)
  {
    this(rootPath,-1,States.NONE,"","",lines,null);

    if (fileDataSet != null)
    {
      // set file names
      ArrayList<String> fileList = new ArrayList<String>();
      for (FileData fileData : fileDataSet)
      {
        fileList.add(fileData.getFileName());
      }
      this.fileNames = fileList.toArray(new String[fileList.size()]);
    }
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param text patch text
   */
/*  Patch(String rootPath, HashSet<FileData> fileDataSet, String text)
  {
// NYI: \n?
    this(rootPath,StringUtils.split(text,"\n"));
  }*/

  /** create patch
   * @param rootPath root path
   * @param lines patch lines
   */
  Patch(String rootPath, String[] lines)
  {
    this(rootPath,null,lines);
  }

  /** create patch
   * @param rootPath root path
   * @param id id
   */
  Patch(String rootPath, int id)
  {
    this(rootPath,id,States.NONE,"","",null,null);
  }

  /** create patch
   * @param id id
   */
  Patch(int id)
  {
    this(null,id,States.NONE,"","",null,null);
  }

  /** done patch
   */
  public void done()
  {
    // clean-up not used/saved patch: discard allocated patch number
    if ((id < 0) && (number >= 0))
    {
      Connection connection = null;
      try
      {
        PreparedStatement preparedStatement;

        // open database
        connection = openPatchesDatabase();

        // remove not use patch number
        preparedStatement = connection.prepareStatement("DELETE FROM numbers WHERE id=?;");
        preparedStatement.setInt(1,number);
        preparedStatement.executeUpdate();

        // close database
        closePatchesDatabase(connection); connection = null;

        number = -1;
      }
      catch (SQLException exception)
      {
        Onzen.printWarning("Cannot clean-up patches database (error: %s)",exception.getMessage());
        return;
      }
      finally
      {
        try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
      }
    }

    // delete tempory file
    if (tmpFile != null) tmpFile.delete();
  }

  /** get patch number
   * @return patch number or -1 if no patch number could be allocated
   */
  public int getNumber()
  {
    // reserve a patch number if required
    if (number < 0)
    {
      Connection connection = null;
      try
      {
        PreparedStatement preparedStatement;

        // open database
        connection = openPatchesDatabase();

        // create number
        SQLException sqlException = null;
        int          retryCount   = 0;
        do
        {
          try
          {
            // create empty patch number entry
            preparedStatement = connection.prepareStatement("INSERT INTO numbers (patchId) VALUES (?);");
            preparedStatement.setInt(1,id);
            preparedStatement.executeUpdate();

            // get patch number (=id)
            number = getLastInsertId(connection);
          }
          catch (SQLException exception)
          {
            // fail -> wait and retry
            sqlException = exception;
            retryCount++;

            try { Thread.sleep(250); } catch (InterruptedException interruptedException) { /* ignored */ }
          }
        }
        while ((number < 0) && (retryCount < 5));

        // close database
        closePatchesDatabase(connection); connection = null;
      }
      catch (SQLException exception)
      {
        // ignored
      }
      finally
      {
        try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
      }
    }

    return number;
  }

  /** get patch file name
   * @return temporary file name or null
   */
  public String getFileName()
  {
    if (tmpFile == null)
    {
      // write temporary file
      try
      {
        tmpFile = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));
        write(tmpFile);
      }
      catch (IOException exception)
      {
Dprintf.dprintf("");
      }
    }

    return (tmpFile != null) ? tmpFile.getAbsolutePath() : null;
  }

  /** save patch into database
   * @return database id
   */
  public int save()
    throws SQLException
  {
    Connection connection = null;
    try
    {
      Statement         statement;
      PreparedStatement preparedStatement;

      // open database
      connection = openPatchesDatabase();

      // store patch data
      if (id >= 0)
      {
        // update
        preparedStatement = connection.prepareStatement("UPDATE patches SET rootPath=?,state=?,summary=?,message=?,data=? WHERE id=?;");
        preparedStatement.setString(1,rootPath);
        preparedStatement.setInt(2,state.ordinal());
        preparedStatement.setString(3,summary);
        preparedStatement.setString(4,message);
        preparedStatement.setString(5,StringUtils.join(lines,"\n"));
        preparedStatement.setInt(6,id);
        preparedStatement.executeUpdate();
      }
      else
      {
        // insert
        preparedStatement = connection.prepareStatement("INSERT INTO patches (rootPath,state,summary,message,data) VALUES (?,?,?,?,?);");
        preparedStatement.setString(1,rootPath);
        preparedStatement.setInt(2,state.ordinal());
        preparedStatement.setString(3,summary);
        preparedStatement.setString(4,message);
        preparedStatement.setString(5,StringUtils.join(lines,"\n"));
        preparedStatement.executeUpdate(); 
        id = getLastInsertId(connection);

        if (number >= 0)
        {
          // update patch number entry
          preparedStatement = connection.prepareStatement("UPDATE numbers SET patchId=? WHERE id=?;");
          preparedStatement.setInt(1,id);
          preparedStatement.setInt(2,number);
          preparedStatement.executeUpdate();
        }
      }

      if (fileNames != null)
      {
        // insert files
        preparedStatement = connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();
        preparedStatement = connection.prepareStatement("INSERT INTO files (patchId,fileName) VALUES (?,?);");
        for (String fileName : fileNames)
        {
          preparedStatement.setInt(1,number);
          preparedStatement.setString(2,fileName);
          preparedStatement.executeUpdate();
        }
      }

      // close database
      closePatchesDatabase(connection); connection = null;
    }
    finally
    {
      try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
    }

    return id;
  }

  /** load patch from database
   */
  public void load()
    throws SQLException
  {
    if (id >= 0)
    {
      Connection connection = null;
      try
      {
        Statement         statement;
        PreparedStatement preparedStatement;
        ResultSet         resultSet;

        // open database
        connection = openPatchesDatabase();

        // load patch data
        preparedStatement = connection.prepareStatement("SELECT "+
                                                        "  rootPath, "+
                                                        "  state, "+
                                                        "  summary, "+
                                                        "  message, "+
                                                        "  data "+
                                                        "FROM patches "+
                                                        "WHERE id=? "+
                                                        ";"
                                                       );
        preparedStatement.setInt(1,id);
        resultSet = null;
        try
        {
          resultSet = preparedStatement.executeQuery();
          if (resultSet.next())
          {
            rootPath = resultSet.getString("rootPath");
Dprintf.dprintf("rootPath=%s",rootPath);
            state    = States.toEnum(resultSet.getInt("state"));
            summary  = resultSet.getString("summary"); if (summary == null) summary = "";
            message  = resultSet.getString("message"); if (message == null) message = "";
            lines    = resultSet.getString("data").split("\n"); 
          }
          else
          {
            throw new SQLException("patch "+id+" not found");
          }
          resultSet.close(); resultSet = null;
        }
        finally
        {
          if (resultSet != null) resultSet.close();
        }

        // load file names
        ArrayList<String> fileNameList = new ArrayList<String>();
        preparedStatement = connection.prepareStatement("SELECT "+
                                                        "  fileName "+
                                                        "FROM files "+
                                                        "WHERE patchId=? "+
                                                        ";"
                                                       );
        preparedStatement.setInt(1,id);
        resultSet = null;
        try
        {
          resultSet = preparedStatement.executeQuery();
          while (resultSet.next())
          {
            fileNameList.add(resultSet.getString("fileName"));
          }
          resultSet.close(); resultSet = null;
        }
        finally
        {
          if (resultSet != null) resultSet.close();
        }
        fileNames = fileNameList.toArray(new String[fileNameList.size()]);

        // close database
        closePatchesDatabase(connection); connection = null;
      }
      finally
      {
        try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
      }
    }
  }

  /** delete patch from database
   */
  public void delete()
    throws SQLException
  {
    if (id >= 0)
    {
      Connection connection = null;
      try
      {
        Statement         statement;
        PreparedStatement preparedStatement;
        ResultSet         resultSet;

        // open database
        connection = openPatchesDatabase();

        // delete patch number
        preparedStatement = connection.prepareStatement("DELETE FROM numbers WHERE patchId=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();

        // delete file names
        preparedStatement = connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();

        // delete patch data
        preparedStatement = connection.prepareStatement("DELETE FROM patches WHERE id=?;");
        preparedStatement.setInt(1,id);
        preparedStatement.executeUpdate();

        // close database
        closePatchesDatabase(connection); connection = null;
      }
      finally
      {
        try { if (connection != null) closePatchesDatabase(connection); } catch (SQLException exception) { /* ignored */ }
      }
    }
  }

  /** write patch data to file
   * @param file file
   */
  public void write(File file)
    throws IOException
  {
    // open file
    PrintWriter output = new PrintWriter(new FileWriter(file,true));

    // write file
    for (String line : lines)
    {
      output.println(line);
    }

    // close file
    output.close();
  }

  /** write patch data to file
   * @param fileName file name
   */
  public void write(String fileName)
    throws IOException
  {
    write(new File(fileName));
  }

  /** apply patch
   * @param maxContextOffset max. offset context can be shifted backward/forward
   * @return true iff patch applied without errors
   */
  public boolean apply(int maxContextOffset)
  {
//Dprintf.dprintf("#%d",lines.length);
//for (String l : lines) Dprintf.dprintf("l=%s",l);
//Dprintf.dprintf("===============");

    PatchChunk        patchChunk  = new PatchChunk();
    ArrayList<String> oldLineList = new ArrayList<String>();
    ArrayList<String> newLineList = new ArrayList<String>();
    while (patchChunk.nextFile())
    {
      File file = new File(rootPath,patchChunk.fileName);

      // load old file lines
      oldLineList.clear();
      BufferedReader input = null;
      try
      {
        input = new BufferedReader(new FileReader(file));
        String line;
        while ((line = input.readLine()) != null)
        {
          oldLineList.add(line);
        }
        input.close(); input = null;
      }
      catch (IOException exception)
      {
Dprintf.dprintf("exception=%s",exception);
        return false;
      }
      finally
      {
        try { if (input != null) input.close(); } catch (IOException exception) { /* ignored */ }
      }

//Dprintf.dprintf("fileName=%s",patchChunk.fileName);
      // apply patch lines and create new file
      int lineNb = 1;
      newLineList.clear();
      while (patchChunk.nextChunk())
      {
Dprintf.dprintf("file %s, line #%d",patchChunk.fileName,patchChunk.lineNb);
//for (PatchLines patchLines : patchChunk.patchLineList) Dprintf.dprintf("  %s: %d",patchLines.type,patchLines.lines.length);

        // add not changed lines
        while (lineNb < patchChunk.lineNb)
        {
          newLineList.add(oldLineList.get(lineNb-1));
          lineNb++;
        }

        // process diff chunk
        int lineNbOffset;
        for (PatchLines patchLines : patchChunk.patchLineList)
        {
//Dprintf.dprintf("  %s: %d",patchLines.type,patchLines.lines.length);
          switch (patchLines.type)
          {
            case CONTEXT:
             // find context
             int     contextOffset = 0;
             boolean contextFound  = false;
             do
             {
               if      (patchLines.match(oldLineList,lineNb+contextOffset))
               {
                 lineNb += contextOffset;
                 contextFound = true;
               }
               else if (patchLines.match(oldLineList,lineNb-contextOffset))
               {
                 lineNb -= contextOffset;
                 contextFound = true;
               }
               else
               {
                 contextOffset++;
               }
             }
             while (   !contextFound
                    && (contextOffset < maxContextOffset)
                   );

             // add context line or unsolved chunk
             if (contextFound)
             {
               // context found -> add context lines
               for (String line : patchLines.lines)
               {
                 newLineList.add(line);
Dprintf.dprintf("cont %s",line);
               }
               lineNb += patchLines.lines.length;
             }
             else
             {
               // context not found -> unsolved merge
newLineList.add("<<<< unsolved begin >>>>");
               for (String line : patchLines.lines)
               {
                 newLineList.add(line);
Dprintf.dprintf("unsolved %s",line);
               }
newLineList.add("<<<< unsolved end >>>>");
             }
             break;
            case ADD:
             // add new lines
             for (String line : patchLines.lines)
             {
               newLineList.add(line);
//Dprintf.dprintf("add %s",line);
             }
             break;
            case REMOVE:
             // remove old lines
             lineNb += patchLines.lines.length;
             break;
          }
        }
      }

      // add rest of not changed lines
      while (lineNb < oldLineList.size())
      {
        newLineList.add(oldLineList.get(lineNb-1));
//Dprintf.dprintf("rest %s",oldLineList.get(lineNb-1));
        lineNb++;
      }

      // write new file
      PrintWriter output = null;
      try
      {
        output = new PrintWriter(new FileWriter(file));
        for (String line : newLineList)
        {
          output.println(line);
        }
        output.close(); output = null;
      }
      catch (IOException exception)
      {
Dprintf.dprintf("exception=%s",exception);
        return false;
      }
      finally
      {
        if (output != null) output.close();
      }
    }

    return true;
  }

  /** apply patch
   * @return true iff patch applied without errors
   */
  public boolean apply()
  {
    return apply(10);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Patch {"+number+", summary: "+summary+", state: "+state.toString()+"}";
  }

  //-----------------------------------------------------------------------

  /** parse unified diff index
   * @param string diff index string
   * @return index array [lineNb,length] or null
   */
  private int[] parseDiffIndex(String string)
  {
    Object[] data = new Object[2];
    if      (StringParser.parse(string,"%d,%d",data))
    {
      return new int[]{(Integer)data[0],(Integer)data[1]};
    }
    else if (StringParser.parse(string,"%d",data))
    {
      return new int[]{(Integer)data[0],1};
    }
    else
    {
      return null;
    }
  }

  // patch lines types
  enum PatchLineTypes
  {
    CONTEXT,
    ADD,
    REMOVE
  };

  // patch lines
  class PatchLines
  {
    PatchLineTypes type;
    String[]       lines;

    PatchLines(PatchLineTypes type, AbstractList<String> lineList)
    {
      this.type  = type;
      this.lines = lineList.toArray(new String[lineList.size()]);
    }

    /** check if patch lines matches
     * @param lineList lines
     * @param lineNb start line nb (1..n)
     * @return true iff lines matches to lines in lineList at line number lineNb
     */
    public boolean match(AbstractList<String> lineList, int lineNb)
    {
      if (((lineNb-1)+lines.length) <= lineList.size())
      {
        for (int z = 0; z < lines.length; z++)
        {
          if (!lineList.get(lineNb-1+z).equals(lines[z])) return false;
        }
        return true;
      }
      else
      {
        return false;
      }
    }
  }

  // patch chunk
  class PatchChunk
  {
    String                fileName;
    int                   lineNb;
    ArrayList<PatchLines> patchLineList;

    private int index;

    PatchChunk()
    {
      this.fileName      = null;
      this.lineNb        = 0;
      this.patchLineList = new ArrayList<PatchLines>();
      this.index         = 0;
    }

    public boolean nextFile()
    {
      final Pattern PATTERN_FILENAME = Pattern.compile("^\\+\\+\\+\\s+(.*)\\s+(\\S+\\s+\\S+\\s+).*",Pattern.CASE_INSENSITIVE);

      fileName = null;

      // find +++ <file name> <date> <time> <time zone>
      Matcher matcher = null;
      while (   (index < lines.length)
             && !(matcher = PATTERN_FILENAME.matcher(lines[index])).matches()
            )
      {
  //Dprintf.dprintf("xxx=%s",lines[patchChunk.index]);
        index++;
      }

      // get file name
      if (index < lines.length)
      {
        fileName = matcher.group(1);
        index++;

        return true;
      }
      else
      {
        return false;
      }
    }

    public boolean nextChunk()
    {
      final Pattern PATTERN_INDEX  = Pattern.compile("^@@\\s+\\-([\\d,]*)\\s+\\+([\\d,]*)\\s+@@$",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_COPY   = Pattern.compile("^\\s(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_ADD    = Pattern.compile("^\\+(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_REMOVE = Pattern.compile("^\\-(.*)",Pattern.CASE_INSENSITIVE);

      lineNb = 0;
      patchLineList.clear();

      // find next chunk @@ -<line nb>,<count> +<line nb>,<count> @@
      Matcher matcher;
      if (   (index < lines.length)
          && (matcher = PATTERN_INDEX.matcher(lines[index])).matches()
         )
      {
        index++;

        int[] indexOld = parseDiffIndex(matcher.group(1));
        int[] indexNew = parseDiffIndex(matcher.group(2));
  //Dprintf.dprintf("%s -- index=%d,%d",matcher.group(1),indexOld[0],indexOld[1]);
        lineNb = indexOld[0];

        ArrayList<String> lineList = new ArrayList<String>();
        int               oldLines = 0;
        int               newLines = 0;
        boolean           done     = false;
        while (   (index < lines.length) 
               && !done
              )
        {
          if      (lines[index].startsWith(" "))
          {
            lineList.clear();
            do
            {
              lineList.add(lines[index].substring(1));
              index++;
            }
            while (   (index < lines.length)
                   && lines[index].startsWith(" ")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.CONTEXT,lineList));
          }
          else if (lines[index].startsWith("+"))
          {
            lineList.clear();
            do
            {
              lineList.add(lines[index].substring(1));
              index++;
            }
            while (   (index < lines.length)
                   && lines[index].startsWith("+")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.ADD,lineList));
          }
          else if (lines[index].startsWith("-"))
          {
            lineList.clear();
            do
            {
              lineList.add(lines[index].substring(1));
              index++;
            }
            while (   (index < lines.length)
                   && lines[index].startsWith("-")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.REMOVE,lineList));
          }
          else
          {
            // done
  //Dprintf.dprintf("xx=%s",lines[patchChunk.index]);
            done = true;
          }
        }

        return true;
      }
      else
      {
        return false;
      }
    }
  }

  /** open history database
   * @return connection
   */
  private static Connection openPatchesDatabase()
    throws SQLException
  {
    Connection connection = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      // load SQLite driver class
      Class.forName("org.sqlite.JDBC");

      // open database
      connection = DriverManager.getConnection("jdbc:sqlite:"+ONZEN_PATCHES_DATABASE_FILE_NAME);
      connection.setAutoCommit(false);

      // create tables if needed
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS patches ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  datetime INTEGER DEFAULT (DATETIME('now')), "+
                              "  rootPath TEXT, "+
                              "  state    INTEGER, "+
                              "  summary  TEXT, "+
                              "  message  TEXT, "+
                              "  data     TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS files ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER, "+
                              "  fileName TEXT "+
                              ");"
                             );
      statement = connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS numbers ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = connection.createStatement();
      resultSet = null;
      try
      {
        resultSet = statement.executeQuery("SELECT name,value FROM meta;");

        if (!resultSet.next())
        {
          preparedStatement = connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
          preparedStatement.setString(1,Integer.toString(ONZEN_PATCHES_DATABASE_VERSION));
          preparedStatement.executeUpdate();
        }

        resultSet.close(); resultSet = null;
      }
      finally
      {
        if (resultSet != null) resultSet.close();
      }
    }
    catch (SQLException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      throw exception;
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
  private static void closePatchesDatabase(Connection connection)
    throws SQLException
  {
    connection.setAutoCommit(true);
    connection.close();
  }

  /** get id of last inserted row
   * @param connection database connection
   * @return id
   */
  private int getLastInsertId(Connection connection)
    throws SQLException
  {
    int id = -1;

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
}

/* end of file */
