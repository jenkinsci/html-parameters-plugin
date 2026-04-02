package org.jenkinsci.plugins.uiparameter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HtmlSanitizerTest {
    @Test
    public void stripsScriptTagsAndEventHandlers() {
        String html = "<div><input id='x' value='1' onclick='alert(1)' style='color:red'></div><script>alert(2)</script>";
        String cleaned = HtmlSanitizer.sanitize(html);

        assertFalse(cleaned.contains("<script"));
        assertFalse(cleaned.toLowerCase().contains("onclick="));
        assertFalse(cleaned.toLowerCase().contains("style="));
        assertTrue(cleaned.contains("id=\"x\"") || cleaned.contains("id='x'"));
    }
}

