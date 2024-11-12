package com.tdtu.phonecommerce.repository;

import com.tdtu.phonecommerce.models.Colors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ColorsRepository extends JpaRepository<Colors, Long> {


}
