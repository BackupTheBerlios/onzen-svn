/***********************************************************************\
*
* $Revision: 1146 $
* $Date: 2012-05-15 08:49:50 +0200 (Di, 15 Mai 2012) $
* $Author: trupp $
* Contents: file utils
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.PrintWriter;

/****************************** Classes ********************************/

public class FileUtils
{
  /** copy file
   * @param fromFile - from file
   * @param toFile   - to file
   */
  public static void copyFile(File fromFile, File toFile)
    throws IOException
  {
    FileInputStream  input  = new FileInputStream(fromFile);
    FileOutputStream output = new FileOutputStream(toFile);
    byte[]           buffer = new byte[64*1024];

    int n;
    while ((n = input.read(buffer)) > 0)
    {
      output.write(buffer,0,n);
    }

    toFile.setExecutable(fromFile.canExecute());
    toFile.setWritable(fromFile.canWrite());

    output.close();
    input.close();
  }

  /** delete file or direcotry
   * @param file file/directory to delete
   */
  public static void deleteFile(File file)
    throws IOException
  {
    if (!file.delete())
    {
      if (file.canWrite())
      {
        throw new IOException("delete "+file.getName()+" fail");
      }
      else
      {
        throw new IOException("delete "+file.getName()+" fail: write protected");
      }
    }
  }

  /** delete directory tree
   * @param directory directory to delete
   */
  public static void deleteDirectoryTree(File directory)
    throws IOException
  {
    File[] files = directory.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          deleteDirectoryTree(file);
        }
        else
        {
          deleteFile(file);
        }
      }
    }

    deleteFile(directory);
  }

  /** delete directory tree
   * @param pathName path name
   */
  public static void deleteDirectoryTree(String pathName)
    throws IOException
  {
    deleteDirectoryTree(new File(pathName));
  }
}

/* end of file */
