package sandipchitale.kubemmander;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

class KubemmanderExplain {
    private static final String DOC_URL_PREFIX = "https://kubernetes.io/docs/reference/kubernetes-api/";

    private static final Map<String, String> DOC_URL_SUFFIX = new LinkedHashMap<>();
    static {
        DOC_URL_SUFFIX.put("helmreleases", "https://helm.sh/docs/chart_template_guide/getting_started/");
        DOC_URL_SUFFIX.put("apiservices", "cluster-resources/api-service-v1");
        DOC_URL_SUFFIX.put("bindings", "cluster-resources/binding-v1");
        DOC_URL_SUFFIX.put("certificatesigningrequests", "authentication-resources/certificate-signing-request-v1");
        DOC_URL_SUFFIX.put("clusterrolebindings", "authorization-resources/cluster-role-binding-v1");
        DOC_URL_SUFFIX.put("clusterroles", "authorization-resources/cluster-role-v1");
        DOC_URL_SUFFIX.put("componentstatuses", "cluster-resources/component-status-v1");
        DOC_URL_SUFFIX.put("configmaps", "config-and-storage-resources/config-map-v1");
        DOC_URL_SUFFIX.put("controllerrevisions", "workload-resources/controller-revision-v1");
        DOC_URL_SUFFIX.put("cronjobs", "workload-resources/cron-job-v1");
        DOC_URL_SUFFIX.put("csidrivers", "config-and-storage-resources/csi-driver-v1");
        DOC_URL_SUFFIX.put("csinodes", "config-and-storage-resources/csi-node-v1");
        DOC_URL_SUFFIX.put("customresourcedefinitions", "extend-resources/custom-resource-definition-v1");
        DOC_URL_SUFFIX.put("daemonsets", "workload-resources/daemon-set-v1");
        DOC_URL_SUFFIX.put("deployments", "workload-resources/deployment-v1");
        DOC_URL_SUFFIX.put("endpoints", "service-resources/endpoints-v1");
        DOC_URL_SUFFIX.put("endpointslices", "service-resources/endpoint-slice-v1");
        DOC_URL_SUFFIX.put("events", "cluster-resources/event-v1");
        DOC_URL_SUFFIX.put("horizontalpodautoscalers", "workload-resources/horizontal-pod-autoscaler-v1");
        DOC_URL_SUFFIX.put("ingressclasses", "service-resources/ingress-class-v1");
        DOC_URL_SUFFIX.put("ingresses", "service-resources/ingress-v1");
        DOC_URL_SUFFIX.put("jobs", "workload-resources/job-v1");
        DOC_URL_SUFFIX.put("leases", "cluster-resources/lease-v1");
        DOC_URL_SUFFIX.put("limitranges", "policy-resources/limit-range-v1");
        DOC_URL_SUFFIX.put("localsubjectaccessreviews", "authorization-resources/local-subject-access-review-v1");
        DOC_URL_SUFFIX.put("mutatingwebhookconfigurations", "extend-resources/mutating-webhook-configuration-v1");
        DOC_URL_SUFFIX.put("namespaces", "cluster-resources/namespace-v1");
        DOC_URL_SUFFIX.put("networkpolicies", "policy-resources/network-policy-v1");
        DOC_URL_SUFFIX.put("nodes", "cluster-resources/node-v1");
        DOC_URL_SUFFIX.put("persistentvolumeclaims", "config-and-storage-resources/persistent-volume-claim-v1");
        DOC_URL_SUFFIX.put("persistentvolumes", "config-and-storage-resources/persistent-volume-v1");
        DOC_URL_SUFFIX.put("poddisruptionbudgets", "policy-resources/pod-disruption-budget-v1");
        DOC_URL_SUFFIX.put("pods", "workload-resources/pod-v1");
        DOC_URL_SUFFIX.put("podsecuritypolicies", "policy-resources/pod-security-policy-v1beta1");
        DOC_URL_SUFFIX.put("podtemplates", "workload-resources/pod-template-v1");
        DOC_URL_SUFFIX.put("priorityclasses", "workload-resources/priority-class-v1");
        DOC_URL_SUFFIX.put("replicasets", "workload-resources/replica-set-v1");
        DOC_URL_SUFFIX.put("replicationcontrollers", "workload-resources/replication-controller-v1");
        DOC_URL_SUFFIX.put("resourcequotas", "policy-resources/resource-quota-v1");
        DOC_URL_SUFFIX.put("rolebindings", "authorization-resources/role-binding-v1");
        DOC_URL_SUFFIX.put("roles", "authorization-resources/role-v1");
        DOC_URL_SUFFIX.put("runtimeclasses", "cluster-resources/runtime-class-v1");
        DOC_URL_SUFFIX.put("secrets", "config-and-storage-resources/secret-v1");
        DOC_URL_SUFFIX.put("selfsubjectaccessreviews", "authorization-resources/self-subject-access-review-v1");
        DOC_URL_SUFFIX.put("selfsubjectrulesreviews", "authorization-resources/self-subject-rules-review-v1");
        DOC_URL_SUFFIX.put("serviceaccounts", "authentication-resources/service-account-v1");
        DOC_URL_SUFFIX.put("services", "service-resources/service-v1");
        DOC_URL_SUFFIX.put("statefulsets", "workload-resources/stateful-set-v1");
        DOC_URL_SUFFIX.put("storageclasses", "config-and-storage-resources/storage-class-v1");
        DOC_URL_SUFFIX.put("subjectaccessreviews", "authorization-resources/subject-access-review-v1");
        DOC_URL_SUFFIX.put("tokenreviews", "authentication-resources/token-review-v1");
        DOC_URL_SUFFIX.put("validatingwebhookconfigurations", "extend-resources/validating-webhook-configuration-v1");
        DOC_URL_SUFFIX.put("volumeattachments", "config-and-storage-resources/volume-attachment-v1");
    }

    static void explain(String resourceTypeName) {
        String docsUrlSuffix = DOC_URL_SUFFIX.get(resourceTypeName);
        if (docsUrlSuffix != null) {
            String url = docsUrlSuffix.startsWith("http") ?
                    docsUrlSuffix : (DOC_URL_PREFIX + docsUrlSuffix);
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException  | URISyntaxException ignore) {
                //
            }
        }

    }
}
