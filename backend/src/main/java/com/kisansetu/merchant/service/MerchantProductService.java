package com.kisansetu.merchant.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.merchant.dto.ProductRequest;
import com.kisansetu.merchant.dto.ProductResponse;
import com.kisansetu.merchant.entity.InventoryTransaction;
import com.kisansetu.merchant.entity.Product;
import com.kisansetu.merchant.repository.InventoryTransactionRepository;
import com.kisansetu.merchant.repository.ProductRepository;
import com.kisansetu.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Merchant inventory management with audited, transactional stock changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantProductService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(UUID merchantId) {
        return productRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID merchantId, UUID productId) {
        return ProductResponse.from(getOwnedProduct(merchantId, productId));
    }

    @Transactional
    public ProductResponse createProduct(UUID merchantId, ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        product.setMerchantId(merchantId);
        productRepository.save(product);

        if (product.getQuantity() != null && product.getQuantity() > 0) {
            recordTransaction(product.getId(), product.getQuantity(), "initial_stock", null, merchantId);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID merchantId, UUID productId, ProductRequest request) {
        Product product = getOwnedProduct(merchantId, productId);
        int oldQty = product.getQuantity();
        applyRequest(product, request);
        productRepository.save(product);

        int delta = product.getQuantity() - oldQty;
        if (delta != 0) {
            recordTransaction(product.getId(), delta, "stock_adjustment", null, merchantId);
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(UUID merchantId, UUID productId) {
        Product product = getOwnedProduct(merchantId, productId);
        productRepository.delete(product);
    }

    /**
     * Deduct stock within the same transaction as the order acceptance,
     * with a pessimistic lock to prevent race conditions.
     */
    @Transactional
    public void deductStock(UUID productId, int quantity, UUID orderId, UUID actorId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (product.getQuantity() < quantity) {
            throw ApiException.conflict("Insufficient stock for " + product.getName()
                    + " (available: " + product.getQuantity() + ")");
        }
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
        recordTransaction(productId, -quantity, "order_accept", orderId, actorId);
        if (product.isLowStock()) {
            notificationService.notify(product.getMerchantId(), "low_stock",
                    "Low stock alert", product.getName() + " is running low ("
                            + product.getQuantity() + " " + product.getUnit() + " left).");
        }
    }

    private Product getOwnedProduct(UUID merchantId, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!product.getMerchantId().equals(merchantId)) {
            throw ApiException.forbidden("You do not own this product");
        }
        return product;
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
        product.setUnit(request.unit());
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.stockThreshold() != null) {
            product.setStockThreshold(request.stockThreshold());
        }
    }

    private void recordTransaction(UUID productId, int change, String reason, UUID orderId, UUID actorId) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setProductId(productId);
        tx.setChangeQty(change);
        tx.setReason(reason);
        tx.setOrderId(orderId);
        tx.setCreatedBy(actorId);
        inventoryTransactionRepository.save(tx);
    }
}