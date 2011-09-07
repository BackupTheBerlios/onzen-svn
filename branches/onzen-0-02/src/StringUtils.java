/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/StringUtils.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: String utility functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;

/****************************** Classes ********************************/

/** string utility functions
 */
public class StringUtils
{
  // --------------------------- constants --------------------------------
  /** quoting characters for string values
   */
  public final static String QUOTE_CHARS = "'\"";

  /** white spaces
   */
  public final static String WHITE_SPACES = " \t\f\r\n";

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** escape ' and \ in string, enclose in "
   * @param string string to escape
   * @param enclosingQuotes true to add enclosing quotes "
   * @param quoteChar quote character
   * @return escaped string
   */
  public static String escape(String string, boolean enclosingQuotes, char quoteChar)
  {
    StringBuilder buffer = new StringBuilder();

    if (enclosingQuotes) buffer.append(quoteChar);
    for (int index = 0; index < string.length(); index++)
    {
      char ch = string.charAt(index);

      if      (ch == quoteChar)
      {
        buffer.append("\\"+quoteChar);
      }
      else if (ch == '\\')
      {
        buffer.append("\\\\");
      }
      else
      {
        buffer.append(ch);
      }
    }
    if (enclosingQuotes) buffer.append(quoteChar);

    return buffer.toString();
  }

  /** escape ' and \ in string, enclose in "
   * @param string string to escape
   * @param enclosingQuotes true to add enclosing quotes "
   * @return escaped string
   */
  public static String escape(String string, boolean enclosingQuotes)
  {
    return escape(string,enclosingQuotes,'"');
  }

  /** escape ' and \ in string, enclose in "
   * @param string string to escape
   * @param quoteChar quote character
   * @return escaped string
   */
  public static String escape(String string, char quoteChar)
  {
    return escape(string,true,quoteChar);
  }

  /** escape ' and \ in string, enclose in "
   * @param string string to escape
   * @return escaped string
   */
  public static String escape(String string)
  {
    return escape(string,true);
  }

  /** remove enclosing ' or "
   * @param string string to unescape
   * @return unescaped string
   */
  public static String unescape(String string)
  {
    if      (string.startsWith("\"") && string.endsWith("\""))
    {
      return string.substring(1,string.length()-1);
    }
    else if (string.startsWith("'") && string.endsWith("'"))
    {
      return string.substring(1,string.length()-1);
    }
    else
    {
      return string;
    }
  }

