package com.juzipi.domain.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TeamCoverUpdateRequest {
    private Long id;
    private MultipartFile file;
}
