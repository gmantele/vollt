package tap.auth;
/*
 * This file is part of UWSLibrary.
 * 
 * UWSLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UWSLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with UWSLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import adql.parser.ADQLParser;
import adql.parser.ParseException;
import adql.db.DBChecker;
import adql.db.exception.UnresolvedTableException;
import adql.db.exception.UnresolvedIdentifiersException;

import tap.TAPJob;
import tap.parameters.TAPParameters;
import tap.metadata.TAPTable;
import tap.metadata.TAPSchema;

import uws.job.user.JobOwner;
import uws.job.UWSJob;
import uws.job.JobList;
import uws.job.user.DefaultJobOwner;

/**
 * <p>A Job Owner who represents an authenticated user and has restricted access to specific 
 * components/tables of the TAP service.
 * To be constructed by {@link tap.auth.ConfigurableAuthUserIdentifier}</p>
 *
 * <p>For now the main thing restricted between users is private datasets. This class will store the 
 * information regarding what tables are allowed them.</p>
 *
 * <p>Inherits many methods from {@link DefaultJobOwner}, such as the various getters and checks on 
 * if a user can read/write a given job, as the requirements for those remain the same. </p>
 *
 * @author Anthony Heng (AAO)
 * @version 04/2025
 *
 * @see uws.service.UserIdentifier
 * @see uws.job.user.JobOwner
 * @see uws.job.user.DefaultJobOwner;
 */
public class AuthJobOwner extends DefaultJobOwner {

	protected LinkedHashMap<String, TAPSchema> allowedData;

	/**
	 * Builds a Job Owner which has the given ID.
	 * Its pseudo will also be equal to the given ID.
	 *
	 * @param name	ID/Pseudo of the Job Owner to create.
	 * @param allowedDataList List of allowed data this jobowner is allowed to access, laid out like
	 * a list of Schemas
	 */
	public AuthJobOwner(final String name, List<TAPSchema> allowedDataList){
		this(name, name, allowedDataList);
	}

	public AuthJobOwner(final String id, final String pseudo, List<TAPSchema> allowedDataList){
		super(id, pseudo);
		// Load Schemas into the linked list
		allowedData = new LinkedHashMap<String, TAPSchema>();
		for (TAPSchema schemaEntry : allowedDataList){
			allowedData.put(schemaEntry.getRawName(), schemaEntry);
		}

	}

	/**
	 * Only have read permissions if this AuthJobOwner has a job in the list
	 * @see uws.job.user.JobOwner#hasReadPermission(uws.job.JobList)
	 */
	@Override
	public boolean hasReadPermission(JobList jl){
		//TODO: Any way to restrict job lists to one per user?
		return jl.getNbJobs(this)>0;
	}

	/**
	 * Authenticated Job Owners only have write permissions to a job list if they own a job in the list
	 * @see uws.job.user.JobOwner#hasWritePermission(uws.job.JobList)
	 */
	// TODO: If this is uncommented then the user doesn't have any way to enter any batch jobs
	// 		 At least the user can't read other jobs? Theres probably a better way to 
	// 		 restrict users to joblists anyway.
	// @Override
	// public boolean hasWritePermission(JobList jl){
	// 	// TODO: Any way to restrict job lists to one per user?
	// 	return jl.getNbJobs(this)>0;
	// }

	/**
	 * {@inheritDoc}
	 *
	 * For authenticated job owners, a job can be executed if the owner owns the job, and the
	 * resources (e.g. tables, schemas) they are trying to access are allowed by them.
	 */
	@Override
	public boolean hasExecutePermission(UWSJob job) {
		boolean nullCheck =  (job == null) || (job.getOwner() == null);
		boolean isOwner = job.getOwner().equals(this);

		if (job instanceof TAPJob){
			try {
				boolean tapJobAllowed = TAPParamsAllowed(((TAPJob) job).getTapParams(), false);
				return (nullCheck||isOwner) && tapJobAllowed;
			} catch (ParseException e){
				// Cannot run this job due to malformed query
				return true; // Let it run. It'll get caught further down the line during execution and produce an informative exception
							 // Can't do it here as JobOwner.hasExecutePermission() does not normally throw checked exceptions
			}
		} else {
			return (nullCheck||isOwner);
		}
	}

