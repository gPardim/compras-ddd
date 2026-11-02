package br.edu.pucgoias.compras.aplicacao;

import br.edu.pucgoias.compras.dominio.Fornecedor;
import br.edu.pucgoias.compras.dominio.ItemOrdem;
import br.edu.pucgoias.compras.dominio.OrdemDeCompra;
import br.edu.pucgoias.compras.dominio.RecursoNaoEncontradoException;
import br.edu.pucgoias.compras.dominio.RegraNegocioException;
import br.edu.pucgoias.compras.dominio.StatusOrdem;
import br.edu.pucgoias.compras.repositorio.FornecedorRepository;
import br.edu.pucgoias.compras.repositorio.ItemOrdemRepository;
import br.edu.pucgoias.compras.repositorio.OrdemDeCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrdemDeCompraService {

    private final OrdemDeCompraRepository ordens;
    private final ItemOrdemRepository itens;
    private final FornecedorRepository fornecedores;

    public OrdemDeCompraService(OrdemDeCompraRepository ordens, ItemOrdemRepository itens,
                                FornecedorRepository fornecedores) {
        this.ordens = ordens;
        this.itens = itens;
        this.fornecedores = fornecedores;
    }

    public Long abrir(Long fornecedorId, String solicitante) {
        Fornecedor fornecedor = fornecedores.findById(fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", fornecedorId));
        if (!fornecedor.isAtivo()) {
            throw new RegraNegocioException("O fornecedor " + fornecedorId + " está inativo.");
        }
        if (solicitante == null || solicitante.isBlank()) {
            throw new RegraNegocioException("O solicitante é obrigatório.");
        }
        OrdemDeCompra ordem = new OrdemDeCompra();
        ordem.setFornecedor(fornecedor);
        ordem.setSolicitante(solicitante);
        ordem.setStatus(StatusOrdem.RASCUNHO);
        return ordens.save(ordem).getId();
    }

    public void adicionarItem(Long ordemId, String codigoProduto, int quantidade, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade deve ser positiva: " + quantidade + ".");
        }
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() != StatusOrdem.RASCUNHO) {
            throw new RegraNegocioException("Não é possível alterar os itens de uma ordem " + ordem.getStatus() + ".");
        }
        Optional<ItemOrdem> existente = itens.findByOrdemIdOrderById(ordemId).stream()
                .filter(i -> i.getCodigoProduto().equals(codigoProduto))
                .findFirst();
        if (existente.isPresent()) {
            ItemOrdem item = existente.get();
            if (item.getPrecoUnitario().compareTo(precoUnitario) != 0) {
                throw new RegraNegocioException("O produto " + codigoProduto + " já consta da ordem com preço diferente.");
            }
            item.setQuantidade(item.getQuantidade() + quantidade);
            itens.save(item);
        } else {
            if (precoUnitario.signum() <= 0) {
                throw new RegraNegocioException("O preço unitário deve ser positivo.");
            }
            ItemOrdem item = new ItemOrdem();
            item.setOrdem(ordem);
            item.setCodigoProduto(codigoProduto);
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(precoUnitario);
            itens.save(item);
        }
    }

    public void removerItem(Long ordemId, String codigoProduto) {
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() != StatusOrdem.RASCUNHO) {
            throw new RegraNegocioException("Não é possível alterar os itens de uma ordem " + ordem.getStatus() + ".");
        }
        ItemOrdem item = itens.findByOrdemIdOrderById(ordemId).stream()
                .filter(i -> i.getCodigoProduto().equals(codigoProduto))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("O produto " + codigoProduto + " não consta da ordem."));
        itens.delete(item);
    }

    public void enviar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() != StatusOrdem.RASCUNHO) {
            throw new RegraNegocioException("Não é possível enviar uma ordem " + ordem.getStatus() + ".");
        }
        if (itens.findByOrdemIdOrderById(ordemId).isEmpty()) {
            throw new RegraNegocioException("Uma ordem sem itens não pode ser enviada.");
        }
        ordem.setStatus(StatusOrdem.ENVIADA);
    }

    public void aprovar(Long ordemId, BigDecimal alcadaDoAprovador) {
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() != StatusOrdem.ENVIADA) {
            throw new RegraNegocioException("Não é possível aprovar uma ordem " + ordem.getStatus() + ".");
        }
        BigDecimal total = total(itens.findByOrdemIdOrderById(ordemId));
        if (total.compareTo(alcadaDoAprovador) > 0) {
            throw new RegraNegocioException("O valor total " + total + " excede a alçada do aprovador ("
                    + alcadaDoAprovador.setScale(2, RoundingMode.HALF_EVEN) + ").");
        }
        ordem.setStatus(StatusOrdem.APROVADA);
    }

    public void receber(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() != StatusOrdem.APROVADA) {
            throw new RegraNegocioException("Não é possível receber uma ordem " + ordem.getStatus() + ".");
        }
        ordem.setStatus(StatusOrdem.RECEBIDA);
    }

    public void cancelar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        if (ordem.getStatus() == StatusOrdem.RECEBIDA || ordem.getStatus() == StatusOrdem.CANCELADA) {
            throw new RegraNegocioException("Uma ordem " + ordem.getStatus() + " não pode ser cancelada.");
        }
        ordem.setStatus(StatusOrdem.CANCELADA);
    }

    @Transactional(readOnly = true)
    public OrdemResumo consultar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        List<ItemOrdem> lista = itens.findByOrdemIdOrderById(ordemId);
        return new OrdemResumo(ordem.getId(), ordem.getFornecedor().getId(), ordem.getStatus().name(), total(lista),
                lista.stream().map(i -> new OrdemResumo.ItemResumo(i.getCodigoProduto(), i.getQuantidade(),
                        i.getPrecoUnitario(), subtotal(i))).toList());
    }

    private OrdemDeCompra carregar(Long ordemId) {
        return ordens.findById(ordemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de compra", ordemId));
    }

    private static BigDecimal subtotal(ItemOrdem i) {
        return i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal total(List<ItemOrdem> lista) {
        return lista.stream().map(OrdemDeCompraService::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_EVEN);
    }
}
