package com.finadvise.crm.clients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientSearchMinimalRepository extends
        JpaRepository<ClientSearchMinimal, String>,
        JpaSpecificationExecutor<ClientSearchMinimal> {
}
