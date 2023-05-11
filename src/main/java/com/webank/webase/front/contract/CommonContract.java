/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.front.contract;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

import java.util.List;


/**
 * Contract's common functions
 * only solidity, not support liquid yet
 */
public final class CommonContract extends Contract {

    protected CommonContract(String contractAddress, Client web3j,
        CryptoKeyPair credentials) {
        //todo check no binary whether matters
        super("", contractAddress, web3j, credentials);
    }

    public TransactionReceipt execTransaction(Function function) {
        return executeTransaction(function);
    }

    @SuppressWarnings("rawtypes")
    public List<Type> execCall(Function function) throws ContractException {
        return executeCallWithMultipleValueReturn(function);
    }

    public static CommonContract deploy(Client web3j, CryptoKeyPair credentials,
            String contractBin, byte[] encodedConstructor) throws ContractException {
        return deploy(CommonContract.class, web3j, credentials,
            contractBin, "", encodedConstructor,"");
    }

    public static CommonContract load(String contractAddress, Client web3j, CryptoKeyPair credentials) {
        return new CommonContract(contractAddress, web3j, credentials);
    }

}
