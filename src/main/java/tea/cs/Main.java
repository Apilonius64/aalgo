package tea.cs;

import java.util.ArrayList;
import java.util.Arrays;

import tea.cs.data.Graph;
import tea.cs.data.Vec2;
import tea.cs.ui.App;

public class Main {
    public static void main(String[] args) {
        var g = new Graph(Arrays.asList(new Graph.Node(new Vec2(0, 0))), new ArrayList<>());
        System.out.println(g);

        @SuppressWarnings("unused")
        App app = new App(g);
    }
}