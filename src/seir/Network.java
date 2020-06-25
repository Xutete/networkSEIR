/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author micha
 */
public class Network 
{
    private int T;
    public static final double INFTY = Double.MAX_VALUE;
    
    
    private Zone[] zones;
    private Link[][] matrix;
    
    public Network(String scenario) throws IOException
    {
        readNetwork(scenario);
    }
    
    public void readNetwork(String dir) throws IOException
    {
        
        
        Scanner filein = new Scanner(new File("data/"+dir+"/MN_population.csv"));
        int count = 0;
        while(filein.hasNext())
        {
            count++;
            filein.nextLine();
        }
        filein.close();
        
        zones = new Zone[count];
        
        filein = new Scanner(new File("data/"+dir+"/MN_population.csv"));
        
        
        int idx = 0;
        while(filein.hasNext())
        {
            Scanner chopper = new Scanner(filein.nextLine());
            chopper.useDelimiter(",");
            
            int county = chopper.nextInt();
            double pop = chopper.nextDouble();
            zones[idx++] = new Zone(county, pop);
        }
        filein.close();
        
        count = 0;
        filein = new Scanner(new File("data/"+dir+"/MN_infected.csv"));
        while(filein.hasNext())
        {
            filein.nextLine();
            count++;
        }
        filein.close();
        
        T = count-1;
        
        filein = new Scanner(new File("data/"+dir+"/MN_infected.csv"));
        double[][] reportedI = new double[zones.length][T];
        
        Map<Integer, Integer> cols = new HashMap<>();

        
        Scanner chopper = new Scanner(filein.nextLine());
        chopper.useDelimiter(",");
        
        chopper.next();
        
        idx = 0;
        while(chopper.hasNextInt())
        {
            cols.put(chopper.nextInt(), idx++);
        }
        
        while(filein.hasNext())
        {
            chopper = new Scanner(filein.nextLine());
            chopper.useDelimiter(",");
            
            int t = chopper.nextInt();
            
            for(int i = 0; i < reportedI.length; i++)
            {
                reportedI[i][t-1] = chopper.nextDouble();
            }
        }
        
        filein.close();
        
        for(Zone z : zones)
        {
            z.setReportedI(reportedI[cols.get(z.getId())]);
        }
        
        
    }
}
