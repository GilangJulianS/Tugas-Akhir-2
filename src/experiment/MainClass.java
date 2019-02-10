/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import IndonesianNLP.IndonesianNETagger;
import IndonesianNLP.IndonesianPhraseChunker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author macair
 */
public class MainClass {
    
    public static void main(String[] args) {
        IndonesianNLP.IndonesianPhraseChunker c = new IndonesianPhraseChunker();
        IndonesianNLP.IndonesianNETagger n = new IndonesianNETagger();
//        int counter = 1;
//        IndonesianNLP.IndonesianNETagger neTagger = new IndonesianNETagger();
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter("result.txt"));
//            BufferedReader reader = new BufferedReader(new FileReader("preprocessed.txt"));
//            String line;
//            while((line = reader.readLine()) != null){
////                System.out.println(counter + " " + line);
//                counter++;
//                ArrayList<String> result = neTagger.extractNamedEntity(line);
//                String[] token = line.split("\\s");
//                for(int i=0; i<token.length; i++){
//                    String tag = "";
//                    if(i < result.size())
//                        tag = result.get(i);
//                    writer.write(token[i] + "/" + tag + " ");
//                }
//                writer.write("\n");
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(MainClass.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
