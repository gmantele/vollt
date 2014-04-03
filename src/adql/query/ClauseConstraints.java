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

import java.util.Vector;

import adql.query.constraint.ADQLConstraint;

/**
 * <p>Represents a clause which deals with {@link ADQLConstraint}s (i.e. WHERE, HAVING).</p>
 * 
 * <p>The logical operators AND (see {@link ClauseConstraints#AND}) and OR (see {@link ClauseConstraints#OR}) are managed
 * in a separated list by this class. To add a constraint you can use the default add functions or use the one defined by this class:
 * <ul>
 * 	<li> {@link ClauseConstraints#add(String, ADQLConstraint) add(String, ADQLConstraint}: which lets you specify the logical operator
 * 		 between the added constraint (index=size()) and the previous one (index=size()-1) in the list.</li>
 * 	<li> {@link ClauseConstraints#add(int, String, ADQLConstraint) add(int, String, ADQLConstraint}: which lets you specify
 * 		 the logical operator between the added constraint (index) and the previous one (index-1) in the list.</li>
 * </ul></p>
 * 
 * <p>If no logical separator is specified the default one is used (see {@link ClauseConstraints#getDefaultSeparator() getDefaultSeparator()}).
 * The only way to set this default separator is during the {@link ClauseConstraints} creation
 * (see {@link ClauseConstraints#ClauseConstraints(String, String) ClauseConstraints(String, String)}).</p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 01/2012
 * 
 * @see adql.query.constraint.ConstraintsGroup
 */
public class ClauseConstraints extends ClauseADQL<ADQLConstraint> {

	/** The logical separator OR <i>(By default, the default separator (see {@link ClauseConstraints#getDefaultSeparator() getDefaultSeparator()}) is equals to OR)</i>. */
	public final static String OR = "OR";

	/** The logical separator AND. */
	public final static String AND = "AND";

	/** The logical separator used when none is specified during a constraint insertion <i>(by default = {@link ClauseConstraints#OR OR})</i>. */
	protected final String defaultSeparator;

	/** The used separators for the constraints list (vSeparators.size() = size()-1 ;
	 * vSeparators.get(i) gives the separator between get(i-1) and get(i)). */
	private Vector<String> vSeparators = new Vector<String>();


	/**
	 * <p>Builds a constraints list with only its name (which will prefix the whole list).</p>
	 * 
	 * <p><i><u>Note:</u> The default separator is automatically set to OR.</i></p>
	 * 
	 * @param name	The name/prefix of the list.
	 */
	public ClauseConstraints(String name) {
		super(name);
		defaultSeparator = OR;
	}

	/**
	 * <p>Builds a constraints list with its name and its default separator.</p>
	 * 
	 * <p><i><u>Note:</u> if the given separator is no {@link ClauseConstraints#AND}
	 * or {@link ClauseConstraints#OR}, the default separator is automatically set to {@link ClauseConstraints#OR}.</i></p>
	 * 
	 * @param name			The name/prefix of the list.
	 * @param logicalSep	The constraints separator (a logical separator: {@link ClauseConstraints#AND} or {@link ClauseConstraints#OR}).
	 * 
	 * @see ClauseConstraints#checkSeparator(String)
	 */
	public ClauseConstraints(String name, String logicalSep) {
		super(name);
		defaultSeparator = checkSeparator(logicalSep);
	}

	/**
	 * <p>Builds a ClauseConstraints by copying the given one.</p>
	 * 
	 * @param toCopy		The ClauseConstraints to copy.
	 * @throws Exception	If there is an error during the copy.
	 * 
	 * @see ClauseConstraints#checkSeparator(String)
	 */
	public ClauseConstraints(ClauseConstraints toCopy) throws Exception {
		super(toCopy);
		defaultSeparator = checkSeparator(toCopy.defaultSeparator);
	}

	/**
	 * Gets the default constraints separator ({@link ClauseConstraints#AND} or {@link ClauseConstraints#OR}).
	 * 
	 * @return	The default separator.
	 */
	public final String getDefaultSeparator(){
		return defaultSeparator;
	}

	/**
	 * Checks/Converts the given separator in one of the two logical separators.
	 * If the given separator is neither AND nor OR, then the returned separator is {@link ClauseConstraints#OR OR}.
	 * 
	 * @param sepToCheck	The separator to check/convert.
	 * @return				The understood separator (OR if neither AND nor OR).
	 */
	public final static String checkSeparator(String sepToCheck){
		if (sepToCheck != null && sepToCheck.equalsIgnoreCase(AND))
			return AND;
		else
			return OR;
	}

	/**
	 * @see adql.query.ADQLList#add(adql.query.ADQLObject)
	 * @see ClauseConstraints#add(String, ADQLConstraint)
	 */
	@Override
	public boolean add(ADQLConstraint constraint) throws NullPointerException {
		return add(defaultSeparator, constraint);
	}

