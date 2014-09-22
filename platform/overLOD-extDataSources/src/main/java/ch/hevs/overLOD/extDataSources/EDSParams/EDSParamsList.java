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
	public TreeMap<String,EDSParams> EDSParamsSortedMap = new TreeMap<String,EDSParams>();
	
	public EDSParamsList() // needed for Jackson read/write operations
	{
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
}
