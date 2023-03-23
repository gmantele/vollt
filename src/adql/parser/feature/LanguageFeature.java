package adql.parser.feature;

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
 * Copyright 2019-2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Objects;

import adql.db.FunctionDef;

/**
 * Description of an ADQL's language feature.
 *
 * <p>
 * 	All {@link adql.query.ADQLObject ADQLObject}s MUST provide an instance of
 * 	this class, even if not optional.
 * </p>
 *
 * <p>
 * 	A {@link LanguageFeature} is indeed particularly useful to identify optional
 * 	ADQL features (e.g. <code>LOWER</code>, <code>WITH</code>). This is the role
 * 	of the {@link adql.parser.ADQLParser ADQLParser} to generate an error if an
 * 	optional feature is used in a query while declared as unsupported.
 * </p>
 *
 * <p><i><b>Note:</b>
 * 	Most of ADQL objects are not associated with any IVOA standard (e.g.
 * 	TAPRegExt) apart from ADQL. In such case, the attribute {@link #type} is
 * 	set to NULL.
 * </i></p>
 *
 * <p><i><b>IMPORTANT note about UDF:</b>
 * 	To create a UDF feature (i.e. a {@link LanguageFeature} with the type
 * 	{@link #TYPE_UDF}), ONLY ONE constructor can be used:
 * 	{@link #LanguageFeature(FunctionDef, String)}. Any attempt with another
 * 	public constructor will fail.
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (10/2022)
 * @since 2.0
 *
 * @see FeatureSet
 */
public final class LanguageFeature {

	/** Unique identifier of this language feature.
	 * <p><i><b>MANDATORY</b></i></p>
	 * <p>This identifier should follow this syntax:</p>
	 * <pre>        {@link #type TYPE}'!'{@link #form FORM}</pre>
	 * <p><b>Examples:</b></p>
	 * <ul>
	 * 	<li><code>!SELECT</code> <em>(no type specified for this in the ADQL
	 * 		standard, so <code>TYPE=''</code>)</li>
	 * 	<li><code>ivo://ivoa.net/std/TAPRegExt#features-adql-string!LOWER</code></li>
	 * 	<li><code>ivo://ivoa.net/std/TAPRegExt#features-udf!MINE(VARCHAR) ->
	 * 		DOUBLE</code></li>
	 * </ul> */
	public final String id;

	/** Type of this language feature.
	 * <p><i><b>OPTIONAL</b></i></p>
	 * <p>
	 * 	All types mentioned in the ADQL standard are listed as public static
	 * 	final attributes of this class ; they all start with <code>TYPE_</code>
	 * 	(ex: {@link #TYPE_ADQL_STRING}).
	 * </p>
	 * <p>
	 * 	If no type is specified for this language feature in the ADQL standard,
	 * 	set this field to <code>null</code>.
	 * </p>
	 * <p><b>Examples:</b></p>
	 * <ul>
	 * 	<li><em><code>null</code> for <code>SELECT</code> (no type specified for
	 * 		this in the ADQL standard)</em></li>
	 * 	<li><code>ivo://ivoa.net/std/TAPRegExt#features-adql-string</code>
	 * 		<em>for <code>LOWER</code></em></li>
	 * 	<li><code>ivo://ivoa.net/std/TAPRegExt#features-udf</code>
	 * 		<em>for <code>the UDF <code>MINE(VARCHAR) -> DOUBLE</code></em></li>
	 * </ul> */
	public final String type;

	/** Name (or function signature).
	 * <p><i><b>MANDATORY</b></i></p>
	 * <p><b>Examples:</b></p>
	 * <ul>
	 * 	<li><code>SELECT</code></li>
	 * 	<li><code>LOWER</code></li>
	 * 	<li><code>MINE(VARCHAR) -> DOUBLE</code></li>
	 * </ul> */
	public final String form;

	/** Definition of the UDF represented by this {@link LanguageFeature}.
	 * <p><i><b>OPTIONAL</b></i></p> */
	public final FunctionDef udfDefinition;

	/** Is this feature optional in the ADQL grammar?
	 * <p><i><b>MANDATORY</b></i></p>
	 * <p>
	 * 	An optional language feature can be used in an ADQL query only if it
	 * 	is declared as supported by the ADQL client (i.e. TAP service). To do,
	 * 	one should use {@link FeatureSet} to declare how supported is an
	 * 	optional feature.
	 * </p>
	 * <p><b>Examples:</b></p>
	 * <ul>
	 * 	<li><code>false</code> <em>for <code>SELECT</code></em></li>
	 * 	<li><code>true</code> <em>for <code>LOWER</code></em></li>
	 * 	<li><code>true</code> <em>for <code>MINE(VARCHAR) -> DOUBLE</code></em></li>
	 * </ul> */
	public final boolean optional;

