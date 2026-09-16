package vollt.type.column;

public class IncorrectDataTypeClass extends RuntimeException {
    public IncorrectDataTypeClass() {
        super();
    }

    public IncorrectDataTypeClass(String s) {
        super(s);
    }

    public IncorrectDataTypeClass(String s, Throwable throwable) {
        super(s, throwable);
    }

    public IncorrectDataTypeClass(Throwable throwable) {
        super(throwable);
    }
}
