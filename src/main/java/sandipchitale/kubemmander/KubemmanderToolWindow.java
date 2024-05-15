package sandipchitale.kubemmander;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class KubemmanderToolWindow {
    private final JPanel contentToolWindow;

    private final DefaultTableModel apiResourceTableModel;
    private final JBTable apiResourceTable;

    private final JTextField kubeconfigTextField;

    private final ComboBox<String> contextComboBox;
    private final ComboBox<String> namespaceComboBox;

    private final JLabel loadingResourcesLabel;
    private final JButton connectToClusterButton;
    private final JButton disconnectFromCusterButton;
    private final JButton reconnectToCusterButton;
    private final JLabel serverVersion;

    private final JCheckBox includeNonNamespacedCheckBox;
    private final JCheckBox includeNamespacedCheckBox;

    private final JCheckBox allNamespacesCheckBox;
    private final ComboBox<String> selectedNamespacesComboBox;

    private final NotificationGroup kubemmanderNotificationGroup;

    private KubernetesClient kubernetesClient = null;

    private String selectedNamespace = null;

    public KubemmanderToolWindow(Project project) {
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);

        kubemmanderNotificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("kubemmanderNotificationGroup");

        apiResourceTableModel = new DefaultTableModel(
                new Object[]{
                        "API Resource",
                        "Short Name",
                        "Version",
                        "Namespaced",
                        "Namespace",
                        "Kind ( API Resource )",
                        ""
                }, 0) {

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) {
                    return Object.class;
                } else return String.class;  //other columns accept String values
            }
        };
        apiResourceTable = new JBTable(apiResourceTableModel);
        ResourceTableCellRenderer resourceTableCellRenderer = new ResourceTableCellRenderer();
        apiResourceTable.setDefaultRenderer(Object.class, resourceTableCellRenderer);
        JBScrollPane scrollPane = new JBScrollPane(apiResourceTable);
        this.contentToolWindow.add(scrollPane, BorderLayout.CENTER);

        TableColumn column = apiResourceTable.getColumnModel().getColumn(6);
        column.setMinWidth(0);
        column.setWidth(0);
        column.setMaxWidth(0);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem getMenuItem = new JMenuItem("Get");
        getMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                execute(actionEvent, "get", project);
            }
        });
        popup.add(getMenuItem );

        JMenuItem describeMenuItem = new JMenuItem("Describe");
        describeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                execute(actionEvent, "describe", project);
            }
        });
        popup.add(describeMenuItem );

        JMenuItem loadMenuItem = new JMenuItem("Load");
        loadMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                execute(actionEvent, "load", project);
            }
        });
        popup.add(loadMenuItem );

        apiResourceTable.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable source = (JTable)e.getSource();
                    int row = source.rowAtPoint( e.getPoint() );
                    int column = source.columnAtPoint( e.getPoint() );

                    if (! source.isRowSelected(row))
                        source.changeSelection(row, column, false, false);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JPanel toolBars = new JPanel(new BorderLayout());

        JPanel topToolBar = new JPanel(new BorderLayout());
        toolBars.add(topToolBar, BorderLayout.NORTH);
        
        JPanel topLeftToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topToolBar.add(topLeftToolBar, BorderLayout.WEST);

        JLabel kubeconfigLabel = new JLabel("Kubeconfig: ");
        kubeconfigLabel.setIcon(KubemmanderIcons.ToolWindow);
        topLeftToolBar.add(kubeconfigLabel);

        kubeconfigTextField = new JTextField(25);
        kubeconfigTextField.setText("~/.kube/config");
        topLeftToolBar.add(kubeconfigTextField);

        JButton editKubeconfigButton = new JButton(AllIcons.Actions.EditSource);
        editKubeconfigButton.addActionListener((ActionEvent actionEvent) -> {
            editKubeConfigFile(project);
        });
        topLeftToolBar.add(editKubeconfigButton);

        connectToClusterButton = new JButton(AllIcons.Actions.Execute);
        connectToClusterButton.setToolTipText("Connect to Cluster");
        connectToClusterButton.addActionListener(this::connectToCluster);
        topLeftToolBar.add(connectToClusterButton);

        reconnectToCusterButton = new JButton(AllIcons.Actions.Refresh);
        reconnectToCusterButton.setToolTipText("Reconnect to Cluster - selected namespace will be used to filter resources");
        reconnectToCusterButton.addActionListener((ActionEvent actionEvent) -> {
            disconnectFromCluster(actionEvent, true);
        });
        topLeftToolBar.add(reconnectToCusterButton);

        disconnectFromCusterButton = new JButton(AllIcons.Actions.Exit);
        disconnectFromCusterButton.setToolTipText("Disconnect from Cluster");
        disconnectFromCusterButton.addActionListener(this::disconnectFromCluster);
        topLeftToolBar.add(disconnectFromCusterButton);

        JPanel topRightToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topToolBar.add(topRightToolBar, BorderLayout.EAST);

        topRightToolBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));

        loadingResourcesLabel = new JLabel("Loading resources...");
        topRightToolBar.add(loadingResourcesLabel);
        loadingResourcesLabel.setVisible(false);

        JLabel contextLabel = new JLabel("Context: ");
        topRightToolBar.add(contextLabel);

        contextComboBox = new ComboBox<>(new DefaultComboBoxModel<>());
        contextComboBox.setMinimumAndPreferredWidth(300);
        topRightToolBar.add(contextComboBox);

        JLabel namespaceLabel = new JLabel("Namespace: ");
        topRightToolBar.add(namespaceLabel);

        namespaceComboBox = new ComboBox<>(new DefaultComboBoxModel<>());
        namespaceComboBox.setMinimumAndPreferredWidth(300);
        topRightToolBar.add(namespaceComboBox);

        JPanel bottomToolBar= new JPanel(new BorderLayout());
        toolBars.add(bottomToolBar, BorderLayout.SOUTH);
        bottomToolBar.add(new JSeparator(), BorderLayout.SOUTH);

        JPanel bottomLeftToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomToolBar.add(bottomLeftToolBar, BorderLayout.WEST);

        JLabel serverVersionLabel = new JLabel("Server version: ");
        bottomLeftToolBar.add(serverVersionLabel);

        serverVersion = new JLabel("");
        bottomLeftToolBar.add(serverVersion);

        JPanel bottomRightToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomToolBar.add(bottomRightToolBar, BorderLayout.EAST);

        JLabel includeLabel = new JLabel("Include: ");
        bottomRightToolBar.add(includeLabel);

        includeNonNamespacedCheckBox = new JCheckBox("Non Namespaced");
        includeNonNamespacedCheckBox.setSelected(true);
        bottomRightToolBar.add(includeNonNamespacedCheckBox);

        bottomRightToolBar.add(new JLabel(" [ "));

        includeNamespacedCheckBox = new JCheckBox("Namespaced");
        includeNamespacedCheckBox.setSelected(true);
        bottomRightToolBar.add(includeNamespacedCheckBox);

        JLabel fromLabel = new JLabel("resources from:");
        bottomRightToolBar.add(fromLabel);

        allNamespacesCheckBox = new JCheckBox();
        allNamespacesCheckBox.setSelected(true);
        bottomRightToolBar.add(allNamespacesCheckBox);

        JLabel allOrSelectedNamespacesLabel = new JLabel("All Namespaces | Selected Namespace:");
        bottomRightToolBar.add(allOrSelectedNamespacesLabel);

        selectedNamespacesComboBox = new ComboBox<>();
        selectedNamespacesComboBox.setMinimumAndPreferredWidth(300);
        bottomRightToolBar.add(selectedNamespacesComboBox);

        bottomRightToolBar.add(new JLabel(" ]"));

        allNamespacesCheckBox.addActionListener(this::adjustSelectedNamespacesComboBoxState);
        adjustSelectedNamespacesComboBoxState(null);

        this.contentToolWindow.add(toolBars, BorderLayout.NORTH);

        connectToCluster(null);
    }

    private void execute(ActionEvent actionEvent, String operation, Project project) {
        if (actionEvent.getSource() instanceof JMenuItem menuItem) {
            JPopupMenu popup = (JPopupMenu) menuItem.getParent();
            if (popup.getInvoker() instanceof JTable table) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow > 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            ApplicationManager.getApplication().runReadAction(() -> {
                                Object valueOfZeroColumn = table.getValueAt(selectedRow, 0);
                                if (valueOfZeroColumn instanceof APIResource apiResource) {
                                    System.out.println(apiResource.getName() + " " + apiResource.getKind());
                                } else if (valueOfZeroColumn instanceof GenericKubernetesResource genericKubernetesResource) {
                                    APIResource apiResource = (APIResource) table.getValueAt(selectedRow, 6);
                                    if (operation.equals("load")) {

                                        List<String> kubectlCommand = new LinkedList<>();
                                        kubectlCommand.add("kubectl");
                                        kubectlCommand.add("get");
                                        kubectlCommand.add("-o");
                                        kubectlCommand.add("yaml");
                                        if (apiResource.getNamespaced()) {
                                            kubectlCommand.add("-n");
                                            kubectlCommand.add(genericKubernetesResource.getMetadata().getNamespace());
                                        }
                                        kubectlCommand.add(apiResource.getKind());
                                        kubectlCommand.add(genericKubernetesResource.getMetadata().getName());

                                        ProcessBuilder kubectlProcessBuilder = new ProcessBuilder(kubectlCommand);
                                        kubectlProcessBuilder.redirectErrorStream(true);

                                        new Thread(() -> {
                                            try {
                                                Process kubectlProcess = kubectlProcessBuilder.start();
                                                String[] kubectlProcessOutput = new String[1];
                                                new Thread(() -> {
                                                    try {
                                                        kubectlProcessOutput[0] = IOUtils.toString(kubectlProcess.getInputStream(), StandardCharsets.UTF_8);
                                                    } catch (IOException ignore) {
                                                    }
                                                }).start();
                                                int exitCode = kubectlProcess.waitFor();
                                                if (exitCode == 0) {
                                                    ApplicationManager.getApplication().invokeLater(() -> {
                                                        FileType  fileType = PlainTextFileType.INSTANCE;

                                                        VirtualFile file = VfsUtil.findFileByIoFile(new File("/path/to/file"), true);

                                                        LightVirtualFile lightVirtualFile = new LightVirtualFile(
                                                                apiResource.getKind() + "-" + genericKubernetesResource.getMetadata().getName() + ".yaml"
                                                                ,fileType
                                                                ,"# " +kubectlCommand.stream().collect(Collectors.joining(" ")) + "\n" + kubectlProcessOutput[0]);
                                                        lightVirtualFile.setWritable(false);
                                                        // Figure out a way to set language for syntax highlighting based on file extension
                                                        lightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
                                                        FileEditorManager.getInstance(project).openFile(lightVirtualFile, true);
                                                        FileEditorManager.getInstance(project).openFile(lightVirtualFile, true);
                                                    });
                                                } else {
                                                    // Show error dialog
                                                    kubemmanderNotificationGroup.createNotification(
                                                                    kubectlCommand.stream().collect(Collectors.joining(" ")) + " failed with exit code " + exitCode
                                                                    ,NotificationType.ERROR)
                                                            .notify(project);
                                                }
                                            } catch (IOException | InterruptedException ignore) {
                                            }
                                        }).start();
                                    } else {
                                        @NotNull ShellTerminalWidget shellTerminalWidget =
                                                TerminalToolWindowManager.getInstance(Objects.requireNonNull(project)).createLocalShellWidget(project.getBasePath(), "kubectl", true, true);
                                        try {
                                            shellTerminalWidget.executeCommand(
                                                    "kubectl "
                                                    + ("get".equals(operation) ? "-o wide " : "")
                                                    + (apiResource.getNamespaced() ? "-n " + genericKubernetesResource.getMetadata().getNamespace() + " " : "")
                                                    + operation
                                                    + " "
                                                    + apiResource.getKind()
                                                    + " "
                                                    + genericKubernetesResource.getMetadata().getName());
                                        } catch (IOException exception) {
                                            // Show error dialog
                                            kubemmanderNotificationGroup
                                                    .createNotification(
                                                            exception.getMessage()
                                                            ,NotificationType.ERROR)
                                                    .notify(project);
                                        }
                                    }
                                }

                            });
                        });
                    });
                }
            }
        }
    }

    private void editKubeConfigFile(Project project) {
        File kubeconfigFile = new File(kubeconfigTextField.getText().replace("~", System.getProperty("user.home")));
        if (kubeconfigFile.isFile()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            VirtualFile kubeconfigVirtualFile = VfsUtil.findFileByIoFile(kubeconfigFile, true);
                            if (kubeconfigVirtualFile != null) {
                                FileEditorManager.getInstance(project).openFile(kubeconfigVirtualFile, true);
                            }
                        } catch (Throwable ignore) {
                        }
                    });
                });
            });
        }
    }

    private void connectToCluster(ActionEvent actionEvent) {
        loadingResourcesLabel.setVisible(true);
        apiResourceTable.setPaintBusy(true);
        contextComboBox.removeAllItems();
        namespaceComboBox.removeAllItems();
        selectedNamespacesComboBox.removeAllItems();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                    kubernetesClient = client;

                    VersionInfo kubernetesVersion = kubernetesClient.getKubernetesVersion();
                    serverVersion.setText(kubernetesVersion.getMajor() + "." + kubernetesVersion.getMinor());
                    Config configuration = client.getConfiguration();
                    configuration.getContexts().forEach((context) -> {
                        contextComboBox.addItem(context.getName());
                    });

                    kubernetesClient
                            .namespaces()
                            .list(new ListOptionsBuilder().withKind("namespace").build()).getItems().forEach((Namespace namespace) -> {
                                namespaceComboBox.addItem(namespace.getMetadata().getName());
                                selectedNamespacesComboBox.addItem(namespace.getMetadata().getName());
                            });
                    if (configuration.isDefaultNamespace()) {
                        String namespace = configuration.getNamespace();
                        if (namespace != null) {
                            namespaceComboBox.setSelectedItem(namespace);
                            selectedNamespacesComboBox.setSelectedItem(namespace);
                        }
                        if (selectedNamespace != null) {
                            int itemCount = selectedNamespacesComboBox.getItemCount();
                            for (int i = 0; i < itemCount; i++) {
                                if (selectedNamespace.equals(selectedNamespacesComboBox.getItemAt(i))) {
                                    selectedNamespacesComboBox.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                    }
                    Set<APIResource> apiResourceSet =
                            new TreeSet<>((APIResource apiResource1, APIResource apiResource2) -> apiResource1.getName().compareTo(apiResource2.getName()));
                    Set.of("v1", "apps/v1", "batch/v1", "batch/v1beta1", "extensions/v1beta1", "networking.k8s.io/v1", "storage.k8s.io/v1").forEach((String apiVersion) -> {
                        APIResourceList apiResources = kubernetesClient.getApiResources(apiVersion);
                        if (apiResources != null && apiResources.getResources() != null) {
                            apiResourceSet.addAll(apiResources.getResources());
                        }
                    });
                    if (includeNamespacedCheckBox.isSelected()) {
                        apiResourceTableModel.addRow(
                                new Object[]{
                                        "helmreleases",
                                        "rel",
                                        "",
                                        "✔",
                                        "",
                                        "helm ( release )",
                                        "helmreleases"
                                });
                    }
                    apiResourceSet.forEach((APIResource apiResource) -> {
                        if (apiResource.getNamespaced() && !includeNamespacedCheckBox.isSelected()) {
                            return;
                        } else if (!apiResource.getNamespaced() && !includeNonNamespacedCheckBox.isSelected()) {
                            return;
                        }
                        apiResourceTableModel.addRow(
                                new Object[]{
                                        apiResource,
                                        String.join(", ", apiResource.getShortNames()),
                                        apiResource.getVersion(),
                                        (apiResource.getNamespaced() ? "✔" : ""),
                                        "",
                                        apiResource.getKind() + " ( " + apiResource.getName() + " )",
                                        apiResource
                                });
                        if (!apiResource.getName().contains("/")) {
                            try {
                                ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                                        .withKind(apiResource.getKind())
                                        .build();
                                kubernetesClient
                                        .genericKubernetesResources(resourceDefinitionContext)
                                        .list()
                                        .getItems().forEach((GenericKubernetesResource genericKubernetesResource) -> {
                                            boolean doAddResourceRow = false;
                                            if (!apiResource.getNamespaced()){
                                                doAddResourceRow = true;
                                            } else {
                                                if (selectedNamespace != null && selectedNamespace.equals(genericKubernetesResource.getMetadata().getNamespace())) {
                                                    doAddResourceRow = true;
                                                }
                                            }
                                            if (doAddResourceRow) {
                                                apiResourceTableModel.addRow(
                                                        new Object[]{
                                                                genericKubernetesResource,
                                                                String.join(", ", apiResource.getShortNames()),
                                                                apiResource.getVersion(),
                                                                (apiResource.getNamespaced() ? "✔" : ""),
                                                                (apiResource.getNamespaced() ? genericKubernetesResource.getMetadata().getNamespace() : ""),
                                                                apiResource.getKind() + " ( " + apiResource.getName() + " )",
                                                                apiResource
                                                        });
                                            }
                                        });
                            } catch (Throwable ignore) {
                            }
                        }
                    });

                } catch (Throwable ignore) {
                    kubernetesClient = null;
                } finally {
                    selectedNamespace = null;
                    loadingResourcesLabel.setVisible(false);
                    apiResourceTable.setPaintBusy(false);
                    adjustStates();
                }
            });
        });
    }
    private void disconnectFromCluster(ActionEvent actionEvent) {
        disconnectFromCluster(actionEvent, false);
    }

    private void disconnectFromCluster(ActionEvent actionEvent, boolean reconnect) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                if (reconnect) {
                    selectedNamespace = (String) selectedNamespacesComboBox.getSelectedItem();
                }
                try {
                    if (kubernetesClient != null) {
                        kubernetesClient.close();
                    }
                    if (reconnect) {
                        connectToCluster(actionEvent);
                    }
                } finally {
                    kubernetesClient = null;
                    apiResourceTableModel.setRowCount(0);
                    if (!reconnect) {
                        adjustStates();
                    }
                }
            });
        });
    }

    private void adjustSelectedNamespacesComboBoxState(ActionEvent actionEvent) {
        selectedNamespacesComboBox.setEnabled(!allNamespacesCheckBox.isSelected());
    }

    private void adjustStates() {
        connectToClusterButton.setVisible(kubernetesClient == null);
        disconnectFromCusterButton.setVisible(kubernetesClient != null);
        reconnectToCusterButton.setVisible(kubernetesClient != null);
        contextComboBox.setEnabled(kubernetesClient != null);
        namespaceComboBox.setEnabled(kubernetesClient != null);
        allNamespacesCheckBox.setEnabled(kubernetesClient != null);
        selectedNamespacesComboBox.setEnabled(kubernetesClient != null && !allNamespacesCheckBox.isSelected());
        if (kubernetesClient == null) {
            contextComboBox.removeAllItems();
            namespaceComboBox.removeAllItems();
            selectedNamespacesComboBox.removeAllItems();
            serverVersion.setText("");
            apiResourceTableModel.setRowCount(0);
        }
    }

    public JComponent getContent() {
        return this.contentToolWindow;
    }

    private static class ResourceTableCellRenderer extends DefaultTableCellRenderer {
        private final Icon FILE_ICON = UIManager.getIcon("Tree.leafIcon");
        private final Icon DIRECTORY_ICON = UIManager.getIcon("Tree.closedIcon");

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object valueInColumnZero = table.getModel().getValueAt(row, 0);
            Object valueInColumnSix = table.getModel().getValueAt(row, 6);

            Icon icon = null;
            if (column == 0) {
                if (valueInColumnSix instanceof APIResource apiResource) {
                    icon = KubemmanderIcons.resourceTypeWithIcon.get(apiResource.getName());
                } else if (valueInColumnSix instanceof String stringValue) {
                    icon = KubemmanderIcons.resourceTypeWithIcon.get(stringValue);
                }
            }

            if (value instanceof APIResource apiResource) {
                value = apiResource.getName();
            } else if (value instanceof GenericKubernetesResource genericKubernetesResource) {
                value = genericKubernetesResource.getMetadata().getName();
            }

            Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (cellRendererComponent instanceof JLabel labelCellRendererComponent) {
                labelCellRendererComponent.setIcon(null);
                if (column == 0) {
                    if (icon == null) {
                        if (valueInColumnZero instanceof APIResource apiResource) {
                            icon = DIRECTORY_ICON;
                        } else if (valueInColumnZero instanceof GenericKubernetesResource) {
                            icon = FILE_ICON;
                        }
                    }
                    labelCellRendererComponent.setIcon(icon);
                    if (valueInColumnZero instanceof GenericKubernetesResource) {
                        labelCellRendererComponent.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                    }
                }
            }

            return cellRendererComponent;
        }
    }
}