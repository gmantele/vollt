package adql.search;

import java.util.Stack;

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
 * Copyright 2012,2016 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import adql.query.ADQLIterator;
import adql.query.ADQLObject;

/**
 * <p>Lets searching and replacing ADQL objects which match with the condition defined in the function {@link #match(ADQLObject)}.</p>
 * <ul>
 * 	<li>By default, this replace handler does not search recursively (that's to say, it does not search in sub-queries).</li>
 * 	<li>By default, this replace handler does not stop to the first matching object.</li>
 * 	<li>The matching objects are simply collected in an ArrayList.</li>
 * 	<li>Matching objects are collected before their replacement.</li>
 * </ul>
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (05/2016)
 * 
 * @see RemoveHandler
 */
public abstract class SimpleReplaceHandler extends SimpleSearchHandler implements IReplaceHandler {

	/** Count matching objects which have been replaced successfully. */
	protected int nbReplacement = 0;

	/**
	 * <p>Builds a SimpleReplaceHandler:</p>
	 * <ul>
	 * 	<li>not recursive,</li>
	 * 	<li>and which does not stop at the first match.</li>
	 * </ul>
	 */
	public SimpleReplaceHandler(){
		super();
	}

	/**
	 * Builds a SimpleReplaceHandler which does not stop at the first match.
	 * 
	 * @param recursive	<i>true</i> to search also in sub-queries, <i>false</i> otherwise.
	 */
	public SimpleReplaceHandler(boolean recursive){
		super(recursive);
	}

	/**
	 * Builds a SimpleReplaceHandler.
	 * 
	 * @param recursive			<i>true</i> to search also in sub-queries, <i>false</i> otherwise.
	 * @param onlyFirstMatch	<i>true</i> to stop at the first match, <i>false</i> otherwise.
	 */
	public SimpleReplaceHandler(boolean recursive, boolean onlyFirstMatch){
		super(recursive, onlyFirstMatch);
	}

	@Override
	public int getNbReplacement(){
		return nbReplacement;
	}

	@Override
	protected void reset(){
		super.reset();
		nbReplacement = 0;
	}

	/**
	 * <p>Adds the given ADQL object as one result of the research, and then replace its reference
	 * inside its parent.</p>
	 * 
	 * <p>Thus, the matched item added in the list is no longer available in its former parent.</p>
	 * 
	 * <p><b><u>Warning:</u> the second parameter (<i>it</i>) may be <i>null</i> if the given match is the root search object itself.</b></p>
	 * 
	 * @param matchObj	An ADQL object which has matched to the research criteria.
	 * @param it		The iterator from which the matched ADQL object has been extracted.
	 * 
	 * @return	The match item after replacement if any replacement has occurred,
	 *        	or <code>null</code> if the item has been removed,
	 *        	or the object given in parameter if there was no replacement.
	 */
	protected ADQLObject addMatchAndReplace(ADQLObject matchObj, ADQLIterator it){
		super.addMatch(matchObj, it);

		if (it != null){
			try{
				ADQLObject replacer = getReplacer(matchObj);
				if (replacer == null)
					it.remove();
				else
					it.replace(replacer);
				nbReplacement++;
				return replacer;
			}catch(IllegalStateException ise){

			}catch(UnsupportedOperationException uoe){

			}
		}

		return matchObj;
	}

	@Override
	public void searchAndReplace(final ADQLObject startObj){
		reset();

		if (startObj == null)
			return;

		// Test the root search object:
		if (match(startObj))
			addMatch(startObj, null);

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
				obj = addMatchAndReplace(obj, it);

			// Continue the research inside the current object (or the new object if a replacement has been performed):
			if (obj != null && goInto(obj)){
				stackIt.push(it);
				it = obj.adqlIterator();
			}

			obj = null;
		}
	}

	/**
	 * <p>Gets (generate on the fly or not) an ADQLObject which must replace the given one (expected to be an ADQLObject that has matched).</p>
	 * 
	 * <p><b><u>IMPORTANT:</u> It is the responsibility of the object which calls this method to apply the replacement !</b></p>
	 * 
	 * <p><i><u>Note:</u> If the returned value is </i>null<i> it may be interpreted as a removal of the matched ADQL object. Note also that it is
	 * still the responsibility of the object which calls this method to apply the removal !</i></p>
	 * 
	 * @param objToReplace	The ADQL item to replace.
	 * 
	 * @return				The replacement ADQL item.
	 * 
	 * @throws UnsupportedOperationException	If the this method must not be used.
	 */
	protected abstract ADQLObject getReplacer(ADQLObject objToReplace) throws UnsupportedOperationException;

}
