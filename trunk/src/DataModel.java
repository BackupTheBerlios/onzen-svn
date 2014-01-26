import java.util.Properties;
import java.util.Enumeration;

class DataModel
{
  public static void main(String[] args)
  {
    /*
    Properties  properties = java.lang.System.getProperties();
    Enumeration names = properties.propertyNames();
    while (names.hasMoreElements())
    {
      String name = (String)names.nextElement();
      String value = properties.getProperty(name);
      System.out.println(name + " := " + value);
    }
    */

    int dataModel = 32;

    String value;

    value = System.getProperty("sun.arch.data.model");
    if (value != null)
    {
      dataModel = Integer.parseInt(value);
    }

    value = System.getProperty("os.arch");
    if (value != null)
    {
      dataModel = value.contains("64") ? 64 : 32;
    }

    System.out.println(dataModel);
    System.exit(dataModel);
  }
}
