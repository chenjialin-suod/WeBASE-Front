package com.webank.webase.front.precntauth.authmanager.committee.entity;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReqResetAdminInfo {

  private String groupId;
  private String contractAddr;
  private String newAdmin;
  private String signUserId;
  @NotNull
  private String fromAddress;

}
