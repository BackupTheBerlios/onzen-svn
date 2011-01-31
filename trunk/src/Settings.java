/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Settings.java,v $
* $Revision: 1.2 $
* $Author: torsten $
* Contents: load/save program settings
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import static java.lang.annotation.ElementType.FIELD; 
import static java.lang.annotation.ElementType.TYPE; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.LinkedHashSet;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

/****************************** Classes ********************************/

/** config comment annotation
 */
@Target({TYPE,FIELD}) 
@Retention(RetentionPolicy.RUNTIME)
@interface ConfigComment
{
  String[] text() default {""};                  // comment before value
}

/** config value annotation
 */
@Target({TYPE,FIELD}) 
@Retention(RetentionPolicy.RUNTIME)
@interface ConfigValue
{
  String name()         default "";              // name of value
  String defaultValue() default "";              // default value
  Class  type()         default DEFAULT.class;   // adapter class

  static final class DEFAULT
  {
  }
}

/** config value adapter
 */
abstract class ConfigValueAdapter<String,Value>
{
  /** convert to value
   * @param string string
   * @return value
   */
  abstract public Value toValue(String string) throws Exception;

  /** convert to string
   * @param value value
   * @return string
   */
  abstract public String toString(Value value) throws Exception;

  /** check if equals
   * @param value0,value1 values to compare
   * @return true if value0==value1
   */
  public boolean equals(Value value0, Value value1)
  {
    return false;
  }
}

/** settings
 */
public class Settings
{
  /** hidden pattern
   */
  static class FilePattern
  {
    public final String  string;
    public final Pattern pattern;

    /** create file pattern
     * @param string glob pattern string
     */
    FilePattern(String string)
    {
      this.string  = string;
      this.pattern = Pattern.compile(StringUtils.globToRegex(string));
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "FilePattern {"+string+"}";
    }
  }

  /** config value adapter String <-> file pattern
   */
  class ConfigValueAdapterFilePattern extends ConfigValueAdapter<String,FilePattern>
  {
    public FilePattern toValue(String string) throws Exception
    {
      return new FilePattern(StringUtils.unescape(string));
    }

    public String toString(FilePattern filePattern) throws Exception
    {
      return StringUtils.escape(filePattern.string);
    }

    public boolean equals(FilePattern filePattern0, FilePattern filePattern1)
    {
      return filePattern0.string.equals(filePattern1.string);
    }
  }

  /** config value adapter String <-> size
   */
  class ConfigValueAdapterSize extends ConfigValueAdapter<String,Point>
  {
    public Point toValue(String string) throws Exception
    {
      Point point = null;

      StringTokenizer tokenizer = new StringTokenizer(string,"x");
      point = new Point(Integer.parseInt(tokenizer.nextToken()),
                        Integer.parseInt(tokenizer.nextToken())
                       );

      return point;
    }

    public String toString(Point p) throws Exception
    {
      return String.format("%dx%d",p.x,p.y);
    }
  }

  /** column sizes
   */
  static class ColumnSizes
  {
    public final int[] width;

    /** create column sizes
     * @param width width array
     */
    ColumnSizes(int[] width)
    {
      this.width = width;
    }

    /** create column sizes
     * @param width width (int list)
     */
    ColumnSizes(Object... width)
    {
      this.width = new int[width.length];
      for (int z = 0; z < width.length; z++)
      {
        this.width[z] = (Integer)width[z];
      }
    }

    /** create column sizes
     * @param widthList with list
     */
    ColumnSizes(ArrayList<Integer> widthList)
    {
      this.width = new int[widthList.size()];
      for (int z = 0; z < widthList.size(); z++)
      {
        this.width[z] = widthList.get(z);
      }
    }

    /** get width
     * @param columNb column index (0..n-1)
     * @return width or 0
     */
    public int get(int columNb)
    {
      return (columNb < width.length) ? width[columNb] : 0;
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "ColumnSizes {"+width+"}";
    }
  }

  /** config value adapter String <-> column width array
   */
  class ConfigValueAdapterWidthArray extends ConfigValueAdapter<String,ColumnSizes>
  {
    public ColumnSizes toValue(String string) throws Exception
    {
      StringTokenizer tokenizer = new StringTokenizer(string,",");
      ArrayList<Integer> widthList = new ArrayList<Integer>();
      while (tokenizer.hasMoreTokens())
      {
        widthList.add(Integer.parseInt(tokenizer.nextToken()));
      }
      return new ColumnSizes(widthList);
    }

