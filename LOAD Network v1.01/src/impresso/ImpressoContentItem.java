package impresso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import construction.Annotation;

public class ImpressoContentItem {

	private String language;
	private String id;
	private String content_txt;
	private Integer year;
	private List<Token> tokens;
	private static Properties prop;
	
	public ImpressoContentItem() {
		
	}
	
	public ImpressoContentItem(SolrDocument document, Properties properties) {
		id = (String) document.getFieldValue("id");
		language = (String) document.getFieldValue("lg_s");
		
		switch(language) {
		case "fr":
			content_txt = (String) document.getFieldValue("content_txt_fr");
			break;
		case "de":
			content_txt = (String) document.getFieldValue("content_txt_de");
			break;
		case "lu":
			content_txt = (String) document.getFieldValue("content_txt_lu");
			break;
		}
		
		year = (Integer) document.getFieldValue("meta_year_i");
		tokens = new ArrayList<Token>();
		prop = properties;
	}
	
	public int injectTokens(JSONArray tokenArray, String tokLang, boolean isToken, int offset) {
		int length = tokenArray.length();
		String[] posTypes = prop.get("PoSTypes").toString().split(",");
		
		if(isToken) {
			for(int i=0; i<length; i++) {
				  JSONObject token = tokenArray.getJSONObject(i);
				  if(tokLang == null) {
					  tokLang = language;
				  }
				  //Filter out any of the entity tokens, these will be filled in by the annotations 
				  if(!token.has("e")) {
					  //Check to see if the token is a pos within the PoS types we would like to keep
					  if(Arrays.stream(posTypes).anyMatch(token.getString("p")::equals)) {
						  tokens.add(new Token(token, tokLang, offset));
					  }
				  }
				  //Look at the final offset of the token, return this offset plus length of the token
				  if(i == length-1) {
					  return token.getInt("o") + token.getString("s").length();
				  }
			}
		}
		/*
		 * WHILE THE ENTITIES ARE BEING DUMPED TO THE S3 BUCKET
		 * SHOULD NOT EXIST IN THE FINAL IMPLEMENTATION
		 */
		//Adding the annotated entities to the token list
		//Adding to the token list but 
		else {
			for(int i=0; i<length; i++) {
				  JSONObject entity = tokenArray.getJSONObject(i);
				  if(tokLang == null) {
					  tokLang = language;
				  }
				  tokens.add(new Entity(entity, tokLang));
				 
			}
			return 0;
		}
		return 0;
	}

	public void sortTokens() {
		Collections.sort(tokens);
	}
	
	public String getlanguage() {
		return language;
	}
	public String getId() {
		return id;
	}
	public String getContent_txt() {
		return content_txt;
	}
	public Integer getYear() {
		return year;
	}
	
	public List<Token> getTokens(){
		return tokens;
	}
}
