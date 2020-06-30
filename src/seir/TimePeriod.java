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
public class TimePeriod {
    private int start, end;
    
    public TimePeriod(int start, int end)
    {
        this.start = start;
        this.end = end;
    }
    
    
    public String toString()
    {
        return "["+start+","+end+")";
    }
    public boolean contains(int t)
    {
        return start <= t && t < end;
    }
    
    public int getStart()
    {
        return start;
    }
    
    public int getEnd()
    {
        return end;
    }
}
