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

/** file copy confirmation class
 */
class FileCopyConfirmation
{
  /** confirm copy
   * @param name file name
   * @return true if file should be copied, false to skip file copy
   */
  public boolean confirm(String name)
  {
    return true;
  }

  /** confirm copy
   * @param file file
   * @return true if file should be copied, false to skip file copy
   */
  public boolean confirm(File file)
  {
    return confirm(file.getPath());
  }
}

/** file utility functions
 */
public class FileUtils
{
  /** copy file
   * @param fromFile from file
   * @param toFile to file
   * @param fileCopyConfirmation confirm file copy
   */
  public static void copyFile(final File fromFile, final File toFile, FileCopyConfirmation fileCopyConfirmation)
    throws IOException
  {
    File fromDirectory = fromFile.getParentFile();

    LinkedList<String> fileList = new LinkedList<String>();
    fileList.add(fromFile.getName());
    while (!fileList.isEmpty())
    {
      String fileName = fileList.removeFirst();

      File sourceFile = new File(fromDirectory,fileName);
      if ((fileCopyConfirmation == null) || fileCopyConfirmation.confirm(sourceFile))
      {
//Dprintf.dprintf("fileName=%s sourceFile=%s",fileName,sourceFile);
        if (sourceFile.isDirectory())
        {
          // add sub-files
          String[] subFileNames = sourceFile.list();
          if (subFileNames != null)
          {
            for (String subFileName : subFileNames)
            {
              fileList.add(fileName+File.separator+subFileName);
            }
          }

          // create directory
          if (toFile.isDirectory())
          {
            File newDirectory = new File(toFile,fileName);
            if (!newDirectory.exists())
            {
              newDirectory.mkdirs();
            }
          }
        }
        else
        {
          // get destination file
          File destinationFile;
          if (toFile.isDirectory())
          {
            destinationFile = new File(toFile,fileName);
          }
          else
          {
            destinationFile = toFile;
          }

          // create destination parent directory
          File newDirectory = destinationFile.getParentFile();
          if (!newDirectory.exists())
          {
            newDirectory.mkdirs();
          }
//Dprintf.dprintf("copy %s -> %s",sourceFile,destinationFile);

          // copy file data
          FileInputStream  input  = new FileInputStream(sourceFile);
          FileOutputStream output = new FileOutputStream(destinationFile);
          byte[]           buffer = new byte[64*1024];

          int n;
          while ((n = input.read(buffer)) > 0)
          {
            output.write(buffer,0,n);
          }
          output.close();
          input.close();

          // set file permissions
          destinationFile.setExecutable(sourceFile.canExecute());
          destinationFile.setWritable(sourceFile.canWrite());
        }
      }
    }
  }

  /** copy file
   * @param fromFile from file
   * @param toFile to file
   */
  public static void copyFile(File fromFile, File toFile)
    throws IOException
  {
    copyFile(fromFile,toFile,(FileCopyConfirmation)null);
  }

  /** copy file
   * @param fromFileName from file name
   * @param toFileName to file name
   * @param fileCopyConfirmation confirm file copy
   */
  public static void copyFile(String fromFileName, String toFileName, FileCopyConfirmation fileCopyConfirmation)
    throws IOException
  {
    copyFile(new File(fromFileName),new File(toFileName),fileCopyConfirmation);
  }

  /** copy file
   * @param fromFileName from file name
   * @param toFileName to file name
   */
  public static void copyFile(String fromFileName, String toFileName)
    throws IOException
  {
    copyFile(fromFileName,toFileName,(FileCopyConfirmation)null);
  }

  /** copy directory tree
   * copy file located in fromDirectory to toDirectory including all sub-directories and files
   * @param fromDirectory from directory
   * @param toDirectory to directory
   */
  public static void copyDirectoryTree(File fromDirectory, File toDirectory)
    throws IOException
  {
    LinkedList<String> fileNameList = new LinkedList<String>();

    // get root-files
    String[] fileNames = fromDirectory.list();
    if (fileNames != null)
    {
      for (String fileName : fileNames)
      {
        fileNameList.add(fileName);
      }
    }

    // copy files
    while (!fileNameList.isEmpty())
    {
      String fileName = fileNameList.removeFirst();
Dprintf.dprintf("fileName=%s",fileName);

      File fromFile = new File(fromDirectory,fileName);
      File toFile   = new File(toDirectory,fileName);

      if (fromFile.isDirectory())
      {
        toFile.mkdir();
        String[] subFileNames = fromFile.list();
        if (subFileNames != null)
        {
          for (String subFileName : subFileNames)
          {
            fileNameList.add(fileName+File.separator+subFileName);
          }
        }
      }
      else
      {
        copyFile(fromFile,toFile);
      }

      toFile.setReadable(fromFile.canRead());
      toFile.setWritable(fromFile.canWrite());
      toFile.setExecutable(fromFile.canExecute());
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
