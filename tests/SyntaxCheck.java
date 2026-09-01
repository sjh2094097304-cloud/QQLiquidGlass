import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;

public class SyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                Arrays.asList("-proc:none", "-source", "11"), null, files.getJavaFileObjects(args));
            task.parse();
            long errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count();
            System.out.println("SYNTAX: " + args.length + " Java source files parsed; errors=" + errors);
            if (errors > 0) {
                diagnostics.getDiagnostics().forEach(System.out::println);
                throw new AssertionError("Java syntax errors");
            }
        }
    }
}
