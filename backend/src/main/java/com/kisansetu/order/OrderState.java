package com.kisansetu.order;

/**
 * Central order state machine definitions.
 * Every transition is validated against the allowed transitions;
 * arbitrary status changes are rejected by the backend.
 */
public final class OrderState {

    private OrderState() {
    }

    // ---------------- Farmer -> Merchant orders ----------------
    public enum MerchantOrderStatus {
        PENDING, ACCEPTED, PROCESSING, COMPLETED, REJECTED, CANCELLED;

        public static MerchantOrderStatus[] allowedNext(MerchantOrderStatus current) {
            return switch (current) {
                case PENDING -> new MerchantOrderStatus[]{ACCEPTED, REJECTED};
                case ACCEPTED -> new MerchantOrderStatus[]{PROCESSING, COMPLETED};
                case PROCESSING -> new MerchantOrderStatus[]{COMPLETED};
                case COMPLETED, REJECTED, CANCELLED -> new MerchantOrderStatus[]{};
            };
        }
    }

    // ---------------- Customer -> Farmer orders ----------------
    public enum CustomerOrderStatus {
        PENDING, CONFIRMED, PACKED, DISPATCHED, DELIVERED, CANCELLED;

        public static CustomerOrderStatus[] allowedNext(CustomerOrderStatus current) {
            return switch (current) {
                case PENDING -> new CustomerOrderStatus[]{CONFIRMED, CANCELLED};
                case CONFIRMED -> new CustomerOrderStatus[]{PACKED, CANCELLED};
                case PACKED -> new CustomerOrderStatus[]{DISPATCHED};
                case DISPATCHED -> new CustomerOrderStatus[]{DELIVERED};
                case DELIVERED, CANCELLED -> new CustomerOrderStatus[]{};
            };
        }
    }

    // ---------------- Farmer -> Farmer marketplace orders ----------------
    public enum MarketplaceOrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

        public static MarketplaceOrderStatus[] allowedNext(MarketplaceOrderStatus current) {
            return switch (current) {
                case PENDING -> new MarketplaceOrderStatus[]{CONFIRMED, CANCELLED};
                case CONFIRMED -> new MarketplaceOrderStatus[]{SHIPPED, CANCELLED};
                case SHIPPED -> new MarketplaceOrderStatus[]{DELIVERED};
                case DELIVERED, CANCELLED -> new MarketplaceOrderStatus[]{};
            };
        }
    }

    // ---------------- Delivery pipeline (logistics) ----------------
    public enum DeliveryStatus {
        PENDING_ASSIGNMENT, ASSIGNED, ACCEPTED, PICKUP_SCHEDULED, PICKED_UP, IN_TRANSIT, DELIVERED, COMPLETED, REJECTED;

        public static DeliveryStatus[] allowedNext(DeliveryStatus current) {
            return switch (current) {
                case ASSIGNED -> new DeliveryStatus[]{ACCEPTED, REJECTED};
                case ACCEPTED -> new DeliveryStatus[]{PICKUP_SCHEDULED, PICKED_UP, REJECTED};
                case PICKUP_SCHEDULED -> new DeliveryStatus[]{PICKED_UP, REJECTED};
                case PICKED_UP -> new DeliveryStatus[]{IN_TRANSIT, REJECTED};
                case IN_TRANSIT -> new DeliveryStatus[]{DELIVERED};
                case DELIVERED -> new DeliveryStatus[]{COMPLETED};
                case PENDING_ASSIGNMENT, COMPLETED, REJECTED -> new DeliveryStatus[]{};
            };
        }

        public static boolean isTerminal(DeliveryStatus status) {
            return status == DELIVERED || status == COMPLETED || status == REJECTED;
        }
    }

    public static <T extends Enum<T>> boolean isAllowed(T current, T next, T[] allowed) {
        for (T candidate : allowed) {
            if (candidate == next) {
                return true;
            }
        }
        return false;
    }
}