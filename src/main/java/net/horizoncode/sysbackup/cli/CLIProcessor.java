package net.horizoncode.sysbackup.cli;

import net.horizoncode.sysbackup.config.Config;
import net.horizoncode.sysbackup.tasks.TaskBuilder;
import org.apache.commons.io.FileUtils;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.IOException;
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
    for (String u : usage) {
      System.out.println(u);
    }
  }

  public void startCLI(String[] args, File executionPath) {
    try {
      if ((args == null) || (args.length == 0)) {
        usage();
        return;
      }

      for (int index = 0; index < args.length; index++) {
        switch (args[index].toLowerCase(Locale.ROOT)) {
          case "backup":
            {
              if (args.length <= 1) {
                System.err.println("Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir()) System.err.println("Failed to create tasks folder!");
              File taskFile = new File(tasksFolder, fileName + ".toml");
              if (!taskFile.exists()) {
                System.err.println("TaskFile " + fileName + ".toml does not exist!");
                return;
              }

              Config taskConfig = new Config(taskFile);
              TaskBuilder taskBuilder =
                  TaskBuilder.builder().executionPath(executionPath).taskConfig(taskConfig).build();
              taskBuilder.start();
              break;
            }
          case "generatetaskconf":
            {
              if (args.length <= 1) {
                System.err.println("Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir()) System.err.println("Failed to create tasks folder!");
              System.out.println("Saving task config " + fileName + ".toml...");
              FileUtils.copyInputStreamToFile(
                  Objects.requireNonNull(getClass().getResourceAsStream("/" + "exampletask.toml")),
                  new File(tasksFolder, fileName + ".toml"));
              System.out.println(fileName + ".toml saved!");
              break;
            }
          case "checktaskconf":
            {
              if (args.length <= 1) {
                System.err.println("Please specify a output task config name!");
                return;
              }
              String fileName = args[1];
              File tasksFolder = new File(executionPath, "tasks");
              if (!tasksFolder.exists())
                if (!tasksFolder.mkdir()) System.err.println("Failed to create tasks folder!");
              File taskFile = new File(tasksFolder, fileName + ".toml");
              if (!taskFile.exists()) {
                System.err.println("TaskFile " + fileName + ".toml does not exist!");
                return;
              }
              TomlParseResult toml;
              try {
                toml = Toml.parse(taskFile.toPath());
              } catch (IOException e) {
                System.err.println("failed to read TaskFile.");
                throw new RuntimeException(e);
              }
              if (toml.hasErrors()) {
                System.err.printf(
                    "TaskFile checked: found %d issues!:\n", (long) toml.errors().size());
                toml.errors().forEach(error -> System.err.println(error.toString()));
              } else {
                System.out.println("TaskFile checked successfully: no issues found!");
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
      t.printStackTrace();
    }
  }
}
