package br.edu.pucgoias.compras.aplicacao;

import br.edu.pucgoias.compras.dominio.Dinheiro;
import br.edu.pucgoias.compras.dominio.Fornecedor;
import br.edu.pucgoias.compras.dominio.OrdemDeCompra;
import br.edu.pucgoias.compras.dominio.Quantidade;
import br.edu.pucgoias.compras.dominio.RecursoNaoEncontradoException;
import br.edu.pucgoias.compras.dominio.RegraNegocioException;
import br.edu.pucgoias.compras.repositorio.FornecedorRepository;
import br.edu.pucgoias.compras.repositorio.OrdemDeCompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class OrdemDeCompraService {

    private final OrdemDeCompraRepository ordens;
    private final FornecedorRepository fornecedores;

    public OrdemDeCompraService(OrdemDeCompraRepository ordens, FornecedorRepository fornecedores) {
        this.ordens = ordens;
        this.fornecedores = fornecedores;
    }

    public Long abrir(Long fornecedorId, String solicitante) {
        Fornecedor fornecedor = fornecedores.findById(fornecedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", fornecedorId));
        if (!fornecedor.isAtivo()) {
            throw new RegraNegocioException("O fornecedor " + fornecedorId + " está inativo.");
        }
        return ordens.save(OrdemDeCompra.abrir(fornecedorId, solicitante)).getId();
    }

    public void adicionarItem(Long ordemId, String codigoProduto, int quantidade, BigDecimal precoUnitario) {
        Quantidade qtd = new Quantidade(quantidade);
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.adicionarItem(codigoProduto, qtd, new Dinheiro(precoUnitario));
        ordens.save(ordem);
    }

    public void removerItem(Long ordemId, String codigoProduto) {
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.removerItem(codigoProduto);
        ordens.save(ordem);
    }

    public void enviar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.enviar();
        ordens.save(ordem);
    }

    public void aprovar(Long ordemId, BigDecimal alcadaDoAprovador) {
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.aprovar(new Dinheiro(alcadaDoAprovador));
        ordens.save(ordem);
    }

    public void receber(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.receber();
        ordens.save(ordem);
    }

    public void cancelar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        ordem.cancelar();
        ordens.save(ordem);
    }

    @Transactional(readOnly = true)
    public OrdemResumo consultar(Long ordemId) {
        OrdemDeCompra ordem = carregar(ordemId);
        return new OrdemResumo(ordem.getId(), ordem.getFornecedorId(), ordem.getStatus().name(),
                ordem.total().valor(),
                ordem.getItens().stream()
                        .map(i -> new OrdemResumo.ItemResumo(i.getCodigoProduto(), i.getQuantidade().valor(),
                                i.getPrecoUnitario().valor(), i.subtotal().valor()))
                        .toList());
    }

    private OrdemDeCompra carregar(Long ordemId) {
        return ordens.findById(ordemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de compra", ordemId));
    }
}
