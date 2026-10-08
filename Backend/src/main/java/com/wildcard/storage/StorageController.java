package com.wildcard.storage;

import com.wildcard.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/v1/files", "/api/uploads"})
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /** kind is only used so the client can tell what the picture is for. */
    @PostMapping
    public ApiResponse<StoredFile> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "post") String kind) {
        return ApiResponse.success("Uploaded", storageService.storeImage(file));
    }
}
