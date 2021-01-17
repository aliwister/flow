package com.badals.flow.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.slf4j.LoggerFactory.getLogger;

import com.badals.flow.payload.RefundData;
import com.badals.flow.payload.ReturnData;
import io.nflow.engine.workflow.definition.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReturnWorkflow extends WorkflowDefinition<ReturnWorkflow.State> {

   private static final Logger logger = getLogger(ReturnWorkflow.class);
   private static final String VAR_KEY = "info";
   private static final String LABEL_KEY = "label";
   public static final String TYPE = "returnWorkflow";

   public ReturnWorkflow() {
      super(TYPE, ReturnWorkflow.State.createReturn, ReturnWorkflow.State.error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0).setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
      setDescription("Order Return processing");
      permit(State.createReturn, State.isApprove);
      permit(State.isApprove, State.reject);
      permit(State.isApprove, State.approved);

      permit(State.reject, State.finish);
      permit(State.approved, State.startReturn);


      permit(State.approved, State.isReplacementApprove);

      permit(State.isReplacementApprove, State.replacementApproved);
      permit(State.replacementApproved, State.createReplacementOrder);


      permit(State.isReplacementApprove, State.replacementRejected);
      permit(State.replacementRejected, State.startReturn);

      permit(State.createReplacementOrder, State.startReturn);
      permit(State.replacementRejected, State.startReturn);


      permit(State.startReturn, State.reviewReturn);
      permit(State.reviewReturn, State.proceed);
      permit(State.reviewReturn, State.gift);
      permit(State.reviewReturn, State.stock);
      permit(State.reviewReturn, State.discard);

      permit(State.gift, State.finish);
      permit(State.stock, State.finish);
      permit(State.discard, State.finish);

      permit(State.proceed, State.toStore);
      permit(State.proceed, State.generateLabels);

      permit(State.toStore, State.toStoreReceived);

      permit(State.toStoreReceived, State.generateLabels);
      permit(State.generateLabels, State.toVendor);


      permit(State.toVendor, State.toVendorReceived);

      permit(State.toVendorReceived, State.startRefundWorkflow);

      permit(State.startRefundWorkflow, State.awaitingVendorRefund);
      permit(State.startRefundWorkflow, State.finish);
      permit(State.awaitingVendorRefund, State.vendorRefunded);
      permit(State.vendorRefunded, State.awaitingVendorRefundVerify);


      permit(State.awaitingVendorRefundVerify, State.vendorRefundVerified);
      permit(State.vendorRefundVerified, State.finish);
      permit(State.finish, State.done);
   }

   public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
      createReturn(start, "Return application is persisted to database"),
/*      makeOffer(manual, "Make an offer to customer to avoid return"),
      discountOffer(normal,"Discount in comments to be added"),
      couponOffer(normal,"To send coupon to customer"),
      persuadeOffer(normal,"Customer persuaded to keep it"),
      */
      isApprove(manual, "Manual return approval is made"),

      approved(normal, "Manual return approval is made"),

      isReplacementApprove(manual, "Manual replacement approval is made"),
      replacementApproved(normal, "Manual return approval is made"),
      replacementRejected(normal, "Replacement is not possible"),

      createReplacementOrder(manual, "Create replacement order"),

      reject(normal, "Manual return reject is made"),

      startReturn(normal, "Start item return"),
      reviewReturn(manual, "Review whether to return"),
      proceed(normal, "Start item return"),
      gift(manual, "Start item return"),
      stock(manual, "Start item return"),
      discard(manual, "Start item return"),

      toStore(manual, "Return is expected in Store"),
      toStoreReceived(normal, "Return is received in Store"),
      generateLabels(manual, "Return being sent to Vendor"),
      toVendor(manual, "Return being sent to Vendor"),
      
      toVendorReceived(normal, "Return is received by Vendor"),
      startRefundWorkflow(normal, "Start customer refund workflow"),
      awaitingVendorRefund(manual, "Return is received by Vendor"),
      vendorRefunded(normal, "Refund email from Vendor"),
      awaitingVendorRefundVerify(manual, "Verify refund to CC/Account"),
      vendorRefundVerified(normal, "Verify refund to CC/Account"),

      finish(normal, "Credit application status is set"),
      done(end, "Return application process finished"),
      error(manual, "Manual processing of failed applications");

      private final WorkflowStateType type;
      private final String description;

      private State(WorkflowStateType type, String description) {
         this.type = type;
         this.description = description;
      }

      @Override
      public WorkflowStateType getType() {
         return type;
      }

      @Override
      public String getDescription() {
         return description;
      }
   }

   public NextAction createReturn(@SuppressWarnings("unused") StateExecution execution,
                                  @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.isApprove, "Return application created");
   }

