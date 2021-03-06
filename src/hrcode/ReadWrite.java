//package edu.cornell.library.vivocornell.hr;

package hrcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class ReadWrite {

	private final Logger logger = Logger.getLogger(this.getClass());
	
	public String getLiteralValue(OntResource ontRes, Property property) throws Exception{
		RDFNode vivoNode = null; 
		String value = "";
		try {
			logger.debug("property " + property);	
			vivoNode = ontRes.getPropertyValue(property);
			logger.debug("vivoNode " + vivoNode);
			if (vivoNode != null) {

				if (vivoNode.isLiteral() ) {
					logger.debug("vivoNode is literal.");
					value = vivoNode.asLiteral().getLexicalForm();
				} else {
					logger.debug("vivoNode is NOT literal.");
					value = vivoNode.toString();
				}
			} else {
				logger.debug("vivoNode is null.");
			}
			//logger.debug("node " + vivoNode);	
			return value;
		} catch ( Exception e ){
			logger.error("That didn't work- problem getting literal value.  Error" , e );
			throw e; 
		}
		// if vivoNode is not null AND vivoNode isLiteral, then return vivoNode.asLiteral.getLexicalForm else return null

	}
	
	public String[] getQueryArgs(Resource vivoIndiv, String vivoPersonEmplId, String vivoPersonNetId) throws IOException  {
		// setup query for grabbing VIVO rdf
       //TODO: don't these need to generate 1.6 rdf with vcard?
		
		String HRISRDFBaseQuery = ReadQueryString(IngestMain.fileQryPath + "qStrGatherHrPersonRdfWithEmplId.rq");
		String HRISRDFnetIdQuery = ReadQueryString(IngestMain.fileQryPath + "qStrGatherHrPersonRdfWithNetId.rq");

		if (vivoPersonEmplId.equals(""))  {
			//String vivoPersonNetId = getLiteralValue(vivoIndiv, HR_NETID);
			// pass VIVO netID to HRIS query to get HRIS RDF
            logger.info("EMPL ID blank!");
			logger.info("constructing HRIS RDF for " + vivoPersonNetId + "...");
			String[] temp =  {HRISRDFnetIdQuery, "VARVALUE", vivoPersonNetId};
			return temp;

		} else {
			// pass VIVO emplId to HRIS query to get HRIS RDF
			logger.info("constructing HRIS RDF for " + vivoPersonEmplId + "...");
			String[] temp = {HRISRDFBaseQuery, "VARVALUE", vivoPersonEmplId};
			return temp;


		}

	}
	
	public String ReplaceRegex(String[] args) 
			throws Exception {
		String returnString = "";
		String startString = args[0];
		String findString = args[1];
		String replString = args[2];

		String finishString = "";

		// Create a pattern to match FindString
		Pattern p = Pattern.compile(findString);
		// Create a matcher with an input string
		Matcher m = p.matcher(startString);
		StringBuffer sb = new StringBuffer();
		boolean result = m.find();
		boolean found = false; 
		// Loop through and create a new String 
		// with the replacements
		while(result) {
			m.appendReplacement(sb, replString);
			result = m.find();
			found = true;
		}
		// Add the last segment of input to 
		// the new String
		m.appendTail(sb);
		finishString = (sb.toString());
		if (!found) {
			logger.debug(findString + " not found." + "\n");
			returnString = startString;
		} else {
			returnString = finishString;
		}
		return returnString;
	}
	

	public String ModifyQuery(String[] args) throws Exception {
		String modifiedString = "";
		String interimString = "";	    		 
		// baseQuery is original query string from text file
		String baseQuery = args[0];

		try {
			interimString = baseQuery;
			String[] replArgs = {interimString, args[1], args[2]};
			String tempString = ReplaceRegex(replArgs);
			modifiedString = tempString;
		} catch (Exception e) {
			logger.error("whoops.  Query mod threw error " , e);
		} finally {

		}
		return modifiedString;
	}
	
	public String ReadQueryString(String filePath) throws IOException {
		String queryString = "";
		String serviceVIVO = "";
		String serviceHRIS = "";
		String modifiedString = "";
		boolean useProductionVIVO = false;

		try {
			StringBuffer fileData = new StringBuffer(1000);
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead=0;
			while((numRead=reader.read(buf)) != -1){
				fileData.append(buf, 0, numRead);
			}
			reader.close();
			queryString = fileData.toString();

			// determine whether we are using production endpoint or offline production mirror endpoint
			// return value of SERVICE 

			if (useProductionVIVO) {
				serviceVIVO = "http://vivoprod01.library.cornell.edu:2020/sparql"; 
			} else {
				// bailey
				//serviceVIVO = "http://bailey.mannlib.cornell.edu:2520/sparql"; 
				// vivo-migrate VM
                //  serviceVIVO = "http://vivo-migrate.library.cornell.edu/VIVO/sparql";
				// cmw48-dev vivo1.6 VM
                // serviceVIVO = "http://cmw48-dev.library.cornell.edu/VIVO/sparql";	
				// vivo-migrate vivo1.6 VM
				serviceVIVO = "http://vivo-migrate.library.cornell.edu/vivo16/sparql";
			}			

			//  Replace VIVOSERV where ever we find it in the query text 
			String substring = "VIVOSERV";

			if (queryString.contains(substring)) {
				String [] replArgs = {queryString, "VIVOSERV", serviceVIVO };
				modifiedString = ReplaceRegex(replArgs);
				queryString = modifiedString;
			} 

			// set a value for the HRIS D2R endpoint
			// Replace HRISSERV where ever we find it in the query text
			substring = "HRISSERV";
			
			// vivo-migrate VM
			serviceHRIS = "http://vivo-migrate.library.cornell.edu/d2r"; 
			// bailey
                        //serviceHRIS = "http://bailey.mannlib.cornell.edu:2020/sparql"; 
			if (queryString.contains(substring)) {
				String [] replArgs = {queryString, "HRISSERV", serviceHRIS };
				modifiedString = ReplaceRegex(replArgs);
				queryString = modifiedString;
			} 
			
			
		} catch (Exception e) {
			logger.error("whoops.  What happened?  " + e);
		} finally {

		}
		return queryString;
	}
	
	public String ReadStringFromFile(String filePath) throws IOException {
		String filetextString = "";
		boolean useProductionVIVO = false;

		try {
			StringBuffer fileData = new StringBuffer(1000);
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead=0;
			while((numRead=reader.read(buf)) != -1){
				fileData.append(buf, 0, numRead);
			}
			reader.close();
			filetextString = fileData.toString();

			// determine whether we are using production endpoint or offline production mirror endpoint
			// return value of SERVICE 
		
		} catch (Exception e) {
			logger.error("whoops.  What happened?  " + e);
		} finally {

		}
		return filetextString;
	}
	
	public Model ReadRdf(String args[]) throws IOException {
		String filename = args[0];
		String basename = args[1];
		String readformat = args[2];

		Model mdlReadModel = ModelFactory.createDefaultModel();
		// now read in the HRIS emplID model
		FileInputStream readstream = null;
		try {
			readstream = new FileInputStream(filename);
			mdlReadModel.read(readstream, basename, readformat);
		} finally {
			if (readstream != null) {
				//            readstream.flush();
				readstream.close();
			}
			logger.info(" Successfully read in the nt data from " + filename);
		}  //end try for read hrisEmplId
		return mdlReadModel;
	}
	
	public void WriteRdf(String filename, Model model, String RDFFormat) throws IOException  {
		//now, use RDFWriter to write the VIVO emplIDs to NTRIPLES file
		FileOutputStream fstream = null;
		try {
			fstream = new FileOutputStream(filename);
			model.write(fstream, RDFFormat);
			//logger.trace("rw thread slowdown disabled...");
			//Thread.sleep(100);
		} catch (Exception e) { 
			// do we have file exists/overwrite/backup logic to insert here?
			logger.error("problem with write process?, Error" + e);

		} finally {
			// close filestream
			if (fstream != null) {
				fstream.flush();
				fstream.close();

			}
		}
	}
	
	public void LogRDF(Model model, String RDFFormat) throws IOException  {
	
        StringWriter strw = new StringWriter();
		
		try {
			logger.trace(model.write(System.out, "N-TRIPLE"));
			model.write(strw, "N-TRIPLE");
			logger.trace(strw);
		} catch (Exception e) { 
			// do we have file exists/overwrite/backup logic to insert here?
			logger.error("problem with rdf logging process?, Error" + e);

		} finally {
			// close filestream
			if (strw != null) {
				strw.flush();
				strw.close();
	    	}
     	}
	}

// use this method for improved args processing
public String processArgs(String[] args) throws Exception {
    int i = 0, j;
    String arg;
    char flag;
    boolean vflag = false;
    String outputfile = "";
	String readFromFile = null;
	
	try {
		 while (i < args.length && args[i].startsWith("-")) {
	            arg = args[i++];

	        if (arg.equals("-verbose")) {
	                System.out.println("verbose mode on");
	                vflag = true;
	        }    
		
			if (args[0].equals("-f")) {
				logger.info("reading VIVO URI's from file " + args[1]);
				readFromFile = args[1];
			}
			
            else if (arg.equals("-output")) {
                if (i < args.length)
                    outputfile = args[i++];
                else
                    System.err.println("-output requires a filename");
                if (vflag)
                    System.out.println("output file = " + outputfile);
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

}