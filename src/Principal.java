import java.io.*;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.*;

import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;


public class Principal {

	static final String inputFileName  = "evacuation.owl"; // tambi�n admite URI

	// Queries Building Evacuation
//	static final String inputFileNameQ1  = "query.txt";
//	static final String inputFileNameQ1  = "queryFindPlan.txt";
	static final String inputFileNameQ1  = "queryEvacuationPlan.txt";
//	static final String inputFileNameQ1  = "queryEvacuationPlan2.txt";
	static final String inputFileNameQ2  = "queryNotAccessibleFor.txt";

//	static final String inputFileNameR1  = "rulesSmartBuildingNotAccessible.txt";
//	static final String inputFileNameR1  = "rulesSmartBuilding.txt";
	static final String inputFileNameR1  = "rules1.txt";
	static final String inputFileNameR2  = "rules2.txt";

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {

        // create an empty model

        Model model = ModelFactory.createDefaultModel();

        InputStream in = FileManager.get().open(inputFileName);
        if (in == null) {
            throw new IllegalArgumentException( "File: " + inputFileName + " not found");
        }

        // read the RDF file
        // the second argument is the base URI
        model.read(in, "", "TURTLE");

        System.out.print("***********************************************\n");
        System.out.print("*****     MODEL READ FROM FILE            *****\n");
        System.out.print("***********************************************\n");

//        model.write(System.out, "TURTLE");

        
        // ******* LOAD ONTOLOGY *******

		InfModel ontoModel = loadOntology(inputFileName);

        System.out.print("***********************************************\n");
        System.out.print("***** MODEL AFTER RDFS Reasoner INFERENCE *****\n");
        System.out.print("***********************************************\n");
        
//        ontoModel.write(System.out, "TURTLE");
        
	    System.out.print("*****    NotAccessibleFor     *****\n");
        queryModel(ontoModel, inputFileNameQ2);

		// *********** JENA Rules **********
		
        ontoModel = loadRules(ontoModel, inputFileNameR1);

        queryModel(ontoModel, inputFileNameQ2);
        
        ontoModel = loadRules(ontoModel, inputFileNameR2);

        // Write the model, which includes all inferences carried out using the rules
        System.out.print("***************************************\n");
        System.out.print("***** MODEL AFTER RULES INFERENCE *****\n");
        System.out.print("***************************************\n");
//        ontoModel.write(System.out, "TURTLE");
        

        
        System.out.print("***************************************\n");
	    System.out.print("*****    SPARQL queries     *****\n");
	    System.out.print("***************************************\n");

	    System.out.print("*****    NotAccessibleFor     *****\n");
        queryModel(ontoModel, inputFileNameQ2);

        queryModel(ontoModel, inputFileNameQ1);


	}
	
	
	private static String readFileAsString(String filePath) throws java.io.IOException{
	    byte[] buffer = new byte[(int) new File(filePath).length()];
	    BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream(new FileInputStream(filePath));
	        f.read(buffer);
	    } finally {
	        if (f != null) try { f.close(); } catch (IOException ignored) { }
	    }
	    return new String(buffer);
	}


	
	/**
	 * Load the ontology from the specified file, which must be in TURTLE format
	 * @param fileName the file containing the ontology
	 * @return the model generated after loading the ontology
	 */
	public static InfModel loadOntology(String fileName) {
		return loadOntology(fileName, "TURTLE");
	}

	/**
	 * Load the ontology from the specified file
	 * @param fileName the file containing the ontology
	 * @param language the format in which the ontology is represented (TURTLE, XML/RDF, ...)
	 * @return
	 */
	public static InfModel loadOntology(String fileName, String language) {

        // create an empty model
    	
        Model model = ModelFactory.createDefaultModel();

        InputStream in = FileManager.get().open(fileName);
        if (in == null) {
            throw new IllegalArgumentException( "File: " + fileName + " not found");
        }

        // read the RDF file
        // the second argument is the base URI
        model.read(in, "", language);

        Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();

        InfModel inf = ModelFactory.createInfModel(reasoner, model);

        return inf;
       
	}
	
	public static InfModel loadRules(InfModel model, String fileName) {

        String rules = null;
		try {
			rules = readFileAsString(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Register a namespace for use in the demo.
		// It doesn't work directly in the file!!
		PrintUtil.registerPrefix("", "http://www.ia.urjc.es/ontologies/evacuation.owl#");
		
		// Load rules
        Reasoner ruleReasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        //InfModel inf = ModelFactory.createInfModel(ruleReasoner, model);
        model = ModelFactory.createInfModel(ruleReasoner, model);

        return model;
	}
	
	public static void queryModel(Model model, String queryFileName) {
		String query = null;
		try {
			query = readFileAsString(queryFileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n**** Query read from file ****\n");
        System.out.println(query);

        consultarSPARQLSelect(model,query);

	}
	

	public static void consultarSPARQLendpointSelect(String endpoint, String queryString){
        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, queryString);
        
        ResultSet results = qe.execSelect();
        
        // Output query results	
        ResultSetFormatter.out(System.out, results, query);

        // Important - free up resources used running the query
        qe.close();
}

	public static void consultarSPARQLSelect(Model model, String queryString){
	        Query query = QueryFactory.create(queryString);

	        // Execute the query and obtain results
	        QueryExecution qe = QueryExecutionFactory.create(query, model);
	        
	        ResultSet results = qe.execSelect();
	        
	        // Output query results	
	        ResultSetFormatter.out(System.out, results, query);

	        // Important - free up resources used running the query
	        qe.close();
	}
	  
	public static void consultarSPARQLConstruct(Model model, String queryString){
	        Query query = QueryFactory.create(queryString);

	        // Execute the query and obtain results
	        QueryExecution qe = QueryExecutionFactory.create(query, model);
	        
		    Model resultModel = qe.execConstruct();
		    
		    resultModel.write(System.out, "RDF/XML-ABBREV");
	        
	        // Important - free up resources used running the query
	        qe.close();
	}	
	 
	public static void consultarSPARQLDescribe(Model model, String queryString){
	        Query query = QueryFactory.create(queryString);

	        // Execute the query and obtain results
	        QueryExecution qe = QueryExecutionFactory.create(query, model);
	        
	        Model mresult = qe.execDescribe();

	        mresult.write(System.out, "RDF/XML-ABBREV");

	        // Important - free up resources used running the query
	        qe.close();  
	}

	  
}
