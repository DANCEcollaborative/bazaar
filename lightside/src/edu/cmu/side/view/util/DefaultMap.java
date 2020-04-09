package edu.cmu.side.view.util;

import java.util.HashMap;

public class DefaultMap<K, V> extends HashMap<K, V>
{
	protected V defaultValue;
	
	public DefaultMap(V defaultValue)
	{
		this.defaultValue = defaultValue;
	}
	
	@Override
	public V get(Object key)
	{
		V v = super.get(key);
		if(v == null)
			v = defaultValue;
		return v;
	}
	
}
