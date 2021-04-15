package adql.parser;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import adql.db.DBChecker;
import adql.db.exception.UnresolvedIdentifiersException;
import adql.db.exception.UnsupportedFeatureException;
import adql.db.region.CoordSys;
import adql.db.region.Region;
import adql.db.region.STCS;
import adql.parser.feature.FeatureSet;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ADQLGrammar;
import adql.parser.grammar.ADQLGrammar.Tokenizer;
import adql.parser.grammar.ADQLGrammar200;
import adql.parser.grammar.ADQLGrammar201;
import adql.parser.grammar.ParseException;
import adql.parser.grammar.Token;
import adql.parser.grammar.TokenMgrError;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ADQLOrder;
import adql.query.ADQLQuery;
import adql.query.ClauseADQL;
import adql.query.ClauseConstraints;
import adql.query.ClauseSelect;
import adql.query.from.FromContent;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.GeometryFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;
import adql.search.ISearchHandler;
import adql.search.SearchOptionalFeaturesHandler;
import adql.search.SimpleSearchHandler;
import adql.translator.PostgreSQLTranslator;
import adql.translator.TranslationException;

/**
 * Parser of ADQL expressions.
 *
 * <h3>Usage</h3>
 *
 * <p>
 * 	The simplest way to use this parser is just to create a default ADQL
 * 	parser, and call the function {@link #parseQuery(String)} on the ADQL query
 * 	to evaluate.
 * </p>
 *
 * <i>
 * <p><b>Example:</b></p>
 * <pre>
 * try {
 *     // 1. CREATE A PARSER:
 *     ADQLParser parser = new {@link #ADQLParser()};
 *
 *     // 2. PARSE AN ADQL QUERY:
 *     ADQLQuery query = parser.{@link #parseQuery(String) parseQuery}("SELECT foo FROM bar WHERE stuff = 1");
 *
 *     System.out.println("((i)) Correct ADQL query ((i))");
 *     System.out.println("((i)) As interpreted: ((i))\n    " + query.toADQL().replaceAll("\n", "\n    "));
 * }
 * // 3. EVENTUALLY DEAL WITH ERRORS:
 * catch({@link ParseException} ex) {
 *     System.err.println("((X)) INCORRECT QUERY! " + ex.getClass().getSimpleName() + " ((X))\n" + ex.getPosition() + " " + ex.getMessage());
 * }</pre>
 * </i>
 *
 * <p>
 * 	In the above example, the parser runs with the minimal set of options. It
 * 	means that only the default optional language features are available, any
 * 	UDF (even if undeclared) and any coordinate system are allowed and no
 * 	consistency with a list of tables and columns is performed. These points can
 * 	be customized at creation with
 * 	{@link #ADQLParser(ADQLVersion, QueryChecker, ADQLQueryFactory, FeatureSet)}
 * 	but also after creation with {@link #setSupportedFeatures(FeatureSet)},
 * 	{@link #setQueryChecker(QueryChecker)} and
 * 	{@link #setAllowedCoordSys(Collection)}.
 * </p>
 *
 * <h3>Runnable class</h3>
 *
 * <p>
 * 	This class includes a main function and thus, can be executed directly.
 * 	Its execution allows to parse an ADQL query. Then, in function of the passed
 * 	parameters, it is possible to just check its syntax, translate it into SQL
 * 	or try to fix the query.
 * </p>
 *
 * <i>
 * <p>
 * 	To get help about this program, just run it with the argument
 * 	<code>-h</code> or <code>--help</code>:
 * </p>
 * <pre>java -jar adqllib.jar --help</pre>
 * </i>
 *
 *
 * <h3>ADQL version</h3>
 *
 * <p>
 * 	It is able to deal with all versions of the ADQL grammar supported by this
 * 	library. All these versions are listed in the enumeration
 * 	{@link ADQLVersion}.
 * </p>
 *
 * <p>
 * 	If a specific version of the grammar must be used, it must be specified in
 * 	the constructor of the parser.
 * </p>
 *
 * <p><i><b>Example: </b></i>
 * 	<code>new {@link #ADQLParser(ADQLVersion) ADQLParser}({@link ADQLVersion#V2_1})</code>
 * </p>
 *
 *
 * <h3>Main functions</h3>
 *
 * <p>Here are the key functions to use:</p>
 * <ul>
 * 	<li>{@link #parseQuery(String)} (or any its alternative with an InputStream)
 * 		to parse an input ADQL query String and get its corresponding ADQL tree
 *   </li>
 *   <li>{@link #tryQuickFix(String)} to try fixing the most common
 * 		issues with ADQL queries (e.g. Unicode confusable characters,
 * 		unescaped ADQL identifiers, SQL reserved keywords, ...)</li>
 * 	<li>{@link #setSupportedFeatures(FeatureSet)} to set which optional ADQL
 * 		features are supported or not ; all optional features used in the query
 * 		while being declared as un-supported will throw an error at the end of
 * 		the parsing</li>
 * 	<li>{@link #setAllowedCoordSys(Collection)} to set which coordinate systems
 * 		are allowed when specifying a geometric region (e.g. POINT, CIRCLE,
 * 		REGION) ; <i><b>note:</b> this function is mainly useful with ADQL-2.0
 * 		because it is the only version in which the coordinate system parameter
 * 		is mandatory</i></li>
 * 	<li>{@link #allowAnyUdf(boolean)} to support any undeclared User Defined
 * 	    Function. By default only UDFs declared as <i>supported features</i>
 * 	    are allowed</li>
 * 	<li>{@link #allowExtendedRegionParam(boolean)} to allow any string
 * 	    expression and serialization as parameter of the
 * 	    <code>REGION(...)</code> function. By default, only a string literal
 * 	    using a supported serialization (see {@link Region#parse(String)} ;
 * 	    e.g. DALI, STC/s) is allowed</li>
 * </ul>
 *
 *
 * <h3>Default general checks</h3>
 *
 * <p>
 * 	This ADQL parser always performs some verification after the parsing of an
 * 	ADQL query. In addition of the syntax, this parser also ensures that no
 * 	unsupported optional language feature and no unsupported coordinate system
 * 	are used in parsed ADQL queries. If unsupported content is detected, a
 * 	{@link ParseException} is immediately raised.
 * </p>
 *
 * <p>
 * 	By default, all optional language features are supported, and any coordinate
 * 	system is allowed.
 * </p>
 *
 * <p>
 * 	By default, no undeclared UDF is allowed. To change this, use
 * 	{@link #allowAnyUdf(boolean)}.
 * </p>
 *
 * <p>
 * 	By default, only a string literal using a supported serialization (e.g. DALI
 * 	and STC/s) is allowed as parameter of the <code>REGION(...)</code> function.
 * 	It is however possible to accept any string expression or to support any
 * 	other serialization thanks to {@link #allowExtendedRegionParam(boolean)}.
 * 	<i>Look at {@link Region#parse(String)} to know the exhaustive list of
 * 	supported region serializations.</i>
 * </p>
 *
 *
 * <h3>Custom checks</h3>
 *
 * <p>
 * 	Besides the general checks, this parser allows the addition of a custom
 * 	validation. Thanks to a {@link QueryChecker} object, it is able to check
 * 	each {@link ADQLQuery} just after its generation and the general checks.
 * 	It could be used, for instance, to check the consistency between the ADQL
 * 	query to parse and the "database" on which the query must be executed.
 * </p>
 *
 * <p>
 * 	By default, there is no {@link QueryChecker}. Thus you must either use an
 * 	already existing {@link QueryChecker} or extend this latter to run your own
 * 	tests on the parsed ADQL queries.
 * </p>
 *
 * <p>
 * 	{@link DBChecker} is an extension of {@link QueryChecker} able to check that
 * 	table and column names used in a query exist in a given set of DB metadata.
 * </p>
 *
 * <h3>Custom Query Factory</h3>
 *
 * <p>
 * 	To create an object representation of the given ADQL query, this parser
 * 	uses a {@link ADQLQueryFactory} object. All parts of the ADQL grammar can
 * 	already be created with this object.
 * </p>
 *
 * <p>
 * 	However, in some special cases, you may need to change the type of some
 * 	specific nodes of the generated ADQL tree (e.g. <code>CONTAINS</code>). In
 * 	such case, you just have to extend the corresponding default object
 * 	(i.e. {@link ContainsFunction}) and to extend the corresponding function of
 * 	{@link ADQLQueryFactory} (i.e. createContains(...)). Then, give an instance
 * 	of this custom factory to the {@link ADQLParser}, at
 * 	{@link #ADQLParser(ADQLVersion, QueryChecker, ADQLQueryFactory, FeatureSet) creation}
 * 	or with the setter {@link #setQueryFactory(ADQLQueryFactory)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (04/2021)
 * @since 2.0
 */
