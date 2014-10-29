package ch.hevs.overLOD.extDataSources.EDSParams;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

/**
 * When a JAX-RS web service needs to return a list of string, this list must be encapsulated in an object
 * to be returned by Response.ok().entity(aStringListForWSReturn).build()
 *  
 * @author Fabian Cretton, HES-SO OverLOD surfer project
 */
public class StringListForWSReturn {
	public List<String> stringList = new ArrayList<String>() ; 
}
