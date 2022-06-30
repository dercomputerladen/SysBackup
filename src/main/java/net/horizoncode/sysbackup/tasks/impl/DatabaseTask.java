package net.horizoncode.sysbackup.tasks.impl;

import lombok.Builder;
import lombok.Getter;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.horizoncode.sysbackup.tasks.Task;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.nio.charset.Charset;
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
        System.out.println(text);
        onDone();
        return;
      }

      if (databaseContent.isEmpty()) {
        System.err.println("database content is empty");
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
      System.out.println("Adding database to backup zip...");
      try (ZipFile zipFile = new ZipFile(getOutputFile())) {
        ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
        zipFile.setRunInThread(true);
        zipFile.addFile(outputSQLFile);
        ProgressBarBuilder pbb =
            new ProgressBarBuilder()
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(progressMonitor.getTotalWork())
                .setTaskName("Adding DB File...");

        try (ProgressBar pb = pbb.build()) {
          while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
            pb.stepTo(progressMonitor.getWorkCompleted());
            Thread.sleep(100);
          }
          pb.stepTo(progressMonitor.getTotalWork());
        } catch (Exception exception) {
          exception.printStackTrace();
          FileUtils.deleteQuietly(outputSQLFile);
          onDone();
        }
        progressMonitor.endProgressMonitor();
        FileUtils.deleteQuietly(outputSQLFile);
        onDone();
      } catch (Exception ex) {
        ex.printStackTrace();
        onDone();
      }

    } catch (IOException e) {
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
