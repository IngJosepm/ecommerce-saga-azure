package com.bigdavi.ecommerce.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Descuento condicional y atómico: si dos pedidos compiten por la última unidad,
     * PostgreSQL serializa ambos UPDATE sobre la fila y solo uno cumple "available >= quantity".
     *
     * @return 1 si se reservó, 0 si no había stock suficiente (o el producto no existe)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.available = p.available - :quantity "
            + "where p.id = :productId and p.available >= :quantity")
    int tryReserve(@Param("productId") String productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.available = p.available + :quantity where p.id = :productId")
    int release(@Param("productId") String productId, @Param("quantity") int quantity);
}
