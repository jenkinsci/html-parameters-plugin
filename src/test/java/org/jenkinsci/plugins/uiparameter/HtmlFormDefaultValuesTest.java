package org.jenkinsci.plugins.uiparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class HtmlFormDefaultValuesTest {

    @Test
    void readsInputValueAttribute() {
        String html = "<input id=\"html-parameters-x\" value=\"main\" />";
        assertEquals(
                "main",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("OUT", "html-parameters-x")))
                        .get("OUT"));
    }

    @Test
    void readsCheckboxUnchecked() {
        String html = "<input id=\"html-parameters-c\" type=\"checkbox\" />";
        assertEquals(
                "false",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("C", "html-parameters-c")))
                        .get("C"));
    }

    @Test
    void readsCheckboxChecked() {
        String html = "<input id=\"html-parameters-c\" type=\"checkbox\" checked />";
        assertEquals(
                "true",
                HtmlFormDefaultValues.fromSanitizedTemplate(
                                html,
                                Collections.singletonList(new HtmlFormMapping("C", "html-parameters-c")))
                        .get("C"));
    }
}
