package adql.query;

/*
 * This file is part of ADQLLibrary.
 * 
 * ADQLLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ADQLLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ADQLLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012-2015 - UDS/Centre de Données astronomiques de Strasbourg (CDS),
 *                       Astronomisches Rechen Institute (ARI)
 */

import adql.parser.Token;

/**
 * Indicates a simple position or a token/string position in a text.
 * It is particularly used to localize columns and tables in the original ADQL query.
 * 
 * @author Gr&eacute;gory Mantelet (CDS;ARI)
 * @version 1.4 (06/2015)
 */
public class TextPosition {

	public final int beginLine;
	public final int beginColumn;

	public final int endLine;
	public final int endColumn;

	/**
	 * Build an unknown position (all fields = -1).
	 */
	public TextPosition(){
		beginLine = beginColumn = endLine = endColumn = -1;
	}

	/**
	 * Builds a position whose the end line and column are unknown => a simple position.
	 * 
	 * @param line		Begin line.
	 * @param column	Begin column.
	 */
	public TextPosition(final int line, final int column){
		beginLine = (line < 0) ? -1 : line;
		beginColumn = (column < 0) ? -1 : column;
		endLine = endColumn = -1;
	}

	/**
	 * Builds a position => a full position (a region in the text).
	 * 
	 * @param beginLine		Begin line.
	 * @param beginColumn	Begin column.
	 * @param endLine		End line.
	 * @param endColumn		End column.
	 */
	public TextPosition(final int beginLine, final int beginColumn, final int endLine, final int endColumn){
		this.beginLine = (beginLine < 0) ? -1 : beginLine;
		this.beginColumn = (beginColumn < 0) ? -1 : beginColumn;
		this.endLine = (endLine < 0) ? -1 : endLine;
		this.endColumn = (endColumn < 0) ? -1 : endColumn;
	}

	/**
	 * Builds a position defining the region delimited by the given token.
	 * 
	 * @param token	The position will be the one of this token.
	 */
	public TextPosition(final Token token){
		this(token.beginLine, token.beginColumn, token.endLine, (token.endColumn < 0) ? -1 : (token.endColumn + 1));
	}

	/**
	 * Builds a position => a full position (a region in the text).
	 * 
	 * @param beginToken	Begin position.
	 * @param endToken		End position.
	 */
	public TextPosition(final Token beginToken, final Token endToken){
		this(beginToken.beginLine, beginToken.beginColumn, endToken.endLine, (endToken.endColumn < 0) ? -1 : (endToken.endColumn + 1));
	}

	/**
	 * Builds a copy of the given position.
	 * 
	 * @param positionToCopy	Position to copy.
	 * @since 1.4
	 */
	public TextPosition(final TextPosition positionToCopy){
		this(positionToCopy.beginLine, positionToCopy.beginColumn, positionToCopy.endLine, positionToCopy.endColumn);
	}

	/**
	 * Builds a position whose the start is the start position of the first parameter and the end is the end position of the second parameter.
	 * 
	 * @param startPos	Start position (only beginLine and beginColumn will be used).
	 * @param endPos	End position (only endLine and endColumn will be used).
	 * @since 1.4
	 */
	public TextPosition(final TextPosition startPos, final TextPosition endPos){
		this(startPos.beginLine, startPos.beginColumn, endPos.endLine, endPos.endColumn);
	}

	@Override
	public String toString(){
		if (beginLine == -1 && beginColumn == -1)
			return "[l.? c.?]";
		else if (endLine == -1 && endColumn == -1)
			return "[l." + beginLine + " c." + beginColumn + "]";
		else
			return "[l." + beginLine + " c." + beginColumn + " - l." + endLine + " c." + endColumn + "]";
	}

}
