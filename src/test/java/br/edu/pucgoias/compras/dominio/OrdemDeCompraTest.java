package br.edu.pucgoias.compras.dominio;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemDeCompraTest {

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
    @Disabled("A escrever pelo estudante")
    @DisplayName("D3. Ordem nasce como rascunho e calcula o total a partir dos itens")
    void d03_total() {
        // Abrir uma ordem com OrdemDeCompra.abrir(1L, "..."), adicionar A (3 x 10,50)
        // e B (2 x 7,25) e verificar status RASCUNHO e total igual a 46,00.
    }

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("D4. Mesmo produto acumula quantidade em um único item")
    void d04_acumulo() {
        // Adicionar o mesmo produto duas vezes, com o mesmo preço,
        // e verificar um único item com a quantidade somada.
    }

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("D5. Ordem enviada não aceita alteração de itens")
    void d05_imutavelAposEnvio() {
        // Enviar uma ordem com item e verificar que adicionarItem lança
        // RegraNegocioException com mensagem contendo "alterar os itens".
    }

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("D6. Aprovação acima da alçada é rejeitada sem alterar o estado")
    void d06_alcada() {
        // Ordem enviada com total 100,00: aprovar com alçada 99,99 deve falhar e
        // manter o status ENVIADA; aprovar com alçada 100,00 deve resultar em APROVADA.
    }

    @Test
    @Disabled("A escrever pelo estudante")
    @DisplayName("D7. Ciclo de vida completo e cancelamento")
    void d07_cicloDeVida() {
        // Percorrer RASCUNHO -> ENVIADA -> APROVADA -> RECEBIDA e verificar
        // que cancelar uma ordem RECEBIDA lança RegraNegocioException.
    }
}
