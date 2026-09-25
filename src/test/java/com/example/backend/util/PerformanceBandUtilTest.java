package com.example.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerformanceBandUtilTest {

    @Test
    void mapsSchoolTemplateBands() {
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("90"))).isEqualTo(PerformanceBandUtil.EXCELLENT);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("89"))).isEqualTo(PerformanceBandUtil.VERY_GOOD);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("80"))).isEqualTo(PerformanceBandUtil.VERY_GOOD);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("79"))).isEqualTo(PerformanceBandUtil.GOOD);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("70"))).isEqualTo(PerformanceBandUtil.GOOD);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("69"))).isEqualTo(PerformanceBandUtil.SATISFACTORY);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("60"))).isEqualTo(PerformanceBandUtil.SATISFACTORY);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("59"))).isEqualTo(PerformanceBandUtil.AVERAGE);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("50"))).isEqualTo(PerformanceBandUtil.AVERAGE);
        assertThat(PerformanceBandUtil.toBand(new BigDecimal("49"))).isEqualTo(PerformanceBandUtil.BELOW_AVERAGE);
        assertThat(PerformanceBandUtil.toBand(null)).isNull();
    }
}
