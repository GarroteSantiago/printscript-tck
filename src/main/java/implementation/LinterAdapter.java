package implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.analyzer.NamingStyle;
import org.printscript.application.CommandResult;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.ProgressReporter;
import org.printscript.diagnostics.Diagnostic;

public final class LinterAdapter implements PrintScriptLinter {
    private final PrintScript printScript = new PrintScript();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        AnalyzerConfig analyzerConfig = readConfig(config);

        CommandResult<java.util.List<Diagnostic>> result =
                printScript.analyze(
                        reader,
                        LanguageVersion.parse(version),
                        analyzerConfig,
                        ProgressReporter.NONE);

        // .diagnostics() covers both real syntax/semantic failures and analyzer-rule
        // violations (StaticAnalyzer reports at ERROR severity, same as failures).
        result.diagnostics().forEach(diagnostic -> handler.reportError(diagnostic.message()));
    }

    @SuppressWarnings("unchecked")
    private AnalyzerConfig readConfig(InputStream config) {
        Map<String, Object> values;
        try {
            values = mapper.readValue(config, Map.class);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        Object identifierFormat = values.get("identifier_format");
        boolean checkIdentifierNaming = identifierFormat != null;
        NamingStyle namingStyle =
                "camel case".equals(identifierFormat) ? NamingStyle.CAMEL_CASE : NamingStyle.SNAKE_CASE;

        boolean restrictPrintln =
                Boolean.TRUE.equals(values.get("mandatory-variable-or-literal-in-println"));
        boolean restrictReadInput =
                Boolean.TRUE.equals(values.get("mandatory-variable-or-literal-in-readInput"));

        return new AnalyzerConfig(namingStyle, checkIdentifierNaming, restrictPrintln, restrictReadInput);
    }
}
