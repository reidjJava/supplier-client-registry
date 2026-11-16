package me.reidj.supplierregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

final class SupplierClient {

  private static final String API_KEY_HEADER = "X-API-Key";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final SupplierConfig config;
  private final RestClient http;
  private final String subscribersUrl;

  SupplierClient(SupplierConfig config, RestClient http) {
    this.config = config;
    this.http = http;
    this.subscribersUrl = URI
        .create(config.baseUrl())
        .toASCIIString()
        .replaceAll("/+$", "") + "/subscribers/";
  }

  SupplierConfig config() {
    return config;
  }

  boolean isAvailable(String phone) {
    SupplierConfig.requireText(phone, "phone");
    String segment = UriUtils.encode(phone, StandardCharsets.UTF_8);

    if (phone.equals(".") || phone.equals("..")) {
      segment = phone.replace(".", "%2E");
    }

    return Boolean.TRUE.equals(http.get()
        .uri(URI.create(subscribersUrl + segment))
        .header(API_KEY_HEADER, config.apiKey())
        .accept(MediaType.APPLICATION_JSON)
        .exchange((request, response) -> {
          int status = response.getStatusCode().value();

          if (status == 404) {
            return false;
          }

          if (status != 200) {
            throw new RestClientResponseException(
                "Supplier '" + config.id() + "' returned HTTP " + status,
                response.getStatusCode(), response.getStatusText(), response.getHeaders(), null,
                null);
          }

          JsonNode body;

          try {
            body = JSON.readTree(response.getBody());
          } catch (JsonProcessingException exception) {
            throw new RestClientException("Supplier '" + config.id() + "' returned invalid JSON");
          }

          if (body == null || !"ok".equals(body.path("status").textValue())) {
            throw new RestClientException(
                "Supplier '" + config.id() + "' returned unexpected JSON");
          }
          return true;
        }));
  }
}
