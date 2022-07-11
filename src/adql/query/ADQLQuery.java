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
 * Copyright 2012-2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.ArrayList;
import java.util.NoSuchElementException;

import adql.db.DBColumn;
import adql.db.DBIdentifier;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.feature.LanguageFeature;
import adql.parser.grammar.ParseException;
import adql.query.from.FromContent;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.UserDefinedFunction;
import adql.query.operand.function.cast.CastFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

/**
 * Object representation of an ADQL query or sub-query.
 *
 * <p>
 * 	The resulting object of the {@link ADQLParser} is an object of this class
 * 	if no set operation is performed.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (07/2022)
 */
public class ADQLQuery extends ADQLSet {

	/** Description of this ADQL Feature.
	 * @since 2.0 */
	public static final LanguageFeature FEATURE = new LanguageFeature(null, "QUERY", false, "An entire ADQL (sub-)query.");

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

	/**
	 * Builds an empty ADQL query.
	 */
	public ADQLQuery() {
		this(ADQLParser.DEFAULT_VERSION);
	}

	/**
	 * Builds an empty ADQL query following the specified ADQL grammar.
	 *
	 * @param version	Followed version of the ADQL grammar.
	 *               	<i>If NULL, the
	 *               	{@link ADQLParserFactory#DEFAULT_VERSION default version}
	 *               	will be set.</i>
	 *
	 * @since 2.0
	 */
	public ADQLQuery(final ADQLVersion version) {
		super(version);
		select = new ClauseSelect();
		from = null;
		where = new ClauseConstraints("WHERE");
		groupBy = new ClauseADQL<ADQLOperand>("GROUP BY");
		having = new ClauseConstraints("HAVING");
	}

	/**
	 * Builds an ADQL query by copying the given one.
	 *
	 * @param toCopy	The ADQL query to copy.
	 *
	 * @throws Exception	If there is an error during the copy.
	 */
	@SuppressWarnings("unchecked")
	public ADQLQuery(ADQLQuery toCopy) throws Exception {
		super(toCopy);
		select = (ClauseSelect)toCopy.select.getCopy();
		from = (FromContent)toCopy.from.getCopy();
		where = (ClauseConstraints)toCopy.where.getCopy();
		groupBy = (ClauseADQL<ADQLOperand>)toCopy.groupBy.getCopy();
		having = (ClauseConstraints)toCopy.having.getCopy();
	}

	@Override
	public final LanguageFeature getFeatureDescription() {
		return FEATURE;
	}

	/**
	 * Clear all the clauses.
	 */
	@Override
	public void reset() {
		super.reset();

		select.clear();
		select.setDistinctColumns(false);
		select.setNoLimit();

		from = null;
		where.clear();
		groupBy.clear();
		having.clear();
	}

	/**
	 * Gets the SELECT clause of this query.
	 *
	 * @return	Its SELECT clause.
	 */
	public final ClauseSelect getSelect() {
		return select;
	}

	/**
	 * Replaces its SELECT clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newSelect	The new SELECT clause.
	 *
	 * @throws NullPointerException	If the given SELECT clause is NULL.
	 */
	public void setSelect(ClauseSelect newSelect) throws NullPointerException {
		if (newSelect == null)
			throw new NullPointerException("Impossible to replace the SELECT clause of a query by NULL!");
		else
			select = newSelect;
		position = null;
	}

	/**
	 * Gets the FROM clause of this query.
	 *
	 * @return	Its FROM clause.
	 */
	public final FromContent getFrom() {
		return from;
	}

	/**
	 * Replaces its FROM clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newFrom	The new FROM clause.
	 *
	 * @throws NullPointerException	If the given FROM clause is NULL.
	 */
	public void setFrom(FromContent newFrom) throws NullPointerException {
		if (newFrom == null)
			throw new NullPointerException("Impossible to replace the FROM clause of a query by NULL!");
		else
			from = newFrom;
		position = null;
	}

	/**
	 * Gets the WHERE clause of this query.
	 *
	 * @return	Its WHERE clause.
	 */
	public final ClauseConstraints getWhere() {
		return where;
	}

	/**
	 * Replaces its WHERE clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newWhere	The new WHERE clause.
	 *
	 * @throws NullPointerException	If the given WHERE clause is NULL.
	 */
	public void setWhere(ClauseConstraints newWhere) throws NullPointerException {
		if (newWhere == null)
			where.clear();
		else
			where = newWhere;
		position = null;
	}

	/**
	 * Gets the GROUP BY clause of this query.
	 *
	 * @return	Its GROUP BY clause.
	 */
	public final ClauseADQL<ADQLOperand> getGroupBy() {
		return groupBy;
	}

