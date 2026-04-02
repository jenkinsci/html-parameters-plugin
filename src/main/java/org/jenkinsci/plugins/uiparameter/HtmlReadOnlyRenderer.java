package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

final class HtmlReadOnlyRenderer {
    private HtmlReadOnlyRenderer() {}

    static @NonNull String renderReadOnly(
            @NonNull String sanitizedTemplateHtml,
            @NonNull Map<String, String> sourceIdByOutputName,
            @NonNull Map<String, String> valuesByOutputName
    ) {
        if (sanitizedTemplateHtml.isEmpty()) {
            return "";
        }

        Document doc = Jsoup.parseBodyFragment(sanitizedTemplateHtml);

        for (Map.Entry<String, String> e : sourceIdByOutputName.entrySet()) {
            String outputName = e.getKey();
            String sourceId = e.getValue();
            if (sourceId == null || sourceId.trim().isEmpty()) {
                continue;
            }
            String value = valuesByOutputName.get(outputName);
            if (value == null) {
                value = "";
            }

            Element el = doc.getElementById(sourceId);
            if (el == null) {
                continue;
            }

            String tag = el.tagName().toLowerCase();
            if ("input".equals(tag)) {
                String type = el.attr("type").toLowerCase();
                if ("checkbox".equals(type)) {
                    boolean checked = "true".equalsIgnoreCase(value);
                    if (checked) {
                        el.attr("checked", "checked");
                    } else {
                        el.removeAttr("checked");
                    }
                } else if ("radio".equals(type)) {
                    // Best-effort: if the element itself is the radio, match by value.
                    boolean checked = value.equals(el.attr("value"));
                    if (checked) {
                        el.attr("checked", "checked");
                    } else {
                        el.removeAttr("checked");
                    }
                } else {
                    el.attr("value", value);
                }
                el.attr("disabled", "disabled");
                el.attr("readonly", "readonly");
            } else if ("textarea".equals(tag)) {
                el.text(value);
                el.attr("disabled", "disabled");
                el.attr("readonly", "readonly");
            } else if ("select".equals(tag)) {
                Elements options = el.getElementsByTag("option");
                for (Element opt : options) {
                    if (value.equals(opt.attr("value"))) {
                        opt.attr("selected", "selected");
                    } else {
                        opt.removeAttr("selected");
                    }
                }
                el.attr("disabled", "disabled");
            } else {
                // For any other tag, render as text.
                el.text(value);
            }
        }

        // Disable all controls (even those not mapped) to prevent edits.
        Elements controls = doc.select("input, select, textarea, button");
        for (Element c : controls) {
            c.attr("disabled", "disabled");
            if ("input".equalsIgnoreCase(c.tagName()) || "textarea".equalsIgnoreCase(c.tagName())) {
                c.attr("readonly", "readonly");
            }
        }

        return doc.body().html();
    }
}

