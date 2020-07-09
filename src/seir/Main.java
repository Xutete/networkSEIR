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
                        
                        network.load(0);
                        
                        double obj = network.calculateSEIR();
                        
                        System.out.println(obj);
                        
                        network.gradientDescent();
                        //network.randomStart(10, start_run);
                        
                        
                        //network.printTotalError();
                        
                        /*
                        network.load(0);
                        PlotErrors test = new PlotErrors(network);
                        
                        Zone z = network.findZone(27005);
                        
                        double obj = network.calculateSEIR();
                        
                        network.calculateGradient_r(z);

                        
                        double step = network.calculateStep(1, obj);
                        
                        for(int pi = 0; pi < network.r_periods.length; pi++)
                        {
                            System.out.println(pi+" "+network.r_periods[pi]+"\t"+z.r[pi]+"\t"+z.gradient_r[pi]+"\t"+(z.r[pi]-step*z.gradient_r[pi]));
                        }
                        
                        System.out.println(step);
                        */
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
