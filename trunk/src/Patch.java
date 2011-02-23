/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Patch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: patch functions
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
  private static final String PATCHES_DATABASE_NAME    = "patches";
  private static final int    PATCHES_DATABASE_VERSION = 1;

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
  public  String   rootPath;               // root path
  public  States   state;                  // patch state; see States
  public  String[] fileNames;              // files belonging to patch
  public  String   summary;                // summary comment
  public  String[] message;                // commit message
  public  String[] lines;                  // patch lines

  private int      databaseId;             // id in database or Database.ID_NONE
  private int      number;                 // patch number or Database.ID_NONE
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

    Database database = null;
    try
    {
      Statement         statement;
      PreparedStatement preparedStatement;
      ResultSet         resultSet1,resultSet2;

      // open database
      database = openPatchesDatabase();

      // get patches (Note: there is no way to use prepared statements with variable "IN"-operator)
      statement = database.connection.createStatement();
      resultSet1 = null;
      try
      {
        resultSet1 = statement.executeQuery("SELECT "+
                                            "  patches.id AS databaseId, "+
                                            "  patches.state, "+
                                            "  patches.summary, "+
                                            "  patches.message, "+
                                            "  patches.data, "+
                                            "  numbers.id AS number "+
                                            "FROM patches "+
                                            "  LEFT JOIN numbers ON numbers.patchId=patches.id "+
                                            "WHERE     patches.rootPath='"+rootPath+"'"+
                                            "      AND (   patches.state="+Patch.States.NONE.ordinal()+" "+
                                            "           OR patches.state IN ("+StringUtils.join(filterStates,",",true)+") "+
                                            "          )"+
                                            "GROUP BY databaseId "+
                                            "LIMIT 0,"+n+" "+
                                            ";"
                                           );

        preparedStatement = database.connection.prepareStatement("SELECT "+
                                                                 "  fileName "+
                                                                 "FROM files "+
                                                                 "WHERE patchId=? "+
                                                                 ";"
                                                                );
        while (resultSet1.next())
        {
          // get patch data
          int      databaseId = resultSet1.getInt("databaseId");
          States   state      = States.toEnum(resultSet1.getInt("state"));
          String   summary    = resultSet1.getString("summary"); if (summary == null) summary = "";
          String[] message    = Database.dataToLines(resultSet1.getString("message"));
          String[] lines      = Database.dataToLines(resultSet1.getString("data"));
          int      number     = resultSet1.getInt("number");
//Dprintf.dprintf("databaseId=%d state=%s summary=%s number=%d",databaseId,state,summary,number);

          // get file names
          ArrayList<String> fileNameList = new ArrayList<String>();
          preparedStatement.setInt(1,databaseId);
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
                                  databaseId,
                                  state,
                                  summary,
                                  message,
                                  lines,
                                  fileNames,
                                  number
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
      closePatchesDatabase(database);
    }
    catch (SQLException exception)
    {
      Onzen.printWarning("Cannot get patches from database (error: %s)",exception.getMessage());
      return null;
    }
    finally
    {
      try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
    }

    return patchList.toArray(new Patch[patchList.size()]);
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   * @param state state
   * @param summary summary text
   * @param message message text lines
   * @param lines patch lines
   * @param fileNames file names
   * @param patch number
   */
  private Patch(String rootPath, int databaseId, States state, String summary, String message[], String[] lines, String[] fileNames, int number)
  {
    this.rootPath   = rootPath;
    this.state      = state;
    this.summary    = summary;
    this.message    = message;
    this.fileNames  = fileNames;

    this.databaseId = databaseId;
    this.number     = number;
    this.lines      = lines;
    this.tmpFile    = null;
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   * @param state state
   * @param summary summary text
   * @param message message text lines
   * @param lines patch lines
   * @param fileNames file names
   */
  public Patch(String rootPath, int databaseId, States state, String summary, String[] message, String[] lines, String[] fileNames)
  {
    this(rootPath,databaseId,state,summary,message,lines,fileNames,Database.ID_NONE);
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param lines patch lines
   */
  public Patch(String rootPath, HashSet<FileData> fileDataSet, String[] lines)
  {
    this(rootPath,Database.ID_NONE,States.NONE,"",null,lines,null);

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
   * @param lines patch lines
   */
  public Patch(String rootPath, String[] lines)
  {
    this(rootPath,null,lines);
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   */
  public Patch(String rootPath, int databaseId)
  {
    this(rootPath,databaseId,States.NONE,"",null,null,null);
  }

  /** create patch
   * @param databaseId database id
   */
  public Patch(int databaseId)
  {
    this(null,databaseId,States.NONE,"",null,null,null);
  }

  /** done patch
   */
  public void done()
  {
    // clean-up not used/saved patch: discard allocated patch number
    if ((databaseId == Database.ID_NONE) && (number != Database.ID_NONE))
    {
      Database database = null;
      try
      {
        PreparedStatement preparedStatement;

        // open database
        database = openPatchesDatabase();

        // remove not use patch number
        preparedStatement = database.connection.prepareStatement("DELETE FROM numbers WHERE id=?;");
        preparedStatement.setInt(1,number);
        preparedStatement.executeUpdate();

        // close database
        closePatchesDatabase(database);

        number = Database.ID_NONE;
      }
      catch (SQLException exception)
      {
        Onzen.printWarning("Cannot clean-up patches database (error: %s)",exception.getMessage());
        return;
      }
      finally
      {
        try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
      }
    }

    // delete tempory file
    if (tmpFile != null) tmpFile.delete();
  }

  /** allocate a patch number
   * @return patch number or Database.ID_NONE if no patch number could be allocated
   */
  public int getNumber()
  {
    // reserve a patch number if required
    if (number == Database.ID_NONE)
    {
      Database database = null;
      try
      {
        PreparedStatement preparedStatement;

        // open database
        database = openPatchesDatabase();

        // create number
        SQLException sqlException = null;
        int          retryCount   = 0;
        do
        {
          try
          {
            // create empty patch number entry
            preparedStatement = database.connection.prepareStatement("INSERT INTO numbers (patchId) VALUES (?);");
            preparedStatement.setInt(1,databaseId);
            preparedStatement.executeUpdate();

            // get patch number (=id)
            number = database.getLastInsertId();
          }
          catch (SQLException exception)
          {
            // fail -> wait and retry
            sqlException = exception;
            retryCount++;

            try { Thread.sleep(250); } catch (InterruptedException interruptedException) { /* ignored */ }
          }
        }
        while ((number == Database.ID_NONE) && (retryCount < 5));

        // close database
        closePatchesDatabase(database);
      }
      catch (SQLException exception)
      {
        // ignored
      }
      finally
      {
        try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
      }
    }

    return number;
  }

  /** get patch number text
   * @return patch number text or "-"
   */
  public String getNumberText()
  {
    return (number != Database.ID_NONE)?Integer.toString(number):"-";
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
    Database database = null;
    try
    {
      Statement         statement;
      PreparedStatement preparedStatement;

      // open database
      database = openPatchesDatabase();

      // store patch data
      if (databaseId >= 0)
      {
        // update
        preparedStatement = database.connection.prepareStatement("UPDATE patches SET rootPath=?,state=?,summary=?,message=?,data=? WHERE id=?;");
        preparedStatement.setString(1,rootPath);
        preparedStatement.setInt(2,state.ordinal());
        preparedStatement.setString(3,summary);
        preparedStatement.setString(4,Database.linesToData(message));
        preparedStatement.setString(5,Database.linesToData(lines));
        preparedStatement.setInt(6,databaseId);
        preparedStatement.executeUpdate();
      }
      else
      {
        // insert
        preparedStatement = database.connection.prepareStatement("INSERT INTO patches (rootPath,state,summary,message,data) VALUES (?,?,?,?,?);");
        preparedStatement.setString(1,rootPath);
        preparedStatement.setInt(2,state.ordinal());
        preparedStatement.setString(3,summary);
        preparedStatement.setString(4,Database.linesToData(message));
        preparedStatement.setString(5,Database.linesToData(lines));
        preparedStatement.executeUpdate();
        databaseId = database.getLastInsertId();

        if (number >= 0)
        {
          // update patch number entry
          preparedStatement = database.connection.prepareStatement("UPDATE numbers SET patchId=? WHERE id=?;");
          preparedStatement.setInt(1,databaseId);
          preparedStatement.setInt(2,number);
          preparedStatement.executeUpdate();
        }
      }

      if (fileNames != null)
      {
        // insert files
        preparedStatement = database.connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();
        preparedStatement = database.connection.prepareStatement("INSERT INTO files (patchId,fileName) VALUES (?,?);");
        for (String fileName : fileNames)
        {
          preparedStatement.setInt(1,databaseId);
          preparedStatement.setString(2,fileName);
          preparedStatement.executeUpdate();
        }
      }

      // close database
      closePatchesDatabase(database);
    }
    finally
    {
      try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
    }

    return databaseId;
  }

  /** load patch from database
   */
  public void load()
    throws SQLException
  {
    if (databaseId >= 0)
    {
      Database database = null;
      try
      {
        Statement         statement;
        PreparedStatement preparedStatement;
        ResultSet         resultSet;

        // open database
        database = openPatchesDatabase();

        // load patch data
        preparedStatement = database.connection.prepareStatement("SELECT "+
                                                                 "  rootPath, "+
                                                                 "  state, "+
                                                                 "  summary, "+
                                                                 "  message, "+
                                                                 "  data "+
                                                                 "FROM patches "+
                                                                 "WHERE id=? "+
                                                                 ";"
                                                                );
        preparedStatement.setInt(1,databaseId);
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
            message  = Database.dataToLines(resultSet.getString("message"));
            lines    = Database.dataToLines(resultSet.getString("data"));
          }
          else
          {
            throw new SQLException("patch "+databaseId+" not found");
          }
          resultSet.close(); resultSet = null;
        }
        finally
        {
          if (resultSet != null) resultSet.close();
        }

        // load file names
        ArrayList<String> fileNameList = new ArrayList<String>();
        preparedStatement = database.connection.prepareStatement("SELECT "+
                                                                 "  fileName "+
                                                                 "FROM files "+
                                                                 "WHERE patchId=? "+
                                                                 ";"
                                                                );
        preparedStatement.setInt(1,databaseId);
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
        closePatchesDatabase(database);
      }
      finally
      {
        try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
      }
    }
  }

  /** delete patch from database
   */
  public void delete()
    throws SQLException
  {
    if (databaseId >= 0)
    {
      Database database = null;
      try
      {
        Statement         statement;
        PreparedStatement preparedStatement;
        ResultSet         resultSet;

        // open database
        database = openPatchesDatabase();

        // delete patch number
        preparedStatement = database.connection.prepareStatement("DELETE FROM numbers WHERE patchId=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();

        // delete file names
        preparedStatement = database.connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();

        // delete patch data
        preparedStatement = database.connection.prepareStatement("DELETE FROM patches WHERE id=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();

        // close database
        closePatchesDatabase(database);

        databaseId = Database.ID_NONE;
      }
      finally
      {
        try { if (database != null) closePatchesDatabase(database); } catch (SQLException exception) { /* ignored */ }
      }
    }
  }

  /** write/append patch data to file
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

  /** write/append patch data to file
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
      File file = new File(rootPath,patchChunk.newFileName);

      // load old file lines (if not /dev/null == file is new)
      oldLineList.clear();
      if (!patchChunk.oldFileName.equals("/dev/null"))
      {
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
      }

      // apply patch lines and create new file
      int lineNb = 1;
      newLineList.clear();
      while (patchChunk.nextChunk())
      {
//Dprintf.dprintf("file '%s', line #%d",patchChunk.fileName,patchChunk.lineNb);
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
//Dprintf.dprintf("#%d:  %s: %d",lineNb,patchLines.type,patchLines.lines.length);
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
//Dprintf.dprintf("cont %d %d %s",lineNb,patchLines.lines.length,line);
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
      while (lineNb <= oldLineList.size())
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
    return "Patch {"+number+", summary: "+summary+", state: "+state.toString()+", lines: "+lines.length+"}";
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
    // --------------------------- constants --------------------------------

    // --------------------------- variables --------------------------------
    PatchLineTypes type;
    String[]       lines;

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

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
      if (((lineNb-1) >= 0) && (((lineNb-1)+lines.length) <= lineList.size()))
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
    // --------------------------- constants --------------------------------

    // --------------------------- variables --------------------------------
    String                oldFileName;
    String                newFileName;
    int                   lineNb;
    ArrayList<PatchLines> patchLineList;

    private int index;

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

    /** create patch chunk
     */
    PatchChunk()
    {
      this.oldFileName   = null;
      this.newFileName   = null;
      this.lineNb        = 0;
      this.patchLineList = new ArrayList<PatchLines>();
      this.index         = 0;
    }

    /** parse next file entry in patch
     * @return true iff file entry found
     */
    public boolean nextFile()
    {
      final Pattern PATTERN_OLD_FILENAME = Pattern.compile("^\\-\\-\\-\\s+(.*)\\s+(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_NEW_FILENAME = Pattern.compile("^\\+\\+\\+\\s+(.*)\\s+(.*)",Pattern.CASE_INSENSITIVE);

      Matcher matcher = null;

      oldFileName = null;
      newFileName = null;

      // find --- <old file name> <date/time>
      while (   (index < lines.length)
             && !(matcher = PATTERN_OLD_FILENAME.matcher(lines[index])).matches()
            )
      {
        index++;
      }
      if (index < lines.length)
      {
        oldFileName = matcher.group(1);
        index++;
      }

      // find +++ <new file name> <date/time>
      while (   (index < lines.length)
             && !(matcher = PATTERN_NEW_FILENAME.matcher(lines[index])).matches()
            )
      {
        index++;
      }
      if (index < lines.length)
      {
        newFileName = matcher.group(1);
        index++;
      }

      return (oldFileName != null) && (newFileName != null);
    }

    /** parse next chunk in patch
     * @return true iff chunk found
     */
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

  /** open patches database
   * @return database
   */
  private static Database openPatchesDatabase()
    throws SQLException
  {
    Database database = null;
    try
    {
      Statement         statement;
      ResultSet         resultSet;
      PreparedStatement preparedStatement;

      database = new Database(PATCHES_DATABASE_NAME);

      // create tables if needed
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS meta ( "+
                              "  name  TEXT, "+
                              "  value TEXT "+
                              ");"
                             );
      statement = database.connection.createStatement();
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
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS files ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER, "+
                              "  fileName TEXT "+
                              ");"
                             );
      statement = database.connection.createStatement();
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS numbers ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER "+
                              ");"
                             );

      // init meta data (if not already initialized)
      statement = database.connection.createStatement();
      resultSet = null;
      try
      {
        resultSet = statement.executeQuery("SELECT name,value FROM meta;");

        if (!resultSet.next())
        {
          preparedStatement = database.connection.prepareStatement("INSERT INTO meta (name,value) VALUES ('version',?);");
          preparedStatement.setString(1,Integer.toString(PATCHES_DATABASE_VERSION));
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

    return database;
  }

  /** close patches database
   * @param database database
   */
  private static void closePatchesDatabase(Database database)
    throws SQLException
  {
    database.close();
  }
}

/* end of file */
