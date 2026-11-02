package br.edu.pucgoias.compras.dominio;

import br.edu.pucgoias.compras.aplicacao.OrdemDeCompraService;
import br.edu.pucgoias.compras.repositorio.FornecedorRepository;
import br.edu.pucgoias.compras.repositorio.OrdemDeCompraRepository;
import jakarta.persistence.Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ModeloRicoTest {

    private static final String PACOTE = "br.edu.pucgoias.compras.";

    @Autowired
    private OrdemDeCompraService servico;

    @Autowired
    private OrdemDeCompraRepository ordens;

    @Autowired
    private FornecedorRepository fornecedores;

    private static List<String> settersPublicos(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getName().startsWith("set"))
                .map(Method::getName)
                .toList();
    }

    private static boolean possuiCampoDoTipo(Class<?> dono, Class<?> tipo) {
        return Arrays.stream(dono.getDeclaredFields()).anyMatch(f -> f.getType() == tipo);
    }

    private static Class<?> classe(String nome) {
        try {
            return Class.forName(PACOTE + nome);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Test
    @DisplayName("E1. OrdemDeCompra não possui setters públicos")
    void e01_raizSemSetters() {
        assertThat(settersPublicos(OrdemDeCompra.class)).isEmpty();
    }

    @Test
    @DisplayName("E2. ItemOrdem não possui setters nem construtores públicos")
    void e02_itemEncapsulado() {
        assertThat(settersPublicos(ItemOrdem.class)).isEmpty();
        long construtoresPublicos = Arrays.stream(ItemOrdem.class.getDeclaredConstructors())
                .map(Constructor::getModifiers).filter(Modifier::isPublic).count();
        assertThat(construtoresPublicos).isZero();
    }

    @Test
    @DisplayName("E3. A ordem referencia o fornecedor pelo identificador, e não pela entidade")
    void e03_referenciaPorId() {
        assertThat(possuiCampoDoTipo(OrdemDeCompra.class, Fornecedor.class)).isFalse();
    }

    @Test
    @DisplayName("E4. ItemOrdem não referencia a ordem (associação controlada pela raiz)")
    void e04_associacaoUnidirecional() {
        assertThat(possuiCampoDoTipo(ItemOrdem.class, OrdemDeCompra.class)).isFalse();
    }

    @Test
    @DisplayName("E5. Dinheiro e Quantidade são objetos de valor (records) usados por ItemOrdem")
    void e05_objetosDeValor() {
        Class<?> dinheiro = classe("dominio.Dinheiro");
        Class<?> quantidade = classe("dominio.Quantidade");
        assertThat(dinheiro != null && dinheiro.isRecord()).isTrue();
        assertThat(quantidade != null && quantidade.isRecord()).isTrue();
        assertThat(possuiCampoDoTipo(ItemOrdem.class, dinheiro)).isTrue();
        assertThat(possuiCampoDoTipo(ItemOrdem.class, quantidade)).isTrue();
    }

    @Test
    @DisplayName("E6. A raiz possui atributo de versão (@Version)")
    void e06_versao() {
        boolean versionada = false;
        for (Field f : OrdemDeCompra.class.getDeclaredFields()) {
            versionada |= f.isAnnotationPresent(Version.class);
        }
        assertThat(versionada).isTrue();
    }

    @Test
    @DisplayName("E7. Só a raiz tem repositório, e ele não estende JpaRepository")
    void e07_repositorioDoAgregado() {
        assertThat(JpaRepository.class.isAssignableFrom(OrdemDeCompraRepository.class)).isFalse();
        assertThat(classe("repositorio.ItemOrdemRepository")).isNull();
    }

    @Test
    @DisplayName("E8. A coleção de itens não pode ser alterada por fora da raiz")
    void e08_colecaoProtegida() {
        Long fornecedorId = fornecedores.save(new Fornecedor("Papelaria Central", "11222333000144")).getId();
        Long id = servico.abrir(fornecedorId, "Setor de Manutenção");
        servico.adicionarItem(id, "A-100", 1, new BigDecimal("10.00"));

        OrdemDeCompra ordem = ordens.findById(id).orElseThrow();
        assertThatThrownBy(() -> ordem.getItens().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
