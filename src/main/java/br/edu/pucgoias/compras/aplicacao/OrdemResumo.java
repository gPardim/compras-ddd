package br.edu.pucgoias.compras.aplicacao;

import java.math.BigDecimal;
import java.util.List;

public record OrdemResumo(Long id, Long fornecedorId, String status, BigDecimal total, List<ItemResumo> itens) {

    public record ItemResumo(String codigoProduto, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
    }
}
