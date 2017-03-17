package tap.formatter;

import java.util.Collection;
import java.util.Iterator;

import adql.db.FunctionDef;
import tap.ServiceConnection;
import tap.TAPFactory;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UserIdentifier;
import uws.service.file.UWSFileManager;

public class ServiceConnection4Test implements ServiceConnection {

	private TAPMetadata metadata = null;
	private TAPFactory factory = null;
	private UWSFileManager fileManager = null;

	public ServiceConnection4Test(){}

	public ServiceConnection4Test(final TAPMetadata metadata, final UWSFileManager fileManager){
		this.metadata = metadata;
		this.fileManager = fileManager;
	}

	@Override
	public int[] getOutputLimit(){
		return new int[]{1000000,1000000};
	}

	@Override
	public LimitUnit[] getOutputLimitType(){
		return new LimitUnit[]{LimitUnit.bytes,LimitUnit.bytes};
	}

	@Override
	public String getProviderName(){
		return null;
	}

	@Override
	public String getProviderDescription(){
		return null;
	}

	@Override
	public boolean isAvailable(){
		return true;
	}

	@Override
	public String getAvailability(){
		return "AVAILABLE";
	}

	@Override
	public int[] getRetentionPeriod(){
		return null;
	}

	@Override
	public int[] getExecutionDuration(){
		return null;
	}

	@Override
	public UserIdentifier getUserIdentifier(){
		return null;
	}

	@Override
	public boolean uploadEnabled(){
		return false;
	}

	@Override
	public int[] getUploadLimit(){
		return null;
	}

	@Override
	public LimitUnit[] getUploadLimitType(){
		return null;
	}

	@Override
	public int getMaxUploadSize(){
		return 0;
	}

	@Override
	public TAPMetadata getTAPMetadata(){
		return metadata;
	}

	@Override
	public Collection<String> getCoordinateSystems(){
		return null;
	}

	@Override
	public Collection<String> getGeometries(){
		return null;
	}

	@Override
	public Collection<FunctionDef> getUDFs(){
		return null;
	}

	@Override
	public TAPLog getLogger(){
		return null;
	}

	@Override
	public TAPFactory getFactory(){
		return factory;
	}

	public void setFactory(TAPFactory factory){
		this.factory = factory;
	}

	@Override
	public UWSFileManager getFileManager(){
		return fileManager;
	}

	@Override
	public Iterator<OutputFormat> getOutputFormats(){
		return null;
	}

	@Override
	public OutputFormat getOutputFormat(String mimeOrAlias){
		return null;
	}

	@Override
	public int getNbMaxAsyncJobs(){
		return -1;
	}

	@Override
	public void setAvailable(boolean isAvailable, String message){}

	@Override
	public int[] getFetchSize(){
		return null;
	}

}