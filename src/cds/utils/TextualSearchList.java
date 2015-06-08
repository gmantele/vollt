package cds.utils;

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
import java.util.Collection;
import java.util.HashMap;

/**
 * <p>A TextualSearchList is an {@link ArrayList} with a textual search capability.</p>
 * <p>
 * 	The interest of this class lies in the fact that objects can be searched with
 * 	or without case sensitivity on their textual key thanks to {@link #get(String, boolean)}.
 * </p>
 * <p>
 * 	The textual key is extracted by an object implementing the {@link KeyExtractor} instance.
 * 	If no {@link KeyExtractor} instance is given at initialization, the string returned
 * 	by the {@link Object#toString() toString()} function will be used as key.
 * </p>
 * <p><b><u>WARNING:</u> The extracted key MUST be CASE-SENSITIVE and UNIQUE !</b></p>
 * 
 * @param <E>	Type of object to manage in this list.
 * @author 		Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 	1.1 (11/2013)
 */
public class TextualSearchList< E > extends ArrayList<E> {
	private static final long serialVersionUID = 1L;

	/** Object to use to extract an unique textual string. */
	public final KeyExtractor<E> keyExtractor;

	/** Map which associates objects of type E with its textual string (case-sensitive). */
	protected final HashMap<String,ArrayList<E>> csMap;

	/** Map which associates objects of type E with their lower-case textual string. */
	protected final HashMap<String,ArrayList<E>> ncsMap;

	/* ************ */
	/* CONSTRUCTORS */
	/* ************ */
	/**
	 * <p>Builds an empty TextualSearchList.</p>
	 * 
	 * <p><i><u>Note:</u>
	 * 	the key of inserted objects will be the string returned by their {@link Object#toString() toString()} function.
	 * </i></p>
	 * 
	 * @see #TextualSearchList(KeyExtractor)
	 */
	public TextualSearchList(){
		this(new DefaultKeyExtractor<E>());
	}

	/**
	 * Builds an empty TextualSearchList.
	 * 
	 * @param keyExtractor	The object to use to extract a textual key from objects to insert.
	 * 
	 * @see ArrayList#ArrayList()
	 */
	public TextualSearchList(final KeyExtractor<E> keyExtractor){
		super();
		this.keyExtractor = keyExtractor;
		csMap = new HashMap<String,ArrayList<E>>();
		ncsMap = new HashMap<String,ArrayList<E>>();
	}

	/**
	 * <p>Builds an empty TextualSearchList with an initial capacity.</p>
	 * 
	 * <p><i><u>Note:</u>
	 * 	the key of inserted objects will be the string returned by their {@link Object#toString() toString()} function.
	 * </i></p>
	 * 
	 * @param initialCapacity	Initial capacity of this list.
	 * 
	 * @see #TextualSearchList(int, KeyExtractor)
	 */
	public TextualSearchList(int initialCapacity){
		this(initialCapacity, new DefaultKeyExtractor<E>());
	}

	/**
	 * Builds an empty TextualSearchList with an initial capacity.
	 * 
	 * @param initialCapacity	Initial capacity of this list.
	 * @param keyExtractor		The object to use to extract a textual key from objects to insert.
	 * 
	 * @see ArrayList#ArrayList(int)
	 */
	public TextualSearchList(final int initialCapacity, final KeyExtractor<E> keyExtractor){
		super(initialCapacity);
		this.keyExtractor = keyExtractor;
		csMap = new HashMap<String,ArrayList<E>>(initialCapacity);
		ncsMap = new HashMap<String,ArrayList<E>>(initialCapacity);
	}

	/**
	 * <p>Builds a TextualSearchList filled with the objects of the given collection.</p>
	 * 
	 * <p><i><u>Note:</u>
	 * 	the key of inserted objects will be the string returned by their {@link Object#toString() toString()} function.
	 * </i></p>
	 * 
	 * @param c	Collection to copy into this list.
	 */
	public TextualSearchList(Collection<? extends E> c){
		this(c, new DefaultKeyExtractor<E>());
	}

	/**
	 * Builds a TextualSearchList filled with the objects of the given collection.
	 * 
	 * @param c				Collection to copy into this list.
	 * @param keyExtractor	The object object to use to extract a textual key from objects to insert.
	 * 
	 * @see #addAll(Collection)
	 */
	public TextualSearchList(Collection<? extends E> c, final KeyExtractor<E> keyExtractor){
		this.keyExtractor = keyExtractor;
		csMap = new HashMap<String,ArrayList<E>>(c.size());
		ncsMap = new HashMap<String,ArrayList<E>>(c.size());
		addAll(c);
	}

