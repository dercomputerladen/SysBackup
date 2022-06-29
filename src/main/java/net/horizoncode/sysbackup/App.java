package net.horizoncode.sysbackup;

import net.horizoncode.sysbackup.cli.CLIProcessor;

public class App {
  private static App instance;

  public static App getInstance() {
    if (instance != null) return instance;
    return instance = new App();
  }

  public void start(String[] args){
    CLIProcessor cliProcessor = new CLIProcessor();
    cliProcessor.startCLI(args);
  }
}
