package implementation;

import interpreter.PrintScriptFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.printscript.application.CommandResult;
import org.printscript.application.JsonPrintScriptConfigReader;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.ProgressReporter;
import org.printscript.formatter.FormatterConfigProvider;

public final class FormatterAdapter implements PrintScriptFormatter {
    private final PrintScript printScript = new PrintScript();
    private final JsonPrintScriptConfigReader configReader = new JsonPrintScriptConfigReader();

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        FormatterConfigProvider formatterConfig = configReader.readFormatterConfig(config);
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
}
