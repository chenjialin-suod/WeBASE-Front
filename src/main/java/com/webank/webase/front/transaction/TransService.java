/**
 * Copyright 2014-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.webank.webase.front.transaction;

import com.webank.webase.front.base.code.ConstantCode;
import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.base.properties.Constants;
import com.webank.webase.front.contract.ContractRepository;
import com.webank.webase.front.keystore.KeyStoreService;
import com.webank.webase.front.keystore.entity.EncodeInfo;
import com.webank.webase.front.keystore.entity.RspMessageHashSignature;
import com.webank.webase.front.keystore.entity.RspUserInfo;
import com.webank.webase.front.precntauth.precompiled.cns.CNSServiceInWebase;
import com.webank.webase.front.precntauth.precompiled.sysconf.SysConfigServiceInWebase;
import com.webank.webase.front.transaction.entity.ReqSignMessageHash;
import com.webank.webase.front.transaction.entity.ReqTransHandle;
import com.webank.webase.front.transaction.entity.ReqTransHandleWithSign;
import com.webank.webase.front.util.CommonUtils;
import com.webank.webase.front.util.ContractAbiUtil;
import com.webank.webase.front.util.JsonUtils;
import com.webank.webase.front.web3api.Web3ApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.fisco.bcos.sdk.jni.common.JniException;
import org.fisco.bcos.sdk.jni.utilities.tx.TransactionBuilderJniObj;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.request.Transaction;
import org.fisco.bcos.sdk.v3.client.protocol.response.Call.CallOutput;
import org.fisco.bcos.sdk.v3.codec.ContractCodec;
import org.fisco.bcos.sdk.v3.codec.ContractCodecException;
import org.fisco.bcos.sdk.v3.codec.Utils;
import org.fisco.bcos.sdk.v3.codec.abi.FunctionReturnDecoder;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinition;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinition.NamedType;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinitionFactory;
import org.fisco.bcos.sdk.v3.codec.wrapper.ContractABIDefinition;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.crypto.signature.ECDSASignatureResult;
import org.fisco.bcos.sdk.v3.crypto.signature.SM2SignatureResult;
import org.fisco.bcos.sdk.v3.crypto.signature.SignatureResult;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.RevertMessageParser;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderService;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.transaction.pusher.TransactionPusherService;
import org.fisco.bcos.sdk.v3.utils.Hex;
import org.fisco.bcos.sdk.v3.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * TransService. handle transactions of deploy/call contract
 */
@Slf4j
@Service
public class TransService {

    @Autowired
    private Web3ApiService web3ApiService;
    @Autowired
    private KeyStoreService keyStoreService;
    @Autowired
    private Constants constants;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    @Qualifier(value = "sm")
    private CryptoSuite smCryptoSuite;
    @Autowired
    @Qualifier(value = "ecdsa")
    private CryptoSuite ecdsaCryptoSuite;
    @Autowired
    private SysConfigServiceInWebase sysConfigServiceInWebase;
    @Autowired
    private CNSServiceInWebase cnsServiceInWebase;
    /**
     * if use wasm(liquid), use 2
     */
    private static final int USE_SOLIDITY = 1;
    private static final int USE_WASM = 2;
    private static final int USE_WASM_DEPLOY = 10;

    /**
     * transHandleWithSign.
     *
     * @param req request
     */
    public Object transHandleWithSign(ReqTransHandleWithSign req) throws FrontException {
        String groupId = req.getGroupId();
        String signUserId = req.getSignUserId();
        String userAddress = keyStoreService.getAddressBySignUserId(signUserId);
        if (StringUtils.isBlank(userAddress)) {
            log.warn("transHandleWithSign this signUser [{}] not record in webase-front", signUserId);
            userAddress = keyStoreService.getCredentialsForQuery(groupId).getAddress();
        }
        String abiStr = JsonUtils.objToString(req.getContractAbi());
        String funcName = req.getFuncName();
        List<Object> funcParam = req.getFuncParam() == null ? new ArrayList<>() : req.getFuncParam();
        String contractAddress = req.getContractAddress();
        return this.transHandleWithSign(groupId, signUserId, contractAddress, abiStr, funcName, funcParam, req.getIsWasm());
    }

