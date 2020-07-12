/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;
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

        

        Network network = new Network("MN_model2", 345);

        // 345 max time
        
        /*
        Network network = new Network("MN_model2_12");

        network.gradientDescent();
        
        network = new Network("MN_travel_12");

        network.gradientDescent();
        */
        
        
        
        
        network.load(0);
        
        System.out.println("trips at 151: "+network.getTotalTrips(151));

        double obj = network.calculateSEIR();
        
        System.out.println("T="+network.T);
        

        /*
        PrintStream fileout = new PrintStream(new FileOutputStream(network.getDirectory()+"/output/total_cases.txt"), true);
        for(Zone i : network.getZones())
        {


            fileout.println(i.getId()+"\t"+network.getTotalCases(i));
        }
        fileout.close();
        
        
        */
        
        
        
        network.printTotalError(new File(network.getDirectory()+"/output/total_error_projected.txt"));

        //PlotErrors test = new PlotErrors(network);
        
        //network.printAverageRates();
        //network.colorZonesTravelI(4);
        //network.colorZonesr(network.getStartTime()+41);
        network.colorZonesLambda();

        //network.colorZonesData(10);
        //network.colorZonesI(500, 100);
        //network.colorZonesRpct(2);

        Gradient colors = new Gradient();
        

        colors.put(Math.pow(10, -2), Color.white);
        colors.put(Math.pow(10, -1.6), new Color(255, 239, 163));
        colors.put(Math.pow(10, -1.25), new Color(255, 230, 140));
        colors.put(Math.pow(10, -0.9), new Color(255, 191, 82));
        colors.put(Math.pow(10, -0.5), new Color(255, 108, 41));
        colors.put(Math.pow(10, 0.25), new Color(230, 0, 30));
        colors.put(Math.pow(10, 1), new Color(128, 0, 0));

        
        CountyDisplay test2 = new CountyDisplay(network);
        MapViewer map = test2.getMap();
        
        
        /*
        for(int t = network.getStartTime(); t < network.T; t++)
        {
            map.setTime(t);
            try
            {
                map.saveHighResScreenshot(new File(network.getDirectory()+"/timeline/t_"+t+".png"));
            }
            catch(Exception ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        */
        
        //network.printTripsPerDay();
        
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
