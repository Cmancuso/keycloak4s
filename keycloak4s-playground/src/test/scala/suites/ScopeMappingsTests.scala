package suites

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import cats.data.EitherT
import com.fullfacing.keycloak4s.core.models._
import monix.eval.Task
import org.scalatest.DoNotDiscover
import utils.{Errors, IntegrationSpec}

@DoNotDiscover
class ScopeMappingsTests extends IntegrationSpec {

  private val rRole1Name = "realmRole1"
  val rRole1Create: Role.Create = Role.Create(
    clientRole = false,
    composite  = false,
    name       = rRole1Name
  )

  private val rRole2Name = "realmRole2"
  val rRole2Create: Role.Create = Role.Create(
    clientRole = false,
    composite  = false,
    name       = rRole2Name
  )

  private val cRole1Name = "clientRole1"
  val cRole1Create: Role.Create = Role.Create(
    clientRole = true,
    composite  = false,
    name       = cRole1Name
  )

  private val cRole2Name = "clientRole2"
  val cRole2Create: Role.Create = Role.Create(
    clientRole = true,
    composite  = false,
    name       = cRole2Name
  )

  val clientCreate = Client.Create("scope-mappings-test")

  val scope1Create: ClientScope.Create = ClientScope.Create(
    name = "scope1"
  )

  val scope2Create: ClientScope.Create = ClientScope.Create(
    name = "scope2"
  )

  private val rRole1     = new AtomicReference[UUID]()
  private val rRole2     = new AtomicReference[UUID]()
  private val cRole1     = new AtomicReference[UUID]()
  private val cRole2     = new AtomicReference[UUID]()
  private val clientUuid = new AtomicReference[UUID]()
  private val scope1     = new AtomicReference[UUID]()
  private val scope2     = new AtomicReference[UUID]()


  "Create Ancillary Objects" should "create all objects needed to test all the ScopeMappings service calls" in {
    val task =
      for {
        _ <- EitherT(clientService.create(Client.Create("scope-mappings-test")))
        c <- EitherT(clientService.fetch(clientId = Some("scope-mappings-test")))
        c1 <- EitherT.fromOption[Task](c.headOption, Errors.CLIENT_NOT_FOUND)
        _ =  clientUuid.set(c1.id)
        _ <- EitherT(realmRoleService.create(rRole1Create))
        _ <- EitherT(realmRoleService.create(rRole2Create))
        _ <- EitherT(clientRoleService.create(clientUuid.get(), cRole1Create))
        _ <- EitherT(clientRoleService.create(clientUuid.get(), cRole2Create))
        _ <- EitherT(clientService.createClientScope(scope1Create))
        r <- EitherT(clientService.createClientScope(scope2Create))
      } yield r

    task.value.shouldReturnSuccess
  }

  "Fetch Ancillary Object's UUIDs" should "retrieve the created objects and store their IDs" in {
    val task: EitherT[Task, KeycloakError, Unit] =
      for {
        rr  <- EitherT(realmRoleService.fetch())
        cr  <- EitherT(clientRoleService.fetch(clientUuid.get()))
        s   <- EitherT(clientService.fetchClientScopes())
        r1  <- EitherT.fromOption[Task](rr.find(_.name == rRole1Name), Errors.ROLE_NOT_FOUND)
        r2  <- EitherT.fromOption[Task](rr.find(_.name == rRole2Name), Errors.ROLE_NOT_FOUND)
        c1  <- EitherT.fromOption[Task](cr.find(_.name == cRole1Name), Errors.ROLE_NOT_FOUND)
        c2  <- EitherT.fromOption[Task](cr.find(_.name == cRole2Name), Errors.ROLE_NOT_FOUND)
        s1  <- EitherT.fromOption[Task](s.find(_.name == "scope1"), Errors.SCOPE_NOT_FOUND)
        s2  <- EitherT.fromOption[Task](s.find(_.name == "scope2"), Errors.SCOPE_NOT_FOUND)
      } yield {
        rRole1.set(r1.id)
        rRole2.set(r2.id)
        cRole1.set(c1.id)
        cRole2.set(c2.id)
        scope1.set(s1.id)
        scope2.set(s2.id)
      }

    task.value.shouldReturnSuccess
  }

  "fetch" should "not return any role mappings" in {
    val task =
      for {
        s1 <- EitherT(scopeMapService.fetch(scope1.get()))
        s2 <- EitherT(scopeMapService.fetch(scope2.get()))
      } yield {
        s1.clientMappings shouldBe Map.empty[String, ClientMappings]
        s1.realmMappings  shouldBe List.empty[Role]
        s2.clientMappings shouldBe Map.empty[String, ClientMappings]
        s2.realmMappings  shouldBe List.empty[Role]
      }

    task.value.shouldReturnSuccess
  }

  "addClientRoles" should "map a scope to a client role" in {
    scopeMapService.addClientRoles(scope1.get(), clientUuid.get(), List(cRole1Name))
      .shouldReturnSuccess
  }

  "fetchClientRoles" should "retrieve all client roles mapped to this scope" in {
    val task =
      EitherT(scopeMapService.fetchClientRoles(scope1.get(), clientUuid.get())).map { s =>
        s.size                   shouldBe 1
        s.headOption.map(_.name) shouldBe Some(cRole1Name)
      }

    task.value.shouldReturnSuccess
  }

