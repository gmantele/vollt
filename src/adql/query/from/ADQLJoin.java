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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.db.SearchColumnList;
import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ClauseConstraints;
import adql.query.operand.ADQLColumn;

/**
 * Defines a join between two "tables".
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 08/2011
 */
public abstract class ADQLJoin implements ADQLObject, FromContent {

	/** The left "table" of this join. */
	private FromContent leftTable;

	/** The right "table" of this join. */
	private FromContent rightTable;

	/** Natural join (use of table keys) ? */
	protected boolean natural = false;

	/** The join condition. */
	protected ClauseConstraints condition = null;

	/** List of columns on which the join must be done. */
	protected ArrayList<ADQLColumn> lstColumns = null;


	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * Builds an {@link ADQLJoin} with at least two {@link FromContent} objects:
	 * the left and the right part of the join (usually two tables: T1 JOIN T2).
	 * 
	 * @param left		Left "table" of the join.
	 * @param right		Right "table" of the join.
	 */
	public ADQLJoin(FromContent left, FromContent right){
		leftTable = left;
		rightTable = right;
	}

	/**
	 * Builds an ADQL join by copying the given one.
	 * 
	 * @param toCopy		The ADQLJoin to copy.
	 * 
	 * @throws Exception	If there is an error during the copy.
	 */
	public ADQLJoin(ADQLJoin toCopy) throws Exception {
		leftTable = (FromContent)(toCopy.leftTable.getCopy());
		rightTable = (FromContent)(toCopy.rightTable.getCopy());
		natural = toCopy.natural;
		condition = (ClauseConstraints)(toCopy.condition.getCopy());
		if (toCopy.lstColumns != null){
			lstColumns = new ArrayList<ADQLColumn>(toCopy.lstColumns.size());
			for(ADQLColumn col : toCopy.lstColumns)
				lstColumns.add((ADQLColumn)col.getCopy());
		}
	}


	/* ***************** */
	/* GETTERS & SETTERS */
	/* ***************** */
	/**
	 * Indicates whether this join is natural or not.
	 * 
	 * @return <i>true</i> means this join is natural, <i>false</i> else.
	 */
	public final boolean isNatural() {
		return natural;
	}

	/**
	 * Lets indicate that this join is natural (it must use the table keys).
	 * 
	 * @param natural <i>true</i> means this join must be natural, <i>false</i> else.
	 */
	public void setNatural(boolean natural) {
		this.natural = natural;
		if (natural){
			condition = null;
			lstColumns = null;
		}
	}

	/**
	 * Gets the left "table" of this join.
	 * 
	 * @return The left part of the join.
	 */
	public final FromContent getLeftTable() {
		return leftTable;
	}

	/**
	 * Sets the left "table" of this join.
	 * 
	 * @param table The left part of the join.
	 */
	public void setLeftTable(FromContent table) {
		leftTable = table;
	}

	/**
	 * Gets the right "table" of this join.
	 * 
	 * @return	The right part of the join.
	 */
	public final FromContent getRightTable(){
		return rightTable;
	}

	/**
	 * Sets the right "table" of this join.
	 * 
	 * @param table The right part of the join.
	 */
	public void setRightTable(FromContent table) {
		rightTable = table;
	}

	/**
	 * Gets the condition of this join (that's to say: the condition which follows the keyword ON).
	 * 
	 * @return The join condition.
	 */
	public final ClauseConstraints getJoinCondition() {
		return condition;
	}

	/**
	 * Sets the condition of this join (that's to say: the condition which follows the keyword ON).
	 * 
	 * @param cond	The join condition (condition following ON).
	 */
	public void setJoinCondition(ClauseConstraints cond) {
		condition = cond;
		if (condition != null){
			natural = false;
			lstColumns = null;
		}
	}

	/**
	 * Gets the list of all columns on which the join is done (that's to say: the list of columns given with the keyword USING).
	 * 
	 * @return	The joined columns (columns listed in USING(...)).
	 */
	public final Iterator<ADQLColumn> getJoinedColumns(){
		if (lstColumns == null){
			return new Iterator<ADQLColumn>() {
				public boolean hasNext() { return false; }
				public ADQLColumn next() { return null; }
				public void remove() { ;  }
			};
		}else
			return lstColumns.iterator();
	}

	/**
	 * Tells whether this join has a list of columns to join.
	 * 
	 * @return	<i>true</i> if some columns must be explicitly joined, <i>false</i> otherwise.
	 */
	public final boolean hasJoinedColumns(){
		return (lstColumns != null);
	}

	/**
	 * Sets the list of all columns on which the join is done (that's to say: the list of columns given with the keyword USING).
	 * 
	 * @param columns	The joined columns.
	 */
	public void setJoinedColumns(Collection<ADQLColumn> columns){
		if (columns != null && !columns.isEmpty()){
			if (lstColumns == null)
				lstColumns = new ArrayList<ADQLColumn>(columns.size());
			else
				lstColumns.clear();
			lstColumns.addAll(columns);

			natural = false;
			condition = null;
		}
	}


	/* ***************** */
	/* INHERITED METHODS */
	/* ***************** */
	public String getName() {
		return getJoinType();
	}

