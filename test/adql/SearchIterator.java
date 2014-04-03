package adql;

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
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Lets iterate on each "real" result <i>({@link SearchResult} objects whose the {@link SearchResult#isResult() isResult()} function returns </i>true<i>)</i>.
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 11/2010
 * 
 * @see SearchResult
 */
public class SearchIterator implements Iterator<SearchResult> {

	/** List of the next SearchResult objects which has at least one result (themselves or included SearchResult). */
	protected Vector<SearchResult> toExplore;


	public SearchIterator(SearchResult r){
		toExplore = new Vector<SearchResult>();
		if (r != null && r.hasResult())
			toExplore.add(r);
	}

	public boolean hasNext() {
		return !toExplore.isEmpty();
	}

	public SearchResult next() throws NoSuchElementException {
		SearchResult next = null;

		while(next == null && !toExplore.isEmpty()){
			SearchResult r = toExplore.remove(0);
			if (!r.isLeaf()){
				Iterator<SearchResult> children = r.getChildren();
				while(children.hasNext()){
					SearchResult child = children.next();
					if (child != null && child.hasResult())
						toExplore.add(child);
				}
			}
			if (r.isResult())
				next = r;
		}

		if (next == null)
			throw new NoSuchElementException("No more search result !");

		return next;
	}

	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("The REMOVE operation is not possible in a search result !");
	}

}
