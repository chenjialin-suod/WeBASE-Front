/*
 * Copyright 2014-2020  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.webank.webase.front.base.properties;

import java.math.BigInteger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Contract Constant linked with constants in application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = Constants.CONSTANT_PREFIX)
public class Constants {
    public static final BigInteger GAS_PRICE = new BigInteger("1");
    public static final BigInteger GAS_LIMIT = new BigInteger("100000000");
    public static final BigInteger INITIAL_WEI_VALUE = new BigInteger("0");

    public static final String CNS_CONTRAC_TNAME = "ContractAbiMgr";
    public static final String CNS_FUNCTION_ADDABI = "addAbi";
    public static final String CNS_FUNCTION_GETABI = "getAbi";

    public static final String SEP = "_";
    public static final String DIAGONAL = "/";
    public static final String SYMPOL = ":";
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_FUNCTION = "function";
    public static final String TYPE_CONSTRUCTOR = "constructor";
    public static final String NODE_CONNECTION = "node@%s:%s";
    public static final String CONFIG_FILE = "/config.ini";
    public static final String TOOL = "/tool/";
    public static final String OUTPUT = "output/";
    public static final String ABI_DIR = "./conf/files/abi";
    public static final String BIN_DIR = "./conf/files/bin";
    public static final String JAVA_DIR = "./conf/files/java";
    public static final String TEMPLATE = "./conf/template";
    public static final String FILE_SOL = ".sol";
    public static final String FILE_ADDRESS = ".address";
    public static final String MGR_PRIVATE_KEY_URI =
            "http://%s/WeBASE-Node-Manager/user/privateKey/%s";
    public static final String WEBASE_SIGN_URI = "http://%s/WeBASE-Sign/sign";
    public static final String WEBASE_SIGN_HASH_URI = "http://%s/WeBASE-Sign/sign/hash";
    public static final String WEBASE_SIGN_USER_URI =
            "http://%s/WeBASE-Sign/user/newUser?encryptType=%s&signUserId=%s&appId=%s&returnPrivateKey=%s";
    public static final String WEBASE_SIGN_USER_INFO_URI =
            "http://%s/WeBASE-Sign/user/%s/userInfo?returnPrivateKey=%s";
    public static final String WEBASE_SIGN_VERSION_URI =
        "http://%s/WeBASE-Sign/version";
    public static final String ACCOUNT1_PATH = "node.key";
    public static final String CONSTANT_PREFIX = "constant";
    public static final String SOLC_DIR_PATH_CONFIG = "solcjs";
    public static final String SOLC_JS_SUFFIX = ".js";
    public static final String RECEIPT_STATUS_0X0 = "0x0";

    private String keyServer = "127.0.0.1:8080";
    private int transMaxWait = 30;
    private String monitorDisk = "/";
    private boolean monitorEnabled = true;
    private String aesKey = "EfdsW23D23d3df43";
    private Integer eventRegisterTaskFixedDelay = 5000;
    private Integer syncEventMapTaskFixedDelay = 60000;
    private boolean statLogEnabled = false;
    private Integer syncStatLogTime = 5000;
    private long syncStatLogCountLimit = 10000;

    // rest factory
    private int http_read_timeOut = 10000;
    private int http_connect_timeOut = 10000;
    // rest todo check
    private int restTemplateMaxTotal = 1000;
    private int restTemplateMaxPerRoute = 100;
    // tomcat todo check
    private int keepAliveRequests = 100;
    private int KeepAliveTimeout = 10;

    // second
    private Integer eventCallbackWait = 10;
    private Integer liquidCompileTimeout = 50000;
    private Integer commandLineTimeout = 50000;

}
