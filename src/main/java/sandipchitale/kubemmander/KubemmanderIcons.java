package sandipchitale.kubemmander;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.icons.CachedImageIcon;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class KubemmanderIcons {

    static Map<String, Icon> resourceTypeWithIcon = new HashMap<>();
    static {
        resourceTypeWithIcon.put("clusterroles", IconLoader.getIcon("/icons/c-role-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("configmaps", IconLoader.getIcon("/icons/cm-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("crb", IconLoader.getIcon("/icons/crb-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("customresourcedefinitions", IconLoader.getIcon("/icons/crd-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("cronjobs", IconLoader.getIcon("/icons/cronjob-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("deployments", IconLoader.getIcon("/icons/deploy-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("daemonsets", IconLoader.getIcon("/icons/ds-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("endpoints", IconLoader.getIcon("/icons/ep-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("group", IconLoader.getIcon("/icons/group-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("helmreleases", IconLoader.getIcon("/icons/helm-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("horizontalpodautoscalers", IconLoader.getIcon("/icons/hpa-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("ingresses", IconLoader.getIcon("/icons/ing-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("jobs", IconLoader.getIcon("/icons/job-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("limitranges", IconLoader.getIcon("/icons/limits-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("networkpolicies", IconLoader.getIcon("/icons/netpol-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("namespaces", IconLoader.getIcon("/icons/ns-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("nodes", IconLoader.getIcon("/icons/node-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("pods", IconLoader.getIcon("/icons/pod-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("psp", IconLoader.getIcon("/icons/psp-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("persistentvolumes", IconLoader.getIcon("/icons/pv-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("persistentvolumeclaims", IconLoader.getIcon("/icons/pvc-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("resourcequotas", IconLoader.getIcon("/icons/quota-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("rolebindings", IconLoader.getIcon("/icons/rb-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("roles", IconLoader.getIcon("/icons/role-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("replicasets", IconLoader.getIcon("/icons/rs-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("serviceaccounts", IconLoader.getIcon("/icons/sa-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("storageclasses", IconLoader.getIcon("/icons/sc-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("secrets", IconLoader.getIcon("/icons/secret-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("statefulsets", IconLoader.getIcon("/icons/sts-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("services", IconLoader.getIcon("/icons/svc-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("user", IconLoader.getIcon("/icons/user-128.png", KubemmanderIcons.class));
        resourceTypeWithIcon.put("volumeattachments", IconLoader.getIcon("/icons/vol-128.png", KubemmanderIcons.class));

        resourceTypeWithIcon.entrySet().forEach((Map.Entry<String, Icon> entry) -> {
            if (entry.getValue() instanceof ScalableIcon cachedImageIcon) {
                entry.setValue(cachedImageIcon.scale(0.125f));
            }
        });
    }

    public static Icon ToolWindow = IconLoader.getIcon("/icons/kubemmander.svg", KubemmanderIcons.class);
}
