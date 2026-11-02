package br.edu.pucgoias.compras.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordem_compra")
public class OrdemDeCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(nullable = false, length = 80)
    private String solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusOrdem status;

    @OneToMany(mappedBy = "ordem")
    private List<ItemOrdem> itens = new ArrayList<>();

    public OrdemDeCompra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public StatusOrdem getStatus() {
        return status;
    }

    public void setStatus(StatusOrdem status) {
        this.status = status;
    }

    public List<ItemOrdem> getItens() {
        return itens;
    }

    public void setItens(List<ItemOrdem> itens) {
        this.itens = itens;
    }
}
