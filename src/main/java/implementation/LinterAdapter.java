package implementation;

import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.printscript.analyzer.AnalyzerConfig;
import org.printscript.toolchain.AnalysisResult;
import org.printscript.toolchain.CommandResult;
import org.printscript.toolchain.JsonPrintScriptConfigReader;
import org.printscript.toolchain.LanguageVersion;
import org.printscript.toolchain.PrintScript;
import org.printscript.toolchain.ProgressReporter;

public final class LinterAdapter implements PrintScriptLinter {
    private final PrintScript printScript = new PrintScript();
    private final JsonPrintScriptConfigReader configReader = new JsonPrintScriptConfigReader();

    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        AnalyzerConfig analyzerConfig = configReader.readAnalyzerConfig(config);

        // Analyzer findings stream straight to the handler; if the source can't be analyzed
        // (a syntax or semantic error), report the errors that stopped it instead.
        CommandResult<AnalysisResult> result =
                printScript.analyze(
                        reader,
                        LanguageVersion.parse(version),
                        analyzerConfig,
                        diagnostic -> handler.reportError(diagnostic.message()),
                        ProgressReporter.NONE);

        if (!result.isSuccess()) {
            result.diagnostics().forEach(diagnostic -> handler.reportError(diagnostic.message()));
        }
    }
}
