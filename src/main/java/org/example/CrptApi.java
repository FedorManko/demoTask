package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;


public class CrptApi {

  private final HttpClient httpClient;
  private final Semaphore semaphore;
  private final long timeIntervalMillis;
  private volatile Instant lastRequestTime;
  private final Object lock = new Object();
  private final ObjectMapper objectMapper;

  public CrptApi(TimeUnit timeUnit, int requestLimit) {
    this.httpClient = HttpClient.newHttpClient();
    this.semaphore = new Semaphore(requestLimit);
    this.timeIntervalMillis = timeUnit.toSeconds(10);
    this.lastRequestTime = Instant.now();
    this.objectMapper = new ObjectMapper();
  }


  public static void main(String[] args) {
    CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

    String signature = "example-signature";
    CrtpApiRequest request = createCrtpApiRequest();

    api.createDocument(request, signature);
  }

  @SneakyThrows
  public void createDocument(CrtpApiRequest request, String signature) {
    synchronized (lock) {
      Instant now = Instant.now();
      if (Duration.between(lastRequestTime, now).toMillis() > timeIntervalMillis) {
        semaphore.release(semaphore.availablePermits() - 1);
        lastRequestTime = now;
      }
    }
    String stringRequest = objectMapper.writeValueAsString(request);

    semaphore.acquire();

    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
        .header("Content-Type", "application/json")
        .header("Signature", signature)
        .POST(HttpRequest.BodyPublishers.ofString(stringRequest))
        .build();

    HttpResponse<String> response = httpClient.send(httpRequest,
        HttpResponse.BodyHandlers.ofString());
    System.out.println("Response status code: " + response.statusCode());
    System.out.println("Response body: " + response.body());

  }

  private static CrtpApiRequest createCrtpApiRequest() {
    Product product = new Product("certificateDocument",
        "certificateDocumentDate",
        "certificateDocumentNumber",
        "ownerInn", "producerInn", "now", "tnvedCode",
        "uitCode", "uiteCode");

    CrtpApiRequest request = new CrtpApiRequest(new Description("description"),
        "docId", "status", DocumentType.SOME_DOCUMENT_TYPE, true,
        "ownerInn", "participantInn", "producerInn", "date",
        ProductionType.SOME_TYPE, List.of(product), "date", "regNumber");
    return request;

  }


  private record CrtpApiRequest(
      Description description,

      @JsonProperty("doc_id")
      String docId,

      @JsonProperty("doc_status")
      String docStatus,

      @JsonProperty("doc_type")
      DocumentType docType,

      Boolean importRequest,

      @JsonProperty("owner_inn")
      String ownerInn,

      @JsonProperty("participant_inn")
      String participantInn,

      @JsonProperty("producer_inn")
      String producerInn,

      @JsonProperty("production_date")
      String productionDate,

      @JsonProperty("production_type")
      ProductionType productionType,

      List<Product> products,

      @JsonProperty("reg_date")
      String regDate,

      @JsonProperty("reg_number")
      String regNumber
  ) {

  }

  private record Description(String participantInn) {

  }

  @Data
  @AllArgsConstructor
  private static class Product {

    @JsonProperty("certificate_document")
    private String certificateDocument;

    @JsonProperty("certificate_document_date")
    private String certificateDocumentDate;

    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private String productionDate;

    @JsonProperty("tnved_code")
    private String tnvedCode;

    @JsonProperty("uit_code")
    private String uitCode;

    @JsonProperty("uitu_code")
    private String uituCode;
  }

  private enum ProductionType {
    SOME_TYPE
  }

  private enum DocumentType {
    SOME_DOCUMENT_TYPE
  }

}
