package edu.cmu.side.model.feature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class RegroupFeatureHitTest extends TestCase {
	FeatureHit fh;
	Feature testFeature;
	Feature testNominalFeature;
	FeatureHit nominalFeatureHit;
	Map<Integer,Integer> indexes;
	String[] options = {"first", "second"};
	
	@Override
	public void setUp(){
		testFeature = Feature.fetchFeature("pre", "fix", Feature.Type.NUMERIC, null);
		fh = new FeatureHit(testFeature, 2, 2);
		testNominalFeature = Feature.fetchFeature("nom", "feat", Arrays.asList(options),null);
		nominalFeatureHit = new FeatureHit(testNominalFeature, "first",2);
		indexes = new HashMap<Integer,Integer>();
		indexes.put(0, 3);
		indexes.put(1, 5);
		indexes.put(0, 2);
		indexes.put(2, 3);
	}

	@Test
	public void testGetOriginalIndex(){
		RegroupFeatureHit rfhTest = new RegroupFeatureHit(fh, indexes);
		assertEquals(fh.getDocumentIndex(), rfhTest.getOriginalIndex());
		RegroupFeatureHit rfhTestWithIndex = new RegroupFeatureHit(fh, indexes, 3);
		assertEquals(3, rfhTestWithIndex.getOriginalIndex());
	}
	@Test
	public void testToString(){
		RegroupFeatureHit rfhTest = new RegroupFeatureHit(fh, indexes);
		String expected = testFeature + "@" + rfhTest.getDocumentIndex() + "/" + rfhTest.getOriginalIndex();
		assertEquals(expected, rfhTest.toString());
	}
	@Test
	public void testCompareToBothRFHAndSameFeature(){
		RegroupFeatureHit rfhTest = new RegroupFeatureHit(fh, indexes, 3);
		RegroupFeatureHit toCompare= new RegroupFeatureHit(fh, indexes, 0);
		assertTrue(rfhTest.compareTo(toCompare)>0);
	}
	@Test
	public void testCompareToBothRFHNotSame(){
		RegroupFeatureHit rfhTest = new RegroupFeatureHit(fh, indexes, 3);
		RegroupFeatureHit toCompare = new RegroupFeatureHit(nominalFeatureHit, indexes, 0);
		assertTrue(rfhTest.compareTo(toCompare)>0);
	}
	@Test
	public void testCompareToNotBothRFH(){
		RegroupFeatureHit rfhTest = new RegroupFeatureHit(fh,indexes, 3);
		assertTrue(rfhTest.compareTo(fh)>0);
	}
	
}
