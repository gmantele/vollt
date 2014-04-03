package adql.search;

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
import java.util.Iterator;
import java.util.Stack;

import adql.query.ADQLIterator;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;

/**
 * <p>Lets searching ADQL objects which match with the condition defined in the function {@link #match(ADQLObject)}.</p>
 * <ul>
 * 	<li>By default, this search handler does not search recursively (that's to say, it does not search in sub-queries).</li>
 * 	<li>By default, this search handler does not stop to the first matching object.</li>
 * 	<li>The matching objects are simply collected in an ArrayList.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 * 
 * @see SearchColumnHandler
 */
public abstract class SimpleSearchHandler implements ISearchHandler {

	/** Indicates whether this handler must search also in sub-queries. */
	private boolean recursive = false;

	/** Indicates whether this handler must stop at the first match. */
	private boolean firstMatch = false;

	/** List of all matching ADQL objects. */
	protected final ArrayList<ADQLObject> results;


	/**
	 * <p>Builds a SimpleSearchHandler:</p>
	 * <ul>
	 * 	<li>not recursive,</li>
	 * 	<li>and which does not stop at the first match.</li>
	 * </ul>
	 */
	public SimpleSearchHandler(){
		results = new ArrayList<ADQLObject>();
	}

	/**
	 * Builds a SimpleSearchHandler which does not stop at the first match.
	 * 
	 * @param recursive	<i>true</i> to search also in sub-queries, <i>false</i> otherwise.
	 */
	public SimpleSearchHandler(boolean recursive){
		this();
		this.recursive = recursive;
	}

	/**
	 * Builds a SimpleSearchHandler.
	 * 
	 * @param recursive			<i>true</i> to search also in sub-queries, <i>false</i> otherwise.
	 * @param onlyFirstMatch	<i>true</i> to stop at the first match, <i>false</i> otherwise.
	 */
	public SimpleSearchHandler(boolean recursive, boolean onlyFirstMatch){
		this(recursive);
		this.firstMatch = onlyFirstMatch;
	}

	/**
	 * Tells whether this handler must search also in sub-queries.
	 * 
	 * @return	<i>true</i> if recursive, <i>false</i> otherwise.
	 */
	public final boolean isRecursive() {
		return recursive;
	}

	/**
	 * Lets configuring this handler so that it must search also in sub-queries or not.
	 * 
	 * @param recursive	<i>true</i> to search recursively, <i>false</i> otherwise.
	 */
	public final void setRecursive(boolean recursive){
		this.recursive = recursive;
	}

	/**
	 * Tells whether this handler must stop at the first match.
	 * 
	 * @return	<i>true</i> if it stops at the first match, <i>false</i> otherwise.
	 */
	public final boolean onlyFirstMatch() {
		return firstMatch;
	}

	/**
	 * Lets configuring this handler so that it must stop at the first match.
	 * 
	 * @param firstMatch	<i>true</i> if it must stop at the first match, <i>false</i> otherwise.
	 */
	public final void setOnlyFirstMatch(boolean firstMatch) {
		this.firstMatch = firstMatch;
	}

	public final Iterator<ADQLObject> iterator() {
		return results.iterator();
	}

	public final int getNbMatch() {
		return results.size();
	}

	/**
	 * Indicates whether the research must finish now or not:
	 * by default, the research ends only at the first match if it has been asked (see {@link #setOnlyFirstMatch(boolean)}).
	 * 
	 * @return	<i>true</i> if the research must end now, <i>false</i> otherwise.
	 */
	protected boolean isFinished(){
		return firstMatch && !results.isEmpty();
	}

	/**
	 * Indicates whether the research must continue inside the given ADQL object or not:
	 * by default, it returns always <i>true</i> except if the given object is an ADQL query while the research must not be recursive.
	 * 
	 * @param obj	An ADQL object.
	 * 
	 * @return		<i>true</i> if the research must continue inside the given ADQL object, <i>false</i> otherwise.
	 */
	protected boolean goInto(ADQLObject obj){
		return recursive || !(obj instanceof ADQLQuery);
	}

	/**
	 * Adds the given ADQL object as one result of the research.
	 * 
	 * @param matchObj	An ADQL object which has matched to the research criteria.
	 * @param it		The iterator from which the matched ADQL object has been extracted.
	 */
	protected void addMatch(ADQLObject matchObj, ADQLIterator it){
		results.add(matchObj);
	}

	/**
	 * Resets this handler before the beginning of the research:
	 * by default, the list of results is cleared.
	 */
	protected void reset(){
		results.clear();
	}

	public final void search(final ADQLObject startObj){
		reset();

		if (startObj == null)
			return;

		if (match(startObj))
			results.add(startObj);

		Stack<ADQLIterator> stackIt = new Stack<ADQLIterator>();
		ADQLObject obj = null;
		ADQLIterator it = startObj.adqlIterator();

		while(!isFinished()){
			// Fetch the next ADQL object to test:
			do{
				if (it != null && it.hasNext())
					obj = it.next();
				else if (!stackIt.isEmpty())
					it = stackIt.pop();
				else
					return;
			}while(obj == null);

			// Add the current object if it is matching:
			if (match(obj))
				addMatch(obj, it);

			// Continue the research inside the current object:
			if (goInto(obj)){
				stackIt.push(it);
				it = obj.adqlIterator();
			}

			obj = null;
		}
	}

	/**
	 * Only tests whether the given ADQL object checks the search conditions.
	 * 
	 * @param obj	The ADQL object to test. (warning: this object may be <i>null</i> !)
	 * 
	 * @return		<i>true</i> if the given object checks the search conditions, <i>false</i> otherwise.
	 */
	protected abstract boolean match(ADQLObject obj);

}
