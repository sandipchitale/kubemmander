package sandipchitale.kubemmander;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;

public record NamespaceSecretReleaseRevision(Namespace namespace, Secret secret, String release, String revision) {}
