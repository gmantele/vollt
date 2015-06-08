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
 * Copyright 2012,2014 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institut (ARI)
 */

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

import com.oreilly.servlet.multipart.ExceededSizeException;

/**
 * Let limit the number of bytes that can be read from a given input stream.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 2.0 (08/2014)
 */
public final class LimitedSizeInputStream extends InputStream {

	/** Input stream whose the number of bytes that can be read must be limited. */
	private final InputStream input;
	/** Maximum number of bytes that can be read. */
	private final long sizeLimit;

	/** Number of bytes currently read. */
	private long counter = 0;

	/** Indicate whether the byte limit has already been reached. If <i>true</i> no more byte can be read ;
	 * all read(...) function will throw an {@link ExceededSizeException}. */
	private boolean exceed = false;

	/**
	 * Wrap the given input stream so that limiting the number of bytes that can be read.
	 * 
	 * @param stream	Stream to limit.
	 * @param sizeLimit	Maximum number of bytes that can be read. <i>If <=0 an {@link InvalidParameterException} will be thrown.</i>
	 * 
	 * @throws NullPointerException	If the input stream is missing.
	 */
	public LimitedSizeInputStream(final InputStream stream, final long sizeLimit) throws NullPointerException{
		if (stream == null)
			throw new NullPointerException("The given input stream is NULL !");
		input = stream;

		if (sizeLimit <= 0)
			throw new InvalidParameterException("The size limit must be a positive number of bytes !");
		this.sizeLimit = sizeLimit;
	}

	/**
	 * Get the input stream wrapped by this instance of {@link LimitedSizeInputStream}.
	 * 
	 * @return	The wrapped input stream.
	 * @since 2.0
	 */
	public final InputStream getInnerStream(){
		return input;
	}

	/**
	 * <p>Update the number of bytes currently read and them check whether the limit has been exceeded.
	 * If the limit has been exceeded, an {@link ExceededSizeException} is thrown.</p>
	 * 
	 * <p>Besides, the flag {@link #exceed} is set to true in order to forbid the further reading of bytes.</p>
	 * 
	 * @param nbReads	Number of bytes read.
	 * 
	 * @throws ExceededSizeException	If, after update, the limit of bytes has been exceeded.
	 */
	private void updateCounter(final long nbReads) throws ExceededSizeException{
		if (nbReads > 0){
			counter += nbReads;
			if (counter > sizeLimit){
				exceed = true;
				throw new ExceededSizeException("Data read overflow: the limit of " + sizeLimit + " bytes has been reached!");
			}
		}
	}

	/**
	 * <p>Tell whether the limit has already been exceeded or not.</p>
	 * 
	 * <p><i>Note:
	 * 	If <i>true</i> is returned, no more read will be allowed, and any attempt to read a byte will throw an {@link ExceededSizeException}.
	 * </i></p>
	 * 
	 * @return	<i>true</i> if the byte limit has been exceeded, <i>false</i> otherwise.
	 */
	public final boolean sizeExceeded(){
		return exceed;
	}

	@Override
	public int read() throws IOException{
		int read = input.read();
		updateCounter(1);
		return read;
	}

	@Override
	public int read(byte[] b) throws IOException{
		int nbRead = input.read(b);
		updateCounter(nbRead);
		return nbRead;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException{
		int nbRead = input.read(b, off, len);
		updateCounter(nbRead);
		return nbRead;
	}

	@Override
	public long skip(long n) throws IOException{
		long nbSkipped = input.skip(n);
		updateCounter(nbSkipped);
		return nbSkipped;
	}

	@Override
	public int available() throws IOException{
		return input.available();
	}

	@Override
	public void close() throws IOException{
		input.close();
	}

	@Override
	public synchronized void mark(int readlimit) throws UnsupportedOperationException{
		input.mark(readlimit);
	}

	@Override
	public boolean markSupported(){
		return input.markSupported();
	}

	@Override
	public synchronized void reset() throws IOException, UnsupportedOperationException{
		input.reset();
	}

}