public class ADQLParser {

	/** Grammar parser to use.
	 * <p><i><b>Implementation note:</b> Never NULL.</i></p> */
	protected final ADQLGrammar grammarParser;

	/** List of all supported features.
	 * <p><i><b>Note:</b>
	 * 	The default set of features can be set with the function
	 * 	{@link #setDefaultFeatures()}.
	 * </i></p>
	 * <p><i><b>Implementation note:</b> Never NULL.</i></p> */
	protected FeatureSet supportedFeatures;

	/** List of all allowed coordinate systems.
	 * <p>
	 * 	Each item of this list must be of the form: "{frame} {refpos} {flavor}".
	 * 	Each of these 3 items can be either of value, a list of values expressed
	 * 	with the syntax "({value1}|{value2}|...)" or a '*' to mean all possible
	 * 	values.
	 * </p>
	 * <p><i><b>Note:</b>
	 * 	since a default value (corresponding to the empty string - '') should
	 * 	always be possible for each part of a coordinate system, the checker
	 * 	will always add the default value (UNKNOWNFRAME, UNKNOWNREFPOS or
	 * 	SPHERICAL2) into the given list of possible values for each coord. sys.
	 * 	part.
	 * </i></p>
	 * <p>
	 * 	If this list is NULL, all coordinates systems are allowed.
	 * 	However, if not, all items of this list must be the only allowed
	 * 	coordinate systems. So, if the list is empty, none is allowed.
	 * </p> */
	protected String[] allowedCoordSys = null;

	/** A regular expression built using the list of allowed coordinate systems.
	 * <p>
	 * 	With this regex, it is possible to known whether a coordinate system
	 * 	expression is allowed or not.
	 * </p>
	 * <p>If NULL, all coordinate systems are allowed.</p> */
	protected String coordSysRegExp = null;

	/** API to check {@link ADQLQuery ADQL queries} (sub-queries or not) just
	 * after their generation.
	 * <p><i><b>Note:</b>
	 * 	This check step is optional. To ignore it, set this attribute to NULL.
	 * </i></p> */
	protected QueryChecker queryChecker = null;

	/** This object is used only when one of the {@link #tryQuickFix(String)}
	 * functions is called. It allows to try fixing common errors in the given
	 * ADQL query.
	 * <p><i><b>Implementation note:</b> Never NULL.</i></p> */
	protected QueryFixer quickFixer;

	/** Indicate whether any UDF (even if not declared) should be considered as
	 * supported. */
	protected boolean anyUdfAllowed = false;

	/** Indicate whether the REGION(...) function accepts any string expression
	 * and any serialization or only a string literal using a supported
	 * serialization (e.g. DALI, STC/s). */
	protected boolean extendedRegionExpressionAllowed = false;

	/* **********************************************************************
	   *                       VERSION MANAGEMENT                           *
	   ********************************************************************** */

	/**
	 * Enumeration of all supported versions of the ADQL grammar.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0 (08/2019)
	 * @since 2.0
	 */
	public static enum ADQLVersion {
		/** Version REC-2.0 - <a href="http://www.ivoa.net/documents/cover/ADQL-20081030.html">http://www.ivoa.net/documents/cover/ADQL-20081030.html</a>. */
		V2_0,
		/** Version PR-2.1 - <a href="http://www.ivoa.net/documents/ADQL/20180112/index.html">http://www.ivoa.net/documents/ADQL/20180112/index.html</a>. */
		V2_1; // TODO Move 2.1 as default when it becomes REC

		@Override
		public String toString() {
			return name().toLowerCase().replace('_', '.');
		}

		/**
		 * Parse the given string as an ADQL version number.
		 *
		 * <p>This function should work with the following syntaxes:</p>
		 * <ul>
		 * 	<li><code>2.0</code></li>
		 * 	<li><code>2_0</code></li>
		 * 	<li><code>v2.0</code> or <code>V2.0</code></li>
		 * 	<li><code>v2_0</code> or <code>V2_0</code></li>
		 * </ul>
		 *
		 * @param str	String to parse as an ADQL version specification.
		 *
		 * @return	The identified ADQL version.
		 */
		public static ADQLVersion parse(String str) {
			if (str == null)
				return null;

			str = str.trim().toUpperCase();

			if (str.isEmpty())
				return null;

			if (str.charAt(0) != 'V')
				str = 'V' + str;

			try {
				return ADQLVersion.valueOf(str.replaceAll("\\.", "_"));
			} catch(IllegalArgumentException iae) {
				return null;
			}
		}
	}

	/** Version of the ADQL grammar to use when none is specified:
	 * {@link ADQLVersion#V2_0 2.0}. */
	public final static ADQLVersion DEFAULT_VERSION = ADQLVersion.V2_0; // TODO Move 2.1 as default when it becomes REC

	/**
	 * Get the list of all supported ADQL grammar versions.
	 *
	 * @return	List of all supported ADQL versions.
	 *
	 * @see ADQLVersion#values()
	 */
	public static ADQLVersion[] getSupportedVersions() {
		return ADQLVersion.values();
	}

	/**
	 * Build on the fly a human list of all supported ADQL grammar versions.
	 *
	 * <p>
	 * 	The default version item will be suffixed by the string
	 * 	<code>(default)</code>.
	 * </p>
	 *
	 * <i>
	 * <p><b>Example:</b></p>
	 * <pre>v2.0, v2.1 (default)</pre>
	 * </i>
	 *
	 * @return	List of all supported ADQL versions.
	 */
	public static String getSupportedVersionsAsString() {
		StringBuilder buf = new StringBuilder();
		for(ADQLVersion v : ADQLVersion.values()) {
			if (buf.length() > 0)
				buf.append(", ");
			buf.append(v.toString());
			if (v == DEFAULT_VERSION)
				buf.append(" (default)");
		}
		return buf.toString();
	}

	/* **********************************************************************
	   *                        PARSER CREATION                             *
	   ********************************************************************** */

	/**
	 * Builds an ADQL query parser for the default (i.e. last stable) version
	 * of the ADQL grammar.
	 *
	 * <p>This parser is set with:</p>
	 * <ul>
	 * 	<li>the {@link #DEFAULT_VERSION default version} of the ADQL
	 * 		grammar,</li>
	 * 	<li>the {@link #setDefaultFeatures() default set} of optional features,</li>
	 * 	<li>the default {@link ADQLQueryFactory ADQL query factory},</li>
	 * 	<li>and no custom check (i.e. no {@link QueryChecker} is set).</li>
	 * </ul>
	 *
	 * @see #DEFAULT_VERSION
	 */
	public ADQLParser() {
		this(DEFAULT_VERSION, null, null, null);
	}

	/**
	 * Builds an ADQL query parser for the specified version of the ADQL
	 * grammar.
	 *
	 * <p>This parser is set with:</p>
	 * <ul>
	 * 	<li>the specified version of the ADQL grammar,</li>
	 * 	<li>the {@link #setDefaultFeatures() default set} of optional features,</li>
	 * 	<li>the default {@link ADQLQueryFactory ADQL query factory},</li>
	 * 	<li>and no custom check (i.e. no {@link QueryChecker} is set).</li>
	 * </ul>
	 *
	 * @param version	Version of the ADQL grammar that the parser must
	 *               	implement.
	 *               	<i>If NULL, the {@link #DEFAULT_VERSION} will be used.</i>
	 */
	public ADQLParser(ADQLVersion version) {
		this(version, null, null, null);
	}

