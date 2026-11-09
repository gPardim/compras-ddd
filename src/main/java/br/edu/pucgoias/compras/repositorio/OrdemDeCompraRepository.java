package br.edu.pucgoias.compras.repositorio;

import br.edu.pucgoias.compras.dominio.OrdemDeCompra;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface OrdemDeCompraRepository extends Repository<OrdemDeCompra, Long> {

    OrdemDeCompra save(OrdemDeCompra ordem);

    Optional<OrdemDeCompra> findById(Long id);
}
