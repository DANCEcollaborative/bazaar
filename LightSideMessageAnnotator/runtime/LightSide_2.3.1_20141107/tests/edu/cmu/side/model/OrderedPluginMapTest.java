package edu.cmu.side.model;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.swing.JPanel;

import junit.framework.TestCase;

import org.junit.Test;

import edu.cmu.side.plugin.SIDEPlugin;

public class OrderedPluginMapTest extends TestCase {
	
	class DummyPlugin extends SIDEPlugin
	{

		@Override
		public String getOutputName()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getType()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Component getConfigurationUIForSubclass()
		{
			// TODO Auto-generated method stub
			return new JPanel();
		}

		@Override
		public Map<String, String> generateConfigurationSettings()
		{
			// TODO Auto-generated method stub
			return new HashMap<String, String>();
		}

		@Override
		public void configureFromSettings(Map<String, String> settings)
		{
			// TODO Auto-generated method stub
			
		}
		
	};
	
	DummyPlugin test1Plugin;
	DummyPlugin test2Plugin;
	DummyPlugin test3Plugin;
	DummyPlugin notEqualPlugin;
	HashMap<String, String> test1Map;
	HashMap<String, String> test2Map;
	HashMap<String, String> test3Map;
	OrderedPluginMap testOnThis;
	@Override
	public void setUp(){
		test1Plugin = new DummyPlugin();
		test2Plugin = new DummyPlugin();
		test3Plugin = new DummyPlugin();
		notEqualPlugin = new DummyPlugin();
		test1Map = new HashMap<String,String>();
		test2Map = new HashMap<String,String>();
		test3Map = new HashMap<String,String>();
		addAll("1", "2");
		addAll("2", "3");
		addAll("3", "4");
		test1Map.put("test1Map", "hereitis");
		test2Map.put("test2Map", "hereitis");
		test3Map.put("test3Map", "hereitis");
		testOnThis = new OrderedPluginMap();
	}
	public void addAll(String first, String second){
		test1Map.put(first, second);
		test2Map.put(first, second);
		test3Map.put(first, second);
	}
	@Test
	public void testPutAndGet(){
		testOnThis.put(test1Plugin, test1Map);
		assertEquals(testOnThis.get(test1Plugin), test1Map);
	}
	@Test
	public void testContainsKey(){
		testOnThis.put(test1Plugin, test1Map);
		assertTrue(testOnThis.containsKey(test1Plugin));
	}
	@Test
	public void testContainsValue(){
		testOnThis.put(test1Plugin, test1Map);
		assertTrue(testOnThis.containsValue(test1Map));
	}
	@Test
	public void testOrdering(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		assertEquals(testOnThis.getOrdering(test2Plugin),1);
		assertEquals(testOnThis.getOrdering(test1Plugin),0);
	}
	@Test
	public void testClear(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.clear();
		assertFalse(testOnThis.containsKey(test2Plugin));
		assertEquals(testOnThis.size(), 0);
	}
	@Test
	public void testIsEmpty(){
		assertTrue(testOnThis.isEmpty());
	}
	@Test
	public void testPutAll(){
		Map<SIDEPlugin, HashMap<String, String>> toAdd = new HashMap<SIDEPlugin, HashMap<String,String>>();
		toAdd.put(test1Plugin, test1Map);
		toAdd.put(test2Plugin, test2Map);
		toAdd.put(test3Plugin, test3Map);
		testOnThis.putAll(toAdd);
		assertEquals(testOnThis.size(), 3);
	}
	@Test
	public void testRemove(){
		testOnThis.put(test1Plugin, test1Map);
		assertTrue(testOnThis.containsKey(test1Plugin));
		testOnThis.remove(test1Plugin);
		assertFalse(testOnThis.containsKey(test1Plugin));
	}
	//This test is kind of silly but we'll see if I end up using it to test a compareTo or something.
	@Test
	public void testComparator(){
		testOnThis.put(test1Plugin, test1Map);
		assertEquals(testOnThis.comparator().getClass(), OrderedPluginComparator.class);
	}
	@Test
	public void testEntrySet(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		Set<Entry<SIDEPlugin, Map<String, String>>> entrySet = testOnThis.entrySet();
		assertEquals(entrySet.size(), 2);
		//TODO: test for equality in Set
	}
	@Test
	public void testFirstKey(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		assertEquals(testOnThis.firstKey(), test1Plugin);
	}
	@Test
	public void testHeadMapWithCorrectHead(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> headed = testOnThis.headMap(test2Plugin);
		assertEquals(1, headed.size());
		assertEquals(test1Plugin, headed.firstKey());
	}
	@Test
	public void testHeadMapWithoutCorrectHead(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> headed = testOnThis.headMap(notEqualPlugin);
		assertEquals(3, headed.size());
		assertTrue(headed.containsKey(test1Plugin));
		assertTrue(headed.containsKey(test2Plugin));
		assertTrue(headed.containsKey(test3Plugin));
	}
	@Test
	public void testKeySet(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		Set<SIDEPlugin> keys = testOnThis.keySet();
		assertEquals(keys.size(), 3);
		assertTrue(keys.contains(test1Plugin));
		assertTrue(keys.contains(test2Plugin));
		assertTrue(keys.contains(test3Plugin));
	}
	@Test
	public void testLastKey(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		assertEquals(testOnThis.lastKey(), test3Plugin);
	}
	@Test
	public void testSubMapValidStartAndFinish(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> submap = testOnThis.subMap(test1Plugin, test3Plugin);
		assertEquals(submap.size(), 2);
		assertTrue(submap.containsKey(test1Plugin));
		assertTrue(submap.containsKey(test2Plugin));
	}
	@Test
	public void testSubMapInvalidStart(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> submap = testOnThis.subMap(notEqualPlugin, test3Plugin);
		assertEquals(submap.size(),0);
	}
	@Test
	public void testSubMapInvalidEnd(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> submap = testOnThis.subMap(test1Plugin, notEqualPlugin);
		assertEquals(submap.size(),3);
		assertTrue(submap.containsKey(test1Plugin));
		assertTrue(submap.containsKey(test2Plugin));
		assertTrue(submap.containsKey(test3Plugin));
	}
	@Test
	public void testTailMapWithValidEnd(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> submap = testOnThis.tailMap(test2Plugin);
		assertEquals(submap.size(), 2);
		assertTrue(submap.containsKey(test3Plugin));
		assertTrue(submap.containsKey(test2Plugin));
	}
	@Test
	public void testTailMapWithInvalidEnd(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		SortedMap<SIDEPlugin, Map<String,String>> submap = testOnThis.tailMap(notEqualPlugin);
		assertEquals(submap.size(), 3);
		assertTrue(submap.containsKey(test1Plugin));
		assertTrue(submap.containsKey(test2Plugin));
		assertTrue(submap.containsKey(test3Plugin));
	}
	@Test
	public void testValues(){
		testOnThis.put(test1Plugin, test1Map);
		testOnThis.put(test2Plugin, test2Map);
		testOnThis.put(test3Plugin, test3Map);
		Collection<Map<String,String>> values =  testOnThis.values();
		assertEquals(values.size(), 3);
		assertTrue(values.contains(test1Map));
		assertTrue(values.contains(test2Map));
		assertTrue(values.contains(test3Map));
	}
//	@Test
//	public void testCompareTo(){
//		testOnThis.put(test1Plugin, test1Map);
//		testOnThis.put(test2Plugin, test2Map);
//		testOnThis.put(test3Plugin, test3Map);
//		testOnThis
//	}
}