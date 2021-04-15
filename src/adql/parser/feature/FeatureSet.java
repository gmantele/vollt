package adql.parser.feature;

import java.util.ArrayList;

/*
 * This file is part of ADQLLibrary.
 *
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2019-2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import adql.db.FunctionDef;
import adql.query.ClauseOffset;
import adql.query.WithItem;
import adql.query.constraint.ComparisonOperator;
import adql.query.operand.function.CastFunction;
import adql.query.operand.function.InUnitFunction;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.query.operand.function.string.LowerFunction;
import adql.query.operand.function.string.UpperFunction;

/**
 * Set of supported ADQL's language features.
 *
 * <p>
 * 	With this class it is possible to declare which language features are
 * 	supported. If a {@link LanguageFeature} is not part of this set, it must be
 * 	considered as <em>unsupported</em>.
 * </p>
 *
 * <h3>(Un-)Support individual features</h3>
 *
 * <p>
 * 	To support a feature, use the function
 * 	{@link #support(LanguageFeature)} while {@link #unsupport(LanguageFeature)}
 * 	lets remove a feature from this set.
 * </p>
 *
 * <p><i><b>Warning:</b>
 * 	Features with <code>{@link LanguageFeature#type type} = NULL</code> or
 * 	which are not declared as optional can never be supported by this set. In
 * 	such case, {@link #support(LanguageFeature)} will return <code>false</code>.
 * </i></p>
 *
 * <p><i><b>Note:</b>
 * 	You do not have to create any new instance of {@link LanguageFeature}. All
 * 	{@link adql.query.ADQLObject ADQLObject} provides a function for this
 * 	purpose: {@link adql.query.ADQLObject#getFeatureDescription() ADQLObject.getFeatureDescription()}.
 * 	Unfortunately, this function can not be static. That's why, the library
 * 	declared a static attribute in every {@link adql.query.ADQLObject ADQLObject}
 * 	called <code>FEATURE</code>.
 * </i></p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>To support the optional function <code>LOWER</code>:</p>
 * <pre>myFeatureSet.{@link #support(LanguageFeature) support}({@link LowerFunction#FEATURE});</pre>
 * <p>And for the geometric function <code>POLYGON</code>:</p>
 * <pre>myFeatureSet.{@link #support(LanguageFeature) support}({@link PolygonFunction#FEATURE});</pre>
 * </i>
 *
 * <h3>(Un-)Support all available features</h3>
 *
 * <p>
 * 	It is also possible to support or un-support all optional features with the
 * 	functions {@link #supportAll()} and {@link #unsupportAll()}.
 * </p>
 *
 * <p><i><b>Note:</b>
 *	The list of all standard optional features can be discovered with
 *	{@link #getAvailableFeatures()}.
 * </i></p>
 *
 * <h3>(Un-)Support a specific type of feature</h3>
 *
 * <p>
 * 	You can also support or un-support all optional features of a given type
 * 	with {@link #supportAll(String)} and {@link #unsupportAll(String)}. You can
 * 	find all standard types of feature in {@link LanguageFeature} as public
 * 	static attributes whose the name starts with <code>TYPE_</code>.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <p>To un-support all geometric functions:</p>
 * <pre>myFeatureSet.{@link #unsupportAll(String) unsupportAll}({@link LanguageFeature#TYPE_ADQL_GEO});</pre>
 * </i>
 *
 * <p><i><b>Warning:</b>
 * 	Both functions will not work for User Defined Functions that has to be
 * 	added individually in the {@link FeatureSet}.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (04/2021)
 * @since 2.0
 */
public class FeatureSet implements Iterable<LanguageFeature> {

	/** Set of all supported features. */
	protected final Map<String, Set<LanguageFeature>> supportedFeatures;

	/**
	 * Create a feature set with all available features supported by default.
	 */
	public FeatureSet() {
		this(true);
	}

	/**
	 * Create a feature set will all available features supported or not,
	 * depending of the given boolean parameter.
	 *
	 * @param allSupported	<code>true</code> to support all available features,
	 *                    	<code>false</code> to not support any.
	 */
	public FeatureSet(final boolean allSupported) {
		// Init. the list of supported features:
		supportedFeatures = new HashMap<>();

		// If asked, support all available features:
		if (allSupported)
			supportAll();
	}