	/**
	 * Returns true if this list contains the specified element.
	 * More formally, returns true if and only if this list contains at least one element
	 * e such that (keyExtractor.getKey(o).equals(keyExtractor.getKey(e))).
	 * 
	 * @see java.util.ArrayList#contains(java.lang.Object)
	 * @see #getKey(Object)
	 * 
	 * @since 1.1
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o){
		try{
			if (o == null)
				return false;
			else{
				E object = (E)o;
				return !get(getKey(object)).isEmpty();
			}
		}catch(Exception e){
			return false;
		}
	}

	/**
	 * Searches (CASE-INSENSITIVE) the object which has the given key.
	 * 
	 * @param key	Textual key of the object to search.
	 * 
	 * @return		The corresponding object or <code>null</code>.
	 */
	public final ArrayList<E> get(final String key){
		return get(key, false);
	}

	/**
	 * Searches of all the object which has the given key.
	 * 
	 * @param key			Textual key of the object to search.
	 * @param caseSensitive	<i>true</i> to consider the case of the key, <i>false</i> otherwise.
	 * 
	 * @return		All the objects whose the key is the same as the given one.
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<E> get(final String key, final boolean caseSensitive){
		if (key == null)
			return new ArrayList<E>(0);

		ArrayList<E> founds = caseSensitive ? csMap.get(key) : ncsMap.get(key.toLowerCase());
		if (founds == null)
			return new ArrayList<E>(0);
		else
			return (ArrayList<E>)founds.clone();
	}

	/**
	 * Generates and checks the key of the given object.
	 * 
	 * @param value	The object whose the key must be generated.
	 * 
	 * @return		Its corresponding key or <i>null</i> if this object already exist in this list.
	 * 
	 * @throws NullPointerException		If the given object or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException	If the extracted key is already used by another object in this list.
	 */
	private final String getKey(final E value) throws NullPointerException, IllegalArgumentException{
		String key = keyExtractor.getKey(value);
		if (key == null)
			throw new NullPointerException("Null keys are not allowed in a TextualSearchList !");
		return key;
	}

	/**
	 * Adds the given object in the two maps with the given key.
	 * 
	 * @param key	The key with which the given object must be associated.
	 * @param value	The object to add.
	 */
	private final void putIntoMaps(final String key, final E value){
		// update the case-sensitive map:
		putIntoMap(csMap, key, value);
		// update the case-INsensitive map:
		putIntoMap(ncsMap, key.toLowerCase(), value);
	}

	/**
	 * Adds the given object in the given map with the given key.
	 * 
	 * @param map	The map in which the given value must be added.
	 * @param key	The key with which the given object must be associated.
	 * @param value	The object to add.
	 * 
	 * @param <E>	The type of objects managed in the given map.
	 */
	private static final < E > void putIntoMap(final HashMap<String,ArrayList<E>> map, final String key, final E value){
		ArrayList<E> lst = map.get(key);
		if (lst == null){
			lst = new ArrayList<E>();
			lst.add(value);
			map.put(key, lst);
		}else
			lst.add(value);
	}

