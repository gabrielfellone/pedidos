package com.example.pedidos.controller.resources;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PedidoRequest {

    private UUID idCliente;
    private Long idProduto;

}
