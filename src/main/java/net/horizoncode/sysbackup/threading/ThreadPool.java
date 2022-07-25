package net.horizoncode.sysbackup.threading;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ThreadPool {

  @Getter private final ExecutorService pool;

  public ThreadPool(int corePoolSize, int maxPoolSize) {
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(corePoolSize);
    scheduledThreadPoolExecutor.setMaximumPoolSize(maxPoolSize);

    this.pool = scheduledThreadPoolExecutor;
  }
}
