import net.horizoncode.sysbackup.cli.CLIProcessor;

import java.io.File;
import java.net.URISyntaxException;

public class Bootstrapper {

  public static void main(String[] args) throws URISyntaxException {

    File executionPath =
        new File(
            new File(Bootstrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent());

    CLIProcessor cliProcessor = new CLIProcessor();
    cliProcessor.startCLI(args, executionPath);
  }
}
