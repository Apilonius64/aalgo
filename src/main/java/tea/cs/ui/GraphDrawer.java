package tea.cs.ui;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tea.cs.algo.AAlgo;
import tea.cs.algo.Solver;
import tea.cs.data.Graph;
import tea.cs.data.Vec2;
import tea.cs.data.Graph.Edge;
import tea.cs.data.Graph.Node;
import tea.cs.data.Graph.hasPosition;
import tea.cs.data.Vec2.Tile;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class GraphDrawer extends JPanel {
    double zoom = 1.0;
    double zoomSpeed = 2;
    double dragSpeed = 0.2;
    Vec2 offset = new Vec2(0, 0);
    Vec2 lastDrag = new Vec2(0, 0);

    public static boolean drawText = true;

    public interface Style<Obj> {
        public void draw(Graphics2D g2, StyleAnnotator<Obj> annotator, double zoom);
    }

    public record EdgeStyle(Color color, Stroke stroke) implements Style<Graph.Edge> {
        public void draw(Graphics2D g2, StyleAnnotator<Graph.Edge> aEdge, double zoom) {
            Graph.Edge edge = aEdge.o();
            g2.setColor(color);
            g2.setStroke(stroke);
            Line2D shape = new Line2D.Float(
                (float)edge.from().pos().x(),
                (float)edge.from().pos().y(),
                (float)edge.to().pos().x(),
                (float)edge.to().pos().y()
            );

            g2.draw(shape);

            if(zoom > 0.25 && drawText) {
                g2.setColor(Color.DARK_GRAY);
                Vec2 textPos = edge.from().pos().sum(edge.to().pos()).div(new Vec2(2));
                g2.setFont(new Font("TimesRoman", Font.PLAIN, (int)(20 * 1/zoom)));
                g2.drawString(String.format("w = %d", edge.w()), (int)textPos.x(), (int)textPos.y());
                g2.setColor(Color.BLACK);
                g2.drawString(aEdge.note(), (int)textPos.x(), (int)textPos.y() + 20);
            }
        }
    }

    public record NodeStyle(Color color, double rad) implements Style<Graph.Node> {
        public void draw(Graphics2D g2, StyleAnnotator<Graph.Node> aNode, double zoom) {
            Graph.Node node = aNode.o();
            g2.setColor(color);
            Ellipse2D shape = new Ellipse2D.Double(node.pos().x()-rad/2, node.pos().y()-rad/2, rad, rad);
            g2.fill(shape);
            if(zoom > 0.25) {
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(node.getUnsafeReadableIdentifier(), (int)node.pos().x() + (int)rad/2, (int)node.pos().y());
                g2.setColor(Color.BLACK);
                g2.drawString(aNode.note(), (int)node.pos().x() + (int)rad/2, (int)node.pos().y() + 20);
            }
        }
    }

    public record StyleAnnotator<Obj>(Obj o, Style<Obj> style, String note) {
        public static <O> List<StyleAnnotator<O>> of(List<O> objs, Style<O> style) {
            List<StyleAnnotator<O>> result = new ArrayList<StyleAnnotator<O>>();
            for (O i : objs) {
                result.add(new StyleAnnotator<O>(i, style, ""));
            }

            return result;
        }

        public void draw(Graphics2D g2, double zoom) {
            style.draw(g2, this, zoom);
        }

        public static <T extends hasPosition> void drawTile(Graphics2D g2, Vec2.Tile t, List<StyleAnnotator<T>> items, double zoom) {
            for (StyleAnnotator<T> i : items) {
                if(!i.o.inTile(t)) {
                    continue;
                }

                i.draw(g2, zoom);
            }
        }

        public static <T> void annotate(List<StyleAnnotator<T>> items, List<T> annotate, Style<T> style, String note) {
            for (int i = 0; i != items.size(); ++i) {
                if(annotate.contains(items.get(i).o)) {
                    items.set(i, new StyleAnnotator<T>(items.get(i).o, style, note));
                }
            }
        }

        public static <T> void annotate(List<StyleAnnotator<T>> items, List<T> annotate, Style<T> style) {
            for (int i = 0; i != items.size(); ++i) {
                if(annotate.contains(items.get(i).o)) {
                    items.set(i, new StyleAnnotator<T>(items.get(i).o, style, items.get(i).note()));
                }
            }
        }
    }

    // Groups styles
    public record StyleSet<S extends Style<?>>(S normal, S focus) {}

    public static StyleSet<NodeStyle> nodeStyle = new StyleSet<NodeStyle>(
        new NodeStyle(Color.BLUE, 40),
        new NodeStyle(Color.GREEN, 80)
    );

    public static StyleSet<EdgeStyle> edgeStyle = new StyleSet<EdgeStyle>(
        new EdgeStyle(Color.RED, new BasicStroke(1)),
        new EdgeStyle(Color.GREEN, new BasicStroke(5))
    );

    public final Graph g;
    protected List<StyleAnnotator<Graph.Node>> nodes;
    protected List<StyleAnnotator<Graph.Edge>> edges;
    protected List<Graph.Node> selectedNodes = new ArrayList<Graph.Node>();

    public void annotateNodes(List<Graph.Node> annotate, Style<Graph.Node> style) {
        StyleAnnotator.annotate(nodes, annotate, style);
    }

    public void annotateNodes(List<Graph.Node> annotate, Style<Graph.Node> style, String note) {
        StyleAnnotator.annotate(nodes, annotate, style, note);
    }

    public void annotateEdges(List<Graph.Edge> annotate, Style<Graph.Edge> style) {
        StyleAnnotator.annotate(edges, annotate, style);
    }

    public void annotateEdges(List<Graph.Edge> annotate, Style<Graph.Edge> style, String note) {
        StyleAnnotator.annotate(edges, annotate, style, note);
    }

    public void annotate(List<Graph.Node> annotate, Style<Graph.Node> nodeS, Style<Graph.Edge> edgeS) {
        annotateNodes(annotate, nodeS);
        annotateEdges(g.getEdgesStrict(annotate), edgeS);
    }

    private void registerListeners() {
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                repaint();
            }
        });

        this.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Zoom to curser
                Vec2 pos = Vec2.of(e);
                Vec2 before = offset.diff(pos).div(new Vec2(zoom));
                double clicks = e.getPreciseWheelRotation();
                double fac = Math.pow(zoomSpeed, -clicks);
                zoom *= fac;

                Vec2 after = before.mul(new Vec2(zoom)).sum(offset);
                offset = pos.diff(after).diff(offset);
                
                repaint();
            }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                lastDrag = Vec2.of(e);
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if(!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                Vec2 mousePos = Vec2.of(e);
                Vec2 velocity = lastDrag.diff(mousePos);
                offset = offset.sum(velocity);
                lastDrag = mousePos;
                repaint();
            }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!SwingUtilities.isRightMouseButton(e)) {
                    return;
                }

                System.out.println("User tries to select something ...");

                Vec2 pos = offset.diff(Vec2.of(e)).div(new Vec2(zoom));

                Optional<StyleAnnotator<Graph.Node>> n = nodes.stream()
                    .filter(a -> new Tile(
                            new Vec2(
                                nodeStyle.normal().rad()*2
                            ),
                            new Vec2(
                                nodeStyle.normal().rad()
                            ).diff(a.o.pos())
                        ).contains(pos)
                    ).findFirst();

                if(!n.isPresent()) {
                    annotateNodes(selectedNodes, nodeStyle.normal(), "");
                    selectedNodes = new ArrayList<Graph.Node>();
                    repaint();
                    return;
                }

                StyleAnnotator<Graph.Node> na = n.get();
                selectedNodes.add(na.o);
                annotateNodes(Arrays.asList(na.o), nodeStyle.focus(), Integer.toString(selectedNodes.size()-1));
                repaint();
                System.out.printf("Selected node %s under cursor%n", na);
            }
        });
    }

    public GraphDrawer(Graph g) {
        this(g, StyleAnnotator.of(g.nodes(), nodeStyle.normal), StyleAnnotator.of(g.edges(), edgeStyle.normal));
    }

    protected Optional<Solver.Backend<?>> getSolveBackend(List<Graph.Node> sNodes) {
        if(sNodes.size() != 2) {
            JOptionPane.showMessageDialog((JFrame)SwingUtilities.getWindowAncestor(this), "Please select two nodes before using solving algorithms (1=Start, 0=Deest).");
            return Optional.empty();
        }

        return Optional.of(new AAlgo(g, sNodes.get(1), sNodes.get(0)));
    }

    public void save()
    {
        // Thanks: https://stackoverflow.com/questions/8202253/saving-a-java-2d-graphics-image-as-png-file
        BufferedImage bImg = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = bImg.createGraphics();
        this.paintAll(cg);
        try {
                ImageIO.write(bImg, "png", new File("img/" + Instant.now().toString() + ".png"));
        } catch (IOException e) {
                e.printStackTrace();
        }
    }

    public GraphDrawer(Graph g, List<StyleAnnotator<Graph.Node>> nodes, List<StyleAnnotator<Graph.Edge>> edges) {
        this.g = g;
        registerListeners();
        this.nodes = nodes;
        this.edges = edges;

        this.setBackground(Color.WHITE);

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("ctrl V"), "visualize");
        inputMap.put(KeyStroke.getKeyStroke("ctrl A"), "visualize_auto");
        inputMap.put(KeyStroke.getKeyStroke("ctrl C"), "clear_styles");
        inputMap.put(KeyStroke.getKeyStroke("ctrl T"), "toggle_text");
        inputMap.put(KeyStroke.getKeyStroke("ctrl X"), "screenshot");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "next_slide");
        GraphDrawer gd = this;

        actionMap.put("screenshot", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        actionMap.put("visualize", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Solver.visualize(getSolveBackend(selectedNodes).get(), gd);
            }
        });

        actionMap.put("visualize_auto", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Solver.visualize_timer(getSolveBackend(selectedNodes).get(), gd, 5);
            }
        });

        actionMap.put("toggle_text", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                drawText = !drawText;
                repaint();
            }
        });

        actionMap.put("clear_styles", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                gd.annotateNodes(g.nodes(), nodeStyle.normal(), "");
                gd.annotateEdges(g.edges(), edgeStyle.normal());
                selectedNodes = new ArrayList<Graph.Node>();
                gd.repaint();
            }
        });

        offset = nodes.get(0).o.pos().diff(offset);
    }
    
    protected void drawTile(Graphics2D g2, Vec2.Tile tile) {
        StyleAnnotator.drawTile(g2, tile, nodes, zoom);
        StyleAnnotator.drawTile(g2, tile, edges, zoom);
    }

    protected void drawViewport(Graphics2D g2) {
        Vec2.Tile bounds = Vec2.Tile.of(g2.getClipBounds());
        System.out.printf("Drawing viewport %s%n", bounds);
        drawTile(g2, bounds);
    }

    @Override
    protected void paintComponent(Graphics g) {
        System.out.println(getSize());
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.translate(offset.x(), offset.y());
        g2.scale(zoom, zoom);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
        drawViewport(g2);
    }

    public static GraphDrawer ofOSMJson(String s) {
        // Thanks: https://stackoverflow.com/a/18998203
        JSONObject obj = new JSONObject(s);
        JSONArray elems = obj.getJSONArray("elements");

        List<Graph.Node> nodes = new ArrayList<>();
        List<Integer> nodeIDs = new ArrayList<>();
        List<StyleAnnotator<Graph.Node>> nodesA = new ArrayList<StyleAnnotator<Graph.Node>>();

        for (int i = 0; i != elems.length(); ++i) {
            JSONObject item = elems.getJSONObject(i);
            if(item.getString("type").equals("node")) {
                Double lat = item.getDouble("lat");
                Double lon = item.getDouble("lon");

                Graph.Node node = new Node(
                    Vec2.ofCoord(lat, lon) 
                );
                nodes.add(node);

                nodeIDs.add(item.getInt("id"));
                nodesA.add(new StyleAnnotator<Graph.Node>(node, nodeStyle.normal(), ""));
            }
        }

        List<Graph.Edge> edges = new ArrayList<Graph.Edge>();
        List<StyleAnnotator<Graph.Edge>> edgesA = new ArrayList<StyleAnnotator<Graph.Edge>>();
        for (int i = 0; i != elems.length(); ++i) {
            JSONObject item = elems.getJSONObject(i);
            if(item.getString("type").equals("way")) {
                JSONArray wayNodes = item.getJSONArray("nodes");

                for(int j=0; j != (wayNodes.length()-1); ++j) {
                    Node a = nodes.get(nodeIDs.indexOf(wayNodes.getInt(j)));
                    Node b = nodes.get(nodeIDs.indexOf(wayNodes.getInt(j+1)));
                    Integer dist = a.pos().dist(b.pos());

                    // No one-way roads for now
                    Graph.Edge edge = new Edge(a, b, dist);
                    edges.add(edge);
                    edges.add(new Edge(b, a, dist));

                    String name = "";
                    try {
                        name = item.getJSONObject("tags").getString("name");
                    }
                    catch (JSONException e) {}
                    
                    edgesA.add(new StyleAnnotator<Graph.Edge>(edge, edgeStyle.normal(), name));
                }
            }
        }

        Graph graph = new Graph(nodes, edges);
        return new GraphDrawer(graph, nodesA, edgesA);
    }
}
