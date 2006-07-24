/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */
/*
 * AddEditDataSourceDialog.java
 * The UI to Add/Edit Datasources.
 * Created on June 8, 2004, 5:07 PM
 */

package tufts.vue;

/**
 * @version $Revision: 1.44 $ / $Date: 2006-07-24 18:09:50 $ / $Author: mike $
 * @author  akumar03
 */
import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;
import java.awt.*;

import tufts.vue.gui.*;

public class AddLibraryDialog extends SizeRestrictedDialog implements ListSelectionListener, ActionListener {
    
    JPanel addLibraryPanel = new JPanel();
    JList addLibraryList;
    JTextArea descriptionTextArea;
    DefaultListModel listModel = new DefaultListModel();
    JScrollPane listJsp;
    JScrollPane descriptionJsp;
    JPanel progressBarPanel = new JPanel();
    JProgressBar progressBar = new JProgressBar();
    edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
    edu.tufts.vue.dsm.OsidFactory factory;
    org.osid.provider.Provider checked[];
    java.util.Vector checkedVector = new java.util.Vector();
    JButton addButton = new JButton("Add");
    JButton cancelButton = new JButton("Done");
    JPanel buttonPanel = new JPanel();
    DataSourceList dataSourceList;
    DataSource oldDataSource = null;
    edu.tufts.vue.dsm.DataSource newDataSource = null;
    
    private static String MY_COMPUTER = VueResources.getString("addLibrary.mycomputer.label");
    private static String MY_COMPUTER_DESCRIPTION = "Add a browse control for your filesystem.  You can configure where to start the tree.";
    private static String MY_SAVED_CONTENT = "My Saved Content";
    private static String MY_SAVED_CONTENT_DESCRIPTION = "Add a browse control for your saved content.  You can configure a name for this source.";
    private static String FTP = "FTP";
    private static String FTP_DESCRIPTION = "Add a browse control for an FTP site.  You must configure this.";
    private static String TITLE = "Add a Resource";
    private static String AVAILABLE = "Resources available:";
    private final Icon remoteIcon = VueResources.getImageIcon("dataSourceRemote");
    private ProviderListCellRenderer providerListRenderer;
    private Timer timer;
    
