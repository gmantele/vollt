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
 * Copyright 2012-2013 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
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
 * @version 1.1 (12/2013)
 * 
 * @param <R>
 */
public interface ServiceConnection< R > {

	public static enum LimitUnit{
		rows, bytes, kilobytes, megabytes, gigabytes;
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
