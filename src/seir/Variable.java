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
public class Variable 
{
    private IloNumVar var;
    private double value;
    private double min, max;
    
    public Variable()
    {
        this(0, Double.MAX_VALUE);
    }
    
    public Variable(double min, double max)
    {
        this.min = min;
        this.max = max;
    }
    
    public double getMin()
    {
        return min;
    }
    
    public double getMax()
    {
        return max;
    }
    
    public void setMin(double min)
    {
        this.min = min;
    }
    
    public void setMax(double max)
    {
        this.max = max;
    }
    
    public void setValue(IloCplex cplex) throws IloException
    {
        value = cplex.getValue(var);
    }
    
    public IloNumVar getVariable()
    {
        return var;
    }
    
    public void initialize(IloCplex cplex) throws IloException
    {
        var = cplex.numVar(min, max);
    }
    
    public double getValue()
    {
        return value;
    }
    
}
