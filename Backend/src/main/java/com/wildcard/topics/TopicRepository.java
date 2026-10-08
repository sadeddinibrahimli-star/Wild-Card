package com.wildcard.topics;

import com.wildcard.posts.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findBySlug(String slug);

    List<Topic> findTop50ByLabelContainingIgnoreCaseOrderByLabelAsc(String label);

    List<Topic> findByCategoryOrderByLabelAsc(Category category);
}
