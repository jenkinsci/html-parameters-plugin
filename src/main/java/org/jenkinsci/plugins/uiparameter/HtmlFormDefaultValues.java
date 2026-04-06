package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Reads default submitted values from sanitized template markup (mirrors client {@code binding.js}
 * enough for Pipeline defaults and server-side fallback when the hidden JSON is empty).
 */
final class HtmlFormDefaultValues {
    private HtmlFormDefaultValues() {}

    static @NonNull Map<String, String> fromSanitizedTemplate(
            @NonNull String sanitizedHtml,
            @NonNull List<HtmlFormMapping> mappings
    ) {
        Map<String, String> map = new LinkedHashMap<>();
        Document doc = Jsoup.parseBodyFragment(sanitizedHtml);
        for (HtmlFormMapping m : mappings) {
            if (m == null) {
                continue;
            }
            String outputName = m.getOutputName();
            if (outputName.trim().isEmpty()) {
                continue;
            }
            Element el = doc.getElementById(m.getSourceId());
            map.put(outputName, el == null ? "" : extractElementDefault(el));
        }
        return map;
    }

    static @NonNull String extractElementDefault(@NonNull Element el) {
        String tag = el.tagName().toLowerCase(Locale.ROOT);
        if ("input".equals(tag)) {
            String type = el.attr("type").toLowerCase(Locale.ROOT);
            if (type.isEmpty()) {
                type = "text";
            }
            if ("checkbox".equals(type)) {
                return el.hasAttr("checked") ? "true" : "false";
            }
            if ("radio".equals(type)) {
                if (!el.hasAttr("checked")) {
                    return "";
                }
                String val = el.attr("value");
                return val.isEmpty() ? "on" : val;
            }
            return el.attr("value");
        }
        if ("select".equals(tag)) {
            Element opt = el.selectFirst("option[selected]");
            if (opt == null) {
                opt = el.selectFirst("option");
            }
            if (opt == null) {
                return "";
            }
            String val = opt.attr("value");
            return val.isEmpty() ? opt.text() : val;
        }
        if ("textarea".equals(tag)) {
            return el.text();
        }
        return el.text().trim();
    }
}
