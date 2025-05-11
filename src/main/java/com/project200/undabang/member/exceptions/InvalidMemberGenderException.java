package com.project200.undabang.member.exceptions;

public class InvalidMemberGenderException extends IllegalArgumentException{
    public InvalidMemberGenderException(String message){
        super(message);
    }
}
