package br.edu.pucgoias.compras.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemDeCompraTest {

    private static OrdemDeCompra ordemComItem(String total) {
        OrdemDeCompra ordem = OrdemDeCompra.abrir(1L, "Setor de Manutenção");
        ordem.adicionarItem("A-100", new Quantidade(1), Dinheiro.de(total));
        return ordem;
    }

    @Test
    @DisplayName("D1. Dinheiro: igualdade por valor e aritmética imutável")
    void d01_dinheiro() {
        assertThat(Dinheiro.de("10.5")).isEqualTo(Dinheiro.de("10.50"));

        Dinheiro original = Dinheiro.de("10.00");
        Dinheiro soma = original.somar(Dinheiro.de("5.00"));
        assertThat(soma).isEqualTo(Dinheiro.de("15.00"));
        assertThat(original).isEqualTo(Dinheiro.de("10.00"));
        assertThat(soma).isNotSameAs(original);

        assertThat(Dinheiro.de("10.00").multiplicar(new Quantidade(3)))
                .isEqualTo(Dinheiro.de("30.00"));
    }

    @Test
    @DisplayName("D2. Quantidade não admite valor nulo ou negativo")
    void d02_quantidade() {
        assertThatThrownBy(() -> new Quantidade(0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new Quantidade(-1)).isInstanceOf(RegraNegocioException.class);

        assertThat(new Quantidade(2).somar(new Quantidade(3))).isEqualTo(new Quantidade(5));
    }

    @Test
    @DisplayName("D3. Ordem nasce como rascunho e calcula o total a partir dos itens")
    void d03_total() {
        OrdemDeCompra ordem = OrdemDeCompra.abrir(1L, "Setor de Manutenção");
        ordem.adicionarItem("A-100", new Quantidade(3), Dinheiro.de("10.50"));
        ordem.adicionarItem("B-200", new Quantidade(2), Dinheiro.de("7.25"));

        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.RASCUNHO);
        assertThat(ordem.total()).isEqualTo(Dinheiro.de("46.00"));
    }

    @Test
    @DisplayName("D4. Mesmo produto acumula quantidade em um único item")
    void d04_acumulo() {
        OrdemDeCompra ordem = OrdemDeCompra.abrir(1L, "Setor de Manutenção");
        ordem.adicionarItem("A-100", new Quantidade(3), Dinheiro.de("10.50"));
        ordem.adicionarItem("A-100", new Quantidade(2), Dinheiro.de("10.5"));

        assertThat(ordem.getItens()).hasSize(1);
        assertThat(ordem.getItens().get(0).getQuantidade()).isEqualTo(new Quantidade(5));
    }

    @Test
    @DisplayName("D5. Ordem enviada não aceita alteração de itens")
    void d05_imutavelAposEnvio() {
        OrdemDeCompra ordem = ordemComItem("10.00");
        ordem.enviar();

        assertThatThrownBy(() -> ordem.adicionarItem("B-200", new Quantidade(1), Dinheiro.de("5.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("alterar os itens");
    }

    @Test
    @DisplayName("D6. Aprovação acima da alçada é rejeitada sem alterar o estado")
    void d06_alcada() {
        OrdemDeCompra ordem = ordemComItem("100.00");
        ordem.enviar();

        assertThatThrownBy(() -> ordem.aprovar(Dinheiro.de("99.99")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede a alçada");
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.ENVIADA);

        ordem.aprovar(Dinheiro.de("100.00"));
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.APROVADA);
    }

    @Test
    @DisplayName("D7. Ciclo de vida completo e cancelamento")
    void d07_cicloDeVida() {
        OrdemDeCompra ordem = ordemComItem("10.00");
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.RASCUNHO);

        ordem.enviar();
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.ENVIADA);

        ordem.aprovar(Dinheiro.de("1000.00"));
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.APROVADA);

        ordem.receber();
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdem.RECEBIDA);

        assertThatThrownBy(ordem::cancelar)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser cancelada");
    }
}
