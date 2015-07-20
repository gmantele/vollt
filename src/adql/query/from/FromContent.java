package adql.query.from;

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
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;

import adql.db.DBColumn;
import adql.db.SearchColumnList;
import adql.db.exception.UnresolvedJoinException;
import adql.query.ADQLObject;
import adql.query.TextPosition;

/**
 * Represents the content of the whole or a part of the clause FROM.
 * It could be either a table ({@link ADQLTable}) or a join ({@link ADQLJoin}).
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public interface FromContent extends ADQLObject {

	/**
	 * <p>Gets the list of all columns (~ database metadata) available in this FROM part.</p>
	 * 
	 * <p><i><u>Note:</u> In the most cases, this list is generated on the fly !</i></p>
	 * 
	 * @return	All the available {@link DBColumn}s.
	 * @throws UnresolvedJoinException If a join is not possible.
	 */
	public SearchColumnList getDBColumns() throws UnresolvedJoinException;

	/**
	 * Gets all {@link ADQLTable} instances contained in this FROM part (itself included, if it is an {@link ADQLTable}).
	 * 
	 * @return	The list of all {@link ADQLTable}s found.
	 */
	public ArrayList<ADQLTable> getTables();

	/**
	 * <p>Gets all the table whose the alias is equals to the given one.</p>
	 * <p><i>
	 * 	<u>Note:</u> Theoretically, only one table may be returned. But, since this object may be generated without the parser,
	 * 	it is possible that several {@link ADQLTable} objects exits with the same alias (particularly if there are JOIN).
	 * </i></p>
	 * 
	 * @param alias			Alias of the table(s) to get.
	 * @param caseSensitive	<i>true</i> if the research must be made with case-sensitivity, <i>false</i> otherwise.
	 * 
	 * @return	The list of all tables found.
	 */
	public ArrayList<ADQLTable> getTablesByAlias(final String alias, final boolean caseSensitive);

	/**
	 * Set the position of this {@link FromContent} in the given ADQL query string.
	 * 
	 * @param position	New position of this {@link FromContent}.
	 * @since 1.4
	 */
	public void setPosition(final TextPosition position);

}
