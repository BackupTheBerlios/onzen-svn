/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Settings.java,v $
* $Revision: 1.1 $
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
import static java.lang.annotation.ElementType.FIELD; 
import static java.lang.annotation.ElementType.TYPE; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.LinkedHashSet;

import java.util.regex.Pattern;

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

/** config value adapter String <-> RGB
 */
class ConfigValueAdapterRGB extends ConfigValueAdapter<String,RGB>
{
  public RGB toValue(String string) throws Exception
  {
    RGB rgb = null;

    StringTokenizer tokenizer = new StringTokenizer(string,",");
    rgb = new RGB(Integer.parseInt(tokenizer.nextToken()),
                  Integer.parseInt(tokenizer.nextToken()),
                  Integer.parseInt(tokenizer.nextToken())
                 );

    return rgb;
  }

  public String toString(RGB rgb) throws Exception
  {
    return String.format("%d,%d,%d",rgb.red,rgb.green,rgb.blue);
  }
}

/** editor
 */
class Editor
{
  public final String  mimeTypePattern;
  public final String  command;
  public final Pattern pattern;

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

/** settings
 */
public class Settings
{
  // --------------------------- constants --------------------------------
  public static final String ONZEN_DIRECTORY                = System.getProperty("user.home")+File.separator+".onzen2";

  public static final String DEFAULT_ONZEN_CONFIG_FILE_NAME = ONZEN_DIRECTORY+File.separator+"onzen.cfg";

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

  // --------------------------- variables --------------------------------

  @ConfigComment(text={"Onzen configuration",""})

  // program settings
  public static HostSystems      hostSystem                     = HostSystems.LINUX;
  @ConfigValue
  public static String           tmpDirectory                   = "/tmp";

  @ConfigValue
  public static String           dateFormat                     = "yyyy-MM-dd HH:mm:ss";

  @ConfigValue
  public static int              maxBackgroundTasks             = 8;

  @ConfigValue
  public static int              maxMessageHistory              = 50;

  @ConfigComment(text={"","UDP message broadcasting"})
  @ConfigValue
  public static String           messageBroadcastAddress        = "192.168.11.255";
  @ConfigValue
  public static int              messageBroadcastPort           = 9583;

  @ConfigComment(text={"","Geometry (<width>x<height>)"})
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryCommit                 = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryAdd                    = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryRemove                 = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryRevert                 = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryRename                 = new Point(500,300);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryDiff                   = new Point(800,600);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryRevisions              = new Point(800,400);
  @ConfigValue(type=ConfigValueAdapterSize.class)
  public static Point            geometryRevisionBox            = new Point(200, 70);

  @ConfigComment(text={"","Colors (RGB)"})
  @ConfigValue(type=ConfigValueAdapterRGB.class)
  public static RGB              colorDiffAdded                 = new RGB(0,255,0);
  @ConfigValue(type=ConfigValueAdapterRGB.class)
  public static RGB              colorDiffDeleted               = new RGB(255,0,0);
  @ConfigValue(type=ConfigValueAdapterRGB.class)
  public static RGB              colorDiffChanged               = new RGB(255,0,0);
  @ConfigValue(type=ConfigValueAdapterRGB.class)
  public static RGB              colorDiffChangedWhitespaces    = new RGB(255,0,0);

/*
  @ConfigComment(text={"Geometry (<width>x<height>)")
  static class geometry
  {
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            commit                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            add                    = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            remove                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            revert                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            rename                 = new Point(500,300);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            diff                   = new Point(800,600);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            revisions              = new Point(800,400);
    @ConfigValue(type=ConfigValueAdapterSize.class)
    public static Point            revisionBox            = new Point(200, 70);
  };

  @ConfigComment(text={"Colors (RGB)")
  static class color
  {
  }
*/

  @ConfigComment(text={"","Editors (<mime type>:<command>)"})
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
                        if      (type == int.class)
                        {
                          int value = Integer.parseInt(string);
                          if (type.isArray())
                          {
                            // update/extend array
                            field.set(null,addArrayUniq((int[])field.get(null),value));
                          }
                          else
                          {
                            // set value
                            field.setInt(null,value);
                          }
                        }
                        else if (type == long.class)
                        {
                          long value = Long.parseLong(string);
                          if (type.isArray())
                          {
                            // update/extend array
                            field.set(null,addArrayUniq((long[])field.get(null),value));
                          }
                          else
                          {
                            // set value
                            field.setLong(null,value);
                          }
                        }
                        else if (type == String.class)
                        {
                          if (type.isArray())
                          {
                            // update/extend array
                            field.set(null,addArrayUniq((String[])field.get(null),StringUtils.unescape(string)));
                          }
                          else
                          {
                            // set value
                            field.set(null,StringUtils.unescape(string));
                          }
                        }
                        else if (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                        {
                          ConfigValueAdapter configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                          Object value = configValueAdapter.toValue(string);
                          if (type.isArray())
                          {
                            // update/extend array
                            field.set(null,addArrayUniq((Object[])field.get(null),value,configValueAdapter));
                          }
                          else
                          {
                            // set value
                            field.set(null,value);
                          }
                        }
                        else
                        {
Dprintf.dprintf("field.getType()=%s",type);
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
    return load(DEFAULT_ONZEN_CONFIG_FILE_NAME);
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
//Dprintf.dprintf("annotation=%s",annotation.annotationType());        
              ConfigValue configValue = (ConfigValue)annotation;

              // get value and write to file
              String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();
              try
              {
                Class type = field.getType();
//Dprintf.dprintf("field.getType()=%s",type);
                if      (type == int.class)
                {
//Dprintf.dprintf("int %s %s",name,field.get(null));
                  if (type.isArray())
                  {
                    for (int value : (int[])field.get(null))
                    {
                      output.printf("%s = %d\n",name,value);
                    }
                  }
                  else
                  {
                    int value = field.getInt(null);
                    output.printf("%s = %d\n",name,value);
                  }
                }
                else if (type == long.class)
                {
//Dprintf.dprintf("long %s %s",name,field.get(null));
                  if (type.isArray())
                  {
                    for (long value : (long[])field.get(null))
                    {
                      output.printf("%s = %ld\n",name,value);
                    }
                  }
                  else
                  {
                    long value = field.getLong(null);
                    output.printf("%s = %ld\n",name,value);
                  }
                }
                else if (type == String.class)
                {
//Dprintf.dprintf("string %s %s",name,field.get(null));
                  if (type.isArray())
                  {
                    for (String value : (String[])field.get(null))
                    {
                      output.printf("%s = %s\n",name,StringUtils.escape(value));
                    }
                  }
                  else
                  {
                    String value = (type != null) ? (String)field.get(null) : configValue.defaultValue();
                    output.printf("%s = %s\n",name,StringUtils.escape(value));
                  }
                }
                else if (ConfigValueAdapter.class.isAssignableFrom(configValue.type()))
                {
                  ConfigValueAdapter configValueAdapter = (ConfigValueAdapter)configValue.type().newInstance();
                  if (type.isArray())
                  {
//Dprintf.dprintf("sa=%s",configValueAdapter.toString(field.get(null)));
                    for (Object object : (Object[])field.get(null))
                    {
                      String value = (String)configValueAdapter.toString(object);
                      output.printf("%s = %s\n",name,value);
                    }
                  }
                  else
                  {
                    String value = (String)configValueAdapter.toString(field.get(null));
                    output.printf("%s = %s\n",name,value);
                  }
                }
                else
                {
Dprintf.dprintf("field.getType()=%s",type);
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
    return save(DEFAULT_ONZEN_CONFIG_FILE_NAME);
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