	/** Description of this feature.
	 * <p><i><b>OPTIONAL</b></i></p> */
	public String description;

	/**
	 * Create a <em>de-facto supported</em> (i.e. non-optional) language
	 * feature.
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	To create a UDF feature, DO NOT use this constructor.
	 * 	You MUST use instead {@link #LanguageFeature(FunctionDef, String)}.
	 * </i></p>
	 *
	 * @param type			[OPTIONAL] Category of the language feature.
	 *            			<em>(see all static attributes starting with
	 *            			<code>TYPE_</code>)</em>
	 * @param form			[REQUIRED] Name (or function signature) of the
	 *            			language feature.
	 *
	 * @throws NullPointerException	If the given form is missing.
	 */
	public LanguageFeature(final String type, final String form) throws NullPointerException {
		this(type, form, false, null);
	}

	/**
	 * Create a language feature.
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	To create a UDF feature, DO NOT use this constructor.
	 * 	You MUST use instead {@link #LanguageFeature(FunctionDef, String)}.
	 * </i></p>
	 *
	 * @param type			[OPTIONAL] Category of the language feature.
	 *            			<em>(see all static attributes starting with
	 *            			<code>TYPE_</code>)</em>
	 * @param form			[REQUIRED] Name (or function signature) of the
	 *            			language feature.
	 * @param optional		[REQUIRED] <code>true</code> if the feature is by
	 *                		default supported in the ADQL standard,
	 *                		<code>false</code> if the ADQL client must declare
	 *                		it as supported in order to use it.
	 *
	 * @throws NullPointerException	If the given form is missing.
	 */
	public LanguageFeature(final String type, final String form, final boolean optional) throws NullPointerException {
		this(type, form, optional, null);
	}

	/**
	 * Create a language feature.
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	To create a UDF feature, DO NOT use this constructor.
	 * 	You MUST use instead {@link #LanguageFeature(FunctionDef, String)}.
	 * </i></p>
	 *
	 * @param type			[OPTIONAL] Category of the language feature.
	 *            			<em>(see all static attributes starting with
	 *            			<code>TYPE_</code>)</em>
	 * @param form			[REQUIRED] Name (or function signature) of the
	 *            			language feature.
	 * @param optional		[REQUIRED] <code>true</code> if the feature is by
	 *                		default supported in the ADQL standard,
	 *                		<code>false</code> if the ADQL client must declare
	 *                		it as supported in order to use it.
	 * @param description	[OPTIONAL] Description of this feature.
	 *
	 * @throws NullPointerException	If given form is missing.
	 */
	public LanguageFeature(final String type, final String form, final boolean optional, final String description) throws NullPointerException {
		this(type, form, null, optional, description);
	}

	/**
	 * Create a UDF feature.
	 *
	 * @param udfDef		[REQUIRED] Detailed definition of the UDF feature.
	 *
	 * @throws NullPointerException	If given {@link FunctionDef} is missing.
	 */
	public LanguageFeature(final FunctionDef udfDef) throws NullPointerException {
		this(udfDef, null);
	}

	/**
	 * Create a UDF feature.
	 *
	 * @param udfDef		[REQUIRED] Detailed definition of the UDF feature.
	 * @param description	[OPTIONAL] Description overwriting the description
	 *                   	provided in the given {@link FunctionDef}.
	 *                   	<em>If NULL, the description of the
	 *                   	{@link FunctionDef} will be used. If empty string,
	 *                   	no description will be set.</em>
	 *
	 * @throws NullPointerException	If given {@link FunctionDef} is missing.
	 */
	public LanguageFeature(final FunctionDef udfDef, final String description) throws NullPointerException {
		this(LanguageFeature.TYPE_UDF, udfDef.toString(), udfDef, true, (description == null ? udfDef.description : (description.trim().isEmpty() ? null : description)));
	}

