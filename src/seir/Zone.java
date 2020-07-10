/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.Color;
import java.util.List;

/**
 *
 * @author micha
 */
public class Zone 
{
    private int index;
    private static int next_index = 0;
    
    protected double[] S, E, I, R;
    protected double[] dS, dE, dI, dR;
    
    protected double[] reportedI, reportedR;
    
    private double N;
    private int id;
    
    protected double[] lambda, r, gradient_lambda, gradient_r;
    protected double gradient_E0, E0;
    
    private List<Location> boundary;
    
    protected Color color;
    
    protected double data;
    
    public Zone(int id, double N)
    {
        index = next_index++;
        
        this.id = id;
        this.N = N;
        
        color = new Color(255, 255, 255, 128);
        
    }
    
    public List<Location> getBoundary()
    {
        return boundary;
    }
    
    
    public void setBoundary(List<Location> bounds)
    {
        boundary = bounds;
    }
    

    
    public void initialize(int T, TimePeriod[] r_periods, TimePeriod[] lambda_periods)
    {
        
        S = new double[T];
        E = new double[T];
        I = new double[T];
        R = new double[T];
        
        dS = new double[T];
        dE = new double[T];
        dI = new double[T];
        dR = new double[T];
        
        r = new double[r_periods.length];
        lambda = new double[lambda_periods.length];
        
        for(int pi = 0; pi < lambda.length; pi++)
        {
            lambda[pi] = 1;
        }
        
        gradient_lambda = new double[lambda_periods.length];
        gradient_r = new double[r_periods.length];
        gradient_E0 = 0;
        E0 = 0;
    }
    
    public void resetDerivs()
    {
        for(int t = 0; t < dS.length; t++)
        {
            dS[t] = 0;
            dE[t] = 0;
            dI[t] = 0;
            dR[t] = 0;
        }
    }
    
    public void setReportedI(double[] val)
    {
        reportedI = val;
    }
    
    public void setReportedR(double[] val)
    {
        reportedR = val;
    }
    
    public void addReportedR(double[] val)
    {
        for(int i = 0; i < reportedR.length; i++)
        {
            reportedR[i] += val[i];
        }
    }
    
    public double getN()
    {
        return N;
    }
    
    public double getN(int t)
    {
        return S[t]+E[t]+I[t]+R[t];
    }
    
    

    public int getId()
    {
        return id;
    }
    
    public int hashCode()
    {
        return id;
    }
    
    public String toString()
    {
        return ""+id;
    }
    
    public int getIdx()
    {
        return index;
    }
}