  "fetchAvailableClientRoles" should "retrieve all client roles to which this scope can be mapped" in {
    val task =
      EitherT(scopeMapService.fetchAvailableClientRoles(scope1.get(), clientUuid.get())).map { s =>
        s.nonEmpty shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "fetchEffectiveClientRoles" should "retrieve all client roles, along with their sub roles, mapped to this scope" in {
    val task =
      for {
        cb <- EitherT(scopeMapService.fetchEffectiveClientRoles(scope1.get(), clientUuid.get()))
        rb <- EitherT(scopeMapService.fetchEffectiveRealmRoles(scope1.get()))
        _ <- EitherT(clientRoleService.addCompositeRoles(clientUuid.get(), cRole1Name, List(cRole2.get(), rRole2.get())))
        ca <- EitherT(scopeMapService.fetchEffectiveClientRoles(scope1.get(), clientUuid.get()))
        ra <- EitherT(scopeMapService.fetchEffectiveRealmRoles(scope1.get()))
        _ <- EitherT(clientRoleService.removeCompositeRoles(clientUuid.get(), cRole1Name, List(cRole2.get(), rRole2.get())))
      } yield {
        cb.exists(_.name == cRole1Name) shouldBe true
        cb.exists(_.name == cRole2Name) shouldBe false
        rb.exists(_.name == rRole2Name) shouldBe false
        ca.exists(_.name == cRole1Name) shouldBe true
        ca.exists(_.name == cRole2Name) shouldBe true
        ra.exists(_.name == rRole2Name) shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "removeClientRoles" should "remove all client role mappings from this scope" in {
    val task =
      for {
        _ <- EitherT(scopeMapService.removeClientRoles(scope1.get(), clientUuid.get(), List(cRole1Name)))
        s <- EitherT(scopeMapService.fetch(scope1.get()))
      } yield {
        s.clientMappings.isEmpty shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "addRealmRoles" should "map a scope to a client role" in {
    scopeMapService.addRealmRoles(scope2.get(), List(rRole1.get()))
      .shouldReturnSuccess
  }

  "fetchRealmRoles" should "retrieve all client roles mapped to this scope" in {
    val task =
      EitherT(scopeMapService.fetchRealmRoles(scope2.get())).map { s =>
        s.size                   shouldBe 1
        s.headOption.map(_.name) shouldBe Some(rRole1Name)
      }

    task.value.shouldReturnSuccess
  }

  "fetchAvailableRealmRoles" should "retrieve all client roles to which this scope can be mapped" in {
    val task =
      EitherT(scopeMapService.fetchAvailableRealmRoles(scope2.get())).map { s =>
        s.nonEmpty shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "fetchEffectiveRealmRoles" should "retrieve all client roles, along with their sub roles, mapped to this scope" in {
    val task =
      for {
        rb <- EitherT(scopeMapService.fetchEffectiveRealmRoles(scope2.get()))
        cb <- EitherT(scopeMapService.fetchEffectiveClientRoles(scope2.get(), clientUuid.get()))
        _ <- EitherT(realmRoleService.addCompositeRoles(rRole1Name, List(cRole2.get(), rRole2.get())))
        ra <- EitherT(scopeMapService.fetchEffectiveRealmRoles(scope2.get()))
        ca <- EitherT(scopeMapService.fetchEffectiveClientRoles(scope2.get(), clientUuid.get()))
        _ <- EitherT(realmRoleService.removeCompositeRoles(rRole1Name, List(cRole2.get(), rRole2.get())))
      } yield {
        rb.exists(_.name == rRole1Name) shouldBe true
        cb.exists(_.name == cRole2Name) shouldBe false
        rb.exists(_.name == rRole2Name) shouldBe false
        ra.exists(_.name == rRole1Name) shouldBe true
        ca.exists(_.name == cRole2Name) shouldBe true
        ra.exists(_.name == rRole2Name) shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "removeRealmRoles" should "remove all client role mappings from this scope" in {
    val task =
      for {
        _ <- EitherT(scopeMapService.removeRealmRoles(scope1.get(), List(rRole1.get())))
        s <- EitherT(scopeMapService.fetch(scope1.get()))
      } yield {
        s.clientMappings.isEmpty shouldBe true
      }

    task.value.shouldReturnSuccess
  }

  "Delete Ancillary Objects" should "delete all objects needed to test all the ScopeMappings service calls" in {
    val task =
      for {
        _ <- EitherT(clientService.deleteClientScope(scope1.get()))
        _ <- EitherT(clientService.deleteClientScope(scope2.get()))
        _ <- EitherT(realmRoleService.remove(rRole1Name))
        _ <- EitherT(realmRoleService.remove(rRole2Name))
        _ <- EitherT(clientRoleService.remove(clientUuid.get(), cRole1Name))
        _ <- EitherT(clientRoleService.remove(clientUuid.get(), cRole2Name))
        r <- EitherT(clientService.delete(clientUuid.get()))
      } yield r

    task.value.shouldReturnSuccess
  }
}