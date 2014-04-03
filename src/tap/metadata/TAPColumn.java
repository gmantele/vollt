package tap.metadata;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;
import java.util.Iterator;

import adql.db.DBColumn;
import adql.db.DBTable;

public class TAPColumn implements DBColumn {

	private final String adqlName;

	private String dbName = null;

	private DBTable table = null;

	private String description = null;

	private String unit = null;

	private String ucd = null;

	private String utype = null;

	private String datatype = null;

	private int size = TAPTypes.NO_SIZE;

	private VotType votType = null;

	private boolean principal = false;

	private boolean indexed = false;

	private boolean std = false;

	protected Object otherData = null;

	protected final ArrayList<TAPForeignKey> lstTargets;

	protected final ArrayList<TAPForeignKey> lstSources;


	public TAPColumn(String columnName){
		if (columnName == null || columnName.trim().length() == 0)
			throw new NullPointerException("Missing column name !");
		int indPrefix = columnName.lastIndexOf('.');
		adqlName = (indPrefix >= 0)?columnName.substring(indPrefix+1).trim():columnName.trim();
		dbName = adqlName;
		lstTargets = new ArrayList<TAPForeignKey>(1);
		lstSources = new ArrayList<TAPForeignKey>(1);
		setDefaultType();
	}

	public TAPColumn(String columnName, String description){
		this(columnName);
		this.description = description;
	}

	public TAPColumn(String columnName, String description, String unit){
		this(columnName, description);
		this.unit = unit;
	}

	public TAPColumn(String columnName, String description, String unit, String ucd, String utype){
		this(columnName, description, unit);
		this.ucd = ucd;
		this.utype = utype;
	}

	/**
	 * @return The name.
	 */
	public final String getName() {
		return getADQLName();
	}

	@Override
	public final String getADQLName() {
		return adqlName;
	}

	@Override
	public final String getDBName() {
		return dbName;
	}

	public final void setDBName(String name){
		name = (name != null)?name.trim():name;
		dbName = (name == null || name.length() == 0)?adqlName:name;
	}

	/**
	 * @return The table.
	 */
	public final DBTable getTable() {
		return table;
	}

	/**
	 * @param table The table to set.
	 */
	public final void setTable(DBTable table) {
		this.table = table;
	}

	/**
	 * @return The description.
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * @param description The description to set.
	 */
	public final void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return The unit.
	 */
	public final String getUnit() {
		return unit;
	}

	/**
	 * @param unit The unit to set.
	 */
	public final void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * @return The ucd.
	 */
	public final String getUcd() {
		return ucd;
	}

	/**
	 * @param ucd The ucd to set.
	 */
	public final void setUcd(String ucd) {
		this.ucd = ucd;
	}

	/**
	 * @return The utype.
	 */
	public final String getUtype() {
		return utype;
	}

	/**
	 * @param utype The utype to set.
	 */
	public final void setUtype(String utype) {
		this.utype = utype;
	}

	/**
	 * @return The datatype.
	 */
	public final String getDatatype() {
		return datatype;
	}

	/**
	 * @return Array size (>0 or 2 special values: {@link TAPTypes#NO_SIZE} and {@link TAPTypes#STAR_SIZE}).
	 */
	public final int getArraySize() {
		return size;
	}

	/**
	 * <p>Sets the DB datatype, the size and uses these information to set the corresponding VOTable type.</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>If the given datatype is not known according to {@link TAPTypes#getDBType(String)}, the datatype of this column is set to its default value (see {@link #setDefaultType()}),</li>
	 * 	<li>The VOTable type is set automatically thanks to {@link TAPTypes#getVotType(String, int)}.</li>
	 * </ul>
	 * 
	 * @param datatype 	The datatype to set.
	 * @param size		Array size (>0 or 2 special values: {@link TAPTypes#NO_SIZE} and {@link TAPTypes#STAR_SIZE}).
	 * 
	 * @see TAPTypes#getDBType(VotType)
	 * @see TAPTypes#getVotType(String, int)
	 * @see #setDefaultType()
	 */
	public final void setDatatype(String datatype, int size) {
		this.datatype = TAPTypes.getDBType(datatype);
		this.size = (size <= 0 && size != TAPTypes.STAR_SIZE)?TAPTypes.NO_SIZE:size;

		if (this.datatype == null)
			setDefaultType();
		else
			this.votType = TAPTypes.getVotType(this.datatype, this.size);
	}

