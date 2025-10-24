package com.project200.undabang.member.repository;

import com.project200.undabang.member.dto.record.MemberBlockRecord;
import com.project200.undabang.member.entity.Member;

import java.util.List;

public interface MemberBlockRepositoryCustom {
    List<MemberBlockRecord> findAllMemberBlockRecordsByMember(Member currentMember);
}
