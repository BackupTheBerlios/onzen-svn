/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandMailPatch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
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
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private String[]                 strings;
  private HashMap<String,Object[]> variableSet;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create macro
   * @param string string to expand
   */
  Macro(String[] strings)
  {
    this.strings = strings;
    variableSet  = new HashMap<String,Object[]>();
  }

  /** create macro
   * @param string string to expand
   */
  Macro(String string)
  {
    this(new String[]{string});
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

  /** expand macro value
   * @return expanded macro values array
   */
  public String[] getValueArray()
  {
    final Pattern PATTERN_VARIABLE = Pattern.compile("(.*?)\\$\\{\\s*(\\w+)\\s*(.*?)\\}(.*)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);

    String[] values = new String[strings.length];

    for (int z = 0; z < strings.length; z++)
    {
      StringBuilder value    = new StringBuilder();
      String        template = strings[z];
      while (!template.isEmpty())
      {
//Dprintf.dprintf("value=%s",value);
        Matcher matcher = PATTERN_VARIABLE.matcher(template);
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
                String s = array[i].toString();
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
                String s = iterator.next().toString();
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
            else
            {
              // expand object
              String s = object.toString();
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

    return (values.length > 0)?values[0]:"";
  }
}

/* end of file */
