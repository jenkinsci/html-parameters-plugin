package org.jenkinsci.plugins.uiparameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ensures {@code customCss} cannot target Jenkins UI outside the plugin's HTML: every
 * comma-separated selector in a style rule must contain {@link HtmlParametersPrefix#PREFIX}.
 *
 * <p>Only {@code @media} at-rules are allowed at the top level (other {@code @} rules are rejected).
 */
final class CustomCssValidator {
    private CustomCssValidator() {}

    static void validateOrThrow(@NonNull String css) {
        String s = stripComments(css).trim();
        if (s.isEmpty()) {
            return;
        }
        validateWithStringSkipping(s);
    }

    private static void validateWithStringSkipping(@NonNull String css) {
        int depth = 0;
        int ruleStart = 0;
        int n = css.length();
        int i = 0;
        while (i < n) {
            char c = css.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipQuotedString(css, i);
                continue;
            }
            if (c == '{') {
                String prelude = css.substring(ruleStart, i).trim();
                if (prelude.isEmpty()) {
                    throw new IllegalArgumentException("Empty selector before '{' in custom CSS.");
                }
                validatePrelude(prelude);
                depth++;
                if (depth == 1 && isMediaAtRule(prelude)) {
                    // Validate selectors inside @media blocks, not the @media prelude repeatedly.
                    ruleStart = i + 1;
                }
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Unbalanced '}' in custom CSS.");
                }
                if (depth == 0) {
                    ruleStart = i + 1;
                }
                i++;
                continue;
            }
            i++;
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Unbalanced '{' in custom CSS.");
        }
        String tail = css.substring(ruleStart).trim();
        if (!tail.isEmpty()) {
            throw new IllegalArgumentException("Trailing content after last rule in custom CSS.");
        }
    }

    private static boolean isMediaAtRule(@NonNull String prelude) {
        if (prelude.charAt(0) != '@') {
            return false;
        }
        return prelude.toLowerCase(Locale.ROOT).startsWith("@media");
    }

    private static void validatePrelude(@NonNull String prelude) {
        if (prelude.charAt(0) == '@') {
            String lower = prelude.toLowerCase(Locale.ROOT);
            if (lower.startsWith("@media")) {
                return;
            }
            throw new IllegalArgumentException(
                    "Only @media at-rules are allowed in custom CSS. Remove or replace in custom CSS: "
                            + prelude.split("\\s", 2)[0]);
        }
        for (String sel : splitTopLevelSelectors(prelude)) {
            if (sel.isEmpty()) {
                continue;
            }
            if (!sel.contains(HtmlParametersPrefix.PREFIX)) {
                throw new IllegalArgumentException(
                        "Each custom CSS selector must include '"
                                + HtmlParametersPrefix.PREFIX
                                + "' so only your form markup is styled. Offending selector: '"
                                + sel
                                + "'.");
            }
        }
    }

    /** Split on commas not inside parentheses or square brackets (attribute selectors). */
    static @NonNull List<String> splitTopLevelSelectors(@NonNull String prelude) {
        List<String> out = new ArrayList<>();
        int paren = 0;
        int bracket = 0;
        int start = 0;
        for (int i = 0; i < prelude.length(); i++) {
            char c = prelude.charAt(i);
            if (c == '(') {
                paren++;
            } else if (c == ')') {
                paren--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            } else if (c == ',' && paren == 0 && bracket == 0) {
                out.add(prelude.substring(start, i).trim());
                start = i + 1;
            }
        }
        out.add(prelude.substring(start).trim());
        return out;
    }

    private static int skipQuotedString(@NonNull String s, int start) {
        char q = s.charAt(start);
        int i = start + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                i += 2;
                continue;
            }
            if (c == q) {
                return i + 1;
            }
            i++;
        }
        return s.length();
    }

    private static @NonNull String stripComments(@NonNull String css) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < css.length()) {
            if (i + 1 < css.length() && css.charAt(i) == '/' && css.charAt(i + 1) == '*') {
                int end = css.indexOf("*/", i + 2);
                if (end < 0) {
                    return sb.toString();
                }
                sb.append(' ');
                i = end + 2;
                continue;
            }
            sb.append(css.charAt(i));
            i++;
        }
        return sb.toString();
    }
}
