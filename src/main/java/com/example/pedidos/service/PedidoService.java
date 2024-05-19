package com.example.pedidos.service;

import com.example.pedidos.controller.resources.PedidoRequest;
import com.example.pedidos.entity.Pedido;
import com.example.pedidos.entity.dto.PedidoDto;
import com.example.pedidos.exception.PedidoException;
import com.example.pedidos.integration.producer.MessageProducer;
import com.example.pedidos.repository.PedidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Service
public class PedidoService {

    @Autowired
    private MessageProducer producer;

    private final PedidoRepository pedidoRepository;

    private final String SUCESSO = "PEDIDO REALIZADO COM SUCESSO, POR FAVOR, AGUARDE";
    private final String ENTREGUE = "PEDIDO ENTREGUE COM SUCESSO";
    private final String ERROR = "TIVEMOS UM PROBLEMA EM SEU PEDIDO";

    public void realizarPedido(PedidoRequest pedidoRequest) {

        PedidoDto pedido = PedidoDto.builder()
                .id(UUID.randomUUID())
                .idCliente(pedidoRequest.getIdCliente())
                .idProduto(pedidoRequest.getIdProduto()).build();


        String validaProduto = validaProduto(pedido);

        log.info(validaProduto);

    }

    public String solicitarPedido(PedidoDto pedido) {
        log.info("Pedido validado enviando para fila de entrega...");
        try {
            producer.entregaPedido(pedido);
        } catch (JsonProcessingException e) {
            return "Ocorreu um erro ao solicitar pedido .. " + e.getMessage();
        }
        return "Pedido aguardando entrega ...";
    }
    public void sucessoPedido(String payload) throws JSONException {
        log.info("Sucesso ao solicitar pedido: {} ", payload);
        getMessage(payload, SUCESSO);
    }

    public void entreguePedido(String payload) throws JSONException {
        log.info("Sucesso ao entregar seu pedido: {} ", payload);
        getMessage(payload, ENTREGUE);
    }
    public void erroPedido(String payload) throws JSONException {
        log.info("Erro ao solicitar pedido: {} ", payload);
        getMessage(payload, ERROR);
    }

    private void getMessage(String payload, String status) throws JSONException {

        JSONObject jsonObject = new JSONObject(payload);
        log.info("Json Message: {} ", jsonObject);

        String id = jsonObject.getString("id");
        String idCliente = jsonObject.getString("idCliente");
        String idProduto = jsonObject.getString("idProduto");

        Pedido pedido = new Pedido(id, idCliente, idProduto, status);

        salvaPedido(pedido);

        if(status.equalsIgnoreCase(SUCESSO)){
            String pedidoMsg = solicitarPedido(
                    PedidoDto.builder().
                            id(UUID.fromString(id))
                            .idCliente(UUID.fromString(idCliente))
                            .idProduto(Long.parseLong(idProduto)).build());

            log.info(pedidoMsg);
        }

    }

    public void salvaPedido(Pedido pedido){
        log.info("Atualizando pedido na base {}", pedido);
        try {
            pedidoRepository.save(pedido);
        }  catch (PedidoException e){
            throw new PedidoException("Erro ao atualizar pedido na base");
        }

    }
    public String validaProduto(PedidoDto pedido){
        log.info("Validando Produto {}", pedido);
        try {
            log.info("Postando na fila para validar produto");
            producer.validaProduto(pedido);
        } catch (JsonProcessingException e) {
            return "Ocorreu um erro ao validar produto .. " + e.getMessage();
        }
        return "Enviado com sucesso para servico de produtos";
    }
}
