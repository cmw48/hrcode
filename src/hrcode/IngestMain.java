//package edu.cornell.library.vivocornell.hr;
package hrcode;

import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

//import edu.cornell.mannlib.vivo.hr.HRUpdateTest20getPosnSubclasses;

public class IngestMain {

	/* HR Ingest main class
	 * note, not static
	 */
	//set up constants for fileIO

	public static String fileRDFPath = "./rdfdump/";
	public static String fileQryPath = "./queries/";     
	public static String fileLogPath = "./logs/";  
	// added to band-aid title/position subclass mapping.  TODO: make this a reader and open/close via in the read method.
	//public static String titleMapFile = fileRDFPath + "jobtitles5.csv";
	
	public final Property HR_EMPLID = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#emplId"); 
	public final Property HR_NETID = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#netId"); 
	
	
	// originally, we thought we could run these queries against production Joseki, let's not do that anymore
	// public static Boolean useProductionVIVO = false;
	// instead, we will modify the queries to take a SERVICE parameter
	// in this case we will query VIVO data in an off-line mirror of production data
	// Joseki on port 2520
	//  http://bailey.mannlib.cornell.edu:2520/sparql
	// and we will query HR data in D2R Server on port 2020
	// http://bailey.mannlib.cornell.edu:2020/sparql
	
	//set up logging for this class
    private static final Logger logger = Logger.getLogger(IngestMain.class);
	
  //set up SPARQLupdate class
    static UpdateDelete ud = new UpdateDelete();
	
	public static void main(String[] args) throws Exception {
		/** main method does the following: 
		 * setup logging
		 * read and process commandline args
		 * fire CreateNewHrisPersonList()
		 *      generates list of HRIS uri's 
		 * fire IterateThroughHrisPersonList()
		 *      iterates through list and fires processNewVivoPerson()
		 *            determines which HRIS not in VIVO, prepares RDF for basic addition
		 *      writes rdf to file?      
		 * if mdlAllUrisAdded !empty, pause to allow user to add them via add/remove interface before proceeding
		 *      else skip the pause and continue with update
		 * fire CreateAllVIVOPersonList()
		 *      builds list of all VIVO persons with netId or emplID
		 * fire IterateThroughVivoPersonList()
		 *      iterates over list of VIVO persons and fires processVivoPerson() for each
		 *           processes each one for HRIS updates to VIVO data, generates RDF
		 *      writes RDF
		 * TODO: then what? are there useful checksums or reports here?      
		 *      
		*/
	    try {	
		// setup logging	
		PropertyConfigurator.configure(fileLogPath + "loggercfg.txt");
		logger.info("Entering application - initializing models.");
		String VivoUriFilename = null;
		CreateModel cm = new CreateModel();
		IteratorMethods im = new IteratorMethods();
		
		try {
		if (args.length != 0)  {
			logger.debug("commandline arguments: " + args);
			VivoUriFilename = cm.readFromFile(args);
		} else {
		logger.debug("no commandline args.");
		}
		} catch ( Exception e ){
			logger.error("Something didn't work while reading from file.  Error" , e );
		} 

		// future development: would we ever need to work from a list of HRIS uris?
		String HrisUriFilename = null;
		String ActiveHrisPersonFilename = null;

		// this is all NEW HRIS PERSON stuff
		String updateStatus = null;
 		String allNEWAdditionsFileName = fileRDFPath + "allNEWAdditions.nt";	
 		// turn this switch to 1 to load HR Active persons from file
 		// set to 0 to renew list of active HR persons - use 0 when processing new HRIS data
 		// input file should be set to process only hr new people: -f allNewHRISPeople.nt
		Integer testingswitch = 0;
		if (testingswitch == 0) {     
		    logger.info("renewing list of active HR persons...");
		    // generate a model of all HRIS uris , check against VIVO model
		    Model mdlAllNewHrisPerson = cm.CreateNewHrisPersonList(HrisUriFilename);
		    logger.info("New HRIS person queries complete.  Iterating over results...");

		    Model mdlAllUrisAdded = im.IterateThroughHrisPersonList(mdlAllNewHrisPerson);
		    logger.info("New HRIS person additions complete.");
		    
		    // pause for user input to allow loading of New Person RDF
			long totalHRISAdditions = mdlAllUrisAdded.size(); 
			logger.info(totalHRISAdditions + " new persons found.  Adding via SPARQL update.");

			if (totalHRISAdditions != 0) {
		           updateStatus = ud.sparqlUpdate(mdlAllUrisAdded);
			} else {
				logger.info("No new HRIS persons to add!" );
			}
		    
		} else {
		    logger.info("Loading active HR persons from most recent file...");
			ActiveHrisPersonFilename = "allHRISActivePersonsURI.nt";
			Model mdlAllUrisAdded = cm.CreateAllActiveHRISPersonList(ActiveHrisPersonFilename);
		}    


 		// turn this bit to 1 to load VIVO Eligible persons from file
 		// set to 0 to renew list of VIVO Eligible  persons - *ALWAYS* use 0 when processing new VIVO data
 		// input file should be set to process only hr new people: -f allNewHRISPeople.nt

		Integer vivotestswitch = 1;
		if (vivotestswitch == 0) {     		
            //  get a model of all (eligible) persons in VIVO
		    logger.info("Creating new list of VIVO persons from most recent nightly backup...");
		
		} else {
			    logger.info("Loading active VIVO persons from most recent file...");
			    VivoUriFilename = "allVIVOPersonURI.nt";

		 }    
        Model mdlAllVIVOPerson = cm.CreateAllVIVOPersonList(VivoUriFilename);
	    logger.info("created a list of " + mdlAllVIVOPerson.size() + " eligible persons in VIVO for update.  Iterating over results...");
		
		// iterate through model and update as needed, and return a model consisting of updated URIs 
		Model mdlVivoURIsUpdated = im.IterateThroughVivoPersonList(mdlAllVIVOPerson);
		logger.info("VIVO update complete.");
		

		} catch ( Exception e ){
			logger.error("Something got messed up while iterating over the person list.  Error" , e );

		} finally {
			//logger.info("done looking at all " + numHRISNewPersons + " VIVO persons.");
		}
	}

}
