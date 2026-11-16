package me.reidj.supplierregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SupplierConfigTest {

  @Test
  void blankFields() {
    assertThrows(IllegalArgumentException.class,
        () -> new SupplierConfig(" ", "https://mts.example", "key"));
    assertThrows(IllegalArgumentException.class, () -> new SupplierConfig("mts", null, "key"));
    assertThrows(IllegalArgumentException.class,
        () -> new SupplierConfig("mts", "https://mts.example", ""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"mts.example", "ftp://mts.example", "https://mts.example?key=1",
      "https://user:pass@mts.example"})
  void invalidBaseUrl(String url) {
    assertThrows(IllegalArgumentException.class, () -> new SupplierConfig("mts", url, "key"));
  }

  @Test
  void apiKeyIsHiddenInToString() {
    var config = new SupplierConfig("mts", "https://mts.example", "secret");

    assertFalse(config.toString().contains("secret"));
  }
}
