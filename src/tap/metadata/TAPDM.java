package tap.metadata;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2014 - Astronomisches Rechen Institute (ARI)
 */

/**
 * Enumeration of all schemas and tables of the TAP datamodel (and particularly of TAP_SCHEMA).
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.0 (07/2014)
 * @since 2.0
 */
public enum TAPDM{
	TAPSCHEMA("TAP_SCHEMA"), SCHEMAS("schemas"), TABLES("tables"), COLUMNS("columns"), FOREIGN_KEYS("foreign_keys"), UPLOADSCHEMA("TAP_UPLOAD");

	/** Real name of the schema/table. */
	private final String label;

	private TAPDM(final String name){
		this.label = name;
	}

	/**
	 * Get the real name of the schema/table of the TAP datamodel.
	 * 
	 * @return	Real name of the schema/table.
	 */
	public String getLabel(){
		return label;
	}

	@Override
	public String toString(){
		return label;
	}
}
