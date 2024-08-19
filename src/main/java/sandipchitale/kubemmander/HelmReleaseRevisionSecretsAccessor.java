package sandipchitale.kubemmander;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class HelmReleaseRevisionSecretsAccessor {
    static KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetAllNamespaces() {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet = new LinkedHashSet<>();
        kubernetesClient
                .namespaces()
                .list()
                .getItems()
                .forEach((Namespace namespace) -> {
                    namespaceStringStringNamespaceSecretReleaseRevisionSet.addAll(
                            getNamespaceSecretReleaseRevisionSetInNamespace(namespace.getMetadata().getName()));
                });
        return namespaceStringStringNamespaceSecretReleaseRevisionSet;
    }

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetInNamespace(String namespace) {
        return getNamespaceSecretReleaseRevisionSetInNamespace(kubernetesClient.namespaces().withName(namespace).get());
    }

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetInNamespace(Namespace namespace) {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet = new LinkedHashSet<>();
        kubernetesClient
                .secrets()
                .inNamespace(namespace.getMetadata().getName())
                .list()
                .getItems()
                .stream()
                .filter(secret -> {
                    Matcher matcher = Constants.helmSecretNamePattern.matcher(secret.getMetadata().getName());
                    return (matcher.matches());
                })
                .forEach(secret -> {
                    Matcher matcher = Constants.helmSecretNamePattern.matcher(secret.getMetadata().getName());
                    if (matcher.matches()) {
                        String release = matcher.group(1);
                        String revision = matcher.group(2);
                        namespaceStringStringNamespaceSecretReleaseRevisionSet.add(new NamespaceSecretReleaseRevision(namespace, secret, release, revision));
                    }
                });

        return namespaceStringStringNamespaceSecretReleaseRevisionSet;
    }
}