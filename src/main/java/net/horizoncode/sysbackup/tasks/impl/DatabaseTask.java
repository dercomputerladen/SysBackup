package net.horizoncode.sysbackup.tasks.impl;

import lombok.Builder;
import lombok.Getter;
import net.horizoncode.sysbackup.tasks.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
      String[] commandArgs =
          new String[] {
            "mysqldump",
            "-u " + getDatabaseCredentials().username,
            "-p" + new String(getDatabaseCredentials().password),
            getDatabaseCredentials().database
          };
      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(commandArgs);

      InputStream inputStream = process.getInputStream();
      FileUtils.copyInputStreamToFile(
          inputStream,
          new File(
              outputFile.getParent(),
              String.format(
                  "%s-%s.sql",
                  getDatabaseCredentials().database, RandomStringUtils.random(16, true, true))));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Builder
  private static class DatabaseCredentials {
    private final String username;
    private final char[] password;
    private final String database;
  }
}
