package br.edu.pucgoias.compras.aplicacao;

import br.edu.pucgoias.compras.dominio.Fornecedor;
import br.edu.pucgoias.compras.dominio.RecursoNaoEncontradoException;
import br.edu.pucgoias.compras.dominio.RegraNegocioException;
import br.edu.pucgoias.compras.repositorio.FornecedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrdemDeCompraAceitacaoTest {

    @Autowired
    private OrdemDeCompraService servico;

    @Autowired
    private FornecedorRepository fornecedores;

    private Long fornecedorId;

    @BeforeEach
    void preparar() {
        fornecedorId = fornecedores.save(new Fornecedor("Papelaria Central", "11222333000144")).getId();
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    /** Ordem com A (3 x 10,50) e B (2 x 7,25): total 46,00. */
    private Long ordemComDoisItens() {
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");
        servico.adicionarItem(id, "A-100", 3, v("10.50"));
        servico.adicionarItem(id, "B-200", 2, v("7.25"));
        return id;
    }

    @Test
    @DisplayName("A1. Ordem aberta com fornecedor ativo nasce como rascunho, sem itens")
    void a01_abrir() {
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");

        OrdemResumo ordem = servico.consultar(id);
        assertThat(ordem.status()).isEqualTo("RASCUNHO");
        assertThat(ordem.fornecedorId()).isEqualTo(fornecedorId);
        assertThat(ordem.total()).isEqualByComparingTo("0");
        assertThat(ordem.itens()).isEmpty();
    }

    @Test
    @DisplayName("A2. Fornecedor inexistente ou inativo impede a abertura")
    void a02_fornecedorInvalido() {
        assertThatThrownBy(() -> servico.abrir(9999L, "Setor de Manutenção"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        Fornecedor inativo = new Fornecedor("Gráfica Norte", "55666777000188");
        inativo.desativar();
        Long inativoId = fornecedores.save(inativo).getId();
        assertThatThrownBy(() -> servico.abrir(inativoId, "Setor de Manutenção"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    @DisplayName("A3. O total é a soma de quantidade x preço unitário dos itens")
    void a03_total() {
        OrdemResumo ordem = servico.consultar(ordemComDoisItens());

        assertThat(ordem.itens()).hasSize(2);
        assertThat(ordem.total()).isEqualByComparingTo("46.00");
    }

    @Test
    @DisplayName("A4. O mesmo produto acumula quantidade; preço diferente é rejeitado")
    void a04_mesmoProduto() {
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");
        servico.adicionarItem(id, "A-100", 3, v("10.50"));
        servico.adicionarItem(id, "A-100", 2, v("10.5"));

        OrdemResumo ordem = servico.consultar(id);
        assertThat(ordem.itens()).hasSize(1);
        assertThat(ordem.itens().get(0).quantidade()).isEqualTo(5);

        assertThatThrownBy(() -> servico.adicionarItem(id, "A-100", 1, v("11.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("preço diferente");
    }

    @Test
    @DisplayName("A5. Quantidade e preço unitário devem ser positivos")
    void a05_valoresPositivos() {
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");

        assertThatThrownBy(() -> servico.adicionarItem(id, "A-100", 0, v("10.50")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("quantidade deve ser positiva");
        assertThatThrownBy(() -> servico.adicionarItem(id, "A-100", 1, v("0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("preço unitário deve ser positivo");
        assertThat(servico.consultar(id).itens()).isEmpty();
    }

    @Test
    @DisplayName("A6. Remoção de item existente; produto ausente é rejeitado")
    void a06_remocao() {
        Long id = ordemComDoisItens();
        servico.removerItem(id, "B-200");

        OrdemResumo ordem = servico.consultar(id);
        assertThat(ordem.itens()).hasSize(1);
        assertThat(ordem.total()).isEqualByComparingTo("31.50");

        assertThatThrownBy(() -> servico.removerItem(id, "Z-999"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não consta");
    }

    @Test
    @DisplayName("A7. Envio exige itens; após o envio, os itens não podem ser alterados")
    void a07_envio() {
        Long vazia = servico.abrir(fornecedorId, "Setor de Manutenção");
        assertThatThrownBy(() -> servico.enviar(vazia))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("sem itens");

        Long id = ordemComDoisItens();
        servico.enviar(id);
        assertThat(servico.consultar(id).status()).isEqualTo("ENVIADA");

        assertThatThrownBy(() -> servico.adicionarItem(id, "C-300", 1, v("5.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("alterar os itens");
        assertThatThrownBy(() -> servico.removerItem(id, "A-100"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("alterar os itens");
    }

    @Test
    @DisplayName("A8. A aprovação exige alçada igual ou superior ao total")
    void a08_alcada() {
        Long id = ordemComDoisItens();
        servico.enviar(id);

        assertThatThrownBy(() -> servico.aprovar(id, v("40.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede a alçada");
        assertThat(servico.consultar(id).status()).isEqualTo("ENVIADA");

        servico.aprovar(id, v("46.00"));
        assertThat(servico.consultar(id).status()).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("A9. Transições fora de ordem são rejeitadas; ordem recebida não é cancelada")
    void a09_transicoes() {
        Long id = ordemComDoisItens();

        assertThatThrownBy(() -> servico.aprovar(id, v("1000.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é possível aprovar");
        servico.enviar(id);
        assertThatThrownBy(() -> servico.receber(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é possível receber");

        servico.aprovar(id, v("1000.00"));
        servico.receber(id);
        assertThat(servico.consultar(id).status()).isEqualTo("RECEBIDA");

        assertThatThrownBy(() -> servico.cancelar(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser cancelada");
    }

    @Test
    @DisplayName("A10. Ordem cancelada não pode ser enviada; ordem inexistente é rejeitada")
    void a10_cancelamento() {
        Long id = ordemComDoisItens();
        servico.cancelar(id);
        assertThat(servico.consultar(id).status()).isEqualTo("CANCELADA");

        assertThatThrownBy(() -> servico.enviar(id))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é possível enviar");
        assertThatThrownBy(() -> servico.consultar(9999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
