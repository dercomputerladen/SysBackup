package net.horizoncode.sysbackup.tasks;

import lombok.Builder;
import lombok.Getter;
import net.horizoncode.sysbackup.config.Config;
import net.horizoncode.sysbackup.tasks.impl.DatabaseTask;
import net.horizoncode.sysbackup.tasks.impl.FileSystemTask;
import org.apache.commons.io.FilenameUtils;
import org.tomlj.TomlArray;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

@Builder
@Getter
public class TaskBuilder {

  private final Config taskConfig;

  @Builder.Default private final LinkedBlockingQueue<Task> taskList = new LinkedBlockingQueue<>();

  private final File executionPath;

  public void start() {

    File backupDir = new File(executionPath, "backups");
    if (!backupDir.exists())
      if (!backupDir.mkdir()) {
        System.err.println("Failed to create backups directory!");
        System.exit(2);
      }

    SimpleDateFormat sdf =
        new SimpleDateFormat(
            getTaskConfig().getStringOrDefault("general.dateFormat", "yyyy-MM-dd HH-mm-ss"));
    String fileName =
        getTaskConfig().getStringOrDefault("general.outputFile", "{date} - {taskName}") + ".zip";

    boolean doFS = getTaskConfig().getBooleanOrDefault("filesystem.enabled", false);
    boolean doDB = getTaskConfig().getBooleanOrDefault("mysql.enabled", false);

    fileName =
        fileName
            .replace(
                "{taskName}",
                FilenameUtils.removeExtension(getTaskConfig().getConfigFile().getName()))
            .replace("{date}", sdf.format(new Date()));

    File outputFile = new File(backupDir, fileName);
    if (doFS && getTaskConfig().getToml().contains("filesystem.targets")) {
      TomlArray filesArray = getTaskConfig().getArray("filesystem.targets");

      IntStream.range(0, filesArray.size())
          .forEach(
              value -> {
                String target = filesArray.getString(value);
                taskList.add(
                    new FileSystemTask(target, outputFile) {
                      @Override
                      public void onDone() {
                        executeNextTask();
                      }
                    });
              });
    }

    if (doDB) {
      String database = getTaskConfig().getStringOrDefault("mysql.database", "");
      String user = getTaskConfig().getStringOrDefault("mysql.user", "");
      String password = getTaskConfig().getStringOrDefault("mysql.password", "");

      if (!database.isEmpty() && !user.isEmpty() && !password.isEmpty()) {
        DatabaseTask.DatabaseCredentials databaseCredentials =
            DatabaseTask.DatabaseCredentials.builder()
                .database(database)
                .username(user)
                .password(password.toCharArray())
                .build();

        taskList.add(
            new DatabaseTask(databaseCredentials, outputFile) {
              @Override
              public void onDone() {
                executeNextTask();
              }
            });
      } else {
        System.err.println("username, password or database is empty.");
      }
    }

    executeNextTask();
  }

  private void executeNextTask() {
    Task nextTask = taskList.poll();
    if (nextTask != null) nextTask.start();
    else {
      System.out.println("Backup completed!");
      System.exit(0);
    }
  }
}
