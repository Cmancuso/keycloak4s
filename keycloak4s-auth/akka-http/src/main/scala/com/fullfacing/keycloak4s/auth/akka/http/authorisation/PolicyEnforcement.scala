package com.fullfacing.keycloak4s.auth.akka.http.authorisation

import com.fullfacing.keycloak4s.auth.akka.http.Logging
import com.fullfacing.keycloak4s.core.Exceptions

import scala.io.{BufferedSource, Source}

object PolicyEnforcement {

  /**
   * Attempts to build an Authorisation object from a JSON configuration in resources.
   * Throws an Exception in case of failure.
   */
  private def attemptBuild(filename: String): BufferedSource = {
    val url = getClass.getResource(s"/$filename")

    try {
      if (url == null) {
        throw Exceptions.CONFIG_NOT_FOUND(filename)
      } else try {
        Source.fromFile(url.getPath)
      } catch {
        case th: Throwable => Logging.configSetupError(); throw th
      }
    }
  }

  /**
   * Builds a NodeAuthorisation object from a JSON configuration file using a node structure.
   * The JSON file must be located in the resources directory.
   *
   * @param filename The file name and extension of the JSON configuration inside the Resources directory.
   *                 Example: config.json
   */
  def buildNodeAuthorisation(filename: String): NodeAuthorisation = {
    val source = attemptBuild(filename)
    NodeAuthorisation(source.mkString.stripMargin)
  }

  /**
   * Builds a PathAuthorisation object from a JSON configuration file using a path structure.
   * The JSON file must be located in the resources directory.
   *
   * @param filename The file name and extension of the JSON configuration inside the Resources directory.
   *                 Example: config.json
   */
  def buildPathAuthorisation(filename: String): PathAuthorisation = {
    val source = attemptBuild(filename)
    PathAuthorisation(source.mkString.stripMargin)
  }
}
