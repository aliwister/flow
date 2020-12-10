package com.badals.flow.payload;

import com.badals.flow.workflow.ReturnWorkflow;

import java.math.BigDecimal;
import java.util.List;

public class ReturnData {
   public Long orderId;
   List<ReturnData.Item> items;
   public boolean replacement = false;
   public boolean toVendor = false;
   public boolean inventoryReturn = false;
   public boolean onUs = false;
   public int weight = 0;
   public Double amount;

   public ReturnData() {
   }

   public ReturnData(Long orderId, List<ReturnData.Item> items, Boolean replacement, Boolean toVendor) {
      this.orderId = orderId;
      this.items = items;
      this.replacement = replacement;
      this.toVendor = toVendor;
   }

   public static class Item {
      Long productId;
      BigDecimal quantity;
   }
}
