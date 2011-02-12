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

//import java.text.SimpleDateFormat;

import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
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
    String                message;

    Data()
    {
      this.fileDataSet = new HashSet<FileData>();
      this.showStates  = EnumSet.copyOf(Settings.patchShowStates);
      this.patch       = null;
      this.message     = "";
    }
  };

  // --------------------------- constants --------------------------------

  // user events
  private final int USER_EVENT_FILTER_PATCHES  = 0xFFFF+0;

  // --------------------------- variables --------------------------------

  // global variable references
  private final RepositoryTab repositoryTab;
  private final Display       display;

  // dialog
  private final Data          data = new Data();
  private final Shell         dialog;

  // widgets
  private final Table         widgetPatches;
  private final List          widgetFileNames;   
  private final Text          widgetMessage;
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
    Listener  listener;

    // initialize variables
    this.repositoryTab = repositoryTab;

    // get display
    display = shell.getDisplay();

    // add files dialog
    dialog = Dialogs.open(shell,"Patches",new double[]{1.0,0.0},1.0);

    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(new double[]{0.0,1.0,0.0,1.0,0.0,1.0},1.0,4));
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
      Widgets.addTableColumn(widgetPatches,0,"State",  SWT.LEFT);
      Widgets.addTableColumn(widgetPatches,1,"Summary",SWT.LEFT);
      Widgets.setTableColumnWidth(widgetPatches,Settings.geometryPatchesColumn.width);

      label = Widgets.newLabel(composite,"Files:");
      Widgets.layout(label,2,0,TableLayoutData.W);

      widgetFileNames = Widgets.newList(composite);
      widgetFileNames.setBackground(Onzen.COLOR_GRAY);
      Widgets.layout(widgetFileNames,3,0,TableLayoutData.NSWE);
      widgetFileNames.setToolTipText("Files of patch.");

      label = Widgets.newLabel(composite,"Message:");
      Widgets.layout(label,4,0,TableLayoutData.W);

      widgetMessage = Widgets.newText(composite,SWT.LEFT|SWT.BORDER|SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
      Widgets.layout(widgetMessage,5,0,TableLayoutData.NSWE);
      widgetMessage.setToolTipText("Commit message.\n\nUse Ctrl-Return to commit patch.");
    }

    // buttons
    composite = Widgets.newComposite(dialog);
    composite.setLayout(new TableLayout(0.0,new double[]{0.0,0.0,0.0,0.0,1.0}));
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
              // open file
              FileWriter patchOutput = new FileWriter(fileName,true);

              // write file
              patchOutput.write(data.patch.text,0,data.patch.text.length());

              // close file
              patchOutput.close();
            }
            catch (IOException exception)
            {
              Dialogs.error(dialog,"Cannot save patch file! (error: %s)",exception.getMessage());
              return;
            }
          }
        }
      });

      button = Widgets.newButton(composite,"Commit");
      button.setEnabled(false);
      Widgets.layout(button,0,1,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.COMMITED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Patch patch = data.patch;
          if (patch != null)
          {
            patch.state = Patch.States.COMMITED;
            patch.save();

            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            setSelectedPatch(patch.getId());
          }
        }
      });

      button = Widgets.newButton(composite,"Apply");
      button.setEnabled(false);
      Widgets.layout(button,0,2,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state != Patch.States.APPLIED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Patch patch = data.patch;
          if (patch != null)
          {
            data.patch.state = Patch.States.APPLIED;
            data.patch.save();

            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            setSelectedPatch(patch.getId());
          }
        }
      });

      button = Widgets.newButton(composite,"Unapply");
      button.setEnabled(false);
      Widgets.layout(button,0,3,TableLayoutData.W);
      Widgets.addModifyListener(new WidgetListener(button,data)
      {
        public void modified(Control control)
        {
          Widgets.setEnabled(control,(data.patch != null) && (data.patch.state == Patch.States.APPLIED));
        }
      });
      button.addSelectionListener(new SelectionListener()
      {
        public void widgetDefaultSelected(SelectionEvent selectionEvent)
        {
        }
        public void widgetSelected(SelectionEvent selectionEvent)
        {
          Button widget = (Button)selectionEvent.widget;

          Patch patch = data.patch;
          if (patch != null)
          {
            data.patch.state = Patch.States.NONE;
            data.patch.save();

            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            setSelectedPatch(patch.getId());
          }
        }
      });

      button = Widgets.newButton(composite,"Discard");
      button.setEnabled(false);
      Widgets.layout(button,0,4,TableLayoutData.W);
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
          Button widget = (Button)selectionEvent.widget;

          Patch patch = data.patch;
          if (patch != null)
          {
            data.patch.state = Patch.States.DISCARDED;
            data.patch.save();

            Widgets.notify(dialog,USER_EVENT_FILTER_PATCHES);
            setSelectedPatch(patch.getId());
          }
        }
      });

      widgetClose = Widgets.newButton(composite,"Close");
      Widgets.layout(widgetClose,0,5,TableLayoutData.E,0,0,0,0,SWT.DEFAULT,SWT.DEFAULT,70,SWT.DEFAULT);
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

    dialog.addListener(USER_EVENT_FILTER_PATCHES,new Listener()
    {
      public void handleEvent(Event event)
      {
        int id = getSelectedPatchId();

        widgetPatches.removeAll();
        for (Patch patch : Patch.getPatches(repositoryTab.repository.rootPath,data.showStates,50))
        {
          Widgets.addTableEntry(widgetPatches,
                                patch,
                                patch.state.toString(),
                                patch.summary
                               );
        }

        setSelectedPatch(id);
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
//      widgetSubject.setFocus();
//      widgetSubject.setSelection(widgetSubject.getText().length(),widgetSubject.getText().length());
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
    return ((tableItems != null) && (tableItems.length > 0)) ? ((Patch)tableItems[0].getData()).getId() : -1;
  }

  /** set selected patch
   * @param patch patch to select
   */
  private void setSelectedPatch(Patch patch)
  {
    data.patch = patch;
    widgetFileNames.removeAll();
    widgetMessage.setText("");

    if (patch != null)
    {
      for (String fileName : data.patch.fileNames)
      {
        widgetFileNames.add(fileName);
      }
      widgetMessage.setText(data.patch.message);
    }

    Widgets.modified(data);
  } 

  /** set selected patch
   * @param id id of patch to select
   */
  private void setSelectedPatch(int id)
  {
    data.patch = null;
    widgetFileNames.removeAll();
    widgetMessage.setText("");

    for (TableItem tableItem : widgetPatches.getItems())
    {
      Patch patch = (Patch)tableItem.getData();
      if (patch.getId() == id)
      {
        data.patch = patch;

        for (String fileName : data.patch.fileNames)
        {
          widgetFileNames.add(fileName);
        }
        widgetMessage.setText(data.patch.message);
        break;
      }
    }

    Widgets.modified(data);
  }
}

/* end of file */
