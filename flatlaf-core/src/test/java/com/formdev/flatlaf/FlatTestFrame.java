/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.SystemInfo;
import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.*;

/**
 * @author Karl Tauber
 */
public class FlatTestFrame
	extends JFrame
{
	private static final String PREFS_ROOT_PATH = "/flatlaf-test";
	private static final String KEY_LAF = "laf";
	private static final String KEY_SCALE_FACTOR = "scaleFactor";

	private final String title;
	private JComponent content;
	private FlatInspector inspector;

	public static FlatTestFrame create( String[] args, String title ) {
		Preferences prefs = Preferences.userRoot().node( PREFS_ROOT_PATH );

		// set scale factor
		if( System.getProperty( "flatlaf.uiScale", System.getProperty( "sun.java2d.uiScale" ) ) == null ) {
			String scaleFactor = prefs.get( KEY_SCALE_FACTOR, null );
			if( scaleFactor != null )
				System.setProperty( "flatlaf.uiScale", scaleFactor );
		}

		// set look and feel
		try {
			if( args.length > 0 )
				UIManager.setLookAndFeel( args[0] );
			else {
				String lafClassName = prefs.get( KEY_LAF, FlatLightLaf.class.getName() );
				UIManager.setLookAndFeel( lafClassName );
			}
		} catch( Exception ex ) {
			ex.printStackTrace();

			// fallback
			try {
				UIManager.setLookAndFeel( new FlatLightLaf() );
			} catch( Exception ex2 ) {
				ex2.printStackTrace();
			}
		}

		// create frame
		return new FlatTestFrame( title );
	}

	private FlatTestFrame( String title ) {
		this.title = title;

		initComponents();

		// initialize look and feels combo box
		DefaultComboBoxModel<LafInfo> lafModel = new DefaultComboBoxModel<>();
		lafModel.addElement( new LafInfo( "Flat Light (F1)", FlatLightLaf.class.getName() ) );
		lafModel.addElement( new LafInfo( "Flat Dark (F2)", FlatDarkLaf.class.getName() ) );
		lafModel.addElement( new LafInfo( "Flat Test (F3)", FlatTestLaf.class.getName() ) );
		lafModel.addElement( new LafInfo( "Flat IntelliJ (F4)", FlatIntelliJLaf.class.getName() ) );
		lafModel.addElement( new LafInfo( "Flat Darcula (F5)", FlatDarculaLaf.class.getName() ) );

		UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
		for( UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels ) {
			String name = lookAndFeel.getName();
			String className = lookAndFeel.getClassName();
			if( className.equals( "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel" ) ||
				className.equals( "com.sun.java.swing.plaf.motif.MotifLookAndFeel" ) )
			  continue;

			if( (SystemInfo.IS_WINDOWS && className.equals( "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" )) ||
				(SystemInfo.IS_MAC && className.equals( "com.apple.laf.AquaLookAndFeel") ) )
				name += " (F9)";
			else if( className.equals( MetalLookAndFeel.class.getName() ) )
				name += " (F10)";
			else if( className.equals( NimbusLookAndFeel.class.getName() ) )
				name += " (F11)";

			lafModel.addElement( new LafInfo( name, className ) );
		}

		LookAndFeel activeLaf = UIManager.getLookAndFeel();
		String activeLafClassName = activeLaf.getClass().getName();
		int sel = lafModel.getIndexOf( new LafInfo( null, activeLafClassName ) );
		if( sel < 0 ) {
			lafModel.addElement( new LafInfo( activeLaf.getName(), activeLafClassName ) );
			sel = lafModel.getSize() - 1;
		}
		lafModel.setSelectedItem( lafModel.getElementAt( sel ) );

		lookAndFeelComboBox.setModel( lafModel );

		updateScaleFactorComboBox();
		String scaleFactor = System.getProperty( "flatlaf.uiScale", System.getProperty( "sun.java2d.uiScale" ) );
		if( scaleFactor != null )
			scaleFactorComboBox.setSelectedItem( scaleFactor );

		// register F1, F2 and F3 keys to switch to Light, Dark or Test LaF
		registerSwitchToLookAndFeel( KeyEvent.VK_F1, FlatLightLaf.class.getName() );
		registerSwitchToLookAndFeel( KeyEvent.VK_F2, FlatDarkLaf.class.getName() );
		registerSwitchToLookAndFeel( KeyEvent.VK_F3, FlatTestLaf.class.getName() );
		registerSwitchToLookAndFeel( KeyEvent.VK_F4, FlatIntelliJLaf.class.getName() );
		registerSwitchToLookAndFeel( KeyEvent.VK_F5, FlatDarculaLaf.class.getName() );

		if( SystemInfo.IS_WINDOWS )
			registerSwitchToLookAndFeel( KeyEvent.VK_F9, "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" );
		else if( SystemInfo.IS_MAC )
			registerSwitchToLookAndFeel( KeyEvent.VK_F9, "com.apple.laf.AquaLookAndFeel" );
		registerSwitchToLookAndFeel( KeyEvent.VK_F10, MetalLookAndFeel.class.getName() );
		registerSwitchToLookAndFeel( KeyEvent.VK_F11, NimbusLookAndFeel.class.getName() );

		// register ESC key to close frame
		((JComponent)getContentPane()).registerKeyboardAction(
			e -> {
				dispose();
			},
			KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0, false ),
			JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );

		// close frame
		closeButton.addActionListener(e -> dispose());

		// update title
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowOpened( WindowEvent e ) {
				updateTitle();
			}
			@Override
			public void windowActivated( WindowEvent e ) {
				updateTitle();
			}
		} );
	}

	private void updateTitle() {
		double systemScaleFactor = UIScale.getSystemScaleFactor( getGraphicsConfiguration() );
		float userScaleFactor = UIScale.getUserScaleFactor();
		setTitle( title + " (Java " + System.getProperty( "java.version" )
			+ (systemScaleFactor != 1 ? (";  system scale factor " + systemScaleFactor) : "")
			+ (userScaleFactor != 1 ? (";  user scale factor " + userScaleFactor) : "")
			+ (systemScaleFactor == 1 && userScaleFactor == 1 ? "; no scaling" : "")
			+ ")" );
	}

	private void registerSwitchToLookAndFeel( int keyCode, String lafClassName ) {
		((JComponent)getContentPane()).registerKeyboardAction(
			e -> {
				selectLookAndFeel( lafClassName );
			},
			KeyStroke.getKeyStroke( keyCode, 0, false ),
			JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
	}

	protected void showFrame( JComponent content ) {
		this.content = content;

		contentPanel.getContentPane().add( content );
		pack();
		setLocationRelativeTo( null );
		setVisible( true );

		EventQueue.invokeLater( () -> {
			closeButton.requestFocusInWindow();
		} );
	}

	private void selectLookAndFeel( String lafClassName ) {
		DefaultComboBoxModel<LafInfo> lafModel = (DefaultComboBoxModel<LafInfo>) lookAndFeelComboBox.getModel();
		int sel = lafModel.getIndexOf( new LafInfo( null, lafClassName ) );
		if( sel >= 0 )
			lookAndFeelComboBox.setSelectedIndex( sel );
	}

	private void lookAndFeelChanged() {
		LafInfo newLaf = (LafInfo) lookAndFeelComboBox.getSelectedItem();
		if( newLaf == null )
			return;

		if( newLaf.className.equals( UIManager.getLookAndFeel().getClass().getName() ) )
			return;

		// hide popup to avoid occasional StackOverflowError when updating UI
		lookAndFeelComboBox.setPopupVisible( false );

		Preferences.userRoot().node( PREFS_ROOT_PATH ).put( KEY_LAF, newLaf.className );

		applyLookAndFeel( newLaf.className, false );
	}

	private void applyLookAndFeel( String lafClassName, boolean pack ) {
		EventQueue.invokeLater( () -> {
			try {
				// change look and feel
				UIManager.setLookAndFeel( lafClassName );

				// update title because user scale factor may change
				updateTitle();

				// enable/disable scale factor combobox
				updateScaleFactorComboBox();

				// update all components
				SwingUtilities.updateComponentTreeUI( this );

				// increase size of frame if necessary
				if( pack )
					pack();
				else {
					int width = getWidth();
					int height = getHeight();
					Dimension prefSize = getPreferredSize();
					if( prefSize.width > width || prefSize.height > height )
						setSize( Math.max( prefSize.width, width ), Math.max( prefSize.height, height ) );
				}

				// limit frame size to screen size
				Rectangle screenBounds = getGraphicsConfiguration().getBounds();
				screenBounds = FlatUIUtils.subtractInsets( screenBounds, getToolkit().getScreenInsets( getGraphicsConfiguration() ) );
				Dimension frameSize = getSize();
				if( frameSize.width > screenBounds.width || frameSize.height > screenBounds.height )
					setSize( Math.min( frameSize.width, screenBounds.width ), Math.min( frameSize.height, screenBounds.height ) );

				// move frame to left/top if necessary
				if( getX() + getWidth() > screenBounds.x + screenBounds.width ||
					getY() + getHeight() > screenBounds.y + screenBounds.height )
				{
					setLocation( Math.min( getX(), screenBounds.x + screenBounds.width - getWidth() ),
								 Math.min( getY(), screenBounds.y + screenBounds.height - getHeight() ) );
				}

				if( inspector != null )
					inspector.update();

			} catch( Exception ex ) {
				ex.printStackTrace();
			}
		} );
	}

	private void explicitColorsChanged() {
		EventQueue.invokeLater( () -> {
			boolean explicit = explicitColorsCheckBox.isSelected();
			ColorUIResource restoreColor = new ColorUIResource( Color.white );

			explicitColors( content, explicit, restoreColor );

			// because colors may depend on state (e.g. disabled JTextField)
			// it is best to update all UI delegates to get correct result
			if( !explicit )
				SwingUtilities.updateComponentTreeUI( content );
		} );
	}

	private void explicitColors( Container container, boolean explicit, ColorUIResource restoreColor ) {
		for( Component c : container.getComponents() ) {
			if( c instanceof JPanel ) {
				explicitColors( (JPanel) c, explicit, restoreColor );
				continue;
			}

			c.setForeground( explicit ? Color.blue : restoreColor );
			c.setBackground( explicit ? Color.red : restoreColor );

			if( c instanceof JScrollPane ) {
				Component view = ((JScrollPane)c).getViewport().getView();
				if( view != null ) {
					view.setForeground( explicit ? Color.magenta : restoreColor );
					view.setBackground( explicit ? Color.orange : restoreColor );
				}
			} else if( c instanceof JTabbedPane ) {
				JTabbedPane tabPane = (JTabbedPane)c;
				int tabCount = tabPane.getTabCount();
				for( int i = 0; i < tabCount; i++ ) {
					Component tab = tabPane.getComponentAt( i );
					if( tab != null ) {
						tab.setForeground( explicit ? Color.magenta : restoreColor );
						tab.setBackground( explicit ? Color.orange : restoreColor );
					}
				}
			}

			if( c instanceof JToolBar )
				explicitColors( (JToolBar) c, explicit, restoreColor );
		}

	}

	private void rightToLeftChanged() {
		contentPanel.applyComponentOrientation( rightToLeftCheckBox.isSelected()
			? ComponentOrientation.RIGHT_TO_LEFT
			: ComponentOrientation.LEFT_TO_RIGHT );
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private void enabledChanged() {
		enabledDisable( content, enabledCheckBox.isSelected() );
	}

	private void enabledDisable( Container container, boolean enabled ) {
		for( Component c : container.getComponents() ) {
			if( c instanceof JPanel ) {
				enabledDisable( (JPanel) c, enabled );
				continue;
			}

			c.setEnabled( enabled );

			if( c instanceof JScrollPane ) {
				Component view = ((JScrollPane)c).getViewport().getView();
				if( view != null )
					view.setEnabled( enabled );
			} else if( c instanceof JTabbedPane ) {
				JTabbedPane tabPane = (JTabbedPane)c;
				int tabCount = tabPane.getTabCount();
				for( int i = 0; i < tabCount; i++ ) {
					Component tab = tabPane.getComponentAt( i );
					if( tab != null )
						tab.setEnabled( enabled );
				}
			}

			if( c instanceof JToolBar )
				enabledDisable( (JToolBar) c, enabled );
		}
	}

	private void inspectChanged() {
		if( inspector == null )
			inspector = new FlatInspector( contentPanel );
		inspector.setEnabled( inspectCheckBox.isSelected() );
	}

	private void scaleFactorChanged() {
		String scaleFactor = (String) scaleFactorComboBox.getSelectedItem();
		if( "default".equals( scaleFactor ) )
			scaleFactor = null;

		// hide popup to avoid occasional StackOverflowError when updating UI
		scaleFactorComboBox.setPopupVisible( false );

		Preferences prefs = Preferences.userRoot().node( PREFS_ROOT_PATH );

		if( scaleFactor != null ) {
			System.setProperty( "flatlaf.uiScale", scaleFactor );
			prefs.put( KEY_SCALE_FACTOR, scaleFactor );
		} else {
			System.clearProperty( "flatlaf.uiScale" );
			prefs.remove( KEY_SCALE_FACTOR );
		}

		applyLookAndFeel( UIManager.getLookAndFeel().getClass().getName(), true );
	}

	private void updateScaleFactorComboBox() {
		scaleFactorComboBox.setEnabled( !UIScale.isJreHiDPIEnabled() && UIManager.getLookAndFeel() instanceof FlatLaf );
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		dialogPane = new JPanel();
		contentPanel = new JRootPane();
		buttonBar = new JPanel();
		lookAndFeelComboBox = new JComboBox<>();
		explicitColorsCheckBox = new JCheckBox();
		rightToLeftCheckBox = new JCheckBox();
		enabledCheckBox = new JCheckBox();
		inspectCheckBox = new JCheckBox();
		scaleFactorComboBox = new JComboBox<>();
		closeButton = new JButton();

		//======== this ========
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());

		//======== dialogPane ========
		{
			dialogPane.setLayout(new BorderLayout());

			//======== contentPanel ========
			{
				Container contentPanelContentPane = contentPanel.getContentPane();
				contentPanelContentPane.setLayout(new MigLayout(
					"insets dialog,hidemode 3",
					// columns
					"[grow,fill]",
					// rows
					"[grow,fill]"));
			}
			dialogPane.add(contentPanel, BorderLayout.CENTER);

			//======== buttonBar ========
			{
				buttonBar.setLayout(new MigLayout(
					"insets dialog",
					// columns
					"[fill]" +
					"[fill]" +
					"[fill]" +
					"[fill]" +
					"[fill]" +
					"[fill]" +
					"[grow,fill]" +
					"[button,fill]",
					// rows
					null));

				//---- lookAndFeelComboBox ----
				lookAndFeelComboBox.addActionListener(e -> lookAndFeelChanged());
				buttonBar.add(lookAndFeelComboBox, "cell 0 0");

				//---- explicitColorsCheckBox ----
				explicitColorsCheckBox.setText("explicit colors");
				explicitColorsCheckBox.setMnemonic('X');
				explicitColorsCheckBox.addActionListener(e -> explicitColorsChanged());
				buttonBar.add(explicitColorsCheckBox, "cell 1 0");

				//---- rightToLeftCheckBox ----
				rightToLeftCheckBox.setText("right-to-left");
				rightToLeftCheckBox.setMnemonic('R');
				rightToLeftCheckBox.addActionListener(e -> rightToLeftChanged());
				buttonBar.add(rightToLeftCheckBox, "cell 2 0");

				//---- enabledCheckBox ----
				enabledCheckBox.setText("enabled");
				enabledCheckBox.setMnemonic('E');
				enabledCheckBox.setSelected(true);
				enabledCheckBox.addActionListener(e -> enabledChanged());
				buttonBar.add(enabledCheckBox, "cell 3 0");

				//---- inspectCheckBox ----
				inspectCheckBox.setText("inspect");
				inspectCheckBox.setMnemonic('I');
				inspectCheckBox.addActionListener(e -> inspectChanged());
				buttonBar.add(inspectCheckBox, "cell 4 0");

				//---- scaleFactorComboBox ----
				scaleFactorComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
					"default",
					"1",
					"1.25",
					"1.5",
					"1.75",
					"2.0",
					"2.25",
					"2.5",
					"3",
					"3.5",
					"4"
				}));
				scaleFactorComboBox.setMaximumRowCount(20);
				scaleFactorComboBox.addActionListener(e -> scaleFactorChanged());
				buttonBar.add(scaleFactorComboBox, "cell 5 0");

				//---- closeButton ----
				closeButton.setText("Close");
				buttonBar.add(closeButton, "cell 7 0");
			}
			dialogPane.add(buttonBar, BorderLayout.SOUTH);
		}
		contentPane.add(dialogPane, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JPanel dialogPane;
	private JRootPane contentPanel;
	private JPanel buttonBar;
	private JComboBox<LafInfo> lookAndFeelComboBox;
	private JCheckBox explicitColorsCheckBox;
	private JCheckBox rightToLeftCheckBox;
	private JCheckBox enabledCheckBox;
	private JCheckBox inspectCheckBox;
	private JComboBox<String> scaleFactorComboBox;
	private JButton closeButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables

	//---- class LafInfo ------------------------------------------------------

	static class LafInfo
	{
		final String name;
		final String className;

		LafInfo( String name, String className ) {
			this.name = name;
			this.className = className;
		}

		@Override
		public boolean equals( Object obj ) {
			return obj instanceof LafInfo && className.equals( ((LafInfo)obj).className );
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
