package br.edu.pucgoias.compras.repositorio;

import br.edu.pucgoias.compras.dominio.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
}
