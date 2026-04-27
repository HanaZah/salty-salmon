package com.finadvise.crm.products;

import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.common.ResourceNotFoundException;
import com.finadvise.crm.users.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProviderRepository providerRepository;
    private final ProductMapper productMapper;
    private final OwnershipValidator ownershipValidator;
    private final ClientRepository clientRepository;
    private final AdvisorRepository advisorRepository;
    private final Clock clock;

    private LocalDate calculateNextAnniversary(LocalDate startDate) {
        LocalDate today = LocalDate.now(clock);

        // If the product hasn't even started yet or starts today, the first anniversary is the start date + 1 year
        if (!startDate.isBefore(today)) {
            return startDate.plusYears(1);
        }

        LocalDate anniversaryThisYear = startDate.withYear(today.getYear());

        if (!anniversaryThisYear.isAfter(today)) {
            return anniversaryThisYear.plusYears(1);
        }

        return anniversaryThisYear;
    }

    @Transactional
    public ProductDTO createProduct(String clientUid, ProductDTO payload, String employeeId) {
        if (!ownershipValidator.canAccessClient(clientUid, employeeId)) {
            throw new AccessDeniedException("Client not found or access denied");
        }

        Long clientId = clientRepository.findIdByClientUid(clientUid)
                .orElseThrow();

        ProductType productType = productTypeRepository.findById(payload.productTypeId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product Type not found with ID: " + payload.productTypeId())
                );
        Provider provider = providerRepository.findById(payload.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with ID: " + payload.providerId()));

        Product newProduct = Product.builder()
                .name(payload.name())
                .amount(payload.amount())
                .productType(productType)
                .client(clientRepository.getReferenceById(clientId))
                .provider(provider)
                .startDate(payload.startDate())
                .endDate(payload.endDate())
                .nextAnniversary(calculateNextAnniversary(payload.startDate()))
                .build();

        if(payload.managedByEmployeeId() != null) {
            Long advisorId = advisorRepository.findIdByEmployeeId(payload.managedByEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Advisor not found with ID: " + payload.managedByEmployeeId()
                    ));
            newProduct.setManagedBy(advisorRepository.getReferenceById(advisorId));
        }

        return productMapper.toDto(productRepository.save(newProduct));
    }

    @Transactional
    public ProductDTO updateProduct(String clientUid, Long productId, ProductDTO payload, String employeeId) {
        if (!ownershipValidator.canModifyProduct(clientUid, productId, employeeId)) {
            throw new ResourceNotFoundException("Product not found, does not belong to this client, or access denied");
        }

        Product existingProduct = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product not found with ID: " + productId)
        );

        existingProduct.setName(payload.name());
        existingProduct.setAmount(payload.amount());
        existingProduct.setEndDate(payload.endDate());

        return productMapper.toDto(productRepository.save(existingProduct));
    }

    @Transactional
    public void deleteProduct(String clientUid, Long productId, String employeeId) {
        if (!ownershipValidator.canModifyProduct(clientUid, productId, employeeId)) {
            throw new ResourceNotFoundException("Product not found, does not belong to this client, or access denied");
        }

        productRepository.deleteById(productId);
    }

    @Transactional
    public ClientProductsDTO getClientProducts(String clientUid, String employeeId) {
        if (!ownershipValidator.hasAnyReadAccessToClientProducts(clientUid, employeeId)) {
            throw new ResourceNotFoundException("Client not found or access denied");
        }

        boolean isPrimaryAdvisor = ownershipValidator.canAccessClient(clientUid, employeeId);

        List<Product> authorizedProducts;
        if (isPrimaryAdvisor) {
            authorizedProducts = productRepository.findAllByClientClientUid(clientUid);
        } else {
            authorizedProducts = productRepository.findAllByClientClientUidAndManagedByEmployeeId(clientUid, employeeId);
        }

        List<ProductDTO> productDTOs = authorizedProducts.stream()
                .map(productMapper::toDto)
                .toList();

        Integer totalActive = (int) authorizedProducts.stream()
                .filter(productMapper::isActive)
                .count();

        return new ClientProductsDTO(clientUid, productDTOs, totalActive);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(ProductSearchCriteriaDTO criteria, String employeeId, Pageable pageable) {

        Specification<Product> spec = ProductSpecifications.withCriteria(criteria, employeeId);
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(productMapper::toDto);
    }
}
