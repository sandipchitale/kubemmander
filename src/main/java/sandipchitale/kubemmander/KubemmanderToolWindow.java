package sandipchitale.kubemmander;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
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
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KubemmanderToolWindow {
    private final JPanel contentToolWindow;

    private final DefaultTableModel apiResourceTableModel;
    private final JBTable apiResourceTable;

    private final JTextField kubeconfigTextField;

    private final ComboBox<String> contextComboBox;
    private boolean addingContexts = false;
    private final ComboBox<String> namespaceComboBox;
    private boolean addingNamespaces = false;

    private final JLabel loadingResourcesLabel;
    private final JButton connectToClusterButton;
    private final JButton disconnectFromCusterButton;
    private final JButton reconnectToCusterButton;
    private final JLabel serverVersion;

    private final JCheckBox includeInstancesCheckBox;

    private final JCheckBox includeNonNamespacedCheckBox;
    private final JCheckBox includeNamespacedCheckBox;

    private final JCheckBox allNamespacesCheckBox;
    private final KubemmanderMenuButton selectedNamespacesMenuButton;

    private final JPopupMenu selectedNamespacesPopupMenu;

    private final Set<String> selectedNamespaces = new TreeSet<>();

    private final NotificationGroup kubemmanderNotificationGroup;

    private KubernetesClient kubernetesClient = null;

    private final Pattern HELM_SECRET_NAME_PATTERN = Pattern.compile("sh\\.helm\\.release\\.v\\d+\\.(.*).v(\\d)+");

    private final WhatPanel whatPanel = WhatPanel.build(BorderLayout.CENTER);

    private final Map<String, Namespace> namespacenameToNamespaceMap = new HashMap<>();

    public KubemmanderToolWindow(Project project) {
        this.contentToolWindow = new SimpleToolWindowPanel(true, true);

        kubemmanderNotificationGroup = NotificationGroupManager.getInstance()
                .getNotificationGroup("kubemmanderNotificationGroup");

        apiResourceTableModel = new DefaultTableModel(
                new Object[]{
                        "API Resource",
                        "Short Name",
                        "Version",
                        "",
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

        TableColumn column = apiResourceTable.getColumnModel().getColumn(1);
        column.setMinWidth(140);
        column.setWidth(140);
        column.setMaxWidth(140);

        column = apiResourceTable.getColumnModel().getColumn(2);
        column.setMinWidth(140);
        column.setWidth(140);
        column.setMaxWidth(140);

        column = apiResourceTable.getColumnModel().getColumn(3);
        column.setMinWidth(26);
        column.setWidth(26);
        column.setMaxWidth(26);
        column.setHeaderRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JLabel) tableCellRendererComponent).setIcon(KubemmanderIcons.resourceTypeWithIcon.get("namespaces"));
                ((JLabel) tableCellRendererComponent).setHorizontalAlignment(SwingConstants.CENTER);
                return tableCellRendererComponent;
            }
        });

        column = apiResourceTable.getColumnModel().getColumn(6);
        column.setMinWidth(0);
        column.setWidth(0);
        column.setMaxWidth(0);

        JPopupMenu helmReleasesPopupMenu = new JPopupMenu();
        JMenuItem helmReleasesListMenuItem = new JMenuItem("List");
        helmReleasesListMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "list", project));
        helmReleasesPopupMenu.add(helmReleasesListMenuItem);

        JMenuItem helmReleasesLoadMenuItem = new JMenuItem("Documentation");
        helmReleasesLoadMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "documentation", project));
        helmReleasesPopupMenu.add(helmReleasesLoadMenuItem);

        helmReleasesPopupMenu.add(new JSeparator());

        // Helm Diff
        JMenuItem helmReleasesDiffMenuItem = new JMenuItem("Diff Release.Revisions...");
        helmReleasesDiffMenuItem.addActionListener((ActionEvent actionEvent) -> HelmDiffAction.showDiff(project));
        helmReleasesPopupMenu.add(helmReleasesDiffMenuItem);

        JPopupMenu helmReleasePopupMenu = new JPopupMenu();
        JMenuItem helmReleaseListMenuItem = new JMenuItem("List");
        helmReleaseListMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "list", project));
        helmReleasePopupMenu.add(helmReleaseListMenuItem);

        JMenuItem helmReleaseHistoryMenuItem = new JMenuItem("History");
        helmReleaseHistoryMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "history", project));
        helmReleasePopupMenu.add(helmReleaseHistoryMenuItem);

        helmReleasePopupMenu.add(new JSeparator());

        JMenuItem helmReleaseLoadMenuItem = new JMenuItem("Get...");
        helmReleaseLoadMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "load", project));
        helmReleasePopupMenu.add(helmReleaseLoadMenuItem);

        JPopupMenu apiResourcePopupMenu = new JPopupMenu();

        JMenuItem loadApiResourceMenuItem = new JMenuItem("Load");
        loadApiResourceMenuItem.addActionListener((ActionEvent actionEvent) -> executeApiResourceActions(actionEvent, "load", project));
        apiResourcePopupMenu.add(loadApiResourceMenuItem);

        apiResourcePopupMenu.add(new JSeparator());

        JMenuItem documentationApiResourceMenuItem = new JMenuItem("Documentation");
        documentationApiResourceMenuItem.addActionListener((ActionEvent actionEvent) -> executeApiResourceActions(actionEvent, "documentation", project));
        apiResourcePopupMenu.add(documentationApiResourceMenuItem);

        JPopupMenu resourcePopupMenu = new JPopupMenu();
        JMenuItem getMenuItem = new JMenuItem("Get");
        getMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "get", project));
        resourcePopupMenu.add(getMenuItem);

        JMenuItem describeMenuItem = new JMenuItem("Describe");
        describeMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "describe", project));
        resourcePopupMenu.add(describeMenuItem);

        resourcePopupMenu.add(new JSeparator());

        JMenuItem loadMenuItem = new JMenuItem("Load");
        loadMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "load", project));
        resourcePopupMenu.add(loadMenuItem);

        resourcePopupMenu.add(new JSeparator());

        JMenuItem documentationMenuItem = new JMenuItem("Documentation");
        documentationMenuItem.addActionListener((ActionEvent actionEvent) -> execute(actionEvent, "documentation", project));
        resourcePopupMenu.add(documentationMenuItem);

        apiResourceTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable apiResourceTable = (JTable) e.getSource();
                    int row = apiResourceTable.rowAtPoint(e.getPoint());
                    int column = apiResourceTable.columnAtPoint(e.getPoint());

                    if (!apiResourceTable.isRowSelected(row)) {
                        apiResourceTable.changeSelection(row, column, false, false);
                    }
                    Object valueOfZeroColumn = apiResourceTable.getValueAt(row, 0);
                    Object valueOfSixColumn = apiResourceTable.getValueAt(row, 6);
                    if (valueOfZeroColumn instanceof String) {
                        if (valueOfSixColumn instanceof Secret) {
                            helmReleasePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        } else {
                            helmReleasesPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    } else if (valueOfZeroColumn instanceof APIResource) {
                        apiResourcePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    } else if (valueOfZeroColumn instanceof GenericKubernetesResource) {
                        resourcePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        JPanel toolBars = new JPanel(new BorderLayout());

        JPanel topToolBar = new JPanel(new BorderLayout());
        toolBars.add(topToolBar, BorderLayout.NORTH);
        topToolBar.add(new JSeparator(), BorderLayout.SOUTH);

        JPanel topLeftToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topToolBar.add(topLeftToolBar, BorderLayout.WEST);

        JLabel kubeconfigLabel = new JLabel("Kubeconfig: ");
        kubeconfigLabel.setIcon(KubemmanderIcons.ToolWindow);
        topLeftToolBar.add(kubeconfigLabel);

        kubeconfigTextField = new JTextField(15);
        kubeconfigTextField.setText("~/.kube/config");
        topLeftToolBar.add(kubeconfigTextField);

        JButton editKubeconfigButton = new JButton(AllIcons.Actions.EditSource);
        editKubeconfigButton.addActionListener((ActionEvent actionEvent) -> editKubeConfigFile(project));
        topLeftToolBar.add(editKubeconfigButton);

        connectToClusterButton = new JButton(AllIcons.Actions.Execute);
        connectToClusterButton.setToolTipText("Connect to Cluster");
        connectToClusterButton.addActionListener(this::connectToCluster);
        topLeftToolBar.add(connectToClusterButton);

        disconnectFromCusterButton = new JButton(AllIcons.Actions.Exit);
        disconnectFromCusterButton.setToolTipText("Disconnect from Cluster");
        disconnectFromCusterButton.addActionListener(this::disconnectFromCluster);
        topLeftToolBar.add(disconnectFromCusterButton);

        loadingResourcesLabel = new JLabel("Loading resources...");
        topLeftToolBar.add(loadingResourcesLabel);
        loadingResourcesLabel.setVisible(false);

        JPanel topRightToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topToolBar.add(topRightToolBar, BorderLayout.EAST);

        topRightToolBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));

        reconnectToCusterButton = new JButton(AllIcons.Actions.Refresh);
        reconnectToCusterButton.setToolTipText("Reconnect to Cluster - selected namespace will be used to filter resources");
        reconnectToCusterButton.addActionListener((ActionEvent actionEvent) -> disconnectFromCluster(actionEvent, true));
        topRightToolBar.add(reconnectToCusterButton);

        JLabel includeLabel = new JLabel("Include: ");
        topRightToolBar.add(includeLabel);

        includeInstancesCheckBox = new JCheckBox("Instances");
        includeInstancesCheckBox.setSelected(false);
        topRightToolBar.add(includeInstancesCheckBox);

        includeNonNamespacedCheckBox = new JCheckBox("Non Namespaced");
        includeNonNamespacedCheckBox.setSelected(true);
        topRightToolBar.add(includeNonNamespacedCheckBox);

        topRightToolBar.add(new JLabel(" | "));

        includeNamespacedCheckBox = new JCheckBox("Namespaced");
        includeNamespacedCheckBox.setSelected(true);
        topRightToolBar.add(includeNamespacedCheckBox);

        JLabel fromLabel = new JLabel("resources from namespaces:");
        topRightToolBar.add(fromLabel);

        allNamespacesCheckBox = new JCheckBox("All");
        allNamespacesCheckBox.setSelected(true);
        topRightToolBar.add(allNamespacesCheckBox);

        JLabel allOrSelectedNamespacesLabel = new JLabel("| Selected:");
        topRightToolBar.add(allOrSelectedNamespacesLabel);

        selectedNamespacesPopupMenu = new JPopupMenu();
        selectedNamespacesMenuButton = new KubemmanderMenuButton("Namespaces", selectedNamespacesPopupMenu);
        topRightToolBar.add(selectedNamespacesMenuButton);

        allNamespacesCheckBox.addActionListener(this::adjustSelectedNamespacesComboBoxState);
        adjustSelectedNamespacesComboBoxState(null);

        JPanel bottomToolBar = new JPanel(new BorderLayout());
        toolBars.add(bottomToolBar, BorderLayout.SOUTH);
        bottomToolBar.add(new JSeparator(), BorderLayout.SOUTH);

        JPanel bottomLeftToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomToolBar.add(bottomLeftToolBar, BorderLayout.WEST);

        JPanel bottomRightToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomToolBar.add(bottomRightToolBar, BorderLayout.EAST);

        JLabel serverVersionLabel = new JLabel("Server version: ");
        bottomRightToolBar.add(serverVersionLabel);

        serverVersion = new JLabel("0000");
        bottomRightToolBar.add(serverVersion);

        JLabel contextLabel = new JLabel("Context: ");
        bottomRightToolBar.add(contextLabel);

        contextComboBox = new ComboBox<>(new DefaultComboBoxModel<>());
        contextComboBox.setMinimumAndPreferredWidth(200);
        bottomRightToolBar.add(contextComboBox);

        contextComboBox.addItemListener((ItemEvent itemEvent) -> {
            if (addingContexts || addingNamespaces) {
                return;
            };
            if (kubernetesClient != null) {
                if (contextComboBox.getSelectedItem() instanceof String contextName) {
                    Config configuration = kubernetesClient.getConfiguration();
                    configuration.getContexts().forEach((context) -> {
                        if (contextName.equals(context.getName())) {
                            configuration.setCurrentContext(context);
                            kubernetesClient.close();
                            disconnectFromCluster(null, true);
                        }
                    });
                }
            }
        });

        JLabel namespaceLabel = new JLabel("Namespace: ");
        bottomRightToolBar.add(namespaceLabel);

        namespaceComboBox = new ComboBox<>(new DefaultComboBoxModel<>());
        namespaceComboBox.setMinimumAndPreferredWidth(200);
        bottomRightToolBar.add(namespaceComboBox);

        namespaceComboBox.addItemListener((ItemEvent itemEvent) -> {
            if (addingContexts || addingNamespaces) {
                return;
            };
            if (kubernetesClient != null) {
                if (namespaceComboBox.getSelectedItem() instanceof String namespaceName) {
                    Config configuration = kubernetesClient.getConfiguration();
                    configuration.setNamespace(namespaceName);
                    kubernetesClient.close();
                    disconnectFromCluster(null, true);
                }
            }
        });

        this.contentToolWindow.add(toolBars, BorderLayout.NORTH);

        connectToCluster(null);
    }

    private void execute(ActionEvent actionEvent, String operation, Project project) {
        if (actionEvent.getSource() instanceof JMenuItem menuItem) {
            JPopupMenu popup = (JPopupMenu) menuItem.getParent();
            if (popup.getInvoker() instanceof JTable table) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    Object valueOfZeroColumn = table.getValueAt(selectedRow, 0);
                    Object valueOfSixthColumn = table.getValueAt(selectedRow, 6);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            switch (operation) {
                                case "list":
                                    if (valueOfSixthColumn instanceof Secret secret) {
                                        // Helm release list
                                        @NotNull ShellTerminalWidget shellTerminalWidget =
                                                TerminalToolWindowManager
                                                        .getInstance(Objects.requireNonNull(project))
                                                        .createLocalShellWidget(project.getBasePath(), "helm", true, true);
                                        try {
                                            shellTerminalWidget.executeCommand(
                                                    "helm list -n " + secret.getMetadata().getNamespace());
                                        } catch (IOException exception) {
                                            // Show error dialog
                                            kubemmanderNotificationGroup
                                                    .createNotification(
                                                            exception.getMessage()
                                                            ,NotificationType.ERROR)
                                                    .notify(project);
                                        }
                                    } else if (valueOfZeroColumn instanceof String) {
                                        // Helm release list
                                        @NotNull ShellTerminalWidget shellTerminalWidget =
                                                TerminalToolWindowManager
                                                        .getInstance(Objects.requireNonNull(project))
                                                        .createLocalShellWidget(project.getBasePath(), "helm", true, true);
                                        try {
                                            shellTerminalWidget.executeCommand(
                                                    "helm list -A");
                                        } catch (IOException exception) {
                                            // Show error dialog
                                            kubemmanderNotificationGroup
                                                    .createNotification(
                                                            exception.getMessage()
                                                            ,NotificationType.ERROR)
                                                    .notify(project);
                                        }
                                    }
                                    return;
                                case "history":
                                    if (valueOfSixthColumn instanceof Secret secret) {
                                        // Helm release list
                                        @NotNull ShellTerminalWidget shellTerminalWidget =
                                                TerminalToolWindowManager
                                                        .getInstance(Objects.requireNonNull(project))
                                                        .createLocalShellWidget(project.getBasePath(), "helm", true, true);
                                        try {
                                            shellTerminalWidget.executeCommand(
                                                    "helm history -n " + secret.getMetadata().getNamespace() + " " + valueOfZeroColumn);
                                        } catch (IOException exception) {
                                            // Show error dialog
                                            kubemmanderNotificationGroup
                                                    .createNotification(
                                                            exception.getMessage()
                                                            ,NotificationType.ERROR)
                                                    .notify(project);
                                        }
                                    }
                                    return;
                                case "get":
                                case "describe":
                                    if (valueOfZeroColumn instanceof GenericKubernetesResource genericKubernetesResource) {
                                        APIResource apiResource = (APIResource) valueOfSixthColumn;
                                        @NotNull ShellTerminalWidget shellTerminalWidget =
                                                TerminalToolWindowManager
                                                        .getInstance(Objects.requireNonNull(project))
                                                        .createLocalShellWidget(project.getBasePath(), "kubectl", true, true);
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
                                    return;
                                case "load":
                                    if (valueOfSixthColumn instanceof Secret secret) {
                                        DialogBuilder builder = new DialogBuilder(project);
                                        builder.setCenterPanel(whatPanel);
                                        builder.setDimensionServiceKey("SelectWhat");
                                        builder.setTitle("Select");
                                        builder.removeAllActions();

                                        builder.addCancelAction();

                                        builder.addOkAction();
                                        builder.setOkOperation(() -> {
                                            if (whatPanel.isAny()) {
                                                builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
                                            } else {
                                                Messages.showMessageDialog(
                                                        project,
                                                        "Please select at least one of chart info, values, templates, manifests, hooks, notes for get",
                                                        "Select at Least One for Get",
                                                        Messages.getInformationIcon());
                                            }
                                        });

                                        try {
                                            String release = (String) table.getValueAt(selectedRow, 1);
                                            String revision = (String) table.getValueAt(selectedRow, 2);
                                            whatPanel.setTitleLabel(release, revision, secret.getMetadata().getNamespace());
                                            boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
                                            if (isOk) {
                                                if (whatPanel.isAny()) {
                                                    Namespace namespace = namespacenameToNamespaceMap.get(secret.getMetadata().getNamespace());
                                                    if (namespace != null) {
                                                        showReleaseRevision(project,
                                                                new NamespaceSecretReleaseRevision(
                                                                        namespace,
                                                                        secret,
                                                                        release,
                                                                        revision),
                                                                whatPanel);
                                                    }
                                                }
                                            }
                                        } finally {
                                        }
                                    } else if (valueOfZeroColumn instanceof GenericKubernetesResource genericKubernetesResource) {
                                        APIResource apiResource = (APIResource) valueOfSixthColumn;
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
                                    }
                                    return;
                                case "documentation":
                                    if (valueOfSixthColumn instanceof APIResource apiResource) {
                                        KubemmanderExplain.explain(apiResource.getName());
                                    } else if (valueOfSixthColumn instanceof String stringValueOfSixthColumn) {
                                        KubemmanderExplain.explain(stringValueOfSixthColumn);
                                    }
                            }
                        });
                    });
                }
            }
        }
    }

    private void executeApiResourceActions(ActionEvent actionEvent, String operation, Project project) {
        if (actionEvent.getSource() instanceof JMenuItem menuItem) {
            JPopupMenu popup = (JPopupMenu) menuItem.getParent();
            if (popup.getInvoker() instanceof JTable table) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ApplicationManager.getApplication().runReadAction(() -> {
                            Object valueOfZeroColumn = table.getValueAt(selectedRow, 0);
                            switch (operation) {
                                case "documentation":
                                    if (valueOfZeroColumn instanceof APIResource apiResource) {
                                        KubemmanderExplain.explain(apiResource.getName());
                                    } else if (valueOfZeroColumn instanceof String stringValueOfZeroColumn) {
                                        KubemmanderExplain.explain(stringValueOfZeroColumn);
                                    }
                                    return;
                                case "load":
                                    if (valueOfZeroColumn instanceof APIResource apiResource) {
                                        List<String> kubectlCommand = new LinkedList<>();
                                        kubectlCommand.add("kubectl");
                                        kubectlCommand.add("explain");
                                        kubectlCommand.add(apiResource.getName());
//                                        kubectlCommand.add("--api-version=" + apiResource.getVersion());
                                        kubectlCommand.add("--recursive=true");
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
                                                        LightVirtualFile lightVirtualFile = new LightVirtualFile(
                                                                apiResource.getName() + ".yaml"
                                                                ,fileType
                                                                ,"# " + kubectlCommand.stream().collect(Collectors.joining(" ")) + "\n" + kubectlProcessOutput[0]);
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
                                    }
                            }
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
        }
    }

    private void connectToCluster(ActionEvent actionEvent) {
        loadingResourcesLabel.setVisible(true);
        apiResourceTable.setPaintBusy(true);
        contextComboBox.removeAllItems();
        namespaceComboBox.removeAllItems();
        namespacenameToNamespaceMap.clear();
        selectedNamespacesPopupMenu.removeAll();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                try (KubernetesClient client = new KubernetesClientBuilder().build()) {
                    kubernetesClient = client;
                    VersionInfo kubernetesVersion = kubernetesClient.getKubernetesVersion();
                    serverVersion.setText(kubernetesVersion.getMajor() + "." + kubernetesVersion.getMinor());
                    Config configuration = client.getConfiguration();

                    addingContexts = true;
                    addingNamespaces = true;
                    try {
                        configuration.getContexts().forEach((context) -> {
                            contextComboBox.addItem(context.getName());
                        });

                        kubernetesClient
                                .namespaces()
                                .list(new ListOptionsBuilder().withKind("namespace").build()).getItems().forEach((Namespace namespace) -> {
                                    namespaceComboBox.addItem(namespace.getMetadata().getName());
                                    namespacenameToNamespaceMap.put(namespace.getMetadata().getName(), namespace);
                                    JCheckBoxMenuItem checkBoxMenuItem =
                                            new JCheckBoxMenuItem(namespace.getMetadata().getName());
                                    checkBoxMenuItem.addActionListener((ActionEvent actionEvent1) -> {
                                       if (checkBoxMenuItem.isSelected()) {
                                           selectedNamespaces.add(namespace.getMetadata().getName());
                                       } else {
                                           selectedNamespaces.remove(namespace.getMetadata().getName());
                                       }
                                    });
                                    selectedNamespacesPopupMenu.add(checkBoxMenuItem);
                                });
                        if (configuration.isDefaultNamespace()) {
                            String namespace = configuration.getNamespace();
                            if (namespace != null) {
                                int componentCount = selectedNamespacesPopupMenu.getComponentCount();
                                for (int i = 0; i < componentCount; i++) {
                                    Component component = selectedNamespacesPopupMenu.getComponent(i);
                                    if (component instanceof JCheckBoxMenuItem checkBoxMenuItem) {
                                        checkBoxMenuItem.setSelected(namespace.equals(checkBoxMenuItem.getText()));
                                    }
                                }
                            }
                        }
                        if (!selectedNamespaces.isEmpty()) {
                            int componentCount = selectedNamespacesPopupMenu.getComponentCount();
                            for (int i = 0; i < componentCount; i++) {
                                Component component = selectedNamespacesPopupMenu.getComponent(i);
                                if (component instanceof JCheckBoxMenuItem checkBoxMenuItem) {
                                    if (selectedNamespaces.contains(checkBoxMenuItem.getText())) {
                                        checkBoxMenuItem.setSelected(true);
                                    }
                                }
                            }
                        }
                    } finally {
                        addingContexts = false;
                        addingNamespaces = false;
                    }

                    Set<APIResource> apiResourceSet =
                            new TreeSet<>(Comparator.comparing(APIResource::getName));
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
                                        "helmrels",
                                        "",
                                        "✔",
                                        "",
                                        "Helmreleases ( helmreleases )",
                                        "helmreleases"
                                });
                        // Add Helm releases
                        if (includeInstancesCheckBox.isSelected()) {
                            kubernetesClient.secrets().list().getItems().forEach((Secret secret) -> {
                                String secretName = secret.getMetadata().getName();
                                Matcher helmSecretNamematcher = HELM_SECRET_NAME_PATTERN.matcher(secretName);
                                if (helmSecretNamematcher.matches()) {
                                    String releaseName = helmSecretNamematcher.group(1);
                                    String releaseRevision = helmSecretNamematcher.group(2);
                                    if (allNamespacesCheckBox.isSelected() || (!selectedNamespaces.isEmpty() && selectedNamespaces.contains(secret.getMetadata().getNamespace()))) {
                                        apiResourceTableModel.addRow(
                                            new Object[]{
                                                    releaseName,
                                                    "helmrel",
                                                    releaseRevision,
                                                    "✔",
                                                    secret.getMetadata().getNamespace(),
                                                    "Helmrelease ( helmrelease )",
                                                    secret
                                            });
                                    }
                                }
                            });
                        }
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
                        if (includeInstancesCheckBox.isSelected() && !apiResource.getName().contains("/")) {
                            try {
                                ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                                        .withKind(apiResource.getKind())
                                        .build();
                                kubernetesClient
                                        .genericKubernetesResources(resourceDefinitionContext)
                                        .list()
                                        .getItems().forEach((GenericKubernetesResource genericKubernetesResource) -> {
                                            boolean doAddResourceRow = false;
                                            if (!apiResource.getNamespaced()) {
                                                doAddResourceRow = true;
                                            } else {
                                                if (allNamespacesCheckBox.isSelected() || (!selectedNamespaces.isEmpty() && selectedNamespaces.contains(genericKubernetesResource.getMetadata().getNamespace()))) {
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
                    selectedNamespaces.clear();
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
                    if (allNamespacesCheckBox.isSelected()) {
                        selectedNamespaces.clear();
                    } else {
                        int componentCount = selectedNamespacesPopupMenu.getComponentCount();
                        for (int i = 0; i < componentCount; i++) {
                            Component component = selectedNamespacesPopupMenu.getComponent(i);
                            if (component instanceof JCheckBoxMenuItem checkBoxMenuItem) {
                                if (checkBoxMenuItem.isSelected()) {
                                    selectedNamespaces.add(checkBoxMenuItem.getText());
                                }
                            }
                        }
                    }
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
        if (allNamespacesCheckBox.isSelected()) {
            selectedNamespaces.clear();
        }
        selectedNamespacesMenuButton.setEnabled(!allNamespacesCheckBox.isSelected());
    }

    private void adjustStates() {
        connectToClusterButton.setVisible(kubernetesClient == null);
        disconnectFromCusterButton.setVisible(kubernetesClient != null);
        reconnectToCusterButton.setEnabled(kubernetesClient != null);
        contextComboBox.setEnabled(kubernetesClient != null);
        namespaceComboBox.setEnabled(kubernetesClient != null);
        allNamespacesCheckBox.setEnabled(kubernetesClient != null);
        selectedNamespacesMenuButton.setEnabled(kubernetesClient != null && !allNamespacesCheckBox.isSelected());
        if (kubernetesClient == null) {
            contextComboBox.removeAllItems();
            namespaceComboBox.removeAllItems();
            namespacenameToNamespaceMap.clear();
            selectedNamespacesPopupMenu.removeAll();
            serverVersion.setText("");
            apiResourceTableModel.setRowCount(0);
        }
    }

    public JComponent getContent() {
        return this.contentToolWindow;
    }

    private static void showHelmDiff(Project project) {
    }

    private static void showReleaseRevision(Project project,
                                            NamespaceSecretReleaseRevision namespaceSecretStringStringNamespaceSecretReleaseRevision,
                                            WhatPanel whatPanel) {
        FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManager.getInstance(project);

        EditorWindow currentWindow = fileEditorManager.getCurrentWindow();
        if (currentWindow != null) {
            fileEditorManager.createSplitter(JSplitPane.VERTICAL_SPLIT, currentWindow);
            currentWindow = fileEditorManager.getCurrentWindow();
        }

        HelmReleaseRevisionAccessor helmReleaseRevisionAccessor = new HelmReleaseRevisionAccessor(namespaceSecretStringStringNamespaceSecretReleaseRevision);
        String title = helmReleaseRevisionAccessor.getTitle();

        // Chart Info
        if (whatPanel.isChartInfo()) {
            LightVirtualFile charInfoLightVirtualFile = new LightVirtualFile(Constants.CHART_INFO + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getChartInfo());
            charInfoLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            charInfoLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(charInfoLightVirtualFile, true, true);
        }

        // Values
        if (whatPanel.isValues()) {
            LightVirtualFile valuesLightVirtualFile = new LightVirtualFile(Constants.VALUES + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getValues());
            valuesLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            valuesLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(valuesLightVirtualFile, true, true);
        }

        // Templates
        if (whatPanel.isTemplates()) {
            LightVirtualFile templatesvaluesLightVirtualFile = new LightVirtualFile(Constants.TEMPLATES + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getTemplates());
            templatesvaluesLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            templatesvaluesLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(templatesvaluesLightVirtualFile, true, true);
        }

        // Manifest
        if (whatPanel.isManifests()) {
            LightVirtualFile manifestLightVirtualFile = new LightVirtualFile(Constants.MANIFESTS + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getManifests());
            manifestLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            manifestLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(manifestLightVirtualFile, true, true);
        }

        // Hooks
        if (whatPanel.isHooks()) {
            LightVirtualFile hooksLightVirtualFile = new LightVirtualFile(Constants.HOOKS + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getHooks());
            hooksLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            hooksLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(hooksLightVirtualFile, true, true);
        }

        // Notes
        if (whatPanel.isNotes()) {
            LightVirtualFile notesvaluesLightVirtualFile = new LightVirtualFile(Constants.NOTES + title,
                    PlainTextFileType.INSTANCE,
                    helmReleaseRevisionAccessor.getNotes());
            notesvaluesLightVirtualFile.setWritable(false);
            // Figure out a way to set language for syntax highlighting based on file extension
            notesvaluesLightVirtualFile.setLanguage(PlainTextLanguage.INSTANCE);
            fileEditorManager.openFile(notesvaluesLightVirtualFile, true, true);
        }
    }

    private static class ResourceTableCellRenderer extends DefaultTableCellRenderer {
        private final Icon FILE_ICON = UIManager.getIcon("Tree.leafIcon");
        private final Icon DIRECTORY_ICON = UIManager.getIcon("Tree.closedIcon");

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object valueInColumnZero = table.getModel().getValueAt(row, 0);
            Object valueInColumnOne = table.getModel().getValueAt(row, 1);
            Object valueInColumnSix = table.getModel().getValueAt(row, 6);

            Icon icon = null;
            if (column == 0) {
                if (valueInColumnSix instanceof APIResource apiResource) {
                    icon = KubemmanderIcons.resourceTypeWithIcon.get(apiResource.getName());
                } else if (valueInColumnSix instanceof String stringValue) {
                    icon = KubemmanderIcons.resourceTypeWithIcon.get(stringValue);
                } else if (valueInColumnSix instanceof Secret && "helmrel".equals(valueInColumnOne)) {
                    icon = KubemmanderIcons.resourceTypeWithIcon.get("helmreleases");
                }
            }

            if (value instanceof APIResource apiResource) {
                value = apiResource.getName();
            } else if (value instanceof GenericKubernetesResource genericKubernetesResource) {
                value = genericKubernetesResource.getMetadata().getName();
            }

            Component cellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (cellRendererComponent instanceof JLabel labelCellRendererComponent) {
                if (column == 3) {
                    labelCellRendererComponent.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    labelCellRendererComponent.setHorizontalAlignment(SwingConstants.LEFT);
                }
                labelCellRendererComponent.setIcon(null);
                if (column == 0) {
                    if (icon == null) {
                        if (valueInColumnZero instanceof APIResource) {
                            icon = DIRECTORY_ICON;
                        } else if (valueInColumnZero instanceof GenericKubernetesResource || valueInColumnSix instanceof Secret) {
                            icon = FILE_ICON;
                        }
                    }
                    labelCellRendererComponent.setIcon(icon);
                    if (valueInColumnZero instanceof GenericKubernetesResource) {
                        labelCellRendererComponent.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                    } else if (valueInColumnSix instanceof Secret && "helmrel".equals(valueInColumnOne)) {
                        labelCellRendererComponent.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
                    }
                }
            }

            return cellRendererComponent;
        }
    }
}