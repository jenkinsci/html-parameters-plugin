package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.util.VariableResolver;
import java.io.Serial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HtmlFormParameterValue extends ParameterValue {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Must be XStream-friendly (avoid Collections.unmodifiableMap wrappers).
     */
    private @NonNull Map<String, String> valuesByOutputName = new LinkedHashMap<>();
    private @NonNull Map<String, String> sourceIdByOutputName = new LinkedHashMap<>();
    private @NonNull String sanitizedTemplateHtml = "";
    private @NonNull String sanitizedCustomCss = "";
    private @NonNull String readOnlyHtml = "";

    public HtmlFormParameterValue(
            @NonNull String name,
            @NonNull Map<String, String> valuesByOutputName,
            @NonNull Map<String, String> sourceIdByOutputName,
            @NonNull String sanitizedTemplateHtml,
            @NonNull String sanitizedCustomCss,
            @NonNull String readOnlyHtml
    ) {
        super(name);
        this.valuesByOutputName = new LinkedHashMap<>(valuesByOutputName);
        this.sourceIdByOutputName = new LinkedHashMap<>(sourceIdByOutputName);
        this.sanitizedTemplateHtml = sanitizedTemplateHtml;
        this.sanitizedCustomCss = sanitizedCustomCss;
        this.readOnlyHtml = readOnlyHtml;
    }

    /**
     * Backward compatibility for already stored builds / older plugin versions.
     */
    public HtmlFormParameterValue(@NonNull String name, @NonNull Map<String, String> valuesByOutputName) {
        this(name, valuesByOutputName, new LinkedHashMap<>(), "", "", "");
    }

    public @NonNull Map<String, String> getValuesByOutputName() {
        return Collections.unmodifiableMap(valuesByOutputName);
    }

    public @NonNull Map<String, String> getSourceIdByOutputName() {
        return Collections.unmodifiableMap(sourceIdByOutputName);
    }

    public @NonNull String getSanitizedTemplateHtml() {
        return sanitizedTemplateHtml;
    }

    public @NonNull String getSanitizedCustomCss() {
        return sanitizedCustomCss;
    }

    public @NonNull String getReadOnlyHtml() {
        return readOnlyHtml;
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        for (Map.Entry<String, String> e : valuesByOutputName.entrySet()) {
            env.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
        Map<String, String> snapshot = new LinkedHashMap<>(valuesByOutputName);
        return name -> snapshot.get(name);
    }

    @Override
    public Object getValue() {
        return valuesByOutputName;
    }

    @Override
    public String getShortDescription() {
        return getName();
    }
}
