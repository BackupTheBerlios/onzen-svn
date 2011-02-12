/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandMailPatch.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: command mail patch
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

//import java.text.SimpleDateFormat;

import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.LinkedHashSet;
import java.util.StringTokenizer;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** mail patch command
 */
class CommandMailPatch
{
  /** dialog data
   */
  class Data
  {
    String[] revisionNames;        // revision names
    String[] lines;                // patch lines
    String[] linesNoWhitespaces;   // patch lines (without whitespaces)
    String   summary;              // summary for patch
    String   message;              // message for patch (without mail prefix/postfix etc.)

    Data()
    {
      this.revisionNames      = null;
      this.lines              = null;
      this.linesNoWhitespaces = null;
      this.summary            = null;
      this.message            = null;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------
  public String summary;
  public String message;

  // global variable references
  private final RepositoryTab     repositoryTab;
  private final int               patchNumber;
  private final Date              date;
  private final Display           display;

  // dialog
  private final Data              data = new Data();
  private final Shell             dialog;

  // widgets
  private final Text              widgetPatch;
  private final Text              widgetSummary;
  private final Text              widgetMessage;
  private final Text              widgetMailTo;
  private final Text              widgetMailCC;
  private final Text              widgetMailSubject;
  private final Text              widgetMailText;
  private final Button            widgetSend;
  private final Button            widgetCancel;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** mail patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param patchNumber patch number
   * @param patchText patch text
   * @param message message text
   */
  CommandMailPatch(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, final int patchNumber, final String patchText, String message)
  {
    Composite composite,subComposite;
    Label     label;
    TabFolder tabFolder;
    Button    button;
    Listener  listener;

    // initialize variables
    this.summary       = null;
    this.message       = null;
    this.repositoryTab = repositoryTab;
    this.patchNumber   = patchNumber;
    this.date          = new Date();

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Mail patch",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Patch:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetPatch = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
      widgetPatch.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetPatch,1,0,TableLayoutData.NSWE);

      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,2,0,TableLayoutData.NSWE);

      subComposite = Widgets.addTab(tabFolder,"Message");
      subComposite.setLayout(new TableLayout(new double[]{0.0,1.0},new double[]{0.0,1.0},2));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(subComposite,"Summary:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetSummary = Widgets.newText(subComposite);
        Widgets.layout(widgetSummary,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Text:");
        Widgets.layout(label,1,0,TableLayoutData.NW);

        widgetMessage = Widgets.newText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
        Widgets.layout(widgetMessage,1,1,TableLayoutData.NSWE);
      }

      subComposite = Widgets.addTab(tabFolder,"Mail");
      subComposite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,1.0},new double[]{0.0,1.0},2));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(subComposite,"To:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetMailTo = Widgets.newText(subComposite);
        Widgets.layout(widgetMailTo,0,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"CC:");
        Widgets.layout(label,1,0,TableLayoutData.W);

        widgetMailCC = Widgets.newText(subComposite);
        Widgets.layout(widgetMailCC,1,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Subject:");
        Widgets.layout(label,2,0,TableLayoutData.W);

        widgetMailSubject = Widgets.newText(subComposite);
        Widgets.layout(widgetMailSubject,2,1,TableLayoutData.WE);

        label = Widgets.newLabel(subComposite,"Text:");
        Widgets.layout(label,3,0,TableLayoutData.NW);

        widgetMailText = Widgets.newText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
        Widgets.layout(widgetMailText,3,1,TableLayoutData.NSWE);
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetSend = Widgets.newButton(composite,"Send");
      Widgets.layout(widgetSend,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetSend.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          data.summary = widgetSummary.getText();
          data.message = widgetMessage.getText();

          File tmpFile = null;
          try
          {
            PrintWriter output;

            // create mail attachment
            tmpFile = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));
            FileWriter attachmentOutput = new FileWriter(tmpFile.getPath());
            attachmentOutput.write(patchText,0,patchText.length());
            attachmentOutput.close();

            // create command
            String[] command = StringUtils.split(Settings.commandMailAttachment,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS,false);
            StringUtils.replace(command,"%to%",widgetMailTo.getText());
            StringUtils.replace(command,"%cc%",widgetMailCC.getText());
            StringUtils.replace(command,"%subject%",widgetMailSubject.getText());
            StringUtils.replace(command,"%file%",tmpFile.getAbsolutePath());
//for (String s : command) Dprintf.dprintf("s=%s",s);

            // execute and add text
            Process process = Runtime.getRuntime().exec(command);
            PrintWriter processOutput = new PrintWriter(process.getOutputStream());
            StringTokenizer stringTokenizer = new StringTokenizer(widgetMailText.getText(),widgetMailText.DELIMITER);
            while (stringTokenizer.hasMoreTokens())
            {
              processOutput.println(stringTokenizer.nextToken());
            }
            processOutput.close();

            // wait done
            int exitcode = process.waitFor();
            if (exitcode != 0)
            {
              Dialogs.error(dialog,"Cannot send patch! (exitcode: %d)",exitcode);
              return;
            }
          }
          catch (IOException exception)
          {
            Dialogs.error(dialog,"Cannot send patch! (error: %s)",exception.getMessage());
            return;
          }
          catch (InterruptedException exception)
          {
            Dialogs.error(dialog,"Cannot send patch! (error: %s)",exception.getMessage());
          }
          finally
          {
            if (tmpFile != null) tmpFile.delete();
          }

          Settings.geometryMailPatch = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Save");
      button.setEnabled(false);
      Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;
Dprintf.dprintf("");

          Settings.geometryMailPatch = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });

      widgetCancel = Widgets.newButton(composite,"Cancel");
      Widgets.layout(widgetCancel,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetCancel.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetSummary.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        updateMailSubject();
      }
    });
    widgetMessage.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        updateMailText();
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryMailPatch);

    // update
    widgetPatch.setText(patchText);
    widgetMailTo.setText(repositoryTab.repository.patchMailTo);
    widgetMailCC.setText(repositoryTab.repository.patchMailCC);
    updateMailSubject();
    updateMailText();
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      Widgets.setFocus(widgetSummary);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        summary = data.summary;
        message = data.message;
      }
    }
  }

  /** run and wait for dialog
   */
  public boolean execute()
  {
    if (!dialog.isDisposed())
    {
      Widgets.setFocus(widgetSummary);
      if ((Boolean)Dialogs.run(dialog,false))
      {
        summary = data.summary;
        message = data.message;

        return true;
      }
      else
      {
        return false;
      }
    }
    else
    {
      return false;
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandMailPatch {}";
  }

  //-----------------------------------------------------------------------

  /** update mail subject line
   */
  private void updateMailSubject()
  {
    String subject = repositoryTab.repository.patchMailSubject;
    subject = subject.replace("%n%",Integer.toString(patchNumber));
    subject = subject.replace("%summary%",widgetSummary.getText());
    widgetMailSubject.setText(subject);
  }

  /** update mail text
   */
  private void updateMailText()
  {
    String text = repositoryTab.repository.patchMailText;
    text = text.replace("%n%",Integer.toString(patchNumber));
    text = text.replace("%summary%",widgetSummary.getText());
    text = text.replace("%date%",Onzen.DATE_FORMAT.format(date));
    text = text.replace("%time%",Onzen.TIME_FORMAT.format(date));
    text = text.replace("%datetime%",Onzen.DATETIME_FORMAT.format(date));
    text = text.replace("%message%",widgetMessage.getText());
    widgetMailText.setText(text);
  }
}

/* end of file */