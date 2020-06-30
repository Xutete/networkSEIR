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
    private double mu;
    

    public Link()
    {

    }
    
    public Link(Zone source, Zone dest, double normal_demand)
    {
        this();
        setNormalDemand(source, dest, normal_demand);
    }
    
    public void setNormalDemand(Zone source, Zone dest, double normal_demand)
    {
        mu = normal_demand * source.getN();
    }
    
    public double getMu(int t)
    {
        return mu;
    }
    
    
    public void initialize(int T)
    {

    }
}
