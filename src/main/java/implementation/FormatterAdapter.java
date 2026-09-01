package implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import interpreter.PrintScriptFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.printscript.application.CommandResult;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.ProgressReporter;
import org.printscript.formatter.FormatterConfig;

public final class FormatterAdapter implements PrintScriptFormatter {
    private final PrintScript printScript = new PrintScript();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        FormatterConfig formatterConfig = readConfig(config);
        try {
            CommandResult<Void> result =
                    printScript.format(
                            reader,
                            LanguageVersion.parse(version),
                            formatterConfig,
                            writer,
                            ProgressReporter.NONE);
            if (!result.isSuccess()) {
                throw new IllegalStateException(
                        "Formatting failed: " + result.diagnostics());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    // Every rule below is Optional.empty() unless the TCK's JSON config actually names the key
    // for it -- an absent key must leave that rule's trivia untouched, not fall back to a
    // default. The TCK never configures semicolon spacing directly (no
    // "spaces-before/after-semicolon"-shaped key exists in any fixture), so those two stay
    // permanently empty here.
    @SuppressWarnings("unchecked")
    private FormatterConfig readConfig(InputStream config) {
        Map<String, Object> values;
        try {
            values = mapper.readValue(config, Map.class);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return new FormatterConfig(
                Optional.empty(),
                Optional.empty(),
                spacesAroundAssignment(values),
                boolKey(values, "mandatory-space-surrounding-operations"),
                intKey(values, "line-breaks-after-println"),
                intKey(values, "indent-inside-if"),
                boolKey(values, "enforce-spacing-before-colon-in-declaration"),
                boolKey(values, "enforce-spacing-after-colon-in-declaration"),
                flag(values, "mandatory-single-space-separation"),
                flag(values, "mandatory-line-break-after-statement"),
                ifBraceOnSameLine(values));
    }

    private Optional<Integer> spacesAroundAssignment(Map<String, Object> values) {
        if (Boolean.TRUE.equals(values.get("enforce-spacing-around-equals"))) {
            return Optional.of(1);
        }
        if (Boolean.TRUE.equals(values.get("enforce-no-spacing-around-equals"))) {
            return Optional.of(0);
        }
        return Optional.empty();
    }

    private Optional<Boolean> ifBraceOnSameLine(Map<String, Object> values) {
        if (Boolean.TRUE.equals(values.get("if-brace-same-line"))) {
            return Optional.of(true);
        }
        if (Boolean.TRUE.equals(values.get("if-brace-below-line"))) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    // Boolean-flag keys that map to a spaces-count of exactly 1 (the rule is either on or
    // untouched -- these fixtures never ask for anything other than a single space).
    private Optional<Integer> boolKey(Map<String, Object> values, String key) {
        return Boolean.TRUE.equals(values.get(key)) ? Optional.of(1) : Optional.empty();
    }

    private Optional<Boolean> flag(Map<String, Object> values, String key) {
        return Boolean.TRUE.equals(values.get(key)) ? Optional.of(true) : Optional.empty();
    }

    private Optional<Integer> intKey(Map<String, Object> values, String key) {
        return values.get(key) instanceof Number number
                ? Optional.of(number.intValue())
                : Optional.empty();
    }
}