    public Object transHandleWithSign(String groupId, String signUserId,
                                      String contractAddress, String abiStr, String funcName, List<Object> funcParam) {
        return this.transHandleWithSign(groupId, signUserId, contractAddress, abiStr, funcName, funcParam, false);
    }

    /**
     * send tx with sign (support precomnpiled contract)
     */
    public Object transHandleWithSign(String groupId, String signUserId,
        String contractAddress, String abiStr, String funcName, List<Object> funcParam, boolean isWasm)
        throws FrontException {
        // check groupId
        Client client = web3ApiService.getWeb3j(groupId);

        byte[] encodeFunction = this.encodeFunction2ByteArr(abiStr, funcName, funcParam, groupId, isWasm);
        String userAddress = keyStoreService.getAddressBySignUserId(signUserId);
        if (StringUtils.isBlank(userAddress)) {
            log.warn("transHandleWithSign this signUser [{}] not record in webase-front", signUserId);
            userAddress = keyStoreService.getCredentialsForQuery(groupId).getAddress();
        }

        boolean isTxConstant = this.getABIDefinition(abiStr, funcName, groupId).isConstant();
        if (isTxConstant) {
            return this.handleCall(groupId, userAddress, contractAddress, encodeFunction, abiStr, funcName, isWasm);
        } else {
            return this.handleTransaction(client, signUserId, contractAddress, encodeFunction);
        }

    }

    /**
     * signMessage to create raw transaction and encode data
     *
     * @param groupId id
     * @param contractAddress info
     * @param data info
     * @return
     */
    public String signMessage(String groupId, Client client, String signUserId, String contractAddress,
            byte[] data, boolean isDeploy) {
        log.info("signMessage data:{}", Hex.toHexString(data));
        Instant nodeStartTime = Instant.now();
        // to encode raw tx
        Pair<String, String> chainIdAndGroupId = TransactionProcessorFactory.getChainIdAndGroupId(client);
        long transactionData = 0L;
        String encodedTransaction = "";
        String transactionDataHash = "";
        try {
            transactionData = TransactionBuilderJniObj
                .createTransactionData(groupId, chainIdAndGroupId.getLeft(),
                    contractAddress, Hex.toHexString(data), "", client.getBlockLimit().longValue());
            encodedTransaction = TransactionBuilderJniObj.encodeTransactionData(transactionData);
            // 使用encodeTx的hash，和sign的hash保持一致
            transactionDataHash = client.getCryptoSuite().hash(encodedTransaction);
//            transactionDataHash = TransactionBuilderJniObj.calcTransactionDataHash(client.getCryptoType(), transactionData);
        } catch (JniException e) {
            log.error("createTransactionData jni error ", e);
        }
        if (transactionData == 0L
            || StringUtils.isBlank(encodedTransaction)
            || StringUtils.isBlank(transactionDataHash)) {
            log.error("signMessage encodedTransaction:{}|{}|{}",
                transactionData, encodedTransaction, transactionDataHash);
            throw new FrontException(ConstantCode.ENCODE_TX_JNI_ERROR);
        }
        log.info("transactionDataHash after:{}", transactionDataHash);
        SignatureResult signData = this.requestSignForSign(encodedTransaction, signUserId, groupId);
        int mark = client.isWASM() ? USE_WASM : USE_SOLIDITY;
        if (client.isWASM() && isDeploy) {
            mark = USE_WASM_DEPLOY;
        }
        log.info("mark {}", mark);
        String transactionDataHashSignedData = Hex.toHexString(signData.encode());
        String signedMessage = null;
        try {
            signedMessage = TransactionBuilderJniObj.createSignedTransaction(transactionData,
                transactionDataHashSignedData,
                transactionDataHash, mark);
        } catch (JniException e) {
            log.error("createSignedTransactionData jni error:", e);
        }
        log.info("***signMessage cost time***: {}",
            Duration.between(nodeStartTime, Instant.now()).toMillis());
        if (StringUtils.isBlank(signedMessage)) {
            throw new FrontException(ConstantCode.DATA_SIGN_ERROR);
        }
        return signedMessage;
    }


