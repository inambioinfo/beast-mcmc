/*
 * PriorDialog.java
 *
 * @author Marc A. Suchard
 */

package dr.app.beauti.priorsPanel;

import dr.app.beauti.components.hpm.HierarchicalModelComponentOptions;
import dr.app.beauti.components.hpm.HierarchicalPhylogeneticModel;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.Parameter;
import dr.app.beauti.types.PriorType;
import dr.app.gui.chart.Axis;
import dr.app.gui.chart.JChart;
import dr.app.gui.chart.LinearAxis;
import dr.app.gui.chart.PDFPlot;
import dr.app.gui.components.RealNumberField;
import dr.app.util.OSType;
import dr.math.distributions.Distribution;
import dr.math.distributions.GammaDistribution;
import dr.math.distributions.NormalDistribution;
import dr.math.distributions.OffsetPositiveDistribution;
import dr.util.NumberFormatter;
import jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marc A. Suchard
 */
public class HierarchicalPriorDialog {

    private JFrame frame;

    private Map<PriorType, PriorOptionsPanel> optionsPanels = new HashMap<PriorType, PriorOptionsPanel>();

    private JComboBox priorCombo = new JComboBox();
    private JCheckBox meanInRealSpaceCheck = new JCheckBox();
    private RealNumberField initialField = new RealNumberField();
    private RealNumberField selectedField;

//    private final SpecialNumberPanel specialNumberPanel;

    private JTextField nameField;
    private JPanel panel;

//    private final SpecialNumberPanel specialNumberPanel;
    private JChart[] chart;
    private JPanel[] quantilePanel;
    private JTextArea[] quantileText;

    private java.util.List<Parameter> parameterList;
    private Parameter parameter;

    final private BeautiOptions options;

    private double hpmMeanMean;
    private double hpmMeanStDev;
    private double hpmPrecShape;
    private double hpmPrecScale;

    public HierarchicalPriorDialog(JFrame frame, BeautiOptions options) {
        this.frame = frame;
        this.options = options;

        initialField.setColumns(10);

        optionsPanels.put(PriorType.NORMAL_PRIOR, new NormalOptionsPanel());
        optionsPanels.put(PriorType.GAMMA_PRIOR, new GammaOptionsPanel());

        chart = new JChart[2];
        quantileText = new JTextArea[2];
        quantilePanel = new JPanel[2];

        for (int i = 0; i < 2; ++i) {
            chart[i] = new JChart(new LinearAxis(Axis.AT_MINOR_TICK, Axis.AT_MINOR_TICK),
                    new LinearAxis(Axis.AT_ZERO, Axis.AT_DATA));

            JLabel quantileLabels = new JLabel();
            quantileLabels.setFont(quantileLabels.getFont().deriveFont(10.0f));
            quantileLabels.setOpaque(false);
            quantileLabels.setText("<html><p align=\"right\">Quantiles: 2.5%:<br>5%:<br>Median:<br>95%:<br>97.5%:</p></html>");

            quantileText[i] = new JTextArea(0, 5);
            quantileText[i].setFont(quantileText[i].getFont().deriveFont(10.0f));
            quantileText[i].setOpaque(false);
            quantileText[i].setEditable(false);
            quantileLabels.setHorizontalAlignment(JLabel.LEFT);

            quantilePanel[i] = new JPanel();
            quantilePanel[i].add(quantileLabels);
            quantilePanel[i].add(quantileText[i]);
        }

//        specialNumberPanel = new SpecialNumberPanel(this);
//        specialNumberPanel.setEnabled(false);
    }

    public void addHPM(java.util.List<Parameter> parameterList) {
        HierarchicalModelComponentOptions comp = (HierarchicalModelComponentOptions)
                options.getComponentOptions(HierarchicalModelComponentOptions.class);
        HierarchicalPhylogeneticModel hpm = comp.addHPM(nameField.getText(), parameterList, parameterList.get(0).priorType);

        hpm.getConditionalParameterList().get(0).mean = hpmMeanMean;
        hpm.getConditionalParameterList().get(0).stdev = hpmMeanStDev;

        hpm.getConditionalParameterList().get(1).shape = hpmPrecShape;
        hpm.getConditionalParameterList().get(1).scale = hpmPrecScale;

    }

    public boolean validateModelName() {
        return validateModelName(nameField.getText());
    }

