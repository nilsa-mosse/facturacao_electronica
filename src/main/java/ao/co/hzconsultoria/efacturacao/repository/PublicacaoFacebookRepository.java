package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.PublicacaoFacebook;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PublicacaoFacebookRepository extends JpaRepository<PublicacaoFacebook, Long> {

    List<PublicacaoFacebook> findByEmpresaOrderByDataPublicacaoDesc(Empresa empresa);

    List<PublicacaoFacebook> findByEmpresaIdOrderByDataPublicacaoDesc(Long empresaId);

    List<PublicacaoFacebook> findTop20ByEmpresaIdOrderByDataPublicacaoDesc(Long empresaId);

    @Query("SELECT COUNT(p) FROM PublicacaoFacebook p WHERE p.empresa.id = :empresaId AND p.dataPublicacao >= :inicio AND p.status = 'SUCESSO'")
    long countPublicacoesHoje(@Param("empresaId") Long empresaId, @Param("inicio") LocalDateTime inicio);

    List<PublicacaoFacebook> findByEmpresaIdAndStatus(Long empresaId, String status);
}
