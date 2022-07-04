package net.horizoncode.sysbackup.tasks.impl;

import lombok.Getter;
import net.horizoncode.sysbackup.tasks.Task;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
public class VacuumTask extends Task {

  private final File backupDir;
  private final TimeUnit unit;
  private final int value;

  public VacuumTask(File backupDir, TimeUnit unit, int value) {
    this.backupDir = backupDir;
    this.unit = unit;
    this.value = value;
  }

  @Override
  public void start() {
    if (backupDir.listFiles() != null) {
      Arrays.stream(Objects.requireNonNull(backupDir.listFiles()))
          .filter(file -> file.lastModified() + unit.toMillis(value) <= System.currentTimeMillis())
          .forEachOrdered(File::deleteOnExit);
    }

    onDone();
  }
}
