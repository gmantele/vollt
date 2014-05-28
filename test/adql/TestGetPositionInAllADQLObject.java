package adql;

import java.util.Iterator;

import adql.parser.ADQLParser;
import adql.query.ADQLObject;
import adql.query.ADQLQuery;
import adql.search.SimpleSearchHandler;

public class TestGetPositionInAllADQLObject {

	public static void main(String[] args) throws Throwable{
		ADQLParser parser = new ADQLParser();
		ADQLQuery query = parser.parseQuery("SELECT truc, bidule.machin FROM foo JOIN bidule USING(id) WHERE truc > 12.5 AND bidule.machin < 5");

		System.out.println("\nOBJECT WITH NO DEFINED POSITION:");
		Iterator<ADQLObject> results = query.search(new SimpleSearchHandler(true){
			@Override
			protected boolean match(ADQLObject obj){
				return obj.getPosition() == null;
			}
		});
		while(results.hasNext())
			System.out.println("    * " + results.next().toADQL());
	}

}
