package vollt.datatype;

public interface DataTypeFactory {

    DataType fromVOTable(final String datatype, final String arraysize, final String xtype);

    static DataTypeFactory getDefault(){ return DataTypeFactoryImp.DEFAULT_INSTANCE; }

}