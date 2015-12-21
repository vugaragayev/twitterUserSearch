package de.unibonn.iai.eis;

import com.github.jsonldjava.utils.*;
import com.github.jsonldjava.core.*;
import com.github.jsonldjava.impl.*;
import com.github.jsonldjava.jena.*;

import java.util.*;
import org.json.*;
import java.io.*;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;

import com.hp.hpl.jena.*;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
 
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
 

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserSearchController {

	static String AccessToken = "Your Access Token";
	static String AccessSecret = "Your Access Secret";
	static String ConsumerKey = "Consumer Key";
	static String ConsumerSecret = "Consumer Secret";
    
    @RequestMapping("/twitter/search")
    public String search(@RequestParam(value="query", required = true) String query) throws Exception{
        
    	OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                ConsumerKey,
                ConsumerSecret);
    	consumer.setTokenWithSecret(AccessToken, AccessSecret);
    	//String url = "https://api.twitter.com/1.1/users/search.json?q="+ query +"&page=1&count=4";
    	String url = "https://api.twitter.com/1.1/users/search.json?q="+ query ;  	
      	String encodedUrl = url.replaceAll(" ","%20");
        
    	
    	HttpGet request = new HttpGet(encodedUrl);
        consumer.sign(request);
 
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(request);

        //int statusCode = response.getStatusLine().getStatusCode();
        //System.out.println(statusCode + ":" + response.getStatusLine().getReasonPhrase());
        //System.out.println(IOUtils.toString(response.getEntity().getContent()));
       
        
        JSONArray resultJson = readResponse(response);
        System.out.println(resultJson);
        
        Model model = createFoafRDF(resultJson);
        Object finalResultAsJsonLd = toJsonLD(model);
        
        System.out.println(JsonUtils.toPrettyString(finalResultAsJsonLd));  
        writeToFile(finalResultAsJsonLd);
        //ClassLoader classLoader = getClass().getClassLoader();
		//File file = new File(classLoader.getResource("result.txt").getFile());

		//PrintWriter pw = new PrintWriter(new File(this.getClass().getResource("result.txt").getFile()));
        
       
        
        return JsonUtils.toPrettyString(finalResultAsJsonLd);
    }
    
    private void writeToFile(Object finalResultAsJsonLd) throws Exception{
    	 
    	 PrintWriter pw = new PrintWriter(new FileWriter("result.json"));
         
         pw.println(JsonUtils.toPrettyString(finalResultAsJsonLd));
         pw.flush();
         pw.close();
    	
    }
    private JSONArray readResponse(HttpResponse response ) throws Exception{
    	
    	BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (String line = null; (line = reader.readLine()) != null;) {
            builder.append(line).append("\n");
        }
        JSONTokener tokener = new JSONTokener(builder.toString());
        JSONArray res = new JSONArray(tokener);
    	return res;
    }
    private Model createFoafRDF(JSONArray finalResult) throws Exception{
    	Model model = ModelFactory.createDefaultModel();
    	
    	if(finalResult.length() > 0 ){
    		
    		for( int i = 0 ; i < finalResult.length(); i++ ){
	    		JSONObject item = finalResult.getJSONObject(i);
	    		//System.out.println(item);
	        
		       
		        Resource user=ResourceFactory.createResource("http://unibonn.eis.de/twitter/search_results#"+item.getString("id_str"));
		        model.add(user,RDF.type,FOAF.Person);
		        model.add(user,RDF.type,FOAF.Agent);
		        model.add(user,FOAF.name,item.getString("name"));
		        model.add(user,FOAF.depiction,ResourceFactory.createResource(item.getString("profile_image_url")));
		        model.add(user,FOAF.accountName,item.getString("screen_name"));
		        model.add(user,FOAF.nick,item.getString("screen_name"));
		        model.add(user,FOAF.homepage,ResourceFactory.createResource(item.getString("url")));
		     }
	     //  model.write(System.out,"NT");
	    }
    	return model;
    }
    
    
    private Object toJsonLD( Model model) throws Exception{
    	
    	final JenaRDFParser parser = new JenaRDFParser();
        Object json_ld = JsonLdProcessor.fromRDF(model, new JsonLdOptions(), parser);
        
       //System.out.println("\n\n\n\n");
       //System.out.println(JsonUtils.toPrettyString(json_ld));
        
        
       // System.out.println("\n\n\n\n");
        Map<String, String> context = new HashMap<String, String>();
        context.put("foaf","http://xmlns.com/foaf/0.1/");
        context.put("fuhsen","http://unibonn.eis.de/fuhsen/common_entities#");
        context.put("gr","http://purl.org/goodrelations/v1#");
        context.put("omv","http://omv.ontoware.org/2005/05/ontology#");
        context.put("owl","http://www.w3.org/2002/07/owl#");
        context.put("prov","http://www.w3.org/ns/prov#");
        context.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        context.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        context.put("xsd","http://www.w3.org/2001/XMLSchema#");
        
        Object compact = JsonLdProcessor.compact(json_ld,context,new JsonLdOptions());
        return compact;
    }
    
}