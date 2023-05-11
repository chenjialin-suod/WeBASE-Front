/**
 * Copyright 2014-2020 the original author or authors.
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
package com.webank.webase.front.contract;

import com.webank.webase.front.base.code.ConstantCode;
import com.webank.webase.front.base.controller.BaseController;
import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.base.response.BasePageResponse;
import com.webank.webase.front.base.response.BaseResponse;
import com.webank.webase.front.contract.entity.*;
import com.webank.webase.front.contract.entity.wasm.CompileTask;
import com.webank.webase.front.contract.entity.wasm.ReqCompileTask;
import com.webank.webase.front.util.FrontUtils;
import com.webank.webase.front.util.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;


/**
 * ContractController.
 */
@Api(value = "/contract", tags = "contract interface")
@Slf4j
@RestController
@RequestMapping(value = "/contract")
public class ContractController extends BaseController {

    @Autowired
    ContractService contractService;
    @Autowired
    LiquidCompileService liquidCompileService;

    /**
     * deploy.
     *
     * @param reqDeploy request data
     * @param result checkResult
     */
    @ApiOperation(value = "contract deploy", notes = "contract deploy")
    @ApiImplicitParam(name = "reqDeploy", value = "contract info", required = true,
            dataType = "ReqDeploy")
    @PostMapping("/deployWithSign")
    public String deploy(@Valid @RequestBody ReqDeploy reqDeploy, BindingResult result)
            throws Exception {
        log.info("contract deployWithSign start. ReqDeploy:[{}]",
                JsonUtils.toJSONString(reqDeploy));
        checkParamResult(result);
        if (StringUtils.isBlank(reqDeploy.getSignUserId())) {
            log.error("contract deployWithSign error: signUserId is empty");
            throw new FrontException(ConstantCode.PARAM_FAIL_SIGN_USER_ID_IS_EMPTY);
        }
        boolean isWasm = reqDeploy.getIsWasm();
        String liquidAddress = reqDeploy.getContractAddress();
        if (isWasm) {
            if (StringUtils.isBlank(liquidAddress)) {
                throw new FrontException(ConstantCode.DEPLOY_LIQUID_ADDRESS_CANNOT_EMPTY);
            }
            if (!liquidAddress.startsWith("/")) {
                throw new FrontException(ConstantCode.CONTRACT_ADDRESS_INVALID);
            }
        }
        String contractAddress = contractService.caseDeploy(reqDeploy, false);
        log.info("success deployWithSign. result:{}", contractAddress);
        return contractAddress;
    }

    /**
     * deploy locally not through sign
     */
    @ApiOperation(value = "contract deploy locally", notes = "contract deploy")
    @ApiImplicitParam(name = "reqDeploy", value = "contract info", required = true,
            dataType = "ReqDeploy")
    @PostMapping("/deploy")
    public String deployLocal(@Valid @RequestBody ReqDeploy reqDeploy, BindingResult result)
            throws Exception {
        log.info("contract deployLocal start. ReqDeploy:[{}]", JsonUtils.toJSONString(reqDeploy));
        checkParamResult(result);
        if (StringUtils.isBlank(reqDeploy.getUser())) {
            log.error("contract deployLocal error: user(address) is empty");
            throw new FrontException(ConstantCode.PARAM_FAIL_USER_IS_EMPTY);
        }
        boolean isWasm = reqDeploy.getIsWasm();
        String liquidAddress = reqDeploy.getContractAddress();
        if (isWasm) {
            if (StringUtils.isBlank(liquidAddress)) {
                throw new FrontException(ConstantCode.DEPLOY_LIQUID_ADDRESS_CANNOT_EMPTY);
            }
            if (!liquidAddress.startsWith("/")) {
                throw new FrontException(ConstantCode.CONTRACT_ADDRESS_INVALID);
            }
        }
        String contractAddress = contractService.caseDeploy(reqDeploy, true);
        log.info("success deployLocal. result:{}", contractAddress);
        return contractAddress;
    }


    /**
     * compile java .
     *
     * @param param request data
     * @param result checkResult
     */
    @ApiOperation(value = "compile java", notes = "compile java")
    @ApiImplicitParam(name = "param", value = "abi info", required = true, dataType = "ReqSendAbi")
    @PostMapping("/compile-java")
    public ResponseEntity<InputStreamResource> compileJavaFile(@Valid @RequestBody ReqSendAbi param,
            BindingResult result) throws FrontException, IOException {
        log.info("compileJavaFile start. reqSendAbi:{}", JsonUtils.toJSONString(param));
        checkParamResult(result);
        FileContentHandle fileContentHandle =
                ContractService.compileToJavaFile(param.getContractName(), param.getAbiInfo(),
                        param.getContractBin(), param.getPackageName());
        return ResponseEntity.ok().headers(FrontUtils.headers(fileContentHandle.getFileName()))
                .body(new InputStreamResource(fileContentHandle.getInputStream()));
    }


    @ApiOperation(value = "delete contract abi", notes = "delete contract abi")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contractName", value = "contractName", required = true,
                    dataType = "String"),
            @ApiImplicitParam(name = "version", value = "version", required = true,
                    dataType = "String")})
    @DeleteMapping("/deleteAbi/{contractName}/{version:.+}")
    public BaseResponse deleteAbi(@PathVariable String contractName, @PathVariable String version)
            throws FrontException {
        log.info("deleteAbi start. contractName:{} version:{}", contractName, version);
        return contractService.deleteAbi(contractName, version);
    }

    /**
     * sendAbi.
     *
     * @param reqSendAbi request data
     * @param result checkResult
     */
    @ApiOperation(value = "send abi", notes = "send abi")
    @ApiImplicitParam(name = "reqSendAbi", value = "abi info", required = true,
            dataType = "ReqSendAbi")
    @PostMapping("/abiInfo")
    public ResponseEntity sendAbi(@Valid @RequestBody ReqSendAbi reqSendAbi, BindingResult result)
            throws FrontException {
        log.info("sendAbi start. ReqSendAbi:[{}]", JsonUtils.toJSONString(reqSendAbi));
        checkParamResult(result);
        if (Objects.isNull(reqSendAbi.getGroupId())) {
            log.warn("fail sendAbi. groupId is null");
            throw new FrontException(ConstantCode.PARAM_FAIL_GROUP_ID_IS_EMPTY);
        }
        contractService.sendAbi(reqSendAbi);
        return ResponseEntity.ok().body(new BaseResponse(ConstantCode.RET_SUCCESS));
    }

    /**
     * save contract.
     */
    @ApiOperation(value = "save contract", notes = "save contract ")
    @ApiImplicitParam(name = "contract", value = "contract info", required = true,
            dataType = "ReqContractSave")
    @PostMapping(value = "/save")
    public Contract saveContract(@RequestBody @Valid ReqContractSave contract, BindingResult result)
            throws FrontException {
        log.info("saveContract start. contract:{}", contract.getContractName());
        checkParamResult(result);
        return contractService.saveContract(contract);
    }

    /**
     * copy Contracts.
     */
    @ApiOperation(value = "copy Contracts", notes = "copy Contracts ")
    @ApiImplicitParam(name = "req", value = "contract info", required = true,
            dataType = "ReqCopyContracts")
    @PostMapping(value = "/copyContracts")
    public BaseResponse copyContracts(@RequestBody @Valid ReqCopyContracts req,
            BindingResult result) throws FrontException {
        log.info("copyContracts start. req:{}", JsonUtils.toJSONString(req));
        checkParamResult(result);
        contractService.copyContracts(req);
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * delete by contractId.
     */
    @ApiOperation(value = "delete by contractId", notes = "delete by contractId")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", value = "groupId", required = true,
                    dataType = "int"),
            @ApiImplicitParam(name = "contractId", value = "contractId", required = true,
                    dataType = "Long")})
    @DeleteMapping("/{groupId}/{contractId}")
    public BaseResponse deleteByContractId(@PathVariable String groupId,
            @PathVariable Long contractId) throws FrontException {
        log.info("deleteByContractId start. groupId:{} contractId:{}", groupId, contractId);
        contractService.deleteContract(contractId, groupId);
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * query list of contract.
     */
    @ApiOperation(value = "query list of contract", notes = "query list of contract ")
    @ApiImplicitParam(name = "req", value = "param info", required = true,
            dataType = "ReqPageContract")
    @PostMapping(value = "/contractList")
    public BasePageResponse findByPage(@RequestBody @Valid ReqPageContract req,
            BindingResult result) throws FrontException, IOException {
        log.info("findByPage start. ReqPageContract:{}", JsonUtils.toJSONString(req));
        checkParamResult(result);
        Page<Contract> page = contractService.findContractByPage(req);
        BasePageResponse response = new BasePageResponse(ConstantCode.RET_SUCCEED);
        response.setTotalCount(page.getTotalElements());
        response.setData(page.getContent());
        return response;
    }

    /**
     * verify that if the contract changed.
     */
    @ApiOperation(value = "verify contract", notes = "verify that if the contract changed")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "groupId", value = "groupId", required = true,
                    dataType = "int"),
            @ApiImplicitParam(name = "contractId", value = "contractId", required = true,
                    dataType = "Long")})
    @GetMapping("/ifChanged/{groupId}/{contractId}")
    public boolean ifChanged(@PathVariable String groupId, @PathVariable Long contractId) {
        log.info("ifChanged start. groupId:{} contractId:{}", groupId, contractId);
        return contractService.verifyContractChange(contractId, groupId);
    }

    /**
     * query list of contract.
     */
    @ApiOperation(value = "compile contract", notes = "compile contract")
    @ApiImplicitParam(name = "req", value = "param info", required = true,
            dataType = "ReqContractCompile")
    @PostMapping(value = "/contractCompile")
    public RspContractCompile contractCompile(@RequestBody @Valid ReqContractCompile req,
            BindingResult result) throws FrontException {
        log.info("contractCompile start. param:{}", JsonUtils.toJSONString(req));
        checkParamResult(result);
        return contractService.contractCompile(req.getContractName(), req.getSolidityBase64(), req.getGroupId());
    }

    /**
     * multiple contract compile.
     */
    @ApiOperation(value = "multiple contract compile", notes = "multiple contract compile")
    @PostMapping(value = "/multiContractCompile")
    public List<RspMultiContractCompile> multiContractCompile(
            @RequestBody @Valid ReqMultiContractCompile req, BindingResult result)
            throws FrontException, IOException {
        Instant startTime = Instant.now();
        log.info("start multiContractCompile startTime:{} param:{}", startTime.toEpochMilli(),
                JsonUtils.toJSONString(req));
        checkParamResult(result);
        List<RspMultiContractCompile> response = contractService.multiContractCompile(req);
        log.info("end multiContractCompile useTime:{}",
                Duration.between(startTime, Instant.now()).toMillis());
        return response;
    }

    @PostMapping(value = "/addContractPath")
    public ContractPath addContractPath(@RequestBody @Valid ReqContractPath req) {
        log.info("addContractPath start. param:{}", JsonUtils.toJSONString(req));
        return contractService.addContractPath(req);
    }

    /**
     * query by groupId.
     */
    @GetMapping(value = "/findPathList/{groupId}")
    public List<ContractPath> findPathList(@PathVariable("groupId") String groupId)
            throws IOException {
        log.info("start findPathList. groupId:{}", groupId);
        return contractService.findPathList(groupId);
    }

    /**
     * only delete path, not delete contract
     * @param groupId
     * @param contractPath
     * @return
     */
    @DeleteMapping("/deletePath/{groupId}/{contractPath}")
    public BaseResponse deletePath(@PathVariable("groupId") String groupId,
            @PathVariable String contractPath) {
        log.info("start deletePath. contractPath:{}", contractPath);
        contractService.deletePath(groupId, contractPath);
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * delete path and contracts in it
     * @param groupId
     * @param contractPath
     * @return
     */
    @DeleteMapping("/batch/{groupId}/{contractPath}")
    public BaseResponse batchDeletePath(@PathVariable("groupId") String groupId,
            @PathVariable String contractPath) {
        log.info("start deletePath. contractPath:{}", contractPath);
        contractService.batchDeleteByPath(groupId, contractPath);
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * query list of contract only contain groupId and contractAddress and contractName
     */
    @ApiOperation(value = "query list of all contract without abi/bin",
            notes = "query list of contract without abi/bin")
    @ApiImplicitParam(name = "groupId", value = "groupId", required = true, dataType = "Integer")
    @GetMapping(value = "/contractList/all/light")
    public BasePageResponse findAll(@RequestParam("groupId") String groupId,
            @RequestParam("contractStatus") Integer contractStatus)
            throws FrontException, IOException {
        log.info("findAll start. groupId:{},contractStatus:{}", groupId, contractStatus);
        List<RspContractNoAbi> contractNoAbiList =
                contractService.findAllContractNoAbi(groupId, contractStatus);
        BasePageResponse response = new BasePageResponse(ConstantCode.RET_SUCCEED);
        response.setTotalCount(contractNoAbiList.size());
        response.setData(contractNoAbiList);
        return response;
    }

    /**
     * query list of contract only contain groupId and contractAddress and contractName
     */
    @ApiOperation(value = "get one contract", notes = "get one contract")
    @ApiImplicitParam(name = "contractId", value = "contractId", required = true,
            dataType = "Integer")
    @GetMapping(value = "/findOne/{contractId}")
    public BaseResponse findOne(@PathVariable Integer contractId) {
        log.info("findOne start. contractId:{}", contractId);
        Contract contract = contractService.findById(contractId.longValue());
        BaseResponse response = new BaseResponse(ConstantCode.RET_SUCCEED);
        response.setData(contract);
        return response;
    }

    /**
     * query list of contract by multi contract path
     */
    @ApiOperation(value = "query list of contract by multi path",
            notes = "query list of contract by multi path")
    @ApiImplicitParam(name = "req", value = "param info", required = true,
            dataType = "ReqListContract")
    @PostMapping(value = "/contractList/multiPath")
    public BasePageResponse listByMultiPath(@RequestBody @Valid ReqListContract req,
            BindingResult result) {
        log.info("listByMultiPath start. ReqListContract:{}", req);
        checkParamResult(result);
        List<Contract> resList = contractService.listContractByMultiPath(req);
        BasePageResponse response = new BasePageResponse(ConstantCode.RET_SUCCEED);
        response.setTotalCount(resList.size());
        response.setData(resList);
        return response;
    }

    /**
     * registerCns.
     *
     * @param req request data
     * @param result checkResult
     */
    @PostMapping("/registerCns")
    public BaseResponse registerCns(@Valid @RequestBody ReqRegisterCns req, BindingResult result)
            throws Exception {
        log.info("registerCns start. req:[{}]", JsonUtils.toJSONString(req));
        checkParamResult(result);
        contractService.registerCns(req);
        log.info("success registerCns.");
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * findCns.
     * 
     * @param req
     * @param result
     * @return
     */
    @PostMapping(value = "/findCns")
    public BaseResponse findCns(@Valid @RequestBody ReqQueryCns req, BindingResult result) throws IOException {
        log.info("findCns start. req:[{}]", JsonUtils.toJSONString(req));
        checkParamResult(result);
        BaseResponse response = new BaseResponse(ConstantCode.RET_SUCCEED);
        Cns cns = contractService.findCns(req);
        response.setData(cns);
        return response;
    }

    /**
     * query list of contract only contain groupId and contractAddress and contractName
     */
    @ApiOperation(value = "check", notes = "check cargo liquid env")
    @GetMapping(value = "/liquid/check")
    public BaseResponse checkLiquidEnv() {
        log.info("checkLiquidEnv start.");
        liquidCompileService.checkLiquidEnv();
        return new BaseResponse(ConstantCode.RET_SUCCEED);
    }

    /**
     * 执行编译的时候，直接new操作，并且执行编译操作，new是同步的，编译是异步的
     * 在db记录已存在的合约目录，已存在且正在running，则报错正在运行；；如果已存在但是已结束，则删除重复的liquid合约目录，删除
     * 一个单独的异步表记录当前异步编译状态
     * @param req
     * @param result
     * @return
     */
    @PostMapping(value = "/liquid/compile")
    public BaseResponse newAndCompileLiquidProject(@Valid @RequestBody ReqContractSave req, BindingResult result) {
        Instant now = Instant.now();
        log.info("newAndSaveLiquidProject start. startTime:{},req:[{}]", now, JsonUtils.toJSONString(req));
        checkParamResult(result);
        CompileTask compileTask = contractService.newAndCompileLiquidContract(req);
        log.info("newAndSaveLiquidProject end. usedTime:{}, compileTask:{}", Duration.between(now, Instant.now()).toMillis(), compileTask);
        return new BaseResponse(ConstantCode.RET_SUCCEED, compileTask);
    }

    @ApiOperation(value = "check compile", notes = "check liquid compile finished")
    @PostMapping(value = "/liquid/compile/check")
    public BaseResponse checkLiquidContractCompile(@Valid @RequestBody ReqCompileTask req, BindingResult result) {
        Instant now = Instant.now();
        log.info("checkLiquidContractCompile start. startTime:{},req:{}", now, req);
        checkParamResult(result);
        CompileTask taskInfo = contractService.getLiquidContract(req.getGroupId(), req.getContractPath(), req.getContractName());
        log.info("newAndSaveLiquidProject end. usedTime:{}, compileTask:{}", Duration.between(now, Instant.now()).toMillis(), taskInfo);
        return new BaseResponse(ConstantCode.RET_SUCCEED, taskInfo);
    }



}
