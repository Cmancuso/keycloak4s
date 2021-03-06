package com.fullfacing.keycloak4s.core.models

import com.fullfacing.keycloak4s.core.models.enums.PolicyEnforcementMode

final case class ResourceServer(allowRemoteResourceManagement: Option[Boolean],
                                clientId: Option[String],
                                id: Option[String],
                                name: Option[String],
                                policies: Option[List[Policy]],
                                policyEnforcementMode: Option[List[PolicyEnforcementMode]],
                                resources: Option[List[Resource]],
                                scopes: Option[List[Scope]])