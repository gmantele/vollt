package adql.db.region;

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
 * Copyright 2021 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.grammar.ParseException;

/**
 * This class helps dealing with the subset of the DALI representation for
 * geometric regions described by the sections 3.3.5, 3.3.6 and 3.3.7 of the
 * "Data Access Layer Interface 1.1" document.
 *
 * <p><i><b>Note:</b>
 * 	No instance of this class can be created. Its usage is only limited to its
 * 	static functions and classes.
 * </i></p>
 *
 * <h3>Parsing a DALI string</h3>
 * <p>
 * 	A string serialization of a region following the DALI syntax can be parsed
 * 	thanks to {@link #parseRegion(String)}. If the given string can not be
 * 	parsed, a {@link ParseException} is thrown. Otherwise a {@link Region} is
 * 	returned.
 * </p>
 *
 * <h3>Region serialization</h3>
 * <p>
 * 	A geometric region can be serialized into a DALI representation with
 * 	{@link #toDALI(Region)}. The shortcut function {@link Region#toDALI()} can
 * 	also be used.
 * </p>
 *
 * <h3>Supported object types</h3>
 * <p>
 * 	According to the "Data Access Layer Interface 1.1" document, only the
 * 	following object types are supported:
 * </p>
 * </ul>
 * 	<li>POINT</li>
 * 	<li>CIRCLE</li>
 * 	<li>POLYGON</li>
 * </ul>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (03/2021)
 * @since 2.0
 */
public final class DALI {

	private DALI() {
	}

	/**
	 * Parse the given DALI expression representing a geometric region.
	 *
	 * @param daliRegion	DALI expression of a region.
	 *                  	<i>Note: MUST be different from NULL.</i>
	 *
	 * @return	The object representation of the specified geometric region.
	 *
	 * @throws ParseException	If the given expression is NULL, empty string
	 *                       	or if the DALI syntax is wrong.
	 */
	public static Region parseRegion(final String daliRegion) throws ParseException {
		if (daliRegion == null || daliRegion.trim().length() == 0)
			throw new ParseException("Missing DALI expression to parse!");

		final String[] values = daliRegion.trim().split("\\s+");
		Region region = null;
		try {
			switch(values.length) {
				// CASE: not a region
				case 0:
				case 1:
					throw new ParseException("Incorrect DALI region!");

					// CASE: POINT
				case 2:
					region = new Region(null, new double[]{ Double.parseDouble(values[0]), Double.parseDouble(values[1]) });
					break;

				// CASE: CIRCLE
				case 3:
					region = new Region(null, new double[]{ Double.parseDouble(values[0]), Double.parseDouble(values[1]) }, Double.parseDouble(values[2]));

					break;

				// CASE: POLYGON
				default:
					if (values.length >= 6 && values.length % 2 == 0) {
						double[][] vertices = new double[values.length / 2][2];
						for(int i = 0; i + 1 < values.length; i += 2) {
							vertices[i / 2] = new double[]{ Double.parseDouble(values[i]), Double.parseDouble(values[i + 1]) };
						}
						region = new Region(null, vertices);
					}
			}
		} catch(NumberFormatException nfe) {
		}

		if (region == null)
			throw new ParseException("Incorrect DALI region!");

		return region;
	}

	/**
	 * Convert into DALIT the given object representation of a geometric region.
	 *
	 * @param region	Region to convert into DALI.
	 *
	 * @return	The corresponding DALI expression.
	 */
	public static String toDALI(final Region region) {
		// CASE: nothing => NULL
		if (region == null)
			return null;

		switch(region.type) {

			// CASE: POINT/POSITION
			case POSITION:
				return region.coordinates[0][0] + " " + region.coordinates[0][1];

			// CASE: CIRCLE
			case CIRCLE:
				return region.coordinates[0][0] + " " + region.coordinates[0][1] + " " + region.radius;

			// CASE: POLYGON
			case POLYGON:
				StringBuffer buf = new StringBuffer();
				for(int i = 0; i < region.coordinates.length; i++) {
					if (i > 0)
						buf.append(' ');
					buf.append(region.coordinates[i][0]).append(' ').append(region.coordinates[i][1]);
				}
				return buf.toString();

			// CASE: anything else is not supported => NULL
			default:
				return null;
		}
	}

}
