package adql.query;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

/**
 * Represents an ADQL clause (i.e. SELECT, FROM, WHERE, ...).
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2010
 */
public class ClauseADQL<T extends ADQLObject> extends ADQLList<T> {

	/**
	 * Builds an anonymous ClauseADQL.
	 */
	public ClauseADQL() {
		super((String)null);
	}

	/**
	 * Builds a ClauseADQL considering its name.
	 * 
	 * @param name	List label.
	 */
	public ClauseADQL(String name) {
		super(name);
	}

	/**
	 * Builds a ClauseADQL by copying the given one. It copies also all the list items of the given ClauseADQL.
	 * 
	 * @param toCopy		The ClauseADQL to copy.
	 * @throws Exception	If there is an unexpected error during the copy.
	 */
	public ClauseADQL(ADQLList<T> toCopy) throws Exception {
		super(toCopy);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ADQLObject getCopy() throws Exception {
		return new ClauseADQL(this);
	}

	/**
	 * Possible separators: only ",".
	 * 
	 * @see adql.query.ADQLList#getPossibleSeparators()
	 */
	@Override
	public String[] getPossibleSeparators() {
		return new String[]{","};
	}

	/**
	 * Returns always ",", except if the index is incorrect (index <= 0 or index >= size()).
	 * 
	 * @see adql.query.ADQLList#getSeparator(int)
	 */
	@Override
	public String getSeparator(int index) throws ArrayIndexOutOfBoundsException {
		if (index <= 0 || index > size())
			throw new ArrayIndexOutOfBoundsException("Impossible to get the separator between the item "+(index-1)+" and "+index+" !");
		else
			return ",";
	}

}
