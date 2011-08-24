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
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.LinkedHashSet;
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
  public  String                rootPath;               // root path
  public  States                state;                  // patch state; see States
  public  String                summary;                // summary comment
  public  String[]              message;                // commit message
  public  String                revision1,revision2;    // revision
  public  boolean               ignoreWhitespaces;      // true when whitespaces are ignored
  public  LinkedHashSet<String> testSet;                // tests done

  private HashSet<String>       fileNameSet;            // files belonging to patch
  private String[]              lines;                  // patch lines or null
  private File                  file;                   // file with patch or null
  private int                   databaseId;             // id in database or Database.ID_NONE
  private int                   number;                 // patch number or Database.ID_NONE
  private File                  tmpFile;                // temporary file with patch

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** get patches from database
   * @param path root path of patches or null for all repositories
   * @param allRepositories TRUE for patches in all repositories
   * @param filterStates states of patches
   * @param n max. number of patches to return
   * @return patches array
   */
  public static Patch[] getPatches(String path, boolean allRepositories, EnumSet<States> filterStates, int n)
  {
    ArrayList<Patch> patchList = new ArrayList<Patch>();

    Database database = null;
    try
    {
      Statement         statement;
      PreparedStatement preparedStatement1,preparedStatement2;
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
                                            "  patches.rootPath, "+
                                            "  patches.state, "+
                                            "  patches.summary, "+
                                            "  patches.message, "+
                                            "  patches.revision1, "+
                                            "  patches.revision2, "+
                                            "  patches.ignoreWhitespaces, "+
                                            "  patches.data, "+
                                            "  CASE WHEN numbers.id IS NOT NULL THEN numbers.id ELSE -1 END AS number "+
                                            "FROM patches "+
                                            "  LEFT JOIN numbers ON numbers.patchId=patches.id "+
                                            "WHERE     "+(!allRepositories ? "patches.rootPath='"+path+"'" : "1")+
                                            "      AND (   patches.state="+Patch.States.NONE.ordinal()+" "+
                                            "           OR patches.state IN ("+StringUtils.join(filterStates,",",true)+") "+
                                            "          )"+
                                            "GROUP BY databaseId "+
                                            "ORDER BY datetime DESC "+
                                            "LIMIT 0,"+n+" "+
                                            ";"
                                           );

        preparedStatement1 = database.connection.prepareStatement("SELECT "+
                                                                  "  fileName "+
                                                                  "FROM files "+
                                                                  "WHERE patchId=? "+
                                                                  "ORDER BY id "+
                                                                  ";"
                                                                 );
        preparedStatement2 = database.connection.prepareStatement("SELECT "+
                                                                  "  test "+
                                                                  "FROM tests "+
                                                                  "WHERE patchId=? "+
                                                                  "ORDER BY id "+
                                                                  ";"
                                                                 );
        while (resultSet1.next())
        {
          // get patch data
          int      databaseId        = resultSet1.getInt("databaseId");
          String   rootPath          = resultSet1.getString("rootPath");
          States   state             = States.toEnum(resultSet1.getInt("state"));
          String   summary           = resultSet1.getString("summary"); if (summary == null) summary = "";
          String[] message           = Database.dataToLines(resultSet1.getString("message"));
          String   revision1         = resultSet1.getString("revision1");
          String   revision2         = resultSet1.getString("revision2");
          boolean  ignoreWhitespaces = resultSet1.getInt("ignoreWhitespaces") == 1;
          String[] lines             = Database.dataToLines(resultSet1.getString("data"));
          int      number            = resultSet1.getInt("number");
//Dprintf.dprintf("databaseId=%d rootPath=%s state=%s summary=%s number=%d",databaseId,rootPath,state,summary,number);

          // get file names
          HashSet<String> fileNameSet = new HashSet<String>();
          preparedStatement1.setInt(1,databaseId);
          resultSet2 = null;
          try
          {
            resultSet2 = preparedStatement1.executeQuery();
            while (resultSet2.next())
            {
              fileNameSet.add(resultSet2.getString("fileName"));
            }
            resultSet2.close(); resultSet2 = null;
          }
          finally
          {
            if (resultSet2 != null) resultSet2.close();
          }
//Dprintf.dprintf("databaseId=%d fileNameSet=%s",databaseId,fileNameSet);

          // get tests
          LinkedHashSet<String> testSet = new LinkedHashSet<String>();
          preparedStatement2.setInt(1,databaseId);
          resultSet2 = null;
          try
          {
            resultSet2 = preparedStatement2.executeQuery();
            while (resultSet2.next())
            {
              testSet.add(resultSet2.getString("test"));
            }
            resultSet2.close(); resultSet2 = null;
          }
          finally
          {
            if (resultSet2 != null) resultSet2.close();
          }
//Dprintf.dprintf("databaseId=%d testSet=%s",databaseId,testSet);

          // add to list
          patchList.add(new Patch(rootPath,
                                  databaseId,
                                  number,
                                  state,
                                  summary,
                                  message,
                                  revision1,
                                  revision2,
                                  ignoreWhitespaces,
                                  fileNameSet,
                                  testSet,
                                  lines,
                                  null
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
   * @param patch number
   * @param state state
   * @param summary summary text
   * @param message message text lines
   * @param revision1,revision2 revisions
   * @param ignoreWhitespaces true iff whitespaces are ignored
   * @param fileNameSet file names
   * @param testSet tests done
   * @param file file with patch
   */
  private Patch(String                rootPath,
                int                   databaseId,
                int                   number,
                States                state,
                String                summary,
                String                message[],
                String                revision1,
                String                revision2,
                boolean               ignoreWhitespaces,
                HashSet<String>       fileNameSet,
                LinkedHashSet<String> testSet,
                String[]              lines,
                File                  file
               )
  {
    this.rootPath          = rootPath;
    this.state             = state;
    this.summary           = summary;
    this.message           = message;
    this.revision1         = revision1;
    this.revision2         = revision2;
    this.ignoreWhitespaces = ignoreWhitespaces;
    this.fileNameSet       = fileNameSet;
    this.testSet           = testSet;

    this.lines             = lines;
    this.file              = file;
    this.databaseId        = databaseId;
    this.number            = number;
    this.tmpFile           = null;
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   * @param state state
   * @param summary summary text
   * @param message message text lines
   * @param revision1,revision2 revisions
   * @param ignoreWhitespaces true iff whitespaces are ignored
   * @param fileNameSet file names
   * @param testSet tests done
   * @param lines patch lines
   */
  public Patch(String                rootPath,
               int                   databaseId,
               States                state,
               String                summary,
               String[]              message,
               String                revision1,
               String                revision2,
               boolean               ignoreWhitespaces,
               HashSet<String>       fileNameSet,
               LinkedHashSet<String> testSet,
               String[]              lines
              )
  {
    this(rootPath,
         databaseId,
         Database.ID_NONE,
         state,
         summary,
         message,
         revision1,
         revision2,
         ignoreWhitespaces,
         fileNameSet,
         testSet,
         lines,
         null
        );
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions
   * @param ignoreWhitespaces true iff whitespaces are ignored
   * @param lines patch lines
   */
  public Patch(String            rootPath,
               HashSet<FileData> fileDataSet,
               String            revision1,
               String            revision2,
               boolean           ignoreWhitespaces,
               String[]          lines
              )
  {
    this(rootPath,
         Database.ID_NONE,
         States.NONE,
         "",
         new String[]{},
         revision1,
         revision2,
         ignoreWhitespaces,
         new HashSet<String>(),
         new LinkedHashSet<String>(),
         lines
        );
    if (fileDataSet != null)
    {
      // set file names
      fileNameSet = new HashSet<String>();
      for (FileData fileData : fileDataSet)
      {
        fileNameSet.add(fileData.getFileName());
      }
    }
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param lines patch lines
   */
  public Patch(String            rootPath,
               HashSet<FileData> fileDataSet,
               String[]          lines
              )
  {
    this(rootPath,fileDataSet,null,null,false,lines);
  }

  /** create patch
   * @param rootPath root path
   * @param lines patch lines
   */
  public Patch(String   rootPath,
               String[] lines
              )
  {
    this(rootPath,null,lines);
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   * @param state state
   * @param summary summary text
   * @param message message text lines
   * @param revision1,revision2 revisions
   * @param ignoreWhitespaces true iff whitespaces are ignored
   * @param fileNameSet file names
   * @param testSet tests done
   * @param file file with patch
   */
  public Patch(String                rootPath,
               int                   databaseId,
               States                state,
               String                summary,
               String[]              message,
               String                revision1,
               String                revision2,
               boolean               ignoreWhitespaces,
               HashSet<String>       fileNameSet,
               LinkedHashSet<String> testSet,
               File                  file
              )
  {
    this(rootPath,
         databaseId,
         Database.ID_NONE,
         state,
         summary,
         message,
         revision1,
         revision2,
         ignoreWhitespaces,
         fileNameSet,
         testSet,
         null,
         file
        );
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param revision1,revision2 revisions
   * @param ignoreWhitespaces true iff whitespaces are ignored
   * @param testSet tests done
   * @param file file with patch
   */
  public Patch(String                rootPath,
               HashSet<FileData>     fileDataSet,
               String                revision1,
               String                revision2,
               boolean               ignoreWhitespaces,
               LinkedHashSet<String> testSet,
               File                  file
              )
  {
    this(rootPath,
         Database.ID_NONE,
         States.NONE,
         "",
         new String[]{},
         revision1,
         revision2,
         ignoreWhitespaces,
         new HashSet<String>(),
         testSet,
         file
        );
    if (fileDataSet != null)
    {
      // set file names
      for (FileData fileData : fileDataSet)
      {
        fileNameSet.add(fileData.getFileName());
      }
    }
  }

  /** create patch
   * @param rootPath root path
   * @param fileDataSet file data set
   * @param file file with patch
   */
  public Patch(String            rootPath,
               HashSet<FileData> fileDataSet,
               File              file
              )
  {
    this(rootPath,
         fileDataSet,
         null,
         null,
         false,
         new LinkedHashSet<String>(),
         file
        );
  }

  /** create patch
   * @param rootPath root path
   * @param file file with patch
   */
  public Patch(String   rootPath,
               File     file
              )
  {
    this(rootPath,null,file);
  }

  /** create patch
   * @param rootPath root path
   * @param databaseId database id
   */
  public Patch(String rootPath,
               int    databaseId
              )
  {
    this(rootPath,
         databaseId,
         States.NONE,
         "",
         new String[]{},
         null,
         null,
         false,
         new HashSet<String>(),
         new LinkedHashSet<String>(),
         (String[])null
        );
  }

  /** create patch
   * @param databaseId database id
   */
  public Patch(int databaseId)
  {
    this(null,databaseId);
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

  /** get file names of patch
   * @return file names
   */
  public String[] getFileNames()
  {
    return fileNameSet.toArray(new String[fileNameSet.size()]);
  }

  /** get tests of patch
   * @return tests
   */
  public String[] getTests()
  {
    return testSet.toArray(new String[testSet.size()]);
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

  /** set state
   * @param state new state
   */
  public void setState(States state)
  {
    this.state = state;
  }

  /** get patch lines
   * @return lines or null
   */
  public String[] getLines()
  {
    if (file != null)
    {
      // read file
      ArrayList<String> lineList = new ArrayList<String>();
      try
      {
        BufferedReader input = new BufferedReader(new FileReader(file));
        String line;
        while ((line = input.readLine()) != null)
        {
          lineList.add(line);
        }
        input.close();
      }
      catch (IOException exception)
      {
        return null;
      }

      return lineList.toArray(new String[lineList.size()]);
    }
    else
    {
      // get lines
      return lines;
    }
  }

  /** set patch lines
   * @param newLines new patch lines
   */
  public void setLines(String[] newLines)
  {
    file  = null;
    lines = newLines;
  }

  /** get patch file name
   * @return temporary file name or null
   */
  public String getFileName()
  {
    String fileName = null;

    if (file != null)
    {
      fileName = file.getAbsolutePath();
    }
    else if (tmpFile == null)
    {
      // create temporary file
      try
      {
        tmpFile = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));
        write(tmpFile);
      }
      catch (IOException exception)
      {
Dprintf.dprintf("");
        fileName = null;
      }
      fileName = (tmpFile != null) ? tmpFile.getAbsolutePath() : null;
    }

    return fileName;
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
        preparedStatement.setString(5,Database.linesToData(getLines()));
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
        preparedStatement.setString(5,Database.linesToData(getLines()));
        preparedStatement.executeUpdate();
        databaseId = database.getLastInsertId();

        if (number != Database.ID_NONE)
        {
          // update patch number entry
          preparedStatement = database.connection.prepareStatement("UPDATE numbers SET patchId=? WHERE id=?;");
          preparedStatement.setInt(1,databaseId);
          preparedStatement.setInt(2,number);
          preparedStatement.executeUpdate();
        }
      }

      if (fileNameSet != null)
      {
        // insert files
        preparedStatement = database.connection.prepareStatement("DELETE FROM files WHERE patchId=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();
        preparedStatement = database.connection.prepareStatement("INSERT INTO files (patchId,fileName) VALUES (?,?);");
        for (String fileName : fileNameSet)
        {
          preparedStatement.setInt(1,databaseId);
          preparedStatement.setString(2,fileName);
          preparedStatement.executeUpdate();
        }
      }

      if (testSet != null)
      {
        // insert tests
        preparedStatement = database.connection.prepareStatement("DELETE FROM tests WHERE patchId=?;");
        preparedStatement.setInt(1,databaseId);
        preparedStatement.executeUpdate();
        preparedStatement = database.connection.prepareStatement("INSERT INTO tests (patchId,test) VALUES (?,?);");
        for (String test : testSet)
        {
          preparedStatement.setInt(1,databaseId);
          preparedStatement.setString(2,test);
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
        if (fileNameSet == null) fileNameSet = new HashSet<String>();
        fileNameSet.clear();
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
            fileNameSet.add(resultSet.getString("fileName"));
          }
          resultSet.close(); resultSet = null;
        }
        finally
        {
          if (resultSet != null) resultSet.close();
        }

        // load tests
        if (testSet == null) testSet = new LinkedHashSet<String>();
        testSet.clear();
        preparedStatement = database.connection.prepareStatement("SELECT "+
                                                                 "  test "+
                                                                 "FROM tests "+
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
            testSet.add(resultSet.getString("fileName"));
          }
          resultSet.close(); resultSet = null;
        }
        finally
        {
          if (resultSet != null) resultSet.close();
        }

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

        // delete tests
        preparedStatement = database.connection.prepareStatement("DELETE FROM tests WHERE patchId=?;");
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
    if (this.file != null)
    {
      // copy file
      FileInputStream  input  = null;
      FileOutputStream output = null;
      byte[] buffer = new byte[64*1024];
      try
      {
        input  = new FileInputStream(this.file);
        output = new FileOutputStream(file);

        int n = 0;
        while ((n = input.read(buffer)) != -1)
        {
          output.write(buffer,0,n);
        }

        output.close(); output = null;
        input.close(); input = null;
      }
      finally
      {
        if (output != null) output.close();
        if (input != null) input.close();
      }
    }
    else
    {
      // create file
      PrintWriter output = new PrintWriter(new FileWriter(file,true));
      for (String line : lines)
      {
        output.println(line);
      }
      output.close();
    }
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
  String[] l = getLines();
/*
Dprintf.dprintf("#%d",l.length);
for (String ll : l) Dprintf.dprintf("ll=%s",ll);
Dprintf.dprintf("===============");
/**/

    PatchChunk  patchChunk = null;
    PrintWriter output     = null;
    try
    {
      patchChunk = new PatchChunk();

      ArrayList<String> oldLineList = new ArrayList<String>();
      ArrayList<String> newLineList = new ArrayList<String>();
      while (patchChunk.nextFile())
      {
//Dprintf.dprintf("patchChunk=%s",patchChunk);
        // get old, new file
        File oldFile      = new File(rootPath,patchChunk.oldFileName);
        File newFile      = new File(rootPath,patchChunk.newFileName);
        File rejectedFile = new File(rootPath,patchChunk.newFileName+".rej");
        File originalFile = new File(rootPath,patchChunk.newFileName+".orig");
        if (!oldFile.exists() && patchChunk.oldFileName.startsWith("a/") && patchChunk.newFileName.startsWith("b/"))
        {
          oldFile = new File(rootPath,patchChunk.oldFileName.substring(2));
          newFile = new File(rootPath,patchChunk.newFileName.substring(2));
        }

        // load old file lines (if not /dev/null => file is new)
        oldLineList.clear();
        if (!patchChunk.oldFileName.equals("/dev/null"))
        {
          BufferedReader input = null;
          try
          {
            input = new BufferedReader(new FileReader(oldFile));
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
        int rejectedCount = 0;
        int oldLineNb     = 1;
        newLineList.clear();
        while (patchChunk.nextChunk())
        {
//Dprintf.dprintf("file '%s' -> '%s', line #%d",patchChunk.oldFileName,patchChunk.newFileName,patchChunk.lineNb);
//for (PatchLines patchLines : patchChunk.patchLineList) Dprintf.dprintf("  %s: %d",patchLines.type,patchLines.lines.length);

          // add not changed lines
          while (oldLineNb < patchChunk.lineNb)
          {
            newLineList.add(oldLineList.get(oldLineNb-1));
            oldLineNb++;
          }

          // process diff chunk
          for (PatchLines patchLines : patchChunk.patchLineList)
          {
//Dprintf.dprintf("at #%d:  type=%s: lines=%d",oldLineNb,patchLines.type,patchLines.lines.length);
            switch (patchLines.type)
            {
              case CONTEXT:
               // find context
               int     lineNb        = oldLineNb;
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
                 // add not changed lines
                 while (oldLineNb < lineNb)
                 {
                   newLineList.add(oldLineList.get(oldLineNb-1));
                   oldLineNb++;
                 }

                 // context found -> add context lines
                 for (String line : patchLines.lines)
                 {
                   newLineList.add(line);
                 }
//Dprintf.dprintf("Diff: applied CONTEXT chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);

                 oldLineNb += patchLines.lines.length;
               }
               else
               {
                 // context not found -> add to rejected file
                 patchChunk.appendToFile(rejectedFile);
//newLineList.add("<<<< unsolved begin >>>>");
//for (String line : patchLines.lines) { Dprintf.dprintf("unsolved %s",line); }
//newLineList.add("<<<< unsolved end >>>>");
                 rejectedCount++;
                 System.err.println(String.format("Patch: rejected diff chunk at %d: %d lines",patchChunk.lineNb,patchLines.lines.length));
               }
               break;
              case ADD:
               // add new lines
               for (String line : patchLines.lines)
               {
                 newLineList.add(line);
               }
//Dprintf.dprintf("Diff: applied ADD chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);
               break;
              case REMOVE:
               // remove old lines
//Dprintf.dprintf("Diff: applied REMOVE chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);

               oldLineNb += patchLines.lines.length;
               break;
            }
          }
        }

        // add rest of not changed lines
        while (oldLineNb <= oldLineList.size())
        {
          newLineList.add(oldLineList.get(oldLineNb-1));
          oldLineNb++;
        }

        // write new file
        output = new PrintWriter(new FileWriter(newFile));
        for (String line : newLineList)
        {
          output.println(line);
        }
        output.close(); output = null;

        // write original file if some patch chunk was rejected
        if (rejectedCount > 0)
        {
          output = new PrintWriter(new FileWriter(originalFile));
          for (String line : oldLineList)
          {
            output.println(line);
          }
          output.close(); output = null;
        }
      }

      patchChunk.close(); patchChunk = null;
    }
    catch (IOException exception)
    {
Dprintf.dprintf("exception=%s",exception);
      return false;
    }
    finally
    {
      if (output != null) output.close();
      if (patchChunk != null) patchChunk.close();
    }

    return true;
  }

  /** apply patch
   * @return true iff patch applied without errors
   */
  public boolean apply()
  {
    return apply(1000);
  }

  /** unapply patch
   * @param maxContextOffset max. offset context can be shifted backward/forward
   * @return true iff patch unapplied without errors
   */
  public boolean unapply(int maxContextOffset)
  {
  String[] l = getLines();
/*
Dprintf.dprintf("#%d",l.length);
for (String ll : l) Dprintf.dprintf("ll=%s",ll);
Dprintf.dprintf("===============");
/**/

    PatchChunk  patchChunk = null;
    PrintWriter output     = null;
    try
    {
      patchChunk = new PatchChunk();

      ArrayList<String> oldLineList = new ArrayList<String>();
      ArrayList<String> newLineList = new ArrayList<String>();
      while (patchChunk.nextFile())
      {
//Dprintf.dprintf("patchChunk=%s",patchChunk);
        // get old, new file
        File oldFile = new File(rootPath,patchChunk.oldFileName);
        File newFile = new File(rootPath,patchChunk.newFileName);
        if (!oldFile.exists() && patchChunk.oldFileName.startsWith("a/") && patchChunk.newFileName.startsWith("b/"))
        {
          oldFile = new File(rootPath,patchChunk.oldFileName.substring(2));
          newFile = new File(rootPath,patchChunk.newFileName.substring(2));
        }

        // load old file lines (if not /dev/null => file is new)
        oldLineList.clear();
        if (!patchChunk.oldFileName.equals("/dev/null"))
        {
          BufferedReader input = null;
          try
          {
            input = new BufferedReader(new FileReader(oldFile));
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
        int rejectedCount = 0;
        int oldLineNb     = 1;
        newLineList.clear();
        while (patchChunk.nextChunk())
        {
//Dprintf.dprintf("file '%s' -> '%s', line #%d",patchChunk.oldFileName,patchChunk.newFileName,patchChunk.lineNb);
//for (PatchLines patchLines : patchChunk.patchLineList) Dprintf.dprintf("  %s: %d",patchLines.type,patchLines.lines.length);

          // add not changed lines
          while (oldLineNb < patchChunk.lineNb)
          {
            newLineList.add(oldLineList.get(oldLineNb-1));
            oldLineNb++;
          }

          // process diff chunk
          for (PatchLines patchLines : patchChunk.patchLineList)
          {
//Dprintf.dprintf("at #%d:  type=%s: lines=%d",oldLineNb,patchLines.type,patchLines.lines.length);
            switch (patchLines.type)
            {
              case CONTEXT:
               // find context
               int     lineNb        = oldLineNb;
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
                 // add not changed lines
                 while (oldLineNb < lineNb)
                 {
                   newLineList.add(oldLineList.get(oldLineNb-1));
                   oldLineNb++;
                 }

                 // context found -> add context lines
                 for (String line : patchLines.lines)
                 {
                   newLineList.add(line);
                 }
//Dprintf.dprintf("Diff: applied CONTEXT chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);

                 oldLineNb += patchLines.lines.length;
               }
               break;
              case ADD:
               // remove added new lines
               oldLineNb += patchLines.lines.length;
//Dprintf.dprintf("Diff: applied ADD chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);
               break;
              case REMOVE:
               // add removed old lines
//Dprintf.dprintf("Diff: applied REMOVE chunk at #%d: %d lines",oldLineNb,patchLines.lines.length);
               for (String line : patchLines.lines)
               {
                 newLineList.add(line);
               }

               break;
            }
          }
        }

        // add rest of not changed lines
        while (oldLineNb <= oldLineList.size())
        {
          newLineList.add(oldLineList.get(oldLineNb-1));
          oldLineNb++;
        }

        // write new file
        output = new PrintWriter(new FileWriter(newFile));
        for (String line : newLineList)
        {
          output.println(line);
        }
        output.close(); output = null;
      }

      patchChunk.close(); patchChunk = null;
    }
    catch (IOException exception)
    {
Dprintf.dprintf("exception=%s",exception);
      return false;
    }
    finally
    {
      if (output != null) output.close();
      if (patchChunk != null) patchChunk.close();
    }

    return true;
  }

  /** unapply patch
   * @return true iff patch unapplied without errors
   */
  public boolean unapply()
  {
    return unapply(1000);
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Patch {"+number+", summary: "+summary+", state: "+state.toString()+", lines: "+lines.length+", tests: "+testSet.toString()+"}";
  }

  //-----------------------------------------------------------------------

  /** parse unified diff index
   * @param string diff index string
   * @param index index array [line nb,length] or null
   */
  private void parseDiffIndex(int[] index, String string)
  {
    Object[] data = new Object[2];
    if      (StringParser.parse(string,"%d,%d",data))
    {
      index[0] = (Integer)data[0];
      index[1] = (Integer)data[1];
    }
    else if (StringParser.parse(string,"%d",data))
    {
      index[0] = (Integer)data[0];
      index[1] = 1;
    }
    else
    {
      index[0] = 0;
      index[1] = 0;
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
    String                 oldFileName;
    String                 newFileName;
//??? required
    int                    lineNb;
    int[]                  oldIndex;       // chunk index old file (line nb, length)
    int[]                  newIndex;       // chunk index new file (line nb, length)
    ArrayList<PatchLines>  patchLineList;  // list of context/add/remove lines

    private int            index;          // line index if lines array given
    private BufferedReader input;          // file input if file given
    private String         line;

    // ------------------------ native functions ----------------------------

    // ---------------------------- methods ---------------------------------

    /** create patch chunk
     */
    PatchChunk()
      throws IOException
    {
      this.oldFileName   = null;
      this.newFileName   = null;
      this.oldIndex      = new int[2];
      this.newIndex      = new int[2];
      this.lineNb        = 0;
      this.patchLineList = new ArrayList<PatchLines>();

      if (file != null)
      {
        this.index = -1;
        this.input = new BufferedReader(new FileReader(file));
        this.line  = null;
      }
      else
      {
        this.index = 0;
        this.input = null;
        this.line  = null;
      }
    }

    public void close()
    {
      if (input != null)
      {
        try { input.close(); } catch (IOException exception) { /* ignored */ }
      }
    }

    /** parse next file entry in patch
     * @return true iff file entry found
     */
    public boolean nextFile()
      throws IOException
    {
      final Pattern PATTERN_OLD_FILENAME1 = Pattern.compile("^\\-\\-\\-\\s+(.*?)\\t\\s*.*",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_OLD_FILENAME2 = Pattern.compile("^\\-\\-\\-\\s+([^\\s]*)\\s*.*",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_NEW_FILENAME1 = Pattern.compile("^\\+\\+\\+\\s+(.*?)\\t\\s*.*",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_NEW_FILENAME2 = Pattern.compile("^\\+\\+\\+\\s+([^\\s]*)\\s*.*",Pattern.CASE_INSENSITIVE);

      String  line;
      Matcher matcher = null;

      oldFileName = null;
      newFileName = null;

      // skip line with diff command
      getLine();

      // find "--- <old file name>[\t<date/time>]"
      while (   ((line = getLine()) != null)
             && !(matcher = PATTERN_OLD_FILENAME1.matcher(line)).matches()
             && !(matcher = PATTERN_OLD_FILENAME2.matcher(line)).matches()
            )
      {
        Onzen.printWarning("unknown diff line1 '%s'",line);
      }
      if ((matcher != null) && matcher.matches())
      {
        oldFileName = matcher.group(1);
      }

      // find "+++ <new file name>[\t<date/time>]"
      while (   ((line = getLine()) != null)
             && !(matcher = PATTERN_NEW_FILENAME1.matcher(line)).matches()
             && !(matcher = PATTERN_NEW_FILENAME2.matcher(line)).matches()
            )
      {
        Onzen.printWarning("unknown diff line2 '%s'",line);
      }
      if ((matcher != null) && matcher.matches())
      {
        newFileName = matcher.group(1);
      }

      return (oldFileName != null) && (newFileName != null);
    }

    /** parse next chunk in patch
     * @return true iff chunk found
     */
    public boolean nextChunk()
      throws IOException
    {
      final Pattern PATTERN_INDEX  = Pattern.compile("^@@\\s+\\-([\\d,]*)\\s+\\+([\\d,]*)\\s+@@$",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_COPY   = Pattern.compile("^\\s(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_ADD    = Pattern.compile("^\\+(.*)",Pattern.CASE_INSENSITIVE);
      final Pattern PATTERN_REMOVE = Pattern.compile("^\\-(.*)",Pattern.CASE_INSENSITIVE);

      lineNb = 0;
      patchLineList.clear();

      // find next chunk @@ -<line nb>,<count> +<line nb>,<count> @@
      String  line;
      Matcher matcher;
      if (   ((line = getLine()) != null)
          && (matcher = PATTERN_INDEX.matcher(line)).matches()
         )
      {
        parseDiffIndex(oldIndex,matcher.group(1));
        parseDiffIndex(newIndex,matcher.group(2));
//Dprintf.dprintf("%s -- index=%d,%d",matcher.group(1),indexOld[0],indexOld[1]);
        lineNb = oldIndex[0];

        ArrayList<String> lineList = new ArrayList<String>();
        int               oldLines = 0;
        int               newLines = 0;
        boolean           done     = false;
        while (   !done
               && ((line = getLine()) != null)
              )
        {
          if      (line.startsWith(" "))
          {
            // get context lines
            lineList.clear();
            do
            {
              lineList.add(line.substring(1));
            }
            while (   ((line = getLine()) != null)
                   && line.startsWith(" ")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.CONTEXT,lineList));

            ungetLine(line);
          }
          else if (line.startsWith("+"))
          {
            // get added lines
            lineList.clear();
            do
            {
              lineList.add(line.substring(1));
            }
            while (   ((line = getLine()) != null)
                   && line.startsWith("+")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.ADD,lineList));

            ungetLine(line);
          }
          else if (line.startsWith("-"))
          {
            // get removed lines
            lineList.clear();
            do
            {
              lineList.add(line.substring(1));
            }
            while (   ((line = getLine()) != null)
                   && line.startsWith("-")
                  );
            patchLineList.add(new PatchLines(PatchLineTypes.REMOVE,lineList));

            ungetLine(line);
          }
          else
          {
            // no context/add/remove line -> done parsing this chunk
//Dprintf.dprintf("xx=%s",lines[patchChunk.index]);
            done = true;
          }
        }
        ungetLine(line);

        return true;
      }
      else
      {
        ungetLine(line);

        return false;
      }
    }

    /** append chunk to file
     */
    public void appendToFile(File file)
      throws IOException
    {
      PrintWriter output = null;
      try
      {
        output = new PrintWriter(new FileWriter(file,true));
        output.println(String.format("--- %s",oldFileName));
        output.println(String.format("+++ %s",newFileName));
        output.println(String.format("@@ -%d,%d +%d,%d @@",oldIndex[0],oldIndex[1],newIndex[0],newIndex[1]));
        for (PatchLines patchLines : patchLineList)
        {
          char prefix = ' ';
          switch (patchLines.type)
          {
            case CONTEXT: prefix = ' '; break;
            case ADD:     prefix = '+'; break;
            case REMOVE:  prefix = '-'; break;
          }

          for (String line : patchLines.lines)
          {
            output.println(prefix+line);
          }
        }
        output.close(); output = null;
      }
      finally
      {
        if (output != null) output.close();
      }
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "PatchChunk {"+oldFileName+", "+newFileName+", line #: "+lineNb+"}";
    }

    /** check if end-of-file reached in chunk
     * @return true iff eof
     */
    private boolean eof()
    {
      if (line == null)
      {
        if (input != null)
        {
          try
          {
            if (line == null) line = input.readLine();
            return (line == null);
          }
          catch (IOException exception)
          {
            return true;
          }
        }
        else
        {
          return (index < lines.length);
        }
      }
      else
      {
        return false;
      }
    }

    /** get next line from patch
     * @return line or null on EOF
     */
    private String getLine()
      throws IOException
    {
      String line;

      if (this.line == null)
      {
        // read line
        if (input != null)
        {
          // file
          line = input.readLine();
        }
        else
        {
          // lines array
          if (index < lines.length)
          {
            line = lines[index];
            index++;
          }
          else
          {
            line = null;
          }
        }
      }
      else
      {
        // get line from last unget
        line = this.line;
        this.line = null;
      }

      return line;
    }

    /** unget line
     * @param line line
     */
    private void ungetLine(String line)
    {
      this.line = line;
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
                              "  id                INTEGER PRIMARY KEY, "+
                              "  datetime          INTEGER DEFAULT (DATETIME('now')), "+
                              "  rootPath          TEXT, "+
                              "  state             INTEGER, "+
                              "  summary           TEXT, "+
                              "  message           TEXT, "+
                              "  data              TEXT,"+
                              "  revision1         TEXT, "+
                              "  revision2         TEXT, "+
                              "  ignoreWhitespaces INTEGER "+
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
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS tests ( "+
                              "  id       INTEGER PRIMARY KEY, "+
                              "  patchId  INTEGER, "+
                              "  test     TEXT "+
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
