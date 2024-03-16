package com.juzipi.domain.req;

import lombok.Data;

@Data
public class TeamKickOutRequest {
    private Long teamId;
    private Long userId;
}