    private boolean validateModelName(String modelName) {
//        System.err.println("Validating: " + modelName);
        // check that the name is valid
        if (modelName.trim().length() == 0) {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }

        // check that the trait name doesn't exist
        if (modelExists(modelName)) {
            JOptionPane.showMessageDialog(frame,
                    "A model with this name already exists.",
                    "HPM name error",
//                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
//            System.err.println("Model name exists");
            return false;

//            if (option == JOptionPane.NO_OPTION) {
//                return false;
//            }
        }

        return true;
    }

    private boolean modelExists(String modelName) {

        HierarchicalModelComponentOptions comp = (HierarchicalModelComponentOptions)
                options.getComponentOptions(HierarchicalModelComponentOptions.class);
        return comp.modelExists(modelName);
    }

    public int showDialog(final java.util.List<Parameter> parameterList) {

        this.parameterList = parameterList;
        this.parameter = parameterList.get(0);
        PriorType priorType = parameter.priorType;

        // Set-up combo box depending on parameters
        priorCombo.removeAllItems();
        if (parameter.isNonNegative) {
            priorCombo.addItem(PriorType.LOGNORMAL_HPM_PRIOR);
        }
        priorCombo.addItem(PriorType.NORMAL_HPM_PRIOR);

        priorCombo.setSelectedItem(priorType);

            double lower = Double.NEGATIVE_INFINITY;
            double upper = Double.POSITIVE_INFINITY;

            if (parameter.isZeroOne) {
                lower = 0.0;
                upper = 1.0;
            } else if (parameter.isNonNegative) {
                lower = 0.0;
            }

            initialField.setRange(lower, upper);
            initialField.setValue(parameter.initial);

        panel = new JPanel(new GridBagLayout());

        setupComponents();

        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setOpaque(false);

        JOptionPane optionPane = new JOptionPane(scrollPane,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                null,
                null);
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        final JDialog dialog = optionPane.createDialog(frame, "Phylogenetic Hierarchical Model Setup");

        priorCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setupComponents();
                dialog.validate();
                dialog.repaint();
                dialog.pack();
            }
        });

        KeyListener listener = new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if (e.getComponent() instanceof RealNumberField) {
                    String number = ((RealNumberField) e.getComponent()).getText();
                    if (!(number.equals("") || number.endsWith("e") || number.endsWith("E")
                            || number.startsWith("-") || number.endsWith("-"))) {
//                        System.out.println(e.getID() + " = \"" + ((RealNumberField) e.getComponent()).getText() + "\"");
                        setupChart();
                        dialog.repaint();
                    }
                }
            }
        };

        dialog.pack();
        if (OSType.isMac()) {
            dialog.setMinimumSize(new Dimension(dialog.getBounds().width, 300));
        } else {
            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension d = tk.getScreenSize();
            if (d.height < 700 && panel.getHeight() > 450) {
                dialog.setSize(new Dimension(panel.getWidth() + 100, 550));
            } else {
                // setSize because optionsPanel is shrunk in dialog
                dialog.setSize(new Dimension(panel.getWidth() + 100, panel.getHeight() + 100));
            }

//            System.out.println("panel width = " + panel.getWidth());
//            System.out.println("panel height = " + panel.getHeight());
        }

        dialog.setResizable(true);
        dialog.setVisible(true);

        int result = JOptionPane.CANCEL_OPTION;
        Integer value = (Integer) optionPane.getValue();
        if (value != null && value != -1) {
            result = value;
        }

//        if (result == JOptionPane.OK_OPTION) {
//            getArguments();
//        }

        return result;
    }

    private void setArguments(PriorType priorType) {
//        PriorOptionsPanel panel;
//        switch (priorType) {
//            case NORMAL_HPM_PRIOR:
//                panel = optionsPanels.get(priorType);
//                panel.getField(0).setValue(parameter.mean);
////                panel.getField(1).setRange(0.0, Double.MAX_VALUE);
//                panel.getField(1).setValue(parameter.stdev);
//                break;
//
//            case LOGNORMAL_HPM_PRIOR:
//                panel = optionsPanels.get(priorType);
//                if (parameter.isMeanInRealSpace() && parameter.mean <= 0) {// if LOGNORMAL && meanInRealSpace = true, then mean > 0
//                    panel.getField(0).setValue(0.01);
//                } else {
//                    panel.getField(0).setValue(parameter.mean);
//                }
//                panel.getField(1).setValue(parameter.stdev);
//                panel.getField(2).setValue(parameter.offset);
//                meanInRealSpaceCheck.setSelected(parameter.isMeanInRealSpace());
//                break;
//        }
//        HierarchicalModelComponentOptions comp = (HierarchicalModelComponentOptions)
//                options.getComponentOptions(HierarchicalModelComponentOptions.class);
//        PriorOptionsPanel panel;

//        panel = optionsPanels.get(PriorType.NORMAL_PRIOR);
//        Parameter meanParameter = comp.get

    }

    public void getArguments() {
        for (Parameter parameter : parameterList) {
            parameter.priorType = (PriorType) priorCombo.getSelectedItem();
        }
        // Get hyperpriors
        optionsPanels.get(PriorType.NORMAL_PRIOR).getArguments(parameter);
        optionsPanels.get(PriorType.GAMMA_PRIOR).getArguments(parameter);

    }

    private void setupComponents() {
        panel.removeAll();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        panel.add(mainPanel);

        OptionsPanel[] optionsPanel = new OptionsPanel[4];
        JPanel panel[] = new JPanel[4];
        for (int i = 0; i < 4; ++i) {
            optionsPanel[i] = new OptionsPanel(12, (OSType.isMac() ? 6 : 24));
            panel[i] = new JPanel(new FlowLayout(FlowLayout.CENTER));
            if (i > 1) {
                panel[i].setLayout(new BoxLayout(panel[i], BoxLayout.PAGE_AXIS));
            }
            panel[i].add(optionsPanel[i]);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = i % 2;
            gbc.gridy = i / 2;
            mainPanel.add(panel[i], gbc);
        }

        optionsPanel[0].addComponent(new JLabel("Select HPM for parameters: "));

        OptionsPanel list = new OptionsPanel();
        list.setBackground(Color.WHITE);
        list.setOpaque(true);

        for (Parameter p : parameterList) {
            JLabel label = new JLabel("\t" + p.getName());
            label.setForeground(Color.DARK_GRAY);
            list.addSpanningComponent(label);
        }

        optionsPanel[0].addSpanningComponent(list);

        PriorType modelType;
        optionsPanel[1].addComponentWithLabel("Hierarchical Distribution: ", priorCombo);
        modelType = (PriorType) priorCombo.getSelectedItem();

        optionsPanel[1].addSeparator();

        String modelName = "untitled";
        nameField = new JTextField(modelName);
        nameField.setColumns(10);
        optionsPanel[1].addComponentWithLabel("Unique Name: ", nameField);

        optionsPanel[2].addSeparator();
        optionsPanel[3].addSeparator();

        optionsPanel[2].addSpanningComponent(new JLabel("Population Mean Hyperprior: Normal"));
        optionsPanel[3].addSpanningComponent(new JLabel("Population Precision Hyperprior: Gamma"));

        optionsPanel[2].addSpanningComponent(optionsPanels.get(PriorType.NORMAL_PRIOR));
        optionsPanel[3].addSpanningComponent(optionsPanels.get(PriorType.GAMMA_PRIOR));

        optionsPanel[2].addSeparator();
        optionsPanel[3].addSeparator();

        setupChart();
        for (int i = 0; i < 2; ++i) {
            chart[i].setPreferredSize(new Dimension(300, 200));
            chart[i].setFontSize(8);
            panel[2 + i].add(chart[i]);
            panel[2 + i].add(quantilePanel[i]);
        }

        setArguments(modelType);
        mainPanel.repaint();
    }

