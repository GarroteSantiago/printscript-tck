package implementation;

import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.application.CommandResult;
import org.printscript.application.JsonPrintScriptConfigReader;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.ProgressReporter;
import org.printscript.diagnostics.Diagnostic;

public final class LinterAdapter implements PrintScriptLinter {
    private final PrintScript printScript = new PrintScript();
    private final JsonPrintScriptConfigReader configReader = new JsonPrintScriptConfigReader();

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        AnalyzerConfig analyzerConfig = configReader.readAnalyzerConfig(config);

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
}
