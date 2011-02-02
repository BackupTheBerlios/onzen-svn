/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/Background.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: background task
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/

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
  protected Object[] userData;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** create background task
   * @param userData user data
   */
  BackgroundRunnable(Object... userData)
  {
    this.userData = userData;
  }

  /** run method
   */
  abstract public void run();
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
    executorService.submit(backgroundRunnable);
  }
}

/* end of file */
