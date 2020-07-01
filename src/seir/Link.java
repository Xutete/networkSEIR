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
    

    public Link(int T)
    {
        mu = new double[T];
    }
    
 
    
    public void setNormalDemand(Zone source, Zone dest, int t, double normal_demand)
    {
        mu[t] = normal_demand / source.getN();
    }
    
    
    public double getMu(int t)
    {
        return mu[t];
    }
    
    
    public void initialize(int T)
    {

    }
}
