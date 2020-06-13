package com.averygrimes.secretschest;

import com.averygrimes.secretschest.interaction.AWSClient;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

@ActiveProfiles("test")
public class BaseTestConfig {

    @MockBean
    private AWSClient awsClient;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }
}