//    public void setSelectedField(RealNumberField selectedField) {
//        this.selectedField = selectedField;
//    }
//
//    public RealNumberField getSelectedField() {
//        return selectedField;
//    }

    NumberFormatter formatter = new NumberFormatter(4);

    private void setupChart() {
        for (int i = 0; i < 2; ++i) {
            chart[i].removeAllPlots();

            double offset = 0.0;
            Distribution distribution = null;
            switch (i) {
                case 0:
                    distribution = optionsPanels.get(PriorType.NORMAL_PRIOR).getDistribution();
                    break;
                case 1:
                    distribution = optionsPanels.get(PriorType.GAMMA_PRIOR).getDistribution();
                    break;
            }

            chart[i].addPlot(new PDFPlot(distribution, offset));
            if (distribution != null) {
                quantileText[i].setText(formatter.format(distribution.quantile(0.025)) +
                        "\n" + formatter.format(distribution.quantile(0.05)) +
                        "\n" + formatter.format(distribution.quantile(0.5)) +
                        "\n" + formatter.format(distribution.quantile(0.95)) +
                        "\n" + formatter.format(distribution.quantile(0.975)));
            }
        }
    }

    // options panels

    class NormalOptionsPanel extends PriorOptionsPanel {

        public NormalOptionsPanel() {
            super(false);
        }

        public void setup() {
            addField("Hyperprior Mean", 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            addField("Hyperprior Stdev", 1000.0, 0.0, Double.MAX_VALUE);
        }

        public Distribution getDistribution() {
            return new NormalDistribution(getValue(0), getValue(1));
        }

        public void setArguments(Parameter parameter) {
            getField(0).setValue(hpmMeanMean);
            getField(1).setValue(hpmMeanStDev);
        }

        public void getArguments(Parameter parameter) {
            hpmMeanMean = getValue(0);
            hpmMeanStDev = getValue(1);
        }
    }

    class GammaOptionsPanel extends PriorOptionsPanel {

        public GammaOptionsPanel() {
            super(false);
        }

        public void setup() {
            addField("Hyperprior Shape", 0.001, Double.MIN_VALUE, Double.MAX_VALUE);
            addField("Hyperprior Scale", 1000.0, Double.MIN_VALUE, Double.MAX_VALUE);
//            addField("Offset", 0.0, 0.0, Double.MAX_VALUE);
        }

        public Distribution getDistribution() {
            return new OffsetPositiveDistribution(
                    new GammaDistribution(getValue(0), getValue(1)), 0.0);
        }

        public void setArguments(Parameter parameter) {
            getField(0).setValue(hpmPrecShape);
            getField(1).setValue(hpmPrecScale);
        }

        public void getArguments(Parameter parameter) {
            hpmPrecShape = getValue(0);
            hpmPrecScale = getValue(1);
        }
    }
}