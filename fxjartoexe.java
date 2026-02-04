///usr/bin/env java --source 25 --enable-preview "$0" "$@" ; exit $?

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;

/**
 * Requirements:
 * - Java 25+
 * - Jpackage
 * - JavaFX jmods
 * - WiX toolset (for windows installers)
 */
public class fxjartoexe 
{
    private static final String CONFIG_FILE = "fxjartoexe.properties";
    private static Properties config = new Properties();

    public static void main(String[] args) 
    {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  JavaFX JAR to Windows EXE Converter (Java 25 + jpackage) ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        try 
        {
            loadOrCreateConfig();

            validatePrerequisites();

            displayConfig();

            if (!confirmExecution()) 
            {
                System.out.println("\nOperation aborted by user.");
                return;
            }

            executeConversion();

            System.out.println("\nCoversion succesful!");
            System.out.println("The executable file can be found in: " + config.getProperty("output.dir"));

        } 
        catch (Exception e) 
        {
            System.err.println("\nError: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void loadOrCreateConfig() throws IOException 
    {
        Path configPath = Paths.get(CONFIG_FILE);

        if (Files.exists(configPath)) 
        {
            try (InputStream input = Files.newInputStream(configPath)) 
            {
                config.load(input);
                System.out.println("Config loaded from: " + CONFIG_FILE);
            }
        } 

        else 
        {
            System.out.println("Creating new config...\n");
            createDefaultConfig();
            saveConfig();
            System.out.println("Config saved in: " + CONFIG_FILE);
            System.out.println("  Modify that file, than re-run this script.\n");
        }
    }

    private static void createDefaultConfig() 
    {
        config.setProperty("jar.path", "app.jar");
        config.setProperty("app.name", "MyJavaFXApp");
        config.setProperty("app.version", "1.0.0");
        config.setProperty("main.class", ""); 
        config.setProperty("javafx.jmods.path", System.getProperty("java.home") + "/jmods");
        config.setProperty("output.dir", "dist");
        config.setProperty("build.type", "app-image");
        config.setProperty("icon.path", ""); 
        config.setProperty("vendor", "");
        config.setProperty("description", "");
        config.setProperty("copyright", "");
        config.setProperty("jvm.options", "");  
        config.setProperty("win.console", "false");  
        config.setProperty("win.menu.group", "");
        config.setProperty("win.shortcut", "true");
    }

    private static void saveConfig() throws IOException 
    {
        try (OutputStream output = Files.newOutputStream(Paths.get(CONFIG_FILE))) 
        { config.store(output, "JavaFX JAR to EXE Configuration"); }
    }

    private static void validatePrerequisites() throws Exception 
    {
        String javaVersion = System.getProperty("java.version");
        System.out.println("Java version: " + javaVersion);

        ProcessBuilder pb = new ProcessBuilder("jpackage", "--version");
        Process p = pb.start();
        if (p.waitFor() != 0) 
        { throw new Exception("jpackage could not be found."); }

        String jarPath = config.getProperty("jar.path");
        if (!Files.exists(Paths.get(jarPath))) 
        { throw new Exception("JAR file could not be found: " + jarPath); }

        String jmodsPath = config.getProperty("javafx.jmods.path");
        if (!Files.exists(Paths.get(jmodsPath))) 
        { throw new Exception("JavaFX jmods could not be found: " + jmodsPath); }

        String iconPath = config.getProperty("icon.path", "");

        if (!iconPath.isEmpty() && !iconPath.equals("null") 
            && !Files.exists(Paths.get(iconPath))) 
        { System.out.println("WARNING: icon not found: " + iconPath); }

        System.out.println("Requirements verified\n");
    }

    private static void displayConfig() 
    {
        System.out.println("═══════════════════════════════════════");
        System.out.println("Current config:");
        System.out.println("═══════════════════════════════════════");
        System.out.println("JAR:              " + config.getProperty("jar.path"));
        System.out.println("App name:         " + config.getProperty("app.name"));
        System.out.println("Version:          " + config.getProperty("app.version"));

        String mainClass = config.getProperty("main.class");
        if (mainClass.isEmpty()) 
        {
            try 
            {
                mainClass = detectMainClass(config.getProperty("jar.path"));
                config.setProperty("main.class", mainClass);
                System.out.println("Main Class:       " + mainClass + " (rilevata automaticamente)");
            } 
            catch (Exception e) 
            { System.out.println("Main Class:       NOT FOUND - set it in the config!"); }
        } 
        else
        { System.out.println("Main Class:       " + mainClass); }

        System.out.println("JavaFX JMODs:     " + config.getProperty("javafx.jmods.path"));
        System.out.println("Output:           " + config.getProperty("output.dir"));

        String icon = config.getProperty("icon.path", "");
        System.out.println("Icon:            " + (icon.isEmpty() || icon.equals("null") ? "(nothing)" : icon));
        System.out.println("═══════════════════════════════════════\n");
    }

    private static String detectMainClass(String jarPath) throws Exception 
    {
        try (JarFile jarFile = new JarFile(jarPath)) 
        {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) 
            {
                String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                if (mainClass != null && !mainClass.isEmpty()) 
                { return mainClass; }
            }
        }
        throw new Exception("Main-Class not found in the JAR MANIFEST");
    }

    private static boolean confirmExecution() throws IOException 
    {
        System.out.print("Proceed with the conversion? (y/n): ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String response = reader.readLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    private static void executeConversion() throws Exception 
    {
        System.out.println("\nConverting...\n");

        String outputDir = config.getProperty("output.dir");
        Files.createDirectories(Paths.get(outputDir));

        List<String> command = new ArrayList<>();
        command.add("jpackage");

        // Parametri base
        command.add("--input");
        command.add(Paths.get(config.getProperty("jar.path")).getParent().toString());

        command.add("--main-jar");
        command.add(Paths.get(config.getProperty("jar.path")).getFileName().toString());

        command.add("--main-class");
        command.add(config.getProperty("main.class"));

        command.add("--name");
        command.add(config.getProperty("app.name"));

        command.add("--app-version");
        command.add(config.getProperty("app.version"));

        command.add("--dest");
        command.add(outputDir);

        command.add("--type");
        command.add(config.getProperty("build.type"));

        command.add("--module-path");
        command.add(config.getProperty("javafx.jmods.path"));

        command.add("--add-modules");
        command.add("javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.web");

        String iconPath = config.getProperty("icon.path", "");
        if (!iconPath.isEmpty() && !iconPath.equals("null") && Files.exists(Paths.get(iconPath))) 
        {
            command.add("--icon");
            command.add(iconPath);
        }

        String vendor = config.getProperty("vendor", "");
        if (!vendor.isEmpty() && !vendor.equals("null")) 
        {
            command.add("--vendor");
            command.add(vendor);
        }

        String description = config.getProperty("description", "");
        if (!description.isEmpty() && !description.equals("null")) 
        {
            command.add("--description");
            command.add(description);
        }

        String copyright = config.getProperty("copyright", "");
        if (!copyright.isEmpty() && !copyright.equals("null")) 
        {
            command.add("--copyright");
            command.add(copyright);
        }

        String jvmOptions = config.getProperty("jvm.options", "");
        if (!jvmOptions.isEmpty() && !jvmOptions.equals("null")) 
        {
            command.add("--java-options");
            command.add(jvmOptions);
        }

        String winConsole = config.getProperty("win.console", "false");
        if (winConsole.equals("true")) 
        { command.add("--win-console"); }

        System.out.println("jpackage command:");
        System.out.println(String.join(" ", command));
        System.out.println();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) 
        { throw new Exception("jpackage failed with exit code: " + exitCode); }
    }
}
