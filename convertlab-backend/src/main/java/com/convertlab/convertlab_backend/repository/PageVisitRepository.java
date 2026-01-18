package com.convertlab.convertlab_backend.repository;


import com.convertlab.convertlab_backend.entity.PageVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PageVisitRepository extends JpaRepository<PageVisit, UUID> {

}
