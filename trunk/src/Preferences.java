/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandView.java,v $
* $Revision: 1.5 $
* $Author: torsten $
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
import java.util.HashSet;

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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FontDialog;
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
  private TabFolder           widgetTabFolder;
  private final Table         widgetKeys;
  private final Table         widgetColors;
  private final Table         widgetFonts;

  private final Table         widgetEditors;
  private final Text          widgetCommandMail;
  private final Text          widgetCommandMailAttachment;

  private final Text          widgetMailSMTPHost;
  private final Spinner       widgetMailSMTPPort;
  private final Button        widgetMailSMTPSSL;
  private final Text          widgetMailLogin;
  private final Text          widgetMailPassword;
  private final Text          widgetMailFrom;

  private final Text          widgetReviewServerHost;
  private final Text          widgetReviewServerLogin;
  private final Text          widgetCommandPostReviewServer;

  private final Text          widgetCVSCommand;
  private final Button        widgetCVSPruneEmptyDirectories;

  private final Text          widgetSVNCommand;
  private final Text          widgetSVNDiffCommand;
  private final Text          widgetSVNDiffCommandOptions;
  private final Text          widgetSVNDiffCommandOptionsIgnoreWhitespaces;

  private final Text          widgetHGCommand;
  private final Text          widgetHGDiffCommand;
  private final Text          widgetHGDiffCommandOptions;
  private final Text          widgetHGDiffCommandOptionsIgnoreWhitespaces;
  private final Button        widgetHGUseForestExtension;
  private final Button        widgetHGUpdateWithFetch;
  private final Button        widgetHGSafeUpdate;
  private final Button        widgetHGSingleLineCommitMessages;
  private final Spinner       widgetHGSingleLineMaxCommitMessageLength;
  private final Button        widgetHGRelativePatchPaths;

  private final Text          widgetGitCommand;

  private final Text          widgetTmpDirectory;
  private final Text          widgetDateFormat;
  private final Text          widgetTimeFormat;
  private final Text          widgetDateTimeFormat;
  private final Spinner       widgetMaxBackgroundTasks;
  private final Spinner       widgetMaxMessageHistory;
  private final Text          widgetMessageBroadcastAddress;
  private final Spinner       widgetMessageBroadcastPort;
  private final List          widgetHiddenFilePatterns;
  private final List          widgetHiddenDirectoryPatterns;

  private final Button        widgetButtonSave;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view command
   * @param shell shell
   * @param onzen Onzen instance
   */
  Preferences(final Shell shell, final Onzen onzen)
  {
    TabFolder tabFolder;
    Composite composite,subComposite,subSubComposite,subSubSubComposite;
    Label     label;
    Button    button;
    Listener  listener;

    // initialize variables
    this.shell = shell;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Preferences",new double[]{1.0,0.0},1.0);

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
        Widgets.addTableColumn(widgetKeys,0,"Name",SWT.LEFT,200,true);
        Widgets.addTableColumn(widgetKeys,1,"Key", SWT.LEFT,100,true);
        widgetKeys.setToolTipText("Keyboard short cuts list.");
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
        addKeys();
      }

      composite = Widgets.addTab(tabFolder,"Colors");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,1,TableLayoutData.NSWE);
      {
        widgetColors = Widgets.newTable(composite);
        Widgets.layout(widgetColors,0,0,TableLayoutData.NSWE);
        Widgets.addTableColumn(widgetColors,0,"Name", SWT.LEFT,320,true);
        Widgets.addTableColumn(widgetColors,1,"Foreground",SWT.LEFT,100,true);
        Widgets.addTableColumn(widgetColors,2,"Background",SWT.LEFT,100,true);
        widgetColors.setToolTipText("Colors list.");
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
                Widgets.setTableEntryColor(widgetColors,color,1,new Color(null,color.foreground));
                Widgets.setTableEntryColor(widgetColors,color,2,new Color(null,color.background));
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
        addColors();
      }

      composite = Widgets.addTab(tabFolder,"Fonts");
      composite.setLayout(new TableLayout(1.0,1.0,2));
      Widgets.layout(composite,0,2,TableLayoutData.NSWE);
      {
        widgetFonts = Widgets.newTable(composite);
        Widgets.layout(widgetFonts,0,0,TableLayoutData.NSWE);
        Widgets.addTableColumn(widgetFonts,0,"Name",SWT.LEFT,200,true);
        Widgets.addTableColumn(widgetFonts,1,"Font",SWT.LEFT,100,true);
        widgetFonts.setToolTipText("Fonts list.");
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
                fontData.name   = newFontData.name;
                fontData.height = newFontData.height;
                fontData.style  = newFontData.style;
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
        addFonts();
      }

      composite = Widgets.addTab(tabFolder,"Commands");
      composite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
      Widgets.layout(composite,0,3,TableLayoutData.NSWE);
      {
        widgetEditors = Widgets.newTable(composite);
        Widgets.layout(widgetEditors,0,0,TableLayoutData.NSWE);
        Widgets.addTableColumn(widgetEditors,0,"Mime type",SWT.LEFT,200,true);
        Widgets.addTableColumn(widgetEditors,1,"Command",  SWT.LEFT,100,true);
        widgetEditors.setToolTipText("Colors list.");
        widgetEditors.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Table widget = (Table)selectionEvent.widget;

            int index = widget.getSelectionIndex();
            if (index >= 0)
            {
              Settings.Editor editor = (Settings.Editor)(widget.getItem(index).getData());

              if (editEditor(editor,"Edit editor","Save"))
              {
                Widgets.updateTableEntry(widgetEditors,editor,editor.mimeTypePattern,editor.command);
              }
            }
          }
        });
        for (Settings.Editor editor : Settings.editors)
        {
          Widgets.addTableEntry(widgetEditors,editor.clone(),editor.mimeTypePattern,editor.command);
        }

        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,null));
        Widgets.layout(subComposite,1,0,TableLayoutData.E);
        {
          button = Widgets.newButton(subComposite,"Add");
          Widgets.layout(button,0,0,TableLayoutData.E);
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
                Widgets.addTableEntry(widgetEditors,editor,editor.mimeTypePattern,editor.command);
              }
            }
          });
          button = Widgets.newButton(subComposite,"Remove");
          Widgets.layout(button,0,1,TableLayoutData.E);
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

        subComposite = Widgets.newGroup(composite,"External Mail");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,2,0,TableLayoutData.WE);
        {
          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,0,0,TableLayoutData.W);
          widgetCommandMail = Widgets.newText(subComposite);
          widgetCommandMail.setText(Settings.commandMail);
          Widgets.layout(widgetCommandMail,0,1,TableLayoutData.WE);
          widgetCommandMail.setToolTipText("External mail command.\nMacros:\n  ${to} - to address\n  ${cc} - CC address\n  ${subject} - subject line\n");

          label = Widgets.newLabel(subComposite,"Command with attachment:");
          Widgets.layout(label,1,0,TableLayoutData.W);
          widgetCommandMailAttachment = Widgets.newText(subComposite);
          widgetCommandMailAttachment.setText(Settings.commandMailAttachment);
          Widgets.layout(widgetCommandMailAttachment,1,1,TableLayoutData.WE);
          widgetCommandMailAttachment.setToolTipText("External mail command with an attachment.\nMacros:\n  ${to} - to address\n  ${cc} - CC address\n  ${subject} - subject line\n  ${file} - attachment file name");
        }

        subComposite = Widgets.newGroup(composite,"SMTP Mail");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,3,0,TableLayoutData.WE);
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
            String password = onzen.getPassword(Settings.mailLogin,Settings.mailSMTPHost);
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
        }

        subComposite = Widgets.newGroup(composite,"Post review server");
        subComposite.setLayout(new TableLayout(null,new double[]{0.0,1.0}));
        Widgets.layout(subComposite,4,0,TableLayoutData.WE);
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

          label = Widgets.newLabel(subComposite,"Command:");
          Widgets.layout(label,2,0,TableLayoutData.W);
          widgetCommandPostReviewServer = Widgets.newText(subComposite);
          widgetCommandPostReviewServer.setText(Settings.commandPostReviewServer);
          Widgets.layout(widgetCommandPostReviewServer,2,1,TableLayoutData.WE);
          widgetCommandPostReviewServer.setToolTipText("Post review server command.\nMacros:\n  ${server} - review server name\n  ${login} - login name\n  ${password} - password\n  ${summary} - summary line\n  ${description} - description\n  ${tests} - tests done\n  ${file} - diff file name\n");
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
                                                  new String[]{"All files",  "*",
                                                               "Scripts",    "*.sh",
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
                                                  new String[]{"All files",  "*",
                                                               "Scripts",    "*.sh",
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
                                                  new String[]{"All files",  "*",
                                                               "Scripts",    "*.sh",
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

            label = Widgets.newLabel(subSubComposite,"No white-spaces:");
            Widgets.layout(label,0,2,TableLayoutData.W);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces = Widgets.newText(subSubComposite);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces.setText(Settings.svnDiffCommandOptionsIgnoreWhitespaces);
            Widgets.layout(widgetSVNDiffCommandOptionsIgnoreWhitespaces,0,3,TableLayoutData.WE);
            widgetSVNDiffCommandOptionsIgnoreWhitespaces.setToolTipText("Options for external diff command with ignoring white-space changes.");
          }
        }

        // --- hg ------------------------------------------------------

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
                                                  new String[]{"All files",  "*",
                                                               "Scripts",    "*.sh",
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
                                                  new String[]{"All files",  "*",
                                                               "Scripts",    "*.sh",
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

            label = Widgets.newLabel(subSubComposite,"No white-spaces:");
            Widgets.layout(label,0,2,TableLayoutData.W);
            widgetHGDiffCommandOptionsIgnoreWhitespaces = Widgets.newText(subSubComposite);
            widgetHGDiffCommandOptionsIgnoreWhitespaces.setText(Settings.hgDiffCommandOptionsIgnoreWhitespaces);
            Widgets.layout(widgetHGDiffCommandOptionsIgnoreWhitespaces,0,3,TableLayoutData.WE);
            widgetHGDiffCommandOptionsIgnoreWhitespaces.setToolTipText("Options for external diff command with ignoring white-space changes.");
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

            widgetHGUpdateWithFetch = Widgets.newCheckbox(subSubComposite,"update with fetch extension");
            widgetHGUpdateWithFetch.setSelection(Settings.hgUpdateWithFetch);
            Widgets.layout(widgetHGUpdateWithFetch,1,0,TableLayoutData.WE);
            widgetHGUpdateWithFetch.setToolTipText("Use HG fetch extension for update (fetch+fpush).");

            widgetHGSafeUpdate = Widgets.newCheckbox(subSubComposite,"'safe' update");
            widgetHGSafeUpdate.setSelection(Settings.hgSafeUpdate);
            Widgets.layout(widgetHGSafeUpdate,2,0,TableLayoutData.WE);
            widgetHGSafeUpdate.setToolTipText("Do 'safe' update. Allow fetch update with not-commited local changes: save local changes, revert, update and restore local changes with merge if needed.");

            subSubSubComposite = Widgets.newComposite(subSubComposite);
            subSubSubComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,0.0}));
            Widgets.layout(subSubSubComposite,3,0,TableLayoutData.WE);
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
              Widgets.addModifyListener(new WidgetListener(widgetHGSingleLineMaxCommitMessageLength,Settings.hgSingleLineCommitMessages)
              {
                public void modified(Control control)
                {
                  control.setEnabled(widgetHGSingleLineCommitMessages.getSelection());
                }
              });
            }

            widgetHGRelativePatchPaths = Widgets.newCheckbox(subSubComposite,"relative patch paths");
            widgetHGRelativePatchPaths.setSelection(Settings.hgRelativePatchPaths);
            Widgets.layout(widgetHGRelativePatchPaths,4,0,TableLayoutData.WE);
            widgetHGRelativePatchPaths.setToolTipText("Remove path prefixes 'a/' and 'b/' Mercurial is adding to file names in patches and convert to a relative path.");
          }
        }

        // --- Git -----------------------------------------------------

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
            widgetGitCommand.setToolTipText("Git command.");

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
                                                  new String[]{"*","All files","*.sh","Scripts","*.exe","Executables"}
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

      composite = Widgets.addTab(tabFolder,"Misc");
      composite.setLayout(new TableLayout(new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0},new double[]{0.0,1.0},2));
      Widgets.layout(composite,0,5,TableLayoutData.NSWE);
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

        label = Widgets.newLabel(composite,"Date format:");
        Widgets.layout(label,1,0,TableLayoutData.W);
        widgetDateFormat = Widgets.newText(composite);
        widgetDateFormat.setText(Settings.dateFormat);
        Widgets.layout(widgetDateFormat,1,1,TableLayoutData.WE);
        widgetDateFormat.setToolTipText("Date format.\nPattens:\n  y - year digit\n  M - month digit\n  d - day digit");

        label = Widgets.newLabel(composite,"Time format:");
        Widgets.layout(label,2,0,TableLayoutData.W);
        widgetTimeFormat = Widgets.newText(composite);
        widgetTimeFormat.setText(Settings.timeFormat);
        Widgets.layout(widgetTimeFormat,2,1,TableLayoutData.WE);
        widgetTimeFormat.setToolTipText("Time format.\nPattens:\n  H - hour digit\n  m - minute digit\n  s - second digit");

        label = Widgets.newLabel(composite,"Date/Time format:");
        Widgets.layout(label,3,0,TableLayoutData.W);
        widgetDateTimeFormat = Widgets.newText(composite);
        widgetDateTimeFormat.setText(Settings.dateTimeFormat);
        Widgets.layout(widgetDateTimeFormat,3,1,TableLayoutData.WE);
        widgetDateTimeFormat.setToolTipText("Date/time format.\nPattens:\n  y - year digit\n  M - month digit\n  d - day digit\n  H - hour digit\n  m - minute digit\n  s - second digit");

        label = Widgets.newLabel(composite,"Max. background tasks:");
        Widgets.layout(label,4,0,TableLayoutData.W);
        widgetMaxBackgroundTasks = Widgets.newSpinner(composite,1,256);
        widgetMaxBackgroundTasks.setSelection(Settings.maxBackgroundTasks);
        Widgets.layout(widgetMaxBackgroundTasks,4,1,TableLayoutData.W);
        widgetMaxBackgroundTasks.setToolTipText("Max. number of background tasks.");

        label = Widgets.newLabel(composite,"Max. message history:");
        Widgets.layout(label,5,0,TableLayoutData.W);
        widgetMaxMessageHistory = Widgets.newSpinner(composite,0);
        widgetMaxMessageHistory.setSelection(Settings.maxMessageHistory);
        Widgets.layout(widgetMaxMessageHistory,5,1,TableLayoutData.W);
        widgetMaxMessageHistory.setToolTipText("Max. length of commit message history.");

        label = Widgets.newLabel(composite,"Message broadcast:");
        Widgets.layout(label,6,0,TableLayoutData.W);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(null,new double[]{1.0,0.0,0.0}));
        Widgets.layout(subComposite,6,1,TableLayoutData.WE);
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

        label = Widgets.newLabel(composite,"Hidden files:");
        Widgets.layout(label,7,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,7,1,TableLayoutData.NSWE);
        {
          widgetHiddenFilePatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetHiddenFilePatterns,0,0,TableLayoutData.NSWE);
          widgetHiddenFilePatterns.setToolTipText("Patterns for hidden files in tree view.");
          widgetHiddenFilePatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              Table widget = (Table)mouseEvent.widget;

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
          for (Settings.FilePattern filePattern : Settings.hiddenFilePatterns)
          {
            widgetHiddenFilePatterns.add(filePattern.string);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(null,null));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.E);
          {
            button = Widgets.newButton(subSubComposite,"Add");
            Widgets.layout(button,0,0,TableLayoutData.E);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String string = Dialogs.string(dialog,"Add file pattern","Pattern:","","Add","Cancel");
                if (string != null)
                {
                  widgetHiddenFilePatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E);
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
        Widgets.layout(label,8,0,TableLayoutData.NW);
        subComposite = Widgets.newComposite(composite);
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0));
        Widgets.layout(subComposite,8,1,TableLayoutData.NSWE);
        {
          widgetHiddenDirectoryPatterns = Widgets.newList(subComposite);
          Widgets.layout(widgetHiddenDirectoryPatterns,0,0,TableLayoutData.NSWE);
          widgetHiddenDirectoryPatterns.setToolTipText("Patterns for hidden directories in tree view.");
          widgetHiddenDirectoryPatterns.addMouseListener(new MouseListener()
          {
            public void mouseDoubleClick(MouseEvent mouseEvent)
            {
              Table widget = (Table)mouseEvent.widget;

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
            Widgets.layout(button,0,0,TableLayoutData.E);
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                String string = Dialogs.string(dialog,"Add directory pattern","Pattern:","","Add","Cancel");
                if (string != null)
                {
                  widgetHiddenDirectoryPatterns.add(string);
                }
              }
            });
            button = Widgets.newButton(subSubComposite,"Remove");
            Widgets.layout(button,0,1,TableLayoutData.E);
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

          Settings.cvsCommand                             = widgetCVSCommand.getText().trim();
          Settings.cvsPruneEmtpyDirectories               = widgetCVSPruneEmptyDirectories.getSelection();

          Settings.svnCommand                             = widgetSVNCommand.getText().trim();
          Settings.svnDiffCommand                         = widgetSVNDiffCommand.getText().trim();
          Settings.svnDiffCommandOptions                  = widgetSVNDiffCommandOptions.getText().trim();
          Settings.svnDiffCommandOptionsIgnoreWhitespaces = widgetSVNDiffCommandOptionsIgnoreWhitespaces.getText().trim();

          Settings.hgCommand                              = widgetHGCommand.getText().trim();
          Settings.hgDiffCommand                          = widgetHGDiffCommand.getText().trim();
          Settings.hgDiffCommandOptions                   = widgetHGDiffCommandOptions.getText().trim();
          Settings.hgDiffCommandOptionsIgnoreWhitespaces  = widgetHGDiffCommandOptionsIgnoreWhitespaces.getText().trim();
          Settings.hgUseForestExtension                   = widgetHGUseForestExtension.getSelection();
          Settings.hgUpdateWithFetch                      = widgetHGUpdateWithFetch.getSelection();
          Settings.hgSafeUpdate                           = widgetHGSafeUpdate.getSelection();
          Settings.hgSingleLineCommitMessages             = widgetHGSingleLineCommitMessages.getSelection();
          Settings.hgSingleLineCommitMessages             = widgetHGSingleLineCommitMessages.getSelection();
          Settings.hgRelativePatchPaths                   = widgetHGRelativePatchPaths.getSelection();

          Settings.gitCommand                             = widgetGitCommand.getText().trim();

          Settings.tmpDirectory                           = widgetTmpDirectory.getText().trim();
          Settings.dateFormat                             = widgetDateFormat.getText().trim();
          Settings.timeFormat                             = widgetTimeFormat.getText().trim();
          Settings.dateTimeFormat                         = widgetDateTimeFormat.getText().trim();
          Settings.maxBackgroundTasks                     = Integer.parseInt(widgetMaxBackgroundTasks.getText());
          Settings.maxMessageHistory                      = Integer.parseInt(widgetMaxMessageHistory.getText());

          Settings.editors                                = getEditors();

          Settings.commandMail                            = widgetCommandMail.getText();
          Settings.commandMailAttachment                  = widgetCommandMailAttachment.getText().trim();

          Settings.mailSMTPHost                           = widgetMailSMTPHost.getText().trim();
          Settings.mailSMTPPort                           = Integer.parseInt(widgetMailSMTPPort.getText());
          Settings.mailSMTPSSL                            = widgetMailSMTPSSL.getSelection();
          Settings.mailLogin                              = widgetMailLogin.getText().trim();
          onzen.setPassword(Settings.mailLogin,Settings.mailSMTPHost,widgetMailPassword.getText());
          Settings.mailFrom                               = widgetMailFrom.getText().trim();

          Settings.reviewServerHost                       = widgetReviewServerHost.getText().trim();
          Settings.reviewServerLogin                      = widgetReviewServerLogin.getText().trim();
//          onzen.setPassword(Settings.reviewServerLogin,Settings.reviewServerHost,widgetReviewServerPassword.getText());
          Settings.commandPostReviewServer                = widgetCommandPostReviewServer.getText().trim();

          Settings.hiddenFilePatterns                     = getHiddenFilePatterns();
          Settings.hiddenDirectoryPatterns                = getHiddenDirectoryPatterns();

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
    Dialogs.show(dialog,Settings.geometryView);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      if ((Boolean)Dialogs.run(dialog,false))
      {
        if (Dialogs.confirm(shell,"Confirmation","Some settings may become active only after restarting Onzen.","Restart now","Cancel"))
        {
          Widgets.notify(shell,SWT.Close,64);
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

                // convert to string
                String string = (String)settingValueAdapter.toString(color);

                // add entry
                color = color.clone();
                Widgets.addTableEntry(widgetColors,color,name.substring(5));
                Widgets.setTableEntryColor(widgetColors,color,1,new Color(null,color.foreground));
                Widgets.setTableEntryColor(widgetColors,color,2,new Color(null,color.background));
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
                fontData = (fontData != null)?new FontData(fontData.name,(int)fontData.height,fontData.style):new FontData();
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
    final Shell dialog = Dialogs.open(this.dialog,"Edit keyboard shortcut",300,SWT.DEFAULT,new double[]{1.0,0.0,0.0},1.0);

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
        Widgets.layout(button,0,1,TableLayoutData.W);
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
    Composite composite;
    Label     label;
    Text      text;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.open(this.dialog,"Edit color",100,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

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
      button = Widgets.newButton(composite);
      button.setBackground(new Color(null,color.foreground));
      Widgets.layout(button,1,1,TableLayoutData.WE,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          ColorDialog colorDialog = new ColorDialog(dialog);
          colorDialog.setRGB(color.foreground);
          RGB rgb = colorDialog.open();
          if (rgb != null)
          {
            color.foreground = rgb;
            widget.setBackground(new Color(null,rgb));
          }
        }
      });

      label = Widgets.newLabel(composite,"Background:");
      Widgets.layout(label,2,0,TableLayoutData.W);
      button = Widgets.newButton(composite);
      button.setBackground(new Color(null,color.background));
      Widgets.layout(button,2,1,TableLayoutData.WE,0,0,0,0,60,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          ColorDialog colorDialog = new ColorDialog(dialog);
          colorDialog.setRGB(color.background);
          RGB rgb = colorDialog.open();
          if (rgb != null)
          {
            color.background = rgb;
            widget.setBackground(new Color(null,rgb));
          }
        }
      });
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

    return (Boolean)Dialogs.run(dialog,false);
  }

  /** edit editor command
   * @param editor editor command
   * @return true if edit OK, false on cancel
   */
  private boolean editEditor(final Settings.Editor editor, String title, String buttonText)
  {
    Composite composite;
    Label     label;
    Button    button;

    // add editor dialog
    final Shell dialog = Dialogs.open(this.dialog,title,300,SWT.DEFAULT,new double[]{1.0,0.0},1.0);

    final Text   widgetMimeTypePattern;
    final Text   widgetCommand;
    final Button widgetAddSave;

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(null,new double[]{0.0,1.0},4));
    Widgets.layout(composite,0,0,TableLayoutData.WE,0,0,4);
    {
      label = Widgets.newLabel(composite,"Mime type:");
      Widgets.layout(label,0,0,TableLayoutData.W);
      widgetMimeTypePattern = Widgets.newText(composite);
      widgetMimeTypePattern.setText(editor.mimeTypePattern);
      Widgets.layout(widgetMimeTypePattern,0,1,TableLayoutData.WE);
      widgetMimeTypePattern.setToolTipText("Mime type pattern. Format: <type>/<sub-type>\n");

      label = Widgets.newLabel(composite,"Command:");
      Widgets.layout(label,1,0,TableLayoutData.W);
      widgetCommand = Widgets.newText(composite);
      widgetCommand.setText(editor.command);
      Widgets.layout(widgetCommand,1,1,TableLayoutData.WE);
      widgetCommand.setToolTipText("Command to run.\nMacros:\n  %file% - file name");
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
          editor.mimeTypePattern = widgetMimeTypePattern.getText();
          editor.command         = widgetCommand.getText();

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
    widgetMimeTypePattern.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetCommand.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetCommand.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        widgetAddSave.setFocus();
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });

    return (Boolean)Dialogs.run(dialog,false);
  }
}

/* end of file */
