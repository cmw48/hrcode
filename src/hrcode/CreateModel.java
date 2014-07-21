//package edu.cornell.library.vivocornell.hr;

package hrcode;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class CreateModel {

	private final Logger logger = Logger.getLogger(this.getClass());

	//private final Logger LOGGER = Logger.getLogger(this.getClass());
	//CreateModel cm = new CreateModel();
	ReadWrite rw = new ReadWrite();  

	public String readFromFile(String[] args) throws Exception {
		String readFromFile = null;
		try {
			if (args[0] != null) {
				if (args[0].equals("-f")) {
					logger.info("reading VIVO URI's from file " + args[1]);
					readFromFile = args[1];
				}
				//if (args[2].equals("-u")) {
				//	logger.info("reading HRIS person URI's from file " + args[3]);
				//	readFromFile = args[3];
				//}
			}
		} catch (Exception e) { 
			logger.error("exception reading args!  Error" + e);
			throw e; 
		} finally {
			//logger.debug("created new model...");
		
		}
		return readFromFile;
	}

	

	public OntModel MakeNewModelCONSTRUCT(String strQueryString) throws Exception {
		/**
		 * Takes a String strqueryString and returns a generic Model
		 *   model is written to RDF file for future use
		 *
		 *   TODO
		 *   create functionality for SELECT
		 */
		// REMOVED 140623 logger.trace("constructing New Model with " + strQueryString);
	    OntModel mdlNewModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {

		    try {
			
			    // load vivo emplIDs into a model, then write RDF to file                
			    Query qryNewQuery = QueryFactory.create(strQueryString, Syntax.syntaxARQ);
			    QueryExecution qexecNewQuery = QueryExecutionFactory.create(qryNewQuery, mdlNewModel);
			    try {
				    //logger.debug("querying source and populating Model...");

    				//query VIVOsource for emplId and populate Model
		    		// execute queries and populate model
	    			// create CONSTRUCT rdf and push to HRIS model /
                
			    	//logger.trace("cm thread slowdown disabled...");
				    //Thread.sleep(100);
				
		    		try {
					
			    		qexecNewQuery.execDescribe(mdlNewModel);
				    } catch (Exception e) {
				    	qexecNewQuery.execConstruct(mdlNewModel);
				    } 

			    } catch (Exception e) { 
				     logger.error("problem (not really writing) the new model!  Error" + e, e);
	    		}  finally {
		    		//close query execution
		    		qexecNewQuery.close();
				
		    	}                
		    } catch (Exception e) { 
			    logger.error("exception creating the VIVO model!  Error" + e);
	    	} finally {
		    	//logger.debug("created new model...");
    		}
		    return mdlNewModel;
		} catch ( Exception e ) {
			logger.error("problem with model construct!  Error" + e);
			throw(e);
		} finally {
			//mdlNewModel.close(); 
		}
		 
	}

	public Model CreateAllVIVOPersonList(String filename) throws Exception {
		// generate a model of all VIVO uris that have either netId or HR emplId

		Model mdlAllVIVOPerson = ModelFactory.createDefaultModel();
		logger.info("querying VIVO service for a list of persons.  (wait time ~90 seconds)...");

		if (filename != null) {
			// optional functionality :create separate logic for reading RDF from existing files 
			// instead of querying services.  Useful?  Maybe when services are down...
			// following commented code is an example read: create mdlHRISMatch from existing nt file

			String VIVOMatchfilename = IngestMain.fileRDFPath + filename;
			String readnamespace  = "http://vivo.cornell.edu/";
			String readfileformat = "N-TRIPLE";
			String[] readargs = {VIVOMatchfilename, readnamespace, readfileformat};
			ReadWrite rw = new ReadWrite();
			mdlAllVIVOPerson = rw.ReadRdf(readargs);
			logger.info("read in Model mdlAllVIVOPerson from file " + VIVOMatchfilename);

		} else {	
			try {
				//  gather ALL VIVO person (with either netId or emplId) in a model 
				ReadWrite rw = new ReadWrite();
				String qStrAllVIVOPerson=  rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllVivoPersons.rq");
				String allVIVOFileName = IngestMain.fileRDFPath + "allVIVOPersonURI.nt";
				logger.info("creating VIVO model...");
				logger.trace("using this query: \n" + qStrAllVIVOPerson);
				mdlAllVIVOPerson = MakeNewModelCONSTRUCT(qStrAllVIVOPerson);                            
				rw.WriteRdf(allVIVOFileName, mdlAllVIVOPerson, "N-TRIPLE");
				logger.info("querying VIVOsource and populating Model mdlAllVIVOPerson...");

			} catch ( Exception e ) {
				logger.error("exception writing All VIVO!  Error" + e);
			} finally {

			}
		}
		return mdlAllVIVOPerson;
	}

	public Model CreateAllActiveHRISPersonList(String filename) throws Exception {
		// generate a model of all VIVO uris that have either netId or HR emplId

		Model mdlAllActiveHRISPerson = ModelFactory.createDefaultModel();
		OntModel mdlHrisAllPositions =  ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntModel mdlHrisTermPositions =  ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

		IteratorMethods im = new IteratorMethods();
		if (filename != null) {
			// optional functionality :create separate logic for reading RDF from existing files 
			// instead of querying services.  Useful?  Maybe when services are down...
			// following commented code is an example read: create mdlHRISMatch from existing nt file

			String HRISMatchfilename = IngestMain.fileRDFPath + filename;
			String readnamespace  = "http://vivo.cornell.edu/";
			String readfileformat = "N-TRIPLE";
			String[] readargs = {HRISMatchfilename, readnamespace, readfileformat};
			ReadWrite rw = new ReadWrite();

			mdlAllActiveHRISPerson = rw.ReadRdf(readargs);
			logger.info("read in Model mdlAllVIVOPerson from file " + HRISMatchfilename);

		} else {	
			try {
				/*
				//  gather ALL VIVO person (with either netId or emplId) in a model 
				ReadWrite rw = new ReadWrite();
				String qStrAllVIVOPerson=  rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllVivoPersons.rq");
				String allVIVOFileName = IngestMain.fileRDFPath + "allVIVOPersonURI.nt";
				logger.info("creating VIVO model...");
				logger.trace("using this query: \n" + qStrAllVIVOPerson);
				mdlAllActiveHRISPerson = MakeNewModelCONSTRUCT(qStrAllVIVOPerson);                            
				rw.WriteRdf(allVIVOFileName, mdlAllActiveHRISPerson, "N-TRIPLE");
				logger.info("querying VIVOsource and populating Model mdlAllVIVOPerson...");
                */
			//  gather ALL HRIS positions in a model 
							String qStrAllHRISPositions=  rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllHrPositions.rq");
							logger.trace(qStrAllHRISPositions);
							String allHRISPosnFileName = IngestMain.fileRDFPath + "allHRISPositionsURI.nt";
							logger.info("creating model with ALL HRIS positions...");

							mdlHrisAllPositions = MakeNewModelCONSTRUCT(qStrAllHRISPositions);                            
							rw.WriteRdf(allHRISPosnFileName, mdlHrisAllPositions, "N-TRIPLE");
							logger.info("found " + mdlHrisAllPositions.size() + " positions for mdlHrisAllPositions...");
							
							//  gather ALL Terminated HRIS positions in a model 
							String qStrAllTermPositions=  rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllHrTermPositions.rq");
							logger.trace(qStrAllTermPositions);
							String allTermHRISPosnFileName = IngestMain.fileRDFPath + "allTermHRISPositionsURI.nt";


							mdlHrisTermPositions = MakeNewModelCONSTRUCT(qStrAllTermPositions);      
							logger.info("creating model mdlHrisTermPositions with " + mdlHrisTermPositions.size() + " Terminated HRIS positions.");
							rw.WriteRdf(allTermHRISPosnFileName, mdlHrisTermPositions, "N-TRIPLE");

							
							//diff AllPositions with TermPositions leaving ActivePositions
							
							OntModel mdlActivePosnDiff = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, mdlHrisAllPositions.difference(mdlHrisTermPositions));
							String allActiveFileName = IngestMain.fileRDFPath + "allHRISActivePositionsURI.nt";
							rw.WriteRdf(allActiveFileName, mdlActivePosnDiff, "N-TRIPLE");
							logger.info("found " + mdlActivePosnDiff.size() + " active positions in HR data.");
							
							mdlAllActiveHRISPerson = im.IterateThroughHrisPositionList(mdlActivePosnDiff);

			} catch ( Exception e ) {
				logger.error("exception writing All VIVO!  Error" + e);
			} finally {

			}
		}
		return mdlAllActiveHRISPerson;
	}
	
	public Model CreateNewHrisPersonList(String HrisUriFilename) throws Exception {
		// generate a model of all VIVO uris that have either netId or HR emplId

		logger.info("querying HRIS service for a list of NEW persons.  wait time ~90 seconds...");
		// generate a model of all HRIS URIs , check against VIVO model


		//OntModel mdlAllHRISPerson = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		//OntModel mdlHRISAllMatch = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		Model mdlAllHRISPerson = ModelFactory.createDefaultModel();
		Model mdlHRISAllMatch = ModelFactory.createDefaultModel();
		
		// TODO: it's a model, it's an ontModel... Which one?  If OntModel, cannot diff below
		//OntModel mdlNewHRISDiff = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

		//if (HrisUriFilename != null) {
			// future implementation of HRIS URI list
			// optional functionality :create separate logic for reading RDF from existing files 
			// instead of querying services.  Useful?  Maybe when services are down...
			// following commented code is an example read: create mdlHRISMatch from existing nt file
			/*
				String HRISMatchfilename = fileRDFPath + "hrisMatchEmplId.nt";
				String readnamespace  = "http://vivo.cornell.edu/";
				String readfileformat = "N-TRIPLE";
				String[] readargs = {HRISMatchfilename, readnamespace, readfileformat};
				Model mdlHRISMatch = ReadRdf(readargs);
				logger.debug("read in Model mdlHRISMatch from file hrisMatchEmplId.nt...");
			 */

		//} else {
			try {
				String nofilename = null;
				mdlAllHRISPerson = CreateAllActiveHRISPersonList(nofilename);
				logger.info("Model mdlAllHRISPerson now contains all active HR persons...");
			    
				//  gather ALL HRIS emplIds in a model 
				//String qStrAllHRISPerson=  rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllHrPersons.rq");
				//String allHRISFileName = IngestMain.fileRDFPath + "allHRISPersonURI.nt";
				//logger.info("creating model with ALL HRIS URIs...");

				//mdlAllHRISPerson = MakeNewModelCONSTRUCT(qStrAllHRISPerson);                            
				//rw.WriteRdf(allHRISFileName, mdlAllHRISPerson, "N-TRIPLE");
				
				//IteratorMethods im = new IteratorMethods();
				

			    
				// pull all HRIS emplIDs from HRIS service WHERE HRIS.emplId matches a VIVO.emplId
				// keep original HRIS URI for diff process
				String qStrHRISmatchVIVOEmplId = rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllHrPersonWithVivoEmplId.rq"); 
				String HRISMatchFileName = IngestMain.fileRDFPath + "hrisMatchEmplId.nt";
				logger.trace(qStrHRISmatchVIVOEmplId);
				logger.trace("creating model where HRIS matches VIVO emplIds..may take a minute or so");
				Model mdlHRISMatchEmplId = MakeNewModelCONSTRUCT(qStrHRISmatchVIVOEmplId);   
				logger.info("found "+ mdlHRISMatchEmplId.size() + " HR matches for VIVO empl ids...");                   
				//WriteRdf(HRISMatchFileName, mdlHRISMatchEmplId, "N-TRIPLE");
				//logger.debug("created model named mdlHRISmatch and wrote to file.");
				mdlHRISAllMatch.add(mdlHRISMatchEmplId);


				
				// pull all HRIS netIDs from HRIS service WHERE HRIS.netId matches a VIVO.netId
				// keep original HRIS URI for diff process
				String qStrHRISmatchVIVONetId = rw.ReadQueryString(IngestMain.fileQryPath + "qStrListAllHrPersonWithVivoNetId.rq"); 
				String HRISMatchNetIdFileName = IngestMain.fileRDFPath + "hrisMatchNetId.nt";
				logger.trace(qStrHRISmatchVIVONetId);
				logger.trace("creating cross-check model where HRIS matches VIVO netIds..");
				Model mdlHRISMatchNetId = MakeNewModelCONSTRUCT(qStrHRISmatchVIVONetId);   
				logger.info("found "+ mdlHRISMatchNetId.size() + " HR matches for VIVO net ids...");                     
				//WriteRdf(HRISMatchNetIdFileName, mdlHRISMatchNetId, "N-TRIPLE");
				//logger.debug("created model named mdlHRISMatchNetId and wrote to file.");

			
				
				mdlHRISAllMatch.add(mdlHRISMatchNetId);
				logger.info("for a total of "+ mdlHRISAllMatch.size() + " HR matches for VIVO persons...");   
				// TODO: this is the statement that fails when mdlNewHRISDiff is an OntModel
				// get "Type Mismatch, cannot convert from Model to OntModel"
				//create model named mdlNewHRISDiff to hold the difference between allHRPersons and allHRMatch
				OntModel mdlNewHRISDiff = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, mdlAllHRISPerson.difference(mdlHRISAllMatch));

			
				// write diff RDF model to file
				String HRISDiffFilename = IngestMain.fileRDFPath + "allNewHRISPeople.nt";
				rw.WriteRdf(HRISDiffFilename, mdlNewHRISDiff, "N-TRIPLE");
				logger.debug("mdlNewHRISDiff now contains emplIds for HRIS not in VIVO .");

				long numHRISNewPersons = mdlNewHRISDiff.size(); 
				logger.info("generated a model of new HRIS uris that don't match a VIVO netId or HR emplId");
				logger.info("found a total of " + numHRISNewPersons + " statements.");

				
				//logger.info("Pausing for user input - look it over and hit ENTER..." );
				//Scanner sc = new Scanner(System.in);
			    //while(!sc.nextLine().equals(""));	
				
				return mdlNewHRISDiff;
				
			} catch ( Exception e ) {
				logger.error("exception writing All HRIS!  Error" + e);
				throw(e);
			} finally {

			}

		//}


	}

	
	
	
	public OntModel CreateOnePersonVivoRDF(String personId) throws Exception {
		OntModel mdlOnePersonVIVORDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {
			// modify VIVO query with personId
			String VIVORDFBaseQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGatherVivoPersonRdf.rq");
			String[] VIVOqueryArgs = {VIVORDFBaseQuery, "VARVALUE", personId};
			String qStrOnePersonVIVORDF = rw.ModifyQuery(VIVOqueryArgs);
			//  *REMOVED 140623**logger.trace("query string for one person VIVO RDF: \n\n" + qStrOnePersonVIVORDF);
			//create ontology model with vivo data for single person
			long startTime = System.currentTimeMillis();		
			mdlOnePersonVIVORDF = this.MakeNewModelCONSTRUCT(qStrOnePersonVIVORDF); 
			logger.debug("VIVOonePerson link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
		} catch ( Exception e ){
			logger.error("Something got messed up while getting RDF for one VIVO person.  Error" , e );

		} finally {
			logger.info("VIVO model has " + mdlOnePersonVIVORDF.size() + " statements.");
		}
		return mdlOnePersonVIVORDF; 
	}

	public OntModel CreateOnePersonHrisRDF(OntResource vivoIndiv, String vivoPersonEmplId, String vivoPersonNetId) throws Exception {
		OntModel mdlOnePersonHrisRDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {
			// modify VIVO query with personId
			String HrisRDFBaseQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGatherHrPersonRdfWithEmplId.rq");
			//String[] VIVOqueryArgs = {HrisRDFBaseQuery, "VARVALUE", personId};

			// send emplId and netId to getQueryArgs, if emplId is blank, then use netId.

			String qStrOnePersonHrisRDF = rw.ModifyQuery(rw.getQueryArgs(vivoIndiv, vivoPersonEmplId, vivoPersonNetId));
		//  *REMOVED 140623**logger.trace("query string for one person HRIS RDF: \n\n" + qStrOnePersonHrisRDF);


			//mdlOnePersonHrisRDF = cm.MakeNewModelCONSTRUCT(qStrOnePersonHrisRDF); 	
			//String qStrOnePersonHrisRDF = rw.ModifyQuery(HrisQueryArgs);
			//create ontology model with vivo data for single person
			long startTime = System.currentTimeMillis();		
			mdlOnePersonHrisRDF = this.MakeNewModelCONSTRUCT(qStrOnePersonHrisRDF); 
			logger.debug("HRISonePerson link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
		} catch ( Exception e ){
			logger.error("Something got messed up while getting RDF for one HRIS person.  Error" , e );

		} finally {
			logger.debug("HRIS model has " + mdlOnePersonHrisRDF.size() + " statements.");
		}
		return mdlOnePersonHrisRDF; 
	}
	
	
	public Model getVivoOrgLinks(Model mdlVIVOPosnRDF) throws Exception {

		try {
			String vivoOrgsQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrListVivoOrgAndPositionMatchDeptId");
			logger.info("here is the Org Links query: \n\n" + vivoOrgsQuery);
			long startTime = System.currentTimeMillis();
			
			// what this does is: 
			// 
			//# designed to query an existing model
			//# find every vivo:Position in model and return URI
			//# with D2R, use URI to get orgURI, return ?deptId
			//# with VIVO Joseki, return VIVO uri for that dept
			//# construct vivoorg and position pairs
			
			//BUT, if the posn URI in VIVO does not match the URI in D2R, then nothing
			//forces us to add 1st pass RDF to realign position URI's and then go again to get correct org data??
			// confusing!
			Model mdlOrgPosnLinksVIVO = QueryAgainstExistingModel(vivoOrgsQuery, mdlVIVOPosnRDF); 
			logger.debug("VIVO link query time: " + (System.currentTimeMillis() - startTime) + " \n\n\n");		
			logger.trace(vivoOrgsQuery);
			logger.trace(mdlOrgPosnLinksVIVO);
			logger.info("Here's your VIVO org links: ");
			rw.LogRDF(mdlOrgPosnLinksVIVO, "N3");
			return mdlOrgPosnLinksVIVO;

		} catch ( Exception e ) {
			logger.error("problem creating VIVO orgLink RDF. Error" , e );
			throw e;
		} finally {
			logger.debug("vivoOrgLinks model created.");
		}

	}

	
	public Model getHRISOrgLinks(Model mdlHRISPosnRDF) throws Exception {

		try {
			String hrisOrgsQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrListHrOrgNotInVivo.rq");
			//String hrisOrgsQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetPosnForOneHrisPerson.txt");
			long startTime = System.currentTimeMillis();			
			Model mdlOrgPosnLinksHRIS = QueryAgainstExistingModel(hrisOrgsQuery, mdlHRISPosnRDF); 
			logger.debug("HRIS link query time: " + (System.currentTimeMillis() - startTime) + " \n\n\n");		
			if (!mdlOrgPosnLinksHRIS.isEmpty()) {
				logger.trace(hrisOrgsQuery);
				logger.trace(mdlOrgPosnLinksHRIS);
			}  

			return mdlOrgPosnLinksHRIS;

		} catch ( Exception e ) {
			logger.error("problem creating VIVO orgLink RDF. Error" , e );
			throw e;
		} finally {
			//logger.debug("hrisOrgLinks model created.");
		}

	}

	public OntModel QueryAgainstExistingModel(String strQueryString, Model existingModel) throws Exception {

		OntModel mdlNewModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try {
			// load vivo emplIDs into a model, then write RDF to file                

			Query qryNewQuery = QueryFactory.create(strQueryString, Syntax.syntaxARQ);
			QueryExecution qexecNewQuery = QueryExecutionFactory.create(qryNewQuery, existingModel);
			try {
				//logger.debug("querying source and populating Model...");

				//query VIVOsource for emplId and populate Model
				// execute queries and populate model
				// create CONSTRUCT rdf and push to HRIS model /

				try {
					qexecNewQuery.execDescribe(mdlNewModel);
				} catch (Exception e) {
					qexecNewQuery.execConstruct(mdlNewModel);
				} 

			} catch (Exception e) { 
				logger.error("problem querying the existing model!  Error", e);
			}  finally {
				//close query execution
				qexecNewQuery.close();
			}                

		} catch (Exception e) { 
			logger.error("exception creating the VIVO model!  Error" + e);

		} finally {
			//logger.debug("created new model...");
		}
		return mdlNewModel;
	}
	

	
}