    public String toString(ColumnSizes columnSizes) throws Exception
    {
      StringBuilder buffer = new StringBuilder();
      for (int width : columnSizes.width)
      {
        if (buffer.length() > 0) buffer.append(',');
        buffer.append(Integer.toString(width));
      }
      return buffer.toString();
    }

    public boolean equals(FilePattern filePattern0, FilePattern filePattern1)
    {
      return filePattern0.string.equals(filePattern1.string);
    }
  }

  /** color
   */
  static class Color
  {
    public final RGB foreground;
    public final RGB background;

    /** create color
     * @param foreground,background foreground/background RGB values
     */
    Color(RGB foreground, RGB background)
    {
      this.foreground = foreground;
      this.background = background;
    }

    /** create color
     * @param foreground foreground/background RGB values
     */
    Color(RGB foreground)
    {
      this(foreground,foreground);
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "Color {"+foreground+", "+background+"}";
    }
  }

  /** config value adapter String <-> editor
   */
  class ConfigValueAdapterColor extends ConfigValueAdapter<String,Color>
  {
    public Color toValue(String string) throws Exception
    {
      Color color = null;

      Object[] data = new Object[6];
      if      (StringParser.parse(string,"%d,%d,%d:%d,%d,%d",data))
      {
        color = new Color(new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]),
                          new RGB((Integer)data[3],(Integer)data[4],(Integer)data[5])
                         );
      }
      else if (StringParser.parse(string,"%d,%d,%d",data))
      {
        color = new Color(new RGB((Integer)data[0],(Integer)data[1],(Integer)data[2]));
      }
      else
      {
        throw new Exception(String.format("Cannot parse color definition '%s'",string));
      }

