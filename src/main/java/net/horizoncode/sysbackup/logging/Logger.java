package net.horizoncode.sysbackup.logging;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Builder
public class Logger {
  private File logFile;

  public void log(LogLevel logLevel, String message, Object... args) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    String prefix =
        String.format(
            "[%s - %s] ",
            sdf.format(new Date()), Ansi.colorize(logLevel.name(), logLevel.getColor()));
    String line = prefix + message;
    System.out.printf(line + "\r\n", args);

    // append to logfile
    try {
      FileUtils.writeStringToFile(
          logFile, line.replaceAll("\u001B\\[[;\\d]*m", "") + "\r\n", StandardCharsets.UTF_8, true);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public enum LogLevel {
    INFO(Attribute.CYAN_TEXT()),
    WARN(Attribute.YELLOW_TEXT()),
    ERROR(Attribute.RED_TEXT());

    @Getter private final Attribute color;

    LogLevel(Attribute color) {
      this.color = color;
    }
  }
}
