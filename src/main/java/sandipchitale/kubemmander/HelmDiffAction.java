package sandipchitale.kubemmander;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Set;

public class HelmDiffAction {
    private static final WhatPanel whatPanel = WhatPanel.build(BorderLayout.EAST);

    private static final JBList<NamespaceSecretReleaseRevision> namespaceSecretReleaseRevisionList1 = new JBList<>();
    private static final JBList<NamespaceSecretReleaseRevision> namespaceSecretReleaseRevisionList2 = new JBList<>();

    static {
        JPanel splitPane = new JPanel(new GridLayout(1, 2, 5, 5));

        namespaceSecretReleaseRevisionList1.setCellRenderer(ReleaseRevisionNamespaceDefaultListCellRenderer.INSTANCE);
        namespaceSecretReleaseRevisionList1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        splitPane.add(new JScrollPane(namespaceSecretReleaseRevisionList1));

        namespaceSecretReleaseRevisionList2.setCellRenderer(ReleaseRevisionNamespaceDefaultListCellRenderer.INSTANCE);
        namespaceSecretReleaseRevisionList2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        splitPane.add(new JScrollPane(namespaceSecretReleaseRevisionList2));

        whatPanel.add(splitPane, BorderLayout.CENTER);
    }

    public static void showDiff(Project project) {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet =
                HelmReleaseRevisionSecretsAccessor.getNamespaceSecretReleaseRevisionSetAllNamespaces();

        namespaceSecretReleaseRevisionList1.setListData(namespaceStringStringNamespaceSecretReleaseRevisionSet.toArray(new NamespaceSecretReleaseRevision[0]));
        namespaceSecretReleaseRevisionList2.setListData(namespaceStringStringNamespaceSecretReleaseRevisionSet.toArray(new NamespaceSecretReleaseRevision[0]));

        DialogBuilder builder = new DialogBuilder(project);

        builder.setCenterPanel(whatPanel);
        builder.setDimensionServiceKey("SelectNamespaceHelmReleaseRevisionForDiff");
        builder.setTitle("Select Helm Release.Revisions [ Namespaces ] for Diff");
        builder.removeAllActions();

        builder.addCancelAction();

        builder.addOkAction();
        builder.setOkActionEnabled(false);
        builder.setOkOperation(() -> {
            NamespaceSecretReleaseRevision selectedValue1 = namespaceSecretReleaseRevisionList1.getSelectedValue();
            NamespaceSecretReleaseRevision selectedValue2 = namespaceSecretReleaseRevisionList2.getSelectedValue();
            if (selectedValue1.equals(selectedValue2)) {
                Messages.showMessageDialog(
                        project,
                        "Please select different Release.Revision for diff",
                        "Select Different Release.Revisions for Diff",
                        Messages.getInformationIcon());
                return;
            }
            if (whatPanel.isAny()) {
                builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
            } else {
                Messages.showMessageDialog(
                        project,
                        "Please select at least one of chart info, values, templates, manifests, hooks, notes for diff",
                        "Select at Least One for Diff",
                        Messages.getInformationIcon());
            }
        });

        ListSelectionListener adjustOkActionState = e1 -> {
            builder.setOkActionEnabled(
                    namespaceSecretReleaseRevisionList1.getSelectedValue() != null
                    && namespaceSecretReleaseRevisionList2.getSelectedValue() != null);
        };

        try {
            namespaceSecretReleaseRevisionList1.addListSelectionListener(adjustOkActionState);
            namespaceSecretReleaseRevisionList2.addListSelectionListener(adjustOkActionState);

            whatPanel.setTitleLabel("What To Diff");
            boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
            if (isOk) {
                NamespaceSecretReleaseRevision selectedValue1 = namespaceSecretReleaseRevisionList1.getSelectedValue();
                NamespaceSecretReleaseRevision selectedValue2 = namespaceSecretReleaseRevisionList2.getSelectedValue();
                if (selectedValue1 != null && selectedValue2 != null) {
                    showReleaseRevisionDiff(project, selectedValue1, selectedValue2);
                }
            }
        } finally {
            // Remove listeners
            namespaceSecretReleaseRevisionList1.removeListSelectionListener(adjustOkActionState);
            namespaceSecretReleaseRevisionList2.removeListSelectionListener(adjustOkActionState);
        }
    }