	/**
	 * Builds a parser whose the query to parse will have to be given as a
	 * String in parameter of
	 * {@link ADQLParser#parseQuery(java.lang.String) parseQuery(String)}.
	 *
	 * @param version		Version of the ADQL grammar that the parser must
	 *               		implement.
	 *               		<i>If NULL, the {@link #DEFAULT_VERSION} will be
	 *               		used.</i>
	 * @param queryChecker	The custom checks to perform.
	 *                    	<i>If NULL, only the general checks (e.g. supported
	 *                    	features, UDFs, types) will be run.</i>
	 * @param factory		The factory of ADQL objects to use.
	 *               		<i>If NULL, the default query factory will be
	 *               		used.</i>
	 * @param features		The set of supported features.
	 *                		<i>If NULL, the default set of supported features
	 *                		will be used (see {@link #setDefaultFeatures()}).</i>
	 */
	public ADQLParser(ADQLVersion version, final QueryChecker queryChecker, final ADQLQueryFactory factory, final FeatureSet features) {
		// Prevent the NULL value by setting the default version if necessary:
		if (version == null)
			version = DEFAULT_VERSION;

		// Create the appropriate parser in function of the specified version:
		switch(version) {
			case V2_0:
				grammarParser = new ADQLGrammar200(new java.io.ByteArrayInputStream("".getBytes()));
				break;
			case V2_1:
			default:
				grammarParser = new ADQLGrammar201(new java.io.ByteArrayInputStream("".getBytes()));
				break;
		}

		// Set the query fixer to use (a default one):
		setQuickFixer(new QueryFixer(grammarParser));

		// Set the appropriate feature set:
		if (features == null)
			setDefaultFeatures();
		else
			setSupportedFeatures(features);

		// Set the query factory:
		setQueryFactory((factory == null) ? new ADQLQueryFactory() : factory);

		// Set the query checker, if any:
		setQueryChecker(queryChecker);

		// Disable debugging messages by default:
		setDebug(false);
	}

	/* **********************************************************************
	   *                        GETTERS & SETTERS                           *
	   ********************************************************************** */

	/**
	 * Get the ADQL grammar version followed by this parser.
	 *
	 * @return	The target ADQL version.
	 */
	public final ADQLVersion getADQLVersion() {
		return grammarParser.getVersion();
	}

	/**
	 * Get the internal grammar parser specific to the target ADQL version.
	 *
	 * <p><i><b>Warning:</b>
	 * 	Changing the configuration of this internal parser might break the
	 * 	normal functioning of this {@link ADQLParser} instance. It is
	 * 	recommended to not use directly this internal parser. The goal of
	 * 	{@link ADQLParser} is to provide a nice and safe parser API. If
	 * 	something is missing or incorrect, please, contact the library
	 * 	developer so that this API can be completed/fixed.
	 * </i></p>
	 *
	 * @return	The internal grammar parser.
	 */
	public final ADQLGrammar getGrammarParser() {
		return grammarParser;
	}

	/**
	 * Get the API used to attempt fixing given ADQL queries with the functions
	 * {@link #tryQuickFix(InputStream)} and {@link #tryQuickFix(String)}.
	 *
	 * @return	The query fixer tool.
	 */
	public final QueryFixer getQuickFixer() {
		return quickFixer;
	}

	/**
	 * Set the tool to use in order to attempt fixing any given ADQL query with
	 * the functions {@link #tryQuickFix(InputStream)} and
	 * {@link #tryQuickFix(String)}.
	 *
	 * @param fixer	The tool to use.
	 *
	 * @throws NullPointerException	If the given fixer is NULL.
	 */
	public final void setQuickFixer(final QueryFixer fixer) throws NullPointerException {
		if (fixer == null)
			throw new NullPointerException("Missing new QuickFixer! An ADQLParser can not try to fix ADQL queries without a QuickFixer instance.");
		quickFixer = fixer;
	}

	/**
	 * Get the query factory used to create ADQL objects during the parsing
	 * of an ADQL query.
	 *
	 * @return	The used ADQL query factory.
	 */
	public final ADQLQueryFactory getQueryFactory() {
		return grammarParser.getQueryFactory();
	}

	/**
	 * Set the query factory to use when creating ADQL objects during the
	 * parsing of an ADQL query.
	 *
	 * @param factory	The ADQL query factory to use.
	 *
	 * @throws NullPointerException	If the given factory is NULL.
	 */
	public final void setQueryFactory(ADQLQueryFactory factory) throws NullPointerException {
		if (factory == null)
			throw new NullPointerException("Missing ADQLQueryFactory to use! It is required for ADQL query parsing.");
		grammarParser.setQueryFactory(factory);
	}

	/**
	 * Get the list of all supported features.
	 *
	 * <p><i><b>Note:</b>
	 * 	To customize the list of supported features, either get the set with
	 * 	this function and then update it directly, or set a new
	 * 	{@link FeatureSet} instance with
	 * 	{@link #setSupportedFeatures(FeatureSet)}.
	 * </i></p>
	 *
	 * @return	Set of supported features.
	 */
	public final FeatureSet getSupportedFeatures() {
		return supportedFeatures;
	}

	/**
	 * Set a default set of supported language features in function of the
	 * target ADQL version.
	 *
	 * <ul>
	 * 	<li><b>ADQL-2.0:</b> the geometric functions are the only supported
	 * 		features.</li>
	 * 	<li><b>ADQL-2.1:</b> all optional features are supported.</li>
	 * </ul>
	 *
	 * <p><i><b>Note:</b>
	 * 	To customize the list of supported features, either get the set with
	 * 	{@link #getSupportedFeatures()} and then update it directly, or set a
	 * 	new {@link FeatureSet} instance with
	 * 	{@link #setSupportedFeatures(FeatureSet)}.
	 * </i></p>
	 */
	public final void setDefaultFeatures() {
		switch(getADQLVersion()) {
			case V2_0:
				// any UDF is allowed and no optional feature supported...:
				this.supportedFeatures = new FeatureSet(false);
				// ...except geometries which are all supported by default:
				supportedFeatures.supportAll(LanguageFeature.TYPE_ADQL_GEO);
				break;
			case V2_1:
			default:
				// all available features are considered as supported:
				this.supportedFeatures = new FeatureSet(true);
				break;
		}
	}

	/**
	 * Set a new set of supported features.
	 *
	 * @param features	The new list of supported features.
	 *
	 * @throws NullPointerException	If the given object is NULL.
	 */
	public final void setSupportedFeatures(final FeatureSet features) throws NullPointerException {
		if (features == null)
			throw new NullPointerException("Missing list of supported features! It is required for ADQL query parsing.");
		supportedFeatures = features;
	}

	/**
	 * Get the list of allowed coordinate systems.
	 *
	 * <p>
	 * 	If NULL, any coordinate system is allowed.
	 * 	But if empty array, no coordinate system is allowed.
	 * </p>
	 *
	 * @return	All allowed coordinate systems.
	 */
	public final String[] getAllowedCoordSys() {
		return allowedCoordSys;
	}

	/**
	 * Set the list of allowed coordinate systems.
	 *
	 * <p>
	 * 	Each item of this list must be of the form: "{frame} {refpos} {flavor}".
	 * 	Each of these 3 items can be either of value, a list of values expressed
	 * 	with the syntax "({value1}|{value2}|...)" or a '*' to mean all possible
	 * 	values.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	since a default value (corresponding to the empty string - '') should
	 * 	always be possible for each part of a coordinate system, this parser
	 * 	will always add the default value (UNKNOWNFRAME, UNKNOWNREFPOS or
	 * 	SPHERICAL2) into the given list of possible values for each coord. sys.
	 * 	part.
	 * </i></p>
	 *
	 * <p>
	 * 	If this list is NULL, all coordinates systems are allowed.
	 * 	However, if not, all items of this list must be the only allowed
	 * 	coordinate systems. So, if the list is empty, none is allowed.
	 * </p>
	 *
	 * <p><i><b>Note:</b>
	 * 	When an exception is thrown, the list of allowed coordinate systems of
	 * 	this parser stays in the same state than before calling this function.
	 * </i></p>
	 *
	 * @param allowedCoordSys	List of allowed coordinate systems.
	 *
	 * @throws ParseException	If the syntax of one of the given coordinate
	 *                       	system is incorrect.
	 */
	public final void setAllowedCoordSys(final Collection<String> allowedCoordSys) throws ParseException {
		String[] tempAllowedCoordSys = specialSort(allowedCoordSys);
		coordSysRegExp = CoordSys.buildCoordSysRegExp(tempAllowedCoordSys);
		this.allowedCoordSys = tempAllowedCoordSys;
	}

