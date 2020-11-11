package com.badals.flow.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.slf4j.LoggerFactory.getLogger;

import io.nflow.engine.workflow.definition.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReturnWorkflow extends WorkflowDefinition<ReturnWorkflow.State> {

   private static final Logger logger = getLogger(ReturnWorkflow.class);
   private static final String VAR_KEY = "info";
   public static final String TYPE = "returnApplication";

   public ReturnWorkflow() {
      super(TYPE, ReturnWorkflow.State.createReturn, ReturnWorkflow.State.error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0).setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
      setDescription("Order Return processing");
      permit(ReturnWorkflow.State.createReturn, State.isApprove);
      permit(ReturnWorkflow.State.isApprove, State.reject);
      permit(ReturnWorkflow.State.isApprove, State.approved);

      permit(ReturnWorkflow.State.reject, State.finish);
      permit(ReturnWorkflow.State.approved, State.toStore);

      permit(ReturnWorkflow.State.toStore, State.toStoreReceived);
      permit(ReturnWorkflow.State.approved, State.toVendor);
      permit(ReturnWorkflow.State.toStoreReceived, State.toVendor);
      permit(ReturnWorkflow.State.toVendor, ReturnWorkflow.State.toVendorReceived);
      permit(ReturnWorkflow.State.toVendorReceived, ReturnWorkflow.State.finish);

      permit(ReturnWorkflow.State.toVendorReceived, ReturnWorkflow.State.awaitingVendorRefund);
      permit(ReturnWorkflow.State.awaitingVendorRefund, ReturnWorkflow.State.vendorRefunded);
      permit(ReturnWorkflow.State.vendorRefunded, ReturnWorkflow.State.awaitingVendorRefundVerify);


      permit(ReturnWorkflow.State.awaitingVendorRefundVerify, ReturnWorkflow.State.vendorRefundVerified);
      permit(ReturnWorkflow.State.vendorRefundVerified, ReturnWorkflow.State.refundCustomer);
      permit(ReturnWorkflow.State.vendorRefundVerified, ReturnWorkflow.State.finish);
      permit(ReturnWorkflow.State.refundCustomer, ReturnWorkflow.State.customerRefunded);

      permit(ReturnWorkflow.State.refundCustomer, ReturnWorkflow.State.finish);
      permit(ReturnWorkflow.State.finish, ReturnWorkflow.State.done);
   }

   public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
      createReturn(start, "Return application is persisted to database"),
      isApprove(manual, "Manual return approval is made"),
      approved(normal, "Manual return approval is made"),
      reject(normal, "Manual return reject is made"),
      toStore(manual, "Return is expected in Store"),
      toStoreReceived(normal, "Return is received in Store"),
      toVendor(manual, "Return being sent to Vendor"),
      toVendorReceived(normal, "Return is received by Vendor"),
      awaitingVendorRefund(manual, "Return is received by Vendor"),
      vendorRefunded(normal, "Refund email from Vendor"),
      awaitingVendorRefundVerify(manual, "Verify refund to CC/Account"),
      vendorRefundVerified(normal, "Verify refund to CC/Account"),
      refundCustomer(manual, "Send Customer Refund"),
      customerRefunded(normal, "Send Customer Refund"),
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
                                  @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                  @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(ReturnWorkflow.State.isApprove, "Return application created");
   }

   public void isApprove(@SuppressWarnings("unused") StateExecution execution,
                               @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                               @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Waiting to approve");
   }

   public NextAction approved(@SuppressWarnings("unused") StateExecution execution,
                                   @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                   @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");
      if (request.toVendor)
         return moveToState(State.toVendor, "Returning directly to vendor");
      else
         return moveToState(State.toStore, "Returning via store");
   }

   public NextAction reject(@SuppressWarnings("unused") StateExecution execution,
                                  @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                  @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(State.finish, "Return application created");
   }

   public void toStore(@SuppressWarnings("unused") StateExecution execution,
                             @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                             @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");

   }

   public NextAction toStoreReceived(@SuppressWarnings("unused") StateExecution execution,
                               @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                               @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Confirm Receipt in store");
      return moveToState(State.toVendor, "Return application created");
   }

   public void toVendor(@SuppressWarnings("unused") StateExecution execution,
                              @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                              @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");

   }

   public NextAction toVendorReceived(@SuppressWarnings("unused") StateExecution execution,
                                @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.awaitingVendorRefund, "Return application created");
   }

   public void awaitingVendorRefund(@SuppressWarnings("unused") StateExecution execution,
                                @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Received confirmation on receipt by vendor");
   }

   public NextAction vendorRefunded(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.awaitingVendorRefundVerify, "Return application created");
   }

   public void awaitingVendorRefundVerify(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Check accounting on receipt by vendor");
   }
   public NextAction vendorRefundVerified(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.refundCustomer, "Return application created");
   }

   public void refundCustomer(@SuppressWarnings("unused") StateExecution execution,
                              @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                              @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Issue a refund to customer");
   }
   public NextAction customerRefunded(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.finish, "Return application created");
   }

   public NextAction finish(@SuppressWarnings("unused") StateExecution execution,
                                             @StateVar(value = "requestData", readOnly = true) ReturnWorkflow.ReturnApplication request,
                                             @StateVar(instantiateIfNotExists = true, value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(ReturnWorkflow.State.done, "Return application created");
   }


   public void done(@SuppressWarnings("unused") StateExecution execution,
                    @SuppressWarnings("unused") @StateVar(value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("Credit application process ended");
   }

   public void error(@SuppressWarnings("unused") StateExecution execution,
                     @SuppressWarnings("unused") @StateVar(value = VAR_KEY) ReturnWorkflow.WorkflowInfo info) {
      logger.info("IRL: some UI should poll for workflows that have reached error state");
   }

   public static class ReturnApplication {
      public Long orderId;
      List<Item> items;
      public boolean replacement = false;
      public boolean toVendor = false;
      public boolean inventoryReturn = false;

      public ReturnApplication() {
      }

      public ReturnApplication(Long orderId, List<Item> items, Boolean replacement, Boolean toVendor) {
         this.orderId = orderId;
         this.items = items;
         this.replacement = replacement;
         this.toVendor = toVendor;
      }
   }

   public static class Item {
      Long productId;
      BigDecimal quantity;
   }

   public static class WorkflowInfo {
      public Long orderId;
   }
}