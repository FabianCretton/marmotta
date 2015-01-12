OverLOD Surfer Modules for Apache Marmotta
==========================================

OverLOD Surfer Introduction
---------------------------

The primary goal of OverLOD Surfer is to enable the management of a server of RDF data, in a distributed data environment, making data available for the specific front-end applications. Programmers of the applications don't need to know about RDF.

When discovering about Apache Marmotta, building OverLOD on top of it became an obvious choice as many functionalities already exist in Marmotta, and other ones could be developped as Marmotta's modules.

Administrators choose which data to store locally, for efficiency as well as data control purpose. This architecture needs to handle:
- automatic update of the local data when the original data is updated 
- possibility to import only a subset of the original data 
- perform data validation before the import 
This is handled by the "External Data Sources" module.  

Administrators then design SPARQL queries that programmers of the front-end applications can simply call by their names to get data in format they are familiar with (JSON, CSV, etc.). This is handled by the "Data View" module.  

Last but not least, building OverLOD on top of Marmotta allows programmers who are familiar with RDF to freely query the store with SPARQL queries, LDPath, and also benefit from features which are not yet specifically needed by OverLOD as LDP or other upcoming features.

The current implementation is based on Marmotta 3.3.0 (hence the 3.3.0 version number of OverLOD modules). It is a demonstrator and by no means a "ready to use" product.



to be completed...



These modules are developed by the HES-SO Valais-Wallis, Institut Informatique de Gestion in Switzerland [1] as part of the OverLOD Surfer project [2]. 

[1] http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/
[2] http://www.hevs.ch/fr/rad-instituts/institut-informatique-de-gestion/projets/overlod-surfer-6349
