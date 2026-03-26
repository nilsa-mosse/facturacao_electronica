package ao.co.hzconsultoria.efacturacao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;

@Repository
public interface ReportRepository extends JpaRepository<ItemCompra, Long> 
{
	
}

	