	/**
	 * Replaces its GROUP BY clause by the given one.
	 *
	 * <p><i><b>Note:</b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newGroupBy	The new GROUP BY clause.
	 *
	 * @throws NullPointerException	If the given GROUP BY clause is NULL.
	 */
	public void setGroupBy(ClauseADQL<ADQLOperand> newGroupBy) throws NullPointerException {
		if (newGroupBy == null)
			groupBy.clear();
		else
			groupBy = newGroupBy;
		position = null;
	}

	/**
	 * Gets the HAVING clause of this query.
	 *
	 * @return	Its HAVING clause.
	 */
	public final ClauseConstraints getHaving() {
		return having;
	}

	/**
	 * Replaces its HAVING clause by the given one.
	 *
	 * <p><i><b>Note:<b>
	 * 	The position of the query is erased.
	 * </i></p>
	 *
	 * @param newHaving	The new HAVING clause.
	 *
	 * @throws NullPointerException	If the given HAVING clause is NULL.
	 */
	public void setHaving(ClauseConstraints newHaving) throws NullPointerException {
		if (newHaving == null)
			having.clear();
		else
			having = newHaving;
		position = null;
	}

	@Override
	public boolean hasLimit() {
		return select.hasLimit();
	}

	@Override
	public int getLimit() {
		return select.getLimit();
	}

	@Override
	public final void setNoLimit() {
		select.setNoLimit();
	}

	@Override
	public final void setLimit(int limit) {
		select.setLimit(limit);
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ADQLQuery(this);
	}

	@Override
	public String getName() {
		return "{ADQL query}";
	}

	/**
	 * Gets the list of columns (database metadata) selected by this query.
	 *
	 * <p><i><b>Note:</b>
	 * 	The list is generated on the fly!
	 * </i></p>
	 *
	 * @return	Selected columns metadata.
	 */
	@Override
	public DBColumn[] getResultingColumns() {
		ArrayList<DBColumn> columns = new ArrayList<DBColumn>(select.size());

		for(SelectItem item : select) {
			ADQLOperand operand = item.getOperand();
			if (item instanceof SelectAllColumns) {
				try {
					// If "{table}.*", add all columns of the specified table:
					if (((SelectAllColumns)item).getAdqlTable() != null)
						columns.addAll(((SelectAllColumns)item).getAdqlTable().getDBColumns());
					// Otherwise ("*"), add all columns of all selected tables:
					else
						columns.addAll(from.getDBColumns());
				} catch(ParseException pe) {
					/* Here, this error should not occur any more, since it must
					 * have been caught by the DBChecker! */
				}
			} else {
				// Create the DBColumn:
				DBColumn col = null;
				// ...whose the name will be set with the SELECT item's alias:
				if (item.hasAlias()) {
					String alias;

					// If delimited, put the alias between double quotes.
					if (item.isCaseSensitive())
						alias = DBIdentifier.denormalize(item.getAlias(), true);
					// If not delimited, put the alias in lower-case.
					else
						alias = item.getAlias().toLowerCase();

					// create the DBColumn:
					if (operand instanceof ADQLColumn && ((ADQLColumn)operand).getDBLink() != null) {
						col = ((ADQLColumn)operand).getDBLink();
						col = col.copy(alias, alias, col.getTable());
					} else
						col = new DefaultDBColumn(alias, null);
				}
				// ...or whose the name will be the name of the SELECT item:
				else {
					if (operand instanceof ADQLColumn && ((ADQLColumn)operand).getDBLink() != null) {
						DBColumn formerCol = ((ADQLColumn)operand).getDBLink();
						// keep the same ADQL and DB name ; just change the table:
						if (formerCol.isCaseSensitive())
							col = formerCol.copy(formerCol.getADQLName(), DBIdentifier.denormalize(formerCol.getADQLName(), true), formerCol.getTable());
						else
							col = formerCol.copy(formerCol.getADQLName().toLowerCase(), formerCol.getADQLName().toLowerCase(), formerCol.getTable());
					} else
						col = new DefaultDBColumn((item.isCaseSensitive() ? DBIdentifier.denormalize(item.getName(), true) : item.getName().toLowerCase()), null);
				}

				/* For columns created by default (from functions and operations generally),
				 * set the adequate type if known: */
				// CASE: UDF
				if (operand instanceof UserDefinedFunction && ((UserDefinedFunction)operand).getDefinition() != null) {
					DBType type = ((UserDefinedFunction)operand).getDefinition().returnType;
					((DefaultDBColumn)col).setDatatype(type);
				}
				// CASE: CAST with a known datatype:
				else if (operand instanceof CastFunction && ((CastFunction)operand).getReturnType() != null)
					((DefaultDBColumn)col).setDatatype(((CastFunction)operand).getReturnType());
				// CASE: Point type:
				else if (operand instanceof PointFunction || operand instanceof CentroidFunction)
					((DefaultDBColumn)col).setDatatype(new DBType(DBDatatype.POINT));
				// CASE: Region type:
				else if (operand instanceof RegionFunction || operand instanceof CircleFunction || operand instanceof BoxFunction || operand instanceof PolygonFunction)
					((DefaultDBColumn)col).setDatatype(new DBType(DBDatatype.REGION));
				// CASE: String and numeric types
				else if (col instanceof DefaultDBColumn && col.getDatatype() == null && operand.isNumeric() != operand.isString()) {
					// CASE: String types
					if (operand.isString())
						((DefaultDBColumn)col).setDatatype(new DBType(DBDatatype.VARCHAR));
					// CASE: Numeric types:
					/* Note: a little special case here since a numeric could be a real, double, integer, or anything
					 *       else and that we don't know precisely here. So we set the special UNKNOWN NUMERIC type. */
					else
						((DefaultDBColumn)col).setDatatype(new DBType(DBDatatype.UNKNOWN_NUMERIC));
				}

				// Add the new column to the list:
				columns.add(col);
			}
		}

		DBColumn[] resColumns = new DBColumn[columns.size()];
		return columns.toArray(resColumns);
	}

