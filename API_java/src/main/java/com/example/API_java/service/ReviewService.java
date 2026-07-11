package com.example.API_java.service;

import com.example.API_java.entity.Review;
import com.example.API_java.entity.User;
import com.example.API_java.entity.Product;
import com.example.API_java.repository.ReviewRepository;
import com.example.API_java.repository.UserRepository;
import com.example.API_java.repository.ProductRepository;
import com.example.API_java.repository.StaffRepository;
import com.example.API_java.entity.Staff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StaffRepository staffRepository;

    public List<Review> getBarberReviews(Integer barberId) {
        return reviewRepository.findByBarberIdOrderByCreatedAtDesc(barberId);
    }

    public List<Review> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public Map<String, Object> getBarberRatingStats(Integer barberId) {
        Double avg = reviewRepository.getAverageRatingForBarber(barberId);
        Long count = reviewRepository.getReviewCountForBarber(barberId);
        return Map.of("averageRating", avg != null ? avg : 0.0, "reviewCount", count != null ? count : 0);
    }

    public Map<String, Object> getProductRatingStats(Long productId) {
        Double avg = reviewRepository.getAverageRatingForProduct(productId);
        Long count = reviewRepository.getReviewCountForProduct(productId);
        return Map.of("averageRating", avg != null ? avg : 0.0, "reviewCount", count != null ? count : 0);
    }

    @Transactional
    public Review createBarberReview(Integer userId, Integer barberId, Integer bookingId, Double rating, String comment) {
        if (reviewRepository.existsByUserIdAndBookingId(userId, bookingId)) {
            throw new RuntimeException("Bạn đã đánh giá cho lịch đặt này rồi!");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Staff barber = staffRepository.findById(barberId).orElseThrow(() -> new RuntimeException("Staff not found"));

        Review review = new Review();
        review.setUser(user);
        review.setBarber(barber);
        review.setBookingId(bookingId);
        review.setRating(rating);
        review.setComment(comment);
        review = reviewRepository.save(review);
        
        Double newAvg = reviewRepository.getAverageRatingForBarber(barberId);
        barber.setRating(newAvg != null ? newAvg : 0.0);
        staffRepository.save(barber);
        
        return review;
    }

    @Transactional
    public Review createProductReview(Integer userId, Long productId, Long orderId, Double rating, String comment) {
        if (reviewRepository.existsByUserIdAndOrderId(userId, orderId)) {
            throw new RuntimeException("Bạn đã đánh giá cho đơn hàng này rồi!");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setComment(comment);
        review = reviewRepository.save(review);
        
        Double newAvg = reviewRepository.getAverageRatingForProduct(productId);
        product.setRating(newAvg != null ? newAvg : 0.0);
        productRepository.save(product);
        
        return review;
    }

    @Transactional
    public Review replyToReview(Integer reviewId, Integer adminId, String reply) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new RuntimeException("Review not found"));
        User admin = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        
        review.setReply(reply);
        review.setRepliedByRole(admin.getRole());
        review.setRepliedByName(admin.getFullName());
        review.setRepliedAt(java.time.LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new RuntimeException("Review not found"));
        Integer barberId = review.getBarber() != null ? review.getBarber().getId() : null;
        Long productId = review.getProduct() != null ? review.getProduct().getId() : null;
        
        reviewRepository.deleteById(reviewId);
        
        if (barberId != null) {
            Staff barber = staffRepository.findById(barberId).orElse(null);
            if (barber != null) {
                Double newAvg = reviewRepository.getAverageRatingForBarber(barberId);
                barber.setRating(newAvg != null ? newAvg : 0.0);
                staffRepository.save(barber);
            }
        }
        if (productId != null) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                Double newAvg = reviewRepository.getAverageRatingForProduct(productId);
                product.setRating(newAvg != null ? newAvg : 0.0);
                productRepository.save(product);
            }
        }
    }
}