  /** join string array
   * @param objects objects to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @param quoteChar quote char
   * @return string
   */
  public static String join(Object[] objects, String joinString, char quoteChar)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (objects != null)
    {
      for (Object object : objects)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        string = object.toString();
        buffer.append((quoteChar != '\0') ? escape(string,true,quoteChar) : string);
      }
    }

    return buffer.toString();
  }

  /** join string array
   * @param objects objects to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @param quote true iff escape strings
   * @return string
   */
  public static String join(Object[] objects, String joinString, boolean quote)
  {
    return join(objects,joinString,(quote) ? '"' : '\0');
  }

  /** join string array
   * @param objects objects to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(Object[] objects, String joinString)
  {
    return join(objects,joinString,'\0');
  }

  /** join string array
   * @param collection collection to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @param quoteChar quote char
   * @return string
   */
  public static String join(Collection collection, String joinString, char quoteChar)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (collection != null)
    {
      for (Object object : collection)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        string = object.toString();
        buffer.append((quoteChar != '\0') ? escape(string,true,quoteChar) : string);
      }
    }

    return buffer.toString();
  }

  /** join string array
   * @param collection collection to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @param quote true iff escape strings
   * @return string
   */
  public static String join(Collection collection, String joinString, boolean quote)
  {
    return join(collection,joinString,(quote) ? '"' : '\0');
  }

  /** join string array
   * @param collection collection to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(Collection collection, String joinString)
  {
    return join(collection,joinString,'\0');
  }

  /** join string array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(String[] strings)
  {
    return join(strings," ");
  }

  /** join boolean array
   * @param array array to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(boolean[] array, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (array != null)
    {
      for (boolean n : array)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append(Boolean.toString(n));
      }
    }

    return buffer.toString();
  }

  /** join boolean array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(boolean[] array)
  {
    return join(array," ");
  }

  /** join integer array
   * @param array array to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(int[] array, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (array != null)
    {
      for (int n : array)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append(Integer.toString(n));
      }
    }

    return buffer.toString();
  }

  /** join integer array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(int[] array)
  {
    return join(array," ");
  }

  /** join long array
   * @param array array to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(long[] array, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (array != null)
    {
      for (long n : array)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append(Long.toString(n));
      }
    }

    return buffer.toString();
  }

  /** join long array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(long[] array)
  {
    return join(array," ");
  }

  /** join float array
   * @param array array to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(float[] array, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (array != null)
    {
      for (float n : array)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append(Float.toString(n));
      }
    }

    return buffer.toString();
  }

  /** join float array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(float[] array)
  {
    return join(array," ");
  }

  /** join double array
   * @param array array to join (convert to string with toString())
   * @param joinString string used to join two strings
   * @return string
   */
  public static String join(double[] array, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (array != null)
    {
      for (double n : array)
      {
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append(Double.toString(n));
      }
    }

    return buffer.toString();
  }

  /** join double array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(double[] array)
  {
    return join(array," ");
  }

  /** join double array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(EnumSet enumSet, String joinString, boolean ordinal)
  {
    StringBuilder buffer = new StringBuilder();
    String        string;
    if (enumSet != null)
    {
      Iterator iterator = enumSet.iterator();
      while (iterator.hasNext())
      {
        Enum value = (Enum)iterator.next();
        if (buffer.length() > 0) buffer.append(joinString);
        buffer.append((ordinal) ? Integer.toString(value.ordinal()) : value.toString());
      }
    }

    return buffer.toString();
  }

  /** join double array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(EnumSet enumSet, String joinString)
  {
    return join(enumSet,joinString,false);
  }

  /** join double array with space
   * @param strings strings to join
   * @return string
   */
  public static String join(EnumSet enumSet)
  {
    return join(enumSet,",");
  }

  /** split string
   * @param string string to split
   * @param splitChars characters used for splitting
   * @param spaceChars spaces characters to skip (can be null)
   * @param quoteChars quote characters (can be null)
   * @param emptyFlag true to return empty parts, false to skip empty parts
   * @return string array
   */
  public static String[] split(String string, String splitChars, String spaceChars, String quoteChars, boolean emptyFlag)
  {
    ArrayList<String> stringList = new ArrayList<String>();
//Dprintf.dprintf("string=%s splitChars=%s spaceChars=%s quoteChars=%s em=%s",string,splitChars,spaceChars,quoteChars,emptyFlag);

    char[]        chars  = string.toCharArray();
    int           i      = 0;
    int           n      = chars.length;
    StringBuilder buffer = new StringBuilder();
    while (i < n)
    {
      if (spaceChars != null)
      {
        // skip spaces
        while ((i < n) && (spaceChars.indexOf(chars[i]) >= 0))
        {
          i++;
        }
      }

      // get next word, respect quotes
      buffer.setLength(0);
      while ((i < n) && splitChars.indexOf(chars[i]) == -1)
      {
        if      (chars[i] == '\\')
        {
          // escaped character
          buffer.append('\\');
          if (i+1 < n) buffer.append(chars[i+1]);
          i += 2;
        }
        else if ((quoteChars != null) && (quoteChars.indexOf(chars[i]) >= 0))
        {
          // quote
          char quoteChar = chars[i];
          i += 1;
          while ((i < n) && (chars[i] != quoteChar))
          {
            if      (chars[i] == '\\')
            {
              // escaped character
              buffer.append('\\');
              if (i+1 < n) buffer.append(chars[i+1]);
              i += 2;
            }
            else
            {
              // character
              buffer.append(chars[i]);
              i += 1;
            }
          }
          i += 1;
        }
        else
        {
          // character
          buffer.append(chars[i]);
          i += 1;
        }
      }
      i += 1;

      // add to list
      if (emptyFlag || (buffer.length() > 0))
      {
        stringList.add(buffer.toString());
      }
    }

    return stringList.toArray(new String[0]);
  }

  /** split string
   * @param string string to split
   * @param splitChar character used for splitting
   * @param spaceChars spaces characters to skip (can be null)
   * @param quoteChars quote characters (can be null)
   * @param emptyFlag true to return empty parts, false to skip empty parts
   * @return string array
   */
  public static String[] split(String string, char splitChar, String spaceChars, String quoteChars, boolean emptyFlag)
  {
    return split(string,new String(new char[]{splitChar}),spaceChars,quoteChars,emptyFlag);
  }

  /** split string, discard white spaces between strings
   * @param string string to split
   * @param splitChars characters used for splitting
   * @param quoteChars quote characters
   * @param emptyFlag TRUE to return empty parts, FALSE to skip empty parts
   * @return string array
   */
  public static String[] split(String string, String splitChars, String quoteChars, boolean emptyFlag)
  {
    return split(string,splitChars,WHITE_SPACES,quoteChars,emptyFlag);
  }

  /** split string, discard white spaces between strings
   * @param string string to split
   * @param splitChar characters used for splitting
   * @param quoteChars quote characters
   * @param emptyFlag TRUE to return empty parts, FALSE to skip empty parts
   * @return string array
   */
  public static String[] split(String string, char splitChar, String quoteChars, boolean emptyFlag)
  {
    return split(string,splitChar,WHITE_SPACES,quoteChars,emptyFlag);
  }

  /** split string, discard white spaces between strings
   * @param string string to split
   * @param splitChars characters used for splitting
   * @param quoteChars quote characters
   * @return string array
   */
  public static String[] split(String string, String splitChars, String quoteChars)
  {
    return split(string,splitChars,WHITE_SPACES,quoteChars,true);
  }

  /** split string, discard white spaces between strings
   * @param string string to split
   * @param splitChar character used for splitting
   * @param quoteChars quote characters
   * @return string array
   */
  public static String[] split(String string, char splitChar, String quoteChars)
  {
    return split(string,splitChar,WHITE_SPACES,quoteChars,true);
  }

  /** split string (no quotes)
   * @param string string to split
   * @param splitChars characters used for splitting
   * @param emptyFlag TRUE to return empty parts, FALSE to skip empty parts
   * @return string array
   */
  public static String[] split(String string, String splitChars, boolean emptyFlag)
  {
    return split(string,splitChars,null,null,emptyFlag);
  }

  /** split string (no quotes)
   * @param string string to split
   * @param splitChar character used for splitting
   * @param emptyFlag TRUE to return empty parts, FALSE to skip empty parts
   * @return string array
   */
  public static String[] split(String string, char splitChar, boolean emptyFlag)
  {
    return split(string,splitChar,null,null,emptyFlag);
  }

  /** split string (no quotes)
   * @param string string to split
   * @param splitChars characters used for splitting
   * @return string array
   */
  public static String[] split(String string, String splitChars)
  {
    return split(string,splitChars,true);
  }

  /** split string (no quotes)
   * @param string string to split
   * @param splitChar character used for splitting
   * @return string array
   */
  public static String[] split(String string, char splitChar)
  {
    return split(string,splitChar,true);
  }

  /** replace string in stringn array
   * @param strings string array
   * @param from,to from/to string
   */
  public static void replace(String[] strings, String from, String to)
  {
    for (int z = 0; z < strings.length; z++)
    {
      strings[z] = strings[z].replace(from,to);
    }
  }

  /** create n-concationation of string
   * @param string string to repeat
   * @param number of concatations
   * @return string+...+string (count times)
   */
  public static String repeat(String string, int count)
  {
    StringBuilder buffer = new StringBuilder();

    for (int z = 0; z < count; z++)
    {
      buffer.append(string);
    }

    return buffer.toString();
  }

  /** create n-concationation of string
   * @param ch charater to repeat
   * @param number of concatations
   * @return string+...+string (count times)
   */
  public static String repeat(char ch, int count)
  {
    StringBuilder buffer = new StringBuilder();

    for (int z = 0; z < count; z++)
    {
      buffer.append(ch);
    }

    return buffer.toString();
  }

  /** convert glob-pattern into regex-pattern
   * @param string glob-pattern
   * @return regex-pattern
   */
  public static String globToRegex(String string)
  {
    StringBuilder buffer = new StringBuilder();

    int z = 0;
    while (z < string.length())
    {
      switch (string.charAt(z))
      {
        case '*':
          buffer.append(".*");
          z++;
          break;
        case '?':
          buffer.append('.');
          z++;
          break;
        case '.':
          buffer.append("\\.");
          z++;
          break;
        case '\\':
          buffer.append('\\');
          z++;
          if (z < string.length())
          {
            buffer.append(string.charAt(z));
            z++;
          }
          break;
        case '[':
        case ']':
        case '^':
        case '$':
        case '(':
        case ')':
        case '{':
        case '}':
        case '+':
        case '|':
          buffer.append('\\');
          buffer.append(string.charAt(z));
          z++;
          break;
        default:
          buffer.append(string.charAt(z));
          z++;
          break;
      }
    }

    return buffer.toString();
  }

  /** parse string with boolean value
   * @param string string
   * @return boolean value or false
   */
  public static boolean parseBoolean(String string)
  {
    final String TRUE_STRINGS[] =
    {
      "1",
      "true",
      "yes",
      "on",
    };

    for (String trueString : TRUE_STRINGS)
    {
      if (string.equalsIgnoreCase(trueString)) return true;
    }

    return false;
  }

  /** parse string with enum value
   * @param enumClass enum class
   * @param string string
   * @return enum value or null
   */
  public static Enum parseEnum(Class enumClass, String string)
  {
    int n;
    try
    {
      n = Integer.parseInt(string);
    }
    catch (NumberFormatException exception)
    {
      n = -1;
    }
    for (Enum enumConstant : (Enum[])enumClass.getEnumConstants())
    {
      if (   string.equalsIgnoreCase(enumConstant.toString())
          || (enumConstant.ordinal() == n)
         )
      {
        return enumConstant;
      }
    }

    return null;
  }

  /** parse string with enum set value
   * @param enumClass enum class
   * @param string string
   * @return enum set value or null
   */
  public static EnumSet parseEnumSet(Class enumClass, String string)
  {
    EnumSet enumSet = EnumSet.noneOf(enumClass);

    Enum[] enumConstants = (Enum[])enumClass.getEnumConstants();
    for (String value : split(string,",",false))
    {
      int n;
      try
      {
        n = Integer.parseInt(value);
      }
      catch (NumberFormatException exception)
      {
        n = -1;
      }
      for (Enum enumConstant : enumConstants)
      {
        if (   value.equalsIgnoreCase(enumConstant.toString())
            || (enumConstant.ordinal() == n)
           )
        {
          enumSet.add(enumConstant);
        }
      }
    }

    return enumSet;
  }
}

/* end of file */
