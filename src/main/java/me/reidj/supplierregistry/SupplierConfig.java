package me.reidj.supplierregistry;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

public record SupplierConfig(String id, String baseUrl, String apiKey) {

  public SupplierConfig {
    requireText(id, "id");
    requireText(baseUrl, "baseUrl");
    requireText(apiKey, "apiKey");
    URI uri;

    try {
      uri = URI.create(baseUrl);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("baseUrl must be an absolute HTTP/HTTPS URL");
    }

    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "baseUrl must be an absolute HTTP/HTTPS URL without credentials, query or fragment"
      );
    }

    if (apiKey.chars().anyMatch(character -> character < 0x20 || character > 0x7e)) {
      throw new IllegalArgumentException("apiKey must contain printable ASCII header characters");
    }
  }

  static void requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  static List<SupplierConfig> validatedCopy(List<SupplierConfig> configs) {
    if (configs == null) {
      throw new IllegalArgumentException("suppliers must not be null");
    }

    List<SupplierConfig> copy;

    try {
      copy = List.copyOf(configs);
    } catch (NullPointerException exception) {
      throw new IllegalArgumentException("supplier must not be null");
    }

    var ids = new HashSet<String>();

    for (SupplierConfig config : copy) {
      if (!ids.add(config.id())) {
        throw new IllegalArgumentException("Duplicate supplier id: " + config.id());
      }
    }

    return copy;
  }

  @Override
  public String toString() {
    return "SupplierConfig[id=" + id + ", baseUrl=" + baseUrl + ", apiKey=<redacted>]";
  }
}
