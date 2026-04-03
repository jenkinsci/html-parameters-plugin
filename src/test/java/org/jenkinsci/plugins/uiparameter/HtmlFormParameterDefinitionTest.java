package org.jenkinsci.plugins.uiparameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import net.sf.json.JSONObject;
import hudson.util.FormValidation;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest2;

public class HtmlFormParameterDefinitionTest {
    @Test
    public void parsesValuesObjectByOutputName() {
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
    public void rejectsTemplateWithUnprefixedIdOrClass() {
        HtmlFormParameterDefinition.DescriptorImpl d = new HtmlFormParameterDefinition.DescriptorImpl();

        FormValidation badId = d.doCheckTemplateHtml("<input id='x' />");
        assertTrue(badId.kind != FormValidation.Kind.OK);

        FormValidation badClass = d.doCheckTemplateHtml("<div class='x html-parameters-ok'></div>");
        assertTrue(badClass.kind != FormValidation.Kind.OK);

        FormValidation ok = d.doCheckTemplateHtml("<div class='html-parameters-x'><input id='html-parameters-y'/></div>");
        assertEquals(FormValidation.Kind.OK, ok.kind);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnprefixedTemplateInSetter() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        def.setTemplateHtml("<input id='x' />");
    }
}

