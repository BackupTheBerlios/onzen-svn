/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: background task
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/

import java.lang.reflect.Method;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

/****************************** Classes ********************************/

/** background task runnable
 */
abstract class BackgroundRunnable implements Runnable
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  protected Object[]            userData;

  private   StackTraceElement[] stackTrace;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create background task
   * @param userData user data
   */
  BackgroundRunnable(Object... userData)
  {
    this.userData = userData;
    this.stackTrace         = Thread.currentThread().getStackTrace();
  }

  /** run method
   */
  public void run()
  {
    Onzen.printInternalError("No run method declared for background task!");
    System.err.println("Stack trace:");
    for (int z = 2; z < stackTrace.length; z++)
    {
      System.err.println("  "+stackTrace[z]);
    }
    System.exit(Onzen.EXITCODE_INTERNAL_ERROR);
  }
}

/** background task
 */
class BackgroundTask implements Runnable
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  private BackgroundRunnable backgroundRunnable;
  private Method             runMethod;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create background task
   * @param backgroundRunnable background runnable to execute
   */
  BackgroundTask(BackgroundRunnable backgroundRunnable)
  {
    this.backgroundRunnable = backgroundRunnable;
    this.runMethod          = null;

    // find match run-method if possible
    for (Method method : backgroundRunnable.getClass().getDeclaredMethods())
    {
      // check name
      if (method.getName().equals("run"))
      {
        // check parameters
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == backgroundRunnable.userData.length)
        {
          boolean match = true;
          for (int z = 0; (z < backgroundRunnable.userData.length) && match; z++)
          {
            if (backgroundRunnable.userData[z] != null)
            {
              match = parameterTypes[z].isAssignableFrom(backgroundRunnable.userData[z].getClass());
            }
            else
            {
              match = Object.class.isAssignableFrom(parameterTypes[z]);
            }
          }

          if (match)
          {
            runMethod = method;
            break;
          }
        }
      }
    }
  }

  /** run background runnable
   */
  public void run()
  {
    try
    {
      if (runMethod != null)
      {
        // call specific run-method
        runMethod.invoke(backgroundRunnable,backgroundRunnable.userData);
      }
      else
      {
        // call general run-method
        backgroundRunnable.run();
      }
    }
    catch (Exception exception)
    {
      Onzen.printError("Unhandled background exception: %s",exception);
      System.err.println("Stack trace:");
      exception.printStackTrace();
    }
    catch (Error exception)
    {
      Onzen.printError("Unhandled background error: %s",exception);
      System.err.println("Stack trace:");
      exception.printStackTrace();
    }
  }
}

/** background executor
 */
public class Background
{
  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  public static ExecutorService executorService = Executors.newFixedThreadPool(Settings.maxBackgroundTasks,new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  });

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** run task in background
   * @param backgroundRunnable task to run in background
   */
  public static void run(BackgroundRunnable backgroundRunnable)
  {
    executorService.submit(new BackgroundTask(backgroundRunnable));
  }
}

/* end of file */
