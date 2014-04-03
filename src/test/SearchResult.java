package test;

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
 * Copyright 2011 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.Iterator;
import java.util.Vector;

import adql.query.ADQLObject;

/**
 * <p>Results of a research in an ADQL query.</p>
 * 
 * <p>This class is built as a tree. A node (leaf or not) corresponds to an item of a part of an ADQL query or merely of a whole ADQL query.
 * It represents a step of the research. That means a node can represents a matched ADQL item and/or a list of other SearchResults (which are the results of the same research into the corresponding ADQL object).
 * Thus it is possible to know the parent (into the ADQL query) of a matched ADQL item.</p>
 * 
 * <p>Here are some useful functions of this class:
 * <ul>
 * 	<li><u>{@link SearchResult#isResult() isResult()}:</u> indicates whether the current node corresponds to a matched ADQL item</li>
 * 	<li><u>{@link SearchResult#getResult() getResult()}:</u> returns the value of this node</li>
 * 	<li><u>{@link SearchResult#getParent() getParent()}:</u> returns the result (node) which encapsulates this result (node)</li>
 * 	<li><u>{@link SearchResult#isLeaf() isLeaf()}:</u> indicates whether this node encapsulates other results (nodes) or not</li>
 * 	<li><u>{@link SearchResult#getChildren() getChildren()}:</u> returns an iterator on all encapsulated results (nodes)</li>
 * </ul></p>
 * 
 * <p>You have two different ways to navigate in a SearchResult object:
 * <ol>
 * 	<li>As said previously a SearchResult is a hierarchical structure. So you can <b>explore it as a tree</b> with the functions {@link SearchResult#getResult() getResult()} (to get the node value), {@link SearchResult#getParent() getParent()} (to get the direct parent node), {@link SearchResult#getChildren() getChildren()} (to explore the children list) and {@link SearchResult#isLeaf() isLeaf()} (to determine if the current node is a leaf or not).</li>
 * 	<li>However you can also <b>iterate directly</b> on each matched ADQL item (leaf or not) thanks to the {@link SearchResult#iterator() iterator()} function. All iterated object corresponds to a matched ADQL object (so {@link SearchResult#isResult() isResult()} always returns <i>true</i> for all iterated results).</li>
 * </ol></p>
 *
 * <p><b><u>Important:</u> Be aware that any SearchResult (leaf or not) may contain a matched ADQL object: to know that, use the function {@link SearchResult#isResult() isResult()}.</b></p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2010
 * 
 * @see SearchIterator
 */
public final class SearchResult implements Iterable<SearchResult> {

	/** Parent node. */
	private SearchResult parent;

	/** Child nodes. */
	private final Vector<SearchResult> children;

	/** Total number of results from this node (included). */
	private int nbResults = 0;

	/** The node value (may be the matched ADQL object). */
	private final ADQLObject value;

	/** Indicates whether this node corresponds to a matched ADQL object or not. */
	private final boolean result;

	/** If it is impossible to replace an ADQL object by another one, a SearchResult must be created (with result = true) and this field must contain an error description. */
	private String error = null;


	/**
	 * <p>Builds a SearchResult (node) with its value (node value).</p>
	 * <p><i><u>Note:</u> By using this constructor the created SearchResult will not correspond to a matched ADQL object.</i></p>
	 * 
	 * @param nodeValue	Value (ADQL object) associated with this node.
	 */
	public SearchResult(ADQLObject nodeValue){
		this(nodeValue, false);
	}

	/**
	 * Builds a SearchResult (node) with its value (node value) and an indication on its interpretation (~ "matched ADQL object ?").
	 * 
	 * @param nodeValue		Value (ADQL object) associated with this node.
	 * @param isResult		Indicates whether the given ADQL object is a match or not.
	 */
	public SearchResult(ADQLObject nodeValue, boolean isResult){
		this.parent = null;
		children = new Vector<SearchResult>();

		value = nodeValue;
		result = (nodeValue != null) && isResult;
		if (result) nbResults = 1;
	}

