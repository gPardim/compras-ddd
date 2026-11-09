package br.edu.pucgoias.compras.aplicacao;

import br.edu.pucgoias.compras.dominio.OrdemAprovada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoFornecedor {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoFornecedor.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAprovar(OrdemAprovada evento) {
        log.info("Notificando fornecedor {}: ordem {} aprovada, total {}",
                evento.fornecedorId(), evento.ordemId(), evento.total().valor());
    }
}
