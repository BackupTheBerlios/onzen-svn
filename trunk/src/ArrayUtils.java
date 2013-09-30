/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/StringUtils.java,v $
* $Revision: 1420 $
* $Author: trupp $
* Contents: Array utility functions
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.lang.reflect.Array;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/****************************** Classes ********************************/

/** array element comparator
 */
interface ArrayComparator<T>
{
  /** compare array element with data
   * @param data0 array element
   * @param data1 data
   * @return true iff equals
   */
  public boolean equals(T data0, Object data1);
}

interface ArrayRunnable<T0,T1>
{
  public boolean run(T0 data0, T1 data1);
}

/** array utility functions
 */
public class ArrayUtils
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** check if value is inside array
   * @param array array
   * @param value value
   * @param comparator comparator
   * @return true iff in array
   */
  public static <T> boolean contains(T[] array, Object value, ArrayComparator arrayComparator)
  {
    for (T arrayValue : array)
    {
      if (arrayComparator.equals(arrayValue,value)) return true;
    }

    return false;
  }

  /** check if value is inside array
   * @param array array
   * @param value value
   * @return true iff in array
   */
  public static <T> boolean contains(T[] array, T value)
  {
    for (T arrayValue : array)
    {
      if (arrayValue.equals(value)) return true;
    }

    return false;
  }

  /** insert value into array if not already in array
   * @param array array
   * @param value value
   * @param index index of insert
   * @param maxLength max. length of new array
   * @return new array
   */
  public static <T> T[] insertUnique(T[] array, T value, int index, int maxLength)
  {
    if ((index < 0) || (index >= maxLength))
    {
      throw new IllegalArgumentException();
    }

    if (!contains(array,value))
    {
      if (array.length < maxLength)
      {
        Class<?> arrayType = array.getClass().getComponentType();
        T[] newArray = (T[])Array.newInstance(arrayType,array.length+1);
        if (index > 0) System.arraycopy(array,0,newArray,0,index);
        newArray[index] = value;
        System.arraycopy(array,index,newArray,index+1,array.length-index);
        array = newArray;
      }
      else
      {
      }
    }

    return array;
  }

  /** add value to array if not already in array
   * @param array array
   * @param value value
   * @param maxLength max. length of new array
   * @return new array
   */
  public static <T> T[] addUnique(T[] array, T value, int maxLength)
  {
    return insertUnique(array,value,array.length,maxLength);
  }

  /** remove value from array
   * @param array array
   * @param index index
   * @return new array
   */
  public static <T> T[] remove(T[] array, int index)
  {
    Class<?> arrayType = array.getClass().getComponentType();
    T[] newArray = (T[])Array.newInstance(arrayType,array.length-1);
    System.arraycopy(array,0,newArray,0,index);
    System.arraycopy(array,index+1,newArray,0,array.length-1-index);

    return newArray;
  }

  /** execute for each array element
   * @param array array
   * @param value value
   * @param arrayComparator array comparator
   * @param arrayRunnable runnable
   */
  public static <T> void forEach(T[] array, Object value, ArrayComparator arrayComparator, ArrayRunnable arrayRunnable)
  {
    for (T arrayValue : array)
    {
      if (arrayComparator.equals(arrayValue,value))
      {
        arrayRunnable.run(arrayValue,value);
      }
    }
  }

  /** execute for each array element
   * @param array array
   * @param value value
   * @param arrayRunnable runnable
   */
  public static <T> void forEach(T[] array, Object value, ArrayRunnable arrayRunnable)
  {
    for (T arrayValue : array)
    {
      arrayRunnable.run(arrayValue,value);
    }
  }
}

/* end of file */
