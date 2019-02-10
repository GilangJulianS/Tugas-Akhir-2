/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import IndonesianNLP.IndonesianStemmer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 *
 * @author macair
 */
public class FeatureExtractor {
    
    static BufferedWriter bw;
    static List<Integer> dotIdx;
    static List<Integer> commaIdx;
    static List<Integer> quoteIdx;
    static List<Node> allPhrase;
    static List<Node> currentActivePhrases;
    static HashMap<String, Integer> phraseMap;
    static List<List<Integer>> corefChain;
    static int maxPhraseIndex;
    static IndonesianStemmer stemmer = new IndonesianStemmer();
    static String[] values = {"di", "dari", "mencapai", "dan", "dengan", "kata", "ini", "karena", "kepada",
                                "mengatakan", "untuk", "menjadi", "selama", "dalam", "pada", "oleh", "sebagai",
                                "nya", "ke", "bahwa", "itu", "per", "tumbuh", "yang", "akan", "juga", "tidak", 
                                "baru", "masih", "tersebut", "telah", "atau", "tambah", "ujar", "menurut", "maka"};
    
    public static void main(String[] args) throws Exception{
//	bw = new BufferedWriter(new FileWriter("feature_complete_inference_stem.arff"));
//	File file = new File("corpus_ne_simple_reweight_coref_inference.xml");
        bw = new BufferedWriter(new FileWriter("feature80_noinference.arff"));
	File file = new File("corpus_coref2_80.xml");
        SAXReader reader = new SAXReader();
	Document document = reader.read(file);
	
	List<Node> sentences = document.selectNodes("/data/sentence");
        phraseMap = new HashMap<>();
        allPhrase = new ArrayList<>();
        allPhrase.addAll(document.selectNodes("/data/sentence/phrase"));
        
        loadCorefChain("coref_key_training2.txt");
        mapIndex();
        
        for(int i=0; i<allPhrase.size(); i++){
            Node phrase = allPhrase.get(i);
            Element phraseElement = (Element) phrase;
            String coref = phraseElement.attributeValue("coref");
            String corefId = getSmallestCorefId(coref);
//            System.out.println(corefId);
            if(corefId != null){
                int startIdx = phraseMap.get(corefId);
                int endIdx = i;
                List<Node> phrases = new ArrayList<>();
                for(int j=startIdx; j<=endIdx; j++){
                    phrases.add(allPhrase.get(j));
                }
                process(phrases);
            }
        }
        
//        for(int i=0; i<sentences.size() - 10; i++){
//            System.out.println(i);
//            List<Node> phrases = new ArrayList<>();
//            for(int j=i; j<i+10; j++){
//                phrases.addAll(sentences.get(j).selectNodes("phrase"));
//                if(j == i){
//                    maxPhraseIndex = phrases.size();
//                }
//            }
//            process(phrases);
//        }
	
	bw.close();
    }
    
    public static void loadCorefChain(String fileName) throws Exception{
        corefChain = CoreferenceChecker.generateCorefChain(fileName);
    }
    
    public static boolean isCoref(int idx1, int idx2){
        boolean found1, found2;
        for(List<Integer> chain : corefChain){
            found1 = false;
            found2 = false;
            for(int i : chain){
                if(i == idx1){
                    found1 = true;
                }
                if(i == idx2){
                    found2 = true;
                }
            }
            if(found1 && found2){
                return true;
            }
        }
        return false;
    }
    
    public static void process(List<Node> phrases) throws Exception{
        setPunctuationIndex(phrases);
	extractFeatureToArff(phrases);
    }
    
    public static void setPunctuationIndex(List<Node> phrases){
        quoteIdx = new ArrayList<>();
        dotIdx = new ArrayList<>();
        commaIdx = new ArrayList<>();
        for(int i=0; i<phrases.size(); i++){
            String[] words = phrases.get(i).getText().split(" ");
            String phraseString = words[words.length-1].split("\\\\")[0];
            if(phraseString.endsWith(".")){
                dotIdx.add(i);
            }else if(phraseString.endsWith(",")){
                commaIdx.add(i);
            }else if(phraseString.equals("\"")){
                quoteIdx.add(i);
            }
        }
    }
    
