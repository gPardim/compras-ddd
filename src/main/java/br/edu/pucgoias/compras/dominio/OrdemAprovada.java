package br.edu.pucgoias.compras.dominio;

public record OrdemAprovada(Long ordemId, Long fornecedorId, Dinheiro total) {
}
