///usr/bin/env java --enable-preview --source 25 "$0" "$@" ; exit $?

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class fxbuild
{

    // ==================== CONFIG ====================

    private static final String PROJECT_DIR = ".";
    private static final String JAVA_HOME = System.getenv("JAVA_HOME");
    private static final String BUILD_DIR = "build-output";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    // ========================================================

    public static void main(String[] args)
    {
        try
        {
            printBanner();
            validateEnvironment();
            cleanBuildDirectory();
            buildWithMaven();
            printSuccess();
        }
        catch (Exception e)
        {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printBanner()
    {
        System.out.println("""
            ╔═══════════════════════════════════════════╗
            ║   JavaFX → jar Builder (Java 25)          ║
            ║   Maven Build                             ║
            ╚═══════════════════════════════════════════╝
            """);
    }

    private static void validateEnvironment() throws Exception
    {
        System.out.println("🔍 Validating enviroment...\n");

        if (JAVA_HOME == null || JAVA_HOME.isBlank())
        { throw new Exception("JAVA_HOME not set! Configure the enviroment variable"); }

        Path javaHomePath = Path.of(JAVA_HOME);
        if (!Files.exists(javaHomePath))
        { throw new Exception("Invalid JAVA_HOME: " + JAVA_HOME); }

        System.out.println("   ✓ JAVA_HOME: " + JAVA_HOME);

        String javaVersion = System.getProperty("java.version");
        System.out.println("   ✓ Java version: " + javaVersion);

        try
        {
            executeCommand("mvn --version", false);
            System.out.println("   ✓ Maven available");
        }

        catch (Exception e)
        { throw new Exception("Maven not found! Download Maven and add it to PATH"); }


        Path pomFile = Path.of(PROJECT_DIR, "pom.xml");
        if (!Files.exists(pomFile))
        { throw new Exception("pom.xml not found in: " + PROJECT_DIR); }
        System.out.println("   ✓ found pom.xml");

        System.out.println("\n✅ Enviroment validated!\n");
    }

    private static void cleanBuildDirectory() throws Exception
    {
        System.out.println("🧹 Cleaning build directory...\n");

        Path buildPath = Path.of(BUILD_DIR);
        if (Files.exists(buildPath))
        {
            deleteDirectory(buildPath);
            System.out.println("   ✓ Existing directory removed");
        }

        System.out.println();
    }

    private static void buildWithMaven() throws Exception
    {
        System.out.println("📦 Maven build...\n");

        executeCommand("mvn clean package -DskipTests", true);

        Path targetDir = Path.of(PROJECT_DIR, "target");
        Optional<Path> jarFile = Files.list(targetDir)
                .filter(p -> p.toString().endsWith(".jar") &&
                        !p.getFileName().toString().startsWith("original-"))
                .findFirst();

        if (jarFile.isEmpty())
        { throw new Exception("Failed to create jar"); }

        System.out.println("\n✅ jar created: " + jarFile.get().getFileName());
        System.out.println("   Size: " + Files.size(jarFile.get()) / 1024 + " KB\n");
    }

    private static void printSuccess()
    {
        System.out.println("""
            
            ╔═══════════════════════════════════════════╗
            ║        ✅ BUILD SUCCESSFUL! ✅           ║ 
            ╚═══════════════════════════════════════════╝
            
            """);
    }

    private static void executeCommand(String command, boolean printOutput) throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder();

        if (IS_WINDOWS)
        { pb.command("cmd.exe", "/c", command);}
        else
        { pb.command("sh", "-c", command); }

        pb.directory(new File(PROJECT_DIR));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (printOutput)
                { System.out.println("   " + line); }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0)
        { throw new Exception("Command failed with exit code: " + exitCode); }
    }

    private static void deleteDirectory(Path directory) throws IOException
    {
        if (Files.exists(directory))
        {
            Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(path ->
            {
                try
                { Files.delete(path); }
                catch (IOException e)
                { System.err.println("Cannot remove: " + path); }
            });
        }
    }
}
