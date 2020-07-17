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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
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

    private boolean drawLegend = true;

    private int scale;
    

    private boolean drawSelection;
    
    private Network network;
    
    private Gradient colors;
    
    public MapViewer(Network network, int viewWidth, int viewHeight)
    {
        this.network = network;
        setPreferredSize(new Dimension(viewWidth, viewHeight));

        colors = null;
        scale = 1;
        drawSelection = false;
        
        setFont(new Font("Arial", Font.BOLD, 14));
        
        setZoom(7);
        recenter();

    }
    
    public MapViewer(Network network, int viewWidth, int viewHeight, Gradient colors)
    {
        this.network = network;
        setPreferredSize(new Dimension(viewWidth, viewHeight));

        setDisplayOSM(false);
        this.colors = colors;
        
        
        scale = 1;
        drawSelection = false;
        
        setFont(new Font("Arial", Font.BOLD, 14));
        
        setZoom(7);
        recenter();

    }
    
    private int t;
    
    public void setTime(int t)
    {
        this.t = t;
        repaint();
    }
    

    public void setScale(int scale)
    {
        this.scale = scale;
    }
    

    
    public int getTime()
    {
        return t;
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
        
        if(colors == null)
        {
            setDisplayPosition(new Point(getWidth()/2, getHeight()/2), new Coordinate(center_y, center_x), zoom);
        }
        else
        {
            setDisplayPosition(new Point(getWidth()/2-300, getHeight()/2), new Coordinate(center_y, center_x), zoom);
        }
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
        
        
        
        if(colors == null)
        {
            g2.setColor(new Color(20, 20, 20));
            g2.setStroke(new BasicStroke(4));
        }
        else
        {
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(4));
        }
        
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        if(colors == null)
        {
            g.setStroke(new BasicStroke(2));
        }
        else
        {
            g.setStroke(new BasicStroke(3));
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
                    if(colors == null)
                    {
                        /*
                        int red = (int)Math.round(255*Math.min(1, network.calcRepRate(i, t)/2));
                        Color color = new Color(255-red, 255, 255-red);
            
                        fillPoly(g2, color, points);
                        */
                        
                        int red = (int)Math.round(255*Math.min(1, Math.max(0, (1.0/i.lambda[network.index_lambda(t)] -0.5)/ (0.5) )));
                        Color color = new Color(255-red, 255-red, 255);
                        fillPoly(g2, color, points);
                        
                        //fillPoly(g2, i.color, points);
                    }
                    else
                    {
                        double cases = i.I[t];
                        double pop = i.getN()/1000;
            

                        fillPoly(g2, colors.getColor(cases/pop), points);
                    }
                    
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
        
        if(colors == null)
        {
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        }
        
        g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        

        if(colors == null)
        {
            Point legendp = this.getMapPosition(46.598787, -91.384587);


            int legendx = (int)(getWidth()*0.75);
            int legendy = getHeight()/2;

            if(legendp != null)
            {
                legendx = (int)legendp.getX();
                legendy = (int)legendp.getY();
            }

            g.setColor(Color.white);

            g.fillRect(legendx, legendy, (int)(getWidth()/7.0+5), getHeight()/3);

            g.setColor(Color.black);

            g.drawRect(legendx, legendy, (int)(getWidth()/7+5), getHeight()/3);



            int fontsize = getHeight()/18;

            g.setFont(new Font("TimesRoman", Font.PLAIN, (int)(fontsize/2)));

            int height = (int)(getHeight()/3.0 - fontsize);

            for(int y = 0; y < height; y++)
            {
                int mixR = network.mincolor.getRed()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getRed() - network.mincolor.getRed()));
                int mixG = network.mincolor.getGreen()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getGreen() - network.mincolor.getGreen()));
                int mixB = network.mincolor.getBlue()  + (int)Math.round(((double)(height - y)/height) * (network.maxcolor.getBlue() - network.mincolor.getBlue()));

                g.setColor(new Color(mixR, mixG, mixB, 160));

                g.drawLine((int)(legendx+fontsize/4), legendy + (int)(fontsize*0.5)+y, (int)(legendx+fontsize/4+fontsize/2), legendy + (int)(fontsize*0.5)+y);
            }

            g.setColor(Color.black);

            g.drawRect((int)(legendx+fontsize/4), legendy + (int)(fontsize*0.5), fontsize/2, height);


            g.drawString(network.maxlabel, (int)(legendx+fontsize/4+fontsize/2+10), legendy + (int)(fontsize*0.5)+fontsize/3);
            g.drawString(network.minlabel, (int)(legendx+fontsize/4+fontsize/2+10), legendy + height+ (int)(fontsize*0.5));
        }
        else
        {
            int legendy = 150;
            int legendx = getWidth()- (int)(getWidth()/2.5)+50;
            
            int legendheight = getHeight()-legendy*2-40-40;
            int legendwidth = 50;
            
            double min = Math.log10(colors.firstKey());
            double max = Math.log10(colors.lastKey());
            
            for(int y = 0; y < legendheight; y++)
            {
                double val = min + (double)y/legendheight * (max - min);
                
                
                Color color = colors.getColor(Math.pow(10, val));
               
                g.setColor(color);
                

                g.drawLine(legendx, legendy + legendheight - y, legendx+legendwidth, legendy + legendheight - y);
            }
            
            
            
            g.setColor(Color.black);
            
            g.drawRect(legendx, legendy, legendwidth, legendheight);
            
            
            int fontsize = getHeight()/18;

            Font font1 = new Font("TimesRoman", Font.PLAIN, (int)(1.5*2*fontsize/3));
            Font font2 = new Font("TimesRoman", Font.PLAIN, (int)(1.5*fontsize/2));
            
            
            
            
            
            
            g.setFont(font1);
            int offset = g.getFontMetrics().stringWidth("10");
            
            
            
            
            
            g.setFont(font1);
            g.drawString("10", legendx+legendwidth+30, legendy + fontsize/8);
            
            g.setFont(font2);
            g.drawString("1", legendx+legendwidth+30 + offset, legendy-fontsize/8);
            
            
            g.setFont(font1);
            g.drawString("10", legendx+legendwidth+30, legendy + fontsize/8+(int)(legendheight/3.0));
            
            g.setFont(font2);
            g.drawString("0", legendx+legendwidth+30 + offset, legendy-fontsize/8+(int)(legendheight/3.0));
            
            
            g.setFont(font1);
            g.drawString("10", legendx+legendwidth+30, legendy + fontsize/8 + (int)(2*legendheight/3.0));
            
            g.setFont(font2);
            g.drawString("-1", legendx+legendwidth+30 + offset, legendy-fontsize/8 + (int)(2*legendheight/3.0));
            
            
            
            g.setFont(font1);
            g.drawString("10", legendx+legendwidth+30, legendy + fontsize/8 +legendheight);
            
            g.setFont(font2);
            g.drawString("-2", legendx+legendwidth+30 + offset, legendy-fontsize/8 +legendheight);
            
            
            g.setFont(font1);
            
            String label = "infections per 1000 residents";
            
            offset = g.getFontMetrics().stringWidth(label);
            
            drawRotate(g, legendx + legendwidth + 190+fontsize, legendy + legendheight/2 + offset/2, -90, label);
            
            
            g.fillRect(legendx+legendwidth, legendy+legendheight -4, 20, 6);
            
            g.fillRect(legendx+legendwidth, (int)(legendy+legendheight/3.0 -2), 20, 6);
            
            g.fillRect(legendx+legendwidth, (int)(legendy+2*legendheight/3.0 -2), 20, 6);
            
            g.fillRect(legendx+legendwidth, legendy -1, 20, 6);
            
            g.setFont(font1);
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy");  
            label = sdf.format(network.getDate(t))+" (day "+(t-network.getStartTime()+1)+")";  
            
            offset = g.getFontMetrics().stringWidth(label);
            
            g.drawString(label, 100, fontsize*2/3+10);
           
        }
    }
    
    public static void drawRotate(Graphics2D g2d, double x, double y, int angle, String text) 
    {    
        g2d.translate((float)x,(float)y);
        g2d.rotate(Math.toRadians(angle));
        g2d.drawString(text,0,0);
        g2d.rotate(-Math.toRadians(angle));
        g2d.translate(-(float)x,-(float)y);
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
        final int time = t;
        
        Thread thread = new Thread()
        {
            public void run()
            {
                int width = getWidth()*2;
                int height = getHeight()*2;
                
                MapViewer map2 = null;
                
                if(colors == null)
                {
                    map2 = new MapViewer(network, width, height);
                }
                else
                {
                    map2 = new MapViewer(network, width, height, colors);
                }
                
                map2.setSize(new Dimension(width, height));


                map2.setZoom(getZoom());
                map2.setCenter(getCenter());
                map2.setZoom(getZoom()+1);
                map2.setScale(2);
                map2.setTime(time);
                
                int zoom = map2.getZoom();
                

                
                
                
                map2.setSize(width, height);
                
                int y = 0;
                
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                
                Graphics g = image.getGraphics();
                
                if(colors == null)
                {
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
                }

                map2.paintComponent(g);



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
                        
                        
                        if(colors == null)
                        {
                            miny = (int)Math.min(miny, p.y-30);
                        }
                        else
                        {
                            miny = (int)Math.min(miny, p.y-120-30);
                        }
                        
                        if(colors == null || !drawLegend)
                        {
                            maxx = (int)Math.max(maxx, p.x+30);
                        }
                        else 
                        {
                            maxx = (int)Math.max(maxx, p.x+30+400);
                        }
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
                
                if(colors == null)
                {
                    g.setColor(Color.black);
                    g.drawRect(0, 0, xdiff-1, ydiff-1);
                }
                
                try
                {
                    ImageIO.write(actual, "png", file);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace(System.err);
                }
                
                if(colors == null)
                {
                    //JOptionPane.showMessageDialog(frame, "Screenshot saved in "+file.getName(), "Screenshot saved", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        //thread.start();
        thread.run();
    }

}
