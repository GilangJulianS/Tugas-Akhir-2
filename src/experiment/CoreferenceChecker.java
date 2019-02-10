/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Gilang
 */
public class CoreferenceChecker {
    
    public static void main(String[] args) throws Exception{
//        check("result.txt", "coref_key_inference.txt");
//        check("result4coref.xml.txt", "coref_key_stem.txt");
//        generateCorefChain("coref_key_test2.txt", "coref_chain_key_test2.txt");
//        generateCorefChain("test_result_rf19_coref.xml.txt", "coref_chain_rf19_test.txt");
//        checkChain("coref_chain_inference.txt", "coref_chain_key_inference.txt");
        mucScoring("test_result_j489_small_coref.xml.txt", "coref_key_test2.txt");
//        mucScoring("training_result_j4816_big2.txt", "coref_key_training2.txt");
    }
    
    public static void check(String inputFilePath, String keyFilePath) throws Exception{
        int yesAsYes = 0;
        int yesAsNo = 0;
        int noAsYes = 0;
        boolean continue1 = true;
        boolean continue2 = true;
        
        BufferedReader inputFile = new BufferedReader(new FileReader(inputFilePath));
        BufferedReader keyFile = new BufferedReader(new FileReader(keyFilePath));
        
        String line1 = inputFile.readLine();
        String line2 = keyFile.readLine();
        while(continue1 && continue2){
            String[] indexes1 = line1.split(", ");
            String[] indexes2 = line2.split(", ");
            int in1 = Integer.valueOf(indexes1[0]);
            int in2 = Integer.valueOf(indexes1[1]);
            String[] keys1 = indexes2[0].split("\\|");
            int key2 = Integer.valueOf(indexes2[1]);
            
            boolean equals = false;
            boolean greater = false;
            
            int firstKey = Integer.valueOf(keys1[0]);
            if(firstKey < in1){
                greater = true;
            }
            
            for(String key : keys1){
                int key1 = Integer.valueOf(key);
                if(key1 == in1){
                    equals = true;
                    break;
                }
            }
            
            if(equals && in2 == key2){
                yesAsYes++;
                if((line1 = inputFile.readLine()) == null) continue1 = false;
                if((line2 = keyFile.readLine()) == null) continue2 = false;
            }else if(greater || in2 > key2){
                yesAsNo++;
                if(!continue2){
                    if((line1 = inputFile.readLine()) == null) continue1 = false;
                    continue;
                }
                if((line2 = keyFile.readLine()) == null) continue2 = false;
            }else if(!greater || key2 > in2){
                noAsYes++;
                if(!continue1){
                    if((line2 = keyFile.readLine()) == null) continue2 = false;
                    continue;
                }
                if((line1 = inputFile.readLine()) == null) continue1 = false;
            }
        }
        
        inputFile.close();
        keyFile.close();
        
        System.out.println("YES as YES : " + yesAsYes);
        System.out.println("YES as NO : " + yesAsNo);
        System.out.println("NO as YES : " + noAsYes);
        System.out.println("Precision : " + (100*(float)yesAsYes/(yesAsYes + noAsYes)) + "%");
        System.out.println("Recall : " + (100*(float)yesAsYes/(yesAsYes + yesAsNo)) + "%");
    }
    
    public static void generateCorefChain(String inputFile, String outputFile) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        
        List<List<Integer>> chains = new ArrayList<>();
        
        String line;
        while((line = reader.readLine()) != null){
            boolean found = false;
            String[] columns = line.split(", ");
            String column1 = columns[0];
            String column2 = columns[1];
            String index1 = column1;
            if(column1.contains("|")){
                String[] indexes = column1.split("\\|");
                index1 = indexes[0];
            }
            int idx1 = Integer.valueOf(index1);
            int idx2 = Integer.valueOf(column2);
            System.out.println(idx1 + " " + idx2);
            for(List<Integer> chain : chains){
                if(chain.contains(idx1)){
                    chain.add(idx2);
                    found = true;
                }else if(chain.contains(idx2)){
                    chain.add(idx1);
                    found = true;
                }
            }
            if(!found){
                List<Integer> newChain = new ArrayList<>();
                newChain.add(idx1);
                newChain.add(idx2);
                chains.add(newChain);
            }
        }
        
