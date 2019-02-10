/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import IndonesianNLP.IndonesianNETagger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.w3c.dom.css.Counter;
import parser.CorpusParser;
import parser.XMLCorpusParser;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 *
 * @author Gilang
 */
public class CoreferenceResolution {
    
    static List<Node> phrases;
    static List<Node> sentences;
    static Instances dataset;
    static Classifier classifier;
    static int counter = 0;
    static int sentencesIdx = 0;
    static BufferedWriter writer, xmlWriter, tempWriter;
    
    public static void main(String[] args) throws Exception{
//        addCoreference("corpus_ne_simple_reweight.txt.xml", "coref_rf19_big_inference.model", "feature_header_complete_inference.arff", 6, "training_result_rf19");
//        addCoreference("corpus2_20.xml", "coref_rf19_80_inference.model", "feature_header_8019.arff", 6, "20_result_rf19_80_inference");
//        addCoreference("test3ne.xml", "coref_j4816_medium.model", "feature_header16.arff", 6, "test3ne");
//        generateKeyLabel("corpus_ne_simple_reweight_coref_inference_20.xml", "coref_key_inference_20.txt");
//        generateKeyLabel("test_result_stem_coref2.xml", "coref_key_test2.txt");
//        generateKeyLabel("corpus_coref2_20.xml", "coref_key_20.txt");
//        resolveCoreference("test_no_tag.txt", "test_result_rf19_medium");
        resolveCoreference("test4.txt", "test4");
    }
    
    public static void resolveCoreference(String inputFile, String outputFile) throws Exception{
        CorpusParser parser = new CorpusParser();
        parser.rawToXML(inputFile, outputFile + "tmp1.xml");
        
        XMLCorpusParser xmlParser = new XMLCorpusParser();
        xmlParser.completeXMLTag(outputFile + "tmp1.xml", outputFile + "tmp2.xml");
        xmlParser.getRawText(outputFile + "tmp2.xml", outputFile + "raw.txt");
        
        IndonesianNETagger neTagger = new IndonesianNETagger();
        neTagger.NETagFileRF(outputFile + "raw.txt", outputFile + "ne.txt");
        
        xmlParser.insertNETag(outputFile + "tmp2.xml", outputFile + "ne.txt", outputFile + "ne.xml");
//        addCoreference(outputFile + "ne.xml", "coref_rf19_big_inference_stem.model", "feature_header_complete_inference_stem.arff", 6, outputFile + "_coref.xml");
//        addCoreference(outputFile + "ne.xml", "coref_rf19_big.model", "feature_header_complete_inference.arff", 6, outputFile + "_coref.xml");
        addCoreference(outputFile + "ne.xml", "coref_j4816_medium.model", "feature_header16.arff", 6, outputFile + "_coref.xml");
        
        File tmp1 = new File(outputFile + "tmp1.xml");
        File tmp2 = new File(outputFile + "tmp2.xml");
        File tmp3 = new File(outputFile + "raw.txt");
        File tmp4 = new File(outputFile + "ne.txt");
        File tmp5 = new File(outputFile + "ne.xml");
        
        tmp1.delete();
        tmp2.delete();
        tmp3.delete();
        tmp4.delete();
        tmp5.delete();
    }
    
