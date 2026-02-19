package com.ecom.inventory.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.ecom.common.DomainEvent;
import com.ecom.inventory.entity.InventoryReservation;
import com.ecom.inventory.entity.InventoryStock;
import com.ecom.inventory.repository.ConsumedEventRepository;
import com.ecom.inventory.repository.InventoryReservationRepository;
import com.ecom.inventory.repository.InventoryStockRepository;
import com.ecom.inventory.repository.OutboxEventRepository;
import com.ecom.inventory.service.InventoryLockService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "app.kafka.topics.order-created=order.created.v1"
})
class InventorySagaConsumerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("inventory_it")
            .withUsername("it")
            .withPassword("it")
            .withStartupTimeout(Duration.ofMinutes(5));

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryStockRepository stockRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ConsumedEventRepository consumedEventRepository;

    @MockBean
    private InventoryLockService inventoryLockService;

    @BeforeEach
    void setUp() {
        when(inventoryLockService.acquire(anyString())).thenReturn(true);
        outboxEventRepository.deleteAll();
        reservationRepository.deleteAll();
        stockRepository.deleteAll();
        consumedEventRepository.deleteAll();
    }

    @Test
    void concurrentOrderCreatedEventsShouldNotOversellStock() {
        InventoryStock stock = new InventoryStock();
        stock.setSku("FLASH-SKU-1");
        stock.setAvailableQuantity(5);
        stock.setReservedQuantity(0);
        stockRepository.save(stock);

        for (int i = 1; i <= 10; i++) {
            String orderId = "order-" + i;
            String rawEvent = writeEvent(
                    UUID.randomUUID(),
                    orderId,
                    List.of(Map.of("sku", "FLASH-SKU-1", "quantity", 1)));
            kafkaTemplate.send("order.created.v1", orderId, rawEvent);
        }
        kafkaTemplate.flush();

        waitUntil(
                () -> consumedEventRepository.count() == 10 && outboxEventRepository.count() == 10,
                Duration.ofSeconds(25));

        InventoryStock updated = stockRepository.findBySku("FLASH-SKU-1").orElseThrow();
        List<InventoryReservation> reservations = reservationRepository.findAll();

        long reservedRows = reservations.stream()
                .filter(r -> "RESERVED".equals(r.getStatus()))
                .count();
        long reservedEvents = outboxEventRepository.findAll().stream()
                .filter(r -> "inventory.reserved.v1".equals(r.getEventType()))
                .count();
        long failedEvents = outboxEventRepository.findAll().stream()
                .filter(r -> "inventory.reservation.failed.v1".equals(r.getEventType()))
                .count();

        assertEquals(0, updated.getAvailableQuantity());
        assertEquals(5, updated.getReservedQuantity());
        assertEquals(5, reservations.size());
        assertEquals(5, reservedRows);
        assertEquals(5, reservedEvents);
        assertEquals(5, failedEvents);
    }

    @Test
    void duplicateEventIdShouldBeDeduplicated() {
        InventoryStock stock = new InventoryStock();
        stock.setSku("DEDUP-SKU-1");
        stock.setAvailableQuantity(10);
        stock.setReservedQuantity(0);
        stockRepository.save(stock);

        UUID sharedEventId = UUID.randomUUID();
        String rawEvent = writeEvent(
                sharedEventId,
                "order-dedup-1",
                List.of(Map.of("sku", "DEDUP-SKU-1", "quantity", 2)));

        kafkaTemplate.send("order.created.v1", "order-dedup-1", rawEvent);
        kafkaTemplate.send("order.created.v1", "order-dedup-1", rawEvent);
        kafkaTemplate.flush();

        waitUntil(() -> consumedEventRepository.count() >= 1, Duration.ofSeconds(20));

        InventoryStock updated = stockRepository.findBySku("DEDUP-SKU-1").orElseThrow();
        List<InventoryReservation> reservations = reservationRepository.findAll();
        long reservedEvents = outboxEventRepository.findAll().stream()
                .filter(r -> "inventory.reserved.v1".equals(r.getEventType()))
                .count();

        assertEquals(8, updated.getAvailableQuantity());
        assertEquals(2, updated.getReservedQuantity());
        assertEquals(1, reservations.size());
        assertEquals(1, consumedEventRepository.count());
        assertEquals(1, reservedEvents);
    }

    private String writeEvent(UUID eventId, String orderId, List<Map<String, Object>> items) {
        try {
            DomainEvent<Map<String, Object>> event = new DomainEvent<>(
                    eventId,
                    "order.created.v1",
                    Instant.now(),
                    "order-service",
                    "v1",
                    UUID.randomUUID().toString(),
                    Map.of("orderId", orderId, "items", items));
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize test event", ex);
        }
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for condition", ex);
            }
        }
        assertTrue(condition.getAsBoolean(), "Condition was not met before timeout");
    }
}
