/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author micha
 */
public class Link 
{
    private double mu;
    
    private VarArray demand;
    
    public Link(Zone source, Zone dest, double normal_demand)
    {
        demand = new VarArray();
        mu = normal_demand * source.getStartingPopulation();
    }
    
    public double getMu()
    {
        return mu;
    }
    
    public VarArray getDemand()
    {
        return demand;
    }
    
    public void initialize(IloCplex cplex, int T) throws IloException
    {
        demand.setSize(T);
        demand.initialize(cplex);
    }
}
