package com.badals.flow.payload;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AbstractRequest {

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }

}