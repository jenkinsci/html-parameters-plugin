package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Class tokens in the HTML template: plugin-prefixed ids/classes plus Jenkins design classes.
 *
 * <p>Per the Jenkins Design Library &quot;Stylesheets&quot; pattern documentation, classes
 * prefixed with {@code jenkins-} are intended for use from plugins; {@code app-} classes are
 * internal and must not be used.
 */
final class TemplateHtmlClassRules {
    private TemplateHtmlClassRules() {}

    static boolean isAllowedClassToken(@NonNull String token) {
        if (token.isEmpty()) {
            return true;
        }
        if (token.startsWith(HtmlParametersPrefix.PREFIX)) {
            return true;
        }
        if (token.startsWith("jenkins-")) {
            return true;
        }
        return false;
    }
}
