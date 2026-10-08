package com.wildcard.dev;

import com.wildcard.common.ApiResponse;
import com.wildcard.common.DevMailbox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev/mailbox")
@Profile({"dev", "docker"})
@RequiredArgsConstructor
public class DevMailboxController {

    private final DevMailbox mailbox;

    @GetMapping
    public ApiResponse<List<Map<String, String>>> list() {
        return ApiResponse.success(mailbox.all());
    }

    @DeleteMapping
    public ApiResponse<Void> clear() {
        mailbox.clear();
        return ApiResponse.success("Dev mailbox cleared", null);
    }
}