    public static void extractFeatureToArff(List<Node> phrases) throws Exception{
        currentActivePhrases = phrases;
	for(int i=0; i<=maxPhraseIndex; i++){
	    Node p = phrases.get(i);
	    Node p2 = null;
	    if(!p.valueOf("@type").contains("np"))
		continue;
	    for(int j=i+1; j<phrases.size(); j++){
//                int j = phrases.size() - 1;
		p2 = phrases.get(j);
		if(!p2.valueOf("@type").contains("np")){
		    p2 = null;
		    continue;
		}
		if(p2 != null){
		    List<String> features = extractFeatures(p, p2, i, j);
                    for(String feature : features){
                        bw.write(feature + ", ");
                    }
//		    bw.write(p.getText() + ", " + p2.getText() + ", ");
//                    bw.write(p.valueOf("@id") + ", " + p2.valueOf("@id") + ", ");
                    bw.write(extractLabelByChain(p, p2) + "\n");
		}
	    }
	}
    }
    
    public static String extractLabel(Node n1, Node n2){
        boolean isCoref = false;
        
        Element e1 = (Element)n1;
        Element e2 = (Element)n2;
        String id = e1.attributeValue("id");
        String coref = e2.attributeValue("coref");
        
        if(coref != null){
            String[] corefs = coref.split("\\|");
            for(String c : corefs){
                if(c.equals(id)){
                    isCoref = true;
                }
            }
        }
        
        if(isCoref) return "YES";
        return "NO";
    }
    
    public static String extractLabelByChain(Node n1, Node n2){
        Element e1 = (Element)n1;
        Element e2 = (Element)n2;
        int id1 = Integer.valueOf(e1.attributeValue("id"));
        int id2 = Integer.valueOf(e2.attributeValue("id"));
        if(isCoref(id1, id2)) return "YES";
        return "NO";
    }
    
    // substring match, ne match, s1 pronoun, s2 pronoun, s1 proper name, s2 proper name
    // distance, apositif, s1 first person, s1 first person, s1 quotation, s2 quotation, nearest
    public static List<String> extractFeatures(Node n1, Node n2, int idx1, int idx2) throws Exception{
        List<String> features = new ArrayList<>();
        features.add(extractFeature01(n1.getText(), n2.getText()) + ""); // exact match
        features.add(extractFeature001(n1.getText(), n2.getText()) + ""); // exact word match
	features.add(extractFeature1(n1.getText(), n2.getText()) + ""); // substring
        features.add(extractFeature2(n1) + ""); // ne 1
        features.add(extractFeature2(n2) + ""); // ne 2
	features.add(extractFeature3(n1.getText()) + ""); // pronoun
        features.add(extractFeature3(n2.getText()) + ""); // pronoun
	features.add(extractFeature4(n1.getText()) + ""); // proper name
        features.add(extractFeature4(n2.getText()) + ""); // proper name
        features.add(getDistance(n1.getText(), n2.getText(), idx1, idx2) + ""); // distance
        features.add(extractFeature6(n1.getText(), idx1, idx2) + ""); // appositive
        features.add(extractFeature8(n1.getText()) + ""); // first person
        features.add(extractFeature8(n2.getText()) + ""); // fist person
        features.add(extractFeature9(idx1) + ""); // quotation
        features.add(extractFeature9(idx2) + ""); // quotation
        features.add(extractFeature10(idx1, idx2) + ""); // nearest
//        features.add(extractFeature11(idx1) + ""); // prev word
//        features.add(extractFeature12(idx1) + ""); // next word
//        features.add(extractFeature11(idx2) + ""); // prev word
        return features;
    }
    
