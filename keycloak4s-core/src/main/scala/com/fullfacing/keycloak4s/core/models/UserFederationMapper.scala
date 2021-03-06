package com.fullfacing.keycloak4s.core.models

final case class UserFederationMapper(config: Option[Map[String, String]],
                                      federationMapperType: Option[String],
                                      federationProviderDisplayName: Option[String],
                                      id: Option[String],
                                      name: Option[String])
