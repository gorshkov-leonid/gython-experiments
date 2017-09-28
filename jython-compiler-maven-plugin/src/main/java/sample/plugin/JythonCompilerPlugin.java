package sample.plugin;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;
import sample.plugin.config.JythonCompilerConfiguration;

import java.io.*;
import java.util.List;

//https://maven.apache.org/plugin-tools/maven-plugin-plugin/examples/using-annotations.html
//https://stackoverflow.com/questions/27995000/how-to-fork-a-maven-lifecycle-in-the-proper-sense-from-a-plugin
//http://maven.apache.org/ref/3.2.2/maven-core/default-bindings.html
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class JythonCompilerPlugin extends AbstractMojo
{
    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "outputDir", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "inputDir", required = true, readonly = true)
    private File inputDirectory;

    @Parameter(property = "includeModules", required = false)
    private List<String> includeModules;

    @Parameter(property = "excludeModules", required = false)
    private List<String> excludeModules;

    public void execute() throws MojoExecutionException
    {
        JythonCompilerConfiguration jythonConfiguration = new JythonCompilerConfiguration(inputDirectory, outputDirectory, includeModules, excludeModules);
        try
        {
            JythonCompiler.compile(jythonConfiguration);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new MojoExecutionException("Error compilation " + jythonConfiguration.inputDirectory.getPath() + " to " + jythonConfiguration.outputDirectory.getPath(), e);
        }
    }
}
