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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.LinkedList;

/****************************** Classes ********************************/

/** file utility functions
 */
public class FileUtils
{
  /** copy file
   * @param fromFile from file
   * @param toFile to file
   */
  public static void copyFile(File fromFile, File toFile)
    throws IOException
  {
    File fromDirectory = fromFile.getParentFile();

    LinkedList<File> fileList = new LinkedList<File>();
    fileList.add(fromFile);

    while (!fileList.isEmpty())
    {
      fromFile = fileList.removeFirst();
      if (fromFile.isDirectory())
      {
        // add sub-files
        File[] subFiles = fromFile.listFiles();
        if (subFiles != null)
        {
          for (File subFile : subFiles)
          {
            fileList.add(subFile);
          }
        }
      }
      else
      {
        // copy file data
        File newDirectory = new File(toFile,fromFile.getName()).getParentFile();
        if (!newDirectory.exists())
        {
          newDirectory.mkdirs();
        }

        FileInputStream  input  = new FileInputStream(fromFile);
        FileOutputStream output = new FileOutputStream(new File(toFile,fromFile.getName()));
        byte[]           buffer = new byte[64*1024];

        int n;
        while ((n = input.read(buffer)) > 0)
        {
          output.write(buffer,0,n);
        }

        output.close();
        input.close();

        // set file permissions
        toFile.setExecutable(fromFile.canExecute());
        toFile.setWritable(fromFile.canWrite());
      }
    }
  }

  /** copy file
   * @param fromFileName from file name
   * @param toFileName to file name
   */
  public static void copyFile(String fromFileName, String toFileName)
    throws IOException
  {
    copyFile(new File(fromFileName),new File(toFileName));
  }

  /** copy directory tree
   * @param fromDirectory from directory
   * @param toDirectory to directory
   */
  public static void copyDirectoryTree(File fromDirectory, File toDirectory)
    throws IOException
  {
    File[] files = fromDirectory.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          copyDirectoryTree(new File(fromDirectory,file.getName()),toDirectory);
        }
        else
        {
          copyFile(file,new File(toDirectory,file.getName()));
        }
      }
    }
  }

  /** copy directory tree
   * @param fromDirectory from directory
   * @param toDirectory to directory
   */
  public static void copyDirectoryTree(String fromPathName, String toPathName)
    throws IOException
  {
    copyDirectoryTree(new File(fromPathName),new File(toPathName));
  }

  /** move file
   * @param fromFile - from file
   * @param toFile   - to file
   */
  public static void moveFile(File fromFile, File toFile)
    throws IOException
  {
    copyFile(fromFile,toFile);
    deleteFile(fromFile);
  }

   /** move file
   * @param fromFileName from file
   * @param toFileName to file
   */
  public static void moveFile(String fromFileName, String toFileName)
    throws IOException
  {
    moveFile(new File(fromFileName),new File(toFileName));
  }

  /** move directory tree
   * @param fromDirectory from directory
   * @param toDirectory to directory
   */
  public static void moveDirectoryTree(File fromDirectory, File toDirectory)
    throws IOException
  {
    copyDirectoryTree(fromDirectory,toDirectory);
    deleteDirectoryTree(fromDirectory);
  }

  /** move directory tree
   * @param fromPath from directory
   * @param toPath to directory
   */
  public static void moveDirectoryTree(String fromPathName, String toPathName)
    throws IOException
  {
    moveDirectoryTree(new File(fromPathName),new File(toPathName));
  }

  /** delete file or direcotry
   * @param file file/directory to delete
   */
  public static void deleteFile(File file)
    throws IOException
  {
//Dprintf.dprintf("deleteFile=%s",file);
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

  /** delete file or direcotry
   * @param fileName file/directory to delete
   */
  public static void deleteFile(String fileName)
    throws IOException
  {
    deleteFile(new File(fileName));
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

  /** create temporary directory
   * @param prefix prefix of name
   * @param suffix suffix of name
   * @param directory directory to create new temporary directory
   */
  public static File createTempDirectory(String prefix, String suffix, File directory)
  {
    File tmpDirectory = null;

    if (suffix == null) suffix = ".tmp";

    long    n           = System.currentTimeMillis();
    boolean createdFlag = false;
    while (!createdFlag)
    {
      tmpDirectory = new File(directory,String.format("%s%08x%s",prefix,n,suffix));
      createdFlag = tmpDirectory.mkdir();
      n++;
    }
Dprintf.dprintf("createTempDirectory=%s",tmpDirectory);

    return tmpDirectory;
  }

  /** create temporary directory
   * @param prefix prefix of name
   * @param suffix suffix of name
   */
  public static File createTempDirectory(String prefix, String suffix)
  {
    return createTempDirectory(prefix,suffix,new File(System.getProperty("java.io.tmpdir")));
  }

  /** escape file name
   * @param name name
   * @return escaped name
   */
  public static String escape(String name)
  {
    return (name.indexOf(' ') >= 0) ? StringUtils.escape(name,true,'"') : name;
  }

  /** unescape file name
   * @param name escaped name
   * @return name
   */
  public static String unescape(String name)
  {
    return StringUtils.unescape(name,true,"\"");
  }
}

/* end of file */
