package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import org.kohsuke.stapler.DataBoundConstructor;

public class HtmlFormMapping extends AbstractDescribableImpl<HtmlFormMapping> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final @NonNull String outputName;
    private final @NonNull String sourceId;

    @DataBoundConstructor
    public HtmlFormMapping(@NonNull String outputName, @NonNull String sourceId) {
        this.outputName = outputName;
        this.sourceId = sourceId;
    }

    public @NonNull String getOutputName() {
        return outputName;
    }

    public @NonNull String getSourceId() {
        return sourceId;
    }

    public @CheckForNull String validate() {
        if (outputName.trim().isEmpty()) {
            return "outputName is required";
        }
        if (sourceId.trim().isEmpty()) {
            return "sourceId is required";
        }
        if (!sourceId.startsWith(HtmlParametersPrefix.PREFIX)) {
            return "sourceId must start with '" + HtmlParametersPrefix.PREFIX + "'";
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HtmlFormMapping> {
        @Override
        public @NonNull String getDisplayName() {
            return "Mapping";
        }
    }
}