      return color;
    }

    public String toString(Color color) throws Exception
    {
      if      (color.foreground != null)
      {
        if      ((color.background != null) && (color.foreground != color.background))
        {
          return  ((color.foreground != null) ? color.foreground.red+","+color.foreground.green+","+color.foreground.blue : "")
                 +":"
                 +((color.background != null) ? color.background.red+","+color.background.green+","+color.background.blue : "");
        }
        else
        {
          return color.foreground.red+","+color.foreground.green+","+color.foreground.blue;
        }
      }
      else if (color.background != null)
      {
        return color.background.red+","+color.background.green+","+color.background.blue;
      }
      else
      {
        return "0,0,0";
      }
    }
  }

  /** config value adapter String <-> editor
   */
  class ConfigValueAdapterKey extends ConfigValueAdapter<String,Integer>
  {
    public Integer toValue(String string) throws Exception
    {
      int accelerator = 0;
      if (!string.isEmpty())
      {
        accelerator = Widgets.textToAccelerator(string);
        if (accelerator == 0)
        {
          throw new Exception(String.format("Cannot parse key definition '%s'",string));
        }
      }

      return accelerator;
    }

    public String toString(Integer accelerator) throws Exception
    {
      return Widgets.acceleratorToText(accelerator);
    }
  }

  /** editor
   */
  static class Editor
  {
    public final String  mimeTypePattern;
    public final String  command;
    public final Pattern pattern;

    /** create editor
     * @param mimeTypePattern glob pattern string
     * @param command command
     */
    Editor(String mimeTypePattern, String command)
    {
      this.mimeTypePattern = mimeTypePattern;
      this.command         = command;
      this.pattern         = Pattern.compile(StringUtils.globToRegex(mimeTypePattern));
    }

    /** convert data to string
     * @return string
     */
    public String toString()
    {
      return "Editor {"+mimeTypePattern+", command: "+command+"}";
    }
  }

  /** config value adapter String <-> editor
   */
  class ConfigValueAdapterEditor extends ConfigValueAdapter<String,Editor>
  {
    public Editor toValue(String string) throws Exception
    {
      Editor editor = null;

      Object[] data = new Object[2];
      if (StringParser.parse(string,"%s:% s",data))
      {
        editor = new Editor(((String)data[0]).trim(),((String)data[1]).trim());
      }
      else
      {
        throw new Exception(String.format("Cannot parse editor definition '%s'",string));
      }

      return editor;
    }

    public String toString(Editor editor) throws Exception
    {
      return editor.mimeTypePattern+":"+editor.command;
    }

    public boolean equals(Editor editor0, Editor editor1)
    {
      return    editor0.mimeTypePattern.equals(editor1.mimeTypePattern)
             && editor0.command.equals(editor1.command);
    }
  }

  /** config value adapter String <-> file data state set
   */
  class ConfigValueAdapterFileDataStates extends ConfigValueAdapter<String,EnumSet<FileData.States>>
  {
    public EnumSet<FileData.States> toValue(String string) throws Exception
    {
      EnumSet<FileData.States> enumSet = EnumSet.noneOf(FileData.States.class);

      StringTokenizer tokenizer = new StringTokenizer(string,",");
      while (tokenizer.hasMoreTokens())
      {
        enumSet.add(FileData.States.parse(tokenizer.nextToken()));
      }

      return enumSet;
    }

    public String toString(EnumSet<FileData.States> enumSet) throws Exception
    {
      StringBuilder buffer = new StringBuilder();

      for (FileData.States state : enumSet)
      {
        if (buffer.length() > 0) buffer.append(',');
        buffer.append(state.toString());
      }

      return buffer.toString();
    }
  }

  // --------------------------- constants --------------------------------
  public static final String ONZEN_DIRECTORY                = System.getProperty("user.home")+File.separator+".onzen2";

  /** host system
   */
  public enum HostSystems
  {
    UNKNOWN,
    LINUX,
    SOLARIS,
    WINDOWS,
    MACOS,
    QNX
  };

  private static final String ONZEN_CONFIG_FILE_NAME = ONZEN_DIRECTORY+File.separator+"onzen.cfg";

  // --------------------------- variables --------------------------------

  @ConfigComment(text={"Onzen configuration",""})

  // program settings
  public static HostSystems              hostSystem                    = HostSystems.LINUX;
  @ConfigValue
  public static String                   tmpDirectory                  = "/tmp";

  @ConfigValue
  public static String                   dateFormat                    = "yyyy-MM-dd";
  @ConfigValue
  public static String                   timeFormat                    = "HH:mm:ss";
  @ConfigValue
  public static String                   dateTimeFormat                = "yyyy-MM-dd HH:mm:ss";

  @ConfigValue
  public static int                      maxBackgroundTasks            = 8;

  @ConfigValue
  public static int                      maxMessageHistory             = 50;

  @ConfigComment(text={"","UDP message broadcasting"})
  @ConfigValue
  public static String                   messageBroadcastAddress       = "192.168.11.255";
  @ConfigValue
  public static int                      messageBroadcastPort          = 9583;

  @ConfigComment(text={"","Hidden files in file tree: <pattern>"})
  @ConfigValue(name="hiddenFilePattern", type=ConfigValueAdapterFilePattern.class)
  public static FilePattern[]            hiddenFilePatterns            = new FilePattern[]{};
  @ConfigComment(text={"","Hidden directories in file tree: <pattern>"})
  @ConfigValue(name="hiddenDirectoryPattern", type=ConfigValueAdapterFilePattern.class)
  public static FilePattern[]            hiddenDirectoryPatterns       = new FilePattern[]
                                                                         {
                                                                           new FilePattern("CVS"),
                                                                           new FilePattern(".svn"),
                                                                           new FilePattern(".hg"),
                                                                           new FilePattern(".git")
                                                                         };

  @ConfigComment(text={"","Geometry: <width>x<height>"})
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryMain                  = new Point(800,600);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryCommit                = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryAdd                   = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRemove                = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRevert                = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRename                = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryDiff                  = new Point(800,600);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRevisions             = new Point(800,400);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRevisionBox           = new Point(200, 70);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryView                  = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryNewFile               = new Point(300,200);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryNewDirectory          = new Point(300,200);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryRenameLocalFile       = new Point(300,200);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryDeleteLocalFiles      = new Point(300,200);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryChangedFiles          = new Point(600,500); 
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point                    geometryAnnotations           = new Point(800,500); 
  @ConfigValue(type=ConfigValueAdapterWidthArray.class)
  public static ColumnSizes              geometryAnnotationsColumn     = new ColumnSizes(60,60,80,60,100);

  @ConfigComment(text={"","Colors: <rgb foreground>:<rgb background> or <rgb foreground>"})
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorDiffAdded                 = new Color(null,new RGB(128,255,128));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorDiffDeleted               = new Color(null,new RGB(128,128,255));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorDiffChanged               = new Color(null,new RGB(255,  0,  0));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorDiffChangedWhitespaces    = new Color(null,new RGB(255,128,128));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorDiffSearchText            = new Color(new RGB(  0,  0,255),null);

  @ConfigComment(text={""})
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusOK                  = new Color(null,new RGB(255,255,255));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusUnknown             = new Color(new RGB(196,196,196),null);
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusUpdate              = new Color(null,new RGB(255, 255, 0));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusCheckout            = new Color(null,new RGB(  0,  0,255));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusModified            = new Color(null,new RGB(  0,255,  0));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusMerge               = new Color(null,new RGB(128,128,  0));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusConflict            = new Color(null,new RGB(255,128,128));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusAdded               = new Color(null,new RGB(255,160,160));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusRemoved             = new Color(null,new RGB( 64, 64,128));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusNotExists           = new Color(null,new RGB(192,192,192));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusWaiting             = new Color(null,new RGB(220,220,220));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusError               = new Color(null,new RGB(255,  0,  0));
  @ConfigValue(type=ConfigValueAdapterColor.class)
  public static Color                   colorStatusUpdateStatus        = new Color(new RGB(128,128,128),null);

  @ConfigComment(text={"","shown file states in changed file list"})
  @ConfigValue(type=ConfigValueAdapterFileDataStates.class)
  public static EnumSet<FileData.States> changedFilesShowStates        = EnumSet.allOf(FileData.States.class);


  @ConfigComment(text={"","Accelerator keys"})
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyOpenRepository             = SWT.CTRL+'O';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyEditRepository             = SWT.CTRL+'E';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyCloseRepository            = SWT.CTRL+'W';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyStatus                     = SWT.F5;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyUpdate                     = SWT.CTRL+'U';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyCommit                     = '#';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyCommitPatch                = SWT.CTRL+'E';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyPatch                      = SWT.CTRL+'P';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyAdd                        = '+';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRemove                     = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRename                     = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRevert                     = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyApplyPatches               = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyUnapplyPatches             = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyDiff                       = SWT.CTRL+'D';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRevisionInfo               = SWT.CTRL+'I';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRevisions                  = SWT.CTRL+'R';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keySolve                      = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyChangedFiles               = SWT.CTRL+'L';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyAnnotations                = SWT.CTRL+'A';
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyReread                     = SWT.F5;

  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyOpenFileWith               = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyNewFile                    = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyNewDirectory               = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyRenameLocal                = SWT.NONE;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyDeleteLocal                = SWT.NONE;

  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyFindPrev                   = SWT.CTRL+SWT.ARROW_LEFT;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyFindNext                   = SWT.CTRL+SWT.ARROW_RIGHT;
  @ConfigValue(type=ConfigValueAdapterKey.class)
  public static int                      keyFindNextDiff               = ' ';


