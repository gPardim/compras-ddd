package br.edu.pucgoias.compras.dominio;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_ordem")
public class ItemOrdem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_produto", nullable = false, length = 20)
    private String codigoProduto;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "quantidade", nullable = false))
    private Quantidade quantidade;

    @Embedded
    @AttributeOverride(name = "valor",
            column = @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2))
    private Dinheiro precoUnitario;

    protected ItemOrdem() {
    }

    ItemOrdem(String codigoProduto, Quantidade quantidade, Dinheiro precoUnitario) {
        if (!precoUnitario.ehPositivo()) {
            throw new RegraNegocioException("O preço unitário deve ser positivo.");
        }
        this.codigoProduto = codigoProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    void acrescentar(Quantidade adicional) {
        this.quantidade = this.quantidade.somar(adicional);
    }

    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade);
    }

    public Long getId() {
        return id;
    }

    public String getCodigoProduto() {
        return codigoProduto;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }

    public Dinheiro getPrecoUnitario() {
        return precoUnitario;
    }
}
