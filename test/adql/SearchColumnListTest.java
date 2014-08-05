package adql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tap.metadata.TAPColumn;
import tap.metadata.TAPSchema;
import tap.metadata.TAPTable;
import tap.metadata.TAPTable.TableType;
import tap.metadata.TAPType;
import tap.metadata.TAPType.TAPDatatype;
import adql.db.DBColumn;
import adql.db.DBCommonColumn;
import adql.db.DBTable;
import adql.db.SearchColumnList;
import adql.db.exception.UnresolvedJoin;
import adql.parser.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.ADQLColumn;

public class SearchColumnListTest {

	public static void main(String[] args) throws ParseException{

		/* SET THE TABLES AND COLUMNS NEEDED FOR THE TEST */
		// Describe the available table:
		TAPTable tableA = new TAPTable("A", TableType.table, "NATURAL JOIN Test table", null);
		TAPTable tableB = new TAPTable("B", TableType.table, "NATURAL JOIN Test table", null);
		TAPTable tableC = new TAPTable("C", TableType.table, "NATURAL JOIN Test table", null);
		TAPTable tableD = new TAPTable("D", TableType.table, "NATURAL JOIN Test table", null);

		// Describe its columns:
		tableA.addColumn(new TAPColumn("id", new TAPType(TAPDatatype.VARCHAR), "Object ID"));
		tableA.addColumn(new TAPColumn("txta", new TAPType(TAPDatatype.VARCHAR), "Text of table A"));
		tableB.addColumn(new TAPColumn("id", new TAPType(TAPDatatype.VARCHAR), "Object ID"));
		tableB.addColumn(new TAPColumn("txtb", new TAPType(TAPDatatype.VARCHAR), "Text of table B"));
		tableC.addColumn(new TAPColumn("Id", new TAPType(TAPDatatype.VARCHAR), "Object ID"));
		tableC.addColumn(new TAPColumn("txta", new TAPType(TAPDatatype.VARCHAR), "Text of table A"));
		tableC.addColumn(new TAPColumn("txtc", new TAPType(TAPDatatype.VARCHAR), "Text of table C"));
		tableD.addColumn(new TAPColumn("id", new TAPType(TAPDatatype.VARCHAR), "Object ID"));
		tableD.addColumn(new TAPColumn("txta", new TAPType(TAPDatatype.VARCHAR), "Text of table A"));
		tableD.addColumn(new TAPColumn("txtd", new TAPType(TAPDatatype.VARCHAR), "Text of table D"));

		// List all available tables:
		TAPSchema schema = new TAPSchema("public");
		schema.addTable(tableA);
		schema.addTable(tableB);
		schema.addTable(tableC);
		schema.addTable(tableD);

		// Build the corresponding SearchColumnList:
		SearchColumnList listA = new SearchColumnList();
		for(DBColumn col : tableA)
			listA.add(col);
		SearchColumnList listB = new SearchColumnList();
		for(DBColumn col : tableB)
			listB.add(col);
		SearchColumnList listC = new SearchColumnList();
		for(DBColumn col : tableC)
			listC.add(col);
		SearchColumnList listD = new SearchColumnList();
		for(DBColumn col : tableD)
			listD.add(col);

		/* TEST OF NATURAL JOIN */
		System.out.println("### CROSS JOIN ###");
		SearchColumnList crossJoin = join(listA, listB, false, null);

		// DEBUG
		for(DBColumn dbCol : crossJoin){
			if (dbCol instanceof DBCommonColumn){
				System.out.print("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in ");
				Iterator<DBTable> it = ((DBCommonColumn)dbCol).getCoveredTables();
				DBTable table;
				while(it.hasNext()){
					table = it.next();
					System.out.print((table == null) ? "<NULL>" : table.getDBName() + ", ");
				}
				System.out.println(")");
			}else
				System.out.println("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getDBName()) + ")");
		}
		System.out.println();

		/* TEST OF NATURAL JOIN */
		System.out.println("### NATURAL JOIN ###");
		SearchColumnList join1 = join(listA, listB, true, null);
		SearchColumnList join2 = join(listC, listD, true, null);
		//SearchColumnList join3 = join(join1, join2, true, null);

		// DEBUG
		for(DBColumn dbCol : join2){
			if (dbCol instanceof DBCommonColumn){
				System.out.print("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in ");
				Iterator<DBTable> it = ((DBCommonColumn)dbCol).getCoveredTables();
				DBTable table;
				while(it.hasNext()){
					table = it.next();
					System.out.print((table == null) ? "<NULL>" : table.getDBName() + ", ");
				}
				System.out.println(")");
			}else
				System.out.println("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getDBName()) + ")");
		}
		System.out.println();

		/* TEST OF JOIN USING 1 */
		System.out.println("\n### USING JOIN 1 ###");
		ArrayList<ADQLColumn> usingList = new ArrayList<ADQLColumn>();
		usingList.add(new ADQLColumn("id"));
		SearchColumnList joinUsing1 = join(join1, join2, false, usingList);

		// DEBUG
		for(DBColumn dbCol : joinUsing1){
			if (dbCol instanceof DBCommonColumn){
				System.out.print("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in ");
				Iterator<DBTable> it = ((DBCommonColumn)dbCol).getCoveredTables();
				DBTable table;
				while(it.hasNext()){
					table = it.next();
					System.out.print((table == null) ? "<NULL>" : table.getDBName() + ", ");
				}
				System.out.println(")");
			}else
				System.out.println("\t- " + dbCol.getADQLName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getADQLName()) + " (= " + dbCol.getDBName() + " in " + ((dbCol.getTable() == null) ? "<NULL>" : dbCol.getTable().getDBName()) + ")");
		}
		System.out.println();

