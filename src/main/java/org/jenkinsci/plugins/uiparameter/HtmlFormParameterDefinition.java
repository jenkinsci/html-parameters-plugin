package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.Descriptor.FormException;
import hudson.model.Failure;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

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
        validateTemplateHtmlOrThrow(templateHtml);
        this.templateHtml = templateHtml;
    }

    public @NonNull List<HtmlFormMapping> getMappings() {
        return mappings;
    }

    @DataBoundSetter
    public void setMappings(@CheckForNull List<HtmlFormMapping> mappings) {
        validateMappingsOrThrow(mappings);
        if (mappings == null) {
            this.mappings = new ArrayList<>();
        } else {
            this.mappings = new ArrayList<>(mappings);
        }
    }

    private static void validateTemplateHtmlOrThrow(@CheckForNull String templateHtml) {
        String html = templateHtml == null ? "" : templateHtml;
        Document doc = Jsoup.parseBodyFragment(html);

        for (Element el : doc.getAllElements()) {
            String id = el.id();
            if (id != null && !id.isEmpty() && !id.startsWith(HtmlParametersPrefix.PREFIX)) {
                throw new IllegalArgumentException("All element ids must start with '" + HtmlParametersPrefix.PREFIX
                        + "' to avoid interfering with Jenkins UI. Offending id: '" + id + "'.");
            }

            String classAttr = el.className();
            if (classAttr != null && !classAttr.trim().isEmpty()) {
                for (String token : classAttr.trim().split("\\s+")) {
                    if (!token.isEmpty() && !TemplateHtmlClassRules.isAllowedClassToken(token)) {
                        throw new IllegalArgumentException(
                                "Each CSS class token must start with '" + HtmlParametersPrefix.PREFIX
                                        + "' or 'jenkins-' (Jenkins Design Library). "
                                        + "Do not use 'app-' classes. Offending class: '" + token + "'.");
                    }
                }
            }
        }
    }

    private static void validateMappingsOrThrow(@CheckForNull List<HtmlFormMapping> mappings) {
        if (mappings == null) {
            return;
        }
        for (HtmlFormMapping m : mappings) {
            if (m == null) {
                continue;
            }
            String err = m.validate();
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
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
        if (customCss != null && !customCss.isBlank()) {
            CustomCssValidator.validateOrThrow(prepareCustomCssText(customCss));
        }
        this.customCss = customCss;
    }

    public @NonNull String getSanitizedCustomCss() {
        String css = customCss == null ? "" : customCss;
        css = prepareCustomCssText(css);
        CustomCssValidator.validateOrThrow(css);
        return css;
    }

    /**
     * Prevent breaking out of {@code <style>} and injecting HTML/Jelly.
     */
    private static @NonNull String prepareCustomCssText(@NonNull String css) {
        css = css.replaceAll("(?i)</\\s*style\\s*>", "");
        css = css.replace("<", "");
        css = css.replace(">", "");
        css = css.replace("]]>", "]]");
        return css;
    }

    @Override
    public @CheckForNull ParameterValue createValue(StaplerRequest2 req, JSONObject jo) {
        try {
            validateTemplateHtmlOrThrow(templateHtml);
            validateMappingsOrThrow(mappings);
        } catch (IllegalArgumentException e) {
            throw new Failure(e.getMessage());
        }
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

        JSONObject reqValues = null;
        if (req != null) {
            String json = req.getParameter("value");
            if (json != null) {
                try {
                    reqValues = (JSONObject) JSONSerializer.toJSON(json);
                } catch (RuntimeException ignored) {
                    reqValues = null;
                }
            }
        }

        Map<String, String> templateDefaults =
                HtmlFormDefaultValues.fromSanitizedTemplate(getSanitizedTemplateHtml(), mappings);
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

            boolean submittedKeyPresent = false;
            String v = "";
            if (values != null && values.has(outputName)) {
                submittedKeyPresent = true;
                Object raw = values.get(outputName);
                v = raw == null ? "" : String.valueOf(raw);
            } else if (reqValues != null && reqValues.has(outputName)) {
                submittedKeyPresent = true;
                Object raw = reqValues.get(outputName);
                v = raw == null ? "" : String.valueOf(raw);
            }
            if (v.isEmpty() && !submittedKeyPresent) {
                v = templateDefaults.getOrDefault(outputName, "");
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
    public @NonNull ParameterValue getDefaultParameterValue() {
        try {
            validateTemplateHtmlOrThrow(templateHtml);
            validateMappingsOrThrow(mappings);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.FINE, "HTML form parameter ''{0}'': no defaults ({1})", new Object[] {getName(), e.getMessage()});
            return new HtmlFormParameterValue(getName(), new LinkedHashMap<>());
        }
        Map<String, String> defaults =
                HtmlFormDefaultValues.fromSanitizedTemplate(getSanitizedTemplateHtml(), mappings);
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
        }
        String readOnlyHtml = HtmlReadOnlyRenderer.renderReadOnly(
                getSanitizedTemplateHtml(),
                sourceIdByOutput,
                defaults
        );
        return new HtmlFormParameterValue(
                getName(),
                defaults,
                sourceIdByOutput,
                getSanitizedTemplateHtml(),
                getSanitizedCustomCss(),
                readOnlyHtml
        );
    }

    @Override
    public @CheckForNull ParameterValue createValue(StaplerRequest2 req) {
        if (req == null) {
            return null;
        }
        try {
            validateTemplateHtmlOrThrow(templateHtml);
            validateMappingsOrThrow(mappings);
        } catch (IllegalArgumentException e) {
            throw new Failure(e.getMessage());
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("createValue(req) called for param=" + getName() + " req.value=" + req.getParameter("value"));
        }

        Map<String, String> templateDefaults =
                HtmlFormDefaultValues.fromSanitizedTemplate(getSanitizedTemplateHtml(), mappings);
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
            boolean submittedKeyPresent = reqValues != null && reqValues.has(outputName);
            String v = "";
            if (submittedKeyPresent) {
                Object raw = reqValues.get(outputName);
                v = raw == null ? "" : String.valueOf(raw);
            }
            if (v.isEmpty() && !submittedKeyPresent) {
                v = templateDefaults.getOrDefault(outputName, "");
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

        @Override
        public ParameterDefinition newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            try {
                // bindJSON will invoke DataBoundConstructor + DataBoundSetters (which perform validation)
                return req.bindJSON(HtmlFormParameterDefinition.class, formData);
            } catch (IllegalArgumentException e) {
                String msg = e.getMessage() == null ? "Invalid configuration" : e.getMessage();
                String field;
                if (msg.contains("sourceId")) {
                    field = "mappings";
                } else if (msg.contains("custom CSS")) {
                    field = "customCss";
                } else {
                    field = "templateHtml";
                }
                throw new FormException(msg, field);
            }
        }

        public FormValidation doCheckTemplateHtml(@QueryParameter String value) {
            String html = value == null ? "" : value;
            Document doc = Jsoup.parseBodyFragment(html);

            for (Element el : doc.getAllElements()) {
                String id = el.id();
                if (id != null && !id.isEmpty() && !id.startsWith(HtmlParametersPrefix.PREFIX)) {
                    return FormValidation.error(
                            "All element ids must start with '%s' to avoid interfering with Jenkins UI. Offending id: '%s'.",
                            HtmlParametersPrefix.PREFIX,
                            id
                    );
                }

                String classAttr = el.className();
                if (classAttr != null && !classAttr.trim().isEmpty()) {
                    for (String token : classAttr.trim().split("\\s+")) {
                        if (!token.isEmpty() && !TemplateHtmlClassRules.isAllowedClassToken(token)) {
                            return FormValidation.error(
                                    "Each CSS class token must start with '%s' or 'jenkins-' (Jenkins Design Library). "
                                            + "Do not use 'app-' classes. Offending class: '%s'.",
                                    HtmlParametersPrefix.PREFIX,
                                    token
                            );
                        }
                    }
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckCustomCss(@QueryParameter String value) {
            String css = value == null ? "" : value;
            if (css.isBlank()) {
                return FormValidation.ok();
            }
            try {
                CustomCssValidator.validateOrThrow(prepareCustomCssText(css));
            } catch (IllegalArgumentException e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }
    }
}