	@Override
	public ADQLIterator adqlIterator() {
		return new ADQLIterator() {

			private int index = -1;
			private ClauseADQL<?> currentClause = null;

			@Override
			public ADQLObject next() {
				index++;
				switch(index) {
					case 0:
						currentClause = with;
						break;
					case 1:
						currentClause = select;
						break;
					case 2:
						currentClause = null;
						return from;
					case 3:
						currentClause = where;
						break;
					case 4:
						currentClause = groupBy;
						break;
					case 5:
						currentClause = having;
						break;
					case 6:
						currentClause = orderBy;
						break;
					case 7:
						currentClause = null;
						return offset;
					default:
						throw new NoSuchElementException();
				}
				return currentClause;
			}

			@Override
			public boolean hasNext() {
				return index + 1 < 8;
			}

			@Override
			@SuppressWarnings("unchecked")
			public void replace(ADQLObject replacer) throws UnsupportedOperationException, IllegalStateException {
				if (index <= -1)
					throw new IllegalStateException("replace(ADQLObject) impossible: next() has not yet been called!");

				if (replacer == null)
					remove();
				else {
					switch(index) {
						case 0:
							if (replacer instanceof ClauseADQL)
								with = (ClauseADQL<WithItem>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + with.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 1:
							if (replacer instanceof ClauseSelect)
								select = (ClauseSelect)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseSelect (" + select.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 2:
							if (replacer instanceof FromContent)
								from = (FromContent)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a FromContent (" + from.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 3:
							if (replacer instanceof ClauseConstraints)
								where = (ClauseConstraints)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseConstraints (" + where.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 4:
							if (replacer instanceof ClauseADQL)
								groupBy = (ClauseADQL<ADQLOperand>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + groupBy.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 5:
							if (replacer instanceof ClauseConstraints)
								having = (ClauseConstraints)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseConstraints (" + having.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 6:
							if (replacer instanceof ClauseADQL)
								orderBy = (ClauseADQL<ADQLOrder>)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseADQL (" + orderBy.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
						case 7:
							if (replacer instanceof ClauseOffset)
								offset = (ClauseOffset)replacer;
							else
								throw new UnsupportedOperationException("Impossible to replace a ClauseOffset (" + offset.toADQL() + ") by a " + replacer.getClass().getName() + " (" + replacer.toADQL() + ")!");
							break;
					}
					position = null;
				}
			}

			@Override
			public void remove() {
				if (index <= -1)
					throw new IllegalStateException("remove() impossible: next() has not yet been called!");

				if (index == 1 || index == 2)
					throw new UnsupportedOperationException("Impossible to remove a " + ((index == 1) ? "SELECT" : "FROM") + " clause from a query!");
				else if (index == 7) {
					offset = null;
					position = null;
				} else {
					if (currentClause != null)
						currentClause.clear();
					position = null;
				}
			}
		};
	}

	@Override
	public String toADQL() {
		StringBuffer adql = new StringBuffer();

		if (!with.isEmpty())
			adql.append(with.toADQL()).append('\n');

		adql.append(select.toADQL());

		adql.append("\nFROM ").append(from.toADQL());

		if (!where.isEmpty())
			adql.append('\n').append(where.toADQL());

		if (!groupBy.isEmpty())
			adql.append('\n').append(groupBy.toADQL());

		if (!having.isEmpty())
			adql.append('\n').append(having.toADQL());

		if (!orderBy.isEmpty())
			adql.append('\n').append(orderBy.toADQL());

		if (offset != null)
			adql.append('\n').append(offset.toADQL());

		return adql.toString();
	}

}
