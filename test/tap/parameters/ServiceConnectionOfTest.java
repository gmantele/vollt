package tap.parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import tap.ServiceConnection;
import tap.TAPFactory;
import tap.TAPJob;
import tap.formatter.FITSFormat;
import tap.formatter.OutputFormat;
import tap.formatter.SVFormat;
import tap.formatter.VOTableFormat;
import tap.log.TAPLog;
import tap.metadata.TAPMetadata;
import uws.service.UserIdentifier;
import uws.service.file.UWSFileManager;
import adql.db.FunctionDef;

public class ServiceConnectionOfTest implements ServiceConnection {

	private int[] retentionPeriod = new int[]{-1,-1};
	private int[] executionDuration = new int[]{(int)TAPJob.UNLIMITED_DURATION,(int)TAPJob.UNLIMITED_DURATION};
	private int[] outputLimit = new int[]{TAPJob.UNLIMITED_MAX_REC,TAPJob.UNLIMITED_MAX_REC};
	private List<OutputFormat> outputFormats = Arrays.asList(new OutputFormat[]{new VOTableFormat(this),new SVFormat(this, SVFormat.COMMA_SEPARATOR),new FITSFormat(this)});

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
		return null;
	}

	@Override
	public int[] getRetentionPeriod(){
		return retentionPeriod;
	}

	public void setRetentionPeriod(final int defaultVal, final int maxVal){
		retentionPeriod[0] = defaultVal;
		retentionPeriod[1] = maxVal;
	}

	@Override
	public int[] getExecutionDuration(){
		return executionDuration;
	}

	public void setExecutionDuration(final int defaultVal, final int maxVal){
		executionDuration[0] = defaultVal;
		executionDuration[1] = maxVal;
	}

	@Override
	public int[] getOutputLimit(){
		return outputLimit;
	}

	public void setOutputLimit(final int defaultVal, final int maxVal){
		outputLimit[0] = defaultVal;
		outputLimit[1] = maxVal;
	}

	@Override
	public LimitUnit[] getOutputLimitType(){
		return new LimitUnit[]{LimitUnit.rows,LimitUnit.rows};
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
		return null;
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
	public int getNbMaxAsyncJobs(){
		return 0;
	}

	@Override
	public TAPLog getLogger(){
		return null;
	}

	@Override
	public TAPFactory getFactory(){
		return null;
	}

	@Override
	public UWSFileManager getFileManager(){
		return null;
	}

	@Override
	public Iterator<OutputFormat> getOutputFormats(){
		return outputFormats.iterator();
	}

	@Override
	public OutputFormat getOutputFormat(String mimeOrAlias){
		for(OutputFormat f : outputFormats)
			if (f.getMimeType().equalsIgnoreCase(mimeOrAlias) || f.getShortMimeType().equalsIgnoreCase(mimeOrAlias))
				return f;
		return null;
	}

}