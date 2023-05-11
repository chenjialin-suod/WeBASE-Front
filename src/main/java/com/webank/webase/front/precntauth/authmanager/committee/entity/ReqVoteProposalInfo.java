package com.webank.webase.front.precntauth.authmanager.committee.entity;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;

@Data
public class ReqVoteProposalInfo {

  private String groupId;
  private BigInteger proposalId;
  private String signUserId;
  private Boolean agree;
  @NotNull
  private String fromAddress;

}
