package ch.hevs.overLOD.extDataSources.EDSParams;

import java.util.Map;
import java.util.TreeMap;


/**
 * Liste of all External Data Source parameters
 * Keeping an ordered map of EDSParms, the key being the context.
 * 
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 * 
 */
public class EDSParamsList {

	//public Map<String,EDSParams> EDSParamsSortedMap = new TreeMap<String,EDSParams>();
	private TreeMap<String,EDSParams> EDSParamsSortedMap = new TreeMap<String,EDSParams>();
	
	public EDSParamsList() // needed for Jackson read/write operations
	{
	}

	// test if an EDSParams exist for that context
	public boolean exists(String context)
	{
		return EDSParamsSortedMap.containsKey(context) ;
	}
	
	public TreeMap<String,EDSParams> getList()
	{
		return EDSParamsSortedMap ; 
	}
	
	/**
	 * Adding one aEDSParams to the sorted map
	 * if an entry already exists, remove that old version first
	 * 
	 * @param aEDSParams
	 */
	public void addEDSParams(EDSParams aEDSParams)
	{
	if (EDSParamsSortedMap.containsKey(aEDSParams.context))
		EDSParamsSortedMap.remove(aEDSParams.context) ;
	
	EDSParamsSortedMap.put(aEDSParams.context, aEDSParams) ;
	}
	
	public EDSParams get(String context)
	{
		if (EDSParamsSortedMap.containsKey(context))
			return EDSParamsSortedMap.get(context) ;
		
		return null ;
	}
	/**
	 * Deleting one aEDSParams identified by a 'context'
	 * 
	 * @param aEDSParams
	 * @return true if deleted, false if not (meaning that the specified context was not found in the list)
	 */
	public boolean deleteEDSParams(String EDSContext)
	{
	if (EDSParamsSortedMap.containsKey(EDSContext))
		{
		EDSParamsSortedMap.remove(EDSContext) ;
		return true;
		}
	else
		return false ;
	}
	
	/**
    * Set a new value for the timeStamp of an EDS
    * @param context  the context (Named Graph) where this EDS is saved locally, which is its identifier
    * @param timeStamp a string representing the timeStamp
    * @return true/false whether the value has been saved or not
	 */
	public boolean setEDSParamsTimeStamp(String context, String timeStamp)
	{
		if (EDSParamsSortedMap.containsKey(context))
		{
			((EDSParams)EDSParamsSortedMap.get(context)).timeStamp = timeStamp ;
			return true;
		}
	else
		return false ;
	}
}
