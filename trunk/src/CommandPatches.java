/***********************************************************************\
*
* $Source: /tmp/cvs/onzen/src/CommandPatches.java,v $
* $Revision: 1.1 $
* $Author: torsten $
* Contents: patches command
* Systems: all
*
\***********************************************************************/

/****************************** Imports ********************************/
// base
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

/** patches command
 */
class CommandPatches
{
  /** dialog data
   */
  class Data
  {
    boolean               showAllRepositories;
    EnumSet<Patch.States> showStates;
    TableItem             tableItem;
    Patch                 patch;
    String                oldSummary;
    String                oldMessage;
    String                oldComment;
    HashSet<String>       oldFileNameSet;
    LinkedHashSet<String> oldTestSet;

    Data()
    {
      this.showAllRepositories = Settings.patchShowAllRepositories;
      this.showStates          = EnumSet.copyOf(Settings.patchShowStates);
      this.tableItem           = null;
      this.patch               = null;
      this.oldSummary          = null;
      this.oldMessage          = null;
      this.oldComment          = null;
      this.oldFileNameSet      = null;
      this.oldTestSet          = null;
    }
  };

  // --------------------------- constants --------------------------------

  // colors
  private final Color COLOR_FIND_TEXT;

  // user events
  private final int USER_EVENT_FILTER_PATCHES = 0xFFFF+0;
  private final int USER_EVENT_ADD_NEW_TEST   = 0xFFFF+1;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Shell         shell;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetPatches;
  private final StyledText    widgetPatch;
  private final ScrollBar     widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final List          widgetFileNames;
  private final Text          widgetFind;
  private final Button        widgetFindPrev;
  private final Button        widgetFindNext;
  private final Text          widgetSummary;
  private final Text          widgetMessage;
  private final Text          widgetComment;
  private final Table         widgetTests;
  private final Text          widgetNewTest;
  private final Button        widgetAddNewTest;
  private final Button        widgetMessageSave;
  private final Button        widgetClose;

  // ------------------------ native functions ----------------------------

  // ---------------------------- methods ---------------------------------

  /** patches command
   * @param shell shell
   * @param repositoryTab repository tab
   * @param lines patch lines
   */
  CommandPatches(final Shell shell, final RepositoryTab repositoryTab)
  {
    Composite composite,subComposite,subSubComposite,subSubSubComposite;
    Menu      menu;
    MenuItem  menuItem;
    Label     label;
    Button    button;
    TabFolder tabFolder;
    Listener  listener;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get shell, display
    this.shell   = shell;
    this.display = shell.getDisplay();

    // init colors
    COLOR_FIND_TEXT = new Color(display,Settings.colorFindText.foreground);

    // add files dialog
    dialog = Dialogs.open(shell,"Patches",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,1.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Patches:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        button = Widgets.newCheckbox(subComposite,"all repositories");
        button.setSelection(data.showAllRepositories);
        Widgets.layout(button,0,1,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            data.showAllRepositories = widget.getSelection();
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });

        button = Widgets.newCheckbox(subComposite,"review");
        button.setSelection(data.showStates.contains(Patch.States.REVIEW));
        Widgets.layout(button,0,2,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(Patch.States.REVIEW);
            }
            else
            {
              data.showStates.remove(Patch.States.REVIEW);
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });

