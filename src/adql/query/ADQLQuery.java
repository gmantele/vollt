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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.db.DefaultDBColumn;
import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.search.ISearchHandler;

/**
 * <p>Object representation of an ADQL query or sub-query.</p>
 * <p>The resulting object of the {@link ADQLParser} is an object of this class.</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.2 (09/2014)
 */
public class ADQLQuery implements ADQLObject {

	/** The ADQL clause SELECT. */
	private ClauseSelect select;

	/** The ADQL clause FROM. */
	private FromContent from;

	/** The ADQL clause WHERE. */
	private ClauseConstraints where;

	/** The ADQL clause GROUP BY. */
	private ClauseADQL<ADQLOperand> groupBy;

	/** The ADQL clause HAVING. */
	private ClauseConstraints having;

	/** The ADQL clause ORDER BY. */
	private ClauseADQL<ADQLOrder> orderBy;

	/**
	 * Builds an empty ADQL query.
	 */
	public ADQLQuery(){
		select = new ClauseSelect();
		from = null;
		where = new ClauseConstraints("WHERE");
		groupBy = new ClauseADQL<ADQLOperand>("GROUP BY");
		having = new ClauseConstraints("HAVING");
		orderBy = new ClauseADQL<ADQLOrder>("ORDER BY");
	}

	/**
	 * Builds an ADQL query by copying the given one.
	 * 
	 * @param toCopy		The ADQL query to copy.
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ADQLQuery(ADQLQuery toCopy) throws Exception{
		select = (ClauseSelect)toCopy.select.getCopy();
		from = (FromContent)toCopy.from.getCopy();
		where = (ClauseConstraints)toCopy.where.getCopy();
		groupBy = (ClauseADQL<ADQLOperand>)toCopy.groupBy.getCopy();
		having = (ClauseConstraints)toCopy.having.getCopy();
		orderBy = (ClauseADQL<ADQLOrder>)toCopy.orderBy.getCopy();
	}

	/**
	 * Clear all the clauses.
	 */
	public void reset(){
		select.clear();
		select.setDistinctColumns(false);
		select.setNoLimit();

		from = null;
		where.clear();
		groupBy.clear();
		having.clear();
		orderBy.clear();
	}

	/**
	 * Gets the SELECT clause of this query.
	 * 
	 * @return	Its SELECT clause.
	 */
	public final ClauseSelect getSelect(){
		return select;
	}

	/**
	 * Replaces its SELECT clause by the given one.
	 * 
	 * @param newSelect					The new SELECT clause.
	 * 
	 * @throws NullPointerException		If the given SELECT clause is <i>null</i>.
	 */
	public void setSelect(ClauseSelect newSelect) throws NullPointerException{
		if (newSelect == null)
			throw new NullPointerException("Impossible to replace the SELECT clause of a query by NULL !");
		else
			select = newSelect;
	}

	/**
	 * Gets the FROM clause of this query.
	 * 
	 * @return	Its FROM clause.
	 */
	public final FromContent getFrom(){
		return from;
	}

	/**
	 * Replaces its FROM clause by the given one.
	 * 
	 * @param newFrom					The new FROM clause.
	 * 
	 * @throws NullPointerException		If the given FROM clause is <i>null</i>.
	 */
	public void setFrom(FromContent newFrom) throws NullPointerException{
		if (newFrom == null)
			throw new NullPointerException("Impossible to replace the FROM clause of a query by NULL !");
		else
			from = newFrom;
	}

	/**
	 * Gets the WHERE clause of this query.
	 * 
	 * @return	Its WHERE clause.
	 */
	public final ClauseConstraints getWhere(){
		return where;
	}

	/**
	 * Replaces its WHERE clause by the given one.
	 * 
	 * @param newWhere					The new WHERE clause.
	 * 
	 * @throws NullPointerException		If the given WHERE clause is <i>null</i>.
	 */
	public void setWhere(ClauseConstraints newWhere) throws NullPointerException{
		if (newWhere == null)
			where.clear();
		else
			where = newWhere;
	}

	/**
	 * Gets the GROUP BY clause of this query.
	 * 
	 * @return	Its GROUP BY clause.
	 */
	public final ClauseADQL<ADQLOperand> getGroupBy(){
		return groupBy;
	}

	/**
	 * Replaces its GROUP BY clause by the given one.
	 * 
	 * @param newGroupBy				The new GROUP BY clause.
	 * @throws NullPointerException		If the given GROUP BY clause is <i>null</i>.
	 */
	public void setGroupBy(ClauseADQL<ADQLOperand> newGroupBy) throws NullPointerException{
		if (newGroupBy == null)
			groupBy.clear();
		else
			groupBy = newGroupBy;
	}

	/**
	 * Gets the HAVING clause of this query.
	 * 
	 * @return	Its HAVING clause.
	 */
	public final ClauseConstraints getHaving(){
		return having;
	}

	/**
	 * Replaces its HAVING clause by the given one.
	 * 
	 * @param newHaving					The new HAVING clause.
	 * @throws NullPointerException		If the given HAVING clause is <i>null</i>.
	 */
	public void setHaving(ClauseConstraints newHaving) throws NullPointerException{
		if (newHaving == null)
			having.clear();
		else
			having = newHaving;
	}

