package org.jenkinsci.plugins.uiparameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.Test;

public class CustomCssValidatorTest {
    @Test
    public void acceptsScopedRule() {
        CustomCssValidator.validateOrThrow(".html-parameters-x { color: red }");
    }

    @Test
    public void acceptsCommaSeparatedScopedSelectors() {
        CustomCssValidator.validateOrThrow(".html-parameters-a, #html-parameters-b { margin: 0 }");
    }

    @Test
    public void acceptsDescendantJenkinsClassUnderScopedRoot() {
        CustomCssValidator.validateOrThrow(".html-parameters-panel .jenkins-button { margin: 0 }");
    }

    @Test
    public void acceptsMediaQueryWithInnerScopedRule() {
        CustomCssValidator.validateOrThrow(
                "@media screen { .html-parameters-panel .jenkins-button { width: 100% } }");
    }

    @Test
    public void rejectsUnscopedRuleInsideMedia() {
        try {
            CustomCssValidator.validateOrThrow("@media screen { .jenkins-button { color: red } }");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("custom CSS"));
        }
    }

    @Test
    public void rejectsBareJenkinsSelector() {
        try {
            CustomCssValidator.validateOrThrow(".jenkins-button { margin: 0 }");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("html-parameters-"));
        }
    }

    @Test
    public void rejectsImport() {
        try {
            CustomCssValidator.validateOrThrow("@import url('x');");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("custom CSS"));
        }
    }

    @Test
    public void splitsSelectorsRespectingParensAndBrackets() {
        assertEquals(
                Arrays.asList(":not(.html-parameters-a, .html-parameters-b)", ".html-parameters-c"),
                CustomCssValidator.splitTopLevelSelectors(
                        ":not(.html-parameters-a, .html-parameters-b), .html-parameters-c"));
    }
}
