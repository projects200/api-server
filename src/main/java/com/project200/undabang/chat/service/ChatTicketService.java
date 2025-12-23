package com.project200.undabang.chat.service;

import com.project200.undabang.chat.dto.response.TicketResponse;
import com.project200.undabang.chat.entity.TicketInfoRecord;

import java.util.UUID;

public interface ChatTicketService {
    TicketResponse issueTicket(Long chatroomId);

    TicketInfoRecord validateTicket(UUID ticketId);
}
