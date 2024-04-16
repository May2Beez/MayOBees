package com.github.may2beez.mayobees.feature;

import com.github.may2beez.mayobees.feature.impl.AutoBazaar;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class FeatureManager {
    private static FeatureManager instance;

    public static FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }

    private final List<IFeature> features = fillFeatures();

    public List<IFeature> fillFeatures() {
        return Arrays.asList(
                AutoBazaar.getInstance()
        );
    }
}
