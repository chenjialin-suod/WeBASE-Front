package com.webank.webase.front.precntauth.authmanager.committee.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigInteger;

@Data
public class ReqSetRateInfo {

  private String groupId;
  private BigInteger participatesRate;
  private BigInteger winRate;
  private String signUserId;
  @NotBlank
  private String fromAddress;


}
