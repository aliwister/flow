package com.badals.flow.workflow;

import com.badals.flow.payload.RefundData;
import io.nflow.engine.workflow.definition.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.*;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class RefundWorkflow extends WorkflowDefinition<RefundWorkflow.State> {

   private static final Logger logger = getLogger(RefundWorkflow.class);
   public static final String VAR_KEY = "requestData";
   public static final String TYPE = "refundWorkflow";

   public RefundWorkflow() {
      super(TYPE, RefundWorkflow.State.createRefund, RefundWorkflow.State.error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0).setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
      setDescription("Order Return processing");
      permit(State.createRefund, State.refundCustomer);

      permit(State.refundCustomer, State.customerRefunded);
      permit(State.customerRefunded, State.done);

   }

   public enum State implements WorkflowState {
      createRefund(start, "Refund request intiated"),
      refundCustomer(manual, "Send Customer Refund"),
      customerRefunded(normal, "Send Customer Refund"),
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

   public NextAction createRefund(@SuppressWarnings("unused") StateExecution execution,
                                  @StateVar(value = VAR_KEY, readOnly = true) RefundWorkflow.RefundApplication request) {
      logger.info("IRL: external service call for persisting credit application using request data");
      return moveToState(RefundWorkflow.State.refundCustomer, "Refund request initiated");
   }

   public void refundCustomer(@SuppressWarnings("unused") StateExecution execution,
                              @StateVar(value = VAR_KEY, readOnly = true) RefundWorkflow.RefundApplication request) {
      logger.info("Issue a refund to customer");
   }
   public NextAction customerRefunded(@SuppressWarnings("unused") StateExecution execution,
                                      @StateVar(value = VAR_KEY, readOnly = true) RefundWorkflow.RefundApplication request) {
      logger.info("Confirm receipt by vendor");
      return moveToState(State.done, "Return application created");
   }

   public void done(@SuppressWarnings("unused") StateExecution execution,
                    @SuppressWarnings("unused") @StateVar(value = VAR_KEY) RefundWorkflow.RefundApplication info) {
      logger.info("Credit application process ended");
   }

   public void error(@SuppressWarnings("unused") StateExecution execution,
                     @SuppressWarnings("unused") @StateVar(value = VAR_KEY) RefundWorkflow.RefundApplication info) {
      logger.info("IRL: some UI should poll for workflows that have reached error state");
   }

   public static class RefundApplication {
      public boolean onUs;
      public Double amount;
      public int weight;

      public RefundApplication() {
      }

      public RefundApplication(int weight, Double amount, boolean onUs) {
         this.weight = weight;
         this.amount = amount;
         this.onUs = onUs;
      }
   }

}