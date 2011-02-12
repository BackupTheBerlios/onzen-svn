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

/****************************** Classes ********************************/

@XmlRootElement()
@XmlType(propOrder={"repositories"})
@XmlAccessorType(XmlAccessType.NONE)
public class RepositoryList implements Iterable<Repository>
{
  // --------------------------- constants --------------------------------
  private final static String LISTS_SUB_DIRECOTRY = "lists";

  // --------------------------- variables --------------------------------
  @XmlElement(name = "repository")
  private ArrayList<Repository> repositories;      // list of repositories

  private String                name;              // repository list name

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create and load repository list
   * @param name name of repository list
   */
  RepositoryList(String name)
  {
    this.repositories = new ArrayList<Repository>();
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

    // get names
    File directory = new File(Settings.ONZEN_DIRECTORY,LISTS_SUB_DIRECOTRY);
    for (File file : directory.listFiles())
    {
      if (file.isFile())
      {
        nameList.add(file.getName());
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
   * @return true iff control config read, false otherwise
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
        RepositoryList tmpRepositories = (RepositoryList)unmarshaller.unmarshal(new FileReader(fileName));
        synchronized(repositories)
        {
          repositories.clear();
          repositories.addAll(tmpRepositories.repositories);
        }
/*
for (Repository repository : repositories)
{
Dprintf.dprintf("repository=%s",repository);
}
/**/
      }

      // store name
      this.name = name;
    }
    catch (FileNotFoundException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return false;
    }
    catch (IOException exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return false;
    }
    catch (Exception exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
      return false;
    }

    return true;
  }

  /** save repository list to file (XML)
   * @param name name of repository list
   */
  public void save(String name)
  {
    try
    {
      String fileName = Settings.ONZEN_DIRECTORY+File.separator+LISTS_SUB_DIRECOTRY+File.separator+name;

      // create JAXB context and instantiate marshaller
      JAXBContext jaxbContext = JAXBContext.newInstance(RepositoryList.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
//marshaller.marshal(controlConfig, System.out);

/*
for (Repository repository : repositories)
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
    catch (Exception exception)
    {
Dprintf.dprintf("exception=%s",exception);
exception.printStackTrace();
    }
  }

  /** save repository list to file (XML)
   */
  public void save()
  {
    save(name);
  }

  /** get iterator for repository list
   * @return iterator
   */
  public Iterator<Repository> iterator()
  {
    synchronized(repositories)
    {
      return repositories.iterator();
    }
  }

  /** clear repository list
   */
  public void clear()
  {
    synchronized(repositories)
    {
      repositories.clear();
    }
  }

  /** add repository to list
   * @param repository repository
   */
  public void add(Repository repository)
  {
    synchronized(repositories)
    {
      repositories.add(repository);
    }
  }

  /** remove repository from list
   * @param repository repository
   */
  public void remove(Repository repository)
  {
    synchronized(repositories)
    {
      repositories.remove(repository);
    }
  }

  /** move repository to new position
   * @param repository repository
   * @param newIndex new index (0..n)
   */
  public void move(Repository repository, int newIndex)
  {
    synchronized(repositories)
    {
      repositories.remove(repository);
      repositories.add(newIndex,repository);
    }    
  }

  /** get repository list size
   * @param list size
   */
  public int size()
  {
    synchronized(repositories)
    {
      return repositories.size();
    }
  }
}

/* end of file */