	/**
	* Let specify whether any UDF (even if not declared) should be considered
	* as supported or not. If not, UDFs must be explicitly declared to be
	* considered as supported (as any other optional language feature).
	*
	* @param allowed	<code>true</code> to support any UDF,
	*               	<code>false</code> to force the declaration of supported
	*               	UDFs.
	*/
	public void allowAnyUdf(final boolean allowed) {
		this.anyUdfAllowed = allowed;
	}

	/**
	* Tell whether UDFs are considered as supported even if undeclared.
	*
	* @return	<code>true</code> if any UDF is considered as supported,
	*        	<code>false</code> if supported UDFs must be explicitly
	*        	declared.
	*/
	public boolean isAnyUdfAllowed() {
		return anyUdfAllowed;
	}

	/**
	* Let specify whether any UDF (even if not declared) should be considered
	* as supported or not. If not, UDFs must be explicitly declared to be
	* considered as supported (as any other optional language feature).
	*
	* @param allowed	<code>true</code> to support any UDF,
	*               	<code>false</code> to force the declaration of supported
	*               	UDFs.
	*/
	public void allowExtendedRegionParam(final boolean allowed) {
		this.anyUdfAllowed = allowed;
	}

	/**
	* Tell whether UDFs are considered as supported even if undeclared.
	*
	* @return	<code>true</code> if any UDF is considered as supported,
	*        	<code>false</code> if supported UDFs must be explicitly
	*        	declared.
	*/
	public boolean isExtendedRegionParamAllowed() {
		return anyUdfAllowed;
	}

	/**
	 * Get the custom checker of parsed ADQL queries.
	 *
	 * @return	Custom query checker,
	 *        	or NULL if no custom check is available.
	 */
	public final QueryChecker getQueryChecker() {
		return queryChecker;
	}

	/**
	 * Set a custom checker of parsed ADQL queries.
	 *
	 * @param checker	The custom query checks to run,
	 *               	or NULL to have no custom check.
	 */
	public final void setQueryChecker(QueryChecker checker) {
		queryChecker = checker;
	}

	/**
	 * Enable/Disable the debugging messages while parsing an ADQL expression.
	 *
	 * @param debug	<code>true</code> to enable debugging,
	 *             	<code>false</code> to disable it.
	 */
	public final void setDebug(final boolean debug) {
		if (debug)
			grammarParser.enable_tracing();
		else
			grammarParser.disable_tracing();
	}

	/* **********************************************************************
	   *                        PARSING FUNCTIONS                           *
	   ********************************************************************** */

	/**
	* Parses the query string given in parameter.
	*
	* @param q	The ADQL query to parse.
	*
	* @return	The object representation of the given ADQL query.
	*
	* @throws ParseException	If there is at least one error.
	*
	* @see #effectiveParseQuery()
	*/
	public final ADQLQuery parseQuery(final String q) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new ByteArrayInputStream(q.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Run the query parsing:
		return effectiveParseQuery();
	}

