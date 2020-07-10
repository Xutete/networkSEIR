/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seir;


import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.Timer;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import static org.openstreetmap.gui.jmapviewer.JMapViewer.MIN_ZOOM;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MemoryTileCache;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileController;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

/**
 *
 * @author ml26893
 */
public class MapViewer extends JMapViewer
{

    
    private int time;
    
    private int scale;
    

    private boolean drawSelection;
    
    private Network network;
    
    public MapViewer(Network network, int viewWidth, int viewHeight)
    {
        this.network = network;
        setPreferredSize(new Dimension(viewWidth, viewHeight));

        
        scale = 1;
        drawSelection = false;
        
        setFont(new Font("Arial", Font.BOLD, 14));
        
        recenter();

    }
    

    public void setScale(int scale)
    {
        this.scale = scale;
    }
    
    public void setTime(int t)
    {
        this.time = t;
        repaint();
    }
    
    public int getTime()
    {
        return time;
    }
    
    public void center(Zone z)
    {
        setDisplayPosition(new Point(getWidth()/2, getHeight()/2), z.getBoundary().get(0), getZoom());
    }
   
    

    
    public void recenter()
    {
        recenter(getZoom());
    }
    
    public void recenter(int zoom)
    {
        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxY = Integer.MIN_VALUE;
        
        for(Zone n : network.getZones())
        {

            Location c = n.getBoundary().get(0);
            
            if(c.getX() < minX)
            {
                minX = c.getX();
            }
            
            if(c.getX() > maxX)
            {
                maxX = c.getX();
            }
            
            if(c.getY() < minY)
            {
                minY = c.getY();
            }
            
            if(c.getY() > maxY)
            {
                maxY = c.getY();
            }
            
        }
        
        double xdiff = maxX - minX;
        double ydiff = maxY - minY;
        
        minX -= xdiff*0.2;
        maxX += xdiff*0.2;
        
        minY -= ydiff*0.2;
        maxY += ydiff*0.2;
        
        double center_x = (maxX + minX)/2;
        double center_y = (maxY + minY)/2;
        
        setDisplayPosition(new Point(getWidth()/2, getHeight()/2), new Coordinate(center_y, center_x), zoom);
        repaint();
    }
    
    public void setZoomControlsVisible(boolean visible) {
        super.setZoomControlsVisible(visible);
    }
    
