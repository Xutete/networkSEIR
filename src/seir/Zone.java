/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import ilog.concert.IloNumVar;

/**
 *
 * @author micha
 */
public class Zone 
{
    private IloNumVar[] S, E, I, R;
    
    private int N;
    private int id;
    
    
    public Zone(int id, int N)
    {
        this.id = id;
        this.N = N;
        
        S = new IloNumVar[Network.T];
        E = new IloNumVar[Network.T];
        I = new IloNumVar[Network.T];
        R = new IloNumVar[Network.T];
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
}
