package org.verduttio.dominicanappbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.verduttio.dominicanappbackend.domain.Role;
import org.verduttio.dominicanappbackend.domain.RoleType;

import java.util.List;
import java.util.Optional;


public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findAllByOrderBySortOrderAsc();

    List<Role> findAllByAreTasksVisibleInPrintsOrderBySortOrderAsc(boolean areTasksVisibleInPrints);

    Optional<Role> findByName(String roleName);

    boolean existsByName(String roleName);

    List<Role> findByTypeOrderBySortOrderAsc(RoleType roleType);

    Optional<Role> findByNameAndType(String name, RoleType type);

    List<Role> findByAreTasksVisibleInPrintsOrderBySortOrderAsc(boolean areTasksVisibleInPrints);

    @Modifying
    @Query("UPDATE Role rl SET rl.sortOrder = rl.sortOrder + 1 WHERE rl.sortOrder >= :sortOrder")
    void incrementSortOrderGreaterThanOrEqualTo(@Param("sortOrder") Long sortOrder);

    @Modifying
    @Query("UPDATE Role rl SET rl.sortOrder = rl.sortOrder + 1 WHERE rl.sortOrder > :sortOrder")
    void incrementSortOrderGreaterThan(@Param("sortOrder") Long sortOrder);

    @Modifying
    @Query("UPDATE Role rl SET rl.sortOrder = rl.sortOrder - 1 WHERE rl.sortOrder > :sortOrder")
    void decrementSortOrderGreaterThan(@Param("sortOrder") Long sortOrder);

    Role findFirstByTypeOrderBySortOrderDesc(RoleType type);
}
