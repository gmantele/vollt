package uws.service.file.io;

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
 * Copyright 2018 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This close action rename a given file into another one. The target file is
 * deleted if it already exists.
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 4.4 (07/2018)
 * @since 4.4
 */
public class RotateFileAction implements CloseAction {

	/** File to rotate (i.e. rename). */
	private final File sourceFile;

	/** File into which {@link #sourceFile} must be renamed. */
	private final File targetFile;

	/**
	 * Create the rotate action.
	 *
	 * @param sourceFile	File to rotate.
	 * @param targetFile	File into which sourceFile must be renamed.
	 *
	 * @throws NullPointerException		If any of the given file is missing.
	 * @throws IllegalArgumentException	If the source file does not exist,
	 *                                 	or if it is not a regular file,
	 *                                 	or if the target file exists but is not
	 *                                 	  a regular file.
	 */
	public RotateFileAction(final File sourceFile, final File targetFile) throws NullPointerException, IllegalArgumentException{
		// Ensure the source file exists and is a regular file:
		if (sourceFile == null)
			throw new NullPointerException("Missing source file!");
		else if (!sourceFile.exists())
			throw new IllegalArgumentException("The source file \"" + sourceFile.getAbsolutePath() + "\" does not exist!");
		else if (sourceFile.isDirectory())
			throw new IllegalArgumentException("The source file \"" + sourceFile.getAbsolutePath() + "\" is a directory instead of a regular file!");

		// Check that if the target file exists, it is a regular file:
		if (targetFile == null)
			throw new NullPointerException("Missing target file!");
		else if (targetFile.exists() && targetFile.isDirectory())
			throw new IllegalArgumentException("The target file \"" + targetFile.getAbsolutePath() + "\" is a directory instead of a regular file!");

		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
	}

	@Override
	public void run() throws IOException{
		// Delete the target file if it already exists:
		try{
			Files.deleteIfExists(targetFile.toPath());
		}catch(IOException ioe){
			throw new IOException("Impossible to perform the file rotation! Cause: the former file can not be deleted.", ioe);
		}

		// Finally rename the source file into the given target file:
		try{
			Files.move(sourceFile.toPath(), targetFile.toPath());
		}catch(IOException ioe){
			throw new IOException("Impossible to perform the file rotation! Cause: [" + ioe.getClass() + "] " + ioe.getMessage(), ioe);
		}
	}

}