        for(List<Integer> chain : chains){
            for(int i=0; i<chain.size(); i++){
                if(i != chain.size() - 1){
                    writer.write(chain.get(i) + ", ");
                }else{
                    writer.write(chain.get(i) + "\n");
                }
            }
        }
        
        reader.close();
        writer.close();
    }
    
    public static void mucScoring(String responseFile, String keyFile) throws Exception{
        List<List<Integer>> response = generateCorefChain(responseFile);
        List<List<Integer>> key = generateCorefChain(keyFile);
        
        float precision = calculate(key, response);
        float recall = calculate(response, key);
        System.out.println("precision : " + precision);
        System.out.println("recall : " + recall);
        System.out.println("f-measure : " + ((2 * precision * recall)/(precision + recall)));
        
    }
    
    public static List<List<Integer>> generateCorefChain(String fileName) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        
        List<List<Integer>> chains = new ArrayList<>();
        
        String line;
        while((line = reader.readLine()) != null){
            boolean found = false;
            String[] columns = line.split(", ");
            String column1 = columns[0];
            String column2 = columns[1];
            String index1 = column1;
            if(column1.contains("|")){
                String[] indexes = column1.split("\\|");
                index1 = indexes[0];
            }
            int idx1 = Integer.valueOf(index1);
            int idx2 = Integer.valueOf(column2);
//            System.out.println(idx1 + " " + idx2);
            for(List<Integer> chain : chains){
                if(chain.contains(idx1)){
                    chain.add(idx2);
                    found = true;
                }else if(chain.contains(idx2)){
                    chain.add(idx1);
                    found = true;
                }
            }
            if(!found){
                List<Integer> newChain = new ArrayList<>();
                newChain.add(idx1);
                newChain.add(idx2);
                chains.add(newChain);
            }
        }
        
        reader.close();
        
        return chains;
    }
    
    public static void checkChain(String responseFile, String keyFile) throws Exception{
        List<List<Integer>> response = parseChain(responseFile);
        List<List<Integer>> key = parseChain(keyFile);
        
        System.out.println("recall : " + calculate(response, key));
        System.out.println("precision : " + calculate(key, response));
    }
    
    // calculate(response, key) to calculate recall
    // calculate(key, response) to calculate precision
    public static float calculate(List<List<Integer>> responseChains, List<List<Integer>> keyChains){
        List<List<Integer>> chains1 = copyChains(responseChains);
        List<List<Integer>> chains2 = copyChains(keyChains);
        int numerator = 0, denominator = 0;
        for(List<Integer> key : chains2){
            numerator += key.size();
            denominator += key.size() - 1;
            for(List<Integer> response : chains1){
                int initialSize = key.size();
                key.removeAll(response);
                int finalSize = key.size();
                if(initialSize != finalSize){
                    numerator--;
                }
            }
            numerator -= key.size();
        }
        float recall = (float)numerator/denominator;
        return recall;
    }
    
    public static List<List<Integer>> copyChains(List<List<Integer>> chains){
        List<List<Integer>> newChains = new ArrayList<>();
        for(List<Integer> chain : chains){
            List<Integer> newChain = new ArrayList<>();
            for(int i : chain){
                newChain.add(i);
            }
            newChains.add(newChain);
        }
        return newChains;
    }
    
    public static List<List<Integer>> parseChain(String fileName) throws Exception{
        List<List<Integer>> chains = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        
        String line;
        
        while((line = reader.readLine()) != null){
            String[] indexes = line.split(", ");
            List<Integer> chain = new ArrayList<>();
            
            for(String index : indexes){
                int idx = Integer.valueOf(index);
                if(!chain.contains(idx)){
                    chain.add(idx);
                }
            }
            chains.add(chain);
        }
        return chains;
    }
}