	/**
	 * Create a language feature.
	 *
	 * <p><i><b>IMPORTANT note:</b>
	 * 	To create a UDF feature, the parameter udfDef MUST be NON-NULL.
	 * </i></p>
	 *
	 * @param type			[OPTIONAL] Category of the language feature.
	 *            			<em>(see all static attributes starting with
	 *            			<code>TYPE_</code>)</em>
	 * @param form			[REQUIRED] Name (or function signature) of the
	 *            			language feature.
	 * @param udfDef		[REQUIRED if type=UDF] Detailed definition of the
	 *              		UDF feature.
	 * @param optional		[REQUIRED] <code>true</code> if the feature is by
	 *                		default supported in the ADQL standard,
	 *                		<code>false</code> if the ADQL client must declare
	 *                		it as supported in order to use it.
	 * @param description	[OPTIONAL] Description of this feature.
	 *
	 * @throws NullPointerException	If given form or udfDef is missing.
	 */
	private LanguageFeature(final String type, final String form, final FunctionDef udfDef, final boolean optional, final String description) throws NullPointerException {
		this.type = (type == null || type.trim().isEmpty()) ? null : type.trim();

		if (form == null || form.trim().isEmpty())
			throw new NullPointerException("Missing form/name of the language feature to create!");
		this.form = form.trim();

		if (TYPE_UDF.equals(this.type) && udfDef == null)
			throw new NullPointerException("Missing UDF definition! To declare a UDF feature, you MUST use the constructor LanguageFeature(FunctionDef, ...) with a non-NULL FunctionDef instance.");
		this.udfDefinition = udfDef;

		this.id = (this.type == null ? "" : this.type) + "!" + this.form;

		this.optional = optional;

		this.description = (description == null || description.trim().isEmpty()) ? null : description.trim();
	}

	@Override
	public boolean equals(final Object obj) {
		if ((obj != null) && (obj instanceof LanguageFeature)) {
			// If UDF, equals IF SAME NAME and SAME NB PARAMETERS:
			if (TYPE_UDF.equals(type) && type.equals(((LanguageFeature)obj).type)) {
				FunctionDef udfDefinition2 = ((LanguageFeature)obj).udfDefinition;
				return udfDefinition.name.equalsIgnoreCase(udfDefinition2.name) && (udfDefinition.nbParams == udfDefinition2.nbParams);
			}
			// Otherwise, equals IF SAME ID:
			else if (id.equals(((LanguageFeature)obj).id))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (udfDefinition != null)
			return Objects.hash(type, udfDefinition.name.toLowerCase(), udfDefinition.nbParams);
		else
			return Objects.hash(type, form, -1);
	}

	@Override
	public String toString() {
		return id;
	}

	/* **********************************************************************
	   *                                                                    *
	   *              ALL KNOWN TYPES OF LANGUAGE FEATURE                   *
	   *                                                                    *
	   ********************************************************************** */

	/** Root IVOID for all the TAPRegExt's language features. */
	public final static String IVOID_TAP_REGEXT = "ivo://ivoa.net/std/TAPRegExt";

	/** User Defined Functions. */
	public final static String TYPE_UDF = IVOID_TAP_REGEXT + "#features-udf";

	/** Geometric functions/regions. */
	public final static String TYPE_ADQL_GEO = IVOID_TAP_REGEXT + "#features-adqlgeo";

	/** String manipulation functions */
	public final static String TYPE_ADQL_STRING = IVOID_TAP_REGEXT + "#features-adql-string";

	/** Row-set manipulation functions. */
	public final static String TYPE_ADQL_SETS = IVOID_TAP_REGEXT + "#features-adql-sets";

	/** Sub-query "alias" (i.e. <code>WITH</code>). */
	public final static String TYPE_ADQL_COMMON_TABLE = IVOID_TAP_REGEXT + "#features-adql-common-table";

	/** Datatype manipulation functions (e.g. <code>CAST</code>). */
	public final static String TYPE_ADQL_TYPE = IVOID_TAP_REGEXT + "#features-adql-type";

	/** Conditional functions (e.g. <code>COALESCE</code>). */
	public final static String TYPE_ADQL_CONDITIONAL = IVOID_TAP_REGEXT + "#features-adql-conditional";

	/** Unit manipulation functions (e.g. <code>IN_UNIT</code>). */
	public final static String TYPE_ADQL_UNIT = IVOID_TAP_REGEXT + "#features-adql-unit";

	/** Bit manipulation functions. */
	public final static String TYPE_ADQL_BITWISE = IVOID_TAP_REGEXT + "#features-adql-bitwise";

	/** Query result offset. */
	public final static String TYPE_ADQL_OFFSET = IVOID_TAP_REGEXT + "#features-adql-offset";

}