    /**
     * send message to node.
     *
     * @param signMsg signMsg
     */
    public TransactionReceipt sendMessage(Client client, String signMsg) {
        TransactionPusherService txPusher = new TransactionPusherService(client);
        log.info("sendMessage signMsg:{}", signMsg);
        TransactionReceipt receipt = txPusher.push(signMsg);
        log.info("sendMessage receipt:{}", JsonUtils.objToString(receipt));
        this.decodeReceipt(client, receipt);
        return receipt;
    }

    /**
     * send transaction locally
     */
    public Object transHandleLocal(ReqTransHandle req) {
        String groupId = req.getGroupId();
        String abiStr = JsonUtils.objToString(req.getContractAbi());
        String funcName = req.getFuncName();
        List<Object> funcParam = req.getFuncParam() == null ? new ArrayList<>() : req.getFuncParam();
        String userAddress = req.getUser();
        boolean isWasm = req.getIsWasm();
        String contractAddress = req.getContractAddress();

        byte[] encodeFunction = this.encodeFunction2ByteArr(abiStr, funcName, funcParam, groupId, isWasm);

        boolean isTxConstant = this.getABIDefinition(abiStr, funcName, groupId).isConstant();
        // get privateKey
        CryptoKeyPair cryptoKeyPair = getCredentials(isTxConstant, userAddress, groupId);

        Client client = web3ApiService.getWeb3j(groupId);

        if (isTxConstant) {
            if (StringUtils.isBlank(userAddress)) {
                userAddress = cryptoKeyPair.getAddress();
            }
            return this.handleCall(groupId, userAddress, contractAddress, encodeFunction, abiStr, funcName, isWasm);
        } else {
            return this.handleTransaction(client, cryptoKeyPair, contractAddress, encodeFunction);
        }
    }


    public TransactionReceipt sendSignedTransaction(String signedStr, Boolean sync, String groupId) {

        Client client = web3ApiService.getWeb3j(groupId);
        if (sync) {
            TransactionReceipt receipt = sendMessage(client, signedStr);
            this.decodeReceipt(client, receipt);
            return receipt;
        } else {
            TransactionPusherService txPusher = new TransactionPusherService(client);
            txPusher.pushOnly(signedStr);
            TransactionReceipt transactionReceipt = new TransactionReceipt();
            transactionReceipt.setTransactionHash(web3ApiService.getCryptoSuite(groupId).hash(signedStr));
            return transactionReceipt;
        }
    }


    public Object sendQueryTransaction(byte[] encodeStr, String contractAddress, String funcName,
            List<Object> contractAbi, String groupId, String userAddress) {

        Client client = web3ApiService.getWeb3j(groupId);
        String callOutput = client
            .call(new Transaction(userAddress, contractAddress,
                        encodeStr))
            .getCallResult().getOutput();

        ABIDefinition abiDefinition = getABIDefinition(funcName, JsonUtils.toJSONString(contractAbi), groupId);
        if (Objects.isNull(abiDefinition)) {
            throw new FrontException(ConstantCode.IN_FUNCTION_ERROR);
        }
        List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
        List<TypeReference<?>> finalOutputs = ContractAbiUtil.outputFormat(funOutputTypes);

        FunctionReturnDecoder functionReturnDecoder = new FunctionReturnDecoder();
        List<Type> typeList = functionReturnDecoder.decode(callOutput, Utils.convert(finalOutputs));
        Object response;
        if (typeList.size() > 0) {
            response = ContractAbiUtil.callResultParse(funOutputTypes, typeList);
        } else {
            response = typeList;
        }
        return response;
    }

    public static ABIDefinition getFunctionAbiDefinition(String functionName, String contractAbi) {
        if (functionName == null) {
            throw new FrontException(ConstantCode.IN_FUNCTION_ERROR);
        }
        List<ABIDefinition> abiDefinitionList =
                JsonUtils.toJavaObjectList(contractAbi, ABIDefinition.class);
        if (abiDefinitionList == null) {
            throw new FrontException(ConstantCode.FAIL_PARSE_JSON);
        }
        ABIDefinition result = null;
        for (ABIDefinition abiDefinition : abiDefinitionList) {
            if (abiDefinition == null) {
                throw new FrontException(ConstantCode.IN_FUNCTION_ERROR);
            }
            if (Constants.TYPE_FUNCTION.equals(abiDefinition.getType())
                    && functionName.equals(abiDefinition.getName())) {
                result = abiDefinition;
                break;
            }
        }
        return result;
    }

