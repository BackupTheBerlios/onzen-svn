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
  private String                   string;
  private HashMap<String,Object[]> variableSet;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create macro
   * @param string string to expand
   */
  Macro(String string)
  {
    this.string = string;
    variableSet = new HashMap<String,Object[]>();
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

  /** expand macro
   * @return expanded macro string
   */
  public String value()
  {
    final Pattern PATTERN_VARIABLE = Pattern.compile("(.*?)\\$\\{\\s*(\\w+)\\s*(.*?)\\}(.*)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE|Pattern.DOTALL);

    StringBuilder buffer = new StringBuilder();
    String        value  = string;
    while (!value.isEmpty())
    {
//Dprintf.dprintf("value=%s",value);
      Matcher matcher = PATTERN_VARIABLE.matcher(value);
      if (matcher.matches())
      {
        buffer.append(matcher.group(1));
        value = matcher.group(4);

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
            Object[] array = (Object[])object;
            for (int z = 0; z < array.length; z++)
            {
              if ((separator != null) && (buffer.length() > 0)) buffer.append(separator);
              String s = array[z].toString();
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
          }
          else if (object instanceof Collection)
          {
            // expand collection
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
          }
          else
          {
            // expand object
            String s = object.toString();
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
        }
        else
        {
          // unknown variable
          buffer.append("???");
        }
      }
      else
      {
        buffer.append(value);
        value = "";
      }
    }

    return buffer.toString();
  }
}

/* end of file */
