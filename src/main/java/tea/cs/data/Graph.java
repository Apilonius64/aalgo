package tea.cs.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record Graph(List<Node> nodes, List<Edge> edges) {
    // Designed for interoperability with TUM's graph txts,
    // https://algorithms.discrete.ma.tum.de/graph-algorithms/spp-a-star/index_de.html#tab_ti
    public enum Token {
        NODE("n"), EDGE("e"), COMMENT("%");

        private final String v;

        Token(String v) {
            this.v = v;
        }

        public boolean equals(String other) {
            return this.v.equals(other);
        }
    }

    public interface hasPosition {
        public boolean inTile(Vec2.Tile t);
    }

    public record Node(Vec2 pos) implements hasPosition {
        public static Node of(List<Integer> l, int offset) {
            return new Node(
                new Vec2(l.get(offset + 0),
                l.get(offset + 1))
            );
        }

        public boolean inTile(Vec2.Tile t) {
            return t.contains(pos);
        }

        public String getUnsafeReadableIdentifier() {
            return Integer.toHexString(hashCode()); // % 251
        }
    }

    public record Edge(Node from, Node to, int w) implements hasPosition{
        public static Edge of(List<Integer> l, int offset, List<Node> nodes, List<Integer> nodeIDs) {
            return new Edge(
                nodes.get(nodeIDs.indexOf(l.get(offset + 0))),
                nodes.get(nodeIDs.indexOf(l.get(offset + 1))),
                l.get(offset + 3)
            );
        }

        public boolean inTile(Vec2.Tile t) {
            return (t.contains(from.pos) || t.contains(to.pos));
        }
    }

    public List<Edge> getEdges(Node node) {
        return edges.stream().filter(v -> node == v.from).toList();
    }

    public List<Edge> getEdgesStrict(List<Node> nodes) {
        return edges.stream()
            .filter(v -> (
                nodes.contains(v.from())
                && nodes.contains(v.to())
            )
        ).toList();
    }

    public static Optional<Graph> of(Path path) {
        try (Stream<String> lines = Files.lines(path)) {
            var rawTokens = lines.map(v -> v.split("\\W+")).map(Arrays::asList).toList();
            // Parse integers, skip comments
            List<List<Integer>> tokens = rawTokens.stream()
                .filter(v -> !Token.COMMENT.equals(v.get(0)) && v.get(0).length() != 0)
                .map((List<String> v) -> v.stream().skip(1).map((String x) -> Integer.parseInt(x)).toList())
                .toList();

            // Parse nodes
            List<List<Integer>> rawNodes = tokens.stream().filter((List<Integer> v) -> (v.size() == 3)).toList();
            List<Node> nodes = rawNodes.stream().map((List<Integer> v) -> Node.of(v, 0)).toList();
            
            // Parse edges
            List<Integer> nodeIDs = tokens.stream().map((List<Integer> v) -> v.get(2)).toList();
            List<Edge> edges = tokens.stream()
                .filter((List<Integer> v) -> (v.size() == 4))
                .map((List<Integer> v) -> Edge.of(v, 0, nodes, nodeIDs)).toList();

            return Optional.of(new Graph(List.copyOf(nodes), List.copyOf(edges)));
        }
        catch(IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<Graph> of(String s) {
        return of(Path.of(s));
    }
}