	/**
	 * Gets the ADQL object associated with this node.
	 * It may be a matched ADQL item (it depends of what returns the {@link SearchResult#isResult() isResult()} function).
	 * 
	 * @return	The node value.
	 */
	public final ADQLObject getResult(){
		return value;
	}

	/**
	 * Indicates whether the ADQL object (returned by {@link SearchResult#getResult() getResult()}) is a match or not.
	 * 
	 * @return	<i>true</i> if this SearchResult corresponds to a matched ADQL item, <i>false</i> otherwise.
	 */
	public final boolean isResult(){
		return result;
	}

	/**
	 * Gets the error that occurs when replacing the matched item.
	 * 
	 * @return	Replacing error.
	 */
	public final String getError(){
		return error;
	}

	/**
	 * Indicates whether there was an error during the replacement of the matched item.
	 * 
	 * @return	<i>true</i> if there was an error during the replacement, <i>false</i> else.
	 */
	public final boolean hasError(){
		return error != null;
	}

	/**
	 * Sets the explanation of why the matched item has not been replaced.
	 * 
	 * @param msg	Error description.
	 */
	public final void setError(String msg){
		if (msg != null){
			msg = msg.trim();
			error = (msg.length()==0)?null:msg;
		}else
			error = null;
	}

	/**
	 * Gets the parent node.
	 * 
	 * @return	Its parent node.
	 */
	public final SearchResult getParent(){
		return parent;
	}

	/**
	 * Changes the parent node.
	 * 
	 * @param newParent	Its new parent node.
	 */
	private final void setParent(SearchResult newParent){
		parent = newParent;
	}

	/**
	 * Gets an iterator on the children list of this SearchResult.
	 * 
	 * @return	An iterator on its children.
	 */
	public final Iterator<SearchResult> getChildren(){
		return children.iterator();
	}

	/**
	 * Indicates whether this node is a leaf (that is to say if it has children).
	 * 
	 * @return	<i>true</i> if this node is a leaf, <i>false</i> otherwise.
	 */
	public final boolean isLeaf(){
		return children.isEmpty();
	}

	/**
	 * Lets adding a child to this node.
	 * 
	 * @param result	The SearchResult to add.
	 */
	public final void add(SearchResult result){
		if (result != null){
			// Add the given result:
			children.add(result);

			// Set its parent:
			result.setParent(this);

			// Update the total number of results from this node:
			updateNbResults();
		}
	}

	/**
	 * Counts exactly the total number of results from this node (included).
	 * Once the counting phase finished the direct parent node is notify that it must update its own number of results.
	 */
	private final void updateNbResults(){
		synchronized (this) {
			// Count all results from this node:
			nbResults = isResult()?1:0;
			for(SearchResult r : children)
				nbResults += r.getNbResults();
		}

		// Notify the direct parent node:
		if (parent != null)
			parent.updateNbResults();
	}

	/**
	 * <p>Indicates whether this node is and/or contains some results (SearchResult objects whose the function isResult() returns <i>true</i>).</p>
	 * 
	 * @return	<i>true</i> if this SearchResult is a result or if one of its children is a result, <i>false</i> otherwise.
	 */
	public final boolean hasResult(){
		return nbResults > 0;
	}

	/**
	 * <p>Tells exactly the number of SearchResult which are really results.</p>
	 * 
	 * @return	The number of matched ADQL item.
	 */
	public final int getNbResults(){
		return nbResults;
	}

	/**
	 * Lets iterating on all contained SearchResult objects (itself included) which are really a result (whose the function isResult() returns <i>true</i>).
	 * 
	 * @see java.lang.Iterable#iterator()
	 * @see SearchIterator
	 */
	public final Iterator<SearchResult> iterator() {
		return new SearchIterator(this);
	}

}
