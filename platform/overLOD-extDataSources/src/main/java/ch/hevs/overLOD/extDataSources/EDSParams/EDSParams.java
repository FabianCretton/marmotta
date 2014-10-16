package ch.hevs.overLOD.extDataSources.EDSParams;

/**
 * External Data Source parameters
 * Information about one external datasource 
 * 
 * @author Fabian Cretton
 * 
 */

public class EDSParams {
	public String EDSType ;
	public String contentType ;
	public String url ;
	public String context ;
	public String timeStamp ; // the specific value of timeStamp is not defined yet, so save it as a string currently 
	
	public EDSParams() // needed for Jackson read/write operations
	{
	}

	public EDSParams(String EDSType, String contentType, String url, String context, String timeStamp)
	{
		this.EDSType = EDSType ;
		this.contentType = contentType ;
		this.url = url ;
		this.context = context ;
		this.timeStamp = timeStamp ;
	}

}
