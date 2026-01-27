package com.project200.undabang.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private UUID chatTicket;

    public static TicketResponse of(UUID ticket) {
        return new TicketResponse(ticket);
    }
}
