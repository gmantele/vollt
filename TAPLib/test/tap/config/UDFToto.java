package tap.config;

import adql.db.DBType;
import adql.db.FunctionDef;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.query.operand.StringConstant;
import adql.query.operand.function.UserDefinedFunction;
import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

/**
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version (11 / 2023)
 */
public class UDFToto extends UserDefinedFunction {

    protected StringConstant fakeParam;

    public UDFToto(final ADQLOperand[] params) throws Exception {
        if (params == null || params.length == 0)
            throw new Exception("Missing parameter for the user defined function \"toto\"!");
        else if (params.length > 1)
            throw new Exception("Too many parameters for the function \"toto\"! Only one is required.");
        else if (!(params[0] instanceof StringConstant))
            throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
        fakeParam = (StringConstant)params[0];
        functionName = "toto";
        languageFeature = (new FunctionDef(getName(), new DBType(DBType.DBDatatype.VARCHAR), new FunctionDef.FunctionParam[]{ new FunctionDef.FunctionParam("txt", new DBType(DBType.DBDatatype.VARCHAR)) })).toLanguageFeature();
    }

    @Override
    public final boolean isNumeric() {
        return false;
    }

    @Override
    public final boolean isString() {
        return true;
    }

    @Override
    public final boolean isGeometry() {
        return false;
    }

    @Override
    public ADQLObject getCopy() throws Exception {
        ADQLOperand[] params = new ADQLOperand[]{ (StringConstant)fakeParam.getCopy() };
        return new UDFToto(params);
    }

    @Override
    public final ADQLOperand[] getParameters() {
        return new ADQLOperand[]{ fakeParam };
    }

    @Override
    public final int getNbParameters() {
        return 1;
    }

    @Override
    public final ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
        if (index != 0)
            throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
        return fakeParam;
    }

    @Override
    public ADQLOperand setParameter(int index, ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
        if (index != 0)
            throw new ArrayIndexOutOfBoundsException("Incorrect parameter index: " + index + "! The function \"toto\" has only one parameter.");
        else if (!(replacer instanceof StringConstant))
            throw new Exception("Wrong parameter type! The parameter of the UDF \"toto\" must be a string constant.");
        return (fakeParam = (StringConstant)replacer);
    }

    @Override
    public String translate(final ADQLTranslator caller) throws TranslationException {
        /* Note: Since this function is totally fake, this function will be replaced in SQL by its parameter (the string). */
        return caller.translate(fakeParam);
    }
}
