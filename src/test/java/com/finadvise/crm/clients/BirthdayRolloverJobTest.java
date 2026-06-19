package com.finadvise.crm.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayRolloverJobTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BirthdayProcessor birthdayProcessor;

    @Mock
    private Clock clock;

    @InjectMocks
    private BirthdayRolloverJob rolloverJob;

    private Client dummyClient;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-26T10:00:00Z"), ZoneId.of("UTC"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());

        today = LocalDate.now(fixedClock);

        dummyClient = Client.builder()
                .id(1L)
                .firstName("Test")
                .lastName("Client")
                .birthDate(today.minusYears(30))
                .nextBirthday(today.minusDays(1))
                .isActive(true)
                .build();
    }

    @Test
    void rollForwardBirthdays_WhenNoClients_DoesNothing() {
        Slice<Client> emptySlice = new SliceImpl<>(List.of());
        when(clientRepository.findByNextBirthdayLessThanAndIsActiveTrue(eq(today), any(PageRequest.class)))
                .thenReturn(emptySlice);

        rolloverJob.rollForwardBirthdays();

        verify(birthdayProcessor, never()).processChunk(anyList());
    }

    @Test
    void rollForwardBirthdays_WhenOneChunk_ProcessesAndExits() {
        Slice<Client> singleChunk = new SliceImpl<>(List.of(dummyClient));
        Slice<Client> emptyChunk = new SliceImpl<>(List.of());

        when(clientRepository.findByNextBirthdayLessThanAndIsActiveTrue(eq(today), any(PageRequest.class)))
                .thenReturn(singleChunk)
                .thenReturn(emptyChunk);

        rolloverJob.rollForwardBirthdays();

        verify(birthdayProcessor, times(1)).processChunk(singleChunk.getContent());
        verify(clientRepository, times(2)).findByNextBirthdayLessThanAndIsActiveTrue(eq(today), any(PageRequest.class));
    }

    @Test
    void rollForwardBirthdays_WhenMultipleChunks_ProcessesInLoop() {
        Slice<Client> chunk1 = new SliceImpl<>(List.of(dummyClient));
        Slice<Client> chunk2 = new SliceImpl<>(List.of(dummyClient));
        Slice<Client> emptyChunk = new SliceImpl<>(List.of());

        when(clientRepository.findByNextBirthdayLessThanAndIsActiveTrue(eq(today), any(PageRequest.class)))
                .thenReturn(chunk1)
                .thenReturn(chunk2)
                .thenReturn(emptyChunk);

        rolloverJob.rollForwardBirthdays();

        verify(birthdayProcessor, times(2)).processChunk(anyList());
        verify(clientRepository, times(3)).findByNextBirthdayLessThanAndIsActiveTrue(eq(today), any(PageRequest.class));
    }
}
