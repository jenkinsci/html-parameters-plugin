package org.jenkinsci.plugins.uiparameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CustomCssValidatorTest {

    @Test
    void acceptsScopedRule() {
        CustomCssValidator.validateOrThrow(".html-parameters-x { color: red }");
    }

    @Test
    void acceptsCommaSeparatedScopedSelectors() {
        CustomCssValidator.validateOrThrow(".html-parameters-a, #html-parameters-b { margin: 0 }");
    }

    @Test
    void acceptsDescendantJenkinsClassUnderScopedRoot() {
        CustomCssValidator.validateOrThrow(".html-parameters-panel .jenkins-button { margin: 0 }");
    }

    @Test
    void acceptsMediaQueryWithInnerScopedRule() {
        CustomCssValidator.validateOrThrow(
                "@media screen { .html-parameters-panel .jenkins-button { width: 100% } }");
    }

    @Test
    void rejectsUnscopedRuleInsideMedia() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CustomCssValidator.validateOrThrow(
                "@media screen { .jenkins-button { color: red } }"));
        assertTrue(e.getMessage().contains("custom CSS"));
    }

    @Test
    void rejectsBareJenkinsSelector() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CustomCssValidator.validateOrThrow(".jenkins-button { margin: 0 }"));
            assertTrue(e.getMessage().contains("html-parameters-"));
    }

    @Test
    void rejectsImport() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CustomCssValidator.validateOrThrow("@import url('x');"));
            assertTrue(e.getMessage().contains("custom CSS"));
    }

    @Test
    void splitsSelectorsRespectingParensAndBrackets() {
        assertEquals(
                Arrays.asList(":not(.html-parameters-a, .html-parameters-b)", ".html-parameters-c"),
                CustomCssValidator.splitTopLevelSelectors(
                        ":not(.html-parameters-a, .html-parameters-b), .html-parameters-c"));
    }
}
