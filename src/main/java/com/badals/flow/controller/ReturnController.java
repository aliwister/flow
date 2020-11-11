package com.badals.flow.controller;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

@Controller
@RequestMapping("return")
public class ReturnController {
   private final WorkflowInstanceService workflowInstances;

   private final WorkflowInstanceFactory workflowInstanceFactory;

   public ReturnController(WorkflowInstanceService workflowInstances, WorkflowInstanceFactory workflowInstanceFactory) {
      this.workflowInstances = workflowInstances;
      this.workflowInstanceFactory = workflowInstanceFactory;
   }

   @GetMapping("/index")
   @ResponseBody
   public Collection<WorkflowInstance> list() {
      return workflowInstances.listWorkflowInstances(new QueryWorkflowInstances.Builder().addStates("awaitingVendorRefund").build());
   }

   @GetMapping("/start")
   @ResponseBody
   public String start(@RequestParam(name="name", required=false, defaultValue="World") String name) {
      ReturnRequest r = new ReturnRequest();
      r.orderId = 12345L;
      r.replacement = false;
      r.toVendor = false;

      workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
              .setType("returnApplication")
              .setExternalId(UUID.randomUUID().toString())
              .putStateVariable("requestData", r)
              .build());
      return name;
   }

   @GetMapping("/approve")
   @ResponseBody
   public String example(@RequestParam(name="id", required=false) Long id) {
      String state = "approved";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/toStoreReceived")
   @ResponseBody
   public String toStoreReceived(@RequestParam(name="id", required=false) Long id) {
      String state = "toStoreReceived";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/toVendorReceived")
   @ResponseBody
   public String toVendorReceived(@RequestParam(name="id", required=false) Long id) {
      String state = "toVendorReceived";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/vendorRefunded")
   @ResponseBody
   public String vendorRefunded(@RequestParam(name="id", required=false) Long id) {
      String state = "vendorRefunded";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/vendorRefundVerified")
   @ResponseBody
   public String vendorRefundVerified(@RequestParam(name="id", required=false) Long id) {
      String state = "vendorRefundVerified";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/customerRefunded")
   @ResponseBody
   public String customerRefunded(@RequestParam(name="id", required=false) Long id) {
      String state = "customerRefunded";
      Boolean b = execute(state, id);
      return b.toString();
   }

   @GetMapping("/error")
   @ResponseBody
   public String error(@RequestParam(name="id", required=false) Long id) {
      String state = "error";
      Boolean b = execute(state, id);
      return b.toString();
   }

   private Boolean execute(String state, Long id)  {
      WorkflowInstance i = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.ACTIONS), 2L);
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(i).setState(state).setType(WorkflowInstanceAction.WorkflowActionType.externalChange).setExecutionStart(DateTime.now())
              .build();
      return workflowInstances.updateWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder().setNextActivation(DateTime.now()).setId(id).setStatus(WorkflowInstance.WorkflowInstanceStatus.inProgress).setState(state).build(), action);
   }
}