package adql.query.operand.function.conditional;

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
 * Copyright 2022 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import adql.parser.feature.LanguageFeature;
import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.query.operand.function.ADQLFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

/**
 * Object representation of the COALESCE function of ADQL.
 *
 * <p>This function returns the first non-null argument.</p>
 *
 * <p><i><b>Example:</b>
 *  <code>COALESCE(utype, 'none')</code>
 * </i></p>
 *
 * @author Gr&eacute;gory Mantelet (CDS)
 * @version 2.0 (10/2022)
 * @since 2.0
 */
public class CoalesceFunction extends ADQLFunction {

    /** Description of this ADQL Feature. */
    public static final LanguageFeature FEATURE = new LanguageFeature(LanguageFeature.TYPE_ADQL_CONDITIONAL, "COALESCE", true, "Return the first of its arguments that is not NULL.");

    /** Constant name of this function. */
    protected final String FCT_NAME = "COALESCE";

    /** All first values (at least one value). */
    protected ADQLOperand[] values;

    public CoalesceFunction(final ADQLOperand[] operands) throws Exception {
        // Create a new array of the same size:
        values = new ADQLOperand[operands.length];

        // Append each item as a new operand (which will check it):
        for(int i=0; i< operands.length; i++)
            setParameter(i, operands[i]);
    }

    public CoalesceFunction(final Collection<ADQLOperand> operands) throws Exception {
        // Create a new array of the same size:
        values = new ADQLOperand[operands.size()];

        // Append each item as a new operand (which will check it):
        int i = 0;
        for(ADQLOperand op : operands)
            setParameter(i++, op);
    }

    @Override
    public String getName() {
        return FCT_NAME;
    }

    @Override
    public LanguageFeature getFeatureDescription() {
        return FEATURE;
    }

    @Override
    public ADQLObject getCopy() throws Exception {
        return new CoalesceFunction(values);
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public boolean isGeometry() {
        return true;
    }

    @Override
    public int getNbParameters() {
        return values.length;
    }

    @Override
    public ADQLOperand[] getParameters() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public ADQLOperand getParameter(int index) throws ArrayIndexOutOfBoundsException {
        if (index > values.length)
            throw new ArrayIndexOutOfBoundsException("No more than "+values.length+" parameters to this "+getName()+" function!");
        return values[index];
    }

    @Override
    public ADQLOperand setParameter(final int index, final ADQLOperand replacer) throws ArrayIndexOutOfBoundsException, NullPointerException, Exception {
        ADQLOperand replaced = null;

        // Check index:
        if (index < 0 || index >= values.length)
            throw new ArrayIndexOutOfBoundsException("No " + index + "-th parameter for the function \""+getName()+"\" ("+toADQL()+")!");

        // If replacer is NULL, delete the corresponding parameters:
        if (replacer == null){
            // ...but only if there is more than one parameter:
            if (values.length == 1)
                throw new NullPointerException("Impossible to remove the only parameter of \"" + getName() + "\" (" + toADQL() + ")!");
            else{
                // create the new array of parameters:
                final ADQLOperand[] newValues = new ADQLOperand[values.length-1];
                // copy all parameters before the one to delete:
                for(int i=0; i<index; i++)
                    newValues[i] = values[i];
                // remember about the dead parameter:
                replaced = values[index];
                // copy all parameters after the one to delete:
                for(int i=index+1; i<values.length; i++)
                    newValues[i-1] = values[i];
                // replace the former list of parameters by the new one:
                values = newValues;
            }
        }
        // Otherwise, just replace the specified value by the given one:
        else {
            replaced = values[index];
            values[index] = replacer;
        }

        /* Set no position (at it does not correspond anymore to an ADQL query
         * string): */
        setPosition(null);

        // Return the replaced value/parameter:
        return replaced;
    }
}
