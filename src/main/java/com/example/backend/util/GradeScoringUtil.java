package com.example.backend.util;

import com.example.backend.entity.AssessmentTypeLookup;
import com.example.backend.entity.Grade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class GradeScoringUtil {

    private GradeScoringUtil() {
    }

    public static Set<String> weightedTypeCodes(List<AssessmentTypeLookup> types) {
        return types.stream()
            .map(AssessmentTypeLookup::getCode)
            .collect(Collectors.toSet());
    }

    public static BigDecimal sumMaxScores(List<AssessmentTypeLookup> types) {
        return types.stream()
            .map(AssessmentTypeLookup::getDefaultMaxScore)
            .filter(max -> max != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal computeSubjectTotal(List<Grade> subjectGrades, Set<String> weightedTypeCodes) {
        BigDecimal total = BigDecimal.ZERO;
        boolean hasScore = false;
        for (Grade grade : subjectGrades) {
            if (!weightedTypeCodes.contains(grade.getAssessmentType())) {
                continue;
            }
            if (grade.getScore() != null) {
                total = total.add(grade.getScore());
                hasScore = true;
            }
        }
        return hasScore ? total.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public static BigDecimal computeTermAverage(List<Grade> grades, Set<String> weightedTypeCodes) {
        if (grades.isEmpty()) {
            return null;
        }
        Map<Long, List<Grade>> bySubject = grades.stream()
            .collect(Collectors.groupingBy(Grade::getSubjectId));
        List<BigDecimal> subjectTotals = new ArrayList<>();
        for (List<Grade> subjectGrades : bySubject.values()) {
            BigDecimal total = computeSubjectTotal(subjectGrades, weightedTypeCodes);
            if (total != null) {
                subjectTotals.add(total);
            }
        }
        if (subjectTotals.isEmpty()) {
            return null;
        }
        BigDecimal sum = subjectTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(subjectTotals.size()), 2, RoundingMode.HALF_UP);
    }
}
