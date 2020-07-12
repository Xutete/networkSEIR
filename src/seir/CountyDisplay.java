/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import static seir.GraphicUtils.*;
/**
 *
 * @author micha
 */
public class CountyDisplay extends JFrame
{
    private MapViewer map;
    private int image_count = 0;
    private Network network;
    
    public CountyDisplay(Network network)
    {
        this.network = network;
        map = new MapViewer(network, 900, 900);
        
        initialize();
    }
    
    public MapViewer getMap()
    {
        return map;
    }
    
    public CountyDisplay(Network network, Gradient gradient)
    {
        map = new MapViewer(network, 1200, 900, gradient);
        this.network = network;
        initialize();
    }
    
    private void initialize()
    {
        JButton screenshot2 = new JButton("Screenshot");
        
        JButton screenshot = new JButton("Hi-res screenshot");
        
        screenshot.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    File file = new File(network.getDirectory()+"/images");
                    file.mkdirs();

                    map.saveHighResScreenshot(new File(network.getDirectory()+"/images/screenshot_"+image_count+".png"));
                    image_count++;
                }
                catch(Exception ex)
                {
                    ex.printStackTrace(System.err);
                }
            }
        });
        
        screenshot2.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    File file = new File(network.getDirectory()+"/images");
                    file.mkdirs();

                    map.saveScreenshot(new File(network.getDirectory()+"/images/screenshot_"+image_count+".png"));
                    image_count++;
                }
                catch(Exception ex)
                {
                    ex.printStackTrace(System.err);
                }
            }
        });
        
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        
        constrain(p, map, 0, 0, 1, 2);
        constrain(p, screenshot2, 1, 1, 1, 1);
        constrain(p, screenshot, 1, 0, 1, 1);
        
        add(p);
        pack();
        setResizable(false);
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        
        setVisible(true);
    }
}
