package com.project200.undabang.member.exceptions;

public class DuplicateMemberNicknameException extends RuntimeException{
    public DuplicateMemberNicknameException(String message){
        super(message);
    }
}
