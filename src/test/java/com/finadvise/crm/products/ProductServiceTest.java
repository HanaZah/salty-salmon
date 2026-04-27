package com.finadvise.crm.products;
import com.finadvise.crm.clients.Client;
import com.finadvise.crm.clients.ClientRepository;
import com.finadvise.crm.common.OwnershipValidator;
import com.finadvise.crm.users.AdvisorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductTypeRepository productTypeRepository;
    @Mock private ProviderRepository providerRepository;
    @Mock private ProductMapper productMapper;
    @Mock private OwnershipValidator ownershipValidator;
    @Mock private ClientRepository clientRepository;
    @Mock private AdvisorRepository advisorRepository;
    @Mock private Clock clock;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private static final String CLIENT_UID = "UID12345";
    private static final String EMPLOYEE_ID = "EMP999";

    @BeforeEach
    void setupClock() {
        // We freeze time. "Today" is explicitly April 26, 2026.
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-26T10:00:00Z"), ZoneId.of("UTC"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    private void setupHappyPathMocks() {
        when(ownershipValidator.canAccessClient(anyString(), anyString())).thenReturn(true);
        when(clientRepository.findIdByClientUid(anyString())).thenReturn(Optional.of(1L));
        when(productTypeRepository.findById(any())).thenReturn(Optional.of(new ProductType()));
        when(providerRepository.findById(any())).thenReturn(Optional.of(new Provider()));
        when(clientRepository.getReferenceById(any())).thenReturn(mock(Client.class));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({
            // Start Date, Expected Next Anniversary (Assuming "Today" is 2026-04-26)
            "2020-01-15, 2027-01-15", // Past date, anniversary already passed this year -> roll to next year
            "2020-04-26, 2027-04-26", // Past date, anniversary is EXACTLY today -> roll to next year
            "2020-08-10, 2026-08-10", // Past date, anniversary is coming up later this year -> keep this year
            "2026-04-26, 2027-04-26", // Starts today -> first anniversary is exactly next year
            "2026-04-27, 2027-04-27", // Starts tomorrow -> first anniversary is exactly next year
            "2027-10-01, 2028-10-01", // Future date -> first anniversary is start date + 1 year
            "2020-02-29, 2027-02-28"  // Leap year start date. (2027 isn't a leap year, Java handles fallback to 28th)
    })

    void shouldCalculateNextAnniversaryCorrectly(LocalDate startDate, LocalDate expectedAnniversary) {
        setupHappyPathMocks();
        ProductDTO payload = new ProductDTO(
                null, "Test Product", new BigDecimal("1000.00"),
                startDate, null,
                1L, null, 1L, null, null
        );

        productService.createProduct(CLIENT_UID, payload, EMPLOYEE_ID);

        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getNextAnniversary())
                .as("Anniversary calculation failed for start date: " + startDate)
                .isEqualTo(expectedAnniversary);
    }
}