    /**
     * get CryptoKeyPair by keyUser locally
     */
    private CryptoKeyPair getCredentials(boolean constant, String keyUser, String groupId) {
        // get privateKey
        CryptoKeyPair credentials;
        if (constant) {
            credentials = keyStoreService.getCredentialsForQuery(groupId);
        } else {
            credentials = keyStoreService.getCredentials(keyUser, groupId);
        }
        return credentials;
    }

    public SignatureResult signMessageHashByType(String messageHash, CryptoKeyPair cryptoKeyPair, int encryptType) {
        try {
            if (encryptType == CryptoType.SM_TYPE) {
                return smCryptoSuite.sign(messageHash, cryptoKeyPair);
            } else {
                return ecdsaCryptoSuite.sign(messageHash, cryptoKeyPair);
            }
        } catch (Exception e) {
            log.error("signMessageHashByType failed:[]", e);
            throw new FrontException(ConstantCode.GET_MESSAGE_HASH, e.getMessage());
        }
    }

    /**
     * signMessageLocal
     * @return SignatureResult
     */
    public Object signMessageLocal(ReqSignMessageHash req) {
        log.info("transHandle start. ReqSignMessageHash:[{}]", JsonUtils.toJSONString(req));
        String groupId = req.getGroupId();
        CryptoKeyPair cryptoKeyPair = this.getCredentials(false, req.getUser(), groupId);

        SignatureResult signResult = signMessageHashByType(
                Numeric.cleanHexPrefix(req.getHash()), cryptoKeyPair,
                web3ApiService.getCryptoSuite(groupId).cryptoTypeConfig);
        if (web3ApiService.getCryptoSuite(groupId).cryptoTypeConfig == CryptoType.SM_TYPE) {
            SM2SignatureResult sm2SignatureResult = (SM2SignatureResult) signResult;
            RspMessageHashSignature rspMessageHashSignature = new RspMessageHashSignature();
            rspMessageHashSignature.setP(Hex.toHexString(sm2SignatureResult.getPub()));
            rspMessageHashSignature.setR(Hex.toHexString(sm2SignatureResult.getR()));
            rspMessageHashSignature.setS(Hex.toHexString(sm2SignatureResult.getS()));
            rspMessageHashSignature.setV((byte) 0);
            return rspMessageHashSignature;
        } else {
            ECDSASignatureResult ecdsaSignatureResult = (ECDSASignatureResult) signResult;
            RspMessageHashSignature rspMessageHashSignature = new RspMessageHashSignature();
            rspMessageHashSignature.setP("0x");
            rspMessageHashSignature.setR(Hex.toHexString(ecdsaSignatureResult.getR()));
            rspMessageHashSignature.setS(Hex.toHexString(ecdsaSignatureResult.getS()));
            rspMessageHashSignature.setV((byte) (ecdsaSignatureResult.getV()+27));
            return rspMessageHashSignature;
        }
    }



    /**
     * signMessageLocalExternal
     */
    public Object signMessageLocalExternal(ReqSignMessageHash req) {
        log.info("transHandle start. ReqSignMessageHash:[{}]", JsonUtils.toJSONString(req));
        String groupId = req.getGroupId();
        RspUserInfo rspUserInfo = keyStoreService.getUserInfoWithSign(req.getSignUserId(),true);
        String privateKeyRaw = new String(Base64.getDecoder().decode(rspUserInfo.getPrivateKey()));
        CryptoKeyPair cryptoKeyPair = web3ApiService.getCryptoSuite(groupId).getKeyPairFactory().createKeyPair(privateKeyRaw);
        SignatureResult signResult = signMessageHashByType(
                Numeric.cleanHexPrefix(req.getHash()),cryptoKeyPair,
                web3ApiService.getCryptoSuite(groupId).cryptoTypeConfig
        );

        if (web3ApiService.getCryptoSuite(groupId).cryptoTypeConfig == CryptoType.SM_TYPE) {
            SM2SignatureResult sm2SignatureResult = (SM2SignatureResult) signResult;
            RspMessageHashSignature rspMessageHashSignature = new RspMessageHashSignature();
            rspMessageHashSignature.setP(Hex.toHexString(sm2SignatureResult.getPub()));
            rspMessageHashSignature.setR(Hex.toHexString(sm2SignatureResult.getR()));
            rspMessageHashSignature.setS(Hex.toHexString(sm2SignatureResult.getS()));
            rspMessageHashSignature.setV((byte) 0);
            return rspMessageHashSignature;
        } else {
            ECDSASignatureResult ecdsaSignatureResult = (ECDSASignatureResult) signResult;
            RspMessageHashSignature rspMessageHashSignature = new RspMessageHashSignature();
            rspMessageHashSignature.setP("0x");
            rspMessageHashSignature.setR(Hex.toHexString(ecdsaSignatureResult.getR()));
            rspMessageHashSignature.setS(Hex.toHexString(ecdsaSignatureResult.getS()));
            rspMessageHashSignature.setV((byte) (ecdsaSignatureResult.getV()+27));
            return rspMessageHashSignature;
        }
    }

