package edu.cmu.side.model;

import java.util.ArrayList;


import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;

public class FreqMapTest extends TestCase {
	FreqMap<String> fm;
	@Override
	public void setUp(){
		fm = new FreqMap<String>();
	}
	@Test
	public void testCount(){
		fm.count("testString1");
		fm.count("testString2");
		fm.count("testString1");
		assertTrue(fm.get("testString1")==2);
		assertTrue(fm.get("testString2")==1);
	}
	@Test
	public void testSafeGet(){
		fm.count("testString1");
		assertTrue(fm.safeGet("testString1")==1);
	}
	@Test
	public void testSafeGetFailure(){
		assertTrue(fm.safeGet("nothere")==0);
	}
	@Test
	public void testSum(){
		fm.count("1");
		fm.count("2");
		fm.count("1");
		assertTrue(fm.sum()==3);
	}
	@Test
	public void testCountWithInteger(){
		fm.count("1",4);
		assertTrue(fm.get("1")==4);
	}
	@Test
	public void testArrayList(){
		String[] first = {"a"};
		String[] second = {"b"};
		String[] third = {"c"};
		String[][] toArrList = {first,second,third};

		ArrayList<String[]> expected = new ArrayList<String[]>( Arrays.asList(toArrList));
		fm.count("a");
		fm.count("b");
		fm.count("c");
		ArrayList<String[]> result = fm.convertToArrayList();
		for(int i=0; i<result.size();i++){
			assertEquals(expected.get(i)[0], result.get(i)[0]);
		}
	}
	@Test
	public void testCountAll(){
		String[] toAddToArr = {"a","b","c"};
		ArrayList<String> toCount = new ArrayList<String>(Arrays.asList(toAddToArr));
		fm.countAll(toCount);
		assertTrue(fm.get("a")==1);
		assertTrue(fm.get("b")==1);
		assertTrue(fm.get("c")==1);
	}
	@Test
	public void testTop(){
		for(int i=0;i<2;i++){
			String[] toAddToArr = {"a","b","c"};
			ArrayList<String> toCount = new ArrayList<String>(Arrays.asList(toAddToArr));
			fm.countAll(toCount);
		}
			fm.count("a");
			fm.count("b");
			ArrayList<String> top = (ArrayList<String>) fm.top(2);
			assertTrue(top.get(0)=="a");
			assertTrue(top.get(1)=="b");
	}
	@Test
	public void testGetMaxKey(){
		String[] toAddToArr = {"a","b","c"};
		ArrayList<String> toCount = new ArrayList<String>(Arrays.asList(toAddToArr));
		fm.countAll(toCount);
		fm.count("a");
		assertTrue(fm.getMaxKey()=="a");
	}
	@Test
	public void testTopTooHighIndex(){
		fm.count("a");
		ArrayList<String> top = (ArrayList<String>) fm.top(5);
		assertTrue(top.size()==1);
		assertTrue(top.get(0)=="a");
	}
}
