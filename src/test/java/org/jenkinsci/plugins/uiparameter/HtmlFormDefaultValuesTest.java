package org.jenkinsci.plugins.uiparameter;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;

public class HtmlFormDefaultValuesTest {
    @Test
    public void readsInputValueAttribute() {
        String html = "<input id=\"html-parameters-x\" value=\"main\" />";
        assertEquals(
                "main",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("OUT", "html-parameters-x")))
                        .get("OUT"));
    }

    @Test
    public void readsCheckboxUnchecked() {
        String html = "<input id=\"html-parameters-c\" type=\"checkbox\" />";
        assertEquals(
                "false",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("C", "html-parameters-c")))
                        .get("C"));
    }

    @Test
    public void readsCheckboxChecked() {
        String html = "<input id=\"html-parameters-c\" type=\"checkbox\" checked />";
        assertEquals(
                "true",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("C", "html-parameters-c")))
                        .get("C"));
    }
}
