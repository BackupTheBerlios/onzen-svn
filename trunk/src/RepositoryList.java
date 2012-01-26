/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/RepositoryList.java,v $
* $Revision: 1.4 $
* $Author: torsten $
* Contents: repository lists
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.PropertyException;
import javax.xml.bind.JAXBException;

/****************************** Classes ********************************/

@XmlRootElement()
@XmlType(propOrder={"repositoryList"})
@XmlAccessorType(XmlAccessType.NONE)
public class RepositoryList implements Iterable<Repository>
{
  // --------------------------- constants --------------------------------
  private final static String LISTS_SUB_DIRECOTRY = "lists";

  // --------------------------- variables --------------------------------
  public String                  name;              // repository list name

  @XmlElement(name = "repository")
  private LinkedList<Repository> repositoryList;    // list of repositories

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create and load repository list
   * @param name name of repository list
   */
  RepositoryList(String name)
  {
    this.repositoryList = new LinkedList<Repository>();
    if (name != null) load(name);
  }

  /** create and load repository list
   */
  RepositoryList()
  {
    this(null);
  }

  /** get sorted list of names with repository lists
   * @return name list array
   */
  public static String[] listNames()
  {
    ArrayList<String> nameList = new ArrayList<String>();

    // read file names
    File directory = new File(Settings.ONZEN_DIRECTORY,LISTS_SUB_DIRECOTRY);
    File[] files = directory.listFiles();
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isFile())
        {
          nameList.add(file.getName());
        }
      }
    }

    // create sorted array
    String[] names = nameList.toArray(new String[nameList.size()]);
    Arrays.sort(names);

    return names;
  }

  /** delete repository list
   * @param name name of repository list
   */
  public static void delete(String name)
  {
    String fileName = Settings.ONZEN_DIRECTORY+File.separator+LISTS_SUB_DIRECOTRY+File.separator+name;

    File file = new File(fileName);
    if (!file.delete())
    {
Dprintf.dprintf("");
    }
  }

  /** load repository list from file (XML)
   * @param name name of repository list
   * @return true iff repository list loaded, false otherwise
   */
  public boolean load(String name)
  {
    try
    {
      String fileName = Settings.ONZEN_DIRECTORY+File.separator+LISTS_SUB_DIRECOTRY+File.separator+name;

      if (new File(fileName).exists())
      {
        // create JAXB context and instantiate unmarshaller
        JAXBContext jaxbContext = JAXBContext.newInstance(RepositoryList.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // read xml file
        RepositoryList tmpRepositoryList = (RepositoryList)unmarshaller.unmarshal(new FileReader(fileName));
        synchronized(repositoryList)
        {
          repositoryList.clear();
          repositoryList.addAll(tmpRepositoryList.repositoryList);
        }
      }

      // store name
      this.name = name;
    }
    catch (FileNotFoundException exception)
    {
Dprintf.dprintf("exception=%s",exception);
      return false;
    }
    catch (JAXBException exception)
    {
      if (Settings.debugFlag)
      {
        throw new Error(exception);
      }
      return false;
    }

    return true;
  }

  /** save repository list to file (XML)
   * @param name name of repository list
   */
  public void save(String name)
    throws IOException
  {
    try
    {
      String fileName = Settings.ONZEN_DIRECTORY+File.separator+LISTS_SUB_DIRECOTRY+File.separator+name;

      // create directory if necessary
      File directory = new File(fileName).getParentFile();
      if      (!directory.exists())
      {
        if (!directory.mkdirs())
        {
          throw new IOException("create directory '"+directory.getName()+"' fail");
        }
      }
      else if (!directory.isDirectory())
      {
        throw new IOException("'"+directory.getName()+"' is not a directory");
      }

      // create JAXB context and instantiate marshaller
      JAXBContext jaxbContext = JAXBContext.newInstance(RepositoryList.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//marshaller.marshal(controlConfig, System.out);

/*
for (Repository repository : repositoryList)
{
Dprintf.dprintf("repository=%s",repository);
for (String s : repository.openDirectories) Dprintf.dprintf("open %s",s);
}
/**/
      // write xml file
      Writer writer = null;
      try
      {
        writer = new FileWriter(fileName);
        marshaller.marshal(this,writer);
      }
      finally
      {
        try
        {
          writer.close();
        }
        catch (Exception exception)
        {
          // ignored
        }
      }

      // store name
      this.name = name;
    }
    catch (PropertyException exception)
    {
      throw new IOException(exception);
    }
    catch (JAXBException exception)
    {
      throw new IOException(exception);
    }
  }

  /** save repository list to file (XML)
   */
  public void save()
    throws IOException
  {
    save(name);
  }

  /** get iterator for repository list
   * @return iterator
   */
  public Iterator<Repository> iterator()
  {
    synchronized(repositoryList)
    {
      return repositoryList.iterator();
    }
  }

  /** clear repository list
   */
  public void clear()
  {
    synchronized(repositoryList)
    {
      repositoryList.clear();
    }
  }

  /** get repository list size
   * @param list size
   */
  public int size()
  {
    synchronized(repositoryList)
    {
      return repositoryList.size();
    }
  }

  /** get repository in list
   * @param index index
   */
  public Repository get(int index)
  {
    synchronized(repositoryList)
    {
      return repositoryList.get(index);
    }
  }

  /** add repository to list
   * @param repository repository
   */
  public void insert(Repository prevRepository, Repository repository)
  {
    synchronized(repositoryList)
    {
      // find index
      int index = 0;
      for (Repository tmpRespository : repositoryList)
      {
        if (prevRepository == tmpRespository)
        {
          break;
        }
        index++;
      }

      // insert
      repositoryList.add(index,repository);
    }
  }

  /** add repository to list
   * @param repository repository
   */
  public void add(Repository repository)
  {
    synchronized(repositoryList)
    {
      repositoryList.add(repository);
    }
  }

  /** remove repository from list
   * @param repository repository
   */
  public void remove(Repository repository)
  {
    synchronized(repositoryList)
    {
      repositoryList.remove(repository);
    }
  }

  /** move repository to new position
   * @param repository repository
   * @param newIndex new index (0..n)
   */
  public void move(Repository repository, int newIndex)
  {
    synchronized(repositoryList)
    {
      repositoryList.remove(repository);
      repositoryList.add(newIndex,repository);
    }
  }
}

/* end of file */