		/* TEST OF JOIN USING 1 *
			System.out.println("\n### USING JOIN 2 ###");
			usingList.clear();
			usingList.add(new TAPColumn("id"));
			SearchColumnList joinUsing2 = joinUsing(listA, join3, usingList);
			
		// DEBUG
			for(DBColumn dbCol : joinUsing2){
				System.out.println("\t- "+dbCol.getADQLName()+" in "+((dbCol.getTable()==null)?"<NULL>":dbCol.getTable().getADQLName())+" (= "+dbCol.getDBName()+" in "+((dbCol.getTable()==null)?"<NULL>":dbCol.getTable().getDBName())+")");
			}
			System.out.println();*/

	}

	public static final SearchColumnList join(final SearchColumnList leftList, final SearchColumnList rightList, final boolean natural, final ArrayList<ADQLColumn> usingList) throws UnresolvedJoin{

		SearchColumnList list = new SearchColumnList();
		/*SearchColumnList leftList = leftTable.getDBColumns();
		SearchColumnList rightList = rightTable.getDBColumns();*/

		/* 1. Figure out duplicated columns */
		HashMap<String,DBCommonColumn> mapDuplicated = new HashMap<String,DBCommonColumn>();
		// CASE: NATURAL
		if (natural){
			// Find duplicated items between the two lists and add one common column in mapDuplicated for each
			DBColumn rightCol;
			for(DBColumn leftCol : leftList){
				// search for at most one column with the same name in the RIGHT list
				// and throw an exception is there are several matches:
				rightCol = findAtMostOneColumn(leftCol.getADQLName(), (byte)0, rightList, false);
				// if there is one...
				if (rightCol != null){
					// ...check there is only one column with this name in the LEFT list,
					// and throw an exception if it is not the case:
					findExactlyOneColumn(leftCol.getADQLName(), (byte)0, leftList, true);
					// ...create a common column:
					mapDuplicated.put(leftCol.getADQLName().toLowerCase(), new DBCommonColumn(leftCol, rightCol));
				}
			}

		}
		// CASE: USING
		else if (usingList != null && !usingList.isEmpty()){
			// For each columns of usingList, check there is in each list exactly one matching column, and then, add it in mapDuplicated
			DBColumn leftCol, rightCol;
			for(ADQLColumn usingCol : usingList){
				// search for exactly one column with the same name in the LEFT list
				// and throw an exception if there is none, or if there are several matches:
				leftCol = findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), leftList, true);
				// idem in the RIGHT list:
				rightCol = findExactlyOneColumn(usingCol.getColumnName(), usingCol.getCaseSensitive(), rightList, false);
				// create a common column:
				mapDuplicated.put((usingCol.isCaseSensitive(IdentifierField.COLUMN) ? ("\"" + usingCol.getColumnName() + "\"") : usingCol.getColumnName().toLowerCase()), new DBCommonColumn(leftCol, rightCol));
			}

		}
		// CASE: NO DUPLICATION TO FIGURE OUT
		else{
			// Return the union of both lists:
			list.addAll(leftList);
			list.addAll(rightList);
			return list;
		}

		/* 2. Add all columns of the left list except the ones identified as duplications */
		addAllExcept(leftList, list, mapDuplicated);

		/* 3. Add all columns of the right list except the ones identified as duplications */
		addAllExcept(rightList, list, mapDuplicated);

		/* 4. Add all common columns of mapDuplicated */
		list.addAll(mapDuplicated.values());

		return list;

	}

	public final static void addAllExcept(final SearchColumnList itemsToAdd, final SearchColumnList target, final Map<String,DBCommonColumn> exception){
		for(DBColumn col : itemsToAdd){
			if (!exception.containsKey(col.getADQLName().toLowerCase()) && !exception.containsKey("\"" + col.getADQLName() + "\""))
				target.add(col);
		}
	}

	public final static DBColumn findExactlyOneColumn(final String columnName, final byte caseSensitive, final SearchColumnList list, final boolean leftList) throws UnresolvedJoin{
		DBColumn result = findAtMostOneColumn(columnName, caseSensitive, list, leftList);
		if (result == null)
			throw new UnresolvedJoin("Column \"" + columnName + "\" specified in USING clause does not exist in " + (leftList ? "left" : "right") + " table!");
		else
			return result;
	}

	public final static DBColumn findAtMostOneColumn(final String columnName, final byte caseSensitive, final SearchColumnList list, final boolean leftList) throws UnresolvedJoin{
		ArrayList<DBColumn> result = list.search(null, null, null, columnName, caseSensitive);
		if (result.isEmpty())
			return null;
		else if (result.size() > 1)
			throw new UnresolvedJoin("Common column name \"" + columnName + "\" appears more than once in " + (leftList ? "left" : "right") + " table!");
		else
			return result.get(0);
	}

	/**
	 * Tells whether the given column is a common column (that's to say, a unification of several columns of the same name).
	 * 
	 * @param col	A DBColumn.
	 * @return		true if the given column is a common column, false otherwise (particularly if col = null).
	 */
	public static final boolean isCommonColumn(final DBColumn col){
		return (col != null && col instanceof DBCommonColumn);
	}

}
