OverLOD Surfer Modules Distribution
===================================

It is possible to simply test the OverLOD modules by adding them to a standard Marmotta 3.3.0 installation.

In order to do that, copy those files to the server's "WEB-INF\lib" for instance. 

Those .jar files are:
* 2 .jar files of the 2 modules 
* dependencies for the 2 modules
* the updated version of "ldclient-provider-rdf-3.3.0.jar" in order to handle RDF files as External Data Sources (see about.html of the module for more information)

When starting Marmotta and accessing the modules, two new folders will be created by the modules in your %Marmotta-home% folder, namely "dataViews" to store your SPARQL queries and "EDS" to store your data filters as well as data validators. Some example files being given and ready to use, according to the examples given on the module's about.html.

(So far we did test the installation only on windows systems)