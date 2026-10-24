package com.kisansetu.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State machine matrices for all four order/delivery pipelines.
 */
class OrderStateTest {

    @Test
    void merchantOrderStatus_allowedTransitions() {
        assertArrayEquals(
                new OrderState.MerchantOrderStatus[]{OrderState.MerchantOrderStatus.ACCEPTED, OrderState.MerchantOrderStatus.REJECTED},
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.PENDING));
        assertArrayEquals(
                new OrderState.MerchantOrderStatus[]{OrderState.MerchantOrderStatus.PROCESSING, OrderState.MerchantOrderStatus.COMPLETED},
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.ACCEPTED));
        assertArrayEquals(
                new OrderState.MerchantOrderStatus[]{OrderState.MerchantOrderStatus.COMPLETED},
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.PROCESSING));
        for (OrderState.MerchantOrderStatus terminal : new OrderState.MerchantOrderStatus[]{
                OrderState.MerchantOrderStatus.COMPLETED,
                OrderState.MerchantOrderStatus.REJECTED,
                OrderState.MerchantOrderStatus.CANCELLED}) {
            assertEquals(0, OrderState.MerchantOrderStatus.allowedNext(terminal).length);
        }
    }

    @Test
    void merchantOrderStatus_validAndInvalidTransitions() {
        assertTrue(OrderState.isAllowed(OrderState.MerchantOrderStatus.PENDING,
                OrderState.MerchantOrderStatus.ACCEPTED,
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.PENDING)));
        assertFalse(OrderState.isAllowed(OrderState.MerchantOrderStatus.PENDING,
                OrderState.MerchantOrderStatus.COMPLETED,
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.PENDING)));
        assertFalse(OrderState.isAllowed(OrderState.MerchantOrderStatus.COMPLETED,
                OrderState.MerchantOrderStatus.PROCESSING,
                OrderState.MerchantOrderStatus.allowedNext(OrderState.MerchantOrderStatus.COMPLETED)));
    }

    @Test
    void customerOrderStatus_allowedTransitions() {
        assertArrayEquals(
                new OrderState.CustomerOrderStatus[]{OrderState.CustomerOrderStatus.CONFIRMED, OrderState.CustomerOrderStatus.CANCELLED},
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.PENDING));
        assertArrayEquals(
                new OrderState.CustomerOrderStatus[]{OrderState.CustomerOrderStatus.PACKED, OrderState.CustomerOrderStatus.CANCELLED},
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.CONFIRMED));
        assertArrayEquals(
                new OrderState.CustomerOrderStatus[]{OrderState.CustomerOrderStatus.DISPATCHED},
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.PACKED));
        assertArrayEquals(
                new OrderState.CustomerOrderStatus[]{OrderState.CustomerOrderStatus.DELIVERED},
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.DISPATCHED));
        assertEquals(0, OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.DELIVERED).length);
        assertEquals(0, OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.CANCELLED).length);
    }

    @Test
    void customerOrderStatus_cancelFromPendingAndConfirmedOnly() {
        assertTrue(OrderState.isAllowed(OrderState.CustomerOrderStatus.PENDING,
                OrderState.CustomerOrderStatus.CANCELLED,
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.PENDING)));
        assertTrue(OrderState.isAllowed(OrderState.CustomerOrderStatus.CONFIRMED,
                OrderState.CustomerOrderStatus.CANCELLED,
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.CONFIRMED)));
        assertFalse(OrderState.isAllowed(OrderState.CustomerOrderStatus.PACKED,
                OrderState.CustomerOrderStatus.CANCELLED,
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.PACKED)));
        assertFalse(OrderState.isAllowed(OrderState.CustomerOrderStatus.DISPATCHED,
                OrderState.CustomerOrderStatus.CANCELLED,
                OrderState.CustomerOrderStatus.allowedNext(OrderState.CustomerOrderStatus.DISPATCHED)));
    }

    @Test
    void marketplaceOrderStatus_allowedTransitions() {
        assertArrayEquals(
                new OrderState.MarketplaceOrderStatus[]{OrderState.MarketplaceOrderStatus.CONFIRMED, OrderState.MarketplaceOrderStatus.CANCELLED},
                OrderState.MarketplaceOrderStatus.allowedNext(OrderState.MarketplaceOrderStatus.PENDING));
        assertArrayEquals(
                new OrderState.MarketplaceOrderStatus[]{OrderState.MarketplaceOrderStatus.SHIPPED, OrderState.MarketplaceOrderStatus.CANCELLED},
                OrderState.MarketplaceOrderStatus.allowedNext(OrderState.MarketplaceOrderStatus.CONFIRMED));
        assertArrayEquals(
                new OrderState.MarketplaceOrderStatus[]{OrderState.MarketplaceOrderStatus.DELIVERED},
                OrderState.MarketplaceOrderStatus.allowedNext(OrderState.MarketplaceOrderStatus.SHIPPED));
        assertEquals(0, OrderState.MarketplaceOrderStatus.allowedNext(OrderState.MarketplaceOrderStatus.DELIVERED).length);
        assertEquals(0, OrderState.MarketplaceOrderStatus.allowedNext(OrderState.MarketplaceOrderStatus.CANCELLED).length);
    }

    @Test
    void deliveryStatus_fullPipelineAndTerminalStates() {
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.ACCEPTED, OrderState.DeliveryStatus.REJECTED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.ASSIGNED));
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.PICKUP_SCHEDULED, OrderState.DeliveryStatus.PICKED_UP, OrderState.DeliveryStatus.REJECTED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.ACCEPTED));
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.PICKED_UP, OrderState.DeliveryStatus.REJECTED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.PICKUP_SCHEDULED));
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.IN_TRANSIT, OrderState.DeliveryStatus.REJECTED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.PICKED_UP));
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.DELIVERED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.IN_TRANSIT));
        assertArrayEquals(
                new OrderState.DeliveryStatus[]{OrderState.DeliveryStatus.COMPLETED},
                OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.DELIVERED));
        assertEquals(0, OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.PENDING_ASSIGNMENT).length);
        assertEquals(0, OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.COMPLETED).length);
        assertEquals(0, OrderState.DeliveryStatus.allowedNext(OrderState.DeliveryStatus.REJECTED).length);
    }

    @Test
    void deliveryStatus_isTerminal() {
        assertTrue(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.DELIVERED));
        assertTrue(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.COMPLETED));
        assertTrue(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.REJECTED));
        assertFalse(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.ASSIGNED));
        assertFalse(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.IN_TRANSIT));
        assertFalse(OrderState.DeliveryStatus.isTerminal(OrderState.DeliveryStatus.PENDING_ASSIGNMENT));
    }
}