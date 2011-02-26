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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
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
    HashSet<FileData>     fileDataSet;
    EnumSet<Patch.States> showStates;
    Patch                 patch;
    String                oldMessage;

    Data()
    {
      this.fileDataSet = new HashSet<FileData>();
      this.showStates  = EnumSet.copyOf(Settings.patchShowStates);
      this.patch       = null;
      this.oldMessage  = null;
    }
  };

  // --------------------------- constants --------------------------------

  // user events
  private final int USER_EVENT_FILTER_PATCHES = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetPatches;
  private final StyledText    widgetChanges;
  private final ScrollBar     widgetHorizontalScrollBar,widgetVerticalScrollBar;
  private final List          widgetFileNames;
  private final Text          widgetMessage;
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
    Composite composite,subComposite;
    Label     label;
    Button    button;
    TabFolder tabFolder;
    Listener  listener;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Patches",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,1.0,0.0,1.0},1.0,4));
    Widgets.layout(composite,0,0,TableLayoutData.NSWE,0,0,4);
    {
      subComposite = Widgets.newComposite(composite);
      subComposite.setLayout(new TableLayout(1.0,new double[]{0.0,1.0}));
      Widgets.layout(subComposite,0,0,TableLayoutData.WE);
      {
        label = Widgets.newLabel(subComposite,"Patches:");
        Widgets.layout(label,0,0,TableLayoutData.W);

        button = Widgets.newCheckbox(subComposite,"review");
        button.setSelection(data.showStates.contains(Patch.States.REVIEW));
        Widgets.layout(button,0,1,TableLayoutData.E);
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
      Widgets.addTableColumn(widgetPatches,0,"#",      SWT.RIGHT);
      Widgets.addTableColumn(widgetPatches,1,"State",  SWT.LEFT );
      Widgets.addTableColumn(widgetPatches,2,"Summary",SWT.LEFT );
      Widgets.setTableColumnWidth(widgetPatches,Settings.geometryPatchesColumn.width);

      tabFolder = Widgets.newTabFolder(composite);
      Widgets.layout(tabFolder,2,0,TableLayoutData.NSWE);
      {
        subComposite = Widgets.addTab(tabFolder,"Changes");
        subComposite.setLayout(new TableLayout(new double[]{1.0,0.0},1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetChanges = Widgets.newStyledText(subComposite,SWT.LEFT|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
          widgetChanges.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetChanges,0,0,TableLayoutData.NSWE);
          widgetChanges.setToolTipText("Changes to commit.");
          widgetHorizontalScrollBar = widgetChanges.getHorizontalBar();
          widgetVerticalScrollBar   = widgetChanges.getVerticalBar();

          button = Widgets.newButton(subComposite,"Refresh");
          button.setEnabled(false);
          Widgets.layout(button,1,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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
                    String[] newLines = repository.getPatchLines(data.patch.fileNames,
                                                                 data.patch.revision1,
                                                                 data.patch.revision2,
                                                                 data.patch.ignoreWhitespaces
                                                                );
                    data.patch.setLines(newLines);
                    setChangesText(newLines);

                    // save
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

        subComposite = Widgets.addTab(tabFolder,"Files");
        subComposite.setLayout(new TableLayout(1.0,1.0,2));
        Widgets.layout(subComposite,0,0,TableLayoutData.NSWE);
        {
          widgetFileNames = Widgets.newList(subComposite);
          widgetFileNames.setBackground(Onzen.COLOR_GRAY);
          Widgets.layout(widgetFileNames,0,0,TableLayoutData.NSWE);
          widgetFileNames.setToolTipText("Files of patch.");
        }
      }

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,3,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,4,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Return to commit patch.");

      widgetMessageSave = Widgets.newButton(composite,"Save");
      widgetMessageSave.setEnabled(false);
      Widgets.layout(widgetMessageSave,5,0,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
      Widgets.addModifyListener(new WidgetListener(widgetMessageSave,data)
      {
        public void modified(Control control)
        {
          String text = widgetMessage.getText().trim();
          Widgets.setEnabled(control,(data.oldMessage != null) && !data.oldMessage.equals(text));
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
              // get text
              String text = widgetMessage.getText().trim();

              // save patch
              data.patch.message = StringUtils.split(text,widgetMessage.DELIMITER);
              data.patch.save();

              // update
              data.oldMessage = text;
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
              switch (Dialogs.select(dialog,"Confirm",String.format("File '%s' already exists.",fileName),new String[]{"Overwrite","Append","Cancel"},2))
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
              data.patch.write(fileName);
            }
            catch (IOException exception)
            {
              Dialogs.error(dialog,"Cannot save patch file! (error: %s)",exception.getMessage());
              return;
            }
          }
        }
      });
      button.setToolTipText("Save patch to file.");

      button = Widgets.newButton(composite,"Mail for review");
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
            // mail patch
            CommandMailPatch commandMailPatch = new CommandMailPatch(dialog,
                                                                     repositoryTab,
                                                                     data.fileDataSet,
                                                                     data.patch,
                                                                     data.patch.summary,
                                                                     data.patch.message
                                                                    );
            if (commandMailPatch.execute())
            {
              try
              {
                // save patch in database
                data.patch.state   = Patch.States.REVIEW;
                data.patch.summary = commandMailPatch.summary;
                data.patch.message = commandMailPatch.message;
                data.patch.save();

                // close dialog
                Dialogs.close(dialog,true);
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot store patch into database (error: %s)",exception.getMessage());
                return;
              }
            }
          }
        }
      });
      button.setToolTipText("Mail patch for reviewing.");

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
            // check state
            switch (data.patch.state)
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

            // commit
            try
            {
              // commit patch
              commit(data.patch);

              // save new state in database
              data.patch.state = Patch.States.COMMITED;
              data.patch.save();

              // notify change of data
              Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
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
            try
            {
              // apply patch
Dprintf.dprintf("");

              // save new state
              data.patch.state = Patch.States.APPLIED;
              data.patch.save();

              // notify change of data
              Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            }
            catch (SQLException exception)
            {
              Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
              return;
            }
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
            try
            {
              // unapply patch
Dprintf.dprintf("");

              // save new state
              data.patch.state = Patch.States.NONE;
              data.patch.save();

              // notify change of data
              Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            }
            catch (SQLException exception)
            {
              Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
              return;
            }
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
            try
            {
              // save new state
              data.patch.state = Patch.States.DISCARDED;
              data.patch.save();

              // notify change of data
              Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            }
            catch (SQLException exception)
            {
              Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
              return;
            }
          }
        }
      });
      button.setToolTipText("Discard patch.");

      button = Widgets.newButton(composite,"Delete");
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
              try
              {
                // delete
                data.patch.delete();
                data.patch = null;

                // notify change of data
                Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
              }
              catch (SQLException exception)
              {
                Dialogs.error(dialog,"Cannot update patch data in database (error: %s)",exception.getMessage());
                return;
              }
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

          Settings.geometryPatches       = dialog.getSize();
          Settings.geometryPatchesColumn = new Settings.ColumnSizes(Widgets.getTableColumnWidth(widgetPatches));
          Settings.patchShowStates       = data.showStates;

          Dialogs.close(dialog,false);
        }
      });
    }

    // listeners
    widgetPatches.addSelectionListener(new SelectionListener()
    {
      public void widgetDefaultSelected(SelectionEvent selectionEvent)
      {
      }
      public void widgetSelected(SelectionEvent selectionEvent)
      {
        Table     widget    = (Table)selectionEvent.widget;
        TableItem tableItem = (TableItem)selectionEvent.item;

        setSelectedPatch((Patch)tableItem.getData());
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

    dialog.addListener(USER_EVENT_FILTER_PATCHES,new Listener()
    {
      public void handleEvent(Event event)
      {
        int id = getSelectedPatchId();

        widgetPatches.removeAll();
        Patch[] patches = Patch.getPatches(repositoryTab.repository.rootPath,data.showStates,50);
        if (patches != null)
        {
          for (Patch patch : patches)
          {
            Widgets.addTableEntry(widgetPatches,
                                  patch,
                                  patch.getNumberText(),
                                  patch.state.toString(),
                                  patch.summary
                                 );
          }

          setSelectedPatch(id);
        }
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

  /** run and wait for dialog
   */
  public boolean execute()
  {
    if ((Boolean)Dialogs.run(dialog,false))
    {
      return true;
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
    return "CommandPatches {}";
  }

  //-----------------------------------------------------------------------

  /** set changes text
   * @param lines changes lines
   * @param widgetText text widget
   * @param widgetHorizontalScrollBar horizontal scrollbar widget
   * @param widgetVerticalScrollBar horizontal scrollbar widget
   */
  private void setChangesText(String[] lines)
  {
    if (   !widgetChanges.isDisposed()
        && !widgetVerticalScrollBar.isDisposed()
        && !widgetHorizontalScrollBar.isDisposed()
       )
    {
      // set text
      widgetChanges.setText(StringUtils.join(lines,"\n"));

      // force redraw (Note: for some reason this is necessary to keep texts and scrollbars in sync)
      widgetChanges.redraw();
      widgetChanges.update();

      // show top
      widgetChanges.setTopIndex(0);
      widgetChanges.setCaretOffset(0);
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

  /** get selected patch
   * @return selected patch id or -1
   */
  private int getSelectedPatchId()
  {
    TableItem[] tableItems = widgetPatches.getSelection();
    return ((tableItems != null) && (tableItems.length > 0)) ? ((Patch)tableItems[0].getData()).getNumber() : -1;
  }

  /** set selected patch
   * @param patch patch to select
   */
  private void setSelectedPatch(Patch patch)
  {
    data.patch = patch;

    if (data.patch != null)
    {
      // set changes text
      setChangesText(data.patch.getLines());

      // set file names
      for (String fileName : data.patch.fileNames)
      {
        widgetFileNames.add(fileName);
      }

      // set message
      String text = StringUtils.join(data.patch.message,widgetMessage.DELIMITER);
      widgetMessage.setText(text);
      data.oldMessage = text;
    }
    else
    {
      // clear changes text, file names, message
      widgetChanges.setText("");
      widgetFileNames.removeAll();
      widgetMessage.setText("");
    }
    Widgets.modified(data);
  }

  /** set selected patch
   * @param patchNumber number of patch to select
   */
  private void setSelectedPatch(int patchNumber)
  {
    Patch patch = null;

    // search patch
    for (TableItem tableItem : widgetPatches.getItems())
    {
      if (((Patch)tableItem.getData()).getNumber() == patchNumber)
      {
        patch = (Patch)tableItem.getData();
        break;
      }
    }

    // select patch
    setSelectedPatch(patch);
  }

  /** commit change in patch to repository
   * @param patch patch to commit
   */
  private void commit(Patch patch)
    throws RepositoryException
  {
    // get repository instance
    Repository repository = Repository.newInstance(patch.rootPath);

    // get patches files
    HashSet<FileData> fileDataSet = new HashSet<FileData>();
    for (String fileName : patch.fileNames)
    {
      fileDataSet.add(new FileData(fileName));
    }
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
      // revert local files changes
      repository.revert(fileDataSet);

      // apply patch
      if (!patch.apply())
      {
        throw new RepositoryException("applying patch fail");
      }

      // commit patch
      Message message = null;
      try
      {
        // add message to history
        message = new Message(patch.message);
        message.addToHistory();

        // add new files
        if (newFileDataSet.size() > 0)
        {
          repository.add(newFileDataSet,null,false);
        }

        // commit files
        repository.commit(fileDataSet,message);

        // update file states
        repositoryTab.asyncUpdateFileStates(fileDataSet);
      }
      catch (IOException exception)
      {
        throw new RepositoryException(exception);
      }
      finally
      {
        if (message != null) message.done();
      }

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
  }

  private void apply()
  {
Dprintf.dprintf("");
  }

  private void unapply()
  {
Dprintf.dprintf("");
  }
}

/* end of file */
