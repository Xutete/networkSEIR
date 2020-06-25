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
public class VarArray 
{
    private IloNumVar[] var;
    private double[] value;
    private double min, max;
    
    public VarArray()
    {
        this(0);
    }
    
    public VarArray(int size)
    {
        this(size, 0, Double.MAX_VALUE);
    }
    
    public VarArray(int size, double min, double max)
    {
        this.min = min;
        this.max = max;
        
        value = new double[size];
        var = new IloNumVar[size];
    }
    
    public int getSize()
    {
        return value.length;
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
        for(int t = 0; t < value.length; t++)
        {
            value[t] = cplex.getValue(var[t]);
        }
    }
    
    public IloNumVar getVariable(int t)
    {
        return var[t];
    }
    
    public void setSize(int size)
    {
        value = new double[size];
        var = new IloNumVar[size];
    }
    
    public void initialize(IloCplex cplex) throws IloException
    {
        for(int t = 0; t < var.length; t++)
        {
            var[t] = cplex.numVar(min, max);
        }
    }
    
    public double getValue(int t)
    {
        return value[t];
    }
    
}
