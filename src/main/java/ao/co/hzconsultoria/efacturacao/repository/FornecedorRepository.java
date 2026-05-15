package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    List<Fornecedor> findByEmpresa_Id(Long empresaId);
    List<Fornecedor> findByNomeContainingIgnoreCaseAndEmpresa_Id(String nome, Long empresaId);
}
