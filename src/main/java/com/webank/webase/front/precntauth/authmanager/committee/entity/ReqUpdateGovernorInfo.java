package com.webank.webase.front.precntauth.authmanager.committee.entity;

import lombok.Data;

import java.math.BigInteger;

@Data
public class ReqUpdateGovernorInfo {

  private String groupId;
  private String fromAddress;     //请求地址
  private String accountAddress;  //账户地址
  private String signUserId;
  private BigInteger weight;

}
