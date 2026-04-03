package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

final class HtmlSanitizer {
    private HtmlSanitizer() {}

    static @NonNull String sanitize(@NonNull String html) {
        // Allow basic formatting + form controls and safe attributes, including inline styles.
        Safelist safelist = Safelist.relaxed()
                .addTags("form", "input", "select", "option", "textarea", "label", "button")
                .addAttributes(":all", "id", "class", "title", "aria-label", "aria-describedby", "style")
                .addAttributes("input", "type", "value", "placeholder", "checked", "disabled", "readonly", "min", "max", "step")
                .addAttributes("select", "disabled", "multiple")
                .addAttributes("option", "value", "selected")
                .addAttributes("textarea", "placeholder", "disabled", "readonly", "rows", "cols")
                .addAttributes("button", "type", "disabled");

        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false);

        // Jsoup Cleaner drops event handlers by default unless explicitly allowed.
        return Jsoup.clean(html, "", safelist, outputSettings);
    }
}
