package com.example.backend.repository;

import com.example.backend.entity.FeeSchoolTemplateItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeSchoolTemplateItemRepository extends JpaRepository<FeeSchoolTemplateItem, Long> {

    List<FeeSchoolTemplateItem> findAllByOrderBySortOrderAscNameAsc();
}
