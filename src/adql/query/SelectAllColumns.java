package adql.query;

import java.util.NoSuchElementException;

import adql.query.from.ADQLTable;

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
 * In ADQL it corresponds to the '*' and '{tableName}.*' items in the SELECT clause.
 * It means: 'select all columns'.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 */
public final class SelectAllColumns extends SelectItem {

	/** Query in which all columns of all selected tables are selected. */
	private ADQLQuery query = null;

	/** The table whose all columns must be selected. */
	private ADQLTable adqlTable = null;


	/**
	 * Builds a {@link SelectItem} which selects all columns available in the given ADQL query.
	 * 
	 * @param query	The query whose all available columns must be selected.
	 */
	public SelectAllColumns(final ADQLQuery query) {
		super(null, null);
		this.query = query;
	}

	/**
	 * Builds a {@link SelectItem} which selects all columns available in the given table.
	 * 
	 * @param table	The table whose all available columns must be selected.
	 */
	public SelectAllColumns(final ADQLTable table) {
		super(null, null);
		adqlTable = table;
	}

	/**
	 * Builds a {@link SelectAllColumns} by copying the given one.
	 * 
	 * @param toCopy		The {@link SelectAllColumns} to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	public SelectAllColumns(SelectAllColumns toCopy) throws Exception {
		super(toCopy);
	}

	/**
	 * Gets the query whose all available columns must be selected.
	 * 
	 * @return	The ADQL query whose all available columns must be selected,
	 * 			or <i>null</i> if the selection does not concern an {@link ADQLQuery} but an {@link ADQLTable}.
	 */
	public final ADQLQuery getQuery(){
		return query;
	}

	/**
	 * Sets the query whose all available columns must be selected.
	 * 
	 * @param query	An {@link ADQLQuery} (MUST NOT BE NULL).
	 */
	public final void setQuery(final ADQLQuery query){
		if (query != null){
			this.query = query;
			adqlTable = null;
		}
	}

	/**
	 * Gets the table whose all columns must be selected.
	 * 
	 * @return	The ADQL table whose all columns must be selected,
	 * 			or <i>null</i> if the selection does not concern an {@link ADQLTable} but an {@link ADQLQuery}.
	 */
	public final ADQLTable getAdqlTable(){
		return adqlTable;
	}

	/**
	 * Sets the table whose all columns must be selected.
	 * 
	 * @param table	An {@link ADQLTable} (MUST NOT BE NULL).
	 */
	public final void setAdqlTable(final ADQLTable table) {
		if (table == null){
			adqlTable = table;
			query = null;
		}
	}

	@Override
	public final ADQLObject getCopy() throws Exception {
		return new SelectAllColumns(this);
	}

	@Override
	public final String getName() {
		return "*";
	}

	@Override
	public final ADQLIterator adqlIterator(){
		return new ADQLIterator() {

			private boolean tableGot = (adqlTable == null);

			public ADQLObject next() throws NoSuchElementException {
				if (tableGot)
					throw new NoSuchElementException();
				tableGot = true;
				return adqlTable;
			}

			public boolean hasNext() {
				return !tableGot;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (replacer == null)
					remove();
				else if (!tableGot)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");
				else if (!(replacer instanceof ADQLTable))
					throw new IllegalStateException("Impossible to replace an ADQLTable by a "+replacer.getClass().getName()+" !");
				else
					adqlTable = (ADQLTable)replacer;
			}

			public void remove() {
				if (!tableGot)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else
					throw new UnsupportedOperationException("Impossible to remove the only operand ("+adqlTable.toADQL()+") from a SelectItem ("+toADQL()+") !");
			}
		};
	}

	@Override
	public final String toADQL() {
		if (adqlTable != null) {
			if (adqlTable.hasAlias())
				return (adqlTable.isCaseSensitive(IdentifierField.ALIAS) ? ("\""+adqlTable.getAlias()+"\"") : adqlTable.getAlias())+".*";
			else
				return adqlTable.getFullTableName()+".*";
		}else
			return "*";
	}



}
