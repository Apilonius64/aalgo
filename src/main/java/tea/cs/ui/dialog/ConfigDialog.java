package tea.cs.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.*;

// Generates dialogs from Java records
public class ConfigDialog<T> extends JDialog {
    public static interface ConfigInput {
        public static Optional<ConfigInput> of(Field field) {
            // Guess field type
            if(field.getType() == String.class) {
                return Optional.of(new TextConfigInput(field));
            }

            if(field.getType() == Double.class) {
                return Optional.of(new DoubleConfigInput(field));
            }

            return Optional.empty();
        }

        public Object getValue();
        public void registerWidgets(JPanel form);
    }

    public static abstract class GenericConfigInput<T extends JComponent> implements ConfigInput {
        protected final String name;
        protected final T input;

        public abstract T getComp();

        public GenericConfigInput(Field field) {
            this.name = field.getName();
            this.input = getComp();
        }

        public void registerWidgets(JPanel form) {
            form.add(new JLabel(String.format("%s:", name)));
            form.add(input);
        }
    }

    public static class TextConfigInput extends GenericConfigInput<JTextField> {
        public JTextField getComp() {
            return new JTextField(15);
        }

        public Object getValue() {
            return input.getText();
        }

        public TextConfigInput(Field field) {
            super(field);
        }
    }

    public static class DoubleConfigInput extends TextConfigInput {
        public Object getValue() {
            return Double.parseDouble(input.getText());
        }

        public DoubleConfigInput(Field field) {
            super(field);
        }
    }

    protected final Class<T> rec;
    protected final Field[] fields;
    protected List<ConfigInput> widgets = new ArrayList<ConfigInput>();
    protected boolean conf = false;

    public ConfigDialog(Frame parent, Class<T> rec) {
        super(parent, rec.getName(), true);
        this.rec = rec;

        fields = rec.getDeclaredFields();

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        for (Field field : fields) {
            ConfigInput widget = ConfigInput.of(field).get();
            widgets.add(widget);
            widget.registerWidgets(form);
        }

        JButton ok = new JButton("Ok");
        JButton cancel = new JButton("Cancel");

        ok.addActionListener(e -> {
            conf = true;
            dispose();
        });
        getRootPane().setDefaultButton(ok);

        cancel.addActionListener(e -> {
            conf = false;
            dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);

        Container main = getContentPane();
        main.add(form, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    public Optional<T> get() {
        setVisible(true);
        if(!conf) {
            return Optional.empty();
        }
        var constr = rec.getDeclaredConstructors()[0];
        int numArgs = constr.getParameters().length;
        assert widgets.size() == numArgs;
        Object[] args = new Object[numArgs];

        // Retrieve values
        for (int i=0;i<numArgs;++i) {
            args [i] = widgets.get(i).getValue();
        }

        // Try to get an instance
        try {
            return Optional.of(rec.cast(constr.newInstance(args)));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
