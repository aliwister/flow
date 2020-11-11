package com.badals.flow.payload;

public class CreateCreditApplicationRequest extends AbstractRequest {

   public String accountNo;
   public int amount;
   public String clientId;
   public String productId;
   public long processWorkflowId;

}
