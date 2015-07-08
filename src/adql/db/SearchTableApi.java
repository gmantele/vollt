package adql.db;

import java.util.List;

import adql.query.from.ADQLTable;

public interface SearchTableApi
    {

    /**
     * Searches all {@link DBTable} elements corresponding to the given {@link ADQLTable} (case insensitive).
     * 
     * @param table	An {@link ADQLTable}.
     * 
     * @return		The list of all corresponding {@link DBTable} elements.
     * 
     * @see #search(String, String, String, byte)
     */
    public List<DBTable> search(final ADQLTable table);

    }
