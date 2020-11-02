package plugins.restructure.multi.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import plugins.restructure.GrowthModel;
import plugins.restructure.multi.model.StructuredLevel;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.view.util.AbstractListPanel;

public class MultilevelBottomPanel extends AbstractListPanel{

	JPanel describePanel = new JPanel(new BorderLayout());
	ArrayList<JTree> trees = new ArrayList<JTree>();
	
	public MultilevelBottomPanel(){
		setLayout(new RiverLayout());
		add("left", new JLabel("Select Levels:"));
		JPanel pan = new JPanel(new RiverLayout());
		ImageIcon iconDelete = new ImageIcon("toolkits/icons/cross.png");
		delete.setText("");
		delete.setIcon(iconDelete);
		delete.setToolTipText("Delete");

		pan.add("right", delete);
		delete.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(JTree tree : trees){
					if(tree.getSelectionCount()>0){
						GrowthModel.deleteLevel((StructuredLevel)((DefaultMutableTreeNode)tree.getSelectionPath().getPath()[0]).getUserObject());
					}
				}
				refreshPanel();
				revalidate();
			}
			
		});
		add("hfill", pan);
		describePanel.add(BorderLayout.CENTER, describeScroll);
		add("br hfill vfill", describePanel);
	}
	
	@Override
	public void refreshPanel(){
		JPanel panel = new JPanel();
		if(GrowthModel.getFinalLevels().size()>0){
			panel = new JPanel(new GridLayout(GrowthModel.getFinalLevels().size(), 1));
			trees.clear();
			for(StructuredLevel level : GrowthModel.getFinalLevels()){
				JTree tree = new JTree(level.getTreeRepresentation());
				panel.add(tree);
				trees.add(tree);
//				System.out.println("Tree added to make " + trees.size() + " total trees! MBP61");
			}			
		}
		describePanel.removeAll();
		describeScroll = new JScrollPane(panel);
		describePanel.add(BorderLayout.CENTER, describeScroll);
	}
}
