package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}
