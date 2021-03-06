Java implementation of the LOAD graph model for cross document entity and event exploration
(c) 2016, Andreas Spitz, spitz@informatik.uni-heidelberg.de

Originally published at:
http://dbs.ifi.uni-heidelberg.de/index.php?id=load


A. Licensing
No license has been assigned so far. You may use this code freely for research purposes and/or personal use.
If in doubt, please contact us.


B. Documentation
This code is a fully working yet preliminary implementation of the LOAD model for event and entity extraction, browsing and summarization. Since it is still in development, the documentation is sparse in some places. Therefore, please do not hesitate to contact us if you run into trouble.


C. Dependencies
The code requires three libraries:

1. The Snowball porter stemmer
http://snowball.tartarus.org/

2. The MongoDb Java driver v3.0 or higher
https://docs.mongodb.com/ecosystem/drivers/java/

3. The Trove collection library v3.0 or higher
http://trove.starlight-systems.com/

The libraries are enclosed in this version. However, we recommend that you download the latest versions at the provided URLs.


D. Contents
The implementation can be split into roughly four parts:
(1) The algorithm for creating a LOAD graph
(2) A console-based query interface
(3) Tools for exporting the graph

LOAD graphs that can be used with the interface are available as separate downloads at
http://dbs.ifi.uni-heidelberg.de/index.php?id=load

(1) Algorithm for creating a LOAD graph
This can be found in ParallelExtractNetworkFromMongo.java.
Input data for the network creation should be stored in a mongoDB. To generate the network, the LOAD code requires two collections: one for the sentences and one for the annotations. These have to be generated by annotating named entities (locations, organizations, actors and dates) in the corpus for which the LOAD network should be constructed. The following information is required:

The collection of sentences should include
* the sentence text
* a consecutive numbering of sentences of each page
* the page ID (document ID) for each sentence

The collection of annotations should contains annotations
of type person, location, organization and date, as well as
* the sentence for each annotation
* the page to which it belongs
* begin offset
* end offset
* cover text

To change the settings for connecting to your instance of MongoDB, please refer to SystemSettings.java. We give a brief overview of the required settings for accessing the two collections in the following.

(1.1) Sentence collection:
For each sentence, the algorithm needs to know which page the sentence belongs to and which content the sentence has. The collection is required to have the following keys (these correspond to the settings in SystemSettings.java).

mongoIdentSentence_id = "_id"; (integer)
This is the unique identifier of the sentence collection in mongoDB and always has to be called "_id". Each sentence should have a unique integer ID. Note that by default, mongoDB will not assign integers as _id, so this has to be done manually when you inpuit your data to the mongoDB.

mongoIdentSentence_wikiId = "WP_page_id"; (integer)
This identifies the page (document) that a sentence belongs to (note that the LOAD code never reads the documents themselves but sentences have to be grouped by document).

mongoIdentSentence_sentenceId = "sen_number_page"; (integer)
This is the unique ID of a sentence *within* a page (document). Here, it is required that all sentences of a document are consecutively numbered (it does not matter whether they start at 0 or 1). This is used for computing sentence distances of named entities.

mongoIdentSentence_content = "content"; (String)
This entry should contain the text of the sentence as a string.

An example of an entry in the sentence collection would be the following:

{
"_id" : 0,
"WP_page_id" : 590,
"sen_number_page" : 1,
"content" : "Austin is the capital of Texas in the United States."
}

(1.2) Annotation collection:
This collection contains all annotations of entities that occur in all sentences of all documents. Therefore, it contains a number of "foreign keys" to other collections.

mongoIdentAnnotation_id = "_id";
This is the unique identifier of the sentence collection in mongoDB and always has to be called "_id".

mongoIdentAnnotation_wikiId = "WP_page_id"; (integer)
This identifies the page (document) that an annotation belongs to. It is the equivalent of "mongoIdentSentence_wikiId" and should point to the same page as the sentence to which the annotation belongs.

mongoIdentAnnotation_coveredText = "coveredText"; (String)
This is the text of the annotation as it occurs in the document. For example, if the Entity "Barack Obama" is mentioned in the text as "President Obama", then the covered Text is "President Obama".

mongoIdentAnnotation_normalized = "normalized"; (String)
The is the normalized form of the entity. For example, if the Entity "Barack Obama" is mentioned in the text as "President Obama" which has Wikidata ID "Q76", then the normalized form based on Wikidata could be "Barack Obama" or "Q76", depending on how normalization is done.

mongoIdentAnnotation_sentenceId = "sen_id"; (integer)
This is the unique identifier of the sentence inside the page that contains the annotation. Thus, it is a kind of foreign key to the sentence collection and corresponds to "sen_number_page".

mongoIdentAnnotation_start = "start_sen"; (integer)
This gives the position of the first character of the covered text in the sentence.

mongoIdentAnnotation_end = "end_sen";  (integer)
This gives the position of the character after the last character of the covered text in the sentence.
(i.e., a standard implementation of a substring method with substring(start_sen, end_sen) should extract the cover text.)

mongoIdentAnnotation_neClass = "neClass"; (String)
This is the named entity class of the annotation (location, organization, actor or date).

mongoIdentAnnotation_tool = "tool";
This should contain the tool which was used to extract the annotations. Unless I am completely mistaken, this field is not used anymore. Only neClass is used to distinguish between annotations.

An Example of an annotation entry looks like the following (corresponding to the above example sentence):

{
"_id" : { "$oid" : "577d02174268fd5db39af8f3" },
"coveredText" : "Austin",
"neClass" : "LOC",
"sen_id" : 1,
"start_sen" : 0,
"end_sen" : 6
"normalized" : "Q16559",
"WP_page_id" : 590
}

(1.3) Remarks about entity data
Dates have to be in the format YYYY-MM-DD, YYYY-MM or YYYY to be parsed correctly. For dates, you always have to use the mongoIdentAnnotation_normalized field for building the graph.

Locations, organizations or actors can be linked to a knowledge base and thus normalized (in which case you should use the mongoIdentAnnotation_normalized field). Otherwise, you can use the mongoIdentAnnotation_coveredText field, in which case I control characters and punctuation should be stripped via the available functions. Currently, these two options are not supported as a parameter but have to be changed manually (see MultiThreadWorker.java, line 166 - 172).


(2) The query interface uses the same graph settings as the algorithm. The graph is loaded into memory with the exception of the sentence collection. if support for sentences is to be enabled, the sentence collection must be available in a MongoDB collection.

The interface has an information function and help available after it is started. For details on query formulation, please refer to the original paper.


(3) A tool for importing the graph representation in a MongoDB is available in MoveLOADNetworkToMongoDB.java. Note that this simply stored the graph as an edge list and creates indices for improved lookup. If necessary, the code can quickly be adjusted to write this edge list format to any other target format.


E. References
If you use this code or approach, please consider citing us:

A. Spitz and M. Gertz
Terms over LOAD: Leveraging Named Entities for Cross-Document
Extraction and Summarization of Events.
Proceedings of the 39th International Conference on Research
and Development in Information Retrieval (SIGIR '16).
doi: 10.1145/2911451.2911529

An authors version of the article is available at
http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/aspitz/publications/Spitz_Gertz_2016_Terms_over_LOAD.pdf


