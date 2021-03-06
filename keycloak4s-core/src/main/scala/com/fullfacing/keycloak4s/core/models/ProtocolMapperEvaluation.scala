package com.fullfacing.keycloak4s.core.models

final case class ProtocolMapperEvaluation(containerId: Option[String],
                                          containerName: Option[String],
                                          containerType: Option[String],
                                          mapperId: Option[String],
                                          mapperName: Option[String],
                                          protocolMapper: Option[String])