	/**
	 * Checks if a given TAPJob is allowed to be run by the owner. Authenticated users will be allowed to
	 * @param  job TAPJob to check against
	 * @param  throwParseException true to throw a parse exception, then insead of false throw a ParseException
	 * @return     <code>true</code> if the this JobOwner meets all requirements for running the job <code>false</code> otherwise.
	 *
	 * @throws ParseException If the query is malformed
	 */
	public boolean TAPParamsAllowed(TAPParameters tapParams, boolean throwParseException) throws ParseException {
		if (tapParams.getRequest().equals(TAPJob.REQUEST_DO_QUERY)){
			// Build collection of TAPTables
	        ArrayList<TAPTable> allowedTables = new ArrayList<>();
	        for (TAPSchema userSchema : this.allowedData.values())
	        	userSchema.forEach(allowedTables::add); // Add those tables to the array list of tables
	        											// Note: this works as TAPSchemas implements Iterable<TAPTable>
	        DBChecker allowedTableChecker = new DBChecker(allowedTables);
	        // Build ADQL Parser with new checker
	        ADQLParser adqlParse = new ADQLParser(allowedTableChecker);
	        // Parse the query from the request
	        String queryString = tapParams.getQuery();

	        // Parse the query for the sake of getting checked by allowedTableChecker.
	        // No need to store the result as ADQLExecutor will do that later
	        try{
	        	adqlParse.parseQuery(queryString);
	        } catch(UnresolvedTableException ute){
	        	// If a parse runs into this exception, then it is not on the list of allowed tables. Return false
	        	if (throwParseException){
	        		throw new ParseException(ute.getMessage());
	        	} else{
		        	return false;
		        }
	        } catch(UnresolvedIdentifiersException e){
	        	// This is also a possibility for not being on the list of allowed tables:
	        	// if the parse does not find the table, unknown tables are reported as "Unknown table". in a UnresolvedIdentifiersException.
	        	if (throwParseException){
	        		throw new ParseException(e.getMessage());
	        	} else{
		        	return false;
		        }
	        }
	        // parsed without any issues from the DBChecker, continue on to return true
	    }
	    return true;
	}

	/**
	 * Returns the list of data the user is allowed to query (i.e. the schemas)
	 * @return List of TAPSchemas representing which tables the user is allowed to access
	 */
	public LinkedHashMap<String, TAPSchema> getallowedData(){
		return allowedData;
	}


	/**
	 * Checks if table <code>t</code> is accessible by the user. The table must have both a matching
	 * schema and matching name within the user's list of allowed data.
	 * @param  t  table to check
	 *
	 * @return true or false if the user has access to table t
	 */
	public boolean canAccessTable(TAPTable t){
		// Find the schema of the table, ensure get table does not return null
		if (t == null)
			return false;
		TAPSchema searchSchema = allowedData.get(t.getSchema().getADQLName());
		if (searchSchema != null){ // schema found
			// Search for the table within the schema
			TAPTable searchTable = searchSchema.getTable(t.getADQLName());
			if (searchTable != null) // Final null check. If fails then will move out and return false
				return (searchTable.getFullName().equals(t.getFullName())); // Check name match
		}
		return false; // All if checks failed. Schema not found, cannot access
	}

	/**
	 * Checks if Schema <code>s</code> is accessible by the user. It should be if the schema is in the user's list of allowed data
	 * @param  s  schema to check
	 *
	 * @return true or false if the user has access to the schema
	 */
	public boolean canAccessSchema(TAPSchema s){
		return (allowedData.get(s.getRawName()) != null);
	}
}