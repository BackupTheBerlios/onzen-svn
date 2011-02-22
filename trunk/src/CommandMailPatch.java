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
import java.util.Arrays;
//import java.util.Comparator;
import java.util.Date;
//import java.util.HashMap;
import java.util.HashSet;
//import java.util.LinkedList;
import java.util.StringTokenizer;

// graphics
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
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
    String[]        revisionNames;        // revision names
    String[]        lines;                // patch lines
    String[]        linesNoWhitespaces;   // patch lines (without whitespaces)
    String          summary;              // summary for patch
    String          message;              // message for patch (without mail prefix/postfix etc.)
    HashSet<String> tests;                // test infos

    Data()
    {
      this.revisionNames      = null;
      this.lines              = null;
      this.linesNoWhitespaces = null;
      this.summary            = null;
      this.message            = null;
      this.tests              = new HashSet<String>();
    }
  };

  // --------------------------- constants --------------------------------

  // user events
  private final int   USER_EVENT_ADD_NEW_TEST = 0xFFFF+0;

  // --------------------------- variables --------------------------------
  public String               summary;
  public String               message;

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Patch         patch;
  private final Date          date;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Text              widgetPatch;
  private final Text              widgetSummary;
  private final Text              widgetMessage;
  private final Table             widgetTests;
  private final Text              widgetNewTest;
  private final Button            widgetAddNewTest;
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
   * @param patch patch
   * @param message message text
   */
  CommandMailPatch(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, final Patch patch, String message)
  {
    Composite         composite,subComposite,subSubComposite,subSubSubComposite;
    Label             label;
    TabFolder         tabFolder;
    Button            button;
    ScrolledComposite scrolledComposite;
    SelectionListener selectionListener;
    Listener          listener;

    // initialize variables
    this.summary       = null;
    this.message       = null;
    this.repositoryTab = repositoryTab;
    this.patch         = patch;
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
      subComposite.setLayout(new TableLayout(1.0,new double[]{1.0,0.0},2));
      Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
      {
        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,1.0},new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"Summary:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetSummary = Widgets.newText(subSubComposite);
          Widgets.layout(widgetSummary,0,1,TableLayoutData.WE);

          label = Widgets.newLabel(subSubComposite,"Message:");
          Widgets.layout(label,1,0,TableLayoutData.NW);

          widgetMessage = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
          Widgets.layout(widgetMessage,1,1,TableLayoutData.NSWE);
        }

        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,1.0},1.0));
        Widgets.layout(subSubComposite,0,1,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"Tests:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetTests = Widgets.newTable(subSubComposite,SWT.CHECK);
          widgetTests.setHeaderVisible(false);
          Widgets.layout(widgetTests,1,0,TableLayoutData.NSWE);
          {
            for (String test : repositoryTab.repository.patchMailTests)
            {
              Widgets.addTableEntry(widgetTests,test,test);
            }
          }

          subSubSubComposite = Widgets.newComposite(subSubComposite,SWT.LEFT,2);
          subSubSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubSubComposite,2,0,TableLayoutData.WE);
          {
            widgetNewTest = Widgets.newText(subSubSubComposite);
            Widgets.layout(widgetNewTest,0,0,TableLayoutData.WE);
            widgetNewTest.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
                Widgets.notify(dialog,USER_EVENT_ADD_NEW_TEST);
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
              }
            });

            
            widgetAddNewTest = Widgets.newButton(subSubSubComposite,"Add");
            widgetAddNewTest.setEnabled(false);
            Widgets.layout(widgetAddNewTest,0,1,TableLayoutData.E);
            widgetAddNewTest.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                Widgets.notify(dialog,USER_EVENT_ADD_NEW_TEST);
              }
            });
          }
        }
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
            patch.write(tmpFile);

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

            // free resources
            tmpFile.delete(); tmpFile = null;
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
    widgetTests.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        updateMailText();
      }
    });
    widgetNewTest.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetAddNewTest.setEnabled(!widgetNewTest.getText().trim().isEmpty());
      }
    });

    dialog.addListener(USER_EVENT_ADD_NEW_TEST,new Listener()
    {
      public void handleEvent(Event event)
      {
        String newTest = widgetNewTest.getText().trim();

        if (!newTest.isEmpty())
        {
          // check for duplicate
          boolean found = false;
          for (String test : repositoryTab.repository.patchMailTests)
          {
            if (test.equalsIgnoreCase(newTest))
            {
              found = true;
              break;
            }
          }

          if (!found)
          {
            int n = repositoryTab.repository.patchMailTests.length;

            // add new test
            repositoryTab.repository.patchMailTests = Arrays.copyOf(repositoryTab.repository.patchMailTests,n+1);
            repositoryTab.repository.patchMailTests[n] = newTest;

            TableItem tableItem = Widgets.addTableEntry(widgetTests,newTest,newTest);
            tableItem.setChecked(true);
          }

          // add test to mail
          data.tests.add(newTest);
          updateMailText();
        }

        widgetNewTest.setText("");
        widgetNewTest.setFocus();
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryMailPatch);

    // update
    widgetPatch.setText(StringUtils.join(patch.lines,widgetPatch.DELIMITER));
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
    Macro macro = new Macro(repositoryTab.repository.patchMailSubject);
    macro.expand("n",      patch.getNumber()      );
    macro.expand("summary",widgetSummary.getText());
    widgetMailSubject.setText(macro.value());
  }

  /** update mail text
   */
  private void updateMailText()
  {
    ArrayList<String> tests = new ArrayList<String>();
    for (TableItem tableItem : widgetTests.getItems())
    {
      if (tableItem.getChecked()) tests.add((String)tableItem.getData());
    }

    Macro macro = new Macro(repositoryTab.repository.patchMailText);
    macro.expand("n",       patch.getNumber()                 );
    macro.expand("summary", widgetSummary.getText()           );
    macro.expand("date",    Onzen.DATE_FORMAT.format(date)    );
    macro.expand("time",    Onzen.TIME_FORMAT.format(date)    );
    macro.expand("datetime",Onzen.DATETIME_FORMAT.format(date));
    macro.expand("message", widgetMessage.getText()           );
    macro.expand("tests",   tests,"\n"                        );
    widgetMailText.setText(macro.value());
  }
}

/* end of file */
