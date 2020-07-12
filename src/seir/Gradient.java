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
        
        if(containsKey(data))
        {
            return get(data);
        }
        
        
        Color minC = null;
        Color maxC = null;
        double min = 0;
        double max = 0;
        
        if(data >= lastKey())
        {
            return get(lastKey());
        }
        
        if(data <= firstKey())
        {
            return get(firstKey());
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
        
        max = Math.log10(max);
        min = Math.log10(min);
        
        if(minC == null || maxC == null)
        {
            System.out.println(data+" "+minC+" "+maxC);
        }
        
        double val = Math.log10(data);
 
        double scale = (val - min) / (max - min);

        
        int red = (int)Math.round(scale * (maxC.getRed() - minC.getRed()) + minC.getRed());
        int green = (int)Math.round(scale * (maxC.getGreen() - minC.getGreen()) + minC.getGreen());
        int blue = (int)Math.round(scale * (maxC.getBlue() - minC.getBlue()) + minC.getBlue());

        
        return new Color(red, green, blue);
    }
}
