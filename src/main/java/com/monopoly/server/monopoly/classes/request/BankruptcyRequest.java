
package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

@Data
public class BankruptcyRequest {
    private Long bankruptPlayerId;
    private Long creditorPlayerId; // null se il debito è verso la Banca
}