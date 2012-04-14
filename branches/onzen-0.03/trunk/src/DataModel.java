class DataModel
{
  public static void main(String[] args)
  {
    int dataModel = Integer.parseInt(System.getProperty("sun.arch.data.model"));
    System.out.println(dataModel);
    System.exit(dataModel);
  }
}
