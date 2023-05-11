package com.webank.webase.front.precntauth.authmanager.committee.entity;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Data
public class ReqRevokeProposalInfo {

  private String groupId;
  private BigInteger proposalId;
  private String signUserId;
  @NotNull
  private String fromAddress;
}
