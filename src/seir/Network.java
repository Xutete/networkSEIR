/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;


import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author micha
 */
public class Network 
{
    
    public static final double INFTY = Double.MAX_VALUE;
    
    private double sigma = 12; // incubation time
    private double ell = 7; // recovery time
    private double xi = 0; // reduction in travel among infected individuals
    private int T;
    
    private Zone[] zones;
    private Link[][] matrix;
    
    private TimePeriod[] lambda_periods, r_periods;
    
    
    
    
    
    

    
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
        
        T = count;
        
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
        
        matrix = new Link[zones.length][zones.length];
        
        for(int r = 0; r < matrix.length; r++)
        {
            for(int c = 0; c < matrix[r].length; c++)
            {
                if(r != c)
                {
                    matrix[r][c] = new Link();
                }
            }
        }
        
        
        
        
        
        filein = new Scanner(new File("data/"+dir+"/timeline_r.txt"));
        
        Date start = null;
        
        List<Integer> timeline = new ArrayList<>();
        
        try
        {
            start = new SimpleDateFormat("MM/dd/yyyy").parse(filein.nextLine().trim()); 
            timeline.add(0);
        }
        catch(ParseException ex)
        {
            ex.printStackTrace(System.err);
        }
        
        
        
        while(filein.hasNext())
        {
            try
            {
                Date date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.nextLine().trim()); 
                timeline.add(daysBetween(start, date));
            }
            catch(ParseException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        filein.close();
        
        timeline.add(T+1);

        Collections.sort(timeline);
        
        r_periods = new TimePeriod[timeline.size()-1];
        
        for(int i = 0; i < timeline.size()-1; i++)
        {
            r_periods[i] = new TimePeriod(timeline.get(i), timeline.get(i+1));
        }
        
        
        
        
        filein = new Scanner(new File("data/"+dir+"/timeline_lambda.txt"));
        
        start = null;
        
        timeline = new ArrayList<>();
        
        try
        {
            start = new SimpleDateFormat("MM/dd/yyyy").parse(filein.nextLine().trim()); 
            timeline.add(0);
        }
        catch(ParseException ex)
        {
            ex.printStackTrace(System.err);
        }
        
        
        
        while(filein.hasNext())
        {
            try
            {
                Date date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.nextLine().trim()); 
                timeline.add(daysBetween(start, date));
            }
            catch(ParseException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        filein.close();
        
        timeline.add(T+1);

        Collections.sort(timeline);
        
        lambda_periods = new TimePeriod[timeline.size()-1];
        
        for(int i = 0; i < timeline.size()-1; i++)
        {
            lambda_periods[i] = new TimePeriod(timeline.get(i), timeline.get(i+1));
        }

    }
    
    public int daysBetween(Date d1, Date d2){
             return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
     }
    
    public int index_r(int t)
    {
        for(int i = 0; i < r_periods.length; i++)
        {
            if(r_periods[i].contains(t))
            {
                return i;
            }
        }
        return -1;
    }
    
    public int index_lambda(int t)
    {
        for(int i = 0; i < lambda_periods.length; i++)
        {
            if(lambda_periods[i].contains(t))
            {
                return i;
            }
        }
        return -1;
    }
    
    public void calibrate() 
    {
        for(Zone z : zones)
        {
            z.initialize(T, r_periods, lambda_periods);
        }
        
        for(int r = 0; r < matrix.length; r++)
        {
            for(int c = 0; c < matrix[r].length; c++)
            {
                if(r != c)
                {
                    matrix[r][c].initialize(T);
                }
            }
        }
        
        // initial calculation!
        calculateSEIR();
        
        System.out.println("T = "+T);
        System.out.println("lambda_periods = "+lambda_periods.length);
        System.out.println("r_periods = "+r_periods.length);
        
        
        // calculate dZ/d lambda_i
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                double sum = 0;
                
                for(int t = lambda_periods[pi].getStart(); t < lambda_periods[pi].getEnd() && t < T; t++)
                {
                    sum += 2* (i.I[t] - i.lambda[pi] * i.reportedI[t]) * i.reportedI[t];
                }
                
                i.gradient_lambda[pi] = sum;
            }
        }
        
        // calculate dZ/dr_i(pi)
        for(Zone i : zones)
        {
            for(int pix = 0; pix < r_periods.length; pix++)
            {
                for(Zone j : zones)
                {
                    j.resetDerivs();
                }
                
                TimePeriod pi = r_periods[pix];
                
                
                for(int t = 0; t < T-1; t++)
                {
                    int t_idx = index_r(t);
                    
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        Zone j = zones[jx];
                        
                        j.dI[t+1] = j.dI[t] + 1/sigma * j.dE[t] - 1/ell * j.dI[t];
                        
                        double drdr = 0;
                        
                        if(i == j && pi.contains(t))
                        {
                            drdr = 1;
                        }
                        
                        j.dE[t+1] = j.dE[t] + drdr * j.S[t] * j.I[t]/j.getN() + j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                                + j.r[t_idx] * j.S[t]/j.getN() * j.dI[t] - 1/sigma * j.dE[t];
                        
                        j.dS[t+1] = j.dS[t] - drdr * j.S[t] * j.I[t]/j.getN() - j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                                - j.r[t_idx] * j.S[t]/j.getN() * j.dI[t];
                        
                        for(int kx = 0; kx < matrix.length; kx++)
                        {
                            if(jx != kx)
                            {
                                Zone k = zones[kx];
                                
                                j.dI[t+1] += xi * (matrix[kx][jx].getMu(t)*k.dI[t] - matrix[jx][kx].getMu(t)*j.dI[t]);
                                
                                j.dE[t+1] += matrix[kx][jx].getMu(t)*k.dE[t] - matrix[jx][kx].getMu(t-1)*j.dE[t];
                                
                                j.dS[t+1] += matrix[kx][jx].getMu(t)*k.dS[t] - matrix[jx][kx].getMu(t)*j.dS[t];
                            }
                        }
                        
                        
                        
                    }
                }
                
                
                i.gradient_r[pix] = 0;
                
                for(int t = 0; t < T; t++)
                {
                    int t_idx = index_lambda(t);
                    
                    for(Zone j : zones)
                    {
                        i.gradient_r[pix] += 2*(j.I[t] - j.lambda[t_idx] * j.reportedI[t])* j.dI[t];
                        
                    }
                }
            }
        }
        
        // calculate dZ/dE[0]
        for(Zone i : zones)
        {
            for(Zone j : zones)
            {
                j.resetDerivs();
            }
            
            i.dE[0] = 1;

            for(int t = 0; t < T-1; t++)
            {
                int t_idx = index_r(t);

                for(int jx = 0; jx < matrix.length; jx++)
                {
                    Zone j = zones[jx];

                    j.dI[t+1] = j.dI[t] + 1/sigma * j.dE[t] - 1/ell * j.dI[t];


                    j.dE[t+1] = j.dE[t] + j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                            + j.r[t_idx] * j.S[t]/j.getN() * j.dI[t] - 1/sigma * j.dE[t];

                    j.dS[t+1] = j.dS[t] - j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                            - j.r[t_idx] * j.S[t]/j.getN() * j.dI[t];

                    for(int kx = 0; kx < matrix.length; kx++)
                    {
                        if(jx != kx)
                        {
                            Zone k = zones[kx];

                            j.dI[t+1] += xi * (matrix[kx][jx].getMu(t)*k.dI[t] - matrix[jx][kx].getMu(t)*j.dI[t]);

                            j.dE[t+1] += matrix[kx][jx].getMu(t)*k.dE[t] - matrix[jx][kx].getMu(t-1)*j.dE[t];

                            j.dS[t+1] += matrix[kx][jx].getMu(t)*k.dS[t] - matrix[jx][kx].getMu(t)*j.dS[t];
                        }
                    }



                }
            }
            
            i.gradient_E0 = 0;
            
            for(int t = 0; t < T; t++)
            {
                int pi = index_lambda(t);
                
                for(Zone j : zones)
                {
                    i.gradient_E0 += 2*(j.I[t] - j.lambda[pi]*j.reportedI[t]) * j.dI[t];
                    
                    
                }
            }
        }
        
        System.out.println(gradDotX());
        
        
        
    }
    
    public double gradDotX()
    {
        double output = 0;
        
        for(Zone i : zones)
        {
            for(int pix = 0; pix < lambda_periods.length; pix++)
            {
                output += i.gradient_lambda[pix] * -i.gradient_lambda[pix];
            }
            
            for(int pix = 0; pix < r_periods.length; pix++)
            {
                output += i.gradient_r[pix] * -i.gradient_r[pix];
            }
            
            output += i.gradient_E0 * -i.gradient_E0;
        }
        
        return output;
    }
    
    
    
    public void calculateSEIR()
    {
        for(Zone i : zones)
        {
            i.I[0] = i.lambda[0] * i.reportedI[0];
            i.R[0] = 0;
            i.S[0] = i.getN() - i.I[0] - i.R[0] - i.E[0];
        }
        
        for(int t = 0; t < T-1; t++)
        {
            int pi_r = index_r(t);
            
            for(int ix = 0; ix < zones.length; ix++)
            {
                Zone i = zones[ix];
                
                i.S[t+1] = i.S[t] - i.r[pi_r] * i.S[t] * i.I[t]/i.getN(t);
                
                i.E[t+1] = i.E[t] + i.r[pi_r] * i.S[t] * i.I[t]/i.getN(t) - 1/sigma*i.E[t];
                
                i.I[t+1] = i.I[t] + 1/sigma*i.E[t] - 1/ell*i.I[t];
                
                i.R[t+1] = i.R[t] + 1/ell*i.I[t];
                
                for(int jx = 0; jx < zones.length; jx++)
                {
                    if(ix != jx)
                    {
                        Zone j = zones[jx];

                        i.S[t+1] += matrix[jx][ix].getMu(t) * j.S[t] - matrix[ix][jx].getMu(t) * i.S[t];

                        i.E[t+1] += matrix[jx][ix].getMu(t) * j.E[t] - matrix[ix][jx].getMu(t) * i.E[t];

                        i.I[t+1] += xi*(matrix[jx][ix].getMu(t) * j.I[t] - matrix[ix][jx].getMu(t) * i.I[t]);

                        i.R[t+1] += matrix[jx][ix].getMu(t) * j.R[t] - matrix[ix][jx].getMu(t) * i.R[t];
                    }
                }
            }
        }
    }
}