	/**
	 * Gets the ORDER BY clause of this query.
	 * 
	 * @return	Its ORDER BY clause.
	 */
	public final ClauseADQL<ADQLOrder> getOrderBy(){
		return orderBy;
	}

	/**
	 * Replaces its ORDER BY clause by the given one.
	 * 
	 * @param newOrderBy				The new ORDER BY clause.
	 * @throws NullPointerException		If the given ORDER BY clause is <i>null</i>.
	 */
	public void setOrderBy(ClauseADQL<ADQLOrder> newOrderBy) throws NullPointerException{
		if (newOrderBy == null)
			orderBy.clear();
		else
			orderBy = newOrderBy;
	}

	@Override
	public ADQLObject getCopy() throws Exception{
		return new ADQLQuery(this);
	}

	@Override
	public String getName(){
		return "{ADQL query}";
	}

	/**
	 * <p>Gets the list of columns (database metadata) selected by this query.</p>
	 * 
	 * <p><i><u>Note:</u> The list is generated on the fly !</i></p>
	 * 
	 * @return	Selected columns metadata.
	 */
	public DBColumn[] getResultingColumns(){
		ArrayList<DBColumn> columns = new ArrayList<DBColumn>(select.size());

		for(SelectItem item : select){
			ADQLOperand operand = item.getOperand();
			if (item instanceof SelectAllColumns){
				try{
					// If "{table}.*", add all columns of the specified table:
					if (((SelectAllColumns)item).getAdqlTable() != null)
						columns.addAll(((SelectAllColumns)item).getAdqlTable().getDBColumns());
					// Otherwise ("*"), add all columns of all selected tables:
					else
						columns.addAll(from.getDBColumns());
				}catch(ParseException pe){
					// Here, this error should not occur any more, since it must have been caught by the DBChecker!
				}
			}else{
				DBColumn col = null;
				if (item.hasAlias()){
					if (operand instanceof ADQLColumn && ((ADQLColumn)operand).getDBLink() != null){
						col = ((ADQLColumn)operand).getDBLink();
						col = col.copy(col.getDBName(), item.getAlias(), col.getTable());
					}else
						col = new DefaultDBColumn(item.getAlias(), null);
				}else{
					if (operand instanceof ADQLColumn && ((ADQLColumn)operand).getDBLink() != null)
						col = ((ADQLColumn)operand).getDBLink();
					if (col == null)
						col = new DefaultDBColumn(item.getName(), null);
				}
				columns.add(col);
			}
		}

		DBColumn[] resColumns = new DBColumn[columns.size()];
		return columns.toArray(resColumns);
	}

	/**
	 * Lets searching ADQL objects into this ADQL query thanks to the given search handler.
	 * 
	 * @param sHandler	A search handler.
	 * 
	 * @return An iterator on all ADQL objects found.
	 */
	public Iterator<ADQLObject> search(ISearchHandler sHandler){
		sHandler.search(this);
		return sHandler.iterator();
	}

	@Override
	public ADQLIterator adqlIterator(){
		return new ADQLIterator(){

			private int index = -1;
			private ClauseADQL<?> currentClause = null;

			@Override
			public ADQLObject next(){
				index++;
				switch(index){
					case 0:
						currentClause = select;
						break;
					case 1:
						currentClause = null;
						return from;
					case 2:
						currentClause = where;
						break;
					case 3:
						currentClause = groupBy;
						break;
					case 4:
						currentClause = having;
						break;
					case 5:
						currentClause = orderBy;
						break;
					default:
						throw new NoSuchElementException();
				}
				return currentClause;
			}

			@Override
			public boolean hasNext(){
				return index + 1 < 6;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException{
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called !");

				if (replacer == null)
					remove();
				else{
					switch(index){
						case 0:
							if (replacer instanceof ClauseSelect)
								select = (ClauseSelect)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseSelect (" + select.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
						case 1:
							if (replacer instanceof FromContent)
								from = (FromContent)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a FromContent (" + from.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
						case 2:
							if (replacer instanceof ClauseConstraints)
								where = (ClauseConstraints)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseConstraints (" + where.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
						case 3:
							if (replacer instanceof ClauseADQL)
								groupBy = (ClauseADQL<ADQLOperand>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + groupBy.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
						case 4:
							if (replacer instanceof ClauseConstraints)
								having = (ClauseConstraints)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseConstraints (" + having.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
						case 5:
							if (replacer instanceof ClauseADQL)
								orderBy = (ClauseADQL<ADQLOrder>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + orderBy.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ") !");
							break;
					}
				}
			}

			@Override
			public void remove(){
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called !");

				if (index == 0 || index == 1)
					throw new UnsupportedOperationException("Impossible to remove a " + ((index == 0) ? "SELECT" : "FROM") + " clause from a query !");
				else
					currentClause.clear();
			}
		};
	}

	@Override
	public String toADQL(){
		StringBuffer adql = new StringBuffer(select.toADQL());
		adql.append("\nFROM ").append(from.toADQL());

		if (!where.isEmpty())
			adql.append('\n').append(where.toADQL());

		if (!groupBy.isEmpty())
			adql.append('\n').append(groupBy.toADQL());

		if (!having.isEmpty())
			adql.append('\n').append(having.toADQL());

		if (!orderBy.isEmpty())
			adql.append('\n').append(orderBy.toADQL());

		return adql.toString();
	}

}