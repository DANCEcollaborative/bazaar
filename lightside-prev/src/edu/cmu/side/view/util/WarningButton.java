package edu.cmu.side.view.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

public class WarningButton extends JButton
{
	private static ImageIcon iconWarn = new ImageIcon("toolkits/icons/error.png");
	public WarningButton()
	{
		super("");
		this.setIcon(iconWarn);
		this.setVisible(false);
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
		this.setOpaque(false);

		this.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(null, WarningButton.this.getToolTipText(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		});
	}
	
	public void setWarning(String warnText)
	{
		this.setVisible(true);
		this.setToolTipText(warnText);
	}
	
	public String getWarning()
	{
		return this.getToolTipText();
	}
	
	public void clearWarning()
	{
		this.setVisible(false);
		this.setToolTipText("");
	}

}