/*
  @ConfigComment(text={"Geometry (<width>x<height>)")
  static class geometry
  {
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   commit                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   add                    = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   remove                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   revert                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   rename                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   diff                   = new Point(800,600);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   revisions              = new Point(800,400);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point                   revisionBox            = new Point(200, 70);
  };

  @ConfigComment(text={"Colors (RGB)")
  static class color
  {
  }
*/

  @ConfigComment(text={"","Editors: <mime type>:<command>"})
  @ConfigValue(type=ConfigValueAdapterEditor.class)
  public static Editor[]         editors                        = new Editor[0];

  // CVS
  public static boolean          cvsPruneEmtpyDirectories       = false;

  // debug
  public static boolean          debugFlag                      = false;

  // help
  public static boolean          helpFlag                       = false;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** load program settings
   * @param fileName file nam
   * @return Errors.NONE or error code
   */
  public static int load(String fileName)
  {
    // load file
    File file = new File(fileName);
    if (file.exists())
    {
      BufferedReader input = null;
      try
      {
        // get setting classes
        Class[] settingClasses = getSettingClasses();

        // open file
        input = new BufferedReader(new FileReader(file));

        // read file
        int      lineNb = 0;
        String   line;
        Object[] data = new Object[2];
        while ((line = input.readLine()) != null)
        {
          line = line.trim();
          lineNb++;

          // check comment
          if (line.isEmpty() || line.startsWith("#"))
          {
            continue;
          }

          // parse
          if (StringParser.parse(line,"%s = % s",data))
          {
            String name   = (String)data[0];
            String string = (String)data[1];

            for (Class clazz : settingClasses)
            {
              for (Field field : clazz.getDeclaredFields())
              {
                for (Annotation annotation : field.getDeclaredAnnotations())
                {
                  if (annotation instanceof ConfigValue)
                  {
                    ConfigValue configValue = (ConfigValue)annotation;

                    if (((!configValue.name().isEmpty()) ? configValue.name() : field.getName()).equals(name))
                    {
                      try
                      {
                        Class type = field.getType();
                        if (type.isArray())
                        {
                          type = type.getComponentType();
                          if      (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                          {
                            // instantiate config adapter class
                            ConfigValueAdapter configValueAdapter;
                            Class enclosingClass = configValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = configValue.type().getDeclaredConstructor(Settings.class);
                              configValueAdapter = (ConfigValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                            }

                            // convert to value
                            Object value = configValueAdapter.toValue(string);
                            field.set(null,addArrayUniq((Object[])field.get(null),value,configValueAdapter));
                          }
                          else if (type == int.class)
                          {
                            int value = Integer.parseInt(string);
                            field.set(null,addArrayUniq((int[])field.get(null),value));
                          }
                          else if (type == long.class)
                          {
                            long value = Long.parseLong(string);
                            field.set(null,addArrayUniq((long[])field.get(null),value));
                          }
                          else if (type == String.class)
                          {
                            field.set(null,addArrayUniq((String[])field.get(null),StringUtils.unescape(string)));
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                        else
                        {
                          if      (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                          {
                            // instantiate config adapter class
                            ConfigValueAdapter configValueAdapter;
                            Class enclosingClass = configValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = configValue.type().getDeclaredConstructor(Settings.class);
                              configValueAdapter = (ConfigValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                            }

                            // convert to value
                            Object value = configValueAdapter.toValue(string);
                            field.set(null,value);
                          }
                          else if (type == int.class)
                          {
                            int value = Integer.parseInt(string);
                            field.setInt(null,value);
                          }
                          else if (type == long.class)
                          {
                            long value = Long.parseLong(string);
                            field.setLong(null,value);
                          }
                          else if (type == String.class)
                          {
                            field.set(null,StringUtils.unescape(string));
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                      }
                      catch (NumberFormatException exception)
                      {
                        Onzen.printWarning("Cannot parse number '%s' for configuration value '%s' in line %d",string,name,lineNb);
                      }
                      catch (Exception exception)
                      {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
                      }
                    }
                  }
                  else
                  {
                  }
                }
              }
            }
          }
          else
          {
            Onzen.printWarning("Unknown configuration value '%s' in line %d",line,lineNb);
          }
        }

        // close file
        input.close();
      }
      catch (IOException exception)
      {
        // ignored
      }
      finally
      {
        try
        {
          if (input != null) input.close();
        }
        catch (IOException exception)
        {
          // ignored
        }
      }
    }

    return 0;
  }

  /** load program settings
   * @param fileName file nam
   * @return Errors.NONE or error code
   */
  public static int load()
  {
    return load(ONZEN_CONFIG_FILE_NAME);
  }

  /** save program settings
   * @param fileName file nam
   * @return Errors.NONE or error code
   */
  public static int save(String fileName)
  {
    // create directory
    File directory = new File(fileName).getParentFile();
    if ((directory != null) && !directory.exists()) directory.mkdirs();

    PrintWriter output = null;
    try
    {
      // get setting classes
      Class[] settingClasses = getSettingClasses();

      // open file
      output = new PrintWriter(new FileWriter(fileName));

      // write settings
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
//Dprintf.dprintf("field=%s",field);
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof ConfigValue)
            {
              ConfigValue configValue = (ConfigValue)annotation;

              // get value and write to file
              String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();
              try
              {
                Class type = field.getType();
                if (type.isArray())
                {
                  type = type.getComponentType();
                  if      (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                  {
                    // instantiate config adapter class
                    ConfigValueAdapter configValueAdapter;
                    Class enclosingClass = configValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = configValue.type().getDeclaredConstructor(Settings.class);
                      configValueAdapter = (ConfigValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                    }

                    // convert to string
                    for (Object object : (Object[])field.get(null))
                    {
                      String value = (String)configValueAdapter.toString(object);
                      output.printf("%s = %s\n",name,value);
                    }
                  }
                  else if (type == int.class)
                  {
                    for (int value : (int[])field.get(null))
                    {
                      output.printf("%s = %d\n",name,value);
                    }
                  }
                  else if (type == long.class)
                  {
                    for (long value : (long[])field.get(null))
                    {
                      output.printf("%s = %ld\n",name,value);
                    }
                  }
                  else if (type == String.class)
                  {
                    for (String value : (String[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.escape(value));
                    }
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
                else
                {
                  if      (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                  {
                    // instantiate config adapter class
                    ConfigValueAdapter configValueAdapter;
                    Class enclosingClass = configValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = configValue.type().getDeclaredConstructor(Settings.class);
                      configValueAdapter = (ConfigValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                    }

                    // convert to string
                    String value = (String)configValueAdapter.toString(field.get(null));
                    output.printf("%s = %s\n",name,value);
                  }
                  else if (type == int.class)
                  {
                    int value = field.getInt(null);
                    output.printf("%s = %d\n",name,value);
                  }
                  else if (type == long.class)
                  {
                    long value = field.getLong(null);
                    output.printf("%s = %ld\n",name,value);
                  }
                  else if (type == String.class)
                  {
                    String value = (type != null) ? (String)field.get(null) : configValue.defaultValue();
                    output.printf("%s = %s\n",name,StringUtils.escape(value));
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
              }
              catch (Exception exception)
              {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
              }
            }
            else if (annotation instanceof ConfigComment)
            {
              ConfigComment configComment = (ConfigComment)annotation;

              for (String line : configComment.text())
              {
                if (!line.isEmpty())
                {
                  output.printf("# %s\n",line);
                }
                else
                {
                  output.printf("\n");
                }
              }
            }
          }
        }
      }

      // close file
      output.close();
    }
    catch (IOException exception)
    {
      return -1;
    }
    finally
    {
      if (output != null) output.close();
    }

    return 0;
  }

  /** save program settings
   * @return Errors.NONE or error code
   */
  public static int save()
  {
    return save(ONZEN_CONFIG_FILE_NAME);
  }

  //-----------------------------------------------------------------------

  /** get all setting classes
   * @return classes array
   */
  private static Class[] getSettingClasses()
  {
    // get all setting classes
    ArrayList<Class> classList = new ArrayList<Class>();

    classList.add(Settings.class);
    for (Class clazz : Settings.class.getDeclaredClasses())
    {
//Dprintf.dprintf("c=%s",clazz);
      classList.add(clazz);
    }

    return classList.toArray(new Class[classList.size()]);
  }

  /** unique add element to int array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static int[] addArrayUniq(int[] array, int n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static long[] addArrayUniq(long[] array, long n)
  {
    int z = 0;
    while ((z < array.length) && (array[z] != n))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = n;
    }

    return array;
  }

  /** unique add element to string array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static String[] addArrayUniq(String[] array, String string)
  {
    int z = 0;
    while ((z < array.length) && !array[z].equals(string))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = string;
    }

    return array;
  }

  /** unique add element to object array
   * @param array array
   * @param object element
   * @param settingAdapter setting adapter (use equals() function)
   * @return extended array or array
   */
  private static Object[] addArrayUniq(Object[] array, Object object, ConfigValueAdapter configValueAdapter)
  {
    int z = 0;
    while ((z < array.length) && !configValueAdapter.equals(array[z],object))
    {
      z++;
    }
    if (z >= array.length)
    {
      array = Arrays.copyOf(array,array.length+1);
      array[array.length-1] = object;
    }

    return array;
  }
}

/* end of file */
