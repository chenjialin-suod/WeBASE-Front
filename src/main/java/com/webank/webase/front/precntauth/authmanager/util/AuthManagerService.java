package com.webank.webase.front.precntauth.authmanager.util;

import com.webank.webase.front.keystore.KeyStoreService;
import com.webank.webase.front.web3api.Web3ApiService;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.contract.auth.manager.AuthManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * init one instance of AuthManager to call auth manager contract (only call)
 */
@Slf4j
@Service
public class AuthManagerService {

  @Autowired
  private Web3ApiService web3ApiService;
  @Autowired
  private KeyStoreService keyStoreService;

  private static Map<String, AuthManager> authManagerMap = new ConcurrentHashMap<>();

  public AuthManager getAuthManagerService(String groupId) throws ContractException {
    Instant startTime = Instant.now();
    if (!authManagerMap.containsKey(groupId) || authManagerMap.get(groupId) == null) {
      AuthManager authManager = new AuthManager(web3ApiService.getWeb3j(groupId),
          keyStoreService.getCredentialsForQuery(groupId));
      authManagerMap.put(groupId, authManager);
      log.info("getAuthManagerService groupId:{},{}", groupId,
          Duration.between(startTime, Instant.now()).toMillis());
    }
    log.info("getAuthManagerService groupId:{},{}", groupId,
        Duration.between(startTime, Instant.now()).toMillis());
    return authManagerMap.get(groupId);
  }


}
