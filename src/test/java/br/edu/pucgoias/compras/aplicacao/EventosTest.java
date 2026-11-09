package br.edu.pucgoias.compras.aplicacao;

import br.edu.pucgoias.compras.dominio.Dinheiro;
import br.edu.pucgoias.compras.dominio.Fornecedor;
import br.edu.pucgoias.compras.dominio.OrdemAprovada;
import br.edu.pucgoias.compras.dominio.RegraNegocioException;
import br.edu.pucgoias.compras.repositorio.FornecedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class EventosTest {

    @Autowired
    private OrdemDeCompraService servico;

    @Autowired
    private FornecedorRepository fornecedores;

    @Autowired
    private ApplicationEvents eventos;

    private Long ordemEnviada() {
        Long fornecedorId = fornecedores.save(new Fornecedor("Papelaria Central", "11222333000144")).getId();
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");
        servico.adicionarItem(id, "A-100", 3, new BigDecimal("10.50"));
        servico.enviar(id);
        return id;
    }

    @Test
    @DisplayName("V1. A aprovação publica OrdemAprovada com o total da ordem")
    void v01_aprovacaoPublicaEvento() {
        Long id = ordemEnviada();

        servico.aprovar(id, new BigDecimal("100.00"));

        assertThat(eventos.stream(OrdemAprovada.class)).hasSize(1);
        OrdemAprovada evento = eventos.stream(OrdemAprovada.class).findFirst().orElseThrow();
        assertThat(evento.ordemId()).isEqualTo(id);
        assertThat(evento.total()).isEqualTo(Dinheiro.de("31.50"));
    }

    @Test
    @DisplayName("V2. Aprovação rejeitada não publica evento")
    void v02_semEventoNaFalha() {
        Long id = ordemEnviada();

        assertThatThrownBy(() -> servico.aprovar(id, new BigDecimal("10.00")))
                .isInstanceOf(RegraNegocioException.class);

        assertThat(eventos.stream(OrdemAprovada.class)).isEmpty();
    }
}
