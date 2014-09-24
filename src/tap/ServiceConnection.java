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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.util.Collection;
import java.util.Iterator;

import tap.file.TAPFileManager;
import tap.formatter.OutputFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UserIdentifier;

/**
 * TODO JAVADOC OF THE WHOLE CLASS!
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (09/2014)
 */
public interface ServiceConnection {

	public static enum LimitUnit{
		rows("row"),
		bytes("byte");
		
		private final String str;
		private LimitUnit(final String str){
			this.str = str;
		}
		public String toString(){
			return str;
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

	/**
	 * <p>Get the maximum number of asynchronous jobs that can run in the same time.</p>
	 * 
	 * <p>A null or negative value means <b>no limit</b> on the number of running asynchronous jobs.</p> 
	 * 
	 * @return	Maximum number of running jobs (&le;0 => no limit).
	 * 
	 * @since 2.0
	 */
	public int getNbMaxAsyncJobs();

	public TAPLog getLogger();

	public TAPFactory getFactory();

	public TAPFileManager getFileManager();

	public Iterator<OutputFormat> getOutputFormats();

	public OutputFormat getOutputFormat(final String mimeOrAlias);

}
