/*
 * Copyright 2017 SIB Visions GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sibvisions.formlayoutvisualization;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.rad.genui.IFontAwesome;
import javax.rad.genui.UIColor;
import javax.rad.genui.UIComponent;
import javax.rad.genui.UIImage;
import javax.rad.genui.celleditor.UIChoiceCellEditor;
import javax.rad.genui.component.UIButton;
import javax.rad.genui.component.UICustomComponent;
import javax.rad.genui.component.UIIcon;
import javax.rad.genui.component.UILabel;
import javax.rad.genui.component.UITextArea;
import javax.rad.genui.container.UIFrame;
import javax.rad.genui.container.UIPanel;
import javax.rad.genui.control.UIEditor;
import javax.rad.genui.layout.UIBorderLayout;
import javax.rad.genui.layout.UIFormLayout;
import javax.rad.model.ColumnDefinition;
import javax.rad.model.IDataRow;
import javax.rad.model.ModelException;
import javax.rad.model.datatype.BooleanDataType;
import javax.rad.model.event.DataRowEvent;
import javax.rad.model.ui.ICellEditor;
import javax.rad.ui.IAlignmentConstants;
import javax.rad.ui.IColor;
import javax.rad.ui.IComponent;
import javax.rad.ui.IContainer;
import javax.rad.ui.layout.IFormLayout.IAnchor;
import javax.rad.ui.layout.IFormLayout.IConstraints;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import com.sibvisions.rad.lua.LuaEnvironment;
import com.sibvisions.rad.lua.LuaException;
import com.sibvisions.rad.model.mem.DataRow;
import com.sibvisions.util.FileViewer;
import com.sibvisions.util.type.ExceptionUtil;
import com.sibvisions.util.type.FileUtil;
import com.sibvisions.util.type.ResourceUtil;

/**
 * The {@link MainFrame} is an {@link UIFrame} extension and is the main frame.
 * 
 * @author Robert Zenz
 */
