/**
 * Copyright 2014-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.webase.front.event.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * entity to store EventLog push register info (not decoded)
 * when Service restarts, registerEventLogPushCallBack auto
 * related with ServiceEventLogPushCallback
 * @author marsli
 */
@Entity
@Data
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "unique_contract_event", columnNames = {"appId", "exchangeName",
                "queueName", "contractAddress"})
})
public class ContractEventInfo {

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid")
    @GeneratedValue(generator="idGenerator")
    private String id;

    /**
     * event type: 1: newBlockEvent, 2: contractEvent, 3: others
     */
    private Integer eventType;

    /**
     * application id to register
     */
    private String appId;

    /**
     * group id
     */
    private String groupId;

    /**
     * MQ info
     */
    private String exchangeName;

    /**
     * @appId as queue name
     */
    private String queueName;

    /**
     * concat appId + "_" + event/block + "_" + randomStr as routing key
     */
    private String routingKey;

    /**
     * if use DecodedEventLogPushCallback, needs abi for decoder
     */
    @Column(columnDefinition = "mediumtext")
    private String contractAbi;

    /**
     * EventLogUserParams info
     */
    private String fromBlock;
    private String toBlock;
    /**
     * single contract address
     */
    private String contractAddress;
    /**
     * List<String> event name list
     */
    @Column(columnDefinition = "text")
    private String topicList;

    /**
     * registerId when register callback in groupManagerService
     */
    private String registerId;

    private LocalDateTime createTime;


}
