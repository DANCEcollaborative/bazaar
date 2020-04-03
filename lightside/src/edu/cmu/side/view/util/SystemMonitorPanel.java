package edu.cmu.side.view.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.util.ThreadPoolManager;

public class SystemMonitorPanel extends JPanel
{
	private static final String SINGLE_CORE_LABEL = "Single Thread";
	private static final ImageIcon SINGLE_CORE_ICON = new ImageIcon("toolkits/icons/arrow_right.png");
	private static final String MULTITHREAD_LABEL = "Multithreaded";
	private static final ImageIcon MULTITHREAD_ICON = new ImageIcon("toolkits/icons/arrow_divide_right.png");
	public static final Object GARBAGE_COLLECTION_OBSERVABLE = "GARBAGE_COLLECTION";
	
	//JLabel textMonitor = new JLabel();
	WarningButton warnButton = new WarningButton();
	//JButton bugButton = linkButton("Report a Bug", "https://bitbucket.org/lightsidelabs/lightside/issues?status=new&status=open", "toolkits/icons/bug.png");
	JButton infoButton = new JButton("Get Support", new ImageIcon("toolkits/icons/help.png"));
	JButton garbageButton = new JButton(new ImageIcon("toolkits/icons/bin_closed.png"));
	JButton parallelButton = new JButton(MULTITHREAD_LABEL, MULTITHREAD_ICON);
	boolean warned = false;

	public SystemMonitorPanel()
	{
		JPanel memories = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		this.setLayout(new BorderLayout(10, 0));
		this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		warnButton.setBorder(BorderFactory.createEmptyBorder());
		garbageButton.setBorder(BorderFactory.createEmptyBorder());
		parallelButton.setBorder(BorderFactory.createEmptyBorder());
		infoButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
		//bugButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
		garbageButton.setHorizontalTextPosition(SwingConstants.LEFT);
		//textMonitor.setBorder(BorderFactory.createEmptyBorder());
		//textMonitor.setFont(textMonitor.getFont().deriveFont(10.0f));
		memories.add(warnButton);
		//memories.add(textMonitor);
		memories.add(garbageButton);
		

		garbageButton.setToolTipText("Force Memory Cleanup");
		garbageButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				int result = JOptionPane.showConfirmDialog(null,
						"Do you want to try forcing memory cleanup?\nThis will freeze LightSide for a while.", "Force Garbage Collection",
						JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION)
				{
					forceCollection();
				}
			}
		});

		garbageButton.setBorderPainted(false);
		garbageButton.setContentAreaFilled(false);
		garbageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
