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

/**
 *
 * @author Gilang
 */
public class Util {
    
    public static void main(String[] args) throws Exception{
        deleteNN("test_result_j4816_medium_coref.xml.txt", "testj4816.txt");
    }
    
    public static void deleteNN(String fileName, String output) throws Exception{
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        
        String line;
        while((line = reader.readLine()) != null){
            if(line.contains("NN") && !line.contains("NNP")){
                
            }else{
                writer.write(line + "\n");
            }
        }
        reader.close();
        writer.close();
    }
}
