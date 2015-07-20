package basilica2.agents.operation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.observers.ComponentObserver;

public class AgentWidget extends JFrame implements ComponentObserver
{

	protected InputCoordinator source;
	protected JTextPane logArea;
	protected JTextField input;
	protected JPanel buttons;
	private boolean masquerade = false;
	private boolean invisibleWizard =  true;


	protected Vector<String> choosableUsers = new Vector<String>();

	private static final SimpleAttributeSet echoAttrs = new SimpleAttributeSet();
	private static final SimpleAttributeSet messageAttrs = new SimpleAttributeSet();
	private static final SimpleAttributeSet otherAttrs = new SimpleAttributeSet();
	{
		StyleConstants.setForeground(messageAttrs, new Color(192, 64, 0));
		StyleConstants.setBold(messageAttrs, true);
		StyleConstants.setForeground(echoAttrs, new Color(64, 64, 192));
		StyleConstants.setItalic(otherAttrs, true);
		StyleConstants.setForeground(otherAttrs, new Color(128, 128, 128));
	}

	public AgentWidget(InputCoordinator s)
	{
		this.setTitle(s.getAgent().getName() + " Wizard");

		this.source = s;
		source.addObserver(this);
		JLabel inputLabel = new JLabel("Input");
		input = new JTextField("");
		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.add(inputLabel, BorderLayout.WEST);
		inputPanel.add(input, BorderLayout.CENTER);

		logArea = new JTextPane();

		StyledDocument doc = logArea.getStyledDocument();
		try
		{
			SimpleAttributeSet attrs = new SimpleAttributeSet();
			StyleConstants.setForeground(attrs, Color.blue);
			StyleConstants.setBold(attrs, true);
			doc.insertString(0, "MESSAGES:\n", attrs);
		}
		catch (Exception e)
		{

		}
		logArea.setEditable(false);
		// logArea.setLineWrap(true);
		// logArea.setWrapStyleWord(true);

		buttons = new JPanel(new GridLayout(1, 0));

		choosableUsers.add(s.getAgent().getUsername());
		choosableUsers.add("Oz");

		final JComboBox<String> userChooser = new JComboBox<String>(choosableUsers);

		JButton launchButton = new JButton("Launch Script");
		JButton messageButton = new JButton("Message!");
		JButton presenceButton = new JButton("New User...");
		buttons.add(messageButton);
		buttons.add(presenceButton);
		buttons.add(launchButton);

		launchButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				source.queueNewEvent(new LaunchEvent(source));
			}

		});
		presenceButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String name = JOptionPane.showInputDialog("Add a new wizarding user:");
				if (!name.isEmpty())
				{
					source.queueNewEvent(new PresenceEvent(source, name, PresenceEvent.PRESENT));
					input.setText("");
					choosableUsers.add(name);
					userChooser.setModel(new DefaultComboBoxModel<String>(choosableUsers));
					userChooser.setSelectedItem(name);
				}
			}

		});
		ActionListener messageListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String name = (String) userChooser.getSelectedItem();
				String agentUsername = source.getAgent().getUsername();
				if (name.equals(agentUsername))
				{
					source.pushEventProposal(new MessageEvent(source, agentUsername, input.getText(), "WIZARD"), 1.0, 10);
				}
				else 
				{
					if (masquerade)
					{
						source.pushEventProposal(new MessageEvent(source, name, input.getText(), "WIZARD"), 1.0, 10);
					}
					else
					{
						source.pushEventProposal(new MessageEvent(source, agentUsername, "("+name+") "+input.getText(), "WIZARD"), 1.0, 10);
					}
					
					if (invisibleWizard)
					{
						source.queueNewEvent(new MessageEvent(source, name, input.getText(), "WIZARD"));
					}
				}
				input.setText("");
			}

		};
		messageButton.addActionListener(messageListener);
		input.addActionListener(messageListener);

		JPanel bottom = new JPanel(new BorderLayout());

		bottom.add(userChooser, BorderLayout.WEST);
		bottom.add(input, BorderLayout.CENTER);
		bottom.add(buttons, BorderLayout.SOUTH);

		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(logArea), BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		this.pack();

		this.setSize(400, 500);
		this.setLocation(100, 100);

	}

	@Override
	public void eventReceived(Component c, Event e)
	{
		Document doc = logArea.getDocument();
		try
		{
			if (e instanceof MessageEvent)
			{
				MessageEvent me = (MessageEvent) e;

				logArea.getDocument().insertString(doc.getLength(), me.getFrom() + ":\t" + me.getText() + "\n", messageAttrs);
				if(me.getAllAnnotations().length > 0)
					logArea.getDocument().insertString(doc.getLength(), "Annotations: "+me.getAnnotationString() + "\n", otherAttrs);
				// logArea.append(me.getFrom()+":\t"+me.getText()+"\n");
			}
			else if(e instanceof EchoEvent)
			{
				EchoEvent ee = (EchoEvent) e;
				logArea.getDocument().insertString(doc.getLength(),  ee.getEvent().getFrom()+":\t"+ee.getEvent().getText()+ "\n", echoAttrs);
				// logArea.append("("+e+")\n");
			}
			else
			{
				logArea.getDocument().insertString(doc.getLength(), e.toString() + "\n", otherAttrs);
				// logArea.append("("+e+")\n");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		logArea.setCaretPosition(doc.getLength());

	}

	@Override
	public void eventSent(Component c, Event e)
	{
	}

	@Override
	public void inform(Component c, String information)
	{
	}

	public boolean isMasquerading()
	{
		return masquerade;
	}

	public void setMasquerading(boolean masquerade)
	{
		this.masquerade = masquerade;
	}

}
