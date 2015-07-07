/**
 * 
 */
package adql.query.operand.function;

import adql.parser.Token;
import adql.query.ADQLObject;
import adql.query.operand.ADQLColumn;
import adql.query.operand.ADQLOperand;

/**
 * @author Zarquan
 *
 */
public class CastFunction
extends ADQLFunction
	{

	private CastFunctionType type ;
	public CastFunctionType type()
		{
		return this.type ;
		}

	private ADQLOperand oper;
	public ADQLOperand oper()
		{
		return this.oper;
		}
	
	/**
	 * Public constructor.
	 * 
	 */
	public CastFunction(final Token type, final ADQLOperand oper)
		{
		this(
			CastFunctionType.type(
				type
				),
			oper
			);
		}

	/**
	 * Public constructor.
	 * 
	 */
	public CastFunction(final CastFunctionType type, final ADQLOperand oper)
		{
		this.type = type ;
		this.oper = oper ;
		}
	
	/**
	 * Public constructor.
	 * 
	 */
	public CastFunction(final CastFunction that)
	throws Exception 
		{
		this.type = that.type();
		this.oper = (ADQLOperand) that.oper().getCopy();
		}
	
	@Override
	public boolean isNumeric()
		{
		switch(this.type)
			{
			case SHORT:
			case SMALLINT:
			case INT:
			case INTEGER:
			case LONG:
			case BIGINT:
			case FLOAT:
			case DOUBLE:
				return true ;
			default :
				return false ;
			}
		}

	@Override
	public boolean isString()
		{
		switch(this.type)
			{
			default :
				return false ;
			}
		}

	@Override
	public String getName()
		{
		if (oper instanceof ADQLColumn)
			{
			return oper.getName();
			}
		else {
			return "CASTED";
			}
		}

	@Override
	public ADQLObject getCopy()
	throws Exception
		{
		return new CastFunction(
			this
			);
		}

	@Override
	public int getNbParameters()
		{
		return 1;
		}

	@Override
	public ADQLOperand[] getParameters()
		{
		if (this.oper != null)
			{
			return new ADQLOperand[]{
				this.oper
				};
			}
		else {
			return new ADQLOperand[0];
			}
		}

	@Override
	public ADQLOperand getParameter(final int index)
	throws ArrayIndexOutOfBoundsException
		{
		switch(index)
			{
			case 0:
				return this.oper;
			default :
				throw new ArrayIndexOutOfBoundsException();
			}
		}

	@Override
	public ADQLOperand setParameter(final int index, final ADQLOperand next)
	throws ArrayIndexOutOfBoundsException
		{
		switch(index)
			{
			case 0:
				final ADQLOperand prev = this.oper ;
				this.oper = next;
				return prev;
			default :
				throw new ArrayIndexOutOfBoundsException();
			}
		}

	@Override
	public boolean isGeometry() {
		// TODO Auto-generated method stub
		return false;
	}
	}