/*   public void makeOffer(@SuppressWarnings("unused") StateExecution execution,
                                 @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");

      if (request.toVendor)
         return moveToState(State.generateLabels, "Returning directly to vendor");
      else
         return moveToState(State.toStore, "Returning via store");
   }*/

   public void isApprove(@SuppressWarnings("unused") StateExecution execution,
                               @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Waiting to approve");
   }



   public void isReplacementApprove(@SuppressWarnings("unused") StateExecution execution,
                                    @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                               @StateVar(value = "isManual", readOnly = true) boolean manual) {
      logger.info("Waiting to approve");
   }

   public NextAction approved(@SuppressWarnings("unused") StateExecution execution,
                                   @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");

      if(request.replacement)
         return moveToState(State.isReplacementApprove, "Get replacement approval");
      else
         return moveToState(State.startReturn, "Start return no/replacement");
   }

   public NextAction startReturn(@SuppressWarnings("unused") StateExecution execution,
                                   @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.reviewReturn, "Returning directly to vendor");
   }

   public void reviewReturn(@SuppressWarnings("unused") StateExecution execution,
                            @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
   }


   public NextAction proceed(@SuppressWarnings("unused") StateExecution execution,
                                 @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      if (request.toVendor)
         return moveToState(State.generateLabels, "Returning directly to vendor");
      else
         return moveToState(State.toStore, "Returning via store");
   }




   public void gift(@SuppressWarnings("unused") StateExecution execution,
                                 @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
   }
   public void stock(@SuppressWarnings("unused") StateExecution execution,
                                 @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
   }
   public void discard(@SuppressWarnings("unused") StateExecution execution,
                                 @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
   }


   public NextAction replacementApproved(@SuppressWarnings("unused") StateExecution execution,
                                         @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.createReplacementOrder, "Start return no/replacement");
   }


   public void createReplacementOrder(@SuppressWarnings("unused") StateExecution execution,
                                    @StateVar(value = "isManual", readOnly = true) boolean manual) {
      logger.info("Waiting to approve");
   }

   public NextAction replacementRejected(@SuppressWarnings("unused") StateExecution execution,
                                   @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.startReturn, "Start return no/replacement");
   }

   public NextAction reject(@SuppressWarnings("unused") StateExecution execution,
                                  @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.finish, "Return application created");
   }

   public void toStore(@SuppressWarnings("unused") StateExecution execution,
                             @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");

   }

   public NextAction toStoreReceived(@SuppressWarnings("unused") StateExecution execution,
                               @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Confirm Receipt in store");
      return moveToState(State.generateLabels, "Return application created");
   }

   public void generateLabels(@SuppressWarnings("unused") StateExecution execution,
                              @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");

   }
   public void toVendor(@SuppressWarnings("unused") StateExecution execution,
                              @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                              @StateVar(instantiateIfNotExists = true, value = "label") ReturnWorkflow.LabelInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");

   }


   public NextAction toVendorReceived(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Confirm receipt by vendor");

      return moveToState(State.startRefundWorkflow, "Return application created");
   }

   public NextAction startRefundWorkflow(@SuppressWarnings("unused") StateExecution execution,
                                         @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                         @StateVar(value = "label", readOnly = true) ReturnWorkflow.LabelInfo label) {
      RefundData refundData = new RefundData();
      refundData.onUs = request.onUs;
      refundData.amount = request.amount;
      refundData.weight = label.weight;

      if(!request.replacement)
         execution.addChildWorkflows(
                 execution.workflowInstanceBuilder().setType(RefundWorkflow.TYPE).setBusinessKey(execution.getBusinessKey())
                         .putStateVariable(RefundWorkflow.VAR_KEY, refundData).build());

      return moveToState(State.awaitingVendorRefund, "No customer refund required");
   }


   public void awaitingVendorRefund(@SuppressWarnings("unused") StateExecution execution,
                                @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
   }

   public NextAction vendorRefunded(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.awaitingVendorRefundVerify, "Return application created");
   }

   public void awaitingVendorRefundVerify(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Check accounting on receipt by vendor");
   }
   public NextAction vendorRefundVerified(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.finish, "Return application created");
   }


   public NextAction finish(@SuppressWarnings("unused") StateExecution execution,
                                             @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(ReturnWorkflow.State.done, "Return application created");
   }


   public void done(@SuppressWarnings("unused") StateExecution execution) {
      logger.info("Credit application process ended");
   }

   public void error(@SuppressWarnings("unused") StateExecution execution) {
      logger.info("IRL: some UI should poll for workflows that have reached error state");
   }

   public static class ReturnApplication {
      public Long productId;
      public Long sequence;
      public BigDecimal quantity;
      public boolean replacement = false;
      public boolean toVendor = false;
      public String  instructions;
      public String  reason;
      public String  sku;
      public String po;
      public String ticketUrl;
      public String  productName;
      public boolean inventoryReturn = false;
      public boolean onUs = false;
      public BigDecimal amount;

      public ReturnApplication() {
      }

      public ReturnApplication(Long sequence, String sku, String productName, Long productId, BigDecimal quantity, boolean replacement, boolean toVendor, String instructions, String reason, boolean onUs, String po, String ticketUrl) {

         this.productId = productId;
         this.sku = sku;
         this.productName= productName;
         this.quantity = quantity;
         this.replacement = replacement;
         this.toVendor = toVendor;
         this.instructions = instructions;
         this.reason = reason;
         this.onUs = onUs;
         this.sequence = sequence;
         this.po = po;
         this.ticketUrl = ticketUrl;
      }
   }
   

   public static class LabelInfo {
      public String trackingNum;
      public String carrier;
      public String labelFile;
      public boolean ourLabel;
      public BigDecimal weight;
      public Double returnFee;

      public LabelInfo() {

      }

      public LabelInfo(String trackingNum, String carrier, String labelFile, boolean ourLabel, BigDecimal weight, Double returnFee) {
         this.trackingNum = trackingNum;
         this.carrier = carrier;
         this.labelFile = labelFile;
         this.ourLabel = ourLabel;
         this.weight = weight;
         this.returnFee = returnFee;
      }
   }
}