    /**
     * get encoded raw transaction
     * @param user  if user not blank, return signed raw tx, else return encoded tx(not sign)
     * @param isLocal  if false, user is signUserId, else, user is userAddress local
     */
    public String createRawTxEncoded(boolean isLocal, String user,
        String groupId, String contractAddress, List<Object> contractAbi,
        boolean isUseCns, String cnsName, String cnsVersion,
        String funcName, List<Object> funcParam, boolean isWasm) throws Exception {

        if (isUseCns) {
            Tuple2<String, String> cnsInfo = cnsServiceInWebase.queryCnsByNameAndVersion(groupId, cnsName, cnsVersion);
            contractAddress = cnsInfo.getValue1();
            log.info("transHandleWithSign cns contractAddress:{}", contractAddress);
        }
        // encode function
        byte[] encodeFunction = this.encodeFunction2ByteArr(JsonUtils.objToString(contractAbi), funcName, funcParam, groupId, isWasm);
        // check groupId
        Client client = web3ApiService.getWeb3j(groupId);
        // isLocal:
        // true: user is userAddress locally
        // false: user is signUserId in webase-sign
        return this.convertRawTx2Str(client, contractAddress, encodeFunction, user, isLocal);
    }


    public String encodeFunction2Str(String abiStr, String funcName, List<Object> funcParam, String groupId, boolean isWasm) {
        byte[] encodeFunctionByteArr = this.encodeFunction2ByteArr(abiStr, funcName, funcParam, groupId, isWasm);
        return Hex.toHexString(encodeFunctionByteArr);
    }
    /**
     * get encoded function for /trans/query-transaction
     * @param abiStr
     * @param funcName
     * @param funcParam
     * @return
     */
    public byte[] encodeFunction2ByteArr(String abiStr, String funcName, List<Object> funcParam, String groupId,
                                         boolean isWasm) {

        funcParam = funcParam == null ? new ArrayList<>() : funcParam;
        this.validFuncParam(abiStr, funcName, funcParam, groupId);
        log.debug("abiStr:{} ,funcName:{},funcParam {},groupID {}", abiStr, funcName,
           funcParam, groupId);
        ContractCodec abiCodec = new ContractCodec(web3ApiService.getCryptoSuite(groupId), isWasm);
        byte[] encodeFunction;
        try {
            encodeFunction = abiCodec.encodeMethod(abiStr, funcName, funcParam);
        } catch (ContractCodecException e) {
            log.error("transHandleWithSign encode fail:[]", e);
            throw new FrontException(ConstantCode.CONTRACT_TYPE_ENCODED_ERROR);
        }
        log.debug("encodeFunction2Str encodeFunction:{}", encodeFunction);
        return encodeFunction;
    }

