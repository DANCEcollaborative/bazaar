package edu.cmu.side.view.util;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class RecipeCellRenderer extends DefaultTreeCellRenderer {
	
	int cutoff;
	
	public RecipeCellRenderer()
	{
		this(1000);
	}
	
	public RecipeCellRenderer(int textCutoff)
	{
		super();
		cutoff = textCutoff;
	}

    @Override
	public Component getTreeCellRendererComponent(JTree tree,
      Object value,boolean sel,boolean expanded,boolean leaf,
      int row,boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, 
          expanded, leaf, row, hasFocus);
        Object nodeObj = ((DefaultMutableTreeNode)value).getUserObject();
        // check whatever you need to on the node user object
        if (nodeObj.toString().startsWith("Documents:")) {
            setIcon(new ImageIcon("toolkits/icons/page_white_stack.png"));
        }else if(nodeObj.toString().startsWith("Feature Table:")){
            setIcon(new ImageIcon("toolkits/icons/application_view_columns.png"));       	
        }else if(nodeObj.toString().startsWith("Filtered Table:")){
            setIcon(new ImageIcon("toolkits/icons/application_side_expand.png"));       	
        }else if(nodeObj.toString().startsWith("Trained Model:")){
            setIcon(new ImageIcon("toolkits/icons/chart_curve.png"));       	
        }else if(nodeObj.toString().startsWith("Restructure Plugins:")){
            setIcon(new ImageIcon("toolkits/icons/table_go.png"));       	
        }else if(nodeObj.toString().startsWith("Feature Plugins:")){
            setIcon(new ImageIcon("toolkits/icons/table_add.png"));       	
        }else if(nodeObj.toString().startsWith("Wrapper Plugins:")){
            setIcon(new ImageIcon("toolkits/icons/table_go.png"));           	
        }else if(nodeObj.toString().startsWith("Learning Plugin:")){
            setIcon(new ImageIcon("toolkits/icons/table_gear.png"));           	
        }else if(nodeObj.toString().startsWith("Validation:")){
            setIcon(new ImageIcon("toolkits/icons/table_gear.png"));       	
        }else{
        	setIcon(null);
        }
        
        if(this.getText().length() > cutoff)
        {
        	this.setText(this.getText().substring(0, cutoff)+"...");
        	
        }
        
        return this;
    }
}