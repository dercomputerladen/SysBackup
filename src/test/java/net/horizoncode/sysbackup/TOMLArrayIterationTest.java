package net.horizoncode.sysbackup;

import net.horizoncode.sysbackup.config.Config;
import org.junit.Test;
import org.tomlj.TomlArray;

import java.io.File;
import java.util.stream.IntStream;

public class TOMLArrayIterationTest {

  @Test
  public void testTOMLIteration() {
    File tasksFolder = new File("tasks");
    if (!tasksFolder.exists())
      if (!tasksFolder.mkdir()) System.err.println("Failed to create tasks folder!");
    Config config = new Config(new File(tasksFolder, "magento.toml"));
    if (config.getToml().contains("filesystem.targets")) {
      TomlArray filesArray = config.getArray("filesystem.targets");

      IntStream.range(0, filesArray.size())
          .forEach(
              value -> {
                String target = filesArray.getString(value);
                System.out.println(target);
              });
    }
  }
}
