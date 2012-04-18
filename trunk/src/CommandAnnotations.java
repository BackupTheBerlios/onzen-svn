/***********************************************************************\
*
* $Revision$
* $Date$
* $Author$
* Contents: command view file annotations
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base

// graphics
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

/****************************** Classes ********************************/

/** view annotations command
 */
class CommandAnnotations
{
  /** dialog data
   */
  class Data
  {
    String[]         revisionNames;      // revision names
    AnnotationData[] annotationData;     // annotation data
    String           selectedRevision;   // selected revision
    String           prevRevision;       // previous revision or null
    String           nextRevision;       // next revision or null

    Data()
    {
      this.revisionNames    = null;
      this.annotationData   = null;
      this.selectedRevision = null;
      this.prevRevision     = null;
      this.nextRevision     = null;
    }
  };

  // --------------------------- constants --------------------------------

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;
  private final Clipboard     clipboard;
  private final FileData      fileData;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Combo         widgetRevision;
  private final Button        widgetRevisionPrev;
  private final Button        widgetRevisionNext;
  private final Table         widgetAnnotations;
  private final TableColumn   widgetAnnotationLineColumn;
  private final MenuItem      menuItemGotoRevision;
  private final MenuItem      menuItemPrevRevision;
  private final MenuItem      menuItemNextRevision;
  private final MenuItem      menuItemShowRevision;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** view annotations
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   * @param revision to view annotions
   */
  CommandAnnotations(final Shell shell, final RepositoryTab repositoryTab, final FileData fileData, String revision)
  {
    Composite composite,subComposite;
    Label     label;
    Menu      menu;
    Button    button;

    // initialize variables
    this.repositoryTab = repositoryTab;
    this.fileData      = fileData;

    // get display, clipboard
    display   = shell.getDisplay();
    clipboard = new Clipboard(display);

    // add files dialog
    dialog = Dialogs.open(shell,"Annotations: "+fileData.getFileName(),new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0,0.0,0.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Revision:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetRevision = Widgets.newSelect(subComposite);
        widgetRevision.setEnabled(false);
        Widgets.layout(widgetRevision,0,1,TableLayoutData.WE);
        Widgets.addModifyListener(new WidgetListener(widgetRevision,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionNames != null));
          }
        });
        widgetRevision.setToolTipText("Revision to view.");

        widgetRevisionPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_LEFT);
        widgetRevisionPrev.setEnabled(false);
        Widgets.layout(widgetRevisionPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetRevisionPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.revisionNames != null) && (widgetRevision.getSelectionIndex() > 0));
          }
        });
        widgetRevisionPrev.setToolTipText("Show previous revision.");

        widgetRevisionNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_RIGHT);
        widgetRevisionNext.setEnabled(false);
        Widgets.layout(widgetRevisionNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetRevisionNext,data)
        {
          public void modified(Control control)
          {

            Widgets.setEnabled(control,(data.revisionNames != null) && (widgetRevision.getSelectionIndex() < data.revisionNames.length-1));
          }
        });
        widgetRevisionNext.setToolTipText("Show next revision.");
      }

      widgetAnnotations = Widgets.newTable(composite,SWT.H_SCROLL|SWT.V_SCROLL);
      widgetAnnotations.setEnabled(false);
      Widgets.layout(widgetAnnotations,1,0,TableLayoutData.NSWE);
      Widgets.addModifyListener(new WidgetListener(widgetAnnotations,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.annotationData != null));
        }
      });
      Widgets.addTableColumn(widgetAnnotations,0,"Revision",SWT.RIGHT);
      Widgets.addTableColumn(widgetAnnotations,1,"Author",  SWT.LEFT );
      Widgets.addTableColumn(widgetAnnotations,2,"Date",    SWT.LEFT );
      Widgets.addTableColumn(widgetAnnotations,3,"Line Nb.",SWT.RIGHT);
      widgetAnnotationLineColumn = Widgets.addTableColumn(widgetAnnotations,4,"Line",SWT.LEFT);
      Widgets.setTableColumnWidth(widgetAnnotations,Settings.geometryAnnotationsColumns.width);
      menu = Widgets.newPopupMenu(dialog);
      {
        menuItemGotoRevision = Widgets.addMenuItem(menu,"Goto revision");
        menuItemGotoRevision.setEnabled(false);
        menuItemGotoRevision.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            show(data.selectedRevision);
          }
        });
        menuItemPrevRevision = Widgets.addMenuItem(menu,"Goto previous revision");
        menuItemPrevRevision.setEnabled(false);
        menuItemPrevRevision.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            show(data.prevRevision);
          }
        });
        menuItemNextRevision = Widgets.addMenuItem(menu,"Goto next revision");
        menuItemNextRevision.setEnabled(false);
        menuItemNextRevision.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            show(data.nextRevision);
          }
        });
        menuItemShowRevision = Widgets.addMenuItem(menu,"Show revision");
        menuItemShowRevision.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            CommandRevisions commandRevisions = new CommandRevisions(shell,repositoryTab,fileData,data.selectedRevision);
            commandRevisions.run();
          }
        });
      }
      widgetAnnotations.setMenu(menu);
      widgetAnnotations.setToolTipText("File annotations. Double-click to view revisions.");

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,2,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Find:",SWT.NONE,Settings.keyFind);
        Widgets.layout(label,0,0,TableLayoutData.W);

        widgetFind = Widgets.newText(subComposite,SWT.SEARCH|SWT.ICON_CANCEL);
        widgetFind.setMessage("Enter text to find");
        Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

        widgetFindPrev = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_UP);
        widgetFindPrev.setEnabled(false);
        Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetFindPrev,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.annotationData != null));
          }
        });
        widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

        widgetFindNext = Widgets.newButton(subComposite,Onzen.IMAGE_ARROW_DOWN);
        widgetFindNext.setEnabled(false);
        Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
        Widgets.addModifyListener(new WidgetListener(widgetFindNext,data)
        {
          public void modified(Control control)
          {
            Widgets.setEnabled(control,(data.annotationData != null));
          }
        });
        widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");
      }
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,1.0));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Close");
      Widgets.layout(button,0,1,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryAnnotations        = dialog.getSize();
          Settings.geometryAnnotationsColumns = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetAnnotations));

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetRevision.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Combo widget = (Combo)selectionEvent.widget;

        int index = widget.getSelectionIndex();
        if ((index >= 0) && (index < data.revisionNames.length))
        {
          show(data.revisionNames[index]);
        }
      }
    });
    widgetRevisionPrev.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetRevision.getSelectionIndex();
        if (index > 0)
        {
          show(data.revisionNames[index-1]);
        }
      }
    });
    widgetRevisionNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = widgetRevision.getSelectionIndex();
        if ((data.revisionNames != null) && (index < data.revisionNames.length-1))
        {
          show(data.revisionNames[index+1]);
        }
      }
    });
    widgetAnnotations.addMouseListener(new MouseListener()
    {
      public void mouseDoubleClick(MouseEvent mouseEvent)
      {
        Table widget = (Table)mouseEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          selectRevision(widget.getItem(index).getText(0));

          switch (Dialogs.select(dialog,
                                 "Confirmation",
                                 "Action:",
                                 new String[]{"goto revision "+data.selectedRevision,
                                              "goto previous revision "+data.prevRevision,
                                              "goto next revision "+data.nextRevision,
                                              "show revision "+data.selectedRevision
                                             },
                                 null,
                                 new boolean[]{!data.selectedRevision.equals(widgetRevision.getText()),
                                               data.prevRevision != null,
                                               data.nextRevision != null,
                                               true
                                              },
                                 "OK",
                                 "Cancel",
                                 0
                                )
                 )
          {
            case 0:
              show(data.selectedRevision);
              break;
            case 1:
              show(data.prevRevision);
              break;
            case 2:
              show(data.nextRevision);
              break;
            case 3:
              CommandRevisions commandRevisions = new CommandRevisions(shell,repositoryTab,fileData,data.selectedRevision);
              commandRevisions.run();
              break;
            default:
              break;
          }
        }
      }
      public void mouseDown(MouseEvent mouseEvent)
      {
        Table widget = (Table)mouseEvent.widget;

        int index = widget.getSelectionIndex();
        if (index >= 0)
        {
          selectRevision(widget.getItem(index).getText(0));
        }
      }
      public void mouseUp(MouseEvent mouseEvent)
      {
      }
    });
    widgetFind.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
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
        int index = findPrev(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
      }
    });
    widgetFindNext.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        int index = findNext(widgetAnnotations,widgetFind);
        if (index >= 0)
        {
          widgetAnnotations.setSelection(index);
        }
      }
    });

    KeyListener keyListener = new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          widgetFind.forceFocus();
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFindPrev))
        {
          Widgets.invoke(widgetFindPrev);
        }
        else if (Widgets.isAccelerator(keyEvent,Settings.keyFindNext))
        {
          Widgets.invoke(widgetFindNext);
        }
        else if (Widgets.isAccelerator(keyEvent,SWT.CTRL+'c'))
        {
          TableItem[] tableItems = widgetAnnotations.getSelection();
          Widgets.setClipboard(clipboard,tableItems,4);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    };
    widgetAnnotations.addKeyListener(keyListener);
    widgetFind.addKeyListener(keyListener);
    widgetFindPrev.addKeyListener(keyListener);
    widgetFindNext.addKeyListener(keyListener);

    // show dialog
    Dialogs.show(dialog,Settings.geometryAnnotations,Settings.setWindowLocation);

    // start get annotations of revision
    show(revision);

    // start add revisions
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(final FileData fileData, final String revision)
      {
        // get revisions
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
                repositoryTab.setStatusText("Get revisions for '%s'...",fileData.getFileName());
        try
        {
          data.revisionNames = repositoryTab.repository.getRevisionNames(fileData);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,String.format("Getting revisions fail: %s",exceptionMessage));
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
          Widgets.resetCursor(dialog);
        }

        if (data.revisionNames.length > 0)
        {
          // add revisions
          if (!dialog.isDisposed())
          {
            display.syncExec(new Runnable()
            {
              public void run()
              {
                // add revisions to selection menu
                for (int z = 0; z < data.revisionNames.length; z++)
                {
                  widgetRevision.add(data.revisionNames[z]);
                }

                // get selected/previous/next revision
                int selectedIndex = getRevisionIndex(revision);
                if (selectedIndex >= 0)
                {
                  selectRevision(selectedIndex);
                }
                else
                {
                  widgetRevision.setText(revision);
                }

                // notify modification
                Widgets.modified(data);
              }
            });
          }
        }
      }
    });
  }

  /** view annotations
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   * @param revisionData revision data
   */
  CommandAnnotations(Shell shell, RepositoryTab repositoryTab, FileData fileData, RevisionData revisionData)
  {
    this(shell,repositoryTab,fileData,revisionData.revision);
  }

  /** view annotations of last revision
   * @param shell shell
   * @param repositoryTab repository tab
   * @param fileData file to view annotions
   */
  CommandAnnotations(Shell shell, RepositoryTab repositoryTab, FileData fileData)
  {
    this(shell,repositoryTab,fileData,repositoryTab.repository.getLastRevision());
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      widgetFind.setFocus();
      Dialogs.run(dialog);
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandAnnotations {}";
  }

  //-----------------------------------------------------------------------

  /** set annotation text
   * @param widgetAnnotations annotation table widget
   * @param annotations annotation data
   */
  private void setText(Table            widgetAnnotations,
                       AnnotationData[] annotations
                      )
  {
    if (!widgetAnnotations.isDisposed())
    {
      widgetAnnotations.removeAll();
      int maxWidth = 0;
      int lineNb   = 1;
      for (AnnotationData annotation : annotations)
      {
        Widgets.addTableEntry(widgetAnnotations,
                              annotation,
                              annotation.revision,
                              annotation.author,
                              Onzen.DATE_FORMAT.format(annotation.date),
                              Integer.toString(lineNb),
                              annotation.line
                             );
        maxWidth = Math.max(maxWidth,Widgets.getTextWidth(widgetAnnotations,annotation.line));
        lineNb++;
      }
      widgetAnnotationLineColumn.setWidth(maxWidth);
    }
  }

  /** update popup menu
   */
  private void updatePopupMenu()
  {
    menuItemGotoRevision.setText("Goto revision "+((data.selectedRevision != null)?data.selectedRevision:""));
    menuItemGotoRevision.setEnabled(data.selectedRevision != null);

    menuItemPrevRevision.setText("Goto previous revision "+((data.prevRevision != null)?data.prevRevision:""));
    menuItemPrevRevision.setEnabled(data.prevRevision != null);

    menuItemNextRevision.setText("Goto next revision "+((data.nextRevision != null)?data.nextRevision:""));
    menuItemNextRevision.setEnabled(data.nextRevision != null);

    menuItemShowRevision.setText("Show revision "+((data.selectedRevision != null)?data.selectedRevision:""));
    menuItemShowRevision.setEnabled(data.selectedRevision != null);
  }

  /** search previous text in annotation
   * @param widgetAnnotations annotation table widget
   * @param widgetFind search text widget
   * @return line index or -1
   */
  private int findPrev(Table widgetAnnotations, Text widgetFind)
  {
    int index = -1;

    if (!widgetAnnotations.isDisposed() && !widgetFind.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get current line index or last line
        int i = widgetAnnotations.getSelectionIndex();
        if ((i < 0) || (i >= widgetAnnotations.getItemCount()))
        {
          i = widgetAnnotations.getItemCount()-1;
        }

        // find previous line index
        boolean foundFlag = false;
        if (i > 0)
        {
          do
          {
            i--;

            TableItem tableItem = widgetAnnotations.getItem(i);
            foundFlag =    tableItem.getText(1).toLowerCase().contains(findText)
                        || tableItem.getText(4).toLowerCase().contains(findText);
          }
          while (!foundFlag && (i > 0));
        }

        if (foundFlag)
        {
          index = i;
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }

    return index;
  }

  /** search next text in annotation
   * @param widgetAnnotations annotation table widget
   * @param widgetFind search text widget
   * @return line index or -1
   */
  private int findNext(Table widgetAnnotations, Text widgetFind)
  {
    int index = -1;

    if (!widgetAnnotations.isDisposed() && !widgetFind.isDisposed())
    {
      String findText = widgetFind.getText().toLowerCase();
      if (!findText.isEmpty())
      {
        // get current line index or first line
        int i = widgetAnnotations.getSelectionIndex();
        if ((i < 0) || (i >= widgetAnnotations.getItemCount()))
        {
          i = 0;
        }

        // find next line index
        boolean foundFlag = false;
        if ((i >= 0) && (i < widgetAnnotations.getItemCount()-1))
        {
          do
          {
            i++;

            TableItem tableItem = widgetAnnotations.getItem(i);
            foundFlag =    tableItem.getText(1).toLowerCase().contains(findText)
                        || tableItem.getText(4).toLowerCase().contains(findText);
          }
          while (!foundFlag && (i < widgetAnnotations.getItemCount()-1));
        }

        if (foundFlag)
        {
          index = i;
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
    }

    return index;
  }

  /** get revision index
   * @param revision revision
   * @return index or -1
   */
  private int getRevisionIndex(String revision)
  {
    int index = -1;
    if (data.revisionNames != null)
    {
      for (int z = 0; z < data.revisionNames.length; z++)
      {
        if (data.revisionNames[z].equals(revision))
        {
          index = z;
        }
      }
      if (index == -1) index = data.revisionNames.length-1;
    }

    return index;
  }

  /** get previous revision
   * @param revision revision
   * @return previous revision
   */
  private String getPrevRevision(String revision)
  {
    String prevRevision = null;
    if (data.revisionNames != null)
    {
      for (int z = 1; z < data.revisionNames.length; z++)
      {
        if (data.revisionNames[z].equals(revision))
        {
          prevRevision = data.revisionNames[z-1];
        }
      }
    }

    return prevRevision;
  }

  /** get next revision
   * @param revision revision
   * @return next revision
   */
  private String getNextRevision(String revision)
  {
    String nextRevision = null;
    if (data.revisionNames != null)
    {
      for (int z = 0; z < data.revisionNames.length-1; z++)
      {
        if (data.revisionNames[z].equals(revision))
        {
          nextRevision = data.revisionNames[z+1];
        }
      }
    }

    return nextRevision;
  }

  /** select revision
   * @param index index
   */
  private void selectRevision(final int index)
  {
    data.selectedRevision = data.revisionNames[index];
    data.prevRevision     = getPrevRevision(data.revisionNames[index]);
    data.nextRevision     = getNextRevision(data.revisionNames[index]);

    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          widgetRevision.select(index);

          menuItemGotoRevision.setText("Goto revision "+((data.selectedRevision != null)?data.selectedRevision:""));
          menuItemGotoRevision.setEnabled(data.selectedRevision != null);

          menuItemPrevRevision.setText("Goto previous revision "+((data.prevRevision != null)?data.prevRevision:""));
          menuItemPrevRevision.setEnabled(data.prevRevision != null);

          menuItemNextRevision.setText("Goto next revision "+((data.nextRevision != null)?data.nextRevision:""));
          menuItemNextRevision.setEnabled(data.nextRevision != null);

          menuItemShowRevision.setText("Show revision "+((data.selectedRevision != null)?data.selectedRevision:""));
          menuItemShowRevision.setEnabled(data.selectedRevision != null);
        }
      });
    };
  }

  /** select revision
   * @param revision revision to select
   */
  private void selectRevision(String revision)
  {
    int index = getRevisionIndex(revision);
    if (index >= 0)
    {
      selectRevision(index);
    }
  }

  /** show annotations
   * @param revision revision to show
   */
  private void show(String revision)
  {
    // clear
    if (!dialog.isDisposed())
    {
      display.syncExec(new Runnable()
      {
        public void run()
        {
          data.annotationData = null;
          Widgets.modified(data);
         }
      });
    }

    // start show annotations
    Background.run(new BackgroundRunnable(fileData,revision)
    {
      public void run(FileData fileData, final String revision)
      {
        // get annotations
        Widgets.setCursor(dialog,Onzen.CURSOR_WAIT);
        repositoryTab.setStatusText("Get annotations for '%s'...",fileData.getFileName());
        try
        {
          data.annotationData = repositoryTab.repository.getAnnotations(fileData,revision);
        }
        catch (RepositoryException exception)
        {
          final String exceptionMessage = exception.getMessage();
          display.syncExec(new Runnable()
          {
            public void run()
            {
              Dialogs.error(dialog,"Getting file annotations fail: %s",exceptionMessage);
            }
          });
          return;
        }
        finally
        {
          repositoryTab.clearStatusText();
          Widgets.resetCursor(dialog);
        }

        // show
        if (!dialog.isDisposed())
        {
          display.syncExec(new Runnable()
          {
            public void run()
            {
              // get selected/previous/next revision
              int selectedIndex = getRevisionIndex(revision);
              if (selectedIndex >= 0)
              {
                selectRevision(selectedIndex);
              }
              else
              {
                widgetRevision.setText(revision);
              }

              // set new text
              setText(widgetAnnotations,
                      data.annotationData
                     );

              // notify modification
              Widgets.modified(data);

              // focus text find
              widgetFind.setFocus();
            }
          });
        }
      }
    });
  }
}

/* end of file */
