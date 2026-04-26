package tn.esprit.spring.baladna.marketplace.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.marketplace.dto.request.ReviewRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ReviewResponse;
import tn.esprit.spring.baladna.marketplace.entity.Review;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequest request) {
        return Review.builder()
                .idUser(request.getIdUser())
                .idProduit(request.getIdProduit())
                .idCommande(request.getIdCommande())
                .rating(request.getRating())
                .commentaire(request.getCommentaire())
                .build();
    }

    public ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setIdReview(review.getIdReview());
        response.setIdUser(review.getIdUser());
        response.setIdProduit(review.getIdProduit());
        response.setIdCommande(review.getIdCommande());
        response.setRating(review.getRating());
        response.setCommentaire(review.getCommentaire());
        response.setDateCreation(review.getDateCreation());
        return response;
    }
}
