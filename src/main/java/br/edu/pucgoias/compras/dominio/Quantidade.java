package br.edu.pucgoias.compras.dominio;

import jakarta.persistence.Embeddable;

@Embeddable
public record Quantidade(int valor) {

    public Quantidade {
        if (valor <= 0) {
            throw new RegraNegocioException("A quantidade deve ser positiva: " + valor + ".");
        }
    }

    public Quantidade somar(Quantidade outra) {
        return new Quantidade(valor + outra.valor);
    }
}
