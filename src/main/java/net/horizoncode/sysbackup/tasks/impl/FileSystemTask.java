package net.horizoncode.sysbackup.tasks.impl;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import me.tongfei.progressbar.TerminalUtils;
import net.horizoncode.sysbackup.tasks.Task;
import net.horizoncode.sysbackup.threading.ThreadPool;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class FileSystemTask extends Task {

  private final File target;
  private File outputZipFile;

  private final ThreadPool threadPool;

  public FileSystemTask(String folderOrFilePath, File outputZipFile) {
    this.threadPool = new ThreadPool(3, 10);
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
    threadPool
        .getPool()
        .submit(
            () -> {
              int terminalWidth = -1;
              try {
                Terminal terminal = TerminalBuilder.terminal();
                try (terminal) {
                  terminalWidth = terminal.getWidth();
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              try (ZipFile zipFile = new ZipFile(outputZipFile)) {
                System.out.println("Indexing files...");
                ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
                zipFile.setRunInThread(true);
                zipFile.addFolder(target);
                ProgressBarBuilder pbb =
                    new ProgressBarBuilder()
                        .setStyle(ProgressBarStyle.ASCII)
                        .setInitialMax(progressMonitor.getTotalWork())
                        .setTaskName("Adding Files...");

                if (terminalWidth != -1) pbb.setMaxRenderedLength(terminalWidth);

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
            });
  }
}
