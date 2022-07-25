package net.horizoncode.sysbackup.tasks.impl;

import lombok.Builder;
import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.horizoncode.sysbackup.SysBackup;
import net.horizoncode.sysbackup.logging.Logger;
import net.horizoncode.sysbackup.tasks.Task;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Getter
public class DatabaseTask extends Task {

  private final DatabaseCredentials databaseCredentials;

  private final File outputFile;

  public DatabaseTask(DatabaseCredentials credentials, File outputFile) {
    this.databaseCredentials = credentials;
    this.outputFile = outputFile;
  }

  @Override
  public void start() {
    try {
      Logger logger = SysBackup.getLogger();
      String commandArgs =
          "mysqldump -u "
              + getDatabaseCredentials().username
              + " -p"
              + new String(getDatabaseCredentials().password)
              + " "
              + getDatabaseCredentials().database;

      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(commandArgs);

      String databaseContent = "";

      while (process.isAlive()) {
        try {
          Thread.sleep(1000);
          databaseContent =
              new BufferedReader(
                      new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))
                  .lines()
                  .collect(Collectors.joining("\n"));
        } catch (InterruptedException e) {
          logger.log(Logger.LogLevel.ERROR, e.getMessage());
          throw new RuntimeException(e);
        }
      }

      int exitValue = process.exitValue();

      if (exitValue != 0) {
        String text =
            new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        logger.log(Logger.LogLevel.ERROR, text);
        onDone();
        return;
      }

      if (databaseContent.isEmpty()) {
        logger.log(Logger.LogLevel.ERROR, "database content is empty");
        onDone();
        return;
      }

      File outputSQLFile =
          new File(
              outputFile.getParent(),
              String.format(
                  "%s-%s.sql",
                  getDatabaseCredentials().database, RandomStringUtils.random(16, true, true)));

      BufferedWriter writer = new BufferedWriter(new FileWriter(outputSQLFile));
      writer.write(databaseContent);
      writer.close();
      logger.log(Logger.LogLevel.INFO, "Adding database to backup zip...");
      try (ZipFile zipFile = new ZipFile(getOutputFile())) {
        ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
        zipFile.setRunInThread(true);
        zipFile.addFile(outputSQLFile);
        ProgressBarBuilder pbb =
            new ProgressBarBuilder()
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(progressMonitor.getTotalWork())
                .setTaskName("Adding DB File...")
                .setUnit("MiB", 1048576);

        try (ProgressBar pb = pbb.build()) {
          while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
            pb.stepTo(progressMonitor.getWorkCompleted());
            Thread.sleep(100);
          }
          pb.stepTo(progressMonitor.getTotalWork());
        } catch (Exception exception) {
          logger.log(Logger.LogLevel.ERROR, exception.getMessage());
          exception.printStackTrace();
          outputSQLFile.deleteOnExit();
          onDone();
        }
        progressMonitor.endProgressMonitor();
        outputSQLFile.deleteOnExit();
        onDone();
      } catch (Exception ex) {
        logger.log(Logger.LogLevel.ERROR, ex.getMessage());
        ex.printStackTrace();
        onDone();
      }

    } catch (IOException e) {
      SysBackup.getLogger().log(Logger.LogLevel.ERROR, e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Builder
  public static class DatabaseCredentials {
    private final String username;
    private final char[] password;
    private final String database;
  }
}
