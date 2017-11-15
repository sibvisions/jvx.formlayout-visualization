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

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.rad.genui.UIComponent;
import javax.rad.genui.container.UIPanel;
import javax.rad.genui.layout.UIFormLayout;
import javax.rad.ui.IColor;
import javax.rad.ui.IComponent;
import javax.rad.ui.IRectangle;
import javax.rad.ui.layout.IFormLayout.IAnchor;
import javax.swing.JPanel;

import com.sibvisions.rad.ui.swing.ext.layout.JVxFormLayout;
import com.sibvisions.util.ArrayUtil;

/**
 * The {@link AnchorShowingPanel} is an {@link UIPanel} extension which displays
 * all anchors of its current layout.
 * <p>
 * This panel only works with an {@link UIFormLayout}.
 * 
 * @author Robert Zenz
 */
public class AnchorShowingPanel extends UIPanel
{
	/** The {@link IColor} for autosizing {@link IAnchor}s. */
	public static final IColor AUTOSIZE_ANCHOR_COLOR = Tango.SKY_BLUE_1;
	
	/** The {@link IColor} for border {@link IAnchor}s. */
	public static final IColor BORDER_ANCHOR_COLOR = Tango.CHOCOLATE_3;
	
	/** The {@link IColor} for fixed {@link IAnchor}s. */
	public static final IColor FIXED_ANCHOR_COLOR = Tango.SKY_BLUE_3;
	
	/** The {@link IColor} for margin {@link IAnchor}s. */
	public static final IColor MARGIN_ANCHOR_COLOR = Tango.ORANGE_2;
	
	/** The object name for the associated {@link IAnchor}. */
	private static final String ANCHOR_OBJECT = "anchor";
	
	/** The size of the displayed {@link IAnchor}. */
	private static final int ANCHOR_SIZE = 1;
	
	/** The object name for the original background {@link IColor}. */
	private static final String BACKGROUND_OBJECT = "background";
	
	/** The range within wich the {@link IAnchor} is highlighted. */
	private static final int SNAP_RANGE = 3;
	
	/** If autosize {@link IAnchor}s are visible. */
	private boolean autosizeAnchorsVisible = true;
	
	/** If order {@link IAnchor}s are visible. */
	private boolean borderAnchorsVisible = true;
	
	/** If fixed {@link IAnchor}s are visible. */
	private boolean fixedAnchorsVisible = true;
	
	/** The currently highlighted {@link IAnchor}. */
	private IAnchor highlightedAnchor = null;
	
	/**
	 * The listener to be notified when the {@link #highlightedAnchor} changes.
	 */
	private Consumer<IAnchor> highlightedAnchorChangedListener = null;
	
	/** If margin {@link IAnchor}s are visible. */
	private boolean marginAnchorsVisible = true;
	
	/** The {@link List} of added placeholders. */
	private List<UIComponent<?>> placeholders = new ArrayList<>();
	
	/** The {@link List} of added {@link IComponent}. */
	private List<IComponent> realComponents = new ArrayList<>();
	
