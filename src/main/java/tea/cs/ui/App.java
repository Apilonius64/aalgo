package tea.cs.ui;

import tea.cs.data.Graph;
import tea.cs.ui.config.OSMAPIImport;
import tea.cs.ui.dialog.ConfigDialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;
public class App {
    public GraphDrawer gd = null;

    private void newWindow(Graph graph) {
        newWindow(new GraphDrawer(graph));
    }

    private void newWindow(GraphDrawer gd) {
        JFrame frame = new JFrame("Tea");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.getContentPane().add(gd, BorderLayout.CENTER);
        frame.setVisible(true);

        JRootPane root = frame.getRootPane();

        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("ctrl O"), "open");
        inputMap.put(KeyStroke.getKeyStroke("ctrl I"), "import_osm");

        actionMap.put("open", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser j = new JFileChooser("./data");
                if(j.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    newWindow(Graph.of(j.getSelectedFile().getAbsolutePath()).get());
                }
            }
        });

        actionMap.put("import_osm", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ConfigDialog<OSMAPIImport> configDialog = new ConfigDialog<>(frame, OSMAPIImport.class);
                OSMAPIImport config = configDialog.get().get();
                String jsonData = config.getData().get();
                GraphDrawer gd = GraphDrawer.ofOSMJson(jsonData);
                frame.dispose();
                newWindow(gd);
            }
        });
    }

    public App(Graph g) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                newWindow(g);
            }
        });
    }
}