    /**
     * get encoded raw transaction
     * handleTransByFunction by whether is constant
     * if use signed data to send tx, call @send-signed-transaction api
     * @case1 if @userAddress is blank, return not signed raw tx encoded str
     * @case2 if @userAddress not blank, return signed str
     * todo support deploy
     * int mark = client.isWASM() ? USE_WASM : USE_SOLIDITY;
     *         if (client.isWASM() && isDeploy) {
     *             mark = USE_WASM_DEPLOY;
     *         }
     */
    private String convertRawTx2Str(Client client, String contractAddress,
        byte[] data, String user, boolean isLocal) {
        Instant startTime = Instant.now();
        String groupId = client.getGroup();
        // to encode raw tx
        Pair<String, String> chainIdAndGroupId = TransactionProcessorFactory.getChainIdAndGroupId(client);
        long transactionData = 0L;
        String encodedTransaction = "";
        String transactionDataHash = "";
        try {
            transactionData = TransactionBuilderJniObj
                .createTransactionData(String.valueOf(groupId), chainIdAndGroupId.getLeft(),
                    contractAddress, Hex.toHexString(data), "", client.getBlockLimit().longValue());
            encodedTransaction = TransactionBuilderJniObj.encodeTransactionData(transactionData);
            transactionDataHash = client.getCryptoSuite().hash(encodedTransaction);
//            transactionDataHash = TransactionBuilderJniObj.calcTransactionDataHash(client.getCryptoType(), transactionData);
        } catch (JniException e) {
            log.error("createTransactionData jni error ", e);
        }
        if (transactionData == 0L
            || StringUtils.isBlank(encodedTransaction)
            || StringUtils.isBlank(transactionDataHash)) {
            log.error("signMessage encodedTransaction:{}|{}|{}",
                transactionData, encodedTransaction, transactionDataHash);
            throw new FrontException(ConstantCode.ENCODE_TX_JNI_ERROR);
        }

        // if user not null: sign, else, not sign
        if (StringUtils.isBlank(user)) {
            // return unsigned raw tx encoded str
            String unsignedResultStr = encodedTransaction;
            log.info("createRawTxEncoded unsignedResultStr:{}", unsignedResultStr);
            return unsignedResultStr;
        } else {
            log.info("createRawTxEncoded use key of address [{}] to sign", user);
            // hash encoded, to sign locally
            String hashMessageStr = client.getCryptoSuite().hash(encodedTransaction);
            log.info("createRawTxEncoded encoded tx of hex str:{}", hashMessageStr);
            // if local, sign locally
            log.info("createRawTxEncoded isLocal:{}", isLocal);
            String signResultStr = null;
            if (isLocal) {
                CryptoKeyPair cryptoKeyPair = this.getCredentials(false, user, groupId);
                SignatureResult signData = signMessageHashByType(hashMessageStr,
                    cryptoKeyPair, client.getCryptoSuite().cryptoTypeConfig);
                // encode again
                String transactionDataHashSignedData = signData.convertToString();
                try {
                    signResultStr = TransactionBuilderJniObj.createSignedTransaction(transactionData,
                        transactionDataHashSignedData,
                        transactionDataHash, client.isWASM() ? USE_WASM : USE_SOLIDITY);
                } catch (JniException e) {
                    log.error("createSignedTransactionData jni error:", e);
                }
            } else {
                // sign by webase-sign
                // convert encoded to hex string (no need to hash then toHex)
                EncodeInfo encodeInfo = new EncodeInfo(user, encodedTransaction);
                String signDataStr = keyStoreService.getSignData(encodeInfo);
                SignatureResult signData = CommonUtils.stringToSignatureData(signDataStr, client.getCryptoSuite().cryptoTypeConfig);
                // encode again
                String transactionDataHashSignedData = signData.convertToString();
                try {
                    signResultStr = TransactionBuilderJniObj.createSignedTransaction(transactionData,
                        transactionDataHashSignedData,
                        transactionDataHash, client.isWASM() ? USE_WASM : USE_SOLIDITY);
                } catch (JniException e) {
                    log.error("createSignedTransactionData jni error:", e);
                }
            }
            log.info("***signMessage cost time***: {}",
                Duration.between(startTime, Instant.now()).toMillis());
            if (StringUtils.isBlank(signResultStr)) {
                throw new FrontException(ConstantCode.ENCODE_TX_JNI_ERROR);
            }
            log.info("createRawTxEncoded signResultStr:{}", signResultStr);
            return signResultStr;
        }
    }

    private ABIDefinition getABIDefinition(String abiStr, String functionName, String groupId) {
        ABIDefinitionFactory factory = new ABIDefinitionFactory(web3ApiService.getCryptoSuite(groupId));

        ContractABIDefinition contractABIDefinition = factory.loadABI(abiStr);
        List<ABIDefinition> abiDefinitionList = contractABIDefinition.getFunctions()
            .get(functionName);
        if (abiDefinitionList.isEmpty()) {
            throw new FrontException(ConstantCode.IN_FUNCTION_ERROR);
        }
        // abi only contain one function, so get first one
        ABIDefinition function = abiDefinitionList.get(0);
        return function;
    }

