package net.horizoncode.sysbackup.tasks.impl;

import lombok.Getter;
import net.horizoncode.sysbackup.tasks.Task;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;

@Getter
public class VacuumTask extends Task {

  private final File backupDir;
  private final ChronoUnit unit;
  private final int value;

  public VacuumTask(File backupDir, ChronoUnit unit, int value) {
    this.backupDir = backupDir;
    this.unit = unit;
    this.value = value;
  }

  @Override
  public void start() {
    if (backupDir.listFiles() != null) {
      Arrays.stream(Objects.requireNonNull(backupDir.listFiles()))
          .filter(
              file ->
                  file.lastModified() + Duration.of(value, unit).toMillis()
                      <= System.currentTimeMillis())
          .forEachOrdered(File::deleteOnExit);
    }

    onDone();
  }
}
