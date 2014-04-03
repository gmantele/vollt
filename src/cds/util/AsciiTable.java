package cds.util;

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
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.ArrayList;

/**
 * An object of this class manages an ascii table: it receives lines to add,
 * made of columns separated by a given separator char. Columns can be aligned
 * (RIGHT, LEFT or CENTER) before display
 * @author Marc Wenger/CDS
 * @version 1.0 May 2008 Creation<br>
 * @version 1.1 May 2008 Fix a bug: lines are kept without a newline at the end<br>
 * @version 1.2 Jun 2008 Add a toString method (items aligned).<br>
 *                       Fix a bug in align() when the last line is not full
 */
public class AsciiTable {
	public static final int LEFT = 0;
	public static final int CENTER = 1;
	public static final int RIGHT = 2;

	private String SPACES = "          ";	// extendable (see addSpaces() method)
	private String HSEP = "--------------------";
	private ArrayList<String> lines = new ArrayList<String>();	// list of lines
	private boolean empty = true;	// as long as the list of lines is empty
	private boolean header = false;	// the list begins with a header
	private String headerPostfix = null;	// string to display after the header
	private int[] sizes;	// size of the columns
	private char csep;	// column separator (as char)
	private String sep;	// column separator (as string)

	/**
	 * Constructor
	 * @param separ character defining the column separator in the input lines
	 */
	public AsciiTable(char separ){
		csep = separ;
		sep = String.valueOf(csep);
	}

	/**
	 * Add a header line. Several lines of header can be defined.
	 * @param headerline header string
	 */
	public void addHeaderLine(String headerline){
		header = true;
		addLine(headerline);
	}

	/**
	 * Specifies that the header lines are finished. This call is mandatory
	 * @param postfix String to append after the header lines (i.e. "\n")
	 * @see #endHeaderLine()
	 */
	public void endHeaderLine(String postfix){
		lines.add(null);
		headerPostfix = postfix;
	}

	/**
	 * Specifies that the header lines are finished. This call is mandatory
	 * @see #endHeaderLine(String)
	 */
	public void endHeaderLine(){
		lines.add(null);
		headerPostfix = null;
	}

	/**
	 * Add a line to the table
	 * @param line string containing the line with all the columns separated by the column separator.
	 * The line should not end up with a newline char. If it is the case, alignement errors can be experienced
	 * depending on the alignement type of the last column.
	 */
	public void addLine(String line){
		// compute the number of columns, if we add the first line
		if (empty){
			int p = 0;
			int nbcol = 1;	// at least one column (also: there is one separator less than columns)
			boolean done = false;
			while(!done){
				p = line.indexOf(sep, p);
				if (p >= 0)
					nbcol++;
				else
					done = true;
				p++;
			}
			// initialize the result
			sizes = new int[nbcol];
			for(int i = 0; i < sizes.length; i++){
				sizes[i] = 0;
			}
			empty = false;
		}

		// get the max size for each column
		int p0, p1, col, colsize;
		p0 = 0;
		col = 0;
		while(p0 < line.length()){
			p1 = line.indexOf(sep, p0);
			if (p1 < 0)
				p1 = line.length();
			colsize = p1 - p0;
			sizes[col] = Math.max(sizes[col], colsize);
			p0 = p1 + 1;
			col++;
		}

		lines.add(line);
	}

	/**
	 * Get all the lines without alignement, as they were entered
	 * @return the array of the lines in the table
	 */
	public String[] displayRaw(){
		return lines.toArray(new String[0]);
	}

	/**
	 * Get all the lines without alignement, as they were entered, with separator control
	 * @param newsep separator to use, replacing the original one
	 * @return the array of the lines in the table
	 */
	public String[] displayRaw(char newsep){
		if (newsep == csep)
			return displayRaw();
		else{
			String[] resu = new String[lines.size()];
			for(int i = 0; i < resu.length; i++){
				resu[i] = (lines.get(i)).replace(csep, newsep);
			}
			return resu;
		}

	}

	/**
	 * Get all the lines in the table, properly aligned.
	 * @param pos array of flags, indicating how each column should be justified.
	 * The array must have as many columns as the table has. Each column can contain
	 * either AsciiTable.LEFT, AsciiTable.CENTER or AsciiTable.RIGHT<br>
	 * if the array contains ONE item, it will be used for every column.
	 * @return an array of the table lines, aligned and justified
	 */
	public String[] displayAligned(int[] pos){
		return align(pos, '\0');
	}

