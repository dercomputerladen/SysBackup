package net.horizoncode.sysbackup.tasks.impl;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
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
    if (!target.exists()) {
      onDone();
      System.err.println("File or folder named \"" + folderOrFilePath + "\" does not exist.");
      System.exit(2);
      return;
    }

    this.outputZipFile = outputZipFile;
  }

  @Override
  public void start() {
    try (ZipFile zipFile = new ZipFile(outputZipFile)) {
      System.out.println("Indexing files...");
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
        exception.printStackTrace();
        onDone();
      }
      progressMonitor.endProgressMonitor();
      onDone();
    } catch (Exception ex) {
      ex.printStackTrace();
      onDone();
    }
  }
}
