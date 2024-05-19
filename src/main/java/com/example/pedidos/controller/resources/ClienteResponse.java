package com.example.pedidos.controller.resources;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClienteResponse {

    private String nome;
    private String endereco;
    private String cep;
    private String cpf;
    private String email;

}
