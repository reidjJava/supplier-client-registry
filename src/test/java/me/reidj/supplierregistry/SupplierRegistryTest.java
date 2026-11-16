package me.reidj.supplierregistry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupplierRegistryTest {

  private final SupplierConfig mts = new SupplierConfig("mts", "https://mts.example", "key-1");
  private final SupplierConfig beeline = new SupplierConfig("beeline", "https://beeline.example", "key-2");

  @Test
  void getSupplier() {
    var registry = new SupplierRegistry(List.of(mts, beeline));

    assertEquals(mts, registry.getSupplier("mts"));
    assertEquals(beeline, registry.getSupplier("beeline"));
    assertThrows(IllegalArgumentException.class, () -> registry.getSupplier("tele2"));
  }

  @Test
  void replaceAll() {
    var registry = new SupplierRegistry(List.of(mts));

    registry.replaceAll(List.of(beeline));

    assertEquals(beeline, registry.getSupplier("beeline"));
    assertThrows(IllegalArgumentException.class, () -> registry.getSupplier("mts"));
  }

  @Test
  void changingSourceListDoesNotAffectRegistry() {
    var suppliers = new ArrayList<>(List.of(mts));
    var registry = new SupplierRegistry(suppliers);

    suppliers.clear();

    assertEquals(mts, registry.getSupplier("mts"));
  }

  @Test
  void duplicateIds() {
    var copy = new SupplierConfig("mts", "https://other.example", "key-3");

    assertThrows(IllegalArgumentException.class, () -> new SupplierRegistry(List.of(mts, copy)));
  }

  @Test
  void failedReplaceKeepsOldSuppliers() {
    var registry = new SupplierRegistry(List.of(mts));
    var copy = new SupplierConfig("beeline", "https://other.example", "key-3");

    assertThrows(IllegalArgumentException.class,
        () -> registry.replaceAll(List.of(beeline, copy)));

    assertEquals(mts, registry.getSupplier("mts"));
  }
}
