/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.Color;
import java.util.TreeMap;

/**
 *
 * @author micha
 */
public class Gradient extends TreeMap<Double, Color>
{
    public Gradient()
    {
        
    }
    
    public Color getColor(double data)
    {
        if(data == 0)
        {
            return Color.lightGray;
        }
        
        
        Color minC = null;
        Color maxC = null;
        double min = 0;
        double max = 0;
        
        if(data > lastKey())
        {
            return get(lastKey());
        }
        
        for(double k : keySet())
        {
            if(data > k)
            {
                min = k;
                minC = get(k);
            }
            if(data < k)
            {
                maxC = get(k);
                max = k;
                break;
            }
        }
        
        
        
        
        double val = Math.log10(data);
 
        double scale = (val - min) / (max - min);
        
        int red = (int)Math.round(val * (maxC.getRed() - minC.getRed()) + minC.getRed());
        
        return Color.red;
    }
}
