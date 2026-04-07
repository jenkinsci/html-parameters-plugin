package org.jenkinsci.plugins.uiparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest2;

class HtmlFormParameterDefinitionTest {

    @Test
    void parsesValuesObjectByOutputName() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        def.setMappings(Arrays.asList(
                new HtmlFormMapping("FOO", "html-parameters-fooId"),
                new HtmlFormMapping("BAR", "html-parameters-barId")
        ));

        JSONObject jo = new JSONObject();
        JSONObject values = new JSONObject();
        values.put("FOO", "aaa");
        values.put("BAR", "bbb");
        jo.put("value", values.toString());

        HtmlFormParameterValue v = (HtmlFormParameterValue) def.createValue((StaplerRequest2) null, jo);
        Map<String, String> map = v.getValuesByOutputName();
        assertEquals("aaa", map.get("FOO"));
        assertEquals("bbb", map.get("BAR"));
    }

    @Test
    void rejectsTemplateWithUnprefixedIdOrClass() {
        HtmlFormParameterDefinition.DescriptorImpl d = new HtmlFormParameterDefinition.DescriptorImpl();

        FormValidation badId = d.doCheckTemplateHtml("<input id='x' />");
        assertNotEquals(Kind.OK, badId.kind);

        FormValidation badClass = d.doCheckTemplateHtml("<div class='x html-parameters-ok'></div>");
        assertNotEquals(Kind.OK, badClass.kind);

        FormValidation badApp = d.doCheckTemplateHtml("<div class='app-bar'><input id='html-parameters-y'/></div>");
        assertNotEquals(Kind.OK, badApp.kind);

        FormValidation jenkinsClass =
                d.doCheckTemplateHtml("<div class='html-parameters-x jenkins-button'><input id='html-parameters-y'/></div>");
        assertEquals(FormValidation.Kind.OK, jenkinsClass.kind);

        FormValidation ok = d.doCheckTemplateHtml("<div class='html-parameters-x'><input id='html-parameters-y'/></div>");
        assertEquals(FormValidation.Kind.OK, ok.kind);
    }

    @Test
    void rejectsUnscopedCustomCss() {
        HtmlFormParameterDefinition.DescriptorImpl d = new HtmlFormParameterDefinition.DescriptorImpl();
        FormValidation bad = d.doCheckCustomCss(".jenkins-button { margin: 0 }");
        assertNotEquals(Kind.OK, bad.kind);
        FormValidation good = d.doCheckCustomCss(".html-parameters-root .jenkins-button { margin: 0 }");
        assertEquals(FormValidation.Kind.OK, good.kind);
    }

    @Test
    void rejectsInvalidCustomCssInSetter() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        assertThrows(IllegalArgumentException.class, () ->
            def.setCustomCss(".jenkins-root { margin: 0 }"));
    }

    @Test
    void rejectsUnprefixedTemplateInSetter() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        assertThrows(IllegalArgumentException.class, () ->
            def.setTemplateHtml("<input id='x' />"));
    }

    @Test
    void defaultParameterValueExtractsTemplateDefaultsForPipeline() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        def.setTemplateHtml("<input id=\"html-parameters-branch\" value=\"main\" />");
        def.setMappings(Collections.singletonList(new HtmlFormMapping("UI_BRANCH", "html-parameters-branch")));
        ParameterValue v = def.getDefaultParameterValue();
        HtmlFormParameterValue hv = (HtmlFormParameterValue) v;
        assertEquals("main", hv.getValuesByOutputName().get("UI_BRANCH"));
    }

    @Test
    void createValueFillsFromTemplateWhenJsonOmitsMappingKey() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        def.setTemplateHtml("<input id=\"html-parameters-branch\" value=\"from-template\" />");
        def.setMappings(Collections.singletonList(new HtmlFormMapping("UI_BRANCH", "html-parameters-branch")));
        JSONObject jo = new JSONObject();
        jo.put("value", "{}");
        HtmlFormParameterValue v = (HtmlFormParameterValue) def.createValue((StaplerRequest2) null, jo);
        assertEquals("from-template", v.getValuesByOutputName().get("UI_BRANCH"));
    }
}

