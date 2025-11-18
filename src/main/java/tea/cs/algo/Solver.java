package tea.cs.algo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Timer;

import tea.cs.data.Graph;
import tea.cs.ui.GraphDrawer;
import tea.cs.ui.GraphDrawer.EdgeStyle;
import tea.cs.ui.GraphDrawer.NodeStyle;

public class Solver {
    public interface AbstractQueryItem {
        public Graph.Node node();
    }

    public interface Backend <T extends AbstractQueryItem> {
        public List<T> initStack();
        public Optional<Optional<List<Graph.Node>>> doStep(List<T> items, List<T> query);
        public Graph.Node getStart();
        public Graph.Node getDeest();
    }

    public static <T extends AbstractQueryItem> Optional<List<Graph.Node>> solve(Backend<T> backend) {
        List<T> items = new ArrayList<>(backend.initStack());
        List<T> query = new ArrayList<>(items);

        while(true) {
            Optional<Optional<List<Graph.Node>>> state = backend.doStep(items, query);

            if(state.isPresent()) {
                return state.get();
            }
        }
    }

    public record VisualizationStyle(
        NodeStyle queued,
        NodeStyle completed
    ) {}

    public static VisualizationStyle vstyle = new VisualizationStyle(
        new NodeStyle(Color.YELLOW, GraphDrawer.nodeStyle.normal().rad()),
        new NodeStyle(Color.GREEN, GraphDrawer.nodeStyle.normal().rad())
    );

    public static EdgeStyle routeStyle = new EdgeStyle(
        Color.MAGENTA, new BasicStroke(30)
    );

    public static <T extends AbstractQueryItem> void visualize(Backend<T> backend, GraphDrawer gd) {
        List<T> items = new ArrayList<>(backend.initStack());
        List<T> query = new ArrayList<>(items);

        gd.annotateNodes(Arrays.asList(backend.getStart()), GraphDrawer.nodeStyle.normal(), "Start");
        gd.annotateNodes(Arrays.asList(backend.getDeest()), GraphDrawer.nodeStyle.normal(), "Ziel");

        ActionMap actionMap = gd.getActionMap();
        
        AbstractAction steps = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Optional<Optional<List<Graph.Node>>> state = backend.doStep(items, query);
                List<T> witems = new ArrayList<>(items);
                witems.removeAll(query);

                System.out.println("Annotating ...");
                gd.annotateNodes(query.stream().map(v -> v.node()).toList(), vstyle.queued());
                gd.annotateNodes(witems.stream().map(v -> v.node()).toList(), vstyle.completed());
                gd.repaint();

                if(state.isPresent()) {
                    gd.annotateEdges(gd.g.getEdgesStrict(state.get().get()), routeStyle);
                    System.out.println(state.get().get());
                    actionMap.remove("next_slide");
                }
            }
        };

        actionMap.put("next_slide", steps);
    }

    public static <T extends AbstractQueryItem> void visualize_timer(Backend<T> backend, GraphDrawer gd, int waitMs) {
        List<T> items = new ArrayList<>(backend.initStack());
        List<T> query = new ArrayList<>(items);

        gd.annotateNodes(Arrays.asList(backend.getStart()), GraphDrawer.nodeStyle.normal(), "Start");
        gd.annotateNodes(Arrays.asList(backend.getDeest()), GraphDrawer.nodeStyle.normal(), "Ziel");
        
        Timer timer = new Timer(waitMs, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Optional<Optional<List<Graph.Node>>> state = backend.doStep(items, query);
                List<T> witems = new ArrayList<>(items);
                witems.removeAll(query);

                System.out.println("Annotating ...");
                gd.annotateNodes(query.stream().map(v -> v.node()).toList(), vstyle.queued());
                gd.annotateNodes(witems.stream().map(v -> v.node()).toList(), vstyle.completed());
                gd.repaint();

                if(state.isPresent()) {
                    gd.annotateEdges(gd.g.getEdgesStrict(state.get().get()), routeStyle);
                    System.out.println(state.get().get());
                    ((Timer)e.getSource()).stop();
                }
            }
        });

        timer.setRepeats(true);
        timer.start();
    }
}
