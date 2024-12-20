package com.tdtu.phonecommerce.repository;

import com.tdtu.phonecommerce.models.CartItems;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemsRepository extends JpaRepository<CartItems, Long> {

    @Query("SELECT ci FROM CartItems ci WHERE ci.cart.id = :cartId")
    List<CartItems> findByCartId(Long cartId);

    @Transactional
    @Modifying
    @Query("DELETE FROM CartItems ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(Long cartId);


}
