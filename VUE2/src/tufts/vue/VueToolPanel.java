package tufts.vue;


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
* VueToolPanel
*
* The VueToolPanel is the component that holds the main VUE toolbar
* and the contextual properties tools.  It listens for selection events
* and broadcasts tool and property change events to registered listeners.
*
**/
public class VueToolPanel extends JPanel
 {

	
	////////////////
	// Fields
	////////////////
	
	/** the panel where the main tools are placed **/
	private JPanel mMainToolPanel = null;
	
	/** the panel where contextual tools are placed **/
	private JPanel mContextualToolPanel = null;
	
	/** the button group used for tool selection **/
	private ButtonGroup mButtonGroup = null;
	
	/** the list of VueTools in the main tool panel **/
	private Vector mTools = new Vector();
	
	/** the current tool selection (TO DO:  remove this)  **/
	private VueTool mCurrentTool = null;
	
	
	
	
	
	///////////////
	// Constructors
	//////////////////
	
	
	/***
	* VueToolPanel()
	* The constructor that builds an initial VUE ToolPanel
	**/
	public VueToolPanel() {
		super();
		mButtonGroup = new ButtonGroup();
		
		
		BoxLayout layoutMrg = new BoxLayout( this, BoxLayout.X_AXIS);
		this.setLayout( layoutMrg);
		
		mMainToolPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout( mMainToolPanel, BoxLayout.X_AXIS);
		mMainToolPanel.setLayout(boxLayout);
		
		mContextualToolPanel = new JPanel();
		BoxLayout ctpLayout = new BoxLayout(  mContextualToolPanel, BoxLayout.X_AXIS); 
		mContextualToolPanel.setLayout( ctpLayout ); 
		
				
		this.add( mMainToolPanel);
		this.add( mContextualToolPanel);
		
	}
	
	
	/**
	 * addToolButton
	 * This method adds a PaletteButton to the main tool panel as
	 * a tool to be used in the set of main tools
	 *
	 * @param PaletteButton - the button to add
	 **/
	public void addToolButton( PaletteButton pButton) {

		mMainToolPanel.add( pButton);
		mButtonGroup.add( pButton);
		if( mButtonGroup.getButtonCount() == 1) {
			pButton.setSelected( true);
			}
	}
	
	
	/**
	 * addTools()
	 * This method adds an array of VueTool items and creates
	 * main toolbar buttons based on the VueTool.
	 *
	 * @param VueTool [] - the list of tools
	 **/
	public void addTools( VueTool [] pTools) {
		for( int i=0; i<pTools.length; i++) {
				addTool( pTools[i] );
				}
	}
	
	
	/**
	 * addTool
	 * This method adds a single VueTool to the main toolbar.
	 * It creates a PaleteButton for the tool and adds it to the toolbar panel.
	 *
	 * #param VueTool - the tool to add.
	 **/
	public void addTool( VueTool pTool) {
	
		if( mTools == null) {
			mTools = new Vector();
			}
		mTools.add( pTool);
		PaletteButton button = createPaletteButton( pTool);
		addToolButton( button);
		
	}
	
	/**
	 * getSelectedTool
	 * This method returns the selected tool based on the radio group
	 **/
	 public VueTool getSelectedTool() {
	 	
	 	Enumeration e = mButtonGroup.getElements();
		PaletteButton cur;
		while( e.hasMoreElements() ) {
			cur = (PaletteButton) e.nextElement();
			if( cur.isSelected() ) {
				return  ((VueTool) cur.getContext()) ;
				}
			}
		return null;	 	
	 }
	
	
	/**
	 * setContextualToolPanel
	 * This method sets the contextual tool panel and removes
	 * any components already displayed.
	 **/
	public void setContextualToolPanel( JPanel pPanel) {
		mContextualToolPanel.removeAll();
                if (pPanel == null)
                    System.err.println("null pPanel in setContextualToolPanel");
                else
                    mContextualToolPanel.add( pPanel);
	}
	
	
	/**
	 * removeTool()
	 * This method removes a tool from the VueToolPanel
	 * @param VueTool the tool to remove
	 **/
	public void removeTool( VueTool pTool) {
		mTools.remove( pTool);
		// FFIX:  tbd we might not need to ever remove, only disable.
		// removeToolButton( pTool.getName() );
	}
	
	
	
	
	/**
	 * createPaletteButton
	 * This method creates a GUI PaleteeButton control from the
	 * a VueTool.
	 * 
	 * @param pTool -= the tool to map to aPaletteButton
	 * @return PaletteButton - a PaletteButton with properties based on the VueTool
	 **/
	protected PaletteButton createPaletteButton( VueTool pTool) {
		
		PaletteButton button = null;
		
		if( pTool.hasSubTools()   ) {
				// create button items
			Vector names = pTool.getSubToolIDs();
			int numSubTools = names.size();
			PaletteButtonItem items [] = new PaletteButtonItem[numSubTools];
			for(int i=0; i<numSubTools; i++) {
				String name = (String) names.get(i);
				VueTool subTool = pTool.getSubTool( name);
				if( subTool != null) {
					PaletteButtonItem item = new PaletteButtonItem();
					
					item.setIcon( subTool.getIcon() );
					item.setSelectedIcon( subTool.getSelectedIcon() );
					item.setDisabledIcon( subTool.getDisabledIcon() );
					item.setRolloverIcon( subTool.getRolloverIcon() );
					item.setPressedIcon( subTool.getDownIcon() );
					item.setMenuItemIcon( subTool.getMenuItemIcon() );
					item.setMenuItemSelectedIcon( subTool.getMenuItemSelectedIcon() );
					item.setToolTipText( subTool.getToolTipText() );
					item.addActionListener( subTool);
					
					items[i] = item;
					}
				}
			button = new PaletteButton( items );
			button.setPropertiesFromItem( items[0]);
			button.setOverlayIcons (pTool.getOverlayUpIcon(), pTool.getOverlayDownIcon() );
			}
		else  {  // just a radio-like button, no popup items 
			button = new PaletteButton();
			button.setIcons( pTool.getIcon(), pTool.getDownIcon(), pTool.getSelectedIcon() ,
							pTool.getDisabledIcon(), pTool.getRolloverIcon() );
			
			
			button.setToolTipText( pTool.getToolTipText() );

			}
			
		// set the user context to the VueTOol
		button.setContext( pTool);
		
		button.addActionListener( pTool);
		return button;
	}
	

//  OLD CODE	
//	public void setEnabled( boolean pState) {
//	  // Manual override
//	  super.setEnabled( true);
//	}
	
	
	// DEBUG :
	
	
	protected void processMouseMontionEvent( MouseEvent pEvent) {
		debug("  VueToolPanel: processMouseMotionEvent "+pEvent.getID() );
		super.processMouseEvent( pEvent);
	}


	protected void processMouseEvent( MouseEvent pEvent) {
		debug("  porcessMouseEvent() "+ pEvent.getID()  );
		super.processMouseEvent( pEvent);
	}


	static private boolean sDebug = false;
	private void debug( String pStr) {
		if( sDebug)
			System.out.println( "VueToolPanel - "+pStr);
	}
}
