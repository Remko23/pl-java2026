package com.truthlens.search;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SearchServiceApplicationTest {

    @Test
    void main() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            SearchServiceApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(SearchServiceApplication.class, new String[]{}));
        }
    }
}
