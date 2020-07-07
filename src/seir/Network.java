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
import java.util.Random;


/**
 *
 * @author micha
 */
public class Network 
{
    
    public static final boolean optimizeParameters = false;
    public static final double INFTY = Double.MAX_VALUE;
    
    private double inv_sigma = 0.15; // incubation time
    private double inv_ell = 0.08* 0.985 + 0.12* 0.015; // recovery time
    
    private double gradient_inv_sigma, gradient_inv_ell;
    
    private double xi = 0.2; // reduction in travel among infected individuals
    private double xi_E = 0.8; // reduction in travel among exposed individuals
    private double gradient_xi, gradient_xiE;
    private double removed_weight = 0.0;
    
    private int T;
    
    private Zone[] zones;
    private Link[][] matrix;
    
    private TimePeriod[] lambda_periods, r_periods;
    
    
    private String scenario;
    
    private boolean includeTravel;
    private int startTime = 0;
    
    private boolean randomize;
    
    private int run = 0;
    private boolean useLambda = true;
    
    
    private boolean noInitialize = false;

    private int randomSeed;
    private Random rand;
    
    public Network(String scenario) throws IOException
    {
        
        
        
        this.scenario = scenario;
        readNetwork(scenario);
    }

    public Zone[] getZones()
    {
        return zones;
    }
    
    public int getStartTime()
    {
        return startTime;
    }
    
    public int getT()
    {
        return T;
    }
    
    public void calcAvgValues(int start_run, int end_run) throws IOException
    {
        Network[] list = new Network[(end_run - start_run)+1];
        
        int idx = 0;
        
        for(int i = start_run; i <= end_run; i++)
        {
            Network saved = new Network(scenario);
            saved.load(i);
            list[idx++] = saved;
        }
        
        calcAvgValues(list);
        
        
        
        noInitialize = true;
        
        run = end_run+1;
        gradientDescent();
        
    }
    
    
    public void calcAvgValues(Network... networks)
    {
        
        initialize();
        
        double total = 0.0;
        
        for(Network n : networks)
        {
            total += n.inv_ell;
        }
        inv_ell = total/networks.length;
        
        total = 0.0;
        
        for(Network n : networks)
        {
            total += n.inv_sigma;
        }
        inv_sigma = total/networks.length;
        
        total = 0.0;
        
        for(Network n : networks)
        {
            total += n.xi;
        }
        xi = total/networks.length;
        
        
        for(int i = 0; i < zones.length; i++)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                total = 0.0;
        
                for(Network n : networks)
                {
                    total += n.zones[i].lambda[pi];
                }
                zones[i].lambda[pi] = total/networks.length;
            }
            
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                total = 0.0;
        
                for(Network n : networks)
                {
                    total += n.zones[i].r[pi];
                }
                zones[i].r[pi] = total/networks.length;
            }
            
            total = 0.0;
        
            for(Network n : networks)
            {
                total += n.zones[i].E0;
            }
            zones[i].E0 = total/networks.length;
        }
    }
    
    public void save() throws IOException
    {
        File file = new File("data/"+scenario+"/output/variables_"+run+".txt");
        
        PrintStream fileout = new PrintStream(new FileOutputStream(file), true);
        
        fileout.println(inv_sigma+"\t"+inv_ell+"\t"+xi+"\t"+xi_E+"\t"+includeTravel+"\t"+startTime+"\t"+removed_weight);
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                fileout.print(i.lambda[pi]+"\t");
            }
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                fileout.print(i.r[pi]+"\t");
            }
            fileout.print(i.E0+"\n");
        }
        
        fileout.close();
    }
    
    public void load(int run) throws IOException
    {
        load(""+run);
    }
    
    public void load(String run) throws IOException
    {
        load(new File("data/"+scenario+"/output/variables_"+run+".txt"));
    }
    
    public void load(File file) throws IOException
    {
        initialize();
        
        Scanner filein = new Scanner(file);
        
        inv_sigma = filein.nextDouble();
        inv_ell = filein.nextDouble();
        xi = filein.nextDouble();
        xi_E = filein.nextDouble();
        includeTravel = filein.next().equalsIgnoreCase("true");
        startTime = filein.nextInt();
        removed_weight = filein.nextDouble();
        
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                i.lambda[pi] = filein.nextDouble();
            }
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                i.r[pi] = filein.nextDouble();
            }
            i.E0 = filein.nextDouble();
        }
        filein.close();
        
        calculateSEIR();
    }
    
    public String getDirectory()
    {
        return "data/"+scenario;
    }
    
    public void readNetwork(String dir) throws IOException
    {
        
        int numZones = Integer.MAX_VALUE;
        
        Scanner filein = new Scanner(new File("data/"+scenario+"/parameters.txt"));
        while(filein.hasNext())
        {
            String key = filein.next();
            String value = filein.next();
            
            if(key.equalsIgnoreCase("travel"))
            {
                includeTravel = value.equalsIgnoreCase("true");
            }
            else if(key.equalsIgnoreCase("startTime"))
            {
                startTime = Integer.parseInt(value.trim());
            }
            else if(key.equalsIgnoreCase("numIter"))
            {
                num_iter = Integer.parseInt(value.trim());
            }
            else if(key.equalsIgnoreCase("minImprovement"))
            {
                min_improvement = Double.parseDouble(value.trim());
            }
            else if(key.equalsIgnoreCase("maxZones"))
            {
                numZones = Integer.parseInt(value.trim());
            }
        }
        filein.close();
        
        
        
        filein = new Scanner(new File("data/"+dir+"/MN_population.csv"));
        int count = 0;
        while(filein.hasNext())
        {
            count++;
            filein.nextLine();
        }
        filein.close();
        
        count = Math.min(numZones, count);
        
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
            
            if(idx >= zones.length)
            {
                break;
            }
        }
        filein.close();
        
        count = 0;
        filein = new Scanner(new File("data/"+dir+"/MN_infected.csv"));
        filein.nextLine();
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
        
        
        reportedI = null;
        
        
        filein = new Scanner(new File("data/"+dir+"/MN_recovered.csv"));
        double[][] reportedR = new double[zones.length][T];
        
        cols = new HashMap<>();

        
        chopper = new Scanner(filein.nextLine());
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
            
            for(int i = 0; i < reportedR.length; i++)
            {
                reportedR[i][t-1] = chopper.nextDouble();
            }
        }
        
        filein.close();
        
        for(Zone z : zones)
        {
            z.setReportedR(reportedR[cols.get(z.getId())]);
        }
        
        
        filein = new Scanner(new File("data/"+dir+"/MN_deaths.csv"));
        reportedR = new double[zones.length][T];
        
        cols = new HashMap<>();

        
        chopper = new Scanner(filein.nextLine());
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
            
            for(int i = 0; i < reportedR.length; i++)
            {
                reportedR[i][t-1] = chopper.nextDouble();
            }
        }
        
        filein.close();
        
        for(Zone z : zones)
        {
            z.addReportedR(reportedR[cols.get(z.getId())]);
        }
        
        
        
        matrix = new Link[zones.length][zones.length];
        
        for(int r = 0; r < matrix.length; r++)
        {
            for(int c = 0; c < matrix[r].length; c++)
            {
                if(r != c)
                {
                    matrix[r][c] = new Link(T);
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

        
        
        
        
        count = 0;
       
        filein = new Scanner(new File("data/"+scenario+"/travel_change.csv"));
        Map<Integer, Double> changes = new HashMap<Integer, Double>();
        
        filein.nextLine();
        
        int lastDay = 0;
        int earliest_change_date = Integer.MAX_VALUE;
        while(filein.hasNext())
        {
            chopper = new Scanner(filein.nextLine());
            chopper.useDelimiter(",");
            
            try
            {
                Date day = new SimpleDateFormat("MM/dd/yyyy").parse(chopper.next());
                chopper.next();
                chopper.next();
                chopper.next();
                chopper.next();
                
                double change = chopper.nextDouble();
                int daysBetween = daysBetween(start, day);
                changes.put(daysBetween, change);
                earliest_change_date = (int)Math.min(daysBetween, earliest_change_date);
                lastDay = (int)Math.max(daysBetween, lastDay);
            }
            catch(ParseException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        filein.close();
        
        for(int t = 0; t < earliest_change_date; t++)
        {
            changes.put(t, 0.0);
        }
        
        
        cols = null;
        
        Map<Integer, Integer> zoneLookup = new HashMap<>();
        
        for(int i = 0; i < zones.length; i++)
        {
            zoneLookup.put(zones[i].getId(), i);
        }
        
        
        filein = new Scanner(new File("data/"+scenario+"/travel_data.txt"));
        while(filein.hasNext())
        {
            Date start_period_date, end_period_date, start_apply_date, end_apply_date;
            
            try
            {
                start_period_date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.next());
                end_period_date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.next());
                start_apply_date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.next());
                end_apply_date = new SimpleDateFormat("MM/dd/yyyy").parse(filein.next());
                String datafile = filein.nextLine().trim();
                
                int start_period = daysBetween(start, start_period_date);
                int end_period = daysBetween(start, end_period_date);
                int start_apply = daysBetween(start, start_apply_date);
                int end_apply = daysBetween(start, end_apply_date);
                
                
                // scale up demand to normal then scale down by % change
                double total_predicted = 0;
                double total_actual = 0;
                count = 0;
                
                for(int i = start_period; i <= end_period; i++)
                {
                    if(changes.containsKey(i))
                    {
                        total_predicted += 1;
                        total_actual += (100+changes.get(i))/100;
                        count++;
                    }
                }
                
                
                double scaleup = 1.0 / (total_actual / total_predicted);
                
                Scanner filein2 = new Scanner(new File("data/"+scenario+"/"+datafile));
                
                while(filein2.hasNext())
                {
                    chopper = new Scanner(filein2.nextLine());
                    chopper.useDelimiter(",");
                    
                    int source = chopper.nextInt();
                    int dest = chopper.nextInt();
                    double demand = chopper.nextDouble();
                    
                    
                    if(!zoneLookup.containsKey(source) || !zoneLookup.containsKey(dest))
                    {
                        continue;
                    }
                    int r = zoneLookup.get(source);
                    int c = zoneLookup.get(dest);
                    
                    if(r == c)
                    {
                        continue;
                    }
                
                    for(int t = (int)Math.max(0, start_apply); t <= end_apply && t < T; t++)
                    {
                        double change;
                        if(!changes.containsKey(t))
                        {
                            change = changes.get(lastDay);
                        }
                        else
                        {
                            change = changes.get(t);
                        }
                        
                        double scaledown = (100+change)/100;

                        
                        matrix[r][c].setNormalDemand(zones[r], zones[c], t, demand*scaleup * scaledown);
                    }
                }
                filein2.close();
                
            }
            catch(ParseException ex)
            {
                ex.printStackTrace(System.err);
            }
            
            
        }
        filein.close();
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
    
    public void printSolution()
    {
        System.out.println("Zone\ttime\tpredicted\treported\terror\tr\tlambda");
        for(Zone i : zones)
        {
            for(int t = startTime; t < T; t++)
            {
                System.out.println(i.getId()+"\t"+t+"\t"+String.format("%.2f", i.I[t])+"\t"+
                    String.format("%.2f", i.lambda[index_lambda(t)] * i.reportedI[t])+"\t"+
                    String.format("%.2f", Math.abs(i.I[t]-i.lambda[index_lambda(t)] * i.reportedI[t]))+"\t"+
                    String.format("%.2f", i.lambda[index_lambda(t)])+"\t"+
                    String.format("%.2f", i.r[index_r(t)]));
            }
        }
    }
    
    public void printTotalError()
    {
        try
        {
            printTotalError(System.out);
        }
        catch(IOException ex){}
    }
    

    
    public void printTotalError(PrintStream out) throws IOException
    {
        out.print("time\ttotal error\t% error\tcount\tpredicted");
        
        for(Zone i : zones)
        {
            out.print("\t"+i+" reported\t"+i+" predicted\t"+i+"% error");
        }
        out.println();
        
        for(int t = startTime; t < T; t++)
        {
            double error = 0;
            double count = 0;
            double predicted = 0;
            
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                error += Math.abs( i.I[t] - i.lambda[index_lambda(t)] * i.reportedI[t]);
                predicted += i.I[t];
                count += i.reportedI[t];
            }

            out.print(t+"\t"+error+"\t"+String.format("%.2f\t%.2f\t%.2f", 100.0*error/count, count, predicted));
            
            for(Zone i : zones)
            {
                double predictedi = i.I[t];
                double reportedi = i.lambda[index_lambda(t)]*i.reportedI[t];
                double errori = Math.abs(i.I[t] - i.lambda[index_lambda(t)]*i.reportedI[t])/i.reportedI[t];
                out.printf("\t%.2f\t%.2f\t%.2f", reportedi, predictedi, errori*100.0);
            }
            out.println();
        }
    }
    
    int num_iter = 100;
    double alpha = 0.4;
    double beta = 0.5;
    int max_step_iter = 50;
    double min_improvement = 0.01;
    
    public int randomStart(int iter) throws IOException
    {
        return randomStart(iter, 0);
    }
    
    public int randomStart(int iter, int start_run) throws IOException
    {
        int best = -1;
        double min = Integer.MAX_VALUE;
        
        randomize = true;
        for(run = start_run; run < iter+start_run; run++)
        {
            double obj = gradientDescent();
            
            if(obj < min)
            {
                min = obj;
                best = run;
            }
        }
        
        System.out.println("Best: run "+best+" with obj "+min);
        
        return best;
    }
    
    public void initialSolution()
    {
        if(noInitialize)
        {
            return;
        }
        
        // initial solution
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                
                
                if(randomize)
                {
                    i.lambda[pi] = 1 + rand.nextDouble()*2;
                }
                else
                {
                    i.lambda[pi] = 2;
                }
            }
            
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                
                
                if(randomize)
                {
                    i.r[pi] = rand.nextDouble()*4;
                }
                else
                {
                    i.r[pi] = 0;
                }
            }
            
            if(randomize)
            {
                i.E0 = i.getN()/100*rand.nextDouble();
            }
            else
            {
                i.E0 = i.reportedI[startTime]*i.lambda[index_lambda(startTime)] * 1/inv_sigma;
            }
        }
        
        if(randomize)
        {
            xi = Math.random();
            xi_E = Math.random();
        }
        else
        {
            xi = 0.2;
            xi_E = 1;
        }
    }
    
    
    public void initialize()
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
        
    }
    
    public double gradientDescent() throws IOException 
    {
        

        long total_time = System.nanoTime();
        
        randomSeed = (int)Math.round(100000*Math.random());
        rand = new Random(randomSeed);
        
        
        initialize();
        
        
        initialSolution();
        
        
        
        
        
        PrintStream fileout =  new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/log_"+run+".txt")), true);
        
        
        System.out.println(scenario + " run "+run);
        fileout.println(scenario + " run "+run);
        System.out.println("T = "+T);
        fileout.println("T = "+T);
        System.out.println("lambda_periods = "+lambda_periods.length);
        fileout.println("lambda_periods = "+lambda_periods.length);
        System.out.println("r_periods = "+r_periods.length);
        fileout.println("r_periods = "+r_periods.length);
        System.out.println("include travel: "+includeTravel);
        fileout.println("include travel: "+includeTravel);
        System.out.println("randomize "+randomize);
        fileout.println("randomize "+randomize);
        if(randomize)
        {
            System.out.println("random seed: "+randomSeed);
        }
        
        
        System.out.println("Iteration\tObjective\tObj. change\tError\tCPU time");
        fileout.println("Iteration\tObjective\tObj. change\tI Error\tCPU time (s)\t R error");
        
        double obj = calculateSEIR();
        double improvement = 100;
        
        System.out.println("0\t"+obj);
        
        double prev_obj = obj;
        
        double updated_obj = obj;
        
        for(int iter = 1; iter <= num_iter && improvement > min_improvement; iter++)
        {   
            long time = System.nanoTime();

            double step = 0;
            for(Zone i : zones)
            {
                //System.out.println(i);
                
                if(useLambda)
                {
                    resetGradients();
                    calculateGradient_lambda(i);
                    

                    step = calculateStep(iter, obj);
                    updateVariables(step);
                    updated_obj = obj;
                    obj = calculateSEIR();
                    
                    if((updated_obj-obj)/updated_obj*100.0 < -0.009)
                    {
                        System.out.println("\t"+i+"-lambda\t"+obj+"\t"+updated_obj+"\t"+step+"\t"+String.format("%.2f", (updated_obj-obj)/updated_obj*100.0)+"\t"+calculateSEIRsearch(0, 0));
                    }
                }
                
            }
            
            for(Zone i : zones)
            {
                
                
                resetGradients();
                calculateGradient_r(i);
                
                step = calculateStep(iter, obj);
                updateVariables(step);
                updated_obj = obj;
                obj = calculateSEIR();
                
                if((updated_obj-obj)/updated_obj*100.0 < -0.009)
                {
                    System.out.println("\t"+i+"-r\t"+obj+"\t"+updated_obj+"\t"+step+"\t"+String.format("%.2f", (updated_obj-obj)/updated_obj*100.0)+"\t"+calculateSEIRsearch(0, 0));
                }
            }
            
            for(Zone i : zones)
            {
                
                resetGradients();
                calculateGradient_E0(i);

                step = calculateStep(iter, obj);
                updateVariables(step);
                updated_obj = obj;
                obj = calculateSEIR();
                
                if((updated_obj-obj)/updated_obj*100.0 < -0.009)
                {
                    System.out.println("\t"+i+"-E0\t"+obj+"\t"+updated_obj+"\t"+step+"\t"+String.format("%.2f", (updated_obj-obj)/updated_obj*100.0)+"\t"+calculateSEIRsearch(0, 0));
                }
                
                //System.out.println("\t"+obj+"\t"+step);
            }
            
            if(optimizeParameters)
            {
                System.out.print("Before: "+obj);

                resetGradients();

                calculateGradient_ell();
                calculateGradient_sigma();
                calculateGradient_xi();

                step = calculateStep(iter, obj);
                updateVariables(step);
                obj = calculateSEIR();

                System.out.print("\tAfter: "+obj+"\n");
            }
            
            else
            {
                resetGradients();

                //System.out.print(xi+" "+xi_E+"\t");
                calculateGradient_xi();
                //calculateGradient_xiE();

                step = calculateStep(iter, obj);
                updateVariables(step);
                obj = calculateSEIR();
                
                //System.out.println("xi step: "+(step * gradient_xi)+" "+(step*gradient_xiE)+"\t"+xi+" "+xi_E);
            }
            
            save();
            
            time = System.nanoTime() - time;
            // System.out.println("Step: "+step);
            //System.out.println("Max step: "+calculateMaxStep());
            //System.out.println("Obj: "+obj);
            improvement = 100.0*(prev_obj - obj) / prev_obj;
            
            double error = calculateInfectedError();
            double error2 = calculateRemovedError();
            
            System.out.println(iter+"\t"+obj+"\t"+String.format("%.2f", improvement)
                    +"%\t"+String.format("%.2f", error)+"%\t"+String.format("%.1f", time/1.0e9)+"s"+"\t"+String.format("%.2f", error2)+"%");
            fileout.println(iter+"\t"+obj+"\t"+String.format("%.2f", improvement)
                    +"%\t"+String.format("%.2f", error)+"%\t"+String.format("%.1f", time/1.0e9)+"s"+String.format("%.2f", error2)+"%");
            prev_obj = obj;
            
            
        }
        
        total_time = System.nanoTime() - total_time;
        
        System.out.println("CPU time: "+String.format("%.2f", total_time/1.0e9/60)+"min");
        fileout.println("CPU time: "+String.format("%.2f", total_time/1.0e9/60)+"min");
        fileout.close();
        
        fileout = new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/total_error_"+run+".txt")));
        printTotalError(fileout);
        fileout.close();
        
        System.out.println("xi: "+xi);
        System.out.println("xiE: "+xi_E);
        
        return obj;
    }
    
    public double calculateInfectedError()
    {
        double error = 0.0;
        double total = 0.0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                total += i.reportedI[t];
                error += Math.abs(i.I[t] - i.lambda[pi] * i.reportedI[t]);
            }
        }
        
        return 100.0*error/total;
    }
    
    
    public double calculateRemovedError()
    {
        double error = 0.0;
        double total = 0.0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                total += i.reportedR[t];
                error += Math.abs(i.R[t] - i.lambda[pi] * i.reportedR[t]);
            }
        }
        
        return 100.0*error/total;
    }
    
    public double calculateStep(int iter, double obj)
    {
        double step = 1;
        //step = calculateMaxStep();

        double change = gradDotX();
        
        int inner_iter = 0;
        
        double output;

        while( (output = calculateSEIRsearch(step, iter)) > obj + alpha * step * change)
        {
            step = step*beta;
            
            inner_iter ++;
            
            if(inner_iter > max_step_iter)
            {
                step = 0;
                break;
            }
            //System.out.println("\t\t"+output+" > "+(obj + alpha * step * change)+"\t"+step+" "+inner_iter);
        }
        
        output = calculateSEIRsearch(step, iter);

        /*
        if(iter == 24)
        {
            System.out.println("\t"+output+" > "+(obj + alpha * step * change)+"\t"+step);
        }
        */
        if((""+output).equals("NaN"))
        {
            printdebug = true;
            System.out.println("\t"+output+" > "+(obj + alpha * step * change)+"\t"+step);
            calculateSEIRsearch(step, iter);
        }
            
        return step;
    }
    
    public void resetGradients()
    {
        gradient_inv_sigma = 0;
        gradient_inv_ell = 0;
        gradient_xi = 0;
        gradient_xiE = 0;
        
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                i.gradient_lambda[pi] = 0;
            }
            
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                i.gradient_r[pi] = 0;
            }
            
            i.gradient_E0 = 0;
        }
    }
    
    public void updateVariables(double step)
    {
        xi = Math.min(1, Math.max(0, xi - step*gradient_xi));
        xi_E = Math.min(1, Math.max(0, xi_E - step*gradient_xiE));
        
        
        inv_sigma = inv_sigma - step*gradient_inv_sigma;
        inv_ell = inv_ell - step*gradient_inv_ell;
        
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                i.lambda[pi] = Math.min(10, Math.max(1, i.lambda[pi] - step * i.gradient_lambda[pi]));
            }
            
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                i.r[pi] = Math.min(20, Math.max(0, i.r[pi] - step * i.gradient_r[pi]));
            }
            
            i.E0 = Math.min(i.getN()/100, Math.max(0, i.E0 - step * i.gradient_E0));
            
            i.I[startTime] = i.lambda[index_lambda(startTime)] * i.reportedI[startTime];
            i.E[startTime] = i.E0;
            
        }
    }
    
    // calculate the maximum step size based on feasibility (r>=0, lambda>=1, E0>=0, E0<=N-I0)
    public double calculateMaxStep()
    {
        double output = 1;
        
        for(Zone i : zones)
        {
            for(int pi = 0; pi < lambda_periods.length; pi++)
            {
                // newlambda = lambda - step*gradient
                double temp = (i.lambda[pi] - 1)/i.gradient_lambda[pi];
                if(temp > 0)
                {
                    output = Math.min(output, temp);
                }
            }
            
            for(int pi = 0; pi < r_periods.length; pi++)
            {
                // newr = r - step*gradient
                double temp = (i.r[pi] - 0)/i.gradient_r[pi];
                if(temp > 0)
                {
                    output = Math.min(output, temp);
                }
            }
            
            // new E0 = E0 - step*gradient
            double temp = (i.E0 - 0)/i.gradient_E0;
            if(temp > 0)
            {
                output = Math.min(output, temp);
            }
        }
        
        return output;
    }
    
    public void calculateGradient_lambda()
    {
        
        // calculate dZ/d lambda_i
        for(Zone i : zones)
        {
            calculateGradient_lambda(i);
        }
    }
    
    public void calculateGradient_sigma()
    {
        for(Zone i : zones)
        {
            i.resetDerivs();
        }
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_r(t);
            for(Zone i : zones)
            {
                double dN = i.dS[t] + i.dE[t] + i.dI[t] + i.dR[t];
                double N = i.getN(t);
                
                i.dI[t+1] = i.dI[t] + inv_sigma * i.dE[t] + inv_sigma * i.E[t] - inv_ell * i.dI[t];
                
                i.dE[t+1] = i.dE[t] + i.r[pi]*i.dS[t]*i.I[t]/N + i.r[pi]*i.S[t]/N*i.dI[t] 
                        - i.r[pi]*i.S[t]*i.I[t]/N/N*dN - inv_sigma*i.dE[t] - inv_sigma*i.E[t];
                
                i.dS[t+1] = i.dS[t] - i.r[pi]*i.dS[t]*i.I[t]/N - i.r[pi]*i.S[t]/N*i.dI[t] +
                        i.r[pi]*i.S[t]*i.I[t]/N/N*dN;
                
                i.dR[t+1] = i.dR[t] + inv_ell*i.dI[t];
                
                if(includeTravel)
                {
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        if(jx != i.getIdx())
                        {
                            Zone j = zones[jx];

                            i.dI[t+1] += xi*(matrix[jx][i.getIdx()].getMu(t)*j.dI[t] - matrix[i.getIdx()][jx].getMu(t)*i.dI[t]);

                            i.dE[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dE[t] - matrix[i.getIdx()][jx].getMu(t)*i.dE[t];

                            i.dS[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dS[t] - matrix[i.getIdx()][jx].getMu(t)*i.dS[t];

                            i.dR[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dR[t] - matrix[i.getIdx()][jx].getMu(t)*i.dR[t];
                        }
                    }
                }
            }
        }
        
        gradient_inv_sigma = 0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                gradient_inv_sigma += 2* (i.I[t] - i.lambda[pi]*i.reportedI[t])*i.dI[t];
            }
        }
    }
    
    public void calculateGradient_ell()
    {
        for(Zone i : zones)
        {
            i.resetDerivs();
        }
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_r(t);
            for(Zone i : zones)
            {
                double dN = i.dS[t] + i.dE[t] + i.dI[t] + i.dR[t];
                double N = i.getN(t);
                
                i.dI[t+1] = i.dI[t] + inv_sigma * i.dE[t]  - inv_ell * i.dI[t] - inv_ell*i.I[t];
                
                i.dE[t+1] = i.dE[t] + i.r[pi]*i.dS[t]*i.I[t]/N + i.r[pi]*i.S[t]/N*i.dI[t] 
                        - i.r[pi]*i.S[t]*i.I[t]/N/N*dN - inv_sigma*i.dE[t];
                
                i.dS[t+1] = i.dS[t] - i.r[pi]*i.dS[t]*i.I[t]/N - i.r[pi]*i.S[t]/N*i.dI[t] +
                        i.r[pi]*i.S[t]*i.I[t]/N/N*dN;
                
                i.dR[t+1] = i.dR[t] + inv_ell*i.dI[t] + inv_ell*i.I[t];
                
                if(includeTravel)
                {
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        if(jx != i.getIdx())
                        {
                            Zone j = zones[jx];

                            i.dI[t+1] += xi*(matrix[jx][i.getIdx()].getMu(t)*j.dI[t] - matrix[i.getIdx()][jx].getMu(t)*i.dI[t]);

                            i.dE[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dE[t] - matrix[i.getIdx()][jx].getMu(t)*i.dE[t];

                            i.dS[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dS[t] - matrix[i.getIdx()][jx].getMu(t)*i.dS[t];

                            i.dR[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dR[t] - matrix[i.getIdx()][jx].getMu(t)*i.dR[t];
                        }
                    }
                }
            }
        }
        
        gradient_inv_ell = 0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                gradient_inv_ell += 2* (i.I[t] - i.lambda[pi]*i.reportedI[t])*i.dI[t];
            }
        }
    }
    
    
    public void calculateGradient_xi()
    {
        for(Zone i : zones)
        {
            i.resetDerivs();
        }
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_r(t);
            for(Zone i : zones)
            {
                double dN = i.dS[t] + i.dE[t] + i.dI[t] + i.dR[t];
                double N = i.getN(t);
                
                i.dI[t+1] = i.dI[t] + inv_sigma * i.dE[t]  - inv_ell * i.dI[t];
                
                i.dE[t+1] = i.dE[t] + i.r[pi]*i.dS[t]*i.I[t]/N + i.r[pi]*i.S[t]/N*i.dI[t] 
                        - i.r[pi]*i.S[t]*i.I[t]/N/N*dN - inv_sigma*i.dE[t];
                
                i.dS[t+1] = i.dS[t] - i.r[pi]*i.dS[t]*i.I[t]/N - i.r[pi]*i.S[t]/N*i.dI[t] +
                        i.r[pi]*i.S[t]*i.I[t]/N/N*dN;
                
                i.dR[t+1] = i.dR[t] + inv_ell*i.dI[t];
                
                if(includeTravel)
                {
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        if(jx != i.getIdx())
                        {
                            Zone j = zones[jx];

                            i.dI[t+1] += xi*(matrix[jx][i.getIdx()].getMu(t)*j.dI[t] - matrix[i.getIdx()][jx].getMu(t)*i.dI[t]);
                            i.dI[t+1] += (matrix[jx][i.getIdx()].getMu(t)*j.I[t] - matrix[i.getIdx()][jx].getMu(t)*i.I[t]);

                            i.dE[t+1] += xi_E * (matrix[jx][i.getIdx()].getMu(t)*j.dE[t] - matrix[i.getIdx()][jx].getMu(t)*i.dE[t]);

                            i.dS[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dS[t] - matrix[i.getIdx()][jx].getMu(t)*i.dS[t];

                            i.dR[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dR[t] - matrix[i.getIdx()][jx].getMu(t)*i.dR[t];
                        }
                    }
                }
            }
        }
        
        gradient_xi = 0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                gradient_xi += 2* (i.I[t] - i.lambda[pi]*i.reportedI[t])*i.dI[t];
                gradient_xi += removed_weight * (2* (i.R[t] - i.lambda[pi]*i.reportedR[t])*i.dR[t]);
            }
        }
    }
    
    public void calculateGradient_xiE()
    {
        for(Zone i : zones)
        {
            i.resetDerivs();
        }
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_r(t);
            for(Zone i : zones)
            {
                double dN = i.dS[t] + i.dE[t] + i.dI[t] + i.dR[t];
                double N = i.getN(t);
                
                i.dI[t+1] = i.dI[t] + inv_sigma * i.dE[t]  - inv_ell * i.dI[t];
                
                i.dE[t+1] = i.dE[t] + i.r[pi]*i.dS[t]*i.I[t]/N + i.r[pi]*i.S[t]/N*i.dI[t] 
                        - i.r[pi]*i.S[t]*i.I[t]/N/N*dN - inv_sigma*i.dE[t];
                
                i.dS[t+1] = i.dS[t] - i.r[pi]*i.dS[t]*i.I[t]/N - i.r[pi]*i.S[t]/N*i.dI[t] +
                        i.r[pi]*i.S[t]*i.I[t]/N/N*dN;
                
                i.dR[t+1] = i.dR[t] + inv_ell*i.dI[t];
                
                if(includeTravel)
                {
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        if(jx != i.getIdx())
                        {
                            Zone j = zones[jx];

                            i.dI[t+1] += xi*(matrix[jx][i.getIdx()].getMu(t)*j.dI[t] - matrix[i.getIdx()][jx].getMu(t)*i.dI[t]);

                            i.dE[t+1] += xi_E * (matrix[jx][i.getIdx()].getMu(t)*j.dE[t] - matrix[i.getIdx()][jx].getMu(t)*i.dE[t]);
                            i.dE[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.E[t] - matrix[i.getIdx()][jx].getMu(t)*i.E[t];

                            i.dS[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dS[t] - matrix[i.getIdx()][jx].getMu(t)*i.dS[t];

                            i.dR[t+1] += matrix[jx][i.getIdx()].getMu(t)*j.dR[t] - matrix[i.getIdx()][jx].getMu(t)*i.dR[t];
                        }
                    }
                }
            }
        }
        
        gradient_xiE = 0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                gradient_xiE += 2* (i.I[t] - i.lambda[pi]*i.reportedI[t])*i.dI[t];
                gradient_xiE += removed_weight * (2* (i.R[t] - i.lambda[pi]*i.reportedR[t])*i.dR[t]);
            }
        }
    }
    
    public void calculateGradient_lambda(Zone i)
    {
        for(int pi = 0; pi < lambda_periods.length; pi++)
        {
            double sum = 0;

            for(int t = Math.max(startTime, lambda_periods[pi].getStart()); t < lambda_periods[pi].getEnd() && t < T; t++)
            {
                sum -= 2* (i.I[t] - i.lambda[pi] * i.reportedI[t]) * i.reportedI[t];
                sum -= removed_weight * (2* (i.R[t] - i.lambda[pi] * i.reportedR[t]) * i.reportedR[t]);
            }

            i.gradient_lambda[pi] = sum;
        }
    }
    
    public void calculateGradient_r()
    {
        
        // calculate dZ/dr_i(pi)
        for(Zone i : zones)
        {
            calculateGradient_r(i);
        }
    }
    
    public void calculateGradient_r(Zone i)
    {
        for(int pix = 0; pix < r_periods.length; pix++)
        {
            for(Zone j : zones)
            {
                j.resetDerivs();
            }

            TimePeriod pi = r_periods[pix];


            for(int t = startTime; t < T-1; t++)
            {
                int t_idx = index_r(t);

                for(int jx = 0; jx < matrix.length; jx++)
                {
                    Zone j = zones[jx];

                    j.dI[t+1] = j.dI[t] + inv_sigma * j.dE[t] - inv_ell * j.dI[t];

                    double drdr = 0;

                    if(i == j && pi.contains(t))
                    {
                        drdr = 1;
                    }

                    double N = j.getN(t);
                    
                    if(N==0)
                    {
                        System.out.println("N is 0");
                    }
                    double dN = j.dS[t] + j.dE[t] + j.dI[t] + j.dR[t];
                    j.dE[t+1] = j.dE[t] + drdr * j.S[t] * j.I[t]/j.getN(t) + j.r[t_idx]*j.dS[t]*j.I[t]/N
                            + j.r[t_idx] * j.S[t]/N * j.dI[t] - j.r[t_idx]*j.S[t]*j.I[t]/N/N*dN
                            - inv_sigma * j.dE[t];

                    j.dS[t+1] = j.dS[t] - drdr * j.S[t] * j.I[t]/N - j.r[t_idx]*j.dS[t]*j.I[t]/N
                            + j.r[t_idx]*j.S[t]*j.I[t]/N/N*dN - j.r[t_idx] * j.S[t]/N * j.dI[t];

                    j.dR[t+1] = j.dR[t] + inv_ell*j.dI[t];

                    if(includeTravel)
                    {
                        for(int kx = 0; kx < matrix.length; kx++)
                        {
                            if(jx != kx)
                            {
                                Zone k = zones[kx];

                                j.dI[t+1] += xi * (matrix[kx][jx].getMu(t)*k.dI[t] - matrix[jx][kx].getMu(t)*j.dI[t]);

                                j.dE[t+1] += xi_E * (matrix[kx][jx].getMu(t)*k.dE[t] - matrix[jx][kx].getMu(t)*j.dE[t]);

                                j.dS[t+1] += matrix[kx][jx].getMu(t)*k.dS[t] - matrix[jx][kx].getMu(t)*j.dS[t];

                                j.dR[t+1] += matrix[kx][jx].getMu(t)*k.dR[t] - matrix[jx][kx].getMu(t)*j.dR[t];
                            }
                        }
                    }


                }
            }


            i.gradient_r[pix] = 0;

            for(int t = startTime; t < T; t++)
            {
                int t_idx = index_lambda(t);

                for(Zone j : zones)
                {
                    i.gradient_r[pix] += 2*(j.I[t] - j.lambda[t_idx] * j.reportedI[t])* j.dI[t];
                    i.gradient_r[pix] += removed_weight * (2*(j.R[t] - j.lambda[t_idx] * j.reportedR[t])* j.dR[t]);
                }
            }

            //System.out.println(i.gradient_r[pix]);
        }
    }
    
    public void calculateGradient_E0()
    {
        
        // calculate dZ/dE[0]
        for(Zone i : zones)
        {
            calculateGradient_E0(i);
        }
        
    }
    
    public void calculateGradient_E0(Zone i)
    {
        for(Zone j : zones)
        {
            j.resetDerivs();
        }

        i.dE[startTime] = 1;

        for(int t = startTime; t < T-1; t++)
        {
            int t_idx = index_r(t);

            for(int jx = 0; jx < matrix.length; jx++)
            {
                Zone j = zones[jx];

                j.dI[t+1] = j.dI[t] + inv_sigma * j.dE[t] - inv_ell * j.dI[t];


                j.dE[t+1] = j.dE[t] + j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                        + j.r[t_idx] * j.S[t]/j.getN() * j.dI[t] - inv_sigma * j.dE[t];

                j.dS[t+1] = j.dS[t] - j.r[t_idx]*j.dS[t]*j.I[t]/j.getN()
                        - j.r[t_idx] * j.S[t]/j.getN() * j.dI[t];

                if(includeTravel)
                {
                    for(int kx = 0; kx < matrix.length; kx++)
                    {
                        if(jx != kx)
                        {
                            Zone k = zones[kx];

                            j.dI[t+1] += xi * (matrix[kx][jx].getMu(t)*k.dI[t] - matrix[jx][kx].getMu(t)*j.dI[t]);

                            j.dE[t+1] += xi_E * (matrix[kx][jx].getMu(t)*k.dE[t] - matrix[jx][kx].getMu(t)*j.dE[t]);

                            j.dS[t+1] += matrix[kx][jx].getMu(t)*k.dS[t] - matrix[jx][kx].getMu(t)*j.dS[t];
                            
                            j.dR[t+1] += matrix[kx][jx].getMu(t)*k.dR[t] - matrix[jx][kx].getMu(t)*j.dR[t];
                        }
                    }
                }


            }
        }

        i.gradient_E0 = 0;

        for(int t = startTime; t < T; t++)
        {
            int pi = index_lambda(t);

            for(Zone j : zones)
            {
                i.gradient_E0 += 2*(j.I[t] - j.lambda[pi]*j.reportedI[t]) * j.dI[t];
                i.gradient_E0 += removed_weight * (2*(j.R[t] - j.lambda[pi]*j.reportedR[t]) * j.dR[t]);

            }
        }

        //System.out.println(i.gradient_E0);
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
    
    
    
    public double calculateSEIR()
    {

        return calculateSEIRsearch(0, 0);

        /*
        for(Zone i : zones)
        {
            i.I[startTime] = i.lambda[index_lambda(startTime)] * i.reportedI[startTime];
            i.E[startTime] = i.E0;
            i.R[startTime] = 0;
            i.S[startTime] = i.getN() - i.I[startTime] - i.R[startTime] - i.E[startTime];
        }
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi_r = index_r(t);
            
            for(int ix = 0; ix < zones.length; ix++)
            {
                Zone i = zones[ix];
                
                double fSE = Math.min(i.S[t], i.r[pi_r] * i.S[t] * i.I[t]/i.getN(t));
                
                i.S[t+1] = i.S[t] - fSE;
                
                i.E[t+1] = i.E[t] + fSE - inv_sigma*i.E[t];
                
                i.I[t+1] = i.I[t] + inv_sigma*i.E[t] - inv_ell*i.I[t];
                
                i.R[t+1] = i.R[t] + inv_ell*i.I[t];
                
                if(includeTravel)
                {
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
        
        double output = 0.0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi_lambda = index_lambda(t);
            
            for(Zone i : zones)
            {
                double temp = i.I[t] - i.lambda[pi_lambda] * i.reportedI[t];
                output += temp*temp;
            }
        }
        
        return output;
        */
    }
    
    private boolean printdebug = false;
    
    public double calculateSEIRsearch(double step, int iter)
    {
        
        for(Zone i : zones)
        {
            i.I[startTime] = Math.min(10, Math.max(1, i.lambda[index_lambda(startTime)] - step*i.gradient_lambda[index_lambda(startTime)])) * i.reportedI[startTime];
            i.E[startTime] = Math.min(i.getN()/100, Math.max(0, i.E0 - step * i.gradient_E0));
            

            i.R[startTime] = 0;
            i.S[startTime] = i.getN() - i.I[startTime] - i.R[startTime] - i.E[startTime];
            
            if(printdebug)
            {
                System.out.println("start "+i.I[startTime]+" "+i.E[startTime]);
            }
            
        }
        
        double newxi = Math.min(1, Math.max(0, xi-step*gradient_xi));
        double newxiE = Math.min(1, Math.max(0, xi_E-step*gradient_xiE));
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi_r = index_r(t);
            
            for(int ix = 0; ix < zones.length; ix++)
            {
                Zone i = zones[ix];
                
                if(i.getN(t) == 0)
                {
                    System.out.println("N is 0\t2");
                }
                double fSE = Math.min(i.S[t], Math.min(20, Math.max(0, i.r[pi_r] - step*i.gradient_r[pi_r])) * i.S[t] * i.I[t]/i.getN());
                
                if((""+i.gradient_r[pi_r]).equals("NaN"))
                {
                    System.out.println("grad r is nan");
                }

                i.S[t+1] = i.S[t] - fSE ;
                
                i.E[t+1] = i.E[t] + fSE - (inv_sigma - step*gradient_inv_sigma) *i.E[t];
                
                i.I[t+1] = i.I[t] + (inv_sigma - step*gradient_inv_sigma)*i.E[t] - (inv_ell - step*gradient_inv_ell)*i.I[t];
                
                i.R[t+1] = i.R[t] + (inv_ell - step*gradient_inv_ell)*i.I[t];
                
                if(printdebug)
                {
                    System.out.println(t+"\t"+fSE+"\t"+i.E[t+1]+"\t"+i.I[t+1]+"\t"+i.R[t+1]);
                }
                
                /*
                if(iter == 2)
                {
                    System.out.println(i.getId()+" "+t+"\t"+i.S[t]+" "+i.E[t]+" "+i.I[t]+" "+i.R[t]+" "+i.getN()+" "+i.getN(t)+
                            " "+Math.max(0, i.r[pi_r] - step*i.gradient_r[pi_r]));
                }
                */
                
                if(includeTravel)
                {
                    for(int jx = 0; jx < zones.length; jx++)
                    {
                        if(ix != jx)
                        {
                            Zone j = zones[jx];

                            i.S[t+1] += matrix[jx][ix].getMu(t) * j.S[t] - matrix[ix][jx].getMu(t) * i.S[t];

                            i.E[t+1] +=newxiE * (matrix[jx][ix].getMu(t) * j.E[t] - matrix[ix][jx].getMu(t) * i.E[t]);

                            i.I[t+1] += newxi*(matrix[jx][ix].getMu(t) * j.I[t] - matrix[ix][jx].getMu(t) * i.I[t]);

                            i.R[t+1] += matrix[jx][ix].getMu(t) * j.R[t] - matrix[ix][jx].getMu(t) * i.R[t];
                        }
                    }
                }

            }
        }
        
        double output = 0.0;
        
        for(int t = startTime; t < T; t++)
        {
            int pi_lambda = index_lambda(t);
            
            for(Zone i : zones)
            {
                double temp = i.I[t] - Math.min(10, Math.max(1, i.lambda[pi_lambda] - step*i.gradient_lambda[pi_lambda])) * i.reportedI[t];
                output += temp*temp;
                
                temp = i.R[t] - Math.min(10, Math.max(1, i.lambda[pi_lambda] - step*i.gradient_lambda[pi_lambda])) * i.reportedR[t];
                output += removed_weight * temp*temp;
            }
        }

        
        return output;
    }

}
