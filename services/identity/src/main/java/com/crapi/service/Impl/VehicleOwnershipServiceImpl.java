/*
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crapi.service.Impl;

import com.crapi.model.VehicleOwnership;
import com.crapi.repository.*;
import com.crapi.service.UserService;
import com.crapi.service.VehicleOwnershipService;
import com.crapi.utils.SMTPMailServer;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class VehicleOwnershipServiceImpl implements VehicleOwnershipService {

  private static final Logger logger = LoggerFactory.getLogger(VehicleOwnershipServiceImpl.class);

  @Autowired VehicleModelRepository vehicleModelRepository;

  @Autowired VehicleLocationRepository vehicleLocationRepository;

  @Autowired VehicleDetailsRepository vehicleDetailsRepository;

  @Autowired UserDetailsRepository userDetailsRepository;

  @Autowired UserService userService;

  @Autowired SMTPMailServer smtpMailServer;

  @Value("${api.egress.url}")
  private String apiEgressURL;

  @Value("${api.egress.username}")
  private String apiEgressUsername;

  @Value("${api.egress.password}")
  private String apiEgressPassword;

  public RestTemplate restTemplate()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    RestTemplateBuilder builder = new RestTemplateBuilder();
    TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
    SSLContext sslContext =
        org.apache.http.ssl.SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build();
    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
    builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient));

    // Add basic auth header
    builder.basicAuthentication(apiEgressUsername, apiEgressPassword);
    RestTemplate restTemplate = builder.build();
    return restTemplate;
  }

  /**
   * @param vin
   * @return List<VehicleOwnership>
   */
  @Override
  public List<VehicleOwnership> getPreviousOwners(String vin) {
    try {
      logger.info("Getting vehicle ownership details for vin: " + vin);
      // get vehicle ownership from crapi. vin query param is required
      RestTemplate restTemplate = restTemplate();
      String ownershipUrl = apiEgressURL + "/vin/ownership?vin=" + vin;
      VehicleOwnership[] vehicleOwnerships =
          restTemplate.getForObject(ownershipUrl, VehicleOwnership[].class);
      if (vehicleOwnerships == null) {
        logger.error("Fail to get vehicle ownerships");
        return List.of();
      }
      return Arrays.asList(vehicleOwnerships);
    } catch (Exception e) {
      logger.error("Fail to get vehicle ownerships -> Message: {}", e);
    }
    return List.of();
  }
}
