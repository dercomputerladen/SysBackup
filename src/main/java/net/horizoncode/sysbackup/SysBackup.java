package net.horizoncode.sysbackup;

import lombok.Getter;
import net.horizoncode.sysbackup.cli.CLIProcessor;
import net.horizoncode.sysbackup.logging.Logger;

import java.io.File;
import java.net.URISyntaxException;

public class SysBackup {

  @Getter private static final Logger logger = Logger.builder().logFile(new File("log")).build();

  public void start(String[] args) throws URISyntaxException {
    File jarFile =
        new File(SysBackup.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    File executionPath =
        new File(
            new File(SysBackup.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent());

    if (!jarFile.isFile()) getLogger().log(Logger.LogLevel.INFO, "Dev environment detected!");

    CLIProcessor cliProcessor = new CLIProcessor();
    cliProcessor.startCLI(
        args, jarFile.isFile() ? executionPath : new File(System.getProperty("user.dir")));
  }
}
