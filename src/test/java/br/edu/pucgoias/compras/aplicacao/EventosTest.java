package br.edu.pucgoias.compras.aplicacao;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class EventosTest {

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("V1. A aprovação publica OrdemAprovada com o total da ordem")
    void v01_aprovacaoPublicaEvento() {
        // Cadastrar fornecedor, abrir ordem, adicionar A (3 x 10,50), enviar e aprovar com alçada 100,00.
        // Verificar, com eventos.stream(OrdemAprovada.class), um único evento com o id da ordem e total 31,50.
    }

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("V2. Aprovação rejeitada não publica evento")
    void v02_semEventoNaFalha() {
        // Repetir o preparo, aprovar com alçada 10,00 (falha esperada) e verificar que nenhum evento foi publicado.
    }
}
