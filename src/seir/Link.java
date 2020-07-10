/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

/**
 *
 * @author micha
 */
public class Link 
{
    private double[] mu;
    private double[] trips;
    

    public Link(int T)
    {
        mu = new double[T];
        trips = new double[T];
    }
    
 
    
    public void setNormalDemand(Zone source, Zone dest, int t, double normal_demand)
    {
        mu[t] = normal_demand / source.getN();
        trips[t] = normal_demand;
    }
    
    
    public double getMu(int t)
    {
        return mu[t];
    }

    
    
    public void initialize(int T)
    {

    }
}
