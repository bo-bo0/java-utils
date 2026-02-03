import java.lang.Process;
import java.lang.ProcessBuilder;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class fxjarrun
{
    private static final String JAVAFX_LIB = "D:\\SDKs\\JavaFx\\javafx-sdk-25.0.1\\lib";
    private static final String JAVAFX_MODULES = "javafx.controls,javafx.fxml";
    private static final String JAR_NAME = "target\\buildme-1.0-SNAPSHOT.jar;";
    private static final String MAIN_CLASS = "com.example.buildme.HelloApplication";

    public static void main(String[] args) throws IOException
    {
        if (args.length > 1)
        {
            System.out.print("Error: this command only takes either zero or one parameter");
            System.exit(1);
        }

        var pb = new ProcessBuilder
        (
            "java",
            "--enable-preview",
            "--module-path", JAVAFX_LIB,
            "--add-modules", JAVAFX_MODULES,
            "-cp", JAR_NAME + ";" + JAVAFX_LIB + "\\*",
            MAIN_CLASS
        );

        if (args.length == 1)
        {
            if (args[0].equals("-o"))
            {
                String cmd = String.join(" ", pb.command());
                System.out.println(cmd);
                System.exit(0);
            }

            else
            {
                System.out.println("Error: invalid command parameter '" + args[0] + "'");
                System.exit(1);
            }
        }

        Process p = null;

        try
        { p = pb.start(); }
        catch (IOException ex)
        {
            System.out.println("Failed to run command");
            System.exit(1);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            { System.out.println(line); }
        }
    }
}