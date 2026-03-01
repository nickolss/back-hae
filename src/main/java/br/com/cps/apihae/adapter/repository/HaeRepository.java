package br.com.cps.apihae.adapter.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cps.apihae.domain.entity.Hae;
import br.com.cps.apihae.useCase.Interface.IHaeRepository;

public interface HaeRepository extends IHaeRepository, JpaRepository<Hae, String>, JpaSpecificationExecutor<Hae> {
    @Query("""
                SELECT h FROM Hae h
                WHERE YEAR(h.createdAt) = :year
                  AND MONTH(h.createdAt) BETWEEN :monthStart AND :monthEnd
            """)
    List<Hae> findBySemestre(@Param("year") int year,
            @Param("monthStart") int monthStart,
            @Param("monthEnd") int monthEnd);

            
}