	/**
	* Parses the query contained in the stream given in parameter.
	*
	* @param stream		The stream which contains the ADQL query to parse.
	*
	* @return	The object representation of the given ADQL query.
	*
	* @throws ParseException	If there is at least one error.
	*
	* @see #effectiveParseQuery()
	*/
	public final ADQLQuery parseQuery(final InputStream stream) throws ParseException {
		// Reset the parser with the stream to parse:
		try {
			grammarParser.reset(stream);
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Run the query parsing:
		return effectiveParseQuery();
	}

	/**
	 * Run the query parsing, then, if successful, all the available checks on
	 * the parsing result (i.e. the query tree).
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @return	The object representation of the successfully parsed query
	 *        	(i.e. the ADQL tree).
	 *
	 * @throws ParseException	If syntax is incorrect (i.e. no ADQL tree can be
	 *                       	generated), or if any check on the parsing
	 *                       	result fails.
	 *
	 * @see #allChecks(ADQLQuery)
	 */
	protected ADQLQuery effectiveParseQuery() throws ParseException {
		// 1. Parse the full ADQL query:
		ADQLQuery parsedQuery;
		try {
			parsedQuery = grammarParser.Query();
		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		}

		// 2. Run all available checks:
		allChecks(parsedQuery);

		// If no syntactic error and that all checks passed, return the result:
		return parsedQuery;
	}

	/**
	 * Parse the given <code>SELECT</code> clause.
	 *
	 * <i>
	 * <p><b>Important note:</b>
	 * 	The given string MUST start with <code>SELECT</code> (case insensitive).
	 * 	It MUST also follow the syntax of the FULL clause as described in the
	 * 	appropriate version of the ADQL Grammar.
	 * </p>
	 * <p><b>Examples of INcorrect parameter:</b></p>
	 * <ul>
	 * 	<li><code>SELECT</code></li>
	 * 	<li><code>aColumn</code></li>
	 * 	<li><code>DISTINCT TOP 10 aColumn, bColumn AS "B"</code></li>
	 * </ul>
	 * <p><b>Example of correct parameter:</b></p>
	 * <pre>SELECT DISTINCT TOP 10 aColumn, bColumn AS "B"</pre>
	 * </i>
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @param adql	The <code>SELECT</code> clause to parse.
	 *
	 * @return	The corresponding object representation of the given clause.
	 *
	 * @throws ParseException	If the syntax of the given clause is incorrect.
	 */
	public final ClauseSelect parseSelect(final String adql) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new java.io.ByteArrayInputStream(adql.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Parse the string:
		try {

			// Parse the string as a SELECT clause:
			grammarParser.Select();

			// Run all available checks on this ADQL query part:
			allChecks(grammarParser.getQuery());

			// Return what's just got parsed:
			return grammarParser.getQuery().getSelect();

		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		}
	}

	/**
	 * Parse the given <code>FROM</code> clause.
	 *
	 * <i>
	 * <p><b>Important note:</b>
	 * 	The given string MUST start with <code>FROM</code> (case insensitive).
	 * 	It MUST also follow the syntax of the FULL clause as described in the
	 * 	appropriate version of the ADQL Grammar.
	 * </p>
	 * <p><b>Examples of INcorrect parameter:</b></p>
	 * <ul>
	 * 	<li><code>FROM</code></li>
	 * 	<li><code>aTable</code></li>
	 * 	<li><code>aTable JOIN bTable</code></li>
	 * 	<li><code>aTable JOIN bTable AS "B" USING(id)</code></li>
	 * </ul>
	 * <p><b>Example of correct parameter:</b></p>
	 * <pre>FROM aTable JOIN bTable AS "B" USING(id)</pre>
	 * </i>
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @param adql	The <code>FROM</code> clause to parse.
	 *
	 * @return	The corresponding object representation of the given clause.
	 *
	 * @throws ParseException	If the syntax of the given clause is incorrect.
	 */
	public final FromContent parseFrom(java.lang.String adql) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new java.io.ByteArrayInputStream(adql.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Parse the string:
		try {

			// Parse the string as a FROM clause:
			grammarParser.From();

			// Run all available checks on this ADQL query part:
			allChecks(grammarParser.getQuery());

			// Return what's just got parsed:
			return grammarParser.getQuery().getFrom();

		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		}
	}

	/**
	 * Parse the given <code>WHERE</code> clause.
	 *
	 * <i>
	 * <p><b>Important note:</b>
	 * 	The given string MUST start with <code>WHERE</code> (case insensitive).
	 * 	It MUST also follow the syntax of the FULL clause as described in the
	 * 	appropriate version of the ADQL Grammar.
	 * </p>
	 * <p><b>Examples of INcorrect parameter:</b></p>
	 * <ul>
	 * 	<li><code>WHERE</code></li>
	 * 	<li><code>foo</code></li>
	 * 	<li><code>foo = 'bar'</code></li>
	 * </ul>
	 * <p><b>Example of correct parameter:</b></p>
	 * <pre>WHERE foo = 'bar'</pre>
	 * </i>
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @param adql	The <code>WHERE</code> clause to parse.
	 *
	 * @return	The corresponding object representation of the given clause.
	 *
	 * @throws ParseException	If the syntax of the given clause is incorrect.
	 */
	public final ClauseConstraints parseWhere(java.lang.String adql) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new java.io.ByteArrayInputStream(adql.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Parse the string:
		try {

			// Parse the string as a WHERE clause:
			grammarParser.Where();

			// Run all available checks on this ADQL query part:
			allChecks(grammarParser.getQuery());

			// Return what's just got parsed:
			return grammarParser.getQuery().getWhere();

		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		}
	}

	/**
	 * Parse the given <code>ORDER BY</code> clause.
	 *
	 * <i>
	 * <p><b>Important note:</b>
	 * 	The given string MUST start with <code>ORDER BY</code> (case insensitive).
	 * 	It MUST also follow the syntax of the FULL clause as described in the
	 * 	appropriate version of the ADQL Grammar.
	 * </p>
	 * <p><b>Examples of INcorrect parameter:</b></p>
	 * <ul>
	 * 	<li><code>ORDER BY</code></li>
	 * 	<li><code>aColumn DESC</code></li>
	 * </ul>
	 * <p><b>Example of correct parameter:</b></p>
	 * <pre>ORDER BY aColumn DESC</pre>
	 * </i>
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @param adql	The <code>ORDER BY</code> clause to parse.
	 *
	 * @return	The corresponding object representation of the given clause.
	 *
	 * @throws ParseException	If the syntax of the given clause is incorrect.
	 */
	public final ClauseADQL<ADQLOrder> parseOrderBy(java.lang.String adql) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new java.io.ByteArrayInputStream(adql.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Parse the string:
		try {

			// Parse the string as a ORDER BY clause:
			grammarParser.OrderBy();

			// Run all available checks on this ADQL query part:
			allChecks(grammarParser.getQuery());

			// Return what's just got parsed:
			return grammarParser.getQuery().getOrderBy();

		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}
	}

	/**
	 * Parse the given <code>GROUP BY</code> clause.
	 *
	 * <i>
	 * <p><b>Important note:</b>
	 * 	The given string MUST start with <code>GROUP BY</code> (case insensitive).
	 * 	It MUST also follow the syntax of the FULL clause as described in the
	 * 	appropriate version of the ADQL Grammar.
	 * </p>
	 * <p><b>Examples of INcorrect parameter:</b></p>
	 * <ul>
	 * 	<li><code>GROUP BY</code></li>
	 * 	<li><code>aColumn</code></li>
	 * </ul>
	 * <p><b>Example of correct parameter:</b></p>
	 * <pre>GROUP BY aColumn</pre>
	 * </i>
	 *
	 * <p>
	 * 	This functions stops immediately with a {@link ParseException} if the
	 * 	parsing failed or if any of the available checks fails.
	 * </p>
	 *
	 * @param adql	The <code>GROUP BY</code> clause to parse.
	 *
	 * @return	The corresponding object representation of the given clause.
	 *
	 * @throws ParseException	If the syntax of the given clause is incorrect.
	 */
	public final ClauseADQL<ADQLOperand> parseGroupBy(java.lang.String adql) throws ParseException {
		// Reset the parser with the string to parse:
		try {
			grammarParser.reset(new java.io.ByteArrayInputStream(adql.getBytes()));
		} catch(Exception ex) {
			throw grammarParser.generateParseException(ex);
		}

		// Parse the string:
		try {

			// Parse the string as a GROUP BY clause:
			grammarParser.GroupBy();

			// Run all available checks on this ADQL query part:
			allChecks(grammarParser.getQuery());

			// Return what's just got parsed:
			return grammarParser.getQuery().getGroupBy();

		} catch(TokenMgrError tme) {
			throw new ParseException(tme);
		}
	}

	/* **********************************************************************
	   *                          QUERY CHECKS                              *
	   ********************************************************************** */

	/**
	 * Run all available checks on the given ADQL tree:
	 *
	 * <ul>
	 * 	<li>the general checks: optional features support, region
	 * 	    serializations, ...</li>
	 * 	<li>the custom checks (if any).</li>
	 * </ul>
	 *
	 * @param q	The ADQL query to check.
	 *
	 * @throws ParseException	If any of the common checks or any of the
	 *                       	optional ones failed
	 *
	 * @see #generalChecks(ADQLQuery)
	 * @see QueryChecker#check(ADQLQuery)
	 */
	protected void allChecks(final ADQLQuery q) throws ParseException {
		/* Run the general checks on the parsed query:
		 * (note: this check is very close to grammar check...hence its higher
		 *        priority) */
		generalChecks(q);

		// Run the custom checks (if any):
		if (queryChecker != null)
			queryChecker.check(q);
	}

	/**
	 * Run the general and common checks on the given ADQL tree.
	 *
	 * <p>
	 * 	By default, this function checks whether or not language features
	 * 	found in the given ADQL tree are supported. It also checks all explicit
	 * 	coordinate systems and STC-s expressions (embedded in REGION function).
	 * 	All unsupported expressions (i.e. feature, coord. sys., STC-s) are
	 * 	appended into an {@link UnresolvedIdentifiersException} which is finally
	 * 	thrown if not empty.
	 * </p>
	 *
	 * @param q	The ADQL query to check.
	 *
	 * @throws ParseException	If any unsupported language feature is used in
	 *                       	the given ADQL tree.
	 */
	protected void generalChecks(final ADQLQuery q) throws ParseException {
		// Create the exception in which errors have to be appended:
		UnresolvedIdentifiersException exUnsupportedFeatures = new UnresolvedIdentifiersException("unsupported expression");

		// Search recursively for all optional features inside the given tree:
		SearchOptionalFeaturesHandler sFeaturesHandler = new SearchOptionalFeaturesHandler(true, false);
		sFeaturesHandler.search(q);

		// Append an error for each unsupported one:
		for(ADQLObject obj : sFeaturesHandler) {
			// ignore UDF if any UDF is allowed:
			if (!isAnyUdfAllowed() || !LanguageFeature.TYPE_UDF.equals(obj.getFeatureDescription().type)) {
				// otherwise, test whether this feature is supported:
				if (!supportedFeatures.isSupporting(obj.getFeatureDescription()))
					exUnsupportedFeatures.addException(new UnsupportedFeatureException(obj));
			}
		}

		// [only for ADQL-2.0] Resolve explicit coordinate system declarations:
		resolveCoordinateSystems(q, exUnsupportedFeatures);

		// [only for ADQL-2.0] Resolve explicit REGION declarations:
		if (supportedFeatures.isSupporting(RegionFunction.FEATURE))
			resolveRegionExpressions(q, exUnsupportedFeatures);

		// If unsupported features have been found, throw a ParseException:
		if (exUnsupportedFeatures.getNbErrors() > 0)
			throw exUnsupportedFeatures;
	}

	/**
	 * Transform the given collection of string elements into a sorted array.
	 * Only non-NULL and non-empty strings are kept.
	 *
	 * @param items	Items to copy and sort.
	 *
	 * @return	A sorted array containing all - except NULL and empty strings -
	 *        	items of the given collection.
	 */
	protected final String[] specialSort(final Collection<String> items) {
		// Nothing to do if the array is NULL:
		if (items == null)
			return null;

		// Keep only valid items (not NULL and not empty string):
		String[] tmp = new String[items.size()];
		int cnt = 0;
		for(String item : items) {
			if (item != null && item.trim().length() > 0)
				tmp[cnt++] = item;
		}

		// Make an adjusted array copy:
		String[] copy = new String[cnt];
		System.arraycopy(tmp, 0, copy, 0, cnt);

		// Sort the values:
		Arrays.sort(copy);

		return copy;
	}

	/**
	 * Search for all explicit coordinate system declarations, check their
	 * syntax and whether they are allowed by this implementation.
	 *
	 * <p><i><b>Note:</b>
	 * 	"explicit" means here that all {@link StringConstant} instances. Only
	 * 	coordinate systems expressed as string can be parsed and so checked. So
	 * 	if a coordinate system is specified by a column, no check can be done at
	 * 	this stage...it will be possible to perform such test only at the
	 * 	execution.
	 * </i></p>
	 *
	 * @param query		Query in which coordinate systems must be checked.
	 * @param errors	List of errors to complete in this function each time a
	 *              	coordinate system has a wrong syntax or is not
	 *              	supported.
	 *
	 * @see #checkCoordinateSystem(StringConstant, UnresolvedIdentifiersException)
	 */
	protected void resolveCoordinateSystems(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
		ISearchHandler sHandler = new SearchCoordSysHandler();
		sHandler.search(query);
		for(ADQLObject result : sHandler)
			checkCoordinateSystem((StringConstant)result, errors);
	}

	/**
	 * Parse and then check the coordinate system contained in the given
	 * {@link StringConstant} instance.
	 *
	 * @param adqlCoordSys	The {@link StringConstant} object containing the
	 *                    	coordinate system to check.
	 * @param errors		List of errors to complete in this function each
	 *              		time a coordinate system has a wrong syntax or is
	 *              		not supported.
	 *
	 * @see STCS#parseCoordSys(String)
	 * @see #checkCoordinateSystem(CoordSys, ADQLOperand, UnresolvedIdentifiersException)
	 */
	protected void checkCoordinateSystem(final StringConstant adqlCoordSys, final UnresolvedIdentifiersException errors) {
		String coordSysStr = adqlCoordSys.getValue();
		try {
			checkCoordinateSystem(STCS.parseCoordSys(coordSysStr), adqlCoordSys, errors);
		} catch(ParseException pe) {
			errors.addException(new ParseException(pe.getMessage(), adqlCoordSys.getPosition()));
		}
	}

	/**
	 * Check whether the given coordinate system is allowed by this
	 * implementation.
	 *
	 * @param coordSys	Coordinate system to test.
	 * @param operand	The operand representing or containing the coordinate
	 *               	system under test.
	 * @param errors	List of errors to complete in this function each time a
	 *              	coordinate system is not supported.
	 */
	protected void checkCoordinateSystem(final CoordSys coordSys, final ADQLOperand operand, final UnresolvedIdentifiersException errors) {
		if (coordSysRegExp != null && coordSys != null && !coordSys.toFullSTCS().matches(coordSysRegExp)) {
			StringBuffer buf = new StringBuffer();
			if (allowedCoordSys != null) {
				for(String cs : allowedCoordSys) {
					if (buf.length() > 0)
						buf.append(", ");
					buf.append(cs);
				}
			}
			if (buf.length() == 0)
				buf.append("No coordinate system is allowed!");
			else
				buf.insert(0, "Allowed coordinate systems are: ");
			errors.addException(new ParseException("Coordinate system \"" + ((operand instanceof StringConstant) ? ((StringConstant)operand).getValue() : coordSys.toString()) + "\" (= \"" + coordSys.toFullSTCS() + "\") not allowed in this implementation. " + buf.toString(), operand.getPosition()));
		}
	}

	/**
	 * Search for all region expressions inside the given query, parse them (and
	 * so check their syntax) and then determine whether the declared coordinate
	 * system and the expressed region are allowed in this implementation.
	 *
	 * @param query		Query in which region expressions must be checked.
	 * @param errors	List of errors to complete in this function each time
	 *              	the region syntax is wrong or each time the declared
	 *              	coordinate system or region is not supported.
	 *
	 * @see Region#parse(String)
	 * @see #checkRegion(Region, RegionFunction, UnresolvedIdentifiersException)
	 */
	protected void resolveRegionExpressions(final ADQLQuery query, final UnresolvedIdentifiersException errors) {
		// Search REGION functions:
		ISearchHandler sHandler = new SearchRegionHandler();
		sHandler.search(query);

		// Parse and check their region expression:
		String regionStr;
		Region region;
		for(ADQLObject result : sHandler) {
			RegionFunction fct = (RegionFunction)result;

			/* ensure the region is translated into the corresponding geometry
			 * if a string literal, or merely as in ADQL if not: */
			fct.setExtendedRegionExpression(isExtendedRegionParamAllowed());

			// no test to run, if not only a string literal is allowed:
			if (!isExtendedRegionParamAllowed()) {

				// ensure the parameter is a string literal:
				if (fct.getParameter(0) instanceof StringConstant) {
					try {

						// ...then, get the region expression:
						regionStr = ((StringConstant)((RegionFunction)result).getParameter(0)).getValue();

						// ...parse it (and so check its syntax):
						region = Region.parse(regionStr);

						// ...and finally check whether the regions (this one + the possible inner ones) and the coordinate systems are allowed:
						checkRegion(region, (RegionFunction)result, errors);

					} catch(ParseException pe) {
						errors.addException(new ParseException(pe.getMessage(), result.getPosition()));
					}
				}
				// if not a string literal, ERROR!
				else
					errors.addException(new ParseException("Unsupported REGION(...) parameter! Only a string literal is accepted.", result.getPosition()));
			}
		}
	}

	/**
	 * Check the given region.
	 *
	 * <p>The following points are checked in this function:</p>
	 * <ul>
	 * 	<li>whether the coordinate system is allowed,</li>
	 * 	<li>whether the type of region is allowed,</li>
	 * 	<li>and whether the inner regions are correct (here this function is
	 * 		called recursively on each inner region).</li>
	 * </ul>
	 *
	 * @param r			The region to check.
	 * @param fct		The REGION function containing the region to check.
	 * @param errors	List of errors to complete in this function if the given
	 *              	region or its inner regions are not supported.
	 *
	 * @see #checkCoordinateSystem(CoordSys, ADQLOperand, UnresolvedIdentifiersException)
	 * @see #checkRegion(Region, RegionFunction, UnresolvedIdentifiersException)
	 */
	protected void checkRegion(final Region r, final RegionFunction fct, final UnresolvedIdentifiersException errors) {
		if (r == null)
			return;

		// Check the coordinate system (if any):
		if (r.coordSys != null)
			checkCoordinateSystem(r.coordSys, fct, errors);

		// Check that the region type is allowed:
		LanguageFeature feature;
		switch(r.type) {
			case POSITION:
				feature = PointFunction.FEATURE;
				break;
			case BOX:
				feature = BoxFunction.FEATURE;
				break;
			case CIRCLE:
				feature = CircleFunction.FEATURE;
				break;
			case POLYGON:
				feature = PolygonFunction.FEATURE;
				break;
			default:
				/* TODO Add a case for UNION and INTERSECT when supported! */
				feature = null;
				break;
		}
		if (r.type != Region.RegionType.NOT && (feature == null || !supportedFeatures.isSupporting(feature)))
			errors.addException(new UnsupportedFeatureException(fct, "Unsupported region type: \"" + r.type + "\"" + (feature == null ? "!" : " (equivalent to the ADQL feature \"" + feature.form + "\" of type '" + feature.type + "')!")));

		// Check all the inner regions:
		if (r.regions != null) {
			for(Region innerR : r.regions)
				checkRegion(innerR, fct, errors);
		}
	}

	/**
	 * Let searching all explicit declarations of coordinate system.
	 * So, only {@link StringConstant} objects will be returned.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0 (08/2019)
	 * @since 2.0
	 */
	private static class SearchCoordSysHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			if (obj instanceof PointFunction || obj instanceof BoxFunction || obj instanceof CircleFunction || obj instanceof PolygonFunction)
				return (((GeometryFunction)obj).getCoordinateSystem() instanceof StringConstant);
			else
				return false;
		}

