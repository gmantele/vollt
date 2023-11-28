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
 * Copyright 2017 - Astronomisches Rechen Institut (ARI)
 */

/**
 * Definition of a coordinate system.
 * 
 * <p>
 * 	This object just annotates a {@link TAPColumn} object (see {@link TAPColumn#getCoosys()}
 * 	and {@link TAPColumn#setCoosys(TAPCoosys)}). Its only purpose is to enrich the VOTable
 * 	metadata so that VO clients (like Aladin and TOPCAT) can use the coordinates the most
 * 	precisely as possible.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (ARI)
 * @version 2.1 (07/2017)
 * @since 2.1
 */
public class TAPCoosys {

	/** ID of this coordinate system definition.
	 * <p>It is particularly used in the VOTable to associate columns with it.</p>
	 * <i>Note: This attribute can NOT be NULL.</i> */
	protected final String id;
	
	/** Name of the coordinate system.
	 * <p>
	 * 	It should be a value among:
	 * 	"ICRS", "eq FK5", "eq FK4", "ecl FK4", "ecl FK5",
	 * 	"galactic", "supergalactic", "barycentric", "geo app"
	 * 	and a user-defined "xy" value.
	 * </p> */
	protected final String system;
	
	/** Equinox of this coordinate system.
	 * <p>
	 * 	This parameter required to fix the equatorial or ecliptic systems
	 * 	(as e.g. "J2000" as the default "eq FK5" or "B1950" as the default "eq FK4").
	 * </p> */
	protected String equinox;
	
	/** Epoch at which the coordinates were measured. */
	protected String epoch;
	
	/**
	 * Create a minimum coordinate system definition.
	 * 
	 * @param id		ID of the definition to create. <i>(must NOT be NULL)</i>
	 * @param system	Coordinate system. <i>(must NOT be NULL)</i>
	 * 
	 * @throws NullPointerException	If any of the given parameters is NULL or an empty string.
	 */
	public TAPCoosys(final String id, final String system) throws NullPointerException {
		this(id, system, null, null);
	}
	
	/**
	 * Create a coordinate system definition.
	 * 
	 * <p>
	 * 	Only the ID and the system are required.
	 * 	The equinox and especially the epoch are optional.
	 * </p>
	 * 
	 * @param id		ID of the definition to create. <i>(must NOT be NULL)</i>
	 * @param system	Coordinate system. <i>(must NOT be NULL)</i>
	 * @param equinox	Equinox of this coordinate system.
	 * @param epoch		Epoch at which the coordinates were measured.
	 * 
	 * @throws NullPointerException	If the ID or the system is NULL or an empty string.
	 */
	public TAPCoosys(final String id, final String system, final String equinox, final String epoch) throws NullPointerException {
		if (id == null || id.trim().length() == 0)
			throw new NullPointerException("Missing Coosys ID!");
		this.id = id;
		
		if (system == null || system.trim().length() == 0)
			throw new NullPointerException("Missing coordinate system!");
		this.system = system;
		
		this.equinox = equinox;
		this.epoch = epoch;
	}

	/**
	 * Get the ID of this coordinate system definition.
	 * 
	 * @return	Its ID. <i>(can NOT be NULL)</i>
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Get the coordinate system of this definition.
	 * 
	 * @return	Its system. <i>(can NOT be NULL)</i>
	 */
	public final String getSystem() {
		return system;
	}

	/**
	 * Get the equinox of this coordinate system.
	 * 
	 * @return	Its equinox. <i>(may be NULL)</i>
	 */
	public final String getEquinox() {
		return equinox;
	}

	/**
	 * Set the equinox of this coordinate system.
	 * 
	 * @param equinox	Its new equinox. <i>(may be NULL)</i>
	 */
	public void setEquinox(final String equinox) {
		this.equinox = equinox;
	}

	/**
	 * Get the epoch at which the coordinates were measured.
	 * 
	 * @return	Its equinox. <i>(may be NULL)</i>
	 */
	public final String getEpoch() {
		return epoch;
	}

	/**
	 * Set the epoch at which the coordinates were measured.
	 * 
	 * @param epoch	Its new epoch. <i>(may be NULL)</i>
	 */
	public void setEpoch(final String epoch) {
		this.epoch = epoch;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (!(obj instanceof TAPCoosys))
			return false;

		TAPCoosys coosys = (TAPCoosys)obj;
		return coosys.getId().equals(getId());
	}

	@Override
	public String toString(){
		return id;
	}
	
}
