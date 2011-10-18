/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Settings.java,v $
* $Revision: 1.9 $
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
import java.util.EnumSet;
import java.util.StringTokenizer;
import java.util.LinkedHashSet;

import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

/****************************** Classes ********************************/

/** setting comment annotation
 */
@Target({TYPE,FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface SettingComment
{
  String[] text() default {""};                  // comment before value
}

/** setting value annotation
 */
@Target({TYPE,FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface SettingValue
{
  String name()         default "";              // name of value
  String defaultValue() default "";              // default value
  Class  type()         default DEFAULT.class;   // adapter class

  static final class DEFAULT
  {
  }
}

/** setting value adapter
 */
abstract class SettingValueAdapter<String,Value>
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
  static class FilePattern implements Cloneable
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

    /** clone object
     * @return cloned object
     */
    public FilePattern clone()
    {
      return new FilePattern(string);
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
  class SettingValueAdapterFilePattern extends SettingValueAdapter<String,FilePattern>
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
  class SettingValueAdapterSize extends SettingValueAdapter<String,Point>
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
  class SettingValueAdapterWidthArray extends SettingValueAdapter<String,ColumnSizes>
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
  static class Color implements Cloneable
  {
    public RGB foreground;
    public RGB background;

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

    /** clone object
     * @return cloned object
     */
    public Color clone()
    {
      return new Color(foreground,background);
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
  class SettingValueAdapterColor extends SettingValueAdapter<String,Color>
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
  class SettingValueAdapterKey extends SettingValueAdapter<String,Integer>
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
      return Widgets.menuAcceleratorToText(accelerator);
    }
  }

  /** editor
   */
  static class Editor implements Cloneable
  {
    public String  mimeTypePattern;
    public String  command;
    public Pattern pattern;

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

    /** create editor
     */
    Editor()
    {
      this("","");
    }

    /** clone object
     * @return cloned object
     */
    public Editor clone()
    {
      return new Editor(mimeTypePattern,command);
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
  class SettingValueAdapterEditor extends SettingValueAdapter<String,Editor>
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
/* obsolete, replaced by generic EnumSet type
  class SettingValueAdapterDiffTypes extends SettingValueAdapter<String,EnumSet<DiffData.Types>>
  {
    public EnumSet<DiffData.Types> toValue(String string) throws Exception
    {
      EnumSet<DiffData.Types> enumSet = EnumSet.noneOf(DiffData.Types.class);

      StringTokenizer tokenizer = new StringTokenizer(string,",");
      while (tokenizer.hasMoreTokens())
      {
        enumSet.add(DiffData.Types.parse(tokenizer.nextToken()));
      }

      return enumSet;
    }

    public String toString(EnumSet<DiffData.Types> enumSet) throws Exception
    {
      StringBuilder buffer = new StringBuilder();

      for (DiffData.Types state : enumSet)
      {
        String string = state.toString();
        if (string != null)
        {
          if (buffer.length() > 0) buffer.append(',');
          buffer.append(string);
        }
      }

      return buffer.toString();
    }
  }
*/

  /** config value adapter String <-> file data state set
   */
/* obsolete, replaced by generic EnumSet type
  class SettingValueAdapterFileDataStates extends SettingValueAdapter<String,EnumSet<FileData.States>>
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
        String string = state.toString();
        if (string != null)
        {
          if (buffer.length() > 0) buffer.append(',');
          buffer.append(string);
        }
      }

      return buffer.toString();
    }
  }
*/

  /** config value adapter String <-> font data
   */
  class SettingValueAdapterFontData extends SettingValueAdapter<String,FontData>
  {
    public FontData toValue(String string) throws Exception
    {
      return Widgets.textToFontData(string);
    }

    public String toString(FontData fontData) throws Exception
    {
      String string;

      if (fontData != null)
      {
        string = Widgets.fontDataToText(fontData);
      }
      else
      {
        string = "";
      }

      return string;
    }
  }

  // --------------------------- constants --------------------------------
  public static final String ONZEN_DIRECTORY                = System.getProperty("user.home")+File.separator+".onzen";

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

  private static long lastModified = 0L;

  @SettingComment(text={"Onzen configuration",""})

  // program settings
  public static HostSystems              hostSystem                     = HostSystems.LINUX;
  @SettingValue
  public static String                   tmpDirectory                   = "/tmp";

  @SettingValue
  public static String                   dateFormat                     = "yyyy-MM-dd";
  @SettingValue
  public static String                   timeFormat                     = "HH:mm:ss";
  @SettingValue
  public static String                   dateTimeFormat                 = "yyyy-MM-dd HH:mm:ss";

  @SettingValue
  public static int                      maxBackgroundTasks             = 8;

  @SettingValue
  public static int                      maxMessageHistory              = 50;

  @SettingComment(text={"","UDP message broadcasting"})
  @SettingValue
  public static String                   messageBroadcastAddress        = "230.0.95.83";
  @SettingValue
  public static int                      messageBroadcastPort           = 9583;

  @SettingComment(text={"","Hidden files in file tree: <pattern>"})
  @SettingValue(name="hiddenFilePattern", type=SettingValueAdapterFilePattern.class)
  public static FilePattern[]            hiddenFilePatterns             = new FilePattern[]{new FilePattern(".*")};
  @SettingComment(text={"","Hidden directories in file tree: <pattern>"})
  @SettingValue(name="hiddenDirectoryPattern", type=SettingValueAdapterFilePattern.class)
  public static FilePattern[]            hiddenDirectoryPatterns        = new FilePattern[]
                                                                          {
                                                                            new FilePattern("CVS"),
                                                                            new FilePattern(".svn"),
                                                                            new FilePattern(".hg"),
                                                                            new FilePattern(".git")
                                                                          };

  @SettingComment(text={"","Geometry: <width>x<height>"})
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryMain                   = new Point(900,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryEditRepositoryList     = new Point(400,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryEditRepository         = new Point(800,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryCommit                 = new Point(500,500);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryAdd                    = new Point(500,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRename                 = new Point(500,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRemove                 = new Point(500,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRevert                 = new Point(500,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometrySetFileMode            = new Point(500,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryDiff                   = new Point(800,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRevisions              = new Point(800,400);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryChanges                = new Point(800,400);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryChangesColumn          = new ColumnSizes(70,150,120);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRevisionBox            = new Point(200, 70);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRevisionInfo           = new Point(600,500);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryView                   = new Point(500,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryNewFile                = new Point(300,200);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryNewDirectory           = new Point(300,200);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryRenameLocalFile        = new Point(400,200);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryDeleteLocalFiles       = new Point(300,200);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryChangedFiles           = new Point(850,500);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryChangedFilesColumn     = new ColumnSizes(600,100);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryAnnotations            = new Point(800,500);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryAnnotationsColumn      = new ColumnSizes(60,60,80,60,100);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryPatches                = new Point(800,600);
  @SettingValue(type=SettingValueAdapterWidthArray.class)
  public static ColumnSizes              geometryPatchesColumn          = new ColumnSizes(50,100,300);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryCreatePatch            = new Point(500,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryStorePatch             = new Point(500,600);
  @SettingValue(type=SettingValueAdapterSize.class)
  public static Point                    geometryPatchReview            = new Point(500,600);

  @SettingComment(text={"","Colors: <rgb foreground>:<rgb background> or  <rgb foreground>"})
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorDiffAdded                  = new Color(null,new RGB(128,255,128));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorDiffDeleted                = new Color(null,new RGB(128,128,255));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorDiffChanged                = new Color(null,new RGB(255,  0,  0));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorDiffChangedWhitespaces     = new Color(null,new RGB(255,128,128));

  @SettingComment(text={""})
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusOK                   = new Color(null,new RGB(255,255,255));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusUnknown              = new Color(null,new RGB(196,196,196));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusUpdate               = new Color(null,new RGB(255, 255, 0));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusCheckout             = new Color(null,new RGB(  0,  0,255));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusModified             = new Color(null,new RGB(  0,255,  0));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusMerge                = new Color(null,new RGB(128,128,  0));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusConflict             = new Color(null,new RGB(255,128,128));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusAdded                = new Color(null,new RGB(255,160,160));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusRemoved              = new Color(null,new RGB( 64, 64,128));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusNotExists            = new Color(null,new RGB(192,192,192));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusWaiting              = new Color(null,new RGB(220,220,220));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusError                = new Color(null,new RGB(255,  0,  0));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorStatusUpdateStatus         = new Color(new RGB(128,128,128),null);

  @SettingComment(text={""})
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorInactive                   = new Color(null,new RGB(196,196,196));
  @SettingValue(type=SettingValueAdapterColor.class)
  public static Color                   colorFindText                   = new Color(new RGB(  0,  0,255),null);

  @SettingComment(text={"","Fonts: <name>,<height>,normal|bold|italic|bold italic"})
  @SettingValue(type=SettingValueAdapterFontData.class)
  public static FontData                fontDiff                        = null;
  @SettingValue(type=SettingValueAdapterFontData.class)
  public static FontData                fontDiffLine                    = null;

  @SettingComment(text={"","Shown diff types"})
  @SettingValue(type=DiffData.Types.class)
  public static EnumSet<DiffData.Types> diffShowTypes                   = EnumSet.allOf(DiffData.Types.class);

  @SettingComment(text={"","Shown file states in changed file list"})
  @SettingValue(type=FileData.States.class)
  public static EnumSet<FileData.States> changedFilesShowStates         = EnumSet.allOf(FileData.States.class);

  @SettingComment(text={"","Shown patches in patch list"})
  @SettingValue
  public static boolean                  patchShowAllRepositories       = false;
  @SettingValue(type=Patch.States.class)
  public static EnumSet<Patch.States>    patchShowStates                = EnumSet.allOf(Patch.States.class);

  @SettingComment(text={"","Accelerator keys"})
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyCheckoutRepository          = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyOpenRepository              = SWT.CTRL+'O';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyEditRepositoryList          = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyEditRepository              = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyCloseRepository             = SWT.CTRL+'W';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyStatus                      = SWT.F5;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyUpdate                      = SWT.CTRL+'U';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyCommit                      = SWT.CTRL+'X';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyCreatePatch                 = SWT.CTRL+'P';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyPatches                     = SWT.CTRL+'E';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyAdd                         = '+';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRemove                      = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRename                      = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keySetFileMode                 = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRevert                      = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyApplyPatches                = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyUnapplyPatches              = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyIncomingChanges             = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyOutgoingChanges             = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyPullChanges                 = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyPushChanges                 = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyDiff                        = SWT.CTRL+'D';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRevisionInfo                = SWT.CTRL+'I';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRevisions                   = SWT.CTRL+'R';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keySolve                       = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyChangedFiles                = SWT.CTRL+'L';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyAnnotations                 = SWT.CTRL+'A';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyReread                      = SWT.F5;

  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyOpenFileWith                = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyNewFile                     = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyNewDirectory                = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyRenameLocal                 = SWT.NONE;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyDeleteLocal                 = SWT.NONE;

  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyFind                        = SWT.CTRL+'f';
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyFindPrev                    = SWT.CTRL+SWT.ARROW_LEFT;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyFindNext                    = SWT.CTRL+SWT.ARROW_RIGHT;
  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyFindNextDiff                = ' ';

  @SettingValue(type=SettingValueAdapterKey.class)
  public static int                      keyQuit                       = SWT.CTRL+'Q';


/*
  @SettingComment(text={"Geometry (<width>x<height>)")
  static class geometry
  {
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   commit                 = new Point(500,300);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   add                    = new Point(500,300);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   remove                 = new Point(500,300);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   revert                 = new Point(500,300);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   rename                 = new Point(500,300);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   diff                   = new Point(800,600);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   revisions              = new Point(800,400);
    @SettingValue(type=SettingValueAdapterSize.class)
    public static Point                   revisionBox            = new Point(200, 70);
  };

  @SettingComment(text={"Colors (RGB)")
  static class color
  {
  }
*/

  @SettingComment(text={"","Editors: <mime type>:<command>","Macros %file%:","  file - file name"})
  @SettingValue(type=SettingValueAdapterEditor.class)
  public static Editor[]         editors                                = new Editor[0];

  @SettingComment(text={"","Default mail settings"})
  @SettingValue
  public static String           mailSMTPHost                           = "";
  @SettingValue
  public static int              mailSMTPPort                           = 25;
  @SettingValue
  public static boolean          mailSMTPSSL                            = false;
  @SettingValue
  public static String           mailLogin                              = "";
  @SettingValue
  public static String           mailFrom                               = "";

  @SettingComment(text={"","Mail commands","Macros ${<name> [<format>]}:","  to - to mail address","  cc - CC mail address","  subject - subject text","  file - attachment file name"})
  @SettingValue
  public static String           commandMail                            = "mail -s '${subject}' ${to} ${cc}";
  @SettingValue
  public static String           commandMailAttachment                  = "mail -s '${subject}' -a ${file} ${to} ${cc}";

  @SettingComment(text={"","Default review server settings"})
  @SettingValue
  public static String           reviewServerHost                       = "";
  @SettingValue
  public static String           reviewServerLogin                      = "";
  @SettingValue
  public static String           reviewServerPassword                   = "";

  @SettingComment(text={"","Review commands","Macros ${<name> [<format>]}:","  server - server name","  login - user login name","  password - user login password","  repository - repository id","  reference - review reference id","  summary - summary text","  description - description text","  tests - tests","  file - diff file name"})
  @SettingValue
  public static String           commandPostReviewServer                = "post-review --server=${server} --username=${login} --password=${password} --repoid=${repository} --publish --summary='${summary}' --description='${description}' --testing-done='${tests}' --diff-filename=${file}";
  @SettingValue
  public static String           commandUpdateReviewServer              = "post-review --server=${server} --username=${login} --password=${password} --repoid=${repository} --publish --review-request-id ${reference} --summary='${summary}' --description='${description}' --testing-done='${tests}' --diff-filename=${file}";

  // general flags
  @SettingComment(text={"","Flags"})
  @SettingValue
  public static boolean          immediateCommit                        = true;

  // CVS
  @SettingComment(text={"","CVS specific settings"})
  @SettingValue
  public static String           cvsCommand                             = "cvs";
  @SettingValue
  public static boolean          cvsPruneEmtpyDirectories               = false;

  // SVN
  @SettingComment(text={"","SVN specific settings"})
  @SettingValue
  public static String           svnCommand                             = "svn";
  @SettingValue
  public static String           svnDiffCommand                         = "";
  @SettingValue
  public static String           svnDiffCommandOptions                  = "-ur";
  @SettingValue
  public static String           svnDiffCommandOptionsIgnoreWhitespaces = "-wbBEdur";

  // HG
  @SettingComment(text={"","HG specific settings"})
  @SettingValue
  public static String           hgCommand                              = "hg";
  @SettingValue
  public static String           hgDiffCommand                          = "";
  @SettingValue
  public static String           hgDiffCommandOptions                   = "-ur";
  @SettingValue
  public static String           hgDiffCommandOptionsIgnoreWhitespaces  = "-wbBEdur";
  @SettingValue
  public static boolean          hgImmediatePush                        = false;
  @SettingValue
  public static boolean          hgUseForestExtension                   = false;
  @SettingValue
  public static boolean          hgUpdateWithFetch                      = false;
  @SettingValue
  public static boolean          hgSafeUpdate                           = false;
  @SettingValue
  public static boolean          hgSingleLineCommitMessages             = false;
  @SettingValue
  public static int              hgSingleLineMaxCommitMessageLength     = 80;
  @SettingValue
  public static boolean          hgRelativePatchPaths                   = false;

  // Git
  @SettingComment(text={"","Git specific settings"})
  @SettingValue
  public static String           gitCommand                             = "git";

  // debug
  public static boolean          debugFlag                              = false;

  // help
  public static boolean          helpFlag                               = false;


  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** load program settings
   * @param file settings file to load
   */
  public static void load(File file)
  {
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
                  if (annotation instanceof SettingValue)
                  {
                    SettingValue settingValue = (SettingValue)annotation;

                    if (((!settingValue.name().isEmpty()) ? settingValue.name() : field.getName()).equals(name))
                    {
                      try
                      {
                        Class type = field.getType();
                        if (type.isArray())
                        {
                          type = type.getComponentType();
                          if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                          {
                            // instantiate config adapter class
                            SettingValueAdapter settingValueAdapter;
                            Class enclosingClass = settingValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                              settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                            }

                            // convert to value
                            Object value = settingValueAdapter.toValue(string);
                            field.set(null,addArrayUniq((Object[])field.get(null),value,settingValueAdapter));
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
                          else if (type == boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.set(null,addArrayUniq((boolean[])field.get(null),value));
                          }
                          else if (type == String.class)
                          {
                            field.set(null,addArrayUniq((String[])field.get(null),StringUtils.unescape(string)));
                          }
                          else if (type.isEnum())
                          {
                            field.set(null,addArrayUniq((Enum[])field.get(null),StringUtils.parseEnum(type,string)));
                          }
                          else if (type == EnumSet.class)
                          {
                            field.set(null,addArrayUniq((EnumSet[])field.get(null),StringUtils.parseEnumSet(type,string)));
                          }
                          else
                          {
Dprintf.dprintf("field.getType()=%s",type);
                          }
                        }
                        else
                        {
                          if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                          {
                            // instantiate config adapter class
                            SettingValueAdapter settingValueAdapter;
                            Class enclosingClass = settingValue.type().getEnclosingClass();
                            if (enclosingClass == Settings.class)
                            {
                              Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                              settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                            }
                            else
                            {
                              settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                            }

                            // convert to value
                            Object value = settingValueAdapter.toValue(string);
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
                          else if (type == boolean.class)
                          {
                            boolean value = StringUtils.parseBoolean(string);
                            field.setBoolean(null,value);
                          }
                          else if (type == String.class)
                          {
                            field.set(null,StringUtils.unescape(string));
                          }
                          else if (type.isEnum())
                          {
                            field.set(null,StringUtils.parseEnum(type,string));
                          }
                          else if (type == EnumSet.class)
                          {
                            Class enumClass = settingValue.type();
                            if (!enumClass.isEnum())
                            {
                              throw new Error(enumClass+" is not an enum class!");
                            }
                            field.set(null,StringUtils.parseEnumSet(enumClass,string));
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
  }

  /** load program settings
   * @param fileName settings file name
   */
  public static void load(String fileName)
  {
    load(new File(fileName));
  }

  /** load default program settings
   */
  public static void load()
  {
    File file = new File(ONZEN_CONFIG_FILE_NAME);

    // load file
    load(file);

    // save last modified time
    lastModified = file.lastModified();
  }

  /** save program settings
   * @param fileName file nam
   */
  public static void save(File file)
  {
    // create directory
    File directory = file.getParentFile();
    if ((directory != null) && !directory.exists()) directory.mkdirs();

    PrintWriter output = null;
    try
    {
      // get setting classes
      Class[] settingClasses = getSettingClasses();

      // open file
      output = new PrintWriter(new FileWriter(file));

      // write settings
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
//Dprintf.dprintf("field=%s",field);
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue settingValue = (SettingValue)annotation;

              // get value and write to file
              String name = (!settingValue.name().isEmpty()) ? settingValue.name() : field.getName();
              try
              {
                Class type = field.getType();
                if (type.isArray())
                {
                  type = type.getComponentType();
                  if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                  {
                    // instantiate config adapter class
                    SettingValueAdapter settingValueAdapter;
                    Class enclosingClass = settingValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                      settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                    }

                    // convert to string
                    for (Object object : (Object[])field.get(null))
                    {
                      String value = (String)settingValueAdapter.toString(object);
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
                  else if (type == boolean.class)
                  {
                    for (boolean value : (boolean[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,value ? "yes" : "no");
                    }
                  }
                  else if (type == String.class)
                  {
                    for (String value : (String[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.escape(value));
                    }
                  }
                  else if (type.isEnum())
                  {
                    for (Enum value : (Enum[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,value.toString());
                    }
                  }
                  else if (type == EnumSet.class)
                  {
                    for (EnumSet enumSet : (EnumSet[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.join(enumSet,","));
                    }
                  }
                  else
                  {
Dprintf.dprintf("field.getType()=%s",type);
                  }
                }
                else
                {
                  if      (SettingValueAdapter.class.isAssignableFrom(settingValue.type()))
                  {
                    // instantiate config adapter class
                    SettingValueAdapter settingValueAdapter;
                    Class enclosingClass = settingValue.type().getEnclosingClass();
                    if (enclosingClass == Settings.class)
                    {
                      Constructor constructor = settingValue.type().getDeclaredConstructor(Settings.class);
                      settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());
                    }
                    else
                    {
                      settingValueAdapter = (SettingValueAdapter)settingValue.type().newInstance();
                    }

                    // convert to string
                    String value = (String)settingValueAdapter.toString(field.get(null));
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
                  else if (type == boolean.class)
                  {
                    boolean value = field.getBoolean(null);
                    output.printf("%s = %s\n",name,value ? "yes" : "no");
                  }
                  else if (type == String.class)
                  {
                    String value = (type != null) ? (String)field.get(null) : settingValue.defaultValue();
                    output.printf("%s = %s\n",name,StringUtils.escape(value));
                  }
                  else if (type.isEnum())
                  {
                    Enum value = (Enum)field.get(null);
                    output.printf("%s = %s\n",name,value.toString());
                  }
                  else if (type == EnumSet.class)
                  {
                    EnumSet enumSet = (EnumSet)field.get(null);
                    output.printf("%s = %s\n",name,StringUtils.join(enumSet,","));
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
            else if (annotation instanceof SettingComment)
            {
              SettingComment settingComment = (SettingComment)annotation;

              for (String line : settingComment.text())
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

      // save last modified time
      lastModified = file.lastModified();
    }
    catch (IOException exception)
    {
      // ignored
    }
    finally
    {
      if (output != null) output.close();
    }
  }

  /** save program settings
   * @param fileName settings file name
   */
  public static void save(String fileName)
  {
    save(new File(fileName));
  }

  /** save default program settings (only if not external modified)
   */
  public static void save()
  {
    File file = new File(ONZEN_CONFIG_FILE_NAME);

    if ((lastModified == 0L) || (file.lastModified() <= lastModified))
    {
      save(file);
    }
  }

  /** check if program settings file is modified
   * @return true iff modified
   */
  public static boolean isFileModified()
  {
    return (lastModified != 0L) && (new File(ONZEN_CONFIG_FILE_NAME).lastModified() > lastModified);
  }

  //-----------------------------------------------------------------------

  /** get all setting classes
   * @return classes array
   */
  protected static Class[] getSettingClasses()
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

  /** unique add element to long array
   * @param array array
   * @param n element
   * @return extended array or array
   */
  private static boolean[] addArrayUniq(boolean[] array, boolean n)
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

  /** unique add element to enum array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static Enum[] addArrayUniq(Enum[] array, Enum n)
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

  /** unique add element to enum set array
   * @param array array
   * @param string element
   * @return extended array or array
   */
  private static EnumSet[] addArrayUniq(EnumSet[] array, EnumSet n)
  {
    int z = 0;
    while ((z < array.length) && (array[z].equals(n)))
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

  /** unique add element to object array
   * @param array array
   * @param object element
   * @param settingAdapter setting adapter (use equals() function)
   * @return extended array or array
   */
  private static Object[] addArrayUniq(Object[] array, Object object, SettingValueAdapter settingValueAdapter)
  {
    int z = 0;
    while ((z < array.length) && !settingValueAdapter.equals(array[z],object))
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
