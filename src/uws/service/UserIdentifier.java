package uws.service;

/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.Serializable;
import java.util.Map;

import uws.job.user.JobOwner;

import uws.service.UWSUrl;

import uws.service.actions.UWSAction;
import uws.service.backup.DefaultUWSBackupManager;
import uws.UWSException;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Lets defining how identifying a user thanks to a HTTP request.</p>
 * 
 * <p>
 *	This interface is mainly used by any subclass of {@link UWSService} to identify the author of a UWS action to apply.
 * 	It can be set by the function: {@link UWSService#setUserIdentifier(UserIdentifier)}.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 06/2012
 */
public interface UserIdentifier extends Serializable {

	/**
	 * <p>Extracts the ID of the user/owner of the current session.</p>
	 * 
	 * <p>This method is called just before choosing and applying a {@link UWSAction}.</p>
	 * 
	 * @param urlInterpreter	The interpreter of the request URL.
	 * @param request			The request.
	 * 
	 * @return					The owner/user
	 * 							or <i>null</i> to mean all users.
	 * 
	 * @throws UWSException		If any error occurs while extraction the user ID from the given parameters.
	 * 
	 * @see UWSService#executeRequest(HttpServletRequest, HttpServletResponse)
	 */
	public JobOwner extractUserId(UWSUrl urlInterpreter, HttpServletRequest request) throws UWSException;

	/**
	 * Creates a user with the given parameters or merely gets it if it already exists after updating it.
	 * 
	 * @param id			ID of the user.
	 * @param pseudo		Pseudo of the user (may be NULL).
	 * @param otherdata		Other data about the user (may be NULL or empty).
	 * 
	 * @return				The corresponding user.
	 * 
	 * @throws UWSException	If any error occurs while creating or getting the user.
	 * 
	 * @see DefaultUWSBackupManager#restoreAll()
	 */
	public JobOwner restoreUser(final String id, final String pseudo, final Map<String,Object> otherData) throws UWSException;

}