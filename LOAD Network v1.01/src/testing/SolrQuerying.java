package testing;

import java.io.IOException;
import java.util.Properties;
import java.io.FileInputStream;


import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.request.QueryRequest;


public class SolrQuerying {
	
	
	public static void main(String[] args) {
		Properties prop=new Properties();
		String propFilePath = "../resources/config.properties";
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(propFilePath);
			prop.load(inputStream);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		 
	    System.out.println(prop.getProperty("solrDBName"));
		HttpSolrClient client = new HttpSolrClient.Builder(prop.getProperty("solrDBName")).build();
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery("*:*");
		solrQuery.set("fl","*");
		solrQuery.setRows(10);
		
		try {
		    QueryRequest queryRequest = new QueryRequest(solrQuery);
		    System.out.println(System.getenv("Solrpassword"));
		    queryRequest.setBasicAuthCredentials(prop.getProperty("solrUserName"),System.getenv("solrPassword"));
		    QueryResponse solrResponse = queryRequest.process(client);
		    System.out.println(solrResponse);
		    System.out.println("Total Documents : "+solrResponse.getResults().getNumFound());
		} catch (SolrServerException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
	}
}