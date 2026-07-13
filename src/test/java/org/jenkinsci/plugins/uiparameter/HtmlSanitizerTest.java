package org.jenkinsci.plugins.uiparameter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void stripsScriptTagsAndEventHandlers() {
        String html = "<div><input id='x' value='1' onclick='alert(1)' style='color:red'></div><script>alert(2)</script>";
        String cleaned = HtmlSanitizer.sanitize(html);

        assertFalse(cleaned.contains("<script"));
        assertFalse(cleaned.toLowerCase().contains("onclick="));
        assertFalse(cleaned.toLowerCase().contains("style="));
        assertTrue(cleaned.contains("id=\"x\"") || cleaned.contains("id='x'"));
    }

    @Test
    void preservesLabelForAndInputName() {
        String html =
                "<label class=\"html-parameters-l\" for=\"html-parameters-a\">L</label>"
                        + "<input id=\"html-parameters-a\" type=\"radio\" name=\"html-parameters-g\" value=\"1\" />";
        String cleaned = HtmlSanitizer.sanitize(html);
        assertTrue(cleaned.contains("for=\"html-parameters-a\"") || cleaned.contains("for='html-parameters-a'"));
        assertTrue(cleaned.contains("name=\"html-parameters-g\"") || cleaned.contains("name='html-parameters-g'"));
    }
}