    public static int getDistance(String phrase1, String phrase2, int phraseIdx1, int phraseIdx2){
        int counter = 0;
//        System.out.println(phrase1 + " " + phrase2);
        for(int i=0; i<dotIdx.size(); i++){
//            System.out.println(phraseIdx1 + " " + dotIdx.get(i) + " " + phraseIdx2 + " " + counter);
            if(phraseIdx1 > dotIdx.get(i)){
                continue;
            }else if(phraseIdx2 <= dotIdx.get(i)){
                break;
            }else{
                counter++;
            }
        }
        return counter;
    }
    
    // fitur exact match
    public static boolean extractFeature01(String s1, String s2){
        String str1 = s1.replaceAll("\\\\[\\w]+", "");
	String str2 = s2.replaceAll("\\\\[\\w]+", "");
        if(str1.toLowerCase().equals(str2.toLowerCase())){
            return true;
        }
        return false;
    }
    
    // fitur exact match per word
    public static boolean extractFeature001(String s1, String s2){
        String str1 = s1.replaceAll("\\\\[\\w]+", "");
	String str2 = s2.replaceAll("\\\\[\\w]+", "");
	String[] a1 = str1.split(" ");
	String[] a2 = str2.split(" ");
	boolean found = false;
	for(String a : a1){
	    for(String b : a2){
		if(a.toLowerCase().equals(b.toLowerCase())){
		    found = true;
		    break;
		}
	    }
	    if(found)
		break;
	}
	return found;
    }
    
    // fitur substring match
    public static boolean extractFeature1(String s1, String s2){
//      fitur substring per kata
	String str1 = s1.replaceAll("\\\\[\\w]+", "");
	String str2 = s2.replaceAll("\\\\[\\w]+", "");
	String[] a1 = str1.split(" ");
	String[] a2 = str2.split(" ");
	boolean found = false;
	for(String a : a1){
	    for(String b : a2){
		if(a.contains(b) || b.contains(a)){
		    found = true;
		    break;
		}
	    }
	    if(found)
		break;
	}
	return found;
//        String str1 = s1.replaceAll("[.,'\"\\-:]", "").toLowerCase();
//        String str2 = s2.replaceAll("[.,'\"\\-:]", "").toLowerCase();
//        if(str1.contains(str2) || str2.contains(str1)){
//            return true;
//        }
//        return false;
    }
    
    // fitur same entity type
//    public static boolean extractFeature2(Node n1, Node n2){
//	String[] neList1 = n1.valueOf("@ne").split("\\|");
//        String[] neList2 = n2.valueOf("@ne").split("\\|");
//        boolean match = false;
//        for(String ne1 : neList1){
//            for(String ne2 : neList2){
//                if(ne1.equals(ne2) && !ne1.equals("OTHER")){
//                    match = true;
//                    break;
//                }
//            }
//            if(match){
//                break;
//            }
//        }
//        return match;
//    }
   
//    fitur entity type
    public static String extractFeature2(Node n){
        String[] neList = n.valueOf("@ne").split("\\|");
        String neFinal = null;
        String[] words = n.getText().split(" ");
        boolean isPronoun = false;
        for(int i=0; i<words.length; i++){
            if(isPronoun(words[i].split("\\\\")[0]) || words[i].contains("PRP")){
                isPronoun = true;
                break;
            }
        }
        if(isPronoun){
            return "PERSON";
        }
        for(int i=0; i<neList.length; i++){
            String tempNE = neList[i];
            String word = words[i];
            if(word.contains("NNP")){
                neFinal = tempNE;
                break;
            }else if(neFinal == null || neFinal.equals("OTHER")){
                neFinal = tempNE;
            }
        }
        return neFinal;
    }
    
    // fitur isPronoun
    public static boolean extractFeature3(String s){
        String s2 = s.split("\\\\")[0];
	if(s.contains("\\PRP") || isPronoun(s2)){
	    return true;
	}
        return false;
    }
    
    public static boolean isPronoun(String s){
        String s2 = s.replaceAll("[,./?:;'\"]+", "");
        if(s2.equals("dia") || s2.equals("mereka") || 
                s2.equals("ia") || s2.equals("kami") || s2.equals("saya") || 
                s2.equals("aku") || s2.equals("kita")){
            return true;
        }
        return false;
    }
    
    // fitur is proper name
    public static boolean extractFeature4(String s){
	if(s.contains("\\NNP")){
	    return true;
	}
        return false;
    }
    
    // fitur apositif
    public static boolean extractFeature6(String s1, int idx1, int idx2){
        // frase bersebelahan dan frase 1 diakhiri tanda koma
        if(idx2 - idx1 == 1 && s1.split("\\\\")[0].endsWith(",")){
            return true;
        }
        return false;
    }
    
//    public static boolean extractFeature7(Node n1, Node n2){
//        
//    }
    
    // fitur first person
    public static boolean extractFeature8(String str){
        String s = str.split("\\\\")[0].toLowerCase().replaceAll("[,./?:;'\"]+", "");;
        return (s.equals("aku") || s.equals("saya"));
    }
    
    // fitur in quotation
    public static boolean extractFeature9(int strIdx){
        int openIdx = -1, closeIdx = -1;
        for(int i=0; i<quoteIdx.size(); i++){
            if(openIdx == -1){
                openIdx = quoteIdx.get(i);
            }else if(closeIdx == -1){
                closeIdx = quoteIdx.get(i);
            }else{
                if(openIdx <= strIdx && closeIdx >= strIdx){
                    return true;
                }else{
                    openIdx = -1;
                    closeIdx = -1;
                }
            }
        }
        return false;
    }
    
    // fitur nearest candidate
    public static boolean extractFeature10(int idx1, int idx2){
        return idx2 - idx1 == 1;
    }
    
    // kata sebelum
    public static String extractFeature11(int idx){
        if(idx == 0 || (idx > 0 && isEndOfSentence(currentActivePhrases.get(idx - 1)))){
            return "\"null\"";
        }else{
            String prevPhrase = currentActivePhrases.get(idx - 1).getText().replaceAll("\\\\[\\w]*", "");
            String[] words = prevPhrase.split(" ");
            String prevWord = words[words.length - 1];
            prevWord = prevWord.replaceAll("[,./?:;'\"]+", "");
            if(prevWord.length() == 0 || prevWord == null)
                prevWord = "null";
//            else
//                prevWord = stemmer.stem(prevWord);
            return "\"" + prevWord + "\"";
        }
    }
    
    // kata sesudah
    public static String extractFeature12(int idx){
        if(idx == currentActivePhrases.size() - 1 || isEndOfSentence(currentActivePhrases.get(idx))){
            return "\"null\"";
        }else{
            String nextWord = currentActivePhrases.get(idx + 1).getText().replaceAll("\\\\[\\w]*", "").split(" ")[0];
            nextWord = nextWord.replaceAll("[,./?:;'\"]+", "");
            if(nextWord.length() == 0 || nextWord == null)
                nextWord = "null";
//            else
//                nextWord = stemmer.stem(nextWord);
            return "\"" + nextWord + "\"";
        }
    }
    
    public static boolean isEndOfSentence(Node n){
        String phrase = n.getText();
        String[] words = phrase.split("\\\\");
        if(words.length > 1 && words[words.length - 2].endsWith(".")){
            return true;
        }
        return false;
    }
    
    public static void setCurrentPhrases(List<Node> phrases){
        currentActivePhrases = phrases;
    }
    
    private static String getSmallestCorefId(String corefId){
        if(corefId == null) return null;
        String smallestId = corefId;
        if(corefId.contains("|")){
            String[] corefsId = corefId.split("\\|");
            smallestId = corefsId[corefsId.length - 1];
//            smallestId = corefsId[0];
        }
        return smallestId;
    }
    
    private static boolean isInArray(String s, String[] arr){
        boolean found = false;
        for(String item : arr){
            if(s.toLowerCase().equals(item.toLowerCase())){
                found = true;
                break;
            }
        }
        return found;
    }
    
    private static void mapIndex(){
        int idx = 0;
        for(Node n : allPhrase){
            Element e = (Element)n;
            String id = e.attributeValue("id");
            if(id != null){
                phraseMap.put(id, idx);
            }
            idx++;
        }
    }
}
