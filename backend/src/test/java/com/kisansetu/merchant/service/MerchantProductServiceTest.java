package com.kisansetu.merchant.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.merchant.dto.ProductRequest;
import com.kisansetu.merchant.dto.ProductResponse;
import com.kisansetu.merchant.entity.InventoryTransaction;
import com.kisansetu.merchant.entity.Product;
import com.kisansetu.merchant.repository.InventoryTransactionRepository;
import com.kisansetu.merchant.repository.ProductRepository;
import com.kisansetu.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProductServiceTest {

    private static final UUID MERCHANT = UUID.fromString("a0000000-0000-4000-8000-000000000011");
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private NotificationService notificationService;

    private MerchantProductService service;

    private ProductRequest request(String name, int quantity) {
        return new ProductRequest(name, "desc", "Seeds", new BigDecimal("50"), quantity, "kg",
                null, 10);
    }

    private Product ownedProduct(int quantity, int threshold) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setMerchantId(MERCHANT);
        product.setName("Fertilizer");
        product.setCategory("Fertilizers");
        product.setPrice(new BigDecimal("500"));
        product.setQuantity(quantity);
        product.setUnit("kg");
        product.setStockThreshold(threshold);
        return product;
    }

    @BeforeEach
    void setUp() {
        service = new MerchantProductService(productRepository, inventoryTransactionRepository, notificationService);
    }

    @Test
    void createProduct_savesAndRecordsInitialStock() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = service.createProduct(MERCHANT, request("Seed Pack", 25));

        assertEquals("Seed Pack", response.name());
        assertEquals(25, response.quantity());
        assertFalse(response.lowStock());
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void createProduct_skipsTransactionWhenZeroStock() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createProduct(MERCHANT, request("Seed Pack", 0));

        verify(inventoryTransactionRepository, never()).save(any());
    }

    @Test
    void updateProduct_recordsStockDelta() {
        Product product = ownedProduct(10, 5);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = service.updateProduct(MERCHANT, PRODUCT_ID, request("Fertilizer", 20));

        assertEquals(20, response.quantity());
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void updateProduct_forbiddenForOtherMerchant() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(ownedProduct(10, 5)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProduct(UUID.randomUUID(), PRODUCT_ID, request("X", 1)));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void deleteProduct_requiresOwnership() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(ownedProduct(10, 5)));

        assertThrows(ApiException.class, () -> service.deleteProduct(UUID.randomUUID(), PRODUCT_ID));
        verify(productRepository, never()).delete(any());

        service.deleteProduct(MERCHANT, PRODUCT_ID);
        verify(productRepository).delete(any());
    }

    @Test
    void deductStock_sufficientStockDecrements() {
        Product product = ownedProduct(10, 5);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service.deductStock(PRODUCT_ID, 4, UUID.randomUUID(), MERCHANT);

        assertEquals(6, product.getQuantity());
        verify(productRepository).save(product);
        verify(inventoryTransactionRepository).save(any());
        verify(notificationService, never()).notify(any(), any(), any(), any());
    }

    @Test
    void deductStock_insufficientStockConflict() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(ownedProduct(3, 5)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deductStock(PRODUCT_ID, 4, UUID.randomUUID(), MERCHANT));
        assertEquals(409, ex.getStatus());
        assertEquals(3, productRepository.findById(PRODUCT_ID).orElseThrow().getQuantity());
    }

    @Test
    void deductStock_notFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.deductStock(PRODUCT_ID, 1, UUID.randomUUID(), MERCHANT));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void deductStock_lowStockAlertFires() {
        Product product = ownedProduct(10, 10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        service.deductStock(PRODUCT_ID, 1, UUID.randomUUID(), MERCHANT);

        assertEquals(9, product.getQuantity());
        verify(notificationService).notify(eq(MERCHANT), eq("low_stock"), any(), any());
    }
}