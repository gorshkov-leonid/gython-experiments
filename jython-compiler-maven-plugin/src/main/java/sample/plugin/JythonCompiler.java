package sample.plugin;

import com.google.common.collect.Lists;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import sample.plugin.config.JythonCompilerConfiguration;
import sample.plugin.util.DirUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.*;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.newHashSet;

public class JythonCompiler {

    public static final String INIT_FILE_NAME = "__init__.py";
    public static final String PYTHON_EXT = ".py";


//    public static void main(String[] args) throws IOException {
//        JythonCompilerConfiguration config = new JythonCompilerConfiguration(
//                new File("D:\\WORK\\SANDBOX\\PYTHON\\___Python-Java\\jython-ruamel-yaml\\src\\main\\java"),
//                new File("D:\\WORK\\SANDBOX\\PYTHON\\___Python-Java\\jython-ruamel-yaml\\target\\classes"),
//                Lists.<String>newArrayList(),
//                Lists.<String>newArrayList("ruamel.yaml.cyaml")
//        );
//        compile(config);
//    }

    public static void compile(JythonCompilerConfiguration configuration) throws IOException {
        Path sourcePath = Paths.get(configuration.inputDirectory.getAbsolutePath());
        final Path targetPath = sourcePath.resolve(configuration.outputDirectory.getPath()).toAbsolutePath().normalize();
        Set<String> includeModulesSet = newHashSet(configuration.includeModules);
        Set<String> excludeModulesSet = newHashSet(configuration.excludeModules);

        targetPath.toFile().mkdirs();

        DirUtils.copy(sourcePath, targetPath);

        Properties props = new Properties();
        props.put("jython.console.encoding", "UTF-8"); // todo is need? Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
        props.put("jython.security.respectJavaAccessibility", "false"); //todo is need? don't respect java accessibility, so that we can access protected members on subclasses
        props.put("jython.import.site", "false"); //do not use standalone python libraries
        PythonInterpreter.initialize(System.getProperties(), props, new String[0]);

        PythonInterpreter interp = new PythonInterpreter();

        PySystemState sys = Py.getSystemState();
        sys.dont_write_bytecode = false;
        sys.path.insert(0, new PyString(targetPath.toString()));
        System.out.println("Jython paths: " + sys.path);

        ImportPythonModulesScriptVisitor visitor = new ImportPythonModulesScriptVisitor(targetPath, includeModulesSet, excludeModulesSet);
        Files.walkFileTree(targetPath, visitor);
        interp.exec(visitor.getScript());

//        final File tempFile = File.createTempFile("jython_lib", ".py", targetPath.toFile());
//        FileUtils.fileWrite(tempFile,visitor.getScript());
//        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@ "+sb.toString());
//        interp.compile(sb.toString(), tempFile.getAbsolutePath().replace("\\.py", "$py.class"));
//        interp.exec("import ruamel.yaml");
        int r = 0;
    }

    private static String toModuleName(Path path, Path targetPath) {
        path = targetPath.relativize(path);
        return path.toString().replaceAll(Pattern.quote(PYTHON_EXT) + "$", "").replaceAll(Pattern.quote(File.separator), ".");
    }

    private static class ImportPythonModulesScriptVisitor extends SimpleFileVisitor<Path> {
        private final Path targetPath;
        private final Set<String> includeModulesSet;
        private final Set<String> excludeModulesSet;
        private final List<String> modules;

        ImportPythonModulesScriptVisitor(Path targetPath, Set<String> includeModulesSet, Set<String> excludeModulesSet) {
            this.targetPath = targetPath;
            this.modules = new ArrayList<>();
            this.includeModulesSet = includeModulesSet;
            this.excludeModulesSet = excludeModulesSet;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (dir.resolve(INIT_FILE_NAME).toFile().exists()) {
                modules.add(toModuleName(dir, targetPath));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            Path fileName = path.getFileName();
            if (!INIT_FILE_NAME.equals(fileName.toString()) && fileName.toString().endsWith(PYTHON_EXT)) {
                modules.add(toModuleName(path, targetPath));
            }
            return FileVisitResult.CONTINUE;
        }

        String getScript() {
            Predicate<String> includePredicate = includeModulesSet.isEmpty() ? (s) -> true : includeModulesSet::contains;
            Predicate<String> excludePredicate = excludeModulesSet.isEmpty() ? (s) -> false : excludeModulesSet::contains;

            StringBuilder sb = new StringBuilder();
            modules.stream().filter(includePredicate.and(excludePredicate.negate())).forEach((s) -> sb.append("import ").append(s).append("\n"));
            String script = sb.toString();
            System.out.println("compile modules \n" + script);
            return script;
        }
    }
}