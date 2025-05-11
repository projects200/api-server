package com.project200.undabang.member.exceptions;

public class MemberNotSavedException extends RuntimeException{
    public MemberNotSavedException(String message){
        super(message);
    }
}
