package br.edu.pucgoias.compras.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.AbstractAggregateRoot;

@Entity
@Table(name = "ordem_compra")
public class OrdemDeCompra extends AbstractAggregateRoot<OrdemDeCompra> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long versao;

    @Column(name = "fornecedor_id", nullable = false)
    private Long fornecedorId;

    @Column(nullable = false, length = 80)
    private String solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusOrdem status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_id", nullable = false)
    private List<ItemOrdem> itens = new ArrayList<>();

    protected OrdemDeCompra() {
    }

    private OrdemDeCompra(Long fornecedorId, String solicitante) {
        this.fornecedorId = fornecedorId;
        this.solicitante = solicitante;
        this.status = StatusOrdem.RASCUNHO;
    }

    public static OrdemDeCompra abrir(Long fornecedorId, String solicitante) {
        if (solicitante == null || solicitante.isBlank()) {
            throw new RegraNegocioException("O solicitante é obrigatório.");
        }
        return new OrdemDeCompra(fornecedorId, solicitante);
    }

    public void adicionarItem(String codigoProduto, Quantidade quantidade, Dinheiro precoUnitario) {
        exigirRascunho();
        Optional<ItemOrdem> existente = buscarItem(codigoProduto);
        if (existente.isPresent()) {
            ItemOrdem item = existente.get();
            if (!item.getPrecoUnitario().equals(precoUnitario)) {
                throw new RegraNegocioException(
                        "O produto " + codigoProduto + " já consta da ordem com preço diferente.");
            }
            item.acrescentar(quantidade);
        } else {
            itens.add(new ItemOrdem(codigoProduto, quantidade, precoUnitario));
        }
    }

    public void removerItem(String codigoProduto) {
        exigirRascunho();
        ItemOrdem item = buscarItem(codigoProduto)
                .orElseThrow(() -> new RegraNegocioException(
                        "O produto " + codigoProduto + " não consta da ordem."));
        itens.remove(item);
    }

    public void enviar() {
        if (status != StatusOrdem.RASCUNHO) {
            throw new RegraNegocioException("Não é possível enviar uma ordem " + status + ".");
        }
        if (itens.isEmpty()) {
            throw new RegraNegocioException("Uma ordem sem itens não pode ser enviada.");
        }
        this.status = StatusOrdem.ENVIADA;
    }

    public void aprovar(Dinheiro alcadaDoAprovador) {
        if (status != StatusOrdem.ENVIADA) {
            throw new RegraNegocioException("Não é possível aprovar uma ordem " + status + ".");
        }
        Dinheiro total = total();
        if (total.maiorQue(alcadaDoAprovador)) {
            throw new RegraNegocioException("O valor total " + total.valor()
                    + " excede a alçada do aprovador (" + alcadaDoAprovador.valor() + ").");
        }
        this.status = StatusOrdem.APROVADA;
        registerEvent(new OrdemAprovada(id, fornecedorId, total));
    }

    public void receber() {
        if (status != StatusOrdem.APROVADA) {
            throw new RegraNegocioException("Não é possível receber uma ordem " + status + ".");
        }
        this.status = StatusOrdem.RECEBIDA;
    }

    public void cancelar() {
        if (status == StatusOrdem.RECEBIDA || status == StatusOrdem.CANCELADA) {
            throw new RegraNegocioException("Uma ordem " + status + " não pode ser cancelada.");
        }
        this.status = StatusOrdem.CANCELADA;
    }

    public Dinheiro total() {
        return itens.stream().map(ItemOrdem::subtotal).reduce(Dinheiro.ZERO, Dinheiro::somar);
    }

    private void exigirRascunho() {
        if (status != StatusOrdem.RASCUNHO) {
            throw new RegraNegocioException("Não é possível alterar os itens de uma ordem " + status + ".");
        }
    }

    private Optional<ItemOrdem> buscarItem(String codigoProduto) {
        return itens.stream().filter(i -> i.getCodigoProduto().equals(codigoProduto)).findFirst();
    }

    public Long getId() {
        return id;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public StatusOrdem getStatus() {
        return status;
    }

    public List<ItemOrdem> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
