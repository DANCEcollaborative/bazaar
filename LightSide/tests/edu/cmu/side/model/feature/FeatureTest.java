package edu.cmu.side.model.feature;

import java.util.Arrays;

import org.junit.Test;

import junit.framework.TestCase;

public class FeatureTest extends TestCase {

	@Test
	public void testEmptyFeatureConstructor(){
		Feature emptyFeature = new Feature();
		assertEquals(emptyFeature.toString(), "none");
	}
	@Test
	public void testUncachedWithNoExtractorFetch(){
		Feature feat = Feature.fetchFeature("shouldNotExist", "thisDoesNotExist", Feature.Type.BOOLEAN);
		Feature.fetchFeature("willExist", "testing", Feature.Type.BOOLEAN);
		assertNull(feat);
	}
	
	@Test
	public void testUncachedWithExtractorFetch(){
		Feature feat = Feature.fetchFeature("prefixTest", "nameTest", Feature.Type.BOOLEAN, null);
		assertEquals(feat.getFeatureName(),"nameTest");
		assertEquals(feat.getExtractorPrefix(), "prefixTest");
		assertEquals(feat.getFeatureType(), Feature.Type.BOOLEAN);
		assertNull(feat.getExtractor());
	}
	
	@Test
	public void testCachedWithExtractorFetch(){
		Feature feat = Feature.fetchFeature("existant", "existant", Feature.Type.BOOLEAN, null);
		assertEquals(Feature.fetchFeature("existant", "existant", Feature.Type.BOOLEAN, null), feat);
		assertEquals(Feature.fetchFeature("existant", "existant", Feature.Type.BOOLEAN),feat);
	}
	@Test
	public void testNominalFeatureFetch(){
		String[] nominalValueArray = {"1","2"};
		Feature feat = Feature.fetchFeature("nom", "nom", Arrays.asList(nominalValueArray),null);
		assertEquals(Feature.fetchFeature("nom", "nom", Arrays.asList(nominalValueArray),null),feat);
	}
	@Test
	public void testClone(){
		String[] nominalValueArray = {"1","2"};
		Feature nominalToClone = Feature.fetchFeature("nominalClone", "nominalClone", Arrays.asList(nominalValueArray), null);
		Feature booleanToClone = Feature.fetchFeature("booleanClone", "booleanClone", Feature.Type.BOOLEAN, null);
		assertEquals(nominalToClone.clone(""),nominalToClone);
		assertEquals(booleanToClone.clone(""),booleanToClone);
	}
	@Test
	public void testGetType(){
		assertEquals(Number.class, Feature.Type.NUMERIC.getClassForType());
		assertEquals(Boolean.class, Feature.Type.BOOLEAN.getClassForType());
		assertEquals(String.class, Feature.Type.STRING.getClassForType());
		assertEquals(String.class, Feature.Type.NOMINAL.getClassForType());
	}
	
	@Test
	public void testGetNominalOnNonNominal(){
		try
		{
			(Feature.fetchFeature("fail", "fail", Feature.Type.BOOLEAN, null)).getNominalValues();
		    fail("Didn't throw expected exception");
		}
		catch(IllegalArgumentException e)
		{
		    assertEquals("fail is not a nominal feature.", e.getMessage());
		}
	}
	@Test
	public void testGetNominal(){
		String[] nominalValueArray = {"1","2"};
		assertEquals((Feature.fetchFeature("nominalnom", "nominalnom", Arrays.asList(nominalValueArray), null)).getNominalValues(), Arrays.asList(nominalValueArray));
	}
	@Test
	public void testCompareToAlternative(){
		Feature smallerBooleanFeature = Feature.fetchFeature("FirstPrefix", "doesntmatter", Feature.Type.BOOLEAN, null);
		Feature largerBooleanFeature = Feature.fetchFeature("SecondPrefix", "doesntmatter", Feature.Type.BOOLEAN, null);
		assertTrue(smallerBooleanFeature.compareTo(largerBooleanFeature)<0);
	}
	@Test
	public void testReconcileNotEqual(){
		try
		{
			Feature a = Feature.fetchFeature("FirstPrefix", "a", Feature.Type.BOOLEAN, null);
			Feature b = Feature.fetchFeature("SecondPrefix", "b", Feature.Type.BOOLEAN, null);
			Feature.reconcile(a,b);
			fail("Didn't throw expected exception");
		}catch(IllegalStateException e){
			assertEquals("a is different from b", e.getMessage());
		}
	}
	@Test
	public void testReconcileNominal(){
		String[] nominalValueArray = {"1","2"};
		String[] differingNominalValueArray = {"1","2","3"};
		Feature a = Feature.fetchFeature("nominalEqual", "a", Arrays.asList(nominalValueArray), null);
		Feature b = Feature.fetchFeature("nominalEqual", "a", Arrays.asList(differingNominalValueArray), null);
		Feature c = Feature.reconcile(a,b);
		assertEquals(a,c);
		assertEquals(Arrays.asList(nominalValueArray), c.getNominalValues());
	}
	@Test
	public void testReconcileNonNominal(){
		Feature a = Feature.fetchFeature("nominalEqual", "a", Feature.Type.STRING, null);
		Feature b = Feature.fetchFeature("nominalEqual", "a", Feature.Type.STRING, null);
		Feature c = Feature.reconcile(a,b);
		assertEquals(a,c);
	}
}
