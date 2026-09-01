package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.printscript.application.CommandResult;
import org.printscript.application.LanguageVersion;
import org.printscript.application.PrintScript;
import org.printscript.application.ProgressReporter;
import org.printscript.interpreter.EnvironmentPort;
import org.printscript.interpreter.InputPort;
import org.printscript.interpreter.RuntimeEnvironment;

public final class InterpreterAdapter implements PrintScriptInterpreter {
    private final PrintScript printScript = new PrintScript();

    @Override
    public void execute(
            InputStream src,
            String version,
            PrintEmitter emitter,
            ErrorHandler handler,
            InputProvider provider) {
        Reader reader = new InputStreamReader(src, StandardCharsets.UTF_8);
        InputPort input = prompt -> {
            emitter.print(prompt);
            return provider.input(prompt);
        };
        EnvironmentPort env = name -> Optional.ofNullable(System.getenv(name));
        // Reserved so that, on OutOfMemoryError, releasing it guarantees enough
        // headroom for the catch block itself to report the error.
        byte[] reserve = new byte[2500000];

        try {
            CommandResult<RuntimeEnvironment> result = printScript.execute(
                    reader, LanguageVersion.parse(version), emitter::print, input, env, ProgressReporter.NONE);

            if (!result.isSuccess()) {
                result.diagnostics().forEach(diagnostic -> handler.reportError(diagnostic.message()));
            }
        } catch (OutOfMemoryError oom) {
            reserve = null;
            System.gc();
            handler.reportError(oom.getMessage());
        }
    }
}
