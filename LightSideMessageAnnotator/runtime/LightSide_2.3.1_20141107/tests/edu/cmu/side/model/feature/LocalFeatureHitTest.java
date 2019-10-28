package edu.cmu.side.model.feature;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import edu.cmu.side.model.feature.LocalFeatureHit.HitLocation;

import junit.framework.TestCase;

public class LocalFeatureHitTest extends TestCase {
	Feature testFeature;
	@Override
	public void setUp(){
		testFeature = Feature.fetchFeature("Pre", "fix", Feature.Type.BOOLEAN, null);
	}
	@Test
	public void testCreateFeatureHit(){
		LocalFeatureHit lfh = new LocalFeatureHit(testFeature, true, 0, "name", 0, 1);
		assertTrue(lfh!=null);
		Collection<HitLocation> hitColl = lfh.getHits();
		LocalFeatureHit alreadyMade = new LocalFeatureHit(testFeature,true,0,hitColl);
		assertTrue(alreadyMade.compareTo(lfh)==0);
	}
	@Test
	public void testAddHit(){
		LocalFeatureHit lfh = new LocalFeatureHit(testFeature, true, 0, "name", 0, 1);
		ArrayList<HitLocation> hitArrList = (ArrayList<HitLocation>) lfh.getHits();
		hitArrList.add(new HitLocation("name",1,2));
		lfh.addHit("name",1,2);
		assertEquals(hitArrList, lfh.getHits());
	}
	@Test
	public void testHitLocations(){
		HitLocation hl = new HitLocation("test",0,1);
		assertEquals(hl.getColumn(), "test");
		assertEquals(hl.getEnd(),1);
		assertEquals(hl.getStart(),0);
	}
	@Test
	public void testToString(){
		LocalFeatureHit lfh = new LocalFeatureHit(testFeature, true, 0, "name", 0, 1);
		lfh.addHit("age", 1, 2);
		lfh.addHit("age", 2, 3);
		String expected = "fix@0(true):(name: 0,1) (age: 1,2) (age: 2,3) ";
		assertEquals(lfh.toString(),expected);
	}
}
