package com.wildcard.posts.dto;

import com.wildcard.posts.Category;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    private Category category;

    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String body;

    @Size(max = 255)
    private String imageUrl;
}
