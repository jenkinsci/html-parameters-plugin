package org.jenkinsci.plugins.uiparameter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import net.sf.json.JSONObject;
import org.junit.Test;

public class HtmlFormParameterDefinitionTest {
    @Test
    public void parsesValuesObjectByOutputName() {
        HtmlFormParameterDefinition def = new HtmlFormParameterDefinition("UI");
        def.setMappings(Arrays.asList(
                new HtmlFormMapping("FOO", "fooId"),
                new HtmlFormMapping("BAR", "barId")
        ));

        JSONObject jo = new JSONObject();
        JSONObject values = new JSONObject();
        values.put("FOO", "aaa");
        values.put("BAR", "bbb");
        jo.put("value", values.toString());

        HtmlFormParameterValue v = (HtmlFormParameterValue) def.createValue(null, jo);
        Map<String, String> map = v.getValuesByOutputName();
        assertEquals("aaa", map.get("FOO"));
        assertEquals("bbb", map.get("BAR"));
    }
}

