/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.JFrame;

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

        

        Network network = new Network("MN_model2");

        /*
        Network network = new Network("MN_model2_12");

        network.gradientDescent();
        
        network = new Network("MN_travel_12");

        network.gradientDescent();
        */
        
        network.load(0);
        
        //network.includeTravel = false;

        //double obj = network.calculateSEIR();
        
        /*
        PrintStream fileout = new PrintStream(new FileOutputStream(network.getDirectory()+"/output/total_cases.txt"), true);
        for(Zone i : network.getZones())
        {


            fileout.println(i.getId()+"\t"+i.getN());
        }
        fileout.close();
        */
        
        //network.printTotalError(new File(network.getDirectory()+"/output/total_error_0.txt"));

        //PlotErrors test = new PlotErrors(network);
        
        //network.printAverageRates();
        //network.colorZonesData(10);
        //network.colorZonesr(network.getStartTime()+40);
        //network.colorZonesI(50, network.T-1);
        network.colorZonesReportedI(10, network.T);


        CountyDisplay test2 = new CountyDisplay(network);
        
        
        
        
        /*


        double obj = network.calculateSEIR();

        System.out.println(obj);
        */

        //network.gradientDescent();
        //network.randomStart(10, start_run);


        //network.printTotalError();

        /*
        network.load("no_xiE");

        double obj = network.calculateSEIR();

        System.out.println(obj);
        */

        //PlotErrors test = new PlotErrors(network);

        //CountyDisplay test2 = new CountyDisplay(network);

        /*
        Zone z = network.findZone(27017);


        network.calculateGradient_r(z);


        double step = network.calculateStep(1, obj);

        for(int pi = 0; pi < network.r_periods.length; pi++)
        {
            System.out.println(pi+" "+network.r_periods[pi]+"\t"+z.r[pi]+"\t"+z.gradient_r[pi]+"\t"+(z.r[pi]-step*z.gradient_r[pi]));
        }

        System.out.println(step);

        network.resetGradients();

        network.calculateGradient_E0(z);

        z.gradient_E0 = -10;

        step = network.calculateStep(1, obj);

        System.out.println(z.E0+"\t"+z.gradient_E0+"\t"+(z.E0-step*z.gradient_E0));
        System.out.println(step);
        */


        
        

    }
    
}