	/**
	 * Adds the given object at the end of this list.
	 * 
	 * @param obj	Object to add (different from NULL).
	 * 
	 * @throws NullPointerException		If the given object or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException	If the extracted key is already used by another object in this list.
	 * 
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(E obj) throws NullPointerException, IllegalArgumentException{
		if (obj == null)
			throw new NullPointerException("Null objects are not allowed in a TextualSearchList !");

		String key = getKey(obj);
		if (key == null)
			return false;

		if (super.add(obj)){
			putIntoMaps(key, obj);
			return true;
		}else
			return false;
	}

	/**
	 * Adds the given object at the given position in this list.
	 * 
	 * @param index	Index at which the given object must be added.
	 * @param obj	Object to add (different from NULL).
	 * 
	 * @throws NullPointerException			If the given object or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException		If the extracted key is already used by another object in this list.
	 * @throws IndexOutOfBoundsException	If the given index is negative or greater than the size of this list.
	 * 
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, E obj) throws NullPointerException, IllegalArgumentException, IndexOutOfBoundsException{
		if (obj == null)
			throw new NullPointerException("Null objects are not allowed in a TextualSearchList !");

		String key = getKey(obj);
		if (key == null)
			return;

		super.add(index, obj);
		if (get(index).equals(obj))
			putIntoMaps(key, obj);
	}

	/**
	 * Appends all the objects of the given collection in this list.
	 * 
	 * @param c	Collection of objects to add.
	 * 
	 * @return <code>true</code> if this list changed as a result of the call, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException		If an object to add or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException	If the extracted key is already used by another object in this list.
	 * 
	 * @see java.util.ArrayList#addAll(java.util.Collection)
	 * @see #add(Object)
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) throws NullPointerException, IllegalArgumentException{
		if (c == null)
			return false;

		boolean modified = false;
		for(E obj : c)
			modified = add(obj) || modified;

		return modified;
	}

	/**
	 * Appends all the objects of the given collection in this list after the given position.
	 * 
	 * @param index	Position from which objects of the given collection must be added.
	 * @param c		Collection of objects to add.
	 * 
	 * @return <code>true</code> if this list changed as a result of the call, <code>false</code> otherwise.
	 * 
	 * @throws NullPointerException			If an object to add or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException		If the extracted key is already used by another object in this list.
	 * @throws IndexOutOfBoundsException	If the given index is negative or greater than the size of this list.
	 * 
	 * @see java.util.ArrayList#addAll(int, java.util.Collection)
	 * @see #add(int, Object)
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) throws NullPointerException, IllegalArgumentException, IndexOutOfBoundsException{
		if (c == null)
			return false;

		boolean modified = false;
		int ind = index;
		for(E obj : c){
			add(ind++, obj);
			modified = get(ind).equals(obj);
		}

		return modified;
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element.
	 * 
	 * @param index	Position of the object to replace.
	 * @param obj	Object to be stored at the given position (different from NULL).
	 * 
	 * @return Replaced object.
	 * 
	 * @throws NullPointerException			If the object to add or its extracted key is <code>null</code>.
	 * @throws IllegalArgumentException		If the extracted key is already used by another object in this list.
	 * @throws IndexOutOfBoundsException	If the given index is negative or greater than the size of this list.
	 * 
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public E set(int index, E obj) throws NullPointerException, IllegalArgumentException{
		if (obj == null)
			throw new NullPointerException("Null objects are not allowed in a TextualSearchList !");

		if (get(index).equals(obj))
			return obj;

		String key = getKey(obj);

		E old = super.set(index, obj);

		// Removes the old object from the index:
		String oldKey = keyExtractor.getKey(old);
		removeFromMaps(oldKey, old);

		// Adds the new object into the index:
		putIntoMaps(key, obj);

		return old;
	}

	@Override
	public void clear(){
		super.clear();
		csMap.clear();
		ncsMap.clear();
	}

	/**
	 * Removes the given object associated with the given key from the two maps.
	 * 
	 * @param key	The key associated with the given object.
	 * @param value	The object to remove.
	 */
	private final void removeFromMaps(final String key, final E value){
		// update the case-sensitive map:
		removeFromMap(csMap, key, value);
		// update the case-insensitive map:
		removeFromMap(ncsMap, key.toLowerCase(), value);
	}

	/**
	 * Removes the given object associated with the given key from the given map.
	 * 
	 * @param map	The map from which the given object must be removed.
	 * @param key	The key associated with the given object.
	 * @param value	The object to remove.
	 * 
	 * @param <E>	The type of objects managed in the given map.
	 */
	private static final < E > void removeFromMap(final HashMap<String,ArrayList<E>> map, final String key, final E value){
		ArrayList<E> lst = map.get(key);
		if (lst != null){
			lst.remove(value);
			if (lst.isEmpty())
				map.remove(key);
		}
	}

	@Override
	public E remove(int index){
		E removed = super.remove(index);
		if (removed != null){
			String key = keyExtractor.getKey(removed);
			removeFromMaps(key, removed);
		}
		return removed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object obj){
		boolean removed = super.remove(obj);
		if (removed){
			String key = keyExtractor.getKey((E)obj);
			removeFromMaps(key, (E)obj);
		}
		return removed;
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) throws IndexOutOfBoundsException{
		if (fromIndex < 0 || fromIndex >= size() || toIndex < 0 || toIndex >= size() || fromIndex > toIndex)
			throw new IndexOutOfBoundsException("Incorrect range indexes: from " + fromIndex + " to " + toIndex + " !");

		for(int i = fromIndex; i < toIndex; i++)
			remove(i);
	}

	/* ************************************************ */
	/* KEY_EXTRACTOR INTERFACE & DEFAULT IMPLEMENTATION */
	/* ************************************************ */
	/**
	 * Lets extract an unique textual key (case-sensitive) from a given type of object.
	 * 
	 * @author G&eacute;gory Mantelet (CDS)
	 * @param <E>	Type of object from which a textual key must be extracted.
	 * @see TextualSearchList
	 */
	public static interface KeyExtractor< E > {
		/**
		 * Extracts an UNIQUE textual key (case-sensitive) from the given object.
		 * @param obj	Object from which a textual key must be extracted.
		 * @return		Its textual key (case-sensitive).
		 */
		public String getKey(final E obj);
	}

	/**
	 * Default implementation of {@link KeyExtractor}.
	 * The extracted key is the string returned by the {@link Object#toString() toString()} function.
	 * 
	 * @author Gr&eacute;gory Mantelet (CDS)
	 * @param <E>	Type of object from which a textual key must be extracted.
	 */
	protected static class DefaultKeyExtractor< E > implements KeyExtractor<E> {
		@Override
		public String getKey(final E obj){
			return obj.toString();
		}
	}

}
