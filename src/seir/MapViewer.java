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
import java.util.HashMap;
import java.util.HashSet;
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
        setPreferredSize(new Dimension(viewWidth, viewHeight));

        
        scale = 1;
        drawSelection = false;
        
        setFont(new Font("Arial", Font.BOLD, 14));
        

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
        
        g.setColor(Color.lightGray);
        g.fillRect(0, 0, getWidth(), getHeight());

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
                int width = getWidth()*8;
                int height = getHeight()*8;
                MapViewer map2 = new MapViewer(network, width, height);
                map2.setSize(new Dimension(width, height));


                map2.setZoom(getZoom());
                map2.setCenter(getCenter());
                map2.setZoom(getZoom()+3);
                map2.setScale(2);
                BufferedImage image = new BufferedImage(map2.getWidth(), map2.getHeight(), BufferedImage.TYPE_INT_ARGB);
                map2.setZoomControlsVisible(false);
                Graphics g = image.getGraphics();

                map2.print(g);
                //g.setColor(Color.black);
                //g.drawRect(0, 0, image.getWidth()-1, image.getHeight()-1);

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
