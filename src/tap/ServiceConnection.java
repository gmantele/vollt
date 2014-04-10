package tap;

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
 * Copyright 2012-2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import java.util.Collection;
import java.util.Iterator;

import tap.file.TAPFileManager;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UserIdentifier;

/**
 * 
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.1 (01/2014)
 * 
 * @param <R>
 */
public interface ServiceConnection< R > {

	/**
	 * Units used to express any limit of the TAP service.
	 * 
	 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
	 * @version 1.1 (01/2014)
	 */
	public static enum LimitUnit{
		rows, bytes, kilobytes, megabytes, gigabytes;

		/**
		 * Tells whether the given unit has the same type (bytes or rows).
		 * 
		 * @param anotherUnit	A unit.
		 * 
		 * @return				true if the given unit has the same type, false otherwise.
		 * 
		 * @since 1.1
		 */
		public boolean isCompatibleWith(final LimitUnit anotherUnit){
			if (this == rows)
				return anotherUnit == rows;
			else
				return anotherUnit != rows;
		}

		/**
		 * Gets the factor to convert into bytes the value expressed in this unit.
		 * <i>Note: if this unit is not a factor of bytes, 1 is returned (so that the factor does not affect the value).</i>
		 * 
		 * @return The factor need to convert a value expressed in this unit into bytes, or 1 if not a bytes derived unit.
		 * 
		 * @since 1.1
		 */
		public long bytesFactor(){
			switch(this){
				case bytes:
					return 1;
				case kilobytes:
					return 1000;
				case megabytes:
					return 1000000;
				case gigabytes:
					return 1000000000l;
				default:
					return 1;
			}
		}

		/**
		 * Compares the 2 given values (each one expressed in the given unit).
		 * Conversions are done internally in order to make a correct comparison between the 2 limits.
		 * 
		 * @param leftLimit	Value/Limit of the comparison left part.
		 * @param leftUnit	Unit of the comparison left part value.
		 * @param rightLimit	Value/Limit of the comparison right part.
		 * @param rightUnit		Unit of the comparison right part value.
		 * 
		 * @return the value 0 if x == y; a value less than 0 if x < y; and a value greater than 0 if x > y
		 * 
		 * @throws TAPException If the two given units are not compatible.
		 * 
		 * @see #isCompatibleWith(LimitUnit)
		 * @see #bytesFactor()
		 * @see Integer#compare(int, int)
		 * @see Long#compare(long, long)
		 * 
		 * @since 1.1
		 */
		public static int compare(final int leftLimit, final LimitUnit leftUnit, final int rightLimit, final LimitUnit rightUnit) throws TAPException{
			if (!leftUnit.isCompatibleWith(rightUnit))
				throw new TAPException("Limit units (" + leftUnit + " and " + rightUnit + ") are not compatible!");

			if (leftUnit == rows || leftUnit == rightUnit)
				return Integer.compare(leftLimit, rightLimit);
			else
				return Long.compare(leftLimit * leftUnit.bytesFactor(), rightLimit * rightUnit.bytesFactor());
		}
	}

	public String getProviderName();

	public String getProviderDescription();

	public boolean isAvailable();

	public String getAvailability();

	public int[] getRetentionPeriod();

	public int[] getExecutionDuration();

	public int[] getOutputLimit();

	public LimitUnit[] getOutputLimitType();

	public UserIdentifier getUserIdentifier();

	public boolean uploadEnabled();

	public int[] getUploadLimit();

	public LimitUnit[] getUploadLimitType();

	public int getMaxUploadSize();

	public TAPMetadata getTAPMetadata();

	public Collection<String> getCoordinateSystems();

	public TAPLog getLogger();

	public TAPFactory<R> getFactory();

	public TAPFileManager getFileManager();

	public Iterator<OutputFormat<R>> getOutputFormats();

	public OutputFormat<R> getOutputFormat(final String mimeOrAlias);

}