	/**
	 * @return The VOTable type to use.
	 */
	public final VotType getVotType(){
		return votType;
	}

	/**
	 * <p>Sets the VOTable type and uses it to set the DB datatype and its size.</p>
	 * <b>Important:</b>
	 * <ul>
	 * 	<li>If the given VOTable type is not known according to {@link TAPTypes#getDBType(VotType)}, the DB datatype of this column and its size are set to the default value (see {@link #setDefaultType()}).</li>
	 * </ul>
	 * 
	 * @param type	A full VOTable type (that's to say: <code>datatype</code>, <code>arraysize</code> and <code>xtype</code>).
	 * 
	 * @see TAPTypes#getDBType(VotType)
	 * @see #setDefaultType()
	 */
	public final void setVotType(final VotType type){
		this.votType = type;
		this.datatype = TAPTypes.getDBType(type);
		this.size = type.arraysize;

		if (this.datatype == null)
			setDefaultType();
	}

	/**
	 * Sets the default DB datatype (VARCHAR) and its corresponding VOTable type (char , *).
	 */
	protected final void setDefaultType(){
		datatype = TAPTypes.VARCHAR;
		size = TAPTypes.STAR_SIZE;
		votType = TAPTypes.getVotType(datatype, size);
	}

	/**
	 * @return The principal.
	 */
	public final boolean isPrincipal() {
		return principal;
	}

	/**
	 * @param principal The principal to set.
	 */
	public final void setPrincipal(boolean principal) {
		this.principal = principal;
	}

	/**
	 * @return The indexed.
	 */
	public final boolean isIndexed() {
		return indexed;
	}

	/**
	 * @param indexed The indexed to set.
	 */
	public final void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

	/**
	 * @return The std.
	 */
	public final boolean isStd() {
		return std;
	}

	/**
	 * @param std The std to set.
	 */
	public final void setStd(boolean std) {
		this.std = std;
	}

	public Object getOtherData(){
		return otherData ;
	}

	public void setOtherData(Object data){
		otherData = data;
	}

	protected void addTarget(TAPForeignKey key){
		if (key != null)
			lstTargets.add(key);
	}

	protected int getNbTargets(){
		return lstTargets.size();
	}

	protected Iterator<TAPForeignKey> getTargets(){
		return lstTargets.iterator();
	}

	protected void removeTarget(TAPForeignKey key){
		lstTargets.remove(key);
	}

	protected void removeAllTargets(){
		lstTargets.clear();
	}

	protected void addSource(TAPForeignKey key){
		if (key != null)
			lstSources.add(key);
	}

	protected int getNbSources(){
		return lstSources.size();
	}

	protected Iterator<TAPForeignKey> getSources(){
		return lstSources.iterator();
	}

	protected void removeSource(TAPForeignKey key){
		lstSources.remove(key);
	}

	protected void removeAllSources(){
		lstSources.clear();
	}

	public DBColumn copy(final String dbName, final String adqlName, final DBTable dbTable){
		TAPColumn copy = new TAPColumn((adqlName==null)?this.adqlName:adqlName, description, unit, ucd, utype);
		copy.setDBName((dbName==null)?this.dbName:dbName);
		copy.setTable(dbTable);

		copy.setDatatype(datatype, size);
		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);

		return copy;
	}

	public DBColumn copy(){
		TAPColumn copy = new TAPColumn(adqlName, description, unit, ucd, utype);
		copy.setDBName(dbName);
		copy.setTable(table);
		copy.setDatatype(datatype, size);
		copy.setIndexed(indexed);
		copy.setPrincipal(principal);
		copy.setStd(std);
		copy.setOtherData(otherData);
		return copy;
	}

	@Override
	public boolean equals(Object obj){
		if (! (obj instanceof TAPColumn))
			return false;

		TAPColumn col = (TAPColumn)obj;
		return col.getTable().equals(table) && col.getName().equals(adqlName);
	}

	@Override
	public String toString(){
		return ((table != null)?(table.getADQLName()+"."):"")+adqlName;
	}

}
