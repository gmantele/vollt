package adql.parser.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import adql.query.ColumnReference;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.string.LowerFunction;

public class TestFeatureSet {

	@Test
	public void testFeatureSet() {
		/* With this constructor, all available features must be already
		 * supported: */
		FeatureSet set = new FeatureSet();
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertTrue(set.supportedFeatures.containsKey(feat.type));
			assertTrue(set.supportedFeatures.get(feat.type).contains(feat));
		}
	}

	@Test
	public void testFeatureSetBoolean() {
		/* With this constructor, all available features must be already
		 * supported: */
		FeatureSet set = new FeatureSet(true);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertTrue(set.supportedFeatures.containsKey(feat.type));
			assertTrue(set.supportedFeatures.get(feat.type).contains(feat));
		}
		assertTrue(set.anyUdfAllowed);

		// With this constructor, none of the available features are supported:
		set = new FeatureSet(false);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertFalse(set.supportedFeatures.containsKey(feat.type));
		}
		assertFalse(set.anyUdfAllowed);
	}

	@Test
	public void testFeatureSetBooleanBoolean() {
		/* With this constructor, all available features and non-declared UDFs
		 * must be already supported: */
		FeatureSet set = new FeatureSet(true, true);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertTrue(set.supportedFeatures.containsKey(feat.type));
			assertTrue(set.supportedFeatures.get(feat.type).contains(feat));
		}
		assertTrue(set.anyUdfAllowed);

		/* With this constructor, all available features must be already
		 * supported BUT NOT non-declared UDFs: */
		set = new FeatureSet(true, false);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertTrue(set.supportedFeatures.containsKey(feat.type));
			assertTrue(set.supportedFeatures.get(feat.type).contains(feat));
		}
		assertFalse(set.anyUdfAllowed);

		/* With this constructor, none of the available features are supported,
		 * as well as non-declared UDFs: */
		set = new FeatureSet(false, false);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertFalse(set.supportedFeatures.containsKey(feat.type));
		}
		assertFalse(set.anyUdfAllowed);

		/* With this constructor, none of the available features are supported,
		 * BUT non-declared UDFs are allowed: */
		set = new FeatureSet(false, true);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertFalse(set.supportedFeatures.containsKey(feat.type));
		}
		assertTrue(set.anyUdfAllowed);
	}

	@Test
	public void testSupport() {
		FeatureSet set = new FeatureSet(false);

		// CASE: NULL => nothing done
		assertFalse(set.support(null));

		// CASE: no type => nothing done
		assertFalse(set.support(ColumnReference.FEATURE));
		assertFalse(set.support(new LanguageFeature(null, "FOO", true)));
		assertFalse(set.support(new LanguageFeature("", "FOO", true)));
		assertFalse(set.support(new LanguageFeature("  ", "FOO", true)));

		// CASE: not optional => nothing done
		assertFalse(set.support(new LanguageFeature("foo", "BAR", false)));

		// CASE: ok => supported!
		assertTrue(set.support(LowerFunction.FEATURE));
		assertTrue(set.supportedFeatures.containsKey(LowerFunction.FEATURE.type));
		assertTrue(set.supportedFeatures.get(LowerFunction.FEATURE.type).contains(LowerFunction.FEATURE));
	}

	@Test
	public void testSupportAllString() {
		FeatureSet set = new FeatureSet(false);

		// CASE: NULL/Empty string => nothing done
		assertFalse(set.supportAll(null));
		assertFalse(set.supportAll(""));
		assertFalse(set.supportAll("  "));

		// CASE: not a know type => nothing done
		assertFalse(set.supportAll("ivo://foo"));

		// CASE: known type => all features supported!
		assertTrue(set.supportAll(LanguageFeature.TYPE_ADQL_GEO));
		assertTrue(set.supportedFeatures.containsKey(LanguageFeature.TYPE_ADQL_GEO));
		Set<LanguageFeature> geoSet = set.supportedFeatures.get(LanguageFeature.TYPE_ADQL_GEO);
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			if (LanguageFeature.TYPE_ADQL_GEO.equals(feat.type)) {
				assertTrue(geoSet.contains(feat));
			}
		}

		// CASE: UDF => non-declared UDFs are allowed
		assertFalse(set.isAnyUdfAllowed());
		assertTrue(set.supportAll(LanguageFeature.TYPE_UDF));
		assertTrue(set.isAnyUdfAllowed());
	}

	@Test
	public void testSupportAll() {
		FeatureSet set = new FeatureSet(false);

		set.supportAll();
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertTrue(set.supportedFeatures.containsKey(feat.type));
			assertTrue(set.supportedFeatures.get(feat.type).contains(feat));
		}
		assertTrue(set.isAnyUdfAllowed());
	}

	@Test
	public void testUnsupport() {
		FeatureSet set = new FeatureSet(true);

		// CASE: NULL => nothing done
		assertFalse(set.unsupport(null));

		// CASE: no type => nothing done
		assertFalse(set.unsupport(ColumnReference.FEATURE));
		assertFalse(set.unsupport(new LanguageFeature(null, "FOO", true)));
		assertFalse(set.unsupport(new LanguageFeature("", "FOO", true)));
		assertFalse(set.unsupport(new LanguageFeature("  ", "FOO", true)));

		// CASE: not optional => nothing done
		assertFalse(set.unsupport(new LanguageFeature("foo", "BAR", false)));

		/* CASE: ok (with a set containing just 1 item)
		 *       => un-supported + no more set for the feature type! */
		assertTrue(set.supportedFeatures.containsKey(LowerFunction.FEATURE.type));
		assertTrue(set.supportedFeatures.get(LowerFunction.FEATURE.type).contains(LowerFunction.FEATURE));
		assertTrue(set.unsupport(LowerFunction.FEATURE));
		assertFalse(set.supportedFeatures.containsKey(LowerFunction.FEATURE.type));

		/* CASE: ok (with a set containing multiple items)
		 *       => un-supported + set for the feature type still exists! */
		assertTrue(set.supportedFeatures.containsKey(PolygonFunction.FEATURE.type));
		assertTrue(set.supportedFeatures.get(PolygonFunction.FEATURE.type).contains(PolygonFunction.FEATURE));
		assertTrue(set.unsupport(PolygonFunction.FEATURE));
		assertTrue(set.supportedFeatures.containsKey(PolygonFunction.FEATURE.type));
		assertFalse(set.supportedFeatures.get(PolygonFunction.FEATURE.type).contains(PolygonFunction.FEATURE));
	}

	@Test
	public void testUnsupportAllString() {
		FeatureSet set = new FeatureSet(true);

		// CASE: NULL/Empty string => nothing done
		assertFalse(set.unsupportAll(null));
		assertFalse(set.unsupportAll(""));
		assertFalse(set.unsupportAll("  "));

		// CASE: not a know type => nothing done
		assertFalse(set.unsupportAll("ivo://foo"));

		// CASE: known type => all features supported!
		assertTrue(set.supportedFeatures.containsKey(LanguageFeature.TYPE_ADQL_GEO));
		assertTrue(set.unsupportAll(LanguageFeature.TYPE_ADQL_GEO));
		assertFalse(set.supportedFeatures.containsKey(LanguageFeature.TYPE_ADQL_GEO));

		// CASE: UDF => non-declared UDFs are forbidden
		assertTrue(set.isAnyUdfAllowed());
		assertTrue(set.unsupportAll(LanguageFeature.TYPE_UDF));
		assertFalse(set.isAnyUdfAllowed());
	}

	@Test
	public void testUnsupportAll() {
		FeatureSet set = new FeatureSet(true);

		set.unsupportAll();
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			assertNotNull(feat);
			assertFalse(set.supportedFeatures.containsKey(feat.type));
		}
		assertFalse(set.isAnyUdfAllowed());
	}

	@Test
	public void testIsSupporting() {
		// CASE: nothing supported at all:
		FeatureSet set = new FeatureSet(false);
		assertFalse(set.isSupporting(PolygonFunction.FEATURE));

		// CASE: NULL => false
		assertFalse(set.isSupporting(null));

		// CASE: now supported => true
		assertTrue(set.support(PolygonFunction.FEATURE));
		assertTrue(set.isSupporting(PolygonFunction.FEATURE));

		// CASE: now supporting the entire type => true for all of them
		assertTrue(set.supportAll(LanguageFeature.TYPE_ADQL_GEO));
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			if (LanguageFeature.TYPE_ADQL_GEO.equals(feat.type))
				assertTrue(set.isSupporting(feat));
		}
	}

	@Test
	public void testGetSupportedFeatures() {
		// CASE: Empty set => no feature returned
		FeatureSet set = new FeatureSet(false);
		assertFalse(set.getSupportedFeatures().hasNext());

		// CASE: Just one feature
		assertTrue(set.support(PolygonFunction.FEATURE));
		Iterator<LanguageFeature> itSupported = set.getSupportedFeatures();
		assertTrue(itSupported.hasNext());
		assertEquals(PolygonFunction.FEATURE, itSupported.next());
		assertFalse(itSupported.hasNext());

		// CASE: Two features of the same type
		assertTrue(set.support(BoxFunction.FEATURE));
		Set<LanguageFeature> sSupported = fetchAllFeatures(set.getSupportedFeatures());
		assertEquals(2, sSupported.size());
		assertTrue(sSupported.contains(PolygonFunction.FEATURE));
		assertTrue(sSupported.contains(BoxFunction.FEATURE));

		// CASE: Features of different type
		assertTrue(set.support(LowerFunction.FEATURE));
		sSupported = fetchAllFeatures(set.getSupportedFeatures());
		assertEquals(3, sSupported.size());
		assertTrue(sSupported.contains(PolygonFunction.FEATURE));
		assertTrue(sSupported.contains(BoxFunction.FEATURE));
		assertTrue(sSupported.contains(LowerFunction.FEATURE));
	}

	@Test
	public void testGetUnsupportedFeatures() {
		// CASE: Empty set => all available features returned
		FeatureSet set = new FeatureSet(false);
		Set<LanguageFeature> sUnsupported = fetchAllFeatures(set.getUnsupportedFeatures());
		for(LanguageFeature feat : FeatureSet.availableFeatures)
			assertTrue(sUnsupported.contains(feat));

		// CASE: one feature supported
		assertTrue(set.support(PolygonFunction.FEATURE));
		sUnsupported = fetchAllFeatures(set.getUnsupportedFeatures());
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			if (PolygonFunction.FEATURE.equals(feat))
				assertFalse(sUnsupported.contains(feat));
			else
				assertTrue(sUnsupported.contains(feat));
		}

		// CASE: two features of the same type supported
		assertTrue(set.support(BoxFunction.FEATURE));
		sUnsupported = fetchAllFeatures(set.getUnsupportedFeatures());
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			if (PolygonFunction.FEATURE.equals(feat) || BoxFunction.FEATURE.equals(feat))
				assertFalse(sUnsupported.contains(feat));
			else
				assertTrue(sUnsupported.contains(feat));
		}

		// CASE: features of different type supported
		assertTrue(set.support(LowerFunction.FEATURE));
		sUnsupported = fetchAllFeatures(set.getUnsupportedFeatures());
		for(LanguageFeature feat : FeatureSet.availableFeatures) {
			if (PolygonFunction.FEATURE.equals(feat) || BoxFunction.FEATURE.equals(feat) || LowerFunction.FEATURE.equals(feat))
				assertFalse(sUnsupported.contains(feat));
			else
				assertTrue(sUnsupported.contains(feat));
		}

	}

	@Test
	public void testGetSupportedFeaturesString() {
		// CASE: Empty set => no feature returned
		FeatureSet set = new FeatureSet(false);
		assertFalse(set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_GEO).hasNext());

		// CASE: Just one feature
		assertTrue(set.support(PolygonFunction.FEATURE));
		assertFalse(set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_STRING).hasNext());
		Iterator<LanguageFeature> itSupported = set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_GEO);
		assertTrue(itSupported.hasNext());
		assertEquals(PolygonFunction.FEATURE, itSupported.next());
		assertFalse(itSupported.hasNext());

		// CASE: Two features of the same type
		assertTrue(set.support(BoxFunction.FEATURE));
		assertFalse(set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_STRING).hasNext());
		Set<LanguageFeature> sSupported = fetchAllFeatures(set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_GEO));
		assertEquals(2, sSupported.size());
		assertTrue(sSupported.contains(PolygonFunction.FEATURE));
		assertTrue(sSupported.contains(BoxFunction.FEATURE));

		// CASE: Features of different type
		assertTrue(set.support(LowerFunction.FEATURE));
		sSupported = fetchAllFeatures(set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_STRING));
		itSupported = set.getSupportedFeatures(LanguageFeature.TYPE_ADQL_STRING);
		assertTrue(itSupported.hasNext());
		assertEquals(LowerFunction.FEATURE, itSupported.next());
		assertFalse(itSupported.hasNext());
	}

	@Test
	public void testIterator() {
		testGetSupportedFeatures();
	}

	private final Set<LanguageFeature> fetchAllFeatures(final Iterator<LanguageFeature> it) {
		Set<LanguageFeature> set = new HashSet<>();
		while(it.hasNext())
			set.add(it.next());
		return set;
	}

}
