package tap.upload;

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
 * Copyright 2024 - Centre de Données astronomiques de Strasbourg (CDS)
 */

import uk.ac.starlink.util.DataSource;
import uws.service.request.UploadFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * STIL data source wrapping an {@link UploadFile}.
 *
 * <p>
 *     It can deal with a limit in bytes thanks to the constructor
 *     {@link #UploadDataSource(UploadFile, long)}.
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.4 (08/2024)
 * @since 2.4
 *
 * @see tap.data.STILTableIterator
 */
public class UploadDataSource extends DataSource {

	protected final UploadFile file;

	protected final long byteLimit;

	public UploadDataSource(final UploadFile upl) {
		this(upl, -1);
	}

	public UploadDataSource(final UploadFile upl, final long maxBytes) {
		super();

		this.file = Objects.requireNonNull(upl, "Missing file to upload representation!");
		setName(upl.getParamName());

		this.byteLimit = maxBytes;
	}

	public final Optional<String> getMimeType(){
		return file.getMimeType();
	}

	public final String getFileName(){
		return file.getFileName();
	}

	@Override
	protected InputStream getRawInputStream() throws IOException {
		if (byteLimit > 0)
			return new LimitedSizeInputStream(file.open(), byteLimit);
		else
			return file.open();
	}

	private long getUploadFileLength(){
		return file.getLength().orElse((long)-1);
	}

	@Override
	public long getRawLength() {
		if (byteLimit > 0)
			return Math.min(byteLimit, getUploadFileLength());
		else
			return getUploadFileLength();
	}



}