    public List<Type> handleCall(String groupId, String userAddress, String contractAddress,
        byte[] encodedFunction, String abiStr, String funcName, boolean isWasm) {

        Client client = web3ApiService.getWeb3j(groupId);
        Pair<String, String> chainIdAndGroupId = TransactionProcessorFactory.getChainIdAndGroupId(client);
        TransactionProcessor transactionProcessor = new TransactionProcessor(client,
            keyStoreService.getCredentialsForQuery(groupId),
            String.valueOf(groupId), chainIdAndGroupId.getLeft());
        CallOutput callOutput = transactionProcessor
            .executeCall(userAddress, contractAddress, encodedFunction)
            .getCallResult();
        // if error
        if (callOutput.getStatus() != 0) {
            Tuple2<Boolean, String> parseResult =
                RevertMessageParser.tryResolveRevertMessage(callOutput.getStatus(), callOutput.getOutput());
            log.error("call contract error:{}", parseResult);
            String parseResultStr = parseResult.getValue1() ? parseResult.getValue2() : "call contract error of status" + callOutput.getStatus();
            throw new FrontException(ConstantCode.CALL_CONTRACT_ERROR.getCode(), parseResultStr);
        } else {
            ContractCodec abiCodec = new ContractCodec(web3ApiService.getCryptoSuite(groupId), client.isWASM());
            try {
                log.debug("========= callOutput.getOutput():{}", callOutput.getOutput());
                //  [
                //  {
                //    "value": "Hello, World!",
                //    "typeAsString": "string"
                //  }
                //]
                List<Type> typeList = abiCodec.decodeMethodAndGetOutputObject(abiStr, funcName, callOutput.getOutput());
                // bytes类型转十六进制
                // todo output is byte[] or string  Hex.decode
                log.info("call contract res:{}", JsonUtils.objToString(typeList));
                return typeList;
            } catch (ContractCodecException e) {
                log.error("handleCall decode call output fail:[]", e);
                throw new FrontException(ConstantCode.CONTRACT_TYPE_DECODED_ERROR);
            }
        }
    }

    /**
     * handle tx locally
     * @param client
     * @param cryptoKeyPair
     * @param contractAddress
     * @param encodeFunction
     * @return
     */
    public TransactionReceipt handleTransaction(Client client, CryptoKeyPair cryptoKeyPair, String contractAddress,
        byte[] encodeFunction) {
        Instant startTime = Instant.now();
        log.info("handleTransaction start startTime:{}", startTime.toEpochMilli());
        // send tx
        TransactionProcessor txProcessor = TransactionProcessorFactory.createTransactionProcessor(client, cryptoKeyPair);
        TransactionReceipt receipt = txProcessor.sendTransactionAndGetReceipt(contractAddress, encodeFunction, cryptoKeyPair, client.isWASM() ? USE_WASM : USE_SOLIDITY);
        // cover null message through statusCode
        this.decodeReceipt(client, receipt);
        log.info("execTransaction end useTime:{},receipt:{}",
            Duration.between(startTime, Instant.now()).toMillis(), receipt);
        return receipt;
    }


    /**
     * handle tx with sign
     * @param client
     * @param signUserId
     * @param contractAddress
     * @param encodeFunction
     * @return
     */
    public TransactionReceipt handleTransaction(Client client, String signUserId, String contractAddress, byte[] encodeFunction) {
        log.debug("handleTransaction signUserId:{},contractAddress:{},encodeFunction:{}",signUserId,contractAddress, encodeFunction);
        String groupId = client.getGroup();
        String signedMessageStr = this.signMessage(groupId, client, signUserId, contractAddress, encodeFunction, false);

        Instant nodeStartTime = Instant.now();
        // send transaction
        TransactionReceipt receipt = sendMessage(client, signedMessageStr);
        this.decodeReceipt(client, receipt);
        log.info("***node cost time***: {}",
            Duration.between(nodeStartTime, Instant.now()).toMillis());
        return receipt;

    }

