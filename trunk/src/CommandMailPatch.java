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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Authenticator;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

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
    Patch[]         history;              // patch history
    String[]        revisionNames;        // revision names
    String[]        lines;                // patch lines
    String[]        linesNoWhitespaces;   // patch lines (without whitespaces)
    String          summary;              // summary for patch
    String[]        message;              // message for patch (without mail prefix/postfix etc.)
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

  // colors
  private final Color COLOR_INACTIVE;
  private final Color COLOR_TEXT;
  private final Color COLOR_FIND_TEXT;

  // user events
  private final int USER_EVENT_ADD_NEW_TEST = 0xFFFF+0;

  // --------------------------- variables --------------------------------
  public String               summary;
  public String[]             message;

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Patch         patch;
  private final Date          date;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final StyledText    widgetPatch;
  private final List          widgetFileNames;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Combo         widgetSummary;
  private final Text          widgetMessage;
  private final Table         widgetTests;
  private final Text          widgetNewTest;
  private final Button        widgetAddNewTest;
  private final Text          widgetMailTo;
  private final Text          widgetMailCC;
  private final Text          widgetMailSubject;
  private final Text          widgetMailText;
  private final Button        widgetSend;
  private final Button        widgetCancel;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** mail patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet file data set
   * @param patch patch
   * @param summary summary text
   * @param message message text lines
   */
  CommandMailPatch(final Shell shell, final RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, final Patch patch, String summary, String[] message)
  {
    Composite composite,subComposite,subSubComposite,subSubSubComposite;
    Label     label;
    TabFolder tabFolder;
    Button    button;
    Menu      menu;
    MenuItem  menuItem;
    Listener  listener;

    // initialize variables
    this.summary       = summary;
    this.message       = message;
    this.repositoryTab = repositoryTab;
    this.patch         = patch;
    this.date          = new Date();

    // get patch history
    data.history = Patch.getPatches(repositoryTab.repository.rootPath,true,EnumSet.allOf(Patch.States.class),50);

    // get display
    display = shell.getDisplay();

    // colors
    COLOR_INACTIVE  = new Color(display,Settings.colorInactive.background);
    COLOR_TEXT      = new Color(display,Settings.colorInactive.background);
    COLOR_FIND_TEXT = new Color(display,Settings.colorFindText.foreground);

    // add files dialog
    dialog = Dialogs.open(shell,"Mail patch",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(1.0,1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,0,0,TableLayoutData.NSWE);
      {
        subComposite = Widgets.addTab(tabFolder,"Patch");
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetPatch = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
          widgetPatch.setBackground(COLOR_TEXT);
          Widgets.layout(widgetPatch,0,0,TableLayoutData.NSWE);

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.WE);
          {
            label = Widgets.newLabel(subSubComposite,"Find:");
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetFind = Widgets.newText(subSubComposite,SWT.SEARCH|SWT.ICON_CANCEL);
            Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

            widgetFindPrev = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_UP);
            Widgets.layout(widgetFindPrev,0,2,TableLayoutData.W);

            widgetFindNext = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_DOWN);
            Widgets.layout(widgetFindNext,0,3,TableLayoutData.W);
          }
        }

        subComposite = Widgets.addTab(tabFolder,"Files");
        subComposite.setLayout(new TableLayout(1.0,new double[]{1.0,0.0},2));
        Widgets.layout(subComposite,0,1,TableLayoutData.NSWE);
        {
          widgetFileNames = Widgets.newList(subComposite);
          widgetFileNames.setBackground(COLOR_INACTIVE);
          Widgets.layout(widgetFileNames,0,0,TableLayoutData.NSWE);
        }
      }

      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,1,0,TableLayoutData.NSWE);
      {
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

            widgetSummary = Widgets.newCombo(subSubComposite);
            widgetSummary.setText(summary);
            Widgets.layout(widgetSummary,0,1,TableLayoutData.WE);

            label = Widgets.newLabel(subSubComposite,"Message:");
            Widgets.layout(label,1,0,TableLayoutData.NW);

            widgetMessage = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
            widgetMessage.setText(StringUtils.join(message,widgetMessage.DELIMITER));
            Widgets.layout(widgetMessage,1,1,TableLayoutData.NSWE);
          }

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(new double[]{0.0,1.0},1.0));
          Widgets.layout(subSubComposite,0,1,TableLayoutData.NSWE);
          {
            label = Widgets.newLabel(subSubComposite,"Tests done:");
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

            menu = Widgets.newPopupMenu(dialog);
            {
              menuItem = Widgets.addMenuItem(menu,"Edit...");
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  int index = widgetTests.getSelectionIndex();
                  if (index >= 0)
                  {
                    TableItem tableItem = widgetTests.getItem(index);

                    String test = Dialogs.string(dialog,"Edit test description","Test:",(String)tableItem.getData());
                    if (test != null)
                    {
                      tableItem.setText(test);
                      tableItem.setData(test);
                      updateMailText();
                    }
                  }
                }
              });

              menuItem = Widgets.addMenuItem(menu,"Move up");
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  int index = widgetTests.getSelectionIndex();
                  if (index >= 0)
                  {
                    if (index > 0)
                    {
                      TableItem tableItem0 = widgetTests.getItem(index-1);
                      TableItem tableItem1 = widgetTests.getItem(index  );

                      String text0 = tableItem0.getText();
                      Object data0 = tableItem0.getData();
                      String text1 = tableItem1.getText();
                      Object data1 = tableItem1.getData();

                      tableItem0.setText(text1);
                      tableItem0.setData(data1);
                      tableItem1.setText(text0);
                      tableItem1.setData(data0);
                      updateMailText();

                      widgetTests.setSelection(index-1);
                    }
                  }
                }
              });

              menuItem = Widgets.addMenuItem(menu,"Move down");
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  int index = widgetTests.getSelectionIndex();
                  if (index >= 0)
                  {
                    if (index < widgetTests.getItemCount()-1)
                    {
                      TableItem tableItem0 = widgetTests.getItem(index  );
                      TableItem tableItem1 = widgetTests.getItem(index+1);

                      String text0 = tableItem0.getText();
                      Object data0 = tableItem0.getData();
                      String text1 = tableItem1.getText();
                      Object data1 = tableItem1.getData();

                      tableItem0.setText(text1);
                      tableItem0.setData(data1);
                      tableItem1.setText(text0);
                      tableItem1.setData(data0);
                      updateMailText();

                      widgetTests.setSelection(index+1);
                    }
                  }
                }
              });

              menuItem = Widgets.addMenuSeparator(menu);

              menuItem = Widgets.addMenuItem(menu,"Remove");
              menuItem.addSelectionListener(new SelectionListener()
              {
                public void widgetDefaultSelected(SelectionEvent selectionEvent)
                {
                }
                public void widgetSelected(SelectionEvent selectionEvent)
                {
                  int index = widgetTests.getSelectionIndex();
                  if (index >= 0)
                  {
                    widgetTests.remove(index);
                    updateMailText();
                  }
                }
              });
            }
            widgetTests.setMenu(menu);
            widgetTests.setToolTipText("Executed tests for patch.\nRight-click to open context menu.");

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
          data.message = StringUtils.split(widgetMessage.getText(),widgetMessage.DELIMITER);

          File tmpFile = null;
          try
          {
            // create patch attachment file
            tmpFile = File.createTempFile("patch",".patch",new File(Settings.tmpDirectory));
            patch.write(tmpFile);

            if      (repositoryTab.repository.mailSMTPHost != null)
            {
              // send mail with JavaMail
//int port = 587;
//int port = 25;
              try
              {
                final String password = repositoryTab.onzen.getPassword(repositoryTab.repository.mailLogin,repositoryTab.repository.mailSMTPHost);

                // mail mime type handlers
                MailcapCommandMap mailcapCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
                mailcapCommandMap.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
                mailcapCommandMap.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
                mailcapCommandMap.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
                mailcapCommandMap.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
                mailcapCommandMap.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
                CommandMap.setDefaultCommandMap(mailcapCommandMap);

                // create mail session
                Properties properties = new Properties();
                properties.put("mail.transport.protocol","smtp");
                properties.put("mail.smtp.host",repositoryTab.repository.mailSMTPHost);
                properties.put("mail.smtp.port",Integer.toString(repositoryTab.repository.mailSMTPPort));
                properties.put("mail.from",repositoryTab.repository.mailFrom);
//properties.put("mail.smtp.starttls.enable","true");
                properties.put("mail.smtp.auth",(password != null) && !password.isEmpty());
                if (repositoryTab.repository.mailSMTPSSL)
                {
                  properties.put("mail.smtp.socketFactory.port",Integer.toString(repositoryTab.repository.mailSMTPPort));
                  properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
                  properties.put("mail.smtp.socketFactory.fallback","false");
                }

//properties.put("mail.smtps.starttls.enable","true");
//                if (Settings.debugFlag) properties.put("mail.debug","true");
                Authenticator auth = new Authenticator()
                {
                  public PasswordAuthentication getPasswordAuthentication()
                  {
                    return new PasswordAuthentication(repositoryTab.repository.mailLogin,password);
                  }
                };
                Session session = Session.getInstance(properties,auth);

                // create message
                MimeMultipart mimeMultipart = new MimeMultipart();

                MimeBodyPart text = new MimeBodyPart();
                text.setText(widgetMailText.getText());
                text.setDisposition(MimeBodyPart.INLINE);
                mimeMultipart.addBodyPart(text);

                MimeBodyPart attachment = new MimeBodyPart();
                attachment.attachFile(tmpFile);
                attachment.setFileName("file.patch");
                attachment.setDisposition(Part.ATTACHMENT);
                attachment.setDescription("Attached File: "+"patch");
// detect mime type?
//                FileDataSource fileDataSource = new FileDataSource(tmpFile);
//                DataHandler dataHandler = new DataHandler(fileDataSource);
                DataHandler dataHandler = new DataHandler(StringUtils.join(patch.getLines(),"\n"),"text/plain");
                attachment.setDataHandler(dataHandler);
                mimeMultipart.addBodyPart(attachment);

                InternetAddress toAddress = new InternetAddress(widgetMailTo.getText().trim());
                ArrayList<InternetAddress> ccAddressList = new ArrayList<InternetAddress>();
                if ((repositoryTab.repository.patchMailCC != null) && !repositoryTab.repository.patchMailCC.trim().isEmpty())
                {
                  for (String address : repositoryTab.repository.patchMailCC.split("\\s"))
                  {
                    ccAddressList.add(new InternetAddress(address));
                  }
                }
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(repositoryTab.repository.mailFrom));
                message.setSubject(widgetMailSubject.getText().trim());
                message.setRecipient(Message.RecipientType.TO,toAddress);
                message.setRecipients(Message.RecipientType.CC,ccAddressList.toArray(new InternetAddress[ccAddressList.size()]));
                message.setSentDate(new Date());
                message.setContent(mimeMultipart);
                message.saveChanges();

                // send message
                Transport.send(message);
              }
              catch (MessagingException exception)
              {
                Dialogs.error(dialog,"Cannot send patch (error: %s)",exception.getMessage());
                return;
              }
            }
            else if (!Settings.commandMailAttachment.isEmpty())
            {
              // use external mail command
              try
              {
                // create command
                Macro macro = new Macro(StringUtils.split(Settings.commandMailAttachment,StringUtils.WHITE_SPACES,StringUtils.QUOTE_CHARS,false));
                macro.expand("to",     widgetMailTo.getText().trim()     );
                macro.expand("cc",     widgetMailCC.getText().trim()     );
                macro.expand("subject",widgetMailSubject.getText().trim());
                macro.expand("file",   tmpFile.getAbsolutePath()         );
                String[] commandArray = macro.getValueArray();
//for (String s : command) Dprintf.dprintf("command=%s",s);

                // execute and add text
                Process process = Runtime.getRuntime().exec(commandArray);
                PrintWriter processOutput = new PrintWriter(process.getOutputStream());
                for (String line : StringUtils.split(widgetMailText.getText(),widgetMailText.DELIMITER))
                {
                  processOutput.println(line);
                }
                processOutput.close();

                // wait done
                int exitcode = process.waitFor();
                if (exitcode != 0)
                {
                  Dialogs.error(dialog,"Cannot send patch (exitcode: %d)",exitcode);
                  return;
                }
              }
              catch (InterruptedException exception)
              {
                Dialogs.error(dialog,"Cannot send patch (error: %s)",exception.getMessage());
                return;
              }
            }
            else
            {
              Dialogs.error(dialog,"No mail command configured.\nPlease check settings.");
              return;
            }

            // free resources
            tmpFile.delete(); tmpFile = null;
          }
          catch (IOException exception)
          {
            Dialogs.error(dialog,"Cannot send patch (error: %s)",exception.getMessage());
            return;
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
Dprintf.dprintf("");

          Dialogs.close(dialog,true);
        }
      });

      button = Widgets.newButton(composite,"Refresh...");
      Widgets.layout(button,0,2,TableLayoutData.W,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (Dialogs.confirm(dialog,"Confirmation","Really refresh patch?"))
          {
            repositoryTab.setStatusText("Refresh patch...");
            try
            {
              // get repository instance
              Repository repository = Repository.newInstance(patch.rootPath);

              // refresh patch
              String[] newLines = repository.getPatchLines(patch.getFileNames(),
                                                           patch.revision1,
                                                           patch.revision2,
                                                           patch.ignoreWhitespaces
                                                          );
              patch.setLines(newLines);
              widgetPatch.setText(StringUtils.join(patch.getLines(),widgetPatch.getLineDelimiter()));
            }
            catch (RepositoryException exception)
            {
              Dialogs.error(dialog,"Cannot get patch (error: %s)",exception.getMessage());
              return;
            }
            finally
            {
              repositoryTab.clearStatusText();
            }
          }
        }
      });

      widgetCancel = Widgets.newButton(composite,"Cancel");
      Widgets.layout(widgetCancel,0,3,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
    widgetPatch.addLineStyleListener(new LineStyleListener()
    {
      public void lineGetStyle(LineStyleEvent lineStyleEvent)
      {
//Dprintf.dprintf("x %d %s",lineStyleEvent.lineOffset,lineStyleEvent.lineText);
         String findText = widgetFind.getText().toLowerCase();
         int    findTextLength = findText.length();
         if (findTextLength > 0)
         {
           ArrayList<StyleRange> styleRangeList = new ArrayList<StyleRange>();
           int                   index = 0;
           while ((index = lineStyleEvent.lineText.toLowerCase().indexOf(findText,index)) >= 0)
           {
             styleRangeList.add(new StyleRange(lineStyleEvent.lineOffset+index,findTextLength,COLOR_FIND_TEXT,null));
             index += findTextLength;
           }
           lineStyleEvent.styles = styleRangeList.toArray(new StyleRange[styleRangeList.size()]);
//Dprintf.dprintf("lineStyleEvent.styles=%d",lineStyleEvent.styles.length);
         }
         else
         {
           lineStyleEvent.styles = null;
         }
      }
    });

    widgetFind.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent leyEvent)
      {
      }
      public void keyReleased(KeyEvent leyEvent)
      {
        find(widgetPatch,widgetFind);
      }
    });
    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        findNext(widgetPatch,widgetFind);
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
      }
    });
    widgetFindPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        findPrev(widgetPatch,widgetFind);
      }
    });
    widgetFindNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        findNext(widgetPatch,widgetFind);
      }
    });

    widgetSummary.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;
        int   index  = widget.getSelectionIndex();

        if (index >= 0)
        {
          widgetMessage.setText(StringUtils.join(data.history[index].message,widgetMessage.DELIMITER));
          updateMailText();
        }
      }
    });
    widgetSummary.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        updateMailSubject();
      }
    });
    Widgets.setNextFocus(widgetSummary,widgetMessage);
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

    // add files
    if (!widgetFileNames.isDisposed())
    {
      for (FileData fileData : fileDataSet)
      {
        widgetFileNames.add(fileData.getFileName());
      }
    }

    // update
    for (Patch history : data.history)
    {
      widgetSummary.add(history.summary);
    }
    widgetPatch.setText(StringUtils.join(patch.getLines(),widgetPatch.getLineDelimiter()));
    widgetMailTo.setText(repositoryTab.repository.patchMailTo);
    widgetMailCC.setText(repositoryTab.repository.patchMailCC);
    updateMailSubject();
    updateMailText();
  }

  /** mail patch command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileDataSet file data set
   * @param patch patch
   */
  CommandMailPatch(Shell shell, RepositoryTab repositoryTab, HashSet<FileData> fileDataSet, Patch patch)
  {
    this(shell,repositoryTab,fileDataSet,patch,"",null);
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

  /** search text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void find(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText();
      if (!findText.isEmpty())
      {
        // get cursor position, text before cursor
        int cursorIndex = widgetPatch.getCaretOffset();

        // search
        int offset = widgetPatch.getText().substring(cursorIndex).indexOf(findText);
        if (offset >= 0)
        {
          widgetPatch.redraw();
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
      else
      {
        widgetPatch.redraw();
      }
    }
  }

  /** search previous text in patch
   * @param widgetPatch text widget
   * @param widgetFind search text widget
   */
  private void findPrev(StyledText widgetPatch, Text widgetFind)
  {
    String findText = widgetFind.getText();
    if (!findText.isEmpty())
    {
      // get cursor position, text before cursor
      int cursorIndex = widgetPatch.getCaretOffset();

      int offset = (cursorIndex > 0) ? widgetPatch.getText(0,cursorIndex-1).lastIndexOf(findText) : -1;
      if (offset >= 0)
      {
        int index = offset;
Dprintf.dprintf("cursorIndex=%d index=%d",cursorIndex,index);

        widgetPatch.setCaretOffset(index);
        widgetPatch.setSelection(index);
        widgetPatch.redraw();
      }
      else
      {
        Widgets.flash(widgetFind);
      }
    }
  }

  /** search next text in patch
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findNext(StyledText widgetPatch, Text widgetFind)
  {
    String findText = widgetFind.getText();
    if (!findText.isEmpty())
    {
      // get cursor position, text before cursor
      int cursorIndex = widgetPatch.getCaretOffset();
//Dprintf.dprintf("cursorIndex=%d: %s",cursorIndex,widgetText.getText().substring(cursorIndex+1).substring(0,100));

      // search
      int offset = widgetPatch.getText().substring(cursorIndex+1).indexOf(findText);
      if (offset >= 0)
      {
        int index = cursorIndex+1+offset;

        widgetPatch.setCaretOffset(index);
        widgetPatch.setSelection(index);
        widgetPatch.redraw();
      }
      else
      {
        Widgets.flash(widgetFind);
      }
    }
  }

  /** update mail subject line
   */
  private void updateMailSubject()
  {
    Macro macro = new Macro(repositoryTab.repository.patchMailSubject);
    macro.expand("n",      patch.getNumber()      );
    macro.expand("summary",widgetSummary.getText().trim());
    widgetMailSubject.setText(macro.getValue());
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
    macro.expand("summary", widgetSummary.getText().trim()    );
    macro.expand("date",    Onzen.DATE_FORMAT.format(date)    );
    macro.expand("time",    Onzen.TIME_FORMAT.format(date)    );
    macro.expand("datetime",Onzen.DATETIME_FORMAT.format(date));
    macro.expand("message", widgetMessage.getText().trim()    );
    macro.expand("tests",   tests,"\n"                        );
    widgetMailText.setText(macro.getValue());
  }
}

/* end of file */
