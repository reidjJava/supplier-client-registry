package me.reidj.supplierregistry;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class JsonSupplierConfigLoader {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final TypeReference<List<SupplierConfig>> CONFIG_LIST = new TypeReference<>() {};

  private JsonSupplierConfigLoader() {
  }

  public static List<SupplierConfig> load(Path path) throws IOException {
    if (path == null) {
      throw new IllegalArgumentException("path must not be null");
    }

    try (InputStream input = Files.newInputStream(path)) {
      return SupplierConfig.validatedCopy(JSON.readValue(input, CONFIG_LIST));
    } catch (JsonMappingException exception) {
      String reason =
          exception.getCause() instanceof IllegalArgumentException cause ? ": " + cause.getMessage()
              : "";
      throw invalid("at " + jsonPath(exception) + reason);
    } catch (JsonProcessingException exception) {
      JsonLocation location = exception.getLocation();
      throw invalid(location == null ? "syntax error"
          : "syntax error at line " + location.getLineNr() + ", column " + location.getColumnNr());
    } catch (IllegalArgumentException exception) {
      throw invalid(exception.getMessage());
    }
  }

  private static String jsonPath(JsonMappingException exception) {
    var path = new StringBuilder("$");
    for (JsonMappingException.Reference reference : exception.getPath()) {
      path.append(reference.getFieldName() != null ? "." + reference.getFieldName()
          : "[" + reference.getIndex() + "]");
    }
    return path.toString();
  }

  private static IOException invalid(String details) {
    return new IOException("Invalid supplier JSON: " + details);
  }
}
