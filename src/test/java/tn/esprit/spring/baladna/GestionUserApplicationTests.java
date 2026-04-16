package tn.esprit.spring.baladna;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = GestionUserApplication.class)
@ActiveProfiles("test")
class GestionUserApplicationTests {

    @Test
    void contextLoads() {
    }

}