        button = Widgets.newCheckbox(subComposite,"commited");
        button.setSelection(data.showStates.contains(Patch.States.COMMITED));
        Widgets.layout(button,0,3,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(Patch.States.COMMITED);
            }
            else
            {
              data.showStates.remove(Patch.States.COMMITED);
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });

        button = Widgets.newCheckbox(subComposite,"applied");
        button.setSelection(data.showStates.contains(Patch.States.APPLIED));
        Widgets.layout(button,0,4,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(Patch.States.APPLIED);
            }
            else
            {
              data.showStates.remove(Patch.States.APPLIED);
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });

        button = Widgets.newCheckbox(subComposite,"discarded");
        button.setSelection(data.showStates.contains(Patch.States.DISCARDED));
        Widgets.layout(button,0,5,TableLayoutData.E);
        button.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            Button widget = (Button)selectionEvent.widget;

            if (widget.getSelection())
            {
              data.showStates.add(Patch.States.DISCARDED);
            }
            else
            {
              data.showStates.remove(Patch.States.DISCARDED);
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });
      }

      widgetPatches = Widgets.newTable(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
      widgetPatches.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetPatches,1,0,TableLayoutData.NSWE);
      Widgets.addTableColumn(widgetPatches,0,"#",         SWT.RIGHT);
      Widgets.addTableColumn(widgetPatches,1,"State",     SWT.LEFT );
      Widgets.addTableColumn(widgetPatches,2,"Repository",SWT.LEFT );
      Widgets.addTableColumn(widgetPatches,3,"Summary",   SWT.LEFT );
      Widgets.setTableColumnWidth(widgetPatches,Settings.geometryPatchesColumn.width);
      menu = Widgets.newPopupMenu(dialog);
      {
        menuItem = Widgets.addMenuItem(menu,"Set state 'none'");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              Patch patch = (Patch)tableItem.getData();
              try
              {
                patch.setState(Patch.States.NONE);
                patch.save();
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Set state 'review'");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              Patch patch = (Patch)tableItem.getData();
              try
              {
                patch.setState(Patch.States.REVIEW);
                patch.save();
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Set state 'commited'");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              Patch patch = (Patch)tableItem.getData();
              try
              {
                patch.setState(Patch.States.COMMITED);
                patch.save();
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Set state 'applied'");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              Patch patch = (Patch)tableItem.getData();
              try
              {
                patch.setState(Patch.States.APPLIED);
                patch.save();
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Set state 'discarded'");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              Patch patch = (Patch)tableItem.getData();
              try
              {
                patch.setState(Patch.States.DISCARDED);
                patch.save();
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
          }
        });

        Widgets.addMenuSeparator(menu);


        menuItem = Widgets.addMenuItem(menu,"Save as file");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              saveAsFile((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Send for review");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              sendForReview((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Commit");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              commit((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Apply");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              apply((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Unapply");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              unapply((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Discard");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              discard((Patch)tableItem.getData());
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Delete...");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            if (widgetPatches.getSelectionCount() > 0)
            {
              if (Dialogs.confirm(dialog,"Confirmation","Really delete "+widgetPatches.getSelectionCount()+" patches?","Delete","Cancel"))
              {
                // delete patches
                for (TableItem tableItem : widgetPatches.getSelection())
                {
                  delete((Patch)tableItem.getData());
                }

                // clear selection
                clearSelectedPatch();
              }
            }
          }
        });
        menuItem = Widgets.addMenuItem(menu,"Edit reference...");
        menuItem.addSelectionListener(new SelectionListener()
        {
          public void widgetDefaultSelected(SelectionEvent selectionEvent)
          {
          }
          public void widgetSelected(SelectionEvent selectionEvent)
          {
            for (TableItem tableItem : widgetPatches.getSelection())
            {
              editReference((Patch)tableItem.getData());
            }
          }
        });
      }
      widgetPatches.setMenu(menu);
      widgetPatches.setToolTipText("Patches.\nRight-click to open context menu.");

      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,2,0,TableLayoutData.NSWE);
      {
        subComposite = Widgets.addTab(tabFolder,"Changes");
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetPatch = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
          widgetPatch.setFont(Onzen.FONT_CHANGES);
          widgetPatch.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetPatch,0,0,TableLayoutData.NSWE);
          widgetPatch.setToolTipText("Changes to commit.");
          widgetHorizontalScrollBar = widgetPatch.getHorizontalBar();
          widgetVerticalScrollBar   = widgetPatch.getVerticalBar();

          subSubComposite = Widgets.newComposite(subComposite);
          subSubComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
          Widgets.layout(subSubComposite,1,0,TableLayoutData.WE);
          {
            label = Widgets.newLabel(subSubComposite,"Find:",SWT.NONE,Settings.keyFind);
            Widgets.layout(label,0,0,TableLayoutData.W);

            widgetFind = Widgets.newText(subSubComposite,SWT.SEARCH|SWT.ICON_CANCEL);
            Widgets.layout(widgetFind,0,1,TableLayoutData.WE);

            widgetFindPrev = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_UP);
            Widgets.layout(widgetFindPrev,0,2,TableLayoutData.NSW);
            widgetFindPrev.setToolTipText("Find previous occurrence of text ["+Widgets.acceleratorToText(Settings.keyFindPrev)+"].");

            widgetFindNext = Widgets.newButton(subSubComposite,Onzen.IMAGE_ARROW_DOWN);
            Widgets.layout(widgetFindNext,0,3,TableLayoutData.NSW);
            widgetFindNext.setToolTipText("Find next occurrence of text  ["+Widgets.acceleratorToText(Settings.keyFindNext)+"].");

            button = Widgets.newButton(subSubComposite,"Refresh...");
            button.setEnabled(false);
            Widgets.layout(button,0,4,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
            Widgets.addModifyListener(new WidgetListener(button,data)
            {
              public void modified(Control control)
              {
                Widgets.setEnabled(control,(data.patch != null));
              }
            });
            button.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                if (data.patch != null)
                {
                  if (Dialogs.confirm(dialog,"Confirmation","Really refresh patch?"))
                  {
                    repositoryTab.setStatusText("Refresh patch...");
                    try
                    {
                      // get repository instance
                      Repository repository = Repository.newInstance(data.patch.rootPath);

                      // refresh patch
                      String[] newLines = repository.getPatchLines(data.patch.getFileNames(),
                                                                   data.patch.revision1,
                                                                   data.patch.revision2,
                                                                   data.patch.ignoreWhitespaces
                                                                  );
                      data.patch.setLines(newLines);
                      setChangesText(newLines);

                      // save new patch lines
                      data.patch.save();
                    }
                    catch (RepositoryException exception)
                    {
                      Dialogs.error(dialog,"Cannot get patch (error: %s)",exception.getMessage());
                      return;
                    }
                    catch (SQLException exception)
                    {
                      Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                      return;
                    }
                    finally
                    {
                      repositoryTab.clearStatusText();
                    }
                  }
                }
              }
            });
            button.setToolTipText("Refresh patch.");
          }
        }

        subComposite = Widgets.addTab(tabFolder,"Files");
        subComposite.setLayout(new TableLayout(1.0,1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetFileNames = Widgets.newList(subComposite);
          widgetFileNames.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetFileNames,0,0,TableLayoutData.NSWE);
          widgetFileNames.setToolTipText("Files of patch.");

          menu = Widgets.newPopupMenu(dialog);
          {
            menuItem = Widgets.addMenuItem(menu,"Add file...");
            menuItem.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                Repository repository = repositoryTab.getRepository(data.patch.rootPath);

                String newFileName = Dialogs.fileOpen(dialog,"Add file to patch",(repository != null)?repository.title:data.patch.rootPath);
                if (newFileName != null)
                {
                  data.patch.addFileName(newFileName);

                  widgetFileNames.removeAll();
                  for (String fileName : data.patch.getFileNames())
                  {
                    widgetFileNames.add(fileName);
                  }

                  Widgets.modified(data);
                }
              }
            });
            menuItem = Widgets.addMenuItem(menu,"Remove files...");
            menuItem.addSelectionListener(new SelectionListener()
            {
              public void widgetDefaultSelected(SelectionEvent selectionEvent)
              {
              }
              public void widgetSelected(SelectionEvent selectionEvent)
              {
                for (String fileName : widgetFileNames.getSelection())
                {
                  data.patch.removeFileName(fileName);
                }

                widgetFileNames.removeAll();
                for (String fileName : data.patch.getFileNames())
                {
                  widgetFileNames.add(fileName);
                }

                Widgets.modified(data);
              }
            });
          }
          widgetFileNames.setMenu(menu);
          widgetFileNames.setToolTipText("Files.\nRight-click to open context menu.");
        }
      }

      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{1.0,0.0},2));
      Widgets.layout(subComposite,3,0,TableLayoutData.NSWE);
      {
        subSubComposite = Widgets.newComposite(subComposite);
        subSubComposite.setLayout(new TableLayout(new double[]{0.0,0.6,0.4},new double[]{0.0,1.0}));
        Widgets.layout(subSubComposite,0,0,TableLayoutData.NSWE);
        {
          label = Widgets.newLabel(subSubComposite,"Summary:");
          Widgets.layout(label,0,0,TableLayoutData.W);

          widgetSummary = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER);
          Widgets.layout(widgetSummary,0,1,TableLayoutData.WE);
          widgetSummary.setToolTipText("Short summary line for patch.");

          label = Widgets.newLabel(subSubComposite,"Message:");
          Widgets.layout(label,1,0,TableLayoutData.NW);

          widgetMessage = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
          Widgets.layout(widgetMessage,1,1,TableLayoutData.NSWE);
          widgetMessage.setToolTipText("Commit message.");

          label = Widgets.newLabel(subSubComposite,"Comment:");
          Widgets.layout(label,2,0,TableLayoutData.NW);

          widgetComment = Widgets.newText(subSubComposite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
          Widgets.layout(widgetComment,2,1,TableLayoutData.NSWE);
          widgetComment.setToolTipText("Additional comment (will not be part of commit message).");
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

                    String  text0    = tableItem0.getText();
                    Object  data0    = tableItem0.getData();
                    boolean checked0 = tableItem0.getChecked();
                    String  text1    = tableItem1.getText();
                    Object  data1    = tableItem1.getData();
                    boolean checked1 = tableItem1.getChecked();

                    tableItem0.setText(text1);
                    tableItem0.setData(data1);
                    tableItem0.setChecked(checked1);
                    tableItem1.setText(text0);
                    tableItem1.setData(data0);
                    tableItem1.setChecked(checked0);

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

                    String  text0    = tableItem0.getText();
                    Object  data0    = tableItem0.getData();
                    boolean checked0 = tableItem0.getChecked();
                    String  text1    = tableItem1.getText();
                    Object  data1    = tableItem1.getData();
                    boolean checked1 = tableItem1.getChecked();

                    tableItem0.setText(text1);
                    tableItem0.setData(data1);
                    tableItem0.setChecked(checked1);
                    tableItem1.setText(text0);
                    tableItem1.setData(data0);
                    tableItem1.setChecked(checked0);

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

      widgetMessageSave = Widgets.newButton(composite,"Save");
      widgetMessageSave.setEnabled(false);
      Widgets.layout(widgetMessageSave,5,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetMessageSave,data)
      {
        public void modified(Control control)
        {
          boolean summaryChanged     = (data.oldSummary != null) && !data.oldSummary.equals(widgetSummary.getText().trim());
          boolean messageChanged     = (data.oldMessage != null) && !data.oldMessage.equals(widgetMessage.getText().trim());
          boolean commentChanged     = (data.oldComment != null) && !data.oldComment.equals(widgetComment.getText().trim());
          boolean fileNameSetChanged = (data.oldFileNameSet != null) && !data.oldFileNameSet.equals(new HashSet(Arrays.asList(widgetFileNames.getItems())));;
//Dprintf.dprintf("fileNameSetChanged=%s",fileNameSetChanged);
//if (data.oldFileNameSet!=null) { Dprintf.dprintf("%d %d",data.oldFileNameSet.size(),new HashSet(Arrays.asList(widgetFileNames.getItems())).size()); }
          boolean testSetChanged     = false;
          for (TableItem tableItem : widgetTests.getItems())
          {
            if (tableItem.getChecked())
            {
              testSetChanged = (data.oldTestSet == null) || !data.oldTestSet.contains((String)tableItem.getData());
            }
            else
            {
              testSetChanged = (data.oldTestSet != null) && data.oldTestSet.contains((String)tableItem.getData());
            }
            if (testSetChanged) break;
          }

          Widgets.setEnabled(control,summaryChanged || messageChanged || commentChanged || fileNameSetChanged || testSetChanged);
        }
      });
      widgetMessageSave.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            try
            {
              // get summary, message
              String summary = widgetSummary.getText().trim();
              String message = widgetMessage.getText().trim();
              String comment = widgetComment.getText().trim();

              // get file names
              HashSet<String> fileNameSet = new HashSet<String>();
              for (String fileName : widgetFileNames.getItems())
              {
                fileNameSet.add(fileName);
              }

              // get tests
              LinkedHashSet<String> testSet = new LinkedHashSet<String>();
              for (TableItem tableItem : widgetTests.getItems())
              {
                if (tableItem.getChecked()) testSet.add((String)tableItem.getData());
              }

              // save patch
              data.patch.summary     = summary;
              data.patch.message     = StringUtils.split(message,widgetMessage.DELIMITER);
              data.patch.comment     = StringUtils.split(comment,widgetComment.DELIMITER);
              data.patch.fileNameSet = (HashSet)fileNameSet.clone();
              data.patch.testSet     = (LinkedHashSet)testSet.clone();
              data.patch.save();

              // update
              data.tableItem.setText(3,summary);
              data.oldSummary     = summary;
              data.oldMessage     = message;
              data.oldComment     = comment;
              data.oldFileNameSet = fileNameSet;
              data.oldTestSet     = testSet;
              Widgets.modified(data);
            }
            catch (SQLException exception)
            {
              Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
              return;
            }
          }
        }
      });
      button.setToolTipText("Save patch message.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,0.0,0.0,1.0}));
    Widgets.layout(composite,1,0,TableLayoutData.WE,0,0,4);
    {
      button = Widgets.newButton(composite,"Save as file");
      button.setEnabled(false);
      Widgets.layout(button,0,0,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            saveAsFile(data.patch);
          }
        }
      });
      button.setToolTipText("Save patch to file.");

      button = Widgets.newButton(composite,"Send for review");
      button.setEnabled(false);
      Widgets.layout(button,0,1,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            sendForReview(data.patch);
          }
        }
      });
      button.setToolTipText("Send patch for reviewing as mail and/or to review server.");

      button = Widgets.newButton(composite,"Commit");
      button.setEnabled(false);
      Widgets.layout(button,0,2,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            commit(data.patch);
          }
        }
      });
      button.setToolTipText("Commit changes in patch.");

      button = Widgets.newButton(composite,"Apply");
      button.setEnabled(false);
      Widgets.layout(button,0,3,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.COMMITED) && (data.patch.state != Patch.States.APPLIED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            apply(data.patch);
          }
        }
      });
      button.setToolTipText("Apply patch.");

      button = Widgets.newButton(composite,"Unapply");
      button.setEnabled(false);
      Widgets.layout(button,0,4,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.COMMITED) && (data.patch.state == Patch.States.APPLIED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            unapply(data.patch);
          }
        }
      });
      button.setToolTipText("Unapply patch.");

      button = Widgets.newButton(composite,"Discard");
      button.setEnabled(false);
      Widgets.layout(button,0,5,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.COMMITED) && (data.patch.state != Patch.States.DISCARDED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            discard(data.patch);
          }
        }
      });
      button.setToolTipText("Discard patch.");

      button = Widgets.newButton(composite,"Delete...");
      button.setEnabled(false);
      Widgets.layout(button,0,6,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.DISCARDED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          if (data.patch != null)
          {
            if (Dialogs.confirm(dialog,"Confirmation","Really delete patch?","Delete","Cancel"))
            {
              // delete
              delete(data.patch);
              data.patch = null;
            }
          }
        }
      });
      button.setToolTipText("Delete patch from database.");

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,7,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      widgetClose.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Settings.geometryPatches          = dialog.getSize();
          Settings.geometryPatchesColumn    = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetPatches));
          Settings.patchShowAllRepositories = data.showAllRepositories;
          Settings.patchShowStates          = data.showStates;

          Dialogs.close(dialog,false);
        }
      });
    }

    widgetPatches.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Table     widget    = (Table)selectionEvent.widget;
        TableItem tableItem = (TableItem)selectionEvent.item;

        setSelectedPatch(tableItem,(Patch)tableItem.getData());
      }
    });
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
    widgetPatch.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
        if      (Widgets.isAccelerator(keyEvent,Settings.keyFind))
        {
          widgetFind.forceFocus();
        }
        else if (   Widgets.isAccelerator(keyEvent,Settings.keyFindPrev)
                 || Widgets.isAccelerator(keyEvent,SWT.CTRL+SWT.ARROW_LEFT)
                )
        {
          Widgets.invoke(widgetFindPrev);
        }
        else if (   Widgets.isAccelerator(keyEvent,Settings.keyFindNext)
                 || Widgets.isAccelerator(keyEvent,SWT.CTRL+SWT.ARROW_RIGHT)
                )
        {
          Widgets.invoke(widgetFindNext);
        }
      }
      public void keyReleased(KeyEvent keyEvent)
      {
      }
    });

    widgetFind.addKeyListener(new KeyListener()
    {
      public void keyPressed(KeyEvent keyEvent)
      {
      }
      public void keyReleased(KeyEvent keyEvent)
      {
        updateViewFindText(widgetPatch,widgetFind);
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
    widgetFind.addKeyListener(new KeyListener()
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
      }
      public void keyReleased(KeyEvent keyEvent)
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

    widgetSummary.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        Widgets.setEnabled(widgetMessageSave,(data.oldSummary != null) && !data.oldSummary.equals(widget.getText().trim()));
      }
    });
    widgetMessage.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        Widgets.setEnabled(widgetMessageSave,(data.oldMessage != null) && !data.oldMessage.equals(widget.getText().trim()));
      }
    });
    widgetComment.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        Text widget = (Text)modifyEvent.widget;

        Widgets.setEnabled(widgetMessageSave,(data.oldComment != null) && !data.oldComment.equals(widget.getText().trim()));
      }
    });
    widgetTests.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Table widget = (Table)selectionEvent.widget;

        boolean testSetChanged = false;
        for (TableItem tableItem : widgetTests.getItems())
        {
          if (tableItem.getChecked())
          {
            testSetChanged = (data.oldTestSet == null) || !data.oldTestSet.contains((String)tableItem.getData());
          }
          else
          {
            testSetChanged = (data.oldTestSet != null) && data.oldTestSet.contains((String)tableItem.getData());
          }
          if (testSetChanged) break;
        }
        Widgets.setEnabled(widgetMessageSave,testSetChanged);
      }
    });
    widgetNewTest.addModifyListener(new ModifyListener()
    {
      public void modifyText(ModifyEvent modifyEvent)
      {
        widgetAddNewTest.setEnabled(!widgetNewTest.getText().trim().isEmpty());
      }
    });

    dialog.addListener(USER_EVENT_FILTER_PATCHES,new Listener()
    {
      public void handleEvent(Event event)
      {
        int patchNumber = getSelectedPatchNumber();

        widgetPatches.removeAll();
        Patch[] patches = Patch.getPatches(repositoryTab.repository.rootPath,data.showAllRepositories,data.showStates,50);
        if (patches != null)
        {
          for (Patch patch : patches)
          {
            Repository repository = repositoryTab.getRepository(patch.rootPath);

            Widgets.addTableEntry(widgetPatches,
                                  patch,
                                  patch.getNumberText(),
                                  patch.state.toString(),
                                  (repository != null)?repository.title:patch.rootPath,
                                  patch.summary
                                 );
          }

          setSelectedPatch(patchNumber);
        }
      }
    });
    dialog.addListener(USER_EVENT_ADD_NEW_TEST,new Listener()
    {
      public void handleEvent(Event event)
      {
        String newPatchTest = widgetNewTest.getText().trim();

        if (!newPatchTest.isEmpty())
        {
          // check for duplicate tests
          boolean found = false;
          for (String patchTest : repositoryTab.repository.patchTests)
          {
            if (patchTest.equalsIgnoreCase(newPatchTest))
            {
              found = true;
              break;
            }
          }

          if (!found)
          {
            // add test to patch
            TableItem tableItem = Widgets.addTableEntry(widgetTests,newPatchTest,newPatchTest);
            tableItem.setChecked(true);

            data.patch.testSet.add(newPatchTest);
            Widgets.modified(data);
          }
        }

        widgetNewTest.setText("");
        widgetNewTest.setFocus();
      }
    });

    // show dialog
    Dialogs.show(dialog,Settings.geometryPatches);

    // add patches
    Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
  }

  /** run dialog
   */
  public void run()
  {
    if (!dialog.isDisposed())
    {
      widgetPatches.setFocus();
      Dialogs.run(dialog);
    }
  }

  /** convert data to string
   * @return string
   */
  public String toString()
  {
    return "CommandPatches {}";
  }

  //-----------------------------------------------------------------------

  /** set changes text
   * @param lines changes lines
   * @param widgetText text widget
   */
  private void setChangesText(String[] lines)
  {
    if (   !widgetPatch.isDisposed()
        && !widgetVerticalScrollBar.isDisposed()
        && !widgetHorizontalScrollBar.isDisposed()
       )
    {
      // set text
      widgetPatch.setText(StringUtils.join(lines,"\n"));

      // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
      widgetPatch.redraw();
      widgetPatch.update();

      // show top
      widgetPatch.setTopIndex(0);
      widgetPatch.setCaretOffset(0);
      widgetVerticalScrollBar.setSelection(0);
    }
  }

  /** get selected patch
   * @return selected patch or null
   */
  private Patch getSelectedPatch()
  {
    TableItem[] tableItems = widgetPatches.getSelection();
    return ((tableItems != null) && (tableItems.length > 0)) ? (Patch)tableItems[0].getData() : null;
  }

  /** get selected patch number
   * @return selected patch number or -1
   */
  private int getSelectedPatchNumber()
  {
    TableItem[] tableItems = widgetPatches.getSelection();
    return ((tableItems != null) && (tableItems.length > 0)) ? ((Patch)tableItems[0].getData()).getNumber() : -1;
  }

  /** set selected patch
   * @param tableItem table item
   * @param patch patch to select
   */
  private void setSelectedPatch(TableItem tableItem, Patch patch)
  {
    data.tableItem = tableItem;
    data.patch     = patch;

    if (data.patch != null)
    {
      // select table item
      widgetPatches.setSelection(tableItem);

      // set changes text
      setChangesText(data.patch.getLines());

      // set file names
      widgetFileNames.removeAll();
      for (String fileName : data.patch.getFileNames())
      {
        widgetFileNames.add(fileName);
      }
      data.oldFileNameSet = (HashSet)patch.getFileNameSet();

      // set summary
      widgetSummary.setText(data.patch.summary);
      data.oldSummary = data.patch.summary;

      // set message
      String message = StringUtils.join(data.patch.message,widgetMessage.DELIMITER);
      widgetMessage.setText(message);
      data.oldMessage = message;

      // set comment
      String comment = StringUtils.join(data.patch.comment,widgetComment.DELIMITER);
      widgetComment.setText(comment);
      data.oldComment = comment;

      // set tests
      widgetTests.removeAll();
      for (String test : patch.testSet)
      {
        TableItem testTableItem = Widgets.addTableEntry(widgetTests,test,test);
        testTableItem.setChecked(true);
      }
      data.oldTestSet = (LinkedHashSet)patch.getTestSet();
    }
    else
    {
      // clear changes text, file names, message
      widgetPatch.setText("");
      widgetFileNames.removeAll();
      widgetMessage.setText("");
      widgetComment.setText("");
    }
    Widgets.modified(data);
  }

  /** set selected patch
   * @param patchNumber number of patch to select
   */
  private void setSelectedPatch(int patchNumber)
  {
    // search patch
    for (TableItem tableItem : widgetPatches.getItems())
    {
      Patch patch = (Patch)tableItem.getData();
      if (patch.getNumber() == patchNumber)
      {
        // select patch
        setSelectedPatch(tableItem,patch);
        break;
      }
    }
  }

  /** clear selected patch
   */
  private void clearSelectedPatch()
  {
    widgetPatch.setText("");
    widgetFileNames.removeAll();
    widgetMessage.setText("");
    widgetComment.setText("");

    Widgets.modified(data);
  }

  /** update view find text
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void updateViewFindText(StyledText widgetText, Text widgetFind)
  {
    if (!widgetText.isDisposed())
    {
      String findText = widgetFind.getText();
      if (!findText.isEmpty())
      {
        // get cursor position
        int cursorIndex = widgetText.getCaretOffset();

        // search
        int offset = widgetText.getText().toLowerCase().substring(cursorIndex).indexOf(findText);
        if (offset >= 0)
        {
          widgetText.redraw();
        }
        else
        {
          Widgets.flash(widgetFind);
        }
      }
      else
      {
        widgetText.redraw();
      }
    }
  }

  /** search previous text in patch
   * @param widgetText text widget
   * @param widgetFind search text widget
   */
  private void findPrev(StyledText widgetText, Text widgetFind)
  {
    String findText = widgetFind.getText().toLowerCase();
    if (!findText.isEmpty())
    {
      // get cursor position
      int cursorIndex = widgetText.getCaretOffset();

      // search
      int offset = -1;
      if (cursorIndex > 0)
      {
        String text = widgetText.getText(0,cursorIndex-1);
        offset = text.toLowerCase().lastIndexOf(findText);
      }
      if (offset >= 0)
      {
        int index = offset;

        widgetText.setCaretOffset(index);
        widgetText.setSelection(index);
        widgetText.redraw();
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
  private void findNext(StyledText widgetText, Text widgetFind)
  {
    String findText = widgetFind.getText().toLowerCase();
    if (!findText.isEmpty())
    {
      // get cursor position
      int cursorIndex = widgetText.getCaretOffset();
//Dprintf.dprintf("cursorIndex=%d: %s",cursorIndex,widgetText.getText().substring(cursorIndex+1).substring(0,100));

      // search
      int offset = -1;
      if (cursorIndex >= 0)
      {
        String text = widgetText.getText();
        offset = (cursorIndex+1 < text.length()) ? text.substring(cursorIndex+1).toLowerCase().indexOf(findText) : -1;
      }
      if (offset >= 0)
      {
        int index = cursorIndex+1+offset;

        widgetText.setCaretOffset(index);
        widgetText.setSelection(index);
        widgetText.redraw();
      }
      else
      {
        Widgets.flash(widgetFind);
      }
    }
  }

  /** save patch into file
   * @param patch patch to save into file
   */
  private void saveAsFile(Patch patch)
  {
    // get file name
    String fileName = Dialogs.fileSave(dialog,"Save patch","",new String[]{".patch","*"});
    if (fileName == null)
    {
      return;
    }

    // check if file exists: overwrite or append
    File file = new File(fileName);
    if (file.exists())
    {
      switch (Dialogs.select(dialog,"Confirmation",String.format("File '%s' already exists.",fileName),new String[]{"Overwrite","Append","Cancel"},2))
      {
        case 0:
          if (!file.delete())
          {
            Dialogs.error(dialog,"Cannot delete file!");
            return;
          }
        case 1:
          break;
        case 2:
          return;
      }
    }

    // create patch file
    try
    {
      patch.write(fileName);
    }
    catch (IOException exception)
    {
      Dialogs.error(dialog,"Cannot save patch file! (error: %s)",exception.getMessage());
      return;
    }
  }

  /** send patch for review
   * @param patch patch to send for review
   */
  private void sendForReview(final Patch patch)
  {
    // review patch
    final CommandPatchReview commandPatchReview = new CommandPatchReview(dialog,
                                                                         repositoryTab,
                                                                         patch
                                                                       );
    commandPatchReview.run(new DialogRunnable()
    {
      public void done()
      {
        try
        {
          // save patch in database
          patch.state   = Patch.States.REVIEW;
          patch.summary = commandPatchReview.summary;
          patch.message = commandPatchReview.message;
          patch.comment = commandPatchReview.comment;
          patch.testSet = commandPatchReview.testSet;
          patch.save();

          if (!dialog.isDisposed() && (patch == data.patch))
          {
            // set summary
            widgetSummary.setText(patch.summary);
            data.oldSummary = patch.summary;

            // set message
            String message = StringUtils.join(patch.message,widgetMessage.DELIMITER);
            widgetMessage.setText(message);
            data.oldMessage = message;

            // set comment
            String comment = StringUtils.join(patch.comment,widgetComment.DELIMITER);
            widgetComment.setText(comment);
            data.oldComment = comment;

            // set tests
            widgetTests.removeAll();
            for (String test : patch.testSet)
            {
              TableItem testTableItem = Widgets.addTableEntry(widgetTests,test,test);
              testTableItem.setChecked(true);
            }
            data.oldTestSet = (LinkedHashSet)patch.getTestSet();
          }
        }
        catch (SQLException exception)
        {
          Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
          return;
        }
      }
      public void cancel()
      {
      }
    });
  }

  /** commit change in patch to repository
   * @param patch patch to commit
   */
  private void commit(Patch patch)
  {
    // check state
    switch (patch.state)
    {
      case COMMITED:
        if (!Dialogs.confirm(dialog,"Confirmation","Patch is already commited. Commit again?"))
        {
          return;
        }
        break;
      case APPLIED:
        if (!Dialogs.confirm(dialog,"Confirmation","Patch is applied. Commit anyway?"))
        {
          return;
        }
        break;
      default:
        break;
    }

    // check commit messaage
    CommitMessage commitMessage = null;
    try
    {
      // get commit message
      commitMessage = new CommitMessage(patch.summary,patch.message);
      if (!repositoryTab.repository.validCommitMessage(commitMessage))
      {
        if (!Dialogs.confirm(shell,"Confirmation","The commit message is probably too long or may not be accepted.\n\nCommit with the message anyway?"))
        {
          return;
        }
      }

      // commit
      try
      {
        // get repository instance
        Repository repository = Repository.newInstance(patch.rootPath);

        // get patches files
        HashSet<FileData> fileDataSet = FileData.toSet(patch.getFileNames());
        repository.updateStates(fileDataSet);

        // get new files
        HashSet<FileData> newFileDataSet = new HashSet<FileData>();
        for (FileData fileData : fileDataSet)
        {
          if (fileData.state == FileData.States.UNKNOWN)
          {
            newFileDataSet.add(fileData);
          }
        }

        // save files
        StoredFiles storedFiles = new StoredFiles(repository.rootPath,fileDataSet);
        try
        {
          // get existing/new files
          HashSet<FileData> existFileDataSet = new HashSet<FileData>();
          if (fileDataSet != null)
          {
            for (FileData fileData : fileDataSet)
            {
              if (fileData.state != FileData.States.UNKNOWN)
              {
                existFileDataSet.add(fileData);
              }
            }
          }
          if (existFileDataSet.size() > 0)
          {
            // revert local changes of existing files
            repository.revert(existFileDataSet);
          }

          // apply patch
          if (!patch.apply())
          {
            throw new RepositoryException("applying patch fail");
          }

          // add new files
          if (newFileDataSet.size() > 0)
          {
            repository.add(newFileDataSet,null,false);
          }

          // add message to history
          commitMessage.addToHistory();

          // commit files
          repository.commit(fileDataSet,commitMessage);

          // update file states
          repositoryTab.asyncUpdateFileStates(fileDataSet);

          // restore files
          if (!storedFiles.restore())
          {
            throw new RepositoryException("restore local changes fail");
          }

          // discard stored files
          storedFiles.discard(); storedFiles = null;
        }
        finally
        {
          if (storedFiles != null)
          {
            storedFiles.restore();
            storedFiles.discard();
          }
        }

        // save new state in database
        patch.state = Patch.States.COMMITED;
        patch.save();
      }
      catch (RepositoryException exception)
      {
        Dialogs.error(dialog,"Cannot commit patch (error: %s)",exception.getMessage());
        return;
      }
      catch (SQLException exception)
      {
        Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
        return;
      }
    }
    finally
    {
      if (commitMessage != null) commitMessage.done();
    }

    // notify change of data
    Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
  }

  /** apply patch
   * @param patch patch to apply
   */
  private void apply(Patch patch)
  {
    try
    {
      StoredFiles storedFiles = null;
      try
      {
        // create files backup of all local changes (in case there occur some error)
        storedFiles = new StoredFiles(repositoryTab.repository.rootPath,repositoryTab.repository.getChangedFiles());

        // apply patch
        if (!patch.apply())
        {
          Dialogs.error(dialog,"Cannot apply patch!");
          storedFiles.restore(); storedFiles = null;
          return;
        }

        // discard files backup
        storedFiles.discard(); storedFiles = null;

        // update file states
        HashSet<FileData> fileDataSet = FileData.toSet(patch.getFileNames());
        repositoryTab.asyncUpdateFileStates(fileDataSet);

        // save new state
        patch.state = Patch.States.APPLIED;
        patch.save();

        // notify change of data
        Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
      }
      finally
      {
        if (storedFiles != null) storedFiles.restore();
      }
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(dialog,"Cannot apply patch (error: %s)",exception.getMessage());
      return;
    }
    catch (SQLException exception)
    {
      Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
      return;
    }
  }

  /** unapply patch
   * @param patch patch to unapply
   */
  private void unapply(Patch patch)
  {
    try
    {
      StoredFiles storedFiles = null;
      try
      {
        // create files backup of all local changes (in case there occur some error)
        storedFiles = new StoredFiles(repositoryTab.repository.rootPath,repositoryTab.repository.getChangedFiles());

        // unapply patch
        if (!patch.unapply())
        {
          Dialogs.error(dialog,"Cannot unapply patch!");
          storedFiles.restore(); storedFiles = null;
          return;
        }

        // discard files backup
        storedFiles.discard(); storedFiles = null;

        // update file states
        HashSet<FileData> fileDataSet = FileData.toSet(patch.getFileNames());
        repositoryTab.asyncUpdateFileStates(fileDataSet);

        // save new state
        patch.state = Patch.States.NONE;
        patch.save();

        // notify change of data
        Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
      }
      finally
      {
        if (storedFiles != null) storedFiles.restore();
      }
    }
    catch (RepositoryException exception)
    {
      Dialogs.error(dialog,"Cannot unapply patch (error: %s)",exception.getMessage());
      return;
    }
    catch (SQLException exception)
    {
      Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
      return;
    }
  }

  /** discard patch
   * @param patch patch to discard
   */
  private void discard(Patch patch)
  {
    try
    {
      // save new state
      patch.state = Patch.States.DISCARDED;
      patch.save();

      // notify change of data
      Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
    }
    catch (SQLException exception)
    {
      Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
      return;
    }
  }

  /** delete patch
   * @param patch patch to delete
   */
  private void delete(Patch patch)
  {
    try
    {
      // delete
      patch.delete();

      // notify change of data
      Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
    }
    catch (SQLException exception)
    {
      Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
      return;
    }
  }

  /** edit reference of patch
   * @param patch patch to commit
   */
  private void editReference(Patch patch)
  {
    String reference = Dialogs.string(dialog,"Edit patch reference","Reference:",patch.reference);
    if (reference != null)
    {
      try
      {
        patch.reference = reference;
        patch.save();
      }
      catch (SQLException exception)
      {
        Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
        return;
      }
    }
  }
}

/* end of file */
