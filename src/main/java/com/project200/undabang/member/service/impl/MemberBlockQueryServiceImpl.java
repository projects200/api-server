package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberPictureRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberBlockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBlockQueryServiceImpl implements MemberBlockQueryService {
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final MemberPictureRepository memberPictureRepository;

    @Override
    public List<GetBlockedMembersResponse> getBlockedMembers() {
        return List.of();
    }
}
