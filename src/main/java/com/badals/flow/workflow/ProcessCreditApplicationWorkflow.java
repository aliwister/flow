package com.badals.flow.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import static org.joda.time.DateTime.now;

import com.badals.flow.payload.CreateCreditApplicationRequest;
import com.badals.flow.payload.CreateLoanResponse;
import com.badals.flow.payload.CreditDecisionData;
import com.badals.flow.payload.QueryCreditApplicationResponse;

import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.WorkflowInstance;

public class ProcessCreditApplicationWorkflow extends WorkflowDefinition<ProcessCreditApplicationWorkflow.State> {

   private static final String CREDIT_APPLICATION_KEY = "credit_application_key";
   private static final String LOAN_KEY = "loan_key";
   private static final String CREDIT_DECISION_RESULT = "credit_decision_result";

   public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
      createCreditApplication(start, "Create new credit application"), //
      startCreditDecisionWorkflow(normal, "Start credit decision workflow"), //
      waitCreditDecisionWorkflow(normal, "Poll for result of credit decision process"), //
      createLoan(normal, "Create the loan based on application"), //
      transferMoney(normal, "Transfer money to deposit account"), //
      transferMoneyFailed(normal, "Transfering money failed, reverse creating loan"), //
      updateCreditApplication(normal, "Update the credit application state"), //
      manualProcessing(manual, "Process must be handled manually because of an unexpected situation"), //
      done(end, "Credit application has been completed.");

      private WorkflowStateType type;
      private String description;

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

   public ProcessCreditApplicationWorkflow() {
      super("processCreditApplication", State.createCreditApplication, State.manualProcessing);
      setName("Process credit application");
      setDescription("Makes credit decision, creates loan, deposits the money and updates credit application");
      permit(State.createCreditApplication, State.startCreditDecisionWorkflow);
      permit(State.startCreditDecisionWorkflow, State.waitCreditDecisionWorkflow);
      permit(State.waitCreditDecisionWorkflow, State.createLoan);
      permit(State.waitCreditDecisionWorkflow, State.updateCreditApplication);
      permit(State.createLoan, State.transferMoney);
      permit(State.transferMoney, State.updateCreditApplication, State.transferMoneyFailed);
      permit(State.transferMoneyFailed, State.manualProcessing);
      permit(State.updateCreditApplication, State.done);
   }

   public NextAction createCreditApplication(StateExecution execution,
                                             @StateVar(readOnly = true, value = "requestData") CreateCreditApplicationRequest request,
                                             @StateVar(value = CREDIT_APPLICATION_KEY) Mutable<QueryCreditApplicationResponse> application) {
      request.processWorkflowId = execution.getWorkflowInstanceId();
      application.setVal(new QueryCreditApplicationResponse());
      return moveToState(State.startCreditDecisionWorkflow, "Credit application created");
   }

   public NextAction startCreditDecisionWorkflow(StateExecution execution,
                                                 @StateVar(value = CREDIT_APPLICATION_KEY) QueryCreditApplicationResponse application) {
      CreditDecisionData creditDecisionData = new CreditDecisionData();
      creditDecisionData.clientId = application.clientId;
      creditDecisionData.amount = application.amount;
      execution.addChildWorkflows(
              execution.workflowInstanceBuilder().setType(CreditDecisionWorkflow.TYPE).setBusinessKey(application.applicationId)
                      .putStateVariable(CreditDecisionWorkflow.VAR_REQUEST_DATA, creditDecisionData).build());
      return moveToStateAfter(State.waitCreditDecisionWorkflow, now().plusMonths(1), "Credit decision request submitted");
   }

   public NextAction waitCreditDecisionWorkflow(StateExecution execution,
                                                @StateVar(value = CREDIT_DECISION_RESULT) Mutable<String> creditDecisionResult) {
      WorkflowInstance decisionWorkflow = execution.getAllChildWorkflows().get(0);
      creditDecisionResult.setVal(decisionWorkflow.state);
      if (CreditDecisionWorkflow.State.approved.name().equals(decisionWorkflow.state)) {
         return moveToState(State.createLoan, "Credit decision approved");
      }
      if (CreditDecisionWorkflow.State.rejected.name().equals(decisionWorkflow.state)) {
         return moveToState(State.updateCreditApplication, "Credit decision rejected");
      }
      return retryAfter(now().plusSeconds(20), "Credit decision workflow in state " + decisionWorkflow.state);
   }

   public NextAction createLoan(@SuppressWarnings("unused") StateExecution execution,
                                @StateVar(value = LOAN_KEY) Mutable<CreateLoanResponse> loan) {
      CreateLoanResponse response = new CreateLoanResponse();
      response.success = true;
      loan.setVal(response);
      return moveToState(State.transferMoney, "Loan created");
   }

   public NextAction transferMoney(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.updateCreditApplication, "Money transferred");
   }

   public NextAction updateCreditApplication(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.done, "Credit application updated");
   }

   public NextAction transferMoneyFailed(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(State.manualProcessing, "Loan cancelled");
   }

}
