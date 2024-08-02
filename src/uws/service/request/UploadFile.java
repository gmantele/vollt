package uws.service.request;

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
 * Copyright 2014-2024 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import uws.job.UWSJob;
import uws.job.parameters.UWSParameters;
import uws.service.file.UWSFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>This class lets represent a file submitted inline in an HTTP request.</p>
 * 
 * <p>
 * 	To read this special kind of parameter, an {@link InputStream} must be open. This class lets do it
 * 	by its function {@link #open()}.
 * </p>
 * 
 * <p>
 * 	When not used any more this file should be deleted, in order to save server disk space.
 * 	This can be easily done thanks to {@link #deleteFile()}. This function actually just call the corresponding function
 * 	of the file manager, which is the only one to known how to deal with this file on the server. Indeed, even if most
 * 	of the time this file is stored on the local file system, it could also be stored on a distant server by a VOSpace.
 * 	In this case, the way to proceed is different, hence the use of the file manager.
 * </p>
 * 
 * @author Gr&eacute;gory Mantelet (CDS,ARI)
 * @version 4.5 (08/2024)
 * 
 * @see UWSParameters
 * @see MultipartParser
 */
public class UploadFile {
	/** Name of the parameter in which the file was submitted. */
	public final String paramName;

	/** File name. It is the name provided in the HTTP request. */
	public final String fileName;

	/** Location at which the content of this upload has been stored.
	 * It can be a local file path, but also any other path or ID allowing
	 * the {@link UWSFileManager} to access its content. */
	protected String location;

	/** Jobs that owns this uploaded file. */
	protected UWSJob owner = null;

	/** Indicate whether this file has been or is used by a UWSJob.
	 * In other words, it is <i>true</i> when an open, move or delete operation has been performed.
	 * An unused {@link UploadFile} instance shall be physically deleted from the file system. */
	protected boolean used = false;

	/** MIME type of the file. */
	protected String mimeType = null;

	/** Length in bytes of the file. */
	protected Long length = null;

	/** File manager to use in order to open, move or delete this uploaded file. */
	protected final UWSFileManager fileManager;

	/**
	 * Build the description of an uploaded file.
	 * 
	 * @param paramName		Name of the HTTP request parameter in which the uploaded content was stored. <b>MUST NOT be NULL</b>
	 * @param location		Location of the file on the server. This String is then used by the given file manager in order to open,
	 *                		move or delete the uploaded file. Thus, it can be a path, an ID or any other String meaningful to the file manager.
	 * @param fileManager	File manager to use in order to open, move or delete this uploaded file from the server.
	 */
	public UploadFile(final String paramName, final String location, final UWSFileManager fileManager){
		this(paramName, null, location, fileManager);
	}

	/**
	 * Build the description of an uploaded file.
	 * 
	 * @param paramName		Name of the HTTP request parameter in which the uploaded content was stored. <b>MUST NOT be NULL</b>
	 * @param fileName		Filename as provided by the HTTP request. <i>If NULL, set by default to paramName.</i>
	 * @param location		Location of the file on the server. This String is then used by the given file manager in order to open,
	 *                		move or delete the uploaded file. Thus, it can be a path, an ID or any other String meaningful to the file manager.
	 * @param fileManager	File manager to use in order to open, move or delete this uploaded file from the server.
	 */
	public UploadFile(final String paramName, final String fileName, final String location, final UWSFileManager fileManager){
		this.paramName   = Objects.requireNonNull(paramName,"Missing name of the parameter in which the uploaded file content was => can not create UploadFile!");
		this.fileName    = (fileName == null || fileName.trim().isEmpty()) ? this.paramName : fileName;
		this.location    = Objects.requireNonNull(location, "Missing server location of the uploaded file => can not create UploadFile!");
		this.fileManager = Objects.requireNonNull(fileManager, "Missing file manager => can not create the UploadFile!");
	}

	/** @since 4.5 */
	public String getParamName() { return paramName; }

	/** @since 4.5 */
	public String getFileName() { return fileName;}

	/**
	 * <p>Get the location (e.g. URI, file path) of this file on the server.</p>
	 * 
	 * <p><i>Important note:
	 * 	This function SHOULD be used only by the {@link UWSFileManager} when open, move and delete operations are executed.
	 * 	The {@link RequestParser} provided by the library set this location to the file URI (i.e. "file://{local-file-path}")
	 * 	since the default behavior is to store uploaded file on the system temporary directory.
	 * </i></p>
	 * 
	 * @return	Location (e.g. URI) or ID or any other meaningful String used by the file manager to access to the uploaded file.
	 */
	public String getLocation(){
		return location;
	}

	/**
	 * Get the job that uses this uploaded file.
	 * 
	 * @return	The owner of this file.
	 */
	public Optional<UWSJob> getOwner(){
		return Optional.ofNullable(owner);
	}

	/**
	 * <p>Tell whether this uploaded file has been or will be used.
	 * That's to say, whether an open, delete or move operation has been executed (even if it failed) on this {@link UploadFile} instance.</p>
	 * 
	 * @return	<i>true</i> if the file must be preserved, <i>false</i> otherwise.
	 */
	public final boolean isUsed(){
		return used;
	}

	/** @since 4.5 */
	public Optional<String> getMimeType() {
		return Optional.ofNullable(mimeType);
	}

	/** @since 4.5 */
	public void setMimeType(final String mimeType) {
		if (mimeType == null || mimeType.trim().isEmpty())
			this.mimeType = null;
		else
			this.mimeType = mimeType.trim();
	}

	/** @since 4.5 */
	public Optional<Long> getLength() {
		if (length == null || length < 0)
			return Optional.empty();
		else
			return Optional.of(length);
	}

	/** @since 4.5 */
	public void setLength(final Long length) {
		this.length = (length == null || length < 0) ? null : length;
	}

	/**
	 * Open a stream toward this uploaded file.
	 * 
	 * @return	Stream toward this upload content.
	 * 
	 * @throws IOException	If an error occurs while opening the stream.
	 * 
	 * @see UWSFileManager#getUploadInput(UploadFile)
	 */
	public InputStream open() throws IOException{
		used = true;
		return fileManager.getUploadInput(this);
	}

	/**
	 * Delete definitely this uploaded file from the server.
	 * 
	 * @throws IOException	If the delete operation can not be performed.
	 *        
	 * @see UWSFileManager#deleteUpload(UploadFile)
	 */
	public void deleteFile() throws IOException{
		fileManager.deleteUpload(this);
		used = true;
	}

	/**
	 * <p>Move this uploaded file in a location related to the given {@link UWSJob}.
	 * It is particularly useful if at reception of an HTTP request uploaded files are stored in a temporary
	 * directory (e.g. /tmp on Unix/Linux systems).</p>
	 * 
	 * <p>
	 * 	This function calls {@link UWSFileManager#moveUpload(UploadFile, UWSJob)} to process to the physical
	 * 	moving of the file, but it then, it updates its location in this {@link UploadFile} instance.
	 * 	<b>The file manager does NOT update this location! That's why it must not be called directly, but
	 * 	through {@link #move(UWSJob)}.</b>
	 * </p>
	 * 
	 * @param destination	The job by which this uploaded file will be exclusively used.
	 * 
	 * @throws IOException	If the move operation can not be performed.
	 * 
	 * @see UWSFileManager#moveUpload(UploadFile, UWSJob)
	 */
	public void move(final UWSJob destination) throws IOException{
		Objects.requireNonNull(destination, "Missing move destination (i.e. the job in which the uploaded file must be stored)!");

		location = fileManager.moveUpload(this, destination);
		used     = true;
		owner    = destination;
	}

	@Override
	public String toString(){
		return (owner != null && owner.getJobList() != null && owner.getUrl() != null) ? owner.getUrl().jobParameter(owner.getJobList().getName(), owner.getJobId(), paramName).toString() : fileName;
	}
}
