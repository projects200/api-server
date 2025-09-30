package com.project200.undabang.match.service;

import java.util.UUID;

public interface MatchService {
    void createMatchRecordBetweenMembers(UUID requesterId, UUID receiverId);
}
