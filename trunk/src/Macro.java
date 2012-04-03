/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: macro functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/****************************** Classes ********************************/

public class Macro
{
  /** hidden string class
   */
  class Hidden
  {
    private String string;

    /** create hidden string
     * @param string string
     */
    Hidden(String string)
    {
      this.string = string;
    }

    /** convert to string
     * @return string
     */
    public String toString()
    {
      return string;
    }
  };

  // --------------------------- constants --------------------------------
  public final static String   PATTERN_START_DOLLAR = "\\$\\{";
  public final static String   PATTERN_END_DOLLAR   = "\\}";
  public final static String[] PATTERN_DOLLAR       = new String[]{PATTERN_START_DOLLAR,PATTERN_END_DOLLAR};

  public final static String   PATTERN_START_PERCENTAGE = "%";
  public final static String   PATTERN_END_PERCENTAGE   = "%";
  public final static String[] PATTERN_PERCENTAGE       = new String[]{PATTERN_START_PERCENTAGE,PATTERN_END_PERCENTAGE};

  // --------------------------- variables --------------------------------
  private String[]                 parameters;
  private Pattern                  patterVariable;
  private HashMap<String,Object[]> variableSet;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create macro
   * @param parameters parameters to expand
   * @param start, end variable start/end regular expression pattern
   */
  Macro(String[] parameters, String start, String end)
  {
    this.parameters = parameters;
    patterVariable  = Pattern.compile("(.*?)"+start+"\\s*(\\w*)\\s*(.*?)"+end+"(.*)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);
    variableSet     = new HashMap<String,Object[]>();
  }

  /** create macro
   * @param parameters parameters to expand
   * @param startEnd variable start/end regular expression pattern
   */
  Macro(String[] parameters, String[] startEnd)
  {
    this(parameters,startEnd[0],startEnd[1]);
  }

  /** create macro
   * @param parameters parameters to expand
   */
  Macro(String[] parameters)
  {
    this(parameters,PATTERN_START_DOLLAR,PATTERN_END_DOLLAR);
  }

  /** create macro
   * @param string string to expand
   * @param start, end variable start/end regular expression pattern
   */
  Macro(String string, String start, String end)
  {
    this(new String[]{string},start,end);
  }

  /** create macro
   * @param string string to expand
   * @param startEnd variable start/end regular expression pattern
   */
  Macro(String string, String[] startEnd)
  {
    this(string,startEnd[0],startEnd[1]);
  }

  /** create macro
   * @param string string to expand
   */
  Macro(String string)
  {
    this(string,PATTERN_DOLLAR);
  }

  /** add expand variable
   * @param name variable name
   * @param value variable value
   * @param seperator string or null
   */
  public void expand(String name, Object value, String separator)
  {
    variableSet.put(name,new Object[]{value,separator});
  }

  /** add expand variable
   * @param name variable name
   * @param value variable value
   */
  public void expand(String name, Object value)
  {
    expand(name,value,null);
  }

  /** create hidden macro value
   * @param string macro value
   * @return hidden macro value
   */
  public Hidden hidden(String string)
  {
    return new Hidden(string);
  }

  /** expand macro value
   * @return expanded macro values array
   */
  public String[] getValueArray()
  {
    String[] values = new String[parameters.length];

    for (int z = 0; z < parameters.length; z++)
    {
      StringBuilder value    = new StringBuilder();
      String        template = parameters[z];
      while (!template.isEmpty())
      {
//Dprintf.dprintf("value=%s",value);
        Matcher matcher = patterVariable.matcher(template);
        if (matcher.matches())
        {
          value.append(matcher.group(1));
          template = matcher.group(4);

          String name    = matcher.group(2);
          String format  = matcher.group(3);
//Dprintf.dprintf("name=%s -- format=#%s#",name,format);

          Object[] variable = variableSet.get(name);
          if (variable != null)
          {
            Object object    = (Object)variable[0];
            String separator = (String)variable[1];
//Dprintf.dprintf("object=%s -- %s",object,separator);

            if      (object instanceof Object[])
            {
              // expand array
              StringBuilder buffer = new StringBuilder();
              Object[] array = (Object[])object;
              for (int i = 0; i < array.length; i++)
              {
                if ((separator != null) && (buffer.length() > 0)) buffer.append(separator);
                object = array[i];
                String s = (object != null) ? object.toString() : "";
                if (!format.isEmpty())
                {
                  try
                  {
                    buffer.append(String.format(format,s));
                  }
                  catch (Exception exception)
                  {
                    buffer.append(s);
                  }
                }
                else
                {
                  buffer.append(s);
                }
              }
              value.append(buffer);
            }
            else if (object instanceof Collection)
            {
              // expand collection
              StringBuilder buffer = new StringBuilder();
              Iterator iterator = ((Collection)object).iterator();
              while (iterator.hasNext())
              {
                if ((separator != null) && (buffer.length() > 0)) buffer.append(separator);
                object = iterator.next();
                String s = (object != null) ? object.toString() : "";
                if (!format.isEmpty())
                {
                  try
                  {
                    buffer.append(String.format(format,s));
                  }
                  catch (Exception exception)
                  {
                    buffer.append(s);
                  }
                }
                else
                {
                  buffer.append(s);
                }
              }
              value.append(buffer);
            }
            else if (object instanceof Hidden)
            {
              // expand object
              String s = (object != null) ? object.toString() : "";
              if (!format.isEmpty())
              {
                try
                {
                  value.append(String.format(format,s));
                }
                catch (Exception exception)
                {
                  value.append(s);
                }
              }
              else
              {
                value.append(s);
              }
            }
            else
            {
              // expand object
              String s = (object != null) ? object.toString() : "";
              if (!format.isEmpty())
              {
                try
                {
                  value.append(String.format(format,s));
                }
                catch (Exception exception)
                {
                  value.append(s);
                }
              }
              else
              {
                value.append(s);
              }
            }
          }
          else
          {
            // unknown variable
            value.append("???");
          }
        }
        else
        {
          value.append(template);
          template = "";
        }
      }
      values[z] = value.toString();
    }

    return values;
  }

  /** expand macro value
   * @return expanded macro value
   */
  public String getValue()
  {
    String[] values = getValueArray();

    return (values.length > 0) ? values[0] : "";
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Macro {"+StringUtils.join(parameters)+"}";
  }

}

/* end of file */
