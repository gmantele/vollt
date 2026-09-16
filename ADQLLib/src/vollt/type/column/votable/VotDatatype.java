package vollt.type.column.votable;

import vollt.type.column.ColumnType;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enumeration of all possible VOTable data types.
 *
 * <p>
 *     Except for {@link #UNKNOWN} which is only for internal and technical
 *     purpose, this enumeration list is a copy of primitive types provided in
 *     the VOTable-1.4 standard (section 2.1).
 * </p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 1.0 (01/2025)
 *
 * @see ColumnType
 */
public enum VotDatatype {
    BOOLEAN,
    BIT,
    UNSIGNED_BYTE,
    SHORT,
    INT,
    LONG,
    CHAR,
    UNICODE_CHAR,
    FLOAT,
    DOUBLE,
    FLOAT_COMPLEX,
    DOUBLE_COMPLEX,
    UNKNOWN;

    private final Pattern patternWordSeparatorWithNextLetter = Pattern.compile("_([A-Za-z])");

    private final String stringVersion;

    VotDatatype(){
        stringVersion = serializeIntoString();
    }

    private String serializeIntoString() {
        // Put the whole datatype in lower-case:
        final String datatypeInLowerCase = this.name().toLowerCase();

        // Remove _ and replace the following letter in upper-case:
        final StringBuffer result = new StringBuffer();
        final Matcher m = patternWordSeparatorWithNextLetter.matcher(datatypeInLowerCase);
        while (m.find()) {
            m.appendReplacement(result, m.group(1).toUpperCase());
        }
        m.appendTail(result);

        return result.toString();
    }

    /**
     * Search for the corresponding VOTable datatype with the given string
     * serialization.
     *
     * <p><i>Note:</i>
     *  This function put the given string in lower case, and remove all spaces
     *  before comparing it with all existing {@link VotDatatype}.
     * </p>
     *
     * @param datatypeString    Datatype serialization.
     *
     * @return  The corresponding {@link VotDatatype}
     *          or {@link Optional#empty()} if not found.
     *
     * @throws NullPointerException When the given datatype is missing or empty.
     */
    public static Optional<VotDatatype> fromString(final String datatypeString) throws NullPointerException {
        if (datatypeString == null || datatypeString.trim().isEmpty())
            throw new NullPointerException("Missing serialization of the VOTable datatype to create!");

        final String normalizedDatatypeString = datatypeString.trim().toLowerCase().replaceAll("[ _\t\n-]", "");

        for(VotDatatype datatype : VotDatatype.values()){
            final String candidateDatatype = datatype.stringVersion.toLowerCase();
            if (candidateDatatype.equals(normalizedDatatypeString))
                return Optional.of(datatype);
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return stringVersion;
    }
}
