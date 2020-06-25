/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 *
 * @author micha
 */
public class Zone 
{
    private int index;
    private static int next_index = 0;
    
    private VarArray S, E, I, R;
    
    private double[] reportedI;
    
    private double N;
    private int id;
    
    
    public Zone(int id, double N)
    {
        index = next_index++;
        
        this.id = id;
        this.N = N;
        
        S = new VarArray();
        E = new VarArray();
        I = new VarArray();
        R = new VarArray();
    }
    
    public void setReportedI(double[] val)
    {
        reportedI = val;
    }
    
    public double getStartingPopulation()
    {
        return N;
    }
    
    public double getPopulation(int t)
    {
        return S.getValue(t)+E.getValue(t)+I.getValue(t)+R.getValue(t);
    }
    
    public void initialize(IloCplex cplex, int T) throws IloException
    {
        S.setSize(T);
        E.setSize(T);
        I.setSize(T);
        R.setSize(T);
        
        S.initialize(cplex);
        E.initialize(cplex);
        I.initialize(cplex);
        R.initialize(cplex);
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