public class MainFrame extends UIFrame
{
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Class members
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * The {@link UILabel} showing the {@link IAnchor#isAutoSize()} property of
	 * the currently highlighted {@link IAnchor}.
	 */
	private UILabel anchorAutoResize = null;
	
	/**
	 * The {@link UILabel} showing thename of the currently highlighted
	 * {@link IAnchor}.
	 */
	private UILabel anchorName = null;
	
	/**
	 * The {@link UILabel} showing the {@link IAnchor#getOrientation()} property
	 * of the currently highlighted {@link IAnchor}.
	 */
	private UILabel anchorOrientation = null;
	
	/**
	 * The {@link UILabel} showing the {@link IAnchor#getPosition()} property of
	 * the currently highlighted {@link IAnchor}.
	 */
	private UILabel anchorPosition = null;
	
	/** The {@link RSyntaxTextArea} for the code. */
	private RSyntaxTextArea codeTextArea = null;
	
	/** The {@link LuaEnvironment} which is used for executing the code. */
	private LuaEnvironment environment = new LuaEnvironment();
	
	/** The {@link UILabel} that is used for displaying any error messages. */
	private UILabel errorLabel = null;
	
	/** The main {@link UIFormLayout}. */
	private UIFormLayout formLayout = null;
	
	/** The main {@link UIPanel}. */
	private AnchorShowingPanel formPanel = null;
	
	/** The {@link IDataRow} that is used as backend for the legend. */
	private IDataRow legendDataRow = null;
	
	/** The {@link Random} that is used to get random colors. */
	private Random random = new Random(1);
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Creates a new instance of {@link MainFrame}.
	 */
	public MainFrame()
	{
		super();
		
		try
		{
			initializeModel();
			initializeUI();
			
			environment.getGlobals().set("panel", CoerceJavaToLua.coerce(formPanel));
			environment.getGlobals().set("layout", CoerceJavaToLua.coerce(formLayout));
			environment.getGlobals().set("stub", new ZeroArgFunction()
			{
				@Override
				public LuaValue call()
				{
					return CoerceJavaToLua.coerce(createPlaceholder());
				}
			});
			
			codeTextArea.setText(new String(FileUtil.getContent(ResourceUtil.getResourceAsStream("/com/sibvisions/formlayoutvisualization/default.lua")), StandardCharsets.UTF_8));
			codeTextArea.setCaretPosition(0);
			codeTextArea.getDocument().addDocumentListener(new CodeChangedListener());
			
			updateLayout();
		}
		catch (ModelException e)
		{
			removeAll();
			
			setLayout(new UIBorderLayout());
			add(new UITextArea(ExceptionUtil.dump(e, true)), UIBorderLayout.CENTER);
		}
	}
	
	/**
	 * Initializes the model.
	 * 
	 * @throws ModelException when initializing the model failed.
	 */
	private void initializeModel() throws ModelException
	{
		legendDataRow = new DataRow();
		legendDataRow.getRowDefinition()
				.addColumnDefinition(new ColumnDefinition("BORDER_ANCHORS_VISIBLE", new BooleanDataType(createBooleanCellEditor(AnchorShowingPanel.BORDER_ANCHOR_COLOR))));
		legendDataRow.getRowDefinition()
				.addColumnDefinition(new ColumnDefinition("MARGIN_ANCHORS_VISIBLE", new BooleanDataType(createBooleanCellEditor(AnchorShowingPanel.MARGIN_ANCHOR_COLOR))));
		legendDataRow.getRowDefinition()
				.addColumnDefinition(new ColumnDefinition("AUTOSIZE_ANCHORS_VISIBLE", new BooleanDataType(createBooleanCellEditor(AnchorShowingPanel.AUTOSIZE_ANCHOR_COLOR))));
		legendDataRow.getRowDefinition()
				.addColumnDefinition(new ColumnDefinition("FIXED_ANCHORS_VISIBLE", new BooleanDataType(createBooleanCellEditor(AnchorShowingPanel.FIXED_ANCHOR_COLOR))));
		legendDataRow.getRowDefinition()
				.addColumnDefinition(new ColumnDefinition("BACKGROUND_VISIBLE", new BooleanDataType(createBooleanCellEditor(Tango.BUTTER_3))));
		legendDataRow.setValue("BORDER_ANCHORS_VISIBLE", Boolean.TRUE);
		legendDataRow.setValue("MARGIN_ANCHORS_VISIBLE", Boolean.TRUE);
		legendDataRow.setValue("AUTOSIZE_ANCHORS_VISIBLE", Boolean.TRUE);
		legendDataRow.setValue("FIXED_ANCHORS_VISIBLE", Boolean.TRUE);
		legendDataRow.setValue("BACKGROUND_VISIBLE", Boolean.FALSE);
		legendDataRow.eventValuesChanged().addListener(this::doAnchorVisibilityChanged);
	}

	/**
	 * Initializes the UI.
	 * 
	 * @throws ModelException when initializing the UI failed.
	 */
	private void initializeUI() throws ModelException
	{
		UIButton jvxLinkButton = new UIButton("About JVx");
		jvxLinkButton.setBackground(null);
		jvxLinkButton.setBorderOnMouseEntered(true);
		jvxLinkButton.setFocusable(false);
		jvxLinkButton.setImage(UIImage.getImage(IFontAwesome.INFO_CIRCLE_LARGE));
		jvxLinkButton.setHorizontalTextPosition(UIButton.ALIGN_CENTER);
		jvxLinkButton.setVerticalTextPosition(UIButton.ALIGN_BOTTOM);
		jvxLinkButton.eventAction().addListener(() -> FileViewer.open("https://sourceforge.net/projects/jvx/"));
		
		UIButton sibVisionsLinkButton = new UIButton("About SIB Visions");
		sibVisionsLinkButton.setBackground(null);
		sibVisionsLinkButton.setBorderOnMouseEntered(true);
		sibVisionsLinkButton.setFocusable(false);
		sibVisionsLinkButton.setImage(UIImage.getImage(IFontAwesome.INFO_CIRCLE_LARGE));
		sibVisionsLinkButton.setHorizontalTextPosition(UIButton.ALIGN_CENTER);
		sibVisionsLinkButton.setVerticalTextPosition(UIButton.ALIGN_BOTTOM);
		sibVisionsLinkButton.eventAction().addListener(() -> FileViewer.open("https://www.sibvisions.com/"));
		
		UIFormLayout headerPanelLayout = new UIFormLayout();
		
		UIPanel headerPanel = new UIPanel();
		headerPanel.setLayout(headerPanelLayout);
		headerPanel.setBackground(UIColor.white);
		headerPanel.add(new UIIcon(new UIImage("/com/sibvisions/formlayoutvisualization/images/jvx.png")), headerPanelLayout.getConstraints(0, 0));
		headerPanel.add(jvxLinkButton, headerPanelLayout.getConstraints(-2, 0));
		headerPanel.add(sibVisionsLinkButton, headerPanelLayout.getConstraints(-1, 0));
		addBorder(headerPanel, IAlignmentConstants.ALIGN_STRETCH, IAlignmentConstants.ALIGN_BOTTOM);
		
		anchorName = new UILabel("---");
		anchorOrientation = new UILabel("---");
		anchorAutoResize = new UILabel("AutoResize: ---");
		anchorPosition = new UILabel("Position: ---");
		
		UIFormLayout legendPanelLayout = new UIFormLayout();
		legendPanelLayout.setVerticalGap(3);
		
		UIPanel legendPanel = new UIPanel();
		legendPanel.setBackground(UIColor.white);
		legendPanel.setLayout(legendPanelLayout);
		legendPanel.add(new UIEditor(legendDataRow, "BORDER_ANCHORS_VISIBLE"), legendPanelLayout.getConstraints(0, 0));
		legendPanel.add(new UILabel("Border-Anchor"), legendPanelLayout.getConstraints(1, 0));
		legendPanel.add(new UIEditor(legendDataRow, "MARGIN_ANCHORS_VISIBLE"), legendPanelLayout.getConstraints(0, 1));
		legendPanel.add(new UILabel("Margin-Anchor"), legendPanelLayout.getConstraints(1, 1));
		legendPanel.add(new UIEditor(legendDataRow, "AUTOSIZE_ANCHORS_VISIBLE"), legendPanelLayout.getConstraints(0, 2));
		legendPanel.add(new UILabel("AutoSize-Anchor"), legendPanelLayout.getConstraints(1, 2));
		legendPanel.add(new UIEditor(legendDataRow, "FIXED_ANCHORS_VISIBLE"), legendPanelLayout.getConstraints(0, 3));
		legendPanel.add(new UILabel("Gap-Anchor"), legendPanelLayout.getConstraints(1, 3));
		legendPanel.add(new UIEditor(legendDataRow, "BACKGROUND_VISIBLE"), legendPanelLayout.getConstraints(0, 4));
		legendPanel.add(new UILabel("Background"), legendPanelLayout.getConstraints(1, 4));
		legendPanel.add(anchorName, legendPanelLayout.getConstraints(0, -4, 1, -4));
		legendPanel.add(anchorOrientation, legendPanelLayout.getConstraints(0, -3, 1, -3));
		legendPanel.add(anchorAutoResize, legendPanelLayout.getConstraints(0, -2, 1, -2));
		legendPanel.add(anchorPosition, legendPanelLayout.getConstraints(0, -1, 1, -1));
		addBorder(legendPanel, IAlignmentConstants.ALIGN_RIGHT, IAlignmentConstants.ALIGN_STRETCH);
		
		formLayout = new UIFormLayout();
		
		formPanel = new AnchorShowingPanel();
		formPanel.setLayout(formLayout);
		formPanel.setBackground(UIColor.white);
		formPanel.setHighlightedAnchorChangedListener(this::updateAnchorInformation);
		
		errorLabel = new UILabel();
		errorLabel.setBackground(UIColor.white);
		errorLabel.setForeground(Tango.SCARLET_RED_3);
		errorLabel.setVisible(false);
		
		UIBorderLayout containerLayout = new UIBorderLayout();
		containerLayout.setMargins(10, 10, 10, 10);
		
		UIPanel container = new UIPanel();
		container.setLayout(containerLayout);
		container.setBackground(UIColor.white);
		container.add(errorLabel, UIBorderLayout.NORTH);
		container.add(formPanel, UIBorderLayout.CENTER);
		
		codeTextArea = new RSyntaxTextArea();
		codeTextArea.setAutoIndentEnabled(true);
		codeTextArea.setBracketMatchingEnabled(true);
		codeTextArea.setCaretPosition(0);
		codeTextArea.setClearWhitespaceLinesEnabled(false);
		codeTextArea.setCloseCurlyBraces(true);
		codeTextArea.setLineWrap(false);
		codeTextArea.setEditable(true);
		codeTextArea.setMarkOccurrences(true);
		codeTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_LUA);
		codeTextArea.setTabSize(4);
		codeTextArea.setWhitespaceVisible(true);
		
		RTextScrollPane codeTextAreaScrollPane = new RTextScrollPane(codeTextArea, true);
		codeTextAreaScrollPane.setBorder(null);
		
		UIComponent<IComponent> wrappedCodeTextArea = new UICustomComponent(codeTextAreaScrollPane);
		wrappedCodeTextArea.setPreferredSize(480, 240);
		
		UIFormLayout codePanelLayout = new UIFormLayout();
		codePanelLayout.setMargins(0, 1, 0, 0);
		
		UIPanel codePanel = new UIPanel();
		codePanel.setLayout(codePanelLayout);
		codePanel.add(wrappedCodeTextArea, codePanelLayout.getConstraints(0, 0, -1, -1));
		addBorder(codePanel, IAlignmentConstants.ALIGN_LEFT, IAlignmentConstants.ALIGN_STRETCH);
		
		setLayout(new UIBorderLayout());
		setIconImage(UIImage.getImage("/com/sibvisions/formlayoutvisualization/images/icon.png"));
		setSize(1024, 600);
		setTitle("Formlayout Visualization");
		add(headerPanel, UIBorderLayout.NORTH);
		add(legendPanel, UIBorderLayout.WEST);
		add(container, UIBorderLayout.CENTER);
		add(codePanel, UIBorderLayout.EAST);
		
		formPanel.updateAnchorDisplay();
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Adds a "border" to the given {@link IContainer}.
	 * <p>
	 * This actually works under the assumption that the {@link IContainer} has
	 * a {@link UIFormLayout} set as layout. The "border" is actually a
	 * stretched image which is added to the {@link IContainer} with the border
	 * anchors as constraint, and then aligned to to look like a border at the
	 * given edge.
	 * 
	 * @param pContainer the parent {@link IContainer}.
	 * @param pHorizontalAlignment the horizontal alignment.
	 * @param pVerticalAlignment the vertical alignment.
	 */
	private static void addBorder(IContainer pContainer, int pHorizontalAlignment, int pVerticalAlignment)
	{
		UIFormLayout layout = (UIFormLayout)pContainer.getLayout();
		IConstraints constraints = layout.getConstraints(
				layout.getTopAnchor(),
				layout.getLeftAnchor(),
				layout.getBottomAnchor(),
				layout.getRightAnchor());
		
		UIIcon border = new UIIcon(UIImage.getImage("/com/sibvisions/formlayoutvisualization/images/border-pixel.png"));
		border.setHorizontalAlignment(pHorizontalAlignment);
		border.setPreserveAspectRatio(false);
		border.setVerticalAlignment(pVerticalAlignment);
		
		pContainer.add(border, constraints);
	}
	
	/**
	 * Creates a new {@link ICellEditor} for a boolean value with the given
	 * {@link IColor}.
	 * 
	 * @param pColor the {@link IColor} to use.
	 * @return the {@link ICellEditor}.
	 */
	private static ICellEditor createBooleanCellEditor(IColor pColor)
	{
		String colorPostfix = ";color=" + UIColor.toHex(pColor);
		
		return new UIChoiceCellEditor(
				new Object[] { Boolean.TRUE, Boolean.FALSE },
				new String[] {
						IFontAwesome.CHECK_SQUARE_SMALL + colorPostfix,
						IFontAwesome.SQUARE_SMALL + colorPostfix });
	}
	
	/**
	 * Creates a new placeholder {@link UIComponent}.
	 * 
	 * @return a new placeholder {@link UIComponent}.
	 */
	private UIComponent<?> createPlaceholder()
	{
		IColor background = Tango.ALUMINIUM_1;
		
		int red = background.getRed();
		int green = background.getGreen();
		int blue = background.getBlue();
		
		red = Math.max(0, Math.min(255, red - random.nextInt(128)));
		green = Math.max(0, Math.min(255, green - random.nextInt(128)));
		blue = Math.max(0, Math.min(255, blue - random.nextInt(128)));
		
		UILabel label = new UILabel("   stub   ");
		label.setBackground(new UIColor(red, green, blue));
		label.setForeground(Tango.ALUMINIUM_6);
		label.setHorizontalAlignment(UILabel.ALIGN_CENTER);
		label.setVerticalAlignment(UILabel.ALIGN_CENTER);
		
		return label;
	}
	
	/**
	 * Updates the information of the currently highlighted {@link IAnchor}.
	 * 
	 * @param pAnchor the currently highlighted {@link IAnchor}.
	 */
	private void updateAnchorInformation(IAnchor pAnchor)
	{
		if (pAnchor != null)
		{
			anchorName.setText(formPanel.getAnchorName(pAnchor));
			
			if (pAnchor.getOrientation() == IAnchor.HORIZONTAL)
			{
				anchorOrientation.setText("Horizontal");
			}
			else
			{
				anchorOrientation.setText("Vertical");
			}
			
			anchorAutoResize.setText("AutoResize: " + Boolean.toString(pAnchor.isAutoSize()));
			anchorPosition.setText("Position: " + Integer.toString(pAnchor.getPosition()));
		}
		else
		{
			anchorName.setText("---");
			anchorOrientation.setText("---");
			anchorAutoResize.setText("AutoResize: ---");
			anchorPosition.setText("Position: ---");
		}
	}
	
	/**
	 * Updates the layout according to the current Lua sourcecode.
	 */
	private void updateLayout()
	{
		errorLabel.setText(null);
		errorLabel.setVisible(false);
		
		formPanel.removeAll();
		formPanel.setVisible(true);
		
		// Reset the Random object ot receive the same colors again.
		random.setSeed(1);
		
		try
		{
			formLayout = new UIFormLayout();
			formPanel.setLayout(formLayout);
			environment.getGlobals().set("layout", CoerceJavaToLua.coerce(formLayout));
			
			environment.execute(codeTextArea.getText());
			
			formPanel.updateAnchorDisplay();
		}
		catch (LuaException e)
		{
			errorLabel.setText("<html>" + e.getMessage() + "</html>");
			errorLabel.setVisible(true);
			formPanel.setVisible(false);
		}
	}
	
	//****************************************************************
	// Subclass definition
	//****************************************************************
	
	/**
	 * Invoked when the values in the {@link #legendDataRow} change.
	 * 
	 * @param pDataRowEvent the event.
	 * @throws ModelException when accessing the data failed.
	 */
	private void doAnchorVisibilityChanged(DataRowEvent pDataRowEvent) throws ModelException
	{
		if (pDataRowEvent.isChangedColumnName("BORDER_ANCHORS_VISIBLE"))
		{
			formPanel.showBorderAnchors(((Boolean)legendDataRow.getValue("BORDER_ANCHORS_VISIBLE")).booleanValue());
		}
		
		if (pDataRowEvent.isChangedColumnName("MARGIN_ANCHORS_VISIBLE"))
		{
			formPanel.showMarginAnchors(((Boolean)legendDataRow.getValue("MARGIN_ANCHORS_VISIBLE")).booleanValue());
		}
		
		if (pDataRowEvent.isChangedColumnName("AUTOSIZE_ANCHORS_VISIBLE"))
		{
			formPanel.showAutosizeAnchors(((Boolean)legendDataRow.getValue("AUTOSIZE_ANCHORS_VISIBLE")).booleanValue());
		}
		
		if (pDataRowEvent.isChangedColumnName("FIXED_ANCHORS_VISIBLE"))
		{
			formPanel.showFixedAnchors(((Boolean)legendDataRow.getValue("FIXED_ANCHORS_VISIBLE")).booleanValue());
		}
		
		if (pDataRowEvent.isChangedColumnName("BACKGROUND_VISIBLE"))
		{
			if (((Boolean)legendDataRow.getValue("BACKGROUND_VISIBLE")).booleanValue())
			{
				formPanel.setBackground(Tango.BUTTER_1);
			}
			else
			{
				formPanel.setBackground(UIColor.white);
			}
		}
	}

	/**
	 * The {@link CodeChangedListener} is a {@link DocumentListener} which
	 * invokes the update.
	 * 
	 * @author Robert Zenz
	 */
	private final class CodeChangedListener implements DocumentListener
	{
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Interface implementation
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void changedUpdate(DocumentEvent pEvent)
		{
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void insertUpdate(DocumentEvent pEvent)
		{
			updateLayout();
		}
		
		@Override
		public void removeUpdate(DocumentEvent pEvent)
		{
			updateLayout();
		}
		
	}	// CodeChangedListener
	
}	// MainFrame
