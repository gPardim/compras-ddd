package br.edu.pucgoias.compras.repositorio;

import br.edu.pucgoias.compras.dominio.OrdemDeCompra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdemDeCompraRepository extends JpaRepository<OrdemDeCompra, Long> {
}
