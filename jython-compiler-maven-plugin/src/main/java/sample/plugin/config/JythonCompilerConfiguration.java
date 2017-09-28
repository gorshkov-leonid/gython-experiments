package sample.plugin.config;

import java.io.File;
import java.util.List;

public class JythonCompilerConfiguration
{
    public final File outputDirectory;
    public final File inputDirectory;
    public final List<String> includeModules;
    public final List<String> excludeModules;


    public JythonCompilerConfiguration(File inputDirectory, File outputDirectory, List<String> includeModules, List<String> excludeModules)
    {
        this.outputDirectory = outputDirectory;
        this.inputDirectory = inputDirectory;
        this.includeModules = includeModules;
        this.excludeModules = excludeModules;
    }
}