package sandipchitale.kubemmander;

import java.util.regex.Pattern;

interface Constants {
    Pattern helmSecretNamePattern = Pattern.compile("^\\Qsh.helm.release.v1.\\E([^.]+)\\Q.v\\E(\\d+)");

    String RELEASE_REVISION_NAMESPACE_FORMAT = " ( %s.%s ) [ %s ]";

    String ALL = "All";
    String CHART_INFO = "Chart Info";
    String VALUES = "Values";
    String TEMPLATES = "Templates";
    String MANIFESTS = "Manifests";
    String HOOKS = "Hooks";
    String NOTES = "Notes";
}
