package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.RatingRequest;
import com.kisansetu.customer.entity.CustomerOrder;
import com.kisansetu.customer.entity.FarmerRating;
import com.kisansetu.customer.repository.CustomerOrderRepository;
import com.kisansetu.customer.repository.FarmerRatingRepository;
import com.kisansetu.farmer.repository.MarketplaceListingRepository;
import com.kisansetu.notification.service.NotificationService;
import com.kisansetu.order.OrderState.CustomerOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Farmer ratings: one rating per delivered order, matching the original.
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private final FarmerRatingRepository ratingRepository;
    private final CustomerOrderRepository orderRepository;
    private final MarketplaceListingRepository listingRepository;
    private final NotificationService notificationService;

    @Transactional
    public FarmerRating rateOrder(UUID customerId, UUID orderId, RatingRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        if (order.getStatus() != CustomerOrderStatus.DELIVERED) {
            throw ApiException.conflict("You can only rate a delivered order");
        }
        if (ratingRepository.existsByCustomerIdAndOrderId(customerId, orderId)) {
            throw ApiException.conflict("You have already rated this order");
        }
        FarmerRating rating = new FarmerRating();
        rating.setCustomerId(customerId);
        rating.setFarmerId(order.getFarmerId());
        rating.setOrderId(orderId);
        rating.setRating(request.rating());
        rating.setReview(request.review());
        ratingRepository.save(rating);

        notificationService.notify(order.getFarmerId(), "rating",
                "New rating received",
                "A customer rated your service " + request.rating() + "/5 stars.");
        return rating;
    }

    @Transactional(readOnly = true)
    public List<FarmerRating> getMyRatings(UUID farmerId) {
        return ratingRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
    }

    @Transactional(readOnly = true)
    public Double averageRating(UUID farmerId) {
        return ratingRepository.avgRating(farmerId);
    }

    @Transactional(readOnly = true)
    public boolean canRate(UUID customerId, UUID orderId) {
        CustomerOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getCustomerId().equals(customerId)
                || order.getStatus() != CustomerOrderStatus.DELIVERED) {
            return false;
        }
        return !ratingRepository.existsByCustomerIdAndOrderId(customerId, orderId);
    }
}