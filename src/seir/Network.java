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
    public static final boolean reduce_travel = true;
    
    protected double inv_sigma = 1.0/6.4; //0.15; // incubation time
    protected double inv_ell = 1.0/7; //0.08* 0.985 + 0.12* 0.015; // recovery time
    
    private double gradient_inv_sigma, gradient_inv_ell;
    
    private double xi = 0.2; // reduction in travel among infected individuals
    private double xi_E = 0.8; // reduction in travel among exposed individuals
    private double gradient_xi, gradient_xiE;
    private double removed_weight = 0.0;
    
    protected int T;
    
    private Zone[] zones;
    private Link[][] matrix;
    
    protected TimePeriod[] lambda_periods, r_periods;
    
    private Date startDate;
    
    private String scenario;
    
    protected boolean includeTravel;
    private int startTime = 0;
    
    private boolean randomize;
    
    private int run = 0;
    private boolean useLambda = true;
    
    
    private boolean noInitialize = false;

    private int randomSeed;
    private Random rand;
    
    private boolean model2 = false;
    
    
    protected String minlabel, maxlabel;
    protected Color mincolor, maxcolor;
    
    private double total_infections;
    private double infections_from_travel;
    
    public Network(String scenario) throws IOException
    {
        
        
        
        this.scenario = scenario;
        readNetwork(scenario);
    }
    
    public Network(String scenario, int T) throws IOException
    {
        
        this.T = T;
        
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
        
        fileout.println(inv_sigma+"\t"+inv_ell+"\t"+xi+"\t"+xi_E+"\t"+includeTravel+"\t"+startTime+"\t"+removed_weight+"\t"+iter+"\t"+model2);
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
    
    private boolean loaded = false;
    
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
        
        loaded = true;
        
        Scanner filein = new Scanner(file);
        
        inv_sigma = filein.nextDouble();
        inv_ell = filein.nextDouble();
        xi = filein.nextDouble();
        xi_E = filein.nextDouble();
        includeTravel = filein.next().equalsIgnoreCase("true");
        startTime = filein.nextInt();
        removed_weight = filein.nextDouble();
        iter = filein.nextInt();
        model2 = filein.next().equalsIgnoreCase("true");
        
        
        
        filein.nextLine();
        
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
    
    public Zone findZone(int id)
    {
        for(Zone i : zones)
        {
            if(i.getId() == id)
            {
                return i;
            }
        }
        
        return null;
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
            else if(key.equalsIgnoreCase("model2"))
            {
                model2 = value.equalsIgnoreCase("true");
            }
            else if(key.equalsIgnoreCase("ell"))
            {
                inv_ell = 1.0/Double.parseDouble(value.trim());
            }
        }
        filein.close();
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        int format = 0;
        
        if(scenario.contains("MN"))
        {
            format = 1;
        }
        else if(scenario.contains("TX"))
        {
            format = 2;
        }
        
        if(format == 1)
        {
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

            if(T == 0)
            {
                T = count;
            }


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
            
        }
        else if(format == 2)
        {
            filein = new Scanner(new File("data/"+dir+"/cumulative_cases.txt"));
            
            int count = 0;
            int countT = 0;
            
            count++;
            filein.next();
            filein.next();
            
            while(filein.hasNextInt())
            {
                countT++;
                filein.nextInt();
            }
            
            filein.nextLine();
            
            
            while(filein.hasNext())
            {
                count++;
                
                filein.nextLine();
            }
            filein.close();
            T = countT;
            
            
            
            
            int idx = 0;
            zones = new Zone[count];
            
            filein = new Scanner(new File("data/"+dir+"/cumulative_cases.txt"));
            
            
            while(filein.hasNext())
            {
                String name = filein.next();
                                
                if(!filein.hasNextInt())
                {
                    filein.next();
                }
                
                int N = filein.nextInt();
                
                zones[idx] = new Zone(48000+idx*2+1, N);
                
                
                double[] reportedI = new double[T];
                
                for(int t = 0; t < reportedI.length; t++)
                {
                    reportedI[t] = filein.nextDouble();
                }
                
                zones[idx].setReportedI(reportedI);
                
                
                filein.nextLine();
                idx++;
                
                
            }
            filein.close();
            
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
        

        try
        {
            filein = new Scanner(new File("data/"+dir+"/county_data.txt"));

            filein.nextLine();

            while(filein.hasNext())
            {
                int id = filein.nextInt();
                double value = filein.nextDouble();

                findZone(id).data = value;
            }
            filein.close();
        }
        catch(IOException ex){}

        
        
        
        
        
        
        filein = new Scanner(new File("data/"+dir+"/timeline_r.txt"));
        
        Date start = null;
        
        List<Integer> timeline = new ArrayList<>();
        
        try
        {
            start = new SimpleDateFormat("MM/dd/yyyy").parse(filein.nextLine().trim()); 
            startDate = start;
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

        
        
        
        
        
        int count = 0;
       
        
        if(format == 1)
        {
            filein = new Scanner(new File("data/"+scenario+"/travel_change.csv"));
            Map<Integer, Double> changes = new HashMap<Integer, Double>();

            filein.nextLine();

            int lastDay = 0;
            int earliest_change_date = Integer.MAX_VALUE;
            while(filein.hasNext())
            {
                Scanner chopper = new Scanner(filein.nextLine());
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

                    if(!reduce_travel)
                    {
                        end_apply = T;
                    }



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


                    double total1 = 0;
                    double total2 = 0;

                    double scaleup = 1.0 / (total_actual / total_predicted);

                    Scanner filein2 = new Scanner(new File("data/"+scenario+"/"+datafile));

                    while(filein2.hasNext())
                    {
                        Scanner chopper = new Scanner(filein2.nextLine());
                        chopper.useDelimiter(",");

                        int source = chopper.nextInt();
                        int dest = chopper.nextInt();
                        double demand = chopper.nextDouble();

                        total1 += demand;


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

                        total2 += demand;

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

                            if(reduce_travel)
                            {
                                matrix[r][c].setNormalDemand(zones[r], zones[c], t, demand*scaleup * scaledown);
                            }
                            else
                            {
                                matrix[r][c].setNormalDemand(zones[r], zones[c], t, demand);
                            }

                        }
                    }
                    filein2.close();



                    System.out.println("Loaded travel: "+start_apply+" to "+end_apply+" "+total1+" "+total2);

                    if(!reduce_travel)
                    {
                        break;
                    }

                }
                catch(ParseException ex)
                {
                    ex.printStackTrace(System.err);
                }


            }
            filein.close();


            File boundsdir = new File("data/"+scenario+"/boundaries/");

            for(File f : boundsdir.listFiles())
            {
                filein = new Scanner(f);

                int county = 0;

                List<Location> coords = new ArrayList<>();

                filein.nextLine();

                while(filein.hasNext())
                {
                    Scanner chopper = new Scanner(filein.nextLine());
                    chopper.useDelimiter(",");

                    chopper.next();
                    county = chopper.nextInt();
                    chopper.next();
                    chopper.next();
                    chopper.next();

                    coords.add(new Location(chopper.nextDouble(), chopper.nextDouble()));
                }

                filein.close();

                for(Zone i : zones)
                {
                    if(i.getId() % 1000 == county % 1000)
                    {
                        i.setBoundary(coords);
                        break;
                    }
                }
            }
        }
        else if(format == 2)
        {
            filein = new Scanner(new File("data/"+dir+"/county_travel.txt"));
            
            filein.nextLine();
            
            for(int i = 0; i < matrix.length; i++)
            {
                int id = filein.nextInt();
   
                
                for(int j = 0; j < matrix[i].length; j++)
                {
                    double demand = filein.nextDouble();
                    
                    //System.out.println(j+"\t"+zones[j].getId());
                    
                    if(i != j)
                    {
                        for(int t = 0; t < T; t++)
                        {
                            
                            matrix[i][j].setNormalDemand(zones[i], zones[j], t, demand);
                        }
                    }
                }
            }
        }
    }
    
    public void colorZonesData(double max)
    {
        for(Zone i : zones)
        {
            int red = (int)Math.round(255*Math.max(0, Math.min(1, i.data / max)));
            i.color = new Color(255-red, 255, 255);
        }
        
        minlabel = "≤0";
        maxlabel = "≥"+max;
        mincolor = Color.white;
        maxcolor = Color.cyan;
    }
    
    public void colorZonesE0(int max_E0)
    {
        for(Zone i : zones)
        {
            int red = (int)Math.round(255*Math.min(1, i.E0 / max_E0));
            i.color = new Color(255, 255-red, 255);
        }
        
        minlabel = "0";
        maxlabel = "≥"+max_E0;
        mincolor = Color.white;
        maxcolor = Color.magenta;
    }
    
    public void colorZonesRpct(double max)
    {
        int total_R = 0;
        
        for(Zone i : zones)
        {
            double pct = i.R[T-1] / i.getN();
            
            //System.out.println((pct*100)+" "+pct / (max/100.0));
            int red = (int)Math.round(255*Math.min(1, pct / (max/100.0)));
            i.color = new Color(255, 255-red, 255);
            
            total_R += i.R[T-1];
        }
        
        minlabel = "0%";
        maxlabel = "≥"+max+"%";
        mincolor = Color.white;
        maxcolor = Color.magenta;
        
        System.out.println(total_R);
    }
    
    public void colorZonesLambda()
    {
        for(Zone i : zones)
        {
            double avg_lambda = 0;
            double count = 0;
            
            for(int t = startTime; t < T; t++)
            {
                avg_lambda += i.lambda[index_lambda(t)];
                count++;
            }
            
            avg_lambda /= count;
            
            int red = (int)Math.round(255*Math.min(1, (avg_lambda -1)/2));
            i.color = new Color(255-red, 255-red, 255);
        }
        
        minlabel = "≤50%";
        maxlabel = "100%";
        mincolor = Color.white;
        maxcolor = Color.blue;
    }
    
    public void colorZonesr()
    {
        for(Zone i : zones)
        {
            double avg_r = 0;
            double count = 0;
            
            for(int t = startTime; t < T; t++)
            {
                avg_r += i.r[index_r(t)];
                count++;
            }
            
            avg_r /= count;
            
            int red = (int)Math.round(255*Math.min(1, avg_r/1));
            i.color = new Color(255-red, 255, 255-red);
        }
        
        minlabel = "0";
        maxlabel = "≥2";
        mincolor = Color.white;
        maxcolor = Color.green;
    }
    
    public double getTotalCases()
    {
        double output = 0;
        
        for(Zone i : zones)
        {
            output += getTotalCases(i);
        }
        
        return output;
    }
    
    public double getTotalCases(Zone i)
    {
        double output = 0;
        for(int t = startTime; t < T; t++)
        {
            output += i.I[t];
        }
        
        return output * inv_ell;
    }
    
    
    
    public void colorZonesr(int t)
    {
        for(Zone i : zones)
        {

            int red = (int)Math.round(255*Math.min(1, calcRepRate(i, t)/2));
            i.color = new Color(255-red, 255, 255-red);
        }
        
        minlabel = "0";
        maxlabel = "≥2";
        mincolor = Color.white;
        maxcolor = Color.green;
    }

    public void colorZonesTravelI(int max)
    {
        for(Zone i : zones)
        {
            int red = (int)Math.round(255*Math.min(1, i.infections_from_travel/(i.getN()/1000) / (max)));
            
            //System.out.println(i.infections_from_travel/(i.getN()/1000));
            i.color = new Color(255, 255-red, 255-red);
        }
        
        minlabel = "0";
        maxlabel = "≥"+max;
        mincolor = Color.white;
        maxcolor = Color.red;
    }
    
    public void colorZonesReportedI(int max, int t)
    {
        for(Zone i : zones)
        {
            double cases = i.reportedI[t];
            double pop = i.getN()/1000;

            
            int red = (int)Math.round(255*Math.min(1, (cases) / max));
            i.color = new Color(255, 255-red, 255-red);
        }
        
        minlabel = "0";
        maxlabel = "≥"+max;
        mincolor = Color.white;
        maxcolor = Color.red;
    }
    
    public Date getDate(int t)
    {
        Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
        
        return date;
    }
    
    public void colorZonesI(int max, int t)
    {
        for(Zone i : zones)
        {
            double cases = i.I[t];
            double pop = i.getN()/1000;

            //System.out.println(i.data);
            
            int red = (int)Math.round(255*Math.min(1, i.I[t] / max));
            i.color = new Color(255, 255-red, 255-red);
        }
        
        minlabel = "0";
        maxlabel = "≥"+max;
        mincolor = Color.white;
        maxcolor = Color.red;
    }
    
    public void printZone(Zone i) throws IOException
    {
        printZone(i, new File(getDirectory()+"/graphs/zone_"+i.getId()+".txt"));
    }
    
    public void printZone(Zone i, File file) throws IOException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(file), true);
        
        fileout.println("time\treported I\t reported*lambda\tpredicted");
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");

        
        for(int t = startTime; t < T; t++)
        {
            Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
            
            fileout.println(simpleDateFormat.format(date)+"\t"+i.reportedI[t]+"\t"+(i.reportedI[t]*i.lambda[index_lambda(t)])+"\t"+i.I[t]);
        }
        
        
        fileout.close();
    }
    
    public void colorZonesE0Prop()
    {
        double max_pct = 0;
        
        for(Zone i : zones)
        {
            max_pct = Math.max(max_pct, i.E0/i.getN());
        }
        
        for(Zone i : zones)
        {
            int red = (int)Math.round(255*Math.min(1, (i.E0 / i.getN()) / max_pct ));
            i.color = new Color(255, 255-red, 255-red);
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
    
    public void printTotalError(File file) throws IOException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(file), true);
        
        printTotalError(fileout);
        
        fileout.close();
    }

    
    public void printTotalError(PrintStream out) throws IOException
    {
        out.print("time\ttotal error\t% error\tcount\tpredicted\treported*lambda\tlong time\ttotal error\t% error\tcount R\tpredicted R\t reported R*lambda\ttrips");
        
        /*
        for(Zone i : zones)
        {
            out.print("\t"+i+" reported\t"+i+" predicted\t"+i+"% error");
        }
        */
        out.println();
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        for(int t = startTime; t < T; t++)
        {
            double error = 0;
            double count = 0;
            double predicted = 0;
            double replambda = 0;
            
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                error += Math.abs( i.I[t] - i.lambda[index_lambda(t)] * i.reportedI[t]);
                predicted += i.I[t];
                count += i.reportedI[t];
                replambda += i.lambda[index_lambda(t)] * i.reportedI[t];
            }

            Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
            out.print(simpleDateFormat.format(date)+"\t"+error+"\t"+(100.0*error/count)+"\t"+ count+"\t"+ predicted+"\t"+replambda+"\t"+date.getTime());
            
            error = 0;
            count = 0;
            predicted = 0;
            replambda = 0;
            
            pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                error += Math.abs( i.R[t] - i.lambda[index_lambda(t)] * i.reportedR[t]);
                predicted += i.R[t];
                count += i.reportedR[t];
                replambda += i.lambda[index_lambda(t)] * i.reportedR[t];
            }
            
            out.print("\t"+error+"\t"+(100.0*error/count)+"\t"+ count+"\t"+ predicted+"\t"+replambda);
            out.print("\t"+getTotalTrips(t));
            
            /*
            for(Zone i : zones)
            {
                double predictedi = i.I[t];
                double reportedi = i.lambda[index_lambda(t)]*i.reportedI[t];
                double errori = Math.abs(i.I[t] - i.lambda[index_lambda(t)]*i.reportedI[t])/i.reportedI[t];
                out.print(reportedi+"\t"+ predictedi+"\t"+ ( errori*100.0));
            }
            */
            out.println();
        }
    }
    
    int num_iter = 100;
    double alpha = 0.001;
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
    
    private int iter;
    
    public double gradientDescent() throws IOException 
    {
        

        long total_time = System.nanoTime();
        
        randomSeed = (int)Math.round(100000*Math.random());
        rand = new Random(randomSeed);
        
        
        if(!loaded)
        {
            initialize();
        
            initialSolution();
        }
        
        
        
        
        
        PrintStream fileout =  new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/log_"+run+".txt"), loaded), true);
        
        
        if(!loaded)
        {
        
            fileout.println(scenario + " run "+run);
            fileout.println("T = "+T);
            fileout.println("lambda_periods = "+lambda_periods.length);
            fileout.println("r_periods = "+r_periods.length);
            fileout.println("include travel: "+includeTravel);
            fileout.println("randomize "+randomize);
            fileout.println("model2 "+model2);
        }
        
        System.out.println(scenario + " run "+run);
        System.out.println("T = "+T);
        System.out.println("lambda_periods = "+lambda_periods.length);
        System.out.println("r_periods = "+r_periods.length);
        System.out.println("include travel: "+includeTravel);
        System.out.println("randomize "+randomize);
        System.out.println("model2 "+model2);
        
        
        if(randomize)
        {
            System.out.println("random seed: "+randomSeed);
        }
        
        
        System.out.println("Iteration\tObjective\tObj. change\tError\tCPU time");
        
        if(!loaded)
        {
            fileout.println("Iteration\tObjective\tObj. change\tI Error\tCPU time (s)");
        }
        
        if(!loaded)
        {
            iter = 1;
        }
        else
        {
            iter++;
        }
        
        double obj = calculateSEIR();
        double improvement = 100;
        
        System.out.println((iter-1)+"\t"+obj);
        
        double prev_obj = obj;
        
        double updated_obj = obj;
        
        
        
        for(;iter <= num_iter && improvement > min_improvement; iter++)
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
                
                double test = calculateSEIRsearch(step, 0);
                updateVariables(step);
                updated_obj = obj;
                obj = calculateSEIR();
                
                if((updated_obj-obj)/updated_obj*100.0 < -0.009)
                {
                    System.out.println("\t"+i+"-r\t"+obj+"\t"+updated_obj+"\t"+step+"\t"+String.format("%.2f", (updated_obj-obj)/updated_obj*100.0)+"\t"+test);
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
                

                //System.out.print(xi+" "+xi_E+"\t");
                
                if(includeTravel)
                {
                    resetGradients();
                
                    calculateGradient_xi();

                    if(!model2)
                    {
                        calculateGradient_xiE();
                    }

                    step = calculateStep(iter, obj);
                    updateVariables(step);
                    obj = calculateSEIR();
                }
                
                //System.out.println("xi step: "+(step * gradient_xi)+" "+(step*gradient_xiE)+"\t"+xi+" "+xi_E);
            }
            
            save();
            
            time = System.nanoTime() - time;
            // System.out.println("Step: "+step);
            //System.out.println("Max step: "+calculateMaxStep());
            //System.out.println("Obj: "+obj);
            improvement = 100.0*(prev_obj - obj) / prev_obj;
            
            double error = calculateInfectedError();
            
            System.out.println(iter+"\t"+obj+"\t"+String.format("%.2f", improvement)
                    +"%\t"+String.format("%.2f", error)+"%\t"+String.format("%.1f", time/1.0e9)+"s");
            fileout.println(iter+"\t"+obj+"\t"+String.format("%.2f", improvement)
                    +"%\t"+String.format("%.2f", error)+"%\t"+String.format("%.1f", time/1.0e9)+"s");
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
                error += Math.abs(i.fEI[t] - i.lambda[pi] * i.reportedI[t]);
            }
        }
        
        return 100.0*error/total;
    }
    
    
    public void printRates(Zone i) throws IOException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/r_lambda_"+i.getId()+".txt")), true);
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        fileout.println("time\tr\tlambda");
        for(int t = startTime; t < T; t++)
        {

            
            Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
            
            fileout.println(simpleDateFormat.format(date)+"\t"+(i.r[index_r(t)]/inv_sigma)+"\t"+i.lambda[index_lambda(t)]);
            
        }
        
        fileout.close();
    }
    
    public void printTripsPerDay() throws IOException
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/trips.txt")), true);
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        fileout.println("time\ttrips");
        for(int t = startTime; t < T; t++)
        {

            
            Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
            
            fileout.println(simpleDateFormat.format(date)+"\t"+getTotalTrips(t));
            
        }
        
        fileout.close();
    }
    
    public double getTotalTrips(int t)
    {
        double output = 0.0;
        
        for(int r = 0; r < matrix.length; r++)
        {
            for(int c = 0; c < matrix[r].length; c++)
            {
                if(r != c)
                {
                    output += matrix[r][c].getTrips(t);
                }
            }
        }
        
        return output;
    }
    
    public double getAvgRepRate(int t)
    {
        double total = 0;
        double count = 0;

        int pi = index_r(t);

        for(Zone i : zones)
        {

            double r = calcRepRate(i, t);
            total += r * i.getN();
            count += i.getN();


        }

        return total/count;
    }
    
    public double getAvgRho(int t)
    {
        double total = 0;
        double count = 0;

        int pi = index_r(t);

        for(Zone i : zones)
        {

            double r = i.r[pi];
            total += r * i.getN();
            count += i.getN();


        }

        return total/count;
    }
    
    public void printAverageRates() throws IOException 
    {
        PrintStream fileout = new PrintStream(new FileOutputStream(new File("data/"+scenario+"/output/avg_r_lambda.txt")), true);
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        
        fileout.println("time\tavg r\tavg lambda\tavg. rho");
        for(int t = startTime; t < T; t++)
        {
            
            double total = 0;
            double count = 0;
            
            int pi = index_r(t);
            
            for(Zone i : zones)
            {
                
                double r = calcRepRate(i, t);
                total += r * i.getN();
                count += i.getN();
                

            }
            
            double avg_r = total/count;
            
            
            
            
            total = 0;
            count = 0;
            
            pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                total += 1.0/i.lambda[pi] * i.getN();
                count += i.getN();
            }
            
            double avg_lambda = total/count;
            
            
            total = 0;
            count = 0;
            
            pi = index_r(t);
            
            for(Zone i : zones)
            {
                total += i.r[pi] * i.getN();
                count += i.getN();
            }
            
            double avg_rho = total/count;
            
            
            Date date = new Date(startDate.getTime() + 1000L*3600*24*t);
            
            fileout.println(simpleDateFormat.format(date)+"\t"+avg_r+"\t"+avg_lambda+"\t"+avg_rho);
            
        }
        
        fileout.close();
    }
    
    public double calcRepRate(Zone i, int t)
    {
        double r = 0.0;
                
        double num = 0.0;
        double denom = 0.0;
        
        int pi = index_r(t);

        num += i.r[pi] * i.S[t] * i.I[t]/i.getN();
        denom += i.I[t];

        int ix = i.getIdx();

        for(int jx = 0; jx < zones.length; jx++)
        {
            Zone j = zones[jx];

            if(ix != jx)
            {
                num += xi * i.r[pi] * i.S[t] * matrix[jx][ix].getMu(t)*j.I[t] / j.getN();
                denom += xi * i.r[pi] * matrix[jx][ix].getMu(t)*j.I[t];
            }
        }

        if(denom == 0)
        {
            r = i.r[pi] / inv_ell;
        }
        else
        {
            r = num/denom / inv_ell;
        }
        
        return r;
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
                if(i.reportedR != null)
                {
                    total += i.reportedR[t];
                    error += Math.abs(i.R[t] - i.lambda[pi] * i.reportedR[t]);
                }
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
        double compare;

        
        while( (output = calculateSEIRsearch(step, iter)) > (compare = obj + alpha * step * change))
        {
            inner_iter ++;
            
            //System.out.println(inner_iter);
            
            if(inner_iter > max_step_iter)
            {
                step = 0;
                break;
            }
            
            //System.out.println("\t\t"+output+" > "+(obj + alpha * step * change)+"\t"+step+" "+inner_iter+"\t"+(output>(obj + alpha * step * change))+"\t"+compare);
            
            step = step*beta;
        }
        
        output = calculateSEIRsearch(step, iter);
        
        if(output > obj)
        {
            System.out.println("output > obj??");
        }

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
            
            i.E0 = Math.min(i.getN()/100.0, Math.max(0, i.E0 - step * i.gradient_E0));
            
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
                //double dN = i.dS[t] + i.dE[t] + i.dI[t] + i.dR[t];
                double dN = 0;
                
                double N = i.getN(t);
                
                i.dI[t+1] = i.dI[t] + inv_sigma * i.dE[t]  - inv_ell * i.dI[t];
                
                i.dE[t+1] = i.dE[t] + i.r[pi]*i.dS[t]*i.I[t]/N + i.r[pi]*i.S[t]/N*i.dI[t] 
                        - i.r[pi]*i.S[t]*i.I[t]/N/N*dN - inv_sigma*i.dE[t];
                
                i.dS[t+1] = i.dS[t] - i.r[pi]*i.dS[t]*i.I[t]/N - i.r[pi]*i.S[t]/N*i.dI[t] +
                        i.r[pi]*i.S[t]*i.I[t]/N/N*dN;
                
                i.dR[t+1] = i.dR[t] + inv_ell*i.dI[t];
                
                
                if(model2 && includeTravel)
                {
                    int ix = i.getIdx();
                    
                    for(int jx = 0; jx < matrix.length; jx++)
                    {
                        if(jx != ix)
                        {
                            Zone j = zones[jx];


                            i.dS[t+1] -= xi*i.r[pi]*i.dS[t] * matrix[jx][ix].getMu(t) * j.I[t] / j.getN();
                            i.dS[t+1] -= xi*i.r[pi] * i.S[t] * matrix[jx][ix].getMu(t) * j.dI[t] / j.getN();
                            i.dS[t+1] -= i.r[pi] * i.S[t] * matrix[jx][ix].getMu(t) * j.I[t] / j.getN();
                            
                            i.dE[t+1] += xi*i.r[pi]*i.dS[t] * matrix[jx][ix].getMu(t) * j.I[t] / j.getN();
                            i.dE[t+1] += xi*i.r[pi] * i.S[t] * matrix[jx][ix].getMu(t) * j.dI[t] / j.getN();
                            i.dE[t+1] += i.r[pi] * i.S[t] * matrix[jx][ix].getMu(t) * j.I[t] / j.getN();
                        }
                    }
                }
                
                if(!model2 && includeTravel)
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
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_lambda(t);
            
            for(Zone i : zones)
            {
                gradient_xi += 2* (i.fEI[t] - i.lambda[pi]* (i.reportedI[t+1] - i.reportedI[t]))* (inv_sigma *i.dE[t]);
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
                
                if(!model2 && includeTravel)
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

            for(int t = Math.max(startTime, lambda_periods[pi].getStart()); t < lambda_periods[pi].getEnd() && t < T-1; t++)
            {
                double value = -1 * 2* (i.fEI[t] - i.lambda[pi] * (i.reportedI[t+1]-i.reportedI[t])) * (i.reportedI[t+1]-i.reportedI[t]);
                sum += value;

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
                    
                    //double dN = j.dS[t] + j.dE[t] + j.dI[t] + j.dR[t];
                    double dN = 0;
                    
                    j.dE[t+1] = j.dE[t] + drdr * j.S[t] * j.I[t]/j.getN(t) + j.r[t_idx]*j.dS[t]*j.I[t]/N
                            + j.r[t_idx] * j.S[t]/N * j.dI[t] - j.r[t_idx]*j.S[t]*j.I[t]/N/N*dN
                            - inv_sigma * j.dE[t];

                    j.dS[t+1] = j.dS[t] - drdr * j.S[t] * j.I[t]/N - j.r[t_idx]*j.dS[t]*j.I[t]/N
                            + j.r[t_idx]*j.S[t]*j.I[t]/N/N*dN - j.r[t_idx] * j.S[t]/N * j.dI[t];

                    j.dR[t+1] = j.dR[t] + inv_ell*j.dI[t];
                    
                    
                    
                    if(model2 && includeTravel)
                    {
                        for(int kx = 0; kx < matrix.length; kx++)
                        {
                            if(jx != kx)
                            {
                                Zone k = zones[kx];

                                j.dS[t+1] -= xi*j.r[t_idx]*j.dS[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();
                                j.dS[t+1] -= xi*j.r[t_idx] * j.S[t] * matrix[kx][jx].getMu(t) * k.dI[t] / k.getN();
                                j.dS[t+1] -= xi*drdr * j.S[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();

                                j.dE[t+1] += xi*j.r[t_idx]*j.dS[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();
                                j.dE[t+1] += xi*j.r[t_idx] * j.S[t] * matrix[kx][jx].getMu(t) * k.dI[t] / k.getN();
                                j.dE[t+1] += xi*drdr * j.S[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();
                            }
                        }
                    }

                    if(!model2 && includeTravel)
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

            for(int t = startTime; t < T-1; t++)
            {
                int t_idx = index_lambda(t);

                for(Zone j : zones)
                {
                    i.gradient_r[pix] += 2*(j.fEI[t] - j.lambda[t_idx] * (j.reportedI[t+1] - j.reportedI[t]))* (inv_sigma * j.dE[t]);
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
                
                
                if(model2 && includeTravel)
                {
                    for(int kx = 0; kx < matrix.length; kx++)
                    {
                        if(jx != kx)
                        {
                            Zone k = zones[kx];
                            
                            j.dS[t+1] -= xi*j.r[t_idx]*j.dS[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();
                            j.dS[t+1] -= xi*j.r[t_idx] * j.S[t] * matrix[kx][jx].getMu(t) * k.dI[t] / k.getN();
                            
                            j.dE[t+1] += xi*j.r[t_idx]*j.dS[t] * matrix[kx][jx].getMu(t) * k.I[t] / k.getN();
                            j.dE[t+1] += xi*j.r[t_idx] * j.S[t] * matrix[kx][jx].getMu(t) * k.dI[t] / k.getN();
                        }
                    }
                }

                if(!model2 && includeTravel)
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

        for(int t = startTime; t < T-1; t++)
        {
            int pi = index_lambda(t);

            for(Zone j : zones)
            {
                i.gradient_E0 += 2*(j.fEI[t] - j.lambda[pi]*(j.reportedI[t+1]-j.reportedI[t])) * (inv_sigma * j.dE[t]);

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
        //System.out.println("step: "+step);
        
        total_infections = 0;
        infections_from_travel = 0;
        
        for(Zone i : zones)
        {
            i.I[startTime] = Math.min(10, Math.max(1, i.lambda[index_lambda(startTime)] - step*i.gradient_lambda[index_lambda(startTime)])) * i.reportedI[startTime];
            i.E[startTime] = Math.min(i.getN()/100.0, Math.max(0, i.E0 - step * i.gradient_E0));
            

            i.R[startTime] = 0;
            i.S[startTime] = i.getN() - i.I[startTime] - i.R[startTime] - i.E[startTime];
            
            if(printdebug)
            {
                System.out.println("start "+i.I[startTime]+" "+i.E[startTime]);
            }
            
            i.total_infections = 0;
            i.infections_from_travel = 0;
        }
        
        double newxi = Math.min(1, Math.max(0, xi-step*gradient_xi));
        double newxiE = Math.min(1, Math.max(0, xi_E-step*gradient_xiE));
        
        for(int t = startTime; t < T-1; t++)
        {
            int pi_r = index_r(t);
            
            for(int ix = 0; ix < zones.length; ix++)
            {
                Zone i = zones[ix];
                

                
                double newr = Math.min(20, Math.max(0, i.r[pi_r] - step*i.gradient_r[pi_r]));
                
                /*
                if(t > 152)
                {
                    pi_r = index_r(startTime+10);
                    newr = Math.min(20, Math.max(0, i.r[pi_r] - step*i.gradient_r[pi_r]));
                }
                */
                
                double fSE = Math.min(i.S[t], newr * i.S[t] * i.I[t]/i.getN());

                
                i.S[t+1] = i.S[t] - fSE ;
                
                i.fEI[t] = (inv_sigma - step*gradient_inv_sigma) *i.E[t];
                i.E[t+1] = i.E[t] + fSE - i.fEI[t];
                
                i.I[t+1] = i.I[t] + i.fEI[t] - (inv_ell - step*gradient_inv_ell)*i.I[t];
                
                i.R[t+1] = i.R[t] + (inv_ell - step*gradient_inv_ell)*i.I[t];
                
                total_infections += fSE;
                i.total_infections += fSE;
                
                if(model2 && includeTravel)
                {
                    fSE = 0;
                    
                    for(int jx = 0; jx < zones.length; jx++)
                    {
                        if(jx != ix)
                        {
                            Zone j = zones[jx];
                        
                            fSE += Math.max(0, Math.min(1, xi - step*gradient_xi)) *newr * i.S[t] * j.I[t]/j.getN() * matrix[jx][ix].getMu(t);
                        }  
                    }
                    
                    
                    //if(t < 153)
                    {
                        fSE = Math.min(i.S[t], fSE);

                        i.S[t+1] -= fSE;
                        i.E[t+1] += fSE;

                        total_infections += fSE;
                        i.total_infections += fSE;

                        infections_from_travel += fSE;
                        i.infections_from_travel += fSE;
                    }
                }
                

                
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
                
                
                if(!model2 && includeTravel)
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
        
        for(int t = startTime+1; t < T-1; t++)
        {
            int pi_lambda = index_lambda(t);
            
            for(Zone i : zones)
            {
                double newlambda = Math.min(10, Math.max(1, i.lambda[pi_lambda] - step*i.gradient_lambda[pi_lambda]));
                double temp = i.fEI[t] - newlambda * (i.reportedI[t+1] - i.reportedI[t]);
                output += temp*temp;
                
            }
        }
        
        /*
        for(Zone i : zones)
        {
            i.data = i.infections_from_travel / i.total_infections;
        }
        */
        return output;
    }

}