		@Override
		protected void addMatch(ADQLObject matchObj, ADQLIterator it) {
			results.add(((GeometryFunction)matchObj).getCoordinateSystem());
		}

	}

	/**
	 * Let search for all {@link RegionFunction}s.
	 *
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @version 2.0 (04/2021)
	 * @since 2.0
	 */
	private static class SearchRegionHandler extends SimpleSearchHandler {
		@Override
		protected boolean match(ADQLObject obj) {
			return (obj instanceof RegionFunction);
		}

	}

	/* **********************************************************************
	   *                     TOKENIZATION FUNCTION                          *
	   ********************************************************************** */

	/**
	 * Parse the given ADQL expression and split it into {@link Token}s.
	 *
	 * <i>
	 * <p><b>Note:</b>
	 * 	If <code>stopAtEnd=true</code>, the encountered EOQ (i.e. End Of Query
	 * 	= <code>;</code>) or EOF (i.e. End Of File) are NOT included in the
	 * 	returned array.
	 * </p>
	 *
	 * <p><b>Example:</b>
	 * <pre> tokenize("SELECT ; FROM", <b>false</b>); // = { SELECT, EOQ, FROM }
	 * tokenize("SELECT ; FROM", <b>true</b>);  // = { SELECT }</pre>
	 * </i>
	 *
	 * @param expr		The ADQL expression to tokenize.
	 * @param stopAtEnd	<code>true</code> to stop the tokenization process when
	 *                 	an EOQ or an EOF is encountered,
	 *                 	<code>false</code> to stop when the end of the string is
	 *                 	reached.
	 *
	 * @return	The corresponding ordered list of tokens.
	 *
	 * @throws ParseException	If an unknown token is encountered.
	 */
	public Token[] tokenize(final String expr, final boolean stopAtEnd) throws ParseException {
		// Start tokenizing the given expression:
		/* (note: if the given expression is NULL, behave exactly as an empty
		 *        string) */
		Tokenizer tokenizer = grammarParser.getTokenizer((expr == null) ? "" : expr);

		// Iterate over all the tokens:
		try {
			ArrayList<Token> tokens = new ArrayList<Token>();
			Token token = tokenizer.nextToken();
			while(token != null && (!stopAtEnd || !grammarParser.isEnd(token))) {
				tokens.add(token);
				token = tokenizer.nextToken();
			}
			return tokens.toArray(new Token[tokens.size()]);
		} catch(TokenMgrError err) {
			// wrap such errors and propagate them:
			throw new ParseException(err);
		}
	}

	/* **********************************************************************
	   *                      CORRECTION SUGGESTION                         *
	   ********************************************************************** */

	/**
	* Try fixing tokens/terms of the input ADQL query.
	*
	* <p>
	* 	<b>This function does not try to fix syntactical or semantical errors.</b>
	* 	It just try to fix the most common issues in ADQL queries, such as:
	* </p>
	* <ul>
	* 	<li>some Unicode characters confusable with ASCII characters (like a
	* 		space, a dash, ...) ; this function replace them by their ASCII
	* 		alternative,</li>
	* 	<li>any of the following are double quoted:
	* 		<ul>
	* 			<li>non regular ADQL identifiers
	* 				(e.g. <code>_RAJ2000</code>),</li>
	* 			<li>ADQL function names used as identifiers
	* 				(e.g. <code>distance</code>)</li>
	* 			<li>and SQL reserved keywords
	* 				(e.g. <code>public</code>).</li>
	* 		</ul>
	* 	</li>
	* </ul>
	*
	* <p><i><b>Note 1:</b>
	* 	The given stream is NOT closed by this function even if the EOF is
	* 	reached. It is the responsibility of the caller to close it.
	* </i></p>
	*
	* <p><i><b>Note 2:</b>
	* 	This function does not use any instance variable of this parser
	* 	(especially the InputStream or Reader provided at initialisation or
	* 	ReInit).
	* </i></p>
	*
	* @param input	Stream containing the input ADQL query to fix.
	*
	* @return	The suggested correction of the input ADQL query.
	*
	* @throws java.io.IOException	If there is any error while reading from the
	*                            	given input stream.
	* @throws ParseException	If any unrecognised character is encountered,
	*                       	or if anything else prevented the tokenization
	*                       	of some characters/words/terms.
	*
	* @see QueryFixer#fix(String)
	*
	* @since 1.5
	*/
	public final String tryQuickFix(final InputStream input) throws IOException, ParseException {
		// Fetch everything into a single string:
		StringBuffer buf = new StringBuffer();
		byte[] cBuf = new byte[1024];
		int nbChar;
		while((nbChar = input.read(cBuf)) > -1) {
			buf.append(new String(cBuf, 0, nbChar));
		}

		// Convert the buffer into a String and now try to fix it:
		return quickFixer.fix(buf.toString());
	}

	/**
	* Try fixing tokens/terms of the given ADQL query.
	*
	* <p>
	* 	<b>This function does not try to fix syntactical or semantical errors.</b>
	* 	It just try to fix the most common issues in ADQL queries, such as:
	* </p>
	* <ul>
	* 	<li>some Unicode characters confusable with ASCII characters (like a
	* 		space, a dash, ...) ; this function replace them by their ASCII
	* 		alternative,</li>
	* 	<li>any of the following are double quoted:
	* 		<ul>
	* 			<li>non regular ADQL identifiers
	* 				(e.g. <code>_RAJ2000</code>),</li>
	* 			<li>ADQL function names used as identifiers
	* 				(e.g. <code>distance</code>)</li>
	* 			<li>and SQL reserved keywords
	* 				(e.g. <code>public</code>).</li>
	* 		</ul>
	* 	</li>
	* </ul>
	*
	* <p><i><b>Note:</b>
	* 	This function does not use any instance variable of this parser
	* 	(especially the InputStream or Reader provided at initialisation or
	* 	ReInit).
	* </i></p>
	*
	* @param adqlQuery	The input ADQL query to fix.
	*
	* @return	The suggested correction of the given ADQL query.
	*
	* @throws ParseException	If any unrecognised character is encountered,
	*                       	or if anything else prevented the tokenization
	*                       	of some characters/words/terms.
	*
	* @see QueryFixer#fix(String)
	*
	* @since 1.5
	*/
	public final String tryQuickFix(String adqlQuery) throws ParseException {
		return quickFixer.fix(adqlQuery);
	}

	/* **********************************************************************
	 *                           MAIN FUNCTION
	 * ********************************************************************** */

	/**
	 * Parse the given ADQL query.
	 *
	 * <h3>Usage</h3>
	 *
	 * <pre>adqlParser.jar [--version=...] [-h] [-d] [-v] [-e] [-a|-s] [-f] [&lt;FILE&gt;|&lt;URL&gt;]</pre>
	 *
	 * <p><i><b>Note:</b>
	 * 	If no file or URL is given, the ADQL query is expected in the standard
	 * 	input. This query must end with a ';' or <Ctrl+D>!
	 * </i></p>
	 *
	 * <h3>Parameters</h3>
	 *
	 * <ul>
	 * 	<li><b><code>--version=...</code>:</b>
	 * 		Set the version of the ADQL grammar to follow.
	 * 		It must be one among: <i>v2.0, v2.1 (default)</i>.</li>
	 * 	<li><b><code>-h</code> or <code>--help</code>:</b>
	 * 		Display this help.</li>
	 * 	<li><b><code>-v</code> or <code>--verbose</code>:</b>
	 * 		Print the main steps of the parsing.</li>
	 * 	<li><b><code>-d</code> or <code>--debug</code>:</b>
	 * 		Print stack traces when a grave error occurs.</li>
	 * 	<li><b><code>-e</code> or <code>--explain</code>:</b>
	 * 		Explain the ADQL parsing (or Expand the parsing tree).</li>
	 * 	<li><b><code>-a</code> or <code>--adql</code>:</b>
	 * 		Display the understood ADQL query.</li>
	 * 	<li><b><code>-s</code> or <code>--sql</code>:</b>
	 * 		Ask the SQL translation of the given ADQL query (SQL compatible with
	 * 		PostgreSQL).</li>
	 * 	<li><b><code>-f</code> or <code>--try-fix</code>:</b>
	 * 		Try fixing the most common ADQL query issues before attempting to
	 * 		parse the query.</li>
	 * </ul>
	 *
	 * <h3>Return</h3>
	 *
	 * <p>
	 * 	By default, nothing if the query is correct. Otherwise a message
	 * 	explaining why the query is not correct is displayed.
	 * </p>
	 *
	 * <p>
	 * 	With the <code>-s</code> option, the SQL translation of the given ADQL query will be
	 * 	returned.
	 * </p>
	 *
	 * <p>
	 * 	With the <code>-a</code> option, the ADQL query is returned as it has been
	 * 	understood.
	 * </p>
	 *
	 * <h3>Exit status</h3>
	 *
	 * <ul>
	 * 	<li><b><code>0</code>:</b>
	 * 		OK!</li>
	 * 	<li><b><code>1</code>:</b>
	 * 		Parameter error (missing or incorrect parameter)</li>
	 * 	<li><b><code>2</code>:</b>
	 * 		File error (incorrect file/url, reading error, ...)</li>
	 * 	<li><b><code>3</code>:</b>
	 * 		Parsing error (syntactic or semantic error)</li>
	 * 	<li><b><code>4</code>:</b>
	 * 		Translation error (a problem has occurred during the translation of
	 * 		the given ADQL query in SQL).</li>
	 * </ul>
	 *
	 * @param args	Program parameters.
	 *
	 * @throws Exception	If any unexpected error occurs.
	 */
	public static final void main(String[] args) throws Exception {
		final String USAGE = "Usage:\n    adqlParser.jar [--version=...] [-h] [-d] [-v] [-e] [-a|-s] [-f] [<FILE>|<URL>]\n\nNOTE: If no file or URL is given, the ADQL query is expected in the standard\n      input. This query must end with a ';' or <Ctrl+D>!\n\nParameters:\n    --version=...   : Set the version of the ADQL grammar to follow.\n                      It must be one among: " + getSupportedVersionsAsString() + "\n    -h or --help    : Display this help.\n    -v or --verbose : Print the main steps of the parsing\n    -d or --debug   : Print stack traces when a grave error occurs\n    -e or --explain : Explain the ADQL parsing (or Expand the parsing tree)\n    -a or --adql    : Display the understood ADQL query\n    -s or --sql     : Ask the SQL translation of the given ADQL query\n                      (SQL compatible with PostgreSQL)\n    -f or --try-fix : Try fixing the most common ADQL query issues before\n                      attempting to parse the query.\n\nReturn:\n    By default: nothing if the query is correct. Otherwise a message explaining\n                why the query is not correct is displayed.\n    With the -s option, the SQL translation of the given ADQL query will be\n    returned.\n    With the -a option, the ADQL query is returned as it has been understood.\n\nExit status:\n    0  OK !\n    1  Parameter error (missing or incorrect parameter)\n    2  File error (incorrect file/url, reading error, ...)\n    3  Parsing error (syntactic or semantic error)\n    4  Translation error (a problem has occurred during the translation of the\n       given ADQL query in SQL).";
		final String NEED_HELP_MSG = "Try -h or --help to get more help about the usage of this program.";
		final String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

		ADQLParser parser;

		short mode = -1;
		String file = null;
		ADQLVersion version = DEFAULT_VERSION;
		boolean verbose = false, debug = false, explain = false, tryFix = false;

		// Parameters reading:
		for(int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--version=")) {
				String[] parts = args[i].split("=");
				if (parts.length <= 1) {
					System.err.println("((!)) Missing ADQL version! It must be one among: " + getSupportedVersionsAsString() + ". ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				}
				version = ADQLVersion.parse(parts[1]);
				if (version == null) {
					System.err.println("((!)) Incorrect ADQL version: \"" + args[i].split("=")[1] + "\"! It must be one among: " + getSupportedVersionsAsString() + ". ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				}
			} else if (args[i].equalsIgnoreCase("-d") || args[i].equalsIgnoreCase("--debug"))
				debug = true;
			else if (args[i].equalsIgnoreCase("-v") || args[i].equalsIgnoreCase("--verbose"))
				verbose = true;
			else if (args[i].equalsIgnoreCase("-e") || args[i].equalsIgnoreCase("--explain"))
				explain = true;
			else if (args[i].equalsIgnoreCase("-a") || args[i].equalsIgnoreCase("--adql")) {
				if (mode != -1) {
					System.err.println("((!)) Too much parameter: you must choose between -s, -c, -a or nothing ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				} else
					mode = 1;
			} else if (args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--sql")) {
				if (mode != -1) {
					System.err.println("((!)) Too much parameter: you must choose between -s, -c, -a or nothing ((!))\n" + NEED_HELP_MSG);
					System.exit(1);
				} else
					mode = 2;
			} else if (args[i].equalsIgnoreCase("-f") || args[i].equalsIgnoreCase("--try-fix"))
				tryFix = true;
			else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--help")) {
				System.out.println(USAGE);
				System.exit(0);
			} else if (args[i].startsWith("-")) {
				System.err.println("((!)) Unknown parameter: \"" + args[i] + "\" ((!))\u005cn" + NEED_HELP_MSG);
				System.exit(1);
			} else
				file = args[i].trim();
		}

		try {

			// Get the parser for the specified ADQL version:
			parser = new ADQLParser(version);

			// Try fixing the query, if asked:
			InputStream in = null;
			if (tryFix) {
				if (verbose)
					System.out.println("((i)) Trying to automatically fix the query...");

				try {
					// get the input stream...
					if (file == null || file.length() == 0)
						in = System.in;
					else if (file.matches(urlRegex))
						in = (new java.net.URL(file)).openStream();
					else
						in = new java.io.FileInputStream(file);

					// ...and try fixing the query:
					String query = parser.tryQuickFix(in);

					if (verbose)
						System.out.println("((i)) SUGGESTED QUERY:\n" + query);

					// Initialize the parser with this fixed query:
					in = new java.io.ByteArrayInputStream(query.getBytes());
				} catch(ParseException pe) {
					System.out.println("((!)) Quick fix failure! Cause: " + pe.getMessage() + ".");
					if (debug)
						pe.printStackTrace();
					else
						System.out.println("      (run again with -d for more details)");
				} finally {
					// close the stream (if opened):
					if (in != null)
						in.close();
					in = null;
				}
			}

			// If no tryQuickFix (or if failed), take the query as provided:
			if (in == null) {
				// Initialise the parser with the specified input:
				if (file == null || file.length() == 0)
					in = System.in;
				else if (file.matches(urlRegex))
					in = (new java.net.URL(file)).openStream();
				else
					in = new java.io.FileInputStream(file);
			}

			// Enable/Disable the debugging in function of the parameters:
			parser.setDebug(explain);

			// Query parsing:
			try {
				if (verbose)
					System.out.print("((i)) Parsing ADQL query...");
				ADQLQuery q = parser.parseQuery(in);
				if (verbose)
					System.out.println("((i)) CORRECT ADQL QUERY ((i))");
				if (mode == 2) {
					PostgreSQLTranslator translator = new PostgreSQLTranslator();
					if (verbose)
						System.out.print("((i)) Translating in SQL...");
					String sql = translator.translate(q);
					if (verbose)
						System.out.println("ok");
					System.out.println(sql);
				} else if (mode == 1) {
					System.out.println(q.toADQL());
				}
			} catch(UnresolvedIdentifiersException uie) {
				System.err.println("((X)) " + uie.getNbErrors() + " unresolved identifiers:");
				for(ParseException pe : uie)
					System.err.println("\t - at " + pe.getPosition() + ": " + uie.getMessage());
				if (debug)
					uie.printStackTrace(System.err);
				System.exit(3);
			} catch(ParseException pe) {
				System.err.println("((X)) Syntax error: " + pe.getMessage() + " ((X))");
				if (debug)
					pe.printStackTrace(System.err);
				System.exit(3);
			} catch(TranslationException te) {
				if (verbose)
					System.out.println("error");
				System.err.println("((X)) Translation error: " + te.getMessage() + " ((X))");
				if (debug)
					te.printStackTrace(System.err);
				System.exit(4);
			}

		} catch(IOException ioe) {
			System.err.println("\n((X)) Error while reading the file \"" + file + "\": " + ioe.getMessage() + " ((X))");
			if (debug)
				ioe.printStackTrace(System.err);
			System.exit(2);
		}

	}

}
