package br.edu.pucgoias.compras.repositorio;

import br.edu.pucgoias.compras.dominio.ItemOrdem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemOrdemRepository extends JpaRepository<ItemOrdem, Long> {

    List<ItemOrdem> findByOrdemIdOrderById(Long ordemId);
}
