import net.horizoncode.sysbackup.cli.CLIProcessor;

import java.io.File;
import java.net.URISyntaxException;

public class Bootstrapper {

  public static void main(String[] args) throws URISyntaxException {

    File jarFile =
        new File(Bootstrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    File executionPath =
        new File(
            new File(Bootstrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent());

    if (!jarFile.isFile()) System.out.println("Dev environment detected!");

    CLIProcessor cliProcessor = new CLIProcessor();
    cliProcessor.startCLI(
        args, jarFile.isFile() ? executionPath : new File(System.getProperty("user.dir")));
  }
}
