package edu.cmu.side.view.util;

import java.util.List;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

import edu.cmu.side.model.data.DocumentList;

public class DocumentListTableModel extends AbstractTableModel
{

	private DocumentList docs;
	private String[] annotationNames = new String[0];

	public DocumentListTableModel(DocumentList docs)
	{
		super();
		this.docs = docs;
	}

	@Override
	public Class<?> getColumnClass(int arg0)
	{
		return String.class;
	}

	@Override
	public int getColumnCount()
	{
		if(docs == null)
			return 0;
		
		if(docs.getTextColumns().isEmpty())
			return annotationNames.length;
		
		else 
			return annotationNames.length + 1;
	}

	@Override
	public String getColumnName(int c)
	{
		if(c == docs.getAnnotationNames().length)
			return "text";
		else if(c < annotationNames.length)
			return annotationNames[c];
		else return "?";
	}

	@Override
	public int getRowCount()
	{
		if(docs == null)
			return 0;
		
		return docs.getSize();
	}

	@Override
	public Object getValueAt(int r, int c)
	{
		if(c == annotationNames.length)
			return docs.getPrintableTextAt(r);
		List<String> annotationArray = docs.getAnnotationArray(annotationNames[c]);
		if(annotationArray != null)
			return annotationArray.get(r);
		else
			return "?";
	}

	@Override
	public boolean isCellEditable(int arg0, int arg1)
	{
		return false;
	}

	@Override
	public void setValueAt(Object arg0, int arg1, int arg2)
	{
		// TODO Auto-generated method stub
	}

	public DocumentList getDocumentList()
	{
		return docs;
	}

	public void setDocumentList(DocumentList docs)
	{
		if(docs != null)
		{
			annotationNames = new TreeSet<String>(docs.allAnnotations().keySet()).toArray(annotationNames);
		}
		else
		{
			annotationNames = new String[0];
		}
		this.docs = docs;
		
		this.fireTableStructureChanged();
		this.fireTableDataChanged();
	}

}
