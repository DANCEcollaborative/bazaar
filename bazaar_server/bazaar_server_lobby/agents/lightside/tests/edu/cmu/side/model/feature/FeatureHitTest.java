package edu.cmu.side.model.feature;

import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;

public class FeatureHitTest extends TestCase {
	FeatureHit fh;
	Feature testFeature;
	Feature testNominalFeature;
	
	FeatureHit nominalFeatureHit;
	String[] options = {"first", "second"};
	@Override
	public void setUp() throws Exception{
		super.setUp();
		
		testNominalFeature = Feature.fetchFeature("nom", "feat", Arrays.asList(options),null);
		nominalFeatureHit = new FeatureHit(testNominalFeature, "first",2);
		testFeature = Feature.fetchFeature("pre", "fix", Feature.Type.NUMERIC, null);
		fh = new FeatureHit(testFeature, 2, 2);
		
	}
	
	@Test
	public void testCloneWithoutPrefix(){
		FeatureHit clonedFh = fh.clone();
		assertEquals(clonedFh.compareTo(fh),0);
	}
	@Test
	public void testCloneWithPrefix(){
		FeatureHit clonedFh = fh.clone("pre");
		assertEquals("pre"+fh.toString(), clonedFh.toString());
	}
	@Test
	public void testFeatureGetterAndSetter(){
		Feature dummyFeature = Feature.fetchFeature("pre","fixture", Feature.Type.NUMERIC,null);
		FeatureHit sameFeature = new FeatureHit(dummyFeature,2,2);
		sameFeature.setFeature(testFeature);
		assertEquals(sameFeature.compareTo(fh),0);
		assertEquals(sameFeature.getFeature(), fh.getFeature());
	}
	@Test
	public void testIncorrectValueSet(){
		try{
			fh.setValue(true);
			fail("Didn't throw expected exception");
		}catch(IllegalArgumentException e){
			;
		}
	}
	@Test
	public void testIncorrectNominalValueSet(){
		try{
			nominalFeatureHit.setValue("notInList");
			fail("Didn't throw expected exception");
		}catch(IllegalArgumentException e){
			;
		}
	}
	@Test
	public void testCorrectValueSet(){
		nominalFeatureHit.setValue("second");
		assertEquals(nominalFeatureHit.getValue(),"second");
		fh.setValue(3);
		assertEquals(fh.getValue(),3);
	}
	@Test
	public void testGetAndSetDocumentIndex(){
		fh.setDocumentIndex(3);
		assertEquals(fh.getDocumentIndex(),3);
	}
	@Test
	public void testCompareToByFeature(){
		Feature dummyFeature = Feature.fetchFeature("pre","fixture", Feature.Type.NUMERIC,null);
		FeatureHit largerFeatureHit = new FeatureHit(dummyFeature,2,2);
		assertTrue(largerFeatureHit.compareTo(fh)>0);
	}
	@Test
	public void testCompareToByDocIndex(){
		FeatureHit clone = fh.clone();
		clone.setDocumentIndex(1);
		assertTrue(fh.compareTo(clone)>0);
	}
	@Test
	public void testCompareToByValue(){
		FeatureHit clone = fh.clone();
		clone.setValue(3);
		assertTrue(clone.compareTo(fh)>0);
	}
}
