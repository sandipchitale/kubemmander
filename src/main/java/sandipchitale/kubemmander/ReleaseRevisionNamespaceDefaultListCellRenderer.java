package sandipchitale.kubemmander;

import javax.swing.*;
import java.awt.*;

class ReleaseRevisionNamespaceDefaultListCellRenderer extends DefaultListCellRenderer {
    static final ReleaseRevisionNamespaceDefaultListCellRenderer INSTANCE = new ReleaseRevisionNamespaceDefaultListCellRenderer();

    private ReleaseRevisionNamespaceDefaultListCellRenderer() {
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (listCellRendererComponent instanceof JLabel listCellRendererComponentLabel) {
            NamespaceSecretReleaseRevision valueNamespaceSecretReleaseRevision4 = (NamespaceSecretReleaseRevision) value;
            listCellRendererComponentLabel.setText(
                    String.format("%-64s [ %s ]",
                            valueNamespaceSecretReleaseRevision4.release() + "." + valueNamespaceSecretReleaseRevision4.revision(),
                            valueNamespaceSecretReleaseRevision4.namespace().getMetadata().getName()));
            listCellRendererComponentLabel.setIcon(HelmetIcons.icon);
        }
        return listCellRendererComponent;
    }
}