//		bugButton.setBorderPainted(false);
//		bugButton.setContentAreaFilled(false);
		

		infoButton.addActionListener(new ActionListener()
		{
			String versionString;
			JPanel body = new JPanel(new RiverLayout());
			{
				try
				{ versionString = new Scanner( new File("toolkits/version.txt") ).nextLine(); }
				catch (FileNotFoundException e1)
				{ versionString = "";}
				
				JLabel welcome = new JLabel("<html><b>LightSide Researcher's Workbench</b><br>"+versionString+"<br><br>"
						+ "Thanks for using LightSide!<br>If you need help, here's a few places to start:</html>");
				welcome.setFont(welcome.getFont().deriveFont(14.0f));
				body.add("left", welcome);
				body.add("br left", new JLabel(" "));
				body.add("br left", linkButton("Read the Manual", "http://ankara.lti.cs.cmu.edu/side/LightSide_Researchers_Manual.pdf", "toolkits/icons/note_go.png"));
				body.add("br left", linkButton("Post Questions to the User Group", "https://groups.google.com/a/lightsidelabs.com/forum/#!forum/workbench-users", "toolkits/icons/help.png"));
				body.add("br left", linkButton("Report a Bug", "https://bitbucket.org/lightsidelabs/lightside/issues?status=new&status=open", "toolkits/icons/bug.png"));
			}
			@Override
			public void actionPerformed(ActionEvent e)
			{
				
				JOptionPane.showMessageDialog(null, body, "About LightSide", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		new Timer().scheduleAtFixedRate(new TimerTask()
		{

			@Override
			public void run()
			{
				double gigs = 1024 * 1024 * 1024;
				MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

				double beanMax = usage.getMax() / gigs;
				double beanUsed = usage.getUsed() / gigs;
				double beanCommitted = usage.getCommitted() / gigs;
				//
				String memoryString = String.format("%.1f GB used, %.1f GB max ", beanUsed, beanMax);
				garbageButton.setText(memoryString);

				double fractionUsed = beanUsed / (double) beanMax;
				if (fractionUsed >= 0.7)
				{
					garbageButton.setForeground(Color.red.darker());
					warnButton.setWarning("<html>You're running out of memory!<br> "
							+ "Delete some old feature tables or models,"
							+ "<br>or give LightSIDE more memory<br>(by editing LightSIDE.bat on Windows, "
							+ "or run.sh on Mac/Linux)</html>");

					if (fractionUsed >= 0.8 && !warned)
					{
						warnButton.doClick();
						warned = true;
					}
				}
				else
				{
					garbageButton.setForeground(Color.black);
					warnButton.clearWarning();
				}

			}

		}, 100, 10000);
		

		JPanel parallels = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		final int cores = Runtime.getRuntime().availableProcessors();

		if(cores < 2)
		{
			parallelButton.setEnabled(false);
			parallelButton.setIcon(SINGLE_CORE_ICON);
			parallelButton.setText(SINGLE_CORE_LABEL);
		}
		else
		{
			parallelButton.setSelected(true);
		}
		
		parallelButton.setBorderPainted(false);
		parallelButton.setContentAreaFilled(false);
		parallelButton.setToolTipText("<html>Toggle between using multiple processors<br>"
									+ "for feature extraction and model training tasks<br>"
									+ "(you have "+cores+" processors available), or just using one.<br>"
									+ "(this will conserve memory during model building)</html>");
		parallelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				boolean doParallel = !parallelButton.isSelected();
				parallelButton.setSelected(doParallel);
				parallelButton.setIcon(doParallel ? MULTITHREAD_ICON : SINGLE_CORE_ICON);
				parallelButton.setText(doParallel ? MULTITHREAD_LABEL : SINGLE_CORE_LABEL);
				ThreadPoolManager.setThreadPoolSize(doParallel ? cores - 1 : 1);
			}
		});
		parallels.add(parallelButton);
		
		//JPanel supportPanel = new JPanel();
		//supportPanel.add(infoButton);
//		supportPanel.add(bugButton);
		//supportPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		

		infoButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		parallels.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		this.add(infoButton, BorderLayout.WEST);
		this.add(memories, BorderLayout.EAST);
		this.add(parallels, BorderLayout.CENTER);
	}

	public JButton linkButton(final String body, final String url, final String imagePath)
	{
		JButton button = new JButton("<html><u><font color=\"blue\">"+body+"</font></u></html>", new ImageIcon(imagePath));
		button.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				try
				{
					Desktop.getDesktop().browse(new URI(url));
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (URISyntaxException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		button.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setFont(button.getFont().deriveFont(14.0f));
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return button;
	}

	protected void forceCollection()
	{
		double gigs = 1024 * 1024 * 1024;
		MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

		double beanMax = usage.getMax() / gigs;
		double beanUsed = usage.getUsed() / gigs;

		System.out.println(String.format("MMP 137: %.1f GB used now... attempting GC", beanUsed, beanMax));

//		WekaTools.invalidateCache();
		Workbench.update(GARBAGE_COLLECTION_OBSERVABLE);
		System.gc();

		usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

		beanMax = usage.getMax() / gigs;
		beanUsed = usage.getUsed() / gigs;

		System.out.println(String.format("MMP 146: %.1f GB used after garbage collection.", beanUsed, beanMax));
	}
}
