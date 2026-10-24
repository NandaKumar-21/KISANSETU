package com.kisansetu.order.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.merchant.entity.Product;
import com.kisansetu.merchant.repository.ProductRepository;
import com.kisansetu.merchant.service.MerchantProductService;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState.MerchantOrderStatus;
import com.kisansetu.order.dto.OrderRequest;
import com.kisansetu.order.dto.OrderResponse;
import com.kisansetu.order.entity.Order;
import com.kisansetu.order.repository.OrderRepository;
import com.kisansetu.order.repository.OrderStatusHistoryRepository;
import com.kisansetu.user.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID FARMER = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID MERCHANT = UUID.fromString("a0000000-0000-4000-8000-000000000011");
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStatusHistoryRepository historyRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MerchantProductService merchantProductService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProfileService profileService;

    private OrderService service;

    private Product product(int stock) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setMerchantId(MERCHANT);
        product.setName("Urea");
        product.setPrice(new BigDecimal("500"));
        product.setQuantity(stock);
        product.setUnit("bag");
        return product;
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setFarmerId(FARMER);
        order.setMerchantId(MERCHANT);
        order.setProductId(PRODUCT_ID);
        order.setQuantity(2);
        order.setUnitPrice(new BigDecimal("500"));
        order.setTotalPrice(new BigDecimal("1000"));
        order.setStatus(MerchantOrderStatus.PENDING);
        return order;
    }

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, historyRepository, productRepository,
                merchantProductService, notificationService, profileService);
    }

    @Test
    void createOrder_computesTotalAndNotifiesMerchant() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(10)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.createOrder(FARMER,
                new OrderRequest(PRODUCT_ID, 3, "Deliver in evening"));

        assertEquals("pending", response.status());
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.totalPrice()));
        assertEquals(MERCHANT, response.merchantId());
        verify(notificationService).notify(eq(MERCHANT), eq("order_received"), any(), any());
        verify(historyRepository).save(any());
    }

    @Test
    void createOrder_insufficientStockRejected() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(2)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createOrder(FARMER, new OrderRequest(PRODUCT_ID, 5, null)));
        assertEquals(409, ex.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_productNotFound() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createOrder(FARMER, new OrderRequest(PRODUCT_ID, 1, null)));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void acceptOrder_transitionsAndDeductsStock() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.acceptOrder(MERCHANT, ORDER_ID);

        assertEquals("accepted", response.status());
        verify(merchantProductService).deductStock(PRODUCT_ID, 2, ORDER_ID, MERCHANT);
        verify(notificationService).notify(eq(FARMER), eq("order_accepted"), any(), any());
    }

    @Test
    void acceptOrder_rejectsInvalidTransition() {
        Order order = pendingOrder();
        order.setStatus(MerchantOrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () -> service.acceptOrder(MERCHANT, ORDER_ID));
        assertEquals(409, ex.getStatus());
        verify(merchantProductService, never()).deductStock(any(), anyInt(), any(), any());
    }

    @Test
    void advanceStatus_movesAcceptedToProcessingToCompleted() {
        Order order = pendingOrder();
        order.setStatus(MerchantOrderStatus.ACCEPTED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("processing", service.advanceStatus(MERCHANT, ORDER_ID, "PROCESSING").status());

        order.setStatus(MerchantOrderStatus.PROCESSING);
        assertEquals("completed", service.advanceStatus(MERCHANT, ORDER_ID, "COMPLETED").status());
        verify(historyRepository, times(2)).save(any());
    }

    @Test
    void advanceStatus_invalidStatusValueBadRequest() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.advanceStatus(MERCHANT, ORDER_ID, "NONSENSE"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void rejectOrder_onlyFromPending() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = service.rejectOrder(MERCHANT, ORDER_ID, "Out of stock");

        assertEquals("rejected", response.status());
        verify(notificationService).notify(eq(FARMER), eq("order_rejected"), any(), contains("Out of stock"));
    }

    @Test
    void merchantOrderOwnershipEnforced() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.acceptOrder(UUID.randomUUID(), ORDER_ID));
        assertEquals(403, ex.getStatus());
    }

    @Test
    void getMerchantOrderCounts_returnsAllStatusKeys() {
        when(orderRepository.countByMerchantIdAndStatus(eq(MERCHANT), any())).thenReturn(1L);

        var counts = service.getMerchantOrderCounts(MERCHANT);

        assertEquals(List.of("pending", "accepted", "processing", "completed"),
                counts.keySet().stream().toList());
        counts.values().forEach(v -> assertEquals(1L, v));
    }

    @Test
    void getFarmerOrders_listsOwnOrders() {
        when(orderRepository.findByFarmerIdOrderByCreatedAtDesc(FARMER))
                .thenReturn(List.of(pendingOrder()));
        when(profileService.getProfileEntityByUserId(any())).thenThrow(new RuntimeException("no profile"));

        List<OrderResponse> orders = service.getFarmerOrders(FARMER);

        assertEquals(1, orders.size());
        assertEquals("pending", orders.get(0).status());
    }
}