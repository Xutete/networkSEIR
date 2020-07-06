/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author micha
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException
    {
        /*
        String[] scenarios = new String[]{"MN_start80"};
        
        for(String x : scenarios)
        {
            Network network = new Network(x);
            //network.gradientDescent();
            System.out.println(network.randomStart(10));
            //network.printTotalError();
        }
                    //network.calcAvgValues(0, 9);
        */
        
        int num_threads = 1;
        
        for(int i = 0; i < num_threads; i++)
        {
            final int start_run = i*10;
            
            Thread t = new Thread()
            {
                public void run()
                {
                    try
                    {
                        Network network = new Network("MN_travel");
                        //network.gradientDescent();
                        network.randomStart(1, start_run);
                    }
                    catch(IOException ex)
                    {
                        ex.printStackTrace(System.err);
                    }
                }
            };
            
            t.start();
            

            
        }
        
        
        

    }
    
}
