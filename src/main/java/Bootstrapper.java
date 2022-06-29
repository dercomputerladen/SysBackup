import net.horizoncode.sysbackup.cli.CLIProcessor;

public class Bootstrapper {

  public static void main(String[] args) {
    CLIProcessor cliProcessor = new CLIProcessor();
    cliProcessor.startCLI(args);
  }
}
