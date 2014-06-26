package tap.data;

import tap.TAPException;

/**
 * Exception that occurs when reading a data input (can be an InputStream, a ResultSet, a SavotTable, ...).
 * 
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 2.0 (06/2014)
 * @since 2.0
 * 
 * @see TableIterator
 */
public class DataReadException extends TAPException {
	private static final long serialVersionUID = 1L;

	public DataReadException(final String message){
		super(message);
	}

	public DataReadException(Throwable cause){
		super(cause);
	}

	public DataReadException(String message, Throwable cause){
		super(message, cause);
	}

}
