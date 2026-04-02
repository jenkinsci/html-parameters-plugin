package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

public class HtmlFormParameterDefinition extends ParameterDefinition {
    private static final Logger LOGGER = Logger.getLogger(HtmlFormParameterDefinition.class.getName());

    private final @NonNull String name;
    private @CheckForNull String description;
    private @CheckForNull String templateHtml;
    private @CheckForNull String customCss;
    private @NonNull List<HtmlFormMapping> mappings = new ArrayList<>();

    @DataBoundConstructor
    public HtmlFormParameterDefinition(@NonNull String name) {
        super(name);
        this.name = name;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @CheckForNull String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(@CheckForNull String description) {
        this.description = description;
        super.setDescription(description);
    }

    public @CheckForNull String getTemplateHtml() {
        return templateHtml;
    }

    @DataBoundSetter
    public void setTemplateHtml(@CheckForNull String templateHtml) {
        this.templateHtml = templateHtml;
    }

    public @NonNull List<HtmlFormMapping> getMappings() {
        return mappings;
    }

    @DataBoundSetter
    public void setMappings(@CheckForNull List<HtmlFormMapping> mappings) {
        if (mappings == null) {
            this.mappings = new ArrayList<>();
        } else {
            this.mappings = new ArrayList<>(mappings);
        }
    }

    public @NonNull String getSanitizedTemplateHtml() {
        String raw = templateHtml == null ? "" : templateHtml;
        return HtmlSanitizer.sanitize(raw);
    }

    public @CheckForNull String getCustomCss() {
        return customCss;
    }

    @DataBoundSetter
    public void setCustomCss(@CheckForNull String customCss) {
        this.customCss = customCss;
    }

    public @NonNull String getSanitizedCustomCss() {
        String css = customCss == null ? "" : customCss;

        // Prevent breaking out of <style> and injecting HTML/Jelly.
        css = css.replaceAll("(?i)</\\s*style\\s*>", "");
        css = css.replace("<", "");
        css = css.replace(">", "");
        css = css.replace("]]>", "]]");
        return css;
    }

    @Override
    public @CheckForNull ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("createValue(req,jo) called for param=" + getName()
                    + " jo=" + (jo == null ? "null" : jo.toString())
                    + " req.value=" + (req == null ? "null" : req.getParameter("value")));
        }
        // Jenkins convention: JSON submit has property "value" (string). See structured form submission docs.
        JSONObject values = null;
        if (jo != null && jo.has("value")) {
            Object raw = jo.get("value");
            if (raw != null) {
                try {
                    values = (JSONObject) JSONSerializer.toJSON(String.valueOf(raw));
                } catch (RuntimeException ignored) {
                    values = null;
                }
            }
        }

        Map<String, String> out = new LinkedHashMap<>();
        Map<String, String> sourceIdByOutput = new LinkedHashMap<>();

        for (HtmlFormMapping m : mappings) {
            if (m == null) {
                continue;
            }
            String outputName = m.getOutputName();
            if (outputName.trim().isEmpty()) {
                continue;
            }
            sourceIdByOutput.put(outputName, m.getSourceId());

            String v = "";
            if (values != null && values.has(outputName)) {
                Object raw = values.get(outputName);
                v = raw == null ? "" : String.valueOf(raw);
            } else if (req != null) {
                // Fallback for non-structured form submit: request parameter "value" is JSON string.
                String json = req.getParameter("value");
                if (json != null) {
                    try {
                        JSONObject reqValues = (JSONObject) JSONSerializer.toJSON(json);
                        if (reqValues.has(outputName)) {
                            Object raw = reqValues.get(outputName);
                            v = raw == null ? "" : String.valueOf(raw);
                        }
                    } catch (RuntimeException ignored) {
                        // ignore
                    }
                }
            }
            out.put(outputName, v);
        }

        String readOnlyHtml = HtmlReadOnlyRenderer.renderReadOnly(
                getSanitizedTemplateHtml(),
                sourceIdByOutput,
                out
        );

        return new HtmlFormParameterValue(
                getName(),
                out,
                sourceIdByOutput,
                getSanitizedTemplateHtml(),
                getSanitizedCustomCss(),
                readOnlyHtml
        );
    }

    @Override
    public @CheckForNull ParameterValue createValue(StaplerRequest req) {
        if (req == null) {
            return null;
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("createValue(req) called for param=" + getName() + " req.value=" + req.getParameter("value"));
        }

        Map<String, String> out = new LinkedHashMap<>();
        Map<String, String> sourceIdByOutput = new LinkedHashMap<>();

        JSONObject reqValues = null;
        String json = req.getParameter("value");
        if (json != null) {
            try {
                reqValues = (JSONObject) JSONSerializer.toJSON(json);
            } catch (RuntimeException ignored) {
                reqValues = null;
            }
        }

        for (HtmlFormMapping m : mappings) {
            if (m == null) {
                continue;
            }
            String outputName = m.getOutputName();
            if (outputName.trim().isEmpty()) {
                continue;
            }
            sourceIdByOutput.put(outputName, m.getSourceId());
            String v = "";
            if (reqValues != null && reqValues.has(outputName)) {
                Object raw = reqValues.get(outputName);
                v = raw == null ? "" : String.valueOf(raw);
            }
            out.put(outputName, v);
        }

        String readOnlyHtml = HtmlReadOnlyRenderer.renderReadOnly(
                getSanitizedTemplateHtml(),
                sourceIdByOutput,
                out
        );
        return new HtmlFormParameterValue(
                getName(),
                out,
                sourceIdByOutput,
                getSanitizedTemplateHtml(),
                getSanitizedCustomCss(),
                readOnlyHtml
        );
    }

    @Extension
    @Symbol("uiHtmlFormParameter")
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public @NonNull String getDisplayName() {
            return "UI HTML Form Parameter";
        }
    }
}
