package net.horizoncode.sysbackup.cli;

import net.horizoncode.sysbackup.SysBackup;
import net.horizoncode.sysbackup.config.Config;
import net.horizoncode.sysbackup.logging.Logger;
import net.horizoncode.sysbackup.tasks.TaskBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class CLIProcessor {

  public static void usage() {
    String[] usage = {
      "Usage: java -jar sysbackup.jar [action] <args>",
      "  Backup:",
      "    backup <backup task>           create a backup based on a backup task configuration file",
      "  Miscellaneous:",
      "    checkTaskConf    <file name>   checks a backup task configuration file",
      "    generateTaskConf <file name>   generate a example backup task configuration file",
      "  Examples:",
      "    java -jar sysbackup.jar generateTaskConf magento",
      "    java -jar sysbackup.jar backup magento"
    };
    Arrays.stream(usage).forEach(System.out::println);
  }

  public void startCLI(String[] args, File executionPath) {
    try {
      if ((args == null) || (args.length == 0)) {
        usage();
        return;
      }

      Logger logger = SysBackup.getLogger();

      for (int index = 0; index < args.length; index++) {
        switch (args[index].toLowerCase(Locale.ROOT)) {
          case "backup":
            {
              if (args.length <= 1) {
                logger.log(Logger.LogLevel.WARN, "Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir())
                  logger.log(Logger.LogLevel.ERROR, "Failed to create tasks folder!");
              File taskFile = new File(tasksFolder, fileName + ".toml");
              if (!taskFile.exists()) {
                logger.log(Logger.LogLevel.ERROR, "TaskFile %s.toml does not exist!", fileName);
                return;
              }

              logger.log(Logger.LogLevel.INFO, "setupping TaskBuilder...");
              Config taskConfig = new Config(taskFile);
              TaskBuilder taskBuilder =
                  TaskBuilder.builder()
                      .executionPath(executionPath)
                      .taskName(FilenameUtils.removeExtension(taskFile.getName()))
                      .taskConfig(taskConfig)
                      .build();
              taskBuilder.start();
              break;
            }
          case "generatetaskconf":
            {
              if (args.length <= 1) {
                logger.log(Logger.LogLevel.ERROR, "Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir())
                  logger.log(Logger.LogLevel.ERROR, "Failed to create tasks folder!");
              logger.log(Logger.LogLevel.INFO, "Saving task config %s.toml...", fileName);
              try {
                FileUtils.copyInputStreamToFile(
                    Objects.requireNonNull(
                        getClass().getResourceAsStream("/" + "exampletask.toml")),
                    new File(tasksFolder, fileName + ".toml"));
              } catch (IOException exception) {
                logger.log(Logger.LogLevel.ERROR, "Failed to save task config.");
              }
              logger.log(Logger.LogLevel.INFO, "%s.toml saved!", fileName);
              break;
            }
          case "checktaskconf":
            {
              if (args.length <= 1) {
                logger.log(Logger.LogLevel.ERROR, "Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir())
                  logger.log(Logger.LogLevel.ERROR, "Failed to create tasks folder!");
              File taskFile = new File(tasksFolder, fileName + ".toml");
              if (!taskFile.exists()) {
                logger.log(Logger.LogLevel.ERROR, "TaskFile %s.toml does not exist!", fileName);
                return;
              }
              TomlParseResult toml;
              try {
                toml = Toml.parse(taskFile.toPath());
              } catch (IOException e) {
                logger.log(Logger.LogLevel.ERROR, "failed to read TaskFile.");
                throw new RuntimeException(e);
              }
              if (toml.hasErrors()) {
                logger.log(
                    Logger.LogLevel.ERROR,
                    "TaskFile checked: found %d issues!:\n",
                    (long) toml.errors().size());
                toml.errors().forEach(error -> logger.log(Logger.LogLevel.ERROR, error.toString()));
              } else {
                logger.log(Logger.LogLevel.INFO, "TaskFile checked successfully: no issues found!");
              }
              break;
            }
          default:
            if (index == 0) {
              usage();
              return;
            }
        }
      }
    } catch (Throwable t) {
      SysBackup.getLogger().log(Logger.LogLevel.ERROR, t.getMessage());
      t.printStackTrace();
    }
  }
}
