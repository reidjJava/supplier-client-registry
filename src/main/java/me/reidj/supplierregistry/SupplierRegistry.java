package me.reidj.supplierregistry;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class SupplierRegistry {

  private final AtomicReference<Map<String, SupplierClient>> clients = new AtomicReference<>(Map.of());
  private final RestClient http;

  public SupplierRegistry(List<SupplierConfig> suppliers) {
    this(suppliers, defaultHttpBuilder());
  }

  public SupplierRegistry(Path path) throws IOException {
    this(JsonSupplierConfigLoader.load(path));
  }

  public SupplierRegistry(List<SupplierConfig> suppliers, RestClient.Builder httpBuilder) {
    if (httpBuilder == null) {
      throw new IllegalArgumentException("httpBuilder must not be null");
    }

    this.http = httpBuilder.clone().build();
    replaceAll(suppliers);
  }

  public SupplierConfig getSupplier(String supplierId) {
    return client(supplierId).config();
  }

  public boolean isAvailable(String supplierId, String phone) {
    return client(supplierId).isAvailable(phone);
  }

  public void replaceAll(List<SupplierConfig> suppliers) {
    List<SupplierConfig> validated = SupplierConfig.validatedCopy(suppliers);
    var replacement = new HashMap<String, SupplierClient>();
    for (SupplierConfig config : validated) {
      replacement.put(config.id(), new SupplierClient(config, http));
    }
    clients.set(Map.copyOf(replacement));
  }

  public void replaceFrom(Path path) throws IOException {
    replaceAll(JsonSupplierConfigLoader.load(path));
  }

  private SupplierClient client(String supplierId) {
    SupplierConfig.requireText(supplierId, "supplierId");
    SupplierClient client = clients.get().get(supplierId);
    if (client == null) {
      throw new IllegalArgumentException("Unknown supplier: " + supplierId);
    }
    return client;
  }

  private static RestClient.Builder defaultHttpBuilder() {
    HttpClient transport = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    var factory = new JdkClientHttpRequestFactory(transport);
    factory.setReadTimeout(Duration.ofSeconds(5));
    return RestClient.builder().requestFactory(factory);
  }
}