    protected void paintComponent(Graphics window) 
    {
        Graphics2D g = (Graphics2D)window;
        
        super.paintComponent(g);
        
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        
        g2.setStroke(new BasicStroke(3));
        
        g2.setColor(new Color(100, 100, 100));
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        
        
        
        
        
        for(Zone i : network.getZones())
        {
            List<Location> coords = i.getBoundary();
            
            List<Point> points = new ArrayList<>();
            
            int start = 0;
            
            for(int j = 0; j < coords.size(); j++)
            {
                Point p = getMapPosition(coords.get(j), false);
                
                points.add(p);
                
                if(j > start && coords.get(j).equals(coords.get(start)))
                {
                    fillPoly(g2, i.color, points);
                    
                    points.clear();
                    start = j+1;
                }
            }
            
            if(points.size() > 0)
            {
                fillPoly(g2, i.color, points);
            }
        }
        

        for(Zone i : network.getZones())
        {
            List<Location> coords = i.getBoundary();
            
            List<Point> points = new ArrayList<>();
            
            int start = 0;
            
            for(int j = 0; j < coords.size(); j++)
            {
                Point p = getMapPosition(coords.get(j), false);
                
                points.add(p);
                
                if(j > start && coords.get(j).equals(coords.get(start)))
                {
                    drawPoly(g2, Color.black, points);
                    
                    points.clear();
                    start = j+1;
                    
                }
            }
            
            if(points.size() > 0)
            {
                drawPoly(g2, Color.black, points);
            }
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        
        g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        

        Point legendp = this.getMapPosition(46.598787, -91.384587);
        
        
        int legendx = (int)(getWidth()*0.75);
        int legendy = getHeight()/2;
        
        if(legendp != null)
        {
            legendx = (int)legendp.getX();
            legendy = (int)legendp.getY();
        }
        
        g.setColor(Color.white);
        
        g.fillRect(legendx, legendy, (int)(getWidth()/7.0), getHeight()/3);
        
        g.setColor(Color.black);
        
        g.drawRect(legendx, legendy, (int)(getWidth()/7.0), getHeight()/3);
        
        
        
        int fontsize = getHeight()/18;
        
        g.setFont(new Font("TimesRoman", Font.PLAIN, (int)(fontsize/2)));
        
        int height = getHeight()/3 - (int)(fontsize*1.5);
        
        for(int y = 0; y < height; y++)
        {
            int mixR = network.mincolor.getRed()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getRed() - network.mincolor.getRed()));
            int mixG = network.mincolor.getGreen()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getGreen() - network.mincolor.getGreen()));
            int mixB = network.mincolor.getBlue()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getBlue() - network.mincolor.getBlue()));
            
            g.setColor(new Color(mixR, mixG, mixB, 160));
            
            g.drawLine((int)(legendx+fontsize/4), legendy + (int)(fontsize)+y, (int)(legendx+fontsize/4+fontsize/2), legendy + (int)(fontsize)+y);
        }
        
        g.setColor(Color.black);
        
        g.drawRect((int)(legendx+fontsize/4), legendy + (int)(fontsize), fontsize/2, height);

        g.drawString("Legend", (int)(legendx)+fontsize/4, legendy+fontsize/6+fontsize/2);
        
        g.drawString(network.maxlabel, (int)(legendx+fontsize/4+fontsize/2+10), legendy + (int)(fontsize)+fontsize/3);
        g.drawString(network.minlabel, (int)(legendx+fontsize/4+fontsize/2+10), legendy + height+ (int)(fontsize));
    }
    
    public void drawPoly(Graphics g, Color color, List<Point> points)
    {
        int[] xpoints = new int[points.size()];
        int[] ypoints = new int[points.size()];
        
        for(int i = 0; i < points.size(); i++)
        {
            Point p = points.get(i);
            xpoints[i] = (int)Math.round(p.getX());
            ypoints[i] = (int)Math.round(p.getY());
        }
        
        Polygon poly = new Polygon(xpoints, ypoints, xpoints.length);
           
        g.setColor(color);
        g.drawPolygon(poly);
    }
    
    public void fillPoly(Graphics g, Color color, List<Point> points)
    {
        
        int[] xpoints = new int[points.size()];
        int[] ypoints = new int[points.size()];
        
        for(int i = 0; i < points.size(); i++)
        {
            Point p = points.get(i);
            xpoints[i] = (int)Math.round(p.getX());
            ypoints[i] = (int)Math.round(p.getY());
        }
        
        Polygon poly = new Polygon(xpoints, ypoints, xpoints.length);
           
        g.setColor(color);
        g.fillPolygon(poly);
    }
    
    public void paintText(Graphics g, String name, Point position, int radius) {

        if (name != null && g != null && position != null) {
            g.setColor(Color.DARK_GRAY);
            g.setFont(getFont());
            g.drawString(name, position.x+radius+2, position.y+radius);
        }
    }
    
    
    public void saveScreenshot(File file) throws Exception
    {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        setZoomControlsVisible(false);
        Graphics g = image.getGraphics();
        paint(g);
        
        setZoomControlsVisible(true);
        
        
        int minx = image.getWidth();
        int miny = image.getHeight();
        int maxx = 0;
        int maxy = 0;
        
        for(Zone n: network.getZones())
        {
            for(Location l : n.getBoundary())
            {
                Point p = getMapPosition(l, false);

                minx = (int)Math.min(minx, p.x-10);
                miny = (int)Math.min(miny, p.y-10);
                maxx = (int)Math.max(maxx, p.x+10);
                maxy = (int)Math.max(maxy, p.y+10);
            }

        }

        
        maxx = (int)Math.min(maxx, image.getWidth());
        maxy = (int)Math.min(maxy, image.getHeight());
        minx = (int)Math.max(minx, 0);
        miny = (int)Math.max(miny, 0);
        
        
        int xdiff = maxx - minx;
        int ydiff = maxy - miny;

        
        BufferedImage actual = new BufferedImage(xdiff, ydiff, BufferedImage.TYPE_INT_ARGB);
        g = actual.getGraphics();
        g.drawImage(image, -minx, -miny, image.getWidth(), image.getHeight(), null);
        g.setColor(Color.black);
        g.drawRect(0, 0, xdiff-1, ydiff-1);
        ImageIO.write(actual, "png", file);
    }
    
    public void saveHighResScreenshot(final File file) throws Exception
    {
        final JComponent frame = this;
        
        
        Thread t = new Thread()
        {
            public void run()
            {
                int width = getWidth()*2;
                int height = getHeight()*2;
                MapViewer map2 = new MapViewer(network, width, height);
                map2.setSize(new Dimension(width, height));


                map2.setZoom(getZoom());
                map2.setCenter(getCenter());
                map2.setZoom(getZoom()+1);
                map2.setScale(2);
                
                int zoom = map2.getZoom();
                

                
                
                
                map2.setSize(width, height);
                
                int y = 0;
                
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                
                Graphics g = image.getGraphics();
                
                for(int i = 0; i < 10; i++)
                {
                    map2.paintComponent(g);
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch(Exception ex)
                    {
                        ex.printStackTrace(System.err);
                    }
                
                    
                }
                map2.paintComponent(g);

                g.setColor(Color.black);
                g.drawRect(0, 0, image.getWidth()-1, image.getHeight()-1);

                int minx = image.getWidth();
                int miny = image.getHeight();
                int maxx = 0;
                int maxy = 0;

                for(Zone n: network.getZones())
                {
                    for(Location l : n.getBoundary())
                    {
                        Point p = map2.getMapPosition(l, false);

                        minx = (int)Math.min(minx, p.x-30);
                        miny = (int)Math.min(miny, p.y-30);
                        maxx = (int)Math.max(maxx, p.x+30);
                        maxy = (int)Math.max(maxy, p.y+30);
                    }

                }


                maxx = (int)Math.min(maxx, image.getWidth());
                maxy = (int)Math.min(maxy, image.getHeight());
                minx = (int)Math.max(minx, 0);
                miny = (int)Math.max(miny, 0);
        


                int xdiff = maxx - minx;
                int ydiff = maxy - miny;


                BufferedImage actual = new BufferedImage(xdiff, ydiff, BufferedImage.TYPE_INT_ARGB);
                g = actual.getGraphics();
                g.drawImage(image, -minx, -miny, image.getWidth(), image.getHeight(), null);
                g.setColor(Color.black);
                g.drawRect(0, 0, xdiff-1, ydiff-1);
                try
                {
                    ImageIO.write(actual, "png", file);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace(System.err);
                }
                
                JOptionPane.showMessageDialog(frame, "Screenshot saved in "+file.getName(), "Screenshot saved", JOptionPane.INFORMATION_MESSAGE);
            
            }
        };
        t.start();
    }

}