	/**
	 * Get all the lines in the table, properly aligned.
	 * @param pos array of flags, indicating how each column should be justified.
	 * The array must have as many columns as the table has. Each column can contain
	 * either AsciiTable.LEFT, AsciiTable.CENTER or AsciiTable.RIGHT<br>
	 * if the array contains ONE item, it will be used for every column.
	 * @param newsep separator to use, replacing the original one
	 * @return an array of the table lines, aligned and justified
	 */
	public String[] displayAligned(int[] pos, char newsep){
		if (newsep == csep)
			newsep = '\0';
		return align(pos, newsep);
	}

	/**
	 * Get the array of lines in which all the columns are aligned
	 * @param pos array of flags, indicating how each column should be justified.
	 * The array must have as many columns as the table has. Each column can contain
	 * either AsciiTable.LEFT, AsciiTable.CENTER or AsciiTable.RIGHT<br>
	 * if the array contains ONE item, it will be used for every column.
	 * @param newsep separator to use, replacing the original one (no replacement if '\0')
	 * @return an array of the table lines, aligned and justified
	 */
	private String[] align(int[] pos, char newsep){
		int nblines = lines.size();
		String[] result = new String[nblines];
		StringBuffer buf = new StringBuffer();
		int p0, p1, col, fldsize, colsize, n1, inserted;
		boolean inHeader = header;	// A header can contain several lines. The end is detected by a line
		// beginning by the separator char
		int uniqueJustif = pos.length == 1 ? pos[0] : -1;
		for(int i = 0; i < nblines; i++){
			buf.delete(0, buf.length());
			String line = lines.get(i);
			p0 = 0;
			col = 0;
			if (inHeader && line == null){
				// end of the header: create the separator line
				for(int k = 0; k < sizes.length; k++){
					if (k > 0)
						buf.append(csep);
					addHsep(buf, sizes[k]);
				}
				if (headerPostfix != null)
					buf.append(headerPostfix);
				inHeader = false;
			}else{
				for(col = 0; col < sizes.length; col++){
					if (col > 0)
						buf.append(sep);
					p1 = line.indexOf(sep, p0);
					if (p1 < 0)
						p1 = line.length();
					fldsize = p1 - p0;
					if (fldsize < 0)
						break;
					colsize = sizes[col];
					inserted = colsize - fldsize;
					if (inserted < 0)
						inserted = 0;
					int justif = inHeader ? CENTER : (uniqueJustif >= 0 ? uniqueJustif : pos[col]);
					switch(justif){
						case LEFT:
						default:
							buf.append(line.substring(p0, p1));
							addspaces(buf, inserted);
							break;
						case CENTER:
							n1 = (inserted) / 2;
							addspaces(buf, n1);
							buf.append(line.substring(p0, p1));
							addspaces(buf, inserted - n1);
							break;
						case RIGHT:
							addspaces(buf, inserted);
							buf.append(line.substring(p0, p1));
							break;
					}

					p0 = p1 + 1;
				}
			}
			result[i] = newsep != '\0' ? buf.toString().replace(csep, newsep) : buf.toString();
		}
		return result;
	}

	/**
	 * Add nb spaces to the stringbuffer
	 * @param buf StringBuffer to modify
	 * @param nb number of spaces to add
	 */
	private void addspaces(StringBuffer buf, int nb){
		while(nb > SPACES.length())
			SPACES = SPACES + SPACES;
		buf.append(SPACES.substring(0, nb));
	}

	/**
	 * Add horizontal separator chars to the stringbuffer
	 * @param buf StringBuffer to modify
	 * @param nb number of chars to add
	 */
	private void addHsep(StringBuffer buf, int nb){
		while(nb > HSEP.length())
			HSEP = HSEP + HSEP;
		buf.append(HSEP.substring(0, nb));
	}

	/**
	 * Display the whole table, with left alignement
	 * @return the table as a unique string
	 */
	@Override
	public String toString(){
		StringBuffer buf = new StringBuffer();
		String[] ids = displayAligned(new int[]{AsciiTable.LEFT});

		for(int i = 0; i < ids.length; i++){
			if (i > 0)
				buf.append("\n");
			buf.append(ids[i]);
		}

		return buf.toString();
	}
}
