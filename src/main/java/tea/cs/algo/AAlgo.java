package tea.cs.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import tea.cs.data.Graph;

public class AAlgo implements Solver.Backend<AAlgo.QueryItem> {
    protected final Graph graph;
    protected final Graph.Node start;
    protected final Graph.Node deest;

    public Graph.Node getStart() {
        return start;
    }

    public Graph.Node getDeest() {
        return deest;
    }

    public AAlgo(
        Graph graph,
        Graph.Node start,
        Graph.Node deest
    ) {
        this.start = start;
        this.deest = deest;
        this.graph = graph;
    }

    public static int approxCost(Graph.Node a, Graph.Node b) {
        return a.pos().dist(b.pos());
    }

    public class QueryItem implements Solver.AbstractQueryItem {
        public Graph.Node node;
        public int from_start, approx;
        public QueryItem parent;

        QueryItem(Graph.Node node, int from_start, int approx, QueryItem parent) {
            this.node = node;
            this.from_start = from_start;
            this.approx = approx;
            this.parent = parent;
        }

        QueryItem(Graph.Node node, Graph.Node to, int from_start, QueryItem parent) {
            this(node, from_start, from_start + AAlgo.approxCost(node, to), parent);
        }

        public String toString() {
            return String.format("Q%s", node.toString());
        }

        public Graph.Node node() {
            return node;
        }
    }

    public List<AAlgo.QueryItem> initStack() {
        return Arrays.asList(new QueryItem(start, deest, 0, null));
    }

    public Optional<Optional<List<Graph.Node>>> doStep(List<QueryItem> items, List<QueryItem> query) {
        // Sortieren der Warteschlange nach Kostenabschätzung
        query.sort((x, y) -> x.approx - y.approx);

        // Prüfen, ob die Query leer ist (Keine Lösung)
        if (query.isEmpty()) {
            return Optional.of(Optional.empty());
        }

        // Prüfen, ob der erste Knoten unserer Query der
        // Zielknoten ist (Lösung gefunden)
        if (query.getFirst().node == deest) {
            List<QueryItem> result = new ArrayList<>(Arrays.asList(query.getFirst()));
            while(result.getLast().node != start) {
                result.add(result.getLast().parent);
            }
            return Optional.of(Optional.of(result.stream().map(v -> v.node).toList()));
        }

        QueryItem item = query.removeFirst();

        // Berechnung der Schätzwerte für Nachbarknoten
        for (Graph.Edge edge : graph.getEdges(item.node)) {
            Optional<QueryItem> existent = items.stream().filter(e -> e.node == edge.to()).findFirst();
            int cost = item.from_start+edge.w();

            // Aktualisieren von bereits verarbeiteten Knoten, wenn der aktuelle Weg
            // kostengünstiger ist.
            if(existent.isPresent()) {
                if (cost < existent.get().from_start) {
                    existent.get().from_start = cost;
                    existent.get().parent = item;
                }
                continue;
            }

            // Hinzufügen des Knotens zur Warteschlange
            QueryItem n = new QueryItem(edge.to(), deest, cost, item);
            items.add(n);
            query.add(n);
        }

        return Optional.empty();
    }
}