    /**
     * sign by
     * @param encodedDataStr
     * @param signUserId
     * @return
     */
    public SignatureResult requestSignForSign(String encodedDataStr, String signUserId, String groupId) {
        EncodeInfo encodeInfo = new EncodeInfo();
        encodeInfo.setSignUserId(signUserId);
        encodeInfo.setEncodedDataStr(encodedDataStr);

        Instant startTime = Instant.now();
        String signDataStr = keyStoreService.getSignData(encodeInfo);
        log.info("get requestSignForSign cost time: {}",
            Duration.between(startTime, Instant.now()).toMillis());
        SignatureResult signData = CommonUtils.stringToSignatureData(signDataStr,
            web3ApiService.getCryptoSuite(groupId).cryptoTypeConfig);
        return signData;
    }

    /**
     * sdk仅用于了预编译合约，其余解析需要自行完成
     * @param client
     * @param receipt
     */
    public void decodeReceipt(Client client, TransactionReceipt receipt) {
        // decode receipt
        TransactionDecoderService txDecoder = new TransactionDecoderService(client.getCryptoSuite(), client.isWASM());
        String receiptMsg = txDecoder.decodeReceiptStatus(receipt).getReceiptMessages();
        receipt.setMessage(receiptMsg);
    }

    /**
     * check input
     * @param contractAbiStr
     * @param funcName
     * @param funcParam
     * @param groupId
     */
    private void validFuncParam(String contractAbiStr, String funcName, List<Object> funcParam, String groupId) {
        ABIDefinition abiDefinition = this.getABIDefinition(contractAbiStr, funcName, groupId);
        List<NamedType> inputTypeList = abiDefinition.getInputs();
        if (inputTypeList.size() != funcParam.size()) {
            log.error("validFuncParam param not match");
            throw new FrontException(ConstantCode.FUNC_PARAM_SIZE_NOT_MATCH);
        }
        for (int i = 0; i < inputTypeList.size(); i++) {
            String type = inputTypeList.get(i).getType();
            if (type.startsWith("bytes")) {
                if (type.contains("[][]")) {
                    // todo bytes[][]
                    log.warn("validFuncParam param, not support bytes 2d array or more");
//                    throw new FrontException(ConstantCode.FUNC_PARAM_BYTES_NOT_SUPPORT_HIGH_D);
                    return;
                }
                // if not bytes[], bytes or bytesN
                if (!type.endsWith("[]")) {
                    // update funcParam
                    String bytesHexStr = (String) (funcParam.get(i));
                    byte[] inputArray = Hex.decode(bytesHexStr);
                    // bytesN: bytes1, bytes32 etc.
                    if (type.length() > "bytes".length()) {
                        int bytesNLength = Integer.parseInt(type.substring("bytes".length()));
                        if (inputArray.length != bytesNLength) {
                            log.error("validFuncParam param of bytesN size not match");
                            throw new FrontException(ConstantCode.FUNC_PARAM_BYTES_SIZE_NOT_MATCH);
                        }
                    }
                    // replace hexString with array
                    funcParam.set(i, inputArray);
                } else {
                    // if bytes[] or bytes32[]
                    List<String> hexStrArray = (List<String>) (funcParam.get(i));
                    List<byte[]> bytesArray = new ArrayList<>(hexStrArray.size());
                    for (int j = 0; j < hexStrArray.size(); j++) {
                        String bytesHexStr = hexStrArray.get(j);
                        byte[] inputArray = Hex.decode(bytesHexStr);
                        // check: bytesN: bytes1, bytes32 etc.
                        if (type.length() > "bytes[]".length()) {
                            // bytes32[] => 32[]
                            String temp = type.substring("bytes".length());
                            // 32[] => 32
                            int bytesNLength = Integer
                                .parseInt(temp.substring(0, temp.length() - 2));
                            if (inputArray.length != bytesNLength) {
                                log.error("validFuncParam param of bytesN size not match");
                                throw new FrontException(
                                    ConstantCode.FUNC_PARAM_BYTES_SIZE_NOT_MATCH);
                            }
                        }
                        bytesArray.add(inputArray);
                    }
                    // replace hexString with array
                    funcParam.set(i, bytesArray);
                }
            }
        }
    }


}