    public AddLibraryDialog(DataSourceList dataSourceList) {    	
        super(VUE.getDialogParentAsFrame(),TITLE,true);        
        this.dataSourceList = dataSourceList;
        
        try {
            factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert("Error instantiating Provider support","Error");
        }
        
        try {
        	VueLabel helpButton = new VueLabel(VueResources.getImageIcon("addLibrary.helpIcon"));
            helpButton.setToolTipText("Help Text");
            
            String helpText = VueResources.getString("addLibrary.helpText");
            
            if (helpText != null)
                helpButton.setToolTipText(helpText);
            
        	setLayout(new GridBagLayout());
            addLibraryList = new JList(listModel);
            addLibraryList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
            addLibraryList.addListSelectionListener(this);
            addLibraryList.setFixedCellHeight(25);
            
            providerListRenderer = new ProviderListCellRenderer();
            addLibraryList.setCellRenderer(providerListRenderer);
            
            descriptionTextArea = new JTextArea();
            descriptionTextArea.setMargin(new Insets(4,4,4,4));
            descriptionTextArea.setLineWrap(true);
            descriptionTextArea.setWrapStyleWord(true);
       
            
            progressBar.setIndeterminate(true);
            
            
            listJsp = new JScrollPane(addLibraryList);
            listJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            listJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            listJsp.setPreferredSize(new Dimension(300,180));
            
            descriptionTextArea.setText("description");
            descriptionJsp = new JScrollPane(descriptionTextArea);
            descriptionJsp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            descriptionJsp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            descriptionJsp.setPreferredSize(new Dimension(300,180));
            
            addLibraryPanel.setBackground(VueResources.getColor("White"));
            setBackground(VueResources.getColor("White"));
            
            java.awt.GridBagLayout gbLayout = new java.awt.GridBagLayout();
            java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();

            gbConstraints.insets = new java.awt.Insets(2,2,2,2);
            addLibraryPanel.setLayout(gbLayout);
            
            JLabel avail = new JLabel(AVAILABLE);
            JPanel availabilityPanel = new JPanel();            
            availabilityPanel.setLayout(new BorderLayout());
            availabilityPanel.add(avail,BorderLayout.CENTER);
            availabilityPanel.add(helpButton,BorderLayout.EAST);
            

            gbConstraints.gridx = 0;
            gbConstraints.gridy = 0;
            gbConstraints.gridwidth = 1;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weightx=1;
            gbConstraints.weighty=0;
            gbConstraints.insets = new Insets(4,15,4,15);
            
            addLibraryPanel.add(availabilityPanel,gbConstraints);
                                                            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 1;
            gbConstraints.weighty=1;
            addLibraryPanel.add(listJsp,gbConstraints);
 
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 2;
            addLibraryPanel.add(descriptionJsp,gbConstraints);
            
            
            
            buttonPanel.setLayout(new BoxLayout(buttonPanel,BoxLayout.X_AXIS));            
            buttonPanel.add(Box.createHorizontalGlue());
            addButton.setPreferredSize(cancelButton.getPreferredSize());
            buttonPanel.add(cancelButton);            
            cancelButton.addActionListener(this);
            buttonPanel.add(Box.createHorizontalStrut(6));
            buttonPanel.add(addButton);
            addButton.addActionListener(this);
            
            getRootPane().setDefaultButton(addButton);
            
            gbConstraints.gridx = 0;
            gbConstraints.gridy = 3;
            gbConstraints.weighty=0;            
            gbConstraints.anchor=GridBagConstraints.EAST;
            addLibraryPanel.add(buttonPanel,gbConstraints);
 
            gbConstraints.gridx=0;
            gbConstraints.gridy=0;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weighty=0;
            progressBarPanel.setLayout(new GridBagLayout());
            progressBarPanel.add(progressBar,gbConstraints);
            JLabel progressBarLabel = new JLabel(VueResources.getString("addLibrary.retrievingDataLabel"));
            
            gbConstraints.gridx=0;
            gbConstraints.gridy=1;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weighty=0;
            progressBarPanel.add(progressBarLabel,gbConstraints);
            
            
            progressBarPanel.setPreferredSize(new Dimension(368,472));            
            gbConstraints.gridx=0;
            gbConstraints.gridy=0;
            gbConstraints.fill=GridBagConstraints.BOTH;
            gbConstraints.weighty=0;
            getContentPane().add(progressBarPanel,gbConstraints);//,BorderLayout.CENTER);
        //    getContentPane().add(addLibraryPanel,gbConstraints);//,BorderLayout.CENTER);
            pack();
            setLocation(300,300);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        this.setMinSizeRestriction(this.getWidth(),this.getHeight());        
        
        
        //populate();
        PopulateThread t = new PopulateThread();
        t.start();
        setVisible(true);
    }
    
    public void refresh() {
    	getContentPane().remove(addLibraryPanel);
    	java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
    	gbConstraints.gridx=0;
        gbConstraints.gridy=0;
        gbConstraints.fill=GridBagConstraints.BOTH;
        gbConstraints.weighty=0;
        getContentPane().add(progressBarPanel,gbConstraints);//,BorderLayout.CENTER);
    	PopulateThread t = new PopulateThread();
        t.start();
    }
    
    private void populate() {
        listModel.removeAllElements();
		this.oldDataSource = null;
		this.newDataSource = null;
        try {
       //     GUI.activateWaitCursor();
            if (dataSourceManager == null) {
                dataSourceManager = edu.tufts.vue.dsm.impl.VueDataSourceManager.getInstance();
            }
            
            listModel.removeAllElements();
			System.out.println("In Add Library Dialog, asking Provider for list of Providers");
            org.osid.provider.ProviderIterator providerIterator = factory.getProviders();
            while (providerIterator.hasNextProvider()) {
                org.osid.provider.Provider nextProvider = providerIterator.getNextProvider();
                // place all providers on list, whether installed or not, whether duplicates or not
                listModel.addElement(nextProvider);
                checkedVector.addElement(nextProvider);
            }
            // copy to an array
            int size = listModel.size();
            checked = new org.osid.provider.Provider[size];
            for (int i=0; i < size; i++) {
                checked[i] = (org.osid.provider.Provider)checkedVector.elementAt(i);
            }
            
        } catch (Throwable t) {
            t.printStackTrace();
            VueUtil.alert(t.getMessage(),"Error");
        } finally {
  //          GUI.clearWaitCursor();
        }
        // add all data sources we include with VUE
        listModel.addElement(MY_COMPUTER);
        listModel.addElement(MY_SAVED_CONTENT);
        listModel.addElement(FTP);
    }
    
    public void valueChanged(ListSelectionEvent lse) {
        int index = ((JList)lse.getSource()).getSelectedIndex();
        if (index != -1) {
            try {
                if (((JList)lse.getSource()).getSelectedValue() instanceof String) {
                    String s = (String)(((JList)lse.getSource()).getSelectedValue());
                    if (s.equals(MY_COMPUTER)) {
                        descriptionTextArea.setText(MY_COMPUTER_DESCRIPTION);
                    } else if (s.equals(MY_SAVED_CONTENT)) {
                        descriptionTextArea.setText(MY_SAVED_CONTENT_DESCRIPTION);
                    } else if (s.equals(FTP)) {
                        descriptionTextArea.setText(FTP_DESCRIPTION);
                    }
                } else {
                    org.osid.provider.Provider p = (org.osid.provider.Provider)(((JList)lse.getSource()).getSelectedValue());
                    descriptionTextArea.setText(p.getDescription());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
    
    public void add() {
        
        try {
			boolean proceed = true;
            this.oldDataSource = null;
            Object o = addLibraryList.getSelectedValue();
            String xml = null;
            String s = null;
            
            if (o instanceof String) {
                s = (String)o;
                if (s.equals(MY_COMPUTER)) {
                    LocalFileDataSource ds = new LocalFileDataSource(MY_COMPUTER,"");
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Starting path</title><description>The path to start from</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>512</maxChars><ui>8</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    String address = ds.getAddress();
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    this.oldDataSource = ds;
                } else if (s.equals(MY_SAVED_CONTENT)) {
                    FavoritesDataSource ds = new FavoritesDataSource(MY_SAVED_CONTENT);
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Name</title><description>Name for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    this.oldDataSource = ds;
                } else if (s.equals(FTP)) {
                    RemoteFileDataSource ds = new RemoteFileDataSource();
                    xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><configuration><field><key>name</key><title>Display Name</title><description>Dane for this datasource</description><default>DEFAULT_NAME</default><mandatory>true</mandatory><maxChars></maxChars><ui>0</ui></field><field><key>address</key><title>Address</title><description>FTP Address</description><default>DEFAULT_ADDRESS</default><mandatory>true</mandatory><maxChars>256</maxChars><ui>0</ui></field><field><key>username</key><title>Username</title><description>FTP site username</description><default>DEFAULT_USERNAME</default><mandatory>true</mandatory><maxChars>64</maxChars><ui>0</ui></field><field><key>password</key><title>Password</title><description>FTP site password for username</description><default>DEFAULT_PASSWORD</default><mandatory>true</mandatory><maxChars></maxChars><ui>1</ui></field></configuration>";
                    String name = ds.getDisplayName();
                    if (name == null) name = "";
                    String address = ds.getAddress();
                    if (address == null) address = "";
                    String username = ds.getUserName();
                    if (username == null) username = "";
                    String password = ds.getPassword();
                    if (password == null) password = "";
                    xml = xml.replaceFirst("DEFAULT_NAME",name);
                    xml = xml.replaceFirst("DEFAULT_ADDRESS",address);
                    xml = xml.replaceFirst("DEFAULT_USERNAME",username);
                    xml = xml.replaceFirst("DEFAULT_PASSWORD",password);
                    this.oldDataSource = ds;
                }
            } else {
                org.osid.provider.Provider provider = (org.osid.provider.Provider)o;
                
                edu.tufts.vue.dsm.DataSource ds = null;
                // show dialog containing license, if any
                try {
                    if (provider.requestsLicenseAcknowledgement()) {
                        String license = provider.getLicense();
                        if (license != null) {
                            javax.swing.JTextArea area = new javax.swing.JTextArea();
                            area.setLineWrap(true);
                            area.setText(license);
                            area.setEditable(false);
                            area.setSize(new Dimension(500,300));
                            if (javax.swing.JOptionPane.showOptionDialog(this,
                                    area,
                                    "License Acknowledgement",
                                    javax.swing.JOptionPane.DEFAULT_OPTION,
                                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    new Object[] {
                                "Accept", "Decline"
                            },
                                    "Decline") != 0) {
                                return;
                            }
                        }
                    }
                    
					System.out.println("In Add Library Dialog, checking if provider is installed");
                    if (proceed && (!provider.isInstalled())) {
						System.out.println("In Add Library Dialog, provider not yet installed, installing...");
                        factory = edu.tufts.vue.dsm.impl.VueOsidFactory.getInstance();
                        try {
                            GUI.activateWaitCursor();
                            factory.installProvider(provider.getId());                            
                        } catch (Throwable t1) {
                            //System.out.println("install failed " + provider.getId().getIdString());
                            VueUtil.alert("Installation Failed","Error");
                            return;
                        } 
                    } else {
						System.out.println("In Add Library Dialog, provider already installed");
                    }
                    
                    if (proceed) {
                        // add to data sources list
                        try {
                            //System.out.println("creating data source");
                            ds = new edu.tufts.vue.dsm.impl.VueDataSource(factory.getIdManagerInstance().createId(),
                                    provider.getId(),
                                    true);
                        } catch (Throwable t) {
                            VueUtil.alert("Loading Manager Failed","Error");
                            return;
                        }
                        //System.out.println("created data source");
                        
                        // show configuration, if needed
                        if (ds.hasConfiguration()) {
                            xml = ds.getConfigurationUIHints();
                        } else {
                            //System.out.println("No configuration to show");
                        }
                        this.newDataSource = ds;
                    }
                } catch (Throwable t) {
                    //System.out.println("configuration setup failed");
                    VueUtil.alert(t.getMessage(),"OSID Installation Error");
                    t.printStackTrace();
                    return;
                }
            }
            
            if (xml != null) {
                edu.tufts.vue.ui.ConfigurationUI cui =
                        new edu.tufts.vue.ui.ConfigurationUI(new java.io.ByteArrayInputStream(xml.getBytes()));
                cui.setPreferredSize(new Dimension(400,200));
                
                if (javax.swing.JOptionPane.showOptionDialog(this,
                        cui,
                        "Configuration",
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[] {
                    "Cancel", "Continue"
                },
                        "Continue") == 0) {
					proceed = false;
				} else {
                    if (s != null) {
                        if (s.equals(MY_COMPUTER)) {
                            java.util.Properties p = cui.getProperties();
                            LocalFileDataSource ds = (LocalFileDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                            ds.setAddress(p.getProperty("address"));
                        } else if (s.equals(MY_SAVED_CONTENT)) {
                            java.util.Properties p = cui.getProperties();
                            FavoritesDataSource ds = (FavoritesDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                        } else if (s.equals(FTP)) {
                            java.util.Properties p = cui.getProperties();
                            RemoteFileDataSource ds = (RemoteFileDataSource)this.oldDataSource;
                            ds.setDisplayName(p.getProperty("name"));
                            ds.setUserName(p.getProperty("username"));
                            ds.setPassword(p.getProperty("password"));
                            try {
                                ds.setAddress(p.getProperty("address")); // this must be set last
                            } catch (Exception ex) {
                                // ignore any error for now
                            }
                        }
                    } else {
                        try {
                            GUI.activateWaitCursor();
                            this.newDataSource.setConfiguration(cui.getProperties());
                            GUI.invokeAfterAWT(new Runnable() { public void run() {
                                try {
                                    synchronized (dataSourceManager) {
                                        dataSourceManager.save();
                                    }
                                } catch (Throwable t) {
                                    System.out.println(t.getMessage());
                                }
                            }});
                            
                        } catch (Throwable t2) {                   
                            t2.printStackTrace();
                        } finally {
                            GUI.clearWaitCursor();
                        }
                    }
                }
            }
			if (proceed) {
				if (this.oldDataSource != null) {
					dataSourceList.addOrdered(this.oldDataSource);
				} else {
					dataSourceList.addOrdered(this.newDataSource);
					dataSourceManager.add(this.newDataSource);
				}
				providerListRenderer.setChecked(addLibraryList.getSelectedIndex());
			}
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            providerListRenderer.endWaitingMode();
            addLibraryList.repaint();
            GUI.clearWaitCursor();
            timer.stop();
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        DataSourceViewer.saveDataSourceViewer();
    }
	
	public DataSource getOldDataSource()
	{
		return this.oldDataSource;
	}
	
	public edu.tufts.vue.dsm.DataSource getNewDataSource()
	{
		return this.newDataSource;
	}
    
    public void actionPerformed(ActionEvent ae) {
        
        
        if (ae.getActionCommand().equals("Add")) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            GUI.activateWaitCursor();
            providerListRenderer.invokeWaitingMode(addLibraryList.getSelectedIndex());
            repaint();
            int ONE_TNTH_SECOND = 100;
            
            timer = new Timer(ONE_TNTH_SECOND, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    repaint();
                }});
                timer.start();
                
                AddDSThread t = new AddDSThread();
                t.start();
                
        } else {
            providerListRenderer.clearAllChecked();
            setVisible(false);
        }
    }
    private class AddDSThread extends Thread {
        public AddDSThread() {
            super();
        }
        public void run() {
            add();
        }
    }
    
    private class PopulateThread extends Thread {
        public PopulateThread() {
            super();
        }
        public void run() {
        	try
        	{
        		populate();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();        		
        	}
            finally
            {
            	java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();            
            	gbConstraints.insets = new Insets(4,15,4,15);
            	gbConstraints.gridwidth = 1;
            	gbConstraints.fill=GridBagConstraints.BOTH;
            	gbConstraints.weightx=1;            
            	getContentPane().remove(progressBarPanel);
            	gbConstraints.gridx=0;
            	gbConstraints.gridy=0;
            	gbConstraints.fill=GridBagConstraints.BOTH;
            	gbConstraints.weighty=1;
            	getContentPane().add(addLibraryPanel,gbConstraints);//,BorderLayout.CENTER);
            	validate();
            	repaint();
            }
            
        }
    }
}



