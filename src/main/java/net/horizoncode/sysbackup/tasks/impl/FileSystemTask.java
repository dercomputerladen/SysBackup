package net.horizoncode.sysbackup.tasks.impl;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.horizoncode.sysbackup.SysBackup;
import net.horizoncode.sysbackup.logging.Logger;
import net.horizoncode.sysbackup.tasks.Task;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.File;
import java.nio.file.Paths;

public class FileSystemTask extends Task {

  private final File target;
  private File outputZipFile;

  public FileSystemTask(String folderOrFilePath, File outputZipFile) {
    this.target = Paths.get(folderOrFilePath).toFile();
    Logger logger = SysBackup.getLogger();
    if (!target.exists()) {
      onDone();
      logger.log(
          Logger.LogLevel.ERROR, "File or folder named \"%s\" does not exist.", folderOrFilePath);
      System.exit(2);
      return;
    }

    this.outputZipFile = outputZipFile;
  }

  @Override
  public void start() {
    Logger logger = SysBackup.getLogger();
    try (ZipFile zipFile = new ZipFile(outputZipFile)) {
      logger.log(Logger.LogLevel.INFO, "Indexing files...");
      ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
      zipFile.setRunInThread(true);
      zipFile.addFolder(target);
      ProgressBarBuilder pbb =
          new ProgressBarBuilder()
              .setStyle(ProgressBarStyle.ASCII)
              .setInitialMax(progressMonitor.getTotalWork())
              .setTaskName("Adding Files...")
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
        onDone();
      }
      progressMonitor.endProgressMonitor();
      onDone();
    } catch (Exception ex) {
      logger.log(Logger.LogLevel.ERROR, ex.getMessage());
      ex.printStackTrace();
      onDone();
    }
  }
}