	/** The {@link List} of used constraints. */
	private List<Object> realConstraints = new ArrayList<>();
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Initialization
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Creates a new instance of {@link AnchorShowingPanel}.
	 */
	public AnchorShowingPanel()
	{
		super();
		
		AnchorHighlightingMouseListener listener = new AnchorHighlightingMouseListener();
		
		((JPanel)getResource()).addMouseListener(listener);
		((JPanel)getResource()).addMouseMotionListener(listener);
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Overwritten methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void add(IComponent pComponent, Object pConstraints, int pIndex)
	{
		if (pIndex >= 0)
		{
			realComponents.add(pIndex, pComponent);
			realConstraints.add(pIndex, pConstraints);
		}
		else
		{
			realComponents.add(pComponent);
			realConstraints.add(pConstraints);
		}
		
		super.add(pComponent, pConstraints, pIndex);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public UIFormLayout getLayout()
	{
		return (UIFormLayout)super.getLayout();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int pIndex)
	{
		realComponents.remove(pIndex);
		realConstraints.remove(pIndex);
		
		super.remove(pIndex);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAll()
	{
		realComponents.clear();
		realConstraints.clear();
		
		while (components.size() > 0)
		{
			super.remove(components.size() - 1);
		}
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// User-defined methods
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Gets the name of the given {@link IAnchor}.
	 * 
	 * @param pAnchor the {@link IAnchor} of which to get the name.
	 * @return the name of the given {@link IAnchor}.
	 */
	public String getAnchorName(IAnchor pAnchor)
	{
		if (pAnchor == getLayout().getBottomAnchor())
		{
			return "Border bottom";
		}
		else if (pAnchor == getLayout().getLeftAnchor())
		{
			return "Border left";
		}
		else if (pAnchor == getLayout().getRightAnchor())
		{
			return "Border right";
		}
		else if (pAnchor == getLayout().getTopAnchor())
		{
			return "Border top";
		}
		else if (pAnchor == getLayout().getBottomMarginAnchor())
		{
			return "Margin bottom / b-1";
		}
		else if (pAnchor == getLayout().getLeftMarginAnchor())
		{
			return "Margin left / l0";
		}
		else if (pAnchor == getLayout().getRightMarginAnchor())
		{
			return "Margin right / r-1";
		}
		else if (pAnchor == getLayout().getTopMarginAnchor())
		{
			return "Margin top / t0";
		}
		else
		{
			IAnchor[] horizontalAnchors = getLayout().getHorizontalAnchors();
			
			String name = getHorizontalAnchorName(horizontalAnchors, pAnchor, 0, 1);
			if (name != null)
			{
				return name;
			}
			
			name = getHorizontalAnchorName(horizontalAnchors, pAnchor, -1, -1);
			if (name != null)
			{
				return name;
			}
			
			IAnchor[] verticalAnchors = getLayout().getVerticalAnchors();
			
			name = getVerticalAnchorName(verticalAnchors, pAnchor, 0, 1);
			if (name != null)
			{
				return name;
			}
			
			name = getVerticalAnchorName(verticalAnchors, pAnchor, -1, -1);
			if (name != null)
			{
				return name;
			}
			
			return "???";
		}
	}
	
	/**
	 * Gets the current listener for when the highlighted {@link IAnchor}
	 * changes.
	 * 
	 * @return the current listener for when the highlighted {@link IAnchor}
	 *         changes.
	 */
	public Consumer<IAnchor> getHighlightedAnchorChangedListener()
	{
		return highlightedAnchorChangedListener;
	}
	
	/**
	 * Sets the listener for when the highlighted {@link IAnchor} changes.
	 * 
	 * @param pHighlightedAnchorChangedListener the listener for when the
	 *            highlighted {@link IAnchor} changes.
	 */
	public void setHighlightedAnchorChangedListener(Consumer<IAnchor> pHighlightedAnchorChangedListener)
	{
		highlightedAnchorChangedListener = pHighlightedAnchorChangedListener;
	}
	
	/**
	 * Sets whether autosize {@link IAnchor}s should be visible.
	 * 
	 * @param pShow {@code true} if autosize {@link IAnchor}s should be visible.
	 */
	public void showAutosizeAnchors(boolean pShow)
	{
		autosizeAnchorsVisible = pShow;
		
		updateAnchorDisplay();
	}
	
	/**
	 * Sets whether border {@link IAnchor}s should be visible.
	 * 
	 * @param pShow {@code true} if border {@link IAnchor}s should be visible.
	 */
	public void showBorderAnchors(boolean pShow)
	{
		borderAnchorsVisible = pShow;
		
		updateAnchorDisplay();
	}
	
	/**
	 * Sets whether fixed {@link IAnchor}s should be visible.
	 * 
	 * @param pShow {@code true} if fixed {@link IAnchor}s should be visible.
	 */
	public void showFixedAnchors(boolean pShow)
	{
		fixedAnchorsVisible = pShow;
		
		updateAnchorDisplay();
	}
	
	/**
	 * Sets whether margin {@link IAnchor}s should be visible.
	 * 
	 * @param pShow {@code true} if margin {@link IAnchor}s should be visible.
	 */
	public void showMarginAnchors(boolean pShow)
	{
		marginAnchorsVisible = pShow;
		
		updateAnchorDisplay();
	}
	
	/**
	 * Updates the display of the visible {@link IAnchor}s.
	 */
	public void updateAnchorDisplay()
	{
		placeholders.clear();
		
		while (components.size() > 0)
		{
			super.remove(components.size() - 1);
		}
		
		for (int index = 0; index < realComponents.size(); index++)
		{
			super.add(
					realComponents.get(index),
					realConstraints.get(index),
					-1);
		}
		
		((JVxFormLayout)getLayout().getResource()).invalidateLayout((Container)getResource());
		((JVxFormLayout)getLayout().getResource()).layoutContainer((Container)getResource());
		
		IAnchor[] horizontalAnchors = getLayout().getHorizontalAnchors();
		IAnchor[] verticalAnchors = getLayout().getVerticalAnchors();
		
		// Border
		if (borderAnchorsVisible)
		{
			super.add(createAnchorPlaceholder(BORDER_ANCHOR_COLOR, getLayout().getBottomAnchor()), getLayout().getConstraints(
					null,
					getLayout().getLeftAnchor(),
					getLayout().createAnchor(getLayout().getBottomAnchor()),
					getLayout().getRightAnchor()), 0);
			super.add(createAnchorPlaceholder(BORDER_ANCHOR_COLOR, getLayout().getLeftAnchor()), getLayout().getConstraints(
					getLayout().getTopAnchor(),
					getLayout().createAnchor(getLayout().getLeftAnchor()),
					getLayout().getBottomAnchor(),
					null), 0);
			super.add(createAnchorPlaceholder(BORDER_ANCHOR_COLOR, getLayout().getRightAnchor()), getLayout().getConstraints(
					getLayout().getTopAnchor(),
					null,
					getLayout().getBottomAnchor(),
					getLayout().createAnchor(getLayout().getRightAnchor())), 0);
			super.add(createAnchorPlaceholder(BORDER_ANCHOR_COLOR, getLayout().getTopAnchor()), getLayout().getConstraints(
					getLayout().createAnchor(getLayout().getTopAnchor()),
					getLayout().getLeftAnchor(),
					null,
					getLayout().getRightAnchor()), 0);
		}
		
		// Margin
		if (marginAnchorsVisible)
		{
			super.add(createAnchorPlaceholder(MARGIN_ANCHOR_COLOR, getLayout().getBottomMarginAnchor()), getLayout().getConstraints(
					null,
					getLayout().getLeftAnchor(),
					getLayout().createAnchor(getLayout().getBottomMarginAnchor()),
					getLayout().getRightAnchor()), 0);
			super.add(createAnchorPlaceholder(MARGIN_ANCHOR_COLOR, getLayout().getLeftMarginAnchor()), getLayout().getConstraints(
					getLayout().getTopAnchor(),
					getLayout().createAnchor(getLayout().getLeftMarginAnchor()),
					getLayout().getBottomAnchor(),
					null), 0);
			super.add(createAnchorPlaceholder(MARGIN_ANCHOR_COLOR, getLayout().getRightMarginAnchor()), getLayout().getConstraints(
					getLayout().getTopAnchor(),
					null,
					getLayout().getBottomAnchor(),
					getLayout().createAnchor(getLayout().getRightMarginAnchor())), 0);
			super.add(createAnchorPlaceholder(MARGIN_ANCHOR_COLOR, getLayout().getTopMarginAnchor()), getLayout().getConstraints(
					getLayout().createAnchor(getLayout().getTopMarginAnchor()),
					getLayout().getLeftAnchor(),
					null,
					getLayout().getRightAnchor()), 0);
		}
		
		// Horizontal
		for (IAnchor anchor : horizontalAnchors)
		{
			if (anchor != getLayout().getLeftMarginAnchor()
					&& anchor != getLayout().getRightMarginAnchor())
			{
				if (anchor.isAutoSize())
				{
					if (autosizeAnchorsVisible)
					{
						showAnchor(anchor, AUTOSIZE_ANCHOR_COLOR);
					}
				}
				else
				{
					if (fixedAnchorsVisible)
					{
						showAnchor(anchor, FIXED_ANCHOR_COLOR);
					}
				}
			}
		}
		
		// Vertical
		for (IAnchor anchor : verticalAnchors)
		{
			if (anchor != getLayout().getTopMarginAnchor()
					&& anchor != getLayout().getBottomMarginAnchor())
			{
				if (anchor.isAutoSize())
				{
					if (autosizeAnchorsVisible)
					{
						showAnchor(anchor, AUTOSIZE_ANCHOR_COLOR);
					}
				}
				else
				{
					if (fixedAnchorsVisible)
					{
						showAnchor(anchor, FIXED_ANCHOR_COLOR);
					}
				}
			}
		}
	}
	
	/**
	 * Creates the {@link UIComponent} which is used as placeholder for an
	 * {@link IAnchor}.
	 * 
	 * @param pColor the {@link IColor} the placeholder should have.
	 * @param pAnchor the {@link IAnchor} to for the placeholder.
	 * @return the placeholder {@link UIComponent}.
	 */
	private UIComponent<?> createAnchorPlaceholder(IColor pColor, IAnchor pAnchor)
	{
		UIPanel panel = new UIPanel();
		panel.setBackground(pColor);
		panel.setPreferredSize(ANCHOR_SIZE, ANCHOR_SIZE);
		
		panel.putObject(ANCHOR_OBJECT, pAnchor);
		panel.putObject(BACKGROUND_OBJECT, pColor);
		
		placeholders.add(panel);
		
		return panel;
	}
	
	/**
	 * Fires the {@link #highlightedAnchorChangedListener}, if any.
	 */
	private void fireHighlightedAnchorChanged()
	{
		if (highlightedAnchorChangedListener != null)
		{
			highlightedAnchorChangedListener.accept(highlightedAnchor);
		}
	}
	
	/**
	 * Gets the name of a horizontal {@link IAnchor}.
	 * 
	 * @param pHorizontalAnchors the {@link IAnchor} array of all horizontal
	 *            {@link IAnchor}.
	 * @param pAnchor the {@link IAnchor} of which to get the name.
	 * @param pStartIndex the start index at which to start the search.
	 * @param pIncrement the amount to increment the search.
	 * @return the name of the given {@link IAnchor}, {@code null} if it could
	 *         not be found.
	 */
	private String getHorizontalAnchorName(IAnchor[] pHorizontalAnchors, IAnchor pAnchor, int pStartIndex, int pIncrement)
	{
		int index = pStartIndex;
		
		while (ArrayUtil.indexOfReference(pHorizontalAnchors, getLayout().getColumnLeftAnchor(index)) >= 0)
		{
			if (getLayout().getColumnLeftAnchor(index) == pAnchor)
			{
				return "l" + Integer.toString(index);
			}
			else if (getLayout().getColumnRightAnchor(index) == pAnchor)
			{
				return "r" + Integer.toString(index);
			}
			
			index = index + pIncrement;
		}
		
		return null;
	}
	
	/**
	 * Gets the name of a vertical {@link IAnchor}.
	 * 
	 * @param pHorizontalAnchors the {@link IAnchor} array of all vertical
	 *            {@link IAnchor}.
	 * @param pAnchor the {@link IAnchor} of which to get the name.
	 * @param pStartIndex the start index at which to start the search.
	 * @param pIncrement the amount to increment the search.
	 * @return the name of the given {@link IAnchor}, {@code null} if it could
	 *         not be found.
	 */
	private String getVerticalAnchorName(IAnchor[] pVerticalAnchors, IAnchor pAnchor, int pStartIndex, int pIncrement)
	{
		int index = pStartIndex;
		
		while (ArrayUtil.indexOfReference(pVerticalAnchors, getLayout().getRowTopAnchor(index)) >= 0)
		{
			if (getLayout().getRowTopAnchor(index) == pAnchor)
			{
				return "t" + Integer.toString(index);
			}
			else if (getLayout().getRowBottomAnchor(index) == pAnchor)
			{
				return "b" + Integer.toString(index);
			}
			
			index = index + pIncrement;
		}
		
		return null;
	}
	
	/**
	 * Adds a placeholder for the given {@link IAnchor} with the given
	 * {@link IColor}.
	 * 
	 * @param pAnchor the {@link IAnchor} to make visible.
	 * @param pColor the {@link IColor} to use.
	 */
	private void showAnchor(IAnchor pAnchor, IColor pColor)
	{
		UIComponent<?> placeholder = createAnchorPlaceholder(pColor, pAnchor);
		
		if (pAnchor.getOrientation() == IAnchor.VERTICAL)
		{
			super.add(placeholder, getLayout().getConstraints(
					getLayout().createAnchor(pAnchor),
					getLayout().getLeftMarginAnchor(),
					null,
					getLayout().getRightMarginAnchor()), 0);
		}
		else
		{
			super.add(placeholder, getLayout().getConstraints(
					getLayout().getTopMarginAnchor(),
					getLayout().createAnchor(pAnchor),
					getLayout().getBottomMarginAnchor(),
					null), 0);
		}
	}
	
	//****************************************************************
	// Subclass definition
	//****************************************************************
	
	/**
	 * The {@link AnchorHighlightingMouseListener} is an implementation of
	 * {@link MouseListener} and {@link MouseMotionListener} which updates the
	 * highlighted {@link IAnchor}.
	 * 
	 * @author Robert Zenz
	 */
	private final class AnchorHighlightingMouseListener implements MouseListener, MouseMotionListener
	{
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Class members
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		/** The currently highlighted {@link UIComponent}. */
		private UIComponent<?> highlightedComponent = null;
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Interface implementation
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseClicked(MouseEvent pEvent)
		{
			// Not required.
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseDragged(MouseEvent pEvent)
		{
			// Not required.
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseEntered(MouseEvent pEvent)
		{
			// Not required.
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseExited(MouseEvent pEvent)
		{
			resetHighlightedComponent();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseMoved(MouseEvent pEvent)
		{
			for (UIComponent<?> component : placeholders)
			{
				if (inside(pEvent.getX(), pEvent.getY(), component.getBounds()))
				{
					if (component != highlightedComponent)
					{
						resetHighlightedComponent();
						
						highlightedComponent = component;
						highlightedComponent.setBackground(Tango.SCARLET_RED_1);
						
						highlightedAnchor = (IAnchor)highlightedComponent.getObject(ANCHOR_OBJECT);
						
						fireHighlightedAnchorChanged();
					}
					
					return;
				}
			}
			
			resetHighlightedComponent();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mousePressed(MouseEvent pE)
		{
			// Not required.
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseReleased(MouseEvent pE)
		{
			// Not required.
		}
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// User-defined methods
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		/**
		 * Tests if the given coordinates are within the given
		 * {@link IRectangle}.
		 * 
		 * @param pX the x coordinate.
		 * @param pY the y coordiante.
		 * @param pBounds the {@link IRectangle}.
		 * @return {@code true} if the given coordinates are within the given
		 *         {@link IRectangle}.
		 */
		private boolean inside(int pX, int pY, IRectangle pBounds)
		{
			if (pBounds.getWidth() == ANCHOR_SIZE)
			{
				return pX >= (pBounds.getX() - SNAP_RANGE)
						&& pX <= (pBounds.getX() + pBounds.getWidth() + SNAP_RANGE);
			}
			else
			{
				return pY >= (pBounds.getY() - SNAP_RANGE)
						&& pY <= (pBounds.getY() + pBounds.getHeight() + SNAP_RANGE);
			}
		}
		
		/**
		 * Resets the currently highlighted component.
		 */
		private void resetHighlightedComponent()
		{
			if (highlightedComponent != null)
			{
				highlightedComponent.setBackground((IColor)highlightedComponent.getObject(BACKGROUND_OBJECT));
				highlightedComponent = null;
				highlightedAnchor = null;
				
				fireHighlightedAnchorChanged();
			}
		}
		
	}	// AnchorHighlightingMouseListener
	
}	// AnchorShowingPanel