    private static void showReleaseRevisionDiff(Project project,
                                                NamespaceSecretReleaseRevision namespaceSecretStringStringNamespaceSecretReleaseRevision1,
                                                NamespaceSecretReleaseRevision namespaceSecretStringStringNamespaceSecretReleaseRevision2) {

        FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManager.getInstance(project);

        HelmReleaseRevisionAccessor helmReleaseRevisionAccessor1 = new HelmReleaseRevisionAccessor(namespaceSecretStringStringNamespaceSecretReleaseRevision1);
        String title1 = helmReleaseRevisionAccessor1.getTitle();

        HelmReleaseRevisionAccessor helmReleaseRevisionAccessor2 = new HelmReleaseRevisionAccessor(namespaceSecretStringStringNamespaceSecretReleaseRevision2);
        String title2 = helmReleaseRevisionAccessor2.getTitle();

        EditorWindow currentWindow = fileEditorManager.getCurrentWindow();
        fileEditorManager.createSplitter(JSplitPane.VERTICAL_SPLIT, currentWindow);

        DiffManager diffManager = DiffManager.getInstance();
        DiffContentFactory diffContentFactory = DiffContentFactory.getInstance();

        // Chart Info diff
        if (whatPanel.isChartInfo()) {
            FileType fileType = FileTypeUtils.getFileType("YAML");
            DiffContent chartInfoContent1 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.CHART_INFO + title1,
                    helmReleaseRevisionAccessor1.getChartInfo(),
                    fileType);
            DiffContent chartInfoContent2 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.CHART_INFO + title2,
                    helmReleaseRevisionAccessor2.getChartInfo(),
                    fileType);
            chartInfoContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            chartInfoContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest chartInfoDiffRequest = new SimpleDiffRequest(
                    Constants.CHART_INFO + title1 + " vs " + Constants.CHART_INFO + title2,
                    chartInfoContent1,
                    chartInfoContent2,
                    Constants.CHART_INFO + title1,
                    Constants.CHART_INFO + title2);
            diffManager.showDiff(project, chartInfoDiffRequest);
        }

        // Values diff
        if (whatPanel.isValues()) {
            FileType fileType = FileTypeUtils.getFileType("JSON");
            DiffContent valuesContent1 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.VALUES + title1,
                    helmReleaseRevisionAccessor1.getValues(),
                    fileType);
            DiffContent valuesContent2 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.VALUES + title2,
                    helmReleaseRevisionAccessor2.getValues(),
                    fileType);
            valuesContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            valuesContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest valuesDiffRequest = new SimpleDiffRequest(Constants.VALUES + title1 + " vs " + Constants.VALUES + title2,
                    valuesContent1,
                    valuesContent2,
                    Constants.VALUES + title1,
                    Constants.VALUES + title2);
            diffManager.showDiff(project, valuesDiffRequest);
        }

        // Templates diff
        if (whatPanel.isTemplates()) {
            FileType fileType = FileTypeUtils.getFileType("Helm YAML template", "YAML");
            DiffContent templatesContent1 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.TEMPLATES + title1,
                    helmReleaseRevisionAccessor1.getTemplates(),
                    fileType);
            DiffContent templatesContent2 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.TEMPLATES + title2,
                    helmReleaseRevisionAccessor2.getTemplates(),
                    fileType);
            templatesContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            templatesContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest templatesDiffRequest = new SimpleDiffRequest(Constants.TEMPLATES + title1 + " vs " + Constants.TEMPLATES + title2,
                    templatesContent1,
                    templatesContent2,
                    Constants.TEMPLATES + title1,
                    Constants.TEMPLATES + title2);
            diffManager.showDiff(project, templatesDiffRequest);
        }

        // Manifests diff
        if (whatPanel.isManifests()) {
            FileType fileType = FileTypeUtils.getFileType("YAML");
            DiffContent manifestsContent1 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.MANIFESTS + title1,
                    helmReleaseRevisionAccessor1.getManifests(),
                    fileType);
            DiffContent manifestsContent2 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.MANIFESTS + title2,
                    helmReleaseRevisionAccessor2.getManifests(),
                    fileType);
            manifestsContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            manifestsContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest manifestsDiffsRequest = new SimpleDiffRequest(Constants.MANIFESTS + title1 + " vs " + Constants.MANIFESTS + title2,
                    manifestsContent1,
                    manifestsContent2,
                    Constants.MANIFESTS + title1,
                    Constants.MANIFESTS + title2);
            diffManager.showDiff(project, manifestsDiffsRequest);
        }

        // Hooks diffs
        if (whatPanel.isHooks()) {
            FileType fileType = FileTypeUtils.getFileType("YAML");
            DiffContent hooksContent1 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.HOOKS + title1,
                    helmReleaseRevisionAccessor1.getHooks(),
                    fileType);
            DiffContent hooksContent2 = DiffUtils.createDiffContent(diffContentFactory,
                    project,
                    Constants.HOOKS + title2,
                    helmReleaseRevisionAccessor2.getHooks(),
                    fileType);
            hooksContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            hooksContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest hooksDiffRequest = new SimpleDiffRequest(Constants.HOOKS + title1 + " vs " + Constants.HOOKS + title2,
                    hooksContent1,
                    hooksContent2,
                    Constants.HOOKS + title1,
                    Constants.HOOKS + title2);
            diffManager.showDiff(project, hooksDiffRequest);
        }

        // Notes diffs
        if (whatPanel.isNotes()) {
            DiffContent notesContent1 = diffContentFactory.create(project,
                    helmReleaseRevisionAccessor1.getNotes());
            DiffContent notesContent2 = diffContentFactory.create(project,
                    helmReleaseRevisionAccessor2.getNotes());
            notesContent1.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            notesContent2.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            SimpleDiffRequest notesDiffRequest = new SimpleDiffRequest(Constants.NOTES + title1 + " vs " + Constants.NOTES + title2,
                    notesContent1,
                    notesContent2,
                    Constants.NOTES + title1,
                    Constants.NOTES + title2);
            diffManager.showDiff(project, notesDiffRequest);
        }
    }

}
