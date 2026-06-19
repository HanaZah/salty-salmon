package com.finadvise.crm.clients;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BirthdayProcessor {

    private final Clock clock;
    private final ClientRepository clientRepository;

    public LocalDate calculateNextBirthday(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate nextBirthday = birthDate.withYear(today.getYear());

        if (nextBirthday.isBefore(today)) {
            return nextBirthday.plusYears(1);
        }

        return nextBirthday;
    }

    public void processChunk(List<Client> clients) {
        for (Client client : clients) {
            client.setNextBirthday(calculateNextBirthday(client.getBirthDate()));
        }
        clientRepository.saveAll(clients);
    }
}
