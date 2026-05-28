package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    List<FeatureFlag> findAllByExposeToFrontendTrueOrderByTitleAsc();

    List<FeatureFlag> findAllByOrderByTitleAsc();
}
