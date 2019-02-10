package parser;

import java.io.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author macair
 * This class was used to create a new dataset to train named entity tagger
 */
public class XMLParser {
    
    static List<String> words;
    static List<Integer> nodeNumber;
    static List<String> entities;
    
    public static void main(String[] args) throws DocumentException, UnsupportedEncodingException, IOException{
        XMLParser parser = new XMLParser();
        parser.xmlToNETaggedTxt();
    }
    
    public XMLParser(){}
    
    /*
     * convert xml to (words)/(ne tag) format txt
     */
    public void xmlToNETaggedTxt() throws DocumentException, UnsupportedEncodingException, IOException{
        File dir = new File("ALL-Train");
        File[] files = dir.listFiles();
        for(File file : files){
            if(!file.getName().contains(".xml"))
                continue;
//            System.out.println(file.getName());
            words = new ArrayList<>();
            nodeNumber = new ArrayList<>();
            entities = new ArrayList<>();
            
//            System.out.println(file.getName());
            
            BufferedWriter bw = new BufferedWriter(new FileWriter("Output/" + file.getName() + ".txt"));
//            bw.write(file.getName());
            SAXReader reader = new SAXReader();
            Document document = reader.read(file);

            List<Node> anno = document.selectNodes("/GateDocument/AnnotationSet/Annotation");

            Node node = document.selectSingleNode("/GateDocument/TextWithNodes");
            String nodeText = node.asXML();
            parse(nodeText);


            for(Node n : anno){
                int startNode = Integer.valueOf(n.valueOf("@StartNode"));
                int endNode = Integer.valueOf(n.valueOf("@EndNode"));
                List<Node> childNodes = n.selectNodes("Feature");
                String entity = "";
                for(Node child : childNodes){
                    String type = child.selectSingleNode("Name").getText();
                    if(type.equals("class")){
                        entity = child.selectSingleNode("Value").getText();
                    }
                }
                if(!entity.equals("")){
                    tag(entity, startNode, endNode);
                }
            }

            for(String w : words){
                bw.write(w);
            }
            bw.close();
        }
    }
    
    public static void tag(String tag, int startNode, int endNode){
        for(int i=0; i<nodeNumber.size()-1; i++){
            String word = words.get(i);
            if(nodeNumber.get(i) >= startNode && nodeNumber.get(i+1) <= endNode && word.trim().length() > 0 && !word.contains("\\")){
                String newWord =  word + "\\" + tag + " ";
//                System.out.println(newWord);
                words.set(i, newWord);
            }
        }
    }
    
    public static void parse(String xml){
        int curIdx = 0;
        int endIdx = 0;
        while(endIdx < xml.length()){
            // find node ID
            curIdx = xml.indexOf('<', endIdx);
            endIdx = xml.indexOf('>', curIdx);
            if(curIdx != -1 && endIdx != -1){
                String temp = xml.substring(curIdx, endIdx + 1);
                int idx1, idx2;
                idx1 = temp.indexOf('\"');
                idx2 = temp.indexOf('\"', idx1 + 1);
                if(idx1 != -1 && idx2 != -1){
                    String nodeId = temp.substring(idx1+1, idx2);
                    nodeNumber.add(Integer.valueOf(nodeId));
//                    System.out.println(nodeId);
                }
            }else{
                break;
            }
            
            // find word
            curIdx = endIdx + 1;
            endIdx = xml.indexOf('<', curIdx);
            if(curIdx != -1 && endIdx != -1){
                if(curIdx != endIdx){
                    String word = xml.substring(curIdx, endIdx);
                    words.add(word);
//                    System.out.println(word + " " + words.size());
                }
            }else{
                break;
            }
        }
//        System.out.println(nodeNumber.size() + " " + words.size());
    }
}
