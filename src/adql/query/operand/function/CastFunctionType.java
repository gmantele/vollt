/**
 * 
 */
package adql.query.operand.function;

import adql.parser.Token;

/**
 * @author Zarquan
 *
 */
public enum CastFunctionType
	{
	SHORT,
	SMALLINT,
	INT,
	INTEGER,
	BIGINT,
	LONG,
	FLOAT,
	DOUBLE
	;
	
	public static CastFunctionType type(final Token token)
		{
		return valueOf(
			token.image.toUpperCase()
			);
		}
	}
