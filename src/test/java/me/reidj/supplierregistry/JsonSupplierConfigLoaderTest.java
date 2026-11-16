package me.reidj.supplierregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSupplierConfigLoaderTest {

  @TempDir
  Path dir;

  @Test
  void load() throws Exception {
    Path file = Path.of(getClass().getResource("/suppliers.json").toURI());

    List<SupplierConfig> suppliers = JsonSupplierConfigLoader.load(file);

    assertEquals(List.of(
        new SupplierConfig("supplier-a", "https://api.supplier-a.example", "test-key-a"),
        new SupplierConfig("supplier-b", "https://api.supplier-b.example/v1", "test-key-b")
    ), suppliers);
  }

  @Test
  void replaceFromFile() throws Exception {
    Path file = Files.writeString(dir.resolve("suppliers.json"),
        "[{\"id\":\"mts\",\"baseUrl\":\"https://mts.example\",\"apiKey\":\"key-1\"}]");
    var registry = new SupplierRegistry(file);

    Files.writeString(file,
        "[{\"id\":\"beeline\",\"baseUrl\":\"https://beeline.example\",\"apiKey\":\"key-2\"}]");
    registry.replaceFrom(file);

    assertEquals("https://beeline.example", registry.getSupplier("beeline").baseUrl());
    assertThrows(IllegalArgumentException.class, () -> registry.getSupplier("mts"));
  }

  @Test
  void invalidJson() throws Exception {
    Path file = Files.writeString(dir.resolve("suppliers.json"), "not json");

    assertThrows(IOException.class, () -> JsonSupplierConfigLoader.load(file));
  }

  @Test
  void errorPointsToBrokenSupplier() throws Exception {
    Path file = Files.writeString(dir.resolve("suppliers.json"), """
        [
          {"id": "mts", "baseUrl": "https://mts.example", "apiKey": "key-1"},
          {"id": "beeline", "baseUrl": "https://beeline.example", "apiKey": ""}
        ]
        """);

    var e = assertThrows(IOException.class, () -> JsonSupplierConfigLoader.load(file));
    assertEquals("Invalid supplier JSON: at $[1]: apiKey must not be blank", e.getMessage());
  }

  @Test
  void missingFile() {
    assertThrows(IOException.class, () -> JsonSupplierConfigLoader.load(dir.resolve("nope.json")));
  }
}
