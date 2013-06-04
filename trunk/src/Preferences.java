/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: set preferences
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.mail.Authenticator;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

// graphics
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/****************************** Classes ********************************/

/** set preferences
 */
class Preferences
{
  /** dialog data
   */
  class Data
  {

    Data()
    {
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final Shell         shell;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetKeys;
  private final Table         widgetColors;
  private final Table         widgetFonts;

  private final Table         widgetEditors;
  private final Table         widgetShellCommands;
  private final Text          widgetCommandLineMail;
  private final Text          widgetCommandLineMailAttachment;

  private final Text          widgetMailSMTPHost;
  private final Spinner       widgetMailSMTPPort;
  private final Button        widgetMailSMTPSSL;
  private final Text          widgetMailLogin;
  private final Text          widgetMailPassword;
  private final Text          widgetMailFrom;

  private final Text          widgetReviewServerHost;
  private final Text          widgetReviewServerLogin;
  private final Text          widgetCommandLinePostReviewServer;
  private final Text          widgetCommandLineUpdateReviewServer;

  private final Text          widgetCVSCommand;
  private final Button        widgetCVSPruneEmptyDirectories;

  private final Text          widgetSVNCommand;
  private final Text          widgetSVNDiffCommand;
  private final Text          widgetSVNDiffCommandOptions;
  private final Text          widgetSVNDiffCommandOptionsIgnoreWhitespaces;
  private final Button        widgetAlwaysTrustServerCertificate;

  private final Text          widgetHGCommand;
  private final Text          widgetHGDiffCommand;
  private final Text          widgetHGDiffCommandOptions;
  private final Text          widgetHGDiffCommandOptionsIgnoreWhitespaces;
  private final Button        widgetHGUseForestExtension;
  private final Button        widgetHGUseQueueExtension;
  private final Button        widgetHGUpdateWithFetch;
  private final Button        widgetHGSafeUpdate;
  private final Button        widgetHGSingleLineCommitMessages;
  private final Spinner       widgetHGSingleLineMaxCommitMessageLength;
  private final Button        widgetHGRelativePatchPaths;

  private final Text          widgetGitCommand;

  private final List          widgetCheckoutHistoryPaths;

  private final Button        widgetEOLAuto;
  private final Button        widgetEOLUnix;
  private final Button        widgetEOLMac;
  private final Button        widgetEOLWindows;
  private final Button        widgetCheckTABs;
  private final Button        widgetCheckTrailingWhitespaces;
  private final List          widgetSkipWhitespaceCheckFilePatterns;
  private final List          widgetHiddenFilePatterns;
  private final List          widgetHiddenDirectoryPatterns;

  private final Text          widgetTmpDirectory;
  private final Text          widgetBackupFileSuffix;
  private final Text          widgetDateFormat;
  private final Text          widgetTimeFormat;
  private final Text          widgetDateTimeFormat;
  private final Spinner       widgetConvertSpacesPerTAB;
  private final Spinner       widgetMaxBackgroundTasks;
  private final Spinner       widgetMaxMessageHistory;
  private final Text          widgetMessageBroadcastAddress;
  private final Spinner       widgetMessageBroadcastPort;
  private final List          widgetAutoSummaryPatterns;
  private final Button        widgetSetWindowLocation;
  private final Table         widgetShowFlags;

  private final Button        widgetButtonSave;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view command
   * @param shell shell
   * @param onzen Onzen instance
   */
  Preferences(final Shell shell, final Onzen onzen)
  {
    TabFolder   tabFolder;
    Composite   composite,subComposite,subSubComposite,subSubSubComposite;
    Label       label;
    Button      button;
    Listener    listener;
    TableItem   tableItem;
    TableColumn tableColumn;

    // initialize variables
    this.shell = shell;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.openModal(shell,"Preferences",new double[]{1.0,0.0},1.0);

    // create tab
    tabFolder = Widgets.newTabFolder(dialog);
    Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);
    {
      composite = Widgets.addTab(tabFolder,"Keys");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,0,TableLayoutData.NSWE);
      {
        widgetKeys = Widgets.newTable(composite);
        Widgets.layout(widgetKeys,0,0,TableLayoutData.NSWE);
        tableColumn = Widgets.addTableColumn(widgetKeys,0,"Name",SWT.LEFT,200,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetKeys,1,"Key", SWT.LEFT,100,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        widgetKeys.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
            Table widget = (Table)mouseEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              TableItem tableItem = widget.getItem(index);
              String    name      = tableItem.getText(0);
              int[]     key       = (int[])tableItem.getData();

              key[0] = editKey(name,key[0]);
              Widgets.updateTableEntry(widgetKeys,key,null,Widgets.menuAcceleratorToText(key[0]));
            }
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
          }
        });
        widgetKeys.setToolTipText("Keyboard short cuts list.");
        addKeys();
      }

      composite = Widgets.addTab(tabFolder,"Colors");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,1,TableLayoutData.NSWE);
      {
        widgetColors = Widgets.newTable(composite);
        Widgets.layout(widgetColors,0,0,TableLayoutData.NSWE);
        tableColumn = Widgets.addTableColumn(widgetColors,0,"Name", SWT.LEFT,320,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetColors,1,"Foreground",SWT.LEFT,100,true);
        tableColumn = Widgets.addTableColumn(widgetColors,2,"Background",SWT.LEFT,100,true);
        widgetColors.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
            Table widget = (Table)mouseEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              TableItem      tableItem = widget.getItem(index);
              String         name      = tableItem.getText(0);
              Settings.Color color     = (Settings.Color)tableItem.getData();

              if (editColor(name,color))
              {
                if (color.foreground != null) Widgets.setTableEntryColor(widgetColors,color,1,new Color(null,color.foreground));
                if (color.background != null) Widgets.setTableEntryColor(widgetColors,color,2,new Color(null,color.background));
                widgetColors.deselectAll();
              }
            }
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
          }
        });
        widgetColors.setToolTipText("Colors list.");
        addColors();
      }

      composite = Widgets.addTab(tabFolder,"Fonts");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,2,TableLayoutData.NSWE);
      {
        widgetFonts = Widgets.newTable(composite);
        Widgets.layout(widgetFonts,0,0,TableLayoutData.NSWE);
        tableColumn = Widgets.addTableColumn(widgetFonts,0,"Name",SWT.LEFT,200,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetFonts,1,"Font",SWT.LEFT,100,true);
        widgetFonts.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
            Table widget = (Table)mouseEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              TableItem tableItem = widget.getItem(index);
              String    name      = tableItem.getText(0);
              FontData  fontData  = (FontData)tableItem.getData();

              FontDialog fontDialog = new FontDialog(dialog);
              fontDialog.setFontList(new FontData[]{fontData});
              FontData newFontData = fontDialog.open();
              if (newFontData != null)
              {
                fontData.setName(newFontData.getName());
                fontData.setHeight(newFontData.getHeight());
                fontData.setStyle(newFontData.getStyle());
                Widgets.updateTableEntry(widgetFonts,fontData,null,Widgets.fontDataToText(fontData));
                Widgets.setTableEntryFont(widgetFonts,fontData,1,fontData);
              }
            }
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
          }
        });
        widgetFonts.setToolTipText("Fonts list.");
        addFonts();
      }

      composite = Widgets.addTab(tabFolder,"Editors");
      composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
      Widgets.layout(composite,0,3,TableLayoutData.NSWE);
      {
        widgetEditors = Widgets.newTable(composite);
        Widgets.layout(widgetEditors,0,0,TableLayoutData.NSWE);
        tableColumn = Widgets.addTableColumn(widgetEditors,0,"Name",     SWT.LEFT,100,false);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetEditors,1,"Mime type",SWT.LEFT,200,false);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetEditors,2,"File name",SWT.LEFT,100,false);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetEditors,3,"Command",  SWT.LEFT,300,true );
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        widgetEditors.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            Table widget = (Table)selectionEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              Settings.Editor editor = (Settings.Editor)(widget.getItem(index).getData());

              if (editEditor(editor,"Edit editor","Save"))
              {
                Widgets.updateTableEntry(widgetEditors,editor,editor.name,editor.mimeType,editor.fileName,editor.commandLine);
              }
            }
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
        widgetEditors.setToolTipText("Editor list.");
        for (Settings.Editor editor : Settings.editors)
        {
          Widgets.addTableEntry(widgetEditors,editor.clone(),editor.name,editor.mimeType,editor.fileName,editor.commandLine);
        }

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,null));
        Widgets.layout(subComposite,1,0,TableLayoutData.E);
        {
          button = Widgets.newButton(subComposite,"Add");
          Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Settings.Editor editor = new Settings.Editor();

              if (editEditor(editor,"Add editor","Add"))
              {
                Widgets.addTableEntry(widgetEditors,editor,editor.name,editor.mimeType,editor.fileName,editor.commandLine);
              }
            }
          });
          button = Widgets.newButton(subComposite,"Remove");
          Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              int index = widgetEditors.getSelectionIndex();
              if (index >= 0)
              {
                widgetEditors.remove(index);
              }
            }
          });
        }
      }

      composite = Widgets.addTab(tabFolder,"Shell commands");
      composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
      Widgets.layout(composite,0,3,TableLayoutData.NSWE);
      {
        widgetShellCommands = Widgets.newTable(composite);
        Widgets.layout(widgetShellCommands,0,0,TableLayoutData.NSWE);
        tableColumn = Widgets.addTableColumn(widgetShellCommands,0,"Name",    SWT.LEFT, 200,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetShellCommands,1,"Command", SWT.LEFT, 200,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_STRING);
        tableColumn = Widgets.addTableColumn(widgetShellCommands,2,"Exitcode",SWT.RIGHT, 20,true);
        tableColumn.addSelectionListener(Widgets.DEFAULT_TABLE_SELECTION_LISTENER_INT);
        widgetShellCommands.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            Table widget = (Table)selectionEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              Settings.ShellCommand shellCommand = (Settings.ShellCommand)(widget.getItem(index).getData());

              if (editShellCommand(shellCommand,"Edit shell commands","Save"))
              {
                Widgets.updateTableEntry(widgetShellCommands,shellCommand,shellCommand.name,shellCommand.commandLine,Integer.toString(shellCommand.validExitcode));
              }
            }
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
        widgetShellCommands.setToolTipText("Shell command list.");
        for (Settings.ShellCommand shellCommand : Settings.shellCommands)
        {
          Widgets.addTableEntry(widgetShellCommands,shellCommand.clone(),shellCommand.name,shellCommand.commandLine,Integer.toString(shellCommand.validExitcode));
        }

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,null));
        Widgets.layout(subComposite,1,0,TableLayoutData.E);
        {
          button = Widgets.newButton(subComposite,"Add");
          Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              Settings.ShellCommand shellCommand = new Settings.ShellCommand();

              if (editShellCommand(shellCommand,"Add shell command","Add"))
              {
                Widgets.addTableEntry(widgetShellCommands,shellCommand,shellCommand.name,shellCommand.commandLine,Integer.toString(shellCommand.validExitcode));
              }
            }
          });
          button = Widgets.newButton(subComposite,"Edit");
          Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              int index = widgetShellCommands.getSelectionIndex();
              if (index >= 0)
              {
                Settings.ShellCommand shellCommand = (Settings.ShellCommand)(widgetShellCommands.getItem(index).getData());

                if (editShellCommand(shellCommand,"Edit shell commands","Save"))
                {
                  Widgets.updateTableEntry(widgetShellCommands,shellCommand,shellCommand.name,shellCommand.commandLine,Integer.toString(shellCommand.validExitcode));
                }
              }
            }
          });
          button = Widgets.newButton(subComposite,"Clone");
          Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              int index = widgetShellCommands.getSelectionIndex();
              if (index >= 0)
              {
                Settings.ShellCommand shellCommand = ((Settings.ShellCommand)(widgetShellCommands.getItem(index).getData())).clone();

                if (editShellCommand(shellCommand,"Add shell command","Add"))
                {
                  Widgets.addTableEntry(widgetShellCommands,shellCommand,shellCommand.name,shellCommand.commandLine,Integer.toString(shellCommand.validExitcode));
                }
              }
            }
          });
          button = Widgets.newButton(subComposite,"Remove");
          Widgets.layout(button,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              int index = widgetShellCommands.getSelectionIndex();
              if (index >= 0)
              {
                widgetShellCommands.remove(index);
              }
            }
          });
        }
      }

      composite = Widgets.addTab(tabFolder,"Mail");
      composite.setLayout(new TableLayout(0.0,1.0,2));
      Widgets.layout(composite,0,3,TableLayoutData.NSWE);
      {
        subComposite = Widgets.newGroup(composite,"External Mail");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,0,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          widgetCommandLineMail = Widgets.newText(subComposite);
          widgetCommandLineMail.setText(Settings.commandMail);
          Widgets.layout(widgetCommandLineMail,0,1,TableLayoutData.WE);
          widgetCommandLineMail.setToolTipText("External mail command.\nMacros:\n  ${to} - to address\n  ${cc} - CC address\n  ${subject} - subject line\n");

          label = Widgets.newLabel(subComposite,"Command with attachment:");
          Widgets.layout(label,1,0,TableLayoutData.W);
          widgetCommandLineMailAttachment = Widgets.newText(subComposite);
          widgetCommandLineMailAttachment.setText(Settings.commandMailAttachment);
          Widgets.layout(widgetCommandLineMailAttachment,1,1,TableLayoutData.WE);
          widgetCommandLineMailAttachment.setToolTipText("External mail command with an attachment.\nMacros:\n  ${to} - to address\n  ${cc} - CC address\n  ${subject} - subject line\n  ${file} - attachment file name");
        }

        subComposite = Widgets.newGroup(composite,"SMTP Mail");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Server:");
          Widgets.layout(label,0,0,TableLayoutData.W,0,0,2);

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{0.7,0.0,0.3,0.0}));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.WE,0,0,2);
          {
            widgetMailSMTPHost = Widgets.newText(subSubComposite);
            widgetMailSMTPHost.setText(Settings.mailSMTPHost);
            Widgets.layout(widgetMailSMTPHost,0,0,TableLayoutData.WE);
            widgetMailSMTPHost.setToolTipText("Mail SMTP server host name.");

            label = Widgets.newLabel(subSubComposite,"Port:");
            Widgets.layout(label,0,1,TableLayoutData.W);

            widgetMailSMTPPort = Widgets.newSpinner(subSubComposite,0,65535);
            widgetMailSMTPPort.setTextLimit(5);
            widgetMailSMTPPort.setSelection(Settings.mailSMTPPort);
            Widgets.layout(widgetMailSMTPPort,0,2,TableLayoutData.WE);
            widgetMailSMTPPort.setToolTipText("Mail SMTP server port number.");

            widgetMailSMTPSSL = Widgets.newCheckbox(subSubComposite,"SSL");
            widgetMailSMTPSSL.setSelection(Settings.mailSMTPSSL);
            Widgets.layout(widgetMailSMTPSSL,0,3,TableLayoutData.E);
            widgetMailSMTPSSL.setToolTipText("Use SMTP with SSL encryption.");
          }

          label = Widgets.newLabel(subComposite,"Login:");
          Widgets.layout(label,1,0,TableLayoutData.W,0,0,2);

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,1.0}));
          Widgets.layout(subSubComposite,1,1,TableLayoutData.WE,0,0,2);
          {
            widgetMailLogin = Widgets.newText(subSubComposite);
            widgetMailLogin.setText(Settings.mailLogin);
            Widgets.layout(widgetMailLogin,0,0,TableLayoutData.WE);
            widgetMailLogin.setToolTipText("Mail server login name.");

            label = Widgets.newLabel(subSubComposite,"Password:");
            Widgets.layout(label,0,1,TableLayoutData.W);

            widgetMailPassword = Widgets.newPassword(subSubComposite);
            String password = onzen.getPassword(Settings.mailLogin,Settings.mailSMTPHost,false);
            if (password != null) widgetMailPassword.setText(password);
            Widgets.layout(widgetMailPassword,0,2,TableLayoutData.WE);
            widgetMailPassword.setToolTipText("Mail server login password.");
          }

          label = Widgets.newLabel(subComposite,"From name:");
          Widgets.layout(label,2,0,TableLayoutData.W,0,0,2);

          widgetMailFrom = Widgets.newText(subComposite);
          widgetMailFrom.setText(Settings.mailFrom);
          Widgets.layout(widgetMailFrom,2,1,TableLayoutData.WE,0,0,2);
          widgetMailFrom.setToolTipText("Mail from address.");

          button = Widgets.newButton(subComposite,"Send test mail");
          Widgets.layout(button,3,1,TableLayoutData.E);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String toAddress = Dialogs.string(dialog,"Mail address","Mail address:",widgetMailFrom.getText().trim(),"Send","Cancel");
              if ((toAddress != null) && !toAddress.trim().isEmpty())
              {
                sendTestMail(widgetMailSMTPHost.getText().trim(),
                             Integer.parseInt(widgetMailSMTPPort.getText()),
                             widgetMailSMTPSSL.getSelection(),
                             widgetMailLogin.getText().trim(),
                             widgetMailPassword.getText().trim(),
                             widgetMailFrom.getText().trim(),
                             toAddress.trim()
                            );
              }
           }
          });
        }

        subComposite = Widgets.newGroup(composite,"Post review server");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,2,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Server:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          widgetReviewServerHost = Widgets.newText(subComposite);
          widgetReviewServerHost.setText(Settings.reviewServerHost);
          Widgets.layout(widgetReviewServerHost,0,1,TableLayoutData.WE);
          widgetReviewServerHost.setToolTipText("Default post review server name.\n");

          label = Widgets.newLabel(subComposite,"Login:");
          Widgets.layout(label,1,0,TableLayoutData.W);
          widgetReviewServerLogin = Widgets.newText(subComposite);
          widgetReviewServerLogin.setText(Settings.reviewServerLogin);
          Widgets.layout(widgetReviewServerLogin,1,1,TableLayoutData.WE);
          widgetReviewServerLogin.setToolTipText("Default review server login name.\n");

          label = Widgets.newLabel(subComposite,"Post command:");
          Widgets.layout(label,2,0,TableLayoutData.W);
          widgetCommandLinePostReviewServer = Widgets.newText(subComposite);
          widgetCommandLinePostReviewServer.setText(Settings.commandPostReviewServer);
          Widgets.layout(widgetCommandLinePostReviewServer,2,1,TableLayoutData.WE);
          widgetCommandLinePostReviewServer.setToolTipText("Post review server command.\nMacros:\n  ${server} - review server name\n  ${login} - login name\n  ${password} - password\n  ${summary} - summary line\n  ${description} - description\n  ${tests} - tests done\n  ${file} - diff file name\n");

          label = Widgets.newLabel(subComposite,"Update command:");
          Widgets.layout(label,3,0,TableLayoutData.W);
          widgetCommandLineUpdateReviewServer = Widgets.newText(subComposite);
          widgetCommandLineUpdateReviewServer.setText(Settings.commandUpdateReviewServer);
          Widgets.layout(widgetCommandLineUpdateReviewServer,3,1,TableLayoutData.WE);
          widgetCommandLineUpdateReviewServer.setToolTipText("Update review server command.\nMacros:\n  ${server} - review server name\n  ${login} - login name\n  ${password} - password\n  ${reference} - reference\n  ${summary} - summary line\n  ${description} - description\n  ${tests} - tests done\n  ${file} - diff file name\n");
        }
      }

      composite = Widgets.addTab(tabFolder,"Repository");
      composite.setLayout(new TableLayout(null,1.0,2));
      Widgets.layout(composite,0,4,TableLayoutData.NSWE);
      {
        // --- CVS -----------------------------------------------------

        subComposite = Widgets.newGroup(composite,"CVS");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.WE);
          {
            widgetCVSCommand = Widgets.newText(subSubComposite);
            widgetCVSCommand.setText(Settings.cvsCommand);
            Widgets.layout(widgetCVSCommand,0,0,TableLayoutData.WE);
            widgetCVSCommand.setToolTipText("CVS command.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetCVSCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetCVSCommand.setText(command);
                }
              }
            });
          }

          label = Widgets.newLabel(subComposite,"Miscellaneous:");
          Widgets.layout(label,1,0,TableLayoutData.W);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,1,TableLayoutData.WE);
          {
            widgetCVSPruneEmptyDirectories = Widgets.newCheckbox(subSubComposite,"prune empty directories");
            widgetCVSPruneEmptyDirectories.setSelection(Settings.cvsPruneEmtpyDirectories);
            Widgets.layout(widgetCVSPruneEmptyDirectories,0,0,TableLayoutData.WE);
            widgetCVSPruneEmptyDirectories.setToolTipText("Delete empty directories after check-out.");
          }
        }

        // --- SVN -----------------------------------------------------

        subComposite = Widgets.newGroup(composite,"SVN");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,1,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.WE);
          {
            widgetSVNCommand = Widgets.newText(subSubComposite);
            widgetSVNCommand.setText(Settings.svnCommand);
            Widgets.layout(widgetSVNCommand,0,0,TableLayoutData.WE);
            widgetSVNCommand.setToolTipText("SVN command.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetCVSCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetSVNCommand.setText(command);
                }
              }
            });
          }

          label = Widgets.newLabel(subComposite,"External diff:");
          Widgets.layout(label,1,0,TableLayoutData.NW);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,1,1,TableLayoutData.WE);
          {
            widgetSVNDiffCommand = Widgets.newText(subSubComposite);
            widgetSVNDiffCommand.setText(Settings.svnDiffCommand);
            Widgets.layout(widgetSVNDiffCommand,0,0,TableLayoutData.WE);
            widgetSVNDiffCommand.setToolTipText("External SVN diff command.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetSVNDiffCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetSVNDiffCommand.setText(command);
                }
              }
            });
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0,1.0}));
          Widgets.layout(subSubComposite,2,1,TableLayoutData.WE);
          {
            label = Widgets.newLabel(subSubComposite,"Options:");
            Widgets.layout(label,0,0,TableLayoutData.W);
            widgetSVNDiffCommandOptions = Widgets.newText(subSubComposite);
            widgetSVNDiffCommandOptions.setText(Settings.svnDiffCommandOptions);
            Widgets.layout(widgetSVNDiffCommandOptions,0,1,TableLayoutData.WE);
            widgetSVNDiffCommandOptions.setToolTipText("Options for external diff command. Leave it empty for using internal diff of SVN.");

            label = Widgets.newLabel(subSubComposite,"No whitespaces:");
            Widgets.layout(label,0,2,TableLayoutData.W);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces = Widgets.newText(subSubComposite);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces.setText(Settings.svnDiffCommandOptionsIgnoreWhitespaces);
            Widgets.layout(widgetSVNDiffCommandOptionsIgnoreWhitespaces,0,3,TableLayoutData.WE);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces.setToolTipText("Options for external diff command with ignoring whitespace changes.");
          }

          label = Widgets.newLabel(subComposite,"Miscellaneous:");
          Widgets.layout(label,3,0,TableLayoutData.NW);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,1.0));
          Widgets.layout(subSubComposite,3,1,TableLayoutData.WE);
          {
            widgetAlwaysTrustServerCertificate = Widgets.newCheckbox(subSubComposite,"always trust server certificate");
            widgetAlwaysTrustServerCertificate.setSelection(Settings.svnAlwaysTrustServerCertificate);
            Widgets.layout(widgetAlwaysTrustServerCertificate,0,0,TableLayoutData.WE);
            widgetAlwaysTrustServerCertificate.setToolTipText("Always trust server certificate.");
          }
        }

        // --- HG ------------------------------------------------------

        subComposite = Widgets.newGroup(composite,"HG");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,2,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.WE);
          {
            widgetHGCommand = Widgets.newText(subSubComposite);
            widgetHGCommand.setText(Settings.hgCommand);
            Widgets.layout(widgetHGCommand,0,0,TableLayoutData.WE);
            widgetHGCommand.setToolTipText("HG command.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetCVSCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetHGCommand.setText(command);
                }
              }
            });
          }

          label = Widgets.newLabel(subComposite,"External diff:");
          Widgets.layout(label,1,0,TableLayoutData.NW);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,1,1,TableLayoutData.WE);
          {
            widgetHGDiffCommand = Widgets.newText(subSubComposite);
            widgetHGDiffCommand.setText(Settings.hgDiffCommand);
            Widgets.layout(widgetHGDiffCommand,0,0,TableLayoutData.WE);
            widgetHGDiffCommand.setToolTipText("External HG diff command. Leave it empty for using internal diff of HG.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetHGDiffCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetHGDiffCommand.setText(command);
                }
              }
            });
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0,0.0,1.0}));
          Widgets.layout(subSubComposite,2,1,TableLayoutData.WE);
          {
            label = Widgets.newLabel(subSubComposite,"Options:");
            Widgets.layout(label,0,0,TableLayoutData.W);
            widgetHGDiffCommandOptions = Widgets.newText(subSubComposite);
            widgetHGDiffCommandOptions.setText(Settings.hgDiffCommandOptions);
            Widgets.layout(widgetHGDiffCommandOptions,0,1,TableLayoutData.WE);
            widgetHGDiffCommandOptions.setToolTipText("Options for external diff command.");

            label = Widgets.newLabel(subSubComposite,"No whitespaces:");
            Widgets.layout(label,0,2,TableLayoutData.W);
            widgetHGDiffCommandOptionsIgnoreWhitespaces = Widgets.newText(subSubComposite);
            widgetHGDiffCommandOptionsIgnoreWhitespaces.setText(Settings.hgDiffCommandOptionsIgnoreWhitespaces);
            Widgets.layout(widgetHGDiffCommandOptionsIgnoreWhitespaces,0,3,TableLayoutData.WE);
            widgetHGDiffCommandOptionsIgnoreWhitespaces.setToolTipText("Options for external diff command with ignoring whitespace changes.");
          }

          label = Widgets.newLabel(subComposite,"Miscellaneous:");
          Widgets.layout(label,3,0,TableLayoutData.NW);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,1.0));
          Widgets.layout(subSubComposite,3,1,TableLayoutData.WE);
          {
            widgetHGUseForestExtension = Widgets.newCheckbox(subSubComposite,"use forest extension");
            widgetHGUseForestExtension.setSelection(Settings.hgUseForestExtension);
            Widgets.layout(widgetHGUseForestExtension,0,0,TableLayoutData.WE);
            widgetHGUseForestExtension.setToolTipText("Use HG forest extension commands.");

            widgetHGUseQueueExtension = Widgets.newCheckbox(subSubComposite,"use queue extension");
            widgetHGUseQueueExtension.setSelection(Settings.hgUseQueueExtension);
            Widgets.layout(widgetHGUseQueueExtension,1,0,TableLayoutData.WE);
            widgetHGUseQueueExtension.setToolTipText("Use HG queue extension commands.");

            widgetHGUpdateWithFetch = Widgets.newCheckbox(subSubComposite,"update with fetch extension");
            widgetHGUpdateWithFetch.setSelection(Settings.hgUpdateWithFetch);
            Widgets.layout(widgetHGUpdateWithFetch,2,0,TableLayoutData.WE);
            widgetHGUpdateWithFetch.setToolTipText("Use HG fetch extension for update (fetch+fpush).");

            widgetHGSafeUpdate = Widgets.newCheckbox(subSubComposite,"'safe' update");
            widgetHGSafeUpdate.setSelection(Settings.hgSafeUpdate);
            Widgets.layout(widgetHGSafeUpdate,3,0,TableLayoutData.WE);
            widgetHGSafeUpdate.setToolTipText("Do 'safe' update. Allow fetch update with not-commited local changes: save local changes, revert, update and restore local changes with merge if needed.");

            subSubSubComposite = Widgets.newComposite(subSubComposite);
            subSubSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,0.0}));
            Widgets.layout(subSubSubComposite,4,0,TableLayoutData.WE);
            {
              widgetHGSingleLineCommitMessages = Widgets.newCheckbox(subSubSubComposite,"single-line commit messages");
              widgetHGSingleLineCommitMessages.setSelection(Settings.hgSingleLineCommitMessages);
              Widgets.layout(widgetHGSingleLineCommitMessages,0,0,TableLayoutData.WE);
              widgetHGSingleLineCommitMessages.setToolTipText("Concat commit message into single-line to make commit message visible in the one-line-only commit message log of Mercurial.");
              widgetHGSingleLineCommitMessages.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  // notify modification
                  Widgets.modified(Settings.hgSingleLineCommitMessages);
                }
              });

              label = Widgets.newLabel(subSubSubComposite,"Max:");
              Widgets.layout(label,0,1,TableLayoutData.W);

              widgetHGSingleLineMaxCommitMessageLength = Widgets.newSpinner(subSubSubComposite);
              widgetHGSingleLineMaxCommitMessageLength.setEnabled(Settings.hgSingleLineCommitMessages);
              widgetHGSingleLineMaxCommitMessageLength.setTextLimit(5);
              widgetHGSingleLineMaxCommitMessageLength.setSelection(Settings.hgSingleLineMaxCommitMessageLength);
              Widgets.layout(widgetHGSingleLineMaxCommitMessageLength,0,2,TableLayoutData.W);
              widgetHGSingleLineMaxCommitMessageLength.setToolTipText("Max. length of single-line commit message. If the length exceed this limit a warning is shown.");
              Widgets.addModifyListener(new WidgetModifyListener(widgetHGSingleLineMaxCommitMessageLength,Settings.hgSingleLineCommitMessages)
              {
                public void modified(Control control)
                {
                  control.setEnabled(widgetHGSingleLineCommitMessages.getSelection());
                }
              });
            }

            widgetHGRelativePatchPaths = Widgets.newCheckbox(subSubComposite,"relative patch paths");
            widgetHGRelativePatchPaths.setSelection(Settings.hgRelativePatchPaths);
            Widgets.layout(widgetHGRelativePatchPaths,5,0,TableLayoutData.WE);
            widgetHGRelativePatchPaths.setToolTipText("Remove path prefixes 'a/' and 'b/' Mercurial is adding to file names in patches and convert to a relative path.");
          }
        }

        // --- GIT -----------------------------------------------------

        subComposite = Widgets.newGroup(composite,"Git");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,3,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.WE);
          {
            widgetGitCommand = Widgets.newText(subSubComposite);
            widgetGitCommand.setText(Settings.gitCommand);
            Widgets.layout(widgetGitCommand,0,0,TableLayoutData.WE);
            widgetGitCommand.setToolTipText("GIT command.");

            button = Widgets.newButton(subSubComposite,Onzen.IMAGE_DIRECTORY);
            Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String command = Dialogs.fileOpen(shell,
                                                  "Select command",
                                                  widgetCVSCommand.getText(),
                                                  new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                               "Scripts",    "*.sh",
                                                               "Batch files","*.cmd",
                                                               "Executables","*.exe"
                                                              }
                                                 );
                if (command != null)
                {
                  widgetGitCommand.setText(command);
                }
              }
            });
          }
        }
      }

      composite = Widgets.addTab(tabFolder,"History");
      composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0},1.0,2));
      Widgets.layout(composite,0,5,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(composite,"Repository paths:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetCheckoutHistoryPaths = Widgets.newList(composite);
        Widgets.layout(widgetCheckoutHistoryPaths,1,0,TableLayoutData.NSWE);
        widgetCheckoutHistoryPaths.setToolTipText("Repository path history list.");
        widgetCheckoutHistoryPaths.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
            List widget = (List)selectionEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              String path = Dialogs.path(dialog,"Add repository path","Path:",widget.getItem(index));
              if (path != null)
              {
                widget.setItem(index,path.trim());
              }
            }
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
          }
        });
        for (String path : Settings.checkoutHistoryPaths)
        {
          widgetCheckoutHistoryPaths.add(path.trim());
        }

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,null));
        Widgets.layout(subComposite,2,0,TableLayoutData.E);
        {
          button = Widgets.newButton(subComposite,"Add");
          Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String path = Dialogs.path(dialog,"Add repository path","Path:");
              if (path != null)
              {
                widgetCheckoutHistoryPaths.add(path.trim());
              }
            }
          });

          button = Widgets.newButton(subComposite,"Remove");
          Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              int index = widgetCheckoutHistoryPaths.getSelectionIndex();
              if (index >= 0)
              {
                widgetCheckoutHistoryPaths.remove(index);
              }
            }
          });

          button = Widgets.newButton(subComposite,"Sort");
          Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String paths[] = widgetCheckoutHistoryPaths.getItems();
              Arrays.sort(paths);
              widgetCheckoutHistoryPaths.setItems(paths);
            }
          });
        }
      }

      composite = Widgets.addTab(tabFolder,"Files");
      composite.setLayout(new TableLayout(new double[]{0.0,0.0,1.0,1.0,1.0},new double[]{0.0,1.0},2));
      Widgets.layout(composite,0,6,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(composite,"End-Of-Line type:");
        Widgets.layout(label,0,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,0.0));
        Widgets.layout(subComposite,0,1,TableLayoutData.WE);
        {
          widgetEOLAuto = Widgets.newRadio(subComposite,"auto");
          widgetEOLAuto.setSelection(Settings.eolType == Settings.EOLTypes.AUTO);
          Widgets.layout(widgetEOLAuto,0,0,TableLayoutData.W);
          widgetEOLAuto.setToolTipText("Set system dependent end-of-line type.");

          widgetEOLUnix = Widgets.newRadio(subComposite,"Unix (LF)");
          widgetEOLUnix.setSelection(Settings.eolType == Settings.EOLTypes.UNIX);
          Widgets.layout(widgetEOLUnix,0,1,TableLayoutData.W);
          widgetEOLUnix.setToolTipText("Set system dependent end-of-line type.");

          widgetEOLMac = Widgets.newRadio(subComposite,"Mac (CR)");
          widgetEOLMac.setSelection(Settings.eolType == Settings.EOLTypes.MAC);
          Widgets.layout(widgetEOLMac,0,2,TableLayoutData.W);
          widgetEOLMac.setToolTipText("Set system dependent end-of-line type.");

          widgetEOLWindows = Widgets.newRadio(subComposite,"Windows (CR+LF)");
          widgetEOLWindows.setSelection(Settings.eolType == Settings.EOLTypes.WINDOWS);
          Widgets.layout(widgetEOLWindows,0,3,TableLayoutData.W);
          widgetEOLWindows.setToolTipText("Set system dependent end-of-line type.");
        }

        label = Widgets.newLabel(composite,"Whitespaces:");
        Widgets.layout(label,1,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,1.0));
        Widgets.layout(subComposite,1,1,TableLayoutData.WE);
        {
          widgetCheckTABs = Widgets.newCheckbox(subComposite,"auto TABs check");
          widgetCheckTABs.setSelection(Settings.checkTABs);
          Widgets.layout(widgetCheckTABs,0,0,TableLayoutData.W);
          widgetCheckTABs.setToolTipText("Check if file contain TABs before file is added or a commit is done.");

          widgetCheckTrailingWhitespaces = Widgets.newCheckbox(subComposite,"auto whitespaces check");
          widgetCheckTrailingWhitespaces.setSelection(Settings.checkTrailingWhitespaces);
          Widgets.layout(widgetCheckTrailingWhitespaces,1,0,TableLayoutData.W);
          widgetCheckTrailingWhitespaces.setToolTipText("Check if a file contain trailing whitespaces before file is added or a commit is done.");
        }

        label = Widgets.newLabel(composite,"Skip whitespace-check files:");
        Widgets.layout(label,2,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,2,1,TableLayoutData.NSWE);
        {
          widgetSkipWhitespaceCheckFilePatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetSkipWhitespaceCheckFilePatterns,0,0,TableLayoutData.NSWE);
          widgetSkipWhitespaceCheckFilePatterns.setToolTipText("Patterns for files where whitespace-check should be skipped.");
          widgetSkipWhitespaceCheckFilePatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              List widget = (List)mouseEvent.widget;

              int index = widget.getSelectionIndex();
              if (index >= 0)
              {
Dprintf.dprintf("");
              }
            }
            public void mouseDown(MouseEvent mouseEvent)
            {
            }
            public void mouseUp(MouseEvent mouseEvent)
            {
            }
          });
          for (Settings.FilePattern filePattern : Settings.skipWhitespaceCheckFilePatterns)
          {
            widgetSkipWhitespaceCheckFilePatterns.add(filePattern.string);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.E);
          {
            button = Widgets.newButton(subSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String string = Dialogs.string(dialog,"Add file pattern","Pattern:","","Add","Cancel","File pattern: * and ?");
                if (string != null)
                {
                  widgetSkipWhitespaceCheckFilePatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetSkipWhitespaceCheckFilePatterns.getSelectionIndex();
                if (index >= 0)
                {
                  widgetSkipWhitespaceCheckFilePatterns.remove(index);
                }
              }
            });
          }
        }

        label = Widgets.newLabel(composite,"Hidden files:");
        Widgets.layout(label,3,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,3,1,TableLayoutData.NSWE);
        {
          widgetHiddenFilePatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetHiddenFilePatterns,0,0,TableLayoutData.NSWE);
          widgetHiddenFilePatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              List widget = (List)mouseEvent.widget;

              int index = widget.getSelectionIndex();
              if (index >= 0)
              {
Dprintf.dprintf("");
              }
            }
            public void mouseDown(MouseEvent mouseEvent)
            {
            }
            public void mouseUp(MouseEvent mouseEvent)
            {
            }
          });
          widgetHiddenFilePatterns.setToolTipText("Patterns for hidden files in tree view.");
          for (Settings.FilePattern filePattern : Settings.hiddenFilePatterns)
          {
            widgetHiddenFilePatterns.add(filePattern.string);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.E);
          {
            button = Widgets.newButton(subSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String string = Dialogs.string(dialog,"Add file pattern","Pattern:","","Add","Cancel","File pattern: * and ?");
                if (string != null)
                {
                  widgetHiddenFilePatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetHiddenFilePatterns.getSelectionIndex();
                if (index >= 0)
                {
                  widgetHiddenFilePatterns.remove(index);
                }
              }
            });
          }
        }

        label = Widgets.newLabel(composite,"Hidden directories:");
        Widgets.layout(label,4,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,4,1,TableLayoutData.NSWE);
        {
          widgetHiddenDirectoryPatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetHiddenDirectoryPatterns,0,0,TableLayoutData.NSWE);
          widgetHiddenDirectoryPatterns.setToolTipText("Patterns for hidden directories in tree view.");
          widgetHiddenDirectoryPatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              List widget = (List)mouseEvent.widget;

              int index = widget.getSelectionIndex();
              if (index >= 0)
              {
Dprintf.dprintf("");
              }
            }
            public void mouseDown(MouseEvent mouseEvent)
            {
            }
            public void mouseUp(MouseEvent mouseEvent)
            {
            }
          });
          for (Settings.FilePattern filePattern : Settings.hiddenDirectoryPatterns)
          {
            widgetHiddenDirectoryPatterns.add(filePattern.string);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.E);
          {
            button = Widgets.newButton(subSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String string = Dialogs.string(dialog,"Add directory pattern","Pattern:","","Add","Cancel","Directory pattern: * and ?");
                if (string != null)
                {
                  widgetHiddenDirectoryPatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetHiddenDirectoryPatterns.getSelectionIndex();
                if (index >= 0)
                {
                  widgetHiddenDirectoryPatterns.remove(index);
                }
              }
            });
          }
        }
      }

      composite = Widgets.addTab(tabFolder,"Misc");
      composite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0,0.0},new double[]{0.0,1.0},2));
      Widgets.layout(composite,0,7,TableLayoutData.NSWE);
      {
        label = Widgets.newLabel(composite,"Temporary directory:");
        Widgets.layout(label,0,0,TableLayoutData.W);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(1.0,new double[]{1.0,0.0}));
        Widgets.layout(subComposite,0,1,TableLayoutData.WE);
        {
          widgetTmpDirectory = Widgets.newText(subComposite);
          widgetTmpDirectory.setText(Settings.tmpDirectory);
          Widgets.layout(widgetTmpDirectory,0,0,TableLayoutData.WE);
          widgetTmpDirectory.setToolTipText("Temporary directory.");

          button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
          Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
          button.addSelectionListener(new SelectionListener()
          {
            public void widgetDefaultSelected(SelectionEvent selectionEvent)
            {
            }
            public void widgetSelected(SelectionEvent selectionEvent)
            {
              String directoryPath = Dialogs.directory(shell,
                                                       "Select temporary directory",
                                                       widgetTmpDirectory.getText()
                                                      );
              if (directoryPath != null)
              {
                widgetTmpDirectory.setText(directoryPath);
              }
            }
          });
        }

        label = Widgets.newLabel(composite,"Backup file suffix:");
        Widgets.layout(label,1,0,TableLayoutData.W);
        widgetBackupFileSuffix = Widgets.newText(composite);
        widgetBackupFileSuffix.setText(Settings.backupFileSuffix);
        Widgets.layout(widgetBackupFileSuffix,1,1,TableLayoutData.WE);
        widgetBackupFileSuffix.setToolTipText("Backup file name suffix.");

        label = Widgets.newLabel(composite,"Date format:");
        Widgets.layout(label,2,0,TableLayoutData.W);
        widgetDateFormat = Widgets.newText(composite);
        widgetDateFormat.setText(Settings.dateFormat);
        Widgets.layout(widgetDateFormat,2,1,TableLayoutData.WE);
        widgetDateFormat.setToolTipText("Date format.\nPatterns:\n  y - year digit\n  M - month digit\n  d - day digit\n  E - week day name");

        label = Widgets.newLabel(composite,"Time format:");
        Widgets.layout(label,3,0,TableLayoutData.W);
        widgetTimeFormat = Widgets.newText(composite);
        widgetTimeFormat.setText(Settings.timeFormat);
        Widgets.layout(widgetTimeFormat,3,1,TableLayoutData.WE);
        widgetTimeFormat.setToolTipText("Time format.\nPatterns:\n  H - hour digit\n  m - minute digit\n  s - second digit");

        label = Widgets.newLabel(composite,"Date/Time format:");
        Widgets.layout(label,4,0,TableLayoutData.W);
        widgetDateTimeFormat = Widgets.newText(composite);
        widgetDateTimeFormat.setText(Settings.dateTimeFormat);
        Widgets.layout(widgetDateTimeFormat,4,1,TableLayoutData.WE);
        widgetDateTimeFormat.setToolTipText("Date/time format.\nPatterns:\n  y - year digit\n  M - month digit\n  d - day digit\n  E - week day name\n  H - hour digit\n  m - minute digit\n  s - second digit");


        label = Widgets.newLabel(composite,"Spaces per TAB:");
        Widgets.layout(label,5,0,TableLayoutData.W);
        widgetConvertSpacesPerTAB = Widgets.newSpinner(composite,1,256);
        widgetConvertSpacesPerTAB.setSelection(Settings.convertSpacesPerTAB);
        Widgets.layout(widgetConvertSpacesPerTAB,5,1,TableLayoutData.W);
        widgetConvertSpacesPerTAB.setToolTipText("Number of spaces when converting TABs.");

        label = Widgets.newLabel(composite,"Max. background tasks:");
        Widgets.layout(label,6,0,TableLayoutData.W);
        widgetMaxBackgroundTasks = Widgets.newSpinner(composite,1,256);
        widgetMaxBackgroundTasks.setSelection(Settings.maxBackgroundTasks);
        Widgets.layout(widgetMaxBackgroundTasks,6,1,TableLayoutData.W);
        widgetMaxBackgroundTasks.setToolTipText("Max. number of background tasks.");

        label = Widgets.newLabel(composite,"Max. message history:");
        Widgets.layout(label,7,0,TableLayoutData.W);
        widgetMaxMessageHistory = Widgets.newSpinner(composite,0);
        widgetMaxMessageHistory.setSelection(Settings.maxMessageHistory);
        Widgets.layout(widgetMaxMessageHistory,7,1,TableLayoutData.W);
        widgetMaxMessageHistory.setToolTipText("Max. length of commit message history.");

        label = Widgets.newLabel(composite,"Message broadcast:");
        Widgets.layout(label,8,0,TableLayoutData.W);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,0.0}));
        Widgets.layout(subComposite,8,1,TableLayoutData.WE);
        {
          widgetMessageBroadcastAddress = Widgets.newText(subComposite);
          widgetMessageBroadcastAddress.setText(Settings.messageBroadcastAddress);
          Widgets.layout(widgetMessageBroadcastAddress,0,0,TableLayoutData.WE);
          widgetMessageBroadcastAddress.setToolTipText("Commit message broadcast address.");

          label = Widgets.newLabel(subComposite,":");
          Widgets.layout(label,0,1,TableLayoutData.W);

          widgetMessageBroadcastPort = Widgets.newSpinner(subComposite,1,65535);
          widgetMessageBroadcastPort.setSelection(Settings.messageBroadcastPort);
          Widgets.layout(widgetMessageBroadcastPort,0,2,TableLayoutData.W);
          widgetMessageBroadcastPort.setToolTipText("Commit message broadcast port number.");
        }

        label = Widgets.newLabel(composite,"Auto-summary patterns:");
        Widgets.layout(label,9,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,9,1,TableLayoutData.NSWE);
        {
          widgetAutoSummaryPatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetAutoSummaryPatterns,0,0,TableLayoutData.NSWE);
          widgetAutoSummaryPatterns.setToolTipText("Patterns for creating summary message line.");
          widgetAutoSummaryPatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              List widget = (List)mouseEvent.widget;

              int index = widget.getSelectionIndex();
              if (index >= 0)
              {
                String  string    = widget.getItem(index);
                boolean retryFlag = false;
                do
                {
                  string = Dialogs.string(dialog,"Add auto-summary pattern","Pattern:",string,"Add","Cancel","Regular expression pattern.");
                  if (string != null)
                  {
                    retryFlag = false;
                    try
                    {
                      Pattern.compile(string);
                    }
                    catch (PatternSyntaxException exception)
                    {
                      if (Dialogs.confirm(dialog,"Confirm","Invalid pattern: '"+string+"' - re-edit?"))
                      {
                        retryFlag = true;
                      }
                      else
                      {
                        string = null;
                      }
                    }
                  }
                }
                while (retryFlag);

                if (string != null)
                {
                  widget.setItem(index,string);
                }
              }
            }
            public void mouseDown(MouseEvent mouseEvent)
            {
            }
            public void mouseUp(MouseEvent mouseEvent)
            {
            }
          });
          for (String pattern : Settings.autoSummaryPatterns)
          {
            widgetAutoSummaryPatterns.add(pattern);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.E);
          {
            button = Widgets.newButton(subSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String  string    = "(.*)";
                boolean retryFlag = false;
                do
                {
                  string = Dialogs.string(dialog,"Add auto-summary pattern","Pattern:",string,"Add","Cancel","Regular expression pattern.");
                  if (string != null)
                  {
                    retryFlag = false;
                    try
                    {
                      Pattern.compile(string);
                    }
                    catch (PatternSyntaxException exception)
                    {
                      if (Dialogs.confirm(dialog,"Confirm","Invalid pattern: '"+string+"' - re-edit?"))
                      {
                        retryFlag = true;
                      }
                      else
                      {
                        string = null;
                      }
                    }
                  }
                }
                while (retryFlag);

                if (string != null)
                {
                  widgetAutoSummaryPatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                int index = widgetAutoSummaryPatterns.getSelectionIndex();
                if (index >= 0)
                {
                  widgetAutoSummaryPatterns.remove(index);
                }
              }
            });
          }
        }

        label = Widgets.newLabel(composite,"Show flags:");
        Widgets.layout(label,10,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(1.0,1.0));
        Widgets.layout(subComposite,10,1,TableLayoutData.NSWE);
        {
          widgetShowFlags = Widgets.newTable(subComposite,SWT.CHECK);
          Widgets.layout(widgetShowFlags,0,0,TableLayoutData.NSWE);
          Widgets.addTableColumn(widgetShowFlags,0,"",SWT.LEFT,500,true);
          widgetShowFlags.setLinesVisible(false);
          widgetShowFlags.setHeaderVisible(false);
          widgetShowFlags.addListener(SWT.Resize, new Listener()
          {
            public void handleEvent(Event event)
            {
              Table       table = (Table)event.widget;
              TableColumn tableColumn = table.getColumn(0);

              tableColumn.setWidth(table.getClientArea().width);
            }
          });
          widgetShowFlags.setToolTipText("Show dialogs flags.");

          Widgets.addTableEntry(widgetShowFlags,Settings.showUpdateStatusErrors,"update status errors").setChecked(Settings.showUpdateStatusErrors);
        }

        label = Widgets.newLabel(composite,"Miscellaneous:");
        Widgets.layout(label,11,0,TableLayoutData.W);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(0.0,0.0));
        Widgets.layout(subComposite,11,1,TableLayoutData.WE);
        {
          widgetSetWindowLocation = Widgets.newCheckbox(subComposite,"set window location");
          widgetSetWindowLocation.setSelection(Settings.setWindowLocation);
          Widgets.layout(widgetSetWindowLocation,0,1,TableLayoutData.W);
          widgetSetWindowLocation.setToolTipText("Set window location relative to cursor location.");
        }
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetButtonSave = Widgets.newButton(composite,"Save");
      Widgets.layout(widgetButtonSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetButtonSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          saveKeys();
          saveColors();
          saveFonts();

          Settings.editors                                = getEditors();
          Settings.shellCommands                          = getShellCommands();

          Settings.commandMail                            = widgetCommandLineMail.getText();
          Settings.commandMailAttachment                  = widgetCommandLineMailAttachment.getText().trim();

          Settings.mailSMTPHost                           = widgetMailSMTPHost.getText().trim();
          Settings.mailSMTPPort                           = Integer.parseInt(widgetMailSMTPPort.getText());
          Settings.mailSMTPSSL                            = widgetMailSMTPSSL.getSelection();
          Settings.mailLogin                              = widgetMailLogin.getText().trim();
          onzen.setPassword(Settings.mailLogin,Settings.mailSMTPHost,widgetMailPassword.getText());
          Settings.mailFrom                               = widgetMailFrom.getText().trim();

          Settings.cvsCommand                             = widgetCVSCommand.getText().trim();
          Settings.cvsPruneEmtpyDirectories               = widgetCVSPruneEmptyDirectories.getSelection();

          Settings.svnCommand                             = widgetSVNCommand.getText().trim();
          Settings.svnDiffCommand                         = widgetSVNDiffCommand.getText().trim();
          Settings.svnDiffCommandOptions                  = widgetSVNDiffCommandOptions.getText().trim();
          Settings.svnDiffCommandOptionsIgnoreWhitespaces = widgetSVNDiffCommandOptionsIgnoreWhitespaces.getText().trim();
          Settings.svnAlwaysTrustServerCertificate        = widgetAlwaysTrustServerCertificate.getSelection();

          Settings.hgCommand                              = widgetHGCommand.getText().trim();
          Settings.hgDiffCommand                          = widgetHGDiffCommand.getText().trim();
          Settings.hgDiffCommandOptions                   = widgetHGDiffCommandOptions.getText().trim();
          Settings.hgDiffCommandOptionsIgnoreWhitespaces  = widgetHGDiffCommandOptionsIgnoreWhitespaces.getText().trim();
          Settings.hgUseForestExtension                   = widgetHGUseForestExtension.getSelection();
          Settings.hgUseQueueExtension                    = widgetHGUseQueueExtension.getSelection();
          Settings.hgUpdateWithFetch                      = widgetHGUpdateWithFetch.getSelection();
          Settings.hgSafeUpdate                           = widgetHGSafeUpdate.getSelection();
          Settings.hgSingleLineCommitMessages             = widgetHGSingleLineCommitMessages.getSelection();
          Settings.hgSingleLineCommitMessages             = widgetHGSingleLineCommitMessages.getSelection();
          Settings.hgRelativePatchPaths                   = widgetHGRelativePatchPaths.getSelection();

          Settings.gitCommand                             = widgetGitCommand.getText().trim();

          Settings.tmpDirectory                           = widgetTmpDirectory.getText().trim();
          Settings.backupFileSuffix                       = widgetBackupFileSuffix.getText().trim();
          Settings.dateFormat                             = widgetDateFormat.getText().trim();
          Settings.timeFormat                             = widgetTimeFormat.getText().trim();
          Settings.dateTimeFormat                         = widgetDateTimeFormat.getText().trim();
          Settings.convertSpacesPerTAB                    = Integer.parseInt(widgetConvertSpacesPerTAB.getText());
          Settings.maxBackgroundTasks                     = Integer.parseInt(widgetMaxBackgroundTasks.getText());
          Settings.maxMessageHistory                      = Integer.parseInt(widgetMaxMessageHistory.getText());

          Settings.checkoutHistoryPaths                   = widgetCheckoutHistoryPaths.getItems();

          if      (widgetEOLAuto.getSelection()   ) Settings.eolType = Settings.EOLTypes.AUTO;
          else if (widgetEOLUnix.getSelection()   ) Settings.eolType = Settings.EOLTypes.UNIX;
          else if (widgetEOLMac.getSelection()    ) Settings.eolType = Settings.EOLTypes.MAC;
          else if (widgetEOLWindows.getSelection()) Settings.eolType = Settings.EOLTypes.WINDOWS;
          else                                      Settings.eolType = Settings.EOLTypes.AUTO;
          Settings.checkTABs                              = widgetCheckTABs.getSelection();
          Settings.checkTrailingWhitespaces               = widgetCheckTrailingWhitespaces.getSelection();

          Settings.messageBroadcastAddress                = widgetMessageBroadcastAddress.getText().trim();
          Settings.messageBroadcastPort                   = Integer.parseInt(widgetMessageBroadcastPort.getText());

          Settings.reviewServerHost                       = widgetReviewServerHost.getText().trim();
          Settings.reviewServerLogin                      = widgetReviewServerLogin.getText().trim();
//          onzen.setPassword(Settings.reviewServerLogin,Settings.reviewServerHost,widgetReviewServerPassword.getText());
          Settings.commandPostReviewServer                = widgetCommandLinePostReviewServer.getText().trim();
          Settings.commandUpdateReviewServer              = widgetCommandLineUpdateReviewServer.getText().trim();

          Settings.skipWhitespaceCheckFilePatterns        = getSkipWhitespaceCheckFilePatterns();
          Settings.hiddenFilePatterns                     = getHiddenFilePatterns();
          Settings.hiddenDirectoryPatterns                = getHiddenDirectoryPatterns();
          Settings.autoSummaryPatterns                    = getAutoSummaryPatterns();
          for (TableItem tableItem : widgetShowFlags.getItems())
          {
            Object data = tableItem.getData();
            if (data == Settings.showUpdateStatusErrors) Settings.showUpdateStatusErrors = tableItem.getChecked();
          }

          Settings.setWindowLocation                      = widgetSetWindowLocation.getSelection();

          Settings.geometryPreferences = dialog.getSize();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetKeys.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetColors.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetFonts.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryPreferences,Settings.setWindowLocation);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      if ((Boolean)Dialogs.run(dialog,false))
      {
        boolean saveSettings = true;
        if (Settings.isFileModified())
        {
          saveSettings = Dialogs.confirm(shell,
                                         "Confirmation",
                                         "Settings were modified externally.\nOverwrite settings?",
                                         "Overwrite",
                                         "Cancel",
                                         false
                                        );
        }
        if (saveSettings)
        {
          Settings.save();
        }

        synchronized(Settings.showRestartAfterConfigChanged)
        {
          if (Settings.showRestartAfterConfigChanged)
          {
// NYI ??? return showRestartAfterConfigChanged flag?
            if (Dialogs.confirm(shell,
                                "Confirmation",
                                "Some settings may become active only after restarting Onzen.\nRestart now?",
                                "Now",
                                "Later",
                                true
                               )
               )
            {
              Widgets.notify(shell,SWT.Close,Onzen.EXITCODE_RESTART);
            }
          }
        }
      }
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "Preferences {}";
  }

  //-----------------------------------------------------------------------

  /** add shortcut keys to keys list
   */
  private void addKeys()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterKey.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterKey.class.isAssignableFrom(configValue.type()))
              {
                // get key
                int key = field.getInt(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // convert to string
                String string = (String)settingValueAdapter.toString(field.get(null));

                // add entry
                Widgets.addTableEntry(widgetKeys,new int[]{key},name.substring(3),string);
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  /** add colors to colors list
   */
  private void addColors()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterColor.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterColor.class.isAssignableFrom(configValue.type()))
              {
                // get color
                Settings.Color color = (Settings.Color)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // add entry
                color = color.clone();
                Widgets.addTableEntry(widgetColors,color,name.substring(5));   // Note: remove prefix "Color"
                if (color.foreground != null) Widgets.setTableEntryColor(widgetColors,color,1,new Color(null,color.foreground));
                if (color.background != null) Widgets.setTableEntryColor(widgetColors,color,2,new Color(null,color.background));
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  /** add fonts to fonts list
   */
  private void addFonts()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterFontData.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterFontData.class.isAssignableFrom(configValue.type()))
              {
                // get font data
                FontData fontData = (FontData)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // convert to string
                String string = (String)settingValueAdapter.toString(field.get(null));

                // add entry
                fontData = (fontData != null) ? new FontData(fontData.getName(),(int)fontData.getHeight(),fontData.getStyle()) : new FontData();
                Widgets.addTableEntry(widgetFonts,fontData,name.substring(4),string);
                Widgets.setTableEntryFont(widgetFonts,fontData,1,fontData);
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  private void saveKeys()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterKey.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterKey.class.isAssignableFrom(configValue.type()))
              {
                // get key
                int key = field.getInt(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // set key
                name = name.substring(3);
                boolean found = false;
                for (TableItem tableItem : widgetKeys.getItems())
                {
                  if (name.equals(tableItem.getText(0)))
                  {
                    field.setInt(null,((int[])tableItem.getData())[0]);
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  Onzen.printInternalError("Key %s not found in table!",name);
                }
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  private void saveColors()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterColor.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterColor.class.isAssignableFrom(configValue.type()))
              {
                // get color
                Settings.Color color = (Settings.Color)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // set color
                name = name.substring(5);
                boolean found = false;
                for (TableItem tableItem : widgetColors.getItems())
                {
                  if (name.equals(tableItem.getText(0)))
                  {
                    field.set(null,tableItem.getData());
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  Onzen.printInternalError("Color %s not found in table!",name);
                }
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  private void saveFonts()
  {
    try
    {
      // instantiate config adapter class
      Constructor         constructor         = Settings.SettingValueAdapterFontData.class.getDeclaredConstructor(Settings.class);
      SettingValueAdapter settingValueAdapter = (SettingValueAdapter)constructor.newInstance(new Settings());

      // get setting classes
      Class[] settingClasses = Settings.getSettingClasses();
      for (Class clazz : settingClasses)
      {
        for (Field field : clazz.getDeclaredFields())
        {
          for (Annotation annotation : field.getDeclaredAnnotations())
          {
            if      (annotation instanceof SettingValue)
            {
              SettingValue configValue = (SettingValue)annotation;
              if (Settings.SettingValueAdapterFontData.class.isAssignableFrom(configValue.type()))
              {
                // get font data
                FontData fontData = (FontData)field.get(null);

                // get name
                String name = (!configValue.name().isEmpty()) ? configValue.name() : field.getName();

                // set font
                name = name.substring(4);
                boolean found = false;
                for (TableItem tableItem : widgetFonts.getItems())
                {
                  if (name.equals(tableItem.getText(0)))
                  {
                    field.set(null,tableItem.getData());
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  Onzen.printInternalError("Font %s not found in table!",name);
                }
              }
            }
          }
        }
      }
    }
    catch (Exception exception)
    {
      // cannot happen
      Onzen.printInternalError(exception);
    }
  }

  /** get editors array from widget
   * @return editors array
   */
  private Settings.Editor[] getEditors()
  {
    Settings.Editor[] editors = new Settings.Editor[widgetEditors.getItemCount()];
    for (int z = 0; z < widgetEditors.getItemCount(); z++)
    {
      editors[z] = (Settings.Editor)(widgetEditors.getItem(z).getData());
    }

    return editors;
  }

  /** get shell commands array from widget
   * @return shell commands array
   */
  private Settings.ShellCommand[] getShellCommands()
  {
    Settings.ShellCommand[] shellCommands = new Settings.ShellCommand[widgetShellCommands.getItemCount()];
    for (int z = 0; z < widgetShellCommands.getItemCount(); z++)
    {
      shellCommands[z] = (Settings.ShellCommand)(widgetShellCommands.getItem(z).getData());
    }

    Arrays.sort(shellCommands,new Comparator<Settings.ShellCommand>()
    {
      public int compare(Settings.ShellCommand shellCommand1, Settings.ShellCommand shellCommand2)
      {
        return shellCommand1.name.compareTo(shellCommand2.name);
      }
    });

    return shellCommands;
  }

  /** get skip whitepace-check file pattern array from widget
   * @return skip whitespae-check file pattern array
   */
  private Settings.FilePattern[] getSkipWhitespaceCheckFilePatterns()
  {
    Settings.FilePattern[] filePatterns = new Settings.FilePattern[widgetSkipWhitespaceCheckFilePatterns.getItemCount()];
    for (int z = 0; z < widgetSkipWhitespaceCheckFilePatterns.getItemCount(); z++)
    {
      filePatterns[z] = new Settings.FilePattern(widgetSkipWhitespaceCheckFilePatterns.getItem(z));
    }

    return filePatterns;
  }

  /** get hidden file pattern array from widget
   * @return hidden file pattern array
   */
  private Settings.FilePattern[] getHiddenFilePatterns()
  {
    Settings.FilePattern[] filePatterns = new Settings.FilePattern[widgetHiddenFilePatterns.getItemCount()];
    for (int z = 0; z < widgetHiddenFilePatterns.getItemCount(); z++)
    {
      filePatterns[z] = new Settings.FilePattern(widgetHiddenFilePatterns.getItem(z));
    }

    return filePatterns;
  }

  /** get hidden directory pattern array from widget
   * @return hidden directory pattern array
   */
  private Settings.FilePattern[] getHiddenDirectoryPatterns()
  {
    Settings.FilePattern[] filePatterns = new Settings.FilePattern[widgetHiddenDirectoryPatterns.getItemCount()];
    for (int z = 0; z < widgetHiddenDirectoryPatterns.getItemCount(); z++)
    {
      filePatterns[z] = new Settings.FilePattern(widgetHiddenDirectoryPatterns.getItem(z));
    }

    return filePatterns;
  }

  /** get auto-summary pattern array from widget
   * @return auto-summary pattern array
   */
  private String[] getAutoSummaryPatterns()
  {
    String[] autoSummaryPatterns = new String[widgetAutoSummaryPatterns.getItemCount()];
    for (int z = 0; z < widgetAutoSummaryPatterns.getItemCount(); z++)
    {
      autoSummaryPatterns[z] = widgetAutoSummaryPatterns.getItem(z);
    }

    return autoSummaryPatterns;
  }

  /** edit keyboard shortcut
   * @param key key code
   * @return new key code
   */
  private int editKey(String name, int key)
  {
    /** dialog data
     */
    class Data
    {
      int key;

      Data()
      {
        this.key = 0;
      }
    };

    final Data data = new Data();

    Composite  composite,subComposite;
    Label      label;
    Text       text;
    Button     button;

    // set data
    data.key = key;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(this.dialog,"Edit keyboard shortcut",300,SWT.DEFAULT,new double[]{1.0,0.0,0.0},1.0);

    final Text   widgetKey;
    final Button widgetSave;
    Listener     keyFilter;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      text = Widgets.newStringView(composite);
      text.setText(name);
      Widgets.layout(text,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Key:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        widgetKey = Widgets.newStringView(subComposite);
        widgetKey.setText(Widgets.menuAcceleratorToText(key));
        Widgets.layout(widgetKey,0,0,TableLayoutData.WE);

        button = Widgets.newButton(subComposite,"Clear");
        Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            data.key = 0;
            widgetKey.setText("");
          }
        });
      }
    }

    label = Widgets.newLabel(dialog,"Note: type key, then select 'Save'");
    Widgets.layout(label,1,0,TableLayoutData.DEFAULT);

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,2,0,TableLayoutData.WE,0,0,4);
    {
      widgetSave = Widgets.newButton(composite,"Save");
      Widgets.layout(widgetSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    keyFilter = new Listener()
    {
      public void handleEvent(Event event)
      {
        data.key = event.stateMask+event.keyCode;
        widgetKey.setText(Widgets.menuAcceleratorToText(data.key));
      }
    };
    display.addFilter(SWT.KeyDown,keyFilter);

    if ((Boolean)Dialogs.run(dialog,false))
    {
      display.removeFilter(SWT.KeyDown,keyFilter);

      return data.key;
    }
    else
    {
      display.removeFilter(SWT.KeyDown,keyFilter);
      return key;
    }
  }

  /** edit color
   * @param name name
   * @param color color
   * @return true if edit OK, false on cancel
   */
  private boolean editColor(String name, final Settings.Color color)
  {
    Composite composite,subComposite;
    Label     label;
    Text      text;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(this.dialog,"Edit color",100,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Canvas widgetColorForeground;
    final Canvas widgetColorBackground;
    final Label  widgetValueForeground;
    final Label  widgetValueBackground;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      text = Widgets.newStringView(composite);
      text.setText(name);
      Widgets.layout(text,0,1,TableLayoutData.WE);

      label = Widgets.newLabel(composite,"Foreground:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        widgetColorForeground = Widgets.newCanvas(subComposite,SWT.BORDER);
        widgetColorForeground.setForeground((color.foreground != null) ? new Color(null,color.foreground) : null);
        widgetColorForeground.setBackground((color.foreground != null) ? new Color(null,color.foreground) : null);
        Widgets.layout(widgetColorForeground,0,0,TableLayoutData.WE,0,0,0,0,60,20);
        String colorName = (color.foreground != null)
                             ? String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue)
                             : "";
        widgetValueForeground = Widgets.newLabel(subComposite,colorName);
        Widgets.layout(widgetValueForeground,0,1,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetColorForeground.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
            ColorDialog colorDialog = new ColorDialog(dialog);
            colorDialog.setRGB(color.foreground);
            RGB rgb = colorDialog.open();
            if (rgb != null)
            {
              color.foreground = rgb;
              widgetColorForeground.setForeground(new Color(null,rgb));
              widgetColorForeground.setBackground(new Color(null,rgb));
              widgetValueForeground.setText(String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue));
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Background:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,2,1,TableLayoutData.WE);
      {
        widgetColorBackground = Widgets.newCanvas(subComposite,SWT.BORDER);
        widgetColorBackground.setForeground((color.background != null) ? new Color(null,color.background) : null);
        widgetColorBackground.setBackground((color.background != null) ? new Color(null,color.background) : null);
        Widgets.layout(widgetColorBackground,0,0,TableLayoutData.WE,0,0,0,0,60,20);
        String colorName = (color.background != null)
                             ? String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue)
                             : "";
        widgetValueBackground = Widgets.newLabel(subComposite,colorName);
        Widgets.layout(widgetValueBackground,0,1,TableLayoutData.W,0,0,0,0,60,SWT.DEFAULT);
        widgetColorBackground.addMouseListener(new MouseListener()
        {
          public void mouseDoubleClick(MouseEvent mouseEvent)
          {
          }
          public void mouseDown(MouseEvent mouseEvent)
          {
          }
          public void mouseUp(MouseEvent mouseEvent)
          {
            ColorDialog colorDialog = new ColorDialog(dialog);
            colorDialog.setRGB(color.background);
            RGB rgb = colorDialog.open();
            if (rgb != null)
            {
              color.background = rgb;
              widgetColorBackground.setForeground(new Color(null,rgb));
              widgetColorBackground.setBackground(new Color(null,rgb));
              widgetValueBackground.setText(String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue));
            }
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save");
      Widgets.layout(button,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Default");
      Widgets.layout(button,0,1,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          color.foreground = color.DEFAULT_FOREGROUND;
          color.background = color.DEFAULT_BACKGROUND;

          widgetColorForeground.setForeground((color.foreground != null) ? new Color(null,color.foreground) : null);
          widgetColorForeground.setBackground((color.foreground != null) ? new Color(null,color.foreground) : null);
          widgetColorBackground.setForeground((color.background != null) ? new Color(null,color.background) : null);
          widgetColorBackground.setBackground((color.background != null) ? new Color(null,color.background) : null);

          String foregroundColorName = (color.foreground != null) ? String.format("#%02x%02x%02x",color.foreground.red,color.foreground.green,color.foreground.blue) : "";
          String backgroundColorName = (color.background != null) ? String.format("#%02x%02x%02x",color.background.red,color.background.green,color.background.blue) : "";
          widgetValueForeground.setText(foregroundColorName);
          widgetValueBackground.setText(backgroundColorName);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,2,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners

    return (Boolean)Dialogs.run(dialog,false);
  }

  /** edit editor command
   * @param editor editor command
   * @param title title text
   * @param buttonText button text
   * @return true if edit OK, false on cancel
   */
  private boolean editEditor(final Settings.Editor editor, String title, String buttonText)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(this.dialog,title,500,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetName;
    final Text   widgetMimeType;
    final Text   widgetFileName;
    final Text   widgetCommandLine;
    final Button widgetAddSave;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);

      widgetName = Widgets.newText(composite);
      widgetName.setText(editor.name);
      Widgets.layout(widgetName,0,1,TableLayoutData.WE);
      widgetName.setToolTipText("Name of editor or empty.\n");

      label = Widgets.newLabel(composite,"Mime type:");
      Widgets.layout(label,1,0,TableLayoutData.W);

      widgetMimeType = Widgets.newText(composite);
      widgetMimeType.setText(editor.mimeType);
      Widgets.layout(widgetMimeType,1,1,TableLayoutData.WE);
      widgetMimeType.setToolTipText("Mime type pattern. Format: <type>/<sub-type>\n");

      label = Widgets.newLabel(composite,"File name:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetFileName = Widgets.newText(composite);
      widgetFileName.setText(editor.fileName);
      Widgets.layout(widgetFileName,2,1,TableLayoutData.WE);
      widgetFileName.setToolTipText("Simple file file name pattern, e. g. *.pdf.\n");

      label = Widgets.newLabel(composite,"Command:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,3,1,TableLayoutData.WE);
      {
        widgetCommandLine = Widgets.newText(subComposite);
        widgetCommandLine.setText(editor.commandLine);
        Widgets.layout(widgetCommandLine,0,0,TableLayoutData.WE);
        widgetCommandLine.setToolTipText("Command to run.\nMacros:\n  %file% - file name\n  %n% - line number\n  %% - %");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String fileName = Dialogs.fileOpen(shell,
                                               "Select program",
                                               widgetCommandLine.getText(),
                                               new String[]{"All files","*",
                                                           }
                                              );
            if (fileName != null)
            {
              widgetCommandLine.setText(fileName);
            }
          }
        });
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetAddSave = Widgets.newButton(composite,buttonText);
      Widgets.layout(widgetAddSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetAddSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          editor.name        = widgetName.getText().trim();
          editor.mimeType    = widgetMimeType.getText().trim();
          editor.fileName    = widgetFileName.getText().trim();
          editor.commandLine = widgetCommandLine.getText().trim();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    Widgets.setNextFocus(widgetName,widgetMimeType);
    Widgets.setNextFocus(widgetMimeType,widgetFileName);
    Widgets.setNextFocus(widgetFileName,widgetCommandLine);
    Widgets.setNextFocus(widgetCommandLine,widgetAddSave);

    // run dialog
    Widgets.setFocus(widgetName);
    return (Boolean)Dialogs.run(dialog,false);
  }

  /** edit shell command
   * @param editor editor command
   * @return true if edit OK, false on cancel
   */
  private boolean editShellCommand(final Settings.ShellCommand shellCommand, String title, String buttonText)
  {
    Composite composite,subComposite;
    Label     label;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.openModal(this.dialog,title,300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text    widgetName;
    final Text    widgetCommandLine;
    final Spinner widgetValidExitcode;
    final Button  widgetAddSave;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Name:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      widgetName = Widgets.newText(composite);
      widgetName.setText(shellCommand.name);
      Widgets.layout(widgetName,0,1,TableLayoutData.WE);
      widgetName.setToolTipText("Name of command");

      label = Widgets.newLabel(composite,"Command:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0}));
      Widgets.layout(subComposite,1,1,TableLayoutData.WE);
      {
        widgetCommandLine = Widgets.newText(subComposite);
        widgetCommandLine.setText(shellCommand.commandLine);
        Widgets.layout(widgetCommandLine,0,0,TableLayoutData.WE);
        widgetCommandLine.setToolTipText("Command to run.\nMacros:\n  %file% - file name\n  %directory% - directory name\n  %n% - line number\n  %% - %");

        button = Widgets.newButton(subComposite,Onzen.IMAGE_DIRECTORY);
        Widgets.layout(button,0,1,TableLayoutData.DEFAULT);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            String command = Dialogs.fileOpen(shell,
                                              "Select command",
                                              widgetCommandLine.getText(),
                                              new String[]{"All files",  Onzen.ALL_FILE_EXTENSION,
                                                           "Scripts",    "*.sh",
                                                           "Batch files","*.cmd",
                                                           "Executables","*.exe"
                                                          }
                                             );
            if (command != null)
            {
              widgetCommandLine.setText(command);
            }
          }
        });
      }

      label = Widgets.newLabel(composite,"Valid exitcode:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      widgetValidExitcode = Widgets.newSpinner(composite);
      widgetValidExitcode.setMinimum(0);
      widgetValidExitcode.setMaximum(255);
      Widgets.layout(widgetValidExitcode,2,1,TableLayoutData.W);
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      widgetAddSave = Widgets.newButton(composite,buttonText);
      Widgets.layout(widgetAddSave,0,0,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetAddSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          shellCommand.name          = widgetName.getText().trim();
          shellCommand.commandLine   = widgetCommandLine.getText();
          shellCommand.validExitcode = widgetValidExitcode.getSelection();

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Cancel");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetName.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetCommandLine.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetCommandLine.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetAddSave.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    // run dialog
    Widgets.setFocus(widgetName);
    return (Boolean)Dialogs.run(dialog,false);
  }

  private void sendTestMail(String        mailSMTPHost,
                            int           mailSMTPPort,
                            boolean       mailSMTPSSL,
                            final String  mailLogin,
                            final String  mailPassword,
                            String        mailFrom,
                            String        toAddress
                           )
  {
    final String TEST_MAIL_TEMPLATE =
    "Test mail:\n" +
    "\n" +
    "  SMTP host: ${SMTPHost}\n" +
    "  SMTP port: ${SMTPPort}\n" +
    "  SMTP SSL: ${SMTPSSL}\n" +
    "  Login name: ${login}\n" +
    "  Mail from: ${from}\n" +
    "\n" +
    "";

    // create mail text
    Macro macro = new Macro(TEST_MAIL_TEMPLATE);
    macro.expand("SMTPHost", mailSMTPHost                  );
    macro.expand("SMTPPort", Integer.toString(mailSMTPPort));
    macro.expand("SMTPSSL",  mailSMTPSSL ? "yes" : "no"    );
    macro.expand("login",    mailLogin                     );
    macro.expand("from",     mailFrom                      );

    try
    {
      // create mail session
      Properties properties = new Properties();
      properties.put("mail.transport.protocol","smtp");
      properties.put("mail.smtp.host",mailSMTPHost);
      properties.put("mail.smtp.port",Integer.toString(mailSMTPPort));
      if (!mailFrom.isEmpty()) properties.put("mail.from",mailFrom);
//properties.put("mail.smtp.starttls.enable","true");
      properties.put("mail.smtp.auth",(mailPassword != null) && !mailPassword.isEmpty());
      if (mailSMTPSSL)
      {
        properties.put("mail.smtp.socketFactory.port",Integer.toString(mailSMTPPort));
        properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback","false");
      }

//properties.put("mail.smtps.starttls.enable","true");
//                  if (Settings.debugFlag) properties.put("mail.debug","true");
      Authenticator auth = new Authenticator()
      {
        public PasswordAuthentication getPasswordAuthentication()
        {
          return new PasswordAuthentication(mailLogin,mailPassword);
        }
      };
      Session session = Session.getInstance(properties,auth);

      // create message
      MimeMultipart mimeMultipart = new MimeMultipart();

      MimeBodyPart text = new MimeBodyPart();
      text.setText(macro.getValue());
      text.setDisposition(MimeBodyPart.INLINE);
      mimeMultipart.addBodyPart(text);

      Message message = new MimeMessage(session);
      if (!mailFrom.isEmpty()) message.setFrom(new InternetAddress(mailFrom));
      message.setSubject("Onzen test mail");
      message.setRecipient(Message.RecipientType.TO,new InternetAddress(toAddress));
      message.setSentDate(new Date());
      message.setContent(mimeMultipart);
      message.saveChanges();

      // send message
      Transport.send(message);

      Dialogs.info(dialog,"Information","Test mail successfully sent!");
    }
    catch (MessagingException exception)
    {
exception.printStackTrace();
Dprintf.dprintf("exception=%s",exception);
      Dialogs.error(dialog,"Cannot send test mail (error: %s)",exception.getMessage());
    }
  }
}

/* end of file */
