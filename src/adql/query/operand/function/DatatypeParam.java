package adql.query.operand.function;

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

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.NullADQLIterator;
import adql.query.TextPosition;
import adql.query.operand.ADQLOperand;

/**
 * It represents a data-type parameter.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (04/2021)
 * @since 2.0
 *
 * @see CastFunction
 */
public class DatatypeParam implements ADQLOperand {

	public static enum DatatypeName {
		// Character
		CHAR(0, 1),
		VARCHAR(0, 1),
		// Exact numeric
		INTEGER,
		SMALLINT,
		BIGINT,
		// Approximate numeric
		REAL,
		DOUBLE_PRECISION,
		// Date, time
		TIMESTAMP;

		private final int nbMinRequiredParameters;
		private final int nbMaxRequiredParameters;

		/** Description of the ADQL Feature based on this type. */
		public final LanguageFeature FEATURE;

		private DatatypeName() {
			this(0);
		}

		private DatatypeName(int nbParams) {
			this(nbParams, nbParams);
		}

		private DatatypeName(int nbMinParams, int nbMaxParams) {
			nbMinRequiredParameters = nbMinParams;
			nbMaxRequiredParameters = nbMaxParams;
			FEATURE = new LanguageFeature(null, this.name(), false);
		}

		public final int nbMinParams() {
			return nbMinRequiredParameters;
		}

		public final int nbMaxParams() {
			return nbMaxRequiredParameters;
		}

		@Override
		public final String toString() {
			return (this == DOUBLE_PRECISION ? "DOUBLE PRECISION" : name());
		}

		public final static DatatypeName getDatatype(final String str) throws NullPointerException, IllegalArgumentException {
			if (str.equalsIgnoreCase("DOUBLE") || str.toUpperCase().matches("DOUBLE\\s+PRECISION"))
				return DOUBLE_PRECISION;
			else
				return DatatypeName.valueOf(str.trim().toUpperCase());
		}

		public final static DatatypeName[] getNumericDatatypes() {
			return new DatatypeName[]{ SMALLINT, INTEGER, BIGINT, REAL, DOUBLE_PRECISION };
		}

		public final static DatatypeName[] getStringDatatypes() {
			return new DatatypeName[]{ CHAR, VARCHAR, TIMESTAMP };
		}

		public final static DatatypeName[] getGeometricDatatypes() {
			return new DatatypeName[0];
		}
	}

	protected DatatypeName typeName;
	protected Integer typeLength;

	/** Number of given parameters. */
	private int nbParams;

	/** Position of this data-type parameter. */
	private TextPosition position = null;

	public DatatypeParam(final DatatypeName type) throws Exception {
		this(type, null);
	}

	public DatatypeParam(final DatatypeName type, final Integer size) throws Exception {
		if (type == null)
			throw new NullPointerException("Impossible to build a datatype without its type (e.g. VARCHAR, INTEGER, POINT, ...)!");
		this.typeName = type;

		// Compute the number of given parameters:
		nbParams = (size != null) ? 1 : 0;

		// Check it and throw immediately an exception if incorrect:
		if (nbParams < typeName.nbMinParams() || nbParams > typeName.nbMaxParams()) {
			if (typeName.nbMinParams() == typeName.nbMaxParams())
				throw new Exception("The datatype " + typeName.name() + " must have " + ((typeName.nbMaxParams() == 0) ? "no parameter!" : ("exactly " + typeName.nbMaxParams() + " parameters!")));
			else {
				switch(typeName.nbMaxParams()) {
					case 0:
						throw new Exception("The datatype " + type.name() + " must have no parameter!");
					case 1:
						throw new Exception("The datatype " + type.name() + " may have only one parameter!");
					default:
						throw new Exception("The datatype " + type.name() + " may have between " + typeName.nbMinRequiredParameters + " and " + typeName.nbMaxRequiredParameters + " parameters!");
				}
			}
		}

		this.typeLength = size;

		position = null;
	}

	@Override
	public boolean isNumeric() {
		if (typeName != null) {
			for(DatatypeName d : DatatypeName.getNumericDatatypes()) {
				if (d == this.typeName)
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean isString() {
		if (typeName != null) {
			for(DatatypeName d : DatatypeName.getStringDatatypes()) {
				if (d == this.typeName)
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean isGeometry() {
		if (typeName != null) {
			for(DatatypeName d : DatatypeName.getGeometricDatatypes()) {
				if (d == this.typeName)
					return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return typeName.name();
	}

	public DatatypeName getTypeName() {
		return typeName;
	}

	public Integer getTypeLength() {
		return typeLength;
	}

	@Override
	public LanguageFeature getFeatureDescription() {
		return typeName.FEATURE;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new DatatypeParam(typeName, typeLength);
	}

	@Override
	public TextPosition getPosition() {
		return position;
	}

	/**
	 * Sets the position at which this {@link DatatypeParam} has been found in
	 * the original ADQL query string.
	 *
	 * @param position	Position of this {@link DatatypeParam}.
	 */
	public void setPosition(final TextPosition newPosition) {
		position = newPosition;
	}

	@Override
	public String toADQL() {
		if (typeLength != null)
			return typeName.toString() + "(" + typeLength + ")";
		else
			return typeName.toString();
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new NullADQLIterator();
	}
}
