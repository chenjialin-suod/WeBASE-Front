package com.webank.webase.front.precntauth.precompiled.bfs.entity;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReqCreateBFSInfo {

  private String groupId;
  private String path;
  private String signUserId;
  @NotNull
  private String fromAddress;

}
