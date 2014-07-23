//package edu.cornell.library.vivocornell.hr;

package hrcode;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.skife.csv.CSVReader;
import org.skife.csv.SimpleReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class UpdateVivoPerson extends IteratorMethods {
	// why won't this compile?
	public final Property HR_EMPLID = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#emplId"); 
	public final Property HR_NETID = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#netId"); 

	public static final Property JOB_TITLE = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#hrJobTitle");
	public static final Property PRIMARY_JOB = ResourceFactory.createProperty("http://vivoweb.org/ontology/hr#primaryJob");
	public static final Property PRIMARY_WORKING_TITLE = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#primaryWorkingTitle");
	public static final Property PRIMARY_UNIT_CODE = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#primaryUnitCode");	 
	public static final Property EMERTI_PROF = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#EmeritusProfessor"); 
	public static final Property WORKING_TITLE = ResourceFactory.createProperty("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#WorkingTitle");
	
	public static final Resource ORGANIZATION = ResourceFactory.createProperty("http://vivoweb.org/ontology/foaf#Organization");
	
	public static final Resource PRIMARY_POSITION = ResourceFactory.createResource("http://vivoweb.org/ontology/core#PrimaryPosition");	

	//assign object properties to Resource variables
	public static final Property HAS_CONTACT_INFO = ResourceFactory.createProperty("http://purl.obolibrary.org/obo/ARG_2000028");
	public static final Property CONTACT_INFO_FOR = ResourceFactory.createProperty("http://purl.obolibrary.org/obo/ARG_2000029");	
	public static final Property PERSON_IN_POSN = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#relatedBy");
	public static final Property POSN_FOR_PERSON = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#relates");
	public static final Property POSITION_IN_ORGANIZATION = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#relatedBy");
	public static final Property ORGANIZATION_FOR_POSITION = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#relates");
			
	
	public static final Resource POSITION_TYPE = ResourceFactory.createResource("http://vivoweb.org/ontology/core#Position");
	public static final Resource THING_TYPE = ResourceFactory.createResource("http://www.w3.org/2002/07/owl#Thing");
	public static final Resource MOST_SPECIFIC_TYPE = ResourceFactory.createResource("http://vitro.mannlib.cornell.edu/ns/vitro/0.7#mostSpecificType");
	public static final Resource MAN_CURATED_TYPE = ResourceFactory.createResource("http://vivo.cornell.edu/ns/hr/0.9/hr.owl#ManuallyCurated");

	private final Logger logger = Logger.getLogger(this.getClass());

	public String titleMapFile = IngestMain.fileRDFPath + "jobtitles5.csv";
	public String individualRdfPath = IngestMain.fileRDFPath +"addretfiles/";
	//CumulativeDeltaModeler cdm = new CumulativeDeltaModeler();


	public CumulativeDeltaModeler processVivoperson(String personId, CumulativeDeltaModeler cdm) throws Exception {

		/**
		 * This method takes a person and processes all the retractions and additions for 
		 *  updating the RDF.  
		 * TODO: needs better documentation
		 */

		// create models for correction and retraction
		Model retractionsForPerson = ModelFactory.createDefaultModel();
		Model additionsForPerson = ModelFactory.createDefaultModel();
		Model retractionsForPosition = ModelFactory.createDefaultModel();
		Model additionsForPosition = ModelFactory.createDefaultModel();	
		Model retractionsForOrg = ModelFactory.createDefaultModel();
		Model additionsForOrg = ModelFactory.createDefaultModel();		

		Model allAdditions = ModelFactory.createDefaultModel();

		Model CorrectedVIVOPersonRDF = ModelFactory.createDefaultModel();	

		// initialize flags
		boolean ignoreDiffRetract = false;
		boolean ignoreDiffAdd = false;
		boolean ignorePosnDiffRetract = false;
		boolean ignorePosnDiffAdd = false;

		CreateModel cm = new CreateModel();
		ReadWrite rw = new ReadWrite(); 
		CorrectHrData chd = new CorrectHrData(); 
		//***CumulativeDeltaModeler cdm = new CumulativeDeltaModeler();	
		//how is it that we aren't using cdm in this class?

		// go get all RDF for this VIVO person
		OntModel mdlOnePersonVIVORDF = cm.CreateOnePersonVivoRDF(personId);

		// if model is empty, that person does not exist in vivo (or something is wrong) so, bail out, next person
		if (mdlOnePersonVIVORDF.size() > 0 ) {

			// with subject (personId), create OntResource named vivoIndiv with all statements for individual
			OntResource vivoIndiv = mdlOnePersonVIVORDF.getOntResource(personId);

			// use vivoIndiv to get vivoEmplID and vivoNetId
			String vivoPersonEmplId = rw.getLiteralValue(vivoIndiv, HR_EMPLID);
			String vivoPersonNetId = rw.getLiteralValue(vivoIndiv, HR_NETID);

			// log VIVO rdf with VIVO netId
			logger.debug("VIVO RDF for " + vivoPersonNetId);			
			rw.LogRDF(mdlOnePersonVIVORDF, "N-TRIPLE");

			if (vivoPersonEmplId.equals("")) {
				logger.warn("WARNING: blank VIVO emplID for " + vivoIndiv + " does not exist in HRIS data.");
				//blankVIVOEmplIdException.add(CorrectedVIVOPersonRDF);
			}
			if (vivoPersonNetId.equals("")) {
				logger.warn("WARNING: blank VIVO netId for " + vivoIndiv + " does not exist in HRIS data.");
				//blankVIVOEmplIdException.add(CorrectedVIVOPersonRDF);
			}
			logger.debug("vivoEmplId - " + vivoPersonEmplId);
			logger.debug("vivoNetId - " + vivoPersonNetId);


			OntModel mdlOnePersonHRISRDF = cm.CreateOnePersonHrisRDF(vivoIndiv, vivoPersonEmplId, vivoPersonNetId);
			logger.debug("HRIS RDF for " + vivoPersonNetId);			
			rw.LogRDF(mdlOnePersonHRISRDF, "N-TRIPLE");


			// send emplId and netId to getQueryArgs, if emplId is blank, then use netId.
			String qStrOnePersonHRISRDF = rw.ModifyQuery(rw.getQueryArgs(vivoIndiv, vivoPersonEmplId, vivoPersonNetId));
			logger.trace("query for one person HR rdf: " + qStrOnePersonHRISRDF);

			mdlOnePersonHRISRDF = cm.MakeNewModelCONSTRUCT(qStrOnePersonHRISRDF); 	
			logger.debug("VIVO model has " + mdlOnePersonVIVORDF.size() + " statements.");
			logger.debug("HRIS model has " + mdlOnePersonHRISRDF.size() + " statements.");
			if (!mdlOnePersonHRISRDF.isEmpty()) {

				//Model CorrectedHRISPersonRDF = ModelFactory.createDefaultModel();	
				Model CorrectedHRISPersonRDF = chd.processHRISCorrections(mdlOnePersonHRISRDF, vivoIndiv);
				logger.info("done correcting HRIS statements for " + personId + ".");

				//WriteRdf(blankVIVOEmplIdExFile, blankVIVOEmplIdException, "N-TRIPLE");
				logger.debug("VIVO Person RDF");
				rw.LogRDF(mdlOnePersonVIVORDF, "N-TRIPLE");
				logger.debug("CorrectedHRISPerson RDF");
				rw.LogRDF(CorrectedHRISPersonRDF, "N-TRIPLE");
				Long numHRStatements = CorrectedHRISPersonRDF.size();
				if (numHRStatements < 1) {
					logger.warn("No statements in HRIS for "+ vivoIndiv + " : " + vivoPersonEmplId + " : " + vivoPersonNetId + ".  Does this person belong in VIVO?");
					//allNoHRISDataException.add(CorrectedVIVOPersonRDF);

					ignoreDiffRetract = true;
				}
				// check to see if we have a manually curated flag set for this node
				if (vivoIndiv.hasRDFType(EMERTI_PROF)) {
					logger.info("is Emeritus!");
    				ignoreDiffRetract = true;
					ignoreDiffAdd = true;
					ignorePosnDiffRetract = true;
					ignorePosnDiffAdd = true;
	
				}		

				//setup jobFamilymapping
				// TODO: determine if this should be another Sesame map done via query?
				Map<String,String> title2family = new HashMap<String,String>();
				//File titleMapFile = new File(getServletContext().getRealPath(TITLE_MAP_PATH));
				FileInputStream fis = new FileInputStream(titleMapFile);
				CSVReader csvReader = new SimpleReader();
				List<String[]> fileRows = csvReader.parse(fis);
				for(String[] row : fileRows) {
					title2family.put(row[0], row[1]);
				}


				// RDF FOR POSITIONS

				// now, look at positions for this person and compare VIVO positions with HRIS positions
				
				// first, examine VIVO positions
				String vivoPersonURI = vivoIndiv.getURI();
				
				// take a VIVO URI and modify query to return all position RDF (less org links)
				String vivoPersonURIString = ("<" + vivoPersonURI + ">");

				// fixing here
				// get list of D2R positions for person
				String vivoPosnQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetVivoPositionsForPerson.rq");

				String[] getPositionsQryArgs = {vivoPosnQuery, "VARVALUE" , vivoPersonURIString};
				String qStrVivoPositions = rw.ModifyQuery(getPositionsQryArgs); 
				// construct model with all hris position RDF for one person

				logger.trace("query for vivo positions for one person "+ qStrVivoPositions);
				long startTime = System.currentTimeMillis();
				
				// create an OntModel for this query
				OntModel mdlVivoPositions = cm.MakeNewModelCONSTRUCT(qStrVivoPositions); 	
				logger.debug("\n*Vivo positions for " + vivoPersonURI + ":\n\n" + mdlVivoPositions);
				rw.LogRDF(mdlVivoPositions, "N-TRIPLE");
				logger.debug("\nVivoposn link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
				OntModel mdlVivoPersonInPosn = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				OntModel mdlVivoPosnRDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				Model mdlCorrectedVIVOposnRdf = ModelFactory.createDefaultModel();
				List<Statement> vivoPositionList = mdlVivoPositions.listStatements().toList();
				Integer numVivoPosn = vivoPositionList.size();
				logger.debug("number of VIVO positions: " + numVivoPosn);

				//read all statements in list of positions
				for (Statement positionList : vivoPositionList ) {
					String vivoPositionURI = positionList.getObject().toString();
					mdlVivoPersonInPosn.add( vivoIndiv, PERSON_IN_POSN, positionList.getObject() );
					logger.info("add statements to the position model: : "+ mdlVivoPersonInPosn);

                    // gather all RDF related to this position
					String vivoPosnRDFQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGatherVivoPositionRdf.rq");

					//  pass position RDF to VIVO getPositionRDF query

					String vivoPosnURIString = ("<" + vivoPositionURI + ">");
					String[] vivoPositionRdfQueryArgs = {vivoPosnRDFQuery, "VARVALUE" , vivoPosnURIString};
					String qStrVivoPosnRdf = rw.ModifyQuery(vivoPositionRdfQueryArgs); 
					// construct model with all hris position RDF for one person

					logger.trace("Here's the query for VIVO positions for " + vivoPersonURI + ":\n\n" + qStrVivoPosnRdf);
					startTime = System.currentTimeMillis();

					// now, mdlVIVOPosnRDF holds all the position information about this VIVO person
					//mdlVivoPosnRDF = cm.MakeNewModelCONSTRUCT(qStrVivoPosnRdf); 
					// removed in favor of mdlVivoOnePosnRdf
					//OntModel mdlCorrectedVIVOPosnRDF = MakeNewModelCONSTRUCT(qStrVIVOPositionRDF);

					logger.debug("VIVOposn link query time: " + (System.currentTimeMillis() - startTime) + " \n");		
					logger.trace(qStrVivoPosnRdf);
					startTime = System.currentTimeMillis();

					OntModel mdlVivoOnePosnRDF = cm.MakeNewModelCONSTRUCT(qStrVivoPosnRdf); 	
					mdlVivoPosnRDF.add(mdlVivoOnePosnRDF);
					logger.debug("\n*Vivo position RDF before corrections*");
					rw.LogRDF(mdlVivoOnePosnRDF, "N-TRIPLE");
					logger.debug("\nVivo posnRDF link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
				} //end if for vivo posn rdf
				mdlVivoPosnRDF.add(mdlVivoPersonInPosn);
				//why this?
				String vivoHRJobTitle = null;

				// with each statement in VIVO position rdf that is a core:hrJobTitle, 
				List<Statement> vivoPosnList = mdlVivoPosnRDF.listStatements((Resource) null, JOB_TITLE, (RDFNode) null).toList();
				Integer numVIVOPosn = vivoPosnList.size();

               // we have a list of statements where the subject is a position and the 
			   // let's go through that list and do stuff to each position	
				for (Statement stmt : vivoPosnList ) {
					logger.info("number of VIVO positions belonging to this person: " + numVIVOPosn);

					/// are HRISPositionLabel and VIVOPosnTitle the same idea?  FIX THIS

					// prettify existing job title (if not already pretty)
					RDFNode VIVOPosnTitle = stmt.getObject();

					if (VIVOPosnTitle.isLiteral()) {
						// now we need the label from the position, because we need to prettify it
						vivoHRJobTitle = VIVOPosnTitle.asLiteral().getLexicalForm();
					} else {
						logger.debug("why is this not a literal? " + VIVOPosnTitle.toString() + " - " + stmt.getSubject().getURI());
						continue;
					}
 
					// this allows us to suppress changes for Emeriti, if that's what's desired
					logger.info("vivoHRJobTitle is " + vivoHRJobTitle);
					if (vivoHRJobTitle.contains("Emerit")){
						logger.info("is Emeriti");
						ignoreDiffRetract = true;
						ignoreDiffAdd = true;
						ignorePosnDiffRetract = true;
						ignorePosnDiffAdd = true;
					}


					try {
						Resource positionType = chd.getPositionType(vivoHRJobTitle, title2family);
						logger.debug("getPositionType thinks that " + vivoHRJobTitle + " should get this position type:  " + positionType);
						
									
						//mdlVIVOPosnRDF.add(stmt.getSubject(),  RDF.type, positionType );
						mdlCorrectedVIVOposnRdf.add(stmt.getSubject(),  RDF.type, positionType );
						Resource employeeType = chd.getEmployeeType(positionType);
						mdlCorrectedVIVOposnRdf.add(stmt.getSubject(),  RDF.type, employeeType );
						//	mdlVIVOPosnRDF.add(stmt2.getSubject(),  RDF.type, positionType );
						//}				

					} catch  ( Exception e ) {
						logger.error("problem getting position subclass. Error", e );
					} 	

					//use the subject of the Statement
					String vivoPositionURIString = ("<" + stmt.getSubject() + ">");
					String VivoOrgQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetVivoOrgForVivoPosition.rq");
					String[] queryArg6 = {VivoOrgQuery, "VARVALUE", vivoPositionURIString} ;
					String qStrmdlVivoOrgRDF = rw.ModifyQuery(queryArg6); 
					logger.debug("This is the query for getting all the Orgs for one position: \n");
					logger.debug(qStrmdlVivoOrgRDF + "\n");
					startTime = System.currentTimeMillis();
					OntModel mdlVivoOrgRDF = cm.MakeNewModelCONSTRUCT(qStrmdlVivoOrgRDF); 	
					logger.debug("Here's the list of VIVO orgs attached to this position.");
					rw.LogRDF(mdlVivoOrgRDF, "N-TRIPLE");
					logger.debug("\nVIVOposn org link query time: " + (System.currentTimeMillis() - startTime) + "\n");	

					mdlVivoPosnRDF.add(mdlVivoOrgRDF);

				}  //done with VIVO position list

				// this is all existing VIVO position RDF 
				logger.debug("\n*VIVO position RDF*");
				rw.LogRDF(mdlVivoPosnRDF, "N-TRIPLE");

				
				
				//   ******************************
				//   start HR position processing 
                //   ******************************				
				
				// TODO consider renaming to MakeNewModel, parse as describe first?
				
				// setup resource to hold hr PersonURI (it's different) and ont model for all the hr RDF
				
				Resource hrisPersonUri = null;
				Resource  hrisPositionUri = null;
				OntModel mdlHRISPosnRDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				OntModel mdlHRISOrgRDF = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				
				// now setup HRIS positions
				try {

					// get list of D2R positions for person
					String hrisPosnQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetHrJobForHrPerson.rq");

					//  get D2R person URI and pass to HRIS position query
					//   ***THIS IS WHERE THE TROUBLE IS ***
					Resource hrisURI = mdlOnePersonHRISRDF.listSubjects().toList().get(0);
					// with a model, iterate through all statements
					StmtIterator hrisIter = mdlOnePersonHRISRDF.listStatements();


					while (hrisIter.hasNext()) {
						Statement stmt      = hrisIter.nextStatement();  // get next statement
						Resource  subject   = stmt.getSubject();     // get the subject
						Property  predicate = stmt.getPredicate();   // get the predicate
						RDFNode   object    = stmt.getObject();      // get the object
						//CorrectedHRISPersonRDF.add(vivoIndiv, predicate, object);
						CorrectedHRISPersonRDF.add(subject, predicate, object);
						logger.info ("sub:"+subject+",  pred:"+predicate+", obj:"+object);
						if (predicate.toString() == HR_EMPLID.toString()) {
						 hrisPersonUri = subject;
						 logger.info("here's the person uri! : " + hrisPersonUri);
						} else {
							//nothing
						}
					}
					
					String hrisURIString = ("<" + hrisPersonUri.getURI() + ">");
					String[] getHrJobQryArgs = {hrisPosnQuery, "VARVALUE" , hrisURIString};
					String qStrHRISPositions = rw.ModifyQuery(getHrJobQryArgs); 
					// construct model with all hris position RDF for one person

					logger.trace(qStrHRISPositions);
					startTime = System.currentTimeMillis();
					OntModel mdlHRISPositions = cm.MakeNewModelCONSTRUCT(qStrHRISPositions); 	
					logger.debug("\n*HRIS position RDF before corrections*");
					rw.LogRDF(mdlHRISPositions, "N-TRIPLE");
					logger.debug("\nHRISposn link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
					
					OntModel mdlHRISPersonInPosn = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
					List<Statement> hrisPosnList = mdlHRISPositions.listStatements().toList();
					Integer numHRISPosn = hrisPosnList.size();
					logger.debug("number of HRIS position for this person: " + numHRISPosn);
					// with list of positions in mdlHRISPositions, iterate to get all Hris Posn Rdf
					for (Statement posnListForOnePerson : hrisPosnList ) {
						String positionURI = posnListForOnePerson.getObject().toString();
						mdlHRISPersonInPosn.add(hrisPersonUri, PERSON_IN_POSN, posnListForOnePerson.getObject() );
						logger.info("TEST- is this a list of the HR positions?: "+ mdlHRISPersonInPosn);
						// this is a bad name for this resource, consider changing it to hrPosition (meaning person relatedBy position)
						hrisPositionUri = mdlHRISPersonInPosn.listSubjects().toList().get(0);

						String hrisPosnRDFQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGatherHrPositionRdf.rq");

						//  pass position RDF to HRIS getPositionRDF query

						String posnURIString = ("<" + positionURI + ">");
						
						String[] posnRdfQryArgs = {hrisPosnRDFQuery, "VARVALUE" , posnURIString};
						String qStrHRISRdf = rw.ModifyQuery(posnRdfQryArgs); 
						// construct model with all hris position RDF for one person

						logger.trace(qStrHRISRdf);
						startTime = System.currentTimeMillis();

						OntModel mdlHRISOnePosnRDF = cm.MakeNewModelCONSTRUCT(qStrHRISRdf); 	
						mdlHRISPosnRDF.add(mdlHRISOnePosnRDF);
						logger.debug("\n*HRIS position RDF before corrections*");
						rw.LogRDF(mdlHRISOnePosnRDF, "N-TRIPLE");
						logger.debug("\nHRISposnRDF link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
						
						//MOVED  ORG DISCOVERY
						// look at D2R position and generate D2R URI for orgs that don't appear in VIVO
						// query contains all D2R RDF for the orgs in question

						//String hrisPosnQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetPosnForOneHrisPerson.txt");
						logger.debug("now finding organization for this position : " + posnURIString);
						String hrisOrgQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetHrJobAndOrgForHrPosition.rq");
						String[] queryArg4 = {hrisOrgQuery, "VARVALUE" , posnURIString};
						String qStrmdlHRISOrgRDF = rw.ModifyQuery(queryArg4); 
						logger.trace("This is the query for getting all the Orgs for one person: \n");
						logger.trace(qStrmdlHRISOrgRDF);
						startTime = System.currentTimeMillis();
						 mdlHRISOrgRDF = cm.MakeNewModelCONSTRUCT(qStrmdlHRISOrgRDF); 	
						logger.debug("Here's the list of D2R orgs attached to this position.");
						rw.LogRDF(mdlHRISOrgRDF, "N-TRIPLE");
						logger.debug("\nHRISposn org link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
						
						
					} //end if for hris posn rdf
					
					mdlHRISPosnRDF.add(mdlHRISPersonInPosn);
					mdlHRISPosnRDF.add(mdlHRISOrgRDF);
					List<Statement> hrisPosnRdf = mdlHRISPosnRDF.listStatements((Resource) null, RDFS.label, (RDFNode) null).toList();
					//Integer numHRISPosn = hrisPosnRdf.size();
					//logger.debug("number of HRIS positions: " + numHRISPosn);
					String hrisPosnLabel = null;
					String prettyTitle = null;
					Model mdlCorrectedHRISposnRdf = ModelFactory.createDefaultModel();
					//iterate through all statements in the hrisPositionRdf

					for (Statement stmt1 : hrisPosnRdf ) {
						RDFNode hrisLabelObject = stmt1.getObject();
						String headIndValue = null;
						if (hrisLabelObject.isLiteral()) {
							// now we need the label from the position, because we need to prettify it
							hrisPosnLabel = hrisLabelObject.asLiteral().getLexicalForm();
						} else {
							logger.debug("why is this not a literal? " + hrisLabelObject.toString() + " - " + stmt1.getSubject().getURI());
							continue;
						}

						try {
							//String hrisPosnLabel = getLiteralValue(hrisLabelObject, RDFS.label);
							//TODO  PUT BACK PRETTY TITLE MATCHING?!?
							//prettyTitle = chd.getPrettyTitle(hrisPosnLabel);

							//mdlHRISPosnRDF.remove(stmt1);
							//mdlHRISPosnRDF.add(stmt1.getSubject(), RDFS.label, ResourceFactory.createPlainLiteral(prettyTitle));

						} catch  ( Exception e ) {
							logger.error("problem getting pretty title for posn. Error", e );
						} 		

						// this try added 02/14 as bandaid fix for position subclass
                        String primaryJobValue = null;
						try {
							Resource positionType = chd.getPositionType(prettyTitle, title2family);

							logger.debug("HRIS Position Type from pretty Title: " + positionType);
							//mdlVIVOPosnRDF.add(stmt.getSubject(),  RDF.type, positionType );
							// create a list of 
							List<Statement> checkPrimaryPosn = mdlHRISPosnRDF.listStatements(stmt1.getSubject(), PRIMARY_JOB, (RDFNode) null).toList();
							for (Statement stmt9 : checkPrimaryPosn ) {
								//logger.info("PRIMARY? : " + stmt9);
								primaryJobValue = stmt9.getObject().toString();
								//mdlHRISPosnRDF.remove(stmt3);
								//mdlHRISPosnRDF.add(stmt3.getSubject(), POSN_FOR_PERSON, vivoIndiv );
							}

							if (positionType != null) {
								mdlCorrectedHRISposnRdf.add(stmt1.getSubject(),  RDF.type, positionType );

								if (primaryJobValue.equals("P")) {
									mdlCorrectedHRISposnRdf.add(stmt1.getSubject(),  RDF.type, PRIMARY_POSITION );   
									Resource employeeType = chd.getEmployeeType(positionType);
									mdlCorrectedHRISposnRdf.add(vivoIndiv,  RDF.type, employeeType );
									//THIS IS WHERE WE ADD primaryWorkingTitle to the PERSON PROFILE
									mdlCorrectedHRISposnRdf.add(vivoIndiv, PRIMARY_WORKING_TITLE, prettyTitle);
									// can we put PrimaryUnitID in here?
									//mdlCorrectedHRISposnRdf.add(vivoIndiv, PRIMARY_UNIT_CODE, primaryUnitCode);
								}

								//List<Statement> changePosnRDFType = mdlVIVOPosnRDF.listStatements((Resource) null, RDF.type, (RDFNode) null).toList();
								//for (Statement stmt2 : changePosnRDFType ) {
								//mdlVIVOPosnRDF.remove(stmt2);

								//	mdlVIVOPosnRDF.add(stmt2.getSubject(),  RDF.type, positionType );
								//}				
								if (mdlCorrectedHRISposnRdf.isEmpty()) {
									logger.debug("*NO Position Type From Pretty Title* \n");
								} else {
									logger.debug("*Here is Position Type From Pretty Title* \n");
									mdlCorrectedHRISposnRdf.write(System.out, "N-TRIPLE");
								} 
							}  // if positionType is null, what do we do?

						} catch  ( Exception e ) {
							logger.error("problem getting position subclass. Error", e );
						} 	

						
						/** REMOVED- not neccessary?
						List<Statement> changePersInPosn = mdlHRISPosnRDF.listStatements((Resource) null, PERSON_IN_POSN, (RDFNode) null).toList();
						for (Statement stmt2 : changePersInPosn ) {
							mdlHRISPosnRDF.remove(stmt2);
							mdlHRISPosnRDF.add(vivoIndiv, PERSON_IN_POSN, stmt2.getObject() );
						}

						List<Statement> changePosnForPerson = mdlHRISPosnRDF.listStatements((Resource) null, POSN_FOR_PERSON, (RDFNode) null).toList();
						for (Statement stmt3 : changePosnForPerson ) {
							mdlHRISPosnRDF.remove(stmt3);
							mdlHRISPosnRDF.add(stmt3.getSubject(), POSN_FOR_PERSON, vivoIndiv );
						}
  
						**/
						
						try {
							logger.debug("*HRIS position RDF BEFORE corrections* \n");
							rw.LogRDF(mdlHRISPosnRDF, "N-TRIPLE");

							mdlHRISPosnRDF.add(mdlCorrectedHRISposnRdf);

							List<Statement> changePosnRDFType = mdlHRISPosnRDF.listStatements((Resource) null, RDF.type, (RDFNode) null).toList();
							for (Statement stmt4 : changePosnRDFType ) {

								// for each statement in HRIS model, remove rdf:type
								//mdlHRISPosnRDF.remove(stmt4);
								// add back rdf:type core:Position
								mdlHRISPosnRDF.add(stmt4.getSubject(),  RDF.type, POSITION_TYPE );
								// add back rdf:type core:Position with vivoURI?
								//mdlHRISPosnRDF.add(vivoIndiv,  RDF.type, POSITION_TYPE );
							}				
							// this is all HRIS position information minus org links - 
							logger.debug("\n*HRIS position RDF after corrections*");
							rw.LogRDF(mdlHRISPosnRDF, "N-TRIPLE");

						} catch  ( Exception e ) {
							logger.error("problem getting position subclass. Error", e );
						} 					

					} //end for stmt hris posn list





					// figure out whether these new HR positions have links to existing VIVO orgs or need new orgs minted

					// look at D2R position and search VIVO for pre-existing organization
					//desired behavior: always use the VIVO org if we have it, else use the D2R org
					// get org links from VIVO
					// if empty, add org links from HRIS
					// if !empty, then we want to use the VIVO org that corresponds to the HRIS D2R generated one, so, 
					// take the unit and dept ID and search through VIVO for it
					// if you find a match, use it in place of the D2R org
					// if !match, then use org generated by D2R

					logger.debug("**mdlVIVOPosnRDF:\n");
					rw.LogRDF(mdlVivoPosnRDF, "N-TRIPLE");
					//TODO: this depends on rdf:type Position being in the model.  FIX THIS!
					//mdlHRISPosnRDF.add(cm.getVivoOrgLinks(mdlVIVOPosnRDF));
					//after VIVO org addition
					//logger.debug("*added VIVO orgs to position RDF* \n\n");
					//mdlHRISPosnRDF.write(System.out, "N-TRIPLE");

/** original location, moved inside the position iterator
					// look at D2R position and generate D2R URI for orgs that don't appear in VIVO
					// query contains all D2R RDF for the orgs in question

					//String hrisPosnQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetPosnForOneHrisPerson.txt");
					String hrisOrgQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrGetHrJobAndOrgForHrPosition.rq");
					String[] queryArg4 = {hrisOrgQuery, "VARVALUE" , posnURIString};
					String qStrmdlHRISOrgRDF = rw.ModifyQuery(queryArg4); 
					logger.trace("This is the query for getting all the Orgs for one person: \n");
					logger.trace(qStrmdlHRISOrgRDF);
					startTime = System.currentTimeMillis();
					OntModel mdlHRISOrgRDF = cm.MakeNewModelCONSTRUCT(qStrmdlHRISOrgRDF); 	
					logger.debug("Here's the list of D2R orgs attached to this position.");
					rw.LogRDF(mdlHRISOrgRDF, "N-TRIPLE");
					logger.debug("\nHRISposn org link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
**/
					// TODO: somewhere in here, we need to make sure that core#organizationForPosition is in the VIVO model (if it exists)

					//was Model mdlHRISOrgRDF = cm.getHRISOrgLinks(mdlHRISPosnRDF);
					if (!mdlHRISOrgRDF.isEmpty()) {
						// with each statement in mdlHRISOrgRDF, get full position data
						String hrisOrgVivoUriQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrListVivoOrgMatchDeptId.rq");
						String hrisOrgHrisUriQuery = rw.ReadQueryString(IngestMain.fileQryPath + "qStrMintNewHrOrgNotInVivo.rq");		
						logger.debug("mdlHRISORGRDF: " + mdlHRISOrgRDF);
						List<Statement> hrisOrgList = mdlHRISOrgRDF.listStatements((Resource) null, POSITION_IN_ORGANIZATION, (RDFNode) null).toList();
						Integer numHRISOrg = hrisOrgList.size();
						logger.debug("there are " + numHRISOrg + " orgs for this person.");
						for (Statement stmt5 : hrisOrgList ) {

							Resource positionUri = stmt5.getSubject();
							String positionUriString = positionUri.toString();
							RDFNode hrisOrgUri = stmt5.getObject();
							String hrisOrgUriString = hrisOrgUri.toString();
							//hrisOrgUriString = "<" + hrisOrgUriString + ">";
                            // removed <> from string
							
							// does this HRIS org exist in VIVO? 
							/////  TODO ORG relates ORG - fix
							
							
							
							String[] queryArg5 = {hrisOrgVivoUriQuery, "VARVALUE" , hrisOrgUriString};	
							String qStrmdlHRISOneOrgRDF = rw.ModifyQuery(queryArg5); 
							logger.debug("TEST2- Here is the query for oneorg: \n\n" + qStrmdlHRISOneOrgRDF);
							startTime = System.currentTimeMillis();
							OntModel mdlHRISOneOrgRDF = cm.MakeNewModelCONSTRUCT(qStrmdlHRISOneOrgRDF); 
							Resource orgVivoUri = null;
							if (mdlHRISOneOrgRDF.isEmpty()) {
								// if nothing in model, then that Org is not in VIVO.  Add with HRIS URI
								String[] queryArg6 = {hrisOrgHrisUriQuery, "VARVALUE" , hrisOrgUriString};
								qStrmdlHRISOneOrgRDF = rw.ModifyQuery(queryArg6); 		
								logger.debug("Organization not in VIVO; revised query for oneorg: \n\n" + qStrmdlHRISOneOrgRDF);
								mdlHRISOneOrgRDF = cm.MakeNewModelCONSTRUCT(qStrmdlHRISOneOrgRDF); 	

							} else {
								// that org does exist in VIVO, so add the RDF that we got from the first query
								// and retract the HRIS org URIs info
						/** - Removed - no need to do this now?
						 		List<Statement> changeOrgLinks = mdlHRISOneOrgRDF.listStatements((Resource) null, RDF.type , (RDFNode) null).toList();
								for (Statement stmt7 : changeOrgLinks ) {
									orgVivoUri = stmt7.getSubject();

									mdlHRISPosnRDF.remove((Resource) hrisOrgUri, ORGANIZATION_FOR_POSITION, positionUri);
									mdlHRISPosnRDF.remove(positionUri, POSITION_IN_ORGANIZATION, hrisOrgUri);

									mdlHRISPosnRDF.add(orgVivoUri, ORGANIZATION_FOR_POSITION, positionUri );
									mdlHRISPosnRDF.add(positionUri, POSITION_IN_ORGANIZATION, orgVivoUri );
								}
															**/
							}	


							rw.LogRDF(mdlHRISPosnRDF,  "N-TRIPLE");
							logger.debug("RDF for HRIS org.");
							rw.LogRDF(mdlHRISOneOrgRDF, "N-TRIPLE");
							logger.debug("\nHRISOneOrg link query time: " + (System.currentTimeMillis() - startTime) + " \n");	
							logger.debug("\nwe have the person's VIVO URI:" + vivoPersonURIString);
							logger.debug("we have the person' D2R URI:" + hrisURIString);
							logger.debug("we have the D2R org URI:" + hrisOrgUriString);	
							logger.debug("we have the VIVO org URI:" + orgVivoUri + "\n");

                           /**  REMOVED - this is just adding the type triple to the model.  Not needed.
							mdlHRISPosnRDF.add(mdlHRISOneOrgRDF);
							logger.info("Added RDF to mdlHRISPosnRDF.");
						  **/
						}


						//after HRIS org addition
						logger.debug("*added HRIS orgs to position RDF* \n");
						mdlHRISPosnRDF.write(System.out, "N-TRIPLE");

					} 
					// this is where the problem if dumps out !!!
					logger.info("mdlHRISORGRDF is empty.");
					logger.info("THERE IS NO D2R ORG FOR THIS POSITION.  WHY?");			
					logger.info("vivoHRJobTitle = " + vivoHRJobTitle);
					

					if (numHRISPosn == 0) {
						ignoreDiffRetract = true;
						ignoreDiffAdd = true;
						ignorePosnDiffRetract = true;
						ignorePosnDiffAdd = true;
					}
				} catch  ( Exception e ) {
					logger.error("problem with making org models. Error", e );
				} 		
				mdlVivoPosnRDF = removeInferredClasses(mdlVivoPosnRDF);
				mdlHRISPosnRDF = removeInferredClasses(mdlHRISPosnRDF);

				logger.debug("look for manually curated flag on VIVO position");
			    //headinignorePosnDiffRetract = false;
			    //ignorePosnDiffAdd = false;
				List<Statement> checkManCuratedPosn = mdlVivoPosnRDF.listStatements((Resource) null, RDF.type, MAN_CURATED_TYPE).toList();
				logger.debug("mancurated? = " + checkManCuratedPosn.size());
				if (checkManCuratedPosn.size() > 0) {
					logger.info("position is manually curated.");
					ignorePosnDiffRetract = true;
					ignorePosnDiffAdd = true;
				}

				// pick this whole try up and put it with position diff section
				// testing retractions and additions
				Model testRetractions = ModelFactory.createDefaultModel();
				Model testRetractions2 = ModelFactory.createDefaultModel();
				try {
					logger.info("preparing retract/add RDF");  
					// killed iso check in favor of straight diff
					//if (!mdlOnePersonVIVORDF.isIsomorphicWith(CorrectedHRISPersonRDF)) {
					//	logger.info("Corrected VIVO is NOT isomorphic with Corrected HRIS.");
					// do work to figure out what's different between models and act accordingly with non-blanknode statements.
					// take the difference between current and existing RDF models

					if (ignoreDiffRetract) {
						logger.debug("retract suppressed.");
					} else {
						retractionsForPerson.add(mdlOnePersonVIVORDF.difference(CorrectedHRISPersonRDF));  

						testRetractions.add(mdlHRISPosnRDF.difference(mdlOnePersonVIVORDF)); 
						testRetractions2.add(retractionsForPerson.difference(testRetractions)); 
						logger.info("here are the test retractions");
						logger.trace(testRetractions2);
						retractionsForPerson.remove(testRetractions2);
						if(retractionsForPerson.size() > 0) {
							logger.info("***" + retractionsForPerson.size() + " PROFILE RETRACTIONS ***");  
							retractionsForPerson.write(System.out, "N-TRIPLE");   
							rw.WriteRdf(individualRdfPath + vivoPersonNetId + ".retprofile.nt", retractionsForPerson, "N-TRIPLE");
							//PWT and others need to be checked against Posn Additions
							// if they appear in both
							cdm.retractModel(retractionsForPerson);
							logger.trace(retractionsForPerson);
							// don't show all retracts/adds here
							// rw.LogRDF(cdm.getRetractions(), "N-TRIPLE");

						} else  {
							logger.info("*** NO PROFILE RETRACTIONS ***");
						}
					} //endif for ignoreDiffRetract

					if (ignoreDiffAdd) {
						logger.debug("addition suppressed.");
					} else {
						additionsForPerson.add(CorrectedHRISPersonRDF.difference(mdlOnePersonVIVORDF));
						if(additionsForPerson.size() > 0) {
							logger.info("*** " + additionsForPerson.size() + " PROFILE ADDITIONS ***");  
							additionsForPerson.write(System.out, "N-TRIPLE");      
							rw.WriteRdf(individualRdfPath + vivoPersonNetId + ".addprofile.nt", additionsForPerson, "N-TRIPLE");
							cdm.addModel(additionsForPerson);
							logger.trace(additionsForPerson);
							//dont' show all retracts/adds here
							//rw.LogRDF(cdm.getAdditions(), "N-TRIPLE");
						} else  {
							logger.info("*** NO PROFILE ADDITIONS ***");
						}
					} //endif for ignoreDiffRetract

					// reset flags
					ignoreDiffRetract = false;
					ignoreDiffAdd = true;

				} catch  ( Exception e ) {
					logger.error("Trouble adding profile RDF for this person to master add/retract model. Error" + e + "\n");
				} finally {
					logger.info("done with profile retract/add for " + personId + "." + "\n");
				}

				// testing retractions and additions
				try {
					logger.info("preparing retract/add RDF for positions");  


					//TODO before you diff, remove rdf:type Position from models?
					// killed iso check in favor of straight diff
					//if (!mdlOnePersonVIVORDF.isIsomorphicWith(CorrectedHRISPersonRDF)) {
					//	logger.info("Corrected VIVO is NOT isomorphic with Corrected HRIS.");
					// do work to figure out what's different between models and act accordingly with non-blanknode statements.
					// take the difference between current and existing RDF models

					logger.info("\n*HRIS position RDF BEFORE retract*");
					rw.LogRDF(mdlHRISPosnRDF, "N-TRIPLE");
					logger.info("\n*VIVO position RDF BEFORE retract*");
					rw.LogRDF(mdlVivoPosnRDF, "N-TRIPLE");					

					
					
					if (ignorePosnDiffRetract) {
						logger.info("position retract suppressed.");
					} else {
						retractionsForPosition.add(mdlVivoPosnRDF.difference(mdlHRISPosnRDF));  
						if(retractionsForPosition.size() > 0) {
							logger.info("***" + retractionsForPosition.size() + " POSITION RETRACTIONS ***");  
							retractionsForPosition.write(System.out, "N-TRIPLE");    
							rw.WriteRdf(individualRdfPath + vivoPersonNetId + ".posnret.nt", retractionsForPosition, "N-TRIPLE");
							cdm.retractModel(retractionsForPosition);
							logger.trace(retractionsForPosition);
						} else  {
							logger.info("*** NO POSITION RETRACTIONS ***");
						}
					} //endif for ignoreDiffRetract

					if (ignorePosnDiffAdd) {
						logger.info("position addition suppressed.");
					} else {
						additionsForPosition.add(mdlHRISPosnRDF.difference(mdlVivoPosnRDF));  
						additionsForPosition.remove(testRetractions2);
						//additionsForPosition.add(mdlCorrectedVIVOposnRdf);
						if(additionsForPosition.size() > 0) {
							logger.info("*** " + additionsForPosition.size() + " POSITION ADDITIONS ***");  
							additionsForPosition.write(System.out, "N-TRIPLE"); 
							rw.WriteRdf(individualRdfPath + vivoPersonNetId + ".posnadd.nt", additionsForPosition, "N-TRIPLE");
							cdm.addModel(additionsForPosition);
							logger.trace(additionsForPosition);
						} else  {
							logger.info("*** NO POSITION ADDITIONS ***");
						}
					} //endif for ignoreDiffRetract

					// reset flags
					ignorePosnDiffRetract = false;
					// why would this have been set to true?  Changed to false
					ignorePosnDiffAdd = false;

				} catch  ( Exception e ) {
					logger.error("Trouble adding position RDF for this person to master add/retract model. Error" + e + "\n");
				} finally {
					logger.info("done with position retract/add for " + personId + "." + "\n");
				}

				/**
				allRetractions.add(retractionsForPerson);

				allAdditions.add(additionsForPerson);
				 **/
				// this needs to change to reflect a position diff.

				//allRetractions.add(retractionsForPosition);
				////cdm.retractModel(retractionsForPosition);
				//allAdditions.add(additionsForPosition);
				////cdm.addModel(additionsForPosition);

				additionsForPerson.close();
				retractionsForPerson.close();
				additionsForPosition.close();
				retractionsForPosition.close();
				allAdditions = cdm.getAdditions();

			} else {
				logger.warn("HRIS model is empty, meaning this emplId/netId does not exist in HR data. Log this in exception file, suppress retract/add and move to next person.");

			}  // endif for !HRISEmplId==null

		} else {
			logger.warn("VIVO model is empty for the URI " + personId + ", meaning this emplId/netId does not exist in VIVO data.");


		}  // endif empty VIVO model
		return cdm;
	}//end method

	public OntModel removeInferredClasses(OntModel mdlBeforeDiff) throws Exception {	

		try {
			//remove inferred classes from vivo model before diff
			//List<Statement> removeVIVORDFType = mdlBeforeDiff.listStatements((Resource) null, RDF.type, (RDFNode) null).toList();
			List<Statement> removeVIVORDFType = mdlBeforeDiff.listStatements().toList();
			for (Statement stmt : removeVIVORDFType ) {
				//logger.info("*RDF stmt: \n" + stmt);	
				//logger.info(stmt.getObject().toString());
				if (stmt.getObject().toString().equals(THING_TYPE.toString())){
					mdlBeforeDiff.remove(stmt);
					logger.debug("*removed core:thing" + stmt);	
				}
				if (stmt.getObject().toString().equals(POSITION_TYPE.toString())){
					mdlBeforeDiff.remove(stmt);
					logger.debug("*removed core:position" + stmt);
				}				
				if (stmt.getPredicate().toString().equals(MOST_SPECIFIC_TYPE.toString())){
					mdlBeforeDiff.remove(stmt);
					logger.debug("*removed vitro:mostSpecificType" + stmt);
				}		


			}		
			//allAdditions.add(additionsForPosn);
		} catch  ( Exception e ) {
			logger.error("problem with retracting inferred types. Error", e );
		}
		return mdlBeforeDiff; 
	}

} //end class


