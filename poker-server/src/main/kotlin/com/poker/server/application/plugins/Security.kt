package com.poker.server.application.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Calendar

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.audience.contains(realm)) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
    }
}

object JwtConfig {
    private const val SECRET = "your-secret-key" // Change this to a secure, random key in production
    private const val ISSUER = "Poker Server"
    val realm = "Poker Server"

    val verifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(SECRET))
        .withIssuer(ISSUER)
        .withAudience(realm)
        .build()

    fun makeToken(subject: String): String {
        // Create the HMAC256 algorithm with the secret key
        val algorithm = Algorithm.HMAC256(SECRET)
        val exp = Calendar.getInstance().run {
            add(Calendar.SECOND, 2000)
            time
        }
        // Set the "not before" time to 5 seconds from now
        val nbf = Calendar.getInstance().run {
            add(Calendar.SECOND, 5)
            time
        }
        // Create and sign the JWT
        return JWT.create()
            .withIssuer(ISSUER)
            .withExpiresAt(exp)
            .withNotBefore(nbf)
            .withAudience(realm)
            .sign(algorithm)
    }
}
