package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.marketplace.dto.request.ReviewRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ReviewResponse;
import tn.esprit.spring.baladna.marketplace.entity.Review;
import tn.esprit.spring.baladna.marketplace.exception.BusinessException;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.mapper.ReviewMapper;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;
import tn.esprit.spring.baladna.marketplace.repository.ReviewRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;

    public ReviewResponse createReview(ReviewRequest request) {
        // Vérifier que le produit existe
        if (!productRepository.existsById(request.getIdProduit())) {
            throw new ResourceNotFoundException("Produit non trouvé avec l'id: " + request.getIdProduit());
        }

        // Un utilisateur ne peut laisser qu'un seul avis par produit
        if (reviewRepository.existsByIdUserAndIdProduit(request.getIdUser(), request.getIdProduit())) {
            throw new BusinessException("Vous avez déjà laissé un avis pour ce produit");
        }

        Review review = reviewMapper.toEntity(request);
        Review saved = reviewRepository.save(review);
        return enrichAuthor(reviewMapper.toResponse(saved));
    }

    public List<ReviewResponse> getReviewsByProduct(Integer idProduit) {
        if (!productRepository.existsById(idProduit)) {
            throw new ResourceNotFoundException("Produit non trouvé avec l'id: " + idProduit);
        }
        return reviewRepository.findByIdProduit(idProduit)
                .stream()
                .map(reviewMapper::toResponse)
                .map(this::enrichAuthor)
                .collect(Collectors.toList());
    }

    private ReviewResponse enrichAuthor(ReviewResponse response) {
        if (response.getIdUser() == null) {
            return response;
        }
        userRepository.findById(response.getIdUser().longValue()).ifPresent(user -> response.setAuteurNom(buildDisplayName(user)));
        return response;
    }

    private static String buildDisplayName(User user) {
        String name = Stream.of(user.getFirstName(), user.getLastName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        if (!name.isEmpty()) {
            return name;
        }
        return user.getEmail() != null ? user.getEmail() : ("User #" + user.getId());
    }
}
