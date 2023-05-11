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
package com.webank.webase.front.contract.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Contract's entity in db(using h2.db)
 */
@Entity
@Data
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "unique_contract", columnNames = {"groupId", "contractPath",
        "contractName"})
})
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String contractPath;
    private String version;
    private String contractName;
    private Integer contractStatus;
    private String groupId;
    @Column(columnDefinition = "mediumtext")
    private String contractSource;
    @Column(columnDefinition = "mediumtext")
    private String contractAbi;
    @Column(columnDefinition = "mediumtext")
    private String contractBin;
    @Column(columnDefinition = "mediumtext")
    private String bytecodeBin;
    private String contractAddress;
    private LocalDateTime deployTime;
    private String description;
    /**
     * 0-solidity, 1-liquid
     */
    private Integer isWasm;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}
