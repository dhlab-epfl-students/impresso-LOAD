package impresso;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.cache.Cache;

import construction.Annotation;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;


/**
 * Creates a LOAD subgraph from a single document
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class MultiThreadWorkerImpresso implements Runnable {
    
    private MultiThreadHubImpresso hub;
    private Cache<String, JSONObject> newspaperCache;
    private Cache<String, JSONObject> entityCache;
    private HashMap<Integer, String> contentIdtoPageId;
    private Properties prop;
    private SolrReader solrReader;
    
    // internal variables
    HashSet<String> invalidTypes;
    private int[] count_ValidAnnotationsByType;
    private long count_unaggregatedEdges;
    private int failedCount;
    private int negativeOffsetCount;
    private static Pattern pattern = Pattern.compile(datepattern);
    
    public MultiThreadWorkerImpresso(MultiThreadHubImpresso hub, Properties prop, SolrReader solrReader, Cache<String, JSONObject> newspaperCache, Cache<String, JSONObject> entityCache, HashMap<Integer, String> contentIdtoPageId) {
        this.hub = hub;
        this.prop = prop;
        this.newspaperCache = newspaperCache;
        this.entityCache = entityCache;
        this.contentIdtoPageId = contentIdtoPageId;
        this.solrReader = solrReader;
        // internal variables
        invalidTypes = new HashSet<String>();
        count_ValidAnnotationsByType = new int[nANNOTATIONS];
        count_unaggregatedEdges = 0;
        failedCount = 0;
        negativeOffsetCount = 0;
        
    }
     
    @Override
    public void run() {
        
        ArrayList<Annotation> annotationsPage = new ArrayList<Annotation>();     	//Change the annotationsPage to contentItems
        ArrayList<Annotation> annotationsSentence = new ArrayList<Annotation>();	//Will have to be with an artificial sentences
        ArrayList<Annotation> annotationsTerms = new ArrayList<Annotation>();		//These will come from the tokens
        
        HashSet<String> invalidTypes = new HashSet<String>();
        ArrayList<String> edges = new ArrayList<String>();
        
        int invalidAnnotationCount = 0;
        int annotationCounter = 0;
        Integer newspaper_year_id = null;
        
        while ( (newspaper_year_id = hub.getPageID()) != null) {
        	//Get the contentId from the hashmap
        	String contentId = contentIdtoPageId.get(newspaper_year_id);
        	//using content id create impressocontentitem and use this to read from solr, then inject tokens
        	//Each worker will read from a single newspaper/year coordinated by the Hub
        	        	
            annotationsPage.clear();
            edges.clear();


                    
                try {
                	//For each id, create a content item
                	ImpressoContentItem contentItem = solrReader.getContentItem(contentId);
                	//Inject the tokens into the content item and create the sentences
                	contentItem = injectLingusticAnnotations(contentItem);
                	//Sort all of the tokens so that they are in order of offset
                	contentItem.sortTokens();
                	
                	List<Token> tokens = contentItem.getTokens();
                	int sentence_id = 0; //Increments as the sentence increases
                	for(int i = 0; i <  tokens.size(); i+=7) {
                		annotationsSentence.clear();
                        annotationsTerms.clear();
	                	//Create an artificial sentence composed of at most 7 tokens
                		List<Token> sentence = tokens.subList(i, Math.min(tokens.size(),i+7));
                		
	                    boolean hasAnnotations = false;
	                    for(Token token : sentence) { // if there are annotations in the sentence
	                        if(token instanceof Entity) {
	                        // DATES: 
	                        // iterate over temporal annotations and extract them to create nodes
                            String annotationType_str = ((Entity) token).getType();
                            
                            char annotationType;
                            if (annotationType_str.equals(loc)) {
                                annotationType = LOC;
                            } else if (annotationType_str.equals(act)) {
                                annotationType = ACT;
                            } else {
                                invalidTypes.add(annotationType_str);
                                invalidAnnotationCount++;
                                continue;
                            }
                                
                            /* Dates are not part of our data set
                            if (annotationType == DAT) {
                                String timexValue = obj.getString(mongoIdentAnnotation_normalized);
                                Matcher m = pattern.matcher(timexValue);
                                if (m.matches()) {
                                    count_ValidAnnotationsByType[DAT]++;
                                                
                                    // mark portion of the sentence that is covered by the date for deletion
                                    int begin = (Integer) obj.get(mongoIdentAnnotation_start);
                                    int end = (Integer) obj.get(mongoIdentAnnotation_end);
                                    for (int p=begin; p<end; p++) {
                                        mask[p] = replaceableChar;
                                    }
                                                
                                    // extract dates and make sure the completeness condition is satisfied
                                    // i.e. for dates YYYY-MM-DD also include YYYY-MM and YYYY, ...
                                    String date = "";
                                    for (int i=1; i<=m.groupCount(); i++) {
                                        if (m.group(i) != null) {
                                            date += m.group(i);
                                            int annId = hub.getAnnotationID(annotationType, date);

                                            // add annotation to list for later edge creation
                                            Annotation ann = new Annotation(date, annId, annotationType, sentence_id);
                                            annotationsSentence.add(ann);
                                        }
                                    }
                                            
                                    hasAnnotations = true;
                                    annotationCounter++;
                                } */

                            if (annotationType == LOC || annotationType == ACT) {
                            	
                            	String value = token.getLemma();
                                // get an id
                                int annId = hub.getAnnotationID(annotationType, value);
    
                                // add annotation to list for later edge creation
                                Annotation ann = new Annotation(value, annId, annotationType, sentence_id);
                                annotationsSentence.add(ann);
                                    
                                hasAnnotations = true;
                                annotationCounter++;
                                count_ValidAnnotationsByType[annotationType]++;

                            }
                        }
	                 }
                    // add this sentence and the corresponding page to the graph if it had valid annotations
                    if (hasAnnotations) {
                            
                        // add sentence to the map
                        int sentenceId = hub.getAnnotationID(SEN, String.valueOf(sentence_id));
                        count_ValidAnnotationsByType[SEN]++;
                            
                        // add page / document to the map
                        int pageId = hub.getAnnotationID(PAG, contentId);
                        count_ValidAnnotationsByType[PAG]++;
                            
                        //If there are annotations in the sentence, turn all other tokens into term annotations
                        for(Token term: sentence) {
                        	if(!(term instanceof Entity)) {
                                int annId = hub.getAnnotationID(TER, term.getLemma());
                                Annotation ann = new Annotation(term.getLemma(), annId, TER, sentence_id);
                        	}
                        	
                        }
                        
                        // turn list of annotations into edges by pairwise comparison
                        // add edge between sentence and page
                        edges.add(PAG + sepChar + SEN + sepChar + pageId + sepChar + sentenceId + sepChar + 0 + "\n");
                        count_unaggregatedEdges++;
                            
                        for (int j=0; j<annotationsSentence.size(); j++) {
                            Annotation an = annotationsSentence.get(j);
                                
                            // NOTE connecting entities to the sentence is enough (sentences are connected to pages)
                            // add edge between annotation and page
                            // ew.append(an.type + sepChar + PAG + sepChar + an.id + sepChar + pageId + sepChar + 0 + "\n");
                            // count_unaggregatedEdges++;
                            
                            // add edge between annotation and sentence
                            edges.add(an.type + sepChar + SEN + sepChar + an.id + sepChar + sentenceId + sepChar + 0 + "\n");
                            count_unaggregatedEdges++;
                                
                            annotationsPage.add(an);
                        }
                            
                        for (int j=0; i<annotationsTerms.size(); j++) {
                            Annotation t = annotationsTerms.get(j);
                                
                            // NOTE connecting terms to the sentence is enough (sentences are connected to pages)
                            // add edge between term and page
                            // ew.append(t.type + sepChar + PAG + sepChar + t.id + sepChar + pageId + sepChar + 0 + "\n");
                            // count_unaggregatedEdges++;
                                
                            // add edge between term and sentence
                            edges.add(t.type + sepChar + SEN + sepChar + t.id + sepChar + sentenceId + sepChar + 0 + "\n");
                            count_unaggregatedEdges++;
                                
                            // add pairwise edges between terms and annotations in the same sentence (but only in one direction) 
                            for (int h=0; h<annotationsSentence.size(); h++) {
                                Annotation an = annotationsSentence.get(h);
                                edges.add(an.type + sepChar + t.type + sepChar + an.id + sepChar + t.id + sepChar + 0 + "\n");
                                count_unaggregatedEdges++;
                            }
                                
                        }
                    }
                    sentence_id ++;
                }
                } catch (Exception e) {
                    e.printStackTrace();
                    failedCount++;
                }
            
            // sort all annotations on a page by sentence ID for easier pairwise comparison
            // in the following, it is assumed that annotations with smaller sentence ID come first
            Collections.sort(annotationsPage);
                
            // turn list of annotations on the entire page into edges by pairwise comparison
            for (int i=0; i<annotationsPage.size(); i++) {
                Annotation an1 = annotationsPage.get(i);
                    
                // add pairwise edges between all annotations (but only in one direction)
                // ORDER: lower entity type first (if this is equal, lower ID first)
                for (int j=i+1; j<annotationsPage.size(); j++) {
                    Annotation an2 = annotationsPage.get(j);
                    
                    // compute the distance in sentences between the two annotations. Since annotations
                    // are ordered non-decreasingly by sentenceID, if this distance is larger than the
                    // maximum distance, we can skip the rest of the list.
                    int weight = an2.sentenceID - an1.sentenceID;
                    if (weight > maxDistanceInSentences) {
                        break;
                    }
                        
                    if (an1.type != an2.type) { // connections between entity types
                        if (an1.type < an2.type) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight + "\n");
                            count_unaggregatedEdges++;
                        } else {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight + "\n");
                            count_unaggregatedEdges++;
                        }
                    } else if (an1.type == LOC || an1.type == ACT || an1.type == ORG) { // connections within entity types
                        if (an1.id < an2.id) {
                            edges.add(an1.type + sepChar + an2.type + sepChar + an1.id + sepChar + an2.id + sepChar + weight + "\n");
                            count_unaggregatedEdges++;
                        } else if (an1.id > an2.id) {
                            edges.add(an2.type + sepChar + an1.type + sepChar + an2.id + sepChar + an1.id + sepChar + weight + "\n");
                            count_unaggregatedEdges++;
                        }
                        // the case where an1.id == an2.id is ignored since we do not want self loops in the network
                    }
                }
            }
            try {
                hub.writeEdges(edges);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // update the total statistics for summing up over all threads
        hub.updateStatistics(annotationCounter, count_unaggregatedEdges, failedCount, invalidAnnotationCount, invalidTypes,
                             count_ValidAnnotationsByType, negativeOffsetCount);
        
        hub.latch.countDown();
        
    }
    
	private ImpressoContentItem injectLingusticAnnotations(ImpressoContentItem contentItem) {
		String tempId = contentItem.getId();
		JSONObject jsonObj = newspaperCache.getIfPresent(tempId);
		JSONArray sents = jsonObj.getJSONArray("sents");
		int length = sents.length();
		int totalOffset = 0; //Keeps track of the total offset
		for(int j=0; j<length; j++) {
		    JSONObject sentence = sents.getJSONObject(j);
		    //This is where the injectTokens of a ImpressoContentItem
		    totalOffset += contentItem.injectTokens(sentence.getJSONArray("tok"), sentence.getString("lg"), true, totalOffset);
		}
	
		/*
		 * WHILE THE ENTITIES ARE BEING DUMPED TO THE S3 BUCKET
		 * SHOULD NOT EXIST IN THE FINAL IMPLEMENTATION
		 */
		
		jsonObj = entityCache.getIfPresent(tempId);
		JSONArray mentions = jsonObj.getJSONArray("mentions");
		//This is where the injectAnnotations of a ImpressoContentItem
		contentItem.injectTokens(mentions, null, false, 0);
		
		return contentItem;
	}
}
