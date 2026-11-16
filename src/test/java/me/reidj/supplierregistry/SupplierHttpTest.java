package me.reidj.supplierregistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupplierHttpTest {

  private static final String URL = "https://supplier.example/v1/subscribers/79991234567";

  private MockRestServiceServer server;
  private SupplierRegistry registry;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    registry = new SupplierRegistry(
        List.of(new SupplierConfig("mts", "https://supplier.example/v1", "key-123")), builder);
  }

  @AfterEach
  void tearDown() {
    server.verify();
  }

  private ResponseActions expectRequest(String url) {
    return server.expect(request -> {
      assertEquals(HttpMethod.GET, request.getMethod());
      assertEquals(url, request.getURI().toString());
      assertEquals("key-123", request.getHeaders().getFirst("X-API-Key"));
    });
  }

  @Test
  void available() {
    expectRequest(URL).andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

    assertTrue(registry.isAvailable("mts", "79991234567"));
  }

  @Test
  void notAvailable() {
    expectRequest(URL).andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertFalse(registry.isAvailable("mts", "79991234567"));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 500, 503})
  void httpError(int status) {
    expectRequest(URL).andRespond(withStatus(HttpStatus.valueOf(status)));

    var e = assertThrows(RestClientResponseException.class,
        () -> registry.isAvailable("mts", "79991234567"));
    assertEquals(status, e.getStatusCode().value());
  }

  @Test
  void networkError() {
    expectRequest(URL).andRespond(request -> {
      throw new ConnectException("Connection refused");
    });

    assertThrows(ResourceAccessException.class, () -> registry.isAvailable("mts", "79991234567"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "{\"status\":\"error\"}", "not json"})
  void unexpectedBody(String body) {
    expectRequest(URL).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

    assertThrows(RestClientException.class, () -> registry.isAvailable("mts", "79991234567"));
  }

  @Test
  void phoneIsEncoded() {
    expectRequest("https://supplier.example/v1/subscribers/%2B7%20999%2F123")
        .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

    assertTrue(registry.isAvailable("mts", "+7 999/123"));
  }

  @Test
  void unknownSupplier() {
    assertThrows(IllegalArgumentException.class, () -> registry.isAvailable("beeline", "79991234567"));
  }

  @Test
  void emptyPhone() {
    assertThrows(IllegalArgumentException.class, () -> registry.isAvailable("mts", " "));
  }
}
