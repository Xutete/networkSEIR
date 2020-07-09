/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ptolemy.plot.Plot;
import static seir.GraphicUtils.*;

/**
 *
 * @author micha
 */
public class PlotErrors extends JFrame
{
    private Network network;
    
    private Plot plot;
    
    private int zone_idx;
    
    private JButton next, prev, screenshot;
    
    private JLabel label;
    private JPanel panel;
    
    public PlotErrors(Network network)
    {
        this.network = network;
        
        label = new JLabel("County 00000");
        
        plot = new Plot();
        
        prev = new JButton("Prev");
        next = new JButton("Next");
        
        screenshot = new JButton("Screenshot");
        
        plot.setPreferredSize(new Dimension(800, 400));
        
        plot.addLegend(0, "Predicted");
        plot.addLegend(1, "Reported*lambda");
        plot.addLegend(2, "Reported");
        
        zone_idx = 0;
        showZone(network.getZones()[zone_idx]);
        
        prev.setEnabled(false);
        next.setEnabled(network.getZones().length > 1);
        
        prev.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                zone_idx--;
                showZone();
            }
        });
        
        next.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                zone_idx++;
                showZone();
            }
        });
        
        screenshot.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent e)
           {
               try
               {
                    File file = new File(network.getDirectory()+"/graphs");
                    file.mkdirs();
                    screenshot(new File(network.getDirectory()+"/graphs/county_"+network.getZones()[zone_idx].getId()+".png"));
               }
               catch(IOException ex)
               {
                   ex.printStackTrace(System.err);
               }
           }
        });
        
        
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        
        constrain(panel, label, 0, 0, 1, 1);
        constrain(panel, plot, 0, 1, 1, 1);
        
        constrain(p, panel, 0, 0, 3, 1);
        constrain(p, prev, 0, 1, 1, 1);
        constrain(p, next, 1, 1, 1, 1);
        constrain(p, screenshot, 2, 1, 1, 1);
        
        
        
        
        add(p);
        pack();
        
        setVisible(true);
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
    }
    
    public void screenshot(File file) throws IOException
    {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = image.getGraphics();
        panel.paint(g);
        ImageIO.write(image, "png", file);
    }
    
    public void showZone()
    {
        prev.setEnabled(zone_idx > 0);
        next.setEnabled(network.getZones().length > zone_idx+1);

        showZone(network.getZones()[zone_idx]);
    }
    
    public void showZone(Zone i)
    {
        label.setText("County "+i.getId());
        plot.clear(0);
        plot.clear(1);
        plot.clear(2);
        
        
        
        for(int t = network.getStartTime(); t < network.getT(); t++)
        {
            plot.addPoint(0, t, i.I[t], true);
            plot.addPoint(1, t, i.reportedI[t] * i.lambda[network.index_lambda(t)], true);
            plot.addPoint(2, t, i.reportedI[t], true);
        }
        
        plot.fillPlot();
    }
}
