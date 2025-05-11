package com.project200.undabang.member.exceptions;

public class DuplicateMemberEmailException extends RuntimeException{
    public DuplicateMemberEmailException(String message){
        super(message);
    }
}
