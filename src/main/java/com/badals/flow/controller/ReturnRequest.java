package com.badals.flow.controller;

import com.badals.flow.workflow.ReturnWorkflow;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequest {
   public Long orderId;
   List<ReturnWorkflow.Item> items;
   public boolean replacement = false;
   public boolean toVendor = false;
}
