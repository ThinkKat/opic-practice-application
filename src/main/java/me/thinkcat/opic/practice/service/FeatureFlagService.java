package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    @Transactional(readOnly = true)
    public boolean isEnabled(String flagName) {
        return featureFlagRepository.findById(flagName)
                .map(flag -> flag.isEnabled())
                .orElse(false);
    }
}