	/**
	 * Support the given optional feature.
	 *
	 * <i>
	 * <p><b>Warning:</b>
	 * 	A feature can not be marked as supported in the following cases:
	 * </p>
	 * <ul>
	 * 	<li>it is NULL,</li>
	 * 	<li>no type is specified,</li>
	 * 	<li>it is not optional.</li>
	 * </ul>
	 * <p>
	 * 	In any of this cases, this function will do nothing else than returning
	 * 	<code>false</code>.
	 * </p>
	 * </i>
	 *
	 * @param feature	The optional language feature to support.
	 *
	 * @return	<code>true</code> if this set already/from now supporting the
	 *        	given feature,
	 *        	<code>false</code> if the given feature can not be supported.
	 */
	public boolean support(final LanguageFeature feature) {
		// If NULL, do nothing:
		if (feature == null || feature.type == null || !feature.optional)
			return false;

		// Get the corresponding Set of features:
		Set<LanguageFeature> features = supportedFeatures.get(feature.type);

		// If needed, create one:
		if (features == null) {
			features = new HashSet<>();
			supportedFeatures.put(feature.type, features);
		}

		// Append the given feature:
		features.add(feature);

		return true;
	}

	/**
	 * Support all the optional features of the given type.
	 *
	 * @param type	The type of language features to support.
	 *
	 * @return	<code>true</code> if all the available features of the given
	 *        	type are already/from now supported by this set,
	 *        	<code>false</code> if the given type is NULL or it does not
	 *        	match any available feature.</i>
	 *
	 * @see #getAvailableFeatures()
	 */
	public final boolean supportAll(final String type) {
		boolean done = false;
		if (type != null) {
			for(LanguageFeature feature : availableFeatures) {
				if (feature.type == type)
					done = support(feature) || done;
			}
		}
		return done;
	}

	/**
	 * Support all available optional features.
	 *
	 * @see #getAvailableFeatures()
	 */
	public final void supportAll() {
		for(LanguageFeature feature : availableFeatures)
			support(feature);
	}

	/**
	 * Un-support the given optional feature.
	 *
	 * <i>
	 * <p><b>Warning:</b>
	 * 	A feature can not be marked as un-supported in the following cases:
	 * </p>
	 * <ul>
	 * 	<li>it is NULL,</li>
	 * 	<li>no type is specified,</li>
	 * 	<li>it is not optional.</li>
	 * </ul>
	 * <p>
	 * 	In any of this cases, this function will do nothing else than returning
	 * 	<code>false</code>.
	 * </p>
	 * </i>
	 *
	 * @param feature	The optional language feature to un-support.
	 *
	 * @return	<code>true</code> if this set already/from now un-supporting the
	 *        	given feature,
	 *        	<code>false</code> if the given feature can not be supported any
	 *        	way.
	 */
	public boolean unsupport(final LanguageFeature feature) {
		// If NULL, do nothing:
		if (feature == null || feature.type == null || !feature.optional)
			return false;

		// Get the corresponding Set of features:
		Set<LanguageFeature> features = supportedFeatures.get(feature.type);

		// If already not supported, do nothing:
		if (features == null)
			return true;

		// Remove the given feature:
		boolean done = features.remove(feature);

		// If needed, drop this features set from the map:
		if (features.isEmpty())
			supportedFeatures.remove(feature.type);

		return done;
	}

	/**
	 * Un-support all the optional features of the given type.
	 *
	 * @param type	The type of language features to un-support.
	 *
	 * @return	<code>true</code> if all the available features of the given
	 *        	type are already/from now un-supported by this set,
	 *        	<code>false</code> if the given type is NULL or it does not
	 *        	match any available feature.</i>
	 *
	 * @see #getAvailableFeatures()
	 */
	public final boolean unsupportAll(final String type) {
		boolean done = false;
		if (type != null) {
			for(LanguageFeature feature : availableFeatures) {
				if (feature.type == type)
					done = unsupport(feature) || done;
			}
		}
		return done;
	}

	/**
	 * Un-support all available features.
	 *
	 * @see #getAvailableFeatures()
	 */
	public final void unsupportAll() {
		for(LanguageFeature feature : this)
			unsupport(feature);
	}

	/**
	 * Tell whether the given optional feature is marked as supported by this
	 * set.
	 *
	 * <i>
	 * <p><b>Warning:</b>
	 * 	A feature can not be marked as supported in the following cases:
	 * </p>
	 * <ul>
	 * 	<li>it is NULL,</li>
	 * 	<li>no type is specified,</li>
	 * 	<li>it is not optional.</li>
	 * </ul>
	 * <p>
	 * 	In any of this cases, this function will do nothing else than returning
	 * 	<code>false</code>.
	 * </p>
	 * </i>
	 *
	 * @param feature	The optional feature to test.
	 *
	 * @return	<code>true</code> if supported according to this set,
	 *        	<code>false</code> otherwise.
	 */
	public boolean isSupporting(final LanguageFeature feature) {
		// If NULL, do nothing:
		if (feature == null || feature.type == null || !feature.optional)
			return false;

		// Get the corresponding Set of features:
		Set<LanguageFeature> features = supportedFeatures.get(feature.type);

		return (features != null && features.contains(feature));
	}

	/**
	 * List all features marked in this set as supported.
	 *
	 * @return	An iterator over all supported features.
	 */
	public Iterator<LanguageFeature> getSupportedFeatures() {
		Set<LanguageFeature> allSupportedFeatures = new HashSet<LanguageFeature>(availableFeatures.length);
		for(String type : supportedFeatures.keySet()) {
			allSupportedFeatures.addAll(supportedFeatures.get(type));
		}
		return allSupportedFeatures.iterator();
	}

	/**
	 * List all available features not marked in this set as supported.
	 *
	 * @return	An iterator over all un-supported features.
	 */
	public Iterator<LanguageFeature> getUnsupportedFeatures() {
		Set<LanguageFeature> allUnsupportedFeatures = new HashSet<LanguageFeature>(availableFeatures.length);
		for(LanguageFeature feature : availableFeatures) {
			if (!isSupporting(feature))
				allUnsupportedFeatures.add(feature);
		}
		return allUnsupportedFeatures.iterator();
	}

	/**
	 * List only features of the given type that are marked in this set as
	 * supported.
	 *
	 * <p><i><b>Note:</b>
	 * 	If the given type is NULL or does not match the type of any supported
	 * 	feature, this function will return an empty iterator.
	 * </i></p>
	 *
	 * @param type	Type of the features to test.
	 *
	 * @return	An iterator over all supported features of the given type.
	 */
	public Iterator<LanguageFeature> getSupportedFeatures(final String type) {
		// Get the corresponding features set:
		Set<LanguageFeature> features = (type == null ? null : supportedFeatures.get(type));

		// If features found, return an iterator on this set:
		if (features != null)
			return features.iterator();

		// Otherwise, return an empty iterator:
		else {
			return new Iterator<LanguageFeature>() {
				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public LanguageFeature next() {
					throw new NoSuchElementException();
				}
			};
		}
	}

	/**
	 * List all features marked as supported in this set.
	 *
	 * @see #getSupportedFeatures()
	 */
	@Override
	public final Iterator<LanguageFeature> iterator() {
		return getSupportedFeatures();
	}

	/**
	 * Get the list of the definition of all declared UDFs.
	 *
	 * @return	List of all supported UDFs.
	 */
	public final Collection<FunctionDef> getSupportedUDFList() {
		Set<LanguageFeature> supportedUDFs = supportedFeatures.get(LanguageFeature.TYPE_UDF);
		if (supportedUDFs != null) {
			ArrayList<FunctionDef> definitions = new ArrayList<FunctionDef>(supportedUDFs.size());
			for(LanguageFeature feature : supportedUDFs)
				definitions.add(feature.udfDefinition);
			return definitions;
		} else
			return new HashSet<FunctionDef>(0);
	}

	/* **********************************************************************
	   *                                                                    *
	   *    ALL AVAILABLE FEATURES (according to the ADQL Language)         *
	   *                                                                    *
	   ********************************************************************** */

	/*public static final LanguageFeature UNION = new LanguageFeature(FeatureType.ADQL_SETS, "UNION"); // TODO UNION
	public static final LanguageFeature EXCEPT = new LanguageFeature(FeatureType.ADQL_SETS, "EXCEPT"); // TODO EXCEPT
	public static final LanguageFeature INTERSECT = new LanguageFeature(FeatureType.ADQL_SETS, "INTERSECT");  // TODO INTERSECT*/

	/** All standard features available.
	 * <p>
	 * 	This list of features is used by the functions
	 * 	{@link #supportAll(String)}, {@link #supportAll()},
	 * 	{@link #unsupportAll(String)}, {@link #unsupportAll()} and
	 * 	{@link #getAvailableFeatures()}.
	 * </p>
	 * <p><i><b>Important note:</b>
	 * 	All of them must be optional and must have a type.
	 * </i></p> */
	static LanguageFeature[] availableFeatures = new LanguageFeature[]{ CastFunction.FEATURE, WithItem.FEATURE, InUnitFunction.FEATURE, ClauseOffset.FEATURE, ComparisonOperator.ILIKE.getFeatureDescription(), LowerFunction.FEATURE, UpperFunction.FEATURE, AreaFunction.FEATURE, BoxFunction.FEATURE, CentroidFunction.FEATURE, CircleFunction.FEATURE, ContainsFunction.FEATURE, ExtractCoord.FEATURE_COORD1, ExtractCoord.FEATURE_COORD2, ExtractCoordSys.FEATURE, DistanceFunction.FEATURE, IntersectsFunction.FEATURE, PointFunction.FEATURE, PolygonFunction.FEATURE, RegionFunction.FEATURE };

	/**
	 * List all available language features.
	 *
	 * @return	An iterator over all available features.
	 */
	public static Iterator<LanguageFeature> getAvailableFeatures() {
		return new Iterator<LanguageFeature>() {

			private int index = -1;

			@Override
			public boolean hasNext() {
				return (index + 1) < availableFeatures.length;
			}

			@Override
			public LanguageFeature next() {
				return availableFeatures[++index];
			}

		};
	}

}
