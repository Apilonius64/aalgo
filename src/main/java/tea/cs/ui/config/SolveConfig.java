package tea.cs.ui.config;

import java.util.List;
import java.util.Optional;

import tea.cs.data.Graph;

public record SolveConfig(String start, String deest) {
    public Optional<Graph.Node> getStart(List<Graph.Node> nodes) {
        return nodes.stream().filter(n -> n.getUnsafeReadableIdentifier().equals(start)).findFirst();
    }

    public Optional<Graph.Node> getDeest(List<Graph.Node> nodes) {
        return nodes.stream().filter(n -> n.getUnsafeReadableIdentifier().equals(deest)).findFirst();
    }
}