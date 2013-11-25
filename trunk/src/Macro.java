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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/****************************** Classes ********************************/

/** external command
 */
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
  private String                   start,end;
  private Pattern                  patternVariable;
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
    this.start      = start;
    this.end        = end;
    patternVariable = Pattern.compile("(.*?)"+start+"\\s*(\\w*)\\s*(.*?)"+end+"(.*)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);
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

  /** check if parameter <start>name<end> exists
   * @param name name of parameter
   * @return true if parameter <start>name<end> exists, false otherwise
   */
  public boolean contains(String name)
  {
    for (String parameter : parameters)
    {
      Matcher matcher = patternVariable.matcher(parameter);
      if (matcher.matches() && matcher.group(2).equals(name))
      {
        return true;
      }
    }
    return false;
  }

  /** add parameter
   * @param name parameter to add as <start>name<end>
   */
  public void add(String name)
  {
    String[] newParameters = new String[parameters.length+1];
    System.arraycopy(parameters,0,newParameters,0,parameters.length);
    newParameters[parameters.length] = start+name+end;
    parameters = newParameters;
  }

  /** add expand variable
   * @param name variable name
   * @param value variable value
   * @param quoteChar quote char
   * @param separator string or null
   */
  public void expand(String name, Object value, Character quoteChar, String separator)
  {
    variableSet.put(name,new Object[]{value,quoteChar,separator});
  }

  /** add expand variable
   * @param name variable name
   * @param value variable value
   * @param separator string or null
   */
  public void expand(String name, Object value, String separator)
  {
    expand(name,value,null,separator);
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
    ArrayList<String> valueList = new ArrayList<String>();

    for (int z = 0; z < parameters.length; z++)
    {
      String template = parameters[z];
      while (!template.isEmpty())
      {
        Matcher matcher = patternVariable.matcher(template);
        if (matcher.matches())
        {
          // get prefix, name, format
          String prefix  = matcher.group(1);
          String name    = matcher.group(2);
          String format  = matcher.group(3);

          // get rest of template
          template = matcher.group(4);

//Dprintf.dprintf("name=%s -- format=#%s#",name,format);

          Object[] variable = variableSet.get(name);
          if (variable != null)
          {
            Object    object    = (Object)variable[0];
            Character quoteChar = (Character)variable[1];
            String    separator = (String)variable[2];
//Dprintf.dprintf("object=%s -- %s",object,separator);

            if      (object instanceof Object[])
            {
              // expand array
              Object[] array = (Object[])object;
              for (int i = 0; i < array.length; i++)
              {
                object = array[i];

                //                if ((separator != null) && (buffer.length() > 0)) buffer.append(separator);
                StringBuilder value = new StringBuilder(prefix);

                String s = (object != null) ? object.toString() : "";
                if (quoteChar != null) s = StringUtils.escape(s,quoteChar);

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
                valueList.add(value.toString());
              }
            }
            else if (object instanceof Collection)
            {
              // expand collection
              Iterator iterator = ((Collection)object).iterator();
              while (iterator.hasNext())
              {
                object = iterator.next();

//                if ((separator != null) && (buffer.length() > 0)) buffer.append(separator);
                StringBuilder value = new StringBuilder(prefix);

                String s = (object != null) ? object.toString() : "";
                if (quoteChar != null) s = StringUtils.escape(s,quoteChar);

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
                valueList.add(value.toString());
              }
            }
            else if (object instanceof Hidden)
            {
              StringBuilder value = new StringBuilder(prefix);

              // expand hidden object
              String s = (object != null) ? object.toString() : "";
              if (quoteChar != null) s = StringUtils.escape(s,quoteChar);

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
              valueList.add(value.toString());
            }
            else
            {
              StringBuilder value = new StringBuilder(prefix);

              // expand object
              String s = (object != null) ? object.toString() : "";
              if (quoteChar != null) s = StringUtils.escape(s,quoteChar);

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
              valueList.add(value.toString());
            }
          }
          else
          {
            // unknown variable
//            value.append("???");
            valueList.add("???");
          }
        }
        else
        {
          valueList.add(template);
          template = "";
        }
      }
    }
for (String s : valueList) Dprintf.dprintf("s=%s",s);

    return valueList.toArray(new String[valueList.size()]);
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
