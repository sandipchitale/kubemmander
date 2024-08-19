package sandipchitale.kubemmander;

import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

public class WhatPanel extends BorderLayoutPanel {

    private static final Border LEFT_BORDER = BorderFactory.createEmptyBorder(0, 20, 0 , 0);

    private JLabel titleLabel;

    private JCheckBox allCheckBox;
    private JCheckBox chartInfoCheckBox;
    private JCheckBox valuesCheckBox;
    private JCheckBox templatesCheckBox;
    private JCheckBox manifestsCheckBox;
    private JCheckBox hooksCheckBox;
    private JCheckBox notesCheckBox;

    private WhatPanel() {
        super(5, 5);
    }

    void setTitleLabel(String release, String revision, String namespace) {
        titleLabel.setText(String.format("What to Load for %s.%s [ %s ]", release, revision, namespace));
    }

    void setTitleLabel(String title) {
        titleLabel.setText(title);
    }

    static WhatPanel build(String at) {
        WhatPanel whatPanel = new WhatPanel();

        JPanel whatCheckBoxesPanel = new JPanel(new GridLayout(0, 1, 5, 5));

        whatPanel.add(whatCheckBoxesPanel, at);

        whatPanel.titleLabel = new JLabel("What to Load for Release.Revision [ Namespace ]");
        whatCheckBoxesPanel.add(whatPanel.titleLabel);

        whatPanel.allCheckBox = new JCheckBox(Constants.ALL, true);
        whatPanel.chartInfoCheckBox = new JCheckBox(Constants.CHART_INFO, true);
        whatPanel.valuesCheckBox = new JCheckBox(Constants.VALUES, true);
        whatPanel.templatesCheckBox = new JCheckBox(Constants.TEMPLATES, true);
        whatPanel.manifestsCheckBox = new JCheckBox(Constants.MANIFESTS, true);
        whatPanel.hooksCheckBox = new JCheckBox(Constants.HOOKS, true);
        whatPanel.notesCheckBox = new JCheckBox(Constants.NOTES, true);

        whatPanel.chartInfoCheckBox.setBorder(LEFT_BORDER);
        whatPanel.valuesCheckBox.setBorder(LEFT_BORDER);
        whatPanel.templatesCheckBox.setBorder(LEFT_BORDER);
        whatPanel.manifestsCheckBox.setBorder(LEFT_BORDER);
        whatPanel.hooksCheckBox.setBorder(LEFT_BORDER);
        whatPanel.notesCheckBox.setBorder(LEFT_BORDER);

        whatCheckBoxesPanel.add(whatPanel.allCheckBox);
//        whatPanel.add(new JSeparator());
        whatCheckBoxesPanel.add(whatPanel.chartInfoCheckBox);
        whatCheckBoxesPanel.add(whatPanel.valuesCheckBox);
        whatCheckBoxesPanel.add(whatPanel.templatesCheckBox);
        whatCheckBoxesPanel.add(whatPanel.manifestsCheckBox);
        whatCheckBoxesPanel.add(whatPanel.hooksCheckBox);
        whatCheckBoxesPanel.add(whatPanel.notesCheckBox);

        whatPanel.allCheckBox.addActionListener(e -> {
            boolean selected = whatPanel.allCheckBox.isSelected();
                whatPanel.chartInfoCheckBox.setSelected(selected);
                whatPanel.valuesCheckBox.setSelected(selected);
                whatPanel.templatesCheckBox.setSelected(selected);
                whatPanel.manifestsCheckBox.setSelected(selected);
                whatPanel.hooksCheckBox.setSelected(selected);
                whatPanel.notesCheckBox.setSelected(selected);
        });

        ActionListener deselectAll = e -> whatPanel.allCheckBox.setSelected(false);
        whatPanel.chartInfoCheckBox.addActionListener(deselectAll);
        whatPanel.valuesCheckBox.addActionListener(deselectAll);
        whatPanel.templatesCheckBox.addActionListener(deselectAll);
        whatPanel.manifestsCheckBox.addActionListener(deselectAll);
        whatPanel.hooksCheckBox.addActionListener(deselectAll);
        whatPanel.notesCheckBox.addActionListener(deselectAll);

        return whatPanel;
    }

    boolean isAny() { return
            chartInfoCheckBox.isSelected()
            || valuesCheckBox.isSelected()
            || templatesCheckBox.isSelected()
            || manifestsCheckBox.isSelected()
            || hooksCheckBox.isSelected()
            || notesCheckBox.isSelected();
    }

    boolean isAll() { return allCheckBox.isSelected(); }
    boolean isChartInfo() { return allCheckBox.isSelected() || chartInfoCheckBox.isSelected(); }
    boolean isValues() { return allCheckBox.isSelected() || valuesCheckBox.isSelected(); }
    boolean isTemplates() { return allCheckBox.isSelected() || templatesCheckBox.isSelected(); }
    boolean isManifests() { return allCheckBox.isSelected() || manifestsCheckBox.isSelected(); }
    boolean isHooks() { return allCheckBox.isSelected() || hooksCheckBox.isSelected(); }
    boolean isNotes() { return allCheckBox.isSelected() || notesCheckBox.isSelected(); }
}
