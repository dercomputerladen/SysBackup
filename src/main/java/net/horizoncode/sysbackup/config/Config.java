package net.horizoncode.sysbackup.config;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Getter
public class Config {

  private final File configFile;

  private TomlParseResult toml;

  private boolean justCreated;

  public Config(File configFile) {
    this.configFile = configFile;
    if (!configFile.exists()) {
      try {
        FileUtils.copyInputStreamToFile(
            Objects.requireNonNull(getClass().getResourceAsStream("/" + configFile.getName())),
            configFile);
        justCreated = true;
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      if (configFile.isDirectory()) {
        try {
          FileUtils.copyInputStreamToFile(
              Objects.requireNonNull(getClass().getResourceAsStream("/" + configFile.getName())),
              configFile);
          justCreated = true;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    if (justCreated) return;

    try {
      toml = Toml.parse(configFile.toPath());
      if (toml.hasErrors()) {
        toml.errors().forEach(error -> System.err.println(error.toString()));
        System.exit(-1);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int getIntOrDefault(String key, int defaultValue) {
    return getToml().contains(key) && getToml().get(key) instanceof Number
        ? Math.toIntExact((long) getToml().get(key))
        : defaultValue;
  }

  public boolean getBooleanOrDefault(String key, boolean defaultValue) {
    return getToml().contains(key) && getToml().get(key) instanceof Boolean
        ? getToml().getBoolean(key)
        : defaultValue;
  }

  public String getStringOrDefault(String key, String defaultValue) {
    return getToml().contains(key) ? getToml().getString(key) : defaultValue;
  }

  public TomlArray getArray(String key) {
    return getToml().getArrayOrEmpty(key);
  }
}