	/**
	 * @see adql.query.ADQLList#add(int, adql.query.ADQLObject)
	 * @see ClauseConstraints#add(int, String, ADQLConstraint)
	 */
	@Override
	public void add(int index, ADQLConstraint constraint) throws NullPointerException, ArrayIndexOutOfBoundsException {
		add(index, defaultSeparator, constraint);
	}

	/**
	 * Adds the given constraint with the given separator. The separator is added just before the added constraint,
	 * that is to say between the last constraint of the list and the added one.
	 * 
	 * @param logicalSep				The separator to add just before the given constraint.
	 * @param constraint				The constraint to add.
	 * @return							<i>true</i> if the constraint has been successfully added, <i>false</i> otherwise.
	 * @throws NullPointerException		If the given constraint is <i>null</i>.
	 * 
	 * @see ADQLList#add(ADQLObject)
	 * @see ClauseConstraints#checkSeparator(String)
	 */
	public boolean add(String logicalSep, ADQLConstraint constraint) throws NullPointerException {
		boolean added = super.add(constraint);
		if (added && size() > 1)
			vSeparators.add(checkSeparator(logicalSep));
		return added;
	}

	/**
	 * Adds the given constraint with the given separator at the given position in the constraints list.
	 * The separator is added just before the added constraint, that is to say between the (index-1)-th constraint of the list and the added one (at the index position).
	 * 
	 * @param index								Position at which the given constraint must be added.
	 * @param logicalSep						The constraints separator to add just before the given constraint.
	 * @param constraint						The constraint to add.
	 * @throws NullPointerException				If the given constraint is <i>null</i>.
	 * @throws ArrayIndexOutOfBoundsException	If the given index is incorrect (index < 0 || index >= size()).
	 * 
	 * @see ADQLList#add(int, ADQLObject)
	 * @see ClauseConstraints#checkSeparator(String)
	 */
	public void add(int index, String logicalSep, ADQLConstraint constraint) throws NullPointerException, ArrayIndexOutOfBoundsException {
		super.add(index, constraint);
		if (index > 0)
			vSeparators.add(index-1, checkSeparator(logicalSep));
	}

	/**
	 * @see adql.query.ADQLList#set(int, adql.query.ADQLObject)
	 * @see ClauseConstraints#set(int, String, ADQLConstraint)
	 */
	@Override
	public ADQLConstraint set(int index, ADQLConstraint constraint) throws NullPointerException, ArrayIndexOutOfBoundsException {
		return set(index, null, constraint);
	}

	/**
	 * Replaces the specified constraint by the given one with the given constraint separator.
	 * The separator is added just before the added constraint, that is to say between the (index-1)-th constraint of the list and the added one (at the index position).
	 * 
	 * @param index								Position of the constraint to replace.
	 * @param logicalSep						The separator to insert just before the given constraint (if <i>null</i>, the previous separator is kept).
	 * @param constraint						The replacer.
	 * @return									The replaced constraint.
	 * @throws NullPointerException				If the given constraint is <i>null</i>.
	 * @throws ArrayIndexOutOfBoundsException	If the given index is incorrect (index < 0 || index >= size()).
	 * 
	 * @see ADQLList#set(int, ADQLObject)
	 */
	public ADQLConstraint set(int index, String logicalSep, ADQLConstraint constraint) throws NullPointerException, ArrayIndexOutOfBoundsException {
		ADQLConstraint replaced = super.set(index, constraint);
		if (replaced != null && logicalSep != null && index > 0)
			vSeparators.set(index-1, logicalSep);
		return replaced;
	}

	@Override
	public void clear() {
		super.clear();
		vSeparators.clear();
	}

	@Override
	public ADQLConstraint remove(int index) throws ArrayIndexOutOfBoundsException {
		ADQLConstraint removed = super.remove(index);
		if (removed != null)
			if (index > 0)
				vSeparators.remove(index-1);
			else if (index == 0)
				vSeparators.remove(index);
		return removed;
	}

	@Override
	public ADQLObject getCopy() throws Exception {
		return new ClauseConstraints(this);
	}

	/**
	 * Only two values in this case: the both logical separators: {@link ClauseConstraints#AND AND} and {@link ClauseConstraints#OR OR}.
	 * 
	 * @see adql.query.ADQLList#getPossibleSeparators()
	 */
	@Override
	public String[] getPossibleSeparators() {
		return new String[]{AND, OR};
	}

	@Override
	public String getSeparator(int index) throws ArrayIndexOutOfBoundsException {
		if (index <= 0 || index > size())
			throw new ArrayIndexOutOfBoundsException("Impossible to get the logical separator between the item "+(index-1)+" and "+index+" !");
		else
			return vSeparators.get(index-1);
	}

}
