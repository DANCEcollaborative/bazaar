package edu.cmu.side.view.util;

import java.util.StringTokenizer;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

//@author santhosh kumar T - santhosh@in.fiorano.com
//http://www.javalobby.org/java/forums/t19857.html
public class JTreeUtil
{

	// is path1 descendant of path2
	public static boolean isDescendant(TreePath path1, TreePath path2)
	{
		int count1 = path1.getPathCount();
		int count2 = path2.getPathCount();
		if (count1 <= count2) return false;
		while (count1 != count2)
		{
			path1 = path1.getParentPath();
			count1--;
		}
		return path1.equals(path2);
	}

	public static String getExpansionState(JTree tree, int row)
	{
		TreePath rowPath = tree.getPathForRow(row);
		StringBuffer buf = new StringBuffer();
		int rowCount = tree.getRowCount();
		for (int i = row; i < rowCount; i++)
		{
			TreePath path = tree.getPathForRow(i);
			if (i == row || isDescendant(path, rowPath))
			{
				if (tree.isExpanded(path)) buf.append("," + String.valueOf(i - row));
			}
			else
				break;
		}
		return buf.toString();
	}

	public static void restoreExpansionState(JTree tree, int row, String expansionState)
	{
		StringTokenizer stok = new StringTokenizer(expansionState, ",");
		while (stok.hasMoreTokens())
		{
			int token = row + Integer.parseInt(stok.nextToken());
			tree.expandRow(token);
		}
	}
}