	public ADQLIterator adqlIterator(){
		return new ADQLIterator() {

			private int index = -1;
			private final int nbItems = 2+((condition==null)?0:1)+((lstColumns==null)?0:lstColumns.size());
			private final int offset = 2+((condition==null)?0:1);
			private Iterator<ADQLColumn> itCol = null;

			public ADQLObject next() {
				index++;

				if (index == 0)
					return leftTable;
				else if (index == 1)
					return rightTable;
				else if (index == 2 && condition != null)
					return condition;
				else if (lstColumns != null && !lstColumns.isEmpty()){
					if (itCol == null) itCol = lstColumns.iterator();
					return itCol.next();
				}else
					throw new NoSuchElementException();
			}

			public boolean hasNext() {
				return (itCol != null && itCol.hasNext()) || index+1 < nbItems;
			}

			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else if (index == 0){
					if (replacer instanceof FromContent)
						leftTable = (FromContent)replacer;
					else
						throw new UnsupportedOperationException("Impossible to replace the left \"table\" of the join ("+leftTable.toADQL()+") by a "+replacer.getClass().getName()+" ("+replacer.toADQL()+") ! The replacer must be a FromContent instance.");
				}else if (index == 1){
					if (replacer instanceof FromContent)
						rightTable = (FromContent)replacer;
					else
						throw new UnsupportedOperationException("Impossible to replace the right \"table\" of the join ("+rightTable.toADQL()+") by a "+replacer.getClass().getName()+" ("+replacer.toADQL()+") ! The replacer must be a FromContent instance.");
				}else if (index == 2 && itCol == null){
					if (replacer instanceof ClauseConstraints)
						condition = (ClauseConstraints)replacer;
					else
						throw new UnsupportedOperationException("Impossible to replace an ADQLConstraint ("+condition+") by a "+replacer.getClass().getName()+" ("+replacer.toADQL()+") !");
				}else if (itCol != null){
					if (replacer instanceof ADQLColumn)
						lstColumns.set(index-offset, (ADQLColumn)replacer);
					else
						throw new UnsupportedOperationException("Impossible to replace an ADQLColumn by a "+replacer.getClass().getName()+" ("+replacer.toADQL()+") !");
				}

			}

			public void remove() {
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");
				else if (index == 0)
					throw new UnsupportedOperationException("Impossible to remove the left \"table\" of the join ("+leftTable.toADQL()+") !");
				else if (index == 1)
					throw new UnsupportedOperationException("Impossible to remove the right \"table\" of the join ("+rightTable.toADQL()+") !");
				else if (index == 2 && itCol == null)
					throw new UnsupportedOperationException("Impossible to remove a condition ("+condition.toADQL()+") from a join ("+toADQL()+") !");
				else if (itCol != null){
					itCol.remove();
					index--;
				}
			}
		};
	}

	public String toADQL(){
		StringBuffer adql = new StringBuffer(leftTable.toADQL());

		adql.append(natural?" NATURAL ":" ").append(getJoinType()).append(' ').append(rightTable.toADQL());

		if (condition != null)
			adql.append(" ON ").append(condition.toADQL());
		else if (lstColumns != null) {
			String cols = null;
			for(ADQLColumn item : lstColumns){
				cols = (cols==null)?("\""+item.toADQL()+"\""):(cols+", \""+item.toADQL()+"\"");
			}
			adql.append(" USING (").append(cols).append(')');
		}

		return adql.toString();
	}

	public SearchColumnList getDBColumns(){
		SearchColumnList list = new SearchColumnList();

		SearchColumnList leftList = leftTable.getDBColumns(), rightList = rightTable.getDBColumns();
		list.addAll(leftList);
		list.addAll(rightList);

		if (natural){
			SearchColumnList doublons = new SearchColumnList(leftList);
			doublons.retainAll(rightList);
			for(DBColumn dbCol : doublons)
				list.add(dbCol.copy(dbCol.getDBName(), dbCol.getADQLName(), null));	// makes a copy which keeps the same DB and ADQL name but set to NULL the dbTable
		}else if (lstColumns != null){
			for(ADQLColumn col : lstColumns){
				ArrayList<DBColumn> dbCols = list.search(col.getCatalogName(), col.getSchemaName(), col.getTableName(), col.getColumnName(), col.getCaseSensitive());
				if (dbCols.size() > 0){
					DBColumn match = dbCols.get(0);
					list.add(match.copy(match.getDBName(), match.getADQLName(), null)); // makes a copy which keeps the same DB and ADQL name but set to NULL the dbTable
				}
			}

		}

		return list;
	}

	public ArrayList<ADQLTable> getTables(){
		ArrayList<ADQLTable> tables = leftTable.getTables();
		tables.addAll(rightTable.getTables());
		return tables;
	}

	public ArrayList<ADQLTable> getTablesByAlias(final String alias, final boolean caseSensitive){
		ArrayList<ADQLTable> tables = leftTable.getTablesByAlias(alias, caseSensitive);
		tables.addAll(rightTable.getTablesByAlias(alias, caseSensitive));
		return tables;
	}


	/* **************** */
	/* ABSTRACT METHODS */
	/* **************** */
	/**
	 * Gets the type of this join.
	 * 
	 * @return Its join type (i.e. CROSS JOIN, LEFT JOIN, LEFT OUTER JOIN, ...).
	 */
	public abstract String getJoinType();

	public abstract ADQLObject getCopy() throws Exception;

}