package uk.gov.hmcts.cpp.audit.bff.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureCredentialConfig {

    /**
     * Creates a DefaultAzureCredential bean for Azure authentication.
     * Uses the default credential chain to authenticate with Azure services.
     *
     * @return configured DefaultAzureCredential instance
     */
    @Bean
    public DefaultAzureCredential defaultAzureCredential() {
        return new DefaultAzureCredentialBuilder()
            .build();
    }
}
