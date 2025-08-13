package com.project200.undabang.auth.service;

import com.project200.undabang.member.entity.Member;

public interface AuthService {

    Member login();

    Member logout();
}
