/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;
import javax.imageio.ImageIO;
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
/*
        for(int t = 60; t < 153; t++)
        {
            createImage(t);
        }
        
        System.exit(0);
*/
        Network network = new Network("MN_model2");

        // 345 max time
        network.printTripsPerDay();

        //network.load(0);
        //network.gradientDescent();
        
        
        
        
        
        network.load(0);
        
        //network.includeTravel = false;
        
        //System.out.println("trips at 200: "+network.getTotalTrips(network.getStartTime()+200));

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
        
        System.out.println("cases: "+network.getTotalCases());
        
        
        
        
        
        
        network.printTotalError(new File(network.getDirectory()+"/output/total_error_0.txt"));
        //network.printZone(network.findZone(27053));
        //network.printZone(network.findZone(27123));

        
        
        //network.printAverageRates();
        
        //PlotErrors test = new PlotErrors(network);
        
        //network.printAverageRates();
        //network.colorZonesTravelI(4);
        //network.colorZonesr(network.getStartTime()+41);
        network.colorZonesLambda();

        //network.colorZonesData(0, 1);
        //network.colorZonesI(500, 100);
        //network.colorZonesRpct(2);
        //network.colorZonesr();

        
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
        map.setTime(network.getStartTime()+92);
        
        
        for(int t = network.getStartTime(); t < network.T; t++)
        {
            map.setTime(t);
            try
            {
                map.saveHighResScreenshot(new File(network.getDirectory()+"/timeline/l_"+t+".png"));
            }
            catch(Exception ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        
        
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
    
    public static void createImage(int t) throws IOException
    {
        BufferedImage image = new BufferedImage(2990, 1682, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        
        BufferedImage cases = ImageIO.read(new File("data/MN_model2/timeline/t_"+t+".png"));
        BufferedImage r = ImageIO.read(new File("data/MN_model2/timeline/r_"+t+".png"));
        BufferedImage lambda = ImageIO.read(new File("data/MN_model2/timeline/l_"+t+".png"));
        
        int x = (2990-2857)/2;
        
        g.setColor(Color.white);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        g.setFont(new Font("TimesRoman", Font.PLAIN, 60));
        
        g.drawImage(cases, x+0, 0, null);
        
        g.drawImage(r, 200+x+1857+50, 80, 662, 728, null);
        
        g.setColor(Color.black);
        
        int offset = (662-g.getFontMetrics().stringWidth("Reproduction rate"))/2;
        g.drawString("Reproduction rate", 200+x+1857+50+offset, 55);
        
        offset = (662-g.getFontMetrics().stringWidth("Infection detection rate"))/2;
        g.drawString("Infection detection rate", 200+x+1857+50+offset, 809+100-5);

        
        g.drawImage(lambda, 200+x+1857+50, 809+120, 662, 728, null);
        
        
        
        g.drawRect(200+x+2411, 457, 2433-2411, 682-457);
        
        g.drawRect(200+x+2411, 1306, 2433-2411, 1531-1306);
        
        ImageIO.write(image, "png", new File("data/MN_model2/timeline2/pic_"+t+".png"));
    }
    
}
