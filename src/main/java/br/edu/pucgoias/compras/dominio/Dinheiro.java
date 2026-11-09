package br.edu.pucgoias.compras.dominio;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Dinheiro(BigDecimal valor) {

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    public Dinheiro {
        valor = valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Dinheiro de(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro multiplicar(Quantidade quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade.valor())));
    }

    public boolean maiorQue(Dinheiro outro) {
        return valor.compareTo(outro.valor) > 0;
    }

    public boolean ehPositivo() {
        return valor.signum() > 0;
    }
}
