package adql.query.operand.function;


import adql.query.ADQLObject;
import adql.query.operand.ADQLOperand;
import adql.translator.ADQLTranslator;
import adql.translator.TranslationException;

public class ADQLTrimFunction extends UserDefinedFunction {
        private ADQLOperand[] operand;
        
        public ADQLTrimFunction() {
            this.operand = new ADQLOperand[1];
        }
        public ADQLTrimFunction(ADQLOperand operand) throws Exception {
            this.operand = new ADQLOperand[1];
            this.operand[0] = operand;
            this.setParameter(0, operand);
        }
        
        @Override
        public boolean isNumeric() {
            return false;
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public ADQLObject getCopy() throws Exception {
            return null;
        }

        @Override
        public String getName() {
            return "trim";
        }

        @Override
        public int getNbParameters() {
            return 1;
        }

        @Override
        public ADQLOperand getParameter(int arg0)
                throws ArrayIndexOutOfBoundsException {
            return this.operand[arg0];
        }

        @Override
        public ADQLOperand[] getParameters() {
            return this.operand;
        }

        @Override
        public ADQLOperand setParameter(int arg0, ADQLOperand arg1)
                throws ArrayIndexOutOfBoundsException, NullPointerException,
                Exception {
            if (arg0 == 0)  
                this.operand[0] = arg1;
            else throw new Exception("bad number of parameters");
                
            return null;
        }
        @Override
        public boolean isGeometry() {
            return false;
        }
        
        @Override
        public String translate(ADQLTranslator caller)
                throws TranslationException {
            System.err.println("TRIM function!!!!!!!");
            return caller.toString();
        }

}