    public static void addCoreference(String xmlFile, String modelFile, String headerFile, int maxSentence, String outputFile) throws Exception{
        // load xml file
        File file = new File(xmlFile);
        SAXReader reader = new SAXReader();
	Document document = reader.read(file);
        
        writer = new BufferedWriter(new FileWriter(outputFile + ".txt"));
        xmlWriter = new BufferedWriter(new FileWriter(outputFile + ".xml"));
        tempWriter = new BufferedWriter(new FileWriter("temp_classification.txt"));
        
        dataset = new Instances(new BufferedReader(new FileReader(headerFile)));
        dataset.setClassIndex(dataset.numAttributes() - 1);
        classifier = (Classifier) weka.core.SerializationHelper.read(modelFile);
        
        sentences = new ArrayList<>();
        sentences.addAll(document.selectNodes("/data/sentence"));
        
        int upperBound = sentences.size() - maxSentence;
        int maxStep = maxSentence;
        if(sentences.size() - maxSentence < 0){
            upperBound = 1;
            maxSentence = sentences.size();
        }
        
        for(int x=0; x<upperBound; x++){
            int phrasesCount = sentences.get(x).selectNodes("phrase").size();
            
            phrases = new ArrayList<>();
            for(int j=x; j<x+maxSentence; j++){
                phrases.addAll(sentences.get(j).selectNodes("phrase"));
            }
            
            FeatureExtractor.setPunctuationIndex(phrases);
            FeatureExtractor.setCurrentPhrases(phrases);

            for(int i=0; i<phrasesCount; i++){
                Node p = phrases.get(i);
                Node p2 = null;
                if(!p.valueOf("@type").contains("np"))
                    continue;
                for(int j=i+1; j<phrases.size(); j++){
                    p2 = phrases.get(j);
                    if(!p2.valueOf("@type").contains("np")){
                        p2 = null;
                        continue;
                    }
                    if(p2 != null && (p.getText().contains("\\NNP") || p.getText().contains("\\PRP"))
                            && (p2.getText().contains("\\NNP") || p2.getText().contains("\\PRP"))){
                        List<String> features = FeatureExtractor.extractFeatures(p, p2, i, j);
                        classifyInstance(features, p, p2);
                    }
                }
            }
        }
        xmlWriter.write(document.asXML());
        xmlWriter.close();
        writer.close();
        tempWriter.close();
    }
    
    public static void classifyInstance(List<String> features, Node n1, Node n2) throws Exception{
        DenseInstance instance = new DenseInstance(dataset.numAttributes());
        instance.setDataset(dataset);
        for(int i=0; i<features.size(); i++){
//            tempWriter.write(features.get(i) + ", ");
            if(i == 9){
                instance.setValue(i, Integer.valueOf(features.get(i)));
            }else if(i > 15 && i < 19){
                try{
                    instance.setValue(i, features.get(i).replace("\"", ""));
                }catch(IllegalArgumentException e){
//                    String mergedValue = dataset.attribute(i).value(0);
//                    instance.setValue(i, mergedValue);
                      instance.setValue(i, "null");
                }
            }else{
                instance.setValue(i, features.get(i));
            }
        }
        double classLabel = classifier.classifyInstance(instance);
        tempWriter.write(instance.toString() + ", " + n1.getText() + ", " + n2.getText() + ", " + n1.valueOf("@id") + ", " + n2.valueOf("@id") + ", ");
        tempWriter.write(classLabel + "\n");
//        System.out.println(classLabel);
        if(classLabel > 0){
//            writer.write(n1.getText() + ", " + n2.getText() + "|||||" + instance.toString() + "\n");
            writer.write(n1.valueOf("@id") + ", " + n2.valueOf("@id") + ", " + n1.getText() + ", " + n2.getText() + "\n");
            Element e = (Element)n2;
            String lastCoref = n2.valueOf("@coref");
            if(lastCoref.length() == 0)
                e.setAttributeValue("coref", n1.valueOf("@id"));
            else
                e.setAttributeValue("coref", lastCoref + "|" + n1.valueOf("@id"));
        }
    }
    
    public static void generateKeyLabel(String xmlFile, String outputFile) throws Exception{
        File file = new File(xmlFile);
        SAXReader reader = new SAXReader();
	Document document = reader.read(file);
        
        writer = new BufferedWriter(new FileWriter(outputFile));
        List<Node> allPhrases = document.selectNodes("/data/sentence/phrase");
        
        for(Node phrase : allPhrases){
            String corefId = phrase.valueOf("@coref");
            if(corefId != null && corefId.length() > 0){
                String firstCoref = corefId.split("\\|")[0];
                Node node = document.selectSingleNode("//phrase[@id='" + firstCoref + "']");
                writer.write(corefId + ", " + phrase.valueOf("@id") + ", " + node.getText() + ", " + phrase.getText() + "\n");
            }
        }
        System.out.println("lalala");
        writer.close();
    }
    
}
