package com.example.project.aloy.bootstrap;

import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.repository.ApartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final ApartmentRepository apartmentRepository;
    private final Random random = new Random();

    public DataInitializer(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<Apartment> apartments = apartmentRepository.findAll();
        int updated = 0;
        for (Apartment a : apartments) {
            if (a.getMonthlyRate() == null || BigDecimal.ZERO.compareTo(a.getMonthlyRate()) == 0) {
                // generate a random rent between 800 and 4500
                int value = 800 + random.nextInt(3701); // 800..4500
                a.setMonthlyRate(BigDecimal.valueOf(value));
                apartmentRepository.save(a);
                updated++;
            }
        }
        if (updated > 0) {
            logger.info("DataInitializer updated {} apartments with random monthly rates", updated);
        } else {
            logger.info("DataInitializer found no apartments needing monthly rate updates");
        }
    }
}
