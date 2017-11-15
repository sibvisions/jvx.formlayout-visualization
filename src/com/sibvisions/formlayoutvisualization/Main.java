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

import javax.rad.genui.UIFactoryManager;
import javax.swing.UIManager;

import com.sibvisions.rad.ui.swing.impl.SwingFactory;

/**
 * The {@link Main} is the main entry point for the application.
 * 
 * @author Robert Zenz
 */
public final class Main
{
	
	/**
	 * The main method.
	 *
	 * @param pArgs the arguments.
	 */
	public static void main(String[] pArgs)
	{
		try
		{
			UIFactoryManager.getFactoryInstance(SwingFactory.class);
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			UIFactoryManager.getFactory().invokeAndWait(() ->
			{
				MainFrame frame = new MainFrame();
				frame.setVisible(true);
				frame.eventWindowClosed().addListener(pEvent -> System.exit(0));
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			System.exit(1);
		}
	}
	
}	// Main
