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
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2011
 * 
 * @see RemoveHandler
 */
public abstract class SimpleReplaceHandler extends SimpleSearchHandler implements IReplaceHandler {

	/** Indicates whether {@link #searchAndReplace(ADQLObject)} (=true) has been called or just {@link #search(ADQLObject)} (=false). */
	protected boolean replaceActive = false;

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

	public int getNbReplacement(){
		return nbReplacement;
	}

	@Override
	protected void reset(){
		super.reset();
		nbReplacement = 0;
	}

	@Override
	protected void addMatch(ADQLObject matchObj, ADQLIterator it){
		super.addMatch(matchObj, it);

		if (replaceActive && it != null){
			try{
				ADQLObject replacer = getReplacer(matchObj);
				if (replacer == null)
					it.remove();
				else
					it.replace(replacer);
				nbReplacement++;
			}catch(IllegalStateException ise){

			}catch(UnsupportedOperationException uoe){

			}
		}
	}

	public void searchAndReplace(final ADQLObject startObj){
		replaceActive = true;
		search(startObj);
		replaceActive = false;
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
