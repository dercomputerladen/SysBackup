package net.horizoncode.sysbackup.tasks;

import lombok.Builder;
import lombok.Getter;
import net.horizoncode.sysbackup.SysBackup;
import net.horizoncode.sysbackup.config.Config;
import net.horizoncode.sysbackup.logging.Logger;
import net.horizoncode.sysbackup.tasks.impl.DatabaseTask;
import net.horizoncode.sysbackup.tasks.impl.FileSystemTask;
import net.horizoncode.sysbackup.tasks.impl.VacuumTask;
import net.horizoncode.sysbackup.threading.ThreadPool;
import org.apache.commons.io.FilenameUtils;
import org.tomlj.TomlArray;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

@Builder
@Getter
public class TaskBuilder {

  private final Config taskConfig;

  private final String taskName;

  @Builder.Default private final LinkedBlockingQueue<Task> taskList = new LinkedBlockingQueue<>();
  @Builder.Default @Getter private final ThreadPool threadPool = new ThreadPool(3, 10);

  private final File executionPath;

  public void start() {

    Logger logger = SysBackup.getLogger();

    File rootBackupDir = new File(executionPath, "backups");
    if (!rootBackupDir.exists())
      if (!rootBackupDir.mkdir()) {
        logger.log(Logger.LogLevel.ERROR, "Failed to create root backup directory!");
        System.exit(2);
      }

    File backupDir = new File(rootBackupDir, getTaskName());
    if (!backupDir.exists())
      if (!backupDir.mkdir()) {
        logger.log(Logger.LogLevel.ERROR, "Failed to create backup directory!");
        System.exit(2);
      }

    SimpleDateFormat sdf =
        new SimpleDateFormat(
            getTaskConfig().getStringOrDefault("general.dateFormat", "yyyy-MM-dd HH-mm-ss"));
    String fileName =
        getTaskConfig().getStringOrDefault("general.outputFile", "{date} - {taskName}") + ".zip";
    boolean doVAC = getTaskConfig().getBooleanOrDefault("vacuum.enabled", false);
    boolean doFS = getTaskConfig().getBooleanOrDefault("filesystem.enabled", false);
    boolean doDB = getTaskConfig().getBooleanOrDefault("mysql.enabled", false);

    fileName =
        fileName
            .replace(
                "{taskName}",
                FilenameUtils.removeExtension(getTaskConfig().getConfigFile().getName()))
            .replace("{date}", sdf.format(new Date()));

    File outputFile = new File(backupDir, fileName);

    if (doVAC) {
      ChronoUnit unit =
          ChronoUnit.valueOf(
              getTaskConfig().getStringOrDefault("vacuum.unit", ChronoUnit.DAYS.name()));

      int value = getTaskConfig().getIntOrDefault("vacuum.time", 5);

      logger.log(
          Logger.LogLevel.INFO, "Adding VacuumTask with lifetime of %d %s%n", value, unit.name());
      taskList.add(
          new VacuumTask(backupDir, unit, value) {
            @Override
            public void onDone() {
              getThreadPool().getPool().submit(() -> executeNextTask());
            }
          });
    }

    if (doFS && getTaskConfig().getToml().contains("filesystem.targets")) {
      logger.log(Logger.LogLevel.INFO, "Adding FileSystemTask...");
      TomlArray filesArray = getTaskConfig().getArray("filesystem.targets");

      IntStream.range(0, filesArray.size())
          .forEach(
              value -> {
                String target = filesArray.getString(value);
                logger.log(Logger.LogLevel.INFO, "Adding \"%s\"", target);
                taskList.add(
                    new FileSystemTask(target, outputFile) {
                      @Override
                      public void onDone() {
                        getThreadPool().getPool().submit(() -> executeNextTask());
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

        logger.log(Logger.LogLevel.INFO, "Adding DatabaseTask for database \"%s\"", database);

        taskList.add(
            new DatabaseTask(databaseCredentials, outputFile) {
              @Override
              public void onDone() {
                getThreadPool().getPool().submit(() -> executeNextTask());
              }
            });
      } else {
        logger.log(Logger.LogLevel.ERROR, "username, password or database is empty.");
      }
    }

    getThreadPool().getPool().submit(this::executeNextTask);
  }

  private void executeNextTask() {
    Task nextTask = taskList.poll();
    if (nextTask != null) nextTask.start();
    else {
      SysBackup.getLogger().log(Logger.LogLevel.INFO, "Backup completed!");
      System.exit(0);
    }
  }
}
