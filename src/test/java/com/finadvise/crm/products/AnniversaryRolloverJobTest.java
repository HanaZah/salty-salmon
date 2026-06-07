package com.finadvise.crm.products;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnniversaryRolloverJobTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AnniversaryProcessor anniversaryProcessor;

    @InjectMocks
    private AnniversaryRolloverJob rolloverJob;

    private Product dummyProduct;

    @BeforeEach
    void setUp() {
        // Using dummies for relations since we only test the loop logic here, not JPA constraints
        dummyProduct = Product.builder()
                .id(1L)
                .name("Unit Test Product")
                .amount(new BigDecimal("1000.00"))
                .startDate(LocalDate.now().minusYears(1))
                .nextAnniversary(LocalDate.now().minusDays(1))
                .productType(new ProductType())
                .client(new com.finadvise.crm.clients.Client())
                .provider(new Provider())
                .build();
    }

    @Test
    void rollForwardAnniversaries_WhenNoProducts_DoesNothing() {
        Slice<Product> emptySlice = new SliceImpl<>(List.of());
        when(productRepository.findByNextAnniversaryLessThan(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(emptySlice);

        rolloverJob.rollForwardAnniversaries();

        verify(anniversaryProcessor, never()).processChunk(anyList());
    }

    @Test
    void rollForwardAnniversaries_WhenOneChunk_ProcessesAndExits() {
        Slice<Product> singleChunk = new SliceImpl<>(List.of(dummyProduct));
        Slice<Product> emptyChunk = new SliceImpl<>(List.of());

        // First call returns data, second call returns empty to break the loop
        when(productRepository.findByNextAnniversaryLessThan(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(singleChunk)
                .thenReturn(emptyChunk);

        rolloverJob.rollForwardAnniversaries();

        verify(anniversaryProcessor, times(1)).processChunk(singleChunk.getContent());
        verify(productRepository, times(2)).findByNextAnniversaryLessThan(any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    void rollForwardAnniversaries_WhenMultipleChunks_ProcessesInLoop() {
        Slice<Product> chunk1 = new SliceImpl<>(List.of(dummyProduct));
        Slice<Product> chunk2 = new SliceImpl<>(List.of(dummyProduct));
        Slice<Product> emptyChunk = new SliceImpl<>(List.of());

        when(productRepository.findByNextAnniversaryLessThan(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(chunk1)
                .thenReturn(chunk2)
                .thenReturn(emptyChunk);

        rolloverJob.rollForwardAnniversaries();

        verify(anniversaryProcessor, times(2)).processChunk(anyList());
        verify(productRepository, times(3)).findByNextAnniversaryLessThan(any(LocalDate.class), any(PageRequest.class));
